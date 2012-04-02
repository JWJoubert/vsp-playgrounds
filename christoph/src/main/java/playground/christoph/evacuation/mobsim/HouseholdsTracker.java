/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdsTracker.java
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

package playground.christoph.evacuation.mobsim;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.christoph.evacuation.mobsim.Tracker.Position;

public class HouseholdsTracker extends AgentsTracker implements 
		MobsimBeforeSimStepListener, MobsimAfterSimStepListener {

	static final Logger log = Logger.getLogger(HouseholdsTracker.class);
	
	/* time since last "info" */
	private int infoTime = 0;
	private static final int INFO_PERIOD = 3600;
	
	private final ObjectAttributes householdObjectAttributes;
	private final Set<Id> householdsToUpdate;
	private final Map<Id, Id> personHouseholdMap;
	private final Map<Id, HouseholdPosition> householdPositions;
	
	public HouseholdsTracker(ObjectAttributes householdObjectAttributes) {
		this.householdObjectAttributes = householdObjectAttributes;
		
		this.householdsToUpdate = new HashSet<Id>();
		this.personHouseholdMap = new HashMap<Id, Id>();
		this.householdPositions = new HashMap<Id, HouseholdPosition>();
	}
	
	public Id getPersonsHouseholdId(Id personId) {
		return this.personHouseholdMap.get(personId);
	}
	
	public HouseholdPosition getPersonsHouseholdPosition(Id personId) {
		return householdPositions.get(personHouseholdMap.get(personId));
	}
	
	public HouseholdPosition getHouseholdPosition(Id householdId) {
		return householdPositions.get(householdId);
	}
	
	public Map<Id, HouseholdPosition> getHouseholdPositions() {
		return this.householdPositions;
	}
	
	public Set<Id> getHouseholdsToUpdate() {
		return this.householdsToUpdate;
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		super.handleEvent(event);
		householdsToUpdate.add(this.personHouseholdMap.get(event.getPersonId()));
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		super.handleEvent(event);
		householdsToUpdate.add(this.personHouseholdMap.get(event.getPersonId()));
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		super.handleEvent(event);
		householdsToUpdate.add(this.personHouseholdMap.get(event.getPersonId()));
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		super.handleEvent(event);
		householdsToUpdate.add(this.personHouseholdMap.get(event.getPersonId()));
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		super.handleEvent(event);
		householdsToUpdate.add(this.personHouseholdMap.get(event.getPersonId()));
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		super.handleEvent(event);
		householdsToUpdate.add(this.personHouseholdMap.get(event.getPersonId()));
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		super.handleEvent(event);
		householdsToUpdate.add(this.personHouseholdMap.get(event.getPersonId()));
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		super.handleEvent(event);
		householdsToUpdate.add(this.personHouseholdMap.get(event.getPersonId()));
	}

	@Override
	public void reset(int iteration) {
		super.reset(iteration);
		this.householdsToUpdate.clear();
		this.personHouseholdMap.clear();
		this.householdPositions.clear();
	}
	
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		super.notifyMobsimInitialized(e);
		
		QSim sim = (QSim) e.getQueueSimulation();
		Households households = ((ScenarioImpl) sim.getScenario()).getHouseholds();
		for (Household household : households.getHouseholds().values()) {
			
			Id homeFacilityId = sim.getScenario().createId(this.householdObjectAttributes.getAttribute(household.getId().toString(), "homeFacilityId").toString());
			
			HouseholdPosition householdPosition = new HouseholdPosition();			
			householdPosition.setHomeFacilityId(homeFacilityId);
			householdPosition.setMeetingPointFacilityId(homeFacilityId);
			for (Id personId : household.getMemberIds()) {
				personHouseholdMap.put(personId, household.getId());
				
				AgentPosition agentPosition = this.getAgentPosition(personId);
				householdPosition.addAgentPosition(agentPosition);
			}
			householdPosition.update();
			
			// only observe household if its member size is > 0
			if (household.getMemberIds().size() > 0) householdPositions.put(household.getId(), householdPosition);
		}
	}
	
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		// clear list for the upcoming time-step
		householdsToUpdate.clear();
	}
	
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		for (Id id : this.householdsToUpdate) {
			householdPositions.get(id).update();
		}
		
		if (e.getSimulationTime() >= this.infoTime) {
			this.infoTime += INFO_PERIOD;
			this.printStatistics();
		}
	}
	
	public void printStatistics() {
		int numHouseholds = householdPositions.size();
		
		int split = 0;
		int joined = 0;
		int joinedOnLink = 0;
		int joinedInVehicles = 0;
		int joinedInFacility = 0;
		int joinedUndefined = 0;
		for (HouseholdPosition householdPosition : householdPositions.values()) {
			if (householdPosition.isHouseholdJoined()) {
				joined++;
				if (householdPosition.getPositionType() == Position.LINK) {
					joinedOnLink++;
				} else if (householdPosition.getPositionType() == Position.VEHICLE) {
					joinedInVehicles++;
				} else if (householdPosition.getPositionType() == Position.FACILITY) {
					joinedInFacility++;
				} else joinedUndefined++;
			} else split++;			
		}
		
		DecimalFormat df = new DecimalFormat("#.##");
		log.info("Households Statistics: # total Households=" + numHouseholds
			+ ", # total joined Households=" + joined + "(" + df.format((100.0*joined)/numHouseholds) + "%)"
			+ ", # on Link joined Households=" + joinedOnLink + "(" + df.format((100.0*joinedOnLink)/numHouseholds) + "%)"
			+ ", # in Vehicle joined Households=" + joinedInVehicles + "(" + df.format((100.0*joinedInVehicles)/numHouseholds) + "%)"
			+ ", # in Facility joined Households=" + joinedInFacility + "(" + df.format((100.0*joinedInFacility)/numHouseholds) + "%)"
			+ ", # undefined joined Households=" + joinedUndefined + "(" + df.format((100.0*joinedUndefined)/numHouseholds) + "%)"
			+ ", # split Households=" + split + "(" + df.format((100.0*split)/numHouseholds) + "%)");
	}

}