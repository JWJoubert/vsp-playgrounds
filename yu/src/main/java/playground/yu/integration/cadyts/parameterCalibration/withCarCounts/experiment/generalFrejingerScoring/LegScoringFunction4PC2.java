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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalFrejingerScoring;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;

import playground.yu.utils.LeftTurnIdentifier;
import playground.yu.utils.NotAnIntersection;

/**
 * U_Leg = betaTraveling * travelTime_car + betaSpeedBumpNb * NbSpeedBumps +
 * betaLeftTurnNb * NbLeftTurns + betaIntersectionNb * NbIntersections. For
 * all the other transport modes except car temporarily is not be accounted.
 * 
 * @author yu
 * 
 */
public class LegScoringFunction4PC2 extends LegScoringFunction {
	private final static Logger log = Logger
			.getLogger(LegScoringFunction4PC2.class);

	private ScoringParameters scoringParams;
	private Map<Id, ? extends Link> links;

	private double travTimeAttrCar/* [h] */= 0d;
	private int nbSpeedBumpsAttr = 0, nbLeftTurnsAttr = 0,
			nbIntersectionsAttr = 0;

	public LegScoringFunction4PC2(Plan plan, Config config, Network network) {
		super(plan, new CharyparNagelScoringParameters(config.planCalcScore()));
		scoringParams = new ScoringParameters(config);
		links = network.getLinks();
	}

	public double getTravTimeAttrCar() {
		return travTimeAttrCar;
	}

	public int getNbSpeedBumpsAttr() {
		return nbSpeedBumpsAttr;
	}

	public int getNbLeftTurnsAttr() {
		return nbLeftTurnsAttr;
	}

	public int getNbIntersectionsAttr() {
		return nbIntersectionsAttr;
	}

	public void reset() {
		super.reset();
		travTimeAttrCar = 0d;
		nbSpeedBumpsAttr = 0;
		nbLeftTurnsAttr = 0;
		nbIntersectionsAttr = 0;
	}

	protected boolean isSpeedBump(Link link) {
		return link.getFreespeed() < 8.5;// approx.<=30km/h
	}

	protected int calcNbSpeedBumps(Leg leg) {
		int nb = 0;
		if (TransportMode.car.equals(leg.getMode())) {
			NetworkRoute route = (NetworkRoute) leg.getRoute();
			Id endLinkId = route.getEndLinkId();
			if (route.getStartLinkId().equals(endLinkId)) {
				return nb;// No speed bump at the same link.
			}
			// route links
			for (Id linkId : route.getLinkIds()) {
				if (isSpeedBump(links.get(linkId))) {
					nb++;
				}
			}
			// end link of route

			/* Route does NOT lies on a same link */
			if (isSpeedBump(links.get(endLinkId))) {
				nb++;
			}

		}
		// else {// other modes i.e. GenericRoute, teleport
		// return 0;
		// }

		return nb;
	}

	protected int calcNbLeftTurns(Leg leg) {
		int nb = 0;
		if (TransportMode.car.equals(leg.getMode())) {
			NetworkRoute route = (NetworkRoute) leg.getRoute();
			Id startLinkId = route.getStartLinkId(), endLinkId = route
					.getEndLinkId();
			if (startLinkId.equals(endLinkId)) {
				return nb;
			}
			// route links
			Link inLink = links.get(startLinkId), outLink;

			for (Id linkId : route.getLinkIds()) {
				outLink = links.get(linkId);
				if (LeftTurnIdentifier.turnLeft(inLink, outLink)) {
					nb++;
				}
				inLink = outLink;
			}
			// end link of route
			outLink = links.get(endLinkId);
			/* Route does NOT lies on a same link */
			if (LeftTurnIdentifier.turnLeft(inLink, outLink)) {
				nb++;
			}
		}
		// else {// other modes i.e. GenericRoute, teleport
		// return 0;
		// }

		return nb;
	}

	protected int calcNbIntersections(Leg leg) {
		int nb = 0;
		if (TransportMode.car.equals(leg.getMode())) {
			NetworkRoute route = (NetworkRoute) leg.getRoute();
			Id endLinkId = route.getEndLinkId();
			if (route.getStartLinkId().equals(endLinkId)) {
				return nb;// Route comprises only one Link
			}
			// route links
			for (Id linkId : route.getLinkIds()) {
				if (!NotAnIntersection.notAnIntersection(links.get(linkId)
						.getFromNode())/* is a real intersection */) {
					nb++;
				}
			}
			// end link of route
			/* Route does NOT lies on a same link */
			if (!NotAnIntersection.notAnIntersection(links.get(endLinkId)
					.getFromNode())/* is a real intersection */) {
				nb++;
			}

		}/*
		 * other modes i.e. GenericRoute, teleport, return 0;
		 */
		return nb;
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
		// double dist = 0.0; // distance in meters

		if (!TransportMode.pt.equals(leg.getMode())
				&& !TransportMode.walk.equals(leg.getMode())
				&& !TransportMode.transit_walk.equals(leg.getMode())) {
			int nbSpeedBumps = calcNbSpeedBumps(leg), nbLeftTurns = calcNbLeftTurns(leg), nbIntersections = calcNbIntersections(leg);

			tmpScore += travelTime * scoringParams.marginalUtilityOfTraveling_s
					+ nbSpeedBumps * scoringParams.betaSpeedBumpNb
					+ nbLeftTurns * scoringParams.betaLeftTurnNb
					+ nbIntersections * scoringParams.betaIntersectionNb;

			travTimeAttrCar/* [h] */+= travelTime/* [s] *// 3600d;
			nbSpeedBumpsAttr += nbSpeedBumps;
			nbLeftTurnsAttr += nbLeftTurns;
			nbIntersectionsAttr += nbIntersections;
		}

		return tmpScore;
	}
}
