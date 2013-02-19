/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelScoringFunctionFactoryWithDetailedLegScoreRecord.java
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

package playground.yu.scoring.postProcessing;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class CharyparNagelScoringFunctionFactoryWithDetailedLegScoreRecord
		extends CharyparNagelScoringFunctionFactory {

	private PlanCalcScoreConfigGroup config;

	public CharyparNagelScoringFunctionFactoryWithDetailedLegScoreRecord(
			PlanCalcScoreConfigGroup config, Network network) {
		super(config, network);
		this.config = config;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator
				.addScoringFunction(new CharyparNagelActivityScoring(new CharyparNagelScoringParameters(config)));

		LegScoringFunctionWithDetailedRecord legScoring = new LegScoringFunctionWithDetailedRecord(
				plan, new CharyparNagelScoringParameters(config), network);
		scoringFunctionAccumulator.addScoringFunction(legScoring);

		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(
				new CharyparNagelScoringParameters(config)));
		scoringFunctionAccumulator
				.addScoringFunction(new CharyparNagelAgentStuckScoring(new CharyparNagelScoringParameters(config)));
		return scoringFunctionAccumulator;
	}
}
