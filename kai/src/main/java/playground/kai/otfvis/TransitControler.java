/* *********************************************************************** *
 * project: org.matsim.*
 * PtControler.java
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

package playground.kai.otfvis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.OTFVisConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFFileWriterFactory;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.kai.usecases.randomizedptrouter.RandomizedTransitRouterTravelTimeAndDisutility3;
import playground.kai.usecases.randomizedptrouter.RandomizedTransitRouterTravelTimeAndDisutility3.DataCollection;

public class TransitControler {
	
	private static boolean useTransit = true ;
	private static boolean useOTFVis = true ;

	public static void main(final String[] args) {
		//		args[0] = "/Users/nagel/kw/rotterdam/config.xml" ;
		if ( args.length > 1 ) {
			useOTFVis = Boolean.parseBoolean(args[1]) ;
		}
		
		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile(args[0]);
		if ( useTransit ) {
			config.scenario().setUseTransit(true);
			config.scenario().setUseVehicles(true);
//		config.otfVis().setColoringScheme( OTFVisConfigGroup.COLORING_BVG ) ;
		}

		config.getQSimConfigGroup().setVehicleBehavior( QSimConfigGroup.VEHICLE_BEHAVIOR_TELEPORT ) ;
		
		config.otfVis().setShowTeleportedAgents(true) ;
		
		Scenario sc = ScenarioUtils.loadScenario(config) ;

		final TransitSchedule schedule = sc.getTransitSchedule() ;
		final TransitRouterConfig trConfig = new TransitRouterConfig( sc.getConfig() ) ; 
		final TransitRouterNetwork routerNetwork = TransitRouterNetwork.createFromSchedule(schedule, trConfig.beelineWalkConnectionDistance);

		Controler tc = new Controler(config) ;
		tc.setOverwriteFiles(true);
		
		Logger.getLogger("main").warn("warning: using randomized pt router!!!!") ;
		
		tc.setTransitRouterFactory( new TransitRouterFactory() {
			@Override
			public TransitRouter createTransitRouter() {
				RandomizedTransitRouterTravelTimeAndDisutility3 ttCalculator = new RandomizedTransitRouterTravelTimeAndDisutility3(trConfig);
				ttCalculator.setDataCollection(DataCollection.randomizedParameters, true) ;
				ttCalculator.setDataCollection(DataCollection.additionalInformation, false) ;

//				TransitRouterNetworkTravelTimeAndDisutility ttCalculator = new TransitRouterNetworkTravelTimeAndDisutility(trConfig) ;

				return new TransitRouterImpl(trConfig, new PreparedTransitSchedule(schedule), routerNetwork, ttCalculator, ttCalculator);
			}
		}) ;


		tc.setMobsimFactory(new MyMobsimFactory()) ;
		tc.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		//		tc.setCreateGraphs(false);

		tc.run();
	}

	static class MyMobsimFactory implements MobsimFactory {

		@Override
		public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
			QSim qSim = (QSim) new QSimFactory().createMobsim(sc, eventsManager) ;
			

			if ( useOTFVis ) {
				// otfvis configuration.  There is more you can do here than via file!
				final OTFVisConfigGroup otfVisConfig = qSim.getScenario().getConfig().otfVis();
				otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
				//				otfVisConfig.setShowParking(true) ; // this does not really work
				otfVisConfig.setColoringScheme(OTFVisConfigGroup.ColoringScheme.bvg) ;


				OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, eventsManager, qSim);
				OTFClientLive.run(sc.getConfig(), server);
			}
			//			if(this.useHeadwayControler){
			//				simulation.getQSimTransitEngine().setAbstractTransitDriverFactory(new FixedHeadwayCycleUmlaufDriverFactory());
			//				this.events.addHandler(new FixedHeadwayControler(simulation));		
			//			}

			return qSim ;
		}
	}
}
