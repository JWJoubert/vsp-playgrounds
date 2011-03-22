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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general2;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;

/**
 * U_Leg = betaTraveling * travelTime_car [or pt, walk etc.] + betaNbSpeedBumps
 * * NbSpeedBumps + betaNbLeftTurns * NbLeftTurns + betaNbIntersections *
 * NbIntersections
 * 
 * @author yu
 * 
 */
public class LegScoringFunction4PC2 extends LegScoringFunction {
	private final static Logger log = Logger
			.getLogger(LegScoringFunction4PC2.class);
	private Network network;
	private double travTimeAttrCar/* [h] */= 0d
	// , travTimeAttrPt/* [h] */= 0d,travTimeAttrWalk/* [h] */= 0d
	// , distanceAttrCar/* [m*utils/unit_of_money*/,distanceAttrPt/*
	// [m*utils/unit_of_money] */,distanceAttrWalk/* [m] */
	;
	private int nbSpeedBumps = 0, nbLeftTurns = 0, nbIntersections = 0;

	// private int carLegNo = 0, ptLegNo = 0, walkLegNo = 0;

	public LegScoringFunction4PC2(Plan plan,
			CharyparNagelScoringParameters params, Network network) {
		super(plan, params);
		this.network = network;
	}

	// public double getTravTimeAttrPt() {
	// return travTimeAttrPt;
	// }
	//
	// public double getTravTimeAttrWalk() {
	// return travTimeAttrWalk;
	// }

	public double getTravTimeAttrCar() {
		return travTimeAttrCar;
	}

	public int getNbSpeedBumps() {
		return nbSpeedBumps;
	}

	public int getNbLeftTurns() {
		return nbLeftTurns;
	}

	public int getNbIntersections() {
		return nbIntersections;
	}

	// public int getCarLegNo() {
	// return carLegNo;
	// }
	//
	// public int getPtLegNo() {
	// return ptLegNo;
	// }
	//
	// public int getWalkLegNo() {
	// return walkLegNo;
	// }
	//
	// public double getDistanceAttrCar() {
	// return distanceAttrCar;
	// }
	//
	// public double getDistanceAttrPt() {
	// return distanceAttrPt;
	// }
	//
	// public double getDistanceAttrWalk() {
	// return distanceAttrWalk;
	// }

	public void reset() {
		super.reset();
		travTimeAttrCar = 0d;
		// travTimeAttrPt = 0d;
		// travTimeAttrWalk = 0d;
		nbSpeedBumps = 0;
		nbLeftTurns = 0;
		nbIntersections = 0;
	}

	private static int distanceWrnCnt = 0;

	protected int calcNbSpeedBumps(Leg leg) {
		if (TransportMode.car.equals(leg.getMode())) {
			NetworkRoute route = (NetworkRoute) leg.getRoute();
			for (Id linkId : route.getLinkIds()) {

			}
		} else {
			return 0;
		}

		return 0;
	}

	protected int calcNbLeftTurns(Leg leg) {
		return 0;
	}

	protected int calcNbIntersections(Leg leg) {
		return 0;
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
				if (distanceWrnCnt < 1) {
					/*
					 * TODO the route-distance does not contain the length of
					 * the first or last link of the route, because the route
					 * doesn't know those. Should be fixed somehow, but how? MR,
					 * jan07
					 */
					/*
					 * TODO in the case of within-day replanning, we cannot be
					 * sure that the distance in the leg is the actual distance
					 * driven by the agent.
					 */
					log
							.warn("leg distance for scoring computed from plan, not from execution (=events)."
									+ "This is not how it is meant to be, and it will fail for within-day replanning.");
					log
							.warn("Also means that first and last link are not included.");
					log.warn(Gbl.ONLYONCE);
					distanceWrnCnt++;
				}
			}
			// distanceCar attr
			// distanceAttrCar += params.marginalUtilityOfMoney * dist;
			tmpScore += travelTime * params.marginalUtilityOfTraveling_s
			// + params.monetaryDistanceCostRateCar * distanceAttrCar
					+ params.constantCar;
			// traveling attr
			travTimeAttrCar += travelTime / 3600d;
			// constantCar attr
			// carLegNo++;

		} else if (TransportMode.pt.equals(leg.getMode())) {
			if (params.monetaryDistanceCostRatePt != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			// distancePt attr
			// distanceAttrPt += params.marginalUtilityOfMoney * dist;
			tmpScore += 0;
			// travelTime * params.marginalUtilityOfTravelingPT_s
			// // + params.monetaryDistanceCostRatePt * distanceAttrPt
			// + params.constantPt;
			// travelingPt attr
			// travTimeAttrPt += travelTime / 3600d;
			// constantPt attr
			// ptLegNo++;

		} else if (TransportMode.walk.equals(leg.getMode())
				|| TransportMode.transit_walk.equals(leg.getMode())) {
			if (params.marginalUtilityOfDistanceWalk_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			tmpScore += travelTime * params.marginalUtilityOfTravelingWalk_s
					+ params.marginalUtilityOfDistanceWalk_m * dist
					+ params.constantWalk;
			// travelingWalk attr
			// travTimeAttrWalk += travelTime / 3600d;
			// constantWalk attr
			// walkLegNo++;
			// distanceWalk attr
			// distanceAttrWalk += dist;
			// } else if (TransportMode.bike.equals(leg.getMode())) {
			// tmpScore += travelTime * params.marginalUtilityOfTravelingBike_s;
			// tmpScore += params.constantBike;
		} else {// other mode?
			if (params.monetaryDistanceCostRateCar != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			// distanceCar attr
			// distanceAttrCar += params.marginalUtilityOfMoney * dist;

			// use the same values as for "car"
			tmpScore += travelTime * params.marginalUtilityOfTraveling_s
			// + params.monetaryDistanceCostRateCar * distanceAttrCar
					+ params.constantCar;
			// traveling attr
			travTimeAttrCar += travelTime / 3600d;
			// constantCar attr
			// carLegNo++;
		}

		return tmpScore;
	}
}
