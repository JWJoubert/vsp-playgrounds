package playground.droeder.bvg09;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.geotools.feature.FeatureType;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.xml.sax.SAXException;

import playground.droeder.DaPaths;
import playground.droeder.gis.DaShapeWriter;

import com.vividsolutions.jts.geom.GeometryFactory;

public class StopsAndRoutes2Shape{
	
	private FeatureType featureType;
	
	private GeometryFactory geometryFactory = new GeometryFactory();
	
	ScenarioImpl vSc;
	ScenarioImpl hSc;
	Map<Id, Id> lines;
	Map<Id, Id> preMatchedStops;
	
	Collection<Id> hStops;
	Collection<Id> vStops;
	Collection<Id> hLines;
	Collection<Id> vLines;
	
	Map<Id, SortedMap<String, String>> linkAttributes;
	
	Map<String, Tuple<Coord, Coord>> preMatched;
	
	
	private final String PATH = DaPaths .OUTPUT + "bvg09/";
	
	private final String VISUMSCHEDULEFILE = PATH + "intermediateTransitSchedule.xml";
	private final String VISUMNETWORK = PATH + "intermediateNetwork.xml";
	private final String VISUMSTOP2SHAPE = PATH + "VisumStops2Shape.shp";
	private final String VISUMROUTE2SHAPE = PATH + "VisumRoutes2Shape.shp";
	private final String VISUMLINKS2SHAPE = PATH + "VisumLinks2Shape.shp";
	private final String VISUMNODES2SHAPE = PATH + "VisumNodes2Shape.shp";
	
	private final String HAFASSCHEDULEFILE = PATH + "transitSchedule-HAFAS-Coord.xml";
	private final String HAFASSTOP2SHAPE = PATH + "HafasStops2Shape.shp";
	private final String HAFASROUTE2SHAPE = PATH + "HafasRoutes2Shape.shp";
	
	private final String MATCHEDSTOPS2SHAPE = PATH + "MatchedStops2Shape.shp";

	private final String NETWORK2SHAPE = PATH + "visumNet2shape.shp";
 	
	public static void main(String[] args) {
		StopsAndRoutes2Shape s2s = new StopsAndRoutes2Shape();
		s2s.run();
	}
	
	public StopsAndRoutes2Shape() {
		vSc = new ScenarioImpl();
		vSc.getConfig().scenario().setUseTransit(true);
		this.readSchedule(VISUMSCHEDULEFILE, vSc);
		this.readNetwork(VISUMNETWORK, vSc);
		
		hSc = new ScenarioImpl();
		hSc.getConfig().scenario().setUseTransit(true);
		this.readSchedule(HAFASSCHEDULEFILE, hSc);
		
		DaVisum2HafasMapper1 mapper = new DaVisum2HafasMapper1();
		this.lines = mapper.getVis2HafLines();
		this.preMatchedStops = mapper.getPrematchedStops();
	}
	
	private void readSchedule(String fileName, ScenarioImpl sc){
		TransitScheduleReader reader = new TransitScheduleReader(sc);
		try {
			reader.readFile(fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void readNetwork(String fileName, ScenarioImpl sc){
		MatsimNetworkReader reader = new MatsimNetworkReader(sc);
		
		reader.readFile(fileName);
	}

	public void run() {

		preProcessLinks();
		DaShapeWriter.writeLinks2Shape(NETWORK2SHAPE , vSc.getNetwork().getLinks(), linkAttributes);
		
//		this.preProcessStopsAndRoutes();
//		DaShapeWriter.writeTransitLines2Shape(HAFASROUTE2SHAPE, hSc.getTransitSchedule(), hLines);
//		DaShapeWriter.writeTransitLines2Shape(VISUMROUTE2SHAPE, vSc.getTransitSchedule(), vLines);
//		DaShapeWriter.writeRouteStops2Shape(HAFASSTOP2SHAPE, hSc.getTransitSchedule().getFacilities(), hStops);
//		DaShapeWriter.writeRouteStops2Shape(VISUMSTOP2SHAPE, vSc.getTransitSchedule().getFacilities(), vStops);
//		DaShapeWriter.writePointDist2Shape(MATCHEDSTOPS2SHAPE, preMatched);
//		DaShapeWriter.writeLinks2Shape(VISUMLINKS2SHAPE, vSc.getNetwork().getLinks());
//		DaShapeWriter.writeNodes2Shape(VISUMNODES2SHAPE, vSc.getNetwork().getNodes());
	}
	
	private void preProcessStopsAndRoutes(){

		hStops = new HashSet<Id>();
		vStops = new HashSet<Id>();
		hLines = new HashSet<Id>();
		vLines = new HashSet<Id>();
		//search for Lines and Stops 2 draw
		for(TransitLine line : vSc.getTransitSchedule().getTransitLines().values()){
			if (lines.containsKey(line.getId()) && hSc.getTransitSchedule().getTransitLines().containsKey(lines.get(line.getId()))){
				hLines.add(lines.get(line.getId()));
				vLines.add(line.getId());
				for(TransitRoute route : line.getRoutes().values()){
					for(TransitRouteStop stop : route.getStops()){
						if(!vStops.contains(stop.getStopFacility().getId())){
							vStops.add(stop.getStopFacility().getId());
						}
					}
				}
				
				for (TransitRoute route : hSc.getTransitSchedule().getTransitLines().get(lines.get(line.getId())).getRoutes().values()){
					for(TransitRouteStop stop : route.getStops()){
						if(!hStops.contains(stop.getStopFacility().getId())){
							hStops.add(stop.getStopFacility().getId());
						}
					}
				}
			}
		}
		
		preMatched = new HashMap<String, Tuple<Coord,Coord>>();
		//sort preMatchedStops
		
		for(Entry<Id, Id> e : preMatchedStops.entrySet()){
			preMatched.put(e.getKey().toString() + "_" + e.getValue().toString(), 
					new Tuple<Coord, Coord>(vSc.getTransitSchedule().getFacilities().get(e.getKey()).getCoord(),
					hSc.getTransitSchedule().getFacilities().get(e.getValue()).getCoord()));
		}
	
	}
	
	private void preProcessLinks(){
		this.linkAttributes = new HashMap<Id, SortedMap<String,String>>();
		SortedMap<String, String> attribs;
		
		for(Link l : vSc.getNetwork().getLinks().values()){
			attribs = new TreeMap<String, String>();
			attribs.put("modes", l.getAllowedModes().toString());
			linkAttributes.put(l.getId(), attribs);
		}
		
	}
	
}

