/* *********************************************************************** *
 * project: org.matsim.*
 * AgeTask.java
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
package playground.johannes.socialnetworks.graph.social.analysis;

import gnu.trove.TDoubleObjectHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.graph.social.SocialEdge;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class AgeTask extends ModuleAnalyzerTask<Age> {

	private static final Logger logger = Logger.getLogger(AgeTask.class);
	
	public static final String AGE_MEAN = "age_mean";
	
	public static final String AGE_MIN = "age_min";
	
	public static final String AGE_MAX = "age_max";
	
	public static final String AGE_CORRELATION = "r_age";
	
	public AgeTask() {
		setModule(new Age());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		Age age = module;
		Distribution distr = age.distribution((Set<? extends SocialVertex>) graph.getVertices());

		double age_min = distr.min();
		double age_max = distr.max();
		double age_mean = distr.mean();
		double r_age = age.correlationCoefficient((Set<? extends SocialEdge>) graph.getEdges());
		
		logger.info(String.format("Mean age = %1$.4f, min age = %2$s, max age = %3$s, r_age = %4$s.", age_mean, age_min, age_max, r_age));
		stats.put(AGE_MIN, age_min);
		stats.put(AGE_MAX, age_max);
		stats.put(AGE_MEAN, age_mean);
		stats.put(AGE_CORRELATION, r_age);
		
		if(getOutputDirectory() != null) {
			try {
				writeHistograms(distr, 1, false, "age.txt");
				TXTWriter.writeMap(age.correlation((Set<? extends SocialVertex>) graph.getVertices()), "age", "age_mean", getOutputDirectory() + "/age_age.mean.txt");
				
				TDoubleObjectHashMap<DescriptiveStatistics> stat = module.boxplot((Set<? extends SocialVertex>) graph.getVertices());
				TXTWriter.writeBoxplotStats(stat, getOutputDirectory() + "age_age.table.txt");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
