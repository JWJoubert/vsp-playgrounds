/* *********************************************************************** *
 * project: org.matsim.*
 * GuidanceMobsimFactory
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
package playground.vsptelematics.ha3;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;

/**
 * @author dgrether
 * 
 */
public class GuidanceMobsimFactory implements MobsimFactory, ShutdownListener {

	private double equipmentFraction;
	private Guidance guidance = null;
	private String type;
	private String outfile;

	public GuidanceMobsimFactory(String type, double equipmentFraction, String outfile) {
		this.equipmentFraction = equipmentFraction;
		this.type = type;
		this.outfile = outfile;
	}
	
	private void initGuidance(Network network){
		if ("reactive".equalsIgnoreCase(type)){
			this.guidance = new ReactiveGuidance(network, outfile);
		}
		else if ("estimated".equalsIgnoreCase(type)){
			this.guidance = new EstimatedGuidance(network, outfile);
		}
		else {
			throw new IllegalStateException("Guidance type " + type + " is not known!");
		}
	}

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
		if (conf == null) {
			throw new NullPointerException(
					"There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}
		QNetsimEngineFactory netsimEngFactory;
		netsimEngFactory = new DefaultQSimEngineFactory();
		QSim qSim = new QSim(sc, eventsManager);
		ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim, MatsimRandom.getRandom());
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);

		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());
		}

		this.initGuidance(sc.getNetwork());
		eventsManager.addHandler(this.guidance);
		qSim.addQueueSimulationListeners(this.guidance);
		AgentFactory agentFactory = new GuidanceAgentFactory(qSim, equipmentFraction, this.guidance);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory,
				qSim);
		qSim.addAgentSource(agentSource);
		return qSim;

	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		this.guidance.notifyShutdown();
	}

}
