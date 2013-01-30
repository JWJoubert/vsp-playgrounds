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
package playground.ikaddoura.optimization;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
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


/**
 * @author Ihab
 *
 */
public class ScheduleGenerator {

	private final static Logger log = Logger.getLogger(ScheduleGenerator.class);

	private double stopTime;
	private Network network;
	private Id transitLineId;
	private Id routeId1;
	private Id routeId2;
	private double startTime;
	private double endTime;
	private double pausenzeit;
	private double scheduleSpeed;
	
	private double umlaufzeit;

	private List<Id> vehicleIDs = new ArrayList<Id>();
	private Map<Id, TransitRoute> routeId2transitRoute;
	
	private TransitScheduleFactory sf = new TransitScheduleFactoryImpl();
	private final TransitSchedule schedule = sf.createTransitSchedule();
	
	public void createSchedule(int numberOfBuses) throws IOException {
		createLineRoutesStops();
		createVehicleIDs(numberOfBuses);
		setDepartureIDs(routeId2transitRoute, numberOfBuses);
	}
	
	// ************************************************************************************
	
	public void createLineRoutesStops() {
		Map<Id,List<Id>> routeID2linkIDs = getIDs();
		Map<Id, List<TransitStopFacility>> routeId2transitStopFacilities = getStopLinkIDs(routeID2linkIDs);
		Map<Id, NetworkRoute> routeId2networkRoute = getRouteId2NetworkRoute(routeID2linkIDs);
		Map<Id, List<TransitRouteStop>> routeId2TransitRouteStops = getRouteId2TransitRouteStops(routeId2transitStopFacilities);
		this.routeId2transitRoute = getRouteId2TransitRoute(routeId2networkRoute, routeId2TransitRouteStops);
		setTransitLine(routeId2transitRoute);
		
		int lastStop = routeId2transitRoute.get(routeId1).getStops().size()-1;
		double routeTravelTime = routeId2transitRoute.get(routeId1).getStops().get(lastStop).getArrivalOffset();
		this.umlaufzeit = (routeTravelTime + this.pausenzeit) * 2.0;
		log.info("RouteTravelTime: "+ Time.writeTime(routeTravelTime, Time.TIMEFORMAT_HHMMSS));
		log.info("Umlaufzeit: "+ Time.writeTime(umlaufzeit, Time.TIMEFORMAT_HHMMSS));			
	}
	
	public void setDepartureIDs(Map<Id, TransitRoute> routeId2transitRoute, int numberOfBuses) {	
		
		double headway = umlaufzeit / numberOfBuses;
		log.info("Takt: "+ Time.writeTime(headway, Time.TIMEFORMAT_HHMMSS));
			
		int routeNr = 0;
		for (Id routeId : routeId2transitRoute.keySet()){
			double firstDepartureTime = 0.0;
			if (routeNr == 1){
				firstDepartureTime = this.startTime;
				log.info(routeId.toString() + ": Route 0 --> First Departure Time: "+ Time.writeTime(firstDepartureTime, Time.TIMEFORMAT_HHMMSS));
			}
			else if (routeNr == 0){
				firstDepartureTime = this.startTime + umlaufzeit/2;
				log.info(routeId.toString() + ": Route 1 --> First Departure Time: "+ Time.writeTime(firstDepartureTime, Time.TIMEFORMAT_HHMMSS));
	
			}
			int vehicleIndex = 0;
			int depNr = 0;
			for (double departureTime = firstDepartureTime; departureTime < this.endTime ; ){
				Departure departure = sf.createDeparture(new IdImpl(depNr), departureTime);
				departure.setVehicleId(vehicleIDs.get(vehicleIndex));
				routeId2transitRoute.get(routeId).addDeparture(departure);
				departureTime = departureTime + headway;
				depNr++;
				if (vehicleIndex == numberOfBuses - 1){
					vehicleIndex = 0;
				}
				else {
					vehicleIndex++;
				}
			}				
			routeNr++;
		}			
	}
			
	public void createVehicleIDs(int numberOfBuses){
		for (int vehicleNr=1 ; vehicleNr <= numberOfBuses ; vehicleNr++){
			vehicleIDs.add(new IdImpl("bus_"+vehicleNr));
		}
	}
	
	public void writeScheduleFile(String scheduleFile) {
		TransitScheduleWriterV1 scheduleWriter = new TransitScheduleWriterV1(schedule);
		scheduleWriter.write(scheduleFile);
	}
	
	// ************************************************************************************
		
