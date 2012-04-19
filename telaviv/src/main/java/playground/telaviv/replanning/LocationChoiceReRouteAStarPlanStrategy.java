/* *********************************************************************** *
 * project: org.matsim.*
 * LocationChoiceReRouteAStarPlanStrategy.java
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

package playground.telaviv.replanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRouteLandmarks;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelDisutility;

import playground.telaviv.locationchoice.LocationChoicePlanModule;

/*
 * PlanStrategy can be selected by using the full path to this Class in
 * the config file, e.g.:
 * <param name="ModuleProbability_1" value="0.1" />
 * <param name="Module_1" value="playground.telaviv.replanning.LocationChoiceReRouteAStarPlanStrategy" /> 
 */
public class LocationChoiceReRouteAStarPlanStrategy implements PlanStrategy {

	private PlanStrategy planStrategyDelegate = null;
	
	public LocationChoiceReRouteAStarPlanStrategy(Controler controler) {
		
		Scenario scenario = controler.getScenario();
		Network network = controler.getNetwork();
		TravelDisutility travelCostCalc = controler.createTravelCostCalculator();
		PersonalizableTravelTime travelTimeCalc = controler.getTravelTimeCalculator();
		Config config = controler.getConfig();
		
		planStrategyDelegate = new PlanStrategyImpl(new RandomPlanSelector());
		planStrategyDelegate.addStrategyModule(new LocationChoicePlanModule(scenario));
		planStrategyDelegate.addStrategyModule(new ReRouteLandmarks(config, network, travelCostCalc, travelTimeCalc, new FreespeedTravelTimeAndDisutility(config.planCalcScore()), ((PopulationFactoryImpl) controler.getPopulation().getFactory()).getModeRouteFactory()));
	}
	
	@Override
	public void addStrategyModule(PlanStrategyModule module) {
		planStrategyDelegate.addStrategyModule(module);
	}

	@Override
	public void finish() {
		planStrategyDelegate.finish();
	}

	@Override
	public int getNumberOfStrategyModules() {
		return planStrategyDelegate.getNumberOfStrategyModules();
	}

	@Override
	public PlanSelector getPlanSelector() {
		return planStrategyDelegate.getPlanSelector();
	}

	@Override
	public void init() {
		planStrategyDelegate.init();
	}

	@Override
	public void run(Person person) {
		planStrategyDelegate.run(person);
	}

	@Override
	public String toString() {
		return planStrategyDelegate.toString();
	}

}