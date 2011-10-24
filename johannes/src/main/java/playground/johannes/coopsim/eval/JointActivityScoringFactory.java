/* *********************************************************************** *
 * project: org.matsim.*
 * JointActivityScoringFactory.java
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
package playground.johannes.coopsim.eval;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;

import playground.johannes.coopsim.pysical.VisitorTracker;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;


/**
 * @author illenberger
 *
 */
public class JointActivityScoringFactory extends CharyparNagelScoringFunctionFactory {

	private final Map<Person, ScoringFunctionAccumulator> accumulators;
	
	private final Map<SocialVertex, JointActivityScoring> jointActScorers;
	
	private final Map<Person, SocialVertex> personVertexMap;
	
	private final VisitorTracker tracker;
	
	private final double beta;
	
	public JointActivityScoringFactory(SocialGraph graph, VisitorTracker tracker, PlanCalcScoreConfigGroup config, double beta, Scenario scenario) {
		super(config, scenario.getNetwork());
		this.tracker = tracker;
		this.beta = beta;
		this.accumulators = new HashMap<Person, ScoringFunctionAccumulator>();
		
		personVertexMap = new HashMap<Person, SocialVertex>(graph.getVertices().size());
		for(SocialVertex v : graph.getVertices())
			personVertexMap.put(v.getPerson().getPerson(), v);
		
		jointActScorers = new HashMap<SocialVertex, JointActivityScoring>(graph.getVertices().size());
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		Person person = plan.getPerson();
		ScoringFunctionAccumulator accumulator = accumulators.get(person);
		
		if(accumulator == null) {
			accumulator = (ScoringFunctionAccumulator) super.createNewScoringFunction(plan); //FIXME act type will always be home
			
			SocialVertex ego = personVertexMap.get(person);
			JointActivityScoring jointActScoring = new JointActivityScoring(ego, tracker, beta);
			accumulator.addScoringFunction(jointActScoring);
			
			accumulators.put(person, accumulator);
			jointActScorers.put(ego, jointActScoring);
		}
		
		return accumulator;
	}
	
	public void resetAccumulators() {
		for(ScoringFunctionAccumulator acc : accumulators.values()) {
			acc.reset();
		}
	}
	
	public Map<SocialVertex, JointActivityScoring> getJointActivityScorers() {
		return jointActScorers;
	}
}
