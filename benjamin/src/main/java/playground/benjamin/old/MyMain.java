/* *********************************************************************** *
 * project: org.matsim.*
 * 
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.benjamin.old;

import org.matsim.core.controler.Controler;
import org.matsim.run.OTFVis;

import playground.benjamin.BkPaths;

public class MyMain {

//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
////		Controler c = new Controler("examples/equil/config.xml");
//		Controler c = new Controler("examples/equil/config.xml");
//		c.setOverwriteFiles(true);
//		c.run();
//		
//		int lastIteration = c.getConfig().controler().getLastIteration();
//		
//		String out = c.getConfig().controler().getOutputDirectory() + "/ITERS/it."+lastIteration+"/Snapshot";
//		
//		String[] visargs = {out};
//		Gbl.reset();
//		NetVis.main(visargs);
//		
//		
//	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

//		String equilExampleConfig = "examples/equil/configOTF.xml";
		String oneRouteNoModeTest = BkPaths.SHAREDSVN + "studies/bkick/oneRouteNoModeTest/config.xml";



//		String config = equilExampleConfig;
		String config = oneRouteNoModeTest;

		
		Controler c = new Controler(config);
		c.setOverwriteFiles(true);
		c.run();
		
		int lastIteration = c.getConfig().controler().getLastIteration();
		
//		String out = c.getConfig().controler().getOutputDirectory() + "/ITERS/it."+lastIteration+"/Snapshot";
		String out = c.getConfig().controler().getOutputDirectory() + "/ITERS/it."+lastIteration+"/"+lastIteration+".otfvis.mvi";
		
		OTFVis.main(new String[] {out});	
	}

	
	
}
