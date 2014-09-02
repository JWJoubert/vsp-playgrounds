package org.matsim.contrib.josm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * The Task that handles the convert action
 * 
 * @author Nico
 * 
 */

class ConvertTask extends PleaseWaitRunnable {

	private MATSimLayer newLayer;

	/**
	 * Creates a new Convert task
	 * 
	 * @see PleaseWaitRunnable
	 */
	public ConvertTask() {
		super("Converting to MATSim Network");
	}

	/**
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#cancel()
	 */
	@Override
	protected void cancel() {
		// TODO Auto-generated method stub
	}

	/**
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#realRun()
	 */
	@Override
	protected void realRun() throws SAXException, IOException,
			OsmTransferException {
		this.progressMonitor.setTicksCount(8);
		this.progressMonitor.setTicks(0);

		Layer layer = Main.main.getActiveLayer();

		Scenario tempScenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		tempScenario.getConfig().scenario().setUseTransit(true);
		tempScenario.getConfig().scenario().setUseVehicles(true);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		Network network = scenario.getNetwork();
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);

		this.progressMonitor.setTicks(1);
		this.progressMonitor.setCustomText("converting osm data..");

		NewConverter.convertOsmLayer(((OsmDataLayer) layer), tempScenario,
				new HashMap<Way, List<Link>>(),
				new HashMap<Link, List<WaySegment>>(),
				new HashMap<Relation, TransitRoute>());
		if (Main.pref.getBoolean("matsim_cleanNetwork")) {
			this.progressMonitor.setTicks(2);
			this.progressMonitor.setCustomText("cleaning network..");
			new NetworkCleaner().run(tempScenario.getNetwork());
		}

		this.progressMonitor.setTicks(3);
		this.progressMonitor.setCustomText("preparing data set..");
		DataSet dataSet = new DataSet();

		HashMap<Way, List<Link>> way2Links = new HashMap<Way, List<Link>>();
		HashMap<Link, List<WaySegment>> link2Segment = new HashMap<Link, List<WaySegment>>();
		HashMap<Relation, TransitRoute> relation2Route = new HashMap<Relation, TransitRoute>();
		HashMap<Node, org.openstreetmap.josm.data.osm.Node> node2OsmNode = new HashMap<Node, org.openstreetmap.josm.data.osm.Node>();
		HashMap<TransitStopFacility, TransitStopFacility> stop2Stop = new HashMap<TransitStopFacility, TransitStopFacility>();
		HashMap<Id, Way> linkId2Way = new HashMap<Id, Way>();

		this.progressMonitor.setTicks(4);
		this.progressMonitor.setCustomText("loading nodes..");

		for (Node node : tempScenario.getNetwork().getNodes().values()) {

			Coord tmpCoor = node.getCoord();
			LatLon coor = new LatLon(tmpCoor.getY(), tmpCoor.getX());
			org.openstreetmap.josm.data.osm.Node nodeOsm = new org.openstreetmap.josm.data.osm.Node(
					coor);
			nodeOsm.put(ImportTask.NODE_TAG_ID, ((NodeImpl) node).getOrigId());
			node2OsmNode.put(node, nodeOsm);
			dataSet.addPrimitive(nodeOsm);
			Node newNode = network.getFactory().createNode(
					new IdImpl(Long.toString(nodeOsm.getUniqueId())),
					node.getCoord());
			((NodeImpl) newNode).setOrigId(((NodeImpl) node).getOrigId());
			network.addNode(newNode);
		}

		this.progressMonitor.setTicks(5);
		this.progressMonitor.setCustomText("loading ways..");
		for (Link link : tempScenario.getNetwork().getLinks().values()) {
			Way way = new Way();
			org.openstreetmap.josm.data.osm.Node fromNode = node2OsmNode
					.get(link.getFromNode());
			way.addNode(fromNode);
			org.openstreetmap.josm.data.osm.Node toNode = node2OsmNode.get(link
					.getToNode());
			way.addNode(toNode);
			way.put(ImportTask.WAY_TAG_ID, ((LinkImpl) link).getOrigId());
			way.put("freespeed", String.valueOf(link.getFreespeed()));
			way.put("capacity", String.valueOf(link.getCapacity()));
			way.put("length", String.valueOf(link.getLength()));
			way.put("permlanes", String.valueOf(link.getNumberOfLanes()));
			StringBuilder modes = new StringBuilder();

			for (String mode : link.getAllowedModes()) {
				modes.append(mode);
				if (link.getAllowedModes().size() > 1) {
					modes.append(";");
				}
			}
			way.put("modes", modes.toString());
			dataSet.addPrimitive(way);

			Link newLink = network.getFactory().createLink(
					new IdImpl(Long.toString(way.getUniqueId())),
					network.getNodes().get(
							new IdImpl(Long.toString(fromNode.getUniqueId()))),
					network.getNodes().get(
							new IdImpl(Long.toString(toNode.getUniqueId()))));
			newLink.setFreespeed(link.getFreespeed());
			newLink.setCapacity(link.getCapacity());
			newLink.setLength(link.getLength());
			newLink.setNumberOfLanes(link.getNumberOfLanes());
			newLink.setAllowedModes(link.getAllowedModes());
			((LinkImpl) newLink).setOrigId(((LinkImpl) link).getOrigId());
			network.addLink(newLink);

			way2Links.put(way, Collections.singletonList(newLink));

			linkId2Way.put(link.getId(), way);
			link2Segment.put(newLink,
					Collections.singletonList(new WaySegment(way, 0)));
		}


