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
package playground.droeder.eMobility;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author droeder
 *
 */
public class ChargingProfiles {
	
	private HashMap<Id, AbstractEnergyProfile> profiles;

	public ChargingProfiles(){
		this.profiles = new HashMap<Id , AbstractEnergyProfile>();
//		this.profiles.put(new IdImpl("default"), value)
	}
	
	public void addProfile(AbstractEnergyProfile profile){
		this.profiles.put(profile.getId(), profile);
	}
	
	public double getCharge(Id profile, double  durationOrSpeed, double chargeStateOrGradient){
		return this.profiles.get(profile).getCharge( durationOrSpeed, chargeStateOrGradient);
	}
	

}
