/* *********************************************************************** *
 * project: org.matsim.*
 * LinkFilter.java
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

/**
 *
 */
package playground.yu.linkUtilOffset.hourVersion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.analysis.LegAlgorithm;

/**
 * extracts the links passed by really
 * 
 * @author yu
 * 
 */
public class LinkWithTrafficFilter extends AbstractPersonAlgorithm implements
		PlanAlgorithm, LegAlgorithm {
	private Set<Id> passedLinkIds = new HashSet<Id>();

	public Set<Id> getPassedLinkIds() {
		return passedLinkIds;
	}

	@Override
	public void run(Person person) {
		run(person.getSelectedPlan());
	}

	@Override
	public void run(Plan plan) {
		List<PlanElement> pes = plan.getPlanElements();
		for (int i = 1; i < pes.size(); i += 2)
			run((LegImpl) pes.get(i));
	}

	@Override
	public void run(LegImpl leg) {
		NetworkRoute route = (NetworkRoute) leg.getRoute();
		// leave link --> volume++
		this.passedLinkIds.add(route.getStartLinkId());
		this.passedLinkIds.addAll(route.getLinkIds());
	}

	public void output() {
		System.out
				.println("the number of the links, through those is really passed\t"
						+ this.passedLinkIds.size());
	}

	public static void main(String[] args) {
		String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml"//
		, popFilename = "../matsimTests/Calibration/output_plans.xml.gz";

		ScenarioImpl sc = new ScenarioImpl();

		// NetworkImpl net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(netFilename);

		Population pop = sc.getPopulation();
		new MatsimPopulationReader(sc).readFile(popFilename);

		LinkWithTrafficFilter lf = new LinkWithTrafficFilter();
		lf.run(pop);
		lf.output();
	}

}
