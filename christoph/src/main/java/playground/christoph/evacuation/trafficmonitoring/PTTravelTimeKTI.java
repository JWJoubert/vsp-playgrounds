/* *********************************************************************** *
 * project: org.matsim.*
 * PTTravelTimeKTI.java
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

package playground.christoph.evacuation.trafficmonitoring;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import playground.balmermi.world.Layer;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.meisterk.kti.router.KtiPtRoute;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;
import playground.meisterk.kti.router.SwissHaltestelle;

public class PTTravelTimeKTI implements TravelTime {

	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo;
	private final PlansCalcRouteConfigGroup configGroup;
	private final Map<Id, Double> agentSpeedMap;
	private final TravelTime ptTravelTime;

	private ThreadLocal<Double> personSpeed;
	
	public PTTravelTimeKTI(PlansCalcRouteKtiInfo plansCalcRouteKtiInfo, 
			PlansCalcRouteConfigGroup configGroup, Map<Id, Double> agentSpeedMap,
			TravelTime ptTravelTime) {
		this.plansCalcRouteKtiInfo = plansCalcRouteKtiInfo;
		this.configGroup = configGroup;
		this.agentSpeedMap = agentSpeedMap;
		this.ptTravelTime = ptTravelTime;
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		setPerson(person);
		if (this.personSpeed.get() != null) {
			return link.getLength() / this.personSpeed.get();			
		} else return ptTravelTime.getLinkTravelTime(link, time, person, vehicle);
	}

	private void setPerson(Person person) {
		Double personSpeed = agentSpeedMap.get(person.getId());
		if (personSpeed != null) {
			this.personSpeed.set(agentSpeedMap.get(person.getId()));			
		} else {
			/*
			 * After removing PersonalizeTravelTime this should not be 
			 * necessary anymore.
			 */
			/*
			 * If no speed value for the person is set, the agent performs
			 * the PT trip before the evacuation starts. Therefore use
			 * the simple PT travel time calculator.
			 */
//			if (this.personSpeed == null) {
//				this.ptTravelTime.setPerson(person);			
//			}			
		}
		
	}

	public void setPersonSpeed(Id personId, double speed) {
		this.agentSpeedMap.put(personId, speed);
	}
	
	/**
	 * Based on PlansCalcRouteKti (see playground meisterk)
	 * Make use of the Swiss National transport model to find more or less realistic travel times.
	 *
	 * The travel time of a leg with mode 'pt' (meaning pseudo/public transport)
	 * is estimated by a municipality-to-municipality travel time matrix, plus walk-speed access to and egress from the next pt stop.
	 *
	 * @param fromAct
	 * @param toAct
	 * @param depTime
	 * @return travel time
	 */
	public double calcSwissPtTravelTime(final Activity fromAct, final Activity toAct, final double depTime) {

		double travelTime = 0.0;

		SwissHaltestelle fromStop = this.plansCalcRouteKtiInfo.getHaltestellen().getClosestLocation(fromAct.getCoord());
		SwissHaltestelle toStop = this.plansCalcRouteKtiInfo.getHaltestellen().getClosestLocation(toAct.getCoord());

		Layer municipalities = this.plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality");
		final List<? extends BasicLocation> froms = municipalities.getNearestLocations(fromStop.getCoord());
		final List<? extends BasicLocation> tos = municipalities.getNearestLocations(toStop.getCoord());
		BasicLocation fromMunicipality = froms.get(0);
		BasicLocation toMunicipality = tos.get(0);

		KtiPtRoute newRoute = new KtiPtRoute(fromAct.getLinkId(), toAct.getLinkId(), plansCalcRouteKtiInfo, fromStop, fromMunicipality, toMunicipality, toStop);

		double timeInVehicle = newRoute.getInVehicleTime();

		// add time penalty for pseudo dynamic pt travel times
		timeInVehicle = addTimePenalty(timeInVehicle, depTime);

		final double walkAccessEgressDistance = newRoute.calcAccessEgressDistance(fromAct, toAct);
		final double walkAccessEgressTime = walkAccessEgressDistance / configGroup.getWalkSpeed();

		newRoute.setDistance((walkAccessEgressDistance + newRoute.calcInVehicleDistance()));

		travelTime = walkAccessEgressTime + timeInVehicle;

		return travelTime;
	}

//	/*
//	 * The underlying travel time matrix is not time dependent. Therefore
//	 * we have to add some kind of time dependent filter. We assume that there
//	 * is almost no PT available between 1:00 and 5:00.
//	 * 
//	 * We use the following penalty factors, which are linear interpolated
//	 * 0:00-1:00: 1.0-3.0
//	 * 1:00-2:00: 3.0-15.0
//	 * 2:00-4:00: 25.0
//	 * 4:00-5:00: 15.0-3.0
//	 * 5:00-6:00: 3.0-1.0
//	 */
//	private double addTimePenalty(double timeInVehicle, double depTime) {
//		
//		double penaltyFactor;
//		if (depTime > 6*3600) {
//			penaltyFactor = 1.0;
//		}
//		else if (depTime > 5*3600) {
//			double dt = (depTime - 5*3600);
//			penaltyFactor = 1.0 + 2.0*(3600.0-dt)/3600.0; 
//		}
//		else if (depTime > 4*3600) {
//			double dt = (depTime - 4*3600);
//			penaltyFactor = 3.0 + 52.0*(3600.0-dt)/3600.0;
//		}
//		else if (depTime > 2*3600) {
//			penaltyFactor = 55.0;
//		}
//		else if (depTime > 1*3600) {
//			double dt = (depTime - 1*3600);
//			penaltyFactor = 55.0 - 52.0*(3600.0-dt)/3600.0;
//		}
//		else {
//			penaltyFactor =  1.0 + 2.0 * (depTime / 3600.0);
//		}
//
//		return penaltyFactor * timeInVehicle;
//	}
	
	/*
	 * New approach for penalty time: if the person tries to depart to early, we add time
	 * between the scheduled departure and the assumed first PT connection to the travel
	 * time.
	 */
	private double addTimePenalty(double timeInVehicle, double depTime) {

		
		double penaltyFactor = 1.0;
		if (depTime >= EvacuationConfig.evacuationTime) penaltyFactor = EvacuationConfig.ptTravelTimePenaltyFactor;
		
		double penaltyTime;
		if (depTime > 6*3600) {
			penaltyTime = 0.0;
		}
		else if (depTime > 5*3600) {
			penaltyTime =  1800.0;
		}
		else if (depTime > 1*3600) {
			// assume that the first available pt connection departs at 5:30
			penaltyTime = 5.5 * 3600 - depTime;
		}
		else {
			penaltyTime =  1800.0;
		}
		return penaltyTime + timeInVehicle * penaltyFactor;
	}
	
}