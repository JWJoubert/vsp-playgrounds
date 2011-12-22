/* *********************************************************************** *
 * project: org.matsim.*
 * SecureActivityPerformingIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.ptproject.qsim.comparators.PersonAgentComparator;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;

import playground.christoph.evacuation.config.EvacuationConfig;

public class SecureActivityPerformingIdentifier extends DuringActivityIdentifier {

	private static final Logger log = Logger.getLogger(SecureActivityPerformingIdentifier.class);
	
	protected ActivityReplanningMap activityReplanningMap;
	protected Coord centerCoord;
	protected double secureDistance;
	
	// Only for Cloning.
	/*package*/ SecureActivityPerformingIdentifier(ActivityReplanningMap activityReplanningMap, Coord centerCoord, double secureDistance) {
		this.activityReplanningMap = activityReplanningMap;
		this.centerCoord = centerCoord;
		this.secureDistance = secureDistance;
	}
	
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {
		Set<PlanBasedWithinDayAgent> activityPerformingAgents = activityReplanningMap.getActivityPerformingAgents();
		Set<PlanBasedWithinDayAgent> agentsToReplan = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());
		
		for (PlanBasedWithinDayAgent personAgent : activityPerformingAgents) {		
			/*
			 *  Get the current PlanElement and check if it is an Activity
			 */
			Activity currentActivity;
			PlanElement currentPlanElement = personAgent.getCurrentPlanElement();
			if (currentPlanElement instanceof Activity) {
				currentActivity = (Activity) currentPlanElement;
			} else continue;
			
			/*
			 * Remove the Agent from the list, if the performed Activity is in an insecure Area.
			 */
			double distance = CoordUtils.calcDistance(currentActivity.getCoord(), centerCoord);
			if (distance <= secureDistance) {
				continue;
			}
			
			// If we reach this point, the agent can be replanned.
			agentsToReplan.add((PlanBasedWithinDayAgent)personAgent);
		}
		if (time == EvacuationConfig.evacuationTime) log.info("Found " + activityPerformingAgents.size() + " Agents performing an Activity in a secure area.");
		
		return agentsToReplan;
	}

}
