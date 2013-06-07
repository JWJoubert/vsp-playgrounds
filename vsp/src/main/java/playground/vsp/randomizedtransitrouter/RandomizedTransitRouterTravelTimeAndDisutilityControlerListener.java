/* *********************************************************************** *
 * project: org.matsim.*
 * RandomizedTransitRouterTravelTimeAndDisutilityControlerListener
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

import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;


/**
 * Sets a TransitRouteFactory that enables routing with randomized routing parameters. 
 * 
 * @author dgrether
 *
 */
public class RandomizedTransitRouterTravelTimeAndDisutilityControlerListener implements StartupListener {

	@Override
	public void notifyStartup(StartupEvent e) {
		RandomizedTransitRouterTravelTimeAndDisutilityFactory routerFactory = 
				new RandomizedTransitRouterTravelTimeAndDisutilityFactory(e.getControler().getConfig(), e.getControler().getScenario().getTransitSchedule());
		e.getControler().setTransitRouterFactory(routerFactory);
	}

}
