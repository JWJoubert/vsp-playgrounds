/* *********************************************************************** *
 * project: org.matsim.*
 * ReadFromUrbansimParcelModelTest.java
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

/**
 * 
 */
package playground.tnicolai.matsim4opus.matsim;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;

import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.utils.io.ReadFromUrbanSimModel;
import playground.tnicolai.matsim4opus.utils.io.ReadFromUrbanSimModel.PopulationCounter;
import playground.tnicolai.matsim4opus.utils.io.TempDirectoryUtil;


/**
 * @author thomas
 *
 */
public class ReadFromUrbansimParcelModelTest extends MatsimTestCase{

	private static final Logger log = Logger.getLogger(ReadFromUrbansimParcelModelTest.class);
	private int popSize = -1;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	// Tests if facilities are created correctly
	public void testReadFromUrbansimParcelModel(){
		log.info("Starting testReadFromUrbansimParcelModel1 run.");
		prepareTest("testReadFromUrbansimParcelModel1", 1);		
	}
	
	@Test
	// Tests if new population is created correctly
	public void testReadFromUrbansimParcelModel2(){
		log.info("Starting testReadFromUrbansimParcelModel2 run.");
		prepareTest("testReadFromUrbansimParcelModel2", 2);	
	}
	
	/**
	 * preparing ReadFromUrbansimParcelModel test run
	 * @param testRunName name of current test case
	 */
	private void prepareTest(String testRunName, int testNr){
		
		// create temp directories
		TempDirectoryUtil.createOPUSDirectories();
		boolean result = false;
		
		// preparing input test file
		int dummyYear = 2000;
		String testFileDirectory = InternalConstants.MATSIM_4_OPUS_TEMP;
		String testFileName = null;
		
		switch (testNr) {
		case 1:
			log.info("Testing computation of UrbanSim zones");
			// create parcel table
			testFileName = InternalConstants.URBANSIM_PARCEL_DATASET_TABLE + dummyYear + InternalConstants.FILE_TYPE_TAB;
			createParcelInputTestFile(testFileDirectory, testFileName);
			// running ReadFromUrbansimParcelModel generating zones and facilities
			ActivityFacilitiesImpl zones = testRunPracels( dummyYear );
			result = validateResult(zones);
			Assert.assertTrue( result );
			log.info("Testing computation of UrbanSim zones finished");
			break;

		case 2:
			log.info("Testing construction of MATSim agents based on UrbanSim output");
			// create parcel table
			testFileName = InternalConstants.URBANSIM_PARCEL_DATASET_TABLE + dummyYear + InternalConstants.FILE_TYPE_TAB;
			createParcelInputTestFile(testFileDirectory, testFileName);
			// create person table
			testFileName = InternalConstants.URBANSIM_PERSON_DATASET_TABLE + dummyYear + InternalConstants.FILE_TYPE_TAB;
			createPersonInputTestFile(testFileDirectory, testFileName);
			// running ReadFromUrbansimParcelModel generating population
			result = testRunPopulation( dummyYear, false );
			Assert.assertTrue(result);
			log.info("Testing construction of MATSim agents finished");
			break;
		}
		
		// remove temp directories
		TempDirectoryUtil.cleaningUpOPUSDirectories();
		log.info("End of " + testRunName + ".");
	}
	
	/**
	 * validating test result
	 * @param zones ActivityFacilitiesImpl result of test run
	 * @return true if validation successful, false otherwise
	 */
	private boolean validateResult(ActivityFacilitiesImpl zones){
		
		Id zone_ID;
		ActivityFacility af;
		Coord coord;
		
		boolean zone1 = false;
		boolean zone2 = false; 
		boolean zone3 = false; 
		boolean zone4 = false;
		
		for( Entry<Id, ActivityFacility> entry : (zones.getFacilities()).entrySet() ){
			zone_ID = entry.getKey();
			af = entry.getValue();
			coord = af.getCoord();

			if(zone_ID.equals(new IdImpl("1")))
				zone1 = (coord.getX() == 50) && (coord.getY() == 150);
			else if(zone_ID.equals(new IdImpl("2")))
				zone2 = (coord.getX() == 150) && (coord.getY() == 150);
			else if(zone_ID.equals(new IdImpl("3")))
				zone3 = (coord.getX() == 50) && (coord.getY() == 50);
			else if(zone_ID.equals(new IdImpl("4")))
				zone4 = (coord.getX() == 150) && (coord.getY() == 50);
				
		}
		return zone1 && zone2 && zone3 && zone4;
	}
	
