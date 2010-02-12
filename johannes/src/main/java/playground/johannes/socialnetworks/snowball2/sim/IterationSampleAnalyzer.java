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

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTask;

/**
 * @author illenberger
 *
 */
public class IterationSampleAnalyzer extends SampleAnalyzer {

	private int lastIteration;

	private String output;
	
	public IterationSampleAnalyzer(String output, AnalyzerTask observed, AnalyzerTask estimated) {
		super(observed, estimated);
		this.output = output;
		lastIteration = 0;
	}
	
	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler) {
		return true;
	}

	@Override
	public boolean beforeSampling(Sampler<?, ?, ?> sampler) {
		if(sampler.getIteration() > lastIteration) {
			File file = new File(String.format("%1$s/it.%2$s", output, lastIteration));
			file.mkdirs();
			analyse(sampler.getSampledGraph(), file.getAbsolutePath());
			lastIteration = sampler.getIteration();
		}
		
		return true;
	}
	

}
