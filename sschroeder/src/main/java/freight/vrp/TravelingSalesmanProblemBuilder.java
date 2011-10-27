package freight.vrp;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import vrp.algorithms.ruinAndRecreate.constraints.TWAndCapacityConstraint;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.SingleDepotVRP;
import vrp.basics.Coordinate;
import vrp.basics.SingleDepotVRPBuilder;

public class TravelingSalesmanProblemBuilder {
	
	private SingleDepotVRPBuilder problemBuilder;
	
	private Network network;
	
	public TravelingSalesmanProblemBuilder(Network network) {
		super();
		this.problemBuilder = new SingleDepotVRPBuilder();
		this.network = network;
	}

	public void addActivity(String activityType, Id locationId, double openingTime, double closingTime, double duration){
		double earliestArrivalTime = openingTime;
		double latestArrivalTime = closingTime - duration;
		problemBuilder.createAndAddCustomer(getId(activityType,locationId.toString()), problemBuilder.getNodeFactory().createNode(locationId.toString(),getCoord(locationId)), 0,
				earliestArrivalTime, latestArrivalTime, duration);
	}
	
	private Coordinate getCoord(Id locationId) {
		Coord coord = network.getLinks().get(locationId).getCoord();
		return new Coordinate(coord.getX(),coord.getY());
	}

	private String getId(String activity, String location) {
		return ""+activity+"@"+location;
	}

	public void setStart(Id locationId, double earliestDepartureTime, double latestArrivalTime){
		Customer customer = problemBuilder.createAndAddCustomer(getId("start",locationId.toString()), problemBuilder.getNodeFactory().createNode(locationId.toString(),getCoord(locationId)), 0, earliestDepartureTime, latestArrivalTime, 0);
		problemBuilder.setDepot(customer);
	}
	
	public void setCosts(Costs costs){
		problemBuilder.setCosts(costs);
	}
	
	public SingleDepotVRP buildTSP(){
		problemBuilder.setConstraints(new TWAndCapacityConstraint());
		problemBuilder.setVehicleType(Integer.MAX_VALUE);
		return problemBuilder.buildVRP();
	}
}