	/**
	 * Runns ReadFromUrbansimParcelModel
	 * @param year dummy year only needed to initialize ReadFromUrbansimParcelModel
	 * @return ActivityFacilitiesImpl constructed zones
	 */
	private ActivityFacilitiesImpl testRunPracels(int year){
		log.info("Running ReadFromUrbansimParcelModel with argument year = " + year);
		// get the data from test urbansim parcels
		ReadFromUrbanSimModel readFromUrbansim = new ReadFromUrbanSimModel( year );
		// read urbansim facilities (these are simply those entities that have the coordinates!)
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
		ActivityFacilitiesImpl zones      = new ActivityFacilitiesImpl("urbansim zones");
		readFromUrbansim.readFacilitiesParcel(facilities, zones);
		// return constructed zones for validation
		return zones;
	}
	
	/**
	 * Runs ReadFromUrbansimParcelModel 
	 * @param year dummy year only needed to initialize ReadFromUrbansimParcelModel
	 * @return returns true if number of generated population aproximatly equals 
	 * 		   "sample rate * number of urbansim persons".
	 */
	private boolean testRunPopulation(int year, boolean enableOldPopulation){
		
		log.info("Running ReadFromUrbansimParcelModel with argument year = " + year);
		
		double sampleRate = 1.0;
		boolean result = false;
		
		// get the data from urbansim (parcels and persons)
		ReadFromUrbanSimModel readFromUrbansim = new ReadFromUrbanSimModel( year );

		// read urbansim facilities (these are simply those entities that have the coordinates!)
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl("dummy locations");
		ActivityFacilitiesImpl zones      = new ActivityFacilitiesImpl("dummy zones");
		readFromUrbansim.readFacilitiesParcel(facilities, zones);
		
		// read urbansim persons. Generates hwh acts as side effect
		Population newPop = readFromUrbansim.readPersonsParcel( null, facilities, null, sampleRate );
		PopulationCounter pCounter = readFromUrbansim.getPopulationCounter();
		// determine result
		result = (pCounter.numberOfUrbanSimPersons == this.popSize) && (newPop.getPersons().size() == this.popSize);
		return result;
	}
	
	/**
	 * creates person table
	 * @param directory points to the directory were to store the test input file
	 * @param fileName name of the test input file
	 * @return boolean indicates whether the creation of the test input file was successful
	 */
	private boolean createPersonInputTestFile(String directory, String fileName){
		
		int personID = 1;
		int home_ID = 1;
		int work_ID = 0;
		
		// create header line
		StringBuffer testFileContent = new StringBuffer();
		
		// create header line
		testFileContent.append( InternalConstants.PERSON_ID + InternalConstants.TAB + 
								InternalConstants.PARCEL_ID_HOME + InternalConstants.TAB + 
								InternalConstants.PARCEL_ID_WORK + InternalConstants.NEW_LINE);
		
		// build person table >> person_id	parcel_id_home	parcel_id_work
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID++ + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		testFileContent.append( personID + InternalConstants.TAB + home_ID + InternalConstants.TAB + getNextWorkID(work_ID++) + InternalConstants.NEW_LINE);
		
		this.popSize = personID;
		
		return testFileWiter(directory, fileName, testFileContent);
	}
	
	/**
	 * returns id's from 1 - 11 via modulo operation
	 * @param workID
	 * @return
	 */
	private int getNextWorkID(int workID){
		
		return (workID % 11)+1;
	}
	
