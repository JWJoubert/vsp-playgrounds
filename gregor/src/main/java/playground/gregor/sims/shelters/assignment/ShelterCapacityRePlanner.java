/* *********************************************************************** *
 * project: org.matsim.*
 * ShelterCapacityRePlanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.sims.shelters.assignment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.TimeVariantLinkImpl;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.evacuation.base.Building;

public class ShelterCapacityRePlanner   implements IterationStartsListener{

	private static final Logger log = Logger.getLogger(ShelterCapacityRePlanner.class);
	
	private ScenarioImpl sc;
	private Dijkstra router;

	private List<ShelterInfo> shelters = new ArrayList<ShelterInfo>();


	private final Node saveNode;
	private final Id saveLinkId = new IdImpl("el1");

	private NetworkFactoryImpl routeFactory;
	private ShelterCounter shc;
	private TravelTimeTimeMachine timeMachine;
	private TravelTime tt;

	private Map<Id,ArrayList<PersonInfo>>  asm = new LinkedHashMap<Id, ArrayList<PersonInfo>>();

	//TODO think about making config parameter out of this magic numbers
	private static final double FULL_RED_BOUNDERY = 600; //seconds
	private static final double MAX_SHIFT = 0.05;
	private double reserveCap = 10;

	public ShelterCapacityRePlanner(ScenarioImpl sc, TravelCost tc, TravelTime tt, List<Building> buildings,ShelterCounter shc)  { //, double pshelter) {

		this.timeMachine = new TravelTimeTimeMachine(tt, tc);
		this.tt = tt;
		this.router =  new Dijkstra(sc.getNetwork(),this.timeMachine,this.timeMachine);
		this.sc = sc;

		this.routeFactory = (NetworkFactoryImpl) sc.getNetwork().getFactory();
		this.shc = shc;//
		this.saveNode = this.sc.getNetwork().getNodes().get(new IdImpl("en1"));
		initShelters(buildings);
		this.reserveCap = (int) (Math.max(1, this.sc.getConfig().evacuation().getSampleSize() * this.reserveCap)+.5);
	}

	private void initShelters(List<Building> buildings) {
		for (Building b : buildings) {
			if (b.getShelterSpace() > 0 && b.isQuakeProof() && !b.getId().toString().equals("super_shelter")) {
				ShelterInfo si = new ShelterInfo();
				si.reserveCapacity = 1;
				si.id = this.shc.getReversMapping().get(b);
				this.shelters.add(si);
				this.asm.put(si.id, new ArrayList<PersonInfo>());
			}
		}

	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (event.getIteration() > 1 ) {
			reset();
			this.shc.printStats();
			runIII();
			reset();
			this.shc.printStats();
		}

	}

	private void runIII() {
		generateAgentShelterMapping();
		calculateSheltersOvercapacity();
		int cap = reduceOvercapacity();
		log.info("Shifted Agent:" + cap + "  this corresponds to:" + ((int)((0.5+ 100. * (double)cap/this.sc.getPopulation().getPersons().size()))) + "% of the population");
		increaseCapactiy();
		
		
	}
	

	private void increaseCapactiy() {
		for (ShelterInfo si : this.shelters) {
			if (!si.on) {
				continue;
			}
			int totalOvercap = si.freeSpace+si.overCapacityDeltT;
			if ( totalOvercap < si.reserveCapacity && si.avgDeltaT < 0) {
				int currentCap = this.shc.getShelter(si.id).getShelterSpace();
				int incr = (int)Math.max((currentCap * MAX_SHIFT),reserveCap);
				increaseShelterCap(incr, si.id);
			}
			
			int free = this.shc.getShelterFreeSpace(si.id); 
			if (free < this.reserveCap) {
				increaseShelterCap((int) (this.reserveCap - free), si.id);
			}
			
		}
		
	}

	private void reset() {
		this.shc.reset(1, this.sc.getPopulation().getPersons().values());
	}



	private void increaseShelterCap(int amount, Id key) {
		this.shc.changeCapacity(key, amount);
	}

	private int reduceOvercapacity() {
		int cap = 0;
		for (ShelterInfo si : this.shelters) {
			int totalOvercap = si.freeSpace+si.overCapacityDeltT;
			if (totalOvercap > 0){
				
				//shelters capacity is to be reduced at most by a fraction of MAX_SHIFT 
				int currentCap = this.shc.getShelter(si.id).getShelterSpace();
				int reduction = (int) Math.max(1, Math.min(totalOvercap , currentCap*MAX_SHIFT));
				if (reduction < this.reserveCap) {
					si.on = false;
				}
				cap += reduction;
				
				int freeSpaceRed = Math.min(si.freeSpace,reduction);
				if (freeSpaceRed > 0) {
					this.shc.changeCapacity(si.id, -freeSpaceRed);
				}
				reduction -= freeSpaceRed;
				
				if (reduction > 0) {
					reduceShelterCap(reduction, si.id);
				}
			}
		}
		return cap;
	}

	private void reduceShelterCap(int amount, Id key) {
		ArrayList<PersonInfo> l = this.asm.get(key);
		if (l.size() < amount) {
			for (PersonInfo pi : l) {
				reRoute(pi.pers);		
			}
		} else {
			Queue<PersonInfo> pers = new PriorityQueue<PersonInfo>(l);
			for (int i = 0; i < amount; i++) {
				PersonInfo pi = pers.poll();
				reRoute(pi.pers);
			}
		}
		this.shc.changeCapacity(key, -amount);
	}

	private void calculateSheltersOvercapacity() {
		for (ShelterInfo si : this.shelters) {
			if (this.shc.getShelter(si.id).getShelterSpace() > 0) {
				si.on = true;
			}
			si.avgDeltaT = 0;
			si.freeSpace = 0;
			si.overCapacityDeltT = 0;
			ArrayList<PersonInfo> persList = this.asm.get(si.id);
			for (PersonInfo pi : persList) {
				calculateDeltaT(pi);
				si.avgDeltaT += pi.deltaT;
				if (pi.deltaT > 0) {
					si.overCapacityDeltT++;
				}
			}
			si.freeSpace += this.shc.getShelter(si.id).getShelterSpace() - persList.size();
			si.avgDeltaT /= persList.size();
		}
	}

	private void calculateDeltaT(PersonInfo pi) {
		Person p = pi.pers;
		Activity act = ((Activity)p.getSelectedPlan().getPlanElements().get(0));
		Id linkId = act.getLinkId();
		Node from = this.sc.getNetwork().getLinks().get(linkId).getToNode();
		Path path = this.router.calcLeastCostPath(from,this.saveNode, act.getEndTime());
		double deltaT = this.timeMachine.getTimeOffsetAndReset();
		if (deltaT == 0) {
			deltaT = getPositiveDeltaT(act.getEndTime(),path);
		}
		pi.deltaT = deltaT;

	}

	private void generateAgentShelterMapping() {
		for (ArrayList<PersonInfo> l : this.asm.values()) {
			l.clear();
		}
		for (Person p : this.sc.getPopulation().getPersons().values()) {
			Id id = ((Activity)p.getSelectedPlan().getPlanElements().get(2)).getLinkId();
			if (id.toString().equals(this.saveLinkId.toString())) {
				continue;
			}
			ArrayList<PersonInfo> list = asm.get(id);
			if (list == null) {
				list = new ArrayList<PersonInfo>();
				asm.put(id, list);
			}
			PersonInfo pi = new PersonInfo();
			pi.pers = p;
			list.add(pi);	
		}		
	}


	private void reRoute(Person pers) {
		((PersonImpl)pers).removeUnselectedPlans();
		Plan plan = pers.getSelectedPlan();
		Activity actA = ((Activity)plan.getPlanElements().get(0));
		Leg leg = ((Leg)plan.getPlanElements().get(1));
		Activity actB = ((Activity)plan.getPlanElements().get(2));
		Node from = this.sc.getNetwork().getLinks().get(actA.getLinkId()).getToNode();
		Path path = this.router.calcLeastCostPath(from, this.saveNode, actA.getEndTime());

		//reset time machine
		double time = this.timeMachine.getTimeOffsetAndReset();

		if (time < 0 || path.travelTime > 120*60) {
			throw new RuntimeException("not a valid path");
		}

		this.shc.rm(actB.getLinkId());
		NetworkRoute route = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, actA.getLinkId(), this.saveLinkId);
		route.setLinkIds(actA.getLinkId(), NetworkUtils.getLinkIds(path.links), this.saveLinkId);
		route.setTravelTime((int) path.travelTime);
		route.setTravelCost(path.travelCost);
		route.setDistance(RouteUtils.calcDistance(route, this.sc.getNetwork()));
		leg.setRoute(route);
		((ActivityImpl)actB).setLinkId(this.saveLinkId);
		double testScore = path.travelCost / -600.;
		plan.setScore(testScore);
		this.shc.testAdd(saveLinkId);
	}

	
	private double getPositiveDeltaT(double startTime, Path path) {
		double currentTime =  startTime;
		double minDelta = FULL_RED_BOUNDERY;
		for (Link l : path.links) {
			TreeMap<Double, NetworkChangeEvent> ce = ((TimeVariantLinkImpl)l).getChangeEvents();
			if (ce == null) {
				continue;
			}
			for (Entry<Double, NetworkChangeEvent>  entr : ce.entrySet()) {
				ChangeValue v = entr.getValue().getFreespeedChange();
				if (v != null && v.getValue() == 0.) {
					double delta = entr.getKey() - currentTime;
	
//					//BEGIN DEBUG
//					if (delta < 0) {
//						throw new RuntimeException("if and only if delta < 0  then deltaT < 0");
//					}
//					//END DEBUG
					
					if (delta < minDelta) {
						minDelta = delta;
					}

				}
			}
			currentTime += this.tt.getLinkTravelTime(l, currentTime);
		}
		//The agent does not start inside the inundation area
		//This means the deltaT would have to be set to Double.POSITIVE_INFINITY however this would have 
		//unwanted side effects!
		return minDelta;
	}

	private static class TravelTimeTimeMachine implements TravelTime, TravelCost {

		double timeOffset = 0;

		private TravelTime tt;
		private TravelCost tc;

		public TravelTimeTimeMachine(TravelTime tt, TravelCost tc) {
			this.tt = tt;
			this.tc = tc;
		}

		@Override
		public double getLinkTravelTime(Link link, double time) {
			while (Double.isInfinite(this.tt.getLinkTravelTime(link, time + timeOffset))) {
				this.timeOffset -= 60;
			}
			return this.tt.getLinkTravelTime(link, time + timeOffset);
		}

		@Override
		public double getLinkTravelCost(Link link, double time) {
			while (Double.isInfinite(this.tc.getLinkTravelCost(link, time + timeOffset))) {
				this.timeOffset -= 60;
			}
			return this.tc.getLinkTravelCost(link, time + timeOffset);
		}

		public double getTimeOffsetAndReset() {
			double ret = this.timeOffset;
			this.timeOffset = 0;
			return ret;
		}
	}


	private static class PersonInfo implements Comparable<PersonInfo> {
		Person pers;
		double deltaT;
		@Override
		public int compareTo(PersonInfo o) {
			if (this.deltaT > o.deltaT) {
				return -1;
			} else if (this.deltaT < o.deltaT) {
				return 1;
			}
			return 0;
		}

	}
	private static class ShelterInfo {
	public boolean on;
	Id id;
		double avgDeltaT;
		int overCapacityDeltT = 0;
		int freeSpace = 0;
		int reserveCapacity;
	}

}

