package freight.vrp;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierContract;
import vrp.basics.Tour;

import java.util.Collection;

public class ClarkeAndWrightSolver implements VRPSolver {

	private Collection<Tour> tours;
	private MatSim2VRPTransformation vrpTransformation;
		
	public ClarkeAndWrightSolver(Collection<Tour> tours,
			MatSim2VRPTransformation vrpTransformation) {
		super();
		this.tours = tours;
		this.vrpTransformation = vrpTransformation;
	}

	/*
	@Override
	public void solve(Collection<CarrierContract> contracts, CarrierVehicle carrierVehicle) {
		Id depotId = findDepotId(contracts);
		VrpBuilder vrpBuilder = new VrpBuilder(depotId);
		vrpBuilder.setConstraints(new ClarkeWrightCapacityConstraint(carrierVehicle.getCapacity()));
		for(CarrierContract c : contracts){
			CarrierShipment s = c.getShipment();
			vrpTransformation.addPickupAndDeliveryOf(s);
		}
		vrpBuilder.setVRPTransformation(vrpTransformation);
		VRP vrp = vrpBuilder.buildVRP();
		ClarkeAndWright clarkAndWright = new ClarkeAndWright(vrp);
		clarkAndWright.run();
		tours.addAll(clarkAndWright.getSolution());
	}
	*/
	
	private Id findDepotId(Collection<CarrierContract> contracts) {
		for(CarrierContract c : contracts){
			return c.getShipment().getFrom();
		}
		throw new RuntimeException("no contracts or shipments");
	}

	@Override
	public Collection<org.matsim.contrib.freight.carrier.Tour> solve() {
		// TODO Auto-generated method stub
		return null;
	}

}
