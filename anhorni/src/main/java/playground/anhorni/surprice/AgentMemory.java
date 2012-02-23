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

package playground.anhorni.surprice;

import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;

import playground.anhorni.surprice.preprocess.Zone;

public class AgentMemory {
	
	private List<Plan> plansWeek = new Vector<Plan>();
	private List<Plan> plansSat = new Vector<Plan>();
	private List<Plan> plansSun = new Vector<Plan>();
	
	private TreeMap<String, Integer> activitiesWeek = new TreeMap<String, Integer>();
	private TreeMap<String, Integer> activitiesSat = new TreeMap<String, Integer>();
	private TreeMap<String, Integer> activitiesSun = new TreeMap<String, Integer>();	
	private Zone homeZone;
		
	public void setHomeZone(Zone homeZone) {
		this.homeZone = homeZone;
	}
	
	public Zone getHomeZone() {
		return this.homeZone;
	}
	
	public void addPlan(Plan plan, String day) {
		if (day.equals("Sat")) {
			this.plansSat.add(plan);		
			this.countActs(plan, this.activitiesSat);
		}
		else if (day.equals("Sun")) {
			this.plansSun.add(plan);		
			this.countActs(plan, this.activitiesSun);
		}
		else {
			this.plansWeek.add(plan);		
			this.countActs(plan, this.activitiesWeek);
		}
	}
	
	public Plan getRandomPlanAndRemove(String day, Random random) {
		if (day.equals("Sat")) {
			Plan plan = this.plansSat.get(0);	
			this.plansSat.clear();
			return plan;
		}
		else if (day.equals("Sun")) {
			Plan plan = this.plansSun.get(0);	
			this.plansSun.clear();
			return plan;		
		}
		else {
			int index = random.nextInt(this.plansWeek.size());
			Plan plan = this.plansWeek.get(index);			
			this.plansWeek.remove(index);
			return plan;
		}
	}
	
	public int getNumberOfActivities(String type, String day) {
		if (day.equals("Sat")) {
			if (this.activitiesSat.containsKey(type)) return this.activitiesSat.get(type);
			else return 0;
		}
		else if (day.equals("Sun")) {
			if (this.activitiesSun.containsKey(type)) return this.activitiesSun.get(type);
			else return 0;			
		}
		else {
			if (this.activitiesWeek.containsKey(type)) return this.activitiesWeek.get(type);
			else return 0;
		}
	}
		
	public List<String> getModeOfLastTripsWithSamePurpose(String purpose, List<Plan> plans) {
		List<String> modes = new Vector<String>();
		
		for (Plan plan : plans) {
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;
					
					if (act.getType().equals(purpose)) {
						PlanImpl pl = (PlanImpl)plan;
						String mode = pl.getPreviousLeg(act).getMode();
						modes.add(mode);
					}
				}
			}			
		}		
		return modes;
	}
	
	// act frequency -------------------------------------------------------------	
	private void countActs(Plan plan, TreeMap<String, Integer> activities) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				ActivityImpl act = (ActivityImpl)pe;
				String type = act.getType();
				int nActs = 0;
				if (activities.containsKey(type)) {
					nActs = activities.get(type);					
				}
				nActs += 1;
				activities.remove(type);
				activities.put(type, nActs);
			}
		}
	}
}
