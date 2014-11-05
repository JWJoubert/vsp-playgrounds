/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.gregor.casim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.LegRouterWrapper;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;

public class CATripRouterFactory implements TripRouterFactory {

	private final String mainMode = "walkca";

	private Scenario scenario;
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

	public CATripRouterFactory(Scenario sc,
			LeastCostPathCalculatorFactory leastCostPathClaculatorFactory) {
		this.scenario = sc;
		this.leastCostPathCalculatorFactory = leastCostPathClaculatorFactory;
	}

	@Override
	public TripRouter instantiateAndConfigureTripRouter(
			RoutingContext routingContext) {

		LeastCostPathCalculator routeAlgo = leastCostPathCalculatorFactory
				.createPathCalculator(scenario.getNetwork(),
						routingContext.getTravelDisutility(),
						routingContext.getTravelTime());

		TripRouter tr = new TripRouter();

		tr.setRoutingModule(
				mainMode,
				new LegRouterWrapper(mainMode, scenario.getPopulation()
						.getFactory(), new NetworkLegRouter(scenario
						.getNetwork(), routeAlgo,
						((PopulationFactoryImpl) scenario.getPopulation()
								.getFactory()).getModeRouteFactory())));
		return tr;
	}

}
