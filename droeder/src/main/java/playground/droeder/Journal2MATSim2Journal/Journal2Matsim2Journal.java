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
package playground.droeder.Journal2MATSim2Journal;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.run.Controler;
import org.xml.sax.SAXException;

import playground.droeder.DaFileReader;
import playground.droeder.DaPaths;


/**
 * @author droeder
 *
 */
public class Journal2Matsim2Journal {
	private static final Logger log = Logger
			.getLogger(Journal2Matsim2Journal.class);
	
	private static final String INPUTDIR = DaPaths.OUTPUT + "journals/";
	
	private String inFile = null;
	private String networkFile = null;
	private String plansUnrouted = null;
	private String schedule = null;
	private String vehicles = null;
	private String configFile = null;
	private String outDir = null;
	private String splitByExpr = null;
	
	private double startTime = Double.MAX_VALUE;
	
	private boolean inFileHasHeader;
	private Trip fileHeader;
	private Set<String[]> inFileContent;
	
	
	private Map<String, List<Trip>> person2ways;
	private Map<String, List<Trip>> person2waysUnrouted;
	private Map<Id, Set<PersonEvent>> person2Event;
	private Map<String, Set<List<PersonEvent>>> carId2TripEvents;
	private Map<String, Set<List<PersonEvent>>> ptId2TripEvents;
	
	private final String ACT_0 = "home";
	private final String ACT_1 = "work";
	private final String ACT_2 = "other";
	
	public static void main(String[] args){
		Journal2Matsim2Journal j = new Journal2Matsim2Journal(INPUTDIR + "dummy.csv", ";", true);
		j.run(INPUTDIR + "multimodalnetwork.xml", INPUTDIR + "transitVehicles.xml", INPUTDIR + "transitSchedule.xml", INPUTDIR);
		
	}
	
	public Journal2Matsim2Journal(String journalFile, String splitByExpr, boolean hasHeader){
		this.inFile = journalFile;
		this.splitByExpr = splitByExpr;
		this.inFileHasHeader = hasHeader;
	}
	
	public void run(String networkFile, String vehicles, String schedule, String outputDirectory){
		this.outDir = outputDirectory;
		this.networkFile = networkFile;
		this.schedule = schedule;
		this.vehicles = vehicles;
		this.plansUnrouted = this.outDir + "plans_unrouted.xml";
		this.configFile = this.outDir + "config.xml";

		this.readJournal();
		this.sortWaysByPerson();
		this.generatePlans();
		
		this.createAndWriteConfig();
		
		this.runQSim();
		
		this.readEventsFile();
		this.sortTripEventsByMode();
		this.analyzeEvents();
		
		this.writeNewJournal();
	}


	private void readJournal() {
		log.info("read infile");
		this.fileHeader = new Trip(DaFileReader.readFileHeader(this.inFile, this.splitByExpr));
		this.fileHeader.setPtTime("travelTimePT", "transitTimePT", "waitingTimePT");
		this.fileHeader.setCarTime("carTime");
		
		this.inFileContent = DaFileReader.readFileContent(this.inFile, this.splitByExpr, this.inFileHasHeader);
		log.info("done...");
	}
	
