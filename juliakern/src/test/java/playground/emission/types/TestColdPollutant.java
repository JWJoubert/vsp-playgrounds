/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * TestHbefaColdEmissionFactorKey.java                                     *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.emission.types;

import org.junit.Assert;
import org.junit.Test;
import playground.vsp.emissions.types.ColdPollutant;

//test for playground.vsp.emissions.types.ColdPollutant

public class TestColdPollutant {
	
	@Test
	public final void testGetValue(){
		//not much to test here since ColdPollutant is an enum
		ColdPollutant cp = null;
		Assert.assertEquals(ColdPollutant.CO, cp.getValue("CO"));
		Assert.assertNotSame(ColdPollutant.FC, cp.getValue("CO"));
		Assert.assertEquals(ColdPollutant.HC, cp.getValue("HC"));
		Assert.assertNotSame(ColdPollutant.getValue("CO"), ColdPollutant.getValue("FC"));

	}

}
