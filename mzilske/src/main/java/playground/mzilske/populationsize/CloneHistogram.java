/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CloneHistogram.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.populationsize;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import playground.mzilske.cdranalysis.StreamingOutput;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CloneHistogram {

    static void cloneHistogram(final Population basePopulation, final Map<Id, Double> travelledDistancePerPerson, RunResource run) {
        final Map<String, Double> expectedNumberOfClones = new HashMap<String, Double>();
        final Map<String, Double> minScores = new HashMap<String, Double>();
        final Map<String, Double> maxScores = new HashMap<String, Double>();
        final Map<String, Double> avgScores = new HashMap<String, Double>();
        final Map<String, Integer> counts = new HashMap<String, Integer>();
        Scenario scenario = run.getOutputScenario();
        for (Person person : scenario.getPopulation().getPersons().values()) {
            String id = person.getId().toString();
            String originalId;
            if (id.startsWith("I"))
                originalId = id.substring(id.indexOf("_")+1);
            else
                originalId = id;
            if (person.getPlans().size() != 2) throw new RuntimeException("Don't know about this kind of Person.");
            for (Plan plan : person.getPlans()) {
                if (plan.getPlanElements().size() > 1) {
                    double selectionProbability = ExpBetaPlanSelector.getSelectionProbability(new ExpBetaPlanSelector<Plan>(1.0), person, plan);
                    Double previous = expectedNumberOfClones.get(originalId);
                    if (previous == null)
                        expectedNumberOfClones.put(originalId, selectionProbability);
                    else
                        expectedNumberOfClones.put(originalId, previous + selectionProbability);
                    Double minScore = minScores.get(originalId);
                    if (minScore == null)
                        minScores.put(originalId, plan.getScore());
                    else
                        minScores.put(originalId, Math.min(minScore, plan.getScore()));
                    Double maxScore = maxScores.get(originalId);
                    if (maxScore == null)
                        maxScores.put(originalId, plan.getScore());
                    else
                        maxScores.put(originalId, Math.max(maxScore, plan.getScore()));
                    Integer count = counts.get(originalId);
                    if (count == null) {
                        count = 0;
                    }
                    count++;
                    counts.put(originalId, count);
                    Double avgScore = avgScores.get(originalId);
                    if (avgScore == null)
                        avgScores.put(originalId, plan.getScore());
                    else
                        avgScores.put(originalId, ((count-1) * avgScore + plan.getScore()) / count);
                }
            }

        }
        run.writeToFile("clone-histogram.txt", new StreamingOutput() {
            @Override
            public void write(PrintWriter pw) throws IOException {
                pw.printf("%s\t%s\t%s\t%s\t%s\t%s\t%s\n", "personid", "expnumber", "avgscore", "scorediff", "stratum", "basekm", "baseacts");
                for (Map.Entry<String, Double> entry : expectedNumberOfClones.entrySet()) {
                    String stratum;
                    IdImpl basePersonId = new IdImpl(entry.getKey());
                    Person basePerson = basePopulation.getPersons().get(basePersonId);
                    int size = basePerson.getSelectedPlan().getPlanElements().size();
                    int nActs;
                    if (size == 0) nActs = 0;
                    else nActs = (size - 1) / 2;
                    if (CountWorkers.isWorker(basePerson)) stratum = "worker";
                    else stratum = "nonworker";
                    pw.printf("%s\t%f\t%f\t%f\t%s\t%f\t%d\n", entry.getKey(), entry.getValue(), avgScores.get(entry.getKey()), maxScores.get(entry.getKey()) - minScores.get(entry.getKey()), stratum, travelledDistancePerPerson.get(basePersonId), nActs);
                }
            }
        });

    }

    public static void clonePopulation(Scenario scenario, int count) {
        // CloneFactor == 1 will leave everything as is, without stay-at-home-plans.
        if (count > 1) {
            for (Person person : scenario.getPopulation().getPersons().values()) {
                Plan plan2 = scenario.getPopulation().getFactory().createPlan();
                person.addPlan(plan2);
                person.setSelectedPlan(new RandomPlanSelector<Plan>().selectPlan(person));
            }
            for (Person person : new ArrayList<Person>(scenario.getPopulation().getPersons().values())) {
                for (int i = 0; i < count - 1; i++) {
                    Id personId = new IdImpl("I" + i + "_" + person.getId().toString());
                    Person clone = scenario.getPopulation().getFactory().createPerson(personId);
                    for (Plan plan : person.getPlans()) {
                        Plan clonePlan = scenario.getPopulation().getFactory().createPlan();
                        ((PlanImpl) clonePlan).copyFrom(plan);
                        clone.addPlan(clonePlan);
                    }
                    clone.setSelectedPlan(new RandomPlanSelector<Plan>().selectPlan(clone));
                    scenario.getPopulation().addPerson(clone);
                }
            }
        }
    }
}
