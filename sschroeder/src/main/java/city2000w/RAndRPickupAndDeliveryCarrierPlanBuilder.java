package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Tour;
import playground.mzilske.freight.Tour.Delivery;
import playground.mzilske.freight.Tour.Pickup;
import playground.mzilske.freight.Tour.TourElement;
import playground.mzilske.freight.TourBuilder;
import vrp.api.Customer;
import vrp.basics.TourActivity;
import freight.LocationsImpl;
import freight.RuinAndRecreateSolver;
import freight.VRPTransformation;

public class RAndRPickupAndDeliveryCarrierPlanBuilder {
	
private static Logger logger = Logger.getLogger(RAndRPickupAndDeliveryCarrierPlanBuilder.class);
	
	private Network network;
	
	private MarginalCostCalculator marginalCostCalculator = new MarginalCostCalculator();
	
	private VRPTransformation vrpTrafo;
	
	public void setMarginalCostCalculator(
			MarginalCostCalculator marginalCostCalculator) {
		this.marginalCostCalculator = marginalCostCalculator;
	}

	public RAndRPickupAndDeliveryCarrierPlanBuilder(Network network){
		this.network = network;
		iniTrafo();
	}

	private void iniTrafo() {
		LocationsImpl locations = new LocationsImpl();
		makeLocations(locations);
		vrpTrafo = new VRPTransformation(locations);
		
	}

	private void makeLocations(LocationsImpl locations) {
		locations.addAllLinks((Collection<Link>) network.getLinks().values());
	}

	public CarrierPlan buildPlan(CarrierCapabilities carrierCapabilities, Collection<Contract> contracts) {
		if(contracts.isEmpty()){
			return getEmptyPlan(carrierCapabilities);
		}
		Collection<Tour> tours = new ArrayList<Tour>();
		Collection<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();
		Collection<vrp.basics.Tour> vrpSolution = new ArrayList<vrp.basics.Tour>();
		RuinAndRecreateSolver ruinAndRecreateSolver = new RuinAndRecreateSolver(vrpSolution, vrpTrafo);
		ruinAndRecreateSolver.solve(contracts, carrierCapabilities.getCarrierVehicles().iterator().next());
		for(CarrierVehicle carrierVehicle : carrierCapabilities.getCarrierVehicles()){
			TourBuilder tourBuilder = new TourBuilder();
			Id vehicleStartLocation = carrierVehicle.getLocation();
			tourBuilder.scheduleStart(vehicleStartLocation);
			for(vrp.basics.Tour tour : vrpSolution){
				List<TourElement> enRouteActivities = new ArrayList<Tour.TourElement>();
				for(TourActivity act : tour.getActivities()){
					Shipment shipment = getShipment(act.getCustomer());
					if(act instanceof vrp.basics.EnRouteDelivery){
						enRouteActivities.add(new Delivery(shipment));
					}
					if(act instanceof vrp.basics.EnRoutePickup){
						enRouteActivities.add(new Pickup(shipment));
					}
				}
				List<TourElement> tourActivities = new ArrayList<Tour.TourElement>();
				tourActivities.addAll(enRouteActivities);
				for(TourElement e : tourActivities){
					tourBuilder.schedule(e);
				}
			}
			tourBuilder.scheduleEnd(vehicleStartLocation);
			Tour tour = tourBuilder.build();
			tours.add(tour);
			ScheduledTour scheduledTour = new ScheduledTour(tour, carrierVehicle, 0.0);
			scheduledTours.add(scheduledTour);
		}
		CarrierPlan carrierPlan = new CarrierPlan(scheduledTours);
		carrierPlan.setScore(ruinAndRecreateSolver.getVrpSolution().getTransportCosts());
		return carrierPlan;
	}
		

	private Shipment getShipment(Customer customer) {
		return vrpTrafo.getShipment(customer.getId());
	}

	private CarrierPlan getEmptyPlan(CarrierCapabilities carrierCapabilities) {
		Collection<Tour> tours = new ArrayList<Tour>();
		Collection<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();
		for(CarrierVehicle cv : carrierCapabilities.getCarrierVehicles()){
			TourBuilder tourBuilder = new TourBuilder();
			Id vehicleStartLocation = cv.getLocation();
			tourBuilder.scheduleStart(vehicleStartLocation);
			tourBuilder.scheduleEnd(vehicleStartLocation);
			Tour tour = tourBuilder.build();
			tours.add(tour);
			ScheduledTour scheduledTour = new ScheduledTour(tour, cv, 0.0);
			scheduledTours.add(scheduledTour);
		}
		CarrierPlan carrierPlan = new CarrierPlan(scheduledTours);
		return carrierPlan;
	}
}
