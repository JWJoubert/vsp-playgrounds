package playground.sergioo.scheduling2013;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.sergioo.passivePlanning2012.core.population.PlaceSharer;
import playground.sergioo.passivePlanning2012.core.population.PlaceSharer.KnownPlace;
import playground.sergioo.passivePlanning2012.core.population.agenda.Agenda;
import playground.sergioo.passivePlanning2012.core.population.agenda.Agenda.AgendaElement;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager.CurrentTime;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager.MobsimStatus;
//import playground.sergioo.scheduling2013.gui.MainApplet;

public class SchedulingNetwork implements Network {

	//Predecessor
	
	public class SchedulingNode extends NodeImpl implements Node {
		
		private double time;
		private List<SchedulingLink> path;
		private double utility = -Double.MAX_VALUE;
		private final double maxUtilityFrom;
		
		public SchedulingNode(Id id, Coord coord, double time, double maxUtilityFrom) {
			super(id, coord);
			this.time = time;
			this.maxUtilityFrom = maxUtilityFrom;
		}
		
		public double getTime() {
			return time;
		}
		public double getMaxUtilityFrom() {
			return maxUtilityFrom;
		}
		public void updatePath(List<SchedulingLink> path, double utility) {
			if(this.utility<utility) {
				this.path = new ArrayList<SchedulingNetwork.SchedulingLink>(path);
				this.utility = utility;
				currentMaximumUtility = utility;
				//System.out.println(path+"("+utility+")");
			}
		}

	}
	
	public abstract class SchedulingLink implements Link {
	
		private final Id id;
		private Node fromNode;
		private Node toNode;
		double duration;
	
		public SchedulingLink(Id id, Node fromNode, Node toNode, double duration) {
			super();
			this.id = id;
			this.fromNode = fromNode;
			this.toNode = toNode;
			this.duration = duration;
		}
	
		public double getDuration() {
			return duration; 
		}
		@Override
		public Coord getCoord() {
			return CoordUtils.getCenter(fromNode.getCoord(), toNode.getCoord());
		}
		@Override
		public Id getId() {
			return id;
		}
		@Override
		public boolean setFromNode(Node node) {
			if(node==null)
				return false;
			this.fromNode = node;
			return true;
		}
		@Override
		public boolean setToNode(Node node) {
			if(node==null)
				return false;
			this.toNode = node;
			return true;
		}
		@Override
		public Node getToNode() {
			return toNode;
		}
		@Override
		public Node getFromNode() {
			return fromNode;
		}
		@Override
		public double getLength() {
			return 0;
		}
		@Override
		public double getNumberOfLanes() {
			return 0;
		}
		@Override
		public double getNumberOfLanes(double time) {
			return 0;
		}
		@Override
		public double getFreespeed() {
			return 0;
		}
		@Override
		public double getFreespeed(double time) {
			return 0;
		}
		@Override
		public double getCapacity() {
			return 0;
		}
		@Override
		public double getCapacity(double time) {
			return 0;
		}
		@Override
		public void setFreespeed(double freespeed) {
			
		}
		@Override
		public void setLength(double length) {
			
		}
		@Override
		public void setNumberOfLanes(double lanes) {
			
		}
		@Override
		public void setCapacity(double capacity) {
			
		}
		@Override
		public void setAllowedModes(Set<String> modes) {
			
		}
		@Override
		public Set<String> getAllowedModes() {
			return null;
		}
	
	}
	
	public class ActivitySchedulingLink extends SchedulingLink {
	
		private final String activityType;
		private final Id facilityId;
		
		public ActivitySchedulingLink(Id id, Node fromNode, Node toNode, String activityType, Id facilityId) {
			super(id, fromNode, toNode, ((SchedulingNode)toNode).time-((SchedulingNode)fromNode).time);
			this.activityType = activityType;
			this.facilityId = facilityId;
		}
	
		public String getActivityType() {
			return activityType;
		}
		public Id getFacilityId() {
			return facilityId;
		}
		@Override
		public String toString() {
			return "("+activityType+")"+((SchedulingNode)getToNode()).time;
		}
	
	}
	public class JourneySchedulingLink extends SchedulingLink {
	
