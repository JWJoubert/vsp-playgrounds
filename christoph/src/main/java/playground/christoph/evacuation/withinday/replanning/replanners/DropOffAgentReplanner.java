/* *********************************************************************** *
 * project: org.matsim.*
 * DropOffAgentReplanner.java
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

package playground.christoph.evacuation.withinday.replanning.replanners;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.utils.EditRoutes;

import playground.christoph.evacuation.controler.PrepareEvacuationScenario;
import playground.christoph.evacuation.mobsim.OldPassengerDepartureHandler;

/**
 * 
 * @author cdobler
 */
public class DropOffAgentReplanner extends WithinDayDuringLegReplanner {

	private static final Logger log = Logger.getLogger(DropOffAgentReplanner.class);
	
	private static final String activityType = "dropoff";
	
	private final RouteFactory routeFactory;
	
	/*package*/ DropOffAgentReplanner(Id id, Scenario scenario, InternalInterface internalInterface) {
		super(id, scenario, internalInterface);
		this.routeFactory = new LinkNetworkRouteFactory();
	}

	@Override
	public boolean doReplanning(PlanBasedWithinDayAgent withinDayAgent) {
		
		// If we don't have a valid Replanner.
		if (this.routeAlgo == null) return false;

		// If we don't have a valid WithinDayPersonAgent
		if (withinDayAgent == null) return false;
		
		if (withinDayAgent.getMode().equals(TransportMode.car)) {
			return replanDriver(withinDayAgent);
		} else if (withinDayAgent.getMode().equals(OldPassengerDepartureHandler.passengerTransportMode)) {
			return replanPassenger(withinDayAgent);
		} else {
			log.warn("Unexpected mode was found: " + withinDayAgent.getMode());
			return false;
		}
	}
	
	private boolean replanDriver(PlanBasedWithinDayAgent withinDayAgent) {
		PlanImpl executedPlan = (PlanImpl) withinDayAgent.getSelectedPlan();

		if (withinDayAgent.getId().toString().equals("5898402")) {
			log.info("found agent...");
		}
		
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
		 * Create new drop off activity.
		 * After the linkMinTravelTime the vehicle is removed from the links buffer.
		 * At this point it is checked whether the vehicle should be parked at the link.
		 */
		double departureTime = this.time + 60.0;
		Activity dropOffActivity = scenario.getPopulation().getFactory().createActivityFromLinkId(activityType, currentLinkId);
		dropOffActivity.setType(activityType);
		dropOffActivity.setStartTime(this.time);
		dropOffActivity.setEndTime(departureTime);
		String idString = currentLinkId.toString() + PrepareEvacuationScenario.pickupDropOffSuffix;
		((ActivityImpl) dropOffActivity).setFacilityId(scenario.createId(idString));
		((ActivityImpl) dropOffActivity).setCoord(scenario.getNetwork().getLinks().get(currentLinkId).getCoord());
			
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
		NetworkRoute carRoute = (NetworkRoute) routeFactory.createRoute(currentLinkId, currentRoute.getEndLinkId());
		carRoute.setLinkIds(currentLinkId, subRoute, currentRoute.getEndLinkId());
		carRoute.setVehicleId(currentVehicleId);
		carLeg.setRoute(carRoute);
		
		/*
		 * End agent's current leg at the current link.
		 * Check whether the agent is already on the routes last link.
		 */
		if (currentLinkId.equals(currentRoute.getEndLinkId())) {
			subRoute.addAll(currentRoute.getLinkIds());	
		} else {
			subRoute = new ArrayList<Id>();
			subRoute.addAll(currentRoute.getLinkIds().subList(0, currentLinkIndex));
		}
		currentRoute.setLinkIds(currentRoute.getStartLinkId(), subRoute, currentLinkId);
		currentLeg.setTravelTime(this.time - currentLeg.getDepartureTime());
		
		/*
		 * Insert drop off activity and driver leg into agent's plan.
		 */
		executedPlan.getPlanElements().add(currentLegIndex + 1, dropOffActivity);
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
		Leg currentLeg = withinDayAgent.getCurrentLeg();
		
		/*
		 * Get agent's current link from the vehicle since the agent's
		 * current link is not updated. 
		 */
		PassengerAgent passenger = (PassengerAgent) withinDayAgent;
		Id currentLinkId = passenger.getVehicle().getCurrentLink().getId();
		
		/*
		 * Create new drop off activity.
		 */
		double departureTime = this.time + 60.0;
		Activity dropOffActivity = scenario.getPopulation().getFactory().createActivityFromLinkId(activityType, currentLinkId);
		dropOffActivity.setType(activityType);
		dropOffActivity.setStartTime(this.time);
		dropOffActivity.setEndTime(departureTime);
		String idString = currentLinkId.toString() + PrepareEvacuationScenario.pickupDropOffSuffix;
		((ActivityImpl) dropOffActivity).setFacilityId(scenario.createId(idString));
		((ActivityImpl) dropOffActivity).setCoord(scenario.getNetwork().getLinks().get(currentLinkId).getCoord());
				
		/*
		 * End agent's current leg at the current link.
		 */
		currentLeg.getRoute().setEndLinkId(currentLinkId);
		currentLeg.setTravelTime(this.time - currentLeg.getDepartureTime());
		
		/*
		 * Create new walk leg to the agents destination.
		 */
		Leg walkLeg = scenario.getPopulation().getFactory().createLeg(TransportMode.walk);
		walkLeg.setDepartureTime(departureTime);
				
		/*
		 * Insert drop off activity and walk leg into agent's plan.
		 */
		executedPlan.getPlanElements().add(currentLegIndex + 1, dropOffActivity);
		executedPlan.getPlanElements().add(currentLegIndex + 2, walkLeg);
		
		/*
		 * Create a new route for the walk leg.
		 */
		new EditRoutes().replanFutureLegRoute(executedPlan, currentLegIndex + 2, this.routeAlgo);
				
		// Finally reset the cached Values of the PersonAgent - they may have changed!
		withinDayAgent.resetCaches();
		
		return true;
	}
}