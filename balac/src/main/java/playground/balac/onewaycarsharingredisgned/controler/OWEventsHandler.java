package playground.balac.onewaycarsharingredisgned.controler;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;

public class OWEventsHandler implements PersonLeavesVehicleEventHandler, PersonEntersVehicleEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler, LinkLeaveEventHandler{
	HashMap<Id, RentalInfoFF> ffRentalsStats = new HashMap<Id, RentalInfoFF>();
	HashMap<Id, String> arrivals = new HashMap<Id, String>();
	ArrayList<RentalInfoFF> arr = new ArrayList<RentalInfoFF>();
	HashMap<Id, Boolean> inVehicle = new HashMap<Id, Boolean>();
	HashMap<Id, Id> personVehicles = new HashMap<Id, Id>();

	Network network;
	public OWEventsHandler(Network network) {
		
		this.network = network;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		ffRentalsStats = new HashMap<Id, RentalInfoFF>();
		arrivals = new HashMap<Id, String>();
		arr = new ArrayList<RentalInfoFF>();
		inVehicle = new HashMap<Id, Boolean>();
		personVehicles = new HashMap<Id, Id>();

	}
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// TODO Auto-generated method stub
		personVehicles.remove(event.getVehicleId());
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// TODO Auto-generated method stub
		if (event.getVehicleId().toString().startsWith("OW"))
			personVehicles.put(event.getVehicleId(), event.getPersonId());
	}
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getVehicleId().toString().startsWith("OW")) {
			Id perid = personVehicles.get(event.getVehicleId());
			
			RentalInfoFF info = ffRentalsStats.get(perid);
			info.distance += network.getLinks().get(event.getLinkId()).getLength();
			
		}
		
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// TODO Auto-generated method stub
		inVehicle.put(event.getPersonId(), false);
		if (event.getLegMode().equals("walk_ow_sb")) {
			
			RentalInfoFF info = new RentalInfoFF();
			info.accessStartTime = event.getTime();
			info.personId = event.getPersonId();
			
			if (ffRentalsStats.get(event.getPersonId()) == null) {
				
				ffRentalsStats.put(event.getPersonId(), info);

			}
			else {
				
				RentalInfoFF info1 = ffRentalsStats.get(event.getPersonId());
				info1.egressStartTime = event.getTime();				
				
			}
			
		}
		else if (event.getLegMode().equals("onewaycarsharing")) {
			inVehicle.put(event.getPersonId(), true);

			RentalInfoFF info = ffRentalsStats.get(event.getPersonId());
			
			info.startTime = event.getTime();
			info.startLinkId = event.getLinkId();
			info.accessEndTime = event.getTime();
		}
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		// TODO Auto-generated method stub
		
		if (event.getLegMode().equals("onewaycarsharing")) {
			RentalInfoFF info = ffRentalsStats.get(event.getPersonId());
			info.endTime = event.getTime();
			info.endLinkId = event.getLinkId();
			
		}
		else if (event.getLegMode().equals("walk_ow_sb")) {
			if (ffRentalsStats.get(event.getPersonId()) != null && ffRentalsStats.get(event.getPersonId()).accessEndTime != 0.0) {
				RentalInfoFF info = ffRentalsStats.remove(event.getPersonId());
				info.egressEndTime = event.getTime();
				arr.add(info);	
			}
		}
		
	}
	
	public ArrayList<RentalInfoFF> rentals() {
		
		return arr;
	}
	
	public class RentalInfoFF {
		private Id personId = null;
		private double startTime = 0.0;
		private double endTime = 0.0;
		private Id startLinkId = null;
		private Id endLinkId = null;
		private double distance = 0.0;
		private double accessStartTime = 0.0;
		private double accessEndTime = 0.0;
		private double egressStartTime = 0.0;
		private double egressEndTime = 0.0;
		public String toString() {
			
			return personId + " " + Double.toString(startTime) + " " + Double.toString(endTime) + " " +
			startLinkId.toString() + " " +	endLinkId.toString()+ " " + Double.toString(distance)+ " " + Double.toString(accessEndTime - accessStartTime)+ " " + Double.toString(egressEndTime - egressStartTime);
		}
	}

}