		private String mode;
	
		public JourneySchedulingLink(Id id, Node fromNode, Node toNode, String mode, double duration) {
			super(id, fromNode, toNode, duration);
			this.mode = mode;
		}
	
		public String getMode() {
			return mode;
		}
		@Override
		public String toString() {
			return mode+","+((SchedulingNode)getToNode()).time;
		}
	
	}

	private static final Map<String, Double> FACTORS = new HashMap<String, Double>();
	{
		FACTORS.put("car", 2.0);
		FACTORS.put("pt", 2.0);
		FACTORS.put("walk", 1.0);
	}

	private static final double MAX_ACTIVITY_UTILITY = 70.0/3600;
	private static final double MAX_FREQUENCY = 1.0/3600;
	
	private static final Map<String, Double> SPEEDS = new HashMap<String, Double>();
	private static final int MAX_PLACES = 22;
	{
		SPEEDS.put("car", 15.0);
		SPEEDS.put("pt", 9.0);
		SPEEDS.put("walk", 1.2);
	}
	
	private NetworkImpl delegate = NetworkImpl.createNetwork();
	private CurrentTime now;
	private double fastest;
	private List<Tuple<String, Tuple<Double, Double>>> previousActivities;
	private List<Tuple<String, Tuple<Double, Double>>> followingActivities;
	private double endTime;
	private String lastActivity;
	private int timeInterval;
	private Id destinationId;
	private ActivityFacilities facilities;
	private double currentMaximumUtility = 0;
	private boolean withMaximumDepth = true;
	private int maximumDepth = Integer.MAX_VALUE;
	
	
	@Override
	public NetworkFactory getFactory() {
		return delegate.getFactory();
	}

	@Override
	public Map<Id<Node>, ? extends Node> getNodes() {
		return delegate.getNodes();
	}

	@Override
	public Map<Id<Link>, ? extends Link> getLinks() {
		return delegate.getLinks();
	}

	@Override
	public double getCapacityPeriod() {
		return delegate.getCapacityPeriod();
	}

	@Override
	public double getEffectiveLaneWidth() {
		return delegate.getEffectiveLaneWidth();
	}

	@Override
	public void addNode(final Node nn) {
		delegate.addNode(nn);
	}

	@Override
	public void addLink(Link ll) {
		delegate.addLink(ll);
	}

	@Override
	public Node removeNode(Id nodeId) {
		return delegate.removeNode(nodeId);
	}

	@Override
	public Link removeLink(Id linkId) {
		return delegate.removeLink(linkId);
	}
	
