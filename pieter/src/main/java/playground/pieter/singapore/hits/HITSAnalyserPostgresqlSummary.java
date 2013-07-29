package playground.pieter.singapore.hits;

//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.TimeZone;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;

import javax.management.timer.Timer;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.AStarEuclidean;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.PreProcessEuclidean;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.Vehicle;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

import playground.pieter.singapore.utils.postgresql.PostgresType;
import playground.pieter.singapore.utils.postgresql.PostgresqlCSVWriter;
import playground.pieter.singapore.utils.postgresql.PostgresqlColumnDefinition;
import playground.pieter.singapore.utils.postgresql.travelcomponents.Activity;
import playground.pieter.singapore.utils.postgresql.travelcomponents.Journey;
import playground.pieter.singapore.utils.postgresql.travelcomponents.Transfer;
import playground.pieter.singapore.utils.postgresql.travelcomponents.TravellerChain;
import playground.pieter.singapore.utils.postgresql.travelcomponents.Trip;
import playground.pieter.singapore.utils.postgresql.travelcomponents.Wait;
import playground.pieter.singapore.utils.postgresql.travelcomponents.Walk;
import playground.sergioo.hitsRouter2013.MultiNodeDijkstra;
import playground.sergioo.hitsRouter2013.TransitRouterVariableImpl;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkTravelTimeAndDisutilityWW;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkWW;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkWW.TransitRouterNetworkLink;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.*;

public class HITSAnalyserPostgresqlSummary {

	private HITSData hitsData;
	static Scenario shortestPathCarNetworkOnlyScenario;
	static NetworkImpl carFreeSpeedNetwork;
	static NetworkImpl fullCongestedNetwork;
	private static HashMap<Integer, Integer> zip2DGP;
	private HashMap<Integer, Integer> zip2SubDGP;
	private static HashMap<Integer, ArrayList<Integer>> DGP2Zip;
	static Connection conn;

	public static Connection getConn() {
		return conn;
	}

	public static void setConn(Connection conn) {
		HITSAnalyserPostgresqlSummary.conn = conn;
	}

	private java.util.Date referenceDate; // all dates were referenced against a
	private HashMap<String, TravellerChain> chains;
	// starting Date of 1st September,
	// 2008, 00:00:00 SGT
	static HashMap<Integer, Coord> zip2Coord;

	private static ArrayList<Integer> DGPs;

	static PreProcessDijkstra preProcessData;
	private static LeastCostPathCalculator shortestCarNetworkPathCalculator;
	static XY2Links xY2Links;
	static Map<Id, Link> links;
	private static Dijkstra carCongestedDijkstra;
	private static TransitRouterVariableImpl transitRouter;
	private static TransitRouterImpl transitScheduleRouter;
	private static Scenario scenario;
	private static TransitRouterNetworkTravelTimeAndDisutilityWW transitTravelFunction;
	private static TransitRouterNetworkWW transitRouterNetwork;
	private static MyTransitRouterConfig transitRouterConfig;
	private static HashSet<TransitLine> mrtLines;
	private static HashSet<TransitLine> lrtLines;
	private static String[] MRT_LINES;
	private static String[] LRT_LINES;
	private static HashMap<String, Coord> mrtCoords;
	private static HashMap<String, Coord> lrtCoords;
	public static boolean freeSpeedRouting;
	private static String eventsFileName;
	private static String plansFileName;

	static void createRouters(String[] args, boolean fSR) {
		freeSpeedRouting = fSR;
		scenario = ScenarioUtils
				.createScenario(ConfigUtils.loadConfig(args[0]));
		(new MatsimNetworkReader(scenario)).readFile(args[1]);
		if (!freeSpeedRouting){
			(new MatsimPopulationReader(scenario)).readFile(args[2]);
			plansFileName = args[2];
		}
		(new TransitScheduleReader(scenario)).readFile(args[3]);
		double startTime = new Double(args[5]), endTime = new Double(args[6]), binSize = new Double(
				args[7]);
		WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(
				scenario.getPopulation(), scenario.getTransitSchedule(),
				(int) binSize, (int) (endTime - startTime));
		TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculatorFactoryImpl()
				.createTravelTimeCalculator(scenario.getNetwork(), scenario
						.getConfig().travelTimeCalculator());
		System.out.println("Loading events");
		if (!freeSpeedRouting) {
			eventsFileName = args[4];
			EventsManager eventsManager = EventsUtils
					.createEventsManager(scenario.getConfig());
			eventsManager.addHandler(waitTimeCalculator);
			eventsManager.addHandler(travelTimeCalculator);
			(new MatsimEventsReader(eventsManager)).readFile(args[4]);
		}
		transitRouterConfig = new MyTransitRouterConfig(scenario.getConfig()
				.planCalcScore(), scenario.getConfig().plansCalcRoute(),
				scenario.getConfig().transitRouter(), scenario.getConfig()
						.vspExperimental());
		transitRouterNetwork = TransitRouterNetworkWW.createFromSchedule(
				scenario.getNetwork(), scenario.getTransitSchedule(),
				transitRouterConfig.beelineWalkConnectionDistance);
		TransitRouterNetwork transitScheduleRouterNetwork = TransitRouterNetwork
				.createFromSchedule(scenario.getTransitSchedule(),
						transitRouterConfig.beelineWalkConnectionDistance);
		PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(
				scenario.getTransitSchedule());
		transitTravelFunction = new TransitRouterNetworkTravelTimeAndDisutilityWW(
				transitRouterConfig, scenario.getNetwork(),
				transitRouterNetwork,
				travelTimeCalculator.getLinkTravelTimes(),
				waitTimeCalculator.getWaitTimes(), scenario.getConfig()
						.travelTimeCalculator(), startTime, endTime,
				preparedTransitSchedule);
		transitRouter = new TransitRouterVariableImpl(transitRouterConfig,
				transitTravelFunction, transitRouterNetwork,
				scenario.getNetwork());
		TransitRouterNetworkTravelTimeAndDisutility routerNetworkTravelTimeAndDisutility = new TransitRouterNetworkTravelTimeAndDisutility(
				transitRouterConfig, preparedTransitSchedule);
		transitScheduleRouter = new TransitRouterImpl(transitRouterConfig,
				preparedTransitSchedule, transitScheduleRouterNetwork,
				routerNetworkTravelTimeAndDisutility,
				routerNetworkTravelTimeAndDisutility);
		// get the set of mrt and lrt lines for special case routing
		mrtLines = new HashSet<TransitLine>();
		lrtLines = new HashSet<TransitLine>();
		Collection<TransitLine> lines = scenario.getTransitSchedule()
				.getTransitLines().values();
		MRT_LINES = new String[] { "EW", "NS", "NE", "CC" };
		LRT_LINES = new String[] { "SW", "SE", "PE", "BP" };
		Arrays.sort(MRT_LINES);
		Arrays.sort(LRT_LINES);
		List<TransitRouteStop> mrtStops = new ArrayList<TransitRouteStop>();
		List<TransitRouteStop> lrtStops = new ArrayList<TransitRouteStop>();
		mrtCoords = new HashMap<String, Coord>();
		lrtCoords = new HashMap<String, Coord>();

		for (TransitLine line : lines) {
			if (line.getRoutes().size() != 0) {
				if (Arrays.binarySearch(MRT_LINES, 0, 4, line.getId()
						.toString()) >= 0) {
					mrtLines.add(line);
					// get the mrt stops
					for (TransitRoute route : line.getRoutes().values()) {
						mrtStops.addAll(route.getStops());
					}

				}
				if (Arrays.binarySearch(LRT_LINES, 0, 4, line.getId()
						.toString()) >= 0) {
					lrtLines.add(line);
					for (TransitRoute route : line.getRoutes().values()) {
						lrtStops.addAll(route.getStops());
					}
				}
			}
		}
		for (TransitRouteStop stop : mrtStops) {
			mrtCoords.put(stop.getStopFacility().getName(), stop
					.getStopFacility().getCoord());
		}
		for (TransitRouteStop stop : lrtStops) {
			mrtCoords.put(stop.getStopFacility().getName(), stop
					.getStopFacility().getCoord());
		}

		// now for car
		TravelDisutility travelDisutility = new TravelCostCalculatorFactoryImpl()
				.createTravelDisutility(travelTimeCalculator
						.getLinkTravelTimes(), scenario.getConfig()
						.planCalcScore());
		carCongestedDijkstra = new Dijkstra(scenario.getNetwork(),
				travelDisutility, travelTimeCalculator.getLinkTravelTimes());
		HashSet<String> modeSet = new HashSet<String>();
		modeSet.add("car");
		carCongestedDijkstra.setModeRestriction(modeSet);
		fullCongestedNetwork = (NetworkImpl) scenario.getNetwork();
		// add a free speed network that is car only, to assign the correct
		// nodes to agents
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(
				scenario.getNetwork());
		HITSAnalyserPostgresqlSummary.carFreeSpeedNetwork = NetworkImpl
				.createNetwork();
		HashSet<String> modes = new HashSet<String>();
		modes.add(TransportMode.car);
		filter.filter(carFreeSpeedNetwork, modes);
		TravelDisutility travelMinCost = new TravelDisutility() {

			@Override
			public double getLinkTravelDisutility(Link link, double time,
					Person person, Vehicle vehicle) {
				return getLinkMinimumTravelDisutility(link);
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength() / link.getFreespeed();
			}
		};
		preProcessData = new PreProcessDijkstra();
		preProcessData.run(carFreeSpeedNetwork);
		TravelTime timeFunction = new TravelTime() {

			@Override
			public double getLinkTravelTime(Link link, double time,
					Person person, Vehicle vehicle) {
				return link.getLength() / link.getFreespeed();
			}
		};

		shortestCarNetworkPathCalculator = new Dijkstra(carFreeSpeedNetwork,
				travelMinCost, timeFunction, preProcessData);
		xY2Links = new XY2Links(carFreeSpeedNetwork, null);
	}
	public void writeSimulationResultsToSQL(File connectionProperties)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException, IOException, SQLException,
			NoConnectionException {
		System.out.println("starting summary");
		DateFormat df = new SimpleDateFormat("yyyy_MM_dd") ;
		String formattedDate = df.format(new Date());
		String postScript = freeSpeedRouting?"_freespeedrouted":"";
		// start with activities
		String actTableName = "m_calibration.hits_activities"+postScript;
		List<PostgresqlColumnDefinition> columns = new ArrayList<PostgresqlColumnDefinition>();
		columns.add(new PostgresqlColumnDefinition("activity_id",
				PostgresType.INT, "primary key"));
		columns.add(new PostgresqlColumnDefinition("person_id",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("pcode",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("type", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("start_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("end_time", PostgresType.INT));
		DataBaseAdmin actDBA = new DataBaseAdmin(connectionProperties);
		PostgresqlCSVWriter activityWriter = new PostgresqlCSVWriter("ACTS",
				actTableName, actDBA, 10000, columns);
		if(!freeSpeedRouting){
			activityWriter.addComment(
					String.format("HITS activities, routed using events file %s and plans file %s",
							eventsFileName,plansFileName,formattedDate));
		}

		String journeyTableName = "m_calibration.hits_journeys"+postScript;
		columns = new ArrayList<PostgresqlColumnDefinition>();
		columns.add(new PostgresqlColumnDefinition("journey_id",
				PostgresType.INT, "primary key"));
		columns.add(new PostgresqlColumnDefinition("person_id",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("trip_idx_hits",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("start_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("end_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("distance",
				PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("main_mode",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("from_act", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("to_act", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("in_vehicle_distance",
				PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("in_vehicle_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("access_walk_distance",
				PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("access_walk_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("access_wait_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("egress_walk_distance",
				PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("egress_walk_time",
				PostgresType.INT));
		DataBaseAdmin journeyDBA = new DataBaseAdmin(connectionProperties);
		PostgresqlCSVWriter journeyWriter = new PostgresqlCSVWriter("JOURNEYS",
				journeyTableName, journeyDBA, 5000, columns);
		if(!freeSpeedRouting){
			journeyWriter.addComment(
					String.format("HITS journeys, routed using events file %s and plans file %s on %s", 
							eventsFileName,plansFileName, formattedDate));
		}

		String tripTableName = "m_calibration.hits_trips"+postScript;
		columns = new ArrayList<PostgresqlColumnDefinition>();
		columns.add(new PostgresqlColumnDefinition("trip_id", PostgresType.INT,
				"primary key"));
		columns.add(new PostgresqlColumnDefinition("journey_id",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("start_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("end_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("distance",
				PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("mode", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("line", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("route", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("boarding_stop",
				PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("alighting_stop",
				PostgresType.TEXT));
		DataBaseAdmin tripDBA = new DataBaseAdmin(connectionProperties);
		PostgresqlCSVWriter tripWriter = new PostgresqlCSVWriter("TRIPS",
				tripTableName, tripDBA, 10000, columns);
		if(!freeSpeedRouting){
			tripWriter.addComment(
					String.format("HITS trips, routed using events file %s and plans file %s",
							eventsFileName,plansFileName,formattedDate));
		}
		
		String transferTableName = "m_calibration.hits_transfers"+postScript;
		columns = new ArrayList<PostgresqlColumnDefinition>();
		columns.add(new PostgresqlColumnDefinition("transfer_id",
				PostgresType.INT, "primary key"));
		columns.add(new PostgresqlColumnDefinition("journey_id",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("start_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("end_time", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("from_trip",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("to_trip", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("walk_distance",
				PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("walk_time",
				PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("wait_time",
				PostgresType.INT));
		DataBaseAdmin transferDBA = new DataBaseAdmin(connectionProperties);
		PostgresqlCSVWriter transferWriter = new PostgresqlCSVWriter(
				"TRANSFERS", transferTableName, transferDBA, 1000, columns);
		if(!freeSpeedRouting){
			transferWriter.addComment(
					String.format("HITS transfers, routed using events file %s and plans file %s on %s", 
							eventsFileName,plansFileName,formattedDate));
		}

		for (Entry<String, TravellerChain> entry : chains.entrySet()) {
			String pax_id = entry.getKey().toString();
			TravellerChain chain = entry.getValue();
			for (Activity act : chain.getActs()) {
				Object[] args = { new Integer(act.getElementId()), pax_id,
						act.getFacility(), act.getType(),
						new Integer((int) act.getStartTime()),
						new Integer((int) act.getEndTime()) };
				activityWriter.addLine(args);
			}
			for (Journey journey : chain.getJourneys()) {
				try {

					Object[] journeyArgs = {
							new Integer(journey.getElementId()), pax_id,journey.getTrip_idx(),
							new Integer((int) journey.getStartTime()),
							new Integer((int) journey.getEndTime()),
							new Double(journey.getDistance()),
							journey.getMainMode(),
							new Integer(journey.getFromAct().getElementId()),
							new Integer(journey.getToAct().getElementId()),
							new Double(journey.getInVehDistance()),
							new Integer((int) journey.getInVehTime()),
							new Double(journey.getAccessWalkDistance()),
							new Integer((int) journey.getAccessWalkTime()),
							new Integer((int) journey.getAccessWaitTime()),
							new Double(journey.getEgressWalkDistance()),
							new Integer((int) journey.getEgressWalkTime())

					};
					journeyWriter.addLine(journeyArgs);
					if (!journey.isCarJourney()) {
						for (Trip trip : journey.getTrips()) {
							Object[] tripArgs = {
									new Integer(trip.getElementId()),
									new Integer(journey.getElementId()),
									new Integer((int) trip.getStartTime()),
									new Integer((int) trip.getEndTime()),
									new Double(trip.getDistance()), trip.getMode(),
									trip.getLine(), trip.getRoute(), trip.getBoardingStop(),
									trip.getAlightingStop() };
							tripWriter.addLine(tripArgs);
						}
						for (Transfer transfer : journey.getTransfers()) {
							Object[] transferArgs = {
									new Integer(transfer.getElementId()),
									new Integer(journey.getElementId()),
									new Integer((int) transfer.getStartTime()),
									new Integer((int) transfer.getEndTime()),
									new Integer(
											transfer.getFromTrip().getElementId()),
									new Integer(transfer.getToTrip().getElementId()),
									new Double(transfer.getWalkDistance()),
									new Integer((int) transfer.getWalkTime()),
									new Integer((int) transfer.getWaitTime()) };
							transferWriter.addLine(transferArgs);
						}
					}else{
						for (Trip trip : journey.getTrips()) {
							Object[] tripArgs = {
									new Integer(trip.getElementId()),
									new Integer(journey.getElementId()),
									new Integer((int) trip.getStartTime()),
									new Integer((int) trip.getEndTime()),
									new Double(trip.getDistance()), trip.getMode(),
									"null", "null", "null",
									"null" };
							tripWriter.addLine(tripArgs);
						}
					}
				} catch (Exception e) {
					
				}
			}

		}
		activityWriter.finish();
		journeyWriter.finish();
		tripWriter.finish();
		transferWriter.finish();
		
		DataBaseAdmin dba = new DataBaseAdmin(connectionProperties);
		// need to update the transit stop ids so they are consistent with LTA
		// list
		String update = "		UPDATE " + tripTableName
				+ " SET boarding_stop = matsim_to_transitstops_lookup.stop_id "
				+ " FROM m_calibration.matsim_to_transitstops_lookup "
				+ " WHERE boarding_stop = matsim_stop ";
		dba.executeUpdate(update);
		update = "		UPDATE "
				+ tripTableName
				+ " SET alighting_stop = matsim_to_transitstops_lookup.stop_id "
				+ " FROM m_calibration.matsim_to_transitstops_lookup "
				+ " WHERE alighting_stop = matsim_stop ";
		dba.executeUpdate(update);
		//drop and write a number of indices
		
		HashMap<String,String[]> idxNames = new HashMap<String, String[]>();
		String[] idx1 = {"person_id","pcode","type"};
		idxNames.put(actTableName, idx1);
		String[] idx2 = {"person_id","trip_idx_hits","from_act","to_act","main_mode"};
		idxNames.put(journeyTableName, idx2);
		String[] idx3 = {"journey_id","mode","line","route","boarding_stop","alighting_stop"};
		idxNames.put(tripTableName, idx3);
		String[] idx4 = {"journey_id","from_trip","to_trip"};
		idxNames.put(transferTableName, idx4);
		for(Entry<String,String[]> entry:idxNames.entrySet()){
			String tableName = entry.getKey();
			String[] columnNames = entry.getValue();
			for(String columnName:columnNames){
				String indexName = tableName.split("\\.")[1] + "_" + columnName;
				String fullIndexName = tableName.split("\\.")[0] + "." + indexName;
				String indexStatement;
				try{
					indexStatement = "DROP INDEX " + fullIndexName + " ;\n ";
					dba.executeStatement(indexStatement);
					System.out.println(indexStatement);
				}catch(SQLException e){
					e.printStackTrace();
				}
				
				try{
					indexStatement = "CREATE INDEX " + indexName + " ON "
							+ tableName + "("+ columnName +");\n";
					dba.executeStatement(indexStatement);
					System.out.println(indexStatement);
				}catch(SQLException e){
					e.printStackTrace();
				}	
				
			}
		}
		
		

	}
	public HITSAnalyserPostgresqlSummary() throws ParseException {

		DateFormat outdfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// outdfm.setTimeZone(TimeZone.getTimeZone("SGT"));
		referenceDate = outdfm.parse("2008-09-01 00:00:00");
	}

	public HITSAnalyserPostgresqlSummary(HITSData h2) throws ParseException,
			SQLException {

		this();
		this.setHitsData(h2);
	}

	public void setHitsData(HITSData hitsData) {
		this.hitsData = hitsData;
	}

	public HITSData getHitsData() {
		return hitsData;
	}

	public static void initXrefs() throws SQLException {
		// fillZoneData();
		setZip2DGP(conn);
		setDGP2Zip(conn);
		setZip2Coord(conn);
	}

	public void writeTripSummary() throws IOException {
		ArrayList<HITSTrip> ht = this.getHitsData().getTrips();
		FileWriter outFile = new FileWriter("f:/temp/tripsummary.csv");
		PrintWriter out = new PrintWriter(outFile);
		out.write(HITSTrip.HEADERSTRING);
		for (HITSTrip t : ht) {
			out.write(t.toString());
		}
		out.close();
	}

	private String getMode(boolean busStage, Id line) {
		if (busStage)
			return "bus";
		if (line.toString().contains("PE") || line.toString().contains("SE")
				|| line.toString().contains("SW"))
			return "lrt";
		else
			return "mrt";
	}

	public void writePersonSummary() throws IOException {
		ArrayList<HITSPerson> hp = this.getHitsData().getPersons();
		FileWriter outFile = new FileWriter("f:/temp/personsummary.csv");
		PrintWriter out = new PrintWriter(outFile);
		out.write(HITSPerson.HEADERSTRING);
		for (HITSPerson p : hp) {
			out.write(p.toString());
		}
		out.close();
	}

	public static TimeAndDistance getCarFreeSpeedShortestPathTimeAndDistance(
			Coord startCoord, Coord endCoord) {
		double distance = 0;
		Node startNode = carFreeSpeedNetwork.getNearestNode(startCoord);
		Node endNode = carFreeSpeedNetwork.getNearestNode(endCoord);

		Path path = shortestCarNetworkPathCalculator.calcLeastCostPath(
				startNode, endNode, 0, null, null);
		for (Link l : path.links) {
			distance += l.getLength();

		}
		return new TimeAndDistance(path.travelTime, distance);
	}

	public static TimeAndDistance getCarCongestedShortestPathDistance(
			Coord startCoord, Coord endCoord, double time) {
		double distance = 0;

		Node startNode = carFreeSpeedNetwork.getNearestNode(startCoord);
		Node endNode = carFreeSpeedNetwork.getNearestNode(endCoord);

		Path path = carCongestedDijkstra.calcLeastCostPath(startNode, endNode,
				time, null, null);
		for (Link l : path.links) {
			distance += l.getLength();
		}

		return new TimeAndDistance(path.travelTime, distance);
	}

	public static double getStraightLineDistance(Coord startCoord,
			Coord endCoord) {
		double x1 = startCoord.getX();
		double x2 = endCoord.getX();
		double y1 = startCoord.getY();
		double y2 = endCoord.getY();
		double xsq = Math.pow(x2 - x1, 2);
		double ysq = Math.pow(y2 - y1, 2);
		return (Math.sqrt(xsq + ysq));
	}

	static void setZip2DGP(Connection conn) throws SQLException {
		// init the hashmap
		zip2DGP = new HashMap<Integer, Integer>();
		Statement s;
		s = conn.createStatement();
		s.executeQuery("select zip, DGP from pcodes_zone_xycoords ;");
		ResultSet rs = s.getResultSet();
		// iterate through the resultset and populate the hashmap
		while (rs.next()) {
			zip2DGP.put(rs.getInt("zip"), rs.getInt("DGP"));
		}
	}

	private static void setZip2Coord(Connection conn) throws SQLException {
		// init the hashmap
		zip2Coord = new HashMap<Integer, Coord>();
		Statement s;
		s = conn.createStatement();
		s.executeQuery("select zip, x_utm48n, y_utm48n from pcodes_zone_xycoords where x_utm48n is not null;");
		ResultSet rs = s.getResultSet();
		// iterate through the resultset and populate the hashmap
		// Scenario scenario =
		// ScenarioUtils.createScenario(ConfigUtils.createConfig());
		while (rs.next()) {
			try {
				zip2Coord.put(
						rs.getInt("zip"),
						new CoordImpl(rs.getDouble("x_utm48n"), rs
								.getDouble("y_utm48n")));

			} catch (NullPointerException e) {
				System.out.println(rs.getInt("zip"));
			}
		}
	}

	static void setDGP2Zip(Connection conn) throws SQLException {
		Statement s;
		s = conn.createStatement();
		// get the list of DGPs, and associate a list of postal codes with each
		DGP2Zip = new HashMap<Integer, ArrayList<Integer>>();
		s.executeQuery("select distinct DGP from pcodes_zone_xycoords where DGP is not null;");
		ResultSet rs = s.getResultSet();
		// iterate through the list of DGPs, create an arraylist for each, then
		// fill that arraylist with all its associated postal codes
		DGPs = new ArrayList<Integer>();
		while (rs.next()) {
			ArrayList<Integer> zipsInDGP = new ArrayList<Integer>();
			Statement zs1 = conn.createStatement();
			zs1.executeQuery(String.format(
					"select zip from pcodes_zone_xycoords where DGP= %d;",
					rs.getInt("DGP")));
			ResultSet zrs1 = zs1.getResultSet();
			// add the zip codes to the arraylist for this dgp
			int currDGP = rs.getInt("DGP");
			// add the current DGP to the list of valid DGPs
			DGPs.add(currDGP);
			while (zrs1.next()) {
				zipsInDGP.add(zrs1.getInt("zip"));
			}
			// add the DGP and list of zip codes to the hashmap
			zipsInDGP.trimToSize();
			DGP2Zip.put(currDGP, zipsInDGP);
		}
		DGPs.trimToSize();

	}

	private void getZip2SubDGP(Connection conn) throws SQLException {
		// init the hashmap
		this.zip2SubDGP = new HashMap<Integer, Integer>();
		Statement s;
		s = conn.createStatement();
		s.executeQuery("select " + "zip, SubDGP from pcodes_zone_xycoords "
				+ ";");
		ResultSet rs = s.getResultSet();
		// iterate through the resultset and populate the hashmap
		while (rs.next()) {
			this.zip2SubDGP.put(rs.getInt("zip"), rs.getInt("SubDGP"));

		}
	}

	public static Coord getZip2Coord(int zip) {
		try {

			return HITSAnalyserPostgresqlSummary.zip2Coord.get(zip);
		} catch (NullPointerException ne) {
			return null;
		}
	}
	private double getValidStartTime(double startTime){
		while(startTime<0 || startTime>24*3600){
			if(startTime<0){
				startTime = 24*3600+startTime;
			}else if(startTime>24*3600){
				startTime = startTime-24*3600;
			}
		}
		return startTime;
	}
	private void compileTravellerChains(double busSearchradius,
			double mrtSearchRadius) {
		System.out.println("Starting summary : " + new java.util.Date());
		ArrayList<HITSPerson> persons = this.getHitsData().getPersons();
		this.chains = new HashMap<String, TravellerChain>();
		int counter = 0;
		PERSONS: for (HITSPerson p : persons) {
			counter++;
			// if( counter > 100)
			// return;
			TravellerChain chain = new TravellerChain();
			this.chains.put(p.pax_idx, chain);
			ArrayList<HITSTrip> trips = p.getTrips();
			int tripcount = trips.size();
			for (HITSTrip t : trips) {
				Coord origCoord;
				Coord destCoord;
				double startTime = ((double) (t.t3_starttime_24h.getTime() - this.referenceDate
						.getTime())) / (double) Timer.ONE_SECOND;

				double endTime = ((double) (t.t4_endtime_24h.getTime() - this.referenceDate
						.getTime())) / (double) Timer.ONE_SECOND;


				origCoord = HITSAnalyserPostgresqlSummary.zip2Coord
						.get(t.p13d_origpcode);
				destCoord = HITSAnalyserPostgresqlSummary.zip2Coord
						.get(t.t2_destpcode);
				if (origCoord == null) {
					System.out.println("Problem ZIP : " + t.p13d_origpcode);
					continue PERSONS;
				} else if (destCoord == null) {
					System.out.println("Problem ZIP : " + t.t2_destpcode);
					continue PERSONS;
				}
				if (t.trip_id == 1) {
					Activity home = chain.addActivity();
					home.setEndTime(startTime);
					home.setFacility(new IdImpl(t.p13d_origpcode));
					home.setStartTime(Math.min(startTime, 0));
					home.setType("home");
				} else {
					// get the last activity created
					try{
					Activity activity = chain.getActs().getLast();
					activity.setEndTime(startTime);
					}catch(NoSuchElementException n){
						continue PERSONS;
					}
				}
				// add the journey

				// if (!(t.mainmode.equals("publBus")
				// || t.mainmode.equals("mrt")
				// || t.mainmode.equals("lrt") || t.mainmode
				// .equals("carDrv")))
				// continue;
				Journey journey = chain.addJourney();
				try {
					journey.setCarJourney(t.mainmode.equals("carDrv"));
				} catch (NullPointerException ne) {

				}
				journey.setTrip_idx(t.h1_hhid + "_" + t.pax_id + "_"
						+ t.trip_id);
				journey.setStartTime(startTime);
				journey.setFromAct(chain.getActs().getLast());
				journey.setEndTime(endTime);
				// create the next activity
				Activity act = chain.addActivity();
				act.setFacility(new IdImpl(t.t2_destpcode));
				act.setStartTime(endTime);
				act.setType(t.t6_purpose);
				journey.setToAct(act);
				if (journey.isCarJourney()) {
					TimeAndDistance carTimeDistance = HITSAnalyserPostgresqlSummary
							.getCarCongestedShortestPathDistance(origCoord,
									destCoord, getValidStartTime(startTime));
					Trip trip = journey.addTrip();
					trip.setStartTime(startTime);
					trip.setEndTime(startTime + carTimeDistance.time);
					trip.setDistance(carTimeDistance.distance);
					journey.setCarDistance(carTimeDistance.distance);
					trip.setMode("car");

				}
				// route transit-only trips using the transit router
				if (t.stageChainSimple.equals(t.stageChainTransit)) {
					Set<TransitLine> lines = new HashSet<TransitLine>();
					Coord orig = origCoord;
					Coord dest = destCoord;
					Coord interimOrig = orig;
					Coord interimDest = dest;
					Path path = null;
					// deal with the 7 most common cases for now
					HITSStage stage = t.getStages().get(0);
					boolean busCheck = stage.t10_mode.equals("publBus");
					boolean mrtCheck = stage.t10_mode.equals("mrt");
					boolean lrtCheck = stage.t10_mode.equals("lrt");
					List<TransitStageRoutingInput> transitStages = new ArrayList<TransitStageRoutingInput>();
					boolean doneCompilingTransitStages = false;
					STAGES: while (!doneCompilingTransitStages) {
						// System.out.println(p.pax_idx + "_"
						// + t.trip_id);
						if (busCheck) {
							lines.add(HITSAnalyserPostgresqlSummary.scenario
									.getTransitSchedule().getTransitLines()
									.get(new IdImpl(stage.t11_boardsvcstn)));
							if (stage.nextStage != null) {
								if (stage.nextStage.t10_mode.equals("publBus")) {
									stage = stage.nextStage;
									continue STAGES;
								} else {

									// going to an lrt or mrt
									// station
									busCheck = false;
									mrtCheck = stage.nextStage.t10_mode
											.equals("mrt");
									lrtCheck = stage.nextStage.t10_mode
											.equals("lrt");
									interimDest = mrtCheck ? mrtCoords
											.get(stage.nextStage.t11_boardsvcstn)
											: lrtCoords
													.get(stage.nextStage.t11_boardsvcstn);
								}
								transitStages.add(new TransitStageRoutingInput(
										interimOrig, interimDest, lines, true));
								lines = new HashSet<TransitLine>();
								interimOrig = interimDest;
								interimDest = dest;
								stage = stage.nextStage;
								continue STAGES;
							}// next stage is null
							transitStages.add(new TransitStageRoutingInput(
									interimOrig, interimDest, lines, true));
							doneCompilingTransitStages = true;
						}// end of bus stage chain check
						if (mrtCheck || lrtCheck) {
							HashMap<String, Coord> theCoords = mrtCheck ? mrtCoords
									: lrtCoords;
							lines = mrtCheck ? mrtLines : lrtLines;
							interimOrig = theCoords.get(stage.t11_boardsvcstn);
							interimDest = theCoords.get(stage.t12_alightstn);
							transitStages.add(new TransitStageRoutingInput(
									interimOrig, interimDest, lines, false));
							if (stage.nextStage != null) {
								busCheck = stage.nextStage.t10_mode
										.equals("publBus");
								mrtCheck = stage.nextStage.t10_mode
										.equals("mrt");
								lrtCheck = stage.nextStage.t10_mode
										.equals("lrt");
								lines = new HashSet<TransitLine>();
								interimOrig = interimDest;
								interimDest = dest;
								stage = stage.nextStage;
								continue STAGES;
							} else {
								doneCompilingTransitStages = true;

							}
						}
					}

					// traverse the list of transitStages and
					// generate paths
					double linkStartTime = startTime;
					Coord walkOrigin = orig;

					int stage_id = 0;
					PATHS: for (int i = 0; i < transitStages.size(); i++) {
						stage_id++;
						int substage_id = 0;
						TransitStageRoutingInput ts = transitStages.get(i);
						transitRouter.setAllowedLines(ts.lines);
						// transitScheduleRouter.setA
						double[] radiusFactors = { 1.0,  1.5, 2.5,
								 5, 10, 
								25 };
						int radiusIdx = 0;
						path = null;
						linkStartTime=getValidStartTime(linkStartTime);
						while (path == null && radiusIdx < radiusFactors.length) {
							if (ts.busStage) {
								HITSAnalyserPostgresqlSummary.transitRouterConfig
										.setSearchradius(busSearchradius
												* radiusFactors[radiusIdx]);
							} else {
								HITSAnalyserPostgresqlSummary.transitRouterConfig
										.setSearchradius(mrtSearchRadius
												* radiusFactors[radiusIdx]);
							}
							try {
								path = transitRouter.calcPathRoute(ts.orig,
										ts.dest, linkStartTime,
										null);
							} catch (NullPointerException e) {

							}
							radiusIdx++;

						}
						if (path == null) {
							System.out.println("Cannot route " + t.h1_hhid
									+ "_" + t.pax_id + "_" + t.trip_id + "\t"
									+ orig + "\t" + dest + "\t line: " + lines);
							break PATHS;
						}

						if (i == 0) {
							Walk walk = journey.addWalk();
							Coord boardCoord = path.nodes.get(0).getCoord();
							double walkDistanceAccessFromRouter = CoordUtils
									.calcDistance(orig, boardCoord);

							double walkTimeAccessFromRouter = walkDistanceAccessFromRouter
									/ transitRouterConfig.getBeelineWalkSpeed();
							walk.setStartTime(linkStartTime);
							linkStartTime += walkTimeAccessFromRouter;
							walk.setEndTime(linkStartTime);
							walk.setDistance(walkDistanceAccessFromRouter);
							walk.setAccessWalk(true);
							substage_id++;

						}
						if (i > 0) {// in-between transitStage
							Coord boardCoord = path.nodes.get(0).getCoord();
							double interModalTransferDistance = CoordUtils
									.calcDistance(walkOrigin, boardCoord);
							double interModalTransferTime = interModalTransferDistance
									/ transitRouterConfig.getBeelineWalkSpeed();
							Walk walk = journey.addWalk();
							walk.setStartTime(linkStartTime);
							walk.setDistance(interModalTransferDistance);
							substage_id++;
							linkStartTime += interModalTransferTime;
							walk.setEndTime(linkStartTime);
						}
						walkOrigin = path.nodes.get(path.nodes.size() - 1)
								.getCoord();
						boolean inVehicle = false;

						for (int j = 0; j < path.links.size(); j++) {
							Link l = path.links.get(j);
							TransitRouterNetworkWW.TransitRouterNetworkLink transitLink = (TransitRouterNetworkLink) l;
							if (transitLink.getRoute() != null) {
								// in line link
								if (!inVehicle) {
									inVehicle = true;
									Trip trip = journey.addTrip();
									trip.setStartTime(linkStartTime);
									trip.setBoardingStop(journey.getWaits().getLast().getStopId());
									trip.setLine(transitLink.getLine().getId());
									trip.setRoute(transitLink.getRoute()
											.getId());
									trip.setMode(getMode(ts.busStage,
											trip.getLine()));
									trip.setEndTime(linkStartTime);
									trip.setDistance(0);
									if (journey.getPossibleTransfer() != null) {
										Transfer transfer = journey
												.getPossibleTransfer();
										transfer.setToTrip(trip);
										transfer.setEndTime(linkStartTime);
										journey.addTransfer(transfer);
										journey.setPossibleTransfer(null);
									}
									substage_id++;
								}
								Trip trip = journey.getTrips().getLast();
								double linkLength = transitLink.getLength();
								trip.incrementDistance(linkLength);
								double linkTime = transitTravelFunction
										.getLinkTravelTime(transitLink,
												linkStartTime,
												null, null);
								trip.incrementTime(linkTime);
								linkStartTime += linkTime;
							} else if (transitLink.toNode.route == null) {
								// transfer link
								double linkLength = transitLink.getLength();
								double linkTime = transitTravelFunction
										.getLinkTravelTime(transitLink,
												linkStartTime, null, null);
								Walk walk;
								if (substage_id > 1) {
									Trip trip = journey.getTrips().getLast();
									trip.setAlightingStop(l.getToNode().getId());
									Transfer transfer = new Transfer();
									journey.setPossibleTransfer(transfer);
									transfer.setFromTrip(trip);
									transfer.setStartTime(linkStartTime);
									walk = journey.addWalk();
									walk.setStartTime(linkStartTime);
									walk.setDistance(linkLength);
									transfer.getWalks().add(walk);
								} else {
									// this is still part of the access walk
									walk = journey.getWalks().getLast();
									walk.setDistance(walk.getDistance()
											+ linkLength);
								}
								if (j + 2 <= path.links.size()) {
									stage_id++;
									substage_id = 1;
								} else {
									substage_id++;
								}
								inVehicle = false;
								linkStartTime += linkTime;
								walk.setEndTime(linkStartTime);

							} else if (transitLink.fromNode.route == null) {
								// wait link
								substage_id++;
								double linkTime = transitTravelFunction
										.getLinkTravelTime(transitLink,
												linkStartTime,
												null, null);

								Wait wait = journey.addWait();
								wait.setStartTime(linkStartTime);
								wait.setStopId(l.getFromNode().getId());
								linkStartTime += linkTime;
								wait.setEndTime(linkStartTime);
								if (i == 0 && j == 0) {
									wait.setAccessWait(true);
								}

							} else
								throw new RuntimeException(
										"Bad transit router link");
						}// end path traversal
						if (i + 1 == transitStages.size()) {
							Coord alightCoord = path.nodes.get(
									path.nodes.size() - 1).getCoord();
							substage_id++;

							double walkDistanceEgressFromRouter = CoordUtils
									.calcDistance(alightCoord, dest);
							double walkTimeEgressFromRouter = walkDistanceEgressFromRouter
									/ transitRouterConfig.getBeelineWalkSpeed();
							Walk walk = journey.addWalk();
							walk.setStartTime(linkStartTime);
							walk.setEndTime(linkStartTime
									+ walkTimeEgressFromRouter);
							walk.setDistance(walkDistanceEgressFromRouter);
							walk.setEgressWalk(true);
						}
					}
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		HITSAnalyserPostgresqlSummary.createRouters(
				Arrays.copyOfRange(args, 3, 11), Boolean.parseBoolean(args[2]));
		System.out.println(new java.util.Date());
		HITSAnalyserPostgresqlSummary hp;
		System.out.println(args[0].equals("sql"));
		String fileName = "data/serial";
		DataBaseAdmin dba = new DataBaseAdmin(
				new File("data/hitsdb.properties"));
		Connection conn = dba.getConnection();
		System.out.println("Database connection established");
		HITSAnalyserPostgresqlSummary.setConn(conn);
		HITSAnalyserPostgresqlSummary.initXrefs();

		if (args[0].equals("sql")) {
			// this section determines whether to write the long or short
			// serialized file, default is the full file
			HITSData h;
			if (args[1].equals("short")) {

				h = new HITSData(conn, true);
			} else {
				h = new HITSData(conn, false);
			}
			hp = new HITSAnalyserPostgresqlSummary(h);
			// object serialization
			fileName = fileName + (args[1].equals("short") ? "short" : "");
			FileOutputStream fos = new FileOutputStream(fileName);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(hp.hitsData);
			oos.flush();
			oos.close();

		} else {
			// load and deserialize
			FileInputStream fis;
			fileName = fileName + (args[1].equals("short") ? "short" : "");
			fis = new FileInputStream(fileName);
			ObjectInputStream ois = new ObjectInputStream(fis);
			hp = new HITSAnalyserPostgresqlSummary((HITSData) ois.readObject());
			ois.close();

		}
		//
		// hp.writePersonSummary();
		// System.out.println("wrote person summary");
		// hp.writeTripSummary();
		// System.out.println("wrote trip summary");
		// hp.jointTripSummary();
		// System.out.println("wrote joint trip summary");
		hp.compileTravellerChains(100, 100);
		hp.writeSimulationResultsToSQL(new File("data/matsim2postgres.properties"));
		System.out.println("exiting...");
		System.out.println(new java.util.Date());
	}
}
