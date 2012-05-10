/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice.scoring;

import java.util.Iterator;
import java.util.Set;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.utils.misc.Time;

import playground.anhorni.surprice.DayConverter;
import playground.anhorni.surprice.Surprice;

public class LaggedActivityScoringFunction extends ActivityScoringFunction {
	
	private CharyparNagelScoringParameters params;
	private double votFactor;
	private Config config;
	private final ActivityFacilities facilities;
	private DayType day;
	private Plan plan;
		
	public LaggedActivityScoringFunction(Plan plan, CharyparNagelScoringParameters params, final Config config,
			ActivityFacilities facilities, double votFactor, String day) {
		super(params);
		super.reset();
		this.params = params;
		this.config = config;
		this.votFactor = votFactor;
		this.facilities = facilities;
		this.day = DayConverter.getDayType(day);
		this.plan = plan;
	}
	
	protected double calcActScore(final double arrivalTime, final double departureTime, final Activity act) {
		
		double f = 1.0;
		if (Boolean.parseBoolean(this.config.findParam(Surprice.SURPRICE_RUN, "useVoT"))) {
			f = this.votFactor;
		}

//		ActivityUtilityParameters actParams = this.params.utilParams.get(act.getType());
//		if (actParams == null) {
//			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters " +
//					"(module name=\"planCalcScore\" in the config file).");
//		}

		double tmpScore = 0.0;
		double[] openingInterval = this.getOpeningInterval(act);
		double openingTime = openingInterval[0];
		double closingTime = openingInterval[1];

		double activityStart = arrivalTime;
		double activityEnd = departureTime;

		if ((openingTime >=  0) && (arrivalTime < openingTime)) {
			activityStart = openingTime;
		}
		if ((closingTime >= 0) && (closingTime < departureTime)) {
			activityEnd = closingTime;
		}
		if ((openingTime >= 0) && (closingTime >= 0)
				&& ((openingTime > departureTime) || (closingTime < arrivalTime))) {
			// agent could not perform action
			activityStart = departureTime;
			activityEnd = departureTime;
		}
		double duration = activityEnd - activityStart;

		// disutility if too early
		if (arrivalTime < activityStart) {
			// agent arrives to early, has to wait
			tmpScore += this.params.marginalUtilityOfWaiting_s * f * (activityStart - arrivalTime);
		}

		// disutility if too late
		double latestStartTime = closingTime;
		if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
			tmpScore += this.params.marginalUtilityOfLateArrival_s * f * (activityStart - latestStartTime);
		}

		// utility of performing an action, duration is >= 1, thus log is no problem
		double typicalDuration = ((PersonImpl) this.plan.getPerson()).getDesires().getActivityDuration(act.getType());

		if (duration > 0) {
			double zeroUtilityDuration = (typicalDuration / 3600.0) * Math.exp( -10.0 / (typicalDuration / 3600.0));
			double utilPerf = this.params.marginalUtilityOfPerforming_s * f * typicalDuration
					* Math.log((duration / 3600.0) / zeroUtilityDuration);
			double utilWait = this.params.marginalUtilityOfWaiting_s * f * duration;
			tmpScore += Math.max(0, Math.max(utilPerf, utilWait));
		} else {
			tmpScore += 2 * this.params.marginalUtilityOfLateArrival_s * f * Math.abs(duration);
		}

//		// disutility if stopping too early
//		double earliestEndTime = actParams.getEarliestEndTime();
//		if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
//			tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * f * (earliestEndTime - activityEnd);
//		}

		// disutility if going away to late
		if (activityEnd < departureTime) {
			tmpScore += this.params.marginalUtilityOfWaiting_s * f * (departureTime - activityEnd);
		}

		// disutility if duration was too short
		double minimalDuration = typicalDuration / 3.0;
		if ((minimalDuration >= 0) && (duration < minimalDuration)) {
			tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * f * (minimalDuration - duration);
		}
		return tmpScore;
	}
	
	protected double[] getOpeningInterval(Activity act) {

		// openInterval has two values
		// openInterval[0] will be the opening time
		// openInterval[1] will be the closing time
		double[] openInterval = new double[]{Time.UNDEFINED_TIME, Time.UNDEFINED_TIME};

		boolean foundAct = false;

		ActivityFacility facility = this.facilities.getFacilities().get(act.getFacilityId());
		Iterator<String> facilityActTypeIterator = facility.getActivityOptions().keySet().iterator();
		String facilityActType = null;
		Set<OpeningTime> opentimes = null;

		while (facilityActTypeIterator.hasNext() && !foundAct) {

			facilityActType = facilityActTypeIterator.next();
			if (act.getType().substring(0, 1).equals(facilityActType.substring(0, 1))) {
				foundAct = true;

				// choose appropriate opentime:
				// if none is given, use undefined opentimes
				opentimes = ((ActivityFacilityImpl) facility).getActivityOptions().get(facilityActType).getOpeningTimes(day);
				if (opentimes == null) {
					opentimes = ((ActivityFacilityImpl) facility).getActivityOptions().get(facilityActType).getOpeningTimes(DayType.wkday);
				}
				if (opentimes != null) {
					// ignoring lunch breaks with the following procedure:
					// if there is only one wed/wkday open time interval, use it
					// if there are two or more, use the earliest start time and the latest end time
					openInterval[0] = Double.MAX_VALUE;
					openInterval[1] = Double.MIN_VALUE;
					for (OpeningTime opentime : opentimes) {
						openInterval[0] = Math.min(openInterval[0], opentime.getStartTime());
						openInterval[1] = Math.max(openInterval[1], opentime.getEndTime());
					}
				}
			}
		}
		if (!foundAct) {
			Gbl.errorMsg("No suitable facility activity type found. Aborting...");
		}
		return openInterval;
	}
}
