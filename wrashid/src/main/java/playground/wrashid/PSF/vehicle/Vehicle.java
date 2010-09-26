/* *********************************************************************** *
 * project: org.matsim.*
 * Vehicle.java
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

package playground.wrashid.PSF.vehicle;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class Vehicle {

	private final EnergyStateMaintainer energyStateMaintainer;
	private Id vehicleId;
	private Id vehicleClassId;

	public Vehicle(EnergyStateMaintainer energyStateMaintainer, Id vehicleId, Id vehicleClassId){
		this.energyStateMaintainer = energyStateMaintainer;
		this.vehicleId=vehicleId;
		this.vehicleClassId=vehicleClassId;
	}
	
	public void updateEnergyState(double timeSpendOnLink, Link link){
		energyStateMaintainer.processVehicleEnergyState(this, timeSpendOnLink, link);
	}

	public Id getVehicleId() {
		return vehicleId;
	}

	public Id getVehicleClassId() {
		return vehicleClassId;
	}
}