	public List<SchedulingLink> createNetwork(CurrentTime now, ActivityFacilities facilities, Id originId, Id destinationId,
			double endTime, int timeInterval, Set<String> modes, PlaceSharer placeSharer, Agenda agenda,
			List<Tuple<String, Tuple<Double, Double>>> previousActivities, List<Tuple<String, Tuple<Double, Double>>> followingActivities,
			final MobsimStatus mobsimStatus) {
		//Create spatiotemporal nodes
		/*if(endTime-previousActivities.get(previousActivities.size()-1).getSecond()>8*3600)
			System.out.println();*/
		this.previousActivities = previousActivities;
		this.followingActivities = followingActivities;
		this.lastActivity = followingActivities.get(0).getFirst();
		this.timeInterval = timeInterval;
		this.destinationId = destinationId;
		this.facilities = facilities;
		this.now = now;
		this.endTime = (int) (endTime-endTime%timeInterval);
		Coord destination = facilities.getFacilities().get(destinationId).getCoord();
		SchedulingNode lastNode = new SchedulingNode(new IdImpl(destinationId.toString()+"("+(int)this.endTime+")"), destination, this.endTime, 0);
		addNode(lastNode);
		if(!placeSharer.getKnownPlace(destinationId).getActivityTypes(this.endTime).contains(lastActivity))
			return null;
		Coord origin = facilities.getFacilities().get(originId).getCoord();
		Map<Id, KnownPlace> knownPlaces = new HashMap<Id, KnownPlace>();
		placeSharer.setAreKnownPlacesUsed(true);
		for(KnownPlace knownPlace:placeSharer.getKnownPlaces())
			if(MatsimRandom.getRandom().nextInt(placeSharer.getKnownPlaces().size())<MAX_PLACES)
				knownPlaces.put(knownPlace.getFacilityId(), knownPlace);
		placeSharer.setAreKnownPlacesUsed(false);
		knownPlaces.put(originId, placeSharer.getKnownPlace(originId));
		Tuple<Double, Double> times = previousActivities.get(previousActivities.size()-1).getSecond();
		double timeOfDay = times.getFirst(), previousTime = timeOfDay-times.getSecond();
		if(timeOfDay>=this.endTime)
			return null;
		previousTime -= previousTime%timeInterval;
		timeOfDay -= timeOfDay%timeInterval;
		//this.maximumDepth = (int) Math.sqrt(((endTime-timeOfDay)*MAX_FREQUENCY));
		this.maximumDepth = (int)((endTime-timeOfDay)*MAX_FREQUENCY);
		SchedulingNode previousNode = new SchedulingNode(new IdImpl(originId.toString()+"("+(int)timeOfDay+")"), origin, timeOfDay, getMaxActivityUtility(this.endTime-timeOfDay));
		addNode(previousNode);
		SchedulingNode fromNode = previousNode;
		List<SchedulingLink> path = new ArrayList<SchedulingLink>();
		previousNode = new SchedulingNode(new IdImpl(originId.toString()+"("+(int)previousTime+")"), origin, previousTime, getMaxActivityUtility(this.endTime-previousTime));
		path.add(new ActivitySchedulingLink(new IdImpl(previousActivities.get(previousActivities.size()-1).getFirst()+","+originId+"("+previousTime+"-"+(int)timeOfDay+")"), previousNode, fromNode, previousActivities.get(previousActivities.size()-1).getFirst(), originId));
		previousActivities.remove(previousActivities.size()-1);
		KnownPlace place = knownPlaces.get(originId);
		fastest = Double.MAX_VALUE;
		for(String mode:modes) {
			double timeMode = place.getTravelTime(mode, fromNode.time, destinationId);
			if(timeMode>0 && fastest>timeMode)
				fastest = timeMode;
		}
		if(fastest==Double.MAX_VALUE) {
			String mode = modes.contains("car")?"car":"pt";
			fastest = CoordUtils.calcDistance(origin, destination)*FACTORS.get(mode)/SPEEDS.get(mode);
		}
		addNodesAndLinks(fromNode, originId, modes, modes.contains("car")?originId:null, knownPlaces, agenda, path, modes.contains("car"), mobsimStatus);
		if(mobsimStatus.isMobsimEnds() || now.getNow()+fastest>endTime)
			return null;
		/*double variableStartTime = ((SchedulingNode)path.get(path.size()-1).toNode).time;
		while(((SchedulingNode)path.get(path.size()-1).toNode).time<endTime) {
			addNodesAndLinks(fromNode, facilities, originId, destinationId, lastActivity, lastTime, timeInterval, modes, modes.contains("car")?originId:null, placeSharer, agenda, path, modes.contains("car"));
			variableStartTime+=timeInterval;
			Id toNodeId = new IdImpl(originId+"("+variableStartTime+")");
			fromNode = (SchedulingNode) delegate.getNodes().get(toNodeId);
			if(fromNode==null) {
				fromNode = new SchedulingNode(toNodeId, facilities.getFacilities().get(originId).getCoord(), variableStartTime, getMaxActivityUtility(endTime-variableStartTime));
				addNode(fromNode);
			}
			String activityType = ((ActivitySchedulingLink)path.get(path.size()-1)).activityType;
			Id linkId = new IdImpl(activityType+","+originId+"("+(int)((SchedulingNode)path.get(path.size()-1).fromNode).time+"-"+fromNode.time+")");
			SchedulingLink link = (SchedulingLink) delegate.getLinks().get(linkId);
			if(link==null) {
				link = new ActivitySchedulingLink(linkId, path.get(path.size()-1).fromNode, fromNode, activityType, originId);
				addLink(link);
			}
			path.remove(path.size()-1);
			path.add(link);
		}*/
		if(lastNode.path!=null) {
			lastNode.path.remove(0);
			for(SchedulingLink link:lastNode.path)
				if(link instanceof ActivitySchedulingLink)
					agenda.getElements().get(((ActivitySchedulingLink)link).activityType).addCurrentPerformedTime(link.duration);
		}
		//System.out.println(lastNode.path+"("+lastNode.utility+")");
		return lastNode.path;
	}

