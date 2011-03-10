/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mzilske.bvg09;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufInterpolator;
import org.matsim.pt.UmlaufStueckI;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetwork.StopPoint;
import org.matsim.visum.VisumNetworkReader;
import org.xml.sax.SAXException;

import playground.mzilske.pt.queuesim.GreedyUmlaufBuilderImpl;

public class NullFallFacilityRollout {

	public class InvalidLineException extends Exception {

		private static final long serialVersionUID = 1L;

	}

	private static final Logger log = Logger.getLogger(NullFallFacilityRollout.class);

	private static String path = "E:/_out/nullfall_berlin_brandenburg/";

	private static String InNetworkFile = path + "intermediateNetwork.xml";
	private static String InTransitScheduleFile = path + "mergedTransitSchedule.xml";
	private static String InVisumNetFile = "D:/Berlin/BVG/berlin-bvg09/urdaten/nullfall2009-05-25.net";
	private static String OutNetworkFile = path + "network.xml";
//	private static String OutNetworkFile = "E:/_out/nullfall_berlin_brandenburg/network.xml";
	private static String OutTransitScheduleFile = path + "transitSchedule.xml";
	private static String OutVehicleFile = path + "vehicles.xml";

	private final ScenarioImpl inScenario;
	private final Config inConfig;
	private final ScenarioImpl outScenario;
	private final Config outConfig;
	private Map<TransitStopFacility, Map<Link, TransitStopFacility>> transitStopInLinks = new HashMap<TransitStopFacility, Map<Link, TransitStopFacility>>();
	private VisumNetwork vNetwork;

	private Collection<Umlauf> umlaeufe;



	public static void main(final String[] args) {
		rollOutFacilitiesAndAssignVehicles();
	}

	public NullFallFacilityRollout() {
		this.inScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.inConfig = this.inScenario.getConfig();
		this.outScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.outConfig = this.outScenario.getConfig();
	}

