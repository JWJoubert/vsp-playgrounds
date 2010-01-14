/* *********************************************************************** *
 * project: org.matsim.*
 * RunMyCommercialDemandGenerator01.java
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

package playground.jjoubert.CommercialDemandGenerator;

public class RunMyCommercialDemandGenerator01 {

	/**
	 * This is the old approach (first run at ETH). It will be replaced to run separate 
	 * demand generators for 'within' and 'through' traffic.
	 * @param args
	 */
	public static void main(String[] args) {
		/*===========================================================
		 * Variables that must be set:
		 */
		String studyArea = "Temp";
		String root = "/Users/johanwjoubert/MATSim/workspace/MATSimData/";

		/*
		 * This is where multiple sa,ples must be 'driven'.
		 */
		
		
		MyCommercialDemandGenerator01 mcdg = new MyCommercialDemandGenerator01(root, studyArea, 0.9);
		mcdg.buildVehicleLists();
		
		
		mcdg.createPlans();

	}
	
	

}