	private void addNodesAndLinks(SchedulingNode node, Id originId,	Set<String> modes, Id carLocationId, Map<Id, KnownPlace> knownPlaces, Agenda agenda, List<SchedulingLink> path, boolean hadCar, final MobsimStatus mobsimStatus) {
		if(mobsimStatus.isMobsimEnds() || now.getNow()+fastest>endTime || (withMaximumDepth && path.size()-1>maximumDepth))
			return;
		double utility = getUtility(path, agenda, hadCar, endTime);
		if(utility+node.maxUtilityFrom<currentMaximumUtility)
			return;
		KnownPlace place = knownPlaces.get(originId);
		if(originId.equals(destinationId) && node.time==endTime)
			node.updatePath(path, utility);
		else {
			SchedulingLink pLink = path.get(path.size()-1);
			if(originId.equals(destinationId) && node.time<endTime) {
				if(!(pLink instanceof ActivitySchedulingLink && ((ActivitySchedulingLink)pLink).getActivityType().equals("visit"))) {
					Id toNodeId = new IdImpl(originId.toString()+"("+(int)endTime+")");
					SchedulingNode toNode = (SchedulingNode) delegate.getNodes().get(toNodeId);
					Id linkId = new IdImpl(lastActivity+","+originId+"("+(int)node.time+"-"+(int)endTime+")");
					SchedulingLink link = (SchedulingLink) delegate.getLinks().get(linkId);
					if(link==null) {
						link = new ActivitySchedulingLink(linkId, node, toNode, lastActivity, originId);
						addLink(link);
					}
					path.add(link);
					addNodesAndLinks(toNode, originId, modes, carLocationId, knownPlaces, agenda, path, hadCar, mobsimStatus);
					path.remove(path.size()-1);
					if(lastActivity.equals("home") && pLink instanceof JourneySchedulingLink && ((JourneySchedulingLink)pLink).getMode().equals("car"))
						hadCar = false;
				}
			}
			boolean hasCar = modes.contains("car");
			if(originId.equals(carLocationId))
				modes.add("car");
			Coord origin = facilities.getFacilities().get(originId).getCoord();
			Coord destination = facilities.getFacilities().get(destinationId).getCoord();
			double fastest = Double.MAX_VALUE;
			for(String mode:modes) {
				double timeMode = place.getTravelTime(mode, node.time, destinationId);
				if(timeMode>0 && fastest>timeMode)
					fastest = timeMode;
			}
			if(fastest==Double.MAX_VALUE) {
				String mode = modes.contains("car")?"car":"pt";
				fastest = CoordUtils.calcDistance(origin, destination)*FACTORS.get(mode)/SPEEDS.get(mode);
			}
			Set<String> activityTypes = new HashSet<String>(place.getActivityTypes(node.time));
			if(activityTypes!=null)
				for(String activityType:activityTypes) {
					AgendaElement agendaElement = agenda.getElements().get(activityType);
					if(((NormalDistribution)agendaElement.getNumHourWeek()).getMean()*3600-agendaElement.getCurrentPerformedTime()>0)
						if(pLink instanceof JourneySchedulingLink || !(((ActivitySchedulingLink)pLink).activityType.equals(activityType)||((ActivitySchedulingLink)pLink).activityType.equals("visit")||activityType.equals("visit")))
							for(int variableEndTime = (int) (endTime-fastest-(endTime-fastest)%timeInterval); variableEndTime>node.time; variableEndTime-=timeInterval) {
								double duration = variableEndTime-node.time;
								if(willingToPerform(agendaElement, duration)) {
									Id toNodeId = new IdImpl(originId+"("+variableEndTime+")");
									SchedulingNode toNode = (SchedulingNode) delegate.getNodes().get(toNodeId);
									if(toNode==null) {
										toNode = new SchedulingNode(toNodeId, facilities.getFacilities().get(originId).getCoord(), variableEndTime, getMaxActivityUtility(endTime-variableEndTime));
										addNode(toNode);
									}
									Id linkId = new IdImpl(activityType+","+originId+"("+(int)node.time+"-"+variableEndTime+")");
									SchedulingLink link = (SchedulingLink) delegate.getLinks().get(linkId);
									if(link==null) {
										link = new ActivitySchedulingLink(linkId, node, toNode, activityType, originId);
										addLink(link);
									}
									path.add(link);
									agendaElement.addCurrentPerformedTime(duration);
									addNodesAndLinks(toNode, originId, modes, carLocationId, knownPlaces, agenda, path, hadCar, mobsimStatus);
									path.remove(path.size()-1);
									agendaElement.substractCurrentPerformedTime(duration);
								}
							}
				}
			if(pLink instanceof ActivitySchedulingLink) {
				if(!originId.equals(destinationId))
					knownPlaces.remove(originId);
				List<KnownPlace> knownPlacesList = new ArrayList<KnownPlace>(knownPlaces.values());
				for(int i=0; i<knownPlacesList.size(); i++) {
					KnownPlace knownPlace = knownPlacesList.get(i);
					Id toFacilityId = knownPlace.getFacilityId();
					if(!toFacilityId.equals(originId)) {
						fastest = Double.MAX_VALUE;
						for(String mode:modes) {
							double timeMode = knownPlace.getTravelTime(mode, node.time, destinationId);
							if(timeMode>0 && fastest>timeMode)
								fastest = timeMode;
						}
						if(fastest==Double.MAX_VALUE)
							fastest = CoordUtils.calcDistance(facilities.getFacilities().get(knownPlace.getFacilityId()).getCoord(), destination)*FACTORS.get("car")/SPEEDS.get("car");
						Set<String> originalModes = new HashSet<String>(modes);
						for(String mode:originalModes) {
							double travelTime = knownPlace.getTravelTime(mode, node.time, toFacilityId);
							if(travelTime<0)
								travelTime = CoordUtils.calcDistance(origin, facilities.getFacilities().get(toFacilityId).getCoord())*FACTORS.get(mode)/SPEEDS.get(mode);
							if(endTime-fastest-travelTime>node.time) {
								int variableEndTime = (int) (node.time+travelTime-(node.time+travelTime)%timeInterval)+timeInterval;
								Id toNodeId = new IdImpl(toFacilityId.toString()+"("+variableEndTime+")");
								SchedulingNode toNode = (SchedulingNode) delegate.getNodes().get(toNodeId);
								if(toNode==null) {
									toNode = new SchedulingNode(toNodeId, facilities.getFacilities().get(toFacilityId).getCoord(), variableEndTime, getMaxActivityUtility(endTime-variableEndTime));
									addNode(toNode);
								}
								Id linkId = new IdImpl(mode+","+originId.toString()+"-"+toFacilityId.toString()+"("+(int)node.time+"-"+variableEndTime+")");
								SchedulingLink link = (SchedulingLink) delegate.getLinks().get(linkId);
								if(link==null) {
									link = new JourneySchedulingLink(linkId, node, toNode, mode, travelTime);
									addLink(link);
								}
								path.add(link);
								if(hasCar && !mode.equals("car"))
									modes.remove("car");
								if(mode.equals("car") && !hadCar)
									hadCar = true;
								Id newCarLocationId = modes.contains("car")?toFacilityId:hasCar?originId:carLocationId;
								addNodesAndLinks(toNode, toFacilityId, modes, newCarLocationId, knownPlaces, agenda, path, hadCar, mobsimStatus);
								if(hasCar)
									modes.add("car");
								path.remove(path.size()-1);
							}
						}
					}
				}
				knownPlaces.put(originId, place);
			}
			if(!hasCar)
				modes.remove("car");
		}
	}

