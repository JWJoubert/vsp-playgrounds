/* *********************************************************************** *
 * project: matsim
 * CreateAgentGroups.java
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.io.IOUtils;

/*
 * Separates traveling agents into groups based on their transport mode.
 * Their id's are adapted and written to a new events file.
 */
public class CreateAgentGroups implements BasicEventHandler {

	private static final String newLine = "\n";
	private static final String separator = "\t";

	private final Set<String> observedModes;	
	private final Map<Id, String> observedAgents = new HashMap<Id, String>();	// personId, transportMode
	private final Map<String, Set<Id>> groups = new HashMap<String, Set<Id>>();

	private EventWriterXML eventWriter;
	private EventsManager eventsManager;
	
	public static void main(String[] args) {
//		String inputEventsFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events.xml.gz";
//		String outputEventsFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_grouped.xml.gz";
//		String ouptputPath = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/";
		
		String inputEventsFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events.xml.gz";
		String outputEventsFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_grouped.xml.gz";
		String ouptputPath = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/";

		
		new CreateAgentGroups("walk,bike,car,ride,pt").createAgentGroups(inputEventsFile, outputEventsFile, ouptputPath);
	}
	
	public CreateAgentGroups(String observedModes) {
		this(CollectionUtils.stringToSet(observedModes));
	}
	
	public CreateAgentGroups(Set<String> observedModes) {
		this.observedModes = observedModes;
	}
	
	public void createAgentGroups(String inputEventsFile, String outputEventsFile, String ouptputPath) {
		
		for (String mode : observedModes) this.groups.put(mode, new LinkedHashSet<Id>());
		
		eventWriter = new EventWriterXML(outputEventsFile);
		
		eventsManager = EventsUtils.createEventsManager();
		MatsimEventsReader eventReader = new MatsimEventsReader(eventsManager);
		
		eventsManager.addHandler(this);
		eventReader.readFile(inputEventsFile);
		eventWriter.closeFile();
		
		writeGroups(ouptputPath);
	}

	@Override
	public void handleEvent(Event event) {
		
		if (event instanceof AgentDepartureEvent) {
			AgentDepartureEvent agentDepartureEvent = (AgentDepartureEvent) event;
			Id personId = agentDepartureEvent.getPersonId();
			String mode = agentDepartureEvent.getLegMode();
			
			if (observedModes.contains(mode)) {
				observedAgents.put(personId, mode);
				
				// clone event but change agent's id
				Id agentGroupId = new IdImpl(personId.toString() + "_" + mode);
				event = this.eventsManager.getFactory().createAgentDepartureEvent(event.getTime(), agentGroupId, 
						agentDepartureEvent.getLinkId(), mode);
				
				// register agent in its group
				this.groups.get(agentDepartureEvent.getLegMode()).add(agentDepartureEvent.getPersonId());
			} else return;
		}
		
		else if (event instanceof LinkEnterEvent) {
			LinkEnterEvent linkEnterEvent = (LinkEnterEvent) event;
			Id personId = linkEnterEvent.getPersonId();
			String mode = observedAgents.get(personId);
			
			if (mode != null) {
				// clone event but change agent's id
				Id agentGroupId = new IdImpl(personId.toString() + "_" + mode);
				event = this.eventsManager.getFactory().createLinkEnterEvent(event.getTime(), agentGroupId, linkEnterEvent.getLinkId(), 
						linkEnterEvent.getVehicleId());
			} else return;
		}
		
		// create xy data for link trips of observed agents
		else if (event instanceof LinkLeaveEvent) {
			LinkLeaveEvent linkLeaveEvent = (LinkLeaveEvent) event;
			Id personId = linkLeaveEvent.getPersonId();
			String mode = observedAgents.get(personId);
			
			if (mode != null) {
				// clone event but change agent's id
				Id agentGroupId = new IdImpl(personId.toString() + "_" + mode);
				event = this.eventsManager.getFactory().createLinkLeaveEvent(event.getTime(), agentGroupId, linkLeaveEvent.getLinkId(), 
						linkLeaveEvent.getVehicleId());
			} else return;
		}
		
		// create xy data for link trips of observed agents
		else if (event instanceof AgentArrivalEvent) {
			AgentArrivalEvent agentArrivalEvent = (AgentArrivalEvent) event;
			Id personId = agentArrivalEvent.getPersonId();
			String mode = observedAgents.remove(personId);
			
			if (mode != null) {
				// clone event but change agent's id
				Id agentGroupId = new IdImpl(personId.toString() + "_" + mode);
				event = this.eventsManager.getFactory().createAgentArrivalEvent(event.getTime(), agentGroupId, 
						agentArrivalEvent.getLinkId(), mode);
			} else return;
		}
		
		// otherwise ignore the event
		else return;
		
		this.eventWriter.handleEvent(event);
	}
	
	private void writeGroups(String ouptputPath) {
		try {
			for (String mode : this.observedModes) {
				Set<Id> agents = this.groups.get(mode);
				BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(ouptputPath + mode + "_grouped.txt.gz");
				
				for (Id agentId : agents) {
					bufferedWriter.write(agentId.toString() + "_" + mode);
					bufferedWriter.write(newLine);
				}			
				
				bufferedWriter.flush();
				bufferedWriter.close();
			}
			
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	@Override
	public void reset(int iteration) {
		groups.clear();
		observedAgents.clear();
	}
}