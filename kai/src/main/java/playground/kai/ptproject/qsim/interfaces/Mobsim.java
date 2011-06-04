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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.vehicles.Vehicle;

class Teleportation implements Updateable {
	public void update() {}
}

class Mobsim {
	
	Map<Id,MobsimNode> mobsimNodes = null ; // dummy
	Map<Id,MobsimLink> mobsimLinks = null ; // dummy

	void run() {
		Teleportation teleportation = new Teleportation() ;
		PlanAgent person = null ; // dummy
		Vehicle veh = null ; // dummy
		MobsimFacility linkFac = new MobsimFacility() ; // dummy

		// INITIALIZATION:
		
		// add a person to an ActivityFacility:
		linkFac.addPerson( person ) ;
		
		// add an empty vehicle to a parking:
		linkFac.addEmptyVehicle( veh ) ;
		
		// UPDATES
		for ( MobsimLink link : mobsimLinks.values() ) {
			link.update() ;  // link facility update included here ?!
		}
		for ( MobsimNode node : mobsimNodes.values() ) {
			node.update() ;
		}
		/* for all link facilities */ {
			linkFac.update() ;
		}
		teleportation.update() ;
	}

	public static void main( String[] args ) {
		Mobsim usecase = new Mobsim() ; // dummy
		usecase.run() ;
	}

	Map<Id, MobsimNode> getMobsimNodes() {
		return mobsimNodes;
	}

	Map<Id, MobsimLink> getMobsimLinks() {
		return mobsimLinks;
	}

}
