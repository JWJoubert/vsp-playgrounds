/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.pieter.singapore.utils.events;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

public class CepasToEvents {
	// internal classes

	private class ErrorTracker {
		class VehicleErrors {
			int stopIdNotInRoute = 0;
			int transactionCount = 0;
			int dwellEventTransactionsDropped = 0;
			int dwellEventsDropped = 0;
			int fastTransactionDropped = 0;
			int interpolatedCount = 0;
			int dwellEventCount = 0;

			@Override
			public String toString() {
				// TODO Auto-generated method stub
				return String.format("%04d\t%04d\t%04d\t%04d\t%04d\t%04d\t%04d", stopIdNotInRoute, transactionCount,
						dwellEventCount, dwellEventsDropped, dwellEventTransactionsDropped, fastTransactionDropped,
						interpolatedCount);
			}
		}

		HashMap<Id, VehicleErrors> veherrs = new HashMap<Id, CepasToEvents.ErrorTracker.VehicleErrors>();
		private boolean calledBefore = false;

		public void stopIdNotInRoute(Id vehicleId) {
			if (veherrs.get(vehicleId) == null)
				veherrs.put(vehicleId, new VehicleErrors());
			veherrs.get(vehicleId).stopIdNotInRoute++;

		}

		public void fastTransactionDropped(Id vehicleId) {
			if (veherrs.get(vehicleId) == null)
				veherrs.put(vehicleId, new VehicleErrors());
			veherrs.get(vehicleId).fastTransactionDropped++;

		}

		public void dwellEventsDropped(Id vehicleId, CepasVehicle.CepasVehicleDwellEvent cepasVehicleDwellEvent) {
			if (veherrs.get(vehicleId) == null)
				veherrs.put(vehicleId, new VehicleErrors());
			veherrs.get(vehicleId).dwellEventsDropped++;
			veherrs.get(vehicleId).dwellEventTransactionsDropped += cepasVehicleDwellEvent.cepasTransactions.size();

		}

		public void transactionCount(Id vehicleId) {
			if (veherrs.get(vehicleId) == null)
				veherrs.put(vehicleId, new VehicleErrors());
			veherrs.get(vehicleId).transactionCount++;

		}

		public String writeStats(Id vehicleId, boolean errorProcessing) {
			StringBuffer sb = new StringBuffer(
					!calledBefore ? "vehId\tstopIdNotInRoute\ttransactionCount\tdwellEventCount"
							+ "\tdwellEventsDropped\tdwellEventTransactionsDropped"
							+ "\tfastTransactionDropped\tinterpolatedDwells\tERROR\n" : "");
			if (!calledBefore)
				calledBefore = true;
			try{
				sb.append(String.format("%s\t%s\t%s\n", vehicleId.toString(), this.veherrs.get(vehicleId).toString(),errorProcessing?"Y":"N"));
				
			}catch(NullPointerException ne){
				sb.append(String.format("%s\t%s\n", vehicleId.toString(), String.format("%04d\t%04d\t%04d\t%04d\t%04d\t%04d\t%04d", 0, 0,
						0, 0, 0, 0,
						0)));
			}

			return sb.toString();
		}

		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer(
					"vehId\tstopIdNotInRoute\ttransactionCount\tdwellEventCount\tdwellEventsDropped\tdwellEventTransactionsDropped\tfastTransactionDropped\tinterpolatedDwells\n");
			for (Entry<Id, VehicleErrors> veh : veherrs.entrySet()) {
				sb.append(String.format("%s\t%s\n", veh.getKey(), veh.getValue().toString()));
			}
			return sb.toString();
		}

		public void interpolatedDwell(Id vehicleId) {
			if (veherrs.get(vehicleId) == null)
				veherrs.put(vehicleId, new VehicleErrors());
			veherrs.get(vehicleId).interpolatedCount++;

		}

