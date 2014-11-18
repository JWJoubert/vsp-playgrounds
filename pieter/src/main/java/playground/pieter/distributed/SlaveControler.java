package playground.pieter.distributed;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleReaderV1;
import playground.pieter.pseudosimulation.mobsim.PSimFactory;
import playground.singapore.scoring.CharyparNagelOpenTimesScoringFunctionFactory;
import playground.singapore.transitRouterEventsBased.TransitRouterWSImplFactory;
import playground.singapore.transitRouterEventsBased.stopStopTimes.StopStopTime;
import playground.singapore.transitRouterEventsBased.stopStopTimes.StopStopTimeCalculatorSerializable;
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTime;
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTimeCalculatorSerializable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

//IMPORTANT: PSim produces events that are not in chronological order. This controler
// will require serious overhaul if chronological order is enforced in all event manager implementations
public class SlaveControler implements IterationStartsListener, StartupListener, Runnable {
    private final Scenario scenario;
    private final MemoryUsageCalculator memoryUsageCalculator;
    private final ReplaceableTravelTime travelTime;
    private boolean initialRouting;
    private final Logger slaveLogger;
    private final int myNumber;
    private int numberOfPSimIterations;
    private int numberOfIterations = -1;
    private Config config;
    private double totalIterationTime;
    private Controler matsimControler;
    private TravelTime linkTravelTimes;
    private WaitTime waitTimes;
    private StopStopTime stopStopTimes;
    private ObjectInputStream reader;
    private ObjectOutputStream writer;
    private PSimFactory pSimFactory;
    private Map<String, PlanSerializable> plansCopyForSending;
    private List<Long> iterationTimes = new ArrayList<>();
    private long lastIterationStartTime;
    private boolean somethingWentWrong = false;
    private long fSLEEP_INTERVAL = 100;
    private boolean isOkForNextIter = true;

    private SlaveControler(String[] args) throws IOException, ClassNotFoundException, ParseException {
        lastIterationStartTime = System.currentTimeMillis();
        System.setProperty("matsim.preferLocalDtds", "true");
        Options options = new Options();
        options.addOption("c", true, "Config file location");
        options.addOption("h", true, "Host name or IP");
        options.addOption("p", true, "Port number of MasterControler");
        options.addOption("s", false, "Switch to indicate if this is the Singapore scenario, i.e. events-based routing");
        options.addOption("t", true, "Number of threads for parallel events handling.");
        CommandLineParser parser = new BasicParser();
        CommandLine commandLine = parser.parse(options, args);


        if (commandLine.hasOption("c")) {
            try {
                config = ConfigUtils.loadConfig(commandLine.getOptionValue("c"));

            } catch (UncheckedIOException e) {
                System.err.println("Config file not found");
                System.out.println(options.toString());
                System.exit(1);
            }
        } else {
            System.err.println("Config file not specified");
            System.out.println(options.toString());
            System.exit(1);
        }

        //The following line will make the controler use the events manager that doesn't check for event order
        config.parallelEventHandling().setSynchronizeOnSimSteps(false);
        //if you don't set the number of threads, org.matsim.core.events.EventsUtils will just use the simstepmanager
        int numThreadsForEventsHandling = 1;
        if (commandLine.hasOption("t"))
            try {
                numThreadsForEventsHandling = Integer.parseInt(commandLine.getOptionValue("t"));
            } catch (NumberFormatException e) {
                System.err.println("Number of event handling threads should be integer.");
                System.out.println(options.toString());
                System.exit(1);
            }
        else {
            System.err.println("Will use the default of a single thread for events handling.");
        }
        config.parallelEventHandling().setNumberOfThreads(numThreadsForEventsHandling);

        String hostname = "localhost";
        if (commandLine.hasOption("h")) {
            hostname = commandLine.getOptionValue("h");
        } else
            System.err.println("No host specified, using default (localhost)");




        /*
        * INITIALIZING COMMS
        * */
        Socket socket;
        int socketNumber = 12345;
        if (commandLine.hasOption("p")) {
            try {
                socketNumber = Integer.parseInt(commandLine.getOptionValue("p"));
            } catch (NumberFormatException e) {
                System.err.println("Port number should be integer");
                System.out.println(options.toString());
                System.exit(1);
            }
        } else {
            System.err.println("Will accept connections on default port number 12345");
        }
        socket = new Socket(hostname, socketNumber);
        this.reader = new ObjectInputStream(socket.getInputStream());
        this.writer = new ObjectOutputStream(socket.getOutputStream());

        myNumber = reader.readInt();
        slaveLogger = Logger.getLogger(("SLAVE_" + myNumber));

        numberOfPSimIterations = reader.readInt();
        slaveLogger.warn("Running " + numberOfPSimIterations + " PSim iterations for every QSim iter");

        initialRouting = reader.readBoolean();
        if (initialRouting) slaveLogger.warn("Performing initial routing.");

        memoryUsageCalculator = new MemoryUsageCalculator();
        writeMemoryStats();
        writer.writeInt(config.global().getNumberOfThreads());
        writer.flush();


        config.controler().setOutputDirectory(config.controler().getOutputDirectory() + "_" + myNumber);
        //limit IO
        config.linkStats().setWriteLinkStatsInterval(0);
        config.controler().setCreateGraphs(false);
        config.controler().setWriteEventsInterval(0);
        config.controler().setWritePlansInterval(0);
        config.controler().setWriteSnapshotsInterval(0);
        scenario = ScenarioUtils.createScenario(config);
        //assume basic scenario for now, can create a loadScenario later
        loadScenario();


        matsimControler = new Controler(scenario);
        matsimControler.setOverwriteFiles(true);
        matsimControler.addControlerListener(this);
        linkTravelTimes = new FreeSpeedTravelTime();
        travelTime = new ReplaceableTravelTime();
        travelTime.setTravelTime(linkTravelTimes);
        matsimControler.setModules(new AbstractModule() {
            @Override
            public void install() {
                bindToInstance(TravelTime.class, travelTime);
            }
        });
//        new Thread(new TimesReceiver()).start();
        if (config.scenario().isUseTransit()) {

            stopStopTimes = new StopStopTimeCalculatorSerializable(scenario.getTransitSchedule(),
                    config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config
                    .qsim().getEndTime() - config.qsim().getStartTime())).getStopStopTimes();

            waitTimes = new WaitTimeCalculatorSerializable(scenario.getTransitSchedule(),
                    config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config
                    .qsim().getEndTime() - config.qsim().getStartTime())).getWaitTimes();

            //tell PlanSerializable to record transit routes
            PlanSerializable.isUseTransit = true;
        }

        if (commandLine.hasOption("s")) {
            slaveLogger.warn("Singapore scenario: Doing events-based transit routing.");
            //this is a fix for location choice to work with pt, by sergioo
            //in location choice, if the facility's link doesn't accommodate the mode you're using,
            //then it won't allow you to go there
            for (Link link : scenario.getNetwork().getLinks().values()) {
                Set<String> modes = new HashSet<>(link.getAllowedModes());
                modes.add("pt");
                link.setAllowedModes(modes);
            }
            //this is some more magic hacking to get location choice by car to work, by sergioo
            //sergioo creates a car-only network, then associates each activity and facility with a car link.
            Set<String> carMode = new HashSet<>();
            carMode.add("car");
            NetworkImpl justCarNetwork = NetworkImpl.createNetwork();
            new TransportModeNetworkFilter(scenario.getNetwork()).filter(justCarNetwork, carMode);
            for (Person person : scenario.getPopulation().getPersons().values())
                for (PlanElement planElement : person.getSelectedPlan().getPlanElements())
                    if (planElement instanceof Activity)
                        ((ActivityImpl) planElement).setLinkId(justCarNetwork.getNearestLinkExactly(((ActivityImpl) planElement).getCoord()).getId());
            for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values())
                ((ActivityFacilityImpl) facility).setLinkId(justCarNetwork.getNearestLinkExactly(facility.getCoord()).getId());
            //the singapore scenario uses intelligent transit routing that takes account of information of the previous iteration
            matsimControler.setTransitRouterFactory(new TransitRouterWSImplFactory(scenario, waitTimes, stopStopTimes));
            //the singapore scoring function
            matsimControler.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(config.planCalcScore(), scenario));
        }
        //no use for this, if you don't exactly know the communicationsMode of population when something goes wrong.
        // better to have plans written out every n successful iterations, specified in the config
        matsimControler.setDumpDataAtEnd(false);
    }

    private void writeMemoryStats() throws IOException {
        writer.writeLong(memoryUsageCalculator.getMemoryUse());
        writer.writeLong(Runtime.getRuntime().maxMemory());
        writer.writeInt(getTotalNumberOfPlans());
    }

    private int getTotalNumberOfPlans() {
        int total = 0;
        try {
            for (Person person : scenario.getPopulation().getPersons().values()) {
                total += person.getPlans().size();
            }
        } catch (NullPointerException e) {
        }
        return total;
    }

    /**
     * currently just standard scenario without attribute files and fancy stuff. should be overridden for fancier scenarios.
     */
    private void loadScenario() {
        slaveLogger.warn("Loading scenario. Currently just standard scenario without attribute files and fancy stuff. should be overridden for fancier scenarios.");
        new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
        new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());
        if (config.scenario().isUseTransit()) {
            new TransitScheduleReader(scenario).readFile(config.transit().getTransitScheduleFile());
            if (this.config.scenario().isUseVehicles()) {
                new VehicleReaderV1(this.scenario.getVehicles()).readFile(this.config.transit().getVehiclesFile());
            }
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, ParseException {
        SlaveControler slave = new SlaveControler(args);
        new Thread(slave).start();
        System.out.printf("Enter KILL to kill the slave: ");
        Scanner in = new Scanner(System.in);
        String s;
        boolean running = true;
        do{
            s = in.nextLine();
            if(s.equals("KILL"))
                running=false;
        }while(running);
        slave.requestShutDown();
    }

    @Override
    public void run() {
        pSimFactory = new PSimFactory();
        matsimControler.setMobsimFactory(pSimFactory);
        matsimControler.run();
    }

    public void requestShutDown(){
        isOkForNextIter = false;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (numberOfIterations >= 0 || initialRouting)
            iterationTimes.add(System.currentTimeMillis() - lastIterationStartTime);

        if (initialRouting || (numberOfIterations > 0 && numberOfIterations % numberOfPSimIterations == 0)) {
            this.totalIterationTime = getTotalIterationTime();
            communications();
            if (somethingWentWrong) Runtime.getRuntime().halt(0);
            initialRouting = false;
        }
        lastIterationStartTime = System.currentTimeMillis();
        travelTime.setTravelTime(linkTravelTimes);
        pSimFactory.setTravelTime(linkTravelTimes);
        if (config.scenario().isUseTransit()) {
            pSimFactory.setStopStopTime(stopStopTimes);
            pSimFactory.setWaitTime(waitTimes);
            if (matsimControler.getTransitRouterFactory() instanceof TransitRouterWSImplFactory) {
                ((TransitRouterWSImplFactory) matsimControler.getTransitRouterFactory()).setStopStopTime(stopStopTimes);
                ((TransitRouterWSImplFactory) matsimControler.getTransitRouterFactory()).setWaitTime(waitTimes);
            }
        }
        Collection<Plan> plans = new ArrayList<>();
        for (Person person : matsimControler.getScenario().getPopulation().getPersons().values())
            plans.add(person.getSelectedPlan());
        pSimFactory.setPlans(plans);
        numberOfIterations++;
    }

    private void removeNonSimulatedAgents(List<String> idStrings) {
        Set<Id<Person>> noIds = new HashSet<>(matsimControler.getScenario().getPopulation().getPersons().keySet());
        Set<String> noIdStrings = new HashSet<>();
        for (Id<Person> id : noIds)
            noIdStrings.add(id.toString());
        noIdStrings.removeAll(idStrings);
        slaveLogger.warn("removing ids");
        for (String idString : noIdStrings) {
            matsimControler.getScenario().getPopulation().getPersons().remove(Id.create(idString, Person.class));
        }

    }

    public double getTotalIterationTime() {
        double sumTimes = 0;
        for (long t : iterationTimes) {
            sumTimes += t;
        }
        return sumTimes;
    }


    private void addPersons(List<PersonSerializable> persons) {
        for (PersonSerializable person : persons) {
            matsimControler.getScenario().getPopulation().addPerson(person.getPerson());
        }
        slaveLogger.warn("Added " + persons.size() + " pax to my population.");
    }

    private List<PersonSerializable> getPersonsToSend(int diff) {
        int i = 0;
        List<PersonSerializable> personsToSend = new ArrayList<>();
        Set<Id<Person>> personIdsToRemove = new HashSet<>();
        for (Id<Person> personId : matsimControler.getScenario().getPopulation().getPersons().keySet()) {
            if (i++ >= diff) break;
            personsToSend.add(new PersonSerializable((PersonImpl) matsimControler.getScenario().getPopulation().getPersons().get(personId)));
            personIdsToRemove.add(personId);
        }
        for (Id<Person> personId : personIdsToRemove)
            matsimControler.getScenario().getPopulation().getPersons().remove(personId);
        return personsToSend;
    }

    public void transmitPlans() throws IOException, ClassNotFoundException {
        Map<String, PlanSerializable> tempPlansCopyForSending = new HashMap<>();
        for (Person person : matsimControler.getScenario().getPopulation().getPersons().values())
            tempPlansCopyForSending.put(person.getId().toString(), new PlanSerializable(person.getSelectedPlan()));
        plansCopyForSending = tempPlansCopyForSending;
        slaveLogger.warn("Sending plans...");
        writer.writeObject(plansCopyForSending);
        slaveLogger.warn("Sending completed.");

    }

    public void transmitTravelTimes() throws IOException, ClassNotFoundException {
        slaveLogger.warn("RECEIVING travel times...");
        linkTravelTimes = (SerializableLinkTravelTimes) reader.readObject();
        if (config.scenario().isUseTransit()) {
            stopStopTimes = (StopStopTime) reader.readObject();
            waitTimes = (WaitTime) reader.readObject();
        }
        slaveLogger.warn("RECEIVING travel times completed.");
    }

    public void transmitPerformance() throws IOException {
        if (totalIterationTime > 0) {
            slaveLogger.warn("Spent a total of " + totalIterationTime +
                    " running " + plansCopyForSending.size() +
                    " person plans for " + numberOfPSimIterations +
                    " PSim iterations.");
        }
        writer.writeDouble(totalIterationTime);
        writer.writeInt(matsimControler.getScenario().getPopulation().getPersons().size());
        //send memory usage fraction of max to prevent being assigned more persons
        writeMemoryStats();

    }

    private double getMemoryUse() {
        putOutTheGarbage();
        double totalMemory = Runtime.getRuntime().totalMemory();
        putOutTheGarbage();
        double freeMemory = Runtime.getRuntime().freeMemory();
        double usedMemoryEst = totalMemory - freeMemory;
        double maxMemory = Runtime.getRuntime().maxMemory();
        return usedMemoryEst / maxMemory;
    }

    private void putOutTheGarbage() {
        collectGarbage();
        collectGarbage();
    }

    private void collectGarbage() {
        try {
            System.gc();
            Thread.currentThread().sleep(fSLEEP_INTERVAL);
            System.runFinalization();
            Thread.currentThread().sleep(fSLEEP_INTERVAL);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            somethingWentWrong = true;
        }
    }

    public void distributePersons() throws IOException, ClassNotFoundException {
        addPersons((List<PersonSerializable>) reader.readObject());
        iterationTimes = new ArrayList<>();
        slaveLogger.warn("Load balancing done. waiting for others to finish...");
    }

    public void poolPersons() throws IOException {
        slaveLogger.warn("Load balancing...");
        int diff = reader.readInt();
        slaveLogger.warn("Received " + diff + " as lb instr from master");
        List<PersonSerializable> personsToSend = new ArrayList<>();
        if (diff > 0) {
            personsToSend = getPersonsToSend(diff);
        }
        writer.writeObject(personsToSend);
        slaveLogger.warn("Sent " + personsToSend.size() + " pax to master");
    }

    public void communications() {
        CommunicationsMode communicationsMode = CommunicationsMode.WAIT;
        slaveLogger.warn("Initializing communications...");
        try {
            while (!communicationsMode.equals(CommunicationsMode.CONTINUE)) {
                communicationsMode = (CommunicationsMode) reader.readObject();
                switch (communicationsMode) {
                    case TRANSMIT_SCENARIO:
                        distributePersons();
                        break;
                    case TRANSMIT_TRAVEL_TIMES:
                        transmitTravelTimes();
                        break;
                    case POOL_PERSONS:
                        poolPersons();
                        break;
                    case DISTRIBUTE_PERSONS:
                        distributePersons();
                        break;
                    case TRANSMIT_PLANS_TO_MASTER:
                        transmitPlans();
                        slaveIsOKForNextIter();
                        break;
                    case TRANSMIT_PERFORMANCE:
                        transmitPerformance();
                        break;
                    case CONTINUE:
                        break;
                    case WAIT:
                        Thread.sleep(10);
                        break;
                    case DIE:
                        slaveLogger.error("Got the kill signal from MASTER. Bye.");
                        Runtime.getRuntime().halt(0);
                        break;
                }
                // sending a boolean forces the thread on the master to wait
                writer.writeBoolean(true);
                writer.flush();
            }
            writer.reset();
        } catch (ClassNotFoundException | IOException | InterruptedException e) {
            e.printStackTrace();
            slaveLogger.error("Something went wrong. Exiting.");
            somethingWentWrong = true;
            return;
        }
        slaveLogger.warn("Communications completed.");
    }

    private void slaveIsOKForNextIter() throws IOException {
        writer.writeBoolean(isOkForNextIter);
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        communications();
    }
}

class ReplaceableTravelTime implements TravelTime{
    private TravelTime delegate;

    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return this.delegate.getLinkTravelTime(link,time,person,vehicle);
    }

    public void setTravelTime(TravelTime linkTravelTimes) {
        this.delegate =linkTravelTimes;
    }
}