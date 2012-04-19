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

package playground.wrashid.parkingChoice;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.parkingChoice.api.ReservedParkingManager;
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.ReservedParking;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;

public class ReservedParkingTest extends MatsimTestCase {

	public void testBaseTestCase(){
		ReservedParkingManager reservedParkingManager = new ReservedParkingManager() {

			@Override
			public boolean considerForChoiceSet(ReservedParking reservedParking, Id personId, double OPTIONALtimeOfDayInSeconds,
					ActInfo targetActInfo) {
				if (personId.equals(new IdImpl(1)) && reservedParking.getAttributes().contains("EV")) {
					return true;
				}
				return false;
			}
		};
		
		assertEquals(2744, walkingDistanceFor3CarScenario(reservedParkingManager,1),5.0);
	}
	
	public void testCaseWithHigherParkingCapacityAllAgentsAllowedToUseReservedParking(){
		ReservedParkingManager reservedParkingManager = new ReservedParkingManager() {

			@Override
			public boolean considerForChoiceSet(ReservedParking reservedParking, Id personId, double OPTIONALtimeOfDayInSeconds,
					ActInfo targetActInfo) {
				if (personId.equals(new IdImpl(1)) && reservedParking.getAttributes().contains("EV")) {
					return true;
				}
				if (personId.equals(new IdImpl(2)) && reservedParking.getAttributes().contains("disabled")) {
					return true;
				}
				if (personId.equals(new IdImpl(3)) && reservedParking.getAttributes().contains("EV")) {
					return true;
				}
				return false;
			}
		};
		
		assertEquals(998, walkingDistanceFor3CarScenario(reservedParkingManager,10),5.0);
	}
	
	
	private double walkingDistanceFor3CarScenario(ReservedParkingManager reservedParkingManager, int parkingCapacity) {
		ParkingChoiceLib.isTestCaseRun=true;
		Config config = super.loadConfig("test/input/playground/wrashid/parkingChoice/chessConfig.xml");
		Controler controler = new Controler(config);

		// setup parking infrastructure
		LinkedList<Parking> parkingCollection = new LinkedList<Parking>();

		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				ParkingImpl parking = new ParkingImpl(new CoordImpl(i * 1000 + 500, j * 1000 + 500));
				parking.setMaxCapacity(parkingCapacity);
				parkingCollection.add(parking);
			}
		}

		ReservedParking reservedParking = new ReservedParking(new CoordImpl(8500.0, 9000), "disabled, EV");
		reservedParking.setMaxCapacity(parkingCapacity);
		parkingCollection.add(reservedParking);

		ParkingModule parkingModule = new ParkingModule(controler, parkingCollection);

		parkingModule.setReservedParkingManager(reservedParkingManager);

		controler.setOverwriteFiles(true);

		controler.run();

		return parkingModule.getAverageWalkingDistance();
	}
	
	
	
	

}
