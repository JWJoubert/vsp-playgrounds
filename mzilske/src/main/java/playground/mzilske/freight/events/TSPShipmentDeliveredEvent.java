package playground.mzilske.freight.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;

import playground.mzilske.freight.carrier.Shipment;

public class TSPShipmentDeliveredEvent implements Event{

	private Id tspId;
	
	private Shipment shipment;
	
	private double time;
		
	public Id getTspId() {
		return tspId;
	}

	public Shipment getShipment() {
		return shipment;
	}

	public TSPShipmentDeliveredEvent(Id tspId, Shipment shipment, double time) {
		super();
		this.tspId = tspId;
		this.shipment = shipment;
		this.time = time;
	}

	@Override
	public double getTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Map<String, String> getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

}
