/* *********************************************************************** *
 * project: org.matsim.*
 * TransitTimeAllocationMutator.java
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

package playground.ikaddoura.parkAndRide.pRstrategy;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Copy/Paste of TransitTimeAllocationMutator, that calls ParkAndRidePlanMutateTimeAllocation instead
 * of TransitPlanMutateTimeAllocation.
 *
 * @author Ihab
 */
public class ParkAndRideTimeAllocationMutator extends AbstractMultithreadedModule {
	public final static String CONFIG_GROUP = "TimeAllocationMutator";
	public final static String CONFIG_MUTATION_RANGE = "mutationRange";

	private final static Logger log = Logger.getLogger(TimeAllocationMutator.class);

	private int mutationRange = 1800;
	private boolean useActivityDurations = true;

	/**
	 * Creates a new TimeAllocationMutator with a mutation range as defined in
	 * the configuration (module "TimeAllocationMutator", param "mutationRange"),
	 * or the default value of 1800 (seconds) if there is no value specified in
	 * the configuration.
	 */
	public ParkAndRideTimeAllocationMutator(Config config) {
		super(config.global());
		String range = config.findParam(CONFIG_GROUP, CONFIG_MUTATION_RANGE);
		if (range == null) {
			log.info("No mutation range defined in the config file. Using default of " + this.mutationRange + " sec.");
		} else {
			this.mutationRange = Integer.parseInt(range);
			log.info("mutation range = " + this.mutationRange);
		}
		if ( config.vspExperimental().getActivityDurationInterpretation().equals( ActivityDurationInterpretation.minOfDurationAndEndTime) ) {
			useActivityDurations = true ;
		} else if ( config.vspExperimental().getActivityDurationInterpretation().equals( ActivityDurationInterpretation.endTimeOnly ) ) {
			useActivityDurations = false ;
		} else if ( config.vspExperimental().getActivityDurationInterpretation().equals( ActivityDurationInterpretation.tryEndTimeThenDuration ) ) {
			throw new UnsupportedOperationException( "need to clarify the correct setting here.  Probably not a big deal, but not done yet.  kai, aug'10") ;
		} else {
			throw new IllegalStateException( "beahvior not defined for this configuration setting") ;
		}
	}

	/**
	 * Creates a new TimeAllocationMutator with the specified mutation range.
	 *
	 * @param mutationRange
	 */
	public ParkAndRideTimeAllocationMutator(Config config, final int mutationRange) {
		super(config.global());
		this.mutationRange = mutationRange;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		ParkAndRidePlanMutateTimeAllocation pmta = new ParkAndRidePlanMutateTimeAllocation(this.mutationRange, MatsimRandom.getLocalInstance());
		pmta.setUseActivityDurations(this.useActivityDurations);
		return pmta;
	}

//	public void setUseActivityDurations(final boolean useActivityDurations) {
//		this.useActivityDurations = useActivityDurations;
//	}
}
