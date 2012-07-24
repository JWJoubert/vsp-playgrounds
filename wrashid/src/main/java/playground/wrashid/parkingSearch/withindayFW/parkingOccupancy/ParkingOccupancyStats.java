/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.parkingSearch.withindayFW.parkingOccupancy;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingOccupancyBins;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingInfrastructure;

public class ParkingOccupancyStats {

	private int numberOfMaximumParkingCapacitConstraintViolations=0;
	public HashMap<Id, ParkingOccupancyBins> parkingOccupancies = new HashMap<Id, ParkingOccupancyBins>();
	
	public void updateParkingOccupancy(Id parkingFacilityId, Double arrivalTime, Double departureTime, ParkingInfrastructure parkingInfrastructure) {
		if (!parkingOccupancies.containsKey(parkingFacilityId)) {
			parkingOccupancies.put(parkingFacilityId, new ParkingOccupancyBins());
		}

		ParkingOccupancyBins parkingOccupancyBins = parkingOccupancies.get(parkingFacilityId);
		parkingOccupancyBins.inrementParkingOccupancy(arrivalTime, departureTime);
		
		int parkingCapacity = parkingInfrastructure.getFacilityCapacities().get(parkingFacilityId);
		//parkingOccupancyBins.removeBlurErrors(parkingCapacity);
		
		if (parkingOccupancyBins.isMaximumCapacityConstraintViolated(parkingCapacity)){
			DebugLib.emptyFunctionForSettingBreakPoint();
			setNumberOfMaximumParkingCapacitConstraintViolations(getNumberOfMaximumParkingCapacitConstraintViolations() + 1);
			//System.out.println(numberOfMaximumParkingCapacitConstraintViolations);
		}
	}
	
	
	public void writeOutParkingOccupanciesTxt(Controler controler, ParkingInfrastructure parkingInfrastructure) {
		String iterationFilename = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
				"parkingOccupancy.txt");

		ArrayList<String> list = new ArrayList<String>();

		// create header line
		StringBuffer row = new StringBuffer("name\tcapacity");
		
		for (int i = 0; i < 96; i++) {
			row.append("\t");
			row.append("bin-"+i);
		}
		
		list.add(row.toString());
		
		// content
		for (Id parkingFacilityId : parkingOccupancies.keySet()) {

			ParkingOccupancyBins parkingOccupancyBins = parkingOccupancies.get(parkingFacilityId);
			row = new StringBuffer(parkingFacilityId.toString());

			row.append("\t");
			row.append(parkingInfrastructure.getFacilityCapacities().get(parkingFacilityId));
			
			for (int i = 0; i < 96; i++) {
				row.append("\t");
				row.append(parkingOccupancyBins.getOccupancy(i * 900));
			}

			list.add(row.toString());
		}

		GeneralLib.writeList(list, iterationFilename);
	}
	
	public void writeOutParkingOccupancySumPng(Controler controler) {
		String fileName = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
		"parkingOccupancyAllParking.png");
		
		double matrix[][] = new double[96][1];
		String title="parked vehicles at all parkings in the simulation";
		String xLabel="time";
		String yLabel="number of vehicles parked";
		String[] seriesLabels={"all parking occupancy"};
		double[] xValues = new double[96];

		for (int i = 0; i < 96; i++) {
			xValues[i] = i *0.25;
		}
		
		for (Id parkingFacilityId : parkingOccupancies.keySet()) {

			int[] occupancy = parkingOccupancies.get(parkingFacilityId).getOccupancy();
			
			for (int i = 0; i < 96; i++) {
				matrix[i][0] += occupancy[i];
			}
		}
		
		GeneralLib.writeGraphic(fileName, matrix, title, xLabel, yLabel, seriesLabels, xValues);
	}


	public int getNumberOfMaximumParkingCapacitConstraintViolations() {
		return numberOfMaximumParkingCapacitConstraintViolations;
	}


	public void setNumberOfMaximumParkingCapacitConstraintViolations(int numberOfMaximumParkingCapacitConstraintViolations) {
		this.numberOfMaximumParkingCapacitConstraintViolations = numberOfMaximumParkingCapacitConstraintViolations;
	}
}
