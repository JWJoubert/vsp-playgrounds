package playground.mzilske.freight;


public class Contract {
	
	private Shipment shipment;
	private Offer offer;
	
	public Contract(Shipment shipment, Offer offer) {
		super();
		this.shipment = shipment;
		this.offer = offer;
	}

	public Shipment getShipment() {
		return shipment;
	}
	
	public Offer getOffer() {
		return offer;
	}
	
}
