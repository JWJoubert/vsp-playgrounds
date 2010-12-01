/* *********************************************************************** *
 * project: org.matsim.*
 * LinkCostOffsets2QGIS.java
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

/**
 * 
 */
package playground.yu.integration.cadyts.demandCalibration.withCarCounts.utils.qgis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.DefaultFeatureTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.yu.utils.qgis.MATSimNet2QGIS;
import playground.yu.utils.qgis.Network2PolygonGraph;
import playground.yu.utils.qgis.X2GraphImpl;
import cadyts.utilities.misc.DynamicData;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * @author yu
 * 
 */
public class LinkCostOffsets2QGISWithArrowhead extends MATSimNet2QGIS {
	public static class LinkCostOffsets2PolygonGraphWithArrowhead extends
			X2GraphImpl {
		private final Set<Id> linkIds;

		public LinkCostOffsets2PolygonGraphWithArrowhead(final Network network,
				final CoordinateReferenceSystem crs, final Set<Id> linkIds) {
			this.network = network;
			this.crs = crs;
			this.linkIds = linkIds;
			geofac = new GeometryFactory();
			features = new ArrayList<Feature>();
			AttributeType geom = DefaultAttributeTypeFactory.newAttributeType(
					"MultiPolygon", MultiPolygon.class, true, null, null, crs);
			AttributeType id = AttributeTypeFactory.newAttributeType("ID",
					String.class);
			defaultFeatureTypeFactory = new DefaultFeatureTypeFactory();
			defaultFeatureTypeFactory.setName("link");
			defaultFeatureTypeFactory
					.addTypes(new AttributeType[] { geom, id });
		}

		public Collection<Feature> getFeatures() throws SchemaException,
				NumberFormatException, IllegalAttributeException {
			for (int i = 0; i < attrTypes.size(); i++) {
				defaultFeatureTypeFactory.addType(attrTypes.get(i));
			}
			FeatureType ftRoad = defaultFeatureTypeFactory.getFeatureType();
			for (Id linkId : linkIds) {
				Link link = network.getLinks().get(linkId);
				LinearRing lr = getLinearRing(link);
				Polygon p = new Polygon(lr, null, geofac);
				MultiPolygon mp = new MultiPolygon(new Polygon[] { p }, geofac);
				int size = 2 + parameters.size();
				Object[] o = new Object[size];
				o[0] = mp;
				o[1] = link.getId().toString();
				for (int i = 0; i < parameters.size(); i++) {
					o[i + 2] = parameters.get(i).get(link.getId());
				}
				Feature ft = ftRoad.create(o, "network");
				features.add(ft);
			}
			return features;
		}

		protected LinearRing getLinearRing(Link link) {
			// //////////////////////////////////////////////////////////////
			double width = getLinkWidth(link);
			// //////////////////////////////////////////////////////////////
			Coordinate from = getCoordinate(link.getFromNode().getCoord());
			Coordinate to = getCoordinate(link.getToNode().getCoord());

			// ////////////////////////OLD
			// CODES////////////////////////////////////
			// double xdiff = to.x - from.x;
			// double ydiff = to.y - from.y;
			// double denominator = Math.sqrt(xdiff * xdiff + ydiff * ydiff);
			// double xwidth = width * ydiff / denominator;
			// double ywidth = -width * xdiff / denominator;
			//
			// Coordinate fromB = new Coordinate(from.x + xwidth, from.y +
			// ywidth,
			// 0);
			// // Coordinate toB = new Coordinate(to.x + xwidth, to.y + ywidth,
			// 0);
			// Coordinate toC = new Coordinate(0.2 * fromB.x + 0.8
			// * (to.x + xwidth), 0.2 * fromB.y + 0.8 * (to.y + ywidth), 0);
			// Coordinate toD = new Coordinate(toC.x + xwidth, toC.y + ywidth,
			// 0);
			// ////////////////////////OLD
			// CODES////////////////////////////////////
			return new LinearRing(getCoordinateArraySequence(width, from, to)
			// ////////////////////////OLD
					// CODES////////////////////////////////////
					// new CoordinateArraySequence(new Coordinate[] {
					// from, to, toD, toC, fromB, from })
					// ////////////////////////OLD
					// CODES////////////////////////////////////
					, geofac);
		}

		private CoordinateArraySequence getCoordinateArraySequence(
				double width, Coordinate from, Coordinate to) {
			return new Arrow().getCoordinateArraySequence(width, from, to);
		}

		@Override
		protected double getLinkWidth(final Link link) {
			Double i = (Double) parameters.get(0).get(link.getId()) * 250d;
			return Math.max(i, 50d);
		}
	}

