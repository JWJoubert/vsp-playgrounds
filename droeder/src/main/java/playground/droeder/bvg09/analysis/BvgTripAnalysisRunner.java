/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.bvg09.analysis;

import java.io.IOException;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.droeder.DaPaths;
import playground.droeder.Analysis.Trips.TripAnalysis;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public class BvgTripAnalysisRunner {
	public static void main(String[] args){
		final String OUTDIR = DaPaths.VSP + "BVG09_Auswertung/"; 
		final String INDIR = OUTDIR + "input/";
		
		final String NETWORKFILE = INDIR + "network.final.xml.gz";
		final String SHAPEFILE = OUTDIR + "BerlinSHP/Berlin.shp"; 
		
//		final String EVENTSFILE = IN + "bvg.run128.25pct.100.events.xml.gz";
//		final String PLANSFILE = IN + "bvg.run128.25pct.100.plans.selected.xml.gz";
		
		final String EVENTSFILE = OUTDIR + "testEvents.xml";
		final String PLANSFILE = OUTDIR + "testPopulation1.xml.gz";
		
		Set<Feature> features = null;
		try {
			features = new ShapeFileReader().readFileAndInitialize(SHAPEFILE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Geometry g =  (Geometry) features.iterator().next().getAttribute(0);
		
		TripAnalysis ana = new TripAnalysis(g);
		ana.run(PLANSFILE, NETWORKFILE, EVENTSFILE, OUTDIR, false);
	}
}
