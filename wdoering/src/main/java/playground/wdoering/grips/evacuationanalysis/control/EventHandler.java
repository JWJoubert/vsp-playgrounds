/* *********************************************************************** *
 * project: org.matsim.*
 * RoadClosuresEditor.java
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

package playground.wdoering.grips.evacuationanalysis.control;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import playground.gregor.sim2d_v3.events.XYVxVyEvent;
import playground.gregor.sim2d_v3.events.XYVxVyEventsHandler;
import playground.wdoering.debugvisualization.model.Agent;
import playground.wdoering.debugvisualization.model.DataPoint;
import playground.wdoering.grips.evacuationanalysis.control.vis.ClearingTimeVisualizer;
import playground.wdoering.grips.evacuationanalysis.control.vis.EvacuationTimeVisualizer;
import playground.wdoering.grips.evacuationanalysis.data.Cell;
import playground.wdoering.grips.evacuationanalysis.data.ColorationMode;
import playground.wdoering.grips.evacuationanalysis.data.EventData;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;


public class EventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentArrivalEventHandler, AgentDepartureEventHandler, Runnable {

//	private HashMap<String, Agent> agents = null;
	private HashMap<Integer, DataPoint> networkNodes = null;
	private HashMap<Integer, int[]> networkLinks = null;
//	private ArrayList<Geometry> geometries = null;

//	private Double maxPosX,maxPosY,maxPosZ,maxTimeStep=Double.MIN_VALUE;
//	private Double minPosX,minPosY,minPosZ,minTimeStep=Double.MAX_VALUE;

	private Double[] timeStepsAsDoubleValues;

	private LinkedList<Double> timeSteps;

	private Thread readerThread;

//	private ShapeFileReader shapeFileReader;
	private double lastEventsTime = 0;
	private double lastTime = Double.NaN;
	private double 	step = 0.066745068285285;
	
	private ArrayList<Double> times;
	private Network network;

	private double cellSize;
	private QuadTree<Cell> cellTree;
	
	private final Map<Id,Event> events = new HashMap<Id, Event>();
	private double timeSum;
	private double maxCellTimeSum;
	private int maxUtilization;
	private int arrivals;
	private List<Tuple<Double, Integer>> arrivalTimes;
	
	private ArrayList<Link> links;
	
	private Rect boundingBox;
	private String eventName;
	
	private HashMap<Id, List<Tuple<Id,Double>>> linkEnterTimes;
	private HashMap<Id, List<Tuple<Id,Double>>> linkLeaveTimes;
	private double maxClearingTime;
	
	private ColorationMode colorationMode = ColorationMode.GREEN_YELLOW_RED;
	

	public EventHandler(String eventFilename, Scenario sc, double cellSize, Thread readerThread)
	{
		this.eventName = eventFilename;
		this.readerThread = readerThread;
		this.network = sc.getNetwork();
		this.cellSize = cellSize;
		this.arrivalTimes = new ArrayList<Tuple<Double, Integer>>();
		init();
	}
	
	private void init() {
		
		this.arrivals = 0;
		this.timeSum = 0;
		this.maxUtilization = 0;
		this.maxClearingTime = Double.NEGATIVE_INFINITY;
		this.maxCellTimeSum = Double.NEGATIVE_INFINITY;
		this.linkEnterTimes = new HashMap<Id, List<Tuple<Id,Double>>>();
		this.linkLeaveTimes = new HashMap<Id, List<Tuple<Id,Double>>>();
		
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		
		this.links = new ArrayList<Link>();
		
		for (Link link: this.network.getLinks().values())
		{
			if ((link.getId().toString().contains("el")) || (link.getId().toString().contains("en")) )
				continue;
			
			minX = Math.min(minX, Math.min(link.getFromNode().getCoord().getX(),link.getToNode().getCoord().getX()));
			minY = Math.min(minY, Math.min(link.getFromNode().getCoord().getY(),link.getToNode().getCoord().getY()));
			maxX = Math.max(maxX, Math.max(link.getFromNode().getCoord().getX(),link.getToNode().getCoord().getX()));
			maxY = Math.max(maxY, Math.max(link.getFromNode().getCoord().getY(),link.getToNode().getCoord().getY()));
			
			this.links.add(link);

		}		
		
//		for (org.matsim.api.core.v01.network.Node n : this.network.getNodes().values()) {
//			
//			if ((n.getId().toString().contains("en")) || (n.getId().toString().contains("el")))
//				continue;
//			
//			double x = n.getCoord().getX();
//			double y = n.getCoord().getY();
//			
//			if (x < minX) {
//				minX = x;
//			}
//			
//			if (x > maxX) {
//				maxX = x;
//			}
//			
//			if (y < minY) {
//				minY = y;
//			}
//			
//			if (y > maxY) {
//				maxY = y;
//			}
//		}
		
		this.boundingBox = new Rect(minX,minY,maxX,maxY);
		
		this.cellTree = new QuadTree<Cell>(minX,minY,maxX,maxY);
		
		for (double x = minX; x <= maxX; x += cellSize) {
			for (double y = minY; y <= maxY; y += cellSize) {
				Cell<List<Event>> cell = new Cell(new LinkedList<Event>());
				
				cell.setCoord(new CoordImpl(x, y));
				
				this.cellTree.put(x, y, cell);
			}
			
		}
		
		
		
	}

	public LinkedList<Double> getTimeSteps()
	{
		this.timeStepsAsDoubleValues = this.timeSteps.toArray(new Double[this.timeSteps.size()]);
		Arrays.sort(this.timeStepsAsDoubleValues);

		this.timeSteps = new LinkedList<Double>();
		for (Double timeStepValue : this.timeStepsAsDoubleValues)
			this.timeSteps.addLast(timeStepValue);

		return this.timeSteps;
		//return timeStepsAsDoubleValues;
	}



	public HashMap<Integer, int[]> getLinks()
	{
		return this.networkLinks;
	}

	public HashMap<Integer, DataPoint> getNodes()
	{
		return this.networkNodes;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void handleEvent(AgentDepartureEvent event)
	{
		//just save departure event
		this.events.put(event.getPersonId(), event);
		
		//get cell from person id
		AgentDepartureEvent departure = (AgentDepartureEvent)this.events.get(event.getPersonId());
		Link link = this.network.getLinks().get(departure.getLinkId());
		Coord c = link.getCoord();
		Cell<List<Event>> cell = this.cellTree.get(c.getX(), c.getY());
		
		//get the cell data, store event to it 
		List<Event> cellEvents = cell.getData();
		cellEvents.add(event);

	}
	
	@Override
	public void handleEvent(AgentArrivalEvent event)
	{
		//get cell from person id
		AgentDepartureEvent departure = (AgentDepartureEvent)this.events.get(event.getPersonId());
		Link link = this.network.getLinks().get(departure.getLinkId());
		Coord c = link.getCoord();
		Cell<List<Event>> cell = this.cellTree.get(c.getX(), c.getY());
		
		//get the cell data, store event to it 
		List<Event> cellEvents = cell.getData();
		cellEvents.add(event);
		
		double time = event.getTime() - departure.getTime();

		cell.setTimeSum(cell.getTimeSum() + time);
		
		//update max timesum of all cells
		this.maxCellTimeSum = Math.max(cell.getTimeSum(), this.maxCellTimeSum);
		
		cell.incrementCount();
		this.timeSum += time;
		this.arrivals++;
		
		if (lastTime!=event.getTime())
		{
			this.arrivalTimes.add(new Tuple(event.getTime(), this.arrivals));
			lastTime = event.getTime();
		}
		
	}


	@Override
	public void run()
	{

	}

	@Override
	public void handleEvent(LinkEnterEvent event)
	{
		//get link id
		Id linkId = event.getLinkId();
		Id personId = event.getPersonId();
		
		//get cell from person id
		Link link = this.network.getLinks().get(linkId);
		Coord c = link.getCoord();
		Cell<List<Event>> cell = this.cellTree.get(c.getX(), c.getY());
		
//		this.cellTree.get
		
		//update cell link enter time
		cell.addLinkEnterTime(linkId, personId, event.getTime());
		
		//check for highest global utilization of a single link
		int enterCount = cell.getLinkEnterTimes().size();
		maxUtilization = Math.max(maxUtilization, enterCount);
		
		//update global link enter times
		List<Tuple<Id,Double>> times;
		if (linkEnterTimes.containsKey(linkId))
			times = linkEnterTimes.get(linkId);
		else
			times = new LinkedList<Tuple<Id,Double>>();
		times.add(new Tuple(personId,event.getTime()));
		
		linkEnterTimes.put(linkId, times);
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event)
	{
		//get link id
		Id linkId = event.getLinkId();
		Id personId = event.getPersonId();
		
		//get cell from person id
		Link link = this.network.getLinks().get(linkId);
		Coord c = link.getCoord();
		Cell<List<Event>> cell = this.cellTree.get(c.getX(), c.getY());
		
		//update cell link leave time
		cell.addLinkLeaveTime(linkId, personId, event.getTime());
		
		//update global link leave times
		List<Tuple<Id,Double>> times;
		if (linkLeaveTimes.containsKey(linkId))
			times = linkLeaveTimes.get(linkId);
		else
			times = new LinkedList<Tuple<Id,Double>>();
		times.add(new Tuple(personId,event.getTime()));
		
		linkLeaveTimes.put(linkId, times);
		
	}
	
	public QuadTree<Cell> getCellTree() {
		return cellTree;
	}
 
	public EventData getData()
	{
		
		getClearingTimes();
		
		EventData eventData = new EventData(eventName);
		
		
		eventData.setCellTree(cellTree);
		eventData.setCellSize(cellSize);
		eventData.setTimeSum(timeSum);
		eventData.setMaxCellTimeSum(maxCellTimeSum);
		eventData.setArrivals(arrivals);
		eventData.setArrivalTimes(arrivalTimes);
		eventData.setBoundingBox(boundingBox);
		eventData.setLinkEnterTimes(linkEnterTimes);
		eventData.setLinkLeaveTimes(linkLeaveTimes);
		eventData.setMaxUtilization(maxUtilization);
		eventData.setMaxClearingTime(maxClearingTime);
		
		//set visualization attributes
		eventData.setEvacuationTimeVisData(EvacuationTimeVisualizer.getVisualData(eventData, this.colorationMode));
		eventData.setClearingTimeVisData(ClearingTimeVisualizer.getVisualData(eventData, this.colorationMode));
		
		
		return eventData;
	}
	
	private void getClearingTimes()
	{
				
		for (Link link : this.links)
		{
			Coord fromNodeCoord = link.getFromNode().getCoord();
			Coord toNodeCoord = link.getToNode().getCoord();
			
			double minX = Math.min(fromNodeCoord.getX(), toNodeCoord.getX()) - cellSize/2d;
			double maxX = Math.max(fromNodeCoord.getX(), toNodeCoord.getX()) + cellSize/2d;
			double minY = Math.min(fromNodeCoord.getY(), toNodeCoord.getY()) - cellSize/2d;
			double maxY = Math.max(fromNodeCoord.getY(), toNodeCoord.getY()) + cellSize/2d;
			
			Rect boundary = new Rect(minX, minY, maxX, maxY);

			//get all cells that are within the boundary from celltree
			LinkedList<Cell> cells = new LinkedList<Cell>();
			List<Tuple<Id, Double>> currentLinkLeaveTimes = linkLeaveTimes.get(link.getId());
			
			if ((currentLinkLeaveTimes!=null) && (currentLinkLeaveTimes.size()>0))
			{
				//cut 5%
				int confidentElementNo = Math.max(0, (int)(currentLinkLeaveTimes.size()*0.95d - 1));
				
				double latestTime = currentLinkLeaveTimes.get(confidentElementNo).getSecond();
				maxClearingTime = Math.max(latestTime, maxClearingTime);
				
				cellTree.get(boundary, cells);
				
				
				for (Cell cell : cells)
					cell.updateClearanceTime(latestTime);
			}
			
		}
	}


}
