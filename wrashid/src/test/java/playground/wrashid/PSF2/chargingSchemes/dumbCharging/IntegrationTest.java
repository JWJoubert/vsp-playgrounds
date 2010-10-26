/* *********************************************************************** *
 * project: org.matsim.*
 * IntegrationTest.java
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

package playground.wrashid.PSF2.chargingSchemes.dumbCharging;

import java.util.LinkedList;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF.PSS.PSSControler;
import playground.wrashid.PSF.energy.charging.ChargeLog;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF.lib.PSFGeneralLib;
import playground.wrashid.PSF2.ParametersPSF2;
import playground.wrashid.PSF2.chargingSchemes.dumbCharging.PSSControlerDumbCharging;

public class IntegrationTest extends MatsimTestCase {

	public void testBasic(){
		PSSControler pssControler=new PSSControlerDumbCharging("test/input/playground/wrashid/PSF2/chargingSchemes/dumbCharging/config.xml", null);
		pssControler.runMATSimIterations();
		
		LinkedList<ChargeLog> chargingTimesForAgent255 = ParametersPSF2.chargingTimes.get(new IdImpl(255)).getChargingTimes();
		assertEquals(10*3600*1000.0, chargingTimesForAgent255.getLast().getEndSOC());
		
		LinkedList<ChargeLog> chargingTimesForAgent253 = ParametersPSF2.chargingTimes.get(new IdImpl(253)).getChargingTimes();
		assertEquals(10*3600*1000.0, chargingTimesForAgent253.getFirst() .getEndSOC());
		
	} 
	
}
