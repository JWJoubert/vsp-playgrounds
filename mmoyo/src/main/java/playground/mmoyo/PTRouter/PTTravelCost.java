package playground.mmoyo.PTRouter;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.network.LinkImpl;

/**
 * Calculates the cost of links for the routing algorithm
 */
public class PTTravelCost implements TravelCost{
	private PTTravelTime ptTravelTime;
	double cost;
	double travelTime;
	double travelDistance;
	double waitTime;
	//byte aliasType;
	
	////coefficients with original values set to count only travelTime///
	double timeCoefficient =.85;
	double distanceCoefficient=.15;
	double transferPenalty=60;
	double walkCoefficient= 0; 
	//double waitCoefficient=0;
	
	public PTTravelCost(final PTTravelTime ptTravelTime) {
		this.ptTravelTime = ptTravelTime;
	}
	
	public PTTravelCost(final PTTravelTime ptTravelTime, double timeCoefficient, double distanceCoefficient, double transferPenalty) {
		this.ptTravelTime = ptTravelTime;
		this.timeCoefficient =timeCoefficient ; 
		this.distanceCoefficient = distanceCoefficient; 
		this.transferPenalty = transferPenalty;
	}

	public double getLinkTravelCost(Link link, double time){
		cost = ptTravelTime.getLinkTravelTime(link, time) ;  //the first assignation is only the travelTime
		
		String type = ((LinkImpl)link).getType();
		if (type.equals( PTValues.DETTRANSFER_STR ) || type.equals( PTValues.TRANSFER_STR )){
			cost += transferPenalty;
		}
		
		//with coefficients
		else if (type.equals( PTValues.STANDARD_STR )){
			cost = (cost * timeCoefficient) + (link.getLength() * distanceCoefficient) ;
		}
		else if (type.equals( PTValues.ACCESS_STR ) || type.equals( PTValues.EGRESS_STR )){
			cost = cost * walkCoefficient;
		}else{
			throw new java.lang.RuntimeException("the pt link does not have a defined type: " + link.getId());
		}
		////////////////////////////////////////////////////////////////
		
		return cost;
	}
}