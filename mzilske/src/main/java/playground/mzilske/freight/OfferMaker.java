package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierOffer;

public interface OfferMaker {

	public abstract void init();
	
	public abstract void reset();
	
	public abstract CarrierOffer requestOffer(Id linkId, Id linkId2, int shipmentSize, Double memorizedPrice);

	public abstract CarrierOffer requestOffer(Id from, Id to, int shimpentSize, Double startPickup, Double endPickup, Double startDelivery, Double endDelivery, Double memorizedPrice);
}