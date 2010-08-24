/* *********************************************************************** *
 * project: org.matsim.*
 * KtiTravelCostCalculatorFactory.java
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

package playground.meisterk.kti.router;

import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;

import playground.meisterk.kti.config.KtiConfigGroup;

public class KtiTravelCostCalculatorFactory implements
		TravelCostCalculatorFactory {

	private KtiConfigGroup ktiConfigGroup = null;
	
	public KtiTravelCostCalculatorFactory(KtiConfigGroup ktiConfigGroup) {
		super();
		this.ktiConfigGroup = ktiConfigGroup;
	}

	public PersonalizableTravelCost createTravelCostCalculator(TravelTime timeCalculator,	CharyparNagelScoringConfigGroup cnScoringGroup) {
		return new KtiTravelTimeDistanceCostCalculator(timeCalculator, cnScoringGroup, ktiConfigGroup);
	}

}
