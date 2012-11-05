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
package playground.andreas.aas.modules.ptTripAnalysis.traveltime.V4;

import java.util.ArrayList;
import java.util.List;

import org.jfree.util.Log;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.utils.collections.Tuple;

/**
 * @author droeder
 * 
 */
public abstract class SomePtTime {
	private boolean finished = false;

	/*
	 * stores the start and the end of a time like SwitchWait, so that there is
	 * the possibility to get durations of parts of a trip and the exact # of
	 * these times in a trip
	 */
	protected List<Tuple<Double, Double>> times;
	private Double temp = null;

	public SomePtTime() {
		this.times = new ArrayList<Tuple<Double, Double>>();
	}

	public void handleEvent(Event e) {
		if (!finished) {
			if (handle(e)) {
				this.addTime(e);
			}
		}
	}

	/**
	 * should return true if the given event is expect and should be handled
	 * 
	 * @param e
	 * @return
	 */
	protected abstract boolean handle(Event e);

	private void addTime(Event e) {
		if (temp == null) {
			temp = e.getTime();
		} else if (temp < e.getTime()) {
			times.add(new Tuple<Double, Double>(temp, e.getTime()));
			temp = null;
		} else {
			temp = null;
		}
	}

	public double getTime() {
		double temp = 0;
		for (Tuple<Double, Double> t : this.times) {
			temp += t.getSecond() - t.getFirst();
		}
		return temp;
	}

	public int getCount() {
		return this.times.size();
	}

	public void finish() {
		if (this.temp == null) {
			Log.error("missing event!");
		}
		this.finished = true;
	}
}

class AccesWalk extends SomePtTime {

	/**
	 * @param waitsFor
	 */
	public AccesWalk() {
		super();
	}

	@Override
	protected boolean handle(Event e) {
		if (e instanceof AgentDepartureEvent) {
			return true;
		} else if (e instanceof AgentArrivalEvent) {
			this.finish();
			return true;
		} else {
			return false;
		}
	}
}

class AccesWait extends SomePtTime {

	public AccesWait() {
		super();
	}

	@Override
	protected boolean handle(Event e) {
		if (e instanceof AgentArrivalEvent) {
			return true;
		} else if (e instanceof PersonEntersVehicleEvent) {
			this.finish();
			return true;
		} else {
			return false;
		}
	}
}

class LineTT extends SomePtTime {

	public LineTT() {
		super();
	}

	@Override
	protected boolean handle(Event e) {
		if (e instanceof PersonEntersVehicleEvent) {
			return true;
		} else if (e instanceof PersonLeavesVehicleEvent) {
			return true;
		} else {
			return false;
		}
	}

}

class SwitchWait extends SomePtTime {

	private String nextExp;

	public SwitchWait() {
		super();
		nextExp = PersonLeavesVehicleEvent.class.toString();
	}

	@Override
	protected boolean handle(Event e) {
		// handle only if the given event is an instance of the next expected
		// event
		if (nextExp.equals(e.getClass().toString())) {
			if (e instanceof PersonLeavesVehicleEvent) {
				nextExp = AgentDepartureEvent.class.toString();
				return true;
			} else if (e instanceof AgentDepartureEvent) {
				/*
				 * after an AgentDepartureEvent a PersonEntersVehicleEvent is
				 * thrown only if the LegMode is ptthe time in the pt-vehicle
				 * starts as recently as the agent enters the vehicle
				 */
				if (((AgentDepartureEvent) e).getLegMode().equals(
						TransportMode.pt)) {
					nextExp = PersonEntersVehicleEvent.class.toString();
					return false;
				} else if (((AgentDepartureEvent) e).getLegMode().equals(
						TransportMode.transit_walk)) {
					nextExp = AgentArrivalEvent.class.toString();
					return true;
				} else {
					return false;
				}
			} else if (e instanceof AgentArrivalEvent) {
				nextExp = PersonEntersVehicleEvent.class.toString();
				return true;
			} else if (e instanceof PersonEntersVehicleEvent) {
				nextExp = PersonLeavesVehicleEvent.class.toString();
				return true;
			} else {
				return false;
			}

		} else {
			return false;
		}
	}
}

class SwitchWalk extends SomePtTime {
	private boolean first;

	public SwitchWalk() {
		super();
		first = true;
	}

	@Override
	protected boolean handle(Event e) {
		if (e instanceof AgentDepartureEvent && !first) {
			if (((AgentDepartureEvent) e).getLegMode().equals(
					TransportMode.transit_walk)) {
				return true;
			}
		} else if (e instanceof AgentArrivalEvent) {
			// the first "SwitchWalk" is the AccesWalk
			if (first) {
				first = false;
				return false;
			} else {
				if (((AgentArrivalEvent) e).getLegMode().equals(TransportMode.transit_walk)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public double getTime() {
		double temp = 0;
		// the last SwitchWalk is the EgressWalk
		for (int i = 0; i < this.times.size() - 1; i++) {
			temp = this.times.get(i).getSecond() - this.times.get(i).getFirst();
		}
		return temp;
	}

	@Override
	public int getCount() {
		return (this.times.size() - 1);
	}

	// the last SwitchWalk is the EgressWalk
	public double getEgressWalkTime() {
		if(times.size() < 1) return 0;
		return (this.times.get(this.times.size() - 1).getSecond() - 
				this.times.get(this.times.size() - 1).getFirst());
	}
}
