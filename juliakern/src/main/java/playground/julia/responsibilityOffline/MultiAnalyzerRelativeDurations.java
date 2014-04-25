/* *********************************************************************** *
 * project: org.matsim.*
 * MulitAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.julia.responsibilityOffline;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroupUtils;
import playground.benjamin.scenarios.munich.analysis.kuhmo.CarDistanceEventHandler;
import playground.benjamin.scenarios.munich.analysis.kuhmo.MultiAnalyzerWriter;
import playground.benjamin.scenarios.munich.analysis.kuhmo.TravelTimePerModeEventHandler;
import playground.benjamin.scenarios.zurich.analysis.MoneyEventHandler;
import playground.julia.distribution.GridTools;
import playground.julia.newInternalization.IntervalHandler;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

/**
 * @author benjamin, julia
 *
 */
public class MultiAnalyzerRelativeDurations {
	private static final Logger logger = Logger.getLogger(MultiAnalyzerRelativeDurations.class);
	
	private static String [] cases = {

	// responsibility
	"baseCase_ctd" ,
	"policyCase_zone30" ,
	"policyCase_pricing",
	"policyCase_exposurePricing"
	};
	
	//latsis
		private static String runDirectoryStub = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_";
	//	private static String initialIterationNo = "1000";
		private static String finalIterationNo = "1500";
	
	private static String netFile;
	private static String configFile;
	private static String plansFile;
	private static String eventsFile;
	private static String emissionEventsFile;

	private final MultiAnalyzerWriter writer;
	private final Map<String, Map<Id, Double>> case2personId2carDistance;

	private final UserGroupUtils userGroupUtils;

	private Double simulationEndTime= 30*60*60.;

	private int noOfXbins = 160;

	private int noOfYbins = 120;

	private Double timeBinSize = 60*60.;

	final double xMin = 4452550.25;
	final double xMax = 4479483.33;
	final double yMin = 5324955.00;
	final double yMax = 5345696.81;
	


	MultiAnalyzerRelativeDurations(){
		this.writer = new MultiAnalyzerWriter(runDirectoryStub + cases[0] + "/");
		this.case2personId2carDistance = new HashMap<String, Map<Id,Double>>();
		this.userGroupUtils = new UserGroupUtils();
	}

	private void run() {
		
		for(String caseName : cases){
			
			String runDirectory = runDirectoryStub + caseName + "/";
			
			//latsis
			netFile = runDirectory + "output_network.xml.gz";
			configFile = runDirectory + "output_config.xml.gz";
			plansFile = runDirectory + "ITERS/it." + finalIterationNo + "/" + finalIterationNo + ".plans.xml.gz";
			eventsFile = runDirectory + "ITERS/it." + finalIterationNo + "/" + finalIterationNo + ".events.xml.gz";
			emissionEventsFile = runDirectory + "ITERS/it." + finalIterationNo + "/" + finalIterationNo + ".emission.events.xml.gz";
			
			calculateUserWelfareAndTollRevenueStatisticsByUserGroup(netFile, configFile, plansFile, eventsFile, caseName);
			calculateDistanceTimeStatisticsByUserGroup(netFile, eventsFile, caseName);
			calculateEmissionStatisticsByUserGroup(eventsFile, emissionEventsFile, caseName);
			calculateExposureEmissionCostsByUserGroup(eventsFile, emissionEventsFile, caseName);
		}
		calculateDistanceTimeStatisticsByUserGroupDifferences(case2personId2carDistance);
	}

	private void calculateDistanceTimeStatisticsByUserGroupDifferences(Map<String, Map<Id, Double>> case2personId2carDistance) {
		
		Map<Id, Double> personId2carDistanceBaseCase = case2personId2carDistance.get(cases[0]);
		
		for(int i=1; i<cases.length; i++){
			Map<Id, Double> personId2carDistanceDiff = new HashMap<Id, Double>();
			Map<Id, Double> personId2carDistancePolicyCase = case2personId2carDistance.get(cases[i]);
			
			for(Id personId : personId2carDistanceBaseCase.keySet()){
				Double baseCaseDist = personId2carDistanceBaseCase.get(personId);
				Double policyCaseDist;
				
				if(personId2carDistancePolicyCase.get(personId) == null){
					policyCaseDist = 0.0;
				} else {
					policyCaseDist = personId2carDistancePolicyCase.get(personId);
				}
				Double distDiff = policyCaseDist - baseCaseDist;
				personId2carDistanceDiff.put(personId, distDiff);
			}
			for(Id personId : personId2carDistancePolicyCase.keySet()){
				if(personId2carDistanceBaseCase.get(personId) == null){
					Double policyCaseDist = personId2carDistancePolicyCase.get(personId);
					personId2carDistanceDiff.put(personId, policyCaseDist);
				}
			}
			writer.setRunName(cases[i] + "-" + cases[0]);
			writer.writeDetailedCarDistanceInformation(personId2carDistanceDiff);
		}
	}

	private void calculateUserWelfareAndTollRevenueStatisticsByUserGroup(String netFile, String configFile, String plansFile, String eventsFile, String runName) {

		Scenario scenario = loadScenario(netFile, plansFile);
		Population pop = scenario.getPopulation();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsReaderXMLv1 eventsReader = new EventsReaderXMLv1(eventsManager);
		MoneyEventHandler moneyEventHandler = new MoneyEventHandler();
		eventsManager.addHandler(moneyEventHandler);
		eventsReader.parse(eventsFile);

		Map<Id, Double> personId2Toll = moneyEventHandler.getPersonId2TollMap();
		
		// TODO: this could be probably done outside of the writer as follows:
//		Map<UserGroup, Double> userGroup2Size = userGroupUtils.getSizePerGroup(pop);
//		Map<UserGroup, Double> userGroup2TollPayers = userGroupUtils.getNrOfTollPayersPerGroup(personId2Toll);
//		Map<UserGroup, Double> userGroup2Welfare = userGroupUtils.getUserLogsumPerGroup(scenario);
//		Map<UserGroup, Double> userGroup2Toll = userGroupUtils.getTollPaidPerGroup(personId2Toll);

		writer.setRunName(runName);
		writer.writeWelfareTollInformation(configFile, pop, personId2Toll);
	}