	private Map<Id,List<Id>> getIDs() {
		List <Link> busLinks = new ArrayList<Link>();
		Map<Id, List<Id>> routeID2linkIDs = new HashMap<Id, List<Id>>();
		List<Id> linkIDsRoute1 = new LinkedList<Id>();
		List<Id> linkIDsRoute2 = new LinkedList<Id>();
		
		for (Link link : this.network.getLinks().values()){
			if (link.getAllowedModes().contains("bus") && !link.getAllowedModes().contains("car")){
				busLinks.add(link);
			}
		}
		
		if (busLinks.isEmpty()) throw new RuntimeException("No bus links found. Link IDs have to contain [bus] in order to create the schedule. Aborting...");
		
		// one direction
		int fromNodeIdRoute1 = 0;
		int toNodeIdRoute1 = 0;
		for (int ii = 0; ii <= busLinks.size(); ii++){
			fromNodeIdRoute1 = ii;
			toNodeIdRoute1 = ii + 1;
			for (Link link : busLinks){
				if (Integer.parseInt(link.getFromNode().getId().toString()) == fromNodeIdRoute1 && Integer.parseInt(link.getToNode().getId().toString()) == toNodeIdRoute1){			
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
		for (int ii = 0; ii <= busLinks.size(); ii++){
			fromNodeIdRoute2 = ii;
			toNodeIdRoute2 = ii - 1;
			for (Link link : busLinks){
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

	private Map<Id,List<TransitStopFacility>> getStopLinkIDs(Map<Id, List<Id>> routeID2linkIDs) {
		Map<Id, List<TransitStopFacility>> routeId2transitStopFacilities = new HashMap<Id, List<TransitStopFacility>>();
			
		for (Id routeID : routeID2linkIDs.keySet()){
			List<TransitStopFacility> stopFacilitiesRoute = new ArrayList<TransitStopFacility>();

			for (Id linkID : routeID2linkIDs.get(routeID)){				
				if (schedule.getFacilities().containsKey(linkID)){
					TransitStopFacility transitStopFacility = schedule.getFacilities().get(linkID);
					stopFacilitiesRoute.add(transitStopFacility);
				}
				else {
					TransitStopFacility transitStopFacility = sf.createTransitStopFacility(linkID, this.network.getLinks().get(linkID).getToNode().getCoord(), false);
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

	private Map<Id, List<TransitRouteStop>> getRouteId2TransitRouteStops(Map<Id, List<TransitStopFacility>> routeId2transitStopFacilities) {

		Map<Id, List<TransitRouteStop>> routeId2transitRouteStops = new HashMap<Id, List<TransitRouteStop>>();
		
		for (Id routeId : routeId2transitStopFacilities.keySet()){
			double arrivalTime = 0;
			double departureTime = arrivalTime + this.stopTime;
			List<TransitRouteStop> transitRouteStops = new ArrayList<TransitRouteStop>();
			List<TransitStopFacility> transitStopFacilities = routeId2transitStopFacilities.get(routeId);

			int ii = 0;
			double travelTimeBus = 0;
			for (TransitStopFacility transitStopFacility : transitStopFacilities){
				
				TransitRouteStop transitRouteStop = sf.createTransitRouteStop(transitStopFacility, arrivalTime, departureTime);
				transitRouteStop.setAwaitDepartureTime(true);
				transitRouteStops.add(transitRouteStop);
				
				if (ii==transitStopFacilities.size()-1){
				} else {
					travelTimeBus = this.network.getLinks().get(transitStopFacilities.get(ii).getId()).getLength() / this.scheduleSpeed;
//					travelTimeBus = this.network.getLinks().get(transitStopFacilities.get(ii).getId()).getLength() / this.network.getLinks().get(transitStopFacilities.get(ii).getId()).getFreespeed();
				}
				
				arrivalTime = departureTime + travelTimeBus;
				departureTime = arrivalTime + this.stopTime;	
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
	
	private List<Id> turnArround(List<Id> myList) {
		List<Id> turnedArroundList = new ArrayList<Id>();
		for (int n = (myList.size() - 1); n >= 0; n = n - 1){
			turnedArroundList.add(myList.get(n));
		}
		return turnedArroundList;
	}
	
	private List<Id> getMiddleRouteLinkIDs(List<Id> linkIDsRoute) {
		List<Id> routeLinkIDs = new ArrayList<Id>();
		int nr = 0;
		for(Id id : linkIDsRoute){
			if (nr >= 1 & nr <= (linkIDsRoute.size() - 2)){ // links between startLink and endLink
				routeLinkIDs.add(id);
			}
			nr++;
		}
		return routeLinkIDs;
	}
	
	// ************************************************************************************

	public void setStopTime(double stopTime) {
		this.stopTime = stopTime;
	}

	public void setNetwork(Network network) {
		this.network = network;
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

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	public void setScheduleSpeed(double scheduleSpeed) {
		this.scheduleSpeed = scheduleSpeed;
	}

	public void setPausenzeit(double pausenzeit) {
		this.pausenzeit = pausenzeit;
	}

	public double getUmlaufzeit() {
		return umlaufzeit;
	}
}