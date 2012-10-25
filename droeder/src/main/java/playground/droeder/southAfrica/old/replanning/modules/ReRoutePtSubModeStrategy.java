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
package playground.droeder.southAfrica.old.replanning.modules;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.droeder.southAfrica.old.run.PtSubModeControlerOld;

/**
 * @author droeder
 *
 */
public class ReRoutePtSubModeStrategy extends AbstractMultithreadedModule{
	private Controler c;
	
	/**
	 * <code>PlanStrategyModule</code> which reroutes pt-legs and stores pt-submodes.
	 * Aborts if the controler is not an instance of instance of <code>PtSubModeControler</code>
	 * @param c
	 */
	public ReRoutePtSubModeStrategy(Controler c) {
		super(c.getConfig().global());
		if(!(c instanceof PtSubModeControlerOld)){
			throw new IllegalArgumentException("If you want to use this replanning-strategy you are forced to use the PtSubModeControler...");
		}
		this.c = c;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return this.c.createRoutingAlgorithm();
	}

//	@Override
//	public void prepareReplanning() {
//		
//	}
//
//	@Override
//	public void handlePlan(Plan plan) {
//		this.c.createRoutingAlgorithm().run(plan);
//	}
//
//	@Override
//	public void finishReplanning() {
//		
//	}

}
