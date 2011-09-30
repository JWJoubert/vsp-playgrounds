package playground.mzilske.freight.events;

import java.util.Map;

import org.matsim.core.api.experimental.events.Event;

import playground.mzilske.freight.Contract;

public class ShipperTSPContractCanceledEvent implements Event{

	private Contract contract;
	
	public ShipperTSPContractCanceledEvent(Contract contract) {
		super();
		this.contract = contract;
	}

	public Contract getContract() {
		return contract;
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
