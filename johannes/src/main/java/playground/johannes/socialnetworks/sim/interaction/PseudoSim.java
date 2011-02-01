/* *********************************************************************** *
 * project: org.matsim.*
 * PseudoSim.java
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
package playground.johannes.socialnetworks.sim.interaction;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.LinkNetworkRoute;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.util.TravelTime;

/**
 * @author illenberger
 * 
 */
public class PseudoSim {

	private static final Logger logger = Logger.getLogger(PseudoSim.class);
	
	private final static double MIN_ACT_DURATION = 1.0;
	
	private final static double MIN_LEG_DURATION = 0.0;

	private Queue<Event> eventQueue;

	private Network network;

	private final Comparator<? super Event> comparator = new Comparator<Event>() {

		@Override
		public int compare(Event o1, Event o2) {
			double r = o1.getTime() - o2.getTime();
			if (r > 0)
				return 1;
			if (r < 0)
				return -1;
			else {
				if (o1 == o2)
					return 0;
				else {
					int rank = rank(o1) - rank(o2);
					if (rank == 0)
						return o1.hashCode() - o2.hashCode();
					else
						return rank;
				}
			}
		}

		private int rank(Event event) {
			if (event instanceof ActivityEndEvent)
				return 1;
			else if (event instanceof AgentDepartureEvent)
				return 2;
			else if (event instanceof AgentArrivalEvent)
				return 3;
			else if (event instanceof ActivityStartEvent)
				return 4;
			else
				return 5;
		}
	};

	public void run(Population population, Network network, TravelTime linkTravelTimes, EventsManagerImpl eventManager) {
		this.network = network;

		eventManager.resetHandlers(0);
		eventQueue = new PriorityQueue<Event>(population.getPersons().size(), comparator);

		logger.info("Creating events...");

		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			List<PlanElement> elements = plan.getPlanElements();

			boolean carMode = true;
			for (int i = 1; i < elements.size(); i += 2) {
				if (!((Leg) elements.get(i)).getMode().equalsIgnoreCase("car")) {
					carMode = false;
					break;
				}
			}

			if (carMode) {
				double prevEndTime = 0;
				for (int idx = 0; idx < elements.size(); idx += 2) {
					Activity act = (Activity) elements.get(idx);
					/*
					 * Make sure that the activity does not end before the
					 * previous activity.
					 */
					double actEndTime = Math.max(prevEndTime + MIN_ACT_DURATION, act.getEndTime());

					if (idx > 0) {
						/*
						 * If this is not the first activity, then there must
						 * exist a leg before.
						 */
						LinkNetworkRoute route = (LinkNetworkRoute) ((Leg) elements.get(idx - 1)).getRoute();
						double travelTime = calcRouteTravelTime(route, prevEndTime, linkTravelTimes);
						travelTime = Math.max(MIN_LEG_DURATION, travelTime);
						double arrivalTime = travelTime + prevEndTime;
						/*
						 * If act end time is not specified...
						 */
						if (Double.isInfinite(actEndTime)) {
							double duration = ((ActivityImpl) act).getDuration();
							if(Double.isInfinite(duration)) {
								/*
								 * Duration is not specified, set end time to the end of day.
								 */
								actEndTime = 86400;
							} else {
								actEndTime = arrivalTime + duration; 
							}
						}
						/*
						 * Make sure that the activity does not end before the
						 * agent arrives.
						 */
						actEndTime = Math.max(arrivalTime + MIN_ACT_DURATION, actEndTime);
						/*
						 * Send arrival and activity start events.
						 */
						AgentArrivalEvent arrivalEvent = new AgentArrivalEventImpl(arrivalTime, person.getId(),	act.getLinkId(), TransportMode.car);
						eventQueue.add(arrivalEvent);
						ActivityEvent startEvent = new ActivityStartEventImpl(arrivalTime, person.getId(), act.getLinkId(), act.getFacilityId(), act.getType());
						eventQueue.add(startEvent);
					}

					
					if (idx < elements.size() - 1) {
						/*
						 * Id this is not the last activity, send activity end and departure events.
						 */
						ActivityEvent endEvent = new ActivityEndEventImpl(actEndTime, person.getId(), act.getLinkId(), act.getFacilityId(), act.getType());
						eventQueue.add(endEvent);
						AgentDepartureEvent deparutreEvent = new AgentDepartureEventImpl(actEndTime, person.getId(), act.getLinkId(), TransportMode.car);
						eventQueue.add(deparutreEvent);
					}

					prevEndTime = actEndTime;
				}
			}
		}

		logger.info("Processing events...");

		Event event;
		while ((event = eventQueue.poll()) != null) {
			eventManager.processEvent(event);
		}
	}

	private double calcRouteTravelTime(LinkNetworkRoute route, double startTime, TravelTime travelTime) {
		double tt = 0;
		List<Id> ids = route.getLinkIds();
		for (int i = 1; i < ids.size(); i++) {
			tt += travelTime.getLinkTravelTime(network.getLinks().get(ids.get(i)), startTime);
		}
		return Math.max(tt, 1.0);
	}
}
