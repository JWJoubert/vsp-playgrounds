/* *********************************************************************** *
 * project: org.matsim.*
 * ModeAvailabilityChecker.java
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

package playground.christoph.evacuation.withinday.replanning.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import playground.christoph.evacuation.mobsim.PassengerDepartureHandler;
import playground.christoph.evacuation.mobsim.VehiclesTracker;

/**
 * Checks whether a car is available for an agent or not.
 * 
 * @author cdobler
 */
public class ModeAvailabilityChecker {

	private final Scenario scenario;
	private final Vehicles vehicles;
	private final VehiclesTracker vehiclesTracker;
	private final WalkSpeedComparator walkSpeedComparator;
	
	public ModeAvailabilityChecker(Scenario scenario, VehiclesTracker vehiclesTracker) {
		this.scenario = scenario;
		this.vehicles = ((ScenarioImpl) scenario).getVehicles();
		this.vehiclesTracker = vehiclesTracker;
		this.walkSpeedComparator = new WalkSpeedComparator();
		
		// initialize walkSpeedComparator
		this.walkSpeedComparator.calcTravelTimes(scenario.getPopulation());
 	}

	// only used when creating a new instance
	private ModeAvailabilityChecker(Scenario scenario, VehiclesTracker vehiclesTracker, 
			WalkSpeedComparator walkSpeedComparator) {
		this.scenario = scenario;
		this.vehicles = ((ScenarioImpl) scenario).getVehicles();
		this.vehiclesTracker = vehiclesTracker;
		this.walkSpeedComparator = walkSpeedComparator;		
	}
	
	public ModeAvailabilityChecker createInstance() {
		return new ModeAvailabilityChecker(scenario, vehiclesTracker, walkSpeedComparator);
	}
	
	/**
	 * Returns true, if the given person has a driving license, otherwise false.
	 * @param personId the Id of the person to check
	 * @return
	 */
	public boolean hasDrivingLicense(Id personId) {
		PersonImpl p = (PersonImpl) this.scenario.getPopulation().getPersons().get(personId);
		return p.hasLicense();
	}
	
	/**
	 * 
	 * @param householdId Id of the household to check
	 * @param facilityId Id of the facility where the household is located
	 * @return
	 */
	public List<Id> getAvailableCars(Id householdId, Id facilityId) {
		Household household = ((ScenarioImpl) scenario).getHouseholds().getHouseholds().get(householdId);
		return getAvailableCars(household, facilityId);
	}
	
	/**
	 * 
	 * @param household household to check
	 * @param facilityId Id of the facility where the household is located
	 * @return
	 */
	public List<Id> getAvailableCars(Household household, Id facilityId) {
		ActivityFacility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(facilityId);
		
		List<Id> vehicles = household.getVehicleIds();
				
		List<Id> availableVehicles = new ArrayList<Id>();
		
		for (Id vehicleId : vehicles) {
			Id carLinkId = this.vehiclesTracker.getParkedVehicles().get(vehicleId);
			if (carLinkId == null) continue;
			else if (carLinkId.equals(facility.getLinkId())) {
				availableVehicles.add(vehicleId);
			}
		}
		
		return availableVehicles;
	}
	
	/**
	 * By default we try to use a car. We can do this, if the previous or the next 
	 * Leg are performed with a car or the agents car is within a reachable distance.
	 * The order is as following:
	 * car is preferred to ride is preferred to pt is preferred to bike if preferred to walk 
	 * 
	 * @param currentActivityIndex index of an activity
	 * @param plan an agents plan plan
	 * @param possibleVehicleId id of a vehicle that the agent might use
	 */
	public String identifyTransportMode(int currentActivityIndex, Plan plan, Id possibleVehicleId) {
		/*
		 * check whether the agent has a car available.
		 */
		Activity currentActivity = (Activity) plan.getPlanElements().get(currentActivityIndex);
		boolean carAvailable = false;

		// Check whether a vehicleId was found.
		if (possibleVehicleId != null) {
			// Check whether the vehicle is currently parked.
			Id linkId = this.vehiclesTracker.getParkedVehicles().get(possibleVehicleId);
			
			if (linkId != null) {
				// Check whether the vehicle is parked at the same link where the agent performs its activity.
				if (linkId.equals(currentActivity.getLinkId())) carAvailable = true;
			}
		}
		if (carAvailable) return TransportMode.car;
		
		/*
		 * Otherwise check for the other modes 
		 */
		boolean hasBike = false;
		boolean hasPt = false;
		boolean hasRide = false;
		
		if (currentActivityIndex > 0) {
			Leg previousLeg = (Leg) plan.getPlanElements().get(currentActivityIndex - 1);
			String transportMode = previousLeg.getMode();
			if (transportMode.equals(TransportMode.bike)) hasBike = true;
			else if (transportMode.equals(TransportMode.pt)) hasPt = true;
			else if (transportMode.equals(TransportMode.ride)) hasRide = true;
		}
		
		if (currentActivityIndex + 1 < plan.getPlanElements().size()) {
			Leg nextLeg = (Leg) plan.getPlanElements().get(currentActivityIndex + 1);
			String transportMode = nextLeg.getMode();
			if (transportMode.equals(TransportMode.bike)) hasBike = true;
			else if (transportMode.equals(TransportMode.pt)) hasPt = true;
			else if (transportMode.equals(TransportMode.ride)) hasRide = true;
		}
		
		if (hasRide) return TransportMode.ride;
		else if (hasPt) return TransportMode.pt;
		else if (hasBike) return TransportMode.bike;
		else return TransportMode.walk;
	}
	
//	/**
//	 * Return a queue containing a households vehicles that are available
//	 * at a given facility ordered by the number of seats, starting with 
//	 * the car with the highest number.
//	 * 
//	 * @param availableVehicles
//	 * @param facilityId
//	 * @return
//	 */
//	public Queue<Vehicle> getAvailableVehicles(Household household, Id facilityId) {
//		List<Id> availableVehicles = this.getAvailableCars(household, facilityId);
//		
//		Queue<Vehicle> queue = new PriorityQueue<Vehicle>(2, new VehicleSeatsComparator());
//		
//		for (Id id : availableVehicles) {
//			Vehicle vehicle = vehicles.getVehicles().get(id);
//			queue.add(vehicle);
//		}
//		
//		return queue;
//	}
		
