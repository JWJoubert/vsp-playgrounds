/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.vsp.analysis.modules.stuckAgents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.EventHandler;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * @author droeder
 *
 */
public class GetStuckEvents extends AbstractAnalyisModule implements AgentStuckEventHandler{
	
	private Collection<AgentStuckEvent> stuckEvents;

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(GetStuckEvents.class);

	public GetStuckEvents() {
		super(GetStuckEvents.class.getSimpleName());
		this.stuckEvents = new ArrayList<AgentStuckEvent>();
	}

	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		this.stuckEvents.add(event);
	}

	/**
	 * @return
	 */
	public Collection<AgentStuckEvent> getEvents() {
		return this.stuckEvents;
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new ArrayList<EventHandler>();
		handler.add(this);
		return handler;
	}

	@Override
	public void preProcessData() {
		// do nothing
	}

	@Override
	public void postProcessData() {
		// do nothing
	}

	@Override
	public void writeResults(String outputFolder) {
		String file = outputFolder + "stuckEvents.xml";
		log.info("writing " + this.stuckEvents.size() + " stuckEvents to " + file + ".");
		EventWriterXML eventWriter = new EventWriterXML(file);
		for(Event e: this.stuckEvents){
			eventWriter.handleEvent(e);
		}
		eventWriter.closeFile();
		log.info("finished...");
	}
}

