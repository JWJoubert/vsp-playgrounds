package playground.yu.scoring;

/* *********************************************************************** *
 * project: org.matsim.*
 * DummyCharyparNagelScoringFunctionFactory4PC.java
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

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.MoneyScoringFunction;


public class CharyparNagelScoringFunctionFactory4AttrRecorder extends
CharyparNagelScoringFunctionFactory {

	private final Network network;

	public CharyparNagelScoringFunctionFactory4AttrRecorder(
			final PlanCalcScoreConfigGroup config, final Network network) {
		super(config, network);
		this.network = network;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		CharyparNagelScoringParameters params = getParams();
		ScoringFunctionAccumulatorWithAttrRecorder scoringFunctionAccumulator = new ScoringFunctionAccumulatorWithAttrRecorder(
				params);
		scoringFunctionAccumulator
		.addScoringFunction(new ActivityScoringFunction(params));
		scoringFunctionAccumulator
		.addScoringFunction(new LegScoringFunctionWithAttrRecorder(plan, params,
				network));
		scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(
				params));
		scoringFunctionAccumulator
		.addScoringFunction(new AgentStuckScoringFunction(params));
		return scoringFunctionAccumulator;
	}
}
