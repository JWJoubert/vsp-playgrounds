/* *********************************************************************** *
 * project: org.matsim.*
 * RunKtiScenario.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package kticompatibility;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;


import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;

/**
 * run a basic KTI-like scenario.
 *
 * There is no facility load (which to my knowledge was not used anyway),
 * and the analysis listenners are not added.
 * @author thibautd
 */
public class RunKtiScenario {
	public static void main(final String[] args) {
		OutputDirectoryLogging.catchLogEntries();
		final String configFile = args[ 0 ];

		// read the config with our special parameters
		// Note that you need 
		final Config config = ConfigUtils.createConfig();
		config.addModule( new KtiPtConfigGroup() );
		ConfigUtils.loadConfig( config , configFile );

		// just make sure the scenario is loaded
		// Controler accepts a config, but if the Scenario is not
		// fully loaded when creating the routing module, we may get into
		// troubles later...
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		final Controler controler = new Controler( scenario );
		controler.setTripRouterFactory(
				new KtiTripRouterFactory(
					controler ) );

		// we're done!
		controler.run();
	}
}