	private void sortWaysByPerson() {
		log.info("sort ways by person");
		this.person2ways = new TreeMap<String, List<Trip>>();
		this.person2waysUnrouted = new TreeMap<String, List<Trip>>();
		String id;
		for(String[] way : this.inFileContent){
			id = way[2];
			
			if(this.person2ways.containsKey(id)&& !(way[23].equals("9999999")) && !(way[181].equals("9999999"))){
				this.person2ways.get(id).add(new Trip(way));
			}else if(!(way[23].equals("9999999")) && !(way[181].equals("9999999"))){
				this.person2ways.put(id, new LinkedList<Trip>());
				this.person2ways.get(id).add(new Trip(way));
			}else{
				if(this.person2waysUnrouted.containsKey(id)){
					this.person2waysUnrouted.get(id).add(new Trip(way));
				}else{
					this.person2waysUnrouted.put(id, new LinkedList<Trip>());
					this.person2waysUnrouted.get(id).add(new Trip(way));
				}
				
				log.error("wayId " + way[1] + "not processed, because no coordinates are given for origin and/or destination!"); 
			}
		}
		log.info("done");
	}
	
	
	private void generatePlans(){
		log.info("generate plans");
		Scenario sc = new ScenarioImpl();
		PopulationFactory fac = sc.getPopulation().getFactory();
		Person personCar;
		Person personPt;
		Plan planCar;
		Plan planPt;
		Coord startCoord;
		Coord endCoord;
		String type;
		
		double tripStartTime;
		double tripEndTime;

		for(Entry<String, List<Trip>> e : this.person2ways.entrySet()){
			personCar = fac.createPerson(sc.createId(e.getKey() + "_" + TransportMode.car.toString()));
			planCar = fac.createPlan(); 
				
			personPt = fac.createPerson(sc.createId(e.getKey() + "_" + TransportMode.pt.toString()));
			planPt = fac.createPlan();
			
			SortedMap<Integer, String> way;
			Iterator<Trip> it = e.getValue().iterator();
			boolean addPerson = true;
			do{
				way = it.next().getAll();
				
				tripStartTime = Double.valueOf(way.get(27));
					if(this.startTime > tripStartTime) this.startTime = tripStartTime;
				tripEndTime = Double.valueOf(way.get(184));
				
				
//				startCoord = new CoordImpl(1000, 1000);
//				endCoord = new CoordImpl(4000, 1000);
				startCoord = new CoordImpl(way.get(24), way.get(25));
				endCoord = new CoordImpl(way.get(182), way.get(183));
				
				
				//add first planelement
				if(planCar.getPlanElements().size() == 0){		
					if(way.get(17).equals("1")) {
						type = this.ACT_0;
					}else{
						type = this.ACT_2;
					}
					addActivity(planCar, startCoord, null, tripStartTime, fac, type);
					addActivity(planPt, startCoord, null, tripStartTime, fac, type);
				}
				// check last planelement, must be an activity
				else if(planCar.getPlanElements().get(planCar.getPlanElements().size()-1) instanceof Leg){
					addPerson = false;
					break;
				}
				// if not first activity and not leg add startTime of the current trip to the former activity
				else{
					((Activity) planCar.getPlanElements().get(planCar.getPlanElements().size() - 1)).setEndTime(tripStartTime);
					((Activity) planPt.getPlanElements().get(planPt.getPlanElements().size() - 1)).setEndTime(tripStartTime);
				}
				
				this.addLeg(planCar, tripStartTime, TransportMode.car, fac);
				this.addLeg(planPt, tripStartTime, TransportMode.pt, fac);
				
				if(way.get(175).equals("1")){
					type = this.ACT_0;
				}else if(way.get(28).equals("1")){
					type = this.ACT_1;
				}else{
					type = this.ACT_2;
				}
				this.addActivity(planCar, endCoord, tripEndTime, null, fac, type);
				this.addActivity(planPt, endCoord, tripEndTime, null, fac, type);
				
			}while(it.hasNext());
			
			//handle Last Activity
			((Activity) planCar.getPlanElements().get(planCar.getPlanElements().size() - 1)).setEndTime(tripEndTime * 1.1);
			((Activity) planPt.getPlanElements().get(planPt.getPlanElements().size() - 1)).setEndTime(tripEndTime * 1.1);
			
			// add only if all ways have an start and endtime
			if(addPerson){
				personCar.addPlan(planCar);
				sc.getPopulation().addPerson(personCar);
				
				personPt.addPlan(planPt);
				sc.getPopulation().addPerson(personPt);
			}else{
				log.error("Person ID " + e.getKey() + " was not added, because of a missing start- or endtime!"); 
			}
		}
		log.info("done");
		new PopulationWriter(sc.getPopulation(),null).writeV4(plansUnrouted);
	}

	private void addActivity(Plan plan, Coord c, Double startTime, Double endTime, PopulationFactory fac, String type) {
		Activity a = fac.createActivityFromCoord(type, c);
		
		if(startTime == null){
			a.setStartTime(endTime * 0.9);
		}else{
			a.setStartTime(startTime);
		}
		
		if(!(endTime == null)){
			a.setEndTime(endTime);
		}
		
		plan.addActivity(a);
	}
	
	private void addLeg(Plan plan, double start, String mode, PopulationFactory fac) {
		Leg l = fac.createLeg(mode);
		l.setDepartureTime(start);
		plan.addLeg(l);
	}

