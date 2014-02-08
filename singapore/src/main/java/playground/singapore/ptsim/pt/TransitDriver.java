/* *********************************************************************** *
 * project: org.matsim.*
 * BusDriver.java
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

package playground.singapore.ptsim.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

public class TransitDriver extends AbstractTransitDriver {

	final NetworkRoute carRoute;

	final TransitLine transitLine;

	final TransitRoute transitRoute;

	final Departure departure;

	double departureTime;

	private final Leg currentLeg;
	
	public TransitDriver(final TransitLine line, final TransitRoute route, final Departure departure, 
			final TransitStopAgentTracker agentTracker, InternalInterface internalInterface) {
		super(internalInterface, agentTracker);
		PersonImpl driver = new PersonImpl(new IdImpl("ptDrvr_" + line.getId() + "_" + route.getId() + "_" + departure.getId().toString()));
		this.carRoute = route.getRoute();
		Plan plan = new PlanImpl();
		Leg leg = new LegImpl(TransportMode.car);
		leg.setRoute(getWrappedCarRoute(getCarRoute()));
		Activity startActivity = new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, leg.getRoute().getStartLinkId());
		Activity endActiity = new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, leg.getRoute().getEndLinkId());
		plan.addActivity(startActivity);
		plan.addLeg(leg);
		plan.addActivity(endActiity);
		driver.addPlan(plan);
		driver.setSelectedPlan(plan);
		this.currentLeg = leg;
		this.departureTime = departure.getDepartureTime();
		this.transitLine = line;
		this.transitRoute = route;
		this.departure = departure;
		setDriver(driver);
		init();
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		sendTransitDriverStartsEvent(now);
		this.state = MobsimAgent.State.LEG ;
	}

	@Override
	public void endLegAndComputeNextState(final double now) {
		this.getSimulation().getEventsManager().processEvent(
				new PersonArrivalEvent(now, this.getId(), this.getDestinationLinkId(), this.getCurrentLeg().getMode()));
		this.state = MobsimAgent.State.ACTIVITY ;
		this.departureTime = Double.POSITIVE_INFINITY ;
	}

	@Override
	public NetworkRoute getCarRoute() {
		return this.carRoute;
	}

	@Override
	public TransitLine getTransitLine() {
		return this.transitLine;
	}

	@Override
	public TransitRoute getTransitRoute() {
		return this.transitRoute;
	}

	@Override
	public double getActivityEndTime() {
		return this.departureTime;
	}

	@Override
	Leg getCurrentLeg() {
		return this.currentLeg;
	}
	
	@Override
	public Double getExpectedTravelTime() {
		return this.currentLeg.getTravelTime() ;
	}
	
	@Override 
	public String getMode() {
		return this.currentLeg.getMode();
	}
	
	@Override
	public Id getPlannedVehicleId() {
		return ((NetworkRoute)this.currentLeg.getRoute()).getVehicleId() ;
	}


	@Override
	public PlanElement getCurrentPlanElement() {
		return this.currentLeg ; // always a leg (?)
	}

	@Override
	public Id getDestinationLinkId() {
		return this.currentLeg.getRoute().getEndLinkId();
	}

	@Override
	public Departure getDeparture() {
		return this.departure;
	}

	@Override
	public PlanElement getNextPlanElement() {
		throw new UnsupportedOperationException() ;
	}

	@Override
	public Plan getSelectedPlan() {
		return PopulationUtils.unmodifiablePlan(this.getPerson().getSelectedPlan());
	}

}
