/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.avparking;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;

/**
 * @author  jbischoff
 *
 */


public class PrivateAVFleetGenerator implements Fleet, BeforeMobsimListener {

	private Map<Id<Vehicle>,Vehicle> vehiclesForIteration;

	private final Population population;
	
	private String mode;

	private Network network;
	
	@Inject
	ParkingSearchManager manager;
	/**
	 * 
	 */
	public PrivateAVFleetGenerator(Scenario scenario) {
		mode = DvrpConfigGroup.get(scenario.getConfig()).getMode();
		this.population= scenario.getPopulation();
		this.network = scenario.getNetwork();
//		this.manager = manager;
		vehiclesForIteration = new HashMap<>();
	}
	
	
	@Override
	public Map<Id<Vehicle>, ? extends Vehicle> getVehicles() {
		return vehiclesForIteration;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.IterationStartsListener#notifyIterationStarts(org.matsim.core.controler.events.IterationStartsEvent)
	 */
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Map<Id<Vehicle>,Vehicle> lastIterationVehicles = new HashMap<>();
		lastIterationVehicles.putAll(vehiclesForIteration);
		vehiclesForIteration.clear();
		for (Person p : population.getPersons().values()){
			
			Id<Link> vehicleStartLink = null;
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Leg){
					if (((Leg) pe).getMode().equals(mode)){
						vehicleStartLink = ((Leg) pe).getRoute().getStartLinkId();
						break;
					}
				}
			}
			if (vehicleStartLink!=null){
				Id<Vehicle> vehicleId = Id.create(p.getId().toString()+"_av", Vehicle.class);
				if (lastIterationVehicles.containsKey(vehicleId)){
					Id<Link> lastParkingPosition = manager.getVehicleParkingLocation(Id.createVehicleId(vehicleId));
					if (lastParkingPosition!=null){
						vehicleStartLink = lastParkingPosition;
						lastIterationVehicles.remove(vehicleId);
					}
				}
				Vehicle veh = new VehicleImpl(vehicleId, network.getLinks().get(vehicleStartLink), 4, 0, 30*3600);
				vehiclesForIteration.put(veh.getId(), veh);
			}
		}
		for (Id<Vehicle> vid : lastIterationVehicles.keySet()){
			Id<org.matsim.vehicles.Vehicle> vehicleId = Id.createVehicleId(vid);
			Id<Link> parkLink = manager.getVehicleParkingLocation(vehicleId);
			if (parkLink!=null){
				manager.unParkVehicleHere(vehicleId, parkLink, 30*3600);
				Logger.getLogger(getClass()).info("Removing AV: "+vid);
			}
		}
	}


	/* (non-Javadoc)
	 * @see org.matsim.contrib.dvrp.data.Fleet#resetSchedules()
	 */
	@Override
	public void resetSchedules() {
		// TODO Auto-generated method stub
		
	}

}
