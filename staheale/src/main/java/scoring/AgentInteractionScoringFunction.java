/* *********************************************************************** *
 * project: org.matsim.*
 * AgentInteractionScoringFunction.java
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

package scoring;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import miniscenario.AgentInteraction;
import occupancy.FacilityOccupancy;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
//import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.ObjectAttributes;


/*
 * Scoring function taking agent interaction into account
 */
public class AgentInteractionScoringFunction extends ActivityScoringFunction {
	private CharyparNagelScoringParameters params;
	private TreeMap<Id, FacilityOccupancy> facilityOccupancies;
	private ObjectAttributes attributes;
	private final ActivityFacilities facilities;
	private final Plan plan;
	int numberOfTimeBins = AgentInteraction.numberOfTimeBins;
	private static Logger log = Logger.getLogger(AgentInteractionScoringFunction.class);

	public AgentInteractionScoringFunction(final Plan plan,
			final CharyparNagelScoringParameters params,
			final TreeMap<Id, FacilityOccupancy> facilityOccupancies,
			final ActivityFacilities facilities,
			final ObjectAttributes attributes, double scaleNumberOfPersons) {
		super(params);
		this.params = params;
		this.facilities = facilities;
		this.plan = plan;
		this.attributes = attributes;
		this.facilityOccupancies = facilityOccupancies;
	}
	
	/* 
	 * All values >= 100800s (4h) are merged into the last time bin
	 */
	
	public int timeBinIndex(double time) {
		int lastBinIndex = this.numberOfTimeBins-1;
		int numberOfBinsPerHour = this.numberOfTimeBins/24;
		int secondsPerBin = 3600/numberOfBinsPerHour;
		return Math.min(lastBinIndex, (int)(time/secondsPerBin));
	}
	
	@Override
	protected double calcActScore(final double arrivalTime,
			final double departureTime, final Activity act) {
		
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

		// utility of performing an action, duration is >= 1, thus log is no problem ----------------
		double typicalDuration = ((PersonImpl) this.plan.getPerson()).getDesires().getActivityDuration(act.getType());

		if (duration > 0) {
			double zeroUtilityDuration = (typicalDuration / 3600.0) * Math.exp( -10.0 / (typicalDuration / 3600.0));
			double utilPerf = this.params.marginalUtilityOfPerforming_s * typicalDuration
					* Math.log((duration / 3600.0) / zeroUtilityDuration);
			double utilWait = this.params.marginalUtilityOfWaiting_s * duration;
			tmpScore += Math.max(0, Math.max(utilPerf, utilWait));
//			log.info("for person 101 utility of performing an action is: " +Math.max(0, Math.max(utilPerf, utilWait)));
			
			
		} else {
			tmpScore += 2 * this.params.marginalUtilityOfLateArrival_s * Math.abs(duration);
//			log.info("for person 101 utility of late arrival is: " +(2 * this.params.marginalUtilityOfLateArrival_s * Math.abs(duration)));
			
		}


		// DISUTILITIES: ==============================================================================
		// disutility if too early
		if (arrivalTime < activityStart) {
			// agent arrives to early, has to wait
			tmpScore += this.params.marginalUtilityOfWaiting_s * (activityStart - arrivalTime);
//			log.info("for person 101 utility of early arrival is: " +(this.params.marginalUtilityOfWaiting_s * (activityStart - arrivalTime)));
			
		}

		// disutility if too late
		double latestStartTime = closingTime;
		if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
			tmpScore += this.params.marginalUtilityOfLateArrival_s * (activityStart - latestStartTime);
//			log.info("for person 101 utility of late arrival is: " +(this.params.marginalUtilityOfLateArrival_s * (activityStart - latestStartTime)));
			
		}

		// disutility if stopping too early
//		double earliestEndTime = params.getEarliestEndTime();
//		if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
//			tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * (earliestEndTime - activityEnd);
//		}

