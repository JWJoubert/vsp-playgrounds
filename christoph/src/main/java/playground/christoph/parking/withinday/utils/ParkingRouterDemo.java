/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingRouterDemo.java
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

package playground.christoph.parking.withinday.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vehicles.Vehicle;

public class ParkingRouterDemo {

	private static final Logger log = Logger.getLogger(ParkingRouterDemo.class);
	
	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		createNetwork(scenario);
		testAdaptStartOfRoute(scenario, 3);
		testAdaptEndOfRoute(scenario, 3);
		testAdaptStartAndEndOfRoute(scenario, 2);
		testAdaptStartOfShortRoute(scenario, 3);
		testAdaptEndOfShortRoute(scenario, 3);
		testAdaptStartAndEndOfShortRoute(scenario, 3);
	}
	
	private static void testAdaptStartOfRoute(Scenario scenario, int nodesToCheck) {
		
		log.info("testAdaptStartOfRoute");
		
		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
		
		TripRouter tripRouter = null;
		ParkingRouter parkingRouter = new ParkingRouter(scenario, travelTime, travelDisutility, tripRouter, nodesToCheck);
		
		NetworkRoute route = new LinkNetworkRouteImpl(scenario.createId("l0"), scenario.createId("l4"));
		List<Id> routeLinkIds = new ArrayList<Id>();
		routeLinkIds.add(scenario.createId("l1"));
		routeLinkIds.add(scenario.createId("l2"));
		routeLinkIds.add(scenario.createId("l3"));
		route.setLinkIds(scenario.createId("l0"), routeLinkIds, scenario.createId("l4"));
		
		Id startLinkId = scenario.createId("l5");
		double time = 100.0;
		Person person = null;
		Vehicle vehicle = null;
		
		parkingRouter.adaptStartOfRoute(route, startLinkId, time, person, vehicle);
		
		log.info(route.getStartLinkId());
		for(Id linkId : route.getLinkIds()) log.info(linkId);
		log.info(route.getEndLinkId());
	}
	
	private static void testAdaptEndOfRoute(Scenario scenario, int nodesToCheck) {
		
		log.info("testAdaptEndOfRoute");
		
		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
		
		TripRouter tripRouter = null;
		ParkingRouter parkingRouter = new ParkingRouter(scenario, travelTime, travelDisutility, tripRouter, nodesToCheck);
		
		NetworkRoute route = new LinkNetworkRouteImpl(scenario.createId("l0"), scenario.createId("l4"));
		List<Id> routeLinkIds = new ArrayList<Id>();
		routeLinkIds.add(scenario.createId("l1"));
		routeLinkIds.add(scenario.createId("l2"));
		routeLinkIds.add(scenario.createId("l3"));
		route.setLinkIds(scenario.createId("l0"), routeLinkIds, scenario.createId("l4"));
		
		Id endLinkId = scenario.createId("l10");
		double time = 100.0;
		Person person = null;
		Vehicle vehicle = null;
		
		parkingRouter.adaptEndOfRoute(route, endLinkId, time, person, vehicle);
		
		log.info(route.getStartLinkId());
		for(Id linkId : route.getLinkIds()) log.info(linkId);
		log.info(route.getEndLinkId());
	}
	
	private static void testAdaptStartAndEndOfRoute(Scenario scenario, int nodesToCheck) {
		
		log.info("testAdaptStartAndEndOfRoute");
		
		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
		
		TripRouter tripRouter = null;
		ParkingRouter parkingRouter = new ParkingRouter(scenario, travelTime, travelDisutility, tripRouter, nodesToCheck);
		
		NetworkRoute route = new LinkNetworkRouteImpl(scenario.createId("l0"), scenario.createId("l4"));
		List<Id> routeLinkIds = new ArrayList<Id>();
		routeLinkIds.add(scenario.createId("l1"));
		routeLinkIds.add(scenario.createId("l2"));
		routeLinkIds.add(scenario.createId("l3"));
		route.setLinkIds(scenario.createId("l0"), routeLinkIds, scenario.createId("l4"));
		
		Id startLinkId = scenario.createId("l5");
		Id endLinkId = scenario.createId("l10");
		double time = 100.0;
		Person person = null;
		Vehicle vehicle = null;
		
		parkingRouter.adaptStartAndEndOfRoute(route, startLinkId, endLinkId, time, person, vehicle);
		
		log.info(route.getStartLinkId());
		for(Id linkId : route.getLinkIds()) log.info(linkId);
		log.info(route.getEndLinkId());
	}
	
	private static void testAdaptStartOfShortRoute(Scenario scenario, int nodesToCheck) {
		
		log.info("testAdaptStartOfShortRoute");
		
		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
		
		TripRouter tripRouter = null;
		ParkingRouter parkingRouter = new ParkingRouter(scenario, travelTime, travelDisutility, tripRouter, nodesToCheck);
		
		NetworkRoute route = new LinkNetworkRouteImpl(scenario.createId("l3"), scenario.createId("l3"));
		
		Id startLinkId = scenario.createId("l5");
		double time = 100.0;
		Person person = null;
		Vehicle vehicle = null;
		
		parkingRouter.adaptStartOfRoute(route, startLinkId, time, person, vehicle);
		
		log.info(route.getStartLinkId());
		for(Id linkId : route.getLinkIds()) log.info(linkId);
		log.info(route.getEndLinkId());
	}
	
	// route starts and ends on the same link
	private static void testAdaptEndOfShortRoute(Scenario scenario, int nodesToCheck) {
		
		log.info("testAdaptEndOfShortRoute");
		
		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
		
		TripRouter tripRouter = null;
		ParkingRouter parkingRouter = new ParkingRouter(scenario, travelTime, travelDisutility, tripRouter, nodesToCheck);
		
		NetworkRoute route = new LinkNetworkRouteImpl(scenario.createId("l3"), scenario.createId("l3"));
		
		Id endLinkId = scenario.createId("l10");
		double time = 100.0;
		Person person = null;
		Vehicle vehicle = null;
		
		parkingRouter.adaptEndOfRoute(route, endLinkId, time, person, vehicle);
		
		log.info(route.getStartLinkId());
		for(Id linkId : route.getLinkIds()) log.info(linkId);
		log.info(route.getEndLinkId());
	}
	
	private static void testAdaptStartAndEndOfShortRoute(Scenario scenario, int nodesToCheck) {
		
		log.info("testAdaptStartAndEndOfShortRoute");
		
		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
		
		TripRouter tripRouter = null;
		ParkingRouter parkingRouter = new ParkingRouter(scenario, travelTime, travelDisutility, tripRouter, nodesToCheck);
		
		NetworkRoute route = new LinkNetworkRouteImpl(scenario.createId("l3"), scenario.createId("l3"));
		
		Id startLinkId = scenario.createId("l5");
		Id endLinkId = scenario.createId("l10");
		double time = 100.0;
		Person person = null;
		Vehicle vehicle = null;
		
		parkingRouter.adaptStartAndEndOfRoute(route, startLinkId, endLinkId, time, person, vehicle);
		
		log.info(route.getStartLinkId());
		for(Id linkId : route.getLinkIds()) log.info(linkId);
		log.info(route.getEndLinkId());
	}
	
	private static void createNetwork(Scenario scenario) {
		
		NetworkFactory networkFactory = scenario.getNetwork().getFactory();
		
		Node n0 = networkFactory.createNode(scenario.createId("n0"), scenario.createCoord(0.0, 0.0));
		Node n1 = networkFactory.createNode(scenario.createId("n1"), scenario.createCoord(1000.0, 0.0));
		Node n2 = networkFactory.createNode(scenario.createId("n2"), scenario.createCoord(2000.0, 0.0));
		Node n3 = networkFactory.createNode(scenario.createId("n3"), scenario.createCoord(3000.0, 0.0));
		Node n4 = networkFactory.createNode(scenario.createId("n4"), scenario.createCoord(4000.0, 0.0));
		Node n5 = networkFactory.createNode(scenario.createId("n5"), scenario.createCoord(5000.0, 0.0));
		Node n6 = networkFactory.createNode(scenario.createId("n6"), scenario.createCoord(2000.0, -1000.0));
		Node n7 = networkFactory.createNode(scenario.createId("n7"), scenario.createCoord(2000.0, -2000.0));
		Node n8 = networkFactory.createNode(scenario.createId("n8"), scenario.createCoord(4000.0, 1000.0));
		Node n9 = networkFactory.createNode(scenario.createId("n9"), scenario.createCoord(4000.0, 2000.0));
		
		scenario.getNetwork().addNode(n0);
		scenario.getNetwork().addNode(n1);
		scenario.getNetwork().addNode(n2);
		scenario.getNetwork().addNode(n3);
		scenario.getNetwork().addNode(n4);
		scenario.getNetwork().addNode(n5);
		scenario.getNetwork().addNode(n6);
		scenario.getNetwork().addNode(n7);
		scenario.getNetwork().addNode(n8);
		scenario.getNetwork().addNode(n9);

		Link l0 = networkFactory.createLink(scenario.createId("l0"), n0, n1);
		l0.setLength(1000.0);
		l0.setFreespeed(10.0);
		
		Link l1 = networkFactory.createLink(scenario.createId("l1"), n1, n2);
		l1.setLength(1000.0);
		l1.setFreespeed(10.0);
		
		Link l2 = networkFactory.createLink(scenario.createId("l2"), n2, n3);
		l2.setLength(1000.0);
		l2.setFreespeed(10.0);
		
		Link l3 = networkFactory.createLink(scenario.createId("l3"), n3, n4);
		l3.setLength(1000.0);
		l3.setFreespeed(10.0);

		Link l4 = networkFactory.createLink(scenario.createId("l4"), n4, n5);
		l4.setLength(1000.0);
		l4.setFreespeed(10.0);

		Link l5 = networkFactory.createLink(scenario.createId("l5"), n7, n6);
		l5.setLength(1000.0);
		l5.setFreespeed(10.0);
		
		Link l6 = networkFactory.createLink(scenario.createId("l6"), n6, n1);
		l6.setLength(1415.0);
		l6.setFreespeed(10.0);

		Link l7 = networkFactory.createLink(scenario.createId("l7"), n6, n2);
		l7.setLength(1000.0);
		l7.setFreespeed(10.0);

		Link l8 = networkFactory.createLink(scenario.createId("l8"), n6, n3);
		l8.setLength(1415.0);
		l8.setFreespeed(10.0);

		Link l9 = networkFactory.createLink(scenario.createId("l9"), n6, n4);
		l9.setLength(2237.0);
		l9.setFreespeed(10.0);

		Link l10 = networkFactory.createLink(scenario.createId("l10"), n8, n9);
		l10.setLength(1000.0);
		l10.setFreespeed(10.0);

		Link l11 = networkFactory.createLink(scenario.createId("l11"), n2, n8);
		l11.setLength(2237.0);
		l11.setFreespeed(10.0);

		Link l12 = networkFactory.createLink(scenario.createId("l12"), n3, n8);
		l12.setLength(1415.0);
		l12.setFreespeed(10.0);

		Link l13 = networkFactory.createLink(scenario.createId("l13"), n4, n8);
		l13.setLength(1000.0);
		l13.setFreespeed(10.0);

		Link l14 = networkFactory.createLink(scenario.createId("l14"), n5, n8);
		l14.setLength(1415.0);
		l14.setFreespeed(10.0);
		
		scenario.getNetwork().addLink(l0);
		scenario.getNetwork().addLink(l1);
		scenario.getNetwork().addLink(l2);
		scenario.getNetwork().addLink(l3);
		scenario.getNetwork().addLink(l4);
		scenario.getNetwork().addLink(l5);
		scenario.getNetwork().addLink(l6);
		scenario.getNetwork().addLink(l7);
		scenario.getNetwork().addLink(l8);
		scenario.getNetwork().addLink(l9);
		scenario.getNetwork().addLink(l10);
		scenario.getNetwork().addLink(l11);
		scenario.getNetwork().addLink(l12);
		scenario.getNetwork().addLink(l13);
		scenario.getNetwork().addLink(l14);
	}
}