/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.bvg09.Visum2HafasMapper;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.xml.sax.SAXException;

import playground.droeder.DaPaths;

/**
 * @author droeder
 *
 */
public class Visum2HafasMapper{
	
	private static final Logger log = Logger
			.getLogger(Visum2HafasMapper.class);
	
	private static String PATH = DaPaths.OUTPUT + "bvg09/";
 	
	private static String VISUM = PATH + "intermediateTransitSchedule.xml";
	private static String HAFAS = PATH + "transitSchedule-HAFAS-Coord.xml";
	
	private ScenarioImpl visumSc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private ScenarioImpl hafasSc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	final Id CHECK = new IdImpl("CHECK");
	final Id REMOVE = new IdImpl("REMOVE");
	final Id DOUBLE = new IdImpl("DOUBLE");

	private SortedMap<Id, Id> allVisumHafasLineIds = null;
	
	private Map<Id, Id> preVisum2HafasMapById = null;
	private Map<Id, List<Id>> unmatchedPreVisum2HafasMap = null;
	private Map<Id, Id> preVisum2HafasMapByDist = null;
	
	private SortedMap<Id, Id> matchedVisumHafasLineIds = null;
	private Map<Id, Map<Id, Id>> visum2HafasMap = null;
	private Map<Id, List<Id>> matchedRoutes = null;

	public Visum2HafasMapper(){
		visumSc.getConfig().scenario().setUseTransit(true);
		readSchedule(VISUM, visumSc);
		hafasSc.getConfig().scenario().setUseTransit(true);
		readSchedule(HAFAS, hafasSc);
		this.createHafasLineIdsFromVisum();
	}
	
	public static void main(String[] args){
		Visum2HafasMapper mapper = new Visum2HafasMapper();
		mapper.run();
		mapper.test();
	}
	
	public void run(){

		this.compareDistAndId();
		this.removePreDoubleMatchedIds();
		this.checkAllLinesAfterPreMatch();
		this.matchAllLines();
		this.matchedId2TxtWithCoord();
	}
	
	public Map<Id, Map<Id, Id>> getVisum2HafasMap(){
		if(visum2HafasMap == null) this.calcVisum2HafasMap();
		return visum2HafasMap;
	}
	
	public Map<Id, Id> getAllVisumHafasLineIds(){
		if (allVisumHafasLineIds == null) this.createHafasLineIdsFromVisum();
		return allVisumHafasLineIds;
	}
	
	public Map<Id, Id> getMatchedLines(){
		if (this.matchedVisumHafasLineIds == null) this.calcVisum2HafasMap();
		return this.matchedVisumHafasLineIds;
	}
	
	private void calcVisum2HafasMap() {
		this.compareDistAndId();
		this.removePreDoubleMatchedIds();
		this.matchAllLines();
		
		// list all matched lines visum2Hafas
		this.matchedVisumHafasLineIds = new TreeMap<Id, Id>();
		for(Id id :  this.visum2HafasMap.keySet()){
			this.matchedVisumHafasLineIds.put(id, allVisumHafasLineIds.get(id));
		}
	}


	
	private void matchAllLines(){
		this.visum2HafasMap = new HashMap<Id, Map<Id,Id>>();
		matchedRoutes = new HashMap<Id, List<Id>>();
		int i = 0;
		for(Entry<Id, Id> e : this.allVisumHafasLineIds.entrySet()){
			if(hafasSc.getTransitSchedule().getTransitLines().containsKey(e.getValue())){
				i++;
				Map<Id, Id> temp = this.matchAllRoutes(e.getKey());
				if(!(temp == null)){
					log.info("matched visumLine " + e.getKey());
					this.visum2HafasMap.put(e.getKey(), temp);
				}else{
					log.error("not able to match visumLine " + e.getKey() + "! Only " + this.matchedRoutes.get(e.getKey()).size() + " of " + 
							visumSc.getTransitSchedule().getTransitLines().get(e.getKey()).getRoutes().size() + " routes are matched!");
				}
			}
		}
		log.info(this.visum2HafasMap.size() + " of " + i + " visumLines are matched (visum2Hafas)!");
	}
	
	private Map<Id, Id> matchAllRoutes(Id vLine){

		Map<Id, Id> temp = new HashMap<Id, Id>();
		List<Id> routes = new ArrayList<Id>();
		for(TransitRoute vRoute : visumSc.getTransitSchedule().getTransitLines().get(vLine).getRoutes().values()){
			Map<Id, Id> temp2 = this.searchHafasRouteForVisum(vRoute, allVisumHafasLineIds.get(vLine));
			if(!(temp2 == null)){
				temp.putAll(temp2);
				routes.add(vRoute.getId());
			}
		}
		
		matchedRoutes.put(vLine, routes);
		if (routes.size() ==  visumSc.getTransitSchedule().getTransitLines().get(vLine).getRoutes().size()){
			return temp;
		}else{
			return null;
		}
	}
	
	private Map<Id, Id> searchHafasRouteForVisum(TransitRoute vRoute, Id hLine){
		 Map<Integer, Tuple<Id, Id>> temp = new HashMap<Integer, Tuple<Id, Id>>();
		
		 // get prematched ids#
		 int  i = 0;
		 for(TransitRouteStop vStop : vRoute.getStops()){
			if(this.preVisum2HafasMapById.containsKey(vStop.getStopFacility().getId())){
				temp.put(i, new Tuple<Id, Id>(vStop.getStopFacility().getId(), preVisum2HafasMapById.get(vStop.getStopFacility().getId())));
			}else{
				temp.put(i, new Tuple<Id, Id>(vStop.getStopFacility().getId(), CHECK));
			}
			i++;
		 }
		 
		 
		 for(TransitRoute hRoute : hafasSc.getTransitSchedule().getTransitLines().get(hLine).getRoutes().values()){
			 Map<Id, Id> m = this.compareRoutes(vRoute, hRoute, temp);
			 if(!(m == null)){
				 return m;
			 }
		 }
		 return null;
		 
	}
	
	private Map<Id, Id> compareRoutes(TransitRoute vRoute, TransitRoute hRoute, Map<Integer, Tuple<Id, Id>> temp){
		Map<Id, Id> matched =  new HashMap<Id, Id>();
		
		ListIterator<TransitRouteStop> hIterator;
		ListIterator<TransitRouteStop> vIterator = vRoute.getStops().listIterator();
		boolean matching = false;
		TransitRouteStop vStop;
		TransitRouteStop hStop;
		
		do{
			if(vIterator.hasNext()){
				vStop = vIterator.next();
			}else{
				return null;
			}
			if(hRoute.getStops().size()> vIterator.previousIndex()){
				hIterator = hRoute.getStops().listIterator(vIterator.previousIndex());
			}else{
				return null;
			}
			while(hIterator.hasNext() && matching == false){
				hStop = hIterator.next();
				if(temp.get(vIterator.previousIndex()).getFirst().equals(vStop.getStopFacility().getId()) && 
						temp.get(vIterator.previousIndex()).getSecond().equals(hStop.getStopFacility().getId())){
					matching = true;
				}
			}
		}while (matching == false);
		
		int i = 0;
		if((vRoute.getStops().size() - vIterator.previousIndex()) > (hRoute.getStops().size() - hIterator.previousIndex())){
			return null;
		}else{
			hIterator = hRoute.getStops().listIterator(hIterator.previousIndex() - vIterator.previousIndex());
			vIterator = vRoute.getStops().listIterator();
			
			while(hIterator.hasNext() && vIterator.hasNext()){
				hStop = hIterator.next();
				vStop = vIterator.next();
				if(temp.get(vIterator.previousIndex()).getFirst().equals(vStop.getStopFacility().getId()) && 
						temp.get(vIterator.previousIndex()).getSecond().equals(hStop.getStopFacility().getId())) {
					matched.put(vStop.getStopFacility().getId(), hStop.getStopFacility().getId());
					i++;
				}else{
					matched.put(vStop.getStopFacility().getId(), hStop.getStopFacility().getId());
				}
			}
		}
		
		if( (0.75 < (1.0 * i / temp.size())) && (temp.size() == matched.size())){
			return matched;
		}else{
			return null;
		}
		
		
	}
	
	private void checkAllLinesAfterPreMatch(){
		
		for(TransitLine vLine: visumSc.getTransitSchedule().getTransitLines().values()){
			if (this.checkAllLineStopsMatched(vLine.getId()) ){
				System.out.println("all stops matched by Id and Dist for visumLine :" + vLine.getId());
			}
		}
	}
	
	private boolean checkAllLineStopsMatched(Id vLine){
		
		for(TransitRoute vRoute : visumSc.getTransitSchedule().getTransitLines().get(vLine).getRoutes().values()){
			for(TransitRouteStop stop : vRoute.getStops()){
				if(!preVisum2HafasMapById.containsKey(stop.getStopFacility().getId())){
					return false;
				}
			}
		}
		return true;
	}
	
	private void preMatchStopsById(){
		preVisum2HafasMapById = new TreeMap<Id, Id>();
		unmatchedPreVisum2HafasMap = new TreeMap<Id, List<Id>>();
		List<Id> hafasStops;
		
		for(TransitStopFacility vStop : visumSc.getTransitSchedule().getFacilities().values()){
			hafasStops = new ArrayList<Id>();
			for(TransitStopFacility hStop : hafasSc.getTransitSchedule().getFacilities().values()){
				if(this.checkFacsById(vStop.getId(), hStop.getId()) && (this.getDist(vStop.getId(), hStop.getId()))< 200){
					hafasStops.add(hStop.getId());
				}
			}
			if(hafasStops.size() == 0){
				unmatchedPreVisum2HafasMap.put(vStop.getId(), null);
			}else if(hafasStops.size() == 1){
				preVisum2HafasMapById.put(vStop.getId(), hafasStops.get(0));
			}else if(hafasStops.size() > 1){
				unmatchedPreVisum2HafasMap.put(vStop.getId(), hafasStops);
			}
		}
	}
	
	private void preMatchStopsByDist(){
		this.preVisum2HafasMapByDist = new TreeMap<Id, Id>();
		
		for(TransitStopFacility vStop : visumSc.getTransitSchedule().getFacilities().values()){
			Id next =  null;
			double shortest = Double.POSITIVE_INFINITY;
			for (TransitStopFacility hStop : hafasSc.getTransitSchedule().getFacilities().values()){
				double dist = this.getDist(vStop.getId(), hStop.getId());
				if(dist<shortest){
					shortest = dist;
					next = hStop.getId();
				}
			}
			preVisum2HafasMapByDist.put(vStop.getId(), next);
		}
	}
	
	private void compareDistAndId(){
		this.preMatchStopsByDist();
		this.preMatchStopsById();
		
		double absDist = 0;
		double maxDist = 0;
		for(Entry<Id, Id> e : this.preVisum2HafasMapById.entrySet()){
			double dist = this.getDist(e.getKey(), e.getValue());
			absDist +=  dist;
			if(dist>maxDist) maxDist = dist;
		}
		System.out.println("matched by Id: " + preVisum2HafasMapById.size() + " of " + visumSc.getTransitSchedule().getFacilities().size());
		System.out.println("maxDist: " + maxDist + " avgDist: " + (absDist/preVisum2HafasMapById.size()));
		System.out.println("----------------------");
		
		absDist = 0;
		maxDist = 0;
		for(Entry<Id, Id> e : this.preVisum2HafasMapByDist.entrySet()){
			double dist = this.getDist(e.getKey(), e.getValue());
			absDist +=  dist;
			if(dist>maxDist) maxDist = dist;
		}
		System.out.println("matched by Dist: " + preVisum2HafasMapByDist.size() + " of " + visumSc.getTransitSchedule().getFacilities().size());
		System.out.println("maxDist: " + maxDist + " avgDist: " + (absDist/preVisum2HafasMapByDist.size()));
		System.out.println("----------------------");
		
	}
	
	private void removePreDoubleMatchedIds(){
		System.out.println("check if matchedById and matchedByDist equal, else -> remove!");
		List<Id> temp = new ArrayList<Id>();
		for(Entry<Id, Id> e : this.preVisum2HafasMapById.entrySet()){
			Id hStopById = e.getValue(); 
			Id hStopByDist = this.preVisum2HafasMapByDist.get(e.getKey());
			if(!hStopById.equals(hStopByDist)){
				List<Id> list = new ArrayList<Id>();
				list.add(hStopByDist);
				list.add(hStopById);
				this.unmatchedPreVisum2HafasMap.put(e.getKey(), list);
				temp.add(e.getKey());
			}
		}
		
		for(Entry<Id, Id> e : this.preVisum2HafasMapByDist.entrySet()){
			if(!preVisum2HafasMapById.containsKey(e.getKey())){
				List<Id> list = new ArrayList<Id>();
				list.add(e.getValue());
				this.unmatchedPreVisum2HafasMap.put(e.getKey(), list);
				temp.add(e.getKey());
			}
		}
		
		for(Id id : temp){
			System.out.println("visumId " + id + " removed and added to unmatched!");
			this.preVisum2HafasMapByDist.remove(id);
			this.preVisum2HafasMapById.remove(id);
		}
		System.out.println("removed: " + temp.size());
		System.out.println("matched by Id and dist: " + preVisum2HafasMapById.size());
	}
	
	
	private double getDist(Id vStop, Id hStop){
		Coord v = visumSc.getTransitSchedule().getFacilities().get(vStop).getCoord();
		Coord h = hafasSc.getTransitSchedule().getFacilities().get(hStop).getCoord();
		
		double xDif = v.getX() - h.getX();
		double yDif = v.getY() - h.getY();
		
		return Math.sqrt(Math.pow(xDif, 2.0) + Math.pow(yDif, 2.0));
	}
	
//	private double getDistance(Coord v, Coord h){
//		double xDif = v.getX() - h.getX();
//		double yDif = v.getY() - h.getY();
//		return Math.sqrt(Math.pow(xDif, 2.0) + Math.pow(yDif, 2.0));
//	}
	


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
	
	
	private void createHafasLineIdsFromVisum(){
		allVisumHafasLineIds = new TreeMap<Id, Id>();
		String[] idToChar;
		StringBuffer createdHafasId;
		String hafasId;
		for(org.matsim.pt.transitSchedule.api.TransitLine line : visumSc.getTransitSchedule().getTransitLines().values()){
			createdHafasId = new StringBuffer();
			idToChar = line.getId().toString().split("");
			
			if(idToChar[1].equals("B")){
				if(idToChar[4].equals(" ") ){
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("   ");
				}else{
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[4]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("  ");
				}
			}else if(idToChar[1].equals("U")){
				createdHafasId.append(idToChar[1]);
				createdHafasId.append(idToChar[3]);
				createdHafasId.append("   ");
			}else if(idToChar[1].equals("T") && idToChar[3].equals("M") ){
				if(idToChar[4].equals(" ") ){
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("   ");
				}else{
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[4]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("  ");
				}
			}else if(idToChar[1].equals("T") && !(idToChar.equals("M")) ){
				createdHafasId.append(idToChar[3]);
				createdHafasId.append(idToChar[4]);
				createdHafasId.append("   ");
			}
			
			hafasId = createdHafasId.toString();
			if(createdHafasId.length()>0 && hafasSc.getTransitSchedule().getTransitLines().containsKey(new IdImpl(hafasId)) ){
				allVisumHafasLineIds.put(line.getId() , new IdImpl(hafasId));
			}else if (createdHafasId.length()>0){
				allVisumHafasLineIds.put(line.getId(), CHECK);
			}
		}
	}
	
	private boolean checkFacsById(Id vis, Id haf){
		boolean equal = false;
		
		String hafas = null;
		String visum =  vis.toString();
		if (visum.length() == 6){
			hafas = haf.toString().substring(2, haf.toString().length()-1);
		}else if(visum.length() == 5){
			hafas = haf.toString().substring(3, haf.toString().length()-1);
		}else if(visum.length() == 7){
			hafas = haf.toString().substring(1, haf.toString().length()-1);
		}
		visum = visum.substring(0, visum.length()-2);
		if(visum.equals(hafas)){
			equal = true;
		}
		
		return equal;
	}
	
	private void matchedId2TxtWithCoord(){
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(PATH + "matchedCoords.txt");
			
			writer.write("vId" + "\t" + "vX" + "\t" + "vY" + "\t" + "hId" + "\t" + "hX" + "\t" + "hY");
			writer.newLine();
			
			
			for(Entry<Id, Id> e : this.preVisum2HafasMapById.entrySet()){
				writer.write(e.getKey().toString() + "\t");
				writer.write(String.valueOf(visumSc.getTransitSchedule().getFacilities().get(e.getKey()).getCoord().getX()) + "\t");
				writer.write(String.valueOf(visumSc.getTransitSchedule().getFacilities().get(e.getKey()).getCoord().getY()) + "\t");
				writer.write(e.getValue().toString() + "\t");
				writer.write(String.valueOf(hafasSc.getTransitSchedule().getFacilities().get(e.getValue()).getCoord().getX()) + "\t");
				writer.write(String.valueOf(hafasSc.getTransitSchedule().getFacilities().get(e.getValue()).getCoord().getY()) + "\t");
				writer.newLine();
			}
			
			writer.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	private void test (){
		System.out.println(checkFacsById(new IdImpl("533520"), new IdImpl("9053352")));
		System.out.println(getDist(new IdImpl("533520"), new IdImpl("9053352")));
		System.out.println(this.preVisum2HafasMapById.get(new IdImpl("533520")));
	}
	
}
