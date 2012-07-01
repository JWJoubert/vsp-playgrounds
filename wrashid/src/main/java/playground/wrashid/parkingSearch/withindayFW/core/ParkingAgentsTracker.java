/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingAgentsTracker.java
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

package playground.wrashid.parkingSearch.withindayFW.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

import com.sun.org.apache.xml.internal.resolver.helpers.Debug;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;
import playground.wrashid.lib.obj.HashMapHashSetConcat;
import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.lib.obj.event.EventHandlerCodeSeparator;
import playground.wrashid.parkingChoice.ParkingManager;
import playground.wrashid.parkingSearch.withinday.InsertParkingActivities;
import playground.wrashid.parkingSearch.withindayFW.parkingOccupancy.ParkingOccupancyStats;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.CaptureDurationOfLastParkingOfDay;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.CaptureFirstCarDepartureTimeOfDay;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.CaptureLastActivityDurationOfDay;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.CaptureParkingWalkTimesDuringDay;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.CapturePreviousActivityDurationDuringDay;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.CaptureWalkDurationOfFirstAndLastOfDay;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.UpdateEndTimeOfPreviousActivity;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.UpdateLastParkingArrivalTime;
import playground.wrashid.parkingSearch.withindayFW.randomTestStrategyFW.ParkingStrategy;
import playground.wrashid.parkingSearch.withindayFW.utility.ParkingPersonalBetas;

// TODO: clearly inspect, which variables have not been reset at beginning of 1st iteration (after 0th iteration).

/**
 * Requirements: If we have car1-park1-walk1-act-walk2-park2-car2, scoring
 * should happen at the end of "park2" activity. The scoring of the last parking
 * activity of the day should happen at the end of the iteration
 * (AfterMobSimListener).
 * 
 * 
 * 
 * @author wrashid
 * 
 */

