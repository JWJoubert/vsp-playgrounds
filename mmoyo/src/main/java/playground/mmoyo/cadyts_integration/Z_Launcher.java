/* *********************************************************************** *
 * project: org.matsim.*
 * Z_Launcher.java
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

/**
 * 
 */
package playground.mmoyo.cadyts_integration;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.config.ConfigUtils;

public class Z_Launcher {

	public static void main(final String[] args) {
		String configFile = null ;
		if (args.length==1){
			configFile = args[0];
		}else{
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
			// the launch root needs to be at a place from where this path is found.  Changing it here by itself will not be enough
			// since the input files mentioned in the config file also rely on "../".  With materialized maven modules, the
			// correct root seems to be ${workspace_loc:playgrounds}, which is not the default.  kai, jul'11
		}

		Config config = null;
		config = ConfigUtils.loadConfig(configFile);

		final Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
	
		//to use cadyts as strategy from code
		//ControlerListener ptBseUCControlerListener = new NewPtBseUCControlerListener();
		//controler.addControlerListener(ptBseUCControlerListener);
		
		//to use cadyts as strategy from file use this in module "strategy" from config File
		//<param name="Module_1" value="playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy.NewPtBsePlanStrategy" />
		
		controler.run();
	}
}
