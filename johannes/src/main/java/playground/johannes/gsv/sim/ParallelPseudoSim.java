/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelPseudoSim.java
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
package playground.johannes.gsv.sim;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.johannes.socialnetworks.utils.CollectionUtils;

/**
 * @author illenberger
 *
 */
public class ParallelPseudoSim {

	private final static double MIN_ACT_DURATION = 1.0;

	private final static double MIN_LEG_DURATION = 0.0;

	private SimThread[] threads;
	
	private Future<?>[] futures;
	
	private final ExecutorService executor;
	
	private final Map<String, LegSimEngine> legSimEngines;
	
	private final LegSimEngine defaultLegSimEngine;
	
	public ParallelPseudoSim(int numThreads) {
		
		executor = Executors.newFixedThreadPool(numThreads);
		
		threads = new SimThread[numThreads];
		for(int i = 0; i < numThreads; i++)
			threads[i] = new SimThread();
		
		futures = new Future[numThreads];
		
		legSimEngines = new HashMap<String, LegSimEngine>();
		defaultLegSimEngine = new DefaultLegSimEngine();
	}
		
	public void run(Collection<Plan> plans, Network network, TravelTime linkTravelTimes, EventsManager eventManager) {
		legSimEngines.put(TransportMode.car, new CarLegSimEngine(network, linkTravelTimes));
		/*
		 * split collection in approx even segments
		 */
		int n = Math.min(plans.size(), threads.length);
		List<Plan>[] segments = CollectionUtils.split(plans, n);
		/*
		 * submit tasks
		 */
		for(int i = 0; i < segments.length; i++) {
			threads[i].init(segments[i], network, linkTravelTimes, eventManager);
			futures[i] = executor.submit(threads[i]);
		}
		/*
		 * wait for threads
		 */
		for(int i = 0; i < segments.length; i++) {
			try {
				futures[i].get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
	}
	
	public class SimThread implements Runnable {
		
		private Collection<Plan> plans;
		
		private EventsManager eventManager;
		
		private LinkedList<Event> eventList;
	
		public void init(Collection<Plan> plans, Network network, TravelTime linkTravelTimes, EventsManager eventManager) {
			this.plans = plans;
			this.eventManager = eventManager;
		}

		@Override
		public void run() {
			eventList = new LinkedList<Event>();
			for (Plan plan : plans) {
				List<PlanElement> elements = plan.getPlanElements();

				double prevEndTime = 0;
				for (int idx = 0; idx < elements.size(); idx += 2) {
					Activity act = (Activity) elements.get(idx);
					/*
					 * Make sure that the activity does not end before the previous
					 * activity.
					 */
					double actEndTime = Math.max(prevEndTime + MIN_ACT_DURATION, act.getEndTime());

					if (idx > 0) {
						/*
						 * If this is not the first activity, then there must exist
						 * a leg before.
						 */
						Leg leg = (Leg) elements.get(idx - 1);
						double travelTime = Double.NaN;
						
						LegSimEngine engine = legSimEngines.get(leg.getMode());
						if(engine == null) {
							engine = defaultLegSimEngine;
						}
						
						travelTime = engine.simulate(plan.getPerson(), leg, actEndTime, eventList);
						travelTime = Math.max(MIN_LEG_DURATION, travelTime);
						double arrivalTime = travelTime + prevEndTime;
						/*
						 * If act end time is not specified...
						 */
						if (Double.isInfinite(actEndTime)) {
							throw new RuntimeException("I think this is discuraged.");
						}
						/*
						 * Make sure that the activity does not end before the agent
						 * arrives.
						 */
						actEndTime = Math.max(arrivalTime + MIN_ACT_DURATION, actEndTime);
						/*
						 * Send arrival and activity start events.
						 */
						PersonArrivalEvent arrivalEvent = new PersonArrivalEvent(arrivalTime, plan.getPerson().getId(),
								act.getLinkId(), leg.getMode());
//						eventManager.processEvent(arrivalEvent);
						eventList.add(arrivalEvent);
						ActivityStartEvent startEvent = new ActivityStartEvent(arrivalTime, plan.getPerson().getId(),
								act.getLinkId(), act.getFacilityId(), act.getType());
//						eventManager.processEvent(startEvent);
						eventList.add(startEvent);
					}

					if (idx < elements.size() - 1) {
						/*
						 * This is not the last activity, send activity end and
						 * departure events.
						 */
						ActivityEndEvent endEvent = new ActivityEndEvent(actEndTime, plan.getPerson().getId(),
								act.getLinkId(), act.getFacilityId(), act.getType());
//						eventManager.processEvent(endEvent);
						eventList.add(endEvent);
						Leg leg = (Leg) elements.get(idx + 1);
						PersonDepartureEvent departureEvent = new PersonDepartureEvent(actEndTime, plan.getPerson()
								.getId(), act.getLinkId(), leg.getMode());
//						eventManager.processEvent(deparutreEvent);
						eventList.add(departureEvent);
					}

					prevEndTime = actEndTime;
				}
			}
			
			for(Event event : eventList) {
				eventManager.processEvent(event);
			}
		}
	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();
		executor.shutdown();
	}
	
	private static class DefaultLegSimEngine implements LegSimEngine {

		@Override
		public double simulate(Person person, Leg leg, double departureTime, LinkedList<Event> eventList) {
			return leg.getRoute().getTravelTime(); //TODO replace
		}
		
	}
}
