/* *********************************************************************** *
 * project: org.matsim.*
 * FakeTravelTimeCost.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.dressler.ea_flow;

// java imports
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;

import playground.dressler.Interval.EdgeIntervals;
import playground.dressler.Interval.Interval;
import playground.dressler.Interval.SourceIntervals;
import playground.dressler.Interval.VertexInterval;
import playground.dressler.Interval.VertexIntervals;
import playground.dressler.ea_flow.FlowCalculationSettings;


/**
 * Implementation of the Moore-Bellman-Ford Algorithm for a static network! i =
 * 1 .. n for all e = (v,w) if l(w) > l(v) + c(e) then l(w) = l(v) + c(e), p(w) =
 * v.
 * @author Manuel Schneider
 */


public class BellmanFordIntervalBased {
		
	/**
	 * data structure to hold the present flow
	 */
	private Flow _flow;
 
	/**
	 * the calculation settings, providing most of the information
	 */
	private FlowCalculationSettings _settings;
	
	/**
	 * The network on which we find routes. We expect the network not to change
	 * between runs!
	 * This is simply for quick access.
	 */
	private final NetworkLayer _network;

	/**
	 * data structure to keep distance labels on nodes during and after one iteration of the shortest TimeExpandedPath Algorithm
	 */
	HashMap<Node, VertexIntervals> _labels;
	
	/**
	 * data structure to keep one label on each source
	 */

	HashMap<Node, VertexInterval> _sourcelabels;
	
	/**
	 * data structure to keep the sources that can send flow
	 */
	//LinkedList<Node> _unprocessedsources;

	
	
	//private static int _warmstart;
	//private LinkedList<Node> _warmstartlist;
	
	/**
	 * debug variable, the higher the value the more it tells
	 */
	private static int _debug=0;

	int gain = 0;
	
	private long _totalpolls=0L;
	
	private int _roundpolls=0;
	
	/*private long _prepstart=0;
	private long _prepend=0;
	private long _totalpreptime=0;*/
	
	private long _calcstart=0;
	private long _calcend=0;
	private long _totalcalctime=0;
	
	
	
	//--------------------CONSTRUCTORS-------------------------------------//
	
	/**
	 * Constructor using all the data initialized in the Flow object use recommended
	 * @param flow 
	 */
	public BellmanFordIntervalBased(FlowCalculationSettings settings, Flow flow) {
		this._settings = settings;
		this._flow = flow;
		this._network = settings.getNetwork();
	}
	
	
	
	/**
	 * Setter for debug mode the higher the value the more it tells
	 * @param debug > 0 is debug mode is on
	 */
	public static void debug(int debug){
		BellmanFordIntervalBased._debug = debug;
	}
	
	/**
	 * Setter for warmstart mode 
	 * @param warmstart > 0 is warmstart mode is on
	 */
	public static void warmstart(int warmstart){
		System.out.println("Warmstart currently not supported.");
		//BellmanFordIntervalBased._warmstart = warmstart;
	}
	
	private class BFTask  {		
		public int time;
		public Interval ival;
		public VirtualNode node;
		
		BFTask(VirtualNode node, Interval ival){
			this.time = ival.getLowBound();
			this.ival = ival;
			this.node = node; 
			
		}
		
		BFTask(VirtualNode node, int time){
			this.time = time;
			this.node = node; 			
			this.ival = null;
		}
		
		Boolean equals(BFTask other){
			return(this.time == other.time && other.node.equals(this.node));
		}
		
		@Override
		public String toString(){
			return node.getRealNode().getId().toString() + " @ " + time;
		}
	}
	
	private class TaskComparator implements Comparator<BFTask> {
		// Note: this comparator imposes orderings that are inconsistent with equals.
		public int compare(BFTask first, BFTask second) {
			if (first.time < second.time) {
				return -1; 
			} else if (first.time > second.time) {
				return 1;
			} else {
				// Important! PriorityQueue assumes that compare = 0 implies the same object ...
				return first.node.getRealNode().getId().compareTo(second.node.getRealNode().getId());
			}

		}
	}
		
	
	/**
	 * refreshes all _labels, _sourcelabels, and _unprocessedsources before one run of the algorithm
	 */
	private void refreshLabels(Queue<BFTask> queue){		
		this._labels = new HashMap<Node, VertexIntervals>();
		this._sourcelabels = new HashMap<Node, VertexInterval>();
		
		for(Node node: this._network.getNodes().values()){
			VertexInterval temp1 = new VertexInterval(0,this._settings.TimeHorizon);
			VertexIntervals label = new VertexIntervals(temp1);
			_labels.put(node, label);
			if (this._settings.isSource(node)) {
				VertexInterval temp2 = new VertexInterval(0, this._settings.TimeHorizon);
				if (this._flow.isActiveSource(node)){				
					queue.add(new BFTask(new VirtualSource(node), temp2));
					temp2.setScanned(false);
					temp2.setReachable(true);				
				} else {
					temp2.setScanned(false);
					temp2.setReachable(false);
				}
				this._sourcelabels.put(node, temp2);
			}
		}		
	}		
		
