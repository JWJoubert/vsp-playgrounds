/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package herbie.creation.ptAnalysis;

import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.TransitRouteImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import utils.Bins;

public class PtScenarioAdaption {
	
	public static final double[] relHeadwayClasses = new double[]{
	    4, 7, 10, 15, 30, 60};  // in [minutes] and ascending order!!
	
	private final static Logger log = Logger.getLogger(PtScenarioAdaption.class);
	private String networkfilePath;
	private String outpath;
	private String transitScheduleFile;
	private String transitVehicleFile;
	private ScenarioImpl scenario;
	private TransitScheduleFactory transitFactory = null;

	private TreeMap<Double, Departure> departuresTimes;
	private TreeMap<Double, Departure> newDepartures;
	private double currentInterval;
	private Double implDeparture;
	
	TreeMap<Id, Vehicle> newVehiclesMap;
	int vehicleNumber;
	private Vehicle currentVehicle;
	
	private Bins old_hdwy_distrib;
	private Bins new_hdwy_distrib;
	private static double[] relevantInterval = new double[]{8.0, 9.0}; // relevant interval for headway in hours!
	
	public static void main(String[] args) {
		if (args.length != 1) {
			log.info("Specify config path."); 
			return;
		}
		
		PtScenarioAdaption ptScenarioAdaption = new PtScenarioAdaption();
		ptScenarioAdaption.initConfig(args[0]);
		ptScenarioAdaption.initScenario();
		ptScenarioAdaption.doubleHeadway();
		ptScenarioAdaption.writeScenario();
		ptScenarioAdaption.evaluateSchedule();
	}

	private void initConfig(String file) {
		log.info("InitConfig ...");
		
		Config config = new Config();
    	MatsimConfigReader matsimConfigReader = new MatsimConfigReader(config);
    	matsimConfigReader.readFile(file);
    	
		this.networkfilePath = config.findParam("ptScenarioAdaption", "networkfilePath");
		this.outpath = config.findParam("ptScenarioAdaption", "output");
		this.transitScheduleFile = config.findParam("ptScenarioAdaption", "transitScheduleFile");
		this.transitVehicleFile = config.findParam("ptScenarioAdaption", "transitVehicleFile");
		
		this.old_hdwy_distrib = new Bins(1, 60d, "Old Headway Distribution");
		this.new_hdwy_distrib = new Bins(1, 60d, "New Headway Distribution");
		
		log.info("InitConfig ... done");
	}

	private void initScenario() {
		log.info("Initialization ...");
		
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseVehicles(true);
		scenario.getConfig().scenario().setUseTransit(true);
		NetworkImpl network = scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		new MatsimNetworkReader(scenario).parse(networkfilePath);
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		this.transitFactory = schedule.getFactory();
		new TransitScheduleReaderV1(schedule, network, scenario).readFile(transitScheduleFile);
		
		Vehicles vehicles = scenario.getVehicles();
		new VehicleReaderV1(vehicles).readFile(transitVehicleFile);
		
		log.info("Initialization ... done");
	}
	
