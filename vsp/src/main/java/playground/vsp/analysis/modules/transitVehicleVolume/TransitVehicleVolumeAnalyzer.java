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
package playground.vsp.analysis.modules.transitVehicleVolume;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceConfigurationError;

import org.apache.log4j.Logger;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.jdesktop.swingx.mapviewer.Tile;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * @author droeder
 *
 */
public class TransitVehicleVolumeAnalyzer extends AbstractAnalyisModule {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(TransitVehicleVolumeAnalyzer.class);
	private Scenario sc;
	private TransitVehicleVolumeHandler handler;
	private HashMap<String, Map<Id, Double>> mode2Link2Total;

	public TransitVehicleVolumeAnalyzer(Scenario sc, Double interval) {
		super(TransitVehicleVolumeAnalyzer.class.getSimpleName());
		this.sc = sc;
		this.handler = new TransitVehicleVolumeHandler(sc.getTransitSchedule(), interval);
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new ArrayList<EventHandler>();
		handler.add(this.handler);
		return handler;
	}

	@Override
	public void preProcessData() {
		//do nothing
	}

	@Override
	public void postProcessData() {
		this.createTotals();
	}
	
	private void createTotals() {
		// count totals
		this.mode2Link2Total = new HashMap<String, Map<Id, Double>>();
		Double total;
		for(Entry<String, Counts> e: this.handler.getMode2Counts().entrySet()){
			Map<Id, Double> temp = new HashMap<Id, Double>(); 
			for(Count c: e.getValue().getCounts().values()){
				total = new Double(0.);
				for(Volume v: c.getVolumes().values()){
					total += v.getValue();
				}
				temp.put(c.getLocId(), total);
			}
			this.mode2Link2Total.put(e.getKey(), temp);
		}
	}

	@Override
	public void writeResults(String outputFolder) {
		for(Entry<String, Counts> e: this.handler.getMode2Counts().entrySet()){
			writeModeShape(e.getKey(), e.getValue(), this.mode2Link2Total.get(e.getKey()), outputFolder + e.getKey() + ".shp");
		}
	}
	
	private void writeModeShape(String name, Counts counts, Map<Id, Double> mode2Total, String file){
		AttributeType[] attribs = new AttributeType[3 + this.handler.getMaxTimeSlice() ];
		attribs[0] = AttributeTypeFactory.newAttributeType("LineString", LineString.class, true, null, null, MGC.getCRS(TransformationFactory.WGS84_UTM35S));
		attribs[1] = AttributeTypeFactory.newAttributeType("name", String.class);
		attribs[2] = AttributeTypeFactory.newAttributeType("total", Double.class);
		for(int  i = 0 ; i< this.handler.getMaxTimeSlice(); i++){
			attribs[3+i] = AttributeTypeFactory.newAttributeType(String.valueOf(i), Double.class);
		}
		FeatureType featureType = null ;
		try {
			featureType = FeatureTypeBuilder.newFeatureType(attribs, name);
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		Collection<Feature> features = new ArrayList<Feature>();
		
		Object[] featureAttribs ;
		for(Count c: counts.getCounts().values()){
			featureAttribs = new Object[2 + this.handler.getMaxTimeSlice() + 1];
			//create linestring from link
			Link l = this.sc.getNetwork().getLinks().get(new IdImpl(c.getCsId()));
			if(l == null){
				log.debug("can not find link " + c.getLocId());
				log.debug("links #" + this.sc.getNetwork().getLinks().size());
				continue;
			}
			Coordinate[] coord = new Coordinate[2];
			coord[0] = new Coordinate(l.getFromNode().getCoord().getX(), l.getFromNode().getCoord().getY(), 0.);
			coord[1] = new Coordinate(l.getToNode().getCoord().getX(), l.getToNode().getCoord().getY(), 0.);
			LineString ls = new GeometryFactory().createLineString(new CoordinateArraySequence(coord));
			//###
			featureAttribs[0] = ls;
			featureAttribs[1] = c.getLocId().toString();
			featureAttribs[2] = mode2Total.get(c.getLocId());
			for(int i = 0; i < this.handler.getMaxTimeSlice() ; i++){
				if(c.getVolume(i) == null){
					featureAttribs[3 + i] = 0.;
				}else{
					featureAttribs[3 + i] = c.getVolume(i).getValue();
				}
			}
//			System.out.println(featureAttribs.toString());
//			for(Object o: featureAttribs){
//				System.out.print(o.toString());
//			}
//			System.out.println();
			try {
				features.add(featureType.create(featureAttribs));
			} catch (IllegalAttributeException e1) {
				e1.printStackTrace();
			}
		}
		try{
			ShapeFileWriter.writeGeometries(features, file);
		}catch(ServiceConfigurationError e){
			e.printStackTrace();
		}
	}
}

