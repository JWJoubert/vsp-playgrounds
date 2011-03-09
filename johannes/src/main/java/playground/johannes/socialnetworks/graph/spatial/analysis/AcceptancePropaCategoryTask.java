/* *********************************************************************** *
 * project: org.matsim.*
 * AcceptancePropaCategoryTask.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;
import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.LinLogDiscretizer;

import playground.johannes.socialnetworks.graph.analysis.AttributePartition;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.ObservedAcceptanceProbability;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 * 
 */
public class AcceptancePropaCategoryTask extends ModuleAnalyzerTask<Accessibility> {

	private Set<Point> destinations;

	private Geometry boundary;

	public AcceptancePropaCategoryTask(Accessibility module) {
		this.setModule(module);
	}
	
	public void setDestinations(Set<Point> destinations) {
		this.destinations = destinations;
	}

	public void setBoundary(Geometry boundary) {
		this.boundary = boundary;
	}

	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		Accessibility access = module;
//		access.setTargets(destinations);

		destinations = new HashSet<Point>();
		Set<Vertex> vertices = new HashSet<Vertex>();
		for (Vertex v : graph.getVertices()) {
			Point p = ((SpatialVertex) v).getPoint();
			if (p != null) {
//				if (boundary.contains(p)) {
					vertices.add(v);
					destinations.add(((SpatialVertex) v).getPoint());
//				}
			}
		}
		access.setTargets(destinations);
		
		TObjectDoubleHashMap<Vertex> normValues = access.values(vertices);
		AttributePartition partitioner = new AttributePartition(FixedSampleSizeDiscretizer.create(normValues.getValues(), 10, 2));
		TDoubleObjectHashMap<?> partitions = partitioner.partition(normValues);
		TDoubleObjectIterator<?> it = partitions.iterator();

//		AcceptanceProbability propa = new ObservedAcceptanceProbability();
		AcceptanceProbability propa = new AcceptanceProbability();

		for (int i = 0; i < partitions.size(); i++) {
			it.advance();
			double key = it.key();
			Set<SpatialVertex> partition = (Set<SpatialVertex>) it.value();
			System.out.println("Partition size = " + partition.size());
			DescriptiveStatistics distr = propa.distribution(partition, destinations);

			try {
				writeHistograms(distr, String.format("p_accept-cat%1$.1f", key), 20, 2);
				writeHistograms(distr, new LinLogDiscretizer(1000.0, 2), String.format("p_accept-cat%1$.1f.log", key),
						true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
