/* *********************************************************************** *
 * project: matsim
 * CreateMarathonPopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.icem2012;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.MultiModalLegRouter;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.BikeTravelTimeFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.MultiModalTravelTimeWrapper;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.MultiModalTravelTimeWrapperFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.PTTravelTimeFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.RideTravelTimeFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.WalkTravelTimeFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.tools.MultiModalNetworkCreator;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.FastDijkstra;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTimeCalculatorFactory;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsFactory;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class CreateMarathonPopulation {
	
	private static Logger log = Logger.getLogger(CreateMarathonPopulation.class);
	
	private final int runners = 1000;
	
	private final String trackModes = "walk2d,walk,bike";
	public static final String startLink = "106474";
	public static final String endLink = "106473";
//	private final double populationFraction = 0.04;	// 0.04 from 25% -> total 1% 
	private final double populationFraction = 1.00;
	
	// shifted
	public static final String[] trackNodes = new String[]{	
			"2952", "2759", "2951", "2531", "2530", "2529", "4263", "4268", "4468", "3496",
			"2530_shifted", "2531_shifted", "2951_shifted", "4508_shifted", "4507_shifted", 
			"4505_shifted", "4504_shifted", "4504", "4505", "4503", "4506", "4508", 
			"2951_shifted_shifted", "2759_shifted", "2758", "2952", "2759", "2951", "2531", 
			"2530", "2529", "2528", "2527", "2526", "2525", "2524", "2523", "2522", "2521", 
			"4239", "2522_shifted", "2523_shifted", "2524_shifted", "2525_shifted", 
			"2526_shifted", "2527_shifted", "2528_shifted", "2529_shifted", "2530_shifted",
			"2531_shifted", "2951_shifted", "4508_shifted", "4507_shifted", "4505_shifted", 
			"4504_shifted", "4504", "4505", "4503", "4506", "4508", "2951_shifted_shifted", 
			"2759_shifted", "2952"};
	
	// track related links
	public static final String[] trackRelatedLinks = new String[]{
			"110604", "108031", "111637", "108032", "111638", "111639", "111640", "111645",
			"111646", "111647", "111648", "111649", "111650", "111651", "111652", "111677",
			"111678", "2521_4239", "2522_2522_shifted", "2522_shifted_2522", "2523_2523_shifted", 
			"110445", "2523_shifted_2523", "110446", "2524_2524_shifted", "2524_shifted_2524",
			"106069", "2525_2525_shifted", "106070", "2525_shifted_2525", "2526_2526_shifted",
			"2526_shifted_2526", "2527_2527_shifted", "2527_shifted_2527", "2528_2528_shifted",
			"2528_shifted_2528", "2529_2529_shifted", "2529_shifted_2529", "2530_2530_shifted",
			"2530_shifted_2530", "2531_2531_shifted", "2531_shifted_2531", "2759_2759_shifted",
			"2759_shifted_2759", "2951_2951_shifted", "2951_shifted_2951",
			"2951_shifted_2951_shifted_shifted", "111499", "2951_shifted_shifted_2951_shifted",
			"111500", "4504_4504_shifted", "4504_shifted_4504", "4505_4505_shifted", "111503",
			"4505_shifted_4505", "111504", "4507_4507_shifted", "4507_shifted_4507", 
			"4508_4508_shifted", "4508_shifted_4508", "110547", "110548", "110555", "110556",
			"105673", "105674", "105675", "105676", "105677", "105678", "105679", "105680",
			"105681", "105682", "105683", "105685", "105686", "105687", "105688", "105689", 
			"105690", "105691", "106469", "105692", "106470", "106471", "106472", "106473",
			"106474", "111635", "111636", "105684", "110603"};
	
	private NetworkRoute route;
	private Households households;
	
//	private String basePath = "D:/Users/Christoph/workspace/matsim/mysimulations/icem2012/";
	private String basePath = "/home/cdobler/workspace/matsim/mysimulations/icem2012/";
	private String trackShapeOutFile = basePath + "input/track.shp";
	private String trackRelatedShapeOutFile = basePath + "input/trackRelated.shp";
	private String barriersShapeOutFile = basePath + "input/barriers.shp";
	private String networkInFile = basePath + "input_zh/network_ivtch.xml.gz";
	private String networkOutFile = basePath + "input/network_ivtch.xml.gz";
	private String networkSHPFile = basePath + "input/network_ivtch.shp";
	private String facilitiesInFile = basePath + "input_zh/facilities.xml.gz";
	private String populationInFile = basePath + "input_zh/plans_25pct.xml.gz";
	private String populationOutFile = basePath + "input/plans_25pct.xml.gz";
	private String householdsHoutFile = basePath + "input/households_25pct.xml.gz";
	
	public static void main(String[] args) {
		new CreateMarathonPopulation();
	}
	
	/*
	 * TODO: create audience population
	 */
	public CreateMarathonPopulation() {

		Config config = ConfigUtils.createConfig();
		config.global().setNumberOfThreads(2);
		config.network().setInputFile(networkInFile);
		config.facilities().setInputFile(facilitiesInFile);
		config.plans().setInputFile(populationInFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		addNodesToNetwork(scenario);
		
		moveLinksToShiftedNodes(scenario);
		
		addLinksToNetwork(scenario);
		
		createRoute(scenario);
		
		prepareNetwork(scenario);
		
		writeTrack(scenario);
		
		writeTrackRelated(scenario);
		
		writeBarriers(scenario);

		adaptBackgroundPopulation(scenario);
		
		createPopulation(scenario);
		
		writePopulation(scenario);
		
//		createHouseholds(scenario);
//		
//		writeHouseholds(scenario);
	}
	
//	private void readAffectedArea() {
//		SHPFileUtil util = new SHPFileUtil();
//		Set<Feature> features = new HashSet<Feature>();
//		features.addAll(util.readFile(affectedAreaFile));		
//		this.affectedArea = util.mergeGeomgetries(features);
//		
//		this.coordAnalyzer = new CoordAnalyzer(this.affectedArea); 
//	}
	
	private void createRoute(Scenario scenario) {
		
		List<Id> nodeIds = new ArrayList<Id>();
		for (String nodeId : trackNodes) {
			Id id = scenario.createId(nodeId);
			nodeIds.add(id);
		}
		
		List<Id> linkIds = new ArrayList<Id>();
		for (int i = 0; i < nodeIds.size() - 1; i++) {
			Id fromId = nodeIds.get(i);
			Id toId = nodeIds.get(i + 1);
			
			Node fromNode = scenario.getNetwork().getNodes().get(fromId);
			for (Link link : fromNode.getOutLinks().values()) {
				if (link.getToNode().getId().equals(toId)) {
					linkIds.add(link.getId());
					break;
				}
			}
		}
		
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		Id startLinkId = linkIds.remove(0);
		Id endLinkId = linkIds.remove(linkIds.size() - 1);
		route = (NetworkRoute) routeFactory.createRoute(startLinkId, endLinkId);
		route.setLinkIds(startLinkId, linkIds, endLinkId);
	}
		
	/*
	 * Add some nodes to the network which are required for the track.
	 */
	private void addNodesToNetwork(Scenario scenario) {
		createShiftedNode(scenario, "4504", 3.0, 0.0);
		createShiftedNode(scenario, "4505", 3.0, 0.0);
		createShiftedNode(scenario, "4507", 3.0, 0.0);
		createShiftedNode(scenario, "4508", 3.0, 0.0);
		createShiftedNode(scenario, "2951", 3.0, 5.0);
		createShiftedNode(scenario, "2951_shifted", -6.0, -5.0);
		createShiftedNode(scenario, "2531", 3.0, 5.0);
		createShiftedNode(scenario, "2530", 3.0, 3.0);
		createShiftedNode(scenario, "2529", 3.0, 0.0);
		createShiftedNode(scenario, "2528", 3.0, 0.0);
		createShiftedNode(scenario, "2527", 3.0, 0.0);
		createShiftedNode(scenario, "2526", 3.0, 0.0);
		createShiftedNode(scenario, "2525", 3.0, 0.0);
		createShiftedNode(scenario, "2524", 3.0, 0.0);
		createShiftedNode(scenario, "2523", 3.0, 0.0);
		createShiftedNode(scenario, "2522", 3.0, 0.0);
		createShiftedNode(scenario, "2759", -1.0, 3.0);
		
		connectedShiftedNodes(scenario, "4504");
		connectedShiftedNodes(scenario, "4505");
		connectedShiftedNodes(scenario, "4507");
		connectedShiftedNodes(scenario, "4508");
		connectedShiftedNodes(scenario, "2951");
		connectedShiftedNodes(scenario, "2951_shifted");
		connectedShiftedNodes(scenario, "2531");
		connectedShiftedNodes(scenario, "2530");
		connectedShiftedNodes(scenario, "2529");
		connectedShiftedNodes(scenario, "2528");
		connectedShiftedNodes(scenario, "2527");
		connectedShiftedNodes(scenario, "2526");
		connectedShiftedNodes(scenario, "2525");
		connectedShiftedNodes(scenario, "2524");
		connectedShiftedNodes(scenario, "2523");
		connectedShiftedNodes(scenario, "2522");
		connectedShiftedNodes(scenario, "2759");
	}
	
	private void createShiftedNode(Scenario scenario, String nodeId, double dx, double dy) {
		Node node = scenario.getNetwork().getNodes().get(scenario.createId(nodeId));
		Coord coord = scenario.createCoord(node.getCoord().getX() + dx, node.getCoord().getY() + dy);
		Id id = scenario.createId(nodeId + "_shifted");
		Node shiftedNode = scenario.getNetwork().getFactory().createNode(id, coord);
		scenario.getNetwork().addNode(shiftedNode);
	}
	
	private void moveLinksToShiftedNodes(Scenario scenario) {
		
		moveLink(scenario, "4239", "2522", "4239", "2522_shifted");
		moveLink(scenario, "2522", "2523", "2522_shifted", "2523_shifted");
		moveLink(scenario, "2523", "2524", "2523_shifted", "2524_shifted");
		moveLink(scenario, "2524", "2525", "2524_shifted", "2525_shifted");
		moveLink(scenario, "2525", "2526", "2525_shifted", "2526_shifted");
		moveLink(scenario, "2526", "2527", "2526_shifted", "2527_shifted");
		moveLink(scenario, "2527", "2528", "2527_shifted", "2528_shifted");
		moveLink(scenario, "2528", "2529", "2528_shifted", "2529_shifted");
		moveLink(scenario, "2529", "2530", "2529_shifted", "2530_shifted");
		moveLink(scenario, "3496", "2530", "3496", "2530_shifted");
		moveLink(scenario, "2530", "2531", "2530_shifted", "2531_shifted");
		moveLink(scenario, "2531", "2951", "2531_shifted", "2951_shifted");
		moveLink(scenario, "2951", "4508", "2951_shifted", "4508_shifted");
		moveLink(scenario, "4508", "4507", "4508_shifted", "4507_shifted");
		moveLink(scenario, "4507", "4505", "4507_shifted", "4505_shifted");
		moveLink(scenario, "4505", "4504", "4505_shifted", "4504_shifted");
		moveLink(scenario, "4508", "2951", "4508", "2951_shifted_shifted");
		moveLink(scenario, "2951", "2759", "2951_shifted_shifted", "2759_shifted");
		moveLink(scenario, "2759", "2952", "2759_shifted", "2952");
		moveLink(scenario, "2759", "2758", "2759_shifted", "2758");
		
//		moveLink(scenario, "", "", "_shifted", "_shifted");
	}
	
	private void moveLink(Scenario scenario, String oldFrom, String oldTo, String newFrom, String newTo) {
		Id oldFromId = scenario.createId(oldFrom);
		Id oldToId = scenario.createId(oldTo);
		Id newFromId = scenario.createId(newFrom);
		Id newToId = scenario.createId(newTo);
		
		Node from = scenario.getNetwork().getNodes().get(oldFromId);
		Node to = scenario.getNetwork().getNodes().get(oldToId);
		for (Link link : from.getOutLinks().values()) {
			if (link.getToNode().getId().equals(oldToId)) {
				
				// remove link from old nodes
				((NodeImpl) from).removeOutLink(link);
				((NodeImpl) to).removeInLink(link);
				
				from = scenario.getNetwork().getNodes().get(newFromId);
				to = scenario.getNetwork().getNodes().get(newToId);

				// add link to shifted nodes
				((NodeImpl) from).addOutLink(link);
				((NodeImpl) to).addInLink(link);
				link.setFromNode(from);
				link.setToNode(to);
				
				break;
			}
		}
	}
	
	private void connectedShiftedNodes(Scenario scenario, String node) {
		Set<String> modes = CollectionUtils.stringToSet("walk2d");
		
		String node1 = node;
		String node2 = node + "_shifted";
		Node n1 = scenario.getNetwork().getNodes().get(scenario.createId(node1));
		Node n2 = scenario.getNetwork().getNodes().get(scenario.createId(node2));
		Id linkId;
		Link link;
		double width;
		double cap;
		double lanes;
		
		linkId = scenario.createId(node1 + "_" + node2);
		link = scenario.getNetwork().getFactory().createLink(linkId, n1, n2);
		link.setLength(CoordUtils.calcDistance(n1.getCoord(), n2.getCoord()));
		width = 4;
		cap = width * 1.33;
		lanes = 5.4 * 0.26 * width;
		link.setNumberOfLanes(lanes);
		link.setCapacity(cap);
		link.setAllowedModes(modes);
		scenario.getNetwork().addLink(link);
		
		linkId = scenario.createId(node2 + "_" + node1);
		link = scenario.getNetwork().getFactory().createLink(linkId, n2, n1);
		link.setLength(CoordUtils.calcDistance(n1.getCoord(), n2.getCoord()));
		width = 4;
		cap = width * 1.33;
		lanes = 5.4 * 0.26 * width;
		link.setNumberOfLanes(lanes);
		link.setCapacity(cap);
		link.setAllowedModes(modes);
		scenario.getNetwork().addLink(link);
	}
	
	/*
	 * Add some links to the network which are required for the track.
	 */
	private void addLinksToNetwork(Scenario scenario) {
		
		Set<String> modes = CollectionUtils.stringToSet("walk2d");
		
		// add a new link at the turning point
		Id fromNodeId = scenario.createId("2521");
		Id toNodeId = scenario.createId("4239");
		Node fromNode = scenario.getNetwork().getNodes().get(fromNodeId);
		Node toNode = scenario.getNetwork().getNodes().get(toNodeId);
		Id linkId = scenario.createId("2521_4239");
		Link link = scenario.getNetwork().getFactory().createLink(linkId, fromNode, toNode);
		link.setLength(CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord()));
		double width = 8;
		double cap = width * 1.33;
		double lanes = 5.4 * 0.26 * width;
		link.setNumberOfLanes(lanes);
		link.setCapacity(cap);
		link.setAllowedModes(modes);
		scenario.getNetwork().addLink(link);
	}
	
	private void prepareNetwork(Scenario scenario) {
		
		// convert zurich network to multi-modal network
		Config config = scenario.getConfig();
		config.multiModal().setCreateMultiModalNetwork(true);
		config.multiModal().setCutoffValueForNonCarModes(80/3.6);
		config.multiModal().setSimulatedModes("walk,ride,bike,pt");
		new MultiModalNetworkCreator(config.multiModal()).run(scenario.getNetwork());
		
		/*
		 * Adapt links that are used for the track.
		 * Ensure that links on the track are only used by runners, walkers and bikers.
		 */
		Set<String> modes = CollectionUtils.stringToSet(trackModes);
		double width = 8;
		double cap = width * 1.33;
		double lanes = 5.4*0.26 * width;
		Set<Id> linksToAdapt = new HashSet<Id>();
		linksToAdapt.add(route.getStartLinkId());
		linksToAdapt.add(route.getEndLinkId());
		linksToAdapt.addAll(route.getLinkIds());
		for (Id id : linksToAdapt) {
			Link link = scenario.getNetwork().getLinks().get(id);			
			link.setNumberOfLanes(lanes);
			link.setCapacity(cap);
			link.setFreespeed(10.0);
			link.setAllowedModes(modes);
		}

		/*
		 * Add walk2d as valid mode to all links which allow walk.
		 */
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.walk)) {
				Set<String> newModes = new HashSet<String>(link.getAllowedModes());
				newModes.add("walk2d");
				link.setAllowedModes(newModes);
			}
		}
		
		new NetworkWriter(scenario.getNetwork()).write(networkOutFile);
		
		new Links2ESRIShape(scenario.getNetwork(), networkSHPFile, "EPSG:4326").write();
	}
	
	private void writeBarriers(Scenario scenario) {
		try {
			log.info("writing barriers to shp file...");
			GeometryFactory geofac = new GeometryFactory();
			List<LineString> barriers = new ArrayList<LineString>();
			
			Coordinate[] barrier;
			Coord c1;
			Coord c2;

			// barrier between 4504 and 4505
			barrier = new Coordinate[2];
			c1 = scenario.getNetwork().getNodes().get(scenario.createId("4504")).getCoord();
			c2 = scenario.getNetwork().getNodes().get(scenario.createId("4504_shifted")).getCoord();
			barrier[0] = new Coordinate((c1.getX() + c2.getX()) * 0.5 - 0.3, (c1.getY() + c2.getY()) * 0.5 - 1.0);			
			c1 = scenario.getNetwork().getNodes().get(scenario.createId("4505")).getCoord();
			c2 = scenario.getNetwork().getNodes().get(scenario.createId("4505_shifted")).getCoord();
			barrier[1] = new Coordinate((c1.getX() + c2.getX()) * 0.5 + 0.3, (c1.getY() + c2.getY()) * 0.5 + 1.0);
			barriers.add(geofac.createLineString(barrier));

			// barrier between 4508 and 2951
			barrier = new Coordinate[2];
			c1 = scenario.getNetwork().getNodes().get(scenario.createId("4508")).getCoord();
			c2 = scenario.getNetwork().getNodes().get(scenario.createId("4508_shifted")).getCoord();
			barrier[0] = new Coordinate((c1.getX() + c2.getX()) * 0.5 + 0.3, (c1.getY() + c2.getY()) * 0.5 - 1.0);
			c1 = scenario.getNetwork().getNodes().get(scenario.createId("2951_shifted")).getCoord();
			c2 = scenario.getNetwork().getNodes().get(scenario.createId("2951_shifted_shifted")).getCoord();
			barrier[1] = new Coordinate((c1.getX() + c2.getX()) * 0.5 - 0.3, (c1.getY() + c2.getY()) * 0.5 + 1.0);
			barriers.add(geofac.createLineString(barrier));

			// barrier between 2951 and 2531
			barrier = new Coordinate[2];
			c1 = scenario.getNetwork().getNodes().get(scenario.createId("2951")).getCoord();
			c2 = scenario.getNetwork().getNodes().get(scenario.createId("2951_shifted")).getCoord();
			barrier[0] = new Coordinate((c1.getX() + c2.getX()) * 0.5 + 1.0, (c1.getY() + c2.getY()) * 0.5 + 0.3);
			c1 = scenario.getNetwork().getNodes().get(scenario.createId("2531")).getCoord();
			c2 = scenario.getNetwork().getNodes().get(scenario.createId("2531_shifted")).getCoord();
			barrier[1] = new Coordinate((c1.getX() + c2.getX()) * 0.5 - 3.0, (c1.getY() + c2.getY()) * 0.5 - 0.9);
			barriers.add(geofac.createLineString(barrier));

			// barrier between 2531 and 2530
			barrier = new Coordinate[2];
			c1 = scenario.getNetwork().getNodes().get(scenario.createId("2531")).getCoord();
			c2 = scenario.getNetwork().getNodes().get(scenario.createId("2531_shifted")).getCoord();
			barrier[0] = new Coordinate((c1.getX() + c2.getX()) * 0.5 + 1.0, (c1.getY() + c2.getY()) * 0.5 - 2.0);
			c1 = scenario.getNetwork().getNodes().get(scenario.createId("2530")).getCoord();
			c2 = scenario.getNetwork().getNodes().get(scenario.createId("2530_shifted")).getCoord();
			barrier[1] = new Coordinate((c1.getX() + c2.getX()) * 0.5 - 0.5, (c1.getY() + c2.getY()) * 0.5 + 1.0);
			barriers.add(geofac.createLineString(barrier));

			// barrier left and right the start/end link (2759 to 2952)
			barrier = new Coordinate[2];
			c1 = scenario.getNetwork().getNodes().get(scenario.createId("2759")).getCoord();
			c2 = scenario.getNetwork().getNodes().get(scenario.createId("2952")).getCoord();
			barrier[0] = new Coordinate(c1.getX() - 5.0, c1.getY() - 5.0);
			barrier[1] = new Coordinate(c2.getX() - 5.0, c2.getY() + 100.0);
			barriers.add(geofac.createLineString(barrier));
			barrier = new Coordinate[2];
			barrier[0] = new Coordinate(c1.getX() + 5.0, c1.getY() - 5.0);
			barrier[1] = new Coordinate(c2.getX() + 5.0, c2.getY() + 100.0);
			barriers.add(geofac.createLineString(barrier));
			
			CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG: 4326");
			AttributeType l = DefaultAttributeTypeFactory.newAttributeType("LineString", LineString.class, true, null, null, targetCRS);
			AttributeType z = AttributeTypeFactory.newAttributeType("dblAvgZ", Double.class);
			AttributeType t = AttributeTypeFactory.newAttributeType("name", String.class);
			FeatureType ftLine = FeatureTypeBuilder.newFeatureType(new AttributeType[] {l, z, t}, "Line");
			
			Collection<Feature> fts = new ArrayList<Feature>();
			for (LineString ls : barriers) {
				fts.add(ftLine.create(new Object[] {ls, 0, "barrier"}));				
			}
			ShapeFileWriter.writeGeometries(fts, barriersShapeOutFile);
			log.info("done");
		} catch (Exception e) {
			Gbl.errorMsg(e);
		}
	}
	
	private void writeTrack(Scenario scenario) {
		try {
			log.info("writing track to shp file...");
			Coordinate[] track = new Coordinate[trackNodes.length];
			
			int i = 0;
			for (String nodeId : trackNodes) {
				Id id = scenario.createId(nodeId);
				Node node = scenario.getNetwork().getNodes().get(id);
				track[i] = new Coordinate(node.getCoord().getX(), node.getCoord().getY());
				i++;
			}
			
			GeometryFactory geofac = new GeometryFactory();
			LineString ls = geofac.createLineString(track);
			Collection<Feature> fts = new  ArrayList<Feature>();
			
			CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG: 4326");
			AttributeType l = DefaultAttributeTypeFactory.newAttributeType("LineString", LineString.class, true, null, null, targetCRS);
			AttributeType z = AttributeTypeFactory.newAttributeType("dblAvgZ", Double.class);
			AttributeType t = AttributeTypeFactory.newAttributeType("name", String.class);
			FeatureType ftLine = FeatureTypeBuilder.newFeatureType(new AttributeType[] {l, z, t}, "Line");
			
			fts.add(ftLine.create(new Object[] {ls, 0, "track"}));
			ShapeFileWriter.writeGeometries(fts, trackShapeOutFile);
			log.info("done");
		} catch (Exception e) {
			Gbl.errorMsg(e);
		}
	}
	
	private void writeTrackRelated(Scenario scenario) {
		try {
			log.info("writing track related links to shp file...");
					
			GeometryFactory geofac = new GeometryFactory();
			Collection<Feature> fts = new  ArrayList<Feature>();
			
			CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG: 4326");
			AttributeType l = DefaultAttributeTypeFactory.newAttributeType("LineString", LineString.class, true, null, null, targetCRS);
			AttributeType z = AttributeTypeFactory.newAttributeType("dblAvgZ", Double.class);
			AttributeType t = AttributeTypeFactory.newAttributeType("name", String.class);
			FeatureType ftLine = FeatureTypeBuilder.newFeatureType(new AttributeType[] {l, z, t}, "Line");
			
			for (String linkId : trackRelatedLinks) {
				Id id = scenario.createId(linkId);
				Link link = scenario.getNetwork().getLinks().get(id);
				Coordinate[] line = new Coordinate[2];
				line[0] = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
				line[1] = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
				LineString ls = geofac.createLineString(line);
				fts.add(ftLine.create(new Object[] {ls, 0, linkId}));
			}

			ShapeFileWriter.writeGeometries(fts, trackRelatedShapeOutFile);
			log.info("done");
		} catch (Exception e) {
			Gbl.errorMsg(e);
		}
	}
	
	private void adaptBackgroundPopulation(Scenario scenario) {
		
		/*
		 * Temporary set modes on track only to walk2d. As a result,
		 * the router will not send other agents over track links.
		 */
		Network network = scenario.getNetwork();
//		Set<Id> trackLinks = new HashSet<Id>();
//		trackLinks.add(route.getStartLinkId());
//		trackLinks.addAll(route.getLinkIds());
//		trackLinks.add(route.getEndLinkId());
		
		// includes track links and their counter links
		Set<Id> trackRelatedLinks = new HashSet<Id>();
		for (String linkId : CreateMarathonPopulation.trackRelatedLinks) {
			Id id = scenario.createId(linkId);
			trackRelatedLinks.add(id);
		}
		
		// allow only walk2d legs until the background populations is created
		Set<String> modes = CollectionUtils.stringToSet("walk2d");
		for (Id linkId : trackRelatedLinks) {
			network.getLinks().get(linkId).setAllowedModes(modes);		
		}
		
		PlansCalcRouteConfigGroup configGroup = scenario.getConfig().plansCalcRoute();
		MultiModalTravelTimeWrapperFactory multiModalTravelTimeFactory = new MultiModalTravelTimeWrapperFactory();
		multiModalTravelTimeFactory.setPersonalizableTravelTimeFactory(TransportMode.walk, new WalkTravelTimeFactory(configGroup));
		multiModalTravelTimeFactory.setPersonalizableTravelTimeFactory(TransportMode.bike, new BikeTravelTimeFactory(configGroup,
				new WalkTravelTimeFactory(configGroup)));
		multiModalTravelTimeFactory.setPersonalizableTravelTimeFactory(TransportMode.ride, new RideTravelTimeFactory(new FreeSpeedTravelTimeCalculatorFactory(), 
				new WalkTravelTimeFactory(configGroup)));
		multiModalTravelTimeFactory.setPersonalizableTravelTimeFactory(TransportMode.pt, new PTTravelTimeFactory(configGroup, 
				new FreeSpeedTravelTimeCalculatorFactory(), new WalkTravelTimeFactory(configGroup)));

		MultiModalTravelTimeWrapper wrapper = multiModalTravelTimeFactory.createTravelTime();
		TravelDisutility cost = new TravelCostCalculatorFactoryImpl().createTravelDisutility(wrapper, scenario.getConfig().planCalcScore());
		Dijkstra dijkstra = new FastDijkstra(network, cost, wrapper);
		MultiModalLegRouter legRouter = new MultiModalLegRouter(scenario.getNetwork(), wrapper, dijkstra);
		
		Counter counterRemove = new Counter("persons to remove ");
		Counter counterAdapt = new Counter("legs to adapt ");
		
		Set<Id> personsToRemove = new HashSet<Id>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			
			for (int index = 0; index < plan.getPlanElements().size(); index++) {
				PlanElement planElement = plan.getPlanElements().get(index);
				
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					
					String type = activity.getType();
					if (type.startsWith("w")) {		
						activity.setType("work");
					} else if (type.startsWith("h")) {
						activity.setType("home");
					} else if (type.startsWith("s")) {
						activity.setType("shop");
					} else if (type.startsWith("l")) {
						activity.setType("leisure");
					} else if (type.startsWith("e")) {
						activity.setType("education");
					} else if (type.startsWith("t")) {
						activity.setType("tta");
					}
					else log.warn("Unknown activity type: " + type);
					
					boolean isOnTrack = trackRelatedLinks.contains(activity.getLinkId());
					if (isOnTrack) {
						personsToRemove.add(person.getId());
						counterRemove.incCounter();
						break;
					}
				} else if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					Route route = leg.getRoute();
					
					// if its a walk2d agent from the PED 2012 population remove it
					if (leg.getMode().equals("walk2d")) {
						personsToRemove.add(person.getId());
						counterRemove.incCounter();
						break;
					}
					
					if (trackRelatedLinks.contains(route.getStartLinkId())) {
						personsToRemove.add(person.getId());
						counterRemove.incCounter();
						break;						
					} else if (trackRelatedLinks.contains(route.getEndLinkId())) {
						personsToRemove.add(person.getId());
						counterRemove.incCounter();
						break;
					} else {
						boolean needsRerouting = false;
						if (route instanceof NetworkRoute) {
							NetworkRoute networkRoute = (NetworkRoute) route;
							
							for (Id linkId : networkRoute.getLinkIds()) {
								if (trackRelatedLinks.contains(linkId)) {
									needsRerouting = true;
									break;
								}
							}	
						} else needsRerouting = true;
						
						if (needsRerouting) {
							leg.setRoute(null);
							counterAdapt.incCounter();
							
							Activity fromAct = (Activity) plan.getPlanElements().get(index-1);
							Activity toAct = (Activity) plan.getPlanElements().get(index+1);
							try {
								legRouter.routeLeg(person, leg, fromAct, toAct, leg.getDepartureTime());							
							}
							/*
							 * If no route is found, a RuntimeException is thrown.
							 * We catch it and remove the agent from the population.
							 */
							catch (RuntimeException e) {
								personsToRemove.add(person.getId());
								counterRemove.incCounter();
								break;
							}
						}
					}
				}
			}
		}
		counterAdapt.printCounter();
		counterRemove.printCounter();
		
		for (Id personId : personsToRemove) {
			scenario.getPopulation().getPersons().remove(personId);
		}
		
		/*
		 * Restore original track modes.
		 */
		modes = CollectionUtils.stringToSet(this.trackModes);
		for (Id linkId : trackRelatedLinks) {
			network.getLinks().get(linkId).setAllowedModes(modes);		
		}
	}
	
	private void createPopulation(Scenario scenario) {
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		
		Id startLinkId = scenario.createId(startLink);
		Link startLink = scenario.getNetwork().getLinks().get(startLinkId);
		
		Id endLinkId = scenario.createId(endLink);
		
		/*
		 * We create a vector from the end to the start of the link since we
		 * start placing runners at the links end and move to the links start
		 */
		Coord fromCoord = startLink.getFromNode().getCoord();
		Coord toCoord = startLink.getToNode().getCoord();
		double dx = fromCoord.getX() - toCoord.getX();
		double dy = fromCoord.getY() - toCoord.getY();
		double dxy = CoordUtils.calcDistance(fromCoord, toCoord);
		dx = dx/dxy;
		dy = dy/dxy;
		
		// start 100m from links end
		double xStart = toCoord.getX() + 100*dx;
		double yStart = toCoord.getY() + 100*dy;
		
		// shift start point to the corner of the starter field
		xStart -= 4*dy;
		yStart -= 4*dx;
		
		Random random = MatsimRandom.getLocalInstance();
		int row = 1;
		int column = 0;
		int rowRunners = 8 + random.nextInt(5);	// 8..12 runners per row
		double dColumn = 8. / (rowRunners - 1);	// width of road: 8m
		
		for (int personCount = 0; personCount < runners; personCount++) {
			
			PersonImpl person = (PersonImpl) populationFactory.createPerson(scenario.createId("runner_" + personCount));

			// set random age and gender
			person.setAge((int)(18 + Math.round(random.nextDouble() * 47)));	// 18 .. 65 years old
			if (random.nextDouble() > 0.5) person.setSex("m");
			else person.setSex("f");
			
			Plan plan = populationFactory.createPlan();
			person.addPlan(plan);
			
			ActivityImpl activity;
			
			double x = xStart;
			double y = yStart;
			
			// move to row position
			x += 0.75*row*dx;
			y += 0.75*row*dy;
			
			// move to column position
			x += dColumn*column*dy;
			y += dColumn*column*dx;
			
			// add some random noise (+/- 0.25m in x and y direction)
			x += 0.5*random.nextDouble() - 0.5;
			y += 0.5*random.nextDouble() - 0.5;
			
			column++;
			if (column >= rowRunners) {
				column = 0;
				row++;
				rowRunners = 8 + random.nextInt(5);	// 8..12 runners per row
				dColumn = 8. / (rowRunners - 1);
			}
			
//			System.out.println(person.getId().toString() + "\t" + x + "\t" + y);
			
			Coord coord = scenario.createCoord(x, y);
			activity = (ActivityImpl) populationFactory.createActivityFromLinkId("preRun", startLinkId);
			activity.setFacilityId(scenario.createId("preRunFacility"));
			activity.setEndTime(9*3600);
			activity.setCoord(coord);
			plan.addActivity(activity);
			
			Leg leg = populationFactory.createLeg("walk2d");
			leg.setRoute(route.clone());
			plan.addLeg(leg);
			
			activity = (ActivityImpl) populationFactory.createActivityFromLinkId("postRun", endLinkId);
			activity.setFacilityId(scenario.createId("postRunFacility"));
			activity.setCoord(coord);
			plan.addActivity(activity);
			
			scenario.getPopulation().addPerson(person);
		}	
	}

	private void writePopulation(Scenario scenario) {
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(), this.populationFraction).writeFileV5(populationOutFile);
	}
	
	/*
	 * Some parts of the evacuation code is based on household to make
	 * decisions on household level. To be able to use this code we create
	 * dummy households where each agent is part of a single person household.
	 */
	private void createHouseholds(Scenario scenario) {
		
		households = new HouseholdsImpl();
		HouseholdsFactory factory = households.getFactory();
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Household household = factory.createHousehold(person.getId());
			household.getMemberIds().add(person.getId());
			households.getHouseholds().put(household.getId(), household);
		}
	}
	
	private void writeHouseholds(Scenario scenario) {
		new HouseholdsWriterV10(households).writeFile(this.householdsHoutFile);
	}
}