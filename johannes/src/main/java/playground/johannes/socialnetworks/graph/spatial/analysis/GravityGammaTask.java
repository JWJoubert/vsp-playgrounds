/* *********************************************************************** *
 * project: org.matsim.*
 * GravityGammaTask.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;

/**
 * @author illenberger
 *
 */
public class GravityGammaTask extends ModuleAnalyzerTask<GravityGamma> {

	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		if(getOutputDirectory() != null) {
			Distribution distr = module.distribution((Set<? extends SpatialVertex>) graph.getVertices());
			try {
				writeHistograms(distr, 0.2, false, "gamma");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("No output dir!");
		}
	}

}
