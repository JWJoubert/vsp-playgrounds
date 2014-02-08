/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.pbouman.crowdedness.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

import playground.pbouman.crowdedness.CrowdedScoringFunctionFactory;
import playground.pbouman.crowdedness.CrowdednessObserver;
import playground.pbouman.crowdedness.events.CrowdedPenaltyEvent;
import playground.pbouman.crowdedness.events.CrowdedPenaltyEventHandler;
import playground.pbouman.crowdedness.events.PersonCrowdednessEvent;
import playground.pbouman.crowdedness.events.PersonCrowdednessEventHandler;
import playground.pbouman.crowdedness.rules.SimpleRule;

/**
 * 
 * @author pcbouman
 *
 */

public class CrowdednessTest
{

	/**
	 * This test currently checks the following things:
	 * 1) Do we get the correct crowdedness ratio's for a 4 person vehicle?
	 * 2) Do we receive the expected number of penalty events?
	 * 3) Are the penalties nonnegative? (In the future, this test should be made more exact)
	 */
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	@Test
	public final void testCrowdednessEvents()
	{
		final ScenarioImpl scen = generateScenario();
		scen.getConfig().controler().setLastIteration(0);
				
		Controler c = new Controler(scen) ;
//		{
//			@Override protected ScoringFunctionFactory loadScoringFunctionFactory()
//			{
//				return new CrowdedScoringFunctionFactory(super.loadScoringFunctionFactory(), getEvents());
//			}
//		};
		
		c.setScoringFunctionFactory(
				new CrowdedScoringFunctionFactory( 
						new CharyparNagelScoringFunctionFactory(c.getConfig().planCalcScore(), c.getNetwork()), c.getEvents()
						)
				) ;

	
		c.getConfig().controler().setOutputDirectory(utils.getOutputDirectory());
		
		//c.setOverwriteFiles(true);
		
		c.getEvents().addHandler(new CrowdednessObserver(scen,c.getEvents(), new SimpleRule()));
		
		c.getEvents().addHandler(
				new PersonCrowdednessEventHandler()
				{

					@Override
					public void reset(int iteration) {}
					@Override
					public void handleEvent(PersonCrowdednessEvent event) {
						
						if (scen.getPopulation().getPersons().get(event.getPersonId()) != null)
						{
						
							Vehicle v = scen.getVehicles().getVehicles().get(event.getVehicleId());
							VehicleCapacity cap = v.getType().getCapacity();
							
							// As we create only 1 agent and there should be a driver in the vehicle,
							// we know how crowded the vehicle should be when we know the capacity.
							// Also, since we use the simple rule, both should be sitting if the seat
							// capacity is larger than 1;
							
							int seats = cap.getSeats();
							int stand = cap.getStandingRoom();
						
							int total = seats + stand;
							
							Assert.assertEquals("The crowdedness should be equal to 2 people divided by the capacity.", 2d / total, event.getTotalCrowdedness(), MatsimTestUtils.EPSILON);
							
							if (seats > 1)
							{
								Assert.assertEquals("The seat crowdedness should be equal to 2 divided by the number of seats.", 2d / seats, event.getSeatCrowdedness(), MatsimTestUtils.EPSILON);
								Assert.assertEquals("Since no one should be sitting, the standing crowdedness should be 0.", 0d, event.getStandCrowdedness(), 0.0001);
							}
							
						}
						
					}
					
				}
		);
		
		final ArrayList<CrowdedPenaltyEvent> cpes = new ArrayList<CrowdedPenaltyEvent>();
		
		c.getEvents().addHandler(
				new CrowdedPenaltyEventHandler()
				{

					@Override
					public void reset(int iteration) {}

					@Override
					public void handleEvent(CrowdedPenaltyEvent event)
					{
						if (scen.getPopulation().getPersons().containsKey(event.getPersonId()))
						{
							//TODO: if we define a formal scoring function and have programmatic control
							//      over the outcome of penalties, we should include them here.
							//      For now, we can only assume that the penalty is nonnegative.
							
							Assert.assertTrue("The penalty for crowdedness cannot be negative", event.getPenalty() >= 0d);
							cpes.add(event);
						}
					}
					
				}
		);
		
		c.run();
		
		// Since there is one agent that should make two trips using Public Transport,
		// we assume that he has received two CrowdedPenaltyEvents.
		Assert.assertEquals("Since we have one agent travelling back and forth, we should have seen two CrowdedPenaltyEvents", 2, cpes.size());
		
		
	}

	
	private ScenarioImpl generateScenario()
	{
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		
		config.controler().setLastIteration(1000);
		config.controler().setWriteEventsInterval(50);
		config.controler().setWritePlansInterval(50);
		
		HashSet<String> transitModes = new HashSet<String>();
		transitModes.add("pt");
		config.transit().setTransitModes(transitModes);
		ScenarioImpl scen = (ScenarioImpl) ScenarioUtils.createScenario(config);

		
		/* ******** *
		 * Settings *
		 * ******** */
		
		double longDist = 5000;
		double boxSize = 1000;
		double ySpan = 100;
		
		double speed = 20;
		double waitBuffer = 60;
		
		double startTime = 7 * 3600;
		
		double workDuration = 8 * 3600;
		
		boolean blockLane = false;
		
		/* ************** *
		 * Create Network *
		 * ************** */
		Network net = scen.getNetwork();
		NetworkFactory nw = net.getFactory();
		
		Node a, b, c, u, v, w;
		
		Node [] nodes = new Node [] {
				a = nw.createNode(scen.createId("a"), scen.createCoord(boxSize/2 , boxSize / 2 - ySpan)),
				b = nw.createNode(scen.createId("b"), scen.createCoord(boxSize/2 , boxSize / 2 + ySpan)),
				c = nw.createNode(scen.createId("c"), scen.createCoord(boxSize/2, boxSize / 2)),
		
				u = nw.createNode(scen.createId("u"), scen.createCoord(longDist + boxSize / 2, boxSize / 2 - ySpan)),
				v = nw.createNode(scen.createId("v"), scen.createCoord(longDist + boxSize / 2, boxSize / 2 + ySpan)),
				w = nw.createNode(scen.createId("w"), scen.createCoord(longDist + boxSize / 2, boxSize / 2)),
		};
		
		for (Node n : nodes)
		{
			net.addNode(n);
		}
		
		Link ac, ca, cb, bc, uw, wu, vw, wv, wc, cw;
		
		Link [] links = new Link [] {
				ac = nw.createLink(scen.createId("ac"), a, c),
				ca = nw.createLink(scen.createId("ca"), c, a),
				cb = nw.createLink(scen.createId("cb"), c, b),
				bc = nw.createLink(scen.createId("bc"), b, c),
		
				uw = nw.createLink(scen.createId("uw"), u, w),
				wu = nw.createLink(scen.createId("wu"), w, u),
				vw = nw.createLink(scen.createId("vw"), v, w),
				wv = nw.createLink(scen.createId("wv"), w, v),
		
				wc = nw.createLink(scen.createId("wc"), w, c),
				cw = nw.createLink(scen.createId("cw"), c, w),
		};
		
		for (Link l : links)
		{
			setDist(l);
			l.setFreespeed(speed);
			l.setCapacity(2);
			net.addLink(l);
		}
		
		/* *************** *
		 * Create Schedule *
		 * *************** */
		
		TransitSchedule schedule = scen.getTransitSchedule();
		TransitScheduleFactory sFac = schedule.getFactory();
				
		TransitStopFacility [] stops = new TransitStopFacility [] {
				sFac.createTransitStopFacility(scen.createId("stop1"), cb.getCoord(), blockLane),
				sFac.createTransitStopFacility(scen.createId("stop2"), wv.getCoord(), blockLane),
				sFac.createTransitStopFacility(scen.createId("stop3"), uw.getCoord(), blockLane),
				sFac.createTransitStopFacility(scen.createId("stop4"), ca.getCoord(), blockLane)
		};
		
		stops[0].setLinkId(cb.getId());
		stops[1].setLinkId(wv.getId());
		stops[2].setLinkId(uw.getId());
		stops[3].setLinkId(ca.getId());
		
		for (TransitStopFacility stop : stops)
		{
			schedule.addStopFacility(stop);
		}
		
		TransitLine line = sFac.createTransitLine(scen.createId("Line1"));
		
		List<Id> routeIds = Arrays.asList(
				ac.getId(),
				cb.getId(),
				bc.getId(),
				cw.getId(),
				wv.getId(),
				vw.getId(),
				wu.getId(),
				uw.getId(),
				wc.getId(),
				ca.getId()
		);
		
		List<TransitRouteStop> stopList = Arrays.asList(
				sFac.createTransitRouteStop(stops[0], 0, waitBuffer),
				sFac.createTransitRouteStop(stops[1], (2* ySpan + longDist)/speed, waitBuffer + (2 * ySpan + longDist)/speed),
				sFac.createTransitRouteStop(stops[2], (5* ySpan + longDist)/speed, waitBuffer + (5 * ySpan + longDist)/speed),
				sFac.createTransitRouteStop(stops[3], (6* ySpan + 2*longDist)/speed, waitBuffer + (6 * ySpan + 2*longDist)/speed)
				
		);
		
		
		
		VehiclesFactory vFac = scen.getVehicles().getFactory();
		
		VehicleCapacity cap = vFac.createVehicleCapacity();
		cap.setSeats(2);
		cap.setStandingRoom(2);
		VehicleType type = vFac.createVehicleType(scen.createId("vtype"));
		type.setCapacity(cap);
		type.setMaximumVelocity(speed);
		scen.getVehicles().addVehicleType(type);
		Vehicle vehicle = vFac.createVehicle(scen.createId("vehicle"), type);

		scen.getVehicles().addVehicle(vehicle);
		
		NetworkRoute route = RouteUtils.createNetworkRoute(routeIds, net);
		TransitRoute transitRoute = sFac.createTransitRoute(scen.createId("route"), route, stopList, "pt");
		
		Departure dep = sFac.createDeparture(scen.createId("dep1"), startTime);
		dep.setVehicleId(vehicle.getId());
		transitRoute.addDeparture(dep);
		
		dep = sFac.createDeparture(scen.createId("dep2"), startTime + workDuration);
		dep.setVehicleId(vehicle.getId());
		transitRoute.addDeparture(dep);
		
		
		line.addRoute(transitRoute);
		schedule.addTransitLine(line);
		
		/* ************************** *
		 * Create Agents & Activities *
		 * ************************** */

		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(3600 * 12);	
		config.planCalcScore().addActivityParams(home);

		ActivityParams work = new ActivityParams("work");
		work.setOpeningTime(0);
		work.setClosingTime(24 * 3600);
		work.setTypicalDuration(workDuration);
		config.planCalcScore().addActivityParams(work);
		
		Population pop = scen.getPopulation();
		PopulationFactory popFac = pop.getFactory();
		
		Person p = popFac.createPerson(scen.createId("Agent"));
		Plan plan = popFac.createPlan();
			
		ActivityImpl homeAct = (ActivityImpl) popFac.createActivityFromCoord("home", stops[0].getCoord());
		homeAct.setEndTime(startTime - 10);
		plan.addActivity(homeAct);
		plan.addLeg(popFac.createLeg("pt"));
			
		ActivityImpl workAct = (ActivityImpl) popFac.createActivityFromCoord("work", stops[1].getCoord());
		workAct.setEndTime(startTime + workDuration - 10);
		plan.addActivity(workAct);
		plan.addLeg(popFac.createLeg("pt"));
			
		Activity homeAct2 = popFac.createActivityFromCoord("home", homeAct.getCoord());
		plan.addActivity(homeAct2);
			
		p.addPlan(plan);
		pop.addPerson(p);
		
		/* ************** *
		 * Setup Strategy *
		 * ************** */
		
		StrategySettings stratSets = new StrategySettings(scen.createId("1"));
		stratSets.setModuleName("SelectExpBeta");
		stratSets.setProbability(0.8);
		config.strategy().addStrategySettings(stratSets);
		stratSets = new StrategySettings(scen.createId("2"));
		// Note: this used to be TransitTimeAllocationMutator, but this mutator does not exist any more (at least not by that name?).
		// Anyway, the test does not really depend on the strategy module selected here.
		stratSets.setModuleName("TimeAllocationMutator");
		stratSets.setProbability(0.2);
		config.strategy().addStrategySettings(stratSets);
		
		return scen;		
	}
	
	private static void setDist(Link l)
	{
		Coord f = l.getFromNode().getCoord();
		Coord t = l.getToNode().getCoord();
		double xDiff = f.getX() - t.getX();
		double yDiff = f.getY() - t.getY();
		l.setLength( Math.sqrt( xDiff*xDiff + yDiff*yDiff));
	}

	
}
