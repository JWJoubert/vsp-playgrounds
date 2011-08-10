/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialAveragingForLinkEmissions.java
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
package playground.benjamin.scenarios.munich.analysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.benjamin.events.emissions.ColdPollutant;
import playground.benjamin.events.emissions.EmissionEventsReader;
import playground.benjamin.events.emissions.WarmPollutant;

/**
 * @author benjamin
 *
 */
public class SpatialAveragingForLinkEmissions {
	private static final Logger logger = Logger.getLogger(SpatialAveragingForLinkEmissions.class);

	private final String runNumber1 = "972";
	private final String runNumber2 = "973";
	private final String runDirectory1 = "../../runs-svn/run" + runNumber1 + "/";
	private final String runDirectory2 = "../../runs-svn/run" + runNumber2 + "/";
	private final String configFile = runDirectory1 + "output_config.xml.gz";
	private final String netFile = runDirectory1 + "output_network.xml.gz";
	private final String emissionFile1 = runDirectory1 + runNumber1 + ".emission.events.xml.gz";
	private final String emissionFile2 = runDirectory2 + runNumber2 + ".emission.events.xml.gz";

	private final String outPath1 = runDirectory2 + "emissions/" + runNumber2 + "-" + runNumber1 + ".";
	private final String outPath2 = runDirectory2 + "emissions/" + runNumber2 + "-" + runNumber1 + ".";

	EmissionsPerLinkWarmEventHandler warmHandler;
	EmissionsPerLinkColdEventHandler coldHandler;
	Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotal1;
	Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotal2;
	private SortedSet<String> listOfPollutants;

	static int noOfTimeBins = 15;
	double simulationEndTime;

	private void run() {
		this.simulationEndTime = getEndTime(configFile);
		defineListOfPollutants();
		Scenario scenario = loadScenario(netFile);

		processEmissions(emissionFile1);
		Map<Double, Map<Id, Map<String, Double>>> time2warmEmissionsTotal1 = warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
		Map<Double, Map<Id, Map<String, Double>>> time2coldEmissionsTotal1 = coldHandler.getColdEmissionsPerLinkAndTimeInterval();
		this.time2EmissionsTotal1 = sumUpEmissions(time2warmEmissionsTotal1, time2coldEmissionsTotal1);
		setNonCalculatedEmissions(scenario.getNetwork(), this.time2EmissionsTotal1);

		this.warmHandler.reset(0);
		this.coldHandler.reset(0);

		processEmissions(emissionFile2);
		Map<Double, Map<Id, Map<String, Double>>> time2warmEmissionsTotal2 = warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
		Map<Double, Map<Id, Map<String, Double>>> time2coldEmissionsTotal2 = coldHandler.getColdEmissionsPerLinkAndTimeInterval();
		time2EmissionsTotal2 = sumUpEmissions(time2warmEmissionsTotal2, time2coldEmissionsTotal2);
		setNonCalculatedEmissions(scenario.getNetwork(), this.time2EmissionsTotal2);

		Map<Double, Map<Id, Map<String, Double>>> time2deltaEmissionsTotal = calcualateEmissionDifferences(time2EmissionsTotal1, time2EmissionsTotal2);
		EmissionWriter eWriter = new EmissionWriter();
		for(double endOfTimeInterval : time2deltaEmissionsTotal.keySet()){
			Map<Id, Map<String, Double>> deltaEmissionsTotal = time2deltaEmissionsTotal.get(endOfTimeInterval);
			String outFile = outPath1 + (int) endOfTimeInterval + ".emissionsTotalPerLinkLocation.txt";
			eWriter.writeLinkLocation2Emissions(scenario.getNetwork(), listOfPollutants, deltaEmissionsTotal, outFile);
		}
	}

	private Map<Double, Map<Id, Map<String, Double>>> calcualateEmissionDifferences(
			Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotal1,
			Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotal2) {

		Map<Double, Map<Id, Map<String, Double>>> time2delta = new HashMap<Double, Map<Id, Map<String, Double>>>();
		for(Entry<Double, Map<Id, Map<String, Double>>> entry0 : time2EmissionsTotal1.entrySet()){
			double endOfTimeInterval = entry0.getKey();
			Map<Id, Map<String, Double>> delta = entry0.getValue();

			for(Entry<Id, Map<String, Double>> entry1 : delta.entrySet()){
				Id linkId = entry1.getKey();
				Map<String, Double> emissionDifferenceMap = new HashMap<String, Double>();
				for(String pollutant : entry1.getValue().keySet()){
					Double emissionsBefore = entry1.getValue().get(pollutant);
					Double emissionsAfter = time2EmissionsTotal2.get(endOfTimeInterval).get(linkId).get(pollutant);

//					if(emissionsBefore == null || emissionsAfter == null){
//						logger.info(linkId);
//						logger.info(endOfTimeInterval + ": " + pollutant + ": " + emissionsBefore);
//						logger.info(endOfTimeInterval + ": " + pollutant + ": " + emissionsAfter);
//					}

					Double emissionDifference = emissionsAfter - emissionsBefore;
					emissionDifferenceMap.put(pollutant, emissionDifference);
				}
				delta.put(linkId, emissionDifferenceMap);
			}
			time2delta.put(endOfTimeInterval, delta);
		}
		return time2delta;
	}

	private void setNonCalculatedEmissions(Network network,	Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotal) {
		for(Double endOfTimeInterval : time2EmissionsTotal.keySet()){
			Map<Id, Map<String, Double>> emissionsTotal = time2EmissionsTotal.get(endOfTimeInterval);

			for(Link link : network.getLinks().values()){
				Id linkId = link.getId();
				Map<String, Double> emissionType2Value = new HashMap<String, Double>();

				if(emissionsTotal.get(linkId) != null){
					for(String pollutant : listOfPollutants){
						emissionType2Value = emissionsTotal.get(linkId);
						if(emissionType2Value.get(pollutant) != null){
							// do nothing
						} else {
							// setting some emission types that are not available for the link to 0.0
							emissionType2Value.put(pollutant, 0.0);
						}
					}
				} else {
					for(String pollutant : listOfPollutants){
						// setting all emission types for links that had no emissions on it to 0.0 
						emissionType2Value.put(pollutant, 0.0);
					}
				}
				emissionsTotal.put(linkId, emissionType2Value);
			}
			time2EmissionsTotal.put(endOfTimeInterval, emissionsTotal);
		}
	}

	private Map<Double, Map<Id, Map<String, Double>>> sumUpEmissions(
			Map<Double, Map<Id, Map<String, Double>>> time2warmEmissionsTotal,
			Map<Double, Map<Id, Map<String, Double>>> time2coldEmissionsTotal) {

		Map<Double, Map<Id, Map<String, Double>>> time2totalEmissions = new HashMap<Double, Map<Id, Map<String, Double>>>();

		for(Entry<Double, Map<Id, Map<String, Double>>> entry0 : time2warmEmissionsTotal.entrySet()){
			double endOfTimeInterval = entry0.getKey();
			Map<Id, Map<String, Double>> warmEmissions = entry0.getValue();
			Map<Id, Map<String, Double>> totalEmissions = new HashMap<Id, Map<String, Double>>();

			for(Entry<Id, Map<String, Double>> entry1 : warmEmissions.entrySet()){
				Id linkId = entry1.getKey();
				Map<String, Double> linkSpecificWarmEmissions = entry1.getValue();

				if(time2coldEmissionsTotal.get(endOfTimeInterval) != null){
					Map<Id, Map<String, Double>> coldEmissions = time2coldEmissionsTotal.get(endOfTimeInterval);

					if(coldEmissions.get(linkId) != null){
						Map<String, Double> linkSpecificSumOfEmissions = new HashMap<String, Double>();
						Map<String, Double> linkSpecificColdEmissions = coldEmissions.get(linkId);
						Double individualValue;

						for(String pollutant : listOfPollutants){
							if(linkSpecificWarmEmissions.containsKey(pollutant)){
								if(linkSpecificColdEmissions.containsKey(pollutant)){
									individualValue = linkSpecificWarmEmissions.get(pollutant) + linkSpecificColdEmissions.get(pollutant);
								} else{
									individualValue = linkSpecificWarmEmissions.get(pollutant);
								}
							} else{
								individualValue = linkSpecificColdEmissions.get(pollutant);
							}
							linkSpecificSumOfEmissions.put(pollutant, individualValue);
						}
						totalEmissions.put(linkId, linkSpecificSumOfEmissions);
					} else{
						totalEmissions.put(linkId, linkSpecificWarmEmissions);
					}
				} else {
					totalEmissions.put(linkId, linkSpecificWarmEmissions);
				}
				time2totalEmissions.put(endOfTimeInterval, totalEmissions);
			}
		}
		return time2totalEmissions;
	}

	private void processEmissions(String emissionFile) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		this.warmHandler = new EmissionsPerLinkWarmEventHandler(this.simulationEndTime, noOfTimeBins);
		this.coldHandler = new EmissionsPerLinkColdEventHandler(this.simulationEndTime, noOfTimeBins);
		eventsManager.addHandler(this.warmHandler);
		eventsManager.addHandler(this.coldHandler);
		emissionReader.parse(emissionFile);
	}

	private Scenario loadScenario(String netFile) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		config.network().setInputFile(netFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario) ;
		scenarioLoader.loadScenario() ;
		return scenario;
	}

	private void defineListOfPollutants() {
		listOfPollutants = new TreeSet<String>();
		for(WarmPollutant wp : WarmPollutant.values()){
			listOfPollutants.add(wp.toString());
		}
		for(ColdPollutant cp : ColdPollutant.values()){
			listOfPollutants.add(cp.toString());
		}
		logger.info("The following pollutants are considered: " + listOfPollutants);
	}

	private Double getEndTime(String configfile) {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configfile);
		Double endTime = config.getQSimConfigGroup().getEndTime();
		logger.info("Simulation end time is: " + endTime / 3600 + " hours.");
		logger.info("Aggregating emissions for " + endTime / 3600 / noOfTimeBins + "time bins.");
		return endTime;
	}

	public static void main(String[] args) throws IOException{
		new SpatialAveragingForLinkEmissions().run();
	}
}