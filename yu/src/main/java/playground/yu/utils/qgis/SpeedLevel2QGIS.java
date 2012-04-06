/* *********************************************************************** *
 * project: org.matsim.*
 * Speed2QGIS.java
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
package playground.yu.utils.qgis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultFeatureTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.handler.EventHandler;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.yu.analysis.CalcLinksAvgSpeed;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * show the quotient (speed/freespeed) in QGIS map
 * 
 * @author yu
 * 
 */
public class SpeedLevel2QGIS extends MATSimNet2QGIS {
	public static class SpeedLevel2PolygonGraph extends Network2PolygonGraph {
		private final Set<Id> linkIds;

		public SpeedLevel2PolygonGraph(Network network,
				CoordinateReferenceSystem crs, Set<Id> linkIds) {
			super(network, crs);
			this.linkIds = linkIds;
			geofac = new GeometryFactory();
			features = new ArrayList<Feature>();
			AttributeType geom = AttributeTypeFactory.newAttributeType(
					"MultiPolygon", MultiPolygon.class, true, null, null, crs);
			AttributeType id = AttributeTypeFactory.newAttributeType("ID",
					String.class);
			defaultFeatureTypeFactory = new DefaultFeatureTypeFactory();
			defaultFeatureTypeFactory.setName("link");
			defaultFeatureTypeFactory
					.addTypes(new AttributeType[] { geom, id });
		}

		@Override
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

	}

	public static List<Map<Id, Double>> createSpeedLevels(
			Collection<Id> linkIds, CalcLinksAvgSpeed clas, Network network) {
		List<Map<Id, Double>> speeds = new ArrayList<Map<Id, Double>>(24);
		for (int i = 0; i < 24; i++) {
			speeds.add(i, null);
		}

		for (Id linkId : linkIds) {
			for (int i = 0; i < 24; i++) {
				Map<Id, Double> m = speeds.get(i);
				if (m == null) {
					m = new HashMap<Id, Double>();
					speeds.add(i, m);
				}
				double speed = clas.getAvgSpeed(linkId, i * 3600)/* km/h */
						/ 3.6 / network.getLinks().get(linkId).getFreespeed();
				m.put(linkId, speed);
			}
		}
		return speeds;
	}

	/**
	 * @param args
	 *            [0] - network filename
	 * @param args
	 *            [1] - coordinate reference system
	 * @param args
	 *            [2] - flow capacity factor
	 * @param args
	 *            [3] - start time
	 * @param args
	 *            [4] - end time
	 * @param args
	 *            [5] - events filename
	 */
	public static void main(String[] args) {
		String networkFilename, coordinateReferenceSystem, eventsFilename;
		double flowCapacityFactor;
		int startTime, endTime;

		if (args.length <= 3) {
			networkFilename = "D:/Daten/work/shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml.gz";
			coordinateReferenceSystem = gk4;
			flowCapacityFactor = 0.02;
			startTime = 7;
			endTime = 20;
			eventsFilename = "D:/workspace2/playgrounds/yu/test/input/bln2pct/SC.2000.events.xml.gz";
		} else {
			networkFilename = args[0];
			coordinateReferenceSystem = args[1];
			flowCapacityFactor = Double.parseDouble(args[2]);
			startTime = Integer.parseInt(args[3]);
			endTime = Integer.parseInt(args[4]);
			eventsFilename = args[5];
		}

		MATSimNet2QGIS mn2q = new MATSimNet2QGIS(networkFilename,
				coordinateReferenceSystem);
		MATSimNet2QGIS.setFlowCapFactor(flowCapacityFactor);
		/*
		 * //////////////////////////////////////////////////////////////////////
		 * /Traffic speed level and MATSim-network to Shp-file
		 * /////////////////////////////////////////////////////////////////////
		 */

		Network net = mn2q.getNetwork();

		CalcLinksAvgSpeed clas = new CalcLinksAvgSpeed(net, 3600);
		VolumesAnalyzer va = new VolumesAnalyzer(3600, 24 * 3600 - 1, net);

		mn2q.readEvents(eventsFilename
		// "../../runs-svn/run1535/ITERS/it.1900/1535.1900.events.xml.gz"
				, new EventHandler[] { clas, va });

		/*
		 * RoadPricingScheme rps = new RoadPricingScheme();
		 * RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(rps);
		 * tollReader .parse(
		 * "../schweiz-ivtch-SVN/baseCase/roadpricing/KantonZurich/KantonZurich.xml"
		 * );
		 * 
		 * Collection<Id> links = rps.getLinkIdSet();
		 */

		Set<Id> links = net.getLinks().keySet();
		List<Map<Id, Double>> sls = createSpeedLevels(links, clas, net);

		// Set<Id> linkIds = rps.getLinkIdSet();

		for (int i = startTime - 1; i < endTime; i++) {
			// mn2q.addParameter("sl" + i + "-" + (i + 1) + "h", Double.class,
			// sls.get(i));

			SpeedLevel2QGIS sl2q = new SpeedLevel2QGIS(networkFilename,
					coordinateReferenceSystem);
			//
			sl2q.setLinkIds(links);
			sl2q.addParameter("sl", Double.class, sls.get(i));
			sl2q.writeShapeFile(eventsFilename.split("xml.gz")[0] + (i + 1)
					+ ".speedLevel.shp");
		}
		// mn2q.writeShapeFile("test/output/bln2pct/travPt-12.1500.speedLevel.shp");

		System.out.println("----->done!");
	}

	public SpeedLevel2QGIS(String netFilename, String coordRefSys) {
		super(netFilename, coordRefSys);
	}

	public void setLinkIds(Set<Id> linkIds) {
		setN2g(new SpeedLevel2PolygonGraph(getNetwork(), crs, linkIds));
	}

}
