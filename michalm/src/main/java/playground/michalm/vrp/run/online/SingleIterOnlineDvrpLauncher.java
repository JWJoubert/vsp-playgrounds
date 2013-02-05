/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.vrp.run.online;

import java.io.*;
import java.util.*;

import org.jfree.chart.JFreeChart;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.*;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.*;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.mobsim.qsim.agents.*;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vis.otfvis.OnTheFlyServer;
import org.matsim.vis.otfvis.gui.OTFQueryControl;

import pl.poznan.put.util.jfreechart.*;
import pl.poznan.put.util.jfreechart.ChartUtils.OutputType;
import pl.poznan.put.util.lang.TimeDiscretizer;
import pl.poznan.put.vrp.dynamic.chart.*;
import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.model.Request.ReqStatus;
import pl.poznan.put.vrp.dynamic.optimizer.listener.ChartFileOptimizerListener;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.*;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiEvaluator.TaxiEvaluation;
import playground.michalm.demand.ODDemandGenerator;
import playground.michalm.util.gis.Schedules2GIS;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.file.DepotReader;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.router.*;
import playground.michalm.vrp.otfvis.VrpOTFClientLive;
import playground.michalm.vrp.taxi.*;
import playground.michalm.vrp.taxi.taxicab.TaxiAgentSource;
import playground.michalm.vrp.taxi.wal.WalTaxiSimEngine;


public class SingleIterOnlineDvrpLauncher
{
    String dirName;
    String netFileName;
    String plansFileName;
    String taxiCustomersFileName;
    String depotsFileName;
    String reqIdToVehIdFileName;
    String dbFileName;
    boolean vrpOutFiles;
    String vrpOutDirName;

    AlgorithmConfig algorithmConfig;
    String eventsFileName;

    Scenario scenario;
    MatsimVrpData data;

    TaxiOptimizerFactory optimizerFactory;

    boolean otfVis;
    public static OTFQueryControl queryControl;

    boolean wal;


    void defaultArgs()
    {
        dirName = "D:\\PP-rad\\taxi\\mielec-morning-variable\\";
        netFileName = dirName + "network.xml";

        plansFileName = dirName + "output\\ITERS\\it.20\\20.plans.xml.gz";

        taxiCustomersFileName = dirName + "taxiCustomers_05_pc.txt";
        // taxiCustomersFileName = dirName + "taxiCustomers_10_pc.txt";

        depotsFileName = dirName + "depots-5_taxis-50.xml";
        // depotsFileName = dirName + "depots-5_taxis-150.xml";

        // reqIdToVehIdFileName = dirName + "reqIdToVehId";

        eventsFileName = dirName + "output\\ITERS\\it.20\\20.events.xml.gz";

        // algorithmConfig = AlgorithmConfig.NOS_STRAIGHT_LINE;
        // algorithmConfig = AlgorithmConfig.NOS_TRAVEL_DISTANCE;
        algorithmConfig = AlgorithmConfig.NOS_FREE_FLOW;
        // algorithmConfig = AlgorithmConfig.NOS_24_H;
        // algorithmConfig = AlgorithmConfig.NOS_15_MIN;
        // algorithmConfig = AlgorithmConfig.OTS_REQ_FREE_FLOW;
        // algorithmConfig = AlgorithmConfig.OTS_REQ_24_H;
        // algorithmConfig = AlgorithmConfig.OTS_REQ_15_MIN;
        // algorithmConfig = AlgorithmConfig.OTS_DRV_FREE_FLOW;
        // algorithmConfig = AlgorithmConfig.OTS_DRV_24_H;
        // algorithmConfig = AlgorithmConfig.OTS_DRV_15_MIN;
        // algorithmConfig = AlgorithmConfig.RES_REQ_FREE_FLOW;
        // algorithmConfig = AlgorithmConfig.RES_REQ_24_H;
        // algorithmConfig = AlgorithmConfig.RES_REQ_15_MIN;
        // algorithmConfig = AlgorithmConfig.RES_DRV_FREE_FLOW;
        // algorithmConfig = AlgorithmConfig.RES_DRV_24_H;
        // algorithmConfig = AlgorithmConfig.RES_DRV_15_MIN;

        otfVis = !true;

        vrpOutFiles = !true;
        vrpOutDirName = dirName + "vrp_output";

        wal = false;
    }


