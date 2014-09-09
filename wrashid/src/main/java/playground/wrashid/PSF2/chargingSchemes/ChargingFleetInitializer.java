/* *********************************************************************** *
 * project: org.matsim.*
 * DumbScenarioFleetInitializer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.PSF2.chargingSchemes;

import java.util.Iterator;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.PSF2.vehicle.energyStateMaintainance.EnergyStateMaintainer;
import playground.wrashid.PSF2.vehicle.vehicleFleet.FleetInitializer;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;

public class ChargingFleetInitializer implements FleetInitializer {

	@Override
	public LinkedListValueHashMap<Id, Vehicle> getVehicles(Set<Id<Person>> personIds, EnergyStateMaintainer energyStateMaintainer) {
		LinkedListValueHashMap<Id, Vehicle> result = new LinkedListValueHashMap<Id, Vehicle>();

		Iterator<Id<Person>> iter = personIds.iterator();

		while (iter.hasNext()) {
			Id<Person> personId = iter.next();

			PlugInHybridElectricVehicle phev = getInitializedPHEV(energyStateMaintainer);

			result.putAndSetBackPointer(personId, phev);
		}

		return result;
	}

	private PlugInHybridElectricVehicle getInitializedPHEV(EnergyStateMaintainer energyStateMaintainer) {
		PlugInHybridElectricVehicle phev = new PlugInHybridElectricVehicle(energyStateMaintainer, new IdImpl(1));
		double joulesInOneKWH = 3600*1000;
		phev.setBatterySizeInJoule(10 * joulesInOneKWH);
		phev.setBatteryMinThresholdInJoule(phev.getBatterySizeInJoule() * 0.035);
		phev.setCurrentBatteryChargeInJoule(phev.getBatterySizeInJoule());
		return phev;
	}

}