public class ParkingAgentsTracker extends EventHandlerCodeSeparator implements MobsimInitializedListener,
		MobsimAfterSimStepListener, AfterMobsimListener {

	private final Scenario scenario;
	private final double distance;
	private ParkingOccupancyStats parkingOccupancy;
	private final Set<Id> carLegAgents;
	private final Set<Id> searchingAgents;
	private final Set<Id> linkEnteredAgents;
	private final Set<Id> lastTimeStepsLinkEnteredAgents;
	protected final Map<Id, ExperimentalBasicWithindayAgent> agents;
	private final Map<Id, Id> selectedParkingsMap;
	private final Map<Id, Activity> nextNonParkingActivity;
	private final ParkingInfrastructure parkingInfrastructure;
	private Map<Id, Id> lastParkingFacilityId;
	private UpdateLastParkingArrivalTime lastCarArrivalTimeAtParking;
	private DoubleValueHashMap<Id> parkingIterationScoreSum;
	private ParkingStrategyManager parkingStrategyManager;
	private HashMapHashSetConcat<DuringLegIdentifier, Id> activeReplanningIdentifiers;
	private Map<Id, Double> previousNonParkingActivityStartTime;
	// private Map<Id, Double> firstParkingWalkTime;
	// private Map<Id, Double> secondParkingWalkTime;
	private Map<Id, Double> searchStartTime;
	private Map<Id, Double> lastCarMovementRegistered;
	private Set<Id> didUseCarOnce;

	private Map<Id, Double> endTimeOfPreviousActivity = new HashMap<Id, Double>();

	private Map<Id, Double> firstParkingWalkTimeOfDay = new HashMap<Id, Double>();

	private Map<Id, Integer> firstParkingActivityPlanElemIndex = new HashMap<Id, Integer>();
	private Map<Id, Integer> lastParkingActivityPlanElemIndex = new HashMap<Id, Integer>();
	private CaptureParkingWalkTimesDuringDay parkingWalkTimesDuringDay;
	private CaptureWalkDurationOfFirstAndLastOfDay walkDurationFirstAndLastOfDay;
	private CaptureDurationOfLastParkingOfDay durationOfLastParkingOfDay;

	private CapturePreviousActivityDurationDuringDay previousActivityDurationDuringDay;

	private CaptureLastActivityDurationOfDay durationOfLastActivityOfDay;
	private CaptureFirstCarDepartureTimeOfDay firstCarDepartureTimeOfDay;

	/**
	 * Tracks agents' car legs and check whether they have to start their
	 * parking search.
	 * 
	 * @param scenario
	 * @param distance
	 *            defines in which distance to the destination of a car trip an
	 *            agent starts its parking search
	 * @param parkingInfrastructure
	 */
	public ParkingAgentsTracker(Scenario scenario, double distance, ParkingInfrastructure parkingInfrastructure) {
		super();

		this.parkingOccupancy = new ParkingOccupancyStats();
		this.scenario = scenario;
		this.distance = distance;
		this.parkingInfrastructure = parkingInfrastructure;

		this.carLegAgents = new HashSet<Id>();
		this.linkEnteredAgents = new HashSet<Id>();
		this.selectedParkingsMap = new HashMap<Id, Id>();
		this.lastTimeStepsLinkEnteredAgents = new TreeSet<Id>(); // This set has
																	// to be be
																	// deterministic!
		this.searchingAgents = new HashSet<Id>();
		this.agents = new HashMap<Id, ExperimentalBasicWithindayAgent>();
		this.nextNonParkingActivity = new HashMap<Id, Activity>();

		this.parkingIterationScoreSum = new DoubleValueHashMap<Id>();
		this.setActiveReplanningIdentifiers(new HashMapHashSetConcat<DuringLegIdentifier, Id>());
		this.previousNonParkingActivityStartTime = new HashMap<Id, Double>();
		this.setSearchStartTime(new HashMap<Id, Double>());
		this.lastParkingFacilityId = new HashMap<Id, Id>();
		this.lastCarMovementRegistered = new HashMap<Id, Double>();
		this.endTimeOfPreviousActivity = new HashMap<Id, Double>();
		this.didUseCarOnce = new HashSet<Id>();

	}

	private void initHandlers() {
		addHandler(new UpdateEndTimeOfPreviousActivity(endTimeOfPreviousActivity));

		this.parkingWalkTimesDuringDay = new CaptureParkingWalkTimesDuringDay(agents, firstParkingActivityPlanElemIndex,
				lastParkingActivityPlanElemIndex);
		addHandler(this.parkingWalkTimesDuringDay);

		this.walkDurationFirstAndLastOfDay = new CaptureWalkDurationOfFirstAndLastOfDay(agents,
				firstParkingActivityPlanElemIndex, lastParkingActivityPlanElemIndex);
		addHandler(this.walkDurationFirstAndLastOfDay);

		this.durationOfLastParkingOfDay = new CaptureDurationOfLastParkingOfDay();
		addHandler(this.durationOfLastParkingOfDay);

		this.lastCarArrivalTimeAtParking = new UpdateLastParkingArrivalTime(agents);
		addHandler(this.lastCarArrivalTimeAtParking);

		this.previousActivityDurationDuringDay = new CapturePreviousActivityDurationDuringDay(agents,
				firstParkingActivityPlanElemIndex, lastParkingActivityPlanElemIndex);
		addHandler(this.previousActivityDurationDuringDay);

		this.durationOfLastActivityOfDay = new CaptureLastActivityDurationOfDay(agents, firstParkingActivityPlanElemIndex,
				lastParkingActivityPlanElemIndex);
		addHandler(this.durationOfLastActivityOfDay);

		this.setFirstCarDepartureTimeOfDay(new CaptureFirstCarDepartureTimeOfDay());
		addHandler(this.getFirstCarDepartureTimeOfDay());
	}

	public Set<Id> getSearchingAgents() {
		return Collections.unmodifiableSet(this.searchingAgents);
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		for (MobsimAgent agent : ((QSim) e.getQueueSimulation()).getAgents()) {
			this.agents.put(agent.getId(), (ExperimentalBasicWithindayAgent) agent);
		}

		initializeFirstAndLastParkingActPlanElemIndex();

		initHandlers();
	}

	private void initializeFirstAndLastParkingActPlanElemIndex() {
		for (ExperimentalBasicWithindayAgent agent : this.agents.values()) {
			Plan executedPlan = agent.getSelectedPlan();

			for (int i = 0; i < executedPlan.getPlanElements().size(); i++) {
				Id personId = agent.getPerson().getId();
				if (!firstParkingActivityPlanElemIndex.containsKey(personId)) {
					if (executedPlan.getPlanElements().get(i) instanceof ActivityImpl) {
						Activity act = (Activity) executedPlan.getPlanElements().get(i);
						if (act.getType().equalsIgnoreCase("parking")) {
							firstParkingActivityPlanElemIndex.put(personId, i);
							break;
						}
					}
				}
			}

			for (int i = executedPlan.getPlanElements().size() - 1; i >= 0; i--) {
				Id personId = agent.getPerson().getId();
				if (!lastParkingActivityPlanElemIndex.containsKey(personId)) {
					if (executedPlan.getPlanElements().get(i) instanceof ActivityImpl) {
						Activity act = (Activity) executedPlan.getPlanElements().get(i);
						if (act.getType().equalsIgnoreCase("parking")) {
							lastParkingActivityPlanElemIndex.put(personId, i);
							break;
						}
					}
				}
			}
		}
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		lastTimeStepsLinkEnteredAgents.clear();
		lastTimeStepsLinkEnteredAgents.addAll(linkEnteredAgents);
		linkEnteredAgents.clear();
	}

	public Set<Id> getLinkEnteredAgents() {
		return lastTimeStepsLinkEnteredAgents;
	}

	public void setSelectedParking(Id agentId, Id parkingFacilityId) {
		selectedParkingsMap.put(agentId, parkingFacilityId);
	}

	public Id getSelectedParking(Id agentId) {
		return selectedParkingsMap.get(agentId);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {

		super.handleEvent(event);

		if (event.getLegMode().equals(TransportMode.car)) {
			Id personId = event.getPersonId();

			getLastCarMovementTime().put(personId, event.getTime());

			getParkingInfrastructure().unParkVehicle(lastParkingFacilityId.get(personId));

			this.carLegAgents.add(personId);

			ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
			Plan executedPlan = agent.getSelectedPlan();
			int planElementIndex = agent.getCurrentPlanElementIndex();

			if (agents.get(personId).getCurrentPlanElementIndex() == 3) {

				DebugLib.traceAgent(personId,3);
				
				// DebugLib.traceAgent(personId);
			}

			// TwoHashMapsConcatenated<Id, Integer, ParkingStrategy>
			// currentlySelectedParkingStrategies =
			// parkingStrategyManager.getCurrentlySelectedParkingStrategies();
			// activeReplanningIdentifiers.put(currentlySelectedParkingStrategies.get(personId,
			// planElementIndex).getIdentifier(), personId);

			/*
			 * Get the coordinate of the next non-parking activity's facility.
			 * The currentPlanElement is a car leg, which is followed by a
			 * parking activity and a walking leg to the next non-parking
			 * activity.
			 */
			Activity nextNonParkingActivity = (Activity) executedPlan.getPlanElements().get(planElementIndex + 3);
			this.getNextNonParkingActivity().put(agent.getId(), nextNonParkingActivity);

			Link nextActivityLink = getNextActivityLink(personId);
	
			//nextActivityFacilityMap.put(personId, facility);

			Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
			double distanceToNextActivity = CoordUtils.calcDistance(nextActivityLink.getCoord(), coord);

			/*
			 * If the agent is within distance 'd' to target activity or OR If
			 * the agent enters the link where its next non-parking activity is
			 * performed, mark him ash searching Agent.
			 * 
			 * (this is actually handling a special case, where already at
			 * departure time the agent is within distance 'd' of next
			 * activity).
			 */

		
			if (shouldStartSearchParking(event.getLinkId(), nextActivityLink, distanceToNextActivity)) {
				searchingAgents.add(personId);
			}
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		super.handleEvent(event);

		if (agents.get(event.getPersonId()).getCurrentPlanElementIndex() == 9) {
			// DebugLib.traceAgent(event.getPersonId());
		}

		Id personId = event.getPersonId();
		this.carLegAgents.remove(personId);
		this.searchingAgents.remove(personId);
		this.linkEnteredAgents.remove(personId);
		this.selectedParkingsMap.remove(personId);

		ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
		int planElementIndex = agent.getCurrentPlanElementIndex();
		TwoHashMapsConcatenated<Id, Integer, ParkingStrategy> currentlySelectedParkingStrategies = parkingStrategyManager
				.getCurrentlySelectedParkingStrategies();

		if (event.getLegMode().equals(TransportMode.car)) {
			getLastCarMovementTime().put(personId, event.getTime());
			activeReplanningIdentifiers.removeValue(currentlySelectedParkingStrategies.get(personId, planElementIndex)
					.getIdentifier(), personId);
		}

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		super.handleEvent(event);

		Id personId = event.getPersonId();

		Integer currentPlanElementIndex = agents.get(personId).getCurrentPlanElementIndex();
		

		getLastCarMovementTime().put(personId, event.getTime());
		if (carLegAgents.contains(personId)) {
			if (!searchingAgents.contains(personId)) {
				Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
				
				Link nextActivityLink = getNextActivityLink(personId);
				
				double distanceToNextActivity = CoordUtils.calcDistance(nextActivityLink.getCoord(), coord);

				/*
				 * If the agent is within the parking radius
				 */
				/*
				 * If the agent enters the link where its next non-parking
				 * activity is performed.
				 */

				if (currentPlanElementIndex == 3) {
					
					ExperimentalBasicWithindayAgent experimentalBasicWithindayAgent = agents.get(personId);
					
					
					 DebugLib.traceAgent(personId,3);
				}
				
				
				
				if (shouldStartSearchParking(event.getLinkId(), nextActivityLink, distanceToNextActivity)) {
					searchingAgents.add(personId);
					linkEnteredAgents.add(personId);
					updateIdentifierOfAgentForParkingSearch(personId);
				}
			}
			// the agent is already searching: update its position
			else {
				linkEnteredAgents.add(personId);
				updateIdentifierOfAgentForParkingSearch(personId);
			}
		}
	}

	private Link getNextActivityLink(Id personId) {
		Integer currentPlanElementIndex = agents.get(personId).getCurrentPlanElementIndex();

		ActivityImpl act=(ActivityImpl) agents.get(personId).getSelectedPlan().getPlanElements().get(currentPlanElementIndex+1);
		
		Id actLinkId=act.getLinkId();
		Link actLink = scenario.getNetwork().getLinks().get(actLinkId);
		
		return actLink;
	}

	private void updateIdentifierOfAgentForParkingSearch(Id personId) {
		ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
		int planElementIndex = agent.getCurrentPlanElementIndex();

		if (agents.get(personId).getCurrentPlanElementIndex() == 3) {
			// DebugLib.traceAgent(personId);
		}

		if (parkingStrategyManager.getCurrentlySelectedParkingStrategies().get(personId, planElementIndex) == null) {
			DebugLib.emptyFunctionForSettingBreakPoint();
		}

		if (parkingStrategyManager.getCurrentlySelectedParkingStrategies().get(personId, planElementIndex).getIdentifier() == null) {
			DebugLib.emptyFunctionForSettingBreakPoint();
		}

		getActiveReplanningIdentifiers().put(
				parkingStrategyManager.getCurrentlySelectedParkingStrategies().get(personId, planElementIndex).getIdentifier(),
				personId);
	}

	
	private boolean shouldStartSearchParking(Id currentLinkId, Link nextActivityLink, double distanceToNextActivity) {
		return distanceToNextActivity <= distance || nextActivityLink.getId().equals(currentLinkId) || nextActivityLink.getLength()>distance;
	}

	@Override
	public void reset(int iteration) {
		super.reset(iteration);

		agents.clear();
		carLegAgents.clear();
		searchingAgents.clear();
		linkEnteredAgents.clear();
		selectedParkingsMap.clear();
		lastTimeStepsLinkEnteredAgents.clear();
		this.parkingIterationScoreSum = new DoubleValueHashMap<Id>();
		didUseCarOnce.clear();

		firstParkingActivityPlanElemIndex.clear();
		lastParkingActivityPlanElemIndex.clear();
		parkingOccupancy = new ParkingOccupancyStats();
	}

	public Map<Id, Activity> getNextNonParkingActivity() {
		return nextNonParkingActivity;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		// DebugLib.traceAgent(event.getPersonId());

		super.handleEvent(event);

		Id personId = event.getPersonId();
		endTimeOfPreviousActivity.put(personId, event.getTime());

		ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
		Plan executedPlan = agent.getSelectedPlan();
		int planElementIndex = agent.getCurrentPlanElementIndex();

		if (event.getActType().equalsIgnoreCase("parking")) {
			lastParkingFacilityId.put(personId, event.getFacilityId());

			Leg nextLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex + 1);

			if (isPlanElementDuringDay(personId, planElementIndex) && nextLeg.getMode().equals(TransportMode.car)) {

				updateParkingScoreDuringDay(event);

			}

		} else {

		}

	}

	private boolean isPlanElementDuringDay(Id personId, int planElementIndex) {
		return planElementIndex > firstParkingActivityPlanElemIndex.get(personId)
				&& planElementIndex < lastParkingActivityPlanElemIndex.get(personId);
	}

	// precondition: method used on activity with type parking
	// private boolean isEndParkingActivity(Id personId) {
	// ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
	// Plan executedPlan = agent.getSelectedPlan();
	// int planElementIndex = agent.getCurrentPlanElementIndex();
	//
	// Leg nextLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex +
	// 1);
	//
	// return nextLeg.getMode().equals(TransportMode.car);
	// }

	private void updateParkingScoreDuringDay(ActivityEndEvent event) {
		double parkingScore = 0.0;

		Id personId = event.getPersonId();

		double parkingArrivalTime = lastCarArrivalTimeAtParking.getTime(personId);
		double parkingDepartureTime = event.getTime();
		double parkingDuration = GeneralLib.getIntervalDuration(lastCarArrivalTimeAtParking.getTime(personId),
				parkingDepartureTime);
		double activityDuration = previousActivityDurationDuringDay.getDuration(personId);
		Id parkingFacilityId = event.getFacilityId();

		// parking cost scoring

		parkingScore += getParkingCostScore(personId, parkingArrivalTime, parkingDuration, parkingFacilityId);

		// parking walk time

		double walkingTimeTotalInMinutes = parkingWalkTimesDuringDay.getSumBothParkingWalkDurationsInSecond(personId) / 60.0;
		parkingScore += getWalkScore(personId, activityDuration, walkingTimeTotalInMinutes);

		// parking search time

		if (this.getSearchStartTime().get(personId) == null) {
			List<PlanElement> planElements = agents.get(personId).getSelectedPlan().getPlanElements();
			System.out.println(agents.get(personId).getCurrentPlanElementIndex());

			System.out
					.println("first possiblity: probably, you should start earlier the search with this algorithm (agent did not register with any identifier for this search till now and already drove to initially planned parking)");

			System.out
			.println("second probability: two consquete parking are on the same link");
			
			System.out
			.println("third probability: the network is too coarse, so that the agent reaches the destination, even before search area started (due to long link) => this should be fixed now in method 'shouldStartSearchParking'");
			
			// as initial parking can be located away from real destination
			// (even further away for test cases than the start search range,
			// this error can occur => must fix it - one way to do it: place
			// initial parking always at the destination link (TODO)

		}

		double parkingSearchTimeInMinutes = 0;

		if (ifParkingSearchTimeDifferentThanZero(personId)) {
			parkingSearchTimeInMinutes = GeneralLib.getIntervalDuration(this.getSearchStartTime().get(personId),
					parkingArrivalTime) / 60;
		}

		parkingScore += getSearchTimeScore(personId, activityDuration, parkingSearchTimeInMinutes);

		parkingIterationScoreSum.incrementBy(personId, parkingScore);

		Integer previousCarLegPlanElementIndex = getIndexOfPreviousCarLeg(personId);
		parkingStrategyManager.getCurrentlySelectedParkingStrategies().get(personId, previousCarLegPlanElementIndex)
				.putScore(personId, previousCarLegPlanElementIndex, parkingScore);

		// System.out.println(agents.get(personId).getCurrentPlanElementIndex());
		// Integer currentPlanElementIndex =
		// agents.get(personId).getCurrentPlanElementIndex();
		// DebugLib.traceAgent(personId);
		// reset search time
		getSearchStartTime().remove(personId);

		parkingOccupancy.updateParkingOccupancy(parkingFacilityId, parkingArrivalTime, parkingDepartureTime);

	}

	private boolean ifParkingSearchTimeDifferentThanZero(Id personId) {
		
		if (this.getSearchStartTime().get(personId)==null){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		return this.getSearchStartTime().get(personId) != Double.NEGATIVE_INFINITY;
	}

	private Integer getIndexOfPreviousCarLeg(Id personId) {
		ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
		Plan executedPlan = agent.getSelectedPlan();
		int planElementIndex = agent.getCurrentPlanElementIndex();

		for (int i = planElementIndex; i > 0; i--) {
			List<PlanElement> planElements = executedPlan.getPlanElements();
			if (planElements.get(i) instanceof Leg) {
				Leg leg = (Leg) planElements.get(i);

				if (leg.getMode().equals(TransportMode.car)) {
					return i;
				}
			}
		}

		DebugLib.stopSystemAndReportInconsistency("this is not allowed to happen - assumption broken");
		return null;
	}

	private void updateNextParkingActivityIfNeeded(Id personId) {
		ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
		Plan executedPlan = agent.getSelectedPlan();
		int planElementIndex = agent.getCurrentPlanElementIndex();

		Activity currentParking = (Activity) executedPlan.getPlanElements().get(planElementIndex);
		Activity nextParking = (Activity) executedPlan.getPlanElements().get(planElementIndex + 2);

		if (currentParking.getLinkId() == nextParking.getLinkId()) {
			Id newParkingFacilityId = getParkingInfrastructure().getClosestParkingFacilityNotOnLink(currentParking.getCoord(),
					currentParking.getLinkId());
			Activity newParkingAct = InsertParkingActivities.createParkingActivity(scenario, newParkingFacilityId);
			executedPlan.getPlanElements().remove(planElementIndex + 2);
			executedPlan.getPlanElements().add(planElementIndex + 2, newParkingAct);
		}

	}

	public double getSearchTimeScore(Id personId, double activityDuration, double parkingSearchTimeInMinutes) {
		ParkingPersonalBetas parkingPersonalBetas = parkingStrategyManager.getParkingPersonalBetas();
		return parkingPersonalBetas.getParkingSearchTimeBeta(personId, activityDuration) * parkingSearchTimeInMinutes;
	}

	public double getWalkScore(Id personId, double activityDuration, double walkingTimeTotalInMinutes) {
		ParkingPersonalBetas parkingPersonalBetas = parkingStrategyManager.getParkingPersonalBetas();

		return parkingPersonalBetas.getParkingWalkTimeBeta(personId, activityDuration) * walkingTimeTotalInMinutes;

	}

	public double getParkingCostScore(Id personId, double parkingArrivalTime, double parkingDuration, Id facilityId) {
		ParkingPersonalBetas parkingPersonalBetas = parkingStrategyManager.getParkingPersonalBetas();

		Double parkingCost = getParkingInfrastructure().getParkingCostCalculator().getParkingCost(facilityId, parkingArrivalTime,
				parkingDuration);

		if (parkingCost == null) {
			DebugLib.stopSystemAndReportInconsistency("probably the facilityId set is not that of a parking, resp. no mapping found");
		}
		return parkingPersonalBetas.getParkingCostBeta(personId) * parkingCost;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		for (Id personId : this.parkingIterationScoreSum.keySet()) {
			processScoreOfLastParking(personId);

			ScoringFunction scoringFunction = event.getControler().getPlansScoring().getScoringFunctionForAgent(personId);
			scoringFunction.addMoney(parkingIterationScoreSum.get(personId));

		}

		parkingOccupancy.writeOutParkingOccupanciesTxt(event.getControler());
		parkingOccupancy.writeOutParkingOccupancySumPng(event.getControler());
	}

	private void processScoreOfLastParking(Id personId) {
		double parkingScore = 0.0;

		Double parkingArrivalTime = lastCarArrivalTimeAtParking.getTime(personId);
		double lastActivityDurationOfDay = durationOfLastParkingOfDay.getDuration(personId);

		// parking cost scoring

		parkingScore += getParkingCostScore(personId, parkingArrivalTime, lastActivityDurationOfDay,
				getLastParkingFacilityIdOfDay(personId));

		// parking walk time

		double walkingTimeTotalInMinutes = walkDurationFirstAndLastOfDay.getDuration(personId) / 60.0;
		parkingScore += getWalkScore(personId, lastActivityDurationOfDay, walkingTimeTotalInMinutes);

		// parking search time
		double parkingSearchTimeInMinutes = 0;
		if (ifParkingSearchTimeDifferentThanZero(personId)) {
			parkingSearchTimeInMinutes = GeneralLib.getIntervalDuration(this.getSearchStartTime().get(personId),
					parkingArrivalTime) / 60;
		}
		parkingScore += getSearchTimeScore(personId, lastActivityDurationOfDay, parkingSearchTimeInMinutes);

		parkingIterationScoreSum.incrementBy(personId, parkingScore);

		Integer lastCarLegIndexOfDay = getLastCarLegIndexOfDay(personId);
		parkingStrategyManager.getCurrentlySelectedParkingStrategies().get(personId, lastCarLegIndexOfDay)
				.putScore(personId, lastCarLegIndexOfDay, parkingScore);

	}

	private Integer getLastCarLegIndexOfDay(Id personId) {
		ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
		Plan executedPlan = agent.getSelectedPlan();
		int planElementIndex = agent.getCurrentPlanElementIndex();

		List<PlanElement> planElements = executedPlan.getPlanElements();
		// -4 should is the first possible index of a car leg, starting from the
		// end of day
		for (int i = planElements.size() - 4; i > 0; i--) {

			if (planElements.get(i) instanceof Leg) {
				Leg leg = (Leg) planElements.get(i);

				if (leg.getMode().equals(TransportMode.car)) {
					return i;
				}
			}
		}

		DebugLib.stopSystemAndReportInconsistency("this is not allowed to happen - assumption broken");
		return null;
	}

	private Id getLastParkingFacilityIdOfDay(Id personId) {
		ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
		Plan executedPlan = agent.getSelectedPlan();

		Activity lastParkingActivity = (Activity) executedPlan.getPlanElements().get(
				lastParkingActivityPlanElemIndex.get(personId));

		return lastParkingActivity.getFacilityId();
	}

	public ParkingStrategyManager getParkingStrategyManager() {
		return parkingStrategyManager;
	}

	public void setParkingStrategyManager(ParkingStrategyManager parkingStrategyManager) {
		this.parkingStrategyManager = parkingStrategyManager;
	}

	public HashMapHashSetConcat<DuringLegIdentifier, Id> getActiveReplanningIdentifiers() {
		return activeReplanningIdentifiers;
	}

	public void setActiveReplanningIdentifiers(HashMapHashSetConcat<DuringLegIdentifier, Id> activeReplanningIdentifiers) {
		this.activeReplanningIdentifiers = activeReplanningIdentifiers;
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		super.handleEvent(event);

		if (!event.getActType().equalsIgnoreCase("parking")) {
			previousNonParkingActivityStartTime.put(event.getPersonId(), event.getTime());
		}

		if (event.getActType().equalsIgnoreCase("parking")) {
			ExperimentalBasicWithindayAgent agent = this.agents.get(event.getPersonId());
			Plan executedPlan = agent.getSelectedPlan();
			int planElementIndex = agent.getCurrentPlanElementIndex();

			Leg nextLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex + 1);

			if (!didUseCarOnce.contains(event.getPersonId()) && nextLeg.getMode().equals(TransportMode.car)) {
				double walkDuration = GeneralLib.getIntervalDuration(endTimeOfPreviousActivity.get(event.getPersonId()),
						event.getTime());
				firstParkingWalkTimeOfDay.put(event.getPersonId(), walkDuration);
				didUseCarOnce.add(event.getPersonId());
			}

			Activity nextAct = (Activity) executedPlan.getPlanElements().get(planElementIndex + 2);

			if (nextAct.getType().equalsIgnoreCase("parking")) {
				// if current parking activity linkId==next parking activity
				// link Id => change link Id of next parking activity!
				// updateNextParkingActivityIfNeeded(event.getPersonId());
			}

		}
	}

	public void putSearchStartTime(Id personId, double searchStartTime) {
		this.getSearchStartTime().put(personId, searchStartTime);
	}

	public Map<Id, Double> getSearchStartTime() {
		return searchStartTime;
	}

	public void setSearchStartTime(Map<Id, Double> searchStartTime) {
		this.searchStartTime = searchStartTime;
	}

	public Map<Id, Double> getLastCarMovementTime() {
		return lastCarMovementRegistered;
	}

	public void setLastCarMovementRegistered(Map<Id, Double> lastCarMovementRegistered) {
		this.lastCarMovementRegistered = lastCarMovementRegistered;
	}

	public ParkingInfrastructure getParkingInfrastructure() {
		return parkingInfrastructure;
	}

	public CaptureFirstCarDepartureTimeOfDay getFirstCarDepartureTimeOfDay() {
		return firstCarDepartureTimeOfDay;
	}

	public void setFirstCarDepartureTimeOfDay(CaptureFirstCarDepartureTimeOfDay firstCarDepartureTimeOfDay) {
		this.firstCarDepartureTimeOfDay = firstCarDepartureTimeOfDay;
	}

}
