/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcPtRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mmoyo.ptRouterAdapted;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.PtConstants;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.pt.router.*;

/**
 * @author mrieser
 */
public class AdaptedPlansCalcTransitRoute extends PlansCalcRoute {

	private final TransitActsRemover transitLegsRemover = new TransitActsRemover();
	private final AdaptedTransitRouter adaptedTransitRouter;
	private final TransitConfigGroup transitConfig;
	private final TransitSchedule schedule;

	private Plan currentPlan = null;
	private final List<Tuple<Leg, List<Leg>>> legReplacements = new LinkedList<Tuple<Leg, List<Leg>>>();

	/**
	 * This essentially does the following (if I see correctly):<ul>
	 * <li> It passes the arguments <tt>config, network, costCalculator, timeCalculator, factory</tt> through to the "normal"
	 *      PlanCalcRoute. </li>
	 * <li> It restricts the usable part of the network for the above to "car". </li>
	 * <li> It sets a non-configurable TransitRouter, based on <tt>schedule</tt> and an internally defined TransitRouterConfig. </li>
	 * <li> It remembers <tt>transitConfig</tt>.
	 * </ul>
	 */
	public AdaptedPlansCalcTransitRoute(final PlansCalcRouteConfigGroup config, final Network network,
			final PersonalizableTravelCost costCalculator, final TravelTime timeCalculator,
			final LeastCostPathCalculatorFactory factory, final TransitSchedule schedule,
			final TransitConfigGroup transitConfig) {
		super(config, network, costCalculator, timeCalculator, factory);

		this.schedule = schedule;
		this.transitConfig = transitConfig;
		this.adaptedTransitRouter = new AdaptedTransitRouter(schedule);

		LeastCostPathCalculator routeAlgo = super.getLeastCostPathCalculator();
		if (routeAlgo instanceof Dijkstra) {
			((Dijkstra) routeAlgo).setModeRestriction(EnumSet.of(TransportMode.car));
		}
		routeAlgo = super.getPtFreeflowLeastCostPathCalculator();
		if (routeAlgo instanceof Dijkstra) {
			((Dijkstra) routeAlgo).setModeRestriction(EnumSet.of(TransportMode.car));
		}
	}

	@Override
	public void handlePlan(Person person, final Plan plan) {
		this.transitLegsRemover.run(plan);
		this.currentPlan = plan;
		this.legReplacements.clear();
		super.handlePlan(person, plan);
		this.replaceLegs();
		this.currentPlan = null;

	}

	@Override
	public double handleLeg(Person person, final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {
		if (this.transitConfig.getTransitModes().contains(leg.getMode())) {
			return this.handlePtPlan(leg, fromAct, toAct, depTime);
		}
		return super.handleLeg(person, leg, fromAct, toAct, depTime);
	}

	private double handlePtPlan(final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {
		List<Leg> legs= this.adaptedTransitRouter.calcRoute(fromAct.getCoord(), toAct.getCoord(), depTime);
		this.legReplacements.add(new Tuple<Leg, List<Leg>>(leg, legs));

		double travelTime = 0.0;
		if (legs != null) {
			for (Leg leg2 : legs) {
				travelTime += leg2.getTravelTime();
			}
		}
		return travelTime;
	}

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
						Id fromLinkId = ((ActivityImpl) planElements.get(i-1)).getLinkId();
						Id toLinkId = null;
						if (currentTuple.getSecond().size() > 1) { // at least one pt leg available
							toLinkId = ((RouteWRefs) currentTuple.getSecond().get(1).getRoute()).getStartLinkId();
						} else {
							toLinkId = ((ActivityImpl) planElements.get(i+1)).getLinkId();
						}
						firstLeg.setRoute(new GenericRouteImpl(fromLinkId, toLinkId));

						Leg lastLeg = currentTuple.getSecond().get(currentTuple.getSecond().size() - 1);
						toLinkId = ((ActivityImpl) planElements.get(i+1)).getLinkId();
						if (currentTuple.getSecond().size() > 1) { // at least one pt leg available
							fromLinkId = ((RouteWRefs) currentTuple.getSecond().get(currentTuple.getSecond().size() - 2).getRoute()).getEndLinkId();
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
									ActivityImpl act = new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, this.schedule.getFacilities().get(tRoute.getAccessStopId()).getCoord(), tRoute.getStartLinkId());
									act.setDuration(0.0);
									planElements.add(i, act);
									nextCoord = this.schedule.getFacilities().get(tRoute.getEgressStopId()).getCoord();
								} else { // walk legs don't have a coord, use the coord from the last egress point
									ActivityImpl act = new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, nextCoord, leg2.getRoute().getStartLinkId());
									act.setDuration(0.0);
									planElements.add(i, act);
								}
								i++;
								planElements.add(i, leg2);
							}
						}
					}
					if (!replacementIterator.hasNext()) {
						return;
					}
					currentTuple = replacementIterator.next();
				}
			}
		}

	}

}
