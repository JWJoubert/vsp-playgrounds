/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.core.mobsim.impl;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.misc.Time;

import playground.mrieser.core.mobsim.api.MobsimKeepAlive;
import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.api.PlanElementHandler;
import playground.mrieser.core.mobsim.api.PlanSimulation;
import playground.mrieser.core.mobsim.api.TimestepSimEngine;
import playground.mrieser.core.mobsim.features.MobsimFeature;

public class DefaultTimestepSimEngine implements TimestepSimEngine {

	private final static Logger log = Logger.getLogger(DefaultTimestepSimEngine.class);

	private final PlanSimulation sim;
	private final EventsManager events;
	private double time;
	private final double timeStepSize;
	private final List<MobsimKeepAlive> aliveKeepers = new LinkedList<MobsimKeepAlive>();
	private double stopTime = Double.POSITIVE_INFINITY;

	public DefaultTimestepSimEngine(final PlanSimulation sim, final EventsManager events) {
		this(sim, events, 1.0);
	}

	public DefaultTimestepSimEngine(final PlanSimulation sim, final EventsManager events, final double timeStepSize) {
		this.sim = sim;
		this.events = events;
		this.timeStepSize = timeStepSize;
		this.time = 0;
	}

	public void setStopTime(final double stopTime) {
		this.stopTime = stopTime;
	}

	@Override
	public double getTimestepSize() {
		return this.timeStepSize;
	}

	@Override
	public double getCurrentTime() {
		return this.time;
	}

	@Override
	public EventsManager getEventsManager() {
		return this.events;
	}

	@Override
	public void handleAgent(final PlanAgent agent) {
		PlanElement currentPE = agent.getCurrentPlanElement();
		if (currentPE != null) {
			// == null could be the case when the agent starts with its first activity
			PlanElementHandler peh = this.sim.getPlanElementHandler(currentPE.getClass());
			if (peh == null) {
				throw new NullPointerException("No PlanElementHandler found for " + currentPE.getClass());
			}
			peh.handleEnd(agent);
		}

		PlanElement nextPE = agent.useNextPlanElement();
		if (nextPE != null) {
			// == null could be the case when the agent was at its last activity
			PlanElementHandler peh = this.sim.getPlanElementHandler(nextPE.getClass());
			if (peh == null) {
				throw new NullPointerException("No PlanElementHandler found for " + nextPE.getClass());
			}
			peh.handleStart(agent);
		}
	}

	@Override
	public void runSim() {

		List<MobsimFeature> tmpList = this.sim.getMobsimFeatures();
		MobsimFeature[] simFeatures = tmpList.toArray(new MobsimFeature[tmpList.size()]);

		log.info("registered features:");
		for (MobsimFeature feature : simFeatures) {
			log.info("  " + feature.getClass());
		}

		for (MobsimFeature feature : simFeatures) {
			feature.beforeMobSim();
		}

		boolean running = true;
		while (running) {
			for (MobsimFeature feature : simFeatures) {
				feature.doSimStep(this.time);
			}
			running = keepAlive();
			if (this.time >= this.stopTime) {
				running = false;
				log.warn("Stopping simulation at " + Time.writeTime(this.time));
				for (MobsimKeepAlive ska : this.aliveKeepers) {
					if (ska.keepAlive()) {
						log.warn("still alive: " + ska.getClass().getCanonicalName());
					}
				}
			}
			if (running) {
				this.time += this.timeStepSize;
			}
		}

		for (MobsimFeature feature : simFeatures) {
			feature.afterMobSim();
		}
	}

	@Override
	public void addKeepAlive(final MobsimKeepAlive keepAlive) {
		this.aliveKeepers.add(keepAlive);
	}

	private boolean keepAlive() {
		for (MobsimKeepAlive ska : this.aliveKeepers) {
			if (ska.keepAlive()) {
				return true;
			}
		}
		return false;
	}

}
