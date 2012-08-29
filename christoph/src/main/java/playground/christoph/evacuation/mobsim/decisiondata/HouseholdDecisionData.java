/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdDecisionData.java
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

package playground.christoph.evacuation.mobsim.decisiondata;

import org.matsim.api.core.v01.Id;

import playground.christoph.evacuation.mobsim.HouseholdPosition;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.EvacuationDecision;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.Participating;

/**
 * Data structure containing information used by a household for the decision
 * "evacuate directly vs. meet at home first".
 * 
 * @author cdobler
 */
public class HouseholdDecisionData {

	/*
	 * Model results
	 */
	private EvacuationDecision evacuationDecision = EvacuationDecision.UNDEFINED;
	
	/*
	 * Model input data
	 */
	private final Id householdId;
	private Id homeLinkId = null;
	private Id homeFacilityId = null;
	private Id meetingFacilityId = null;
	private HouseholdPosition householdPosition = null;
	private boolean hasChildren = false;
	private Participating participating = Participating.UNDEFINED;
	private boolean homeFacilityIsAffected = false;
	private double latestAcceptedLeaveTime = Double.MAX_VALUE;
	private double householdReturnHomeTime = Double.MAX_VALUE;
	private double householdEvacuateFromHomeTime = Double.MAX_VALUE;
	private double householdDirectEvacuationTime = Double.MAX_VALUE;
	
	public HouseholdDecisionData(Id householdId) {
		this.householdId = householdId;
	}
	
	public Id getHouseholdId() {
		return this.householdId;
	}
	
	public Id getHomeLinkId() {
		return homeLinkId;
	}

	public void setHomeLinkId(Id homeLinkId) {
		this.homeLinkId = homeLinkId;
	}

	public Id getHomeFacilityId() {
		return homeFacilityId;
	}

	public void setHomeFacilityId(Id homeFacilityId) {
		this.homeFacilityId = homeFacilityId;
	}
	
	public Id getMeetingPointFacilityId() {
		return this.meetingFacilityId;
	}
	
	public void setMeetingPointFacilityId(Id meetingFacilityId) {
		this.meetingFacilityId = meetingFacilityId;
	}
	
	public EvacuationDecision getEvacuationDecision() {
		return evacuationDecision;
	}

	public void setEvacuationDecision(EvacuationDecision evacuationDecision) {
		this.evacuationDecision = evacuationDecision;
	}

	public HouseholdPosition getHouseholdPosition() {
		return householdPosition;
	}

	public void setHouseholdPosition(HouseholdPosition householdPosition) {
		this.householdPosition = householdPosition;
	}
	
	public boolean isJoined() {
//		return isJoined;
		return this.householdPosition.isHouseholdJoined();
	}

//	public void setJoined(boolean isJoined) {
//		this.isJoined = isJoined;
//	}

	public boolean hasChildren() {
		return hasChildren;
	}

	public void setChildren(boolean hasChildren) {
		this.hasChildren = hasChildren;
	}
	
	public Participating getParticipating() {
		return participating;
	}

	public void setParticipating(Participating participating) {
		this.participating = participating;
	}

	public boolean isHomeFacilityIsAffected() {
		return homeFacilityIsAffected;
	}

	public void setHomeFacilityIsAffected(boolean homeFacilityIsAffected) {
		this.homeFacilityIsAffected = homeFacilityIsAffected;
	}

	public double getLatestAcceptedLeaveTime() {
		return latestAcceptedLeaveTime;
	}

	public void setLatestAcceptedLeaveTime(double latestAcceptedLeaveTime) {
		this.latestAcceptedLeaveTime = latestAcceptedLeaveTime;
	}

	public double getHouseholdReturnHomeTime() {
		return householdReturnHomeTime;
	}

	public void setHouseholdReturnHomeTime(double householdReturnHomeTime) {
		this.householdReturnHomeTime = householdReturnHomeTime;
	}

	public double getHouseholdEvacuateFromHomeTime() {
		return householdEvacuateFromHomeTime;
	}

	public void setHouseholdEvacuateFromHomeTime(
			double householdEvacuateFromHomeTime) {
		this.householdEvacuateFromHomeTime = householdEvacuateFromHomeTime;
	}

	public double getHouseholdDirectEvacuationTime() {
		return householdDirectEvacuationTime;
	}

	public void setHouseholdDirectEvacuationTime(double householdDirectEvacuationTime) {
		this.householdDirectEvacuationTime = householdDirectEvacuationTime;
	}
	
}