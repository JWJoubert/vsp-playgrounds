/* *********************************************************************** *
 * project: org.matsim.*
 * TestControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.ikaddoura.parkAndRide.pR;


import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.pt.PtConstants;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.ikaddoura.parkAndRide.pRscoring.BvgScoringFunctionConfigGroupPR;
import playground.ikaddoura.parkAndRide.pRscoring.BvgScoringFunctionFactoryPR;

/**
 * @author Ihab
 *
 */
public class ParkAndRideMain {
	private static final Logger log = Logger.getLogger(ParkAndRideMain.class);
	
	static String configFile;
	static String prFacilityFile;
	static int prCapacity;
	static int gravity;
	
	static double addPRProb = 0.;
	static int addPRDisable = 0;
	static double changeLocationProb = 0.;
	static int changeLocationDisable = 0;
	static double timeAllocationProb = 0.;
	static int timeAllocationDisable = 0;
	static double removePRProb = 0.;
	static int removePRDisable = 0;
	static double addPRtimeAllocationProb = 0.;
	static int addPRtimeAllocationDisable = 0;
	static double reRouteProb = 0.;
	static int reRouteDisable = 0;
		
	public static void main(String[] args) throws IOException {
				
		if (args.length > 0) {
			configFile = args[0];
			prFacilityFile = args[1];
			prCapacity = Integer.parseInt(args[2]);
			gravity = Integer.parseInt(args[3]);
			
			log.info("configFile: "+ configFile);
			log.info("prFacilityFile: "+ prFacilityFile);
			log.info("prCapacity: "+ prCapacity);
			log.info("gravity: "+ gravity);
			
			if (args.length <= 4) {
				log.warn("No strategies defined.");
			} else {
				for (int arg = 4; arg < args.length ; arg = arg + 3) {
					
					int probIndex = arg+1;
					int disableIndex = arg+2;
					
					System.out.println(probIndex);
					System.out.println(args.length);
					
					if (probIndex >= args.length - 1){
						throw new RuntimeException("Missing strategy specifications. Aborting...");
					}
					
					String name = args[arg];
					
					if (name.equals("addPR")){
						addPRProb = Double.parseDouble(args[probIndex]);
						addPRDisable = Integer.parseInt(args[disableIndex]);
						log.info("strategy: "+ name);
						log.info("addPRProb: "+ addPRProb);
						log.info("addPRDisable: "+ addPRDisable);
					} else if (name.equals("changeLocation")) {
						changeLocationProb = Double.parseDouble(args[probIndex]);
						changeLocationDisable = Integer.parseInt(args[disableIndex]);
						log.info("strategy: "+ name);
						log.info("changeLocationProb: "+ changeLocationProb);
						log.info("changeLocationDisable: "+ changeLocationDisable);
					} else if (name.equals("timeAllocation")) {
						timeAllocationProb = Double.parseDouble(args[probIndex]);
						timeAllocationDisable = Integer.parseInt(args[disableIndex]);
						log.info("strategy: "+ name);
						log.info("timeAllocationProb: "+ timeAllocationProb);
						log.info("timeAllocationDisable: "+ timeAllocationDisable);
					} else if (name.equals("removePR")) {
						removePRProb = Double.parseDouble(args[probIndex]);
						removePRDisable = Integer.parseInt(args[disableIndex]);
						log.info("strategy: "+ name);
						log.info("removePRProb: "+ removePRProb);
						log.info("removePRDisable: "+ removePRDisable);
					} else if (name.equals("addPRtimeAllocation")) {
						addPRtimeAllocationProb = Double.parseDouble(args[probIndex]);
						addPRtimeAllocationDisable = Integer.parseInt(args[disableIndex]);
						log.info("strategy: "+ name);
						log.info("addPRtimeAllocationProb: "+ addPRtimeAllocationProb);
						log.info("addPRtimeAllocationDisable: "+ addPRtimeAllocationDisable);
					} else if (name.equals("reRoute")) {
						reRouteProb = Double.parseDouble(args[probIndex]);
						reRouteDisable = Integer.parseInt(args[disableIndex]);
						log.info("strategy: "+ name);
						log.info("reRouteProb: "+ reRouteProb);
						log.info("reRouteDisable: "+ reRouteDisable);
					} else {
						throw new RuntimeException("Strategy " + name + " unknown. Aborting...");
					}
				}
			}
		} else {
			
			configFile = "../../shared-svn/studies/ihab/parkAndRide/inputBerlinTest/berlinConfigTEST.xml";
			prFacilityFile = "../../shared-svn/studies/ihab/parkAndRide/inputBerlinTest/PRfacilities_berlin.txt";
			prCapacity = 100;
			gravity = 2;
			
			addPRProb = 0.;
			addPRDisable = 0;
			
			changeLocationProb = 0.;
			changeLocationDisable = 0;
			
			timeAllocationProb = 0.;
			timeAllocationDisable = 0;
			
			addPRtimeAllocationProb = 100.;
			addPRtimeAllocationDisable = 500;
			
			reRouteProb = 100.;
			reRouteDisable = 500;
		}
		
		ParkAndRideMain main = new ParkAndRideMain();
		main.run();
	}
	
	private void run() {
		
		Controler controler = new Controler(configFile);
		controler.setOverwriteFiles(true);
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		
		PRFileReader prReader = new PRFileReader(prFacilityFile);
		Map<Id, ParkAndRideFacility> id2prFacility = prReader.getId2prFacility();

		final AdaptiveCapacityControl adaptiveControl = new AdaptiveCapacityControl(id2prFacility, prCapacity);
				
		ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);
		controler.getConfig().planCalcScore().addActivityParams(transitActivityParams);
		
		controler.setScoringFunctionFactory(new BvgScoringFunctionFactoryPR(controler.getConfig().planCalcScore(), new BvgScoringFunctionConfigGroupPR(controler.getConfig()), controler.getNetwork()));

		ParkAndRideControlerListener prControlerListener = new ParkAndRideControlerListener(controler, adaptiveControl, id2prFacility, gravity);
		prControlerListener.setAddPRProb(addPRProb);
		prControlerListener.setAddPRDisable(addPRDisable);
		prControlerListener.setChangeLocationProb(changeLocationProb);
		prControlerListener.setChangeLocationDisable(changeLocationDisable);
		prControlerListener.setRemovePRProb(removePRProb);
		prControlerListener.setRemovePRDisable(removePRDisable);
		prControlerListener.setReRouteProb(reRouteProb);
		prControlerListener.setReRouteDisable(reRouteDisable);
		prControlerListener.setTimeAllocationProb(timeAllocationProb);
		prControlerListener.setTimeAllocationDisable(timeAllocationDisable);
		prControlerListener.setAddPRtimeAllocationProb(addPRtimeAllocationProb);
		prControlerListener.setAddPRtimeAllocationDisable(addPRtimeAllocationDisable);
		controler.addControlerListener(prControlerListener);
		
		final MobsimFactory mf = new QSimFactory();
		controler.setMobsimFactory(new MobsimFactory() {
			private QSim mobsim;

			@Override
			public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
				mobsim = (QSim) mf.createMobsim(sc, eventsManager);
				mobsim.addMobsimEngine(adaptiveControl);
				return mobsim;
			}
		});
			
		controler.run();
	}
}
	
