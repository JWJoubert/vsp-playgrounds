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

package playground.michalm.vrp.data.network.shortestpath;

import org.matsim.api.core.v01.Id;


public class ShortestPath
{
    // optimization
    public static final ShortestPath ZERO_PATH_ENTRY = new ShortestPath(0, 0, new Id[0]);

    public final int travelTime;
    public final double travelCost;
    public final Id[] linkIds;


    public ShortestPath(int travelTime, double travelCost, Id[] linkIds)
    {
        this.travelTime = travelTime;
        this.travelCost = travelCost;
        this.linkIds = linkIds;
    }
}
