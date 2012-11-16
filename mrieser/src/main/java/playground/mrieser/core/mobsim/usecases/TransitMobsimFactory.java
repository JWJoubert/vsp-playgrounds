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
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import playground.mrieser.core.mobsim.api.AgentSource;
import playground.mrieser.core.mobsim.api.DepartureHandler;
import playground.mrieser.core.mobsim.features.StatusFeature;
import playground.mrieser.core.mobsim.features.refQueueNetworkFeature.RefQueueNetworkFeature;
import playground.mrieser.core.mobsim.impl.ActivityHandler;
import playground.mrieser.core.mobsim.impl.CarDepartureHandler;
import playground.mrieser.core.mobsim.impl.DefaultTimestepSimEngine;
import playground.mrieser.core.mobsim.impl.LegHandler;
import playground.mrieser.core.mobsim.impl.PlanMobsimImpl;
import playground.mrieser.core.mobsim.impl.PopulationAgentSource;
import playground.mrieser.core.mobsim.impl.TeleportationHandler;
import playground.mrieser.core.mobsim.transit.TransitDepartureHandler;
import playground.mrieser.core.mobsim.transit.TransitDriverAgentSource;
import playground.mrieser.core.mobsim.transit.TransitDriverDepartureHandler;
import playground.mrieser.core.mobsim.transit.TransitFeature;

public class TransitMobsimFactory implements MobsimFactory {

	private double populationWeight = 1.0;

	private double mobsimStopTime = Double.POSITIVE_INFINITY;

	private final static String TRANSIT_VEHICLE_LEG_TYPE = "transitVehicleLeg";

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

	/**
	 * Sets a time at which the mobsim will stop its execution, no matter
	 * if all agents have completed their plans or not. Initially set to
	 * {@link Double#POSITIVE_INFINITY}.
	 *
	 * @param stopTime
	 */
	public void setMobsimStopTime(final double stopTime) {
		this.mobsimStopTime = stopTime;
	}


	@Override
	public Mobsim createMobsim(Scenario scenario, EventsManager eventsManager) {
		// setup transit related stuff
		TransitSchedule schedule = ((ScenarioImpl) scenario).getTransitSchedule();
		Vehicles transitVehicles = ((ScenarioImpl) scenario).getVehicles();

		// setup mobsim
		PlanMobsimImpl planSim = new PlanMobsimImpl(scenario);
		DefaultTimestepSimEngine engine = new DefaultTimestepSimEngine(planSim, eventsManager);
		engine.setStopTime(this.mobsimStopTime);
		planSim.setMobsimEngine(engine);

		// setup network
		RefQueueNetworkFeature netFeature = new RefQueueNetworkFeature(scenario.getNetwork(), engine);

		// setup transit stuff
		TransitStopAgentTracker agentTracker = new TransitStopAgentTracker(eventsManager);
		TransitFeature transitFeature = new TransitFeature(agentTracker);
		AgentSource transitAgentSource = new TransitDriverAgentSource(schedule, transitVehicles, scenario.getNetwork(), agentTracker, TRANSIT_VEHICLE_LEG_TYPE);
		DepartureHandler transitDriverDepartureHandler = new TransitDriverDepartureHandler(engine, netFeature, transitFeature, scenario);

		// setup PlanElementHandlers
		ActivityHandler ah = new ActivityHandler(engine);
		LegHandler lh = new LegHandler(engine);
		planSim.setPlanElementHandler(Activity.class, ah);
		planSim.setPlanElementHandler(Leg.class, lh);

		// setup DepartureHandlers
		lh.setDepartureHandler(TransportMode.car, new CarDepartureHandler(engine, netFeature, scenario));
		lh.setDepartureHandler(TransportMode.pt, new TransitDepartureHandler(agentTracker));
		lh.setDepartureHandler(TRANSIT_VEHICLE_LEG_TYPE, transitDriverDepartureHandler);
		TeleportationHandler teleporter = new TeleportationHandler(engine);
		lh.setDepartureHandler(TransportMode.walk, teleporter);
		lh.setDepartureHandler(TransportMode.transit_walk, teleporter);
		lh.setDepartureHandler(TransportMode.bike, teleporter);

		// setup features; order is important!
		planSim.addMobsimFeature(new StatusFeature());
		planSim.addMobsimFeature(teleporter);
		planSim.addMobsimFeature(transitFeature);
		planSim.addMobsimFeature(ah);
		planSim.addMobsimFeature(netFeature);

		// register agent sources
		planSim.addAgentSource(new PopulationAgentSource(scenario.getPopulation(), this.populationWeight));
		planSim.addAgentSource(transitAgentSource);
		return planSim;
	}

}
