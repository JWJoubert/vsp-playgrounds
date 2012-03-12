/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideTravelTimeCost.java
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
package playground.thibautd.parknride;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.pt.router.TransitRouterConfig;

/**
 * @author thibautd
 */
public class ParkAndRideTravelTimeCost implements PersonalizableTravelCost, PersonalizableTravelTime {
	private final TransitRouterConfig config;

	public ParkAndRideTravelTimeCost(final TransitRouterConfig config) {
		this.config = config;
	}

	@Override
	public double getLinkGeneralizedTravelCost(final Link link, final double time) {
		double transfertime = getLinkTravelTime(link, time);
		double waittime = this.config.additionalTransferTime;
		
		// say that the effective walk time is the transfer time minus some "buffer"
		double walktime = transfertime - waittime;
		
		// weigh the "buffer" not with the walk time disutility, but with the wait time disutility:
		// (note that this is the same "additional disutl of wait" as in the scoring function.  Its default is zero.
		// only if you are "including the opportunity cost of time into the router", then the disutility of waiting will
		// be the same as the marginal opprotunity cost of time).  kai, nov'11
		return -walktime * this.config.getMarginalUtilityOfTravelTimeWalk_utl_s()
			   -waittime * this.config.getMarginalUtiltityOfWaiting_utl_s();
	}

	@Override
	public double getLinkTravelTime(final Link link, final double time) {
		double distance = link.getLength();
		return distance / this.config.getBeelineWalkSpeed() + this.config.additionalTransferTime;
	}

	@Override
	public void setPerson(final Person person) {
		// nothing to do here
	}
}

