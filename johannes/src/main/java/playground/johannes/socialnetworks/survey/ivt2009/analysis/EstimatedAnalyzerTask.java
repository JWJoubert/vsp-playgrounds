/* *********************************************************************** *
 * project: org.matsim.*
 * EstimatedAnalyzerTask.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;


import playground.johannes.sna.graph.analysis.DegreeTask;
import playground.johannes.sna.graph.analysis.TransitivityTask;
import playground.johannes.sna.snowball.SampledGraph;
import playground.johannes.sna.snowball.analysis.EstimatedTransitivity;
import playground.johannes.sna.snowball.analysis.PiEstimator;
import playground.johannes.sna.snowball.analysis.SimplePiEstimator;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.snowball2.analysis.WSMStatsFactory;
import playground.johannes.socialnetworks.snowball2.sim.EstimatorTask;
import playground.johannes.socialnetworks.snowball2.sim.deprecated.EstimatedDegree2;

/**
 * @author illenberger
 *
 */
public class EstimatedAnalyzerTask extends AnalyzerTaskComposite {

	private final static int N = 5200000;
	
	public EstimatedAnalyzerTask(SampledGraph graph) {
		PiEstimator estim = new SimplePiEstimator(N);
//		ProbabilityEstimator estim = new NormalizedEstimator(new Estimator1(N), N);
		estim.update(graph);
		
		DegreeTask kTask = new DegreeTask();
//		kTask.setModule(new EstimatedDegree2(estim, new HTEstimator(N), new HTEstimator(N)));
		kTask.setModule(new EstimatedDegree2(estim, new WSMStatsFactory()));
		addTask(kTask);
		
		TransitivityTask tTask = new TransitivityTask();
		tTask.setModule(new EstimatedTransitivity(estim, new WSMStatsFactory(), true));
		addTask(tTask);
		
		addTask(new EstimatorTask(estim));
	}
}
