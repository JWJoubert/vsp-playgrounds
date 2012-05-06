/* *********************************************************************** *
 * project: matsim
 * TransportModeFilter.java
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

package playground.christoph.ped2012;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEvent;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.collections.CollectionUtils;

/*
 * Create an events file that contains only arrival, departure and link events of
 * agents using a given transport mode.
 */
public class TransportModeFilter implements BasicEventHandler {
	
	private final Set<String> observedModes;
	private final Set<Id> observedAgents = new HashSet<Id>();

	private EventWriterXML eventWriter;
	
	public static void main(String[] args) {
//		String inputFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events.xml.gz";
		String inputFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events.xml.gz";
		String outputFile;
		
//		outputFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_non_walk2d.xml.gz";
		outputFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_non_walk2d.xml.gz";
		new TransportModeFilter("walk,bike,car,ride,pt").filterFile(inputFile, outputFile);
		
//		outputFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_walk.xml.gz";
//		new TransportModeFilter("walk").filterFile(inputFile, outputFile);
////		
//		outputFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_bike.xml.gz";
//		new TransportModeFilter("bike").filterFile(inputFile, outputFile);
		
//		outputFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_car.xml.gz";
//		new TransportModeFilter("car").filterFile(inputFile, outputFile);

//		outputFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_ride.xml.gz";
//		new TransportModeFilter("ride").filterFile(inputFile, outputFile);
//	
//		outputFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_pt.xml.gz";
//		new TransportModeFilter("pt").filterFile(inputFile, outputFile);
	}
			
	public TransportModeFilter(String observedModes) {
		this(CollectionUtils.stringToSet(observedModes));
	}
	
	public TransportModeFilter(Set<String> observedModes) {
		this.observedModes = observedModes;
	}
	
	public void filterFile(String inputEventsFile, String outputEventsFile) {		
		
		eventWriter = new EventWriterXML(outputEventsFile);
		EventsManager eventsManager = EventsUtils.createEventsManager();
		MatsimEventsReader eventReader = new MatsimEventsReader(eventsManager);
		
		eventsManager.addHandler(this);
		eventReader.readFile(inputEventsFile);
		eventWriter.closeFile();
	}
	
	@Override
	public void reset(int iteration) {
		observedAgents.clear();
		eventWriter.reset(iteration);
	}

	@Override
	public void handleEvent(Event event) {
		// check whether the agent's mode is observed
		if (event instanceof AgentDepartureEvent) {
			AgentDepartureEvent agentDepartureEvent = (AgentDepartureEvent) event;
			if (observedModes.contains(agentDepartureEvent.getLegMode())) observedAgents.add(agentDepartureEvent.getPersonId());
			else return;
		}
		
		// remove agent from observation set
		else if (event instanceof AgentArrivalEvent) {
			AgentArrivalEvent agentArrivalEvent = (AgentArrivalEvent) event;
			if (observedModes.contains(agentArrivalEvent.getLegMode())) observedAgents.remove(agentArrivalEvent.getPersonId());
			else return;
		}
		
		// skip link events from not observed agents
		else if (event instanceof LinkEvent) {
			LinkEvent linkEvent = (LinkEvent) event;
			if (!observedAgents.contains(linkEvent.getPersonId())) return;
		}
		
		// skip agent wait 2 link events from not observed agents
		else if (event instanceof AgentWait2LinkEvent) {
			AgentWait2LinkEvent agentWait2LinkEvent = (AgentWait2LinkEvent) event;
			if (!observedAgents.contains(agentWait2LinkEvent.getPersonId())) return;
		}
		// skip all other events
		else return;
		
		eventWriter.handleEvent(event);
	}
}