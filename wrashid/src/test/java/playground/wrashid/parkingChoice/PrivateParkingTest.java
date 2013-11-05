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

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;

public class PrivateParkingTest extends MatsimTestCase {

	public void testBaseCase(){
	//	assertEquals(2744, walkingDistanceFor3CarScenarioWithVariableParkingCapacity(1),5.0);
	}
	
	public void testHigherParkingCapacityMakesWalkingDistanceShorter(){
	//	assertEquals(998, walkingDistanceFor3CarScenarioWithVariableParkingCapacity(3),5.0);
	}
	
	public void testMakingTheCapacityHigherThanNumberOfCarsWillNotMakeWalkingDistanceShorter(){
	//	assertEquals(998, walkingDistanceFor3CarScenarioWithVariableParkingCapacity(10),5.0);
	}
	
	private double walkingDistanceFor3CarScenarioWithVariableParkingCapacity(int parkingCapacity) {
		ParkingChoiceLib.isTestCaseRun=true;
		Config config= super.loadConfig("test/input/playground/wrashid/parkingChoice/chessConfig.xml");
		Controler controler=new Controler(config);
		
		// setup parking infrastructure
		LinkedList<Parking> parkingCollection = new LinkedList<Parking>();

		for (int i=0;i<10;i++){
			for (int j=0;j<10;j++){
				ParkingImpl parking = new ParkingImpl(new CoordImpl(i*1000+500,j*1000+500));
				parking.setMaxCapacity(parkingCapacity);
				parkingCollection.add(parking);
			}
		}
			 
		PrivateParking privateParking=new PrivateParking(new CoordImpl(8500.0,9000),new ActInfo(new IdImpl(36), "work"));
		privateParking.setMaxCapacity(parkingCapacity);
		parkingCollection.add(privateParking);
		
		
		ParkingModule parkingModule = new ParkingModule(controler,parkingCollection);
		
		controler.setOverwriteFiles(true);
		
		controler.run();
		
		return parkingModule.getAverageWalkingDistance();
	}
	
	public void testChagingTheActTypeOfPrivateParkingShouldLeadToLongerWalkingDistances(){
		Config config= super.loadConfig("test/input/playground/wrashid/parkingChoice/chessConfig.xml");
		Controler controler=new Controler(config);
		
		// setup parking infrastructure
		LinkedList<Parking> parkingCollection = new LinkedList<Parking>();

		for (int i=0;i<10;i++){
			for (int j=0;j<10;j++){
				ParkingImpl parking = new ParkingImpl(new CoordImpl(i*1000+500,j*1000+500));
				parking.setMaxCapacity(1);
				parkingCollection.add(parking);
			}
		}
			 
		PrivateParking privateParking=new PrivateParking(new CoordImpl(8500.0,9000),new ActInfo(new IdImpl(36), "home"));
		privateParking.setMaxCapacity(1);
		parkingCollection.add(privateParking);
		
		
		ParkingModule parkingModule = new ParkingModule(controler,parkingCollection);
		
		controler.setOverwriteFiles(true);
		
		controler.run();
		
	//	assertEquals(3489,parkingModule.getAverageWalkingDistance(),5.0);
	}
	
}
