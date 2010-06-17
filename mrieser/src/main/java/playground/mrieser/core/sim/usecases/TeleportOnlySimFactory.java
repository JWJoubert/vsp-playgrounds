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

package playground.mrieser.core.sim.usecases;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Simulation;

import playground.mrieser.core.sim.features.StatusFeature;
import playground.mrieser.core.sim.impl.ActivityHandler;
import playground.mrieser.core.sim.impl.LegHandler;
import playground.mrieser.core.sim.impl.PlanSimulationImpl;
import playground.mrieser.core.sim.impl.TeleportationHandler;
import playground.mrieser.core.sim.impl.TimestepSimEngine;

public class TeleportOnlySimFactory implements MobsimFactory {

	@Override
	public Simulation createMobsim(Scenario sc, EventsManager eventsManager) {

		PlanSimulationImpl planSim = new PlanSimulationImpl(sc, eventsManager);

		TimestepSimEngine engine = new TimestepSimEngine(planSim, eventsManager);
		planSim.setSimEngine(engine);

		// setup features; order is important!
		planSim.addSimFeature(new StatusFeature());

		// setup PlanElementHandlers
		ActivityHandler ah = new ActivityHandler(engine);
		LegHandler lh = new LegHandler(engine);
		planSim.setPlanElementHandler(Activity.class, ah);
		planSim.setPlanElementHandler(Leg.class, lh);

		planSim.addSimFeature(ah); // how should a user now ah is a simfeature, bug lh not?

		// setup DepartureHandlers
		TeleportationHandler teleporter = new TeleportationHandler(engine);
		teleporter.setDefaultTeleportationTime(180); // this should usually not be set! TODO [MR] remove line
		planSim.addSimFeature(teleporter); // how should a user now teleporter is a simfeature?
		lh.setDepartureHandler(TransportMode.car, teleporter);
		lh.setDepartureHandler(TransportMode.pt, teleporter);
		lh.setDepartureHandler(TransportMode.walk, teleporter);
		lh.setDepartureHandler(TransportMode.bike, teleporter);

		return planSim;
	}

}
