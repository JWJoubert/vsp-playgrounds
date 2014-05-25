/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CloneHistogramControlerListener.java
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

package playground.mzilske.clones;

import com.google.common.collect.ImmutableMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import playground.mzilske.cdranalysis.StreamingOutput;
import playground.mzilske.util.IterationSummaryFileControlerListener;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

class CloneHistogramControlerListener implements Provider<ControlerListener> {

    @Inject
    Scenario scenario;

    @Inject
    OutputDirectoryHierarchy controlerIO;

    @Override
    public ControlerListener get() {
        return new IterationSummaryFileControlerListener(controlerIO,
                ImmutableMap.<String, IterationSummaryFileControlerListener.Writer>of(
                        "clonestats.txt",
                        new IterationSummaryFileControlerListener.Writer() {
                            @Override
                            public StreamingOutput notifyStartup(StartupEvent event) {
                                return new StreamingOutput() {
                                    @Override
                                    public void write(PrintWriter pw) throws IOException {
                                        pw.printf("%s\t%s\t%s\n", "personid", "expnumber", "iteration");
                                    }
                                };
                            }

                            @Override
                            public StreamingOutput notifyIterationEnds(final IterationEndsEvent event) {
                                final Map<String, Double> expectedNumberOfClones = new HashMap<String, Double>();
                                for (Person person : scenario.getPopulation().getPersons().values()) {
                                    String id = person.getId().toString();
                                    String originalId;
                                    if (id.startsWith("I"))
                                        originalId = id.substring(id.indexOf("_") + 1);
                                    else
                                        originalId = id;
                                    if (person.getPlans().size() > 2)
                                        throw new RuntimeException("Don't know about this kind of Person.");
                                    for (Plan plan : person.getPlans()) {
                                        if (plan.getPlanElements().size() > 1) {
                                            double selectionProbability = ExpBetaPlanSelector.getSelectionProbability(new ExpBetaPlanSelector<Plan>(1.0), person, plan);
                                            Double previous = expectedNumberOfClones.get(originalId);
                                            if (previous == null)
                                                expectedNumberOfClones.put(originalId, selectionProbability);
                                            else
                                                expectedNumberOfClones.put(originalId, previous + selectionProbability);

                                        }
                                    }
                                }
                                return new StreamingOutput() {
                                    @Override
                                    public void write(PrintWriter pw) throws IOException {
                                        for (Map.Entry<String, Double> entry : expectedNumberOfClones.entrySet()) {
                                            pw.printf("%s\t%s\t%d\n", entry.getKey(), entry.getValue(), event.getIteration());
                                        }
                                    }
                                };
                            }
                        }
                )
        );
    }


}