	private void calculateExposureEmissionCostsByUserGroup(String eventsFile, String emissionEventsFile, String runName) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();		
		MatsimPopulationReader mpr = new MatsimPopulationReader(scenario);
		mpr.readFile(plansFile);
		
		config = scenario.getConfig();
//		Population pop = scenario.getPopulation();
//		Controler controler=  new Controler(config);
		
		// map links to cells
		GridTools gt = new GridTools(network.getLinks(), xMin, xMax, yMin, yMax);
		Map<Id, Integer> link2xbins = gt.mapLinks2Xcells(noOfXbins);
		Map<Id, Integer> link2ybins = gt.mapLinks2Ycells(noOfYbins);
		
		// calc durations
		IntervalHandler intervalHandler = new IntervalHandler(timeBinSize, simulationEndTime, noOfXbins, noOfYbins, link2xbins, link2ybins); 
				//new IntervalHandler(timeBinSize, simulationEndTime, noOfXbins, noOfYbins, link2xbins, link2ybins, gt, network);
		intervalHandler.reset(0);
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(intervalHandler);
		MatsimEventsReader mer = new MatsimEventsReader(eventsManager);
		mer.readFile(eventsFile);
		HashMap<Double, Double[][]> durations = intervalHandler.getDuration();
		
		EmissionsAnalyzerRelativeDurations ema = new EmissionsAnalyzerRelativeDurations(emissionEventsFile, durations, link2xbins, link2ybins);
		ema.init(null);
		ema.preProcessData();
		ema.postProcessData();
		
		Map<Id, SortedMap<String, Double>> person2totalEmissionCosts = ema.getPerson2totalEmissionCosts();
		SortedMap<UserGroup, SortedMap<String, Double>> group2totalEmissionCosts = userGroupUtils.getEmissionsPerGroup(person2totalEmissionCosts);

		writer.setRunName(runName);
		writer.writeEmissionCostInformation(group2totalEmissionCosts);
		
	}
	
	private void calculateEmissionStatisticsByUserGroup(String eventsFile, String emissionFile, String runName) {
		EmissionsAnalyzer ema = new EmissionsAnalyzer(emissionFile);
		ema.init(null);
		ema.preProcessData();
		ema.postProcessData();
		
		Map<Id, SortedMap<String, Double>> person2totalEmissions = ema.getPerson2totalEmissions();
		SortedMap<UserGroup, SortedMap<String, Double>> group2totalEmissions = userGroupUtils.getEmissionsPerGroup(person2totalEmissions);

		writer.setRunName(runName);
		writer.writeEmissionInformation(group2totalEmissions);
	}

	private void calculateDistanceTimeStatisticsByUserGroup(String netFile, String eventsFile, String runName) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile(netFile);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsReaderXMLv1 eventsReader = new EventsReaderXMLv1(eventsManager);
		
		CarDistanceEventHandler carDistanceEventHandler = new CarDistanceEventHandler(sc.getNetwork());
		TravelTimePerModeEventHandler ttHandler = new TravelTimePerModeEventHandler();

		eventsManager.addHandler(carDistanceEventHandler);
		eventsManager.addHandler(ttHandler);
		eventsReader.parse(eventsFile);
		
		Map<Id, Double> personId2carDistance = carDistanceEventHandler.getPersonId2CarDistance();
		Map<UserGroup, Double> userGroup2carTrips = carDistanceEventHandler.getUserGroup2carTrips();
		Map<String, Map<Id, Double>> mode2personId2TravelTime = ttHandler.getMode2personId2TravelTime();
		Map<UserGroup, Map<String, Double>> userGroup2mode2noOfTrips = ttHandler.getUserGroup2mode2noOfTrips();
		
		case2personId2carDistance.put(runName, personId2carDistance);
		
		logger.warn(runName + ": number of car users in distance map (users with departure events): " + personId2carDistance.size());
//		int depArrOnSameLinkCnt = carDistanceEventHandler.getDepArrOnSameLinkCnt().size();
//		logger.warn("number of car users with two activities followed one by another on the same link: +" + depArrOnSameLinkCnt);
//		int personIsDrivingADistance = 0;
//		for(Id personId : carDistanceEventHandler.getDepArrOnSameLinkCnt().keySet()){
//			if(personId2carDistance.get(personId) == null){
//				// do nothing
//			} else {
//				personIsDrivingADistance ++;
//			}
//		}
//		logger.warn(runName + ": number of car users with two activities followed one by another on the same link BUT driving to other acts: -" + personIsDrivingADistance);
		logger.warn(runName + ": number of car users in traveltime map (users with departure and arrival events): " + mode2personId2TravelTime.get(TransportMode.car).size());
		
		// TODO: this could be probably done outside of the writer (as for welfare above):
		writer.setRunName(runName);
		writer.writeAvgCarDistanceInformation(personId2carDistance, userGroup2carTrips);
		writer.writeDetailedCarDistanceInformation(personId2carDistance);
		writer.writeAvgTTInformation(mode2personId2TravelTime, userGroup2mode2noOfTrips);
	}

	private Scenario loadScenario(String netFile, String plansFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	public static void main(String[] args) {
		MultiAnalyzerRelativeDurations ma = new MultiAnalyzerRelativeDurations();
		ma.run();
	}
}