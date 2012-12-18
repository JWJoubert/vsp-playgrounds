/* *********************************************************************** *
 * project: org.matsim.*
 * Emme2ExternalTripsCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.population;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.population.Desires;

import playground.telaviv.config.TelAvivConfig;

public class Emme2ExternalTripsCreator {

	private static final Logger log = Logger.getLogger(Emme2ExternalTripsCreator.class);
	
	private String networkFile = TelAvivConfig.basePath + "/network/network.xml";
	private String facilitiesFile = TelAvivConfig.basePath + "/facilities/facilities.xml";
//	private String externalTripsFile = TelAvivConfig.basePath + "/population/external_trips.csv";
	private String externalAMTripsFile = TelAvivConfig.basePath + "/population/car_ext_AM.csv";
	private String externalOPTripsFile = TelAvivConfig.basePath + "/population/car_ext_OP.csv";
	private String externalPMTripsFile = TelAvivConfig.basePath + "/population/car_ext_PM.csv";
	
	private String outFile = TelAvivConfig.basePath + "/population/external_plans_10.xml.gz";
	
	private Scenario scenario;
//	private ZoneMapping zoneMapping;
	private Random random = new Random(123456);
	private Network network;
	private ActivityFacilities activityFacilities; 
	
	/*
	 * We got Trips per Hour in the input file.
	 * The Period lasts from 06:00 to 09:00 therefore
	 * we have to multiply the number of trips by 3.
	 */
