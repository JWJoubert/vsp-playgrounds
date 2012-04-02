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

package playground.mrieser.core.mobsim.usecases;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Mobsim;

import playground.mrieser.core.mobsim.features.StatusFeature;
import playground.mrieser.core.mobsim.impl.ActivityHandler;
import playground.mrieser.core.mobsim.impl.DefaultTimestepSimEngine;
import playground.mrieser.core.mobsim.impl.LegHandler;
import playground.mrieser.core.mobsim.impl.PlanMobsimImpl;
import playground.mrieser.core.mobsim.impl.PopulationAgentSource;
import playground.mrieser.core.mobsim.impl.TeleportationHandler;

public class TeleportOnlyMobsimFactory implements MobsimFactory {

	private double populationWeight = 1.0;

	/**
	 * Sets the weight for agents created from the population.
	 * If your population is a 20%-sample, the weight is typically 5
	 * (each agent should count for 5 persons).
	 *
	 * @param populationWeight
	 */
	public void setPopulationWeight(double populationWeight) {
		this.populationWeight = populationWeight;
	}

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {

		PlanMobsimImpl planSim = new PlanMobsimImpl(sc);

		DefaultTimestepSimEngine engine = new DefaultTimestepSimEngine(planSim, eventsManager);
		planSim.setMobsimEngine(engine);

		// setup features; order is important!
		planSim.addMobsimFeature(new StatusFeature());

		// setup PlanElementHandlers
		ActivityHandler ah = new ActivityHandler(engine);
		LegHandler lh = new LegHandler(engine);
		planSim.setPlanElementHandler(Activity.class, ah);
		planSim.setPlanElementHandler(Leg.class, lh);

		planSim.addMobsimFeature(ah); // how should a user now ah is a simfeature, bug lh not?

		// setup DepartureHandlers
		TeleportationHandler teleporter = new TeleportationHandler(engine);
		teleporter.setDefaultTeleportationTime(180); // this should usually not be set! TODO [MR] remove line
		planSim.addMobsimFeature(teleporter); // how should a user now teleporter is a simfeature?
		lh.setDepartureHandler(TransportMode.car, teleporter);
		lh.setDepartureHandler(TransportMode.pt, teleporter);
		lh.setDepartureHandler(TransportMode.walk, teleporter);
		lh.setDepartureHandler(TransportMode.bike, teleporter);
		lh.setDepartureHandler("undefined", teleporter);

		// register agent sources
		planSim.addAgentSource(new PopulationAgentSource(sc.getPopulation(), this.populationWeight));

		return planSim;
	}

}
