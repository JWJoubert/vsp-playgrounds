package vrp.algorithms.ruinAndRecreate.basics;

import java.util.Collection;
import java.util.Collections;

import org.apache.log4j.Logger;

import vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer;
import vrp.algorithms.ruinAndRecreate.api.TourActivityStatusUpdater;
import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.basics.BestTourBuilder.TourInformation;
import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.basics.Tour;
import vrp.basics.TourActivity;
import vrp.basics.Vehicle;


/**
 * 
 * @author stefan schroeder
 *
 */

class RRTourAgent implements TourAgent {
	
	private static Logger logger = Logger.getLogger(RRTourAgent.class);
	
	private Tour tour;
	
	private Vehicle vehicle;

	private Double currentCost = null;
	
	private Costs costs;
	
	private Constraints constraint;
	
	private Offer openOffer = null;
	
	private Double costOfOfferedTour = null; 
	
	private Tour tourOfLastOffer = null;
	
	private TourActivityStatusUpdater activityStatusUpdater;
	
	private TourBuilder tourBuilder;
	
	RRTourAgent(Costs costs, Tour tour, Vehicle vehicle, TourActivityStatusUpdater updater) {
		super();
		this.tour = tour;
		this.vehicle = vehicle;
		this.costs = costs;
		this.activityStatusUpdater = updater;
		updater.update(tour);
		currentCost = updater.getTourCost();
	}

	public void setTourBuilder(TourBuilder tourBuilder) {
		this.tourBuilder = tourBuilder;
	}


	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#getConstraint()
	 */
	Constraints getConstraint() {
		return constraint;
	}
	
	public boolean tourIsValid(){
		activityStatusUpdater.update(tour);
		currentCost = activityStatusUpdater.getTourCost();
		if(constraint.judge(tour)){
			return true;
		}
		else{
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#setConstraint(api.basic.Constraints)
	 */
	public void setConstraint(Constraints constraint) {
		this.constraint = constraint;
	}

	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#requestService(core.basic.Node)
	 */
	
	public Offer requestService(Shipment shipment){
		TourInformation tourTrippel = tourBuilder.buildTour(tour, shipment);
		if(tourTrippel != null){
			Offer offer = new Offer(this, tourTrippel.marginalCosts);
			openOffer = offer;
			tourOfLastOffer = tourTrippel.tour;
			costOfOfferedTour = tourTrippel.totalCosts;
			logger.debug("lastOffer: " + offer);
			return offer;
		}
		else{
			return null;
		}
		
	}
	
	
	 /* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#getTourSize()
	 */
	public int getTourSize(){
		return tour.getActivities().size();
	}

	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#offerGranted(core.basic.Node)
	 */
	public void offerGranted(Shipment shipment){
		if(tourOfLastOffer != null){
			tour = tourOfLastOffer;
			currentCost = costOfOfferedTour;
			logger.debug("granted offer: " + openOffer);
			logger.debug("");
			openOffer = null;
			tourOfLastOffer = null;
			costOfOfferedTour = null;
		}
		else {
			throw new IllegalStateException("cannot grant offer where no offer has been given");
		}
	}
	
	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#offerRejected(core.algorithms.ruinAndRecreate.RuinAndRecreate.Offer)
	 */
	public void offerRejected(Offer offer){
		
	}

	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#getTotalCost()
	 */
	public double getTotalCost(){
		return currentCost;
	}
	
	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#removeNode(core.basic.Node)
	 */
	public void removeCustomer(Customer customer){
//		logger.debug("remove node: " + node);
		for(TourActivity c : tour.getActivities()){
			if(c.getCustomer().getId().equals(customer.getId())){
				tour.getActivities().remove(c);
				activityStatusUpdater.update(tour);
				currentCost = activityStatusUpdater.getTourCost();
				break;
			}
		}
	}

	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#hasNode(core.basic.Node)
	 */
	public boolean hasCustomer(Customer customer) {
		for(TourActivity c : tour.getActivities()){
			if(c.getCustomer().getId().equals(customer.getId())){
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#getVehicleCapacity()
	 */
	public int getVehicleCapacity() {
		return vehicle.getCapacity();
	}
	
	public Collection<TourActivity> getTourActivities(){
		return Collections.unmodifiableCollection(tour.getActivities());
	}
	
	@Override
	public String toString() {
		return tour.toString();
	}

	public Tour getTour() {
		return tour;
	}

}