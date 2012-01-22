/* *********************************************************************** *
 * project: org.matsim.*
 * BusCorridorScheduleVehiclesGenerator.java
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

/**
 * 
 */
package playground.ikaddoura.busCorridor.finalDyn2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

/**
 * @author Ihab
 *
 */
public class ScheduleVehiclesGenerator {
	
	private final static Logger log = Logger.getLogger(ScheduleVehiclesGenerator.class);
			
	private double stopTime;
	private String networkFile;
	private String scheduleFile;
	private String vehicleFile;
	private Id transitLineId;
	private Id routeId1;
	private Id routeId2;
	private Id vehTypeId;
	private Map<Integer, TimePeriod> day;
	private Map<Integer, TimePeriod> newDay = new HashMap<Integer, TimePeriod>();

	private int busSeats;
	private int standingRoom;
	private double length;
	private double egressSeconds;
	private double accessSeconds;
	private double scheduleSpeed;
	private double pausenzeit;
	
	private double umlaufzeit;

	private Map<Integer, Double> vehNr2lastPeriodDepTimeHin = new HashMap<Integer, Double>();
	private int vehicleIndexLastDepartureHin = 0;
	private double lastPeriodDepTimeHin = 0;
	private int vehicleIndexRueck = 0;
	private int periodNr = 0;

	TransitScheduleFactory sf = new TransitScheduleFactoryImpl();
	private TransitSchedule schedule = sf.createTransitSchedule();
	

	Vehicles veh = VehicleUtils.createVehiclesContainer();

	public void createSchedule() throws IOException {
		
		Scenario scen = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());	
		Config config = scen.getConfig();
		config.network().setInputFile(this.networkFile);
		ScenarioUtils.loadScenario(scen);		
		Network network = scen.getNetwork();
			
		scen.getConfig().scenario().setUseTransit(true);
		scen.getConfig().scenario().setUseVehicles(true);
		
		Map<Id,List<Id>> routeID2linkIDs = getIDs(network);
		
		Map<Id, List<TransitStopFacility>> routeId2transitStopFacilities = getStopLinkIDs(routeID2linkIDs, network);
			
		Map<Id, NetworkRoute> routeId2networkRoute = getRouteId2NetworkRoute(routeID2linkIDs);
		Map<Id, List<TransitRouteStop>> routeId2TransitRouteStops = getRouteId2TransitRouteStops(routeId2transitStopFacilities, network, this.stopTime);
			
		Map<Id, TransitRoute> routeId2transitRoute = getRouteId2TransitRoute(routeId2networkRoute, routeId2TransitRouteStops);
		setTransitLine(routeId2transitRoute);
		
