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

package playground.mrieser.svi.replanning;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mrieser.svi.data.ActivityToZoneMapping;
import playground.mrieser.svi.data.DynamicODMatrix;

/**
 * @author mrieser
 */
public class DynamicODDemandCollector implements PlanAlgorithm {

	private final DynamicODMatrix odm;
	private final ActivityToZoneMapping mapping;
	
	
	public DynamicODDemandCollector(final DynamicODMatrix odm, final ActivityToZoneMapping actToZoneMapping) {
		this.odm = odm;
		this.mapping = actToZoneMapping;
	}

	@Override
	public void run(final Plan plan) {

		Activity lastAct = null;
		String lastZoneId = null;
		int idx = 0;
		String[] activityZones = this.mapping.getAgentActivityZones(plan.getPerson().getId());
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;

				String zoneId = activityZones[idx];

				if (lastAct != null) {
					
					double tripStartTime = lastAct.getEndTime();
					if (tripStartTime == Time.UNDEFINED_TIME) {
						tripStartTime = lastAct.getStartTime() + lastAct.getMaximumDuration();
					}
					
					this.odm.addTrip(tripStartTime, lastZoneId, zoneId);
				}

				lastAct = act;
				lastZoneId = zoneId;
				idx++;
			}
		}
	}

}
