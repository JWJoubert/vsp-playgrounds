/* *********************************************************************** *
 * project: org.matsim.*
 * Users.java
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

/**
 * 
 */
package playground.ikaddoura.busCorridor.version4;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author Ihab
 *
 */
public class Users {
	private double avgExecScore;
	
	public void analyzeScores(String directoryExtIt) {
		List<Double> scores = new ArrayList<Double>();
		double scoreSum = 0.0;
		
		String outputPlanFile = directoryExtIt+"/internalIterations/output_plans.xml.gz";
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(outputPlanFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();

		for(Person person : population.getPersons().values()){
			double score = person.getSelectedPlan().getScore();
			scores.add(score);
		}
		
		for (Double score : scores){
			scoreSum = scoreSum+score;
		}
		
		this.setAvgExecScore(scoreSum/scores.size());
	}

	/**
	 * @param avgExecScore the avgExecScore to set
	 */
	public void setAvgExecScore(double avgExecScore) {
		this.avgExecScore = avgExecScore;
	}

	/**
	 * @return the avgExecScore
	 */
	public double getAvgExecScore() {
		return avgExecScore;
	}

}
