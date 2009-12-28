/* *********************************************************************** *
 * project: org.matsim.*
 * NewAgentPtPlan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.yu.newPlans;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.yu.analysis.PlanModeJudger;

/**
 * writes new Plansfile, in which every person will has 3 plans, with type
 * "car", "pt" and "walk", whose leg mode will be "pt" or "walk" and who will
 * have only a blank <Route></Rout>
 * 
 * @author ychen
 * 
 */
public class NewAgentWalkPlan extends NewPopulation {
	/**
	 * Constructor, writes file-head
	 * 
	 * @param plans
	 *            - a Plans Object, which derives from MATSim plansfile
	 */
	public NewAgentWalkPlan(final PopulationImpl plans) {
		super(plans);
	}

	public NewAgentWalkPlan(final PopulationImpl population, final String filename) {
		super(population, filename);
	}

	@SuppressWarnings( { "deprecation", "unchecked" })
	@Override
	public void run(final Person person) {
		if (Integer.parseInt(person.getId().toString()) < 1000000000) {
			List<PlanImpl> copyPlans = new ArrayList<PlanImpl>();
			// copyPlans: the copy of the plans.
			for (Plan pl : person.getPlans()) {
				if (hasLongLegs(pl))
					break;
				// set plan type for car, pt, walk
				if (PlanModeJudger.usePt(pl)) {
					PlanImpl walkPlan = new org.matsim.core.population.PlanImpl(
							person);
					walkPlan.setType(PlanImpl.Type.WALK);
					List actsLegs = pl.getPlanElements();
					for (int i = 0; i < actsLegs.size(); i++) {
						Object o = actsLegs.get(i);
						if (i % 2 == 0) {
							walkPlan.addActivity((ActivityImpl) o);
						} else {
							LegImpl leg = (LegImpl) o;
							// -----------------------------------------------
							// WITHOUT routeSetting!
							// -----------------------------------------------
							LegImpl walkLeg = new org.matsim.core.population.LegImpl(
									TransportMode.walk);
							walkLeg.setDepartureTime(leg.getDepartureTime());
							walkLeg.setTravelTime(leg.getTravelTime());
							walkLeg.setArrivalTime(leg.getArrivalTime());
							walkLeg.setRoute(null);
							walkPlan.addLeg(walkLeg);
							// if (!leg.getMode().equals(Mode.car)) {
							// leg.setRoute(null);
							// leg.setMode(Mode.car);
							// }
						}
					}
					copyPlans.add(walkPlan);
				}
			}
			for (PlanImpl copyPlan : copyPlans) {
				person.addPlan(copyPlan);
			}
		}
		this.pw.writePerson(person);
	}

	private boolean hasLongLegs(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof LegImpl) {
				LegImpl leg = (LegImpl) pe;
				if (CoordUtils.calcDistance(((PlanImpl) plan).getPreviousActivity(leg)
						.getLink().getCoord(), ((PlanImpl) plan).getNextActivity(leg)
						.getLink().getCoord()) / 1000.0 > 3.0)
					return true;
			}
		}
		return false;
	}

	public static void main(final String[] args) {
		Config config = new ScenarioLoaderImpl(args[0]).loadScenario().getConfig();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(config.network()
				.getInputFile());

		PopulationImpl population = new PopulationImpl();
		NewAgentWalkPlan nawp = new NewAgentWalkPlan(population);
		new MatsimPopulationReader(population, network).readFile(config.plans()
				.getInputFile());
		nawp.run(population);
		nawp.writeEndPlans();
	}
}
