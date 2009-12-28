/* *********************************************************************** *
 * project: org.matsim.*
 * EventFilterTestAveTraSpeCal_personSpecific.java
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
import playground.yu.visum.filter.EventFilterPersonSpecific;
import playground.yu.visum.filter.PersonFilterAlgorithm;
import playground.yu.visum.filter.PersonIDFilter;
import playground.yu.visum.filter.finalFilters.AveTraSpeCal;
import playground.yu.visum.filter.finalFilters.PersonIDsExporter;
import playground.yu.visum.writer.PrintStreamLinkATT;
import playground.yu.visum.writer.PrintStreamUDANET;

/**
 * This class offers a test, that contains: [to create Network object] [to read
 * networkfile] [to create plans object] [to set plans algorithms
 * (PersonFilterAlgorithm, PersonIDsExporter)] [to create events reader] [to
 * read plans file] [to running plans algorithms] [to set events algorithms
 * (EventFilterAlgorithm, EventFilterPersonSpecific, AveTraSpeCal)] [to read
 * events file] [to run events algorithms] [to print additiv netFile of
 * Visum...] [to print attributsFile of link...]
 * 
 * @author yu chen
 */
public class EventFilterTestAveTraSpeCal_personSpecific {

	/**
	 * @param args
	 *            "test/yu/config_hm.xml config_v1.dtd"
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {

		Gbl.startMeasurement();
		Config config = new ScenarioLoaderImpl(args[0]).loadScenario().getConfig();
		testRunAveTraSpeCal(config);
		Gbl.printElapsedTime();
	}

	public static void testRunAveTraSpeCal(Config config) throws IOException {

		// network
		System.out.println("  creating network object... ");
		NetworkLayer network = new NetworkLayer();

		System.out.println("  done.");

		System.out.println("  reading network file... ");
		new MatsimNetworkReader(network).readFile(config.network()
				.getInputFile());
		System.out.println("  done.");

		// plans
		System.out.println("  creating plans object... ");
		PopulationImpl plans = new PopulationImpl();
		plans.setIsStreaming(true);
		System.out.println("  done.");

		System.out.println("  setting plans algorithms... ");
		PersonIDFilter pidf = new PersonIDFilter(100);
		PersonIDsExporter pide = new PersonIDsExporter();
		PersonFilterAlgorithm pfa = new PersonFilterAlgorithm();
		pidf.setNextFilter(pide);
		pfa.setNextFilter(pidf);
		System.out.println("  done.");

		// events
		System.out.println("  creating events object... ");
		EventsManagerImpl events = new EventsManagerImpl();
		System.out.println("  done.");

		System.out.println("  reading plans xml file... ");
		PopulationReader plansReader = new MatsimPopulationReader(plans,
				network);
		plansReader.readFile(config.plans().getInputFile());
		System.out.println("  done.");

		System.out.println("  running plans algos ... ");
		pfa.run(plans);
		System.out.println("we have " + pfa.getCount()
				+ "persons at last -- FilterAlgorithm");
		System.out.println("we have " + pide.getCount()
				+ "persons at last -- DepTimeFilter");
		System.out.println("  done.");

		System.out.println("  setting events algorithms...");
		EventFilterPersonSpecific efpsc = new EventFilterPersonSpecific(pide
				.idSet());
		AveTraSpeCal atsc = new AveTraSpeCal(plans, network);
		EventFilterAlgorithm efa = new EventFilterAlgorithm();
		efa.setNextFilter(efpsc);
		efpsc.setNextFilter(atsc);
		events.addHandler(efa);
		System.out.println("  done");

		// read file, run algos
		System.out.println("  reading events file and running events algos");
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		System.out.println("we have\t" + atsc.getCount()
				+ "\tevents\tat last -- AveTraSpeCal.");
		System.out.println("  done.");

		System.out.println("\tprinting additiv netFile of Visum...");
		PrintStreamUDANET psUdaNet = new PrintStreamUDANET(config.getParam(
				"attribut_aveTraSpe", "outputAttNetFile"));
		psUdaNet.output(atsc);
		psUdaNet.close();
		System.out.println("\tdone.");

		System.out.println("\tprinting attributsFile of link...");
		PrintStreamLinkATT psLinkAtt = new PrintStreamLinkATT(config.getParam(
				"attribut_aveTraSpe", "outputAttFile"), network);
		psLinkAtt.output(atsc);
		psLinkAtt.close();
		System.out.println("\tdone.");
	}
}