	/**
	 * Constructs  a TimeExpandedPath based on the labels set by the algorithm 
	 * @return shortest TimeExpandedPath from one active source to the sink if it exists
	 */
	private Collection<TimeExpandedPath> constructRoutes() throws BFException {
		
		//System.out.println("Constructing routes ...");
		
		Set<TimeExpandedPath> result = new HashSet<TimeExpandedPath>();
		
		Node toNode = this._settings.getSink();
		//VertexIntervalls tolabels = this._labels.get(to);
		VertexInterval toLabel = this._labels.get(toNode).getFirstPossible();
		if (toLabel == null) {
			throw new BFException("Sink cannot be reached at all!");
		}			
		
		int toTime = toLabel.getLowBound();		
		
		//check if TimeExpandedPath can be constructed
		
		if(Integer.MAX_VALUE == toTime){
			throw new BFException("Sink cannot be reached (totime == MAX_VALUE)!");
		}
		if(toTime >= this._settings.TimeHorizon){
			throw new BFException("Sink cannot be reached within TimeHorizon.");
		}
		
		
		
		// disabled (Daniel)
		//collect all reachable sinks, that are connected by a zero transit time,
		// infinite capacity arc.
		/*Set<Node> realSinksToSendTo = new HashSet<Node>();
		realSinksToSendTo.add(_sink);
		
		for(Link link : _sink.getInLinks().values())
		{
			Node realSink = link.getFromNode();
			VertexIntervall realSinkIntervall = this._labels.get(realSink).getIntervallAt(totime);
			if(realSinkIntervall.getReachable() && realSinkIntervall.getLowBound() == arrivalAtSuperSink)
				realSinksToSendTo.add(realSink);
		}*/		
		
		//start constructing the TimeExpandedPath
		TimeExpandedPath TEP = new TimeExpandedPath();		
		
		PathStep pred;
		pred = toLabel.getPredecessor();
		
		while (pred != null) {
			pred = pred.copyShiftedToArrival(toTime);
						
			TEP.prepend(pred);			
			
			toNode = pred.getStartNode().getRealNode();
			toTime = pred.getStartTime();
			//TEP.setStartTime(toTime); // really startTime
			
			if (pred instanceof StepEdge) {			  		
				toLabel = this._labels.get(toNode).getIntervalAt(toTime);			 
			} else if (pred instanceof StepSourceFlow) {
				if (pred.getForward()) {
					toLabel = this._sourcelabels.get(toNode);				  
				} else {
					toLabel = this._labels.get(toNode).getIntervalAt(toTime);
				}
			} else {
				throw new RuntimeException("Unknown instance of PathStep in ConstructRoutes()");
			}
			pred = toLabel.getPredecessor();
			
		}
		
		result.add(TEP);
		return result;
	}
	
	
	/**
	 * method for updating the labels of Node to during one iteration of the algorithm
	 * @param from a Task with a VertexInterval
	 * @param to Node to which we want to go 
	 * @param over Link upon which we travel
	 * @param forward indicates, weather we use a forward or backwards edge
	 * @return null or the list of labels that have changed
	 */
	private ArrayList<VertexInterval> relabel(Node from, Interval ival, Node to, Link over, boolean forward, int timehorizon) {		
			VertexIntervals labelto = _labels.get(to);
			EdgeIntervals flowover = this._flow.getFlow(over);
			ArrayList<Interval> arrive;
			ArrayList<VertexInterval> changed;
						
			//Interval start = _labels.get(from).getIntervalAt(time);
			
			// Create predecessor. It will be shifted correctly deeper down in the calls.			
			PathStep pred;
			if (forward) {
			  pred = new StepEdge(over, 0, this._settings.getLength(over), forward);
			} else {
			  pred = new StepEdge(over, this._settings.getLength(over), 0, forward);				
			}
						
			//if(start.getReachable() && !start.isScanned()){
				arrive = flowover.propagate(ival, this._settings.getCapacity(over),forward, timehorizon);
					
				if(arrive != null && !arrive.isEmpty()){
					changed = labelto.setTrueList(arrive , pred);
					return changed;
				}else{					
					return null;
				}					
			/*} else {
				System.out.println("Weird. Relabel called for unreachable or unscanned interval!");
			}
			return null;*/
					
	}
	
