/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.PLOC.analysis.postprocessing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Point;


public class LinkVolumesShapeFileWriter {
	private FeatureType featureType;		
	public void writeLinkVolumesAtCountStations(String outpath, List<LinkWInfo> links, int hour) {		
		this.initGeometries();
		ArrayList<Feature> features = new ArrayList<Feature>();	
		
		for (LinkWInfo link : links) {
			Feature feature = this.createFeature(link.getCoord(), link.getId(), link.getStdDevs(hour), link.getAvgVolume(hour));
			features.add(feature);
		}
		
		if (!features.isEmpty()) {
			ShapeFileWriter.writeGeometries((Collection<Feature>)features, outpath  + "/volumes_" + hour + ".shp");
		}
	}
		
	private void initGeometries() {
		AttributeType [] attr = new AttributeType[4];
		attr[0] = AttributeTypeFactory.newAttributeType("Point", Point.class);
		attr[1] = AttributeTypeFactory.newAttributeType("ID", String.class);
		attr[2] = AttributeTypeFactory.newAttributeType("stdDev", Double.class);
		attr[3] = AttributeTypeFactory.newAttributeType("avgVolume", Double.class);
		
		try {
			this.featureType = FeatureTypeBuilder.newFeatureType(attr, "point");
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}
	
	private Feature createFeature(Coord coord, Id id, double stdDev, double avgVolume) {
		Feature feature = null;
		try {
			feature = this.featureType.create(new Object [] {MGC.coord2Point(coord), id.toString(), stdDev, avgVolume});
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}
		return feature;
	}
}
