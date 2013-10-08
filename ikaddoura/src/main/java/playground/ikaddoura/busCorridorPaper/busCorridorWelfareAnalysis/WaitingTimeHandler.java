/* *********************************************************************** *
 * project: org.matsim.*
 * InVehWaitHandler.java
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

/**
 * 
 */
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author Ihab
 *
 */
public class WaitingTimeHandler implements PersonEntersVehicleEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler, VehicleArrivesAtFacilityEventHandler {
	private final List <Double> waitingTimes = new ArrayList<Double>();
	private final List <Double> waitingTimesMissed = new ArrayList<Double>();
	private final List <Double> waitingTimesNotMissed = new ArrayList<Double>();
	private Map <Id, List<Double>> personId2waitingTimes = new HashMap<Id, List<Double>>();

	private final Map <Id, Double> personId2PersonEntersVehicleTime = new HashMap<Id, Double>();
	private final Map <Id, Double> personId2AgentDepartureTime = new HashMap<Id, Double>();
	private final Map <Id, Double> personId2InVehicleTime = new HashMap<Id, Double>();
	private final Map <Id, Id> busId2currentFacilityId = new HashMap<Id, Id>();
	private final Map <Id, FacilityWaitTimeInfo> facilityId2facilityInfos = new HashMap<Id, FacilityWaitTimeInfo>();

	private int numberOfMissedVehicles;
		
	private final double headway;
	private int waitingTimeCounter = 0;
	
	public WaitingTimeHandler(double headway) {
		this.headway = headway;
	}

	@Override
	public void reset(int iteration) {
		waitingTimes.clear();
		waitingTimesMissed.clear();
		waitingTimesNotMissed.clear();
		personId2waitingTimes.clear();
		personId2PersonEntersVehicleTime.clear();
		personId2AgentDepartureTime.clear();
		personId2InVehicleTime.clear();
		busId2currentFacilityId.clear();
		facilityId2facilityInfos.clear();
		this.waitingTimeCounter = 0;
		this.numberOfMissedVehicles = 0;
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id personId = event.getPersonId();
		Id vehId = event.getVehicleId();
		
		if (personId.toString().contains("person") && vehId.toString().contains("bus")){
			personId2PersonEntersVehicleTime.put(personId, event.getTime());
			
			double waitingTime;
			if (personId2AgentDepartureTime.get(personId) == null){
				throw new RuntimeException("Person " + personId + " is entering vehicle " + vehId + " without having departed from an activity. Aborting...");
			} else {
				waitingTime =  event.getTime() - personId2AgentDepartureTime.get(personId);
			}
			
			waitingTimes.add(waitingTime);
			
			// save waitingTimes per person
			if (personId2waitingTimes.get(personId) == null){
				List<Double> waitingTimes = new ArrayList<Double>();
				waitingTimes.add(waitingTime);
				personId2waitingTimes.put(personId, waitingTimes);
			} else {
				List<Double> waitingTimes = personId2waitingTimes.get(personId);
				waitingTimes.add(waitingTime);
				personId2waitingTimes.put(personId, waitingTimes);
			}
						
//			System.out.println("Headway --------------> " + this.headway);
//			System.out.println("WaitingTime ----------> " + waitingTime);
				
			// analyze waitingTime and save waitingTime per person differentiated for agents who missed and who didn't miss a bus

			if (waitingTime > this.headway){
				
				waitingTimesMissed.add(waitingTime);
							
				int missed = (int) (waitingTime / this.headway);
//				System.out.println("Missed Busses: " + missed);
				this.numberOfMissedVehicles = this.numberOfMissedVehicles + missed;
			}
			
			if (waitingTime <= this.headway){
			
				waitingTimesNotMissed.add(waitingTime);
			}
			
			// save waitingTime per stop
			
			Id currentFacilityId = this.busId2currentFacilityId.get(vehId);

			if (this.facilityId2facilityInfos.get(currentFacilityId) == null){
				FacilityWaitTimeInfo facilityInfo = new FacilityWaitTimeInfo();
				SortedMap<Id, Double> waitingEvent2WaitingTime = new TreeMap<Id, Double>();
				SortedMap<Id, Double> waitingEvent2DayTime = new TreeMap<Id, Double>(); // daytime when person enters vehicle
				SortedMap<Id, Id> waitingEvent2PersonId = new TreeMap<Id, Id>();

				facilityInfo.setFacilityId(currentFacilityId);
				waitingEvent2WaitingTime.put(new IdImpl(waitingTimeCounter), waitingTime);
				facilityInfo.setWaitingEvent2WaitingTime(waitingEvent2WaitingTime);
				waitingEvent2DayTime.put(new IdImpl(waitingTimeCounter), event.getTime());
				facilityInfo.setWaitingEvent2DayTime(waitingEvent2DayTime);
				waitingEvent2PersonId.put(new IdImpl(waitingTimeCounter), event.getPersonId());
				facilityInfo.setWaitingEvent2PersonId(waitingEvent2PersonId);
								
				if (waitingTime > this.headway){
					facilityInfo.setNumberOfWaitingTimesMoreThanHeadway(1);
					int missed = (int) (waitingTime / this.headway);
					facilityInfo.setNumberOfMissedVehicles(missed);
				}
				
				this.facilityId2facilityInfos.put(currentFacilityId, facilityInfo);
				
			} else {
				FacilityWaitTimeInfo facilityInfo = this.facilityId2facilityInfos.get(currentFacilityId);
				SortedMap<Id, Double> waitingEvent2WaitingTime = facilityInfo.getWaitingEvent2WaitingTime();
				SortedMap<Id, Double> waitingEvent2DayTime = facilityInfo.getWaitingEvent2DayTime();
				SortedMap<Id, Id> waitingEvent2PersonId = facilityInfo.getWaitingEvent2PersonId();
				
				waitingEvent2WaitingTime.put(new IdImpl(waitingTimeCounter), waitingTime);
				waitingEvent2DayTime.put(new IdImpl(waitingTimeCounter), event.getTime());
				waitingEvent2PersonId.put(new IdImpl(waitingTimeCounter), event.getPersonId());

				facilityInfo.setWaitingEvent2WaitingTime(waitingEvent2WaitingTime);
				facilityInfo.setWaitingEvent2DayTime(waitingEvent2DayTime);
				facilityInfo.setWaitingEvent2PersonId(waitingEvent2PersonId);
				
				if (waitingTime > this.headway){
					facilityInfo.setNumberOfWaitingTimesMoreThanHeadway(facilityInfo.getNumberOfWaitingTimesMoreThanHeadway() + 1);
					int missed = (int) (waitingTime / this.headway);
					facilityInfo.setNumberOfMissedVehicles(facilityInfo.getNumberOfMissedVehicles() + missed);
				}
				
				this.facilityId2facilityInfos.put(currentFacilityId, facilityInfo);
			}
		
			waitingTimeCounter++;
			
		} else {
			// no person enters a bus
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id personId = event.getPersonId();
		
		if (event.getLegMode().toString().equals("pt")){
			personId2AgentDepartureTime.put(personId, event.getTime());
		} else {
			// not a pt Leg
		}
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Id personId = event.getPersonId();
		
		if (event.getLegMode().toString().equals("pt")){

			double inVehicleTime = 0.0;
			if (personId2PersonEntersVehicleTime.get(personId) == null){
				throw new RuntimeException("Person " + personId + " is arriving without having departed from an activity. Aborting...");
			} else {
				inVehicleTime = event.getTime() - personId2PersonEntersVehicleTime.get(personId);
			}
			
			if (personId2InVehicleTime.get(personId) == null) {
				personId2InVehicleTime.put(personId, inVehicleTime);
			} else {
				double inVehicleTimeSum = personId2InVehicleTime.get(personId) + inVehicleTime;
				personId2InVehicleTime.put(personId, inVehicleTimeSum);
			}			
			
		} else {
			// not a pt Leg
		}
	}

	public List <Double> getWaitingTimes() {
		return waitingTimes;
	}

	public Map<Id, Double> getPersonId2InVehicleTime() {
		return personId2InVehicleTime;
	}
	
	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		Id busId = event.getVehicleId();
		Id facilityId = event.getFacilityId();
//		System.out.println("Bus " + busId + " arrives at " + facilityId + ".");
		this.busId2currentFacilityId.put(busId, facilityId);
	}
	
	public int getNumberOfMissedVehicles() {
		return numberOfMissedVehicles;
	}

	public Map<Id, FacilityWaitTimeInfo> getFacilityId2facilityInfos() {
		return facilityId2facilityInfos;
	}

	public List <Double> getWaitingTimesMissed() {
		return waitingTimesMissed;
	}

	public List <Double> getWaitingTimesNotMissed() {
		return waitingTimesNotMissed;
	}

	public Map <Id, List<Double>> getPersonId2waitingTimes() {
		return personId2waitingTimes;
	}
	
}
