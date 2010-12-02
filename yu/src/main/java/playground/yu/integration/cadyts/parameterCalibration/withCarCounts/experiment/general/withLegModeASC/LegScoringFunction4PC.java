/* *********************************************************************** *
 * project: org.matsim.*
 * sdfasfwaeg.java
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general.withLegModeASC;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;

public class LegScoringFunction4PC extends LegScoringFunction {

	private double travTimeAttrCar/* [h] */= 0d, travTimeAttrPt/* [h] */= 0d,
			travTimeAttrWalk/* [h] */= 0d, distanceAttrCar/*
														 * [m*utils/unit_of_money
														 * ]
														 */,
			distanceAttrPt/* [m*utils/unit_of_money] */,
			distanceAttrWalk/* [m] */;

	private int carLegNo = 0, ptLegNo = 0, walkLegNo = 0;

	public LegScoringFunction4PC(Plan plan,
			CharyparNagelScoringParameters params) {
		super(plan, params);
	}

	public double getTravTimeAttrPt() {
		return travTimeAttrPt;
	}

	public double getTravTimeAttrWalk() {
		return travTimeAttrWalk;
	}

	public double getTravTimeAttrCar() {
		return travTimeAttrCar;
	}

	public int getCarLegNo() {
		return carLegNo;
	}

	public int getPtLegNo() {
		return ptLegNo;
	}

	public int getWalkLegNo() {
		return walkLegNo;
	}

	public double getDistanceAttrCar() {
		return distanceAttrCar;
	}

	public double getDistanceAttrPt() {
		return distanceAttrPt;
	}

	public double getDistanceAttrWalk() {
		return distanceAttrWalk;
	}

	public void reset() {
		super.reset();
		travTimeAttrCar = 0d;
		travTimeAttrPt = 0d;
		travTimeAttrWalk = 0d;
		carLegNo = 0;
		ptLegNo = 0;
		walkLegNo = 0;
		distanceAttrCar = 0;
		distanceAttrPt = 0d;
		distanceAttrWalk = 0d;
	}

	@Override
	protected double calcLegScore(double departureTime, double arrivalTime,
			Leg leg) {
		double tmpScore = 0d;
		double travelTime = arrivalTime - departureTime; // traveltime in
		// seconds

		/*
		 * we only as for the route when we have to calculate a distance cost,
		 * because route.getDist() may calculate the distance if not yet
		 * available, which is quite an expensive operation
		 */
		double dist = 0.0; // distance in meters

		if (TransportMode.car.equals(leg.getMode())) {
			if (params.monetaryDistanceCostRateCar != 0.0) {
				RouteWRefs route = (RouteWRefs) leg.getRoute();
				dist = route.getDistance();
				/*
				 * TODO the route-distance does not contain the length of the
				 * first or last link of the route, because the route doesn't
				 * know those. Should be fixed somehow, but how? MR, jan07
				 */
				/*
				 * TODO in the case of within-day replanning, we cannot be sure
				 * that the distance in the leg is the actual distance driven by
				 * the agent.
				 */
			}
			// distanceCar attr
			distanceAttrCar += params.marginalUtilityOfMoney * dist;
			tmpScore += travelTime * params.marginalUtilityOfTraveling_s
					+ params.monetaryDistanceCostRateCar * distanceAttrCar
					+ CharyparNagelScoringFunctionFactory4PC.offsetCar;
			// traveling attr
			travTimeAttrCar += travelTime / 3600d;
			// offsetCar attr
			carLegNo++;

		} else if (TransportMode.pt.equals(leg.getMode())) {
			if (params.monetaryDistanceCostRatePt != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			// distanceCar attr
			distanceAttrPt += params.marginalUtilityOfMoney * dist;
			tmpScore += travelTime * params.marginalUtilityOfTravelingPT_s
					+ params.monetaryDistanceCostRatePt * distanceAttrPt
					+ CharyparNagelScoringFunctionFactory4PC.offsetPt;
			// travelingPt attr
			travTimeAttrPt += travelTime / 3600d;
			// offsetPt attr
			ptLegNo++;

		} else if (TransportMode.walk.equals(leg.getMode())
				|| TransportMode.transit_walk.equals(leg.getMode())) {
			if (params.marginalUtilityOfDistanceWalk_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			tmpScore += travelTime * params.marginalUtilityOfTravelingWalk_s
					+ params.marginalUtilityOfDistanceWalk_m * dist
					+ CharyparNagelScoringFunctionFactory4PC.offsetWalk;
			// travelingWalk attr
			travTimeAttrWalk += travelTime / 3600d;
			walkLegNo++;
			distanceAttrWalk += dist;
		} else {// other mode?
			if (params.monetaryDistanceCostRateCar != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			// distanceCar attr
			distanceAttrCar += params.marginalUtilityOfMoney * dist;

			// use the same values as for "car"
			tmpScore += travelTime * params.marginalUtilityOfTraveling_s
					+ params.monetaryDistanceCostRateCar * distanceAttrCar
					+ CharyparNagelScoringFunctionFactory4PC.offsetCar;
			// traveling attr
			travTimeAttrCar += travelTime / 3600d;
			// offsetCar attr
			carLegNo++;
		}

		return tmpScore;
	}
}
