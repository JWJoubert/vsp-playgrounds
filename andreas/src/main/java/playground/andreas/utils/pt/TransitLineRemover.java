package playground.andreas.utils.pt;

import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * Removes every line from the transit schedule serving a designated stop or a stop in a given area 
 * 
 * @author aneumann
 *
 */
public class TransitLineRemover {
	
	private static final Logger log = Logger.getLogger(TransitLineRemover.class);

	public static void main(String[] args) {
		final String SCHEDULEFILE = "e:/_shared-svn/andreas/paratransit/input/txl/remove/transitSchedule_orig.xml.gz";
		final String NETWORKFILE  = "e:/_shared-svn/andreas/paratransit/input/txl/remove/network.final.xml.gz";
		final String FILTERED_SCHEDULE_FILE = "e:/_shared-svn/andreas/paratransit/input/txl/remove/transitSchedule.xml.gz";
		
		Coord minCoord = new CoordImpl(4587744.0, 5824664.0);
		Coord maxCoord = new CoordImpl(4588400.0, 5825400.0);
		
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule transitSchedule = builder.createTransitSchedule();

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(NETWORKFILE);
		new TransitScheduleReaderV1(transitSchedule, network, scenario).readFile(SCHEDULEFILE);
		
		Set<Id> stopsInArea = TransitLineRemover.getStopIdsWithinArea(transitSchedule, minCoord, maxCoord);
		Set<Id> linesToRemove = TransitLineRemover.getLinesServingTheseStops(transitSchedule, stopsInArea);
		
		TransitSchedule filteredTransitSchedule = TransitLineRemover.removeTransitLinesFromTransitSchedule(transitSchedule, linesToRemove);
		new TransitScheduleWriterV1(filteredTransitSchedule).write(FILTERED_SCHEDULE_FILE);
	}
	
	public static Set<Id> getLinesServingTheseStops(TransitSchedule transitSchedule, Set<Id> stopIds){
		log.info("Searching for lines serving one of the following stops:" + stopIds);
		Set<Id> linesServingOneOfThoseStops = new TreeSet<Id>();
		
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					if(stopIds.contains(stop.getStopFacility().getId())){
						linesServingOneOfThoseStops.add(line.getId());
					}						
				}
			}
		}
		
		log.info("Found the following " + linesServingOneOfThoseStops.size() + " lines: " + linesServingOneOfThoseStops);
		return linesServingOneOfThoseStops;
	}
	
	public static Set<Id> getStopIdsWithinArea(TransitSchedule transitSchedule, Coord minCoord, Coord maxCoord){
		log.info("Searching for stops within the area of " + minCoord.toString() + " - " + maxCoord.toString());

		Set<Id> stopsInArea = new TreeSet<Id>();
		
		for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
			if (minCoord.getX() < stop.getCoord().getX() && maxCoord.getX() > stop.getCoord().getX()) {
				if (minCoord.getY() < stop.getCoord().getY() && maxCoord.getY() > stop.getCoord().getY()) {
					stopsInArea.add(stop.getId());
				}
			}
		}
		
		log.info("The following " + stopsInArea.size() + " stops are within the area: " + stopsInArea);		
		return stopsInArea;
	}

	public static TransitSchedule removeTransitLinesFromTransitSchedule(TransitSchedule transitSchedule, Set<Id> linesToRemove){
		log.info("Removing " + linesToRemove + " lines from transit schedule...");
		
		TransitSchedule tS = new TransitScheduleFactoryImpl().createTransitSchedule();
		
		for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
			tS.addStopFacility(stop);			
		}
		
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			if(!linesToRemove.contains(line.getId())) {
				tS.addTransitLine(line);
			}
		}
		
		log.info("Old schedule contained " + transitSchedule.getTransitLines().values().size() + " lines.");
		log.info("New schedule contains " + tS.getTransitLines().values().size() + " lines.");
		return tS;		
	}

}
