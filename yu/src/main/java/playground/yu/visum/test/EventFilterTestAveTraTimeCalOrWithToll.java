/* *********************************************************************** *
 * project: org.matsim.*
 * EventFilterTestAveTraSpeCal_ohne_Maut.java
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

package playground.yu.visum.test;

import java.io.IOException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.yu.visum.filter.EventFilterAlgorithm;
import playground.yu.visum.filter.finalFilters.AveTraTimeCal;
import playground.yu.visum.writer.PrintStreamATTA;
import playground.yu.visum.writer.PrintStreamLinkATT;
import playground.yu.visum.writer.PrintStreamUDANET;

/**
 * @author yu chen
 */
public class EventFilterTestAveTraTimeCalOrWithToll {

	public static void testRunAveTraTimeCal(Config config) throws IOException {

		ScenarioImpl scenario = new ScenarioImpl(config);
		
		// network
		System.out.println("  reading network file... ");
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network()
				.getInputFile());
		System.out.println("  done.");

		// plans
		System.out.println("  creating plans object... ");
		PopulationImpl plans = scenario.getPopulation();
		plans.setIsStreaming(true);
		System.out.println("  done.");

		// events
		System.out.println("  creating events object... ");
		EventsManagerImpl events = new EventsManagerImpl();
		System.out.println("  done.");

		System.out.println("  reading plans xml file... ");
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(config.plans().getInputFile());
		System.out.println("  done.");

		System.out.println("  adding events algorithms...");
		AveTraTimeCal attc = new AveTraTimeCal(plans, network);
		EventFilterAlgorithm efa = new EventFilterAlgorithm();
		efa.setNextFilter(attc);
		events.addHandler(efa);
		System.out.println("  done");

		// read file, run algos if streaming is on
		System.out
				.println("  reading events file and (probably) running events algos");
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		System.out.println("we have " + efa.getCount()
				+ " events at last -- EventFilterAlgorithm.");
		System.out.println("we have " + attc.getCount()
				+ " events at last -- AveTraTimeCal.");
		System.out.println("  done.");

		// run algos if needed, only if streaming is off
		System.out
				.println("  running events algorithms if they weren't already while reading the events...");

		System.out.println("\tprinting additiv netFile of Visum...");
		PrintStreamUDANET psUdaNet = new PrintStreamUDANET(config.getParam(
				"attribut_aveTraTime", "outputAttNetFile"));
		psUdaNet.output(attc);
		psUdaNet.close();
		System.out.println("\tdone.");

		System.out.println("\tprinting attributsFile of link...");
		PrintStreamATTA psLinkAtt = new PrintStreamLinkATT(config.getParam(
				"attribut_aveTraTime", "outputAttFile"), network);
		psLinkAtt.output(attc);
		psLinkAtt.close();
		System.out.println("  done.");
	}

	/**
	 * @param args
	 *            - test/yu/config_hm_ohne_Maut.xml config_v1.dtd
	 *            or
	 *            - test/yu/config_hm_mit_Maut_test.xml config_v1.dtd
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {

		Gbl.startMeasurement();
		Config config =new ScenarioLoaderImpl(args[0]).loadScenario().getConfig();
		testRunAveTraTimeCal(config);
		Gbl.printElapsedTime();
	}
}
