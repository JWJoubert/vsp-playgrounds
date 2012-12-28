/* *********************************************************************** *
 * project: org.matsim.*
 * DgKoehlerStrehlerScenario2Commodities
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.utils.zones;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author dgrether
 * 
 */
public class DgMatsimPopulation2Zones {

	private static final Logger log = Logger.getLogger(DgMatsimPopulation2Zones.class);

	private List<DgZone> cells = null;
	
	private GeometryFactory geoFac = new GeometryFactory();

	private List<SimpleFeature> featureCollection;

	private PolylineFeatureFactory featureFactory;

	//FIXME remove shape file writer
	public List<DgZone> convert2Zones(Network network, Population pop, List<DgZone> cells, Envelope networkBoundingBox, double startTime, double endTime) {
		this.cells = cells;
		this.initShapeFileWriter();
		this.convertPopulation2OD(network, pop, networkBoundingBox, startTime, endTime);
		this.writeShape();
		return cells;
	}
	
	private void writeShape() {
		ShapeFileWriter.writeGeometries(featureCollection, DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_small/od_pairs.shp");
	}

	private void initShapeFileWriter() {
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
		this.featureCollection = new ArrayList<SimpleFeature>();
		
		this.featureFactory = new PolylineFeatureFactory.Builder().
				setCrs(crs).
				setName("od_pair").
				create();
	}

	
	private void convertPopulation2OD(Network net, Population pop, Envelope networkBoundingBox, double startTime, double endTime) {
		for (Person person : pop.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			Activity startAct = null;
			Activity targetAct = null;
			Leg leg = null;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					if (startAct == null) {
						startAct = (Activity) pe;
					}
					else if (targetAct == null) {
						targetAct = (Activity) pe;
						if (startTime <= startAct.getEndTime() && startAct.getEndTime() <= endTime) {
							processLeg(net, startAct, leg, targetAct, networkBoundingBox);
						}
						startAct = targetAct;
						targetAct = null;
					}
				}
				else if (pe instanceof Leg) {
					leg = (Leg) pe;
				}
			}
		}
	}

	private void addFromToRelationshipToShape(Coordinate startCoordinate, Coordinate endCoordinate){
		Coordinate[] coordinates = {startCoordinate, endCoordinate};
		SimpleFeature feature = this.featureFactory.createPolyline(coordinates);
		this.featureCollection.add(feature);
	}

	
	private void addFromZoneToZoneRelationshipToGrid(Coordinate startCoordinate, Coordinate endCoordinate){
		this.addFromToRelationshipToShape(startCoordinate, endCoordinate);
		DgZone startCell = this.searchGridCell(startCoordinate);
		DgZone endCell = this.searchGridCell(endCoordinate);
//		log.debug("  created od pair from cell " + startCell.getId() + " to " + endCell.getId());
		startCell.addToZoneRelation(endCell);
	}

	private void addFromLinkToLinkRelationshipToGrid(Link startLink, Link endLink){
		Coordinate startCoordinate = MGC.coord2Coordinate(startLink.getCoord());
		DgZone startCell = this.searchGridCell(startCoordinate);
		Coordinate endCoordinate = MGC.coord2Coordinate(endLink.getCoord());
		DgZone endCell = this.searchGridCell(endCoordinate);
		startCell.getFromLink(startLink).addToLinkRelation(endLink);
		this.addFromToRelationshipToShape(startCoordinate, endCoordinate);
//		log.debug("  created od pair from cell " + startCell.getId() + " to " + endCell.getId());
	}

	private void addFromZoneToLinkRelationshipToGrid(Coordinate startCoordinate, Link endLink){
		DgZone startCell = this.searchGridCell(startCoordinate);
		Coordinate endCoordinate = MGC.coord2Coordinate(endLink.getCoord());
		DgZone endCell = this.searchGridCell(endCoordinate);
		startCell.addToLinkRelation(endLink);
		this.addFromToRelationshipToShape(startCoordinate, endCoordinate);
//		log.debug("  created od pair from cell " + startCell.getId() + " to " + endCell.getId());
	}

	private void addFromLinkToZoneRelationshipToGrid(Link startLink, Coordinate endCoordinate){
		Coordinate startCoordinate = MGC.coord2Coordinate(startLink.getCoord());
		DgZone startCell = this.searchGridCell(startCoordinate);
		DgZone endCell = this.searchGridCell(endCoordinate);
		startCell.getFromLink(startLink).addToZoneRelation(endCell);
		this.addFromToRelationshipToShape(startCoordinate, endCoordinate);
//		log.info("  created od pair from cell " + startCell.getId() + " to " + endCell.getId());

	}

	
	private DgZone searchGridCell(Coordinate coordinate){
		Point p = this.geoFac.createPoint(coordinate);
		for (DgZone cell : this.cells){
			if (cell.getPolygon().covers(p)){
//				log.debug("  found cell " + cell.getId() + " for Coordinate: " + coordinate);
				return cell;
			}
		}
		log.warn("No cell found for Coordinate: " + coordinate);
		return null;
	}

	private void processLeg(Network network, Activity startAct, Leg leg, Activity targetAct,
			Envelope networkBoundingBox) {
		Coordinate startCoordinate = MGC.coord2Coordinate(startAct.getCoord());
		Coordinate endCoordinate = MGC.coord2Coordinate(targetAct.getCoord());
//		log.debug("Processing leg from: " + startCoordinate + " to " + endCoordinate);
		boolean netContainsStartCoordinate  = networkBoundingBox.contains(startCoordinate);
		boolean netContainsEndCoordinate = networkBoundingBox.contains(endCoordinate);
		if (netContainsStartCoordinate
				&& netContainsEndCoordinate) {
//			log.debug("  coordinates in grid...");
			this.addFromZoneToZoneRelationshipToGrid(startCoordinate, endCoordinate);
		}
		else if (netContainsStartCoordinate && ! netContainsEndCoordinate){
			NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
			List<Link> route = this.createFullRoute(network, networkRoute);
			Link lastLink = null;
			Coordinate coordinate = null;
			for (Link link : route){
				coordinate = MGC.coord2Coordinate(link.getCoord());
				if (! networkBoundingBox.contains(coordinate)) {
					break;
				}
				lastLink = link;
			}
			if (lastLink != null) {
				this.addFromZoneToLinkRelationshipToGrid(startCoordinate, lastLink);
			}
		}
		else if (! netContainsStartCoordinate &&  netContainsEndCoordinate){
			NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
			List<Link> route = this.createFullRoute(network, networkRoute);
			Link firstLink = null;
			Coordinate coordinate = null;
			for (Link link : route){
				coordinate = MGC.coord2Coordinate(link.getCoord());
				if (networkBoundingBox.contains(coordinate)) {
					firstLink = link;
					break;
				}
			}
			if (firstLink != null){
				this.addFromLinkToZoneRelationshipToGrid(firstLink, endCoordinate);
			}
		}
		else {
			NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
			List<Link> route = this.createFullRoute(network, networkRoute);
//			List<Coordinate> coordinateSequence = this.createCoordinateSequenceFromRoute(network, networkRoute);
			boolean isRouteInGrid = false;
			while (! route.isEmpty()){
				Tuple<Link, Link> nextFromTo = this.getNextFromToOfRoute(network, route, networkBoundingBox);
				if (nextFromTo != null){
					this.addFromLinkToLinkRelationshipToGrid(nextFromTo.getFirst(), nextFromTo.getSecond());
					isRouteInGrid = true;
				}
			}
//			if (! isRouteInGrid){
//				log.debug("  Route is not in area of interest");
//			}
		}
	}

	private List<Link> createFullRoute(Network network, NetworkRoute route) {
		List<Id> linkIds = new ArrayList<Id>();
		linkIds.add(route.getStartLinkId());
		linkIds.addAll(route.getLinkIds());
		linkIds.add(route.getEndLinkId());
		List<Link> links = new ArrayList<Link>();
		for (Id linkId : linkIds){
			Link currentLink = network.getLinks().get(linkId);
			links.add(currentLink);
		}
		return links;
	}

	
	private Tuple<Link, Link> getNextFromToOfRoute(Network network, List<Link> route,
			Envelope networkBoundingBox) {
		Link routeStartLink = null;
		Link routeEndLink = null;
		Link currentLink = null;
		Coordinate currentCoordinate = null;
		//search next start coordinate within grid on route
		while (! route.isEmpty()){
			currentLink = route.remove(0);
			currentCoordinate = MGC.coord2Coordinate(currentLink.getCoord());
			if (networkBoundingBox.contains(currentCoordinate)){
				routeStartLink = currentLink;
				break;
			}
		}
		//search last link that is contained in grid
		while (! route.isEmpty()){
			currentLink = route.remove(0);
			currentCoordinate = MGC.coord2Coordinate(currentLink.getCoord());
			if (networkBoundingBox.contains(currentCoordinate)){
				routeEndLink = currentLink;
			}
			else {
				break;
			}
		}
		if (routeStartLink != null && routeEndLink != null){
			return new Tuple<Link, Link>(routeStartLink, routeEndLink);
		}
		return null;
	}
	
}
