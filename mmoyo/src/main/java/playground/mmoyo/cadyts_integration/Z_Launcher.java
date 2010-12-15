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
package playground.mmoyo.w_ptCounts_from_kai;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.listener.ControlerListener;

public class Z_Launcher {
	private static final Logger log = Logger.getLogger("noname");

	public static void main(final String[] args) {
		String configFile = null ;
		if (args.length==1){
			configFile = args[0];
		}else{
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
		}

		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile(configFile);
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		Controler ptBseUCControler = new Controler( config ) ;
		
		ControlerListener ptBseUCControlerListener = new NewPtBseUCControlerListener();
		ptBseUCControler.addControlerListener(ptBseUCControlerListener);

		ptBseUCControler.setCreateGraphs(false);
		ptBseUCControler.setOverwriteFiles(true);
//		ptBseUCControler.setWriteEventsInterval(5); 
		//ptBseUCControler.setUseOTFVis(false) ;
		ptBseUCControler.run();
		
		// adding a comment at the end
		
	}
}
