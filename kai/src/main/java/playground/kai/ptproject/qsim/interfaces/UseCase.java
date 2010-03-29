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

import org.matsim.ptproject.qsim.PersonAgentI;
import org.matsim.vehicles.BasicVehicle;

class Teleportation implements Updateable {
	public void update() {}
}

public class UseCase {

	void run() {
		Teleportation teleportation = null ;
		PersonAgentI person = null ; // dummy
		BasicVehicle veh = null ; // dummy
		LinkFacility linkFac = null ; // dummy
		QLink link = new QLink() ;
		QNode node = new QNode() ;

		// add a person to an ActivityFacility (initialization):
		linkFac.addPerson( person ) ;
		
		// add an empty vehicle to a parking (initialization):
		linkFac.addEmptyVehicle( veh ) ;
		
		// move everything one time step forward:
		/* for all links */ {
			link.update() ;  // link facility update included here ?!
		}
		/* for all nodes */ {
			node.update() ;
		}
		/* for all link facilities */ {
			linkFac.update() ;
		}
		teleportation.update() ;
	}

	public static void main( String[] args ) {
		UseCase usecase = null ; // dummy
		usecase.run() ;
	}

}
