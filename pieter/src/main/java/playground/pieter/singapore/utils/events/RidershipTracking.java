/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
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
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitRouteImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;
import playground.pieter.events.EventsMergeSort;
import playground.pieter.singapore.utils.events.listeners.IncrementEventWriterXML;
import playground.pieter.singapore.utils.postgresql.PostgresType;
import playground.pieter.singapore.utils.postgresql.PostgresqlCSVWriter;
import playground.pieter.singapore.utils.postgresql.PostgresqlColumnDefinition;

public class RidershipTracking {

	class RidershipTracker {
		FullDeparture fullDeparture;
		Id driverId;
		TransitRoute route;

		int ridership = 0;
		int stopsVisited = 0;
		Id fullDepartureId;
		int lastRidership = 0;

		public RidershipTracker(FullDeparture fullDeparture, Id driverId) {
			super();
			this.fullDeparture = fullDeparture;
			this.fullDepartureId = fullDeparture.fullDepartureId;
			route = departureIdToRoute.get(fullDepartureId);
			this.driverId = driverId;
		}

		public void ridershipIncrement(PersonEntersVehicleEvent event) {
			if (!event.getPersonId().equals(driverId))
				ridership++;
		}

		public void ridershipDecrement(PersonLeavesVehicleEvent event) {
			if (!event.getPersonId().equals(driverId))
				ridership--;
		}

		public void incrementStopsVisited() {
			stopsVisited++;
		}
		
		public int getIncrement(){
			int temp = lastRidership;
			lastRidership=ridership;
			return(ridership-temp);
			
		}

	}

	class FullDeparture {
		Id fullDepartureId;
		Id lineId;
		Id routeId;
		Id vehicleId;
		Id departureId;

		public FullDeparture(Id lineId, Id routeId, Id vehicleId, Id departureId) {
			super();
			this.lineId = lineId;
			this.routeId = routeId;
			this.vehicleId = vehicleId;
			this.departureId = departureId;
			fullDepartureId = new IdImpl(lineId.toString() + "_" + routeId.toString() + "_" + vehicleId.toString()
					+ "_" + departureId.toString());
		}
	}

	class RidershipHandler implements BasicEventHandler {
		private final Set<String> filteredEvents;
		{
			filteredEvents = new HashSet<String>();
			filteredEvents.add(TransitDriverStartsEvent.EVENT_TYPE);
			filteredEvents.add(VehicleArrivesAtFacilityEvent.EVENT_TYPE);
			filteredEvents.add(VehicleDepartsAtFacilityEvent.EVENT_TYPE);
			filteredEvents.add(PersonEntersVehicleEvent.EVENT_TYPE);
			filteredEvents.add(PersonLeavesVehicleEvent.EVENT_TYPE);
		}

		@Override
		public void reset(int iteration) {
		}

		@Override
		public void handleEvent(Event event) {
			if (filteredEvents.contains(event.getEventType())) {
				RidershipTracker tracker = null;

				tracker = vehicletrackers.get(event.getAttributes().get("vehicle"));
				if (tracker == null) {
					// it's a transit driver starts event, so has a different
					// attribute for vehid
					TransitDriverStartsEvent tdse = (TransitDriverStartsEvent) event;
					tracker = new RidershipTracker(new FullDeparture(tdse.getTransitLineId(), tdse.getTransitRouteId(),
							tdse.getVehicleId(), tdse.getDepartureId()), tdse.getDriverId());
					vehicletrackers.put(event.getAttributes().get("vehicleId"), tracker);
				}
				if (event.getEventType().equals(VehicleArrivesAtFacilityEvent.EVENT_TYPE)) {
					VehicleArrivesAtFacilityEvent vehArr = (VehicleArrivesAtFacilityEvent) event;
					tracker.incrementStopsVisited();
					Object[] args = { 
							tracker.fullDeparture.vehicleId, 
							tracker.fullDeparture.routeId,
							tracker.fullDeparture.lineId, 
							vehArr.getFacilityId(), 
							new Integer(tracker.stopsVisited),
							new Double(vehArr.getTime()), 
							tracker.ridership ,
							tracker.getIncrement()};
					ridershipWriter.addLine(args);
				}
				if (event.getEventType().equals(VehicleDepartsAtFacilityEvent.EVENT_TYPE)) {
					VehicleDepartsAtFacilityEvent vehDep = (VehicleDepartsAtFacilityEvent) event;
					Object[] args = { 
							tracker.fullDeparture.vehicleId, 
							tracker.fullDeparture.routeId,
							tracker.fullDeparture.lineId, 
							vehDep.getFacilityId(), 
							new Integer(tracker.stopsVisited),
							new Double(vehDep.getTime()), 
							tracker.ridership,
							tracker.getIncrement()};
					ridershipWriter.addLine(args);
				}
				if (event.getEventType().equals(PersonEntersVehicleEvent.EVENT_TYPE)) {
					tracker.ridershipIncrement((PersonEntersVehicleEvent) event);
				}
				if (event.getEventType().equals(PersonLeavesVehicleEvent.EVENT_TYPE)) {
					tracker.ridershipDecrement((PersonLeavesVehicleEvent) event);
				}

			}

		}

	}

