/* *********************************************************************** *
 * project: org.matsim.*
 * RunOTFVisDebugRandomizedTransitRouter
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
package playground.vsp.randomizedtransitrouter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;


/**
 * @author dgrether
 *
 */
public class RunOTFVisDebugRandomizedTransitRouterTravelTimeAndDisutility {

	
	public static void main(String[] args) {
		final Config config = ConfigUtils.loadConfig(args[0]) ;

		boolean doVisualization = true;

		config.planCalcScore().setWriteExperiencedPlans(true) ;

		config.otfVis().setDrawTransitFacilities(true) ; // this DOES work
		config.otfVis().setDrawTransitFacilityIds(false);
		config.otfVis().setShowTeleportedAgents(true) ;
		config.otfVis().setDrawNonMovingItems(true);
		config.otfVis().setScaleQuadTreeRect(true);

		final Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		final Controler ctrl = new Controler(scenario) ;
		
		ctrl.setOverwriteFiles(true) ;
		
		ctrl.addControlerListener(new RandomizedTransitRouterTravelTimeAndDisutilityControlerListener());
		
		if (doVisualization){
		ctrl.setMobsimFactory(new MobsimFactory(){

			@Override
			public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
				QSim qSim = (QSim) new QSimFactory().createMobsim(sc, eventsManager) ;
				
				OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, eventsManager, qSim);
				OTFClientLive.run(sc.getConfig(), server);
				
				return qSim ;
			}}) ;
		}
		ctrl.run() ;
		
	}
}
