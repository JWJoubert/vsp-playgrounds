/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioPlayer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mrieser.pt.demo;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.pt.queuesim.TransitQueueSimulation;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.xml.sax.SAXException;

import playground.mrieser.OTFDemo;

/**
 * Visualizes a transit schedule and simulates the transit vehicle's movements.
 *
 * @author mrieser
 */
public class ScenarioPlayer {

	private static final String SERVERNAME = "ScenarioPlayer";

	public static void play(final ScenarioImpl scenario, final EventsManagerImpl events) {
		scenario.getConfig().simulation().setSnapshotStyle("queue");
		final TransitQueueSimulation sim = new TransitQueueSimulation(scenario, events);
		sim.startOTFServer(SERVERNAME);
		OTFDemo.ptConnect(SERVERNAME);
		sim.run();
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public static void main(final String[] args) throws SAXException, ParserConfigurationException, IOException {
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl("test/input/playground/marcel/pt/config.xml");
		ScenarioImpl scenario = sl.getScenario();

		NetworkImpl network = scenario.getNetwork();
		network.getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());

		sl.loadScenario();

		scenario.getConfig().simulation().setSnapshotPeriod(0.0);
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);

		TransitSchedule schedule = scenario.getTransitSchedule();
		new TransitScheduleReaderV1(schedule, network).parse("test/input/org/matsim/transitSchedule/TransitScheduleReaderTest/transitSchedule.xml");
		new CreateVehiclesForSchedule(schedule, scenario.getVehicles()).run();

		final EventsManagerImpl events = new EventsManagerImpl();
		EventWriterXML writer = new EventWriterXML("./output/testEvents.xml");
		events.addHandler(writer);

		play(scenario, events);

		writer.closeFile();
	}

}
