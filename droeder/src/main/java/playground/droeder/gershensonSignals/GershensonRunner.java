/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.gershensonSignals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.signalsystems.control.AdaptiveSignalSystemControlerImpl;
import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.signalsystems.systems.SignalGroupDefinition;
import org.matsim.vis.otfvis.OTFVisMobsimFactoryImpl;

import playground.droeder.DaPaths;
import playground.droeder.Analysis.AverageTTHandler;
import playground.droeder.Analysis.SignalSystems.SignalGroupStateTimeHandler;
import playground.droeder.charts.DaBarChart;
import playground.droeder.charts.DaChartWriter;
import playground.droeder.charts.DaSignalPlanChart;

/**
 * @author droeder
 *
 */
public class GershensonRunner implements AgentStuckEventHandler {
	
	// "D" run denver -- "G" run gershensonTestNetwork --- "C" run cottbus
	private static final String config = "D";
	
	private boolean liveVis;

	//writeSignalPlans
	private boolean writeSignalPlans;
	private double signalPlanMin;
	private double signalPlanMax;
	
	private double startTime;
	
	private int u;
	private int n;
	private double cap;
	private double d;
	private int maxRed;
	
	
//	private Map<Id, Id> corrGroups;
//	private Map<Id, List<Id>> compGroups;
	private Map<Id, Id> mainOutLinks;
	
	private CalculateSignalGroups csg;
	
	private Map<Id, Map<Id, SignalGroupDefinition>> signalSystems = new HashMap<Id, Map<Id,SignalGroupDefinition>>();
	private Map<Id, Map<Id, List<SignalGroupDefinition>>> newCorrGroups = new HashMap<Id, Map<Id,List<SignalGroupDefinition>>>();
	private Map<Id, Map<Id, Id>> newMainOutlinks = new HashMap<Id, Map<Id,Id>>();
	
	private Map<Integer, Double> averageTT;
	private static double avTT = 0;
	
//	private static Map<Integer, Map<Integer, Double>> nAndUT = new LinkedHashMap<Integer, Map<Integer,Double>>();
//	private static LinkedHashMap<Number, Number> nAndT = new LinkedHashMap<Number, Number>();
	
	
	private AverageTTHandler handler1;
	private CarsOnLinkLaneHandler handler2;
	private SignalGroupStateTimeHandler handler3;
	
	
	private static final Logger log = Logger.getLogger(GershensonRunner.class);
	
	public GershensonRunner(int minRed, int n, double cap, double d, int maxRed, boolean writeSignalplans){
		this.u = minRed;
		this.n = n;
		this.cap = cap;
		this.d = d;
		this.maxRed = maxRed;
		this.writeSignalPlans= writeSignalplans;
	}

	
	public void runScenario (final String configFile){
		String conf = null;
		
		if (configFile == "G"){
			log.info("start gershensonTest");
			GershensonScenarioGenerator gsg = new GershensonScenarioGenerator();
			gsg.createScenario();
			conf = DaPaths.DASTUDIES + "gershenson\\gershensonConfigFile2.xml";		
		}else if (configFile == "D"){
			log.info("start Denver");
			DenverScenarioGenerator dsg = new DenverScenarioGenerator();
			dsg.createScenario();
			conf = DaPaths.DASTUDIES + "denver\\denverConfig.xml";
		}else if (configFile == "opti"){
			conf = DaPaths.DASTUDIES + "denver\\denverConfig.xml";
		}
		else if (configFile == "C"){
			conf = DaPaths.DASTUDIES + "cottbus\\cottbusConfig.xml";
		}
		else{
			conf = configFile;
		}
		
		Controler controler = new Controler(conf);	
		controler.setOverwriteFiles(true);
		Config config = controler.getConfig();
		
		this.addListener(controler);
		this.addQueueSimListener(controler);
		controler.run();

		
//		this.startVisualizer(config);		
		
		
	}
	
