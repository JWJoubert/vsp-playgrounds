/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.julia.toi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class ShapeConverterPopRandom {

	static String shapeFile = "input/oslo/Start_og_stopp_i_TRD_fra_RVU2/testplott5_end.shp";
	static String networkFile = "input/oslo/trondheim_network.xml";
	static String plansFile = "input/oslo/plans_from_start_og_random.xml";
	static Collection<SimpleFeature> features;
	static Logger logger = Logger.getLogger(ShapeConverterPopRandom.class);
	private static int countKnownActTypes =0;
	private static Double factor = 3.69;
	private static Double shift = 100.;
	private static Scenario scenario;
	private static List<Person> newPersons;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Config config = new Config();
		config.addCoreModules();
		Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);

		ShapeFileReader sfr = new ShapeFileReader();
		features = sfr.readFileAndInitialize(shapeFile);
		
		ActivityParams home = new ActivityParams("home");
		config.planCalcScore().addActivityParams(home);
		home.setTypicalDuration(16*3600);
		ActivityParams work = new ActivityParams("work");
		config.planCalcScore().addActivityParams(work);
		work.setTypicalDuration(8*3600);
		ActivityParams other = new ActivityParams("other");
		other.setTypicalDuration(1*3600);
		config.planCalcScore().addActivityParams(other);
		
		scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile(networkFile);
		Population pop = fillScenario();
		new PopulationWriter(pop, scenario.getNetwork()).write(plansFile);

		
		//Node node1 = network.createAndAddNode(scenario.createId("1"), scenario.createCoord(0.0, 10000.0));
		logger.info("Population size "+ pop.getPersons().size());
		logger.info("number of features" + features.size());
		logger.info("features with know acttype " + countKnownActTypes);
				
		}

	private static Population fillScenario() {
		
		Population population = scenario.getPopulation();
		
		PopulationFactory populationFactory = population.getFactory();
		
		newPersons = new ArrayList<Person>();
		
		Map<Id, List<Trip>> personid2trips= new HashMap<Id, List<Trip>>();
		
		for(SimpleFeature sf: features){
			
			
			
			Double time = getTimeInSeconds((String) sf.getAttribute("klokkeslet"));
			
			if (time>0.0) {
				
				
				Integer id = (Integer) sf.getAttribute("ID_NUM2"); //ID_2 --> pop size = 59059
				String idstring = Integer.toString(id);
				Id personId = new IdImpl(idstring);
				
				Double startx = (Double) sf.getAttribute("start_x");
				Double starty = (Double) sf.getAttribute("start_y");
				Double endx = (Double) sf.getAttribute("ende_x");
				Double endy = (Double) sf.getAttribute("ende_y");
				
				Coord startCoordinates = scenario.createCoord(startx, starty);
				Coord endCoordinates = scenario.createCoord(endx, endy);
				
				String actType = (String) sf.getAttribute("Formal_enk"); // activity type
				actType = actType.toLowerCase();
				
				if(!(actType.equals("home")||actType.equals("work")||actType.equals("commute")||actType.equals("other"))){
					logger.warn("new act type " + actType);
				}else{
					countKnownActTypes ++;
				}
				
				String legmode = (String) sf.getAttribute("trmh_enkel"); //TODO
				legmode = legmode.toLowerCase();
				
				Double tripnr = (Double) sf.getAttribute("reisenr");
				int tripnumber = tripnr.intValue();
				
				// map: person id 2 list of trips
				Trip trip = new Trip(startCoordinates, endCoordinates, time, legmode, actType, tripnumber);
				
				if(personid2trips.containsKey(personId)){
					personid2trips.get(personId).add(trip);
					
				}else{
					
					personid2trips.put(personId, new ArrayList<Trip>());
					personid2trips.get(personId).add(trip);
				}
				
			}else{
			//	logger.info(sf.getAttributes().toString());
			}			
			
		}
		
		for(Id id: personid2trips.keySet()){
			
			Person person = populationFactory.createPerson(id);
			population.addPerson(person);
			Plan plan = populationFactory.createPlan();
			person.addPlan(plan);
			Trip sortedTrips[] = getSortedTrips(personid2trips.get(id));
		
			String firstActType = sortedTrips[sortedTrips.length-1].getActivityType();
			Activity firstAct = populationFactory.createActivityFromCoord(firstActType, sortedTrips[0].getStartCoord());
			firstAct.setEndTime(sortedTrips[0].getTime());
			plan.addActivity(firstAct);
			
			
			for(int i = 0; i<sortedTrips.length; i++){
				// add leg
				plan.addLeg(populationFactory.createLeg(sortedTrips[i].getLeg()));
				// add end activity
				Activity nextAct = populationFactory.createActivityFromCoord(sortedTrips[i].getActivityType(), sortedTrips[i].getEndCoord());
				if(i<sortedTrips.length-1){
					nextAct.setEndTime(sortedTrips[i+1].getTime());
				}
				plan.addActivity(nextAct);
			}
		}
		

		multiplyPopulation(population, populationFactory, factor );
		
		for(Person p: newPersons){
			population.addPerson(p);			
		}
		return population;
	}
	
	
	private static void multiplyPopulation(Population population, PopulationFactory popfac, Double factor) {
		
		int additionalNumber = (int) Math.floor(factor);
		double randomNumber = factor - additionalNumber;
		
		for(Id personId: population.getPersons().keySet()){
			for(int i=0; i<additionalNumber; i++){
				createNewRandomPerson(population, popfac, population.getPersons().get(personId), i);
			}
			if(Math.random()<randomNumber){
				createNewRandomPerson(population, popfac, population.getPersons().get(personId), additionalNumber+1);
			}
		}
		
		
	}

	private static void createNewRandomPerson(Population population, PopulationFactory popfac, Person person, int i) {
		Person newPerson = popfac.createPerson(new IdImpl(person.getId().toString()+"-"+i));
		
		Plan originalPlan = person.getSelectedPlan();
		Plan newPlan = popfac.createPlan();
		
		for(PlanElement pe: originalPlan.getPlanElements()){
			if(pe instanceof Leg){
				Leg newLeg = popfac.createLeg(((Leg) pe).getMode());
				newPlan.addLeg(newLeg);
			}
			if(pe instanceof Activity){
				Activity originalActivity = (Activity) pe;
				Double xCoord = originalActivity.getCoord().getX() + Math.random()*shift - .5*shift;
				Double yCoord = originalActivity.getCoord().getY() + Math.random()*shift - .5*shift;
				Activity act = popfac.createActivityFromCoord(originalActivity.getType(), scenario.createCoord(xCoord, yCoord));
				newPlan.addActivity(act);
			}
		}
		
		Activity firstAct = (Activity) newPlan.getPlanElements().get(0);
		Double firstx = firstAct.getCoord().getX();
		Double firsty = firstAct.getCoord().getY();
		Activity lastAct = (Activity) newPlan.getPlanElements().get(newPlan.getPlanElements().size()-1);
		lastAct.getCoord().setXY(firstx, firsty);
		
		newPerson.addPlan(newPlan);
		newPersons.add(newPerson);
		
		
	}

	private static Trip[] getSortedTrips(List<Trip> list) {
		
		int maxTripnr =0;
		for(Trip trip: list){
			if(trip.getNumber()>maxTripnr)maxTripnr=trip.getNumber();
		}
		
		Trip[] sortedTripsBig = new Trip[maxTripnr];
		
		for(Trip trip: list){
			int tripnumber = trip.getNumber();
			sortedTripsBig[tripnumber-1]= trip;
		}
		
		int nextToTry =0;
		Trip[] sortedTrips = new Trip[list.size()];
		for(int i=0; i< sortedTrips.length; i++){
			while(sortedTripsBig[nextToTry]==null){
				nextToTry++;
			}
			sortedTrips[i]=sortedTripsBig[nextToTry];
			nextToTry++;
		}
		return sortedTrips;
	}
	private static Double getTimeInSeconds(String timeString) {
//		logger.info(timeString);
		try{
		String[] split = timeString.split(" ");
		String[] split2 = split[1].split(":");
		Double time = 60 *60 * Double.parseDouble(split2[0]) 
				+ 60 * Double.parseDouble(split2[1]) 
				+ Double.parseDouble(split2[2]);
		return time;
		}catch(ArrayIndexOutOfBoundsException e){
		//	logger.warn("couldnt parse time from " + timeString);
		}
		return -1.;
	}
		
		
	
//	}
}
