/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.incidents;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;

import playground.ikaddoura.incidents.data.TrafficItem;

/**
 * so far decoded:
 * 
 * abcD: abc-Gefahr
 * abcE: abc erwartet
 * 
 * abc1: 1 km
 * abc2: 2 km
 * ...
 * 
 * 
 * 
 * A1: stationary traffic (Stau)
 * A2: queuing traffic (stockend)
 * A3: slow traffic (dicht)
 * A4: heavy traffic (rege)
 * A5: traffic flows freely
 * A6
 * A10: less traffic than normal
 * A11, A12: more traffic than normal
 * A10X: stationary traffic, x km
 * A50: traffic problem
 * B1: accident
 * B2: serious accident 
 * B3: multi-vehicle accident
 * B4: accident with truck
 * B5: accident with hazardous materials
 * B6: accident with fuel spillage
 * B7: accident with chemical spillage
 * B9: accident with bus
 * B11: broken down vehicle
 * B12: broken down truck
 * B13: vehicle fire
 * B14: incident (Verkehrsbehinderung)
 * B15 --> accident with oil spillage
 * B16: overturned vehicle
 * B17: overturned truck
 * B18: jackknifed trailer
 * B19: jackknifed caravan
 * B20: jackknifed truck
 * B21: vehicles spun around (Fahrzeug verunglückt)
 * B22: earlier accident
 * B23: accident investigation work
 * B24: secondary accidents (Folgeunfälle)
 * B25: broken down bus
 * C1: closed
 * C3: closed ahead
 * C4: blocked ahead
 * D1: certain number of lanes closed (see quantifier)
 * D2: certain number of right lanes closed
 * D3: certain number of center lanes closed
 * D4: certain number of left lanes closed
 * D5: hard shoulder closed (Standstreifen gesperrt)
 * D6: two lanes closed
 * D7: three lanes closed
 * D14: emergency lane closed (Notfallspur gesperrt)
 * D15: reduced to one lane
 * D16: reduced to two lanes
 * D17: reduced to three lanes
 * D18: contraflow (Verkehr über Gegenfahrbahn geleitet)
 * D19: narrow lanes
 * D24: one lane closed
 * D50: overtaking lane closed
 * E1: roadworks
 * E4: resurfacing work
 * E6: roadmarking work
 * E12: slow moving maintenance vehicles
 * F5: flooding
 * F18: clearance work affecting traffic
 * F29: rescue and recovery work
 * P1: major event
 * P2: sports event
 * P6: fair (Volksfest)
 * P15: security alert affecting traffic
 * P49: evacuation
 * T4: traffic lights not working
 * T6: rail crossing failure
 * 
 * 
* @author ikaddoura
*/

public class TMCAlerts {
	private static final Logger log = Logger.getLogger(TMCAlerts.class);
	
