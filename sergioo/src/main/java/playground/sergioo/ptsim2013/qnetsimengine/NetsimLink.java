/* *********************************************************************** *
 * project: org.matsim.*
 * QueueLink
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
package playground.sergioo.ptsim2013.qnetsimengine;

import java.util.Collection;

import org.matsim.api.core.v01.Customizable;
import org.matsim.core.api.internal.MatsimNetworkObject;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vis.snapshotwriters.VisLink;

public interface NetsimLink extends Customizable, VisLink, MatsimNetworkObject {
	
	void recalcTimeVariantAttributes(double time);
	// yyyy my intuition says that this should be moved to the InternalInterface.  kai, dec'11


	@Override
	Collection<MobsimVehicle> getAllVehicles();
	// not terribly efficient, but a possible method also for general mobsims

	Collection<MobsimVehicle> getAllNonParkedVehicles();
	// not terribly efficient, but a possible method also for general mobsims

}