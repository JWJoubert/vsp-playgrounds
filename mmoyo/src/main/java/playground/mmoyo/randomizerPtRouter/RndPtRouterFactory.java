/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mmoyo.randomizerPtRouter;

import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.kai.usecases.randomizedptrouter.RandomizedTransitRouterTravelTimeAndDisutility3;
//import playground.kai.usecases.randomizedptrouter.RandomizedTransitRouterTravelTimeAndDisutility3.DataCollection;

public class RndPtRouterFactory {

	public TransitRouterFactory createFactory (final TransitSchedule schedule, final TransitRouterConfig trConfig, final TransitRouterNetwork routerNetwork, final boolean rndParams, final boolean addInfo){
		return 
		new TransitRouterFactory() {
			@Override
			public TransitRouter createTransitRouter() {
				RandomizedTransitRouterTravelTimeAndDisutility3 ttCalculator = new RandomizedTransitRouterTravelTimeAndDisutility3(trConfig);
//				ttCalculator.setDataCollection(DataCollection.randomizedParameters, rndParams) ;
//				ttCalculator.setDataCollection(DataCollection.additionalInformation, addInfo) ;
				return new TransitRouterImpl(trConfig, new PreparedTransitSchedule(schedule), routerNetwork, ttCalculator, ttCalculator);
			}
		};
	}

}