/* *********************************************************************** *
 * project: org.matsim.*
 * ExternalMobsimTest.java
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

package playground.david;

import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.mobsim.queuesim.SimulationTimer;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;

public class ExternalMobsimTest {

	public static void main(final String[] args) {
		String[] defaultArgs = {"test/simple/default_config.xml"};
		Gbl.createConfig(defaultArgs);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile("e:/Development/tmp/studies/equil//equil_netENG.xml");

		System.out.println("[External MOBSIM called"  + "]");

		EventsManagerImpl events_ = new EventsManagerImpl();
		PopulationImpl population_ = new PopulationImpl();

		//load pop from popfile
		System.out.println("[External MOBSIM"  + "] loading plansfile: " + args[0]);
		PopulationReader plansReader = new MatsimPopulationReader(population_, network);
		plansReader.readFile(args[0]);
		System.out.println("[External MOBSIM"  + "]...done");

		System.out.println("[External MOBSIM"  + "] writing to eventsfile: " + args[1]);
		EventWriterXML writer = new EventWriterXML(args[1]);
		events_.addHandler(writer);

		//
		// run mobsim
		//
		System.out.println("["  + "] mobsim starts");
		SimulationTimer.setTime(0);
		QueueSimulation sim = new QueueSimulation(network, population_, events_);
		sim.run();

		writer.reset(1);
	}

}
