/* *********************************************************************** *
 * project: org.matsim.*
 * MyZone.java
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

package playground.southafrica.utilities.containers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class MyZone extends MultiPolygon implements Identifiable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Id id;

	public MyZone(Polygon[] polygons, GeometryFactory factory, Id id) {
		super(polygons, factory);
		this.id = id;
	}
	
	@Override
	public Id getId() {
		return this.id;
	}
	
}

