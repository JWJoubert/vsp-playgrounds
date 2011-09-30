package freight.offermaker;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

import playground.mzilske.freight.OfferMaker;
import playground.mzilske.freight.carrier.Carrier;
import playground.mzilske.freight.carrier.CarrierContract;
import playground.mzilske.freight.carrier.CarrierCostFunction;
import playground.mzilske.freight.carrier.CarrierOffer;
import playground.mzilske.freight.carrier.CarrierPlan;
import playground.mzilske.freight.carrier.CarrierPlanBuilder;
import playground.mzilske.freight.carrier.CarrierShipment;
import playground.mzilske.freight.carrier.CarrierUtils;
import playground.mzilske.freight.carrier.CarrierVehicle;
import city2000w.RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder;
import freight.vrp.Locations;
import freight.vrp.VRPTransformation;

public class RuinAndRecreateAverageMarginalCostOM_V2 implements OfferMaker{

	private Carrier carrier;
	
	private CarrierVehicle carrierVehicle;

	private Locations locations;
	
	private Network network;
	
	private CarrierCostFunction carrierCostCalculator;
	
	private CarrierPlanBuilder carrierPlanBuilder;
	
	public void setCarrierCostFunction(CarrierCostFunction carrierCostCalculator) {
		this.carrierCostCalculator = carrierCostCalculator;
	}


	public void setNetwork(Network network) {
		this.network = network;
	}


	public RuinAndRecreateAverageMarginalCostOM_V2(Carrier carrier, Locations locations) {
		super();
		this.carrier = carrier;
		carrierVehicle = carrier.getCarrierCapabilities().getCarrierVehicles().iterator().next();
		this.locations = locations;
	}

	
	@Override
	public CarrierOffer requestOffer(Id linkId, Id linkId2, int shipmentSize, Double memorizedPrice) {
		return null;
	}

	@Override
	public CarrierOffer requestOffer(Id from, Id to, int size, Double startPickup, Double endPickup, Double startDelivery, Double endDelivery, Double memorizedPrice) {
		if(memorizedPrice != null){
//			if(MatsimRandom.getRandom().nextDouble() < 0.8){
				CarrierOffer offer = new CarrierOffer();
				offer.setId(carrier.getId());
				offer.setPrice(memorizedPrice);
				return offer;
//			}
		}
		VRPTransformation vrpTransformation = new VRPTransformation(locations);
		for(CarrierContract c : carrier.getContracts()){
			CarrierShipment s = c.getShipment();
			vrpTransformation.addShipment(s);
		}
		CarrierShipment requestedShipment = CarrierUtils.createShipment(from, to, size, startPickup, endPickup, startDelivery, endDelivery);
		vrpTransformation.addShipment(requestedShipment);
		List<CarrierContract> carrierContracts = new ArrayList<CarrierContract>(carrier.getContracts());
		carrierContracts.add(CarrierUtils.createContract(requestedShipment, new CarrierOffer()));
		CarrierPlan plan = carrierPlanBuilder.buildPlan(carrier.getCarrierCapabilities(), carrierContracts);
		double tourCost = plan.getScore();
		double avgMCInMeters=new AverageMarginalCostOM.AvgMarginalCostCalculator(carrierContracts, network, carrierVehicle.getLocation()).calculateShareOfDistance(requestedShipment, tourCost);
		double mcCost = carrierCostCalculator.calculateCost(carrierVehicle, avgMCInMeters, 0.0);
		CarrierOffer offer = new CarrierOffer();
		offer.setId(carrier.getId());
		offer.setPrice(mcCost);
		return offer;
	}
		@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}


	public void setCarrierPlanBuilder(
			RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder rAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder) {
		this.carrierPlanBuilder = rAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder;
		
	}
}
