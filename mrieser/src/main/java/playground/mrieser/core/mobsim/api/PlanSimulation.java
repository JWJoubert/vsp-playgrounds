/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.core.mobsim.api;

import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;

import playground.mrieser.core.mobsim.features.SimFeature;

/**
 * @author mrieser
 */
public interface PlanSimulation {

	/**
	 * Sets the {@link PlanElementHandler} for the given class <tt>klass</tt> of {@link PlanElement}s.
	 *
	 * @param klass
	 * @param handler
	 * @return the previous <tt>PlanElementHandler</tt> associated with <tt>klass</tt>, or
   *         <tt>null</tt> if there was not yet any <tt>PlanElementHandler</tt> registered for <tt>klass</tt>.
	 */
	public PlanElementHandler setPlanElementHandler(final Class<? extends PlanElement> klass, final PlanElementHandler handler);

	public PlanElementHandler removePlanElementHandler(final Class<? extends PlanElement> klass);

	public PlanElementHandler getPlanElementHandler(final Class<? extends PlanElement> klass);

	public void setSimEngine(final NewSimEngine engine);

	public void runSim();

	public void addSimFeature(final SimFeature feature);

	public List<SimFeature> getSimFeatures();
}
