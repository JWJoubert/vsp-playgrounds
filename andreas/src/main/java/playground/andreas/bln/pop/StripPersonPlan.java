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
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationReader;

/**
 * Reset all "personal" attributes of a person
 *
 * @author aneumann
 *
 */
public class StripPersonPlan extends NewPopulation {
	private int planswritten = 0;
	private int personshandled = 0;

	public StripPersonPlan(Network network, Population plans, String filename) {
		super(network, plans, filename);
	}

	@Override
	public void run(Person pp) {

		PersonImpl person = (PersonImpl) pp;

		this.personshandled++;

		person.setAge(Integer.MIN_VALUE);
		person.setCarAvail(null);
		person.setEmployed(null);
		person.setLicence(null);
		person.setSex(null);

		this.popWriter.writePerson(person);
	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		ScenarioImpl sc = new ScenarioImpl();

		String networkFile = "./bb_cl.xml.gz";
		String inPlansFile = "./plans3.xml.gz";
		String outPlansFile = "./plans3_stripped.xml.gz";

		NetworkLayer net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(networkFile);

		Population inPop = sc.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(inPlansFile);

		StripPersonPlan dp = new StripPersonPlan(net, inPop, outPlansFile);
		dp.run(inPop);
		System.out.println(dp.personshandled + " persons handled; " + dp.planswritten + " plans written to file");
		dp.writeEndPlans();

		Gbl.printElapsedTime();
	}
}