	private int countsStartTime = 1, countsEndTime = 24;
	private List<Map<Id, Double>> linkCostOffsetsAbsoluts = null;
	private List<Map<Id, Integer>> linkCostOffsetsSigns = null;

	public LinkCostOffsets2QGISWithArrowhead(final int countsStartTime,
			final int countsEndTime, String netFilename, String crs) {
		super(netFilename, crs);
		this.countsStartTime = countsStartTime;
		this.countsEndTime = countsEndTime;
	}

	protected LinkCostOffsets2QGISWithArrowhead() {
	}

	public LinkCostOffsets2QGISWithArrowhead(Network network,
			String coordinateSystem, int countsStartTime, int countsEndTime) {
		((ScenarioImpl) scenario).setNetwork((NetworkImpl) network);
		crs = MGC.getCRS(coordinateSystem);
		n2g = new Network2PolygonGraph(scenario.getNetwork(), crs);
		this.countsStartTime = countsStartTime;
		this.countsEndTime = countsEndTime;
	}

	public void createLinkCostOffsets(final Collection<Link> links,
			final DynamicData<Link> linkCostOffsets) {
		linkCostOffsetsAbsoluts = new ArrayList<Map<Id, Double>>(countsEndTime
				- countsStartTime + 1);
		linkCostOffsetsSigns = new ArrayList<Map<Id, Integer>>(countsEndTime
				- countsStartTime + 1);
		for (int i = 0; i <= countsEndTime - countsStartTime; i++) {
			linkCostOffsetsAbsoluts.add(i, null);
			linkCostOffsetsSigns.add(i, null);
		}
		for (Link link : links) {
			Id linkId = link.getId();
			for (int i = 0; i <= countsEndTime - countsStartTime; i++) {
				double costOffset = linkCostOffsets.getSum(link, (i
						+ countsStartTime - 1) * 3600,
						(i + countsStartTime) * 3600 - 1);
				Map<Id, Double> ma = linkCostOffsetsAbsoluts.get(i);
				if (ma == null) {
					ma = new HashMap<Id, Double>();
					linkCostOffsetsAbsoluts.add(i, ma);
				}
				ma.put(linkId, Math.abs(costOffset)// / flowCapFactor
						);

				Map<Id, Integer> ms = linkCostOffsetsSigns.get(i);
				if (ms == null) {
					ms = new HashMap<Id, Integer>();
					linkCostOffsetsSigns.add(i, ms);
				}
				ms.put(linkId, (int) Math.signum(costOffset));
			}
		}
	}

	public void setLinkIds(final Set<Id> linkIds) {
		setN2g(new LinkCostOffsets2PolygonGraphWithArrowhead(getNetwork(), crs,
				linkIds));
	}

	public void output(final Set<Id> linkIds, final String outputBase) {
		for (int i = 0; i <= countsEndTime - countsStartTime; i++) {
			setLinkIds(linkIds);
			addParameter("costOffset", Double.class, linkCostOffsetsAbsoluts
					.get(i));
			addParameter("sgn", Integer.class, linkCostOffsetsSigns.get(i));
			writeShapeFile(outputBase + (i + countsStartTime) + ".shp");
		}
	}
}
