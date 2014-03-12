/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

package playground.michalm.taxi.optimizer.fifo;

import java.util.*;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;
import playground.michalm.taxi.vehreqpath.VehicleRequestPath;


public class FIFOSchedulingProblem
{
    private final TaxiOptimizerConfiguration optimConfig;
    private final Comparator<VehicleRequestPath> vrpComparator;


    public FIFOSchedulingProblem(TaxiOptimizerConfiguration optimConfig)
    {
        this.optimConfig = optimConfig;
        this.vrpComparator = optimConfig.getVrpComparator();
    }


    public void scheduleUnplannedRequests(Queue<TaxiRequest> unplannedRequests)
    {
        while (!unplannedRequests.isEmpty()) {
            TaxiRequest req = unplannedRequests.peek();

            VehicleRequestPath best = optimConfig.vrpFinder.findBestVehicleForRequest(req,
                    optimConfig.context.getVrpData().getVehicles(), vrpComparator);

            if (best == null) {
                return;
            }

            optimConfig.scheduler.scheduleRequest(best);
            unplannedRequests.poll();
        }
    }
}
