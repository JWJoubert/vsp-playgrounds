/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.droeder.southAfrica.run;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;

import playground.droeder.southAfrica.old.routing.PtSubModeRouterFactoryOld;
import playground.droeder.southAfrica.qSimHook.TransitSubModeQSimFactory;
import playground.droeder.southAfrica.routing.PtSubModeRouterFactory;
import playground.droeder.southAfrica.routing.PtSubModeTripRouterFactory;

/**
 * @author droeder
 *
 */
public class PtSubModeControler extends Controler {
	private static final Logger log = Logger
			.getLogger(PtSubModeControler.class);
	
	/**
	 * This class is a extension of the original MATSim-Controler. It will only work with an enabled pt-simulation.
	 * It uses an own implementation of the TransitRouter and will work with the strategy-module <code>ReRouteFixedPtSubMode</code>. 
	 * @param configFile
	 */
	public PtSubModeControler(String configFile, boolean routeOnSameMode) {
		super(configFile);
		log.warn("This controler uses not the default-implementation of public transport. make sure this is what you want!");
		super.setTransitRouterFactory(new PtSubModeRouterFactory(this, routeOnSameMode));
		//necessary for departure-handling
		super.setMobsimFactory(new TransitSubModeQSimFactory(routeOnSameMode));
		super.setTripRouterFactory(new PtSubModeTripRouterFactory(this));
	}
	
	/**
	 * This class is a extension of the original MATSim-Controler. It will only work with an enabled pt-simulation.
	 * It uses an own implementation of the TripRouter and will work with the strategy-module <code>ReRouteFixedPtSubMode</code>. 
	 * @param configFile
	 */
	public PtSubModeControler(Scenario sc, boolean routeOnSameMode) {
		super(sc);
		log.warn("This controler uses not the default-implementation of public transport. make sure this is what you want!");
		super.setTransitRouterFactory(new PtSubModeRouterFactory(this, routeOnSameMode));
		//necessary for departure-handling
		super.setMobsimFactory(new TransitSubModeQSimFactory(routeOnSameMode));
		//TODO[dr] add TripRouterFactory at right place...
		super.setTripRouterFactory(new PtSubModeTripRouterFactory(this));

	}
	
	@Override
	public void run(){
		if(!(super.getTripRouterFactory() instanceof PtSubModeTripRouterFactory)){
			throw new IllegalArgumentException("TripRouterFactory needs to be instance of PtSubModeTripRouterFactory..."); 
		}
		if(!(super.getMobsimFactory() instanceof TransitSubModeQSimFactory)){
			throw new IllegalArgumentException("QSIMFactory needs to be instance of TransitSubModeQsimFactory...");
		}
		// need to add the PtSubmodeDependRouterFactory as last to controlerlistener, so it is explicitly called last, after all changes in schedule are done...
		super.addControlerListener((PtSubModeRouterFactory)super.getTransitRouterFactory());
		super.run();
	}
	
}