		this.progressMonitor.setTicks(6);
		this.progressMonitor
				.setCustomText("loading transit line stops and route relations..");
		for (TransitLine line : tempScenario.getTransitSchedule()
				.getTransitLines().values()) {
			TransitLine newLine = scenario.getTransitSchedule().getFactory()
					.createTransitLine(line.getId());
			for (TransitRoute route : line.getRoutes().values()) {

				Relation relation = new Relation();

				List<TransitRouteStop> newTransitStops = new ArrayList<TransitRouteStop>();

				for (TransitRouteStop tRStop : route.getStops()) {

					TransitStopFacility stop = scenario
							.getTransitSchedule()
							.getFactory()
							.createTransitStopFacility(
									tRStop.getStopFacility().getId(),
									tRStop.getStopFacility().getCoord(), true);
					stop.setName(tRStop.getStopFacility().getName());
					Way stopLink2Way = linkId2Way.get(tRStop.getStopFacility()
							.getLinkId());
					stopLink2Way.put("stop_name", tRStop.getStopFacility()
							.getName());
					stopLink2Way.put("stop_id", tRStop.getStopFacility()
							.getId().toString());
					List<Link> newStopLinks = way2Links.get(stopLink2Way);
					Id linkId = newStopLinks.get(newStopLinks.size() - 1)
							.getId();
					stop.setLinkId(linkId);

					newTransitStops.add(scenario.getTransitSchedule()
							.getFactory().createTransitRouteStop(stop, 0, 0));

					scenario.getTransitSchedule().addStopFacility(stop);
				}

				List<Link> startWay2Links = way2Links.get(linkId2Way.get(route
						.getRoute().getStartLinkId()));
				Id firstLinkId = startWay2Links.get(startWay2Links.size() - 1)
						.getId();
				RelationMember start = new RelationMember("stop_link",
						linkId2Way.get(route.getRoute().getStartLinkId()));
				relation.addMember(start);

				List<Id<Link>> links = new ArrayList<Id<Link>>();
				for (Id linkId : route.getRoute().getLinkIds()) {
					RelationMember member = new RelationMember("stop_link",
							linkId2Way.get(linkId));
					relation.addMember(member);
					for (Link link : way2Links.get(linkId2Way.get(linkId))) {
						links.add(link.getId());
					}
				}
				RelationMember end = new RelationMember("stop_link",
						linkId2Way.get(route.getRoute().getEndLinkId()));
				relation.addMember(end);
				List<Link> endWay2Links = way2Links.get(linkId2Way.get(route
						.getRoute().getEndLinkId()));
				Id lastLinkId = endWay2Links.get(endWay2Links.size() - 1)
						.getId();

				NetworkRoute networkRoute = new LinkNetworkRouteImpl(
						firstLinkId, lastLinkId);

				networkRoute.setLinkIds(firstLinkId, links, lastLinkId);

				TransitRoute newRoute = scenario
						.getTransitSchedule()
						.getFactory()
						.createTransitRoute(route.getId(), networkRoute,
								newTransitStops, "pt");
				newLine.addRoute(newRoute);
				relation.put("type", "matsimRoute");
				relation.put("routeId", newRoute.getId().toString());
				relation.put("name", newLine.getId().toString());

				dataSet.addPrimitive(relation);
				relation2Route.put(relation, newRoute);
			}
			scenario.getTransitSchedule().addTransitLine(newLine);
		}

		node2OsmNode.clear();
		stop2Stop.clear();
		linkId2Way.clear();
		
		this.progressMonitor.setTicks(7);
		this.progressMonitor.setCustomText("creating layer..");

		newLayer = new MATSimLayer(dataSet, null, null, scenario,
				TransformationFactory.WGS84, way2Links, link2Segment,
				relation2Route);
	}

	/**
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#finish()
	 */
	@Override
	protected void finish() {
		if (newLayer != null) {
			Main.main.addLayer(newLayer);
			Main.map.mapView.setActiveLayer(newLayer);
		}
	}

}
