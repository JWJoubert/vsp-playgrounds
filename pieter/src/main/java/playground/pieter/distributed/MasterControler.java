package playground.pieter.distributed;


import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;
import playground.pieter.pseudosimulation.util.CollectionUtils;
import playground.singapore.ptsim.qnetsimengine.PTQSimFactory;
import playground.singapore.scoring.CharyparNagelOpenTimesScoringFunctionFactory;
import playground.singapore.transitRouterEventsBased.stopStopTimes.StopStopTimeCalculatorSerializable;
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTimeCalculatorSerializable;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MasterControler implements AfterMobsimListener, ShutdownListener, StartupListener, IterationStartsListener {
    public static final Logger masterLogger = Logger.getLogger(MasterControler.class);
    private final HashMap<String, Plan> newPlans = new HashMap<>();
    private final long startMillis;
    private final Hydra hydra;
    private int writeFullPlansInterval;
    private long bytesPerPerson;
    private Scenario scenario;
    private long scenarioMemoryUse;
    private long bytesPerPlan;
    private boolean initialRoutingOnSlaves = false;
    private int numberOfPSimIterations = 5;
    private Config config;
    private Controler matsimControler;
    private TreeMap<Integer, Slave> slaves;
    private WaitTimeCalculatorSerializable waitTimeCalculator;
    private StopStopTimeCalculatorSerializable stopStopTimeCalculator;
    private SerializableLinkTravelTimes linkTravelTimes;
    private AtomicInteger numThreads = new AtomicInteger(0);
    private List<PersonSerializable> personPool;
    private int loadBalanceInterval;
    private boolean isLoadBalanceIteration;
    private boolean somethingWentWrong = false;
    private int socketNumber = 12345;
    public static double planAllocationLimiter = 10.0;
    public static final long bytesPerSlaveBuffer = (long) 2e8;
    public int slaveUniqueNumber = 0;

    /**
     * value between 0 and 1; increasing it increases the dampening effect of preventing
     * large transfers of persons during load balance iterations
     */
    private static final double loadBalanceDampeningFactor = 0.4;

    public MasterControler(String[] args) throws NumberFormatException, IOException, ParseException, InterruptedException {
        startMillis = System.currentTimeMillis();
        System.setProperty("matsim.preferLocalDtds", "true");
        Options options = new Options();
        options.addOption("c", true, "Config file location");
        options.addOption("p", true, "Port number of MasterControler");
        options.addOption("s", false, "Switch to indicate if this is the Singapore scenario, i.e. events-based routing");
        options.addOption("n", true, "Number of slaves to distribute to.");
        options.addOption("i", true, "Number of PSim iterations for every QSim iteration.");
        options.addOption("r", false, "Perform initial routing of plans on slaves.");
        options.addOption("l", true, "Number of iterations between load balancing. Default = 5");
        options.addOption("w", true, "Number of iterations between dumping plans from all slaves. Defaults to the value in the config (so disabled if set to zero).");
        CommandLineParser parser = new BasicParser();
        CommandLine commandLine = parser.parse(options, args);
        int numSlaves = 1;
        if (commandLine.hasOption("n"))
            numSlaves = Integer.parseInt(commandLine.getOptionValue("n"));
        else {
            System.err.println("Unspecified number of slaves. Will start with the default of a single slave.");
        }
        if (commandLine.hasOption("i")) {
            numberOfPSimIterations = Integer.parseInt(commandLine.getOptionValue("i"));
            masterLogger.warn("Running  " + numberOfPSimIterations + " PSim iterations for every QSim iteration run on the master");
        } else {
            masterLogger.warn("Unspecified number of PSim iterations for every QSim iteration run on the master.");
            masterLogger.warn("Using default value of " + numberOfPSimIterations);
        }
        slaves = new TreeMap<>();
        if (commandLine.hasOption("p"))
            try {
                socketNumber = Integer.parseInt(commandLine.getOptionValue("p"));
            } catch (NumberFormatException e) {
                masterLogger.warn("Port number should be integer");
                System.out.println(options.toString());
                System.exit(1);
            }
        else {
            masterLogger.warn("Will accept connections on default port number 12345");
        }
        loadBalanceInterval = 5;
        if (commandLine.hasOption("l"))
            try {
                loadBalanceInterval = Integer.parseInt(commandLine.getOptionValue("l"));
            } catch (NumberFormatException e) {
                masterLogger.warn("loadBalanceInterval number should be integer");
                System.out.println(options.toString());
                System.exit(1);
            }
        else {
            masterLogger.warn("Will perform load Balancing every 5 iterations as per default");
        }
        if (commandLine.hasOption("r")) {
            masterLogger.warn("ROUTING initial plans on slaves.");
            initialRoutingOnSlaves = true;
        }
        if (commandLine.hasOption("c")) {
            config = ConfigUtils.loadConfig(commandLine.getOptionValue("c"));
        } else {
            masterLogger.warn("Config file not specified");
            System.out.println(options.toString());
            System.exit(1);
        }


//        register initial number of slaves
        ServerSocket writeServer = new ServerSocket(socketNumber);
        for (int i = 0; i < numSlaves; i++) {
            Socket socket = writeServer.accept();
            masterLogger.warn("Slave " + (i + 1) + " out of an initial " + numSlaves + " accepted.");
            Slave slave = new Slave(socket, slaveUniqueNumber);
            slaves.put(slaveUniqueNumber, slave);
            //order is important
            slave.sendNumber(slaveUniqueNumber++);
            slave.sendNumber(numberOfPSimIterations);
            slave.sendBoolean(initialRoutingOnSlaves);
            slave.readMemoryStats();
            slave.readNumberOfThreadsOnSlave();
            Thread.sleep(1000);
        }
        writeServer.close();
        masterLogger.warn("MASTER accepted minimum number of incoming connections. All further slaves will be registered on the Hydra.");
        hydra = new Hydra();
        new Thread(hydra).start();

        scenario = ScenarioUtils.createScenario(config);
        loadScenario();
//        determine the memory use of the population for some initial load balancing
        MemoryUsageCalculator memoryUsageCalculator = new MemoryUsageCalculator();
        scenarioMemoryUse = memoryUsageCalculator.getMemoryUse();
        loadPopulation();
        long currentPopulationMemoryUse = memoryUsageCalculator.getMemoryUse() - scenarioMemoryUse;
        bytesPerPlan = currentPopulationMemoryUse / getTotalNumberOfPlansOnMaster();
        bytesPerPerson = bytesPerPlan;
        masterLogger.warn("Estimated memory use per plan is " + bytesPerPlan + " bytes");

        matsimControler = new Controler(scenario);
        matsimControler.setOverwriteFiles(true);

        this.writeFullPlansInterval = config.controler().getWritePlansInterval();
        if (commandLine.hasOption("w")) {
            writeFullPlansInterval = Integer.parseInt(commandLine.getOptionValue("w"));
            masterLogger.warn("Will dump all plans from all slaves every  " + writeFullPlansInterval + " cycles.");
        } else {
            masterLogger.warn("No interval defined for writing all plans from all slaves to disk.");
            masterLogger.warn("Will use the interval from the config " + writeFullPlansInterval);
        }

        //split the population to be sent to the slaves


        double[] totalIterationTime = new double[numSlaves];
        int[] personsPerSlave = new int[numSlaves];
        long[] usedMemoryPerSlave = new long[numSlaves];
        long[] maxMemoryPerSlave = new long[numSlaves];
        int j = 0;
        for (int i : slaves.keySet()) {
            totalIterationTime[j] = 1/(double)slaves.get(i).numThreadsOnSlave;
            personsPerSlave[j] = scenario.getPopulation().getPersons().size()/numSlaves;
            usedMemoryPerSlave[j] = slaves.get(i).usedMemory;
            maxMemoryPerSlave[j] = slaves.get(i).maxMemory;
            j++;
        }
        int[] initialWeights = getSlaveTargetPopulationSizes(totalIterationTime, personsPerSlave, maxMemoryPerSlave, usedMemoryPerSlave, bytesPerPlan, bytesPerPerson, 0.0, scenario.getPopulation().getPersons().size());
        List<PersonImpl>[] personSplit = (List<PersonImpl>[]) CollectionUtils.split(scenario.getPopulation().getPersons().values(), initialWeights);
        j = 0;
        for (int i : slaves.keySet()) {
            List<PersonSerializable> personsToSend = new ArrayList<>();
            for (PersonImpl p : personSplit[j]) {
                personsToSend.add(new PersonSerializable(p));
            }
            slaves.get(i).slavePersonPool = personsToSend;
            j++;
        }


        if (config.scenario().isUseTransit()) {
//            linkTimeCalculator =
            waitTimeCalculator = new WaitTimeCalculatorSerializable(matsimControler.getScenario().getTransitSchedule(), config.travelTimeCalculator().getTraveltimeBinSize(),
                    (int) (config.qsim().getEndTime() - config.qsim().getStartTime()));
            matsimControler.getEvents().addHandler(waitTimeCalculator);
            stopStopTimeCalculator = new StopStopTimeCalculatorSerializable(matsimControler.getScenario().getTransitSchedule(),
                    config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config.qsim()
                    .getEndTime() - config.qsim().getStartTime()));
            matsimControler.getEvents().addHandler(stopStopTimeCalculator);
            //tell PlanSerializable to record transit routes
            PlanSerializable.isUseTransit = true;
        }

        matsimControler.addPlanStrategyFactory("ReplacePlanFromSlave", new ReplacePlanFromSlaveFactory(newPlans));
        matsimControler.addControlerListener(this);
        if (commandLine.hasOption("s")) {
            masterLogger.warn("Singapore scenario: Doing events-based transit routing.");
            //our scoring function
            matsimControler.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(config.planCalcScore(), matsimControler.getScenario()));
            //this qsim engine uses our boarding and alighting model, derived from smart card data
            matsimControler.setMobsimFactory(new PTQSimFactory());
        }
    }



    private int getTotalNumberOfPlansFromSlaves() {
        int total = 0;
        for (Slave slave : slaves.values()) {
            total += slave.numberOfPlans;
        }
        return total;
    }

    private int getTotalNumberOfPlansOnMaster() {
        int total = 0;
        for (Person person : scenario.getPopulation().getPersons().values()) {
            total += person.getPlans().size();
        }
        return total;
    }

    /**
     * Currently a simple population without attributes
     */
    private void loadPopulation() {
        new MatsimPopulationReader(scenario).readFile(config.plans().getInputFile());
    }

    /**
     * currently just standard scenario without attribute files and fancy stuff. should be overridden for fancier scenarios.
     */
    private void loadScenario() {
        masterLogger.warn("Loading scenario. Currently just standard scenario without attribute files and fancy stuff. should be overridden for fancier scenarios.");
        new MatsimNetworkReader(scenario).readFile(this.config.network().getInputFile());
        new MatsimFacilitiesReader(scenario).readFile(this.config.facilities().getInputFile());
        if (this.config.scenario().isUseTransit()) {
            new TransitScheduleReader(scenario).readFile(this.config.transit().getTransitScheduleFile());
            if (this.config.scenario().isUseVehicles()) {
                new VehicleReaderV1(scenario.getVehicles()).readFile(this.config.transit().getVehiclesFile());
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        MasterControler master = null;
        try {
            master = new MasterControler(args);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            Runtime.getRuntime().halt(0);
        }
        master.run();
        Runtime.getRuntime().halt(0);
    }

    void run() {
        matsimControler.run();
    }

    public void startSlavesInMode(CommunicationsMode mode) {
        if (numThreads.get() > 0)
            masterLogger.warn("All slaves have not finished previous operation but they are being asked to " + mode.toString());
        numThreads = new AtomicInteger(slaves.size());
        for (Slave slave : slaves.values()) {
            slave.communicationsMode = mode;
            new Thread(slave).start();

        }
    }

    public void waitForSlaveThreads() {
        masterLogger.warn("Waiting for " + numThreads.get() +
                " slaves");
        while (numThreads.get() > 0)
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
        if (somethingWentWrong) {
            masterLogger.error("Something went wrong. Exiting.");
            throw new RuntimeException();
        }
        masterLogger.warn("All slaves done.");
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        startSlavesInMode(CommunicationsMode.TRANSMIT_SCENARIO);
        if (initialRoutingOnSlaves) {
            waitForSlaveThreads();
            startSlavesInMode(CommunicationsMode.TRANSMIT_PLANS_TO_MASTER);
            waitForSlaveThreads();
            mergePlansFromSlaves();
            //this code is a copy of the replanning strategy
            for (Person person : matsimControler.getScenario().getPopulation().getPersons().values()) {
                person.removePlan(person.getSelectedPlan());
                Plan plan = newPlans.get(person.getId().toString());
                person.addPlan(plan);
                person.setSelectedPlan(plan);
            }
            if (slaves.size() > 1 || slavesHaveRequestedShutdown() || hydra.hydraSlaves.size()>0)
                loadBalance();
            waitForSlaveThreads();
            startSlavesInMode(CommunicationsMode.CONTINUE);
        }
    }


    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        //wait for previous transmissions to complete, if necessary
        waitForSlaveThreads();
        //start receiving plans from slaves as the QSim runs
        startSlavesInMode(CommunicationsMode.TRANSMIT_PLANS_TO_MASTER);
        IterationStopWatch stopwatch = matsimControler.stopwatch ;
        int firstIteration = config.controler().getFirstIteration();
        if ((writeFullPlansInterval > 0) && ((event.getIteration() % writeFullPlansInterval== 0) && event.getIteration()>0
                || (event.getIteration() == (firstIteration + 1)))) {
            stopwatch.beginOperation("dump all plans from all slaves");
            masterLogger.info("dumping ALL plans from slaves, can be re-assembled afterwards into a single file...");
            waitForSlaveThreads();
            startSlavesInMode(CommunicationsMode.TRANSMIT_PERFORMANCE);
            waitForSlaveThreads();
            startSlavesInMode(CommunicationsMode.DUMP_PLANS);
            waitForSlaveThreads();
            startSlavesInMode( CommunicationsMode.CONTINUE);

            masterLogger.info("finished FULL plans dump.");
            stopwatch.endOperation("dump all plans from all slaves");
        }
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        //wating for slaves to receive plans or finish plans dumping
        waitForSlaveThreads();
        mergePlansFromSlaves();
        isLoadBalanceIteration = event.getIteration() % loadBalanceInterval == 0 || slavesHaveRequestedShutdown() || hydra.hydraSlaves.size()>0;
        //do load balancing, if necessary
        if (isLoadBalanceIteration)
            loadBalance();
        isLoadBalanceIteration = false;
        waitForSlaveThreads();
        linkTravelTimes = new SerializableLinkTravelTimes(matsimControler.getLinkTravelTimes(),
                config.travelTimeCalculator().getTraveltimeBinSize(),
                config.qsim().getEndTime(),
                scenario.getNetwork().getLinks().values());
        startSlavesInMode(CommunicationsMode.TRANSMIT_TRAVEL_TIMES);
    }

    private boolean slavesHaveRequestedShutdown() {
        for(Slave slave:slaves.values()){
            if(!slave.isOkForNextIter)
                return true;
        }
        return false;
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        //start receiving plans from slaves as the QSim runs
        startSlavesInMode(CommunicationsMode.DIE);
    }

    private void loadBalance() {
        waitForSlaveThreads();
        //add any newly registered slaves
        slaves.putAll(hydra.getNewSlaves());
        if (slaves.size() < 2)
            return;
        startSlavesInMode(CommunicationsMode.TRANSMIT_PERFORMANCE);
        waitForSlaveThreads();
        if (getTotalNumberOfPlansFromSlaves() > 0) {
            bytesPerPlan = getTotalSlavePopulationMemoryUse() / getTotalNumberOfPlansFromSlaves();
            bytesPerPerson = getTotalSlavePopulationMemoryUse() / scenario.getPopulation().getPersons().size();
        }
        personPool = new ArrayList<>();
        masterLogger.warn("About to start load balancing.");
        Set<Integer> validSlaves  = new TreeSet<>();
        Set<Integer> inValidSlaves  = new TreeSet<>();
        validSlaves.addAll(slaves.keySet());
        for(Slave slave:slaves.values()){
            if(!slave.isOkForNextIter) {
                validSlaves.remove(slave.myNumber);
                inValidSlaves.add(slave.myNumber);
                slave.targetPopulationSize = 0;
            }
        }

        double[] totalIterationTime = new double[validSlaves.size()];
        int[] personsPerSlave = new int[validSlaves.size()];
        long[] usedMemoryPerSlave = new long[validSlaves.size()];
        long[] maxMemoryPerSlave = new long[validSlaves.size()];
        int j = 0;
        for (int i : validSlaves) {
            totalIterationTime[j] = slaves.get(i).totalIterationTime;
            personsPerSlave[j] = slaves.get(i).currentPopulationSize;
            usedMemoryPerSlave[j] = slaves.get(i).usedMemory;
            maxMemoryPerSlave[j] = slaves.get(i).maxMemory;
            j++;
        }

        setSlaveTargetPopulationSizes(validSlaves,
                getSlaveTargetPopulationSizes(totalIterationTime, personsPerSlave, maxMemoryPerSlave, usedMemoryPerSlave,
                        bytesPerPlan, bytesPerPerson, loadBalanceDampeningFactor, scenario.getPopulation().getPersons().size()));
        startSlavesInMode(CommunicationsMode.POOL_PERSONS);
        waitForSlaveThreads();
        mergePersonsFromSlaves();
        masterLogger.warn("Distributing persons between  slaves");
        //kill slaves that are not ok for another round
        for(int i: inValidSlaves){
            slaves.get(i).communicationsMode = CommunicationsMode.DIE;
            new Thread(slaves.get(i)).start();
            slaves.remove(i);
        }
        startSlavesInMode(CommunicationsMode.DISTRIBUTE_PERSONS);
    }

    private long getTotalSlavePopulationMemoryUse() {
        long total = 0;
        for (Slave slave : slaves.values()) {
            total += (slave.usedMemory - scenarioMemoryUse);
        }
        return total;
    }

    private void setSlaveTargetPopulationSizes(Set<Integer> keys,int[] slaveTargetPopulationSizes) {
        int j=0;
        for(int i:keys){
            slaves.get(i).targetPopulationSize = slaveTargetPopulationSizes[j];
            j++;
        }
    }

    private void mergePlansFromSlaves() {
        newPlans.clear();
        for (Slave slave : slaves.values()) {
            newPlans.putAll(slave.plans);
        }

    }

    private synchronized List<PersonSerializable> getPersonsFromPool(int diff) throws IndexOutOfBoundsException {
        List<PersonSerializable> outList = new ArrayList<>();
        if (diff < 0) {
            for (int i = 0; i > diff; i--) {
                outList.add(personPool.get(0));
                personPool.remove(0);
            }
        }
        return outList;
    }

    private void mergePersonsFromSlaves() {
        personPool.clear();
        for (Slave loadBalanceThread : slaves.values()) {
            personPool.addAll(loadBalanceThread.getPersons());
        }
    }

    private static Map<Integer, Integer> getOptimalNumbers(int popSize, double[] timesPerPlan, Set<Integer> validSlaveIndices) {
        Map<Integer, Integer> output = new HashMap<>();
        double sumOfReciprocals = 0.0;
        for (int i : validSlaveIndices) {
            sumOfReciprocals += 1 / timesPerPlan[i];
        }
        //        find number of persons that should be allocated to each slave
        int total = 0;
        for (int i : validSlaveIndices) {
            output.put(i, ((int) Math.ceil(popSize / timesPerPlan[i] / sumOfReciprocals)));
            total += output.get(i);
        }
        int j = 0;
        while (total > popSize) {
            for (int i : validSlaveIndices) {
                output.put(i, output.get(i) - 1);
                total--;
                if (total == popSize)
                    break;
            }
        }
        return output;
    }

    public static int[] getSlaveTargetPopulationSizes(double[] totalIterationTime, int[] personsPerSlave,
                                                      long[] maxMemory, long[] usedMemory, long bytesPerPlan, long bytesPerPerson,
                                                      double dampeningFactor, int popSize) {
        int numSlaves = totalIterationTime.length;
        StringBuffer sb = new StringBuffer();
        sb.append("\n");
        sb.append(String.format("\t\t\t%20s:\t%20d\n", "bytesPerPlan", bytesPerPlan));
        sb.append(String.format("\t\t\t%20s:\t%20d\n", "bytesPerPerson", bytesPerPerson));
        String[] lines = {"slave", "totalIterationTime", "personsPerSlave", "maxMemory", "usedMemory"};
        sb.append("\n");
        for (int j = 0; j < 5; j++) {

            sb.append("\t\t\t" + String.format("%20s\t", lines[j]));
            for (int i = 0; i < numSlaves; i++) {
                switch (j) {
                    case 0:
                        sb.append(String.format("%20s\t", "slave_" + i));
                        break;
                    case 1:
                        sb.append(String.format("%20.3f\t", totalIterationTime[i]));
                        break;
                    case 2:
                        sb.append(String.format("%20d\t", personsPerSlave[i]));
                        break;
                    case 3:
                        sb.append(String.format("%20d\t", maxMemory[i]));
                        break;
                    case 4:
                        sb.append(String.format("%20d\t", usedMemory[i]));
                        break;
                }

            }
            sb.append("\n");
        }
        masterLogger.warn(sb.toString());


        Map<Integer, Integer> optimalNumberPerSlave = new HashMap<>();
        double[] timesPerPlan = new double[numSlaves];
        int[] allocation = new int[numSlaves];
        boolean[] fullyAllocated = new boolean[numSlaves];
        long[] overheadMemory = new long[numSlaves];

        Set<Integer> validSlaveIndices = new HashSet<>();
        Set<Integer> newSlaves = new HashSet<>();
        double fastestTimePerPlan = Double.POSITIVE_INFINITY;

        for (int i = 0; i < numSlaves; i++) {
            if (personsPerSlave[i] > 0 && totalIterationTime[i]>0) {
                timesPerPlan[i] = (totalIterationTime[i] / personsPerSlave[i]);
                if (timesPerPlan[i] < fastestTimePerPlan)
                    fastestTimePerPlan = timesPerPlan[i];
            } else
                newSlaves.add(i);
            validSlaveIndices.add(i);
            maxMemory[i] = maxMemory[i] - bytesPerSlaveBuffer;
            overheadMemory[i] = usedMemory[i] - (personsPerSlave[i] * bytesPerPerson);
        }
        fastestTimePerPlan = fastestTimePerPlan>0 && !new Double(fastestTimePerPlan).equals(Double.POSITIVE_INFINITY)?fastestTimePerPlan:1;
        for(int i: newSlaves)
                timesPerPlan[i] = fastestTimePerPlan;
//        adjust numbers taking account of memory avail on slaves
        boolean allGood = false;
        int remainder = popSize;
        while (remainder > 0 && validSlaveIndices.size() > 0) {
            Set<Integer> valid = new HashSet<>();
            valid.addAll(validSlaveIndices);
            optimalNumberPerSlave.putAll(getOptimalNumbers(remainder, timesPerPlan, valid));
            for (int i : valid) {
                fullyAllocated[i] = false;
            }
            while (!isAllTrue(fullyAllocated)) {
                for (int i : valid) {
                    if (fullyAllocated[i]) {
                        continue;
                    }
                    long maxAvailMemory = (long) (maxMemory[i] - (planAllocationLimiter * (optimalNumberPerSlave.get(i) + allocation[i]) * bytesPerPlan));
                    long memoryAvailableForPersons = maxAvailMemory - overheadMemory[i];
                    int maxPersonAllocation = (int) (memoryAvailableForPersons / bytesPerPerson);

                    if (optimalNumberPerSlave.get(i) > maxPersonAllocation) {
                        optimalNumberPerSlave.put(i, optimalNumberPerSlave.get(i) - 1);
                        validSlaveIndices.remove(i);
                        continue;
                    }else {
                        //dampen the difference
                        if(optimalNumberPerSlave.get(i)>personsPerSlave[i] && optimalNumberPerSlave.get(i) > 10) {
                            int dampenedNumber = (int) (((1 - dampeningFactor) * (double) optimalNumberPerSlave.get(i)) + (dampeningFactor * (double) Math.min(personsPerSlave[i], maxPersonAllocation))) ;
                            optimalNumberPerSlave.put(i, dampenedNumber);
                        }
                    }
                    remainder -= optimalNumberPerSlave.get(i);
                    fullyAllocated[i] = true;
                }
            }
            for (int i : valid) {
                allocation[i] += optimalNumberPerSlave.get(i);
            }
            if (validSlaveIndices.size() == 0 && remainder > 0) {
                masterLogger.error("All slaves are nearing their maximum memory capacity!! Probably not a sustainable situation...");
                planAllocationLimiter--;
                //just distribute as if nothing has happened..
                for (int i = 0; i < numSlaves; i++) {
                    validSlaveIndices.add(i);
                }
            }
        }


        sb = new StringBuffer();
        lines = new String[]{"slave", "time per plan", "pax per slave", "allocation", "memUSED_MB", "memAVAIL_MB"};
        sb.append("\n");
        for (int j = 0; j < 6; j++) {

            sb.append("\t\t\t" + String.format("%20s\t", lines[j]));
            for (int i = 0; i < numSlaves; i++) {
                switch (j) {
                    case 0:
                        sb.append(String.format("%20s\t", "slave_" + i));
                        break;
                    case 1:
                        sb.append(String.format("%20.3f\t", timesPerPlan[i]));
                        break;
                    case 2:
                        sb.append(String.format("%20d\t", personsPerSlave[i]));
                        break;
                    case 3:
                        sb.append(String.format("%20d\t", allocation[i]));
                        break;
                    case 4:
                        sb.append(String.format("%20d\t", usedMemory[i]));
                        break;
                    case 5:
                        sb.append(String.format("%20d\t", maxMemory[i]));
                        break;
                }

            }
            sb.append("\n");
        }
        masterLogger.warn(sb.toString());
        return allocation;
    }

    private static boolean isAllTrue(boolean[] fullyAllocated) {
        for (boolean b : fullyAllocated) {
            if (!b) {
                return false;
            }
        }
        return true;

    }


    private class Slave implements Runnable {
        final Logger slaveLogger = Logger.getLogger(this.getClass());
        final Map<String, Plan> plans = new HashMap<>();
        ObjectInputStream reader;
        ObjectOutputStream writer;
        double totalIterationTime;
        List<PersonSerializable> slavePersonPool;
        int targetPopulationSize = 0;
        CommunicationsMode communicationsMode = CommunicationsMode.TRANSMIT_SCENARIO;
        Collection<String> idStrings;
        private int myNumber;
        private int currentPopulationSize;
        private long usedMemory;
        private long maxMemory;
        private int numberOfPlans;
        private int numThreadsOnSlave;
        private boolean isOkForNextIter =true;

        public Slave(Socket socket, int i) throws IOException {
            super();
            myNumber = i;
            this.writer = new ObjectOutputStream(socket.getOutputStream());
            this.reader = new ObjectInputStream(socket.getInputStream());
        }

        public void transmitPlans() throws IOException, ClassNotFoundException {
            plans.clear();
            slaveLogger.warn("Waiting to receive plans from slave number " + myNumber);
            Map<String, PlanSerializable> serialPlans = (Map<String, PlanSerializable>) reader.readObject();
            slaveLogger.warn("RECEIVED plans from slave number " + myNumber);
            for (Entry<String, PlanSerializable> entry : serialPlans.entrySet()) {
                plans.put(entry.getKey(), entry.getValue().getPlan(matsimControler.getScenario().getPopulation()));
            }
        }

        public void transmitPerformance() throws IOException {
            totalIterationTime = this.reader.readDouble();
            currentPopulationSize = this.reader.readInt();
            readMemoryStats();
        }

        public void transmitTravelTimes() throws IOException {
            slaveLogger.warn("About to send travel times to slave number " + myNumber);
            writer.writeObject(linkTravelTimes);
            if (config.scenario().isUseTransit()) {
                writer.writeObject(stopStopTimeCalculator.getStopStopTimes());
                writer.writeObject(waitTimeCalculator.getWaitTimes());
            }
            writer.flush();
            slaveLogger.warn("SENT travel times to slave number " + myNumber);
        }

        public void poolPersons() throws IOException, ClassNotFoundException {
            slaveLogger.warn("Trying to receive persons from slave " + myNumber);
            slaveLogger.warn("Currently has " + currentPopulationSize + " persons, target is " + targetPopulationSize);
            slavePersonPool = new ArrayList<>();
            writer.writeInt(currentPopulationSize - targetPopulationSize);
            writer.flush();
            slavePersonPool = (List<PersonSerializable>) reader.readObject();
        }

        public void distributePersons() throws IOException, InterruptedException {
            slaveLogger.warn("Distributing persons to slave" + myNumber);
            writer.writeObject(getPersonsFromPool(currentPopulationSize - targetPopulationSize));
            writer.flush();
        }

        public void transmitInitialPlans() throws IOException {
            writer.writeObject(slavePersonPool);
            writer.flush();

        }

        @Override
        public void run() {
            try {
                slaveLogger.warn("Slave " + myNumber+" entering comms mode: "+communicationsMode.toString());
                writer.writeObject(communicationsMode);
                writer.flush();
                switch (communicationsMode) {
                    case TRANSMIT_TRAVEL_TIMES:
                        transmitTravelTimes();
                        reader.readBoolean();
                        communicationsMode = CommunicationsMode.CONTINUE;
                        writer.writeObject(communicationsMode);
                        writer.flush();
                        break;
                    case POOL_PERSONS:
                        poolPersons();
                        break;
                    case DISTRIBUTE_PERSONS:
                        distributePersons();
                        break;
                    case TRANSMIT_PLANS_TO_MASTER:
                        writer.reset();
                        transmitPlans();
                        slaveIsOKForNextIter();
                        break;
                    case TRANSMIT_PERFORMANCE:
                        transmitPerformance();
                        break;
                    case DUMP_PLANS:
                        dumpPlans(matsimControler.getIterationNumber());
                        break;
                    case TRANSMIT_SCENARIO:
                        transmitInitialPlans();
                        reader.readBoolean();
                        communicationsMode = CommunicationsMode.CONTINUE;
                        writer.writeObject(communicationsMode);
                        writer.flush();
                        break;
                    case DIE:
                        return;
                }
                reader.readBoolean();
            } catch (IOException | InterruptedException | IndexOutOfBoundsException | ClassNotFoundException e) {
                e.printStackTrace();
                somethingWentWrong = true;
                numThreads.decrementAndGet();
            }
            //end of a successful Thread.run()
            numThreads.decrementAndGet();
            slaveLogger.warn("Slave " + myNumber + " leaving comms mode: " + communicationsMode.toString());
        }

        private void dumpPlans(int iteration) throws IOException, ClassNotFoundException {
            Population temp = PopulationUtils.createPopulation(config);
            OutputDirectoryHierarchy controlerIO = matsimControler.getControlerIO();
            slaveLogger.warn("Dumping population of "+currentPopulationSize+" agents on slave number " + myNumber);
            List<PersonSerializable> tempPax = (List<PersonSerializable>) reader.readObject();
            for(PersonSerializable p : tempPax){
                temp.addPerson(p.getPerson());
            }
            new PopulationWriter(temp, scenario.getNetwork()).write(controlerIO.getIterationFilename(iteration, "FULLplans_slave_"+myNumber+".xml.gz"));
            slaveLogger.warn("Done writing on slave number " + myNumber);
        }

        private void slaveIsOKForNextIter() throws IOException {
            this.isOkForNextIter = reader.readBoolean();
        }


        public void sendNumber(int i) throws IOException {
            writer.writeInt(i);
            writer.flush();
        }


        public Collection<? extends PersonSerializable> getPersons() {
            return slavePersonPool;
        }

        public void sendBoolean(boolean initialRouting) throws IOException {
            writer.writeBoolean(initialRouting);
            writer.flush();
        }

        public void readNumberOfThreadsOnSlave() throws IOException {
            this.numThreadsOnSlave = reader.readInt();
        }

        public void readMemoryStats() throws IOException {
            this.usedMemory = reader.readLong();
            this.maxMemory = reader.readLong();
            this.numberOfPlans = reader.readInt();
            ;
        }
    }

    private class Hydra implements Runnable {
        TreeMap<Integer, Slave> hydraSlaves = new TreeMap<>();
        AtomicBoolean accessingMap = new AtomicBoolean(false);

        public void run() {
            ServerSocket writeServer = null;
            try {
                writeServer = new ServerSocket(socketNumber);
                while (true) {
                    Socket socket = writeServer.accept();
                    int i = slaveUniqueNumber++;
                    masterLogger.warn("Slave accepted.");
                    Slave slave = new Slave(socket, i);
                    while (accessingMap.get()) {
//                    wait for the other process to finish modifying the map
                        Thread.sleep(10);
                    }
                    accessingMap.set(true);
                    hydraSlaves.put(i, slave);
                    //order is important
                    slave.sendNumber(i);
                    slave.sendNumber(numberOfPSimIterations);
                    slave.sendBoolean(false);
                    slave.readMemoryStats();
                    slave.readNumberOfThreadsOnSlave();
                    slave.slavePersonPool = new ArrayList<>();
                    accessingMap.set(false);
                    Thread.sleep(1000);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public TreeMap<Integer, Slave> getNewSlaves() {
            TreeMap<Integer, Slave> slaveBatch;
            while (accessingMap.get()) {
                try {
//                    wait for the other process to finish modifying the map
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            accessingMap.set(true);
            List<Integer> slavesToDrop = new ArrayList<>();
            for(Slave slave:hydraSlaves.values()){
                if(!slave.isOkForNextIter) slavesToDrop.add(slave.myNumber);
            }
            for(int i:slavesToDrop) hydraSlaves.remove(i);
            slaveBatch = hydraSlaves;
            hydraSlaves = new TreeMap<>();
            accessingMap.set(false);
            return slaveBatch;
        }
    }

}


class MemoryUsageCalculator {
    private static long fSLEEP_INTERVAL = 100;

    public long getMemoryUse() {
        putOutTheGarbage();
        long totalMemory = Runtime.getRuntime().totalMemory();
        putOutTheGarbage();
        long freeMemory = Runtime.getRuntime().freeMemory();
        return (totalMemory - freeMemory);
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
        }
    }
}


