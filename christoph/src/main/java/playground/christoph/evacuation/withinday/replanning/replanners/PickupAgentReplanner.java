/* *********************************************************************** *
 * project: org.matsim.*
 * PickupAgentReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.replanners;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRouteFactory;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;

import playground.christoph.evacuation.controler.PrepareEvacuationScenario;
import playground.christoph.evacuation.mobsim.OldPassengerDepartureHandler;

/**
 * 
 * @author cdobler
 */
public class PickupAgentReplanner extends WithinDayDuringLegReplanner {

	private static final Logger log = Logger.getLogger(PickupAgentReplanner.class);
	
	public static final String activityType = "pickup";
	
	private final RouteFactory carRouteFactory;
	private final RouteFactory rideRouteFactory;
	
	/*package*/ PickupAgentReplanner(Id id, Scenario scenario, InternalInterface internalInterface) {
		super(id, scenario, internalInterface);
		this.carRouteFactory = new LinkNetworkRouteFactory(); 
		this.rideRouteFactory = new GenericRouteFactory();
	}

	@Override
	public boolean doReplanning(PlanBasedWithinDayAgent withinDayAgent) {
		
		// If we don't have a valid WithinDayPersonAgent
		if (withinDayAgent == null) return false;
		
		if (withinDayAgent.getMode().equals(TransportMode.car)) {
			return replanDriver(withinDayAgent);
		} else if (withinDayAgent.getMode().equals(TransportMode.walk)) {
			return replanPassenger(withinDayAgent);
		} else {
			log.warn("Unexpected mode was found: " + withinDayAgent.getMode());
			return false;
		}
	}
	
	private boolean replanDriver(PlanBasedWithinDayAgent withinDayAgent) {
		PlanImpl executedPlan = (PlanImpl) withinDayAgent.getSelectedPlan();

		// If we don't have an executed plan
		if (executedPlan == null) return false;
		
		int currentLegIndex = withinDayAgent.getCurrentPlanElementIndex();
		int currentLinkIndex = withinDayAgent.getCurrentRouteLinkIdIndex();
		Id currentLinkId = withinDayAgent.getCurrentLinkId();
		Leg currentLeg = withinDayAgent.getCurrentLeg();
		NetworkRoute currentRoute = (NetworkRoute) currentLeg.getRoute();
		Id currentVehicleId = currentRoute.getVehicleId();
		List<Id> subRoute;
		
		/*
		 * Create new pickup activity.
		 */
		double departureTime = this.time + 60.0;
		Activity waitForPickupActivity = scenario.getPopulation().getFactory().createActivityFromLinkId(activityType, currentLinkId);
		waitForPickupActivity.setType(activityType);
		waitForPickupActivity.setStartTime(this.time);
		waitForPickupActivity.setEndTime(departureTime);
		String idString = currentLinkId.toString() + PrepareEvacuationScenario.pickupDropOffSuffix;
		((ActivityImpl) waitForPickupActivity).setFacilityId(scenario.createId(idString));
		((ActivityImpl) waitForPickupActivity).setCoord(scenario.getNetwork().getLinks().get(currentLinkId).getCoord());
		
		/*
		 * Create new car leg from the current position to the current legs destination.
		 * Re-use existing routes vehicle.
		 */
		Leg carLeg = scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		carLeg.setDepartureTime(departureTime);
		subRoute = new ArrayList<Id>();
		/*
		 * If the driver is not already at the end link of its current route, copy the
		 * so far not passed parts of the route to the new route.
		 */
		if (!currentLinkId.equals(currentRoute.getEndLinkId())) {
			/*
			 * If currentLinkIndex == currentRoute.getLinkIds().size(), only
			 * the route's endLink is left of the route. As a result, the new
			 * route will start on this link and end on the next one. Therefore,
			 * there are no links in between.
			 */
			if (currentLinkIndex < currentRoute.getLinkIds().size()) {
				subRoute.addAll(currentRoute.getLinkIds().subList(currentLinkIndex, currentRoute.getLinkIds().size()));				
			}
		}
		NetworkRoute carRoute = (NetworkRoute) carRouteFactory.createRoute(currentLinkId, currentRoute.getEndLinkId());
		carRoute.setLinkIds(currentLinkId, subRoute, currentRoute.getEndLinkId());
		carRoute.setVehicleId(currentVehicleId);
		carLeg.setRoute(carRoute);
		
		/*
		 * End agent's current leg at the current link.
		 * Check whether the agent is already on the routes last link.
		 */
		subRoute = new ArrayList<Id>();
		if (currentLinkId.equals(currentRoute.getEndLinkId())) {
			subRoute.addAll(currentRoute.getLinkIds());	
		} else {
			subRoute.addAll(currentRoute.getLinkIds().subList(0, currentLinkIndex));
		}
		currentRoute.setLinkIds(currentRoute.getStartLinkId(), subRoute, currentLinkId);
		currentLeg.setTravelTime(this.time - currentLeg.getDepartureTime());
			
		/*
		 * Insert pickup activity and driver leg into agent's plan.
		 */
		executedPlan.getPlanElements().add(currentLegIndex + 1, waitForPickupActivity);
		executedPlan.getPlanElements().add(currentLegIndex + 2, carLeg);
		
		// Finally reset the cached Values of the PersonAgent - they may have changed!
		withinDayAgent.resetCaches();
		
		return true;
	}
	
