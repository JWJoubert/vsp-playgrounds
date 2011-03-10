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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;

/**
 * @author mrieser
 */
public class UseCase3_TransitMobsim {

	public static void main(String[] args) {

		String prefix = "../../matsim/";

		// load data
		Config config;
		config = ConfigUtils.loadConfig(prefix + "examples/pt-tutorial/0.config.xml");
		ConfigUtils.modifyFilePaths(config, prefix);

		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);

		((NetworkFactoryImpl) loader.getScenario().getNetwork().getFactory()).setRouteFactory("pt", new ExperimentalTransitRouteFactory());

		Scenario scenario = loader.loadScenario();
		EventsManager events = new EventsManagerImpl();
		EventWriterXML ew = new EventWriterXML("testEvents.xml");
		events.addHandler(ew);

		/* **************************************************************** */

		TransitMobsimFactory factory = new TransitMobsimFactory();
		factory.setMobsimStopTime(60.0*3600);
		Simulation sim = factory.createMobsim(scenario, events);

		sim.run(); // replace with PlanSimulation.runMobsim();

		/* **************************************************************** */

		ew.closeFile();
	}
}
