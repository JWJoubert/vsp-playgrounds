/* *********************************************************************** *
 * project: org.matsim.*
 * PCStrategyManagerCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.fine;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.replanning.modules.ChangeSingleLegMode;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.utils.misc.StringUtils;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalNormal.paramCorrection.PCCtlListener;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalNormal.paramCorrection.PCStrMn;

/**
 * @author yu
 *
 */
public class PCStrategyManagerCreator {

	public static StrategyManager createStrategyManager(Controler controler) {
		Config config = controler.getConfig();
		int firstIteration = controler.getFirstIteration();
		PlanCalcScoreConfigGroup planCalcScore = config.planCalcScore();
		double brainExpBeta = planCalcScore.getBrainExpBeta();
		StrategyManager manager = new PCStrMn(controler.getNetwork(),
				firstIteration, brainExpBeta, Integer.parseInt(config
						.findParam(PCCtlListener.BSE_CONFIG_MODULE_NAME,
								"parameterDimension"/*
													 * 2, traveling , performing
													 */)));
		StrategyManagerConfigLoader.load(controler, manager);

		// deactivate generating of new Plans by plan innovation
		String disablePlanGeneratingAfterIterStr = config.findParam("bse",
				"disablePlanGeneratingAfterIter");
		int disablePlanGeneratingAfterIter;
		if (disablePlanGeneratingAfterIterStr == null) {
			disablePlanGeneratingAfterIter = controler.getLastIteration() + 1;
		} else {
			disablePlanGeneratingAfterIter = Integer
					.parseInt(disablePlanGeneratingAfterIterStr);
		}

		String[] modules = StringUtils.explode(config.findParam(
				PCCtlListener.BSE_CONFIG_MODULE_NAME, "strategyModules"), ',');
		String[] moduleProbs = StringUtils.explode(config.findParam(
				PCCtlListener.BSE_CONFIG_MODULE_NAME,
				"strategyModuleProbabilities"), ',');

		if (modules.length != moduleProbs.length) {
			throw new RuntimeException(
					"Length of Parameter :\tstrategyModules and Parameter :\tstrategyModuleProbabilities should be the same.");
		}

		for (int i = 0; i < modules.length; i++) {
			String module = modules[i].trim();
			double prob = Double.parseDouble(moduleProbs[i].trim());

			if (module.equals("ChangeExpBeta")) {
				// ChangeExpBeta
				PlanStrategy changeExpBeta = new PlanStrategyImpl(
						new ExpBetaPlanChanger(brainExpBeta));
				manager.addStrategy(changeExpBeta, 0.0);
				manager.addChangeRequest(
						firstIteration + manager.getMaxPlansPerAgent() + 1/* 505 */,
						changeExpBeta, prob);
			} else if (module.equals("SelectExpBeta")) {
				// SelectExpBeta
				PlanStrategy selectExpBeta = new PlanStrategyImpl(
						new ExpBetaPlanSelector(planCalcScore));
				manager.addStrategy(selectExpBeta, 0.0);
				manager.addChangeRequest(
						firstIteration + manager.getMaxPlansPerAgent() + 1/* 505 */,
						selectExpBeta, prob);

			} else if (module.equals("ReRoute")) {
				// ReRoute
				PlanStrategy reRoute = new PlanStrategyImpl(
						new RandomPlanSelector());
				reRoute.addStrategyModule(new ReRoute(controler));
				manager.addStrategy(reRoute, 0.0);
				manager.addChangeRequest(
						firstIteration + manager.getMaxPlansPerAgent() + 1,
						reRoute, prob);
				manager.addChangeRequest(disablePlanGeneratingAfterIter + 1,
						reRoute, 0);
			} else if (module.equals("TimeAllocationMutator")) {
				// TimeAllocationMutator
				PlanStrategy timeAllocationMutator = new PlanStrategyImpl(
						new RandomPlanSelector());
				timeAllocationMutator
						.addStrategyModule(new TimeAllocationMutator(config));
				manager.addStrategy(timeAllocationMutator, 0.0);
				manager.addChangeRequest(
						firstIteration + manager.getMaxPlansPerAgent() + 1,
						timeAllocationMutator, prob);
				manager.addChangeRequest(disablePlanGeneratingAfterIter + 1,
						timeAllocationMutator, 0);
			} else if (module.equals("ChangeSingleLegMode")) {
				// TimeAllocationMutator
				PlanStrategy changeSingleLegMode = new PlanStrategyImpl(
						new RandomPlanSelector());
				changeSingleLegMode.addStrategyModule(new ChangeSingleLegMode(
						config));
				changeSingleLegMode.addStrategyModule(new ReRoute(controler));

				manager.addStrategy(changeSingleLegMode, 0.0);
				manager.addChangeRequest(
						firstIteration + manager.getMaxPlansPerAgent() + 1,
						changeSingleLegMode, prob);
				manager.addChangeRequest(disablePlanGeneratingAfterIter + 1,
						changeSingleLegMode, 0);
			}
		}

		return manager;
	}

}