	private boolean willingToPerform(AgendaElement agendaElement, double duration) {
		NormalDistribution numHour = (NormalDistribution)agendaElement.getNumHourWeek();
		NormalDistribution typicalDuration = (NormalDistribution)agendaElement.getDuration();
		if(duration<=3600*(numHour.getMean()+numHour.getStandardDeviation())-agendaElement.getCurrentPerformedTime() &&
				duration<=typicalDuration.getMean()+typicalDuration.getStandardDeviation() && 
				duration>=typicalDuration.getMean()-typicalDuration.getStandardDeviation()) {
			return true;
		}
		return false;
	}
	
	private double getUtility(List<SchedulingLink> path, Agenda agenda, boolean hadCar, double endTime) {	
		double utility = 0;
		Map<String, Double> acts = new HashMap<String, Double>();
		double time = 0;
		String lastMode = hadCar?"car":"";
		boolean firstActivity = true;
		for(Tuple<String, Tuple<Double, Double>> previousActivity:previousActivities) {
			String activityType = previousActivity.getFirst();
			if(!firstActivity) {
				Double lastTime = acts.get(activityType);
				if(lastTime==null)
					lastTime = -24*3600.0;
				utility += getActivityUtility(activityType, previousActivity.getSecond().getSecond(), time-lastTime, agenda.getElements().get(activityType));
			}
			else
				firstActivity = false;
			time = previousActivity.getSecond().getFirst();
			acts.put(activityType, time);
		}
		for(SchedulingLink link:path) {
			if(link instanceof ActivitySchedulingLink) {
				String activityType = ((ActivitySchedulingLink)link).activityType;
				Double lastTime = acts.get(activityType);
				if(lastTime==null)
					lastTime = -24*3600.0;
				utility += getActivityUtility(activityType, link.getDuration(), time-lastTime, agenda.getElements().get(activityType));
				acts.put(activityType, time+link.getDuration());
			}
			else {
				lastMode = ((JourneySchedulingLink)link).mode;
				utility += getJourneyUtility(lastMode, link.getDuration());
			}
			time += link.getDuration();
		}
		utility-= hadCar && !lastMode.equals("car")?50:0;
		ActivitySchedulingLink lastActivityLink = null;
		if(path.size()>0 && path.get(path.size()-1) instanceof ActivitySchedulingLink && followingActivities.get(0).getFirst().equals(((ActivitySchedulingLink)path.get(path.size()-1)).getActivityType()))
			lastActivityLink = (ActivitySchedulingLink) path.remove(path.size()-1);
		time = endTime;
		boolean lastActivityAdded = lastActivityLink==null;
		for(int i=0; i<followingActivities.size()-1; i++) {
			Tuple<String, Tuple<Double, Double>> act = followingActivities.get(i);
			String activityType = act.getFirst();
			Double lastTime = acts.get(activityType);
			if(lastTime==null)
				lastTime = -24*3600.0;
			if(i==0) {
				double sinceLastTime = time-lastTime-(lastActivityLink==null?0:lastActivityLink.duration);
				double duration = act.getSecond().getSecond()+(lastActivityLink==null?0:lastActivityLink.duration);
				utility += getActivityUtility(activityType, duration, sinceLastTime, agenda.getElements().get(activityType));
				lastActivityAdded = true;
			}
			else
				utility += getActivityUtility(activityType, act.getSecond().getSecond(), time-lastTime, agenda.getElements().get(activityType));
			time = act.getSecond().getFirst();
			acts.put(activityType, time);
		}
		String firstAct = null;
		double firstDur = 0;
		if(previousActivities.size()>0) {
			firstAct = previousActivities.get(0).getFirst();
			firstDur = previousActivities.get(0).getSecond().getSecond();
		}
		else {
			firstAct = path.size()==0?lastActivityLink.activityType:((ActivitySchedulingLink)path.get(0)).activityType;
			firstDur = path.size()==0?lastActivityLink.duration:path.get(0).duration;
		}
		String lastAct = followingActivities.get(followingActivities.size()-1).getFirst();
		double lastDur = followingActivities.get(followingActivities.size()-1).getSecond().getSecond()+(lastActivityAdded?0:lastActivityLink.duration);
		time -= lastActivityAdded?0:lastActivityLink.duration;
		Double lastTime = acts.get(lastAct);
		if(lastTime==null)
			lastTime = -24*3600.0;
		if(lastAct.equals(firstAct))
			utility += getActivityUtility(lastAct, lastDur+firstDur, time-lastTime, agenda.getElements().get(lastAct));
		else {
			Double lastTimeF = acts.get(firstAct);
			if(lastTimeF==null)
				lastTimeF = 0.0;
			utility += getActivityUtility(firstAct, firstDur, 24*3600.0-lastTimeF, agenda.getElements().get(firstAct));
			utility += getActivityUtility(lastAct, lastDur, time-lastTime, agenda.getElements().get(lastAct));
		}
		if(lastActivityLink!=null)
			path.add(lastActivityLink);
		return utility;
	}

