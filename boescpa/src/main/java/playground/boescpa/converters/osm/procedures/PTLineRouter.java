/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.osm.procedures;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Provides the contract for an implementation of pt-lines routing.
 *
 * @author boescpa
 */
public abstract class PTLineRouter {

	protected static Logger log = Logger.getLogger(PTLineRouter.class);

	protected final TransitSchedule schedule;

	/**
	 * The provided schedule is expected to already contain for each line
	 * 	- the stops in the sequence they will be served.
	 * 	- the scheduled times.
	 * The routes will be newly routed. Any former routes will be overwritten.
	 * Changes are done on the schedule provided here.
	 *
	 * @param schedule which will be newly routed.
	 */
	protected PTLineRouter(TransitSchedule schedule) {
		this.schedule = schedule;
	}

	/**
	 * Based on the stops in this.schedule und given the provided network, the lines will be routed.
	 *
	 * @param network is a multimodal network (see MultimodalNetworkCreator)
	 */
	public abstract void routePTLines(Network network);
}