	private boolean replanPassenger(PlanBasedWithinDayAgent withinDayAgent) {
		PlanImpl executedPlan = (PlanImpl) withinDayAgent.getSelectedPlan();

		// If we don't have an executed plan
		if (executedPlan == null) return false;
		
		int currentLegIndex = withinDayAgent.getCurrentPlanElementIndex();
		int currentLinkIndex = withinDayAgent.getCurrentRouteLinkIdIndex();
		Id currentLinkId = withinDayAgent.getCurrentLinkId();
		Leg currentLeg = withinDayAgent.getCurrentLeg();
		
		/*
		 * Create new pickup activity.
		 */
		double departureTime = this.time + 60.0;
		Activity waitForPickupActivity = scenario.getPopulation().getFactory().createActivityFromLinkId(activityType, currentLinkId);
		waitForPickupActivity.setType(activityType);
		waitForPickupActivity.setStartTime(this.time);
		waitForPickupActivity.setEndTime(departureTime);
		String idString = currentLinkId.toString() + PrepareEvacuationScenario.pickupDropOffSuffix;
		((ActivityImpl) waitForPickupActivity).setFacilityId(scenario.createId(idString));
		((ActivityImpl) waitForPickupActivity).setCoord(scenario.getNetwork().getLinks().get(currentLinkId).getCoord());
				
		/*
		 * Create new ride_passenger leg to the rescue facility.
		 * Set mode to ride, then create route for the leg, then
		 * set the mode to the correct value (ride_passenger).
		 */
		Leg ridePassengerLeg = scenario.getPopulation().getFactory().createLeg(OldPassengerDepartureHandler.passengerTransportMode);
		ridePassengerLeg.setDepartureTime(departureTime);
		Route ridePassengerRoute = rideRouteFactory.createRoute(currentLinkId, currentLeg.getRoute().getEndLinkId());
		ridePassengerLeg.setRoute(ridePassengerRoute);
		
		/*
		 * Insert pickup activity and ride_passenger leg into agent's plan.
		 */
		executedPlan.getPlanElements().add(currentLegIndex + 1, waitForPickupActivity);
		executedPlan.getPlanElements().add(currentLegIndex + 2, ridePassengerLeg);
		
		/*
		 * End agent's current leg at the current link.
		 */
		NetworkRoute route = (NetworkRoute) currentLeg.getRoute();
		List<Id> subRoute = new ArrayList<Id>(route.getLinkIds().subList(0, currentLinkIndex));
		route.setLinkIds(route.getStartLinkId(), subRoute, currentLinkId);
		currentLeg.setTravelTime(this.time - currentLeg.getDepartureTime());	
		
		// Finally reset the cached Values of the PersonAgent - they may have changed!
		withinDayAgent.resetCaches();
		
		return true;
	}
}