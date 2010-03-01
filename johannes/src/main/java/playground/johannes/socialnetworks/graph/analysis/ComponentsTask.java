/* *********************************************************************** *
 * project: org.matsim.*
 * ComponentsTask.java
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
package playground.johannes.socialnetworks.graph.analysis;

import java.util.Map;

import org.matsim.contrib.sna.graph.Graph;

/**
 * @author illenberger
 *
 */
public class ComponentsTask extends ModuleAnalyzerTask<Components> {

	private static final String NUM_COMPONENTS = "n_components";
	
	public ComponentsTask() {
		setModule(new Components());
	}
	
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		int numComponents = module.countComponents(graph);
		stats.put(NUM_COMPONENTS, new Double(numComponents));		
	}

}
