/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.ikaddoura.internalizationCar;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;

/**
 * Event to indicate that an agent entering or leaving a link is delaying other agents on that link later on.
 * 
 * @author ikaddoura
 */
public final class MarginalCongestionEvent extends Event {
	
	public static final String EVENT_TYPE = "MarginalCongestionEffect";
	public static final String EVENT_CAPACITY_CONSTRAINT = "capacityConstraint";
	public static final String ATTRIBUTE_PERSON = "causingAgent";
	public static final String ATTRIBUTE_AFFECTED_AGENT = "affectedAgent";
	public static final String ATTRIBUTE_DELAY = "delay";
	public static final String ATTRIBUTE_LINK = "link";
	
	private final Id causingAgentId;
	private final Id affectedAgentId;
	private final double delay;
	private final Id linkId;
	private final String capacityConstraint;

	public MarginalCongestionEvent(double time, String capacityConstraint, Id causingAgentId, Id affectedAgentId, double externalDelay, Id linkId) {
		super(time);
		this.capacityConstraint = capacityConstraint;
		this.causingAgentId = causingAgentId;
		this.affectedAgentId = affectedAgentId;
		this.delay = externalDelay;
		this.linkId = linkId;
	}
	
	public double getDelay() {
		return delay;
	}

	public Id getCausingAgentId() {
		return causingAgentId;
	}

	public Id getAffectedAgentId() {
		return affectedAgentId;
	}

	public Id getLinkId() {
		return linkId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attrs = super.getAttributes();
		attrs.put(EVENT_CAPACITY_CONSTRAINT, this.capacityConstraint);
		attrs.put(ATTRIBUTE_PERSON, this.causingAgentId.toString());
		attrs.put(ATTRIBUTE_AFFECTED_AGENT, this.affectedAgentId.toString());
		attrs.put(ATTRIBUTE_DELAY, Double.toString(this.delay));
		attrs.put(ATTRIBUTE_LINK, this.linkId.toString());
		return attrs;
	}

	public String getCapacityConstraint() {
		return capacityConstraint;
	}

}
