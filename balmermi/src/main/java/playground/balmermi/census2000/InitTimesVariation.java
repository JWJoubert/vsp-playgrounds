/* *********************************************************************** *
 * project: org.matsim.*
 * InitTimesVariation.java
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

package playground.balmermi.census2000;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.world.World;

import playground.balmermi.census2000.modules.PersonVaryTimes;

public class InitTimesVariation {

	public static void varyInitTimes(Config config) {

		System.out.println("MATSim-IIDM: vary init times.");

		World world = Gbl.createWorld();
		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading network xml file...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  setting up plans objects...");
		PopulationImpl plans = new PopulationImpl();
		plans.setIsStreaming(true);
		PopulationWriter plansWriter = new PopulationWriter(plans);
		plansWriter.startStreaming(config.plans().getOutputFile());
		PopulationReader plansReader = new MatsimPopulationReader(plans, network);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  adding person modules... ");
		plans.addAlgorithm(new PersonVaryTimes());
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading, processing, writing plans...");
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		plans.printPlansCount();
		plansWriter.closeStreaming();
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  writing network xml file... ");
		NetworkWriter net_writer = new NetworkWriter(network);
		net_writer.writeFile(config.network().getOutputFile());
		System.out.println("  done.");

		System.out.println("  writing config xml file... ");
		new ConfigWriter(config).writeFile(config.config().getOutputFile());
		System.out.println("  done.");

		System.out.println("done.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		Config config = Gbl.createConfig(args);

		varyInitTimes(config);

		Gbl.printElapsedTime();
	}
}
