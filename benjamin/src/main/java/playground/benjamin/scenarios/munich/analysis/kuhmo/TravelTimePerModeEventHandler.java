/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimePerModeEventHandler.java
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
package playground.benjamin.scenarios.munich.analysis.kuhmo;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;

import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author benjamin
 *
 */
public class TravelTimePerModeEventHandler implements AgentArrivalEventHandler, AgentDepartureEventHandler{
	private static final Logger logger = Logger.getLogger(TravelTimePerModeEventHandler.class);

	Map<String, Map<Id, Double>> mode2personId2DepartureTime;
	Map<String, Map<Id, Double>> mode2personId2TravelTime;
	Map<UserGroup, Map<String, Double>> userGroup2mode2noOfTrips;
	private final PersonFilter personFilter;

	public TravelTimePerModeEventHandler() {
		this.mode2personId2DepartureTime = new HashMap<String, Map<Id, Double>>();
		this.mode2personId2TravelTime = new HashMap<String, Map<Id, Double>>();
		this.userGroup2mode2noOfTrips = new HashMap<UserGroup, Map<String,Double>>();
		this.personFilter = new PersonFilter();
	}

	@Override
	public void reset(int iteration) {
		this.mode2personId2DepartureTime = new HashMap<String, Map<Id, Double>>();
		logger.info("Resetting mode2personId2DepartureTime to " + this.mode2personId2DepartureTime);
		this.mode2personId2TravelTime = new HashMap<String, Map<Id, Double>>();
		logger.info("Resetting mode2personId2TravelTime to " + this.mode2personId2TravelTime);
		this.userGroup2mode2noOfTrips = new HashMap<UserGroup, Map<String,Double>>();
		logger.info("Resetting userGroup2mode2noOfTrips to " + this.userGroup2mode2noOfTrips);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		Map<Id, Double> personId2DepartureTime;

		String legMode = event.getLegMode();
		Id personId = event.getPersonId();
		Double departureTime = event.getTime();

		if(this.mode2personId2DepartureTime.get(legMode) == null){
			personId2DepartureTime = new HashMap<Id, Double>();
			personId2DepartureTime.put(personId, departureTime);
		} else {
			personId2DepartureTime = this.mode2personId2DepartureTime.get(legMode);

			if(personId2DepartureTime.get(personId) == null){
				personId2DepartureTime.put(personId, departureTime);
			} else {
				throw new RuntimeException("Person " + personId + " is still in departure list. Aborting...");
			}
		}
		this.mode2personId2DepartureTime.put(legMode, personId2DepartureTime);

		for(UserGroup userGroup : UserGroup.values()){
			if(personFilter.isPersonIdFromUserGroup(personId, userGroup)){
				Map<String, Double> mode2noOfTrips;
				double modeTripsAfter;
				if(userGroup2mode2noOfTrips.get(userGroup) == null){
					mode2noOfTrips = new HashMap<String, Double>();
					modeTripsAfter = 1.0;
				} else {
					mode2noOfTrips = userGroup2mode2noOfTrips.get(userGroup);

					if(mode2noOfTrips.get(legMode) == null){
						modeTripsAfter = 1.0;
					} else {
						double modeTripsSoFar = mode2noOfTrips.get(legMode);
						modeTripsAfter = modeTripsSoFar + 1.0;
					}
					mode2noOfTrips.put(legMode, modeTripsAfter);
				}
				userGroup2mode2noOfTrips.put(userGroup, mode2noOfTrips);
			}
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Map<Id, Double> personId2DepartureTime;
		Map<Id, Double> personId2TravelTime;

		String legMode = event.getLegMode();
		Id personId = event.getPersonId();
		Double arrivalTime = event.getTime();

		if(this.mode2personId2DepartureTime.get(legMode) == null){
			throw new RuntimeException("Person " + personId + " is arriving with an unknown transport mode. Aborting...");
		} else {
			personId2DepartureTime = mode2personId2DepartureTime.get(legMode);

			if(personId2DepartureTime.get(personId) == null){
				throw new RuntimeException("Person " + personId + " is arriving with another transport mode than leaving at departure. Aborting...");
			} else {
				double departureTime = personId2DepartureTime.get(personId);
				double travelTimeOfEvent = arrivalTime - departureTime;

				if(this.mode2personId2TravelTime.get(legMode) == null){
					personId2TravelTime = new HashMap<Id, Double>();
				} else {
					personId2TravelTime = this.mode2personId2TravelTime.get(legMode);

					if(personId2TravelTime.get(personId) == null){
						personId2TravelTime.put(personId, travelTimeOfEvent);
					} else {
						double travelTimeSoFar = personId2TravelTime.get(personId);
						double sumOfTravelTime = travelTimeSoFar + travelTimeOfEvent;
						personId2TravelTime.put(personId, sumOfTravelTime);
					}
				}
			}
			this.mode2personId2TravelTime.put(legMode, personId2TravelTime);
			this.mode2personId2DepartureTime.get(legMode).remove(personId);
		}
	}

	protected Map<String, Map<Id, Double>> getMode2personId2TravelTime() {
		return this.mode2personId2TravelTime;
	}

	protected Map<UserGroup, Map<String, Double>> getUserGroup2mode2noOfTrips() {
		return userGroup2mode2noOfTrips;
	}
}
