package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordUtils;

public class BeeLineOfferMaker implements OfferMaker {

	private Network network;
	
	private CarrierImpl carrier;
	
	private CarrierCostFunction costFunction;
	
	public BeeLineOfferMaker(CarrierImpl carrier, Network network, CarrierCostFunction costFunction) {
		super();
		this.carrier = carrier;
		this.network = network;
		this.costFunction = costFunction;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.OfferMaker#requestOffer(org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id, int, java.lang.Double)
	 */
	@Override
	public CarrierOffer requestOffer(Id linkId, Id linkId2, int shipmentSize, Double memorizedPrice) {
		CarrierOffer offer = new CarrierOffer();
		offer.setId(carrier.getId());
		double price;
	
		if (memorizedPrice != null) {
			price = memorizedPrice;
			if (MatsimRandom.getRandom().nextDouble() < 0.2) {
				double tenPercent = price * 0.5;
				double verrauscht = MatsimRandom.getRandom().nextDouble() * tenPercent;
				price = price + verrauscht * ( MatsimRandom.getRandom().nextDouble() < 0.5 ? -1 : 1);
			}
			System.out.println("Ich bin " + carrier.getId()+". Biete an: " + linkId + " nach " +linkId2 + " fuer " + price);
		} else {
			if(MatsimRandom.getRandom().nextDouble() < 0.5){
				price = getForwardLookingPrice(linkId, linkId2, shipmentSize);
			}
			else{
				price = getRandomPrice();
			}	
			System.out.println("Ich bin " + carrier.getId() + ". Biete an: " + linkId + " nach " +linkId2 + " fuer einen forwardLooking Preis " + price);
		}
		offer.setPrice(price);
		offer.setDuration(120.0);
		return offer;
	}

	private double getRandomPrice() {
		return MatsimRandom.getRandom().nextDouble()*300;
	}

	private double getForwardLookingPrice(Id linkId, Id linkId2, int shipmentSize) {
		CarrierVehicle carrierVehicle = carrier.getCarrierCapabilities().getCarrierVehicles().iterator().next();
		int vehicleCapacity = carrierVehicle.getCapacity();
		double crowFlyDist = calcDist(linkId,linkId2) + calcDist(carrier.getDepotLinkId(),linkId) + calcDist(linkId2,carrier.getDepotLinkId());
		double assumedTravelTime = crowFlyDist/14;
		//annahme, dass der lkw min. 1/5 wird
		double assumedLoadFactor = Math.min(0.2, MatsimRandom.getRandom().nextDouble());
		double sharedDistance = Math.min(crowFlyDist / ((double)vehicleCapacity*assumedLoadFactor) * shipmentSize, crowFlyDist);
		double sharedTime = Math.min(assumedTravelTime / ((double)vehicleCapacity*assumedLoadFactor) * shipmentSize, assumedTravelTime);
		
//		double factor = Math.min(0.6, Math.random());
//		return costFunction.calculateCost(carrierVehicle, crowFlyDist*factor, assumedTravelTime*factor);
		return costFunction.calculateCost(carrierVehicle, sharedDistance, sharedTime);
	}
	
	private double calcDist(Id link1, Id link2) {
		return CoordUtils.calcDistance(network.getLinks().get(link1).getCoord(), network.getLinks().get(link2).getCoord());
	}


	@Override
	public CarrierOffer requestOffer(Id from, Id to, int shimpentSize,
			Double startPickup, Double endPickup, Double startDelivery,
			Double endDelivery, Double memorizedPrice) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

}
