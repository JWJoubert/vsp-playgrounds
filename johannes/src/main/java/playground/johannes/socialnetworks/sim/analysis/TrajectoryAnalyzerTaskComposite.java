/* *********************************************************************** *
 * project: org.matsim.*
 * TrajectoryAnalyzerTaskComposite.java
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
package playground.johannes.socialnetworks.sim.analysis;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

/**
 * @author illenberger
 *
 */
public class TrajectoryAnalyzerTaskComposite extends TrajectoryAnalyzerTask {

	private List<TrajectoryAnalyzerTask> tasks;
	
	public TrajectoryAnalyzerTaskComposite() {
		tasks = new LinkedList<TrajectoryAnalyzerTask>();
	}
	
	public void addTask(TrajectoryAnalyzerTask task) {
		tasks.add(task);
	}
	
	public void setOutputDirectory(String output) {
		for(TrajectoryAnalyzerTask task : tasks) {
			task.setOutputDirectory(output);
		}
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		for(TrajectoryAnalyzerTask task : tasks) {
			task.analyze(trajectories, results);
		}

	}

}
