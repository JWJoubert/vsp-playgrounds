/* *********************************************************************** *
 * project: org.matsim.*
 * PreparePopulation.java
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

package playground.christoph.evacuation.population;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.mobsim.qsim.multimodalsimengine.tools.MultiModalNetworkCreator;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.LegRouter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;

import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.router.KtiPtRouteFactory;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;

public class PreparePopulation {

	final private static Logger log = Logger.getLogger(PreparePopulation.class);
	
	/**
	 * Input arguments:
	 * <ul>
	 *	<li>path to config file</li>
	 *  <li>path to events file</li>
	 *  <li>path to population output file</li>
	 * </ul>
	 */
	public static void main(String[] args) {
		if (args.length != 3) return;
		
		Config config = ConfigUtils.createConfig();
		
		KtiConfigGroup ktiConfigGroup = new KtiConfigGroup();
		config.addModule(KtiConfigGroup.GROUP_NAME, ktiConfigGroup);
		
		ConfigUtils.loadConfig(config, args[0]);
			
		String populationInputFile = config.plans().getInputFile();
		String eventsFile = args[1];
		String populationOutputFile = args[2];
		
		// remove population input file from config
		config.plans().setInputFile(null);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		new PreparePopulation(scenario, eventsFile, populationInputFile, populationOutputFile);
	}

	public PreparePopulation(Scenario scenario, String eventsFile, String populationInFile, String populationOutFile) {
		
		log.info("Reading events file to get data for travel time calculator...");
		EventsManager eventsManager = EventsUtils.createEventsManager();
		TravelTimeCalculator travelTime = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(scenario.getNetwork(), 
				scenario.getConfig().travelTimeCalculator());
		eventsManager.addHandler(travelTime);
		new MatsimEventsReader(eventsManager).readFile(eventsFile);
		log.info("done.");

		log.info("Setup multi-modal network...");
		new MultiModalNetworkCreator(scenario.getConfig().multiModal()).run(scenario.getNetwork());
		log.info("done.");
		
		log.info("Setup multi-modal router...");
		Map<String, LegRouter> legRouters = CreateMultiModalLegRouters.createLegRouters(scenario.getConfig(), 
				scenario.getNetwork(), travelTime);
		log.info("done.");
		
		log.info("Reading, processing, writing plans...");
		PopulationImpl population = (PopulationImpl) scenario.getPopulation();

		// add support for KTI pt routes
		KtiConfigGroup ktiConfigGroup = (KtiConfigGroup) scenario.getConfig().getModule(KtiConfigGroup.GROUP_NAME);
		PlansCalcRouteKtiInfo plansCalcRouteKtiInfo = new PlansCalcRouteKtiInfo(ktiConfigGroup);
		plansCalcRouteKtiInfo.prepare(scenario.getNetwork());
		((PopulationFactoryImpl) population.getFactory()).setRouteFactory(TransportMode.pt, new KtiPtRouteFactory(plansCalcRouteKtiInfo));
//		((PopulationFactoryImpl) population.getFactory()).setRouteFactory(TransportMode.car, new KtiLinkNetworkRouteFactory(this.getNetwork(), super.getConfig().planomat()));
		
		population.setIsStreaming(true);
		
		PopulationWriter populationWriter = new PopulationWriter(population, scenario.getNetwork(), ((ScenarioImpl)scenario).getKnowledges());
		populationWriter.startStreaming(populationOutFile);

		Set<String> modesToReroute = new HashSet<String>();
		modesToReroute.add(TransportMode.ride);
		modesToReroute.add(TransportMode.bike);
		modesToReroute.add(TransportMode.walk);
		modesToReroute.add(TransportMode.pt);
		
		population.addAlgorithm(new RemoveUnselectedPlans());
		population.addAlgorithm(new CreateMultiModalRoutes(legRouters, modesToReroute));
		population.addAlgorithm(populationWriter);
		
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(populationInFile);
		
		population.printPlansCount();
		populationWriter.closeStreaming();
		log.info("done.");
	}

}