	private double getJourneyUtility(String mode, double duration) {
		return (mode.equals("car")?-8:mode.equals("pt")?-4:-1)*duration/3600;
	}

	private double getActivityUtility(String activityType, double duration, double sinceLastTime, AgendaElement agendaElement) {
		double tx = ((NormalDistribution)agendaElement.getDuration()).getMean();
		double period = 1.5*tx;
		double t0 = tx*Math.exp(-10*3600/tx);
		if(period<sinceLastTime)
			sinceLastTime = period;
		if(duration<0)
			duration=1;
		if(sinceLastTime<0)
			sinceLastTime=0;
		double utility = getActivityBeta(agendaElement.getType())*tx*Math.log((duration+f2(sinceLastTime,period))/(t0+f2(sinceLastTime,period)));
		return utility<0?0:utility;
	}

	private double f2(double y, double T) {
		double a = 0.005;
		double b = Math.log((T+a)/a)/T;
		return a*Math.exp(b*(T-y))-a;
	}
	
	private double getMaxActivityUtility(double duration) {
		/*double tx = duration;
		double t0 = tx*Math.exp(-10*3600/tx);
		return MAX_ACTIVITY_UTILITY*tx*(Math.log((duration+t0)/t0));*/
		return MAX_ACTIVITY_UTILITY*duration;
	}

