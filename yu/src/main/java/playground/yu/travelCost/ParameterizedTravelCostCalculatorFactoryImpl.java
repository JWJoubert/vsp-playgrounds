/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultTravelCostCalculatorFactoryImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.yu.travelCost;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;

/**
 * @author dgrether
 * 
 */
public class ParameterizedTravelCostCalculatorFactoryImpl implements
		TravelCostCalculatorFactory {
	private final double A, B;

	public ParameterizedTravelCostCalculatorFactoryImpl(double A, double B) {
		this.A = A;
		this.B = B;
	}

	public PersonalizableTravelCost createTravelCostCalculator(
			PersonalizableTravelTime timeCalculator,
			PlanCalcScoreConfigGroup cnScoringGroup) {
		return new ParameterizedTravelTimeDistanceCostCalculator(
				timeCalculator, cnScoringGroup, A, B);
	}

}
