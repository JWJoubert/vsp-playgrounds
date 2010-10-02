/* *********************************************************************** *
 * project: org.matsim.*
 * ARTEMISEnergyStateMaintainer.java
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

package playground.wrashid.PSF.vehicle.energyStateMaintainance;

import org.matsim.api.core.v01.network.Link;

import playground.wrashid.PSF.vehicle.energyConsumption.EnergyConsumptionTable;
import playground.wrashid.PSF.vehicle.vehicleFleet.ConventionalVehicle;
import playground.wrashid.PSF.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.GeneralLib;

public class ARTEMISEnergyStateMaintainer_StartChargingUponArrival extends EnergyStateMaintainer {

	
	
	public ARTEMISEnergyStateMaintainer_StartChargingUponArrival(EnergyConsumptionTable energyConsumptionTable) {
		super(energyConsumptionTable);
	}

	@Override
	public void processVehicleEnergyState(Vehicle vehicle, double timeSpendOnLink, Link link) {
		double energyConsumptionOnLinkInJoule=energyConsumptionTable.getEnergyConsumptionInJoule(vehicle.getVehicleClassId(),vehicle.getAverageSpeedOfVehicleOnLink(timeSpendOnLink, link),link.getFreespeed(),link.getLength());
		
		vehicle.updateEnergyState(energyConsumptionOnLinkInJoule);
	}
	
	
	
	public void chargeVehicle(Vehicle vehicle,double arrivalTime, double departureTime, double plugSizeInWatt){
		if (vehicle instanceof PlugInHybridElectricVehicle){
			PlugInHybridElectricVehicle phev=(PlugInHybridElectricVehicle) vehicle;
			
			double maxChargingAvailableInJoule=GeneralLib.getIntervalDuration(arrivalTime, departureTime)*plugSizeInWatt;
			
			if (phev.getRequiredBatteryCharge()>0){
				if (phev.getRequiredBatteryCharge()<maxChargingAvailableInJoule){
					//phev.processCharging();
					
					
					// TODO: cont.
					// log, when charging started and ended
					// update phev state => perform also consitency check there...
				}
			}
			
			
		}
	}
	
	
}
