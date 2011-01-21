/* *********************************************************************** *
 * project: org.matsim.*
 * Ergm.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnetworks.graph.mcmc;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.sna.util.Composite;




/**
 * @author illenberger
 *
 */
public class Ergm extends Composite<EnsembleProbability> implements EnsembleProbability {

//	private ErgmTerm[] ergmTerms;
//	
////	private TIntDoubleHashMap norm_i = new TIntDoubleHashMap();
//	
//	public void setErgmTerms(ErgmTerm[] terms) {
//		ergmTerms = terms;
//	}
//	
//	public ErgmTerm[] getErgmTerms() {
//		return ergmTerms;
//	}
	

	public <V extends Vertex> double ratio(AdjacencyMatrix<V> y, int i, int j, boolean y_ij) {
		double prod = 1;
		for(int k = 0; k < components.size(); k++) {
			prod *= components.get(k).ratio(y, i, j, y_ij);
		}
		
//		if(Double.isInfinite(prod))
//			throw new IllegalArgumentException("H(y) must not be infinity!");
		if(Double.isNaN(prod))
			throw new IllegalArgumentException("H(y) must not be NaN!");
		
		return prod;
	}

//	public double evaluateExpHamiltonian(AdjacencyMatrix y, int i, int j, boolean y_ij) {
//		double sum = 0;
//		for(ErgmTerm term : ergmTerms) {
//			sum += term.changeStatistic(y, i, j, y_ij); 
//		}
//		return Math.exp(sum);
//	}
//
//	/* (non-Javadoc)
//	 * @see playground.johannes.socialnetworks.graph.mcmc.ConditionalDistribution#addEdge(playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix, int, int)
//	 */
//	public void addEdge(AdjacencyMatrix y, int i, int j) {
//		for(ErgmTerm term : ergmTerms) {
//			term.addEdge(y, i, j);
//		}
//		
//	}
//
//	/* (non-Javadoc)
//	 * @see playground.johannes.socialnetworks.graph.mcmc.ConditionalDistribution#removeEdge(playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix, int, int)
//	 */
//	public void removeEdge(AdjacencyMatrix y, int i, int j) {
//		for(ErgmTerm term : ergmTerms) {
//			term.removeEdge(y, i, j);
//		}
//		
//	}
	
}
