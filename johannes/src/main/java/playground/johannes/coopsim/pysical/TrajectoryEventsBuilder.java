/* *********************************************************************** *
 * project: org.matsim.*
 * TrajectoryEventsBuilder.java
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
package playground.johannes.coopsim.pysical;

import gnu.trove.TObjectIntHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;

/**
 * @author illenberger
 *
 */
public class TrajectoryEventsBuilder implements AgentDepartureEventHandler, AgentArrivalEventHandler {

	private Map<Id, Trajectory> trajectories;
	
	private TObjectIntHashMap<Id> indices;
	
	private Map<Id, Person> persons;
	
	public TrajectoryEventsBuilder(Set<Plan> plans) {
		persons = new HashMap<Id, Person>(plans.size());
		for(Plan plan : plans) {
			persons.put(plan.getPerson().getId(), plan.getPerson());
		}
	}
	
	public Map<Id, Trajectory> getTrajectories() {
		return trajectories;
	}
	
	@Override
	public void reset(int iteration) {
		trajectories = new HashMap<Id, Trajectory>(persons.size());
		indices = new TObjectIntHashMap<Id>(persons.size());
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		addElement(event);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		addElement(event);
	}

	private void addElement(AgentEvent event) {
		Trajectory t = trajectories.get(event.getPersonId());
		int index = indices.get(event.getPersonId());
		Person person = persons.get(event.getPersonId());
		if(t == null) {
			t = new Trajectory(person);
			trajectories.put(event.getPersonId(), t);
			indices.put(event.getPersonId(), 0);
		}
		
		Plan plan = person.getSelectedPlan();
		
		t.addElement(plan.getPlanElements().get(index), event.getTime());
		if(index == plan.getPlanElements().size() - 2) {
			/*
			 * This is the last element.
			 */
			t.addElement(plan.getPlanElements().get(index + 1), Math.max(86400, event.getTime() + 1));
		}
		
		indices.increment(event.getPersonId());
	}
}