	public Object[] getIncidentObject(Link link, TrafficItem trafficItem) {
		Object[] incidentObject = null;
		
		if (trafficItem.getTMCAlert() != null) {
			
			if (containsOrEndsWith(trafficItem, "D1") || containsOrEndsWith(trafficItem, "D2") || containsOrEndsWith(trafficItem, "D3") || containsOrEndsWith(trafficItem, "D5")) { // eine bestimmte Zahl an Fahrstreifen gesperrt
				double remainingNumberOfLanes = 0.;
				if (link.getNumberOfLanes() == 1.) {
					remainingNumberOfLanes = 1.;
					log.warn("Lane closed even though the road segment contains only one lane. Setting the lane number to 1.0.");
				} else {
					remainingNumberOfLanes = link.getNumberOfLanes() - 1.;
				}
				log.warn("Assuming that there is only one lane closed. Check the message: " + trafficItem.getTMCAlert().getDescription());
				
				incidentObject = new Object[] {link.getId(), trafficItem.getId(), (trafficItem.getOrigin().getDescription() + " --> " + trafficItem.getTo().getDescription()), trafficItem.getTMCAlert().getPhraseCode(), link.getLength(),
						
						// the parameters under normal conditions
						link.getAllowedModes(), link.getCapacity(), link.getNumberOfLanes(), link.getFreespeed(),
						
						// minus one lane
						link.getAllowedModes(), remainingNumberOfLanes * (link.getCapacity() / link.getNumberOfLanes()), remainingNumberOfLanes, getChangedFreeSpeed(link, trafficItem.getTMCAlert().getPhraseCode()),
						
						// start and end time
						trafficItem.getStartTime(), trafficItem.getEndTime()
				};
			}
			
			if (containsOrEndsWith(trafficItem, "D15")) { // nur ein Fahrstreifen
				incidentObject = new Object[] {link.getId(), trafficItem.getId(), (trafficItem.getOrigin().getDescription() + " --> " + trafficItem.getTo().getDescription()), trafficItem.getTMCAlert().getPhraseCode(), link.getLength(),
						
						// the parameters under normal conditions
						link.getAllowedModes(), link.getCapacity(), link.getNumberOfLanes(), link.getFreespeed(),
						
						// D15: reduction to one lane
						link.getAllowedModes(), (link.getCapacity() / link.getNumberOfLanes()), 1.0, getChangedFreeSpeed(link, trafficItem.getTMCAlert().getPhraseCode()),
						
						// start and end time
						trafficItem.getStartTime(), trafficItem.getEndTime()
				};
			}
			
			if (containsOrEndsWith(trafficItem, "D16")) { // nur zwei Fahrstreifen
				incidentObject = new Object[] {link.getId(), trafficItem.getId(), (trafficItem.getOrigin().getDescription() + " --> " + trafficItem.getTo().getDescription()), trafficItem.getTMCAlert().getPhraseCode(), link.getLength(),
						
						// the parameters under normal conditions
						link.getAllowedModes(), link.getCapacity(), link.getNumberOfLanes(), link.getFreespeed(),
						
						// D16: reduction to two lanes
						link.getAllowedModes(), 2.0 * (link.getCapacity() / link.getNumberOfLanes()), 2.0, getChangedFreeSpeed(link, trafficItem.getTMCAlert().getPhraseCode()),
						
						// start and end time
						trafficItem.getStartTime(), trafficItem.getEndTime()
				};
			}
			
			if (containsOrEndsWith(trafficItem, "E14")) { // abwechselnd in beide Richtungen nur ein Fahrstreifen frei // TODO: what about the other direction?
				incidentObject = new Object[] {link.getId(), trafficItem.getId(), (trafficItem.getOrigin().getDescription() + " --> " + trafficItem.getTo().getDescription()), trafficItem.getTMCAlert().getPhraseCode(), link.getLength(),
						
						// the parameters under normal conditions
						link.getAllowedModes(), link.getCapacity(), link.getNumberOfLanes(), link.getFreespeed(),
						
						// D16: reduction to two lanes
						link.getAllowedModes(), 1.5 * (link.getCapacity() / link.getNumberOfLanes()), 1.0, getChangedFreeSpeed(link, trafficItem.getTMCAlert().getPhraseCode()),
						
						// start and end time
						trafficItem.getStartTime(), trafficItem.getEndTime()
				};
			}
			
			if (containsOrEndsWith(trafficItem, "C1")) { // gesperrt
				incidentObject = new Object[] {link.getId(), trafficItem.getId(), (trafficItem.getOrigin().getDescription() + " --> " + trafficItem.getTo().getDescription()), trafficItem.getTMCAlert().getPhraseCode(), link.getLength(),
						
						// the parameters under normal conditions
						link.getAllowedModes(), link.getCapacity(), link.getNumberOfLanes(), link.getFreespeed(),
						
						// closed
						link.getAllowedModes(), 1.0, 1.0, 0.22227,
						
						// start and end time
						trafficItem.getStartTime(), trafficItem.getEndTime()
				};
			}		
		}
		
		return incidentObject;
	}

	private boolean containsOrEndsWith(TrafficItem trafficItem, String code) {
		if (trafficItem.getTMCAlert().getPhraseCode() == null) {
			log.warn("Alert with no code: " + trafficItem.toString());
			return false;
		} else {
			if (trafficItem.getTMCAlert().getPhraseCode().contains(code + ".") || trafficItem.getTMCAlert().getPhraseCode().endsWith(code)) {
				return true;
			} else {
				return false;
			}
		}
	}
	
	private double getChangedFreeSpeed(Link link, String code) {
		double changedFreeSpeed = 0.;

		if ( code.contains("E") || // Baustelle
				code.contains("B") || // Verkehrsbehinderung
				code.contains("F") || // F27 Gegenstand auf der Fahrbahn, F14: geplatzte Wasserleitung
				code.contains("P") // P39: Personen auf der Fahrbahn
				) {
			
			if (link.getFreespeed() >= 16.66667) {
				changedFreeSpeed = 16.66667;
				
			} else if (link.getFreespeed() < 16.66667 && link.getFreespeed() > 8. ) {
				changedFreeSpeed = 8.33334;
			}
			
			else if (link.getFreespeed() <= 8. ) {
				changedFreeSpeed = 2.77778;
			}
			
		}
		return changedFreeSpeed;
	}

}