		setDepartures(routeId2transitRoute);
	}

	private Map<Id,List<Id>> getIDs(Network network) {
		Map<Id, List<Id>> routeID2linkIDs = new HashMap<Id, List<Id>>();
		List<Id> linkIDsRoute1 = new LinkedList<Id>();
		List<Id> linkIDsRoute2 = new LinkedList<Id>();
		
		// one direction
		int fromNodeIdRoute1 = 0;
		int toNodeIdRoute1 = 0;
		for (int ii = 0; ii<=network.getLinks().size(); ii++){
			fromNodeIdRoute1 = ii;
			toNodeIdRoute1 = ii+1;
			for (Link link : network.getLinks().values()){
				if (Integer.parseInt(link.getFromNode().getId().toString())==fromNodeIdRoute1 && Integer.parseInt(link.getToNode().getId().toString())==toNodeIdRoute1){			
					linkIDsRoute1.add(link.getId());
				}
				else {
					// nothing
				}
			}
		}
		// other direction
		int fromNodeIdRoute2 = 0;
		int toNodeIdRoute2 = 0;
		for (int ii = 0; ii<=network.getLinks().size(); ii++){
			fromNodeIdRoute2 = ii;
			toNodeIdRoute2 = ii-1;
			for (Link link : network.getLinks().values()){
				if (Integer.parseInt(link.getFromNode().getId().toString())==fromNodeIdRoute2 && Integer.parseInt(link.getToNode().getId().toString())==toNodeIdRoute2){			
					linkIDsRoute2.add(link.getId());
				}
				else {
					// nothing
				}
			}
		}

		List<Id> linkIDsRoute2rightOrder = turnArround(linkIDsRoute2);

		linkIDsRoute1.add(0, linkIDsRoute2rightOrder.get(linkIDsRoute2rightOrder.size()-1));
		linkIDsRoute2rightOrder.add(0, linkIDsRoute1.get(linkIDsRoute1.size()-1));
		routeID2linkIDs.put(routeId1, linkIDsRoute1);
		routeID2linkIDs.put(routeId2, linkIDsRoute2rightOrder);
		return routeID2linkIDs;
	}

	private Map<Id,List<TransitStopFacility>> getStopLinkIDs(Map<Id, List<Id>> routeID2linkIDs, Network network) {
		Map<Id, List<TransitStopFacility>> routeId2transitStopFacilities = new HashMap<Id, List<TransitStopFacility>>();
			
		for (Id routeID : routeID2linkIDs.keySet()){
			List<TransitStopFacility> stopFacilitiesRoute = new ArrayList<TransitStopFacility>();

			for (Id linkID : routeID2linkIDs.get(routeID)){				
				if (schedule.getFacilities().containsKey(linkID)){
					TransitStopFacility transitStopFacility = schedule.getFacilities().get(linkID);
					stopFacilitiesRoute.add(transitStopFacility);
					// don't create transitStopFacility!
				}
				else {
					TransitStopFacility transitStopFacility = sf.createTransitStopFacility(linkID, network.getLinks().get(linkID).getToNode().getCoord(), false);
					transitStopFacility.setLinkId(linkID);
					stopFacilitiesRoute.add(transitStopFacility);
					schedule.addStopFacility(transitStopFacility);
				}
			}	
			routeId2transitStopFacilities.put(routeID, stopFacilitiesRoute);
		}
		return routeId2transitStopFacilities;
	}
	
	private Map<Id, NetworkRoute> getRouteId2NetworkRoute(Map<Id, List<Id>> routeID2linkIDs) {
		Map<Id, NetworkRoute> routeId2NetworkRoute = new HashMap<Id, NetworkRoute>();
		for (Id routeId : routeID2linkIDs.keySet()){
			NetworkRoute netRoute = new LinkNetworkRouteImpl(routeID2linkIDs.get(routeId).get(0), routeID2linkIDs.get(routeId).get(routeID2linkIDs.get(routeId).size()-1));	// Start-Link, End-Link	
			netRoute.setLinkIds(routeID2linkIDs.get(routeId).get(0), getMiddleRouteLinkIDs(routeID2linkIDs.get(routeId)), routeID2linkIDs.get(routeId).get(routeID2linkIDs.get(routeId).size()-1)); // Start-link, link-Ids als List, End-link
			routeId2NetworkRoute.put(routeId, netRoute);
		}
		return routeId2NetworkRoute;
	}

	private Map<Id, List<TransitRouteStop>> getRouteId2TransitRouteStops(Map<Id, List<TransitStopFacility>> routeId2transitStopFacilities, Network network, double stopTime) {

		Map<Id, List<TransitRouteStop>> routeId2transitRouteStops = new HashMap<Id, List<TransitRouteStop>>();
		
		for (Id routeId : routeId2transitStopFacilities.keySet()){
			double arrivalTime = 0;
			double departureTime = 0;
			List<TransitRouteStop> transitRouteStops = new ArrayList<TransitRouteStop>();
			List<TransitStopFacility> transitStopFacilities = routeId2transitStopFacilities.get(routeId);

			int ii = 0;
			
			double travelTimeBus = 0;
			
			for (TransitStopFacility transitStopFacility : transitStopFacilities){
				
				TransitRouteStop transitRouteStop = sf.createTransitRouteStop(transitStopFacility, arrivalTime, departureTime);
				transitRouteStop.setAwaitDepartureTime(true);
				transitRouteStops.add(transitRouteStop);
				
				if (ii==transitStopFacilities.size()-1){
				}
					
				else {
					if (this.getScheduleSpeed()==0){
						travelTimeBus = network.getLinks().get(transitStopFacilities.get(ii).getId()).getLength() / network.getLinks().get(transitStopFacilities.get(ii).getId()).getFreespeed(); // (from link freespeed) v = s/t --> t = s/v
					}
					else {
						travelTimeBus = network.getLinks().get(transitStopFacilities.get(ii).getId()).getLength() / this.getScheduleSpeed(); // v = s/t --> t = s/v
					}
				}
				arrivalTime = departureTime + travelTimeBus;
				departureTime = arrivalTime + stopTime;	
				
				ii++;
			}
		routeId2transitRouteStops.put(routeId, transitRouteStops);
		}
		return routeId2transitRouteStops;
	}

	
	private Map<Id, TransitRoute> getRouteId2TransitRoute(Map<Id, NetworkRoute> routeId2networkRoute, Map<Id, List<TransitRouteStop>> routeId2TransitRouteStops) {
		
		Map<Id, TransitRoute> routeId2transitRoute = new HashMap<Id, TransitRoute>();			
		for (Id routeId : routeId2networkRoute.keySet()){
			TransitRoute transitRoute = sf.createTransitRoute(routeId, routeId2networkRoute.get(routeId), routeId2TransitRouteStops.get(routeId), "bus");
			routeId2transitRoute.put(routeId, transitRoute);
		}
		return routeId2transitRoute;
	}
	
	private void setTransitLine(Map<Id, TransitRoute> routeId2transitRoute) {
		TransitLine transitLine = sf.createTransitLine(this.transitLineId);
		schedule.addTransitLine(transitLine);
		transitLine.addRoute(routeId2transitRoute.get(this.routeId1));
		transitLine.addRoute(routeId2transitRoute.get(this.routeId2));
	}
	
	private void setDepartures(Map<Id, TransitRoute> routeId2transitRoute) {
		
		int lastStop = routeId2transitRoute.get(routeId1).getStops().size()-1;
		double routeTravelTime = routeId2transitRoute.get(routeId1).getStops().get(lastStop).getArrivalOffset() + this.stopTime;
		this.umlaufzeit = (routeTravelTime + pausenzeit) * 2;
		log.info("Umlaufzeit: "+Time.writeTime(umlaufzeit, Time.TIMEFORMAT_HHMMSS));
		
		// die zeiträume die kürzer sind als eine umlaufzeit entfernen
		List<Integer> removePeriods = new ArrayList<Integer>();
		for (Integer pNr : this.day.keySet()){
			double fromTime = day.get(pNr).getFromTime();
			double toTime = day.get(pNr).getToTime();
			if(fromTime >= toTime || (toTime-fromTime) < umlaufzeit){
				removePeriods.add(pNr);
			}
		}
		for (Integer pNr : removePeriods){
			this.day.remove(pNr);
		}
		//-------------------------------------------------------------

		// die PeriodNr in eine Reihenfolge bringen
		int nr = 1;
		for (TimePeriod t : this.day.values()){
			t.setOrderId(nr);
			this.newDay.put(nr, t);
			nr++;
		}
		//-------------------------------------------------------------

		for (TimePeriod t : this.day.values()){
			t.setScheduleTakt(umlaufzeit / t.getNumberOfBuses());
		}
		
		for (TimePeriod period : newDay.values()){
			setDepartureIDs(routeId2transitRoute, "period_"+period.getOrderId(), period.getToTime(), period.getFromTime(), period.getNumberOfBuses(), createVehicles(period.getNumberOfBuses()));
		}		
	}
	
	private void setDepartureIDs(Map<Id, TransitRoute> routeId2transitRoute, String zeitraum, double endTime, double startTime, int numberOfBusesZeitraum, List<Id> vehicleIDsZeitraum) {	
		this.periodNr++;
		System.out.println("+++++ Beginn von period "+ periodNr+": "+this.vehNr2lastPeriodDepTimeHin);

		if (numberOfBusesZeitraum == 0){
			log.info("No buses in time period "+periodNr+".");
		}
		
		if (numberOfBusesZeitraum > 0){
			log.info("Number of buses in time period "+periodNr+": "+numberOfBusesZeitraum);
			
			//-------------------------------------------------------------
			double takt = umlaufzeit / numberOfBusesZeitraum; //sec
			log.info("Takt für diesen Zeitraum: "+Time.writeTime(takt, Time.TIMEFORMAT_HHMMSS));
			//-------------------------------------------------------------

			double departureTime = 0;
			double firstDepartureTime = 0;
			double lastDepartureTime = 0;
			int routeNrCounter = 0;
			int vehicleIndex = 0;
			for (Id routeId : routeId2transitRoute.keySet()){
				routeNrCounter++;
				if (this.periodNr > 1 && routeNrCounter == 1){  // nicht der erste Zeitraum, hin
					
					lastDepartureTime = endTime;
				
					if (newDay.get(periodNr-1).getNumberOfBuses()==0){
						firstDepartureTime = startTime;
						vehicleIndex = vehicleIDsZeitraum.size()-1;
					}
					if (newDay.get(periodNr-1).getNumberOfBuses() >= 0){
						if (newDay.get(periodNr-1).getNumberOfBuses() > numberOfBusesZeitraum){
							// weniger Busse als in der Periode davor
							
							if (this.vehicleIndexLastDepartureHin == 0){
								vehicleIndex = vehicleIDsZeitraum.size()-1;
							}
							else {
								System.out.println("vehIDsZeitraum: "+vehicleIDsZeitraum);
								System.out.println("size: "+vehicleIDsZeitraum.size());
								vehicleIndex = this.vehicleIndexLastDepartureHin-1;
								
								if (vehicleIDsZeitraum.size()-1 < vehicleIndex){
									for (int i = 0; vehicleIDsZeitraum.size()-1 < vehicleIndex; i++){
										vehicleIndex = vehicleIndex - 1;
									}
								}
							}
							
							System.out.println(vehicleIDsZeitraum.toString());
							Map<Integer,Double> vehNr2minimalFirstDeparture = new HashMap<Integer, Double>();
							
							int vehNr = 0;
							for (Id vehID : vehicleIDsZeitraum){
								double depNotBefore = this.vehNr2lastPeriodDepTimeHin.get(vehNr) + umlaufzeit;
								System.out.println("vehNr: "+vehNr+" / lastPeriodDepTime: " + Time.writeTime(this.vehNr2lastPeriodDepTimeHin.get(vehNr), Time.TIMEFORMAT_HHMMSS) + " / no departure before: "+Time.writeTime(depNotBefore, Time.TIMEFORMAT_HHMMSS));
								vehNr2minimalFirstDeparture.put(vehNr, depNotBefore);
								vehNr++;
							}
							this.vehicleIndexRueck = vehicleIndex;
							
							
							double maximalAufschlag = 0;
							double firstDepartureTimeTmp = this.vehNr2lastPeriodDepTimeHin.get(vehicleIndex) + umlaufzeit;
							int vehNummer = vehicleIndex;
							for (int zz = 0; zz < vehicleIDsZeitraum.size(); zz++){
								System.out.println("*** vehNummer: "+vehNummer+" --> " +vehicleIDsZeitraum.get(vehNummer));
								System.out.println("firstDepartureTimeTmp: "+Time.writeTime(firstDepartureTimeTmp, Time.TIMEFORMAT_HHMMSS)); 
								System.out.println("first departure not before: "+Time.writeTime(vehNr2minimalFirstDeparture.get(vehNummer), Time.TIMEFORMAT_HHMMSS)); 
								double aufschlag = 0;
								if (firstDepartureTimeTmp < vehNr2minimalFirstDeparture.get(vehNummer)){
									aufschlag = vehNr2minimalFirstDeparture.get(vehNummer) - firstDepartureTimeTmp;
									System.out.println("Aufschlag = "+aufschlag);
								}
								if (aufschlag > maximalAufschlag){
									maximalAufschlag = aufschlag;
								}
								firstDepartureTimeTmp = firstDepartureTimeTmp + takt;
								
								if (vehNummer == 0){
									vehNummer = vehicleIDsZeitraum.size()-1;
								}
								else {
									vehNummer--;
								}
							}
							System.out.println("maximalAufschlag: "+maximalAufschlag);
							firstDepartureTime = this.vehNr2lastPeriodDepTimeHin.get(vehicleIndex) + umlaufzeit + maximalAufschlag;
						}

						else if (newDay.get(periodNr-1).getNumberOfBuses() <= numberOfBusesZeitraum){
							// mehr Busse als in der Periode davor oder gleichviele Busse
							vehicleIndex = vehicleIDsZeitraum.size()-1;
							if (this.vehNr2lastPeriodDepTimeHin.isEmpty()){
								firstDepartureTime = startTime;
								log.warn("No departure in last period! --> first Departure Time set to startTime!");
							}
							else {
								firstDepartureTime = this.lastPeriodDepTimeHin + takt;
							}
							this.vehicleIndexRueck = vehicleIndex;
						}
					}
				}
				else if (this.periodNr > 1 && routeNrCounter == 2) { // nicht der erste Zeitraum, rück
					lastDepartureTime = endTime + umlaufzeit/2;
					firstDepartureTime = firstDepartureTime + umlaufzeit/2;
					vehicleIndex = this.vehicleIndexRueck;
				}
				else if (this.periodNr == 1 && routeNrCounter == 1) { // erster Zeitraum, hin
					vehicleIndex = vehicleIDsZeitraum.size()-1;
					this.vehicleIndexRueck = vehicleIndex;
					firstDepartureTime = startTime;
					lastDepartureTime = endTime;
				}
				else if (this.periodNr == 1 && routeNrCounter == 2) { // erster Zeitraum, rück
					vehicleIndex = this.vehicleIndexRueck;
					firstDepartureTime = startTime + umlaufzeit/2;
					lastDepartureTime = endTime + umlaufzeit/2;
				}
				else {
					System.out.println("ERROR!");
				}
	
				departureTime = firstDepartureTime;
					for (int depNr=1 ; departureTime <= lastDepartureTime - umlaufzeit ; depNr++){
						Departure departure = sf.createDeparture(new IdImpl(zeitraum+"_"+depNr), departureTime);
						departure.setVehicleId(vehicleIDsZeitraum.get(vehicleIndex));
						routeId2transitRoute.get(routeId).addDeparture(departure);
						
						if (routeNrCounter == 1){
							if (depNr==1){
								this.vehNr2lastPeriodDepTimeHin.clear();
							}
							this.vehNr2lastPeriodDepTimeHin.put(vehicleIndex, departureTime);
							this.vehicleIndexLastDepartureHin = vehicleIndex;
							this.lastPeriodDepTimeHin = departureTime;
						}
						
						departureTime = departureTime+takt;
						
						if (vehicleIndex==0){
							vehicleIndex = vehicleIDsZeitraum.size()-1;
						}
						else {
							vehicleIndex--;
						}	
					}
			}
		}
	}
	
	private List<Id> turnArround(List<Id> myList) {
		List<Id> turnedArroundList = new ArrayList<Id>();
		for (int n=(myList.size()-1); n>=0; n=n-1){
			turnedArroundList.add(myList.get(n));
		}
		return turnedArroundList;
	}

	public List<Id> createVehicles(int numberOfBusesZeitraum) {
		List<Id> vehicleIdList = new ArrayList<Id>();
		// Vehicle-Typ: Bus
		VehicleType type = veh.getFactory().createVehicleType(this.vehTypeId);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(this.busSeats);
		cap.setStandingRoom(this.standingRoom);
		type.setCapacity(cap);
		type.setLength(length);
		type.setAccessTime(accessSeconds);
		type.setEgressTime(egressSeconds);
		veh.getVehicleTypes().put(this.vehTypeId, type); 
		
		for (int vehicleNr=1 ; vehicleNr<=numberOfBusesZeitraum ; vehicleNr++){
			vehicleIdList.add(new IdImpl("bus_"+vehicleNr));
		}

		for (Id vehicleId : vehicleIdList){
			Vehicle vehicle = veh.getFactory().createVehicle(vehicleId, veh.getVehicleTypes().get(vehTypeId));
			veh.getVehicles().put(vehicleId, vehicle);
		}
		
		return vehicleIdList;

	}
	
	public void writeScheduleFile() {
		TransitScheduleWriterV1 scheduleWriter = new TransitScheduleWriterV1(schedule);
		scheduleWriter.write(scheduleFile);
	}
	
	public void writeVehicleFile() {
		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(veh);
		vehicleWriter.writeFile(vehicleFile);
	}

	private List<Id> getMiddleRouteLinkIDs(List<Id> linkIDsRoute) {
		List<Id> routeLinkIDs = new ArrayList<Id>();
		int nr = 0;
		for(Id id : linkIDsRoute){
			if (nr>=1 & nr <= (linkIDsRoute.size()-2)){ // die Links zwischen dem Start- und Endlink
				routeLinkIDs.add(id);
			}
			nr++;
		}
		return routeLinkIDs;
	}

	public void setStopTime(double stopTime) {
		this.stopTime = stopTime;
	}

	public void setNetworkFile(String networkFile) {
		this.networkFile = networkFile;
	}

	public void setVehicleFile(String vehicleFile) {
		this.vehicleFile = vehicleFile;
	}

	public void setScheduleFile(String scheduleFile) {
		this.scheduleFile = scheduleFile;
	}
	
	public void setSeats(int seats) {
		this.busSeats = seats;
	}

	public void setTransitLineId(Id transitLineId) {
		this.transitLineId = transitLineId;
	}

	public void setRouteId1(Id routeId1) {
		this.routeId1 = routeId1;
	}

	public void setRouteId2(Id routeId2) {
		this.routeId2 = routeId2;
	}

	public void setVehTypeId(Id vehTypeId) {
		this.vehTypeId = vehTypeId;
	}

	public void setStandingRoom(int standingRoom) {
		this.standingRoom = standingRoom;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public void setEgressSeconds(double egressSeconds) {
		this.egressSeconds = egressSeconds;
	}

	public void setAccessSeconds(double accessSeconds) {
		this.accessSeconds = accessSeconds;
	}

	public void setScheduleSpeed(double scheduleSpeed) {
		this.scheduleSpeed = scheduleSpeed;
	}

	public double getScheduleSpeed() {
		return scheduleSpeed;
	}
	
	public Map<Integer, TimePeriod> getDay() {
		return day;
	}

	public void setDay(Map<Integer, TimePeriod> day) {
		this.day = day;
	}

	public Map<Integer, TimePeriod> getNewDay() {
		return newDay;
	}

	public void setNewDay(Map<Integer, TimePeriod> newDay) {
		this.newDay = newDay;
	}

	public void setVehNr2lastPeriodDepTimeHin(
			Map<Integer, Double> vehNr2lastPeriodDepTimeHin) {
		this.vehNr2lastPeriodDepTimeHin = vehNr2lastPeriodDepTimeHin;
	}

	public Map<Integer, Double> getVehNr2lastPeriodDepTimeHin() {
		return vehNr2lastPeriodDepTimeHin;
	}

	/**
	 * @return the pausenzeit
	 */
	public double getPausenzeit() {
		return pausenzeit;
	}

	/**
	 * @param pausenzeit the pausenzeit to set
	 */
	public void setPausenzeit(double pausenzeit) {
		this.pausenzeit = pausenzeit;
	}
	
	
	
}
