/* *********************************************************************** *
 * project: org.matsim.*
 * ObservedAnalyzerTask.java
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

import java.util.Set;

import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.contrib.sna.graph.analysis.DegreeTask;
import org.matsim.contrib.sna.graph.analysis.GraphSizeTask;
import org.matsim.contrib.sna.graph.analysis.TransitivityTask;

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.analysis.ComponentsTask;
import playground.johannes.socialnetworks.graph.social.analysis.AgeTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.AcceptanceProbabilityTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.DegreeDensityTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.DistanceTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.EdgeCostsTask;
import playground.johannes.socialnetworks.graph.spatial.generators.GravityCostFunction;
import playground.johannes.socialnetworks.snowball2.analysis.ObservedDegree;
import playground.johannes.socialnetworks.snowball2.analysis.ObservedTransitivity;
import playground.johannes.socialnetworks.snowball2.analysis.WaveSizeTask;
import playground.johannes.socialnetworks.snowball2.social.analysis.ObservedAge;
import playground.johannes.socialnetworks.snowball2.spatial.analysis.ObservedDistance;
import playground.johannes.socialnetworks.snowball2.spatial.analysis.ObservedEdgeCosts;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class ObservedAnalyzerTask extends AnalyzerTaskComposite {
	
	public ObservedAnalyzerTask(ZoneLayer zones, Set<Point> choiceSet) {
		addTask(new GraphSizeTask());
		addTask(new WaveSizeTask());
		
		DegreeTask degree = new DegreeTask();
		degree.setModule(new ObservedDegree());
		addTask(degree);
		
		DegreeIterationTask degreeIt = new DegreeIterationTask();
		degreeIt.setModule(new ObservedDegree());
		addTask(degreeIt);
		
		TransitivityTask transitivity = new TransitivityTask();
		transitivity.setModule(new ObservedTransitivity());
		addTask(transitivity);
		
		DistanceTask distance = new DistanceTask();
		distance.setModule(new ObservedDistance());
		addTask(distance);
		
		AcceptanceProbabilityTask pAccept = new AcceptanceProbabilityTask(choiceSet);
		addTask(pAccept);
		
		AgeTask age = new AgeTask();
		age.setModule(new ObservedAge());
		addTask(age);
		
		DegreeDensityTask density = new DegreeDensityTask(zones);
		density.setModule(new ObservedDegree());
		addTask(density);
		
		addTask(new ComponentsTask());
		
		EdgeCostsTask costs = new EdgeCostsTask(null);
		costs.setModule(new ObservedEdgeCosts(new GravityCostFunction(1.6, 1.0)));
		addTask(costs);
	}

}
