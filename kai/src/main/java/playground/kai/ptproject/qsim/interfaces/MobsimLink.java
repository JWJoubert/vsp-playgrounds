/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.kai.ptproject.qsim.interfaces;

import org.matsim.api.core.v01.network.Link;


/**
 * @author nagel
 *
 */
public class MobsimLink implements Updateable {
	Link link = null ; // underlying link; dummy
	Link getLink() { return link ; }
	
	MobsimFacility linkFac = new MobsimFacility() ; // dummy
	MobsimVehicle veh = new MobsimVehicle() ; // dummy
	
	/**
	 * @param veh
	 * @return true if accepted, false if not
	 */
	boolean addVehicleFromIntersection( MobsimVehicle veh ) {
		return true ;
	}
	
	/**
	 * @param veh
	 * @return true if accepted, false if not
	 */
	boolean addVehicleFromFacilityNormal( MobsimVehicle veh ) {
		return true ;
	}
	
	boolean addVehicleFromFacilityForced( MobsimVehicle veh ) {
		return true ;
	}
	
	public void update() {
		// queue2xxx (buffer or parking)
		/* if vehicle arrival */
		{
			linkFac.addOccupiedVehicle(veh) ;
		}
		
		// wait2buffer
	}

}
