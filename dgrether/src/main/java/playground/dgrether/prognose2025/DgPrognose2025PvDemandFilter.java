/* *********************************************************************** *
 * project: org.matsim.*
 * BavariaPvCreator
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
package playground.dgrether.prognose2025;

import java.io.IOException;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.misc.Time;


/**
 * Extension of DgPrognose2025DemandFilter for private transport demands.
 * @author dgrether
 *
 */
public class DgPrognose2025PvDemandFilter extends DgPrognose2025DemandFilter {

	private static final Logger log = Logger.getLogger(DgPrognose2025PvDemandFilter.class);
	
	private Random random;
	
	public DgPrognose2025PvDemandFilter(){
		random = MatsimRandom.getLocalInstance();
	}

	private void addNewPerson(Link startLink, Person person, Population newPop, Route route, double endTime){
		PopulationFactory popFactory = newPop.getFactory();
		Person newPerson = popFactory.createPerson(person.getId());
		newPop.addPerson(newPerson);
		Plan newPlan = popFactory.createPlan();
		newPerson.addPlan(newPlan);
		Activity oldWorkAct = ((Activity)((Plan)person.getPlans().get(0)).getPlanElements().get(2));
		//home activity
		Activity newAct = popFactory.createActivityFromCoord("pvHome", startLink.getCoord());
		
		newAct.setEndTime(endTime);
		newPlan.addActivity(newAct);
		//leg
		Leg leg = popFactory.createLeg("car");
		newPlan.addLeg(leg);
		//work activity
		Link endLink = net.getLinks().get(route.getEndLinkId());
		newAct = popFactory.createActivityFromCoord("pvWork", endLink.getCoord());
		newAct.setEndTime(oldWorkAct.getEndTime());
		newPlan.addActivity(newAct);
		//leg
		leg = popFactory.createLeg("car");
		newPlan.addLeg(leg);
		newAct = popFactory.createActivityFromCoord("pvHome", startLink.getCoord());
		newPlan.addActivity(newAct);
	}
	
	@Override
	protected void addNewPerson(Link startLink, Person person, Population newPop, Route route) {
		LinkLeaveEvent leaveEvent = this.collector.getLinkLeaveEvent(person.getId(), startLink.getId());		
		double filterAreaEnterTime = leaveEvent.getTime();
		if (filterAreaEnterTime > 24.0 *3600.0){
			if (random.nextDouble() < 0.33){
				double endTime = filterAreaEnterTime % (24.0 * 3600.0);
				log.info("Old end time: " + Time.writeTime(filterAreaEnterTime) + " new end time: " + Time.writeTime(endTime));
				this.addNewPerson(startLink, person, newPop, route, leaveEvent.getTime());
			}
		}
		else {
			this.addNewPerson(startLink, person, newPop, route, leaveEvent.getTime());
		}
	}
	
	public static void main(String[] args) throws IOException {
		if (args == null || args.length == 0){
			new DgPrognose2025GvDemandFilter().filterAndWriteDemand(DgDetailedEvalFiles.PROGNOSE_2025_2004_NETWORK, 
					DgDetailedEvalFiles.PV_POPULATION_INPUT_FILE, DgDetailedEvalFiles.PV_EVENTS_FILE, DgDetailedEvalFiles.BAVARIA_SHAPE_FILE,
					DgDetailedEvalFiles.PV_POPULATION_OUTPUT_FILE);
		}
		else if (args.length == 5){
			new DgPrognose2025GvDemandFilter().filterAndWriteDemand(args[0], args[1], args[2], args[3], args[4]);
		}
	}


}
