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
package playground.andreas.aas.modules.ptTripAnalysis.traveltime.V4;

import java.util.LinkedList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;

import playground.andreas.aas.modules.ptTripAnalysis.AbstractAnalysisTrip;
import playground.andreas.aas.modules.ptTripAnalysis.traveltime.AbstractTTAnalysisTrip;
import playground.andreas.aas.modules.ptTripAnalysis.traveltime.AbstractTTtripEventsHandler;

/**
 * @author droeder
 *
 */
public class TTtripEventsHandlerV4 extends AbstractTTtripEventsHandler implements 
										PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
	
	
	
	
	@Override
	public void handleEvent(ActivityStartEvent e){
		// do nothing
	}
	
	@Override
	public void handleEvent(ActivityEndEvent e){
		//do nothing
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent e) {
		//drivers have no plans
		if(super.id2Trips.containsKey(e.getPersonId())){
			if(((TTAnalysisTripV4) super.id2Trips.get(e.getPersonId()).getFirst()).handleEvent(e)){
				this.addTrip2TripSet(e.getPersonId());
			}
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent e) {
		if(super.id2Trips.containsKey(e.getPersonId())){
			if(((TTAnalysisTripV4) super.id2Trips.get(e.getPersonId()).getFirst()).handleEvent(e)){
				this.addTrip2TripSet(e.getPersonId());
			}
		}
	}
	
	private void addTrip2TripSet(Id id){
		// store number of processed Trips
		this.nrOfprocessedTrips++;
		
		// store this for getUncompletedPlans()
		this.nrOfTrips.get(id)[1]++;
		
		//get and remove the first Trip of this agent
		AbstractTTAnalysisTrip trip = (AbstractTTAnalysisTrip) this.id2Trips.get(id).removeFirst();
		
		//add for all zones
		for(String s : this.zone2tripSet.keySet()){
			this.zone2tripSet.get(s).addTrip(trip);
		}
	}
	
	@Override
	public void addTrips(Map<Id, LinkedList<AbstractAnalysisTrip>> map) {
		super.addTrips(map);
		this.id2Events = null;
	}

	@Override
	protected void processEvent(Event e) {
		// TODO Auto-generated method stub
		
	}
}


