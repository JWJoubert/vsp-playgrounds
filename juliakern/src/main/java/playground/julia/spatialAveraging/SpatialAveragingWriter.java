/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.julia.spatialAveraging;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
//import com.vividsolutions.jts.util.Assert;

public class SpatialAveragingWriter {

	private CoordinateReferenceSystem targetCRS;
	private int noOfXbins;
	private int noOfYbins;
	private Double yMin;
	private Double yMax;
	private Double xMin;
	private Double xMax;
	
	Collection<SimpleFeature> featuresInVisBoundary;

	private static final Logger logger = Logger.getLogger(SpatialAveragingWriter.class);
	
	public SpatialAveragingWriter(double xMin, double xMax, double yMin, double yMax, int noOfXbins, int noOfYbins, double smoothingRadius_m, String munichShapeFile, CoordinateReferenceSystem targetCRS){
		this.xMin = xMin;
		this.xMax = xMax;
		this.yMin = yMin;
		this.yMax = yMax;
		this.noOfXbins = noOfXbins;
		this.noOfYbins = noOfYbins;
		this.featuresInVisBoundary = ShapeFileReader.getAllFeatures(munichShapeFile);
		this.targetCRS = targetCRS;
	}
	
	public void writeRoutput(double[][] results, String outputPathForR) {
		try {
			BufferedWriter buffW = new BufferedWriter(new FileWriter(outputPathForR));
			String valueString = new String();
			valueString = "\t";

			//x-coordinates as first row
			for(int xIndex = 0; xIndex < results.length; xIndex++){
				valueString += findBinCenterX(xIndex) + "\t";
			}
			buffW.write(valueString);
			buffW.newLine();
			valueString = new String();

			for(int yIndex = 0; yIndex < results[0].length; yIndex++){
				//y-coordinates as first column
				valueString += findBinCenterY(yIndex) + "\t";
				//table contents
				for(int xIndex = 0; xIndex < results.length; xIndex++){ 
					Coord cellCentroid = findCellCentroid(xIndex, yIndex);
					if(isInVisBoundary(cellCentroid)){
						valueString += Double.toString(results[xIndex][yIndex]) + "\t"; 
					} else {
						valueString += "NA" + "\t";
					}
				}
				buffW.write(valueString);
				buffW.newLine();
				valueString = new String();
			}
			buffW.close();	
		} catch (IOException e) {
			throw new RuntimeException("Failed writing output for R. Reason: " + e);
		}	
		logger.info("Finished writing output for R to " + outputPathForR);
	}

	void writeGISoutput(Map<Double, double[][]> time2results, String outputPathForGIS) throws IOException {
		
		PointFeatureFactory factory = new PointFeatureFactory.Builder()
		.setCrs(this.targetCRS)
		.setName("EmissionPoint")
		.addAttribute("Time", String.class)
		.addAttribute("Emissions", Double.class)
		.create();
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for(Double endOfTimeInterval : time2results.keySet()){
//			if(endOfTimeInterval < Time.MIDNIGHT){ // time manager in QGIS does not accept time beyond midnight...
				for(int xIndex = 0; xIndex < noOfXbins; xIndex++){
					for(int yIndex = 0; yIndex < noOfYbins; yIndex++){
						Coord cellCentroid = findCellCentroid(xIndex, yIndex);
						if(isInVisBoundary(cellCentroid)){
							String dateTimeString = convertSeconds2dateTimeFormat(endOfTimeInterval);
							double result = time2results.get(endOfTimeInterval)[xIndex][yIndex];
							SimpleFeature feature = factory.createPoint(new Coordinate(cellCentroid.getX(), cellCentroid.getY()), new Object[] {dateTimeString, result}, null);
							features.add(feature);
						}
					}
				}
//			}
		}
		ShapeFileWriter.writeGeometries(features, outputPathForGIS);
		logger.info("Finished writing output for GIS to " + outputPathForGIS);
	}

	private double findBinCenterY(int yIndex) {
		double yBinCenter = yMin + ((yIndex + .5) / noOfYbins) * (yMax - yMin);
		//Assert.equals(mapYCoordToBin(yBinCenter), yIndex);
		return yBinCenter ;
	}

	private double findBinCenterX(int xIndex) {
		double xBinCenter = xMin + ((xIndex + .5) / noOfXbins) * (xMax - xMin);
		//Assert.equals(mapXCoordToBin(xBinCenter), xIndex);
		return xBinCenter ;
	}
	
	private boolean isInVisBoundary(Coord cellCentroid) {
		boolean isInMunichShape = false;
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(cellCentroid.getX(), cellCentroid.getY()));
		for(SimpleFeature feature : this.featuresInVisBoundary){
			if(((Geometry) feature.getDefaultGeometry()).contains(geo)){
				isInMunichShape = true;
				break;
			}
		}
		return isInMunichShape;
	}
	
//	private Integer mapYCoordToBin(double yCoord) {
//		if (yCoord <= yMin || yCoord >= yMax) return null; // yCoord is not in area of interest
//		double relativePositionY = ((yCoord - yMin) / (yMax - yMin) * noOfYbins); // gives the relative position along the y-range
//		return (int) relativePositionY; // returns the number of the bin [0..n-1]
//	}
//
//	private Integer mapXCoordToBin(double xCoord) {
//		if (xCoord <= xMin || xCoord >= xMax) return null; // xCorrd is not in area of interest
//		double relativePositionX = ((xCoord - xMin) / (xMax - xMin) * noOfXbins); // gives the relative position along the x-range
//		return (int) relativePositionX; // returns the number of the bin [0..n-1]
//	}
	
	public Coord findCellCentroid(int xIndex, int yIndex) {
		double xCentroid = findBinCenterX(xIndex);
		double yCentroid = findBinCenterY(yIndex);
		Coord cellCentroid = new CoordImpl(xCentroid, yCentroid);
		return cellCentroid;
	}
	
	private String convertSeconds2dateTimeFormat(double endOfTimeInterval) {
		String date = "2012-04-13 ";
		String time = Time.writeTime(endOfTimeInterval, Time.TIMEFORMAT_HHMM);
		String dateTimeString = date + time;
		return dateTimeString;
	}
}
