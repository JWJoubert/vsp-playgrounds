/* *********************************************************************** *
 * project: org.matsim.*
 * Controller.java
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
package playground.vsptelematics.ha1;



import org.matsim.core.controler.Controler;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import playground.vsptelematics.common.IncidentGenerator;

/**
 * @author illenberger
 *
 */
public class Controller extends Controler {
	
	public Controller(String[] args) {
		super(args);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		String[] args2 = {"/media/data/work/repos/lehre-svn/lehrveranstaltungen/L058_methoden_telematik/2012_ss/uebungen/data/ha1/scenario/config.xml"};
//		args = args2;
		Controller c = new Controller(args);
		c.setOverwriteFiles(true);
		c.setCreateGraphs(false);
		addListener(c);
		c.run();
	}
	
	
	@Override
	protected void loadCoreListeners() {

//		this.addCoreControlerListener(new CoreControlerListener());

		this.addCoreControlerListener(new PlansReplanning());
		this.addCoreControlerListener(new PlansDumping());

		this.addCoreControlerListener(new EventsHandling(this.events)); // must be last being added (=first being executed)
	}
	

	private static void addListener(Controler c){
		c.addControlerListener(new StartupListener(){
			@Override
			public void notifyStartup(StartupEvent event) {
				Controler con = event.getControler();
				double alpha = con.getConfig().planCalcScore().getLearningRate();
				final RouteTTObserver observer = new RouteTTObserver(con.getControlerIO().getOutputFilename("routeTravelTimes.txt"));
				con.addControlerListener(observer);
				con.getEvents().addHandler(observer);
				
				con.addControlerListener(new Scorer(observer));
				
				if (con.getScenario().getConfig().network().isTimeVariantNetwork()){
					IncidentGenerator generator = new IncidentGenerator(con.getScenario().getConfig().getParam("telematics", "incidentsFile"), con.getScenario().getNetwork());
					con.addControlerListener(generator);
				}
			}});
	}
}
