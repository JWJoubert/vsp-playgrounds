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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioLoaderImpl;

/**
 * @author mrieser
 */
public class UseCase2_RefMobsim {

	public static void main(final String[] args) {

		String prefix = "../../MATSim/";

		// load data
		Config config;
		config = ConfigUtils.loadConfig(prefix + "test/scenarios/berlin/config.xml");
//		config.plans().setInputFile("test/scenarios/equil/plans1.xml");
//		config.plans().setInputFile("test/scenarios/berlin/plans_hwh_sample.xml");
		ConfigUtils.modifyFilePaths(config, prefix);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		Scenario scenario = loader.loadScenario();
		System.out.println("# persons: " + scenario.getPopulation().getPersons().size());
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
//		EventWriterXML ew;
		/* **************************************************************** */

//		ew = new EventWriterXML("testEventsNewBln.xml");
//		events.addHandler(ew);
		Mobsim sim = new RefMobsimFactory().createMobsim(scenario, events);
		sim.run(); // replace with PlanSimulation.runMobsim();

		/* **************************************************************** */

//		ew.closeFile();
	}
}
