/* *********************************************************************** *
 * project: org.matsim.*
 * EndActivityAndEvacuateReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.replanners;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.agents.DefaultPersonDriverAgent;

import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;

/*
 * Persons who use this replanner perform an activity at a "save" place
 * when the evacuation order is given. They extend their current activity
 * until the end of the day.
 * 
 */
public class ExtendCurrentActivityReplanner extends WithinDayDuringActivityReplanner {

	private static final Logger log = Logger.getLogger(ExtendCurrentActivityReplanner.class);
	
	/*package*/ ExtendCurrentActivityReplanner(Id id, Scenario scenario) {
		super(id, scenario);
	}
	
	@Override
	public boolean doReplanning(PersonAgent personAgent) {		
		
		// If we don't have a valid WithinDayPersonAgent
		if (personAgent == null) return false;
		
		WithinDayPersonAgent withinDayPersonAgent = null;
		if (!(personAgent instanceof WithinDayPersonAgent)) return false;
		else {
			withinDayPersonAgent = (WithinDayPersonAgent) personAgent;
		}
	
		PersonImpl person = (PersonImpl)withinDayPersonAgent.getPerson();
		PlanImpl selectedPlan = (PlanImpl)person.getSelectedPlan(); 
		
		// If we don't have a selected plan
		if (selectedPlan == null) return false;
		
		Activity currentActivity;

		/*
		 *  Get the current PlanElement and check if it is an Activity
		 */
		PlanElement currentPlanElement = withinDayPersonAgent.getCurrentPlanElement();
		if (currentPlanElement instanceof Activity) {
			currentActivity = (Activity) currentPlanElement;
		} else return false;
		
		// If we don't have a current Activity.
		if (currentActivity == null) return false;
		
		/*
		 * If it is already the last Activity that lasts until the end
		 * of the simulation the end time is Time.UNDEFINED_TIME. In that
		 * case we do not have to do any replanning anymore. 
		 */
		if (currentActivity.getEndTime() == Time.UNDEFINED_TIME) return true;
		
		/*
		 * Remove the endtime of the current Activity therefore the activity 
		 * will last until the end of the simulation.
		 */
		currentActivity.setEndTime(Time.UNDEFINED_TIME);
				
		// Remove all legs and activities after the current activity.
		int currentActivityIndex = selectedPlan.getActLegIndex(currentActivity);
		
		while (selectedPlan.getPlanElements().size() - 1 > currentActivityIndex) {
			selectedPlan.removeActivity(selectedPlan.getPlanElements().size() - 1);
		}
	
		/*
		 * Reschedule the currently performed Activity in the QSim - there
		 * the activityEndsList has to be updated.
		 */
		// yyyy a method getMobsim in MobimAgent would be useful here. cdobler, Oct'10
		if (withinDayPersonAgent instanceof DefaultPersonDriverAgent) {
			// yyyy do we have to check that? We have a currentActivity... cdobler, Oct'10
			boolean found = ((DefaultPersonDriverAgent) personAgent).getQSimulation().getActivityEndsList().contains(this);
			
			// If the agent is not in the activityEndsList return without doing anything else.
			if (!found) return false;
			
			double oldDepartureTime = personAgent.getDepartureTime();
			
			withinDayPersonAgent.calculateDepartureTime(currentActivity);
			double newDepartureTime = personAgent.getDepartureTime();
			withinDayPersonAgent.getQSimulation().rescheduleActivityEnd(personAgent, oldDepartureTime, newDepartureTime);
			return true;
		}
		else {
			log.warn("PersonAgent is no DefaultPersonDriverAgent - the new departure time cannot be calcualted!");
			return false;
		}
	}	
}