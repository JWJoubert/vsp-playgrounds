/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.ev;

import java.util.function.DoubleSupplier;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.vsp.ev.data.ElectricVehicle;
import org.matsim.vsp.ev.discharging.AuxEnergyConsumption;
import org.matsim.vsp.ev.discharging.OhdeSlaskiAuxEnergyConsumption;

public class ETaxiAuxConsumptionFactory implements AuxEnergyConsumption.Factory {
	@Inject
	private Fleet fleet;

	private final DoubleSupplier temperatureProvider;
	private final Predicate<Vehicle> isTurnedOnPredicate;

	public ETaxiAuxConsumptionFactory(DoubleSupplier temperatureProvider, Predicate<Vehicle> isTurnedOnPredicate) {
		this.temperatureProvider = temperatureProvider;
		this.isTurnedOnPredicate = isTurnedOnPredicate;
	}

	@Override
	public AuxEnergyConsumption create(ElectricVehicle electricVehicle) {
		Vehicle vehicle = fleet.getVehicles().get(electricVehicle.getId());
		return new OhdeSlaskiAuxEnergyConsumption(electricVehicle, temperatureProvider,
				ev -> isTurnedOnPredicate.test(vehicle));
	}
}
