/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.Sim2DQTransitionLink;

import playground.gregor.sim2d_v4.cgal.TwoDObject;
import playground.gregor.sim2d_v4.debugger.VisDebugger;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.LinkSwitcher;


public class Sim2DAgent implements TwoDObject {
	
	private double v0 = 1.f;
	
	private final double [] pos = {0,0};
	
	private final double [] v = {0,0};
	
	private final QVehicle veh;
	private final MobsimDriverAgent driver;
	private PhysicalSim2DSection currentPSec;

	private final double r = MatsimRandom.getRandom().nextDouble()*.1 + 0.15; //radius
	
//	private Id currentLinkId;
//
//	private LinkInfo cachedLi;

	private final Scenario sc;

	private final LinkSwitcher ls;

	private final PhysicalSim2DEnvironment pEnv;

	private boolean hasLeft2DSim = false;

	private VelocityUpdater vu;

	public Sim2DAgent(Scenario sc, QVehicle veh, double spawnX, double spawnY, LinkSwitcher ls, PhysicalSim2DEnvironment pEnv) {
		this.pos[0] = spawnX;
		this.pos[1] = spawnY;
		this.veh = veh;
		this.driver = veh.getDriver();
		this.sc = sc;
		this.ls = ls;
		this.pEnv = pEnv;
		this.vu = new SimpleVelocityUpdater(this, ls, sc);
	}

	public void setVelocityUpdater(VelocityUpdater vu) {
		this.vu = vu;
	}
	
	public QVehicle getQVehicle() {
		return this.veh;
	}

	public void updateVelocity() {
		this.vu.updateVelocity();
	}



	public void setPSec(PhysicalSim2DSection physicalSim2DSection) {
		this.currentPSec = physicalSim2DSection;
		
	}

	public boolean move(double dx, double dy, double time) {
		if (this.ls.isSwitchLink(this.pos, dx, dy, this.getCurrentLinkId())) {
			Id nextLinkId = this.chooseNextLinkId();
			Sim2DQTransitionLink loResLink = this.pEnv.getLowResLink(nextLinkId);
			if (loResLink != null) { //HACK? we are in the agent's mental model but perform a physical sim2D --> qSim transition 
				// this should be handled in to the link corresponding PhysicalSim2DSection [gl April '13]
				if (loResLink.hasSpace()) {
					QVehicle veh = this.getQVehicle();
					veh.setCurrentLink(loResLink.getLink());
					loResLink.addFromIntersection(veh);
					this.hasLeft2DSim = true;
				} else {
					return false;
				}
			}
			this.pEnv.getEventsManager().processEvent(new LinkLeaveEvent(time, getId(), this.getCurrentLinkId(), this.veh.getId()));
			this.notifyMoveOverNode(nextLinkId);
			this.pEnv.getEventsManager().processEvent(new LinkEnterEvent(time, getId(), nextLinkId, this.veh.getId()));
		}
		
		
		this.pos[0] += dx;
		this.pos[1] += dy;
		
		
		
		return true;
	}

	public double[] getVelocity() {
		return this.v;
	}

	public Id getCurrentLinkId() {
		return this.driver.getCurrentLinkId();
	}

	public double[] getPos() {
		return this.pos;
	}

	public Id chooseNextLinkId() {
		Id id = this.driver.chooseNextLinkId();
		return id;
	}

	public Id getId() {
		return this.driver.getId();
	}

	public void notifyMoveOverNode(Id nextLinkId) {
		this.driver.notifyMoveOverNode(nextLinkId);
	}

	public void debug(VisDebugger visDebugger) {
		if (getId().toString().contains("g")) {
			visDebugger.addCircle((float)this.getPos()[0],(float) this.getPos()[1], (float)this.r, 0, 192, 64, 128,0,true);
		} else if (getId().toString().contains("r")) {
			visDebugger.addCircle((float)this.getPos()[0], (float)this.getPos()[1],(float) this.r, 192, 0, 64, 128,0,true);
		} else {
			int nr = this.hashCode()%3*255;
			int r,g,b;
			if (nr > 2*255) {
				r= nr-2*255;
				g =0;
				b=64;
			} else if (nr > 255) {
				r=0;
				g=nr-255;
				b=64;
			} else {
				r=64;
				g=0;
				b=nr;
			}
			visDebugger.addCircle((float)this.getPos()[0],(float) this.getPos()[1], (float)this.r, r, g, b, 222,0,true);
		}
		visDebugger.addText((float)this.getPos()[0],(float)this.getPos()[1], this.getId().toString(), 90);
		
		
	}

	public PhysicalSim2DSection getPSec() {
		return this.currentPSec;
	}

	public double getRadius() {
		return this.r;
	}

	@Override
	public double getXLocation() {
		return this.pos[0];
	}

	@Override
	public double getYLocation() {
		return this.pos[1];
	}

	public void setDesiredSpeed(double v) {
		this.v0 = v;
		
	}

	public boolean hasLeft2DSim() {
		return this.hasLeft2DSim ;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Sim2DAgent) {
			return getId().equals(((Sim2DAgent) obj).getId());
		}
		return false;
	}
	
}