	/**
	 * create a test parcel data set table 
	 * @param directory points to the directory were to store the test input file
	 * @param fileName name of the test input file
	 * @return boolean indicates weather the creation of the test input file was successful
	 */
	private boolean createParcelInputTestFile(String directory, String fileName){
		
		// test file will contain four zones:
		// zone1 will have 4 facilities located at coordinates
		//		fac1: 0,200, fac2: 100,200, fac3: 0,100 and fac4: 100,100
		// 		expected center of zone1 is 50,150
		// zone2 will have 2 facilities located at coordinates
		//		fac1: 200,200 and fac2: 100,100
		// 		expected center of zone2 is 150,150
		// zone3 will have 4 facilities located at coordinates
		//		fac1: 50,100, fac2: 100,50, fac3: 50,0 and fac4: 0,50
		// 		expected center of zone3 is 50,50
		// zone4 will have 1 facility located at coordinate
		//		fac1: 150,50
		// 		expected center of zone4 is 150,50
		
		StringBuffer testFileContent = new StringBuffer();
		
		// create header line
		testFileContent.append( InternalConstants.PARCEL_ID + InternalConstants.TAB + 
								InternalConstants.X_COORDINATE_SP + InternalConstants.TAB + 
								InternalConstants.Y_COORDINATE_SP + InternalConstants.TAB + 
								InternalConstants.ZONE_ID + InternalConstants.NEW_LINE);
		
		// put in zone 1 >> pracel_id	x_coordinate	y_coordinate	zone_id
		testFileContent.append( "1" + InternalConstants.TAB + "0" + InternalConstants.TAB + "200" + InternalConstants.TAB + "1" + InternalConstants.NEW_LINE);
		testFileContent.append( "2" + InternalConstants.TAB + "100" + InternalConstants.TAB + "200" + InternalConstants.TAB + "1" + InternalConstants.NEW_LINE);
		testFileContent.append( "3" + InternalConstants.TAB + "0" + InternalConstants.TAB + "100" + InternalConstants.TAB + "1" + InternalConstants.NEW_LINE);
		testFileContent.append( "4" + InternalConstants.TAB + "100" + InternalConstants.TAB + "100" + InternalConstants.TAB + "1" + InternalConstants.NEW_LINE);
		
		// put in zone 2 >> pracel_id	x_coordinate	y_coordinate	zone_id
		testFileContent.append( "5" + InternalConstants.TAB + "200" + InternalConstants.TAB + "200" + InternalConstants.TAB + "2" + InternalConstants.NEW_LINE);
		testFileContent.append( "6" + InternalConstants.TAB + "100" + InternalConstants.TAB + "100" + InternalConstants.TAB + "2" + InternalConstants.NEW_LINE);
	
		// put in zone 3 >> pracel_id	x_coordinate	y_coordinate	zone_id
		testFileContent.append( "7" + InternalConstants.TAB + "50" + InternalConstants.TAB + "100" + InternalConstants.TAB + "3" + InternalConstants.NEW_LINE);
		testFileContent.append( "8" + InternalConstants.TAB + "100" + InternalConstants.TAB + "50" + InternalConstants.TAB + "3" + InternalConstants.NEW_LINE);
		testFileContent.append( "9" + InternalConstants.TAB + "50" + InternalConstants.TAB + "0" + InternalConstants.TAB + "3" + InternalConstants.NEW_LINE);
		testFileContent.append( "10" + InternalConstants.TAB + "0" + InternalConstants.TAB + "50" + InternalConstants.TAB + "3" + InternalConstants.NEW_LINE);
		
		// put in zone 4 >> pracel_id	x_coordinate	y_coordinate	zone_id
		testFileContent.append( "11" + InternalConstants.TAB + "150" + InternalConstants.TAB + "50" + InternalConstants.TAB + "4" + InternalConstants.NEW_LINE);
		
		return testFileWiter(directory, fileName, testFileContent);
	}
	
	/**
	 * writes the content of a created test file into a file
	 * @param directory
	 * @param fileName
	 * @param testFileContent
	 * @return true if successful, false otherwise
	 */
	private boolean testFileWiter(String directory, String fileName, StringBuffer testFileContent){
		
		FileWriter fileStream;
		BufferedWriter bufferedOutput;
		// write test input file into temp directory
		try{
			fileStream = new FileWriter(new File(directory, fileName));
			bufferedOutput = new BufferedWriter(fileStream);
			
			log.info("Created test input file:");
			log.info( InternalConstants.NEW_LINE + testFileContent.toString() );
			
			bufferedOutput.write( testFileContent.toString() );
			bufferedOutput.flush();
			bufferedOutput.close();
			fileStream.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
		return true;
		
	}
}