		public void dwellEventsCount(Id vehicleId, int dwelleventcount) {
			if (veherrs.get(vehicleId) == null)
				veherrs.put(vehicleId, new VehicleErrors());
			veherrs.get(vehicleId).dwellEventCount = dwelleventcount;

		}

	}

	private class ValueComparator implements Comparator<Id> {

		Map<Id, Integer> base;

		public ValueComparator(Map<Id, Integer> base) {
			this.base = base;
		}

		// Note: this comparator imposes orderings that are inconsistent with
		// equals.
		@Override
		public int compare(Id a, Id b) {
			if (base.get(a) >= base.get(b)) {
				return 1;
			} else {
				return -1;
			} // returning 0 would merge keys
		}
	}

	/**
	 * A Cepas line has the same id as the corresponding MATSim line from the
	 * transit schedule. It contains a maximum of two routes, traveling in
	 * opposite directions.
	 * 
	 */
	private class CepasLine {
		public CepasLine(Id lineId) {
			super();
			this.lineId = lineId;
		}

		Id lineId;
		/**
		 * a Cepas line has a maximum of two routes, traveling in opposite
		 * directions, distinguished by 0 and 1.
		 */
		HashMap<Integer, CepasRoute> routes = new HashMap<Integer, CepasToEvents.CepasRoute>();

		@Override
		public String toString() {
			return (routes.values().toString());
		}
	}

	/**
	 * 
	 * A Cepas route can refer to different MATSim routes (as per GTFS). In the
	 * tap-in/tap-out data, the only distinguishing attribute is the line id,
	 * and the direction. Each CepasRoute has a number of PTVehicles (buses only
	 * at this stage, because we don't know which line the MRT passengers use,
	 * nor which vehicle.
	 */
	private class CepasRoute {
		/**
		 * @param direction
		 *            , values are 0 and 1
		 * @param cepasLine
		 *            , the line id which corresponds to the MATSim line id
		 */
		public CepasRoute(int direction, CepasLine cepasLine) {
			super();
			this.direction = direction;
			this.line = cepasLine;
		}

		int direction;
		CepasLine line;
		HashMap<Id, CepasVehicle> vehicles = new HashMap<Id, CepasToEvents.CepasVehicle>();

		@Override
		public String toString() {
			return (line.lineId.toString() + " : " + direction + " : " + vehicles.keySet() + "\n");
		}
	}

	public int departureId = 0;
	public static final int minimumNumberOfDwellEventsForProcessing = 2;

	private class CepasVehicle {
		/**
		 * Clusters dwell events together into a route.
		 */
		private class CepasVehicleDwellEventCluster {
			private TreeMap<Integer, CepasVehicleDwellEvent> orderedDwellEvents = new TreeMap<Integer, CepasVehicleDwellEvent>();
			private Id routeId;

			public CepasVehicleDwellEventCluster(List<CepasVehicleDwellEvent> orderedDwellEvents) {
				super();
				for (CepasVehicleDwellEvent de : orderedDwellEvents) {
					this.orderedDwellEvents.put(de.arrivalTime, de);
				}
			}

			public TreeMap<Integer, CepasVehicleDwellEvent> getOrderedDwellEvents() {
				return orderedDwellEvents;
			}

			public Id getRouteId() {
				return routeId;
			}

			public void setRouteId(Id routeId) {
				this.routeId = routeId;
			}

			private void interpolateDwellEvents() {
				LinkedList<Id> stopIds = new LinkedList<Id>();
				stopIds.addAll(routeIdToStopIdSequence.get(this.getRouteId()));
				// boolean circleRoute =
				// stopIds.getFirst().equals(stopIds.getLast());
				ArrayList<CepasVehicleDwellEvent> dwellEventsList = new ArrayList<CepasVehicleDwellEvent>();
				dwellEventsList.addAll(orderedDwellEvents.values());
				ArrayList<Integer> dwellEventsAsStopIndexList = new ArrayList<Integer>();
				for (CepasVehicleDwellEvent de : dwellEventsList) {
					dwellEventsAsStopIndexList.add(stopIds.indexOf(de.stopId));
				}
				// boolean success = true;
				for (int i = 1; i < dwellEventsAsStopIndexList.size(); i++) {
					if (dwellEventsAsStopIndexList.get(i) < dwellEventsAsStopIndexList.get(i - 1)) {
						if (dwellEventsAsStopIndexList.get(i) == 0) {
							dwellEventsAsStopIndexList.remove(i);
							dwellEventsAsStopIndexList.add(i, stopIds.size() - 1);
						}
					}
				}
				int j = 0;
				for (int i : dwellEventsAsStopIndexList) {
					if (dwellEventsAsStopIndexList.indexOf(i) == 0) {
						j = i;
						// for cases where the route starts after the 1st
						// stop
						continue;
					}
					if (i - j > 1) {
						CepasVehicleDwellEvent origDwellEvent = dwellEventsList.get(dwellEventsAsStopIndexList
								.indexOf(j));
						CepasVehicleDwellEvent destDwellEvent = dwellEventsList.get(dwellEventsAsStopIndexList
								.indexOf(i));
						double availableTime = destDwellEvent.arrivalTime - origDwellEvent.departureTime;
						ArrayList<Id> stopsToVisit = new ArrayList<Id>();
						ArrayList<Double> travelDistancesBetweenStops = new ArrayList<Double>();
						ArrayList<Double> timeWeights = new ArrayList<Double>();
						while (++j < i) {
							stopsToVisit.add(stopIds.get(j));
							double interStopDistance = getInterStopDistance(stopIds.get(j - 1), stopIds.get(j));
							if (interStopDistance < 10)
								System.err
										.printf("found an interstop distance of less than 10 metres for pair of stops %s and %s on route %s\n",
												stopIds.get(j - 1).toString(), stopIds.get(j).toString(),
												this.getRouteId());
							// errorTracker.smallInterStopDistance()
							travelDistancesBetweenStops.add(Math.max(10, interStopDistance));
						}
						// add the last element
						travelDistancesBetweenStops.add(getInterStopDistance(stopIds.get(j - 1), stopIds.get(i)));
						// weight by the distances between stops over
						// the
						// total distance
						double totalTravelDistanceBetweenStops = 0;
						for (double distance : travelDistancesBetweenStops) {
							totalTravelDistanceBetweenStops += distance;
						}
						for (double distance : travelDistancesBetweenStops) {
							timeWeights.add(distance / totalTravelDistanceBetweenStops);
						}
						int dwellTime = origDwellEvent.departureTime;
						// recall that the first stop is the origin; we
						// already have a dwell event for that
						for (Id stop : stopsToVisit) {
							dwellTime += (int) (availableTime * timeWeights.get(stopsToVisit.indexOf(stop)));

							CepasVehicleDwellEvent dwellEvent = new CepasVehicleDwellEvent(dwellTime, dwellTime + 1,
									stop);
							this.orderedDwellEvents.put(dwellTime, dwellEvent);
							errorTracker.interpolatedDwell(vehicleId);
						}
						// interpolateDwellEvents();

					} else {
						j = i;
					}

				}
			}

		}

		private class CepasVehicleDwellEvent implements Comparable<CepasVehicleDwellEvent> {
			int arrivalTime;
			int departureTime;
			Id stopId;

			/*
			 * if the arrival event is triggered by alighting event, mark the
			 * stop event, and replace the arrival time by the first tap-in
			 * time, if any tap-ins occurred
			 */

			ArrayList<CepasTransaction> cepasTransactions = new ArrayList<CepasTransaction>();

			public double getDwellTime() {
				return departureTime - arrivalTime;
			}

			public CepasVehicleDwellEvent(int arrivalTime, int departureTime, Id stopId) {
				super();
				this.arrivalTime = arrivalTime;
				this.departureTime = departureTime;
				this.stopId = stopId;
			}

			/**
			 * For cases where there are too few transactions to register a
			 * realistic dwell time, or where there are early tap-ins and
			 * tap-outs, or GPS errors.
			 */
			public void simpleDwellTimeAdjustment() {
				CepasTransaction lastAlighting = null;
				CepasTransaction firstBoarding = null;
				for (CepasTransaction transaction : cepasTransactions) {
					if (transaction.type.equals(CepasTransactionType.alighting))
						lastAlighting = transaction;
					else {
						if (firstBoarding == null)
							firstBoarding = transaction;
					}
				}
				if (firstBoarding != null) {
					this.arrivalTime = (int) firstBoarding.time;
				}
				if (lastAlighting != null) {
					this.departureTime = (int) lastAlighting.time;
				}
				if (departureTime <= arrivalTime) {
					departureTime = (departureTime + arrivalTime) / 2;
					arrivalTime = departureTime;
					arrivalTime -= minDwellTime / 2;
					departureTime += minDwellTime / 2;
					return;
				}
				if (getDwellTime() < minDwellTime) {
					double avgtime = (arrivalTime + departureTime) / 2;
					arrivalTime = (int) (avgtime - minDwellTime / 2);
					departureTime = (int) (avgtime + minDwellTime / 2);
					return;
				}
			}

			public void findTrueDwellTime() {
				if (getDwellTime() < minDwellTime) {
					double avgtime = (arrivalTime + departureTime) / 2;
					arrivalTime = (int) (avgtime - minDwellTime / 2);
					departureTime = (int) (avgtime + minDwellTime / 2);
					return;
				}

				// if we have only 2-4 transactions,and we're over the minimum
				// dwell time, we need to first find the median time, then find
				// the last alighting after that time
				// and set that as the departure time, and/or the first boarding
				// and
				// set that as the arrival time (only if it is less than the
				// arrival time)
				if (cepasTransactions.size() <= minTransactionClusterSize) {
					simpleDwellTimeAdjustment();
					return;
				}
				/*
				 * cluster the transactions based on the deltaTapTimeLimit
				 */
				int deltaTime = 0;
				int lastTime = (int) cepasTransactions.get(0).time;

				ArrayList<Integer> deltaTimes = new ArrayList<Integer>();
				deltaTimes.add(0);
				ArrayList<ArrayList<CepasTransaction>> transactionClusters = new ArrayList<ArrayList<CepasTransaction>>();
				for (int i = 1; i < cepasTransactions.size(); i++) {
					CepasTransaction transaction = cepasTransactions.get(i);
					deltaTime = (int) (transaction.time - lastTime);
					deltaTimes.add(deltaTime);
					lastTime = (int) transaction.time;
				}
				ArrayList<CepasTransaction> transactionCluster = new ArrayList<CepasTransaction>();
				transactionClusters.add(transactionCluster);
				for (int i = 0; i < deltaTimes.size(); i++) {
					deltaTime = deltaTimes.get(i);
					CepasTransaction transaction = cepasTransactions.get(i);
					if (deltaTime < maximumDeltaTapTimeForTransactionsBelongingToOneCluster || i == 0) {
						transactionCluster.add(transaction);
					} else {
						transactionCluster = new ArrayList<CepasTransaction>();
						transactionCluster.add(transaction);
						transactionClusters.add(transactionCluster);
					}
				}
				// find the biggest cluster
				ArrayList<CepasTransaction> targetCluster = null;
				int maxSize = 1;
				for (ArrayList<CepasTransaction> cluster : transactionClusters) {
					if (cluster.size() > maxSize) {
						targetCluster = cluster;
						maxSize = cluster.size();
					}
				}
				if (targetCluster == null) {
					// no clusters bigger than 1, run the simplified procedure;
					simpleDwellTimeAdjustment();
					return;
				} else {
					this.arrivalTime = (int) targetCluster.get(0).time;
					this.departureTime = (int) targetCluster.get(targetCluster.size() - 1).time;
					if (getDwellTime() < minDwellTime) {
						int avgtime = (arrivalTime + departureTime) / 2;
						arrivalTime = avgtime - minDwellTime / 2;
						departureTime = avgtime + minDwellTime / 2;
					}
					return;
				}
			}

			private void updateTransactionTimes() {
				Random random = MatsimRandom.getRandom();
				for (CepasTransaction transaction : cepasTransactions) {
					if (transaction.time <= arrivalTime) {
						transaction.time = arrivalTime + random.nextDouble();
					}
					if (transaction.time >= departureTime) {
						transaction.time = departureTime - random.nextDouble();
					}

				}

			}

			@Override
			public String toString() {

				return "stop: " + stopId.toString() + ", time: " + arrivalTime + " - " + departureTime + "\n";
			}

			@Override
			public int compareTo(CepasVehicleDwellEvent o) {
				return (this.arrivalTime < o.arrivalTime) ? -1 : ((this.arrivalTime == o.arrivalTime) ? 0 : 1);
			}

		}

		private Id vehicleId;
		private Id transitLineId;
		private CepasLine cepasLine;
		private CepasRoute cepasRoute;
		/**
		 * All possible routes in both directions from the transit schedule.
		 */
		private Map<Id, TransitRoute> possibleMatsimRoutes;
		private TreeSet<Id> possibleMatsimRoutesSortedBynumberOfStops;
		/**
		 * Dwell events, ordered by arrival time.
		 */
		private TreeMap<Integer, CepasVehicleDwellEvent> orderedDwellEvents = new TreeMap<Integer, CepasVehicleDwellEvent>();
		/**
		 * Created by handlePassengers method, then used to create dwell events.
		 */
		private HashMap<Id, ArrayList<CepasTransaction>> cepasTransactionsByStopId = new HashMap<Id, ArrayList<CepasTransaction>>();
		private TreeSet<Id> routesSortedByNumberOfTransactions;
		private List<CepasVehicleDwellEventCluster> dwellEventClusters;
		private ArrayList<CepasTransaction> cepasTransactions = new ArrayList<CepasToEvents.CepasTransaction>();
		private LinkedList<Event> eventQueue;
		private final double maxSpeed = 80 / 3.6;

		// Constructors
		public CepasVehicle(Id matsimTransitLineId, CepasRoute cepasRoute, Id busRegNumber) {
			this.transitLineId = matsimTransitLineId;
			this.cepasRoute = cepasRoute;
			this.cepasLine = cepasRoute.line;
			this.vehicleId = busRegNumber;
			this.eventQueue = new LinkedList<Event>();
			sortMatsimRoutesbyNumberOfStops();
		}

		/**
		 * For each bus, line and direction combo, the boarding and alighting
		 * transactions are recorded by stop id. Passengers are created for all
		 * records that have both a boarding and alighting time.
		 * 
		 * @param resultSet
		 * @throws SQLException
		 */
		public void handlePassengers(ResultSet resultSet) throws SQLException {
			CepasTransaction previoustransaction = null;
			Random random = MatsimRandom.getRandom();
			while (resultSet.next()) {
				CepasTransaction boardingTransaction = null;
				CepasTransaction alightingTransaction = null;
				CepasVehiclePassenger passenger;
				int boardingTime = resultSet.getInt("boarding_time");
				Id boardingStop = cepasStoptoMatsimStopLookup.get(resultSet.getString("boarding_stop_stn"));
				if (boardingStop == null) {
					System.err.println(this.vehicleId.toString() + " has a stop id = "
							+ resultSet.getString("boarding_stop_stn")
							+ " not appearing in matsim stops lookup table, ignoring transaction.");
					errorTracker.stopIdNotInRoute(this.vehicleId);
					continue;
				}
				Id alightingStop = cepasStoptoMatsimStopLookup.get(resultSet.getString("alighting_stop_stn"));
				if (alightingStop == null) {
					// didn't tap out, or stop is not in the schedule, skip this
					// guy{
					boardingTransaction = new CepasTransaction(null, CepasTransactionType.boarding, boardingTime
							+ random.nextDouble(), boardingStop);
					this.cepasTransactions.add(boardingTransaction);
					errorTracker.transactionCount(this.vehicleId);
					continue;
				}
				int alightingTime = resultSet.getInt("alighting_time");
				Id personId = new IdImpl(resultSet.getLong("card_id"));
				passenger = new CepasVehiclePassenger(personId, boardingStop, alightingStop, boardingTime,
						alightingTime);

				boardingTransaction = new CepasTransaction(passenger, CepasTransactionType.boarding, boardingTime
						+ random.nextDouble(), boardingStop);
				this.cepasTransactions.add(boardingTransaction);
				errorTracker.transactionCount(this.vehicleId);

				alightingTransaction = new CepasTransaction(passenger, CepasTransactionType.alighting, alightingTime
						+ random.nextDouble(), alightingStop);
				this.cepasTransactions.add(alightingTransaction);
				errorTracker.transactionCount(this.vehicleId);

			}
		}

		public void createDwellEventsFromTransactions() {
			for (Id stopId : this.cepasTransactionsByStopId.keySet()) {
				ArrayList<CepasTransaction> transactions = this.cepasTransactionsByStopId.get(stopId);
				Collections.sort(transactions);
				int deltaTime = 0;
				int lastTime = 0;
				ArrayList<Integer> deltaTimes = new ArrayList<Integer>();
				for (int i = 0; i < transactions.size(); i++) {
					CepasTransaction transaction = transactions.get(i);
					deltaTime = (int) (transaction.time - lastTime);
					deltaTimes.add(deltaTime);
					lastTime = (int) transaction.time;
				}
				ArrayList<CepasVehicleDwellEvent> dwellEvents = new ArrayList<CepasVehicleDwellEvent>();
				CepasVehicleDwellEvent dwellEvent = null;
				for (int i = 0; i < deltaTimes.size(); i++) {
					deltaTime = deltaTimes.get(i);
					CepasTransaction transaction = transactions.get(i);
					if (deltaTime > interDwellEventTimeForTheSameStopTimeLimit || i == 0) {
						if (dwellEvent != null) {
							dwellEvents.add(dwellEvent);
							dwellEvent.findTrueDwellTime();
						}
						dwellEvent = new CepasVehicleDwellEvent((int) transaction.time, (int) transaction.time, stopId);
					} else {
						dwellEvent.departureTime = (int) transaction.time;
					}
					dwellEvent.cepasTransactions.add(transaction);
					// if
					// (transaction.type.equals(CepasTransactionType.boarding))
					// {
					// } else {
					// if (transaction.passenger != null) {
					// }
					// }
				}
				// add the last dwell event
				dwellEvents.add(dwellEvent);
				dwellEvent.findTrueDwellTime();
				for (CepasVehicleDwellEvent stopEvent1 : dwellEvents) {
					this.orderedDwellEvents.put(stopEvent1.arrivalTime, stopEvent1);
				}
				errorTracker.dwellEventsCount(vehicleId, orderedDwellEvents.size());

			}
			// System.out.println(this.printStopsVisited());
		}

		/**
		 * Goes through the list of ordered dwell events, and checks if stops
		 * are visited in order. If dwell event n is at a stop out of the route
		 * order, it first checks if the dwell event n+1 is in order, in which
		 * case the bad dwell event n is erased.
		 * <P>
		 * If dwell event n+1 also visits a stop out of order, it then resets
		 * the iterator for stops, and creates a new cluster of dwell events.
		 */
		public void clusterDwellEventsIntoRoutes() {
			Id likeliestRoute = this.routesSortedByNumberOfTransactions.last();
			LinkedList<Id> stopIds = new LinkedList<Id>();
			stopIds.addAll(routeIdToStopIdSequence.get(likeliestRoute));
			boolean circleRoute = stopIds.getFirst().equals(stopIds.getLast());
			ArrayList<CepasVehicleDwellEvent> dwellEventsList = new ArrayList<CepasVehicleDwellEvent>();
			dwellEventsList.addAll(orderedDwellEvents.values());
			ArrayList<Integer> dwellEventsAsStopIndexList = new ArrayList<Integer>();
			for (CepasVehicleDwellEvent de : dwellEventsList) {
				dwellEventsAsStopIndexList.add(stopIds.indexOf(de.stopId));
			}
			LinkedList<CepasVehicleDwellEventCluster> clusters = new LinkedList<CepasVehicleDwellEventCluster>();
			List<CepasVehicleDwellEvent> currentCluster = new ArrayList<CepasVehicleDwellEvent>();
			currentCluster.add(dwellEventsList.get(0));
			boolean success = true;
			for (int i = 1; i < dwellEventsAsStopIndexList.size(); i++) {
				// if (dwellEventsAsStopIndexList.get(i) == 0
				// && dwellEventsAsStopIndexList.get(i) ==
				// dwellEventsAsStopIndexList.get(i - 1) && circleRoute) {
				// // exception where the travel time to the last stop in the
				// // route was longer, maybe due to skipped stops, and now we
				// // have a repeat dwellevent at the same stop
				// clusters.getLast().getOrderedDwellEvents()
				// .put(dwellEventsList.get(i - 1).arrivalTime,
				// dwellEventsList.get(i - 1));
				// currentCluster = new ArrayList<CepasVehicleDwellEvent>();
				// currentCluster.add(dwellEventsList.get(i));
				// continue;
				// }
				if (dwellEventsAsStopIndexList.get(i) < dwellEventsAsStopIndexList.get(i - 1)) {
					if (i < dwellEventsAsStopIndexList.size() - 1) {

						if (circleRoute && dwellEventsAsStopIndexList.get(i) == 0) {
							if (getInterDwellEventSpeed(dwellEventsList.get(i - 1), dwellEventsList.get(i)) > getInterDwellEventSpeed(
									dwellEventsList.get(i), dwellEventsList.get(i + 1))) {
								// the dwell event is closer to the previous
								// one
								// than to the next
								// this is actually an end stop; cluster it
								dwellEventsAsStopIndexList.set(i, stopIds.size() - 1);
								currentCluster.add(dwellEventsList.get(i));
							} else {
								// start a new cluster
								clusters.add(new CepasVehicleDwellEventCluster(currentCluster));
								currentCluster = new ArrayList<CepasVehicleDwellEvent>();
								currentCluster.add(dwellEventsList.get(i));
							}
						} else if (dwellEventsAsStopIndexList.get(i + 1) > dwellEventsAsStopIndexList.get(i - 1)) {
							// The next dwell event is in sequence, so this must
							// be an error
							orderedDwellEvents.remove(dwellEventsList.get(i).arrivalTime);
							errorTracker.dwellEventsDropped(this.vehicleId, dwellEventsList.get(i));
							success = false;
							break;
						} else {
							// this is a new cluster that doesn't start from the
							// first stop
							clusters.add(new CepasVehicleDwellEventCluster(currentCluster));
							currentCluster = new ArrayList<CepasVehicleDwellEvent>();
							currentCluster.add(dwellEventsList.get(i));
						}
					} else {
						if (circleRoute && dwellEventsAsStopIndexList.get(i) == 0) {
							// dealing with the last dwell event
							currentCluster.add(dwellEventsList.get(i));
						}
					}
				} else if (dwellEventsAsStopIndexList.get(i) == dwellEventsAsStopIndexList.get(i - 1)) {
					// merge two consecutive dwellevents at the same stop into
					// the first
					dwellEventsList.get(i - 1).departureTime = dwellEventsList.get(i).departureTime;
					dwellEventsList.get(i - 1).cepasTransactions.addAll(dwellEventsList.get(i).cepasTransactions);
					orderedDwellEvents.remove(dwellEventsList.get(i).arrivalTime);
					errorTracker.dwellEventsDropped(vehicleId, dwellEventsList.get(i));
					dwellEventsList.get(i - 1).findTrueDwellTime();
					success = false;
					break;
				} else {

					// part of the same cluster
					currentCluster.add(dwellEventsList.get(i));
				}
			}
			// add the last cluster
			if (success) {
				if (currentCluster.size() > 1) {
					clusters.add(new CepasVehicleDwellEventCluster(currentCluster));
				}
				this.dwellEventClusters = clusters;

			} else {
				clusterDwellEventsIntoRoutes();
			}
			// Id firstStopId = null;
			// Id secondStopId = null;
			// CepasVehicleDwellEvent firstDwellEvent = null;
			// CepasVehicleDwellEvent secondDwellEvent = null;
			// int dwellEvtIdx = -1;
			// int stopIdIdx = 0;
			// Iterator<Id> stopIdIterator = stopIds.iterator();
			// while (dwellEvtIdx < orderedDwellEvents.size() - 1) {
			// dwellEvtIdx++;
			// if (stopIdIterator.hasNext()) {
			// firstStopId = stopIdIterator.next();
			// }
			// firstStopId = stopIdIterator.next();
			// firstDwellEvent = dwellEventsList.get(dwellEvtIdx);
			// secondDwellEvent = dwellEventsList.get(dwellEvtIdx + 1);
			// if (firstStopId.equals(firstDwellEvent.stopId)) {
			// dwellEvents.put(firstDwellEvent.arrivalTime, firstDwellEvent);
			// } else {
			// }
			// }

		}

		public boolean dropDwellEventsNotInRoute() {
			Set<Id> stopIdsInRoute = new HashSet<Id>();
			stopIdsInRoute.addAll(routeIdToStopIdSequence.get(routesSortedByNumberOfTransactions.last()));
			List<Integer> dwellEventsToRemove = new ArrayList<Integer>();
			for (int i : orderedDwellEvents.keySet()) {
				if (!stopIdsInRoute.contains(orderedDwellEvents.get(i).stopId)) {
					dwellEventsToRemove.add(i);
				}
			}
			for (int j : dwellEventsToRemove) {
				System.err.printf("removing dwell event : %s", orderedDwellEvents.get(j).toString());
				errorTracker.dwellEventsDropped(vehicleId, orderedDwellEvents.get(j));
				orderedDwellEvents.remove(j);
			}
			if (orderedDwellEvents.size() < minimumNumberOfDwellEventsForProcessing)
				return false;
			else
				return true;
		}

		// Methods
		public String printClusters() {
			StringBuffer sb = new StringBuffer("line\tdirection\tbus_reg_num\tcluster\tstop_id\ttime\ttype\tspeed\n");
			for (CepasVehicleDwellEventCluster dec : this.dwellEventClusters) {
				try {
					CepasVehicleDwellEvent prevStop = dec.getOrderedDwellEvents().firstEntry().getValue();
					for (Entry<Integer, CepasVehicleDwellEvent> entry : dec.getOrderedDwellEvents().entrySet()) {
						CepasVehicleDwellEvent stopEvent = entry.getValue();
						sb.append(String.format(
								"%s\t%s\t%s\t%s\t%s\t%06d\tarrival\t%f\n%s\t%s\t%s\t%s\t%s\t%06d\tdeparture\t%f\n",
								this.cepasLine.lineId.toString(), this.cepasRoute.direction, this.vehicleId.toString(),
								this.dwellEventClusters.indexOf(dec), stopEvent.stopId, stopEvent.arrivalTime,
								getInterDwellEventSpeed(prevStop, stopEvent), this.cepasLine.lineId.toString(),
								this.cepasRoute.direction, this.vehicleId.toString(),
								this.dwellEventClusters.indexOf(dec), stopEvent.stopId, stopEvent.departureTime, 0.0));
						prevStop = stopEvent;
					}

				} catch (NullPointerException ne) {
					sb.append(String.format(
							"%s\t%s\t%s\t%s\t%s\t%06d\tarrival\t%f\n%s\t%s\t%s\t%s\t%06d\tdeparture\t%f\n",
							this.cepasLine.lineId.toString(), this.cepasRoute.direction, this.vehicleId.toString(),
							this.dwellEventClusters.indexOf(dec), "NO DATA", -1, -1.0,
							this.cepasLine.lineId.toString(), this.cepasRoute.direction, this.vehicleId.toString(),
							"NO DATA", -1, -1.0));
				}

			}
			return sb.toString();
		}

		public String printStopsVisited() {
			StringBuffer sb = new StringBuffer("line\tdirection\tbus_reg_num\tstop_id\ttime\ttype\tspeed\n");

			try {
				CepasVehicleDwellEvent prevStopEvent = orderedDwellEvents.firstEntry().getValue();
				for (Entry<Integer, CepasVehicleDwellEvent> entry : orderedDwellEvents.entrySet()) {
					CepasVehicleDwellEvent stopEvent = entry.getValue();
					sb.append(String.format("%s\t%s\t%s\t%s\t%06d\tarrival\t%f\n%s\t%s\t%s\t%s\t%06d\tdeparture\t%f\n",
							this.cepasLine.lineId.toString(), this.cepasRoute.direction, this.vehicleId.toString(),
							stopEvent.stopId, stopEvent.arrivalTime, getInterDwellEventSpeed(prevStopEvent, stopEvent),
							this.cepasLine.lineId.toString(), this.cepasRoute.direction, this.vehicleId.toString(),
							stopEvent.stopId, stopEvent.departureTime, 0.0));
					prevStopEvent = stopEvent;
				}

			} catch (NullPointerException ne) {
				// sb.append(String.format("%s\t%s\t%s\t%s\t%06d\tarrival\t%f\n%s\t%s\t%s\t%06d\tdeparture\t%f\n",
				// this.cepasLine.lineId.toString(), this.cepasRoute.direction,
				// this.vehicleId.toString(),
				// "NO DATA", -1, -1.0, this.cepasLine.lineId.toString(),
				// this.cepasRoute.direction,
				// this.vehicleId.toString(), "NO DATA", -1, -1.0));
			}

			return sb.toString();
		}

		/**
		 * orders the possible routes for this vehicle in ascending number of
		 * stops.
		 */
		private void sortMatsimRoutesbyNumberOfStops() {
			try {
				possibleMatsimRoutes = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes();
			} catch (NullPointerException ne) {
				// this route doesn't exist in the transit schedule
				// TODO write an exception handler for this case,
				// for now, just ignore these and report their number of events
				System.out.println("line " + transitLineId.toString() + " does not exist in transit schedule.");
				return;
			}
			HashMap<Id, Integer> unsortedRouteSizes = new HashMap<Id, Integer>();
			for (Id transitRouteId : possibleMatsimRoutes.keySet()) {
				unsortedRouteSizes.put(transitRouteId, possibleMatsimRoutes.get(transitRouteId).getStops().size());
			}
			this.possibleMatsimRoutesSortedBynumberOfStops = new TreeSet<Id>(new ValueComparator(unsortedRouteSizes));
			this.possibleMatsimRoutesSortedBynumberOfStops.addAll(unsortedRouteSizes.keySet());
		}

		/**
		 * Orders the possible Matsim routes for this line and direction by the
		 * number of transactions associated with the stops in the route
		 */
		private void assignRouteScoresByNumberOfTransactions() {
			HashMap<Id, Integer> correlationCount = new HashMap<Id, Integer>();
			for (Id routeId : possibleMatsimRoutesSortedBynumberOfStops) {
				ArrayList<Id> stopList = routeIdToStopIdSequence.get(routeId);
				int score = 0;
				for (CepasTransaction transaction : cepasTransactions) {
					if (stopList.contains(transaction.stopId))
						score++;
				}
				correlationCount.put(routeId, score);
			}
			routesSortedByNumberOfTransactions = new TreeSet<Id>(new ValueComparator(correlationCount));
			routesSortedByNumberOfTransactions.addAll(correlationCount.keySet());
		}

		/**
		 * assume all stops belong to the longest route. interpolate stops not
		 * visited
		 */

		private double getInterDwellEventSpeed(CepasVehicleDwellEvent previousDwellEvent,
				CepasVehicleDwellEvent nextDwellEvent) {
			double distance = getInterStopDistance(previousDwellEvent.stopId, nextDwellEvent.stopId);
			double time = nextDwellEvent.arrivalTime - previousDwellEvent.departureTime;
			double speed = distance / time * 3.6;
			if (speed < 0) {
				return -1;
			} else {

				return Math.min(speed, 1000);
			}
		}

		private double getInterStopSpeed(Id fromId, Id toId, double deltaTime) {
			double distance = getInterStopDistance(fromId, toId);
			double speed = distance / deltaTime * 3.6;
			if (speed < 0) {
				return -1;
			} else {

				return Math.min(speed, 1000);
			}
		}

		private double getInterStopDistance(Id fromStopId, Id toStopId) {
			Id likeliestRoute = this.routesSortedByNumberOfTransactions.last();
			List<TransitRouteStop> stops = this.possibleMatsimRoutes.get(likeliestRoute).getStops();
			Link fromLink = null;
			Link toLink = null;
			for (TransitRouteStop tss : stops) {
				if (tss.getStopFacility().getId().equals(fromStopId))
					fromLink = scenario.getNetwork().getLinks().get(tss.getStopFacility().getLinkId());
				if (tss.getStopFacility().getId().equals(toStopId))
					toLink = scenario.getNetwork().getLinks().get(tss.getStopFacility().getLinkId());
			}
			if (fromLink == null || toLink == null)
				return Double.POSITIVE_INFINITY;
			else {
				NetworkRoute route = scenario.getTransitSchedule().getTransitLines().get(this.transitLineId)
						.getRoutes().get(likeliestRoute).getRoute();
				try {
					NetworkRoute subRoute = route.getSubRoute(fromLink.getId(), toLink.getId());
					return RouteUtils.calcDistance(subRoute, scenario.getNetwork()) + toLink.getLength();

				} catch (IllegalArgumentException e) {
					double distance = RouteUtils.calcDistance(
							route.getSubRoute(fromLink.getId(), route.getEndLinkId()), scenario.getNetwork())
							+ scenario.getNetwork().getLinks().get(route.getEndLinkId()).getLength();
					return distance
							+ RouteUtils.calcDistance(route.getSubRoute(route.getStartLinkId(), toLink.getId()),
									scenario.getNetwork()) + toLink.getLength();

				}

			}

		}

		private void dwellEventOverlapCheck() {
			for (CepasVehicleDwellEventCluster cluster : dwellEventClusters) {
				Id likeliestRoute = cluster.getRouteId();
				List<TransitRouteStop> stops = this.possibleMatsimRoutes.get(likeliestRoute).getStops();
				Set<Integer> removalTimes = new HashSet<Integer>();
				CepasVehicleDwellEvent fromDwellEvent = null;
				boolean success = true;
				for (CepasVehicleDwellEvent dwellEvent : cluster.getOrderedDwellEvents().values()) {
					if (dwellEvent.arrivalTime == cluster.getOrderedDwellEvents().firstKey()) {
						fromDwellEvent = dwellEvent;
						continue;
					}
					if (fromDwellEvent.departureTime >= dwellEvent.arrivalTime) {
						removalTimes.add(fromDwellEvent.arrivalTime);
						success = false;
					}
					Link fromLink = null;
					Link toLink = null;
					for (TransitRouteStop tss : stops) {
						Id fromStopId = fromDwellEvent.stopId;
						Id toStopId = dwellEvent.stopId;
						if (tss.getStopFacility().getId().equals(fromStopId))
							fromLink = scenario.getNetwork().getLinks().get(tss.getStopFacility().getLinkId());
						if (tss.getStopFacility().getId().equals(toStopId))
							toLink = scenario.getNetwork().getLinks().get(tss.getStopFacility().getLinkId());
					}
					NetworkRoute route = scenario.getTransitSchedule().getTransitLines().get(this.transitLineId)
							.getRoutes().get(likeliestRoute).getRoute();
					try {
						route.getSubRoute(fromLink.getId(), toLink.getId());
					} catch (IllegalArgumentException e) {
						removalTimes.add(fromDwellEvent.arrivalTime);
						success = false;
					}
					fromDwellEvent = dwellEvent;
				}
				for (int i : removalTimes) {
					System.err.printf("removing dwell event : %s", cluster.orderedDwellEvents.get(i).toString());
					errorTracker.dwellEventsDropped(vehicleId, cluster.orderedDwellEvents.get(i));
					cluster.orderedDwellEvents.remove(i);
				}
				if (!success)
					dwellEventOverlapCheck();
			}

		}

		@Override
		public String toString() {
			String out = String.format("line %s, bus reg %s", this.transitLineId.toString(), this.vehicleId.toString());
			return out;
		}

		public void correctGPSErrors() {
			int errorCount = 0;
			Collections.sort(this.cepasTransactions);
			int deltaTime = 0;
			int lastTime = 0;
			ArrayList<Integer> deltaTimes = new ArrayList<Integer>();
			for (int i = 0; i < cepasTransactions.size(); i++) {
				CepasTransaction transaction = cepasTransactions.get(i);
				deltaTime = (int) (transaction.time - lastTime);
				deltaTimes.add(deltaTime);
				lastTime = (int) transaction.time;
			}
			Id stopId = null;
			Id prevStopId = null;
			CepasTransaction referenceTransaction = null;
			CepasTransaction suspectTransaction = null;
			ArrayList<CepasTransaction> localCluster = null;
			for (int i = 0; i < deltaTimes.size(); i++) {
				deltaTime = deltaTimes.get(i);
				CepasTransaction transaction = cepasTransactions.get(i);
				if (deltaTime > minimumTravelTimeBetweenConsecutiveStops || i == 0) {
					if (i > 0) {
						prevStopId = stopId;
					}
					stopId = transaction.stopId;
					referenceTransaction = transaction;
					localCluster = new ArrayList<CepasToEvents.CepasTransaction>();
					localCluster.add(referenceTransaction);
				} else {
					if (transaction.stopId.equals(stopId)) {
						localCluster.add(transaction);
					} else {
						// TODO: debug
						errorCount++;
						if (errorCount > 150)
							return;
						suspectTransaction = transaction;
						System.err.println("Found a suspect for " + this.vehicleId + " at time "
								+ suspectTransaction.time);
						// if (localCluster.size() < minTransactionClusterSize)
						// {
						// fill the localCluster up until you reach the
						// deltaTapTimeLimit
						int j = i + 1;
						while (j < deltaTimes.size() && deltaTimes.get(j) < minimumTravelTimeBetweenConsecutiveStops) {
							localCluster.add(cepasTransactions.get(j));
							j++;
						}
						// }
						// iterate through the local cluster and see what is
						// the majority opinion
						int incumbentStopIdScore = 0;
						int candidateStopIdScore = 1;
						for (CepasTransaction lcTransaction : localCluster) {
							if (lcTransaction.stopId.equals(stopId))
								incumbentStopIdScore++;
							else if (lcTransaction.stopId.equals(suspectTransaction.stopId))
								candidateStopIdScore++;
							else
								System.err.println("Trouble classifying seemingly erroneous transactions for "
										+ this.vehicleId + " at time " + suspectTransaction.time);
						}
						if (incumbentStopIdScore >= candidateStopIdScore) {
							try {
								getInterStopDistance(stopId, prevStopId);
								// if we get this far, the stops are in
								// order, so maybe an early transaction for the
								// following stop.
								stopId = transaction.stopId;
								referenceTransaction = transaction;
								localCluster = new ArrayList<CepasToEvents.CepasTransaction>();
								localCluster.add(referenceTransaction);
							} catch (Exception e) {
								transaction.stopId = stopId;
							}
						} else {
							// the reference transaction is the culprit, change
							// everything in the local cluster to the new stop
							// id
							for (CepasTransaction lcTransaction : localCluster) {
								lcTransaction.stopId = transaction.stopId;
							}
							localCluster = new ArrayList<CepasToEvents.CepasTransaction>();
							stopId = transaction.stopId;
						}

					}
				}
			}

		}

		private ArrayList<Integer> getTransactionDeltaTimes() {
			Collections.sort(this.cepasTransactions);
			int deltaTime = 0;
			int lastTime = 0;
			ArrayList<Integer> deltaTimes = new ArrayList<Integer>();
			for (int i = 0; i < cepasTransactions.size(); i++) {
				CepasTransaction transaction = cepasTransactions.get(i);
				deltaTime = (int) (transaction.time - lastTime);
				deltaTimes.add(deltaTime);
				lastTime = (int) transaction.time;
			}
			return deltaTimes;
		}

		public void correctGPSErrorsV2() {
			int errorCount = 0;
			int deltaTime = 0;

			ArrayList<Integer> deltaTimes = getTransactionDeltaTimes();
			Id stopId = null;
			Id prevStopId = null;
			CepasTransaction referenceTransaction = null;
			CepasTransaction previousViableTransaction = null;

			for (int i = 0; i < deltaTimes.size() - 1; i++) {
				deltaTime = deltaTimes.get(i);
				CepasTransaction transaction = cepasTransactions.get(i);
				if (i == 0) {
					stopId = transaction.stopId;
					referenceTransaction = transaction;
					continue;
				}
				if (!transaction.stopId.equals(stopId)) {
					if (transaction.speedToNext > 80) {
						// too fast, check for next viable speed
						// int j = i;
						// CepasTransaction nextViableTransaction = null;
						// CepasTransaction afterNextViableTransaction = null;
						// boolean foundTwo = false;
						//
						// while (j < deltaTimes.size() - 1 && !foundTwo) {
						// if (cepasTransactions.get(j).speedToNext > 0) {
						// if (nextViableTransaction == null){
						// nextViableTransaction = cepasTransactions.get(j);
						// }
						// else{
						// afterNextViableTransaction =
						// cepasTransactions.get(j);
						// foundTwo=true;
						// }
						// }
						// }
						// if(nextViableTransaction == null){
						// just drop this transaction and see what happens
						CepasTransaction prev = cepasTransactions.get(i - 1);
						CepasTransaction next = cepasTransactions.get(i + 1);
						prev.speedToNext = getInterStopSpeed(prev.stopId, next.stopId, next.time - prev.time);
						next.speedFromPrevious = prev.speedToNext;
						cepasTransactions.remove(i);
						deltaTimes = getTransactionDeltaTimes();
						// start again
						i = -1;
						errorCount++;
						errorTracker.fastTransactionDropped(this.vehicleId);
						continue;
					}
					// if(foundTwo){
					//
					// }else{
					// if(nextViableTransaction.speedToNext > 50 &&
					// nextViableTransaction.equals(cepasTransactions.get(i))){
					// CepasTransaction prev = cepasTransactions.get(i-1);
					// CepasTransaction next = cepasTransactions.get(i+1);
					// prev.speedToNext = getInterStopSpeed(prev.stopId,
					// next.stopId, next.time-prev.time);
					// next.speedFromPrevious = prev.speedToNext;
					// cepasTransactions.remove(i);
					// deltaTimes =getTransactionDeltaTimes();
					// //start again
					// i=-1;
					// continue;
					// }
					// if(nextViableTransaction.speedToNext < 50){
					// if(getInterStopSpeed(referenceTransaction.stopId,
					// nextViableTransaction.stopId,
					// nextViableTransaction.time-referenceTransaction.time)<50){
					// CepasTransaction prev = cepasTransactions.get(i-1);
					// CepasTransaction next = cepasTransactions.get(i+1);
					// prev.speedToNext = getInterStopSpeed(prev.stopId,
					// next.stopId, next.time-prev.time);
					// next.speedFromPrevious = prev.speedToNext;
					// cepasTransactions.remove(i);
					// deltaTimes =getTransactionDeltaTimes();
					// //start again
					// i=-1;
					// continue;
					// }
					// }else{
					// i = cepasTransactions.indexOf(referenceTransaction);
					// CepasTransaction prev = cepasTransactions.get(i-1);
					// CepasTransaction next = cepasTransactions.get(i+1);
					// prev.speedToNext = getInterStopSpeed(prev.stopId,
					// next.stopId, next.time-prev.time);
					// next.speedFromPrevious = prev.speedToNext;
					// cepasTransactions.remove(i);
					// deltaTimes =getTransactionDeltaTimes();
					// //start again
					// i=-1;
					// continue;
					// }
					// }
					//
					//
					// }

				} else {
					referenceTransaction = transaction;
				}
			}
			System.err.printf("removed %d transactions in the speedfilter\n", errorCount);
		}

		public void sortCepasTransactionsByStopId() {
			for (CepasTransaction transaction : this.cepasTransactions) {
				try {
					this.cepasTransactionsByStopId.get(transaction.stopId).add(transaction);
				} catch (NullPointerException ne) {
					this.cepasTransactionsByStopId.put(transaction.stopId, new ArrayList<CepasTransaction>());
					this.cepasTransactionsByStopId.get(transaction.stopId).add(transaction);
				}
			}

		}

		public String printTransactions() {
			StringBuffer sb = new StringBuffer(
					"veh_id\tstop_id\ttransaction_type\ttime\tspeedFromPrevious\tspeedToNext\n");
			for (CepasTransaction transaction : this.cepasTransactions) {
				sb.append(this.vehicleId.toString() + "\t" + transaction.toString() + "\n");
			}

			return sb.toString();
		}

		public void assignRoutesToDwellEventClusters() {
			for (CepasVehicleDwellEventCluster cluster : dwellEventClusters) {
				Id assignedRoute = null;
				int maxScore = 0;
				for (Id routeId : possibleMatsimRoutesSortedBynumberOfStops) {
					ArrayList<Id> stopList = routeIdToStopIdSequence.get(routeId);
					int score = 0;
					for (CepasVehicleDwellEvent de : cluster.getOrderedDwellEvents().values()) {
						if (stopList.contains(de.stopId))
							score++;
					}
					if (score > maxScore) {
						assignedRoute = routeId;
						maxScore = score;
					}
				}
				cluster.setRouteId(assignedRoute);
			}
		}

		public void interpolateDwellEvents() {
			for (CepasVehicleDwellEventCluster cluster : this.dwellEventClusters) {
				cluster.interpolateDwellEvents();
			}

		}

		public void updatetransactionTimes() {
			for (CepasVehicleDwellEvent dwellEvent : this.orderedDwellEvents.values()) {
				dwellEvent.updateTransactionTimes();
			}

		}

		public void fireEvents() {
			IdImpl driverId = new IdImpl("pt_tr_" + this.vehicleId.toString());
			Id busRegNum = vehicleId;
			Map<Id<Link>, ? extends Link> links = scenario.getNetwork().getLinks();
			for (CepasVehicleDwellEventCluster cluster : this.dwellEventClusters) {

				NetworkRoute route = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes()
						.get(cluster.getRouteId()).getRoute();
				List<TransitRouteStop> stops = scenario.getTransitSchedule().getTransitLines().get(transitLineId)
						.getRoutes().get(cluster.getRouteId()).getStops();
				TransitRouteStop firstStop = stops.get(0);
				Id departureLinkId = null;
				for (TransitRouteStop tss : stops) {
					if (tss.getStopFacility().getId().equals(cluster.orderedDwellEvents.firstEntry().getValue().stopId))
						departureLinkId = tss.getStopFacility().getLinkId();
				}
				Event driverStarts = new TransitDriverStartsEvent(cluster.orderedDwellEvents.firstKey() - 0.004,
						driverId, busRegNum, this.transitLineId, cluster.routeId, new IdImpl(departureId++));
				Event transitDriverDeparture = new PersonDepartureEvent(
						(double) cluster.orderedDwellEvents.firstKey() - 0.003, driverId, departureLinkId,
						TransportMode.car);
				Event personEntersVehicle = new PersonEntersVehicleEvent(
						(double) cluster.orderedDwellEvents.firstKey() - 0.002, driverId, busRegNum);
				Event wait2Link = new Wait2LinkEvent((double) cluster.orderedDwellEvents.firstKey() - 0.001, driverId,
						departureLinkId, busRegNum);
				this.eventQueue.addLast(driverStarts);
				this.eventQueue.addLast(transitDriverDeparture);
				this.eventQueue.addLast(personEntersVehicle);
				this.eventQueue.addLast(wait2Link);
				CepasVehicleDwellEvent lastDwellEvent = null;
				Event vehArrival = null;
				Event vehDeparture = null;
				int nullPassengerCounter = 0;
				Id fromLinkId = departureLinkId;
				for (CepasVehicleDwellEvent dwellEvent : cluster.getOrderedDwellEvents().values()) {
					if (dwellEvent.arrivalTime != cluster.getOrderedDwellEvents().firstKey()) {
						Id toLinkId = null;
						for (TransitRouteStop tss : stops) {
							if (tss.getStopFacility().getId().equals(lastDwellEvent.stopId))
								fromLinkId = tss.getStopFacility().getLinkId();
							if (tss.getStopFacility().getId().equals(dwellEvent.stopId))
								toLinkId = tss.getStopFacility().getLinkId();
						}
						NetworkRoute subRoute = route.getSubRoute(fromLinkId, toLinkId);
						LinkedList<Double> linkTravelTimes = new LinkedList<Double>();
						double totalExpectedtravelTime = 0;
						for (Id linkId : subRoute.getLinkIds()) {
							Link link = links.get(linkId);
							linkTravelTimes.add(link.getLength() / Math.min(link.getFreespeed(), maxSpeed));
							totalExpectedtravelTime += linkTravelTimes.getLast();
						}
						Link toLink = links.get(toLinkId);
						totalExpectedtravelTime += toLink.getLength() / Math.min(toLink.getFreespeed(), maxSpeed);

						double availableTime = dwellEvent.arrivalTime - lastDwellEvent.departureTime;
						double lastTime = lastDwellEvent.departureTime;
						Event linkLeave = new LinkLeaveEvent(lastTime += 0.001, driverId, fromLinkId, busRegNum);
						Event linkEnter = null;

						this.eventQueue.addLast(linkLeave);
						List<Id<Link>> linkIds = subRoute.getLinkIds();
						for (int i = 0; i < linkIds.size(); i++) {
							linkEnter = new LinkEnterEvent(lastTime += 0.001, driverId, linkIds.get(i), busRegNum);
							linkLeave = new LinkLeaveEvent(
									lastTime += (availableTime * linkTravelTimes.get(i) / totalExpectedtravelTime),
									driverId, linkIds.get(i), busRegNum);
							this.eventQueue.addLast(linkEnter);
							this.eventQueue.addLast(linkLeave);
						}
						linkEnter = new LinkEnterEvent(lastTime += 0.001, driverId, toLinkId, busRegNum);
						this.eventQueue.addLast(linkEnter);
					}
					vehArrival = new VehicleArrivesAtFacilityEvent(dwellEvent.arrivalTime, busRegNum,
							dwellEvent.stopId, 0.0);
					this.eventQueue.addLast(vehArrival);
					for (CepasTransaction transaction : dwellEvent.cepasTransactions) {
						Event transactionEvent = null;
						if (transaction.type.equals(CepasTransactionType.boarding)) {
							this.eventQueue.addLast(new PersonEntersVehicleEvent(transaction.time,
									transaction.passenger == null ? new IdImpl(vehicleId.toString()
											+ nullPassengerCounter) : transaction.passenger.personId, vehicleId));
						} else {
							this.eventQueue.addLast(new PersonLeavesVehicleEvent(transaction.time,
									transaction.passenger == null ? new IdImpl(vehicleId.toString()
											+ nullPassengerCounter) : transaction.passenger.personId, vehicleId));
						}
					}
					vehDeparture = new VehicleDepartsAtFacilityEvent(dwellEvent.departureTime, busRegNum,
							dwellEvent.stopId, 0.0);
					this.eventQueue.addLast(vehDeparture);
					lastDwellEvent = dwellEvent;
				}
				Id arrivalLinkId = null;
				for (TransitRouteStop tss : stops) {
					if (tss.getStopFacility().getId().equals(cluster.orderedDwellEvents.lastEntry().getValue().stopId))
						arrivalLinkId = tss.getStopFacility().getLinkId();
				}
				Event personLeavesvehicle = new PersonLeavesVehicleEvent(lastDwellEvent.departureTime + 0.001,
						driverId, busRegNum);
				Event transitDriverArrival = new PersonArrivalEvent(lastDwellEvent.departureTime + 0.002, driverId,
						arrivalLinkId, TransportMode.car);
				eventQueue.addLast(personLeavesvehicle);
				eventQueue.addLast(transitDriverArrival);
			}

		}

		public void processEventsQueue(EventsManager eventsManager) {
			for (Event event : eventQueue) {
				eventsManager.processEvent(event);
			}
		}

		public void assignTransactionSpeeds() {
			Collections.sort(this.cepasTransactions);
			CepasTransaction previoustransaction = null;
			double lastSpeed = 0.0;
			for (CepasTransaction transaction : this.cepasTransactions) {
				if (previoustransaction == null) {
					transaction.speedFromPrevious = 0.0;
					previoustransaction = transaction;
					continue;
				}
				if (transaction.stopId.equals(previoustransaction.stopId)) {
					previoustransaction.speedToNext = 0;
					transaction.speedFromPrevious = 0;
				} else {

					double speed = getInterStopSpeed(previoustransaction.stopId, transaction.stopId, transaction.time
							- previoustransaction.time);
					previoustransaction.speedToNext = speed;
					transaction.speedFromPrevious = speed;
				}
				previoustransaction = transaction;
			}
		}

		public void removeTransactionsNotInMostPopularRoute() {
			Id likeliestRoute = this.routesSortedByNumberOfTransactions.last();
			LinkedList<Id> stopIds = new LinkedList<Id>();
			stopIds.addAll(routeIdToStopIdSequence.get(likeliestRoute));
			for (int i = 0; i < this.cepasTransactions.size(); i++) {
				if (!stopIds.contains(cepasTransactions.get(i).stopId)) {
					cepasTransactions.remove(i);
					errorTracker.stopIdNotInRoute(vehicleId);
					i = -1;
				}
			}

		}
	}

	private enum CepasTransactionType {
		boarding, alighting
	}

	private class CepasTransaction implements Comparable<CepasTransaction> {
		public double speedToNext;
		public double speedFromPrevious;
		CepasVehiclePassenger passenger;
		Id stopId;

		public CepasTransaction(CepasVehiclePassenger passenger, CepasTransactionType type, double time, Id stopId) {
			super();
			this.passenger = passenger;
			this.stopId = stopId;
			this.type = type;
			this.time = time;
		}

		CepasTransactionType type;
		double time;

		@Override
		public int compareTo(CepasTransaction o) {

			return ((Double) this.time).compareTo(o.time);
		}

		/**
		 * @return stopId, typeOfTransaction, time
		 */
		@Override
		public String toString() {

			return String.format("\"%s\"\t%s\t%.5f\t%.0f\t%.0f", this.stopId.toString(), this.type.toString(),
					this.time, this.speedFromPrevious, this.speedToNext);
		}
	}

	private class CepasVehiclePassenger implements Comparable<CepasVehiclePassenger> {
		Id personId;

		public CepasVehiclePassenger(Id personId, Id boardingStopId, Id alightingStopId, int boardingTime,
				int alightingTime) {
			super();
			this.personId = personId;
		}

		@Override
		public int compareTo(CepasVehiclePassenger o) {

			return this.personId.compareTo(o.personId);
		}
	}

	// when looking at transactions for each stop, the time required
	// between two consecutive transactions to recognize it as two separate
	// events
	/**
	 * When transactions are evaluated for each stop, this constant is used as
	 * the minimum time between two consecutive transactions that tells us that
	 * they belong to separate dwell events.
	 * <P>
	 * So, if a route starts and ends at the same stop, this is the minimum rest
	 * time between two runs of the route by the same vehicle.
	 */
	private static final int interDwellEventTimeForTheSameStopTimeLimit = 100;
	private static final int minimumTravelTimeBetweenConsecutiveStops = 15;
	private static final int maximumDeltaTapTimeForTransactionsBelongingToOneCluster = 15;
	private static final int minDwellTime = 10;
	private static final int minTransactionClusterSize = 4;

	// fields
	DataBaseAdmin dba;
	Scenario scenario;
	String outputEventsPath;
	private String stopLookupTableName;
	private String tripTableName;
	/**
	 * This map of lines, organized by id, should correspond to the lines in the
	 * transit schedule.
	 */
	private HashMap<Id, CepasLine> cepasLines;
	/**
	 * Cepas vehicles distinguished by an id composed as line_route_busRegNum.
	 */
	private HashMap<String, CepasVehicle> cepasVehicles = new HashMap<String, CepasToEvents.CepasVehicle>();
	private String serviceTableName;
	private HashMap<String, Id> cepasStoptoMatsimStopLookup = new HashMap<String, Id>();
	/**
	 * Iterate through the stops in a route quite often, so easier to have the
	 * ids in a map than have to call getId() every time
	 */
	private HashMap<Id, ArrayList<Id>> routeIdToStopIdSequence;
	private ErrorTracker errorTracker;
	private ArrayList<String> alreadyCompletedVehicles = new ArrayList<String>();

	// constructors
	/**
	 * @param databaseProperties
	 *            : a properties file used by sergio's {@link DataBaseAdmin}
	 *            class
	 * @param transitSchedule
	 * @param networkFile
	 * @param outputEventsPath
	 * @param tripTableName
	 *            : where in the database to find the list of Cepas records,
	 *            given as schema.table
	 * @param stopLookupTableName
	 *            : a table in the database that equates each Cepas stop as
	 *            given in the Cepas record with a stop id given in the matsim
	 *            transit schedule
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	public CepasToEvents(String databaseProperties, String transitSchedule, String networkFile,
			String outputEventsPath, String tripTableName, String stopLookupTableName) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, IOException, SQLException {
		this.errorTracker = new ErrorTracker();
		this.dba = new DataBaseAdmin(new File(databaseProperties));
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader nwr = new MatsimNetworkReader(scenario);
		nwr.readFile(networkFile);
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		TransitScheduleReader tsr = new TransitScheduleReader(scenario);
		tsr.readFile(transitSchedule);
		this.outputEventsPath = outputEventsPath;

		this.tripTableName = tripTableName;
		this.stopLookupTableName = stopLookupTableName;

	}

	public void run(boolean continueFromBreak) throws SQLException, NoConnectionException, IOException {
		generateRouteIdToStopIdSequence();
		createVehiclesByCepasLineDirectionAndBusRegNum();
		createCepasToMatsimStopLookupTable();
		System.out.println(new Date());
		processCepasTransactionRecordsByLineDirectionBusRegNum(continueFromBreak);
		System.out.println(new Date());
	}

	private void generateRouteIdToStopIdSequence() {
		routeIdToStopIdSequence = new HashMap<Id, ArrayList<Id>>();
		for (Id lineId : scenario.getTransitSchedule().getTransitLines().keySet()) {
			Map<Id, TransitRoute> routes = scenario.getTransitSchedule().getTransitLines().get(lineId).getRoutes();
			for (TransitRoute route : routes.values()) {
				ArrayList<Id> stopIds = new ArrayList<Id>();
				ArrayList<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
				stops.addAll(route.getStops());
				for (TransitRouteStop trStop : stops) {
					stopIds.add(trStop.getStopFacility().getId());
				}
				routeIdToStopIdSequence.put(route.getId(), stopIds);
			}
		}
	}

	private void createCepasToMatsimStopLookupTable() throws SQLException, NoConnectionException {
		ResultSet resultSet = dba.executeQuery("select *  from " + this.stopLookupTableName
				+ " where matsim_stop is not null and ezlink_stop is not null");
		while (resultSet.next()) {
			String cepasId = resultSet.getString("ezlink_stop");
			Id matsimId = new IdImpl(resultSet.getString("matsim_stop"));
			this.cepasStoptoMatsimStopLookup.put(cepasId, matsimId);
		}

	}

	private void processCepasTransactionRecordsByLineDirectionBusRegNum(boolean continueFromBreak) throws SQLException,
			NoConnectionException, IOException {
		BufferedWriter transactionsBeforeWriter = null;
		BufferedWriter transactionsAfterWriter = null;
		BufferedWriter transactionsAfterTimeCorrectionWriter = null;
		BufferedWriter clusterWriter = null;
		BufferedWriter dwellEventWriter = null;
		BufferedWriter errorWriter = null;
		try {
			if (continueFromBreak) {
				checkForExistingfiles(outputEventsPath);
				errorWriter = IOUtils.getAppendingBufferedWriter(outputEventsPath + "/" + "errors.csv");
				transactionsBeforeWriter = IOUtils.getAppendingBufferedWriter(outputEventsPath + "/"
						+ "transactionsBeforeGPSCorrection.csv");
				transactionsAfterWriter = IOUtils.getAppendingBufferedWriter(outputEventsPath + "/"
						+ "transactionsAfterGPSCorrection.csv");
				transactionsAfterTimeCorrectionWriter = IOUtils.getAppendingBufferedWriter(outputEventsPath + "/"
						+ "transactionsAfterTimeCorrection.csv");
				clusterWriter = IOUtils.getAppendingBufferedWriter(outputEventsPath + "/" + "clusters.csv");
				dwellEventWriter = IOUtils.getAppendingBufferedWriter(outputEventsPath + "/" + "dwellEvents.csv");
			} else {

				errorWriter = new BufferedWriter(new FileWriter(outputEventsPath + "/" + "errors.csv"));
				transactionsBeforeWriter = new BufferedWriter(new FileWriter(outputEventsPath + "/"
						+ "transactionsBeforeGPSCorrection.csv"));
				transactionsAfterWriter = new BufferedWriter(new FileWriter(outputEventsPath + "/"
						+ "transactionsAfterGPSCorrection.csv"));
				transactionsAfterTimeCorrectionWriter = new BufferedWriter(new FileWriter(outputEventsPath + "/"
						+ "transactionsAfterTimeCorrection.csv"));
				clusterWriter = new BufferedWriter(new FileWriter(outputEventsPath + "/" + "clusters.csv"));
				dwellEventWriter = new BufferedWriter(new FileWriter(outputEventsPath + "/" + "dwellEvents.csv"));
			}
			int vehicles = 0;
			int numberOfVehicles = this.cepasVehicles.size()-this.alreadyCompletedVehicles.size();
			//copy the keys into a set so we can drop completed vehicles to save memory
			Set<String> vehicleIdStrings = new HashSet<String>();
			vehicleIdStrings.addAll(this.cepasVehicles.keySet());
			for ( String vehIdString:vehicleIdStrings) {
				CepasVehicle ptVehicle = this.cepasVehicles.get(vehIdString);
				// if (vehicles <= 2367) {
				// vehicles++;
				// continue;
				// }
//				 if (!ptVehicle.vehicleId.toString().equals("853C_1_923"))
//				 continue;
				if (alreadyCompletedVehicles.contains(ptVehicle.vehicleId.toString()))
					continue;
				if (ptVehicle.possibleMatsimRoutes==null || ptVehicle.possibleMatsimRoutes.size()<1){
					// TODO: if we don't have this transit line in the schedule,
					// ignore
					System.err.printf("The vehicle with id %s has no possible Matsim route, not firing events for it\n", ptVehicle.vehicleId,
							minimumNumberOfDwellEventsForProcessing);
					BufferedWriter emptyXML = IOUtils.getBufferedWriter(outputEventsPath + "/" + ptVehicle.vehicleId.toString()
						+ ".xml");
					emptyXML.close();
					errorWriter.write(errorTracker.writeStats(ptVehicle.vehicleId,true));
					errorWriter.flush();
					continue;
					
				}

				String query = String
						.format("select *"
								+ " from %s_passenger_preprocess where srvc_number = \'%s\' and direction = \'%d\' and bus_reg_num=\'%s\' "
								+ " and boarding_time > 10000 and alighting_time > 10000 "
								// +
								// " and boarding_time > 64000 and alighting_time > 64000 "
								// +
								// " and boarding_time < 69000 and alighting_time < 69000 "
								+ " order by boarding_time, alighting_time", tripTableName,
								ptVehicle.cepasLine.lineId.toString(), ptVehicle.cepasRoute.direction,
								ptVehicle.vehicleId.toString().split("_")[2]);
				System.out.printf("%06d:%06d - Running vehicle id: %s\n", vehicles, numberOfVehicles,
						ptVehicle.vehicleId.toString());
				ResultSet resultSet = dba.executeQuery(query);
				ptVehicle.handlePassengers(resultSet);
				ptVehicle.assignRouteScoresByNumberOfTransactions();
				ptVehicle.removeTransactionsNotInMostPopularRoute();
				ptVehicle.assignTransactionSpeeds();
				transactionsBeforeWriter.write(ptVehicle.printTransactions());
				transactionsBeforeWriter.flush();
				ptVehicle.correctGPSErrorsV2();
				transactionsAfterWriter.write(ptVehicle.printTransactions());
				transactionsAfterWriter.flush();
				ptVehicle.sortCepasTransactionsByStopId();
				ptVehicle.createDwellEventsFromTransactions();
				if (ptVehicle.orderedDwellEvents.size() < minimumNumberOfDwellEventsForProcessing) {
					System.err.printf("The vehicle with id %s has fewer than the required number of dwell events (%d)"
							+ ", not firing events for it\n", ptVehicle.vehicleId,
							minimumNumberOfDwellEventsForProcessing);
					BufferedWriter emptyXML = IOUtils.getBufferedWriter(outputEventsPath + "/" + ptVehicle.vehicleId.toString()
						+ ".xml");
					emptyXML.close();
					transactionsBeforeWriter.flush();
					transactionsAfterWriter.flush();
					errorWriter.write(errorTracker.writeStats(ptVehicle.vehicleId,true));
					errorWriter.flush();
					continue;

				}
				dwellEventWriter.write(ptVehicle.printStopsVisited());
				dwellEventWriter.flush();
				// if (!ptVehicle.dropDwellEventsNotInRoute()) {
				// }

				try {
					ptVehicle.clusterDwellEventsIntoRoutes();
				} catch (StackOverflowError e1) {
					System.err.println("Something went wrong with clustering dwellEvents for " + ptVehicle.vehicleId.toString());
					BufferedWriter emptyXML = IOUtils.getBufferedWriter(outputEventsPath + "/" + ptVehicle.vehicleId.toString()
							+ ".xml");
						emptyXML.close();
						transactionsBeforeWriter.flush();
						transactionsAfterWriter.flush();
						errorWriter.write(errorTracker.writeStats(ptVehicle.vehicleId,true));
						errorWriter.flush();
						continue;
				}
				ptVehicle.assignRoutesToDwellEventClusters();
				ptVehicle.dwellEventOverlapCheck();
				ptVehicle.interpolateDwellEvents();
				ptVehicle.updatetransactionTimes();

				EventsManager eventsManager = EventsUtils.createEventsManager();
				EventWriterXML eventWriter = new EventWriterXML(outputEventsPath + "/" + ptVehicle.vehicleId.toString()
						+ ".xml");
				eventsManager.addHandler(eventWriter);
				try{
					ptVehicle.fireEvents();
				}catch(Exception e){
					System.err.println("Something went wrong witgh firing events for " + ptVehicle.vehicleId.toString());
					BufferedWriter emptyXML = IOUtils.getBufferedWriter(outputEventsPath + "/" + ptVehicle.vehicleId.toString()
							+ ".xml");
						emptyXML.close();
						transactionsBeforeWriter.flush();
						transactionsAfterWriter.flush();
						errorWriter.write(errorTracker.writeStats(ptVehicle.vehicleId,true));
						errorWriter.flush();
						continue;
				}
				ptVehicle.processEventsQueue(eventsManager);
				eventWriter.closeFile();

				transactionsAfterTimeCorrectionWriter.write(ptVehicle.printTransactions());
				transactionsAfterTimeCorrectionWriter.flush();
				clusterWriter.write(ptVehicle.printClusters());
				clusterWriter.flush();
				errorWriter.write(errorTracker.writeStats(ptVehicle.vehicleId,false));
				errorWriter.flush();
				vehicles++;
				//save memory
				this.cepasVehicles.remove(vehIdString);
				// if (vehicles > 100)
				// break;
			}
			transactionsBeforeWriter.close();
			transactionsAfterWriter.close();
			transactionsAfterTimeCorrectionWriter.close();
			dwellEventWriter.close();
			clusterWriter.close();
			errorWriter.close();
		} catch (SQLException se) {

			String query = String
//					.format("create table %s_board_alight_preprocess as select * from (select card_id, boarding_stop_stn as stop_id, "
//							+ "(EXTRACT(epoch FROM (ride_start_time::TEXT)::interval)) as event_time,"
//							+ "\'boarding\' as type,"
//							+ "srvc_number, direction, bus_reg_num"
//							+ " from %s "
//							+ " union "
//							+ "select card_id, alighting_stop_stn as stop_id, "
//							+ "((EXTRACT(epoch FROM (ride_start_time::TEXT)::interval)) + (60 * ride_duration))::INT AS event_time,"
//							+ "\'alighting\' as type, "
//							+ "srvc_number, direction, bus_reg_num"
//							+ " from %s "
//							+ " ) as prequery where event_time is not null order by srvc_number, direction, bus_reg_num, event_time;"
//							+ "alter table %s_board_alight_preprocess add column idx serial;"
//							+ "alter table %s_board_alight_preprocess add column deltatime int;"
//
//					, tripTableName, tripTableName, tripTableName, tripTableName,tripTableName);
//			dba.executeStatement(query);
//			query = String
					.format("create table %s_passenger_preprocess as select card_id, boarding_stop_stn, alighting_stop_stn, (EXTRACT(epoch FROM (ride_start_time::TEXT)::interval)) as boarding_time,"
							+ "((EXTRACT(epoch FROM (ride_start_time::TEXT)::interval)) + (60 * ride_duration))::INT AS alighting_time, "
							+ "srvc_number, direction, bus_reg_num"
							+ " from %s order by srvc_number, direction, bus_reg_num, boarding_time, alighting_time;"
							+ "alter table %s_passenger_preprocess add column idx serial;", tripTableName,
							tripTableName, tripTableName);
			dba.executeStatement(query);
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			transactionsBeforeWriter.close();
			transactionsAfterWriter.close();
			transactionsAfterTimeCorrectionWriter.close();
			dwellEventWriter.close();
			errorWriter.close();
			clusterWriter.close();
			errorWriter.close();
			System.exit(-1);
		}

	}

	/**
	 * This goes through the Cepas lines and routes one by one and creates a
	 * unique vehicle assigned to each Cepas route. It also creates the list of
	 * Cepas lines on the fly from whatever records are contained in the Cepas
	 * transaction table. Each vehicle is put in a map, distinguished by
	 * line_route_busRegNum.
	 * <P>
	 * This method is recursive: if the supporting table doesn't exist, it
	 * creates it and call itself again, to make future calls faster.
	 * 
	 * @throws SQLException
	 * @throws NoConnectionException
	 */
	private void createVehiclesByCepasLineDirectionAndBusRegNum() throws SQLException, NoConnectionException {
		this.cepasLines = new HashMap<Id, CepasToEvents.CepasLine>();
		this.serviceTableName = this.tripTableName + "_services_by_vehicle";
		try {
			ResultSet resultSet = dba.executeQuery("select distinct srvc_number from " + this.serviceTableName
					+ " where srvc_number is not null");
			while (resultSet.next()) {
				IdImpl lineId = new IdImpl(resultSet.getString(1));
				CepasLine cepasLine = new CepasLine(lineId);
				this.cepasLines.put(lineId, cepasLine);
			}
			resultSet = dba.executeQuery("select distinct srvc_number, direction from " + this.serviceTableName
					+ " where srvc_number is not null");
			while (resultSet.next()) {
				IdImpl lineId = new IdImpl(resultSet.getString(1));
				CepasLine cepasLine = this.cepasLines.get(lineId);
				CepasRoute cepasRoute = new CepasRoute(resultSet.getInt(2), cepasLine);
				cepasLine.routes.put(resultSet.getInt(2), cepasRoute);
			}
			resultSet = dba.executeQuery("select distinct srvc_number, direction, bus_reg_num from "
					+ this.serviceTableName + " where srvc_number is not null");
			while (resultSet.next()) {
				IdImpl lineId = new IdImpl(resultSet.getString(1));
				CepasLine cepasLine = this.cepasLines.get(lineId);
				CepasRoute cepasRoute = cepasLine.routes.get(resultSet.getInt(2));
				Id ptVehicleId = new IdImpl(lineId.toString() + "_" + resultSet.getInt(2) + "_"
						+ resultSet.getString(3));
				CepasVehicle ptVehicle = new CepasVehicle(lineId, cepasRoute, ptVehicleId);
				cepasRoute.vehicles.put(ptVehicleId, ptVehicle);
				this.cepasVehicles.put(ptVehicleId.toString(), ptVehicle);
			}

			System.out.println(this.cepasLines);
		} catch (SQLException se) {
			// necessary to create a summary table
			System.out.println("Indexing....");
			dba.executeUpdate("update " + this.tripTableName + " set srvc_number = trim(srvc_number);");
			dba.executeUpdate("create index " + tripTableName.split("\\.")[1] + "_idx on " + this.tripTableName
					+ "(srvc_number, direction, bus_reg_num)");
			dba.executeStatement("create table " + serviceTableName
					+ " as select distinct srvc_number, direction, bus_reg_num from " + this.tripTableName
					+ " where srvc_number is not null");
			createVehiclesByCepasLineDirectionAndBusRegNum();
			return;
		}

	}

	// static methods
	public static void main(String[] args) throws InstantiationException, IllegalAccessException,
			ClassNotFoundException, IOException, SQLException, NoConnectionException {
		String databaseProperties = args[0];
		String transitSchedule = args[1];
		String networkFile  = args[2];
		String outputEventsPath  = args[3];
		String tripTableName  = args[4];
		String stopLookupTableName  = args[5];
		MatsimRandom.reset(12345678L);
		CepasToEvents cepasToEvents = new CepasToEvents(databaseProperties, transitSchedule, networkFile,
				outputEventsPath, tripTableName, stopLookupTableName);
		cepasToEvents.run(true);
	}

	private void checkForExistingfiles(String inputEventsPath) {
		File f = new File(inputEventsPath);
		if(f.list() == null)
			f.mkdir();
		ArrayList<String> tempFileNames = new ArrayList<String>(Arrays.asList(f.list()));
		ArrayList<String> removeFiles = new ArrayList<String>();
		for (String fileName : tempFileNames) {
			if (!(fileName.endsWith("xml") || fileName.endsWith("xml.gz")) || fileName.startsWith("merge")
					|| fileName.startsWith("OUT")) {
				removeFiles.add(fileName);

			}
		}
		for (String file : removeFiles) {
			tempFileNames.remove(file);
		}
		alreadyCompletedVehicles = new ArrayList<String>();
		for (String fileName : tempFileNames) {
			alreadyCompletedVehicles.add(fileName.split("\\.")[0]);
		}
		System.out.println("skipping vehicle ids: " + alreadyCompletedVehicles.toString());

	}

}
