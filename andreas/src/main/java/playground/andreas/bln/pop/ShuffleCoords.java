/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.bln.pop;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * Change the coords of a given plan for every person, except the original one.
 * It is assumed that the original persons has a digit only Id.
 *
 * @author aneumann
 *
 */
public class ShuffleCoords extends NewPopulation {

	private double radius; // meter
	private String homeActString = null;

	public ShuffleCoords(Network network, Population plans, String filename, double radius) {
		super(network, plans, filename);
		this.radius = radius;
	}

	/**
	 * If set, the coords of a home activity will only be changed once, thus all home acts have the same shuffled coords.
	 *
	 * @param givenHomeActString The String defining a home act
	 */
	public void setChangeHomeActsOnlyOnceTrue(String givenHomeActString) {
		this.homeActString = givenHomeActString;
	}

	@Override
	public void run(Person person) {

		try {
			// Keep old person untouched
			Double.parseDouble(person.getId().toString());
			this.popWriter.writePerson(person);
		} catch (Exception e) {
			// clones need to be handled

			Plan plan = person.getPlans().get(0);

			CoordImpl coordOfHomeAct = null;

			for (PlanElement planElement : plan.getPlanElements()) {
				if(planElement instanceof ActivityImpl){
					ActivityImpl act = (ActivityImpl) planElement;

					double x = -0.5 + MatsimRandom.getRandom().nextDouble();
					double y = -0.5 + MatsimRandom.getRandom().nextDouble();

					double scale = Math.sqrt((this.radius * this.radius)/(x * x + y * y)) * MatsimRandom.getRandom().nextDouble();

					CoordImpl shuffledCoords = new CoordImpl(act.getCoord().getX() + x * scale, act.getCoord().getY() + y * scale);

					if(this.homeActString != null){
						if(act.getType().equalsIgnoreCase(this.homeActString)){
							if (coordOfHomeAct == null){
								coordOfHomeAct = shuffledCoords;
							}
							act.setCoord(coordOfHomeAct);
						} else {
							act.setCoord(shuffledCoords);
						}
					} else {
						act.setCoord(shuffledCoords);
					}

				}
			}

			this.popWriter.writePerson(person);
		}
	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		ScenarioImpl sc = new ScenarioImpl();

		String networkFile = "./bb_cl.xml.gz";
		String inPlansFile = "./plan_korridor_50x.xml.gz";
		String outPlansFile = "./plan_korridor_50x_sc.xml.gz";

		NetworkLayer net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(networkFile);

		Population inPop = sc.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(inPlansFile);

		ShuffleCoords shuffleCoords = new ShuffleCoords(net, inPop, outPlansFile, 10.0);
		shuffleCoords.run(inPop);
		shuffleCoords.writeEndPlans();

		Gbl.printElapsedTime();
	}
}
