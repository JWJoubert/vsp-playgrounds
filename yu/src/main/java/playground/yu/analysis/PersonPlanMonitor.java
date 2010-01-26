/* *********************************************************************** *
 * project: org.matsim.*
 * LegMonitor.java
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

/**
 * 
 */
package playground.yu.analysis;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;

import playground.yu.utils.DebugTools;

/**
 * monitor distance and time of leg
 * 
 * @author yu
 * 
 */
public class PersonPlanMonitor {
	private double legDepTime, legArrTime = Double.NaN, legDist = 0.0/* [km] */,
			legDur = 0.0/* [h] */, actStartTime = Double.NaN,
			actEndTime = Double.NaN, actDur = 0.0/* [h] */;

	private int idx;
	private Plan plan;
	private boolean stuck = false;

	/**
	 * 
	 */
	public PersonPlanMonitor(Plan plan) {
		idx = -1;
		this.plan = plan;
	}

	public void setLegDepTime(double depTime) {
		idx += 1;
		this.legDepTime = depTime;
		this.legArrTime = Double.NaN;
	}

	public void setLegArrTime(double arrTime) {
		this.legArrTime = arrTime;
		this.legDur += this.calcLegTravelTime_h();
		this.legDist += this.calcLegDist_km();

		this.actStartTime = Double.NaN;
	}

	public void setActStartTime(double startTime) {
		this.idx += 1;
		this.actStartTime = startTime;
		this.actEndTime = Double.NaN;
	}

	public void setActEndTime(double endTime, ActivityParams actParams) {
		if (this.idx == -1)
			this.idx += 1;
		this.actEndTime = endTime;
		this.actDur += this.calcActDuration_h(actParams);
		this.legDepTime = Double.NaN;
	}

	public double getTotalDistances_km() {
		if (this.stuck)
			return 0.0;
		return legDist;
	}

	public double getTotalTravelTimes_h() {
		if (this.stuck)
			return 24.0;
		return legDur;
	}

	public double getTotalPerformTime_h(CharyparNagelScoringConfigGroup scoring) {
		if (this.stuck)
			return 0.0;
		if (this.idx % 2 == 1)
			throw new RuntimeException(PersonPlanMonitor.class.getName()
					+ "\tline:\t" + DebugTools.getLineNumber(new Exception())
					+ "\tthis.idx%2=1, it's impossible!!!\tfrom person\t"
					+ this.plan.getPerson());

		String actType = ((Activity) this.plan.getPlanElements().get(this.idx))
				.getType();
		if (Double.isNaN(this.actEndTime) && actType.startsWith("h")) {
			this.actEndTime = 24.0 * 3600.0 - 1.0;
			this.actDur += this.calcActDuration_h(scoring
					.getActivityParams(actType));
		}
		return this.actDur;
	}

	public boolean isStuck() {
		return stuck;
	}

	/**
	 * @return leg Distance in [m]
	 * @see {@code org.matsim.core.scoring.charyparNagel.LegScoringFunction}
	 *      line 100
	 */
	private double calcLegDist_km() {
		return ((Leg) this.plan.getPlanElements().get(this.idx)).getRoute()
				.getDistance() / 1000.0;
	}

	/**
	 * @return duration of this leg in [h]
	 */
	private double calcLegTravelTime_h() {
		return (this.legArrTime - this.legDepTime) / 3600.0;
	}

	private double calcActDuration_h(ActivityParams actParams) {
		if (Double.isNaN(actStartTime) || actParams.getType().startsWith("h"))
			// h or home
			this.actStartTime = 0.0;

		double openTime = actParams.getOpeningTime(), closeTime = actParams
				.getClosingTime(); //
		if (actParams.getType().startsWith("h")) {
			openTime = 0.0;
			closeTime = 24.0 * 3600.0 - 1.0;
		}

		double typicalDuration = actParams.getTypicalDuration(), zeroUtilityDuration// [h]
		= (typicalDuration / 3600.0)
				* Math.exp(-10.0 / (typicalDuration / 3600.0)
						/ actParams.getPriority()), //

		actStart = this.actStartTime, actEnd = this.actEndTime;

		if (openTime >= 0 && this.actStartTime < openTime)
			actStart = openTime;
		if (closeTime >= 0 && closeTime < this.actEndTime)
			actEnd = closeTime;
		if (openTime >= 0
				&& closeTime >= 0
				&& (openTime > this.actEndTime || closeTime < this.actStartTime)) {
			// agent could not perform action
			actStart = this.actEndTime;
			actEnd = this.actEndTime;
		}
		double durAttr = typicalDuration
				* Math
						.log(((actEnd - actStart) / 3600.0)
								/ zeroUtilityDuration) / 3600.0;
		return Math.max(durAttr, 0);
	}

	public void notifyStuck() {
		this.stuck = true;
	}

}
