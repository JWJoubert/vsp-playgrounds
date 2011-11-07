/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLegTimesTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.kai.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

public class CalcLegTimesTest extends MatsimTestCase {

	public static final String BASE_FILE_NAME = "tripdurations.txt";
	public static final Id DEFAULT_PERSON_ID = new IdImpl(123);
	public static final Id DEFAULT_LINK_ID = new IdImpl(456);

	private Population population = null;
	private Network network = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		super.loadConfig(null);

		ScenarioImpl s = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.population = s.getPopulation();
		PersonImpl person = new PersonImpl(DEFAULT_PERSON_ID);
		this.population.addPerson(person);
		PlanImpl plan = person.createAndAddPlan(true);
		plan.createAndAddActivity("act1", new CoordImpl(100.0, 100.0));
		plan.createAndAddLeg("undefined");
		plan.createAndAddActivity("act2", new CoordImpl(200.0, 200.0));
		plan.createAndAddLeg("undefined");
		plan.createAndAddActivity("act3", new CoordImpl(200.0, 200.0));
		plan.createAndAddLeg("undefined");
		plan.createAndAddActivity("act4", new CoordImpl(200.0, 200.0));
		plan.createAndAddLeg("undefined");
		plan.createAndAddActivity("act5", new CoordImpl(200.0, 200.0));
		this.network = s.getNetwork();
		Node fromNode = this.network.getFactory().createNode(new IdImpl("123456"), new CoordImpl(100.0, 100.0));
		this.network.addNode(fromNode);
		Node toNode = this.network.getFactory().createNode(new IdImpl("789012"), new CoordImpl(200.0, 200.0));
		this.network.addNode(toNode);
		Link link = this.network.getFactory().createLink(DEFAULT_LINK_ID, fromNode.getId(), toNode.getId());
		link.setLength(Math.sqrt(20000.0));
		link.setFreespeed(13.333);
		link.setCapacity(2000);
		link.setNumberOfLanes(1);
		this.network.addLink(link);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.population = null;
		this.network = null;
	}

	public void testNoEvents() {

		CalcLegTimes testee = new CalcLegTimes(this.population);

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(testee);

		// add events to handle here

		this.runTest(testee);
	}

	public void testAveraging() {

		CalcLegTimes testee = new CalcLegTimes(this.population);

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(testee);

		LegImpl leg = new LegImpl(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("07:10:00"));
		leg.setArrivalTime(Time.parseTime("07:30:00"));
		testee.handleEvent(new AgentDepartureEventImpl(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));
		testee.handleEvent(new AgentArrivalEventImpl(leg.getArrivalTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));

		leg = new LegImpl(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("07:00:00"));
		leg.setArrivalTime(Time.parseTime("07:10:00"));
		testee.handleEvent(new AgentDepartureEventImpl(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));
		testee.handleEvent(new AgentArrivalEventImpl(leg.getArrivalTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));

		leg = new LegImpl(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("31:12:00"));
		leg.setArrivalTime(Time.parseTime("31:22:00"));
		testee.handleEvent(new AgentDepartureEventImpl(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));
		testee.handleEvent(new AgentArrivalEventImpl(leg.getArrivalTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));

		leg = new LegImpl(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("30:12:00"));
		leg.setArrivalTime(Time.parseTime("30:12:01"));
		testee.handleEvent(new AgentDepartureEventImpl(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));
		testee.handleEvent(new AgentArrivalEventImpl(leg.getArrivalTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));

		this.runTest(testee);
	}

	protected void runTest(CalcLegTimes calcLegTimes) {

		calcLegTimes.writeStats(this.getOutputDirectory() + CalcLegTimesTest.BASE_FILE_NAME);

		// actual test: compare checksums of the files
		final long expectedChecksum = CRCChecksum.getCRCFromFile(this.getInputDirectory() + CalcLegTimesTest.BASE_FILE_NAME);
		final long actualChecksum = CRCChecksum.getCRCFromFile(this.getOutputDirectory() + CalcLegTimesTest.BASE_FILE_NAME);
		assertEquals("Output files differ.", expectedChecksum, actualChecksum);
	}

}
