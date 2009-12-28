/* *********************************************************************** *
 * project: org.matsim.*
 * CalcSwissPtPlan.java
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

package playground.meisterk.kti.router;

import java.util.EnumSet;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.world.Layer;
import org.matsim.world.Location;
import org.matsim.world.MappedLocation;


/**
 * Modifications of the roting module for the KTI project.
 * 
 * @author mrieser
 * @author meisterk
 *
 */
public class PlansCalcRouteKti extends PlansCalcRoute {

	/**
	 * Transport modes which are considered in the kti project.
	 * 
	 * Use this set to check whether the mode of a leg is allowed or not.
	 */
	public static final EnumSet<TransportMode> KTI_MODES = EnumSet.of(
			TransportMode.car, 
			TransportMode.bike, 
			TransportMode.pt, 
			TransportMode.walk, 
			TransportMode.ride);
	
	private final Network network;
	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo;
	
	public PlansCalcRouteKti(
			final PlansCalcRouteConfigGroup group,
			final Network network, 
			final TravelCost costCalculator,
			final TravelTime timeCalculator, 
			final LeastCostPathCalculatorFactory factory,
			final PlansCalcRouteKtiInfo ptRoutingInfo) {
		super(group, network, costCalculator, timeCalculator, factory);
		this.network = network;
		this.plansCalcRouteKtiInfo = ptRoutingInfo;
	}
	
	@Override
	public double handleLeg(final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {

		TransportMode mode = leg.getMode();

		// TODO meisterk: This is a shortcut. Please find a general solution for that. [balmermi]
//		if (mode == TransportMode.ride) { mode = TransportMode.car; }

		if (!KTI_MODES.contains(mode)) {
			throw new RuntimeException("cannot handle legmode '" + mode.toString() + "'.");
		}
		
		double travelTime = 0.0;
		
		Link fromLink = fromAct.getLink();
		Link toLink = toAct.getLink();
		if (fromLink.equals(toLink)) {
			// create an empty route == staying on place if toLink == endLink
			RouteWRefs route = this.getRouteFactory().createRoute(mode, fromLink, toLink);
			route.setTravelTime(travelTime);
			if (Double.isNaN(route.getDistance())) {
				route.setDistance(0.0);
			}
			leg.setRoute(route);
			leg.setDepartureTime(depTime);
			leg.setTravelTime(travelTime);
			leg.setArrivalTime(depTime + travelTime);
		} else {
			if (mode.equals(TransportMode.pt)) {
				travelTime = handleSwissPtLeg(fromAct, leg, toAct, depTime);
			} else {
				travelTime = super.handleLeg(leg, fromAct, toAct, depTime);
			}
		}
		
		return travelTime;
	}

	/**
	 * Make use of the Swiss National transport model to find more or less realistic travel times.
	 * 
	 * The travel time of a leg with mode 'pt' (meaning pseudo/public transport) 
	 * is estimated by a municipality-to-municipality travel time matrix, plus walk-speed access to and egress from the next pt stop.
	 * 
	 * @param fromAct
	 * @param leg
	 * @param toAct
	 * @param depTime
	 * @return
	 */
	public double handleSwissPtLeg(final ActivityImpl fromAct, final LegImpl leg, final ActivityImpl toAct, final double depTime) {
		
		double travelTime = 0.0;
		
		SwissHaltestelle fromStop = this.plansCalcRouteKtiInfo.getHaltestellen().getClosestLocation(fromAct.getCoord());
		SwissHaltestelle toStop = this.plansCalcRouteKtiInfo.getHaltestellen().getClosestLocation(toAct.getCoord());

		Layer municipalities = this.plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality");
		final List<MappedLocation> froms = municipalities.getNearestLocations(fromStop.getCoord());
		final List<MappedLocation> tos = municipalities.getNearestLocations(toStop.getCoord());
		Location fromMunicipality = froms.get(0);
		Location toMunicipality = tos.get(0);

		KtiPtRoute newRoute = new KtiPtRoute(fromAct.getLink(), toAct.getLink(), plansCalcRouteKtiInfo, fromStop, fromMunicipality, toMunicipality, toStop);
		leg.setRoute(newRoute);

//		final double timeInVehicle = newRoute.calcInVehicleTime();
		final double timeInVehicle = newRoute.getInVehicleTime();
		
		final double walkAccessEgressDistance = newRoute.calcAccessEgressDistance(fromAct, toAct);
		final double walkAccessEgressTime = PlansCalcRouteKti.getAccessEgressTime(walkAccessEgressDistance, this.configGroup);

		newRoute.setDistance((walkAccessEgressDistance + newRoute.calcInVehicleDistance()));
		
		travelTime = walkAccessEgressTime + timeInVehicle;
		
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travelTime);
		leg.setArrivalTime(depTime + travelTime);
		
		return travelTime;
	}

	public static double getAccessEgressTime(final double distance, final PlansCalcRouteConfigGroup group) {
		return distance / group.getWalkSpeedFactor();
	}
	
}