		// disutility if going to away to late
		if (activityEnd < departureTime) {
			tmpScore += this.params.marginalUtilityOfWaiting_s * (departureTime - activityEnd);
//			log.info("for person 101 utility of going away too late is: " +(this.params.marginalUtilityOfWaiting_s * (departureTime - activityEnd)));
			
		}

		// disutility if duration was too short
		double minimalDuration = typicalDuration / 3.0;
		if ((minimalDuration >= 0) && (duration < minimalDuration)) {
			tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * (minimalDuration - duration);
//			log.info("for person " +this.plan.getPerson().getId()+ " utility of going away too soon is: " +(this.params.marginalUtilityOfEarlyDeparture_s * (minimalDuration - duration)));
			
		}
				
		// ------------disutilities of agent interaction-----------
		//TODO: how to handle activities that last longer than 24 hours...
		if (act.getType().startsWith("s")|| act.getType().startsWith("g")) {
			ActivityFacility facility = this.facilities.getFacilities().get(act.getFacilityId());
			double capacity = facility.getActivityOptions().get(act.getType()).getCapacity();
			double lowerBound = (Double) this.attributes.getAttribute(facility.getId().toString(), "LowerThreshold");
			//log.info("lower bound is " +lowerBound);
			double lowerMarginalUtility = ((Double) this.attributes.getAttribute(facility.getId().toString(), "MarginalUtilityOfUnderArousal"))/3600;

			double upperBound = (Double) this.attributes.getAttribute(facility.getId().toString(), "UpperThreshold");
			//log.info("upper bound is " +upperBound);
			double upperMarginalUtility = ((Double) this.attributes.getAttribute(facility.getId().toString(), "MarginalUtilityOfOverArousal"))/3600;

			
			int timeBinStart = timeBinIndex(activityStart);
			int timeBinEnd = timeBinIndex(activityEnd);
			//log.info("timeBinStart ("+activityStart+") is " +timeBinStart+ " and timeBinEnd ("+activityEnd+") is " +timeBinEnd+ ", therefore numberOfTimeBins is " +(timeBinEnd-timeBinStart));
			double offsetStart = ((timeBinStart+1)*900)-activityStart;
			double offsetEnd = 1;
			if ((activityEnd/900)>95){
				offsetEnd = 900; //activityEnd-(95*900);
			}
			else {
			offsetEnd = 900+(timeBinEnd*900)-activityEnd;
			}
						
			for (int i = 0; i < (timeBinEnd-timeBinStart); i++){
				double occupancy = this.facilityOccupancies.get(facility.getId()).getCurrentOccupancy((timeBinStart+i));
				//log.info("for facility " +facility.getId()+ " and agent " +plan.getPerson().getId()+ " current occupancy is: " +occupancy+ " while performing " +act.getType()+ " activity at "+(activityStart+i*900)/3600);
				double load = occupancy/capacity;
				//log.info("load is " +load);
				double dur = 900;
				if (i == 0){
					if (timeBinEnd==timeBinStart){
						dur = duration;
						//log.info("firstTimeBin, duration is: " +dur);
					}
					else{
					dur = offsetStart;
					//log.info("firstTimeBin, duration is: " +dur);
					}
				}
				if (i+1 == (timeBinEnd-timeBinStart)){
					dur = offsetEnd;
					//log.info("lastTimeBin, duration is: " +dur);
				}
				// -----------------------disutility of agent interaction underarousal
				if ((load < lowerBound)) {
					if (load < 0.02){
						load = 0.02;
					}
					double penaltyUnderarousal = (lowerMarginalUtility/load * dur);
					if (penaltyUnderarousal>0){
						log.info("positive penaltyUnderarousal of " +penaltyUnderarousal+ ", load = " +load+ ", dur = "+dur+" beta = "+lowerMarginalUtility+", " +offsetStart+ ", " +offsetEnd+", " +activityStart+ ", " +activityEnd+ ", "+timeBinEnd);
						Gbl.errorMsg("Positive penaltyUpperarousal is computed. Aborting...");
					}
					tmpScore += penaltyUnderarousal;
				//	log.info("lowerMarginalUtility is " +lowerMarginalUtility);
				//	log.info("an underarousal penalty of " +penaltyUnderarousal+ " is given due to load " +load);
				}
				// -----------------------disutility of agent interaction overarousal
				if ((load > upperBound)) {
					//load values between upperBound and 1 are treated as underarousal loads
					if (load < 1){
						double loadTemp = 1-load;
						if (loadTemp < 0.02){
							loadTemp = 0.02;
						}
						double penaltyUpperarousal = (upperMarginalUtility/loadTemp * dur);
						if (penaltyUpperarousal>0){
							log.info("positive penaltyUpperarousal of " +penaltyUpperarousal+ ", load = " +loadTemp+ ", dur = "+dur+" beta = "+upperMarginalUtility+", " +offsetStart+ ", " +offsetEnd+", " +activityStart+ ", " +activityEnd+ ", "+timeBinEnd);
							Gbl.errorMsg("Positive penaltyUpperarousal is computed. Aborting...");

						}
						tmpScore += penaltyUpperarousal;
					//	log.info("upperMarginalUtility is " +upperMarginalUtility);
					//	log.info("an overarousal penalty of " +penaltyUpperarousal+ " is given due to load " +load);
					}
					// load values higher than 1 are penalized with a fixed part and a part dependent on the load
					else {
						double penaltyStart = (upperMarginalUtility/0.02);
						double penaltyUpperarousal = ((penaltyStart + upperMarginalUtility) * load * dur);
						if (penaltyUpperarousal>0){
							log.info("positive penaltyUpperarousal of " +penaltyUpperarousal+ ", load = " +load+ ", dur = "+dur+" beta = "+upperMarginalUtility+", " +offsetStart+ ", " +offsetEnd+", " +activityStart+ ", " +activityEnd+ ", "+timeBinEnd);
							Gbl.errorMsg("Positive penaltyUpperarousal is computed. Aborting...");
						}
						tmpScore += penaltyUpperarousal;
					//	log.info("an overarousal penalty of " +penaltyUpperarousal+ " is given due to load " +load);
					}
				}
			}
		}
				
//		log.info("for person " +this.plan.getPerson().getId()+ " total activity score is: " +tmpScore);
		

		
		return tmpScore;
	}
	@Override
	protected double[] getOpeningInterval(Activity act) {

		//log.info("activity type = "+act.getType());
		// openInterval has two values
		// openInterval[0] will be the opening time
		// openInterval[1] will be the closing time
		double[] openInterval = new double[]{Time.UNDEFINED_TIME, Time.UNDEFINED_TIME};

		boolean foundAct = false;
		
		ActivityFacility facility = this.facilities.getFacilities().get(act.getFacilityId());
		
		
		
			Set<OpeningTime> opentimes = null;
			if (!act.getType().startsWith("h") && !act.getType().endsWith("a")) {
				//	if (!(facility.getActivityOptions().containsKey("home"))){
				opentimes = ((ActivityFacilityImpl) facility).getActivityOptions().get(act.getType()).getOpeningTimes(DayType.wed);
					if (opentimes == null) {
						opentimes = ((ActivityFacilityImpl) facility).getActivityOptions().get(act.getType()).getOpeningTimes(DayType.wkday);
					}
					if (opentimes == null) {
					opentimes = ((ActivityFacilityImpl) facility).getActivityOptions().get(act.getType()).getOpeningTimes(DayType.wk);
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
						//	log.info("for activity type = " +act.getType()+ " opentimes are: " +Math.min(openInterval[0], opentime.getStartTime())+ " - " +Math.max(openInterval[1], opentime.getEndTime()));
						}
						
					}

				for (OpeningTime opentime : opentimes) {

					openInterval[0] = opentime.getStartTime();
					openInterval[1] = opentime.getEndTime();
				}
			}
				
			
			return openInterval;
		
	}
	
}