	private ScenarioImpl loRes;

	private File outpath;
	private Map<String, RidershipTracker> vehicletrackers;

	private String eventsFile;
	private HashMap<Id, TransitRoute> departureIdToRoute;
	private final double maxSpeed = 80 / 3.6;

	private PostgresqlCSVWriter ridershipWriter;

	public RidershipTracking(String loResNetwork, String loResSchedule, String loResEvents) {
		loRes = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		loRes.getConfig().scenario().setUseTransit(true);
		new MatsimNetworkReader(loRes).readFile(loResNetwork);
		new TransitScheduleReader(loRes).readFile(loResSchedule);
		outpath = new File(new File(loResEvents).getParent() + "/temp");
		outpath.mkdir();
		vehicletrackers = new HashMap<String, RidershipTracking.RidershipTracker>();
		this.eventsFile = loResEvents;
	}

	public void run() {

		identifyVehicleRoutes();
		readEvents();
		
	}

	private void readEvents() {
		RidershipHandler handler = new RidershipHandler();
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(handler);
		EventsReaderXMLv1 eventReader = new EventsReaderXMLv1(eventsManager);
		eventReader.parse(eventsFile);

	}

	public void initWriter(File connectionProperties, String schemaName, String suffix)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException,
			NoConnectionException {
		DateFormat df = new SimpleDateFormat("yyyy_MM_dd");
		String formattedDate = df.format(new Date());
		// start with activities
		String actTableName = "" + schemaName + ".matsim_ridership" + suffix;
		List<PostgresqlColumnDefinition> columns = new ArrayList<PostgresqlColumnDefinition>();
		columns.add(new PostgresqlColumnDefinition("veh_id", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("route_id", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("line_id", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("stop_id", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("stop_no", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("time", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("ridership", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("ridership_increment", PostgresType.INT));
		DataBaseAdmin actDBA = new DataBaseAdmin(connectionProperties);
		ridershipWriter = new PostgresqlCSVWriter("RIDERSHIP", actTableName, actDBA, 1000, columns);
		ridershipWriter.addComment(String.format("MATSim ridership from events file %s, created on %s.", eventsFile,
				formattedDate));
	}

	private void identifyVehicleRoutes() {
		departureIdToRoute = new HashMap<Id, TransitRoute>();
		Collection<TransitLine> lines = loRes.getTransitSchedule().getTransitLines().values();
		for (TransitLine line : lines) {
			Collection<TransitRoute> routes = line.getRoutes().values();
			for (TransitRoute route : routes) {
				Collection<Departure> departures = route.getDepartures().values();
				for (Departure departure : departures) {
					departureIdToRoute.put(new FullDeparture(line.getId(), route.getId(), departure.getVehicleId(),
							departure.getId()).fullDepartureId, route);
				}
			}
		}
	}

	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException, NoConnectionException {
		String loResNetwork = args[0];
		String loResSchedule = args[1];
		String loResEvents = args[2];
		RidershipTracking xfer = new RidershipTracking(loResNetwork, loResSchedule, loResEvents);

		xfer.initWriter(new File(args[3]), args[4], args[5]);
		xfer.run();
	}

}