	private static void rollOutFacilitiesAndAssignVehicles() {
		NullFallFacilityRollout app = new NullFallFacilityRollout();
		app.prepareConfig();
		app.loadData();
		app.copyRoutes();
		app.enterFacilities();
		app.emptyVehicles();
		app.buildUmlaeufe();
		app.removeUnusedNetworkParts();
		try {
			app.writeNetworkAndScheduleAndVehicles();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("done.");
	}

	private void prepareConfig() {
		this.inConfig.scenario().setUseTransit(true);
		this.inConfig.scenario().setUseVehicles(true);
		this.inConfig.network().setInputFile(InNetworkFile);
		this.outConfig.scenario().setUseTransit(true);
		this.outConfig.scenario().setUseVehicles(true);
		this.outConfig.network().setInputFile(InNetworkFile);
	}

	private void loadData() {
		ScenarioLoaderImpl inLoader = new ScenarioLoaderImpl(inScenario);
		inLoader.loadScenario();
		try {
			new TransitScheduleReaderV1(inScenario.getTransitSchedule(), inScenario.getNetwork(), inScenario).readFile(InTransitScheduleFile);
		} catch (SAXException e) {
			throw new RuntimeException("could not read transit schedule.", e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("could not read transit schedule.", e);
		} catch (IOException e) {
			throw new RuntimeException("could not read transit schedule.", e);
		}
		ScenarioLoaderImpl outLoader = new ScenarioLoaderImpl(outScenario);
		outLoader.loadScenario();
		readVisumNetwork();
	}

	private void readVisumNetwork()  {
		vNetwork = new VisumNetwork();
		log.info("reading visum network.");
		try {
			new VisumNetworkReader(vNetwork).read(InVisumNetFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void copyRoutes() {
		TransitSchedule inTransitSchedule = this.inScenario.getTransitSchedule();
		TransitSchedule outTransitSchedule = this.outScenario.getTransitSchedule();
		for (TransitLine transitLine : inTransitSchedule.getTransitLines().values()) {
			outTransitSchedule.addTransitLine(transitLine);
		}
	}

	private void enterFacilities() {
		TransitSchedule inTransitSchedule = this.inScenario.getTransitSchedule();
		TransitSchedule outTransitSchedule = this.outScenario.getTransitSchedule();
		Map<Id, TransitStopFacility> facilityTemplates = new HashMap<Id, TransitStopFacility>(inTransitSchedule.getFacilities());
		Iterator<TransitLine> transitLineI = outTransitSchedule.getTransitLines().values().iterator();
		int linesSkipped = 0;
		while (transitLineI.hasNext()) {
			TransitLine transitLine = transitLineI.next();
			try {
				enterFacilitiesForLine(facilityTemplates, transitLine);
			} catch (InvalidLineException e) {
				transitLineI.remove();
				++linesSkipped;
			}
		}
		log.info("Skipped " + linesSkipped + " lines.");
	}

	private void enterFacilitiesForLine(
			Map<Id, TransitStopFacility> facilityTemplates,
			TransitLine transitLine) throws InvalidLineException {
		for (TransitRoute transitRouteI: transitLine.getRoutes().values()) {
			NetworkRoute linkNetworkRoute = transitRouteI.getRoute();
			Collection<Link> links = getAllLink(linkNetworkRoute, this.inScenario.getNetwork());
			Iterator<Link> linkIterator = links.iterator();
			for (TransitRouteStop stop : transitRouteI.getStops()) {
				Link link = linkIterator.next();
				Id stopPointNo = stop.getStopFacility().getId();
				StopPoint stopPoint = findStopPoint(stopPointNo);
				if (stopPoint == null) {
					log.error("Stops: ");
					dumpStops(transitRouteI.getStops());
					log.error("Route: ");
					dumpRoute(linkNetworkRoute, this.inScenario.getNetwork());
					log.error("Stop point " + stopPointNo + " doesn't appear to be in the visum network.");
				}
				Id stopPointNodeNo = stopPoint.nodeId;
				while (!stopPointNodeNo.equals(link.getToNode().getId())) {
					if (linkIterator.hasNext()) {
						link = linkIterator.next();
					} else {
						log.error("On route " + transitRouteI.getId());
						log.error("Stops: ");
						dumpStops(transitRouteI.getStops());
						log.error("Route: ");
						dumpRoute(linkNetworkRoute, this.inScenario.getNetwork());
						throw new InvalidLineException();
					}
				}
				enterNewFacilityIfNecessary(stop, stopPointNo, link, facilityTemplates);
			}
		}
	}

	private StopPoint findStopPoint(Id stopPointNo) {
		for (StopPoint stopPoint : vNetwork.stopPoints.values()) {
			if (stopPointNo.toString().equals(stopPoint.id.toString())) {
				return stopPoint;
			}
		}
		return null;
	}

	private void dumpRoute(NetworkRoute linkNetworkRoute, Network network) {
		Collection<Link> links = getAllLink(linkNetworkRoute, network);
		for (Link link : links) {
			log.error(link.getFromNode().getId() + " ---> " + link.getToNode().getId());
		}
	}

	private void dumpStops(List<TransitRouteStop> stops) {
		for (TransitRouteStop stop : stops) {
			log.error("Stop: " + stop.getStopFacility().getId());
		}
	}

	private List<Link> getAllLink(NetworkRoute linkNetworkRoute, Network network) {
		ArrayList<Link> links = new ArrayList<Link>();
		links.add(network.getLinks().get(linkNetworkRoute.getStartLinkId()));
		for (Id linkId : linkNetworkRoute.getLinkIds()) {
			links.add(network.getLinks().get(linkId));
		}
		links.add(network.getLinks().get(linkNetworkRoute.getEndLinkId()));
		return links;
	}

	private void enterNewFacilityIfNecessary(TransitRouteStop stop,
			Id stopPointNo, Link link, Map<Id, TransitStopFacility> facilityTemplates) {
		if (stopPointNo != null) {
			TransitStopFacility facility = checkFacility(stopPointNo, link, facilityTemplates);
			enterFacility(stopPointNo, facility, stop);
		}
	}

	private void enterFacility(Id stopPointNo, TransitStopFacility facility, TransitRouteStop stop) {
		if (stopPointNo.equals(stop.getStopFacility().getId())) {
			stop.setStopFacility(facility);
		} else {
			throw new RuntimeException();
		}
	}

	private TransitStopFacility checkFacility(Id stopPointNo, Link link, Map<Id, TransitStopFacility> facilityTemplates) {
		TransitSchedule outSchedule = this.outScenario.getTransitSchedule();
		TransitStopFacility toFacility = facilityTemplates.get(
				stopPointNo);
		Map<Link, TransitStopFacility> inLinks = getTransitStopInLinks(toFacility);
		TransitStopFacility facility = inLinks.get(link);
		if (facility == null) {
			IdImpl newId = new IdImpl(toFacility.getId().toString() + "."
					+ Integer.toString(inLinks.size() + 1));
			TransitStopFacility newFacility = outSchedule.getFactory()
					.createTransitStopFacility(newId, toFacility.getCoord(),
							toFacility.getIsBlockingLane());
			newFacility.setStopPostAreaId(toFacility.getId());
			newFacility.setLinkId(link.getId());
			outSchedule.addStopFacility(newFacility);
			inLinks.put(link, newFacility);
			return newFacility;
		} else {
			return facility;
		}
	}

	private Map<Link, TransitStopFacility> getTransitStopInLinks(TransitStopFacility toFacility) {
		Map<Link, TransitStopFacility> inLinks = this.transitStopInLinks.get(toFacility);
		if (inLinks == null) {
			inLinks = new HashMap<Link, TransitStopFacility>();
			this.transitStopInLinks.put(toFacility, inLinks);
		}
		return inLinks;
	}

	private void writeNetworkAndScheduleAndVehicles() throws IOException,
			FileNotFoundException {
		NetworkImpl network = outScenario.getNetwork();
		log.info("writing network to file.");
		new NetworkWriter(network).write(OutNetworkFile);
		log.info("writing TransitSchedule to file.");
		new TransitScheduleWriterV1(outScenario.getTransitSchedule()).write(OutTransitScheduleFile);
		log.info("writing vehicles to file.");
		new VehicleWriterV1(outScenario.getVehicles()).writeFile(OutVehicleFile);
		try {
			new TransitScheduleWriter(outScenario.getTransitSchedule()).writeFile(OutTransitScheduleFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void emptyVehicles() {
		outScenario.getVehicles().getVehicles().clear();
	}

	private Map<Id, VehicleType> readVehicles(){
		Map<String, VehicleType> vehicleTypeMap = DefaultVehTypes.getDefaultVehicleTypes();
		Map<Id, VehicleType> lineId2VehTypeMap = new HashMap<Id, VehicleType>();

		for (Entry<Id, org.matsim.visum.VisumNetwork.TransitLine> entry : this.vNetwork.lines.entrySet()) {
			lineId2VehTypeMap.put(entry.getKey(), vehicleTypeMap.get(entry.getValue().tCode));
		}

		return lineId2VehTypeMap;
	}

	private void buildUmlaeufe() {
		Collection<TransitLine> transitLines = outScenario.getTransitSchedule().getTransitLines().values();
		GreedyUmlaufBuilderImpl greedyUmlaufBuilder = new GreedyUmlaufBuilderImpl(new UmlaufInterpolator(outScenario.getNetwork(), outScenario.getConfig().planCalcScore()), transitLines);
		umlaeufe = greedyUmlaufBuilder.build();

		VehiclesFactory vb = outScenario.getVehicles().getFactory();
		VehicleType vehicleType = vb.createVehicleType(new IdImpl(
				"defaultTransitVehicleType"));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(101));
		capacity.setStandingRoom(Integer.valueOf(0));
		vehicleType.setCapacity(capacity);

		Map<Id, VehicleType> lineId2VehTypeMap = this.readVehicles();

		long vehId = 0;
		for (Umlauf umlauf : umlaeufe) {
			if(lineId2VehTypeMap.containsKey(umlauf.getLineId())){
				vehicleType = lineId2VehTypeMap.get(umlauf.getLineId());
			}
			Vehicle veh = vb.createVehicle(new IdImpl("veh_"+ Long.toString(vehId++)), vehicleType);
			outScenario.getVehicles().getVehicleTypes().put(vehicleType.getId(), vehicleType);
			outScenario.getVehicles().getVehicles().put(veh.getId(), veh);
			umlauf.setVehicleId(veh.getId());
		}
	}

	private void removeUnusedNetworkParts() {
		Collection<Id> usedNodes = new HashSet<Id>();
		for (Umlauf umlauf : umlaeufe) {
			for (UmlaufStueckI umlaufstueck : umlauf.getUmlaufStuecke()) {
				NetworkRoute route = umlaufstueck.getCarRoute();
				for (Link link : getAllLink(route, this.inScenario.getNetwork())) {
					usedNodes.add(link.getFromNode().getId());
					usedNodes.add(link.getToNode().getId());
				}
			}
		}
		Collection<Id> allNodes = new ArrayList<Id>(outScenario.getNetwork().getNodes().keySet());
		for (Id nodeId : allNodes) {
			if (!usedNodes.contains(nodeId)) {
				outScenario.getNetwork().removeNode(nodeId);
			}
		}
		Collection<TransitStopFacility> allFacilities = new ArrayList<TransitStopFacility>(outScenario.getTransitSchedule().getFacilities().values());
		for (TransitStopFacility transitStopFacility : allFacilities) {
			Link link = outScenario.getNetwork().getLinks().get(transitStopFacility.getLinkId());
			if (link == null) {
				outScenario.getTransitSchedule().getFacilities().remove(transitStopFacility.getId());
				log.warn("Removed facility "+transitStopFacility.getId());
			}
		}
	}

}