	/**
	 * main bellman ford algorithm calculating a shortest TimeExpandedPath
	 * @return shortest TimeExpandedPath from one active source to the sink if it exists
	 */
	public Collection<TimeExpandedPath> doCalculations() {
		System.out.println("Running BellmanFord with single Intervals (IntervalNode queue)");
		
		//set fresh labels

		// queue to save nodes we have to scan
		TaskComparator taskcomp = new TaskComparator();
		Queue<BFTask> queue = new PriorityQueue<BFTask>((1), taskcomp);
		
		refreshLabels(queue);
		
		
		// TODO warmstart
//		if(_warmstart>0 && _warmstartlist!=null){
//			queue.addAll(_warmstartlist);					
//			 for( Node node : activesources){
//				if(!queue.contains(node)){
//					queue.add(node);
//				}
//				
//			}
//			queue.addAll(activesources);
//		}else{
//			queue.addAll(activesources);
//		}
		//queue.addAll(this);

		BFTask task;

		// main loop
		//int gain = 0;
		this._roundpolls=0;
		this._calcstart=System.currentTimeMillis();
		while (true) {
			this._roundpolls++;
			this._totalpolls++;			
			
			// gets the first task in the queue			
			task = queue.poll();
			if (task == null) {
				break;
			}
				
			Node v = task.node.getRealNode();
			
			if (task.node instanceof VirtualSource) {				
				// send out of source v
				// just set the regular label on v
				
				VertexInterval inter = this._sourcelabels.get(v);
				
				// already scanned or not reachable (neither should occur ...)				
				if (inter.isScanned() || !inter.getReachable()) {
					System.out.println("Source " + v.getId() + " was already scanned or not reachable ...");
					continue;
				}
				inter.setScanned(true);
				
				PathStep pred = new StepSourceFlow(v, 0, true);
				
				Interval i = new Interval(0, this._settings.TimeHorizon); 
				ArrayList<VertexInterval> changed = this._labels.get(v).setTrueList(i , pred);
				for(VertexInterval changedintervall : changed){
					queue.add(new BFTask(new VirtualNormalNode(v, 0), changedintervall));
				}												
				
				continue; // no need to scan another interval
			} else if (task.node instanceof VirtualNormalNode) {
				// Clean Up before we do anything!
				// However, clean up seems totally useless for VertexIntervalls.
				//gain += _labels.get(iv._node).cleanup();

				// visit neighbors
				// link is outgoing edge of v => forward edge
				for (Link link : v.getOutLinks().values()) {				
					Node w = link.getToNode();
					ArrayList<VertexInterval> changed = relabel(v, task.ival, w, link, true, this._settings.TimeHorizon);
					if (changed == null) continue;
					if (!this._settings.isSink(w)) {
						for(VertexInterval changedinterval : changed){
							queue.add(new BFTask(new VirtualNormalNode(w, 0), changedinterval));
						}
					}
				}
				// link is incoming edge of v => backward edge
				for (Link link : v.getInLinks().values()) {
					Node w = link.getFromNode();
					ArrayList<VertexInterval> changed = relabel(v, task.ival, w, link, false, this._settings.TimeHorizon);
					if (changed == null) continue;

					if (!this._settings.isSink(w)) {
						for(VertexInterval changedinterval : changed){
							queue.add(new BFTask(new VirtualNormalNode(w, 0), changedinterval));
						}
					}
				}
				
				// treat empty sources! 
				if (this._flow.isNonActiveSource(v)) {
					if (!this._sourcelabels.get(v).getReachable()) {
						// we might have to do something ...
						// check if we can reverse flow
						SourceIntervals si = this._flow.getSourceOutflow(v);
						Interval arrive = si.canSendFlowBack(task.ival);
						if (arrive != null) {
							
							// indeed, we need to process this source
							VertexInterval temp = new VertexInterval(0, this._settings.TimeHorizon);
							temp.setScanned(false);
							temp.setReachable(true);	
							
							
							queue.add(new BFTask(new VirtualSource(v), temp));
																		  
							StepSourceFlow pred = new StepSourceFlow(v, arrive.getLowBound(), false);	
							this._sourcelabels.get(v).setArrivalAttributes(pred);
						}
					}
				}
				
			} else {
				throw new RuntimeException("Unsupported instance of VirtualNode in BellmanFordIntervalBased");
			}
						
			
			if(_debug>3){
				printStatus();
			}
		}
		this._calcend= System.currentTimeMillis();
		this._totalcalctime+=(this._calcend-this._calcstart);
		if (_debug>3) {
		  System.out.println("Removed " + gain + " intervals.");
		}
		
		//System.out.println("final labels: \n");
		//printStatus();
		
		Collection<TimeExpandedPath> TEPs = null; 
		try{ 
			TEPs  = constructRoutes();
		}catch (BFException e){
			System.out.println("stop reason: " + e.getMessage());
		}
		
		return TEPs;
		
	}
	
	
	
