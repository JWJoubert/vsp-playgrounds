/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkFactory2.java
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
package playground.johannes.socialnetworks.sim;

import org.matsim.contrib.sna.graph.GraphFactory;

import playground.johannes.socialnetworks.graph.social.SocialPerson;

/**
 * @author illenberger
 *
 */
public class SimSocialGraphFactory implements GraphFactory<SimSocialGraph, SimSocialVertex, SimSocialEdge>{

	@Override
	public SimSocialEdge createEdge() {
		return new SimSocialEdge(0);
	}
	
	public SimSocialEdge createEdge(int created) {
		return new SimSocialEdge(created);
	}

	@Override
	public SimSocialGraph createGraph() {
		return new SimSocialGraph();
	}

	@Override
	public SimSocialVertex createVertex() {
		throw new UnsupportedOperationException();
	}
	
	public SimSocialVertex createVertex(SocialPerson person) {
		return new SimSocialVertex(person);
	}

	@Override
	public SimSocialGraph copyGraph(SimSocialGraph graph) {
		throw new UnsupportedOperationException("Seems like someone is using this method...");
	}

	@Override
	public SimSocialVertex copyVertex(SimSocialVertex vertex) {
		throw new UnsupportedOperationException("Seems like someone is using this method...");
	}

	@Override
	public SimSocialEdge copyEdge(SimSocialEdge edge) {
		throw new UnsupportedOperationException("Seems like someone is using this method...");
	}

}