	private double getActivityBeta(String activityType) {
		if(activityType.equals("home"))
			return 12.0/3600;
		else if(activityType.equals("visit"))
			return 0;
		return 6.0/3600;
	}
	
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader(scenario).readFile(args[0]);
		boolean origin = false, destination = false;
		int shop = 10;
		int sport = 10;
		Id originId = null, destinationId = null;
		PlaceSharer placeSharer = new PlaceSharer() {
		};
		double startTime = Double.parseDouble(args[3]);
		ActivityFacilities facilities = new ActivityFacilitiesImpl();
		for(ActivityFacility facility:scenario.getActivityFacilities().getFacilities().values()) {
			if(!origin && facility.getActivityOptions().keySet().contains(args[1])) {
				facilities.addActivityFacility(facility);
				originId = facility.getId();
				placeSharer.addKnownPlace(originId, startTime, args[1]);
				placeSharer.addKnownPlace(originId, startTime+2*3600, args[1]);
				placeSharer.addKnownPlace(originId, startTime+4*3600, args[1]);
				placeSharer.addKnownPlace(originId, startTime+6*3600, args[1]);
				/*placeSharer.addKnownPlace(facility.getId(), startTime, "visit");
				placeSharer.addKnownPlace(facility.getId(), startTime+2*3600, "visit");
				placeSharer.addKnownPlace(facility.getId(), startTime+4*3600, "visit");
				placeSharer.addKnownPlace(facility.getId(), startTime+6*3600, "visit");*/
				origin = true;
				if(destination && shop==0 && sport==0)
					break;
			}
			else if(!destination && facility.getActivityOptions().keySet().contains(args[2])) {
				facilities.addActivityFacility(facility);
				destinationId = facility.getId();
				placeSharer.addKnownPlace(destinationId, startTime, args[2]);
				placeSharer.addKnownPlace(destinationId, startTime+2*3600, args[2]);
				placeSharer.addKnownPlace(destinationId, startTime+4*3600, args[2]);
				placeSharer.addKnownPlace(destinationId, startTime+6*3600, args[2]);
				/*placeSharer.addKnownPlace(facility.getId(), startTime, "visit");
				placeSharer.addKnownPlace(facility.getId(), startTime+2*3600, "visit");
				placeSharer.addKnownPlace(facility.getId(), startTime+4*3600, "visit");
				placeSharer.addKnownPlace(facility.getId(), startTime+6*3600, "visit");*/
				destination = true;
				if(origin && shop==0 && sport==0)
					break;
				
			}
			if(shop>0 && facility.getActivityOptions().keySet().contains("shop")) {
				if(facilities.getFacilities().get(facility.getId())==null)
					facilities.addActivityFacility(facility);
				placeSharer.addKnownPlace(facility.getId(), startTime, "shop");
				placeSharer.addKnownPlace(facility.getId(), startTime+2*3600, "shop");
				placeSharer.addKnownPlace(facility.getId(), startTime+4*3600, "shop");
				placeSharer.addKnownPlace(facility.getId(), startTime+6*3600, "shop");
				shop--;
				if(origin && destination && sport==0)
					break;
			}
			else if(sport>0 && facility.getActivityOptions().keySet().contains("sport")) {
				if(facilities.getFacilities().get(facility.getId())==null)
					facilities.addActivityFacility(facility);
				placeSharer.addKnownPlace(facility.getId(), startTime, "sport");
				placeSharer.addKnownPlace(facility.getId(), startTime+2*3600, "sport");
				placeSharer.addKnownPlace(facility.getId(), startTime+4*3600, "sport");
				placeSharer.addKnownPlace(facility.getId(), startTime+6*3600, "sport");
				sport--;
				if(origin && shop==0 && destination)
					break;
			}
			
		}
		new FacilitiesWriter(facilities).write(args[6]);
		SchedulingNetwork network = new SchedulingNetwork();
		Agenda agenda = new Agenda();
		agenda.addElement(args[1], new NormalDistributionImpl(8, 2), new NormalDistributionImpl(8*3600, 2*3600));
		agenda.addElement(args[2], new NormalDistributionImpl(10, 2), new NormalDistributionImpl(10*3600, 2*3600));
		agenda.addElement("shop", new NormalDistributionImpl(2, 1), new NormalDistributionImpl(2*3600, 1*3600));
		agenda.addElement("sport", new NormalDistributionImpl(2, 0.5), new NormalDistributionImpl(2*3600, 0.5*3600));
		//agenda.addElement("visit", new NormalDistributionImpl(0.4, 0.2), new NormalDistributionImpl(0.2*3600, 0.1*3600));
		List<Tuple<String, Tuple<Double, Double>>> previousActivities = new ArrayList<Tuple<String, Tuple<Double, Double>>>();
		previousActivities.add(new Tuple<String,Tuple<Double, Double>>(args[2], new Tuple<Double, Double>(startTime, 28800.0)));
		previousActivities.add(new Tuple<String,Tuple<Double, Double>>(args[1], new Tuple<Double, Double>(startTime-28800, startTime-28800)));
		List<Tuple<String, Tuple<Double, Double>>> followingActivities = new ArrayList<Tuple<String, Tuple<Double, Double>>>();
		followingActivities.add(new Tuple<String, Tuple<Double, Double>>(args[2], new Tuple<Double, Double>(24*3600.0, 24*3600.0-Double.parseDouble(args[4]))));
		long time = System.currentTimeMillis();
		List<SchedulingLink> path = network.createNetwork(new CurrentTime(), scenario.getActivityFacilities(), originId, destinationId, Double.parseDouble(args[4]), Integer.parseInt(args[5]), new HashSet<String>(Arrays.asList("car", "pt", "walk")), placeSharer, agenda, previousActivities, followingActivities, new MobsimStatus());
		System.out.println(System.currentTimeMillis()-time);
		new MatsimNetworkReader(scenario).readFile(args[7]);
		JFrame frame = new JFrame();
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        /*MainApplet mainApplet = new MainApplet(facilities, placeSharer, network, scenario.getNetwork(), path);
        frame.add(mainApplet);
        mainApplet.init();
        frame.setSize(Toolkit.getDefaultToolkit().getScreenSize());
		frame.setVisible(true);*/
	}
	
}
