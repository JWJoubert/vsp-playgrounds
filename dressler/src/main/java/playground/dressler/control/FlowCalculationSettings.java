/* *********************************************************************** *
 * project: org.matsim.*												   *
 * GlobalFlowCalculationSettings.java									   *
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


package playground.dressler.control;

//matsim imports
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;

import playground.dressler.Interval.Interval;

public class FlowCalculationSettings {

	/* ---------------- public settings ---------------- */

	/* some constants */
	public static final int SEARCHALGO_FORWARD = 1;
	public static final int SEARCHALGO_REVERSE = 2;
	public static final int SEARCHALGO_MIXED = 3;

	/* default scaling parameters */
	public double timeStep = 1;
	public double flowFactor = 1.0;
	public int minTravelTime = 0; //set to 1 to avoid edges with time 0 after scaling! 
	public double addTravelTime = 0.0; //this is added to the travel times before conversion!

	/* deault timeouts */
	public int TimeHorizon = 7654321; // should be safe
	public int MaxRounds = 7654321;	// should be safe up to 3.8m flow units

	/* default search settings */
	public boolean useSinkCapacities = true;
	public int searchAlgo = SEARCHALGO_FORWARD;
    public boolean usePriorityQueue = false; // use a priority queue instead of simple BFS
	public boolean useVertexCleanup = false;
	public boolean useImplicitVertexCleanup = true; // unite vertex intervals before propagating?
	public boolean useShadowFlow = false; // use arrays and shadow flows for storing the edge flow
	public int checkConsistency = 0; // after how many iterations should consistency be checked? 0 = off
	public boolean checkTouchedNodes = true; // should Flow.UnfoldAndAugment() try to shortcut the search for suitable forward steps?
    public int doGarbageCollection = 0; // after how many iterations should the GC be called? 0 = off

	public boolean sortPathsBeforeAugmenting = true; // try to augment shorter (#steps) first?
	public boolean keepPaths = true; // should TEPs be stored at all?
	public boolean unfoldPaths = true; // if they are stored, should they be unfolded to contain only forward edges?
	public boolean mapLinksToTEP = true; // remember which path uses an edge at a given time
	public boolean useRepeatedPaths = true && !useSinkCapacities; // try to repeat paths
	public double quickCutOff = -1.0; // values < 0 continue fully, otherwise ratio of how much additional polls are done, e.g., 0.0 is stop immediately when the first path is found.   
	public boolean delaySinkPropagation = false; // propagate sinks (and resulting intervals) only if the search has nothing else to do 
	public boolean useHoldover = false; // allow holdover at all nodes
	public boolean useHoldoverCapacities = false;// limit holdover on each node

	public boolean trackUnreachableVertices = true && (searchAlgo == FlowCalculationSettings.SEARCHALGO_REVERSE);; // only works in REVERSE, wastes time otherwise

	
	
	/* when are links available? not included means "always" */
	public HashMap<Id, Interval> whenAvailable = null;
	public Node supersink = null;

	/* ---------------  private data from now on --------------- */

	/* interal storage for the network parameters */
	private HashMap<Link, Integer> _capacities;
	private HashMap<Link, Integer> _lengths;

	private NetworkImpl _network;

	// leave these untouched!
	private HashMap<Node, Integer> _demands = null;

	private int _totaldemandsources;
	private int _totaldemandsinks;
	private int _numsources;
	private int _numsinks;

	private int _roundedtozerocapacity;
	private int _roundedtozerolength;
	private double _roundingErrorCap;
	private double _roundingErrorLength;
	
	private boolean _ready = false;


	/**
	 * Constructor that prepares the default settings, but no network or anything yet.
	 */
	FlowCalculationSettings () {
	}
	

	/**
	 * Applies and checks the settings and creates all necessary data structures.
	 * @return true iff everything is okay
	 */
	public boolean prepare() {
		return prepare(false); // default is to prepare just once
	}

	public void setNetwork(NetworkImpl network) {
		this._network = network;
	}

	public void setDemands(HashMap<Node, Integer> demands) {
		this._demands = demands;
	}



	/**
	 * Applies and checks the settings and creates all necessary data structures.
	 * @param force If true, the data will always be recreated.
	 * @return true iff everything is okay
	 */
	public boolean prepare(boolean force) {

		// don't do the work twice ...
		if (this._ready && !force) {
			return true;
		}

		// basic checks
		if (this._network == null) {
			System.out.println("No network given!");
			return false;
		}

		if (this._demands == null) {
			System.out.println("No demands given!");
			return false;
		}

		// check search settings for some (not all) bad combinations
		if (this.useSinkCapacities) {
			if (this.searchAlgo != this.SEARCHALGO_FORWARD) {
				System.out.println("Only FORWARD search works with sink capacities enabled!");
				return false;
			}
			if (this.useRepeatedPaths) {
				System.out.println("Repeated paths cannot be used with sink capacities enabled!");
				return false;
			}
		}
		
		if (this.useHoldover) {
			if (this.searchAlgo != this.SEARCHALGO_FORWARD && this.searchAlgo != this.SEARCHALGO_REVERSE) {
				System.out.println("Only FORWARD and REVERSE search work with holdover enabled!");
				return false;
			}
			if (this.useSinkCapacities) {
				System.out.println("Holdover cannot be combined with sink capacities!");
				return false;
			}
			if (this.unfoldPaths) {
				System.out.println("Paths cannot be unfolded on the fly with holdover enabled!");
				return false;
			}
		}
		
		if (this.searchAlgo != this.SEARCHALGO_REVERSE && this.trackUnreachableVertices) {
			System.out.println("TrackUnreachableVertices does not make sense with anything but REVERSE search! I will disable it now.");
			this.trackUnreachableVertices = false;
		}
		
		if (this.usePriorityQueue) {
			if (this.searchAlgo != this.SEARCHALGO_FORWARD && this.searchAlgo != this.SEARCHALGO_REVERSE) {
				System.out.println("Only FORWARD and REVERSE search support the priority queue!");
				return false;
			}
		}

		scaleParameters();

		if (this.supersink != null) {
			int totaldemand = 0;
			int overrideerrors = 0;
			for (Node node : this._network.getNodes().values()) {
				Integer i = this._demands.get(node);
				if (i != null && i > 0)
				  totaldemand += i;
				if (i != null && i < 0 && !useSinkCapacities) {
					if (!node.equals(supersink)) {
					  this._demands.put(node, 0);
					  overrideerrors++;
					}
				}
			}
			if (this._demands.get(this.supersink) != null) {
				System.out.println("Warning: Supersink had demand " + this._demands.get(this.supersink) + ". It will be overwritten!");
			}
			this._demands.put(this.supersink, -totaldemand);
			if (overrideerrors > 0) {
				System.out.println("Warning! " + overrideerrors + " sink(s) were removed because a supersink was specified.");
			}
		}

		prepareDemands();

		this._ready = true;

		return true;
	}

	private void scaleParameters() {
		this._capacities = new HashMap<Link, Integer>();
		this._lengths = new HashMap<Link, Integer>();

        double capperiod = this._network.getCapacityPeriod();

		this._roundedtozerocapacity = 0;
		this._roundedtozerolength = 0;
		this._roundingErrorCap = 0d;
		this._roundingErrorLength = 0d;

		for (Link link : this._network.getLinks().values()){

			double exactTravelTime = link.getLength() / link.getFreespeed();
			
			// 0 should stay 0, e.g. on links to the supersink
			if (exactTravelTime > 0.01) {
			  exactTravelTime += this.addTravelTime;		
			}

			// from long to int ...
			int newTravelTime = (int) Math.round(exactTravelTime / this.timeStep);
			if (newTravelTime < this.minTravelTime) {
				newTravelTime = minTravelTime;
			}
			if (newTravelTime == 0 && exactTravelTime > 0.01) {
				this._roundedtozerolength++;
			}
			
			if (exactTravelTime > 0.01) {
			  this._roundingErrorLength += Math.abs(exactTravelTime / this.timeStep - newTravelTime) / (exactTravelTime / this.timeStep);
			}

			this._lengths.put(link, newTravelTime);


			double exactCapacity = link.getCapacity() * this.timeStep * this.flowFactor / capperiod;
			long newcapacity = Math.round(exactCapacity);

			// no one uses that much capacity for real ...
			if (newcapacity > Integer.MAX_VALUE) {
				newcapacity = Integer.MAX_VALUE;
				System.out.println("Note: capping capacity at Integer.MAX_VALUE");
			}

			if (newcapacity == 0l && link.getCapacity() != 0d) {
				this._roundedtozerocapacity++;
			}
			
			if (exactCapacity != 0d) {
			  this._roundingErrorCap += Math.abs(exactCapacity - newcapacity) / exactCapacity;	
			}
			this._capacities.put(link, (int) newcapacity);
		}
		
		this._roundingErrorCap /= this._network.getLinks().size();
		this._roundingErrorLength /= this._network.getLinks().size();
	}

	private void prepareDemands() {
		this._totaldemandsources = 0;
		this._totaldemandsinks = 0;
		this._numsources = 0;
		this._numsinks = 0;

		// find all sources
		for (Node node : this._network.getNodes().values()) {
			Integer i = this._demands.get(node);
			if (i != null) {
				if (i > 0) {
					this._totaldemandsources += i;
					this._numsources++;
				}
			}
		}

		// find all sinks and give them infinite capacity if needed
		for (Node node : this._network.getNodes().values()) {
			Integer i = this._demands.get(node);
			if (i != null) {
				if (i < 0) {
					this._numsinks++;
					if (this.useSinkCapacities) {
					  this._totaldemandsinks += -i;
					} else {
					  this._demands.put(node, -this._totaldemandsources);
					  this._totaldemandsinks += this._totaldemandsources;
					}
				}
			}
		}

	}

	public void printStatus() {
		System.out.println("==== Flow Calculation Settings ====");
		if (!this._ready) {
			System.out.println("WARNING: Prepare() has not run successfully yet.");
		}
		System.out.println("Network has " + this._network.getNodes().size() + " nodes and " + this._network.getLinks().size() + " edges.");
		System.out.println("Number sources: " + this._numsources + " | sinks: " + this._numsinks);
		System.out.println("Total demand sources: " + this._totaldemandsources + " | sinks: " + this._totaldemandsinks);
		System.out.println("Time Horizon: " + this.TimeHorizon);
		System.out.println("Use holdover: " + this.useHoldover);
		System.out.println(" finite holdover capacity: " + this.useHoldoverCapacities);
		System.out.println("Timestep: " + this.timeStep);
		System.out.println("FlowFactor: " + this.flowFactor);
		
		System.out.println("Max Rounds: " + this.MaxRounds);
		System.out.println("Min Travel Time : " + this.minTravelTime);
		System.out.println("Added (raw) Travel Time : " + this.addTravelTime);
		System.out.println("Edges rounded to zero length: " + this._roundedtozerolength);
		System.out.println("Rounding error in % of old lengths: " + 100 * this._roundingErrorLength);
		System.out.println("Edges rounded to zero capacity: " + this._roundedtozerocapacity);
		System.out.println("Rounding error in % of old capacities: " + 100 * this._roundingErrorCap);

		switch (this.searchAlgo) {
		  case FlowCalculationSettings.SEARCHALGO_FORWARD:
			  System.out.println("Algorithm to use: Forward Search");
			  break;
		  case FlowCalculationSettings.SEARCHALGO_REVERSE:
			  System.out.println("Algorithm to use: Reverse Search");
			  break;
		  case FlowCalculationSettings.SEARCHALGO_MIXED:
			  System.out.println("Algorithm to use: Mixed Search");
			  break;
		  default:
			  System.out.println("Algorithm to use: Unkown (" + this.searchAlgo +")");
		}
		
		System.out.println("Use PriorityQueue: " + this.usePriorityQueue);
		System.out.println("Sinks have finite capacity: " + this.useSinkCapacities);

		System.out.println("Track unreachable vertices: " + this.trackUnreachableVertices);
		System.out.println("Quick cutoff: " + this.quickCutOff);
		System.out.println("Delay sink propagation: " + this.delaySinkPropagation);
		System.out.println("Use vertex cleanup: " + this.useVertexCleanup);
		System.out.println("Use implicit vertex cleanup: " + this.useImplicitVertexCleanup);
		System.out.println("Use Shadow Flow: " + this.useShadowFlow);
		System.out.println("Use repeated paths: " + this.useRepeatedPaths);
		System.out.println("Sort paths before augmenting: " + this.sortPathsBeforeAugmenting);
		System.out.println("Use touched nodes hashmaps: " + this.checkTouchedNodes);
		System.out.println("Keep paths at all: " + this.keepPaths);
		System.out.println("Unfold stored paths: " + this.unfoldPaths);
		System.out.println(" Remeber which paths use an edge: "+ this.mapLinksToTEP);
		System.out.println("Check consistency every: " + this.checkConsistency + " rounds (0 = off)");
		System.out.println("Garbage collection every: " + this.doGarbageCollection + " rounds (0 = off)");

		System.out.println("===================================");
	}

	/**
	 * Returns the capacity of the edge as intended for the calculation!
	 * @param edge The edge to check.
	 * @return The flow capacity per tiemstep for the calculation.
	 */
	public int getCapacity(Link edge) {
	  return this._capacities.get(edge);
	}

	/**
	 * Returns the length of the edge as intended for the calculation!
	 * @param edge The edge to check.
	 * @return The length (in timesteps) for the calculation.
	 */
	public int getLength(Link edge) {
	  return this._lengths.get(edge);
	}

	public NetworkImpl getNetwork() {
		return this._network;
	}

	/**
	 * decides whether a node is or was a sink
	 * @param node to be checked
	 * @return true if there was originally negative demand on the node
	 */
	public boolean isSink(Node node) {
		Integer demand = this._demands.get(node);
		return (demand != null && demand < 0);
	}

	/**
	 * decides whether a node is or was a source
	 * @param node to be checked
	 * @return true if there was originally positive demand on the node
	 */
	public boolean isSource(Node node) {
		Integer demand = this._demands.get(node);
		return (demand != null && demand > 0);
	}

	public int getDemand(Node node) {
		Integer i = this._demands.get(node);
		if (i != null) return i;
		return 0;
	}

	public int getTotalDemand() {
		return this._totaldemandsources;
	}

	public void writeSimpleNetwork(boolean newformat) {
		// write simple data format to sysout
		// representing the dynamic graph, not the time-expanded graph
		System.out.println("% generated from matsim data");
        System.out.println("N " + this._network.getNodes().size());
        System.out.println("TIME " + this.TimeHorizon);
        HashMap<Node,Integer> newNodeNames = new HashMap<Node,Integer>();
        //find maximal id
        int max = 0;
        for (Node node : this._network.getNodes().values()) {
        	try {
        		int i = Integer.parseInt(node.getId().toString());
        		if (i > 0) 	newNodeNames.put(node,i);
        		if (i > max) max = i;
        	} catch (Exception except) {

        	}
        }
        //find negative ids
        for (Node node : this._network.getNodes().values()) {
        	try {
        		int i = Integer.parseInt(node.getId().toString());
        		if (i <= 0) {
        		  max += 1;
            	  newNodeNames.put(node, max);
            	  System.out.println("% node " + max + " was '" + node.getId()+ "'");
        		}
        	} catch (Exception except) {
        		max += 1;
        		newNodeNames.put(node, max);
        		System.out.println("% node " + max + " was '" + node.getId()+ "'");

        	}
        }
        //write nodes
        for (Node node : this._network.getNodes().values()) {
        	int d = 0;
        	if(!newformat){
        		if (this._demands.containsKey(node)) {
	        		d = this._demands.get(node);
	        		// for backwards compatibility we output the old S and T labels.
	        		if (d > 0) {
	        			System.out.println("S " + newNodeNames.get(node) + " " + d);
	        		}
	        		if (d < 0) {
	        			System.out.println("T " + newNodeNames.get(node) + " " + (-d));
	        		}
	        	}
	        	System.out.println("V " + newNodeNames.get(node) + " " + d + " " + node.getCoord().getX() + " " + node.getCoord().getY());
        	}else{
        		if (this._demands.containsKey(node)){
        			d = this._demands.get(node);
        			//outputs node lables as  V  allways
        			System.out.println("V " + newNodeNames.get(node) + " " + d + " " + node.getCoord().getX() + " " + node.getCoord().getY());
        		}
        	}
        }

        System.out.println("% E from to capacity length");
        for (Link link : this._network.getLinks().values()) {
        	System.out.println("E " + (newNodeNames.get(link.getFromNode())) + " " + (newNodeNames.get(link.getToNode())) + " " + getCapacity(link) + " " + getLength(link));

        }
	}

	private String NameEdge(Link link, int t, HashMap<Node,Integer> nodeNames) {
		if (t < 0) return null;
		if (t + getLength(link) >= this.TimeHorizon) return null;
		return "a#" + nodeNames.get(link.getFromNode()) + "#" +nodeNames.get(link.getToNode()) + "#t" + t;
	}

	private String NameNode(Node node, int t, HashMap<Node,Integer> nodeNames) {
		if (t < 0) return null;
		if (t >= this.TimeHorizon) return null;
		return "n#" + nodeNames.get(node) + "#t" + t;
	}

	private String NameSource(Node node, HashMap<Node,Integer> nodeNames) {
		return "source#" + nodeNames.get(node);
	}

	private String NameSink(Node node, HashMap<Node,Integer> nodeNames) {
		return "sink#" + nodeNames.get(node);
	}

	private String NameSourceLink(Node node, int t, HashMap<Node,Integer> nodeNames) {
		if (t < 0) return null;
		if (t >= this.TimeHorizon) return null;
		return "sourcelink#" + nodeNames.get(node) + "#t" + t;
	}

	private String NameSinklink(Node node, int t, HashMap<Node,Integer> nodeNames) {
		if (t < 0) return null;
		if (t >= this.TimeHorizon) return null;
		return "sinklink#" + nodeNames.get(node) + "#t" + t;
	}

	private String NameSupersink() {
		return "supersink";
	}

	private String NameSupersource() {
		return "supersource";
	}

	private String NameSuperSinkLink(Node node, HashMap<Node,Integer> nodeNames) {
		return "supersinklink#" + nodeNames.get(node);
	}

	private String NameSuperSourceLink(Node node, HashMap<Node,Integer> nodeNames) {
		return "supersourcelink#" + nodeNames.get(node);
	}

	/**
	 * Writes the EAT problem as .lp file for CPLEX etc to standard out
	 * This might be a big file and it includes some useless comments at the top ...
	 */
	public void writeLP() {
		System.out.println("\\ generated from matsim data");
        System.out.println("\\ N " + this._network.getNodes().size());
        System.out.println("\\ TIME " + this.TimeHorizon);
        HashMap<Node,Integer> newNodeNames = new HashMap<Node,Integer>();
        int max = 0;
        for (Node node : this._network.getNodes().values()) {
        	try {
        		int i = Integer.parseInt(node.getId().toString());
        		if (i > 0) 	newNodeNames.put(node,i);
        		if (i > max) max = i;
        	} catch (Exception except) {

        	}
        }

        for (Node node : this._network.getNodes().values()) {
        	try {
        		int i = Integer.parseInt(node.getId().toString());
        		if (i <= 0) {
        		  max += 1;
            	  newNodeNames.put(node, max);
            	  System.out.println("\\ node " + max + " was '" + node.getId()+ "'");
        		}
        	} catch (Exception except) {
        		max += 1;
        		newNodeNames.put(node, max);
        		System.out.println("\\ node " + max + " was '" + node.getId()+ "'");

        	}
        }

        System.out.println("Minimize");
        System.out.println("totaltraveltime: ");

        StringBuilder Sobj = new StringBuilder(" ");
        for (Node node : this._network.getNodes().values()) {
        	int d = 0;
        	if (this._demands.containsKey(node)) {
        		d = this._demands.get(node);
        	}

        	if (d < 0) {
        		for (Link link : node.getOutLinks().values()) {
        			for (int t = 0; t < this.TimeHorizon - this.getLength(link); t++) {
        				String tmp = NameEdge(link, t, newNodeNames);
        				if (tmp != null) {
        					// append " -" + t + " " + tmp
        					Sobj.append(" -");
        				    Sobj.append(t);
        				    Sobj.append(" ");
        				    Sobj.append(tmp);
        				}
        			}
        		}

        		for (Link link : node.getInLinks().values()) {
        			for (int t = 0; t < this.TimeHorizon - this.getLength(link); t++) {
        				String tmp = NameEdge(link, t, newNodeNames);
        				if (tmp != null) {
        					//Sobj += " +" + (t + this.getLength(link)) + " " + tmp;
        					Sobj.append(" + ");
        					Sobj.append((t + this.getLength(link)));
        					Sobj.append(" ");
        					Sobj.append(tmp);
        				}

        			}
        		}


        	}
        }
        System.out.println(Sobj);



        System.out.println("Subject to");


        for (Node node : this._network.getNodes().values()) {
        	int d = 0;
        	if (this._demands.containsKey(node)) {
        		d = this._demands.get(node);
        	}
        	// write flow conservation for each time step
        	for (int t = 0; t < this.TimeHorizon; t++) {
        		System.out.println("flow_conservation#" + newNodeNames.get(node) + "@t" + t + ":");
        		StringBuilder S = new StringBuilder(" ");
        		for (Link link : node.getOutLinks().values()) {
        			String tmp = NameEdge(link, t, newNodeNames);
        			if (tmp != null) {
        				//S += " + " + tmp;
        				S.append(" + ");
        				S.append(tmp);
        			}
        		}
        		for (Link link : node.getInLinks().values()) {
        			String tmp = NameEdge(link, t - this.getLength(link), newNodeNames);
        			if (tmp != null) {
        				// S += " - " + tmp;
        				S.append(" - ");
        				S.append(tmp);
        			}
        		}


        		if (d > 0) {
        			//S += " >= 0";
        			S.append(" >= 0");
        		} else if (d < 0) {
        			//S += " <= 0";
        			S.append(" <= 0");
        		} else {
        			//S += " = 0";
        			S.append(" = 0");
        		}
        		System.out.println(S);
        	}


        	// this could be weaved into the step above ... for a little speedup (<= factor 2)
        	if (d != 0) {
        		if (d > 0) {
        		  System.out.println("supply@" + newNodeNames.get(node) + ":");
        		}  else {
        		  System.out.println("demand@" + newNodeNames.get(node) + ":");
        		}
        		StringBuilder S = new StringBuilder(" ");
        		// write supply / demand constraint

        		for (Link link : node.getOutLinks().values()) {
        			for (int t = 0; t < this.TimeHorizon - this.getLength(link); t++) {
        				String tmp = NameEdge(link, t, newNodeNames);
        				if (tmp != null) {
        					//S += " + " + tmp;
        					S.append(" + ");
            				S.append(tmp);
        				}
        			}
        		}

        		for (Link link : node.getInLinks().values()) {
        			for (int t = 0; t < this.TimeHorizon - this.getLength(link); t++) {
        				String tmp = NameEdge(link, t, newNodeNames);
        				if (tmp != null) {
        					//S += " - " + tmp;
        					S.append(" - ");
            				S.append(tmp);
        				}
        			}
        		}


        		if (d > 0) {
        			//S += " = " + d;;
        			S.append(" = ");
        			S.append(d);
        		} else  { // note: d < 0
        			//S += " >= " + d;
        			S.append(" >= ");
        			S.append(d);
        		}
        		System.out.println(S);
        	}
        }

        //System.out.println("% E from to capacity length");
        System.out.println("Bounds");
        for (Link link : this._network.getLinks().values()) {
        	for (int t = 0; t < this.TimeHorizon; t++) {
        		String tmp = NameEdge(link, t, newNodeNames);
        		if (tmp != null)
        		  System.out.println(" 0 <= " + tmp + " <= " + this.getCapacity(link));
        	}
        }
        System.out.println("End");
	}


	/**
	 * Writes the EAT problem as .net Network file for CPLEX to standard out
	 * This might be a big file and it includes some useless comments at the top ...
	 * @param costOnSinks if true, only the sink links will have non-zero costs, which is an equivalent formulation
	 */
	public void writeNET(boolean costOnSinks) {
		System.out.println("\\ generated from matsim data");
        System.out.println("\\ N " + this._network.getNodes().size());
        System.out.println("\\ TIME " + this.TimeHorizon);
        HashMap<Node,Integer> newNodeNames = new HashMap<Node,Integer>();
        int max = 0;
        for (Node node : this._network.getNodes().values()) {
        	try {
        		int i = Integer.parseInt(node.getId().toString());
        		if (i > 0) 	newNodeNames.put(node,i);
        		if (i > max) max = i;
        	} catch (Exception except) {

        	}
        }

        for (Node node : this._network.getNodes().values()) {
        	try {
        		int i = Integer.parseInt(node.getId().toString());
        		if (i <= 0) {
        		  max += 1;
            	  newNodeNames.put(node, max);
            	  System.out.println("\\ node " + max + " was '" + node.getId()+ "'");
        		}
        	} catch (Exception except) {
        		max += 1;
        		newNodeNames.put(node, max);
        		System.out.println("\\ node " + max + " was '" + node.getId()+ "'");

        	}
        }

        System.out.println("MINIMIZE NETWORK frommatsim");

        System.out.println("SUPPLY");
        System.out.println(NameSupersource() + " : " + this._totaldemandsources);
        // we need an transshipment! and sink demands are just upper bounds anyway
        System.out.println(NameSupersink() + " : " + (- this._totaldemandsources));

        System.out.println("ARCS");

        // the time-expanded arcs
        for (Link link : this._network.getLinks().values()) {
        	Interval when = null;
        	int low = 0;
        	int high = this.TimeHorizon;
        	if (this.whenAvailable != null) {
        		when = this.whenAvailable.get(link.getId());
        		if (when != null) {
        			low = when.getLowBound();
        			high = when.getHighBound();
        		}
        	}

        	for (int t = low; t < high; t++) {
        		StringBuilder S = new StringBuilder();
        		String tmp = NameEdge(link, t, newNodeNames);
        		if (tmp != null) {
        			S.append(tmp + " : " + NameNode(link.getFromNode(), t, newNodeNames));
        			S.append(" -> " + NameNode(link.getToNode(), t + this.getLength(link), newNodeNames));
        		}
        		System.out.println(S);
        	}
        }

        // the arcs from the virtual sources to the source node
        // and from the sink nodes to the virtual sinks
        for (Node node : this._network.getNodes().values()) {
        	int d = 0;
        	if (this._demands.containsKey(node)) {
        		d = this._demands.get(node);
        	}
        	if (d == 0)
        		continue;

        	if (d < 0) {
        		for (int t = 0; t < this.TimeHorizon; t++) {
        			StringBuilder sb = new StringBuilder();
        		    sb.append(NameSinklink(node, t, newNodeNames));
        		    sb.append(" : " + NameNode(node, t, newNodeNames));
        		    sb.append(" -> " + NameSink(node, newNodeNames));
        		    System.out.println(sb);
        		}
        	} else {
        		for (int t = 0; t < this.TimeHorizon; t++) {
        			StringBuilder sb = new StringBuilder();
        		    sb.append(NameSourceLink(node, t, newNodeNames));
        		    sb.append(" : " + NameSource(node, newNodeNames));
        		    sb.append(" -> " + NameNode(node, t, newNodeNames));
        		    System.out.println(sb);
        		}
        	}
        }


        // the arcs from the supersource/supersink to the virtual sources/sinks
        for (Node node : this._network.getNodes().values()) {
        	int d = 0;
        	if (this._demands.containsKey(node)) {
        		d = this._demands.get(node);
        	}
        	if (d == 0)
        		continue;

        	StringBuilder sb = new StringBuilder();
        	if (d < 0) {
        		sb.append(NameSuperSinkLink(node, newNodeNames));
        		sb.append(" : " + NameSink(node, newNodeNames));
        		sb.append(" -> " + NameSupersink());
        	} else {
        		sb.append(NameSuperSourceLink(node, newNodeNames));
        		sb.append(" : " + NameSupersource());
        		sb.append(" -> " + NameSource(node, newNodeNames));
        	}
            System.out.println(sb);
        }


        System.out.println("OBJECTIVE");

        // there are two options here
        // put all weights on the sinklinks
        // or give every link a weight equal to its length (except sinklinks)

        if (costOnSinks) {
        	// the sinklinks have their departure time as cost
        	for (Node node : this._network.getNodes().values()) {
        		int d = 0;
        		if (this._demands.containsKey(node)) {
        			d = this._demands.get(node);
        		}
        		if (d < 0) {
        			for (int t = 0; t < this.TimeHorizon; t++) {
        				StringBuilder sb = new StringBuilder();
        				sb.append(NameSinklink(node, t, newNodeNames));
        				sb.append(" : " + t);
        				System.out.println(sb);
        			}
        		}
        	}
        } else {
        	 // the time-expanded arcs have their length as cost
       	
            for (Link link : this._network.getLinks().values()) {
            	Interval when = null;
            	int low = 0;
            	int high = this.TimeHorizon;
            	if (this.whenAvailable != null) {
            		when = this.whenAvailable.get(link.getId());
            		if (when != null) {
            			low = when.getLowBound();
            			high = when.getHighBound();
            		}
            	}

            	for (int t = low; t < high; t++) {
            		StringBuilder S = new StringBuilder();
            		String tmp = NameEdge(link, t, newNodeNames);
            		if (tmp != null) {
            			S.append(tmp + " : " + this.getLength(link));
            		}
            		System.out.println(S);
            	}
            }

            // the arcs from the virtual sources to the source node have their arrival time as cost
            for (Node node : this._network.getNodes().values()) {
            	int d = 0;
            	if (this._demands.containsKey(node)) {
            		d = this._demands.get(node);
            	}
            	if (d > 0) {
            		for (int t = 0; t < this.TimeHorizon; t++) {
            			StringBuilder sb = new StringBuilder();
            		    sb.append(NameSourceLink(node, t, newNodeNames));
            		    sb.append(" : " + t);
            		    System.out.println(sb);
            		}
            	}
            }
        }

        System.out.println("BOUNDS");

        for (Link link : this._network.getLinks().values()) {
        	Interval when = null;
        	int low = 0;
        	int high = this.TimeHorizon;
        	if (this.whenAvailable != null) {
        		when = this.whenAvailable.get(link.getId());
        		if (when != null) {
        			low = when.getLowBound();
        			high = when.getHighBound();
        		}
        	}

        	for (int t = low; t < high; t++) {
        		String tmp = NameEdge(link, t, newNodeNames);
        		if (tmp != null) {
        	  	  System.out.println(" 0 <= " + tmp + " <= " + this.getCapacity(link));
        		}
        	}
        }

        // source/sink links are left free ... dunno if this matters

        // the arcs from the supersource/supersink to the virtual sources/sinks have
        // capacity equal to the demands there
        for (Node node : this._network.getNodes().values()) {
        	int d = 0;
        	if (this._demands.containsKey(node)) {
        		d = this._demands.get(node);
        	}
        	if (d == 0)
        		continue;

        	StringBuilder sb = new StringBuilder();
        	sb.append("0 <= ");
        	if (d < 0) {
        		sb.append(NameSuperSinkLink(node, newNodeNames));
        		sb.append(" <= " + (-d));
        	} else {
        		sb.append(NameSuperSourceLink(node, newNodeNames));
        		sb.append(" <= " + d);
        	}
            System.out.println(sb);
        }


        System.out.println("ENDNETWORK");
	}

	/**
	 * Writes a single-sink EAF problem as .lod network file for LODYFA EAF to standard out
	 * This might be a big file.
	 * The network looks like this:
	 * supersource s* -> source s with capacity == demand but only at t = 0
	 * source s has links with travel time 0 to the real node and infinite holdover
	 * No holdover anywhere else.
	 * The sink nodes have travel time 0 directly to the supersink and there is no intermediate node.
	 */
	public void writeLodyfa() {
		HashMap<Node,Integer> newNodeNames = new HashMap<Node,Integer>();

		// - 1 seems to be what Lodyfa does ...T = 10 gives 11 time steps there
		System.out.println(this.TimeHorizon - 1);

		// the supersource has to be 0, the supersink n - 1
		// We give up on keeping names here.
        int nnodes = this._network.getNodes().size();

        // We always want a supersource so that we can limit the supply,
        // even if there is just one source.
        // We need additional source nodes, as well.
        nnodes += this._numsources + 1;

        // do we need a supersink?
        if (this._numsinks > 1) {
        	nnodes++;
        }

        System.out.println(nnodes);

        // skip the supersource
        int currentnode = 1;

        for (Node node : this._network.getNodes().values()) {
        	Integer d = this._demands.get(node);
        	if (this._numsinks == 1 && d != null && d < 0) {
        		// this is the only sink! make it the supersink
        		newNodeNames.put(node, nnodes - 1);
        	} else {
        		newNodeNames.put(node, currentnode);
        		currentnode++;
        	}
        }

        int narcs = this._network.getLinks().size();

        // two more for each source
        narcs += 2 * this._numsources;

    	// and 1 more for each sink, if we need a supersink
        if (this._numsinks > 1) {
        	narcs += this._numsinks;
        }

        System.out.println(narcs);

        // the time-expanded arcs
        for (Link link : this._network.getLinks().values()) {
        	StringBuilder S = new StringBuilder();
        	S.append(newNodeNames.get(link.getFromNode()));
        	S.append("\t");
        	S.append(newNodeNames.get(link.getToNode()));
        	S.append("\t");

        	int l = getLength(link);
        	int c = getCapacity(link);

        	String constant = l + "\t" + c + "\t";

        	for (int t = 0; t < this.TimeHorizon; t++) {
        		S.append(constant);
        	}
    		System.out.println(S);
        }


        int sourcesstartat = currentnode;

        // the arcs from and to the virtual sources
        // and to the supersink
        for (Node node : this._network.getNodes().values()) {
        	int d = 0;
        	if (this._demands.containsKey(node)) {
        		d = this._demands.get(node);
        	}
        	if (d == 0)
        		continue;


        	if (d < 0) {
        		// sinks

        		// nothing to do if there is no supersink
        		if (this._numsinks == 1) continue;

        		StringBuilder sb = new StringBuilder();
            	sb.append(newNodeNames.get(node));
            	sb.append("\t");
            	sb.append(nnodes - 1); // the supersink
            	sb.append("\t");

            	String constant = "0\t654321\t"; // hopefully enough capacity ...

        		for (int t = 0; t < this.TimeHorizon; t++) {
        			sb.append(constant);
        		}
    		    System.out.println(sb);

        	} else {
        		StringBuilder sb = new StringBuilder();
        		// the link from the supersource
        		sb.append("0\t");
        		sb.append(currentnode); // to the virtual source

        		sb.append("\t0\t");
        		sb.append(d);
        		sb.append("\t");

        		String constant = "0\t0\t"; // link disappears

        		// NB: start is t = 1
        		for (int t = 1; t < this.TimeHorizon; t++) {
        			sb.append(constant);
        		}

        		System.out.println(sb);

        		sb = new StringBuilder();
        		sb.append(currentnode);
        		sb.append("\t");
        		sb.append(newNodeNames.get(node));
        		sb.append("\t");

        		constant = "0\t654321\t"; // hopefully enough capacity ...

        		for (int t = 0; t < this.TimeHorizon; t++) {
        			sb.append(constant);
        		}

        		System.out.println(sb);

        		// we created a node
        		currentnode++;
        	}
        }

        // create holdover ... only at the virtual sources
        // NB: node 0 and node n - 1 must be omitted!

        for (int i = 1; i < sourcesstartat; i++) {
        	StringBuilder sb = new StringBuilder();
        	String constant = "0\t0\t";
        	for (int t = 0; t < this.TimeHorizon; t++) {
    			sb.append(constant);
    		}
        	System.out.println(sb);
        }

        for (int i = sourcesstartat; i < nnodes - 1; i++) {
        	StringBuilder sb = new StringBuilder();
        	String constant = "654321\t654321\t";
        	for (int t = 0; t < this.TimeHorizon; t++) {
    			sb.append(constant);
    		}
        	System.out.println(sb);
        }

	}
	
	
	
    public String readConfig(String contents) {
    	String lines[] = contents.split("\n");
    	String error = "";
    	
    	int i = 0;
    	while (i < lines.length) {
    		String s = lines[i++];
    		s = s.trim().toLowerCase();
    		
    		String t = "";
    		if (i < lines.length) {   
    			t = lines[i++];
    			t = t.trim().toLowerCase(); // would be bad for filenames etc
    		}
    		
    	    if (s.equals("--timestep")) {
    	    	timeStep = Double.parseDouble(t);
    	    } else if (s.equals("--flowfactor")) {
    	    	flowFactor = Double.parseDouble(t);
    	    } else if (s.equals("--mintraveltime")) {
    	    	minTravelTime  = Integer.parseInt(t);
    	    } else if (s.equals("--addtraveltime")) {
    	    	addTravelTime  = Integer.parseInt(t);
    	    } else if (s.equals("--timehorizon")) {
    	    	TimeHorizon = Integer.parseInt(t);
    	    } else if (s.equals("--maxrounds")) {
    	    	MaxRounds = Integer.parseInt(t);
    	    } else if (s.equals("--usesinkcapacities")) {
    	    	useSinkCapacities = Boolean.parseBoolean(t);
    	    } else if (s.equals("--searchalgo")) {
    	    	searchAlgo = Integer.parseInt(t);
    	    } else if (s.equals("--usepriorityqueue")) {
    	    	usePriorityQueue  = Boolean.parseBoolean(t);    	    	
    	    } else if (s.equals("--usevertexcleanup")) {
    	    	useVertexCleanup  = Boolean.parseBoolean(t);
    	    } else if (s.equals("--useimplicitvertexcleanup")) {
    	    	useImplicitVertexCleanup  = Boolean.parseBoolean(t);
    	    } else if (s.equals("--useshadowflow")) {
    	    	useShadowFlow  = Boolean.parseBoolean(t);
    	    } else if (s.equals("--checkconsistency")) {
    	    	checkConsistency  = Integer.parseInt(t);
    	    } else if (s.equals("--checkTouchedNodes")) {
    	    	checkTouchedNodes  = Boolean.parseBoolean(t);
    	    } else if (s.equals("--dogarbagecollection")) {
    	    	doGarbageCollection  = Integer.parseInt(t);
    	    } else if (s.equals("--sortpathsbeforeaugmenting")) {
    	    	sortPathsBeforeAugmenting  = Boolean.parseBoolean(t);
    	    } else if (s.equals("--keeppaths")) {
    	    	keepPaths  = Boolean.parseBoolean(t);
    	    } else if (s.equals("--unfoldpaths")) {
    	    	unfoldPaths  = Boolean.parseBoolean(t);
    	    } else if (s.equals("--maplinkstotep")) {
    	    	mapLinksToTEP  = Boolean.parseBoolean(t);
    	    } else if (s.equals("--userepeatedpaths")) {
    	    	useRepeatedPaths  = Boolean.parseBoolean(t);
    	    } else if (s.equals("--quickcutoff")) {
    	    	quickCutOff  = Double.parseDouble(t);
    	    } else if (s.equals("--delaysinkpropagation")) {
    	    	delaySinkPropagation  = Boolean.parseBoolean(t);
    	    } else if (s.equals("--useholdover")) {
    	    	useHoldover  = Boolean.parseBoolean(t);
    	    } else if (s.equals("--useholdovercapacities")) {
    	    	useHoldoverCapacities  = Boolean.parseBoolean(t);
    	    } else if (s.equals("--trackunreachable")) {
    	    	trackUnreachableVertices  = Boolean.parseBoolean(t);
    	    } else {
    	    	error += "Unknown option: " + s + "\n";
    	    }
    	}
    	return error;
	}
    
    public String writeConfig() {
    	String s = "";
    	s += "--timestep\n" + timeStep + "\n";
    	s += "--flowfactor\n" + flowFactor + "\n";
    	s += "--mintraveltime\n" + minTravelTime + "\n"; 
    	s += "--addtraveltime\n" + addTravelTime + "\n";

    	s += "--timehorizon\n" + TimeHorizon + "\n";
    	s += "--maxrounds\n" + MaxRounds + "\n";

    	s += "--useSinkCapacities\n" + useSinkCapacities + "\n";
    	s += "--searchAlgo\n" + searchAlgo + "\n";
        s += "--usepriorityqueue\n" + usePriorityQueue + "\n";
    	s += "--usevertexcleanup\n" + useVertexCleanup + "\n";
    	
    	s += "--useimplicitvertexcleanup\n" + useImplicitVertexCleanup + "\n";
    	s += "--useshadowflow\n" + useShadowFlow + "\n";
    	s += "--checkconsistency\n" + checkConsistency + "\n";
    	s += "--checkTouchedNodes\n" + checkTouchedNodes + "\n";
        s += "--dogarbagecollection\n" + doGarbageCollection + "\n";

    	s += "--sortpathsbeforeaugmenting\n" + sortPathsBeforeAugmenting + "\n";
    	s += "--keeppaths\n" + keepPaths + "\n";
    	s += "--unfoldpaths\n" + unfoldPaths + "\n";
    	s += "--maplinkstotep\n" + mapLinksToTEP + "\n";
    	s += "--userepeatedpaths\n" + useRepeatedPaths + "\n";
    	s += "--quickcutoff\n" + quickCutOff + "\n";
    	s += "--delaysinkpropagation\n" + delaySinkPropagation  + "\n"; 
    	s += "--useholdover\n" + useHoldover + "\n";
    	s += "--useholdovercapacities\n" + useHoldoverCapacities + "\n";

    	s += "--trackunreachable\n" + trackUnreachableVertices + "\n";
    	return s;
    }

}
