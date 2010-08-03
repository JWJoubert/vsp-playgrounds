/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityReplanningMap
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
package playground.christoph.withinday.replanning.identifiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.QSimI;
import org.matsim.ptproject.qsim.netsimengine.QNetwork;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;

import playground.christoph.withinday.mobsim.WithinDayQSim;

/*
 * This Module is used by a NextLegReplanner. It calculates the time
 * when an agent should do NextLegReplanning.
 * 
 * When an ActivityStartEvent is thrown the Replanning Time is set to
 * the scheduled departure Time of the Activity.
 */
public class ActivityReplanningMap implements AgentStuckEventHandler, 
		ActivityStartEventHandler, ActivityEndEventHandler,
		SimulationInitializedListener<QSimI>, SimulationAfterSimStepListener<QSimI> {
		
	private QNetwork qNetwork;
	
	private static final Logger log = Logger.getLogger(ActivityReplanningMap.class);

	/*
	 * Agents that have started an Activity in the current Time Step.
	 */
	private Set<Id> startingAgents;	// PersonId
	
	/*
	 * Contains the Agents that are currently performing an Activity.
	 */
	private Set<Id> replanningSet;	// PersonDriverAgentId

	/*
	 * Mapping between the PersonDriverAgents and the PersonIds.
	 * The events only contain a PersonId.
	 */
	private Map<Id, PersonAgent> personAgentMapping;	// PersonId, PersonDriverAgent
	
	public ActivityReplanningMap(Controler controler) {	
		// add ActivityReplanningMap to the QueueSimulation's EventsManager
		controler.getEvents().addHandler(this);
		
		// add ActivityReplanningMap to the QueueSimulation's SimulationListeners
		controler.getQueueSimulationListener().add(this);
		
		init();
	}
	
	public ActivityReplanningMap(QSim qSim) {
		//Add LinkReplanningMap to the QueueSimulation's EventsManager
		qSim.getEventsManager().addHandler(this);
		
		// add ActivityReplanningMap to the QueueSimulation's SimulationListeners
		qSim.addQueueSimulationListeners(this);
		
		init();
	}
	
	private void init() {
		this.personAgentMapping = new TreeMap<Id, PersonAgent>();
		this.replanningSet = new TreeSet<Id>();
		this.startingAgents = new TreeSet<Id>();
	}
	
	/*
	 * When the simulation starts the agents are all performing an activity.
	 * There is no activity start event so we have to collect by hand by
	 * iterating over all links and vehicles.
	 * 
	 * Additionally we create the Mapping between the PersonIds and the
	 * PersonDriverAgents.
	 */
	public void notifySimulationInitialized(SimulationInitializedEvent<QSimI> e) {			

		QSimI sim = e.getQueueSimulation();
		
		// Update Reference to QNetwork
		this.qNetwork = (QNetwork) sim.getQNetwork();
		
		personAgentMapping = null;
		
		if (sim instanceof WithinDayQSim) {
			personAgentMapping = ((WithinDayQSim) sim).getPersonAgents();
			
			replanningSet.addAll(personAgentMapping.keySet());
		}
		else new HashMap<Id, PersonAgent>();
	}
	
	/*
	 * The Activity Start Events are thrown before the Activity Start has been handled
	 * by the Simulation. As a result the Departure Time is not set at that time.
	 * We have to wait until the SimStep has been fully simulated and retrieve
	 * the Activity DepatureTimes then. 
	 */
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent<QSimI> e) {
		
		for (Id id : startingAgents) {
			PersonAgent personAgent = personAgentMapping.get(id);
			
			double now = e.getSimulationTime();
			double departureTime = personAgent.getDepartureTime();
					
			/*
			 * If it is the last scheduled Activity the departureTime is -infinity.
			 * Otherwise we select the agent for a replanning.
			 */
			if (departureTime >= now) {
				replanningSet.add(id);
			}
			else {
				log.warn("Departure time is in the past - ignoring activity!");
			}
		}
		startingAgents.clear();
	}
	
	/*
	 * Collect the Agents that are starting an Activity in
	 * the current Time Step.
	 */
	public void handleEvent(ActivityStartEvent event) {
		Id id = event.getPersonId();
		this.startingAgents.add(id);
	}
	
	/*
	 * Nothing to do here?
	 * Agents should be removed from the map if their
	 * replanning time has come.
	 */
	public void handleEvent(ActivityEndEvent event) {	
		startingAgents.remove(event.getPersonId());
		replanningSet.remove(event.getPersonId());
	}

	/*
	 * Nothing to do here?
	 * Agents should not be removed from the simulation
	 * if they are performing an activity.
	 */
	public void handleEvent(AgentStuckEvent event) {
//		replanningMap.remove(personAgentMapping.get(event.getPersonId()));
	}
	
	/*
	 * Returns a List of all Agents, that are currently performing an Activity.
	 */
	public synchronized List<PersonAgent> getActivityPerformingAgents() {		
		ArrayList<PersonAgent> activityPerformingAgents = new ArrayList<PersonAgent>();
		
		for (Id id : replanningSet) activityPerformingAgents.add(personAgentMapping.get(id));
		
		return activityPerformingAgents;
	}
	
	public synchronized List<PersonAgent> getReplanningDriverAgents(double time) {
		ArrayList<PersonAgent> personAgentsToReplanActivityEnd = new ArrayList<PersonAgent>();
				
		Iterator<Id> ids = replanningSet.iterator();
		while(ids.hasNext()) {
			Id id = ids.next();
			
			PersonAgent personAgent = personAgentMapping.get(id);
          	
			double replanningTime = personAgent.getDepartureTime();
	       
			if (time >= replanningTime) {
				ids.remove();
				personAgentsToReplanActivityEnd.add(personAgent);
			}
//			else break;
		}
		
		return personAgentsToReplanActivityEnd;
	}

	public void reset(int iteration) {
		replanningSet.clear();
		personAgentMapping.clear();
	}
	
}
