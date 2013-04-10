/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.sergioo.singapore2012.transitRouterVariable;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;

/**
 * Factory for the variable transit router
 * 
 * @author sergioo
 */
public class TransitRouterWWImplFactory implements TransitRouterFactory, AfterMobsimListener {

	private final TransitRouterConfig config;
	private final TransitRouterNetworkWW routerNetwork;
	private final Network network;
	private TransitRouterNetworkTravelTimeAndDisutilityWW transitRouterNetworkTravelTimeAndDisutilityWW;
	private Controler controler;
	private final WaitTime waitTime;
	
	public TransitRouterWWImplFactory(final TransitRouterConfig config, final Controler controler, final WaitTime waitTime) {
		this.config = config;
		this.network = controler.getNetwork();
		this.controler = controler;
		this.waitTime = waitTime;
		routerNetwork = TransitRouterNetworkWW.createFromSchedule(network, controler.getScenario().getTransitSchedule(), this.config.beelineWalkConnectionDistance);
	}
	@Override
	public TransitRouter createTransitRouter() {
		transitRouterNetworkTravelTimeAndDisutilityWW = new TransitRouterNetworkTravelTimeAndDisutilityWW(config, network, routerNetwork, controler.getLinkTravelTimes(), waitTime, controler.getConfig().travelTimeCalculator(), controler.getConfig().getQSimConfigGroup(), new PreparedTransitSchedule(controler.getScenario().getTransitSchedule()));
		return new TransitRouterVariableImpl(config, transitRouterNetworkTravelTimeAndDisutilityWW, routerNetwork, network);
	}
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		transitRouterNetworkTravelTimeAndDisutilityWW.initTravelTimes();
	}

}