	private void addListener(final Controler c) {
		
		
		c.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				Map<Id, SignalGroupDefinition> groups = c.getScenario().getSignalSystems().getSignalGroupDefinitions();
				
				for (Entry<Id, SignalGroupDefinition> e : groups.entrySet()){
					if(signalSystems.containsKey(e.getValue().getSignalSystemDefinitionId())){
						signalSystems.get(e.getValue().getSignalSystemDefinitionId()).put(e.getValue().getId(), e.getValue());
					}else {
						signalSystems.put(e.getValue().getSignalSystemDefinitionId(), new HashMap<Id, SignalGroupDefinition>());
						signalSystems.get(e.getValue().getSignalSystemDefinitionId()).put(e.getValue().getId(), e.getValue());
					}
				}
				
				for (Entry<Id, Map<Id, SignalGroupDefinition>> e : signalSystems.entrySet()){
					csg = new CalculateSignalGroups(e.getValue(), c.getNetwork());
					newCorrGroups.put(e.getKey(), csg.calcCorrGroups());
					newMainOutlinks.put(e.getKey(), csg.calculateMainOutlinks());
				}
				
				
				csg = new CalculateSignalGroups(groups, c.getNetwork());
//				corrGroups = csg.calculateCorrespondingGroups();
//				compGroups = csg.calculateCompetingGroups(corrGroups);
				mainOutLinks = csg.calculateMainOutlinks();
				
				handler1 = new AverageTTHandler(c.getPopulation().getPersons().size());
				handler2 = new CarsOnLinkLaneHandler(groups, d, c.getNetwork());
				handler3 = new SignalGroupStateTimeHandler();
				
				event.getControler().getEvents().addHandler(handler1);
				event.getControler().getEvents().addHandler(handler2);
				event.getControler().getEvents().addHandler(handler3);
				
				//enable live-visualization
				event.getControler().setMobsimFactory(new OTFVisMobsimFactoryImpl());
				
				//output of stucked vehicles
				event.getControler().getEvents().addHandler(GershensonRunner.this);	
			}
			
		}
		);
		
		c.addControlerListener(new IterationStartsListener() {
			@Override
			public void notifyIterationStarts(IterationStartsEvent event) {
				handler1.reset(event.getIteration());
				handler2.reset(event.getIteration());
				handler3.reset(event.getIteration());
			}
		});
		
		c.addControlerListener(new IterationEndsListener() {
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				avTT = handler1.getAverageTravelTime();
				DaSignalPlanChart planChart;
//				handler3.writeToTxt(DaPaths.OUTPUT+ "denver\\ITERS\\it." + event.getIteration() + "\\greenTimes.txt");
				if(writeSignalPlans == true){
					for(Entry<Id, TreeMap<Id, TreeMap<Double, SignalGroupState>>> e : handler3.getSystemGroupTimeStateMap().entrySet()){
						planChart = new DaSignalPlanChart();
						planChart.addData(e.getValue(), startTime, signalPlanMax);
						new DaChartWriter().writeChart(DaPaths.OUTPUT + "/signalPlans/signalPlanId" + e.getKey() + ".png", 2560, 1600, planChart.createSignalPlanChart("signalPlan", "ids", "time", signalPlanMin, signalPlanMax));
					}
					
				}
			}
		});		
		
		c.addControlerListener(new ShutdownListener() {
			@Override
			public void notifyShutdown(ShutdownEvent event) {
				
			}
		});
		
	}
	
	// adaptiveController
	private void addQueueSimListener(final Controler c) {
		c.getQueueSimulationListener().add(new SimulationInitializedListener<QSim>() {
			//add the adaptive controller as events listener
			public void notifySimulationInitialized(SimulationInitializedEvent<QSim> e) {
//				AdaptiveSignalSystemControlerImpl adaptiveController;
				QSim qs = e.getQueueSimulation();
				
				for(Entry<Id, Map<Id, SignalGroupDefinition>> ee: signalSystems.entrySet()){
					DaAdaptivController adaptiveController = (DaAdaptivController) qs.getQSimSignalEngine().getSignalSystemControlerBySystemId().get(ee.getKey());
					adaptiveController.setParameters(n, u, cap, maxRed);
					adaptiveController.init(newCorrGroups.get(ee.getKey()), newMainOutlinks.get(ee.getKey()), e.getQueueSimulation().getQNetwork(), handler2);
					c.getEvents().addHandler(adaptiveController);
				}
				
//				GershensonAdaptiveTrafficLightController adaptiveController = (GershensonAdaptiveTrafficLightController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(new IdImpl("1"));
//				adaptiveController.setParameters(n, u, cap, maxGreen);
//				adaptiveController.init(corrGroups, compGroups, mainOutLinks, e.getQueueSimulation().getQNetwork(), handler2);
//				c.getEvents().addHandler(adaptiveController);				
				
				handler2.setQNetwork(e.getQueueSimulation().getQNetwork());

				qs.getEventsManager().addHandler(handler3);
				
			}
		});
		//remove the adaptive controller
		c.getQueueSimulationListener().add(new SimulationBeforeCleanupListener<QSim>() {
			public void notifySimulationBeforeCleanup(SimulationBeforeCleanupEvent<QSim> e) {
				QSim qs = e.getQueueSimulation();
				for(Entry<Id, Map<Id, SignalGroupDefinition>> ee: signalSystems.entrySet()){
					DaAdaptivController adaptiveController = (DaAdaptivController) qs.getQSimSignalEngine().getSignalSystemControlerBySystemId().get(ee.getKey());
					if(writeSignalPlans == true){
						writeDemandOnRefLinkChart(adaptiveController, ee.getKey());
					}
						c.getEvents().removeHandler(adaptiveController);	
				}
//				GershensonAdaptiveTrafficLightController adaptiveController = (GershensonAdaptiveTrafficLightController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(new IdImpl("1"));
//				c.getEvents().removeHandler(adaptiveController);

			}
		});

//	//randomController
//	private void addQueueSimListener(final Controler c) {
//		c.getQueueSimulationListener().add(new SimulationInitializedListener<QSim>() {
//			//add the adaptive controller as events listener
//			public void notifySimulationInitialized(SimulationInitializedEvent<QSim> e) {
////				AdaptiveSignalSystemControlerImpl adaptiveController;
//				QSim qs = e.getQueueSimulation();
//				
//				for(Entry<Id, Map<Id, SignalGroupDefinition>> ee: signalSystems.entrySet()){
//					DaRandomController adaptiveController = (DaRandomController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(ee.getKey());
//					adaptiveController.init(newCorrGroups.get(ee.getKey()), newMainOutlinks.get(ee.getKey()), e.getQueueSimulation().getQNetwork(), handler2);
//					c.getEvents().addHandler(adaptiveController);
//				}
//				handler2.setQNetwork(e.getQueueSimulation().getQNetwork());
//
//				qs.getEventsManager().addHandler(handler3);
//				
//			}
//		});
//		//remove the adaptive controller
//		c.getQueueSimulationListener().add(new SimulationBeforeCleanupListener<QSim>() {
//			public void notifySimulationBeforeCleanup(SimulationBeforeCleanupEvent<QSim> e) {
//				QSim qs = e.getQueueSimulation();
//				for(Entry<Id, Map<Id, SignalGroupDefinition>> ee: signalSystems.entrySet()){
//					DaRandomController adaptiveController = (DaRandomController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(ee.getKey());
////					if(writeSignalPlans == true){
////						writeDemandOnRefLinkChart(adaptiveController, ee.getKey());
////					}
//					c.getEvents().removeHandler(adaptiveController);	
//				}
//
//			}
//		});
		
//		c.getQueueSimulationListener().add(new SimulationAfterSimStepListener<QSim>() {
//			public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent<QSim> e) {
//				QSim qs = e.getQueueSimulation();
//			}
//		});
	
	}
	
	public void writeDemandOnRefLinkChart(DaAdaptivController contr, Id signalSystem){
		DaBarChart chart = new DaBarChart();
		for (Entry<Id, SortedMap<Double, Double>> ee : contr.getDemandOnRefLink().entrySet()){
			chart.addSeries(ee.getKey().toString(), (Map)ee.getValue().subMap(signalPlanMin, signalPlanMax));
		}
		new DaChartWriter().writeChart(DaPaths.OUTPUT + "/signalPlans/demandOnLinkSystem" + signalSystem.toString() + ".png", 2560, 1600,
				chart.createChart("demandOnRefLink for SignalSystem " + signalSystem.toString(), "time [s]", "demand [cars]", 30));
		
	}
	
	
	public void setU (int u){
		this.u = u;
	}
	public void setN (int n){
		this.n = n;
	}
	public void setCap (double cap){
		this.cap = cap;
	}
	public void setD (double d){
		this.d = d;
	}
	public void setMaxRed(int maxRed){
		this.maxRed = maxRed;
	}
	public double getAvTT(){
		return this.avTT;
	}
	
	public void setSignalPlanBounds(double startTime, double min, double max){
		this.signalPlanMax = max;
		this.signalPlanMin = min;
		this.startTime = startTime;
	}
	
	
	public static void main(String[] args) {
		DaBarChart barChart = new DaBarChart();
		GershensonRunner runner;
		Map<Number, Number> xAndValue;
		double temp = 0;
		double category = 0;
		double xx = 0;
		double value = 0;
		
		double cap ;
		double d;
		int minRed;
		int carTime;
		int maxRed;
		
		//Denver opti
		runner = new GershensonRunner(14, 468, 0.65, 55, 38, false);
		runner.setSignalPlanBounds(21600, 22000, 22240);
		runner.runScenario("D");
//		
// 		//cottbus opti		
//		runner = new GershensonRunner(31, 215, 0.75, 55, 107, false);
//		runner.setSignalPlanBounds(21600, 22000, 22240);
//		runner.runScenario("C");
		
//		cap = 0.81;
//		d = 80;
//		minRed = 10;
//		carTime = 10;
//		maxRed = 35;
//
//		
//		for(int i = (int) minRed; i<minRed+10; i++){
//			xAndValue = new TreeMap<Number,Number>();
//			category = i;
//			for(int ii = carTime; ii<carTime + 91; ii = ii+10){
//				xx = ii;
//				runner = new GershensonRunner((int) category, (int) xx, cap, d, maxRed, false);
//				Gbl.reset();
//				runner.runScenario("opti");
//				value = runner.getAvTT();
//				xAndValue.put(xx, value);
//				if(value>temp) temp = value;
//			}
//			barChart.addSeries("u=" + String.valueOf(category), xAndValue);
//		}
//		new DaChartWriter().writeChart(DaPaths.OUTPUT + "denver\\charts\\" + "d80_cap0.81_maxRed35_n10-100_u10-20", 2560, 1600, 
//				barChart.createChart("d = 80m, cap = 0.81, maxRed = 35s", "waitingCars * redTime [1*s]", "average travelTime t [s]", 100, temp+10));
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		log.error("got stuck event for agent: " + event.getPersonId() + " on Link " + event.getLinkId());
	}

	@Override
	public void reset(int iteration) {
	}
	

}
//		for (int d = 80; d<151; d = d+10){
//			for (int c = 0; c < 16; c++){
//				temp = 0;
//				barChart = new DaBarChart();
//				cap = (80.00+(double)c)/100.00;
//				for (int u = 5; u < 30; u = u + 2){
//					nAndT = new LinkedHashMap<Number, Number>();
//					for (int n = 10; n < 401; n = n+10){
//						runner = new GershensonRunner();
//						Gbl.reset();
//						runner.setU(u);
//						runner.setN(n);
//						runner.setCap(cap);
//						runner.setD(d);
//						runner.setMaxGreen(0);
//						runner.runScenario(config);
//						nAndT.put(n, avTT);
//						if(avTT>temp){
//							temp = avTT;
//						}
//					}
//					barChart.addSeries("u=" + String.valueOf(u), nAndT);
//		//			nAndUT.put(n, uAndT);
//				}	
//				new DaChartWriter().writeChart(DaPaths.OUTPUT+"DENVER\\Charts_10_03_17.2\\" + "n10-400_u5-30_cap" + 
//						String.valueOf(cap) + "_d" + String.valueOf(d), 2560, 1600, barChart.createChart("capacityFactor = " + String.valueOf(cap) + " d = " + String.valueOf(d), "waitingCars * redTime [1*s]", "average travelTime t [s]", temp));
//			}
//		}
