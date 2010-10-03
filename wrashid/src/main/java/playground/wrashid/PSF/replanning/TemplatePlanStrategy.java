/* *********************************************************************** *
 * project: org.matsim.*
 * TemplatePlanStrategy.java
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

package playground.wrashid.PSF.replanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

public class TemplatePlanStrategy implements PlanStrategy {
	
	PlanStrategy planStrategyDelegate = null ;

	public TemplatePlanStrategy(Scenario scenario) {
		this.planStrategyDelegate = new PlanStrategyImpl( new RandomPlanSelector() ) ;
		this.addStrategyModule(new TemplateStrategyModule());
	}

	public void addStrategyModule(PlanStrategyModule module) {
		planStrategyDelegate.addStrategyModule(module);
	}

	public void finish() {
		planStrategyDelegate.finish();
	}

	public int getNumberOfStrategyModules() {
		return planStrategyDelegate.getNumberOfStrategyModules();
	}

	public PlanSelector getPlanSelector() {
		return planStrategyDelegate.getPlanSelector();
	}

	public void init() {
		planStrategyDelegate.init();
	}

	public void run(Person person) {
		planStrategyDelegate.run(person);
	}

	@Override
	public String toString() {
		return planStrategyDelegate.toString();
	}
}
