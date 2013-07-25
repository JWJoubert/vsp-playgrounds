/* *********************************************************************** *
 * project: org.matsim.*
 * PhysicalSim2DEnvironment.java
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

package playground.gregor.sim2d_v4.simulation.physics;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.qnetsimengine.QSim2DTransitionLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.Sim2DQTransitionLink;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.events.debug.LineEvent;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection.Segment;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class PhysicalSim2DEnvironment {

	private static final Logger log = Logger.getLogger(PhysicalSim2DEnvironment.class);

	@Deprecated
	private static final double DEP_BOX_WIDTH = 2./1.3; // must be >= agents' diameter

	private static final double ARRIVAL_AREA_LENGTH = 5; //must be a meaning full value (for pedestrians 5m seems to be fine)

	private final Sim2DEnvironment env;

	Map<Id,PhysicalSim2DSection> psecs = new HashMap<Id,PhysicalSim2DSection>();
	Map<Id,PhysicalSim2DSection> linkIdPsecsMapping = new HashMap<Id,PhysicalSim2DSection>();

	private final Sim2DScenario sim2dsc;


	//	//DEBUG
	//	public static boolean DEBUG = false;
	//	public static  VisDebugger visDebugger;
	//	static {
	//		if (DEBUG) {
	//			visDebugger = new VisDebugger();
	//		} else {
	//			visDebugger = null;
	//		}
	//	}

	private Map<Id, Sim2DQTransitionLink> lowResLinks;

	private final EventsManager eventsManager;


	public PhysicalSim2DEnvironment(Sim2DEnvironment env, Sim2DScenario sim2dsc, EventsManager eventsManager) {
		this.env = env;
		this.sim2dsc = sim2dsc;
		this.eventsManager = eventsManager;
		init();
	}

	private void init() {
		for (Section sec : this.env.getSections().values()) {
			PhysicalSim2DSection psec = createAndAddPhysicalSection(sec);
			for (Id id : sec.getRelatedLinkIds()) {
				this.linkIdPsecsMapping.put(id, psec);
			}
		}
		for (PhysicalSim2DSection psec : this.psecs.values()) {
			psec.connect();
		}

	}

	private PhysicalSim2DSection createAndAddPhysicalSection(Section sec) {
		PhysicalSim2DSection psec = new PhysicalSim2DSection(sec,this.sim2dsc,this);
		this.psecs.put(sec.getId(),psec);
		return psec;
	}

	private PhysicalSim2DSection createAndAddDepartureBox(Section sec) {
		PhysicalSim2DSection psec = new PhysicalSim2DSection(sec,this.sim2dsc,this);
		this.psecs.put(sec.getId(),psec);
		return psec;
	}

	public PhysicalSim2DSection getPhysicalSim2DSectionAssociatedWithLinkId(Id id) {
		return this.linkIdPsecsMapping.get(id);
	}

	public void doSimStep(double time) {
		for (PhysicalSim2DSection psec : this.psecs.values()) {
			psec.updateAgents(time);
		}
		for (PhysicalSim2DSection psec : this.psecs.values()) {
			psec.moveAgents(time);
		}

		//		//DEBUG
		//		for (Sim2DQTransitionLink e : this.lowResLinks.values()) {
		//			e.debug();
		//		}

	}

	public void createAndAddPhysicalTransitionSection(
			Sim2DQTransitionLink lowResLink, Section sec, Link pred) {
//		Id id = sec.getId();
//		PhysicalSim2DSection psec = this.psecs.get(id);
//
//		//find the corresponding opening
//		Segment o = null;
//		//1. try to find touching element
//		double x0 = lowResLink.getLink().getFromNode().getCoord().getX();
//		double y0 = lowResLink.getLink().getFromNode().getCoord().getY();
//		double x1 = lowResLink.getLink().getToNode().getCoord().getX();
//		double y1 = lowResLink.getLink().getToNode().getCoord().getY();
//		for ( Segment po : psec.getOpenings()) {
//			boolean touches = CGAL.isOnVector(x0, y0, po.x0, po.y0, po.x1, po.y1);
//			if (touches) {
//				double left2 = CGAL.isLeftOfLine(po.x0, po.y0, x0,y0,x1,y1);
//				double left3 = CGAL.isLeftOfLine(po.x1, po.y1, x0,y0,x1,y1);
//				if (left2*left3 < 0) {
//					o = po;
//					break;
//				}
//			}
//		}
//		if (o == null) {
//			//2. not found! check for lowResLink opening intersection
//			for ( Segment po : psec.getOpenings()) {
//				double left0 = CGAL.isLeftOfLine(x0, y0, po.x0, po.y0, po.x1, po.y1);
//				double left1 = CGAL.isLeftOfLine(x1, y1, po.x0, po.y0, po.x1, po.y1);
//				if (left0*left1 < 0) {
//					double left2 = CGAL.isLeftOfLine(po.x0, po.y0, x0,y0,x1,y1);
//					double left3 = CGAL.isLeftOfLine(po.x1, po.y1, x0,y0,x1,y1);
//					if (left2*left3 < 0) {
//						o = po;
//						break;
//					}
//				}
//			}
//			if (o == null) {
//				//3. still not found! last chance, pred intersects opening
//				double x2 = pred.getFromNode().getCoord().getX();
//				double y2 = pred.getFromNode().getCoord().getY();
//				for ( Segment po : psec.getOpenings()) {
//					double left0 = CGAL.isLeftOfLine(x0, y0, po.x0, po.y0, po.x1, po.y1);
//					double left1 = CGAL.isLeftOfLine(x2, y2, po.x0, po.y0, po.x1, po.y1);
//					if (left0*left1 < 0) {
//						double left2 = CGAL.isLeftOfLine(po.x0, po.y0, x0,y0,x2,y2);
//						double left3 = CGAL.isLeftOfLine(po.x1, po.y1, x0,y0,x2,y2);
//						if (left2*left3 < 0) {
//							o = po;
//							break;
//						}
//					}
//				}
//
//			}
//
//		} // if o is still null then something is wrong! not checked here
//
//		Segment o1 = new Segment();
//		o1.dx = -o.dy;
//		o1.dy = o.dx;
//		o1.x0 = o.x0;
//		o1.y0 = o.y0;
//		o1.x1 = o1.x0 + ARRIVAL_AREA_LENGTH * o1.dx;
//		o1.y1 = o1.y0 + ARRIVAL_AREA_LENGTH * o1.dy;
//		//		psec.getObstacles().add(o1);
//
//
//		Segment o2 = new Segment();
//		o2.dx = o.dy;
//		o2.dy = -o.dx;
//		o2.x0 = o.x1 - ARRIVAL_AREA_LENGTH * o2.dx;
//		o2.y0 = o.y1 - ARRIVAL_AREA_LENGTH * o2.dy;
//		o2.x1 = o.x1;
//		o2.y1 = o.y1;
//		//		psec.getObstacles().add(o2);
//
//		Segment o3 = new Segment();
//		double l = lowResLink.getLink().getLength();
//		o3.dx = o.dx;
//		o3.dy = o.dy;
//		o3.x0 = o.x0 + l*-o.dy;
//		o3.y0 = o.y0 + l*o.dx;
//		o3.x1 = o.x1 + l*-o.dy;
//		o3.y1 = o.y1 + l*o.dx;
//		if (l < ARRIVAL_AREA_LENGTH) {
//			log.warn("2D/Q transition link:" + lowResLink.getLink().getId() + " is shorter than arrival area length. This might have negative side effects on the flow!");
//		}
//		//		psec.getObstacles().add(o3);
//		lowResLink.setEndOfQueueLine(o3);


		//		//DEBUG
		//		this.eventsManager.processEvent(new LineEvent(0, o1, true));
		//		this.eventsManager.processEvent(new LineEvent(0, o2, true));
		//		this.eventsManager.processEvent(new LineEvent(0, o3, true));
	}


	public void createAndAddPhysicalTransitionSection(
			QSim2DTransitionLink hiResLink) {
		Section sec = this.env.getSection(hiResLink.getLink());
		Id id = sec.getId();
		PhysicalSim2DSection psec = this.psecs.get(id);

		//retrieve opening
		Segment opening = null;
		Coord c = hiResLink.getLink().getFromNode().getCoord();
		double cx = c.getX();
		double cy = c.getY();
		for (Segment op : psec.getOpenings()) {
			if (CGAL.isOnVector(cx, cy, op.x0, op.y0, op.x1, op.y1)){ 
				double cx1 = hiResLink.getLink().getToNode().getCoord().getX();
				double cy1 = hiResLink.getLink().getToNode().getCoord().getX();
				double left1 = CGAL.isLeftOfLine(op.x0, op.y0, cx, cy, cx1, cy1);
				double left2 = CGAL.isLeftOfLine(op.x1, op.y1, cx, cy, cx1, cy1);
				if (left1*left2 < 0) {
					opening = op;
					break;
				}
			}
		}

		if (opening == null) {
			double x0 = hiResLink.getLink().getToNode().getCoord().getX();
			double y0 = hiResLink.getLink().getToNode().getCoord().getY();
			double dx = x0 -cx;
			double dy = y0 -cy;
			double length = Math.sqrt(dx*dx+dy*dy);
			dx /= length;
			dy /= length;
			opening = new Segment();
			opening.x0 = (cx+x0)/2 + dy*DEP_BOX_WIDTH/2; //should be capacity dependent [GL July '13]
			opening.y0 = (cy+y0)/2 - dx*DEP_BOX_WIDTH/2;
			opening.x1 = (cx+x0)/2 - dy*DEP_BOX_WIDTH/2;
			opening.y1 = (cy+y0)/2 + dx*DEP_BOX_WIDTH/2;
			opening.dx = +dy;
			opening.dy = -dx;
		}

		double dx = opening.x1 - opening.x0;
		double dy = opening.y1 - opening.y0;
		double width = Math.sqrt(dx*dx+dy*dy);
		dx /= width;
		dy /= width;

		boolean ccw = CGAlgorithms.isCCW(sec.getPolygon().getExteriorRing().getCoordinates());
		double bx;
		double by;
		if (ccw) { // rotate right(currently not supported)
			throw new RuntimeException("Polygon describing section: " + sec.getId() + " has a counter clockwise orientation, which currently is not supported!");
		} else { // rotate left 
			bx = -dy;
			by = dx;
		}


		GeometryFactory geofac = new GeometryFactory();
		Coordinate c0 = new Coordinate(opening.x0,opening.y0);
		Coordinate c1 = new Coordinate(opening.x0+3*bx,opening.y0+3*by);
		Coordinate c2 = new Coordinate(opening.x1+3*bx,opening.y1+3*by);
		Coordinate c3 = new Coordinate(opening.x1,opening.y1);

		Coordinate [] coords = {c0,c1,c2,c3,c0};
		LinearRing lr = geofac.createLinearRing(coords);
		Polygon p = geofac.createPolygon(lr, null);

		Id hiResLinkId = hiResLink.getLink().getId();
		Id boxId = this.sim2dsc.getMATSimScenario().createId(id.toString() + "_link" + hiResLinkId + "_dep_box_");
		int [] openings = {3};
		Id [] neighbors = {id};
		int level = sec.getLevel();
		Section s = this.env.createSection(boxId, p, openings, neighbors, level);
		double spawnX = (c0.x+c2.x)/2;
		double spawnY = (c0.y+c2.y)/2;

		double flowCap = hiResLink.getLink().getFromNode().getInLinks().values().iterator().next().getCapacity() / this.sim2dsc.getMATSimScenario().getNetwork().getCapacityPeriod();
		TransitionArea ta = new TransitionArea(s,this.sim2dsc,this,(int) flowCap+1);
		this.psecs.put(s.getId(),ta);


		Segment o = ta.getOpenings()[0];
		ta.putNeighbor(o,psec);
		hiResLink.createDepartureBox(ta,spawnX,spawnY);
		
		//DEBUG
		for ( Segment bo : ta.getObstacles()) {
			this.eventsManager.processEvent(new LineEvent(0,bo,true,0,0,0,255,0));
		}


	}

	public void registerLowResLinks(Map<Id, Sim2DQTransitionLink> lowResLinks2) {
		this.lowResLinks = lowResLinks2;

	}

	/*package*/ Sim2DQTransitionLink getLowResLink(Id nextLinkId) {
		return this.lowResLinks.get(nextLinkId);
	}

	public EventsManager getEventsManager() {
		return this.eventsManager;
	}



}
