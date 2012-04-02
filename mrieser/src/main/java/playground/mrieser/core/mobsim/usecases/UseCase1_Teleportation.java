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
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.config.ConfigUtils;

/**
 * @author mrieser
 */
public class UseCase1_Teleportation {

	public static void main(String[] args) {

		String prefix = "../../MATSim/";

		// load data
		Config config;
		config = ConfigUtils.loadConfig(prefix + "test/scenarios/equil/config.xml");
		ConfigUtils.modifyFilePaths(config, prefix);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		Scenario scenario = loader.loadScenario();
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		EventWriterXML ew = new EventWriterXML("testEvents.xml");
		events.addHandler(ew);

		/* **************************************************************** */

		Mobsim sim = new TeleportOnlyMobsimFactory().createMobsim(scenario, events);
		sim.run(); // replace with PlanSimulation.runMobsim();

		/* **************************************************************** */

		ew.closeFile();
	}
}
