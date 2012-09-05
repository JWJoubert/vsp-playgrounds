/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.droeder.southAfrica.routing;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.LegRouter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.PtConstants;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.droeder.southAfrica.replanning.modules.PtSubModePtInteractionRemover;

/**
 * @author droeder based on mrieser
 *
 */
public class PlansCalcSubModeTransitRoute extends PlansCalcRoute{
	
	
	private class PtSubModeLegHandler implements LegRouter {

		@Override
		public double routeLeg(Person person, Leg leg, Activity fromAct, Activity toAct, double depTime) {
			return handlePtPlan(person, leg, fromAct, toAct, depTime);
		}

	}

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(PlansCalcSubModeTransitRoute.class);
	
	private PtSubModePtInteractionRemover remover = new PtSubModePtInteractionRemover();
	private PtSubModeRouter router;
	private Plan currentPlan = null;
	private final List<Tuple<Leg, List<Leg>>> legReplacements = new LinkedList<Tuple<Leg, List<Leg>>>();

	private TransitSchedule schedule;

	/**
	 * 	Most of this is copy & paste from the original PlansCalcTransitRoute. But for
	 * 	some reasons extending the class provide problems with the transitActsRemover
	 * 
	 * 
	 * @param config
	 * @param network
	 * @param costCalculator
	 * @param travelTimes
	 * @param factory
	 * @param routeFactory
	 * @param transitConfig
	 * @param transitRouter
	 * @param transitSchedule
	 */
	public PlansCalcSubModeTransitRoute(
			PlansCalcRouteConfigGroup config, Network network,
			TravelDisutility costCalculator,
			TravelTime travelTimes,
			LeastCostPathCalculatorFactory factory,
			ModeRouteFactory routeFactory, TransitConfigGroup transitConfig,
			TransitRouter transitRouter, TransitSchedule transitSchedule) {
		super(config, network, costCalculator, travelTimes, factory, routeFactory);
		if(!(transitRouter instanceof PtSubModeRouter)){
			throw new IllegalArgumentException("the transitRouter needs to be an instance of " + PtSubModeRouter.class.getSimpleName() +". ABORT!");
		}
		this.router = (PtSubModeRouter) transitRouter;
		this.schedule = transitSchedule;

		//////
		LeastCostPathCalculator routeAlgo = super.getLeastCostPathCalculator();
		if (routeAlgo instanceof IntermodalLeastCostPathCalculator) {
			((IntermodalLeastCostPathCalculator) routeAlgo).setModeRestriction(Collections.singleton(TransportMode.car));
		}
		routeAlgo = super.getPtFreeflowLeastCostPathCalculator();
		if (routeAlgo instanceof IntermodalLeastCostPathCalculator) {
			((IntermodalLeastCostPathCalculator) routeAlgo).setModeRestriction(Collections.singleton(TransportMode.car));
		}
		/////
		
		// add the default
		this.addLegHandler(TransportMode.pt, new PtSubModeLegHandler());
		// add all other modes (maybe pt again)
		for (String transitMode : transitConfig.getTransitModes()) {
			this.addLegHandler(transitMode, new PtSubModeLegHandler());
		}
	}
	
	@Override
	protected void handlePlan(Person person, final Plan plan) {
		this.remover.run(plan);
		this.currentPlan = plan;
		this.legReplacements.clear();
		super.handlePlan(person, plan);
		this.replaceLegs();
		this.currentPlan = null;
	}

	protected double handlePtPlan(Person person, final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
//		//use own method if leg is a transit-leg (not only pt!)
		List<Leg> legs= this.router.calcRoute(person, leg, fromAct, toAct, depTime);
		if(!(legs == null)){
			for(int i = 0; i < legs.size(); i++) {
				//not very nice, but legMode needs to be replaced here, because TransportMode.pt is 'hardcoded' in TransitRouterImpl... 
				if(!legs.get(i).getMode().equals(TransportMode.transit_walk) && this.router.calculatedRouteForMode(leg.getMode())){
					legs.get(i).setMode(leg.getMode());
				}
			}
		}
		this.legReplacements.add(new Tuple<Leg, List<Leg>>(leg, legs));
		double travelTime = 0.0;
		if (legs != null) {
			for (Leg leg2 : legs) {
				travelTime += leg2.getTravelTime();
			}
		}
		return travelTime;
	}
	
	/**
	 * c&p from org.matsim.PlansCalcTransitRoute
	 */
	private void replaceLegs() {
		Iterator<Tuple<Leg, List<Leg>>> replacementIterator = this.legReplacements.iterator();
		if (!replacementIterator.hasNext()) {
			return;
		}
		List<PlanElement> planElements = this.currentPlan.getPlanElements();
		Tuple<Leg, List<Leg>> currentTuple = replacementIterator.next();
		for (int i = 0; i < this.currentPlan.getPlanElements().size(); i++) {
			PlanElement pe = planElements.get(i);
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				if (leg == currentTuple.getFirst()) {
					// do the replacement
					if (currentTuple.getSecond() != null) {
						// first and last leg do not have the route set, as the start or end  link is unknown.
						Leg firstLeg = currentTuple.getSecond().get(0);
						Id fromLinkId = ((Activity) planElements.get(i-1)).getLinkId();
						Id toLinkId = null;
						if (currentTuple.getSecond().size() > 1) { // at least one pt leg available
							toLinkId = (currentTuple.getSecond().get(1).getRoute()).getStartLinkId();
						} else {
							toLinkId = ((Activity) planElements.get(i+1)).getLinkId();
						}
						firstLeg.setRoute(new GenericRouteImpl(fromLinkId, toLinkId));

						Leg lastLeg = currentTuple.getSecond().get(currentTuple.getSecond().size() - 1);
						toLinkId = ((Activity) planElements.get(i+1)).getLinkId();
						if (currentTuple.getSecond().size() > 1) { // at least one pt leg available
							fromLinkId = (currentTuple.getSecond().get(currentTuple.getSecond().size() - 2).getRoute()).getEndLinkId();
						}
						lastLeg.setRoute(new GenericRouteImpl(fromLinkId, toLinkId));

						boolean isFirstLeg = true;
						Coord nextCoord = null;
						for (Leg leg2 : currentTuple.getSecond()) {
							if (isFirstLeg) {
								planElements.set(i, leg2);
								isFirstLeg = false;
							} else {
								i++;
								if (leg2.getRoute() instanceof ExperimentalTransitRoute) {
									ExperimentalTransitRoute tRoute = (ExperimentalTransitRoute) leg2.getRoute();
									ActivityImpl act = new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, 
											this.schedule.getFacilities().get(tRoute.getAccessStopId()).getCoord(), 
											tRoute.getStartLinkId());
									act.setMaximumDuration(0.0);
									planElements.add(i, act);
									nextCoord = this.schedule.getFacilities().get(tRoute.getEgressStopId()).getCoord();
								} else { // walk legs don't have a coord, use the coord from the last egress point
									ActivityImpl act = new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, nextCoord, 
											leg2.getRoute().getStartLinkId());
									act.setMaximumDuration(0.0);
									planElements.add(i, act);
								}
								i++;
								planElements.add(i, leg2);
							}
						}
					}
//					else{
//						// as it is a pt-route the route is set to null here, to prevent undesired teleporting (the transit-walk-legs are removed, but a new route was not found!)
						// should be done now in ptinteraction-remover
//						leg.setRoute(null);
//					}
					if (!replacementIterator.hasNext()) {
						return;
					}
					currentTuple = replacementIterator.next();
				}
			}
		}

	}
}