	/**
	 * increase headway according to the values in headwayClasses!
	 */
	private void doubleHeadway() {
		log.info("DoubleHeadway ...");
		
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		
		newVehiclesMap = new TreeMap<Id, Vehicle>();
		vehicleNumber = 0;
		
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				
				Map<Id, Departure> departures = route.getDepartures();
				
				if(departures == null) continue;
				
				newDepartures = new TreeMap<Double, Departure>();
				
				departuresTimes = new TreeMap<Double, Departure>();
				for(Departure departure : departures.values()){
					departuresTimes.put(departure.getDepartureTime(), departure);
				}
				
				implDeparture = departuresTimes.firstKey();
				Id vehicleId = departuresTimes.get(implDeparture).getVehicleId();
				currentVehicle = scenario.getVehicles().getVehicles().get(vehicleId);
				
				copyFirstDeparture();
				
				if(departures.size() >= 2) {
					
					considerHeadwayAdaption();
				}
				
				this.removeDepartures((TransitRouteImpl) route);
				this.copyNewDepartures(route);
				
			}
		}
		this.removeVehicles();
		this.copyNewVehicles();
		
		log.info("DoubleHeadway ... done");
	}
	
	private void considerHeadwayAdaption() {
		
		currentInterval = (departuresTimes.higherKey(implDeparture) - implDeparture) / 60d;
		
		for(Double depTime : departuresTimes.keySet()){
			
			Id vehicleId = departuresTimes.get(depTime).getVehicleId();
			currentVehicle = scenario.getVehicles().getVehicles().get(vehicleId);
			
			if(departuresTimes.lastKey() != depTime &&
					(departuresTimes.higherKey(depTime) - depTime) / 60d == currentInterval){
				continue;
			}
			
			if(departuresTimes.lastKey() == depTime) {
				addNewDepartures(depTime + currentInterval * 60 - getNewInterval() * 60);
				
				old_hdwy_distrib.addVal(currentInterval, 1d);
			}
			else {
				
				addNewDepartures((depTime));
				currentInterval = (departuresTimes.higherKey(depTime) - depTime) / 60d;
				
				old_hdwy_distrib.addVal(currentInterval, 1d);
			}
		}
	}

	private void copyFirstDeparture() {
		
		setNewDeparture();
	}

	private void removeVehicles() {
		
		scenario.getVehicles().getVehicles().clear();
	}
	
	private void copyNewVehicles() {
		
		for(Id id : newVehiclesMap.keySet()){
			
			scenario.getVehicles().getVehicles().put(id, newVehiclesMap.get(id));
		}
	}

	private void removeDepartures(TransitRouteImpl route) {
		
		Stack<Id> stackId = new Stack<Id>();
		
		for(Id id : route.getDepartures().keySet()){
			stackId.push(id);
		}
		
		while(stackId.size() > 0){
			route.removeDeparture(route.getDepartures().get(stackId.pop()));
		}
	}

	private void copyNewDepartures(TransitRoute route) {
		for(Departure departure : newDepartures.values()){
			
			route.addDeparture(departure);
		}
	}
	
	private void addNewDepartures(Double upperThreshold) {
		
		if(currentInterval > relHeadwayClasses[0] && currentInterval <= relHeadwayClasses[relHeadwayClasses.length - 1]) 
		{
			double newInterval = getNewInterval();

			while(implDeparture  < upperThreshold){
				
				implDeparture = implDeparture + newInterval * 60d;
				
				setNewDeparture();
			}
			new_hdwy_distrib.addVal(newInterval, 1d);
		}
		else
		{	
			copyExistingDepartures(upperThreshold);
			new_hdwy_distrib.addVal(currentInterval, 1d);
		}
	}

	private void copyExistingDepartures(double upperThreshold) {
		
		while(implDeparture  < upperThreshold){
			
			implDeparture = implDeparture + currentInterval * 60d;
			
			setNewDeparture();
		}
	}

	private void setNewDeparture() {
		
		IdImpl newId = new IdImpl(vehicleNumber);
		
		Departure newDepImpl = this.transitFactory.createDeparture(newId, implDeparture);
		
		Id newVehicleId = new IdImpl("tr_" + vehicleNumber);
		
		newDepImpl.setVehicleId(newVehicleId);
		newDepartures.put(implDeparture, newDepImpl);
		
		currentVehicle = new VehicleImpl(newVehicleId, currentVehicle.getType());
		
		
		newVehiclesMap.put(newVehicleId, currentVehicle);
		
		vehicleNumber++;
	}

	private boolean courseIsInConsideration(double departureTime) {
		
		if(departureTime > relevantInterval[0] * 3600d 
				&& departureTime < relevantInterval[1] * 3600d) return true;
		return false;
	}

	private double getNewInterval() {
		
		if(relHeadwayClasses[0] > currentInterval) return currentInterval;
		
		for (int i = 1; i < relHeadwayClasses.length; i++) {
			if(relHeadwayClasses[i] >= currentInterval) return relHeadwayClasses[(i-1)];
		}
		return currentInterval;
	}

	private void writeScenario() {
		log.info("Writing new network file ...");
		
		new TransitScheduleWriter(this.scenario.getTransitSchedule()).writeFile(this.outpath + "newTransitSchedule.xml.gz");
		
		new VehicleWriterV1(this.scenario.getVehicles()).writeFile(this.outpath + "newTransitVehicles.xml.gz");
		
		log.info("Writing new network file ... done");
	}
	
	private void evaluateSchedule() {
		old_hdwy_distrib.plotBinnedDistribution(this.outpath + "HeadwayDistribution", "Headway", "[#]");
		new_hdwy_distrib.plotBinnedDistribution(this.outpath + "HeadwayDistribution", "Headway", "[#]");
	}
}