	private void createAndWriteConfig() {
		Scenario s = new ScenarioImpl();
		Config c = s.getConfig();
		
		c.getModule(GlobalConfigGroup.GROUP_NAME).addParam("coordinateSystem", "Atlantis");
		c.getModule(GlobalConfigGroup.GROUP_NAME).addParam("randomSeed", "4711");
		
		c.getModule(NetworkConfigGroup.GROUP_NAME).addParam("inputNetworkFile", this.networkFile);
		
		c.getModule(PlansConfigGroup.GROUP_NAME).addParam("inputPlansFile", this.plansUnrouted);
		
		c.controler().setFirstIteration(0);
		c.controler().setLastIteration(0);
		c.controler().setOutputDirectory(this.outDir);
			Set<EventsFileFormat> eventsFormat = new TreeSet<EventsFileFormat>();
			eventsFormat.add(EventsFileFormat.xml);
			c.controler().setEventsFileFormats(eventsFormat);
		c.controler().setMobsim(QSimConfigGroup.GROUP_NAME);
		
		
		c.addQSimConfigGroup(new QSimConfigGroup());
		c.getQSimConfigGroup().setStartTime(this.startTime);
		c.getQSimConfigGroup().setEndTime(108000);
		c.getQSimConfigGroup().setSnapshotPeriod(0);
		c.getQSimConfigGroup().setSnapshotFormat("otfvis");
		
		c.getModule(StrategyConfigGroup.GROUP_NAME).addParam("maxAgentPlanMemorySize", "1");
		c.getModule(StrategyConfigGroup.GROUP_NAME).addParam("ModuleProbability_1", "1");
		c.getModule(StrategyConfigGroup.GROUP_NAME).addParam("Module_1", "BestScore");
		
		c.getModule(PlanCalcScoreConfigGroup.GROUP_NAME).addParam("activityType_0"  , this.ACT_0);
		c.getModule(PlanCalcScoreConfigGroup.GROUP_NAME).addParam("activityTypicalDuration_0"  , "12:00:00");
		
		c.getModule(PlanCalcScoreConfigGroup.GROUP_NAME).addParam("activityType_1"  , this.ACT_1);
		c.getModule(PlanCalcScoreConfigGroup.GROUP_NAME).addParam("activityTypicalDuration_1"  , "08:00:00");
		
		c.getModule(PlanCalcScoreConfigGroup.GROUP_NAME).addParam("activityType_2"  , this.ACT_2);
		c.getModule(PlanCalcScoreConfigGroup.GROUP_NAME).addParam("activityTypicalDuration_2"  , "01:00:00");
		
		
		c.getModule(TransitConfigGroup.GROUP_NAME).addParam("transitScheduleFile", this.schedule);
		c.getModule(TransitConfigGroup.GROUP_NAME).addParam("vehiclesFile", this.vehicles);
		c.getModule(TransitConfigGroup.GROUP_NAME).addParam("transitModes", "pt");
		
		c.scenario().setUseTransit(true);
		c.scenario().setUseVehicles(true);
		
		new ConfigWriter(c).write(this.configFile);
	}

	private void runQSim() {
		Controler c = new Controler(this.configFile);
		c.setOverwriteFiles(true);
		c.run();
	}

	
	private void readEventsFile(){
		EventsManager events = new EventsManagerImpl();
		TripDurationHandler handler = new TripDurationHandler();
		events.addHandler(handler);
		
		try {
			new EventsReaderXMLv1(events).parse(this.outDir + "ITERS/it.0/0.events.xml.gz");
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.person2Event = handler.getId2Event();
	}
	
	private void sortTripEventsByMode(){
		log.info("sort events by trip and mode");
		this.carId2TripEvents = new HashMap<String, Set<List<PersonEvent>>>();
		this.ptId2TripEvents = new HashMap<String, Set<List<PersonEvent>>>();
		
		for(Entry<Id, Set<PersonEvent>> entry : this.person2Event.entrySet()){
			
			boolean start = true;
			String[] id = entry.getKey().toString().split("_");
			
			
			if(id[1].equals("pt")){
				this.ptId2TripEvents.put(id[0], new LinkedHashSet<List<PersonEvent>>());
			}else if(id[1].equals("car")){
				this.carId2TripEvents.put(id[0], new LinkedHashSet<List<PersonEvent>>());
			}else{
				continue;
			}
			
			List<PersonEvent> temp = null;
			for(PersonEvent e : entry.getValue()){
				if(start){
					temp = new LinkedList<PersonEvent>();
					start = false;
				}
				temp.add(e);
				
				if(e instanceof ActivityStartEvent && !((ActivityStartEvent)e).getActType().equals("pt interaction")){
					start = true;
					if(id[1].equals("pt")){
						this.ptId2TripEvents.get(id[0]).add(temp);
					}else{
						this.carId2TripEvents.get(id[0]).add(temp);
					}
				}
			}
		}
		log.info("done...");
	}
	
	private void analyzeEvents(){
		log.info("analyze events");
		double travelTime, transitTime, waitingTime;
		int i;
		
		for(Entry<String, Set<List<PersonEvent>>> e :this.carId2TripEvents.entrySet()){
			
			// get time for car
			i = 0;
			for(List<PersonEvent> list : e.getValue()){
				travelTime = 0;
				travelTime = ((LinkedList<PersonEvent>) list).getLast().getTime() - ((LinkedList<PersonEvent>) list).getFirst().getTime();
				this.person2ways.get(e.getKey()).get(i).setCarTime(travelTime);
				i++;
			}
			
			// get times for pt
			i = 0;
			for(List<PersonEvent> list : this.ptId2TripEvents.get(e.getKey())){
				travelTime = 0;
				transitTime = 0;
				waitingTime = 0;

				travelTime = ((LinkedList<PersonEvent>) list).getLast().getTime() - ((LinkedList<PersonEvent>) list).getFirst().getTime();
				for(PersonEvent pe : list){
					if(pe instanceof ActivityStartEvent){
						if(((ActivityStartEvent) pe).getActType().equals("pt interaction")){
							waitingTime = waitingTime - pe.getTime();
						}
					}else if (pe instanceof ActivityEndEvent){
						if(((ActivityEndEvent) pe).getActType().equals("pt interaction")){
							waitingTime = waitingTime + pe.getTime();
						}
					}else if (pe instanceof AgentDepartureEvent){
						if(((AgentDepartureEvent)pe).getLegMode().equals(TransportMode.transit_walk)){
							transitTime = transitTime - pe.getTime();
						}
					}else if (pe instanceof AgentArrivalEvent){
						if(((AgentArrivalEvent)pe).getLegMode().equals(TransportMode.transit_walk)){
							transitTime = transitTime + pe.getTime();
						}
					}
				}
				this.person2ways.get(e.getKey()).get(i).setPtTime(travelTime, transitTime, waitingTime);
				i++;
			}
		}
		log.info("done...");
	}
	
	private void writeNewJournal() {
		String outFile = this.outDir + "MATSim_journal.csv";
		log.info("write new journal");
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
			
			for(Entry<Integer, Trip> t : this.preProcessForWriting().entrySet()){
				System.out.print(t.getKey() + "\t");
				for(String s : t.getValue().getAll().values()){
					writer.write(s + ";");
					System.out.print(s + "\t");
				}
				System.out.println();
				writer.newLine();
			}
			
			writer.close();
			log.info("journal written to " + outFile + "...");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
//		try {
//			BufferedWriter writer = IOUtils.getBufferedWriter(outdir);
//			
//			for(String s : this.fileHeader.getAll().values()){
//				writer.write(s + ";");
//			}
//			writer.newLine();
//			
//			for(List<Trip> l : this.person2ways.values()){
//				for(Trip t : l){
//					for(String s : t.getAll().values()){
//						writer.write(s + ";");
//					}
//					writer.newLine();
//				}
//			}
//			writer.close();
//			
//			log.info("journal written to " + outdir + "...");
//			
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
	}

	private SortedMap<Integer, Trip> preProcessForWriting() {
		SortedMap<Integer, Trip> trips = new TreeMap<Integer, Trip>();
		trips.put(0, this.fileHeader);
		
		for(List<Trip> l: person2ways.values()){
			for(Trip t : l){
				trips.put(Integer.valueOf(t.getElement(1)+1),t);
			}
		}
		for(List<Trip> l: person2waysUnrouted.values()){
			for(Trip t : l){
				trips.put(Integer.valueOf(t.getElement(1)+1),t);
			}
		}
		
		return trips; 
	}
}

class Trip{
	
	private SortedMap<Integer, String> values;
	private int oldSize;
	
	public Trip(String[] oldTrip){
		this.oldSize = oldTrip.length;
		this.values = new TreeMap<Integer, String>();
		
		for(int i = 0; i < oldTrip.length; i++){
			this.values.put(i, oldTrip[i]);
		}
	}
	
	public Trip(String[] oldTrip, boolean toRoot){
		this.values = new TreeMap<Integer, String>();
		
		for(int i = 0; i < oldTrip.length; i++){
			this.values.put(i, oldTrip[i]);
		}
		this.setPtTime(9999999, 9999999, 9999999);
		this.setCarTime(9999999);
	}
	
	public void setPtTime(double travelTime, double transitTime, double waitingTime){
		this.values.put(this.oldSize, String.valueOf(travelTime));
		this.values.put(this.oldSize+1, String.valueOf(transitTime));
		this.values.put(this.oldSize+2, String.valueOf(waitingTime));
	}
	
	public void setPtTime(String travelTime, String transitTime, String waitingTime){
		this.values.put(this.oldSize, travelTime);
		this.values.put(this.oldSize+1, transitTime);
		this.values.put(this.oldSize+2, waitingTime);
	}
	
	public void setCarTime(double travelTime){
		this.values.put(this.oldSize+3, String.valueOf(travelTime));
	}
	public void setCarTime(String travelTime){
		this.values.put(this.oldSize+3, travelTime);
	}
	
	public String getElement(int index){
		return this.values.get(index);
	}
	
	public SortedMap<Integer, String> getAll(){
		return this.values;
	}
}