//	private double periodDuration = 3;
	private double periodDurationAM = 3.0;
	private double periodDurationOP = 6.0;
	private double periodDurationPM = 5.0;
	
	/*
	 * We use a 10% Scenario therefore we have to reduce
	 * the created Trips by a Factor of 10.
	 */
	private double scaleFactor = 0.1; 
	
	public static void main(String[] args) {
		new Emme2ExternalTripsCreator(((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())));
	}
	
	public Emme2ExternalTripsCreator(Scenario scenario) {		
		this.scenario = scenario;
		
		new MatsimNetworkReader(scenario).readFile(networkFile);
		network = scenario.getNetwork();
		log.info("Loading Network ... done");
		
		new MatsimFacilitiesReader((ScenarioImpl)scenario).readFile(facilitiesFile);
		activityFacilities = ((ScenarioImpl)scenario).getActivityFacilities();
		log.info("Loading Facilities ... done");
		
		Counter counter = new Counter("Created people from external population #: ");
		
		if(externalAMTripsFile != null && !externalAMTripsFile.equals("")) {
			createExternalTrips(externalAMTripsFile, periodDurationAM, counter, new AMDepartureTimeCalculator());
		}

		if(externalOPTripsFile != null && !externalOPTripsFile.equals("")) {
			createExternalTrips(externalOPTripsFile, periodDurationOP, counter, new OPDepartureTimeCalculator());
		}
		
		if(externalPMTripsFile != null && !externalPMTripsFile.equals("")) {
			createExternalTrips(externalPMTripsFile, periodDurationPM, counter, new PMDepartureTimeCalculator());
		}
		
		counter.printCounter();
		
		log.info("Writing MATSim population to file...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(), scaleFactor).write(outFile);
		log.info("done.");
	}
	
	private void createExternalTrips(String externalTripsFile, double periodDuration, Counter counter,
			DepartureTimeCalculator departureTimeCalculator) {
		log.info("\tParsing external trips file...");
		List<Emme2ExternalTrip> externalTrips = new Emme2ExternalTripFileParser(externalTripsFile).readFile();
		log.info("done.");
		
		log.info("\tCreating MATSim external Trips...");
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		
		for (Emme2ExternalTrip externalTrip : externalTrips) {
			int numOfTrips = (int)(periodDuration * Double.valueOf(externalTrip.numOfTrips));
			
			for (int i = 0; i < numOfTrips; i++) {
				Id id = scenario.createId("tta_" + String.valueOf(counter.getCounter()));
				PersonImpl person = (PersonImpl)populationFactory.createPerson(id);
				
				setBasicParameters(person);		
				
				createAndAddInitialPlan(person, externalTrip, departureTimeCalculator);
				
				scenario.getPopulation().addPerson(person);
				
				counter.incCounter();
			}
		}	
		log.info("\tdone.");
	}
	
	/*
	 * Set some basic person parameters like age, sex, license and car availability.
	 */
	private void setBasicParameters(PersonImpl person) {
		person.setAge(100);
		person.setSex("m");
		person.setLicence("yes");
		person.setCarAvail("always");		
	}
	
	/*
	 * Create initial plan.
	 * 
	 * Activity Coding:
	 * 0 - no (home)
	 * 1 - work
	 * 2 - study (education type depending on type of zone!)
	 * 3 - shopping
	 * 4 - other (leisure)
	 */
	public void createAndAddInitialPlan(PersonImpl person, Emme2ExternalTrip externalTrip, 
			DepartureTimeCalculator departureTimeCalculator) {
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		
		Plan plan = populationFactory.createPlan();
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		Desires desires = person.createDesires("");
		
		LegImpl leg;
		ActivityImpl activity;
		ActivityFacility activityFacility;

		int originNode = externalTrip.originNodeId;
		int destionationNode = externalTrip.destinationNodeId;
		
		Id originNodeId = scenario.createId(String.valueOf(originNode));
		Id destinationNodeId = scenario.createId(String.valueOf(destionationNode));
		
		Id originLinkId = selectLinkByStartNode(originNodeId);
		Id destinationLinkId = selectLinkByStartNode(destinationNodeId);
		
		double departureTime = departureTimeCalculator.getDepartureTime(random);
		
		/*
		 * create tta activity in origin zone
		 * create car leg from origin zone to destination zone
		 * create tta activity in destination zone 
		 */
		activity = (ActivityImpl) populationFactory.createActivityFromLinkId("tta", originLinkId);
		activity.setStartTime(0.0);
		activity.setMaximumDuration(departureTime);
		activity.setEndTime(departureTime);
		activityFacility = getActivityFacilityByLinkId(originLinkId);
		activity.setFacilityId(activityFacility.getId());
		activity.setCoord(activityFacility.getCoord());
		plan.addActivity(activity);
		
		leg = (LegImpl)populationFactory.createLeg(TransportMode.car);
		leg.setDepartureTime(departureTime);
		leg.setTravelTime(0.0);
		leg.setArrivalTime(departureTime);
		plan.addLeg(leg);
		
		activity = (ActivityImpl) populationFactory.createActivityFromLinkId("tta", destinationLinkId);
		activity.setStartTime(departureTime);
		activityFacility = getActivityFacilityByLinkId(destinationLinkId);
		activity.setFacilityId(activityFacility.getId());
		activity.setCoord(activityFacility.getCoord());
		plan.addActivity(activity);
		
		/*
		 * Finally add a tta desire that has a duration of 86400 - all other activities.
		 */
		desires.accumulateActivityDuration("tta", 86400);
	}
	
	/*
	 * The link is selected randomly but the length of the links 
	 * is used to weight the probability.
	 */
	private Id selectLinkByStartNode(Id nodeId) {		
		Node startNode = network.getNodes().get(nodeId);
		List<Id> linkIds = new ArrayList<Id>();
		
		for (Link link : startNode.getOutLinks().values()) linkIds.add(link.getId());
		
		if (linkIds.size() == 0) {
			log.warn("startNode " + startNode.getId() + " has no outgoing Links!");
			return null;
		}
		
		double totalLength = 0;
		for (Id id : linkIds) {
			Link link = network.getLinks().get(id);
			totalLength = totalLength + link.getLength();
		}
		
		double[] probabilities = new double[linkIds.size()];
		double sumProbability = 0.0;
		for (int i = 0; i < linkIds.size(); i++) {
			Link link = network.getLinks().get(linkIds.get(i));
			double probability = link.getLength() / totalLength;
			probabilities[i] = sumProbability + probability;
			sumProbability = probabilities[i];
		}
		
		// if we have only one link we can return that one
		if (linkIds.size() == 1) return linkIds.get(0);
		
		// else find the right one
		double randomProbability = random.nextDouble();
		for (int i = 0; i <= linkIds.size(); i++) {
			if (randomProbability <= probabilities[i]) return linkIds.get(i);
		}
		return null;
	}
	
	
	/*
	 * We get the Id of a Link that is connected to an external Node.
	 */
	private ActivityFacility getActivityFacilityByLinkId(Id id) {	
		return activityFacilities.getFacilities().get(id);
	}
	
//	private double getDepartureTime() {
//		double d = random.nextDouble();
//		
////		d = d * 12;
////		
////		if (d < 1)       return 6.0 * 3600 + Math.round(d * 30 * 60);
////		else if (d < 3)  return 6.5 * 3600 + Math.round((d - 1)/2 * 30 * 60);
////		else if (d < 6)  return 7.0 * 3600 + Math.round((d - 3)/3 * 30 * 60);
////		else if (d < 9)  return 7.5 * 3600 + Math.round((d - 6)/3 * 30 * 60);
////		else if (d < 11) return 8.0 * 3600 + Math.round((d - 9)/2 * 30 * 60);
////		else             return 8.5 * 3600 + Math.round((d - 11)/2 * 30 * 60);	// shouldn't that be (d - 11) * 30 * 60???
//		
//		d = d * 6;
//		
//		if (d < 1)       return 6.5 * 3600 + Math.round(d * 30 * 60);
//		else if (d < 3)  return 7.0 * 3600 + Math.round((d - 1)/2 * 30 * 60);
//		else if (d < 5)  return 7.5 * 3600 + Math.round((d - 3)/2 * 30 * 60);
//		else             return 8.0 * 3600 + Math.round((d - 5) * 30 * 60);
//	}

	private static interface DepartureTimeCalculator {
		
		double getDepartureTime(Random random);
	}
	
//	AM: 0600-0900
//	0600 - 0630: 1/12 of all AM trips	0 .. 1/12
//	0630 - 0700: 2/12 of all AM trips	1/12 .. 3/12
//	0700 - 0730: 3/12 of all AM trips	3/12 .. 6/12
//	0730 - 0800: 3/12 of all AM trips	6/12 .. 9/12
//	0800 - 0830: 2/12 of all AM trips	9/12 .. 11/12
//	0830 - 0900: 1/12 of all AM trips	11/12 .. 12/12
	private static class AMDepartureTimeCalculator implements DepartureTimeCalculator {
		
		@Override
		public double getDepartureTime(Random random) {
			return getAMDepartureTime(random);
		}
		
		private double getAMDepartureTime(Random random) {
			double d = random.nextDouble();	// select time bin
			double d2 = random.nextDouble();	// select offset within time bin
			
			double delta = 0.0;
			if (d < 1d/12d) delta = Math.round(d2 * 1800.0);
			else if (d < 3d/12d) delta = 0.5 * 3600.0 + Math.round(d2 * 1800.0);
			else if (d < 6d/12d) delta = 1.0 * 3600.0 + Math.round(d2 * 1800.0);
			else if (d < 9d/12d) delta = 1.5 * 3600.0 + Math.round(d2 * 1800.0);
			else if (d < 11d/12d) delta = 2.0 * 3600.0 + Math.round(d2 * 1800.0);
			else delta = 2.5 * 3600.0 + Math.round(d2 * 1800.0);
			
			return 6 * 3600.0 + delta;
		}
	}

//	OP: 0900-1500
//	equally distributed (1/12 of all OP trips for each half an hour)
	private static class OPDepartureTimeCalculator implements DepartureTimeCalculator {
		
		@Override
		public double getDepartureTime(Random random) {
			return getOPDepartureTime(random);
		}
		
		private double getOPDepartureTime(Random random) {
			double d = random.nextDouble();
			return 9 * 3600.0 + Math.round(d * 6*3600.0);
		}
	}

//	PM: 1500-2000
//	1500 - 1530: 1/20 of all PM trips	0 .. 0.05
//	1530 - 1600: 2/20 of all PM trips	0.05 .. 0.15
//	1600 - 1630: 2/20 of all PM trips	0.15 .. 0.25
//	1630 - 1700: 2/20 of all PM trips	0.25 .. 0.35
//	1700 - 1730: 3/20 of all PM trips	0.35 .. 0.50
//	1730 - 1800: 3/20 of all PM trips	0.50 .. 0.65
//	1800 - 1830: 2/20 of all PM trips	0.65 .. 0.75
//	1830 - 1900: 2/20 of all PM trips	0.75 .. 0.85
//	1900 - 1930: 2/20 of all PM trips	0.85 .. 0.95
//	1930 - 2000: 1/20 of all PM trips	0.95 .. 1.00
	private static class PMDepartureTimeCalculator implements DepartureTimeCalculator {
		
		@Override
		public double getDepartureTime(Random random) {
			return getPMDepartureTime(random);
		}
		
		private double getPMDepartureTime(Random random) {
			double d = random.nextDouble();	// select time bin
			double d2 = random.nextDouble();	// select offset within time bin
			
			double delta = 0.0;
			if (d < 0.05) delta = Math.round(d2 * 1800.0);
			else if (d < 0.15) delta = 0.5 * 3600.0 + Math.round(d2 * 1800.0);
			else if (d < 0.25) delta = 1.0 * 3600.0 + Math.round(d2 * 1800.0);
			else if (d < 0.35) delta = 1.5 * 3600.0 + Math.round(d2 * 1800.0);
			else if (d < 0.50) delta = 2.0 * 3600.0 + Math.round(d2 * 1800.0);
			else if (d < 0.65) delta = 2.5 * 3600.0 + Math.round(d2 * 1800.0);
			else if (d < 0.75) delta = 3.0 * 3600.0 + Math.round(d2 * 1800.0);
			else if (d < 0.85) delta = 3.5 * 3600.0 + Math.round(d2 * 1800.0);
			else if (d < 0.95) delta = 4.0 * 3600.0 + Math.round(d2 * 1800.0);
			else delta = 4.5 * 3600.0 + Math.round(d2 * 1800.0);
			
			return 15 * 3600.0 + delta;
		}
	}
}