/* *********************************************************************** *
 * project: org.matsim.*
 * NormalizedEstimator.java
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

import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.sim.ProbabilityEstimator;

/**
 * This estimator normalizes the estimates from a delegate estimator such that
 * <code>\sum_i{1/p_i} = N</code>.
 * 
 * @author illenberger
 * 
 */
public class NormalizedEstimator2 implements ProbabilityEstimator {

	private ProbabilityEstimator delegate;

	private final double N;

	private double konst;

	/**
	 * Creates a new estimator.
	 * 
	 * @param delegate
	 *            a delegate estimator
	 * @param N
	 *            the size of the total population of vertices
	 */
	public NormalizedEstimator2(ProbabilityEstimator delegate, int N) {
		this.delegate = delegate;
		this.N = N;
	}

	/**
	 * Returns the estimate of the delegate estimator multiplied with
	 * <code>\sum_i{1/p_i} / N</code>.
	 * 
	 * @param vertex
	 *            a sampled vertex
	 * @return the estimate of the delegate estimator multiplied with
	 *         <code>\sum_i{1/p_i} / N</code>.
	 */
	@Override
	public double getProbability(SampledVertex vertex) {
		return konst * delegate.getProbability(vertex);
	}

	/**
	 * Updates the delegate estaimtor and calculates the normalization constant.
	 * 
	 * @param graph
	 *            a sampled graph
	 */
	@Override
	public void update(SampledGraph graph) {
		delegate.update(graph);

		double sum_p_i = 0;
		int n = 0;
		for (SampledVertex vertex : graph.getVertices()) {
			if (vertex.isSampled()) {
				sum_p_i += delegate.getProbability(vertex);
				n++;
			}
		}

		konst = n*n /(N * sum_p_i);

		
//		double sum = 0;
//		for (SampledVertex vertex : graph.getVertices()) {
//			if (vertex.isSampled()) {
//				sum += delegate.getProbability(vertex);
//			}
//		}
//
//		SampleStats stats = new SampleStats(graph);
//		
//		double n_square = Math.pow(stats.getAccumulatedNumSampled(stats.getMaxIteration()), 2);
//		konst = n_square/(double)N * 1/sum;

	}

}
