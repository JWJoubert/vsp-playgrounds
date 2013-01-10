/* *********************************************************************** *
 * project: org.matsim.*
 * Ped.java
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
package playground.gregor.multidestpeds.io;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 * 
 */
public class Ped {

	public String color;
	public Id id;
	public double depart;
	public double arrived;
	public Map<Double, Coordinate> coords = new LinkedHashMap<Double, Coordinate>();
	public Map<Double, Coordinate> velocities = new LinkedHashMap<Double, Coordinate>();

	Coordinate lastPos;
	Path path;
	int currLink = 0;

}
