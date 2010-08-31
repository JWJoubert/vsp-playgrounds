/* *********************************************************************** *
 * project: org.matsim.*
 * BufferedTravelTime.java
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

package playground.christoph.multimodal.router.costcalculator;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelTime;

public class BufferedTravelTime implements PersonalizableTravelTime {

	private double scaleFactor = 1.0;
	private TravelTimeCalculatorWithBuffer buffer;
	
	public BufferedTravelTime (TravelTimeCalculatorWithBuffer buffer) {
		this.buffer = buffer;
	}
	
	public void setScaleFactor(double scaleFactor) {
		this.scaleFactor = scaleFactor;
	}
	
	public double getScaleFactor() {
		return this.scaleFactor;
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time) {
		return buffer.getBufferedLinkTravelTime(link, time) * scaleFactor;
	}

	@Override
	public void setPerson(Person person) {
		// TODO Auto-generated method stub
	}

}
