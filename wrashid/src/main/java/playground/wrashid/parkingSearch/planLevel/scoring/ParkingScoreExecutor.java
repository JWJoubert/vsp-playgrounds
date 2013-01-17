/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.planLevel.scoring;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.obj.plan.PersonGroups;
import playground.wrashid.parkingSearch.planLevel.ParkingGeneralLib;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;

public class ParkingScoreExecutor {

	private static HashMap<Id, Double> scoreHashMap = new HashMap<Id, Double>();

	public static HashMap<Id, Double> getScoreHashMap() {
		return scoreHashMap;
	}

	public void performScoring(AfterMobsimEvent event) {
		for (Person person : event.getControler().getPopulation().getPersons()
				.values()) {
			double score = 0.0;

			LinkedList<ActivityImpl> parkingTargetActivities = ParkingGeneralLib
					.getParkingTargetActivities(person.getSelectedPlan());

			for (ActivityImpl targetActivity : parkingTargetActivities) {
				Id parkingFacilityId = ParkingGeneralLib.getArrivalParkingAct(
						person.getSelectedPlan(), targetActivity)
						.getFacilityId();
				score += ParkingRoot.getParkingScoringFunction().getScore(
						targetActivity, person.getSelectedPlan(),
						parkingFacilityId, false);
			}

			scoreHashMap.put(person.getId(), score);

			event.getControler()
					.getEvents()
					.processEvent(
							new AgentMoneyEvent(0.0, person.getId(), score));

			updateParkingScoreSumInPersonGroupsForPerson(event.getIteration(), person.getId(), score);
		}
		printParkingScoreGraphForPersonGroups();
	}

	private void printParkingScoreGraphForPersonGroups() {
		if (ParkingRoot.getPersonGroupsForStatistics() != null) {
			PersonGroupParkingScoreGraphicGenerator.generateGraphic(ParkingRoot
					.getPersonGroupsForStatistics(),
					GlobalRegistry.controler.getControlerIO()
							.getOutputFilename("personGroupsParkingScore.png"));
		}
	}

	private void updateParkingScoreSumInPersonGroupsForPerson(int iteration, Id personId,
			Double sumOfParkingScoresForPerson) {
		if (ParkingRoot.getPersonGroupsForStatistics() != null) {
			PersonGroups personGroupsStatistics = ParkingRoot
					.getPersonGroupsForStatistics();
			int iterationNumber = iteration;
			String attribute = PersonGroupParkingScoreGraphicGenerator.iterationScoreSum + iterationNumber;
			Double currentSum = (Double) personGroupsStatistics
					.getAttributeValueForGroupToWhichThePersonBelongs(personId,
							attribute);

			if (currentSum == null) {
				currentSum = 0.0;
			}

			currentSum += sumOfParkingScoresForPerson;

			personGroupsStatistics
					.setAttributeValueForGroupToWhichThePersonBelongs(personId,
							attribute, currentSum);
		}
	}

}