    void readArgs(String paramFile)
        throws FileNotFoundException
    {
        Scanner scanner = new Scanner(new BufferedReader(new FileReader(paramFile)));
        Map<String, String> params = new HashMap<String, String>();

        while (scanner.hasNext()) {
            String key = scanner.next();
            String value = scanner.next();
            params.put(key, value);
        }

        dirName = params.get("dirName") + '\\';
        netFileName = dirName + params.get("netFileName");

        plansFileName = dirName + params.get("plansFileName");

        taxiCustomersFileName = dirName + params.get("taxiCustomersFileName");

        depotsFileName = dirName + params.get("depotsFileName");

        eventsFileName = dirName + params.get("eventsFileName");

        algorithmConfig = AlgorithmConfig.ALL[Integer.valueOf(params.get("algorithmConfig"))];

        otfVis = Boolean.valueOf(params.get("otfVis"));

        vrpOutFiles = Boolean.valueOf(params.get("vrpOutFiles"));

        vrpOutDirName = dirName + params.get("vrpOutDirName");

        wal = false;
    }


    void prepareMatsimData()
        throws IOException
    {
        Config config = ConfigUtils.createConfig();
        scenario = ScenarioUtils.createScenario(config);

        new MatsimNetworkReader(scenario).readFile(netFileName);
        new MatsimPopulationReader(scenario).readFile(plansFileName);

        List<String> taxiCustomerIds = ODDemandGenerator.readTaxiCustomerIds(taxiCustomersFileName);

        for (String id : taxiCustomerIds) {
            Person person = scenario.getPopulation().getPersons().get(scenario.createId(id));
            Leg leg = (Leg)person.getSelectedPlan().getPlanElements().get(1);
            leg.setMode(TaxiModeDepartureHandler.TAXI_MODE);
        }
    }


    void initMatsimVrpData()
        throws IOException
    {
        int travelTimeBinSize = algorithmConfig.ttimeSource.travelTimeBinSize;
        int numSlots = algorithmConfig.ttimeSource.numSlots;

        scenario.getConfig().travelTimeCalculator().setTraveltimeBinSize(travelTimeBinSize);

        TravelTime ttimeCalc;
        TravelDisutility tcostCalc;

        switch (algorithmConfig.ttimeSource) {
            case FREE_FLOW_SPEED:
                ttimeCalc = new FreeSpeedTravelTime();
                break;

            case EVENTS_15_MIN:
            case EVENTS_24_H:
                ttimeCalc = TravelTimeCalculators.createTravelTimeFromEvents(eventsFileName, scenario);
                break;

            default:
                throw new IllegalArgumentException();
        }

        switch (algorithmConfig.tcostSource) {
            case DISTANCE:
                tcostCalc = new DistanceAsTravelDisutility();
                break;

            case TIME:
                tcostCalc = new TimeAsTravelDisutility(ttimeCalc);
                break;

            default:
                throw new IllegalArgumentException();
        }

        TimeDiscretizer timeDiscretizer = new TimeDiscretizer(travelTimeBinSize, numSlots);
        MatsimVrpGraph graph = MatsimVrpGraphCreator.create(scenario, ttimeCalc, tcostCalc,
                timeDiscretizer, false);

        VrpData vrpData = new VrpData();
        vrpData.setVrpGraph(graph);
        vrpData.setCustomers(new ArrayList<Customer>());
        vrpData.setRequests(new ArrayList<Request>());
        new DepotReader(scenario, vrpData).readFile(depotsFileName);

        data = new MatsimVrpData(vrpData, scenario);
    }


    void initOptimizerFactory()
        throws IOException
    {
        switch (algorithmConfig.algorithmType) {
            case NO_SCHEDULING:
                optimizerFactory = IdleTaxiDispatcher.createFactory(
                        algorithmConfig == AlgorithmConfig.NOS_STRAIGHT_LINE,
                        algorithmConfig.optimizationPolicy);
                break;

            case ONE_TIME_SCHEDULING:
                optimizerFactory = TaxiOptimizerWithoutReassignment
                        .createFactory(algorithmConfig.optimizationPolicy);
                break;

            case RE_SCHEDULING:
                optimizerFactory = TaxiOptimizerWithReassignment
                        .createFactory(algorithmConfig.optimizationPolicy);
                break;

            case PRE_ASSIGNMENT:
                File reqIdToVehIdFile = new File(reqIdToVehIdFileName);
                Scanner scanner = new Scanner(reqIdToVehIdFile);

                List<Vehicle> vehicles = data.getVrpData().getVehicles();
                Vehicle[] reqIdToVehMapping = new Vehicle[scanner.nextInt()];

                for (int i = 0; i < reqIdToVehMapping.length; i++) {
                    reqIdToVehMapping[i] = vehicles.get(scanner.nextInt());
                }

                optimizerFactory = TaxiOptimizerWithPreassignment.createFactory(reqIdToVehMapping,
                        algorithmConfig.optimizationPolicy);
                break;

            default:
                throw new IllegalStateException();
        }
    }


    void runSim()
    {
        if (scenario.getConfig().getQSimConfigGroup() == null) {
            QSimConfigGroup qSimConfig = new QSimConfigGroup();
            qSimConfig.setSnapshotStyle(QSimConfigGroup.SNAPSHOT_AS_QUEUE);
            qSimConfig.setRemoveStuckVehicles(false);
            scenario.getConfig().addQSimConfigGroup(qSimConfig);
        }

        EventsManager events = EventsUtils.createEventsManager();
        EventWriter writer = new EventWriterXML(dirName + "events.xml.gz");
        events.addHandler(writer);
        
        QSim qSim = new QSim(scenario, events);
        ActivityEngine activityEngine = new ActivityEngine();
        qSim.addMobsimEngine(activityEngine);
        qSim.addActivityHandler(activityEngine);
        QNetsimEngine netsimEngine = new DefaultQSimEngineFactory().createQSimEngine(qSim);
        qSim.addMobsimEngine(netsimEngine);
        qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
        TeleportationEngine teleportationEngine = new TeleportationEngine();
        qSim.addMobsimEngine(teleportationEngine);

        TaxiSimEngine taxiSimEngine = wal ? new WalTaxiSimEngine(qSim, data, optimizerFactory,
                dbFileName) : new TaxiSimEngine(qSim, data, optimizerFactory);
        qSim.addMobsimEngine(taxiSimEngine);
        qSim.addAgentSource(new PopulationAgentSource(scenario.getPopulation(),
                new DefaultAgentFactory(qSim), qSim));
        qSim.addAgentSource(new TaxiAgentSource(data, taxiSimEngine));
        qSim.addDepartureHandler(new TaxiModeDepartureHandler(taxiSimEngine, data));

        if (vrpOutFiles) {
            taxiSimEngine.addListener(new ChartFileOptimizerListener(new ChartCreator() {
                @Override
                public JFreeChart createChart(VrpData data)
                {
                    return RouteChartUtils.chartRoutesByStatus(data);
                }
            }, OutputType.PNG, vrpOutDirName + "\\routes_", 800, 800));

            taxiSimEngine.addListener(new ChartFileOptimizerListener(new ChartCreator() {
                @Override
                public JFreeChart createChart(VrpData data)
                {
                    return ScheduleChartUtils.chartSchedule(data);
                }
            }, OutputType.PNG, vrpOutDirName + "\\schedules_", 1200, 800));
        }

        if (otfVis) { // OFTVis visualization
            OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(),
                    scenario, qSim.getEventsManager(), qSim);
            VrpOTFClientLive.run(scenario.getConfig(), server);
        }

        // events.addHandler(runningVehicleRegister = new RunningVehicleRegister());

        qSim.run();
        
        events.finishProcessing();
        writer.closeFile();
    }


    // RunningVehicleRegister runningVehicleRegister;

    void generateVrpOutput()
    {
        // DVRP-based evaluation (using schedules)
        TaxiEvaluation taxiEval = (TaxiEvaluation)new TaxiEvaluator()
                .evaluateVrp(data.getVrpData());
        System.out.println(TaxiEvaluation.HEADER);
        System.out.println(taxiEval.toString());

        // MATSim-based evaluation (using events)

        if (vrpOutFiles) {
            new File(vrpOutDirName).mkdir();
            new Schedules2GIS(data.getVrpData().getVehicles(), data, vrpOutDirName + "\\route_")
                    .write();
        }

        // ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(data.getVrpData()));
        ChartUtils.showFrame(ScheduleChartUtils.chartSchedule(data.getVrpData()));
    }


    void go()
        throws IOException
    {
        initMatsimVrpData();
        initOptimizerFactory();
        runSim();

        // check
        for (Request r : data.getVrpData().getRequests()) {
            if (r.getStatus() != ReqStatus.PERFORMED) {
                throw new IllegalStateException();
            }
        }
    }


    public static void main(String... args)
        throws IOException
    {
        String paramFile;
        if (args.length == 0) {
            paramFile = null;
        }
        else if (args.length == 1) {
            paramFile = args[0];
        }
        else {
            throw new RuntimeException();
        }

        SingleIterOnlineDvrpLauncher launcher = new SingleIterOnlineDvrpLauncher();

        if (paramFile == null) {
            launcher.defaultArgs();
        }
        else {
            launcher.readArgs(paramFile);
        }

        launcher.prepareMatsimData();
        launcher.go();
        launcher.generateVrpOutput();
    }
}
