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

package playground.johannes.gsv.synPop.mid;

/**
 * @author johannes
 *
 */
public interface MIDKeys {

	public static final String HOUSEHOLD_ID = "hhid";
	
	public static final String PERSON_ID = "pid";
	
	public static final String PERSON_MUNICIPALITY = "polgk";
	
	public static final String LEG_START_TIME_HOUR = "st_std";
	
	public static final String LEG_START_TIME_MIN = "st_min";
	
	public static final String LEG_END_TIME_HOUR = "en_std";
	
	public static final String LEG_END_TIME_MIN = "en_min";
	
	public static final String LEG_MAIN_TYPE = "w04";
	
	public static final String LEG_ORIGIN = "w01";
	
	public static final String LEG_DESTINATION = "w13";
	
	public static final String LEG_DISTANCE = "wegkm_k";
	
	
//	public static final String PERSON_MUNICIPALITY_LOWER = "inhabLow";
	
//	public static final String PERSON_MUNICIPALITY_UPPER = "inhabUp";
	
	public static final String PERSON_MUNICIPALITY_CLASS = "inhabClass";
	
	public static final String PERSON_WEIGHT = "p_gew";
}
