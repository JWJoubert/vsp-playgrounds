/* *********************************************************************** *
 * project: org.matsim.*
 * CRSUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.spatial;

import java.util.HashMap;
import java.util.Map;

import org.geotools.referencing.CRS;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author illenberger
 *
 */
public class CRSUtils {

	private static final Map<Integer, CoordinateReferenceSystem> crsMappings = new HashMap<Integer, CoordinateReferenceSystem>();
	
	public static CoordinateReferenceSystem getCRS(int srid) {
		CoordinateReferenceSystem crs = crsMappings.get(srid);
		if(crs == null) {
			CRSAuthorityFactory factory = CRS.getAuthorityFactory(false); //TODO: check this!
			try {
				crs = factory.createCoordinateReferenceSystem("EPSG:" + srid);
			} catch (FactoryException e) {
				e.printStackTrace();
			}
		}
		
		return crs;
	}
	
	public static int getSRID(CoordinateReferenceSystem crs) {
		/*
		 * Randomly get one identifier.
		 */
		Identifier identifier = (Identifier)(crs.getIdentifiers().iterator().next()); 
		if(identifier == null) {
			return 0;
		} else {
			return Integer.parseInt(identifier.getCode());
		}
	}
}
