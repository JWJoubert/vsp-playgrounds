package freight;

import freight.listener.ShipperEventImpl;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;

import java.util.Map;

public class TotalCostStatusEvent extends ShipperEventImpl implements Event{
	private double totCost;
	public TotalCostStatusEvent(Id shipperId, double totCost) {
		super(shipperId);
		this.totCost = totCost;
	}
	public double getTotCost() {
		return totCost;
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