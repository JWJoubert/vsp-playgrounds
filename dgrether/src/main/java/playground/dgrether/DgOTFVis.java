package playground.dgrether;

import java.net.URL;
import java.net.URLClassLoader;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.dgrether.utils.DgOTFVisUtils;

/* *********************************************************************** *
 * project: org.matsim.*
 * DgOTFVis
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

/**
 * @author dgrether
 *
 */
public class DgOTFVis {
	
	
	private static final Logger log = Logger.getLogger(DgOTFVis.class);
	
	
	public void playScenario(Scenario scenario) {
		if (scenario.getConfig().getQSimConfigGroup() == null){
			log.error("Cannot play live config without config module for QSim (in Java QSimConfigGroup). " +
					"Fixing this by adding default config module for QSim. " +
					"Please check if default values fit your needs, otherwise correct them in " +
					"the config given as parameter to get a valid visualization!");
			scenario.getConfig().addQSimConfigGroup(new QSimConfigGroup());
		}
		EventsManager events = EventsUtils.createEventsManager();
//		events.addHandler(new LogOutputEventHandler());
		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, events);

		if (scenario.getConfig().scenario().isUseSignalSystems()){
			SignalEngine engine = new QSimSignalEngine(new FromDataBuilder(scenario, events).createAndInitializeSignalSystemsManager());
			qSim.addQueueSimulationListeners(engine);
		}
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
		OTFClientLive.run(scenario.getConfig(), server);
		qSim.run();

	}
	
	public void playAndRouteConfig(String config){
		Config cc = ConfigUtils.loadConfig(config);
		cc.otfVis().setMapOverlayMode(true);
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.loadScenario(cc);
		DgOTFVisUtils.preparePopulation4Simulation(sc);
		this.playScenario(sc);
	}
	
	public static void  printClasspath(){
		System.out.println("Classpath: ");
	//Get the System Classloader
    ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
    //Get the URLs
    URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();
    for(int i=0; i< urls.length; i++)
    {
        System.out.println("  " + urls[i].getFile());
    }
	}

	
	public static void main(String[] args) {
		new DgOTFVis().playAndRouteConfig(args[0]);
	}
}
