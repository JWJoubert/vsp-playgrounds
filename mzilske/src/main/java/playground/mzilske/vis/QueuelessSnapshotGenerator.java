/* *********************************************************************** *
 * project: org.matsim.*
 * SnapshotGenerator.java
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

package playground.mzilske.vis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshotwriters.SnapshotWriter;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo.AgentState;

/**
 * 
 * Consumes a stream of mobility simulation events and produces a stream of simulation snapshots. A simulation snapshot is a set of colored dots in the plane
 * representing agents, where they are, and what state they are in. This SnapshotGenerator only considers drivers of moving vehicles, and it colours them 
 * by their speed normalized to the maximum speed of the link on which they are driving. It generates agent positions by linear interpolation between 
 * the LinkEnterEvent and the LinkLeaveEvent.
 * 
 * Previously, this SnapshotGenerator would think up agent positions by re-simulating what happens in the queue model between these events. 
 * We replaced that with this simpler version because we think that one should not make complex assumptions about what happens on a link when 
 * only looking at an event file. Implementations of the mobility simulation, of course, still decide on their own where exactly the agents are at any 
 * given time.
 * 
 * @author michaz
 *
 */
public class QueuelessSnapshotGenerator implements AgentDepartureEventHandler, AgentArrivalEventHandler, LinkEnterEventHandler,
		LinkLeaveEventHandler, AgentWait2LinkEventHandler, AgentStuckEventHandler {

	private final Network network;
	private final int snapshotPeriod;
	private final HashMap<Id, EventAgent> eventAgents;
	private final List<SnapshotWriter> snapshotWriters = new ArrayList<SnapshotWriter>();

	public QueuelessSnapshotGenerator(final Network network, final int snapshotPeriod) {
		this.network = network;
		this.eventAgents = new HashMap<Id, EventAgent>();
		this.snapshotPeriod = snapshotPeriod;
		reset(-1);
	}

	public void addSnapshotWriter(final SnapshotWriter writer) {
		this.snapshotWriters.add(writer);
	}

	public boolean removeSnapshotWriter(final SnapshotWriter writer) {
		return this.snapshotWriters.remove(writer);
	}

	public void handleEvent(final AgentDepartureEvent event) {
		agentDeparture(event.getPersonId(), event.getLinkId(), event.getTime());
	}

	private void agentDeparture(Id personId, Id linkId, double time) {
		EventAgent agent = new EventAgent();
		eventAgents.put(personId, agent);
	}

	public void handleEvent(final AgentArrivalEvent event) {
		linkLeave(event.getPersonId(), event.getLinkId(), (int) event.getTime());
		eventAgents.remove(event.getPersonId());
	}

	public void handleEvent(final LinkEnterEvent event) {
		linkEnter(event.getPersonId(), event.getLinkId(), (int) event.getTime());
	}

	private void linkEnter(Id personId, Id linkId, int time) {
		EventAgent agent = eventAgents.get(personId);
		if (agent != null) {
			agent.startTime = time;
			Link link = network.getLinks().get(linkId);
			agent.startCoord = link.getFromNode().getCoord();
			agent.endCoord = link.getToNode().getCoord();
		}
	}

	public void handleEvent(final LinkLeaveEvent event) {
		linkLeave(event.getPersonId(), event.getLinkId(), (int) event.getTime());
	}

	private void linkLeave(Id personId, Id linkId, int endTime) {
		EventAgent agent = eventAgents.get(personId);
		if (agent != null) {
			if (agent.startCoord != null) {
				doSnapshots(personId, linkId, endTime, agent);
			}
		}
	}

	private void doSnapshots(Id personId, Id linkId, int endTime, EventAgent agent) {
		double x = agent.startCoord.getX();
		double y = agent.startCoord.getY();
		int dTime = endTime - agent.startTime;
		double dx = (agent.endCoord.getX() - x) / dTime;
		double dy = (agent.endCoord.getY() - y) / dTime;
		Link link = network.getLinks().get(linkId);
		double dist = link.getLength();
		double speed = dist / dTime;
		double freespeed = link.getFreespeed(endTime);
		double speedRatio = speed / freespeed;
		for (int i = agent.startTime; i <= endTime; i++) {
			if (i % snapshotPeriod == 0) {
				double easting = x + (i - agent.startTime) * dx;
				double northing = y + (i - agent.startTime) * dy; 
				AgentSnapshotInfo agentPositionInfo = AgentSnapshotInfoFactory.staticCreateAgentSnapshotInfo(personId, easting, northing, 0.0d, 0.0d);
				agentPositionInfo.setAgentState(AgentState.PERSON_DRIVING_CAR);
				agentPositionInfo.setColorValueBetweenZeroAndOne(speedRatio);
				doSnapshot(i, agentPositionInfo);
			}
		}
	}

	public void handleEvent(final AgentWait2LinkEvent event) {
		// wait2Link(event.getPersonId(), event.getLinkId(), event.getTime());
	}

	public void handleEvent(final AgentStuckEvent event) {
		linkLeave(event.getPersonId(), event.getLinkId(), (int) event.getTime());
		eventAgents.remove(event.getPersonId());
	}

	public void reset(final int iteration) {
		this.eventAgents.clear();
	}

	private void doSnapshot(int time, AgentSnapshotInfo agentPositionInfo) {
		for (SnapshotWriter writer : this.snapshotWriters) {
			writer.beginSnapshot(time);
			writer.addAgent(agentPositionInfo);
			writer.endSnapshot();
		}
	}

	public void finish() {
		for (SnapshotWriter writer : this.snapshotWriters) {
			writer.finish();
		}
	}

	private static class EventAgent {
		public Coord endCoord;
		public Coord startCoord;
		protected int startTime;
	}

}
