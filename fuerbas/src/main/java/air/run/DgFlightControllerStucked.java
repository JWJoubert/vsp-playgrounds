/* *********************************************************************** *
 * project: org.matsim.*
 * DgFlightController2013
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package air.run;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

/**
 * @author dgrether
 * 
 */
public class DgFlightControllerStucked {
	
	private static final Logger log = Logger.getLogger(DgFlightControllerStucked.class);
	
	public static void main(String[] args) {
//		String[] args2 = {"/media/data/work/repos/shared-svn/studies/countries/eu/flight/dg_oag_flight_model_2_runways_3600vph_one_line/air_config.xml"};
		Controler controler = new Controler(args);
		FlightConfigModule flightConfig = new FlightConfigModule(controler.getConfig());
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		controler.setOverwriteFiles(true);
		ControlerListener lis = new SfFlightTimeControlerListener();
		controler.addControlerListener(lis);
		if (flightConfig.doRerouteStuckedPersons()){
			controler.addControlerListener(new FlightStuckedReplanning());
			log.info("Switched on flight stucked replanning");
		}
		controler.run();

	}

}
