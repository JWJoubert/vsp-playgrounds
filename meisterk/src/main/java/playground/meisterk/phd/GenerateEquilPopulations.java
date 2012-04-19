/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateEquilPopulations.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.phd;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.replanning.modules.PlanomatModule;
import org.matsim.core.replanning.modules.ReRouteDijkstra;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.misc.Time;

public class GenerateEquilPopulations {

	public static final int NUM_AGENTS = 4000;

	public GenerateEquilPopulations() {
		// TODO Auto-generated constructor stub
	}

	protected void generateRandomInitialDemand(ScenarioImpl scenario) {

		PopulationImpl pop = (PopulationImpl) scenario.getPopulation();
		PopulationFactory popFactory = pop.getFactory();

		Network network = scenario.getNetwork();

		Activity act = null;
		Leg leg = null;
		for (int ii=0; ii < NUM_AGENTS; ii++) {

			PersonImpl person = (PersonImpl) popFactory.createPerson(new IdImpl(ii));
			person.setEmployed(null);

			pop.addPerson(person);

			Plan plan = popFactory.createPlan();
			person.addPlan(plan);

			act = popFactory.createActivityFromLinkId("h", network.getLinks().get(new IdImpl(1)).getId());
			plan.addActivity(act);
			leg = popFactory.createLeg("undefined");
			plan.addLeg(leg);
			act = popFactory.createActivityFromLinkId("w", network.getLinks().get(new IdImpl(20)).getId());
			plan.addActivity(act);
			leg = popFactory.createLeg("undefined");
			plan.addLeg(leg);
			act = popFactory.createActivityFromLinkId("h", network.getLinks().get(new IdImpl(1)).getId());
			plan.addActivity(act);
		}

		Config config = scenario.getConfig();
		// no activity facilities are used here
		config.planomat().setTripStructureAnalysisLayer(PlanomatConfigGroup.TripStructureAnalysisLayerOption.link);

		EventsManager emptyEvents = EventsUtils.createEventsManager();
		TravelTimeCalculator tTravelEstimator = new TravelTimeCalculator(scenario.getNetwork(), config.travelTimeCalculator());
		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory(config.planCalcScore(), scenario.getNetwork());
		TravelDisutility travelCostEstimator = new TravelTimeAndDistanceBasedTravelDisutility(tTravelEstimator, config.planCalcScore());

		Controler dummyControler = new Controler(scenario);
		dummyControler.setLeastCostPathCalculatorFactory(new DijkstraFactory());

		PlanomatModule planomat = new PlanomatModule(
				dummyControler,
				emptyEvents,
				scenario.getNetwork(),
				scoringFunctionFactory,
				travelCostEstimator,
				tTravelEstimator);

		planomat.prepareReplanning();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getPlans().get(0);
			planomat.handlePlan(plan);

		}
		planomat.finishReplanning();

	}

	protected void generateAll6AMInitialDemand(ScenarioImpl scenario) {

		PopulationImpl pop = (PopulationImpl) scenario.getPopulation();
		PopulationFactory popFactory = pop.getFactory();

		Network network = scenario.getNetwork();

		Activity act = null;
		Leg leg = null;
		for (int ii=0; ii < NUM_AGENTS; ii++) {

			PersonImpl person = (PersonImpl) popFactory.createPerson(new IdImpl(ii));
			person.setEmployed(null);

			pop.addPerson(person);

			Plan plan = popFactory.createPlan();
			person.addPlan(plan);

			act = popFactory.createActivityFromLinkId("h", network.getLinks().get(new IdImpl(1)).getId());
			act.setEndTime(Time.parseTime("06:00:00"));
			plan.addActivity(act);
			leg = popFactory.createLeg(TransportMode.car);
			leg.setDepartureTime(Time.parseTime("06:00:00"));
			plan.addLeg(leg);
			act = popFactory.createActivityFromLinkId("w", network.getLinks().get(new IdImpl(20)).getId());
			act.setEndTime(Time.parseTime("14:00:00"));
			plan.addActivity(act);
			leg = popFactory.createLeg(TransportMode.car);
			leg.setDepartureTime(Time.parseTime("14:00:00"));
			plan.addLeg(leg);
			act = popFactory.createActivityFromLinkId("h", network.getLinks().get(new IdImpl(1)).getId());
			plan.addActivity(act);

		}

		// initial routes = free speed routes
		TravelTimeCalculatorFactory travelTimeCalculatorFactory = new TravelTimeCalculatorFactoryImpl();
		PersonalizableTravelTime travelTimeCalculator = travelTimeCalculatorFactory.createTravelTimeCalculator(
				network,
				scenario.getConfig().travelTimeCalculator());
		TravelDisutilityFactory travelCostCalculatorFactory = new TravelCostCalculatorFactoryImpl();
		TravelDisutility travelCostCalculator = travelCostCalculatorFactory.createTravelDisutility(
				travelTimeCalculator,
				scenario.getConfig().planCalcScore());

		ReRouteDijkstra router = new ReRouteDijkstra(
				scenario.getConfig(),
				network,
				travelCostCalculator,
				travelTimeCalculator,
				((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory());

		router.prepareReplanning();
		for (Person person : scenario.getPopulation().getPersons().values()) {

			Plan plan = person.getPlans().get(0);
			router.handlePlan(plan);

		}
		router.finishReplanning();

	}

}