	private Queue<Vehicle> getVehiclesQueue(List<Id> vehicleIds) {
		Queue<Vehicle> queue = new PriorityQueue<Vehicle>(2, new VehicleSeatsComparator());
		for (Id id : vehicleIds) {
			Vehicle vehicle = vehicles.getVehicles().get(id);
			queue.add(vehicle);
		}
		return queue;
	}
	
	/*
	 * For decisions on household level. 
	 */
	public HouseholdModeAssignment getHouseholdModeAssignment(Household household, Id facilityId) {
		List<Id> availableVehicleIds = this.getAvailableCars(household, facilityId);
		Queue<Vehicle> availableVehicles = getVehiclesQueue(availableVehicleIds);
		return getHouseholdModeAssignment(household.getMemberIds(), availableVehicles, facilityId);
	}
	
	/*
	 * For decisions on non-household or only part-household level.
	 */
	public HouseholdModeAssignment getHouseholdModeAssignment(Collection<Id> personIds, Collection<Id> vehicleIds, Id facilityId) {
		
		Queue<Vehicle> queue = new PriorityQueue<Vehicle>(2, new VehicleSeatsComparator());
		for (Id id : vehicleIds) {
			Vehicle vehicle = vehicles.getVehicles().get(id);
			queue.add(vehicle);
		}
		return this.getHouseholdModeAssignment(personIds, queue, facilityId);
	}
	
	private HouseholdModeAssignment getHouseholdModeAssignment(Collection<Id> personIds, Queue<Vehicle> availableVehicles, Id facilityId) {	
		HouseholdModeAssignment assignment = new HouseholdModeAssignment();
		
		Queue<Id> possibleDrivers = new PriorityQueue<Id>(4, walkSpeedComparator);
		Queue<Id> possiblePassengers = new PriorityQueue<Id>(4, walkSpeedComparator);
		
		// identify potential drivers and passengers
		for (Id personId : personIds) {
			if (this.hasDrivingLicense(personId)) possibleDrivers.add(personId);
			else possiblePassengers.add(personId);					
		}
		
		/*
		 * Fill people into vehicles. Start with largest cars.
		 * Will end if all people are assigned to a vehicle
		 * or no further vehicles or drivers a available.
		 * Remaining agents will walk.
		 */
		while (availableVehicles.peek() != null) {
			Vehicle vehicle = availableVehicles.poll();
			int seats = vehicle.getType().getCapacity().getSeats();
			
			// if no more drivers are available
			if (possibleDrivers.peek() == null) break;
			
			// set transport mode for driver
			Id driverId = possibleDrivers.poll();
			assignment.addTransportModeMapping(driverId, TransportMode.car);
			assignment.addDriverVehicleMapping(driverId, vehicle.getId());
			seats--;
			
			List<Id> passengers = new ArrayList<Id>();
			while (seats > 0) {
				Id passengerId = null;
				if (possiblePassengers.peek() != null) {
					passengerId = possiblePassengers.poll();
				} else if (possibleDrivers.peek() != null) {
					passengerId = possibleDrivers.poll();
				} else {
					break;
				}
				
				passengers.add(passengerId);
				assignment.addTransportModeMapping(passengerId, PassengerDepartureHandler.passengerTransportMode);
				seats--;
			}
			
			// register person as passenger in the vehicle
			for (Id passengerId : passengers) {
				assignment.addPassengerVehicleMapping(passengerId, vehicle.getId());
			}
			
		}
		// if vehicle capacity is exceeded, remaining agents have to walk
		while (possibleDrivers.peek() != null) {
			assignment.addTransportModeMapping(possibleDrivers.poll(), TransportMode.walk);
		}
		while (possiblePassengers.peek() != null) {
			assignment.addTransportModeMapping(possiblePassengers.poll(), TransportMode.walk);
		}
		
		return assignment;
	}
	
	private static class VehicleSeatsComparator implements Comparator<Vehicle>, Serializable, MatsimComparator {

		private static final long serialVersionUID = 1L;
		
		@Override
		public int compare(Vehicle v1, Vehicle v2) {
			
			int seats1 = v1.getType().getCapacity().getSeats();
			int seats2 = v2.getType().getCapacity().getSeats();
			
			if (seats1 > seats2) return 1;
			else if (seats1 < seats2) return -1;
			// both have the same number of seats: order them based on their Id
			else return v1.getId().compareTo(v2.getId());
		}
	}
}
