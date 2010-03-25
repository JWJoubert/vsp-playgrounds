/* *********************************************************************** *
 * project: org.matsim.*
 * IterationSampleAnalyzer.java
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
package playground.johannes.socialnetworks.snowball2.sim;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTask;
import playground.johannes.socialnetworks.snowball2.SampledVertexDecorator;

/**
 * @author illenberger
 *
 */
public class IterationSampleAnalyzer extends SampleAnalyzer {

	private int lastIteration;

	public IterationSampleAnalyzer(Map<String, AnalyzerTask> tasks, Collection<BiasedDistribution> estimators, String output) {
		super(tasks, estimators, output);
		lastIteration = 0;
	}
	
	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		return true;
	}

	@Override
	public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		if(sampler.getIteration() > lastIteration) {
			File file = makeDirectories(String.format("%1$s/it.%2$s", getRootDirectory(), lastIteration));
			analyse(sampler.getSampledGraph(), file.getAbsolutePath());
			lastIteration = sampler.getIteration();
		}
		
		return true;
	}

	@Override
	public void endSampling(Sampler<?, ?, ?> sampler) {
	}
	

}