	/**
	 * creates a new warmstartlist, from the data of one run of the BF algorithm an sets _warmstartlist accordingly
	 */
	private void createwarmstartList() {/*
		// use cases of _warmstart to decide what to do
		if (_warmstart == 1) { // add the found path
		  _warmstartlist = new LinkedList<Node>();
		  if (_timeexpandedpath != null)
		  for (TimeExpandedPath.PathEdge edge : _timeexpandedpath.getPathEdges()) {
			  _warmstartlist.add(edge.getEdge().getFromNode());
			  //System.out.println(edge.getEdge().getFromNode().getId());
		  }
		} else if (_warmstart == 2) { // rebuild shortest path tree from last interval
		  _warmstartlist = new LinkedList<Node>();
		 
		  _warmstartlist.addAll(_labels.keySet());
		  
		  Collections.sort(_warmstartlist, new Comparator<Node>() {
		          public int compare(Node n1, Node n2) {
		        	   int v1 = _labels.get(n1).getLast().getLowBound();		        	   
		        	   int v2 = _labels.get(n2).getLast().getLowBound();
		        	   if (v1 > v2) {
		        		  return 1;
		        	   } else if (v1 == v2) {
		        		   return 0;
		        	   } else {
		        		   return -1;
		        	   }
		        	   		               
		          }
		     });
		  
		  for (Node node : _warmstartlist) {
			  System.out.println(node.getId().toString() + " " + _labels.get(node).getLast().getLowBound());
		  }
		  
		} else if (_warmstart == 3) { // rebuild shortest path tree from firstPossibleTime
			  _warmstartlist = new LinkedList<Node>();
				 
			  _warmstartlist.addAll(_labels.keySet());
			  
			  Collections.sort(_warmstartlist, new Comparator<Node>() {
			          public int compare(Node n1, Node n2) {
			        	   int v1 = _labels.get(n1).firstPossibleTime();		        	   
			        	   int v2 = _labels.get(n2).firstPossibleTime();
			        	   if (v1 > v2) {
			        		  return 1;
			        	   } else if (v1 == v2) {
			        		   return 0;
			        	   } else {
			        		   return -1;
			        	   }
			        	   		               
			          }
			     });
			  
			  for (Node node : _warmstartlist) {
				  System.out.println(node.getId().toString() + " " + _labels.get(node).getLast().getLowBound());
			  }
			  
			}*/
		
	}



	/**
	 * prints the Status on the console
	 *
	 */
	private void printStatus() {
		StringBuilder print = new StringBuilder();
		print.append("Regular lables");
		for(Node node : this._network.getNodes().values()){
			VertexIntervals inter = this._labels.get(node);
			int t =  inter.firstPossibleTime();
			if(t==Integer.MAX_VALUE){
				print.append(node.getId().toString() + " t: "+ "inf." +"\n");
			}else{
				print.append(node.getId().toString() + " t: "+ t +" over: "+ inter.getIntervalAt(t).getPredecessor() + "\n");				
			}
		}
		
		print.append("Source labels");
		for (Node node : this._flow.getSources()) {
			VertexInterval inter = this._sourcelabels.get(node);
			print.append(node.getId().toString() + " " + inter  +"\n");			
		}
		print.append("\n");
		System.out.println(print.toString());	
	}



	public String measure() {
		String result=
		"  Polls: "+this._roundpolls+
		//"\n      Preptime (ms): "+(this._prepend-this._prepstart)+
		"\n  Calctime (ms): "+(this._calcend-this._calcstart)+
		"\n  Totalpolls: "+(this._totalpolls)+
		//"\n  Totalpreptime (s): "+(this._totalpreptime/1000)+
		"\n  Totalcalctime (s): "+(this._totalcalctime/1000);
		
		// statistics for VertexIntervalls
		// min, max, avg size
		int min = Integer.MAX_VALUE;
		int max = 0;
		long sum = 0;
		for (Node node : this._network.getNodes().values()) {
			int size = this._labels.get(node).getSize(); 
			sum += size;
			min = Math.min(min, size);
			max = Math.max(max, size);
		}
		result += "\n  Size of VertexIntervalls (min/avg/max): " + min + " / " + sum / (double) this._network.getNodes().size() + " / " + max + "\n"; 
		return result;
	}
	

}