package playground.wrashid.parkingSearch.planLevel.ranking;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilityImpl;

import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.IncomeRelevantForParking;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.ParkingPriceMapping;

public class Ranking {

	private ParkingPriceMapping parkingPriceMapping;
	private IncomeRelevantForParking incomeRelevantForParking;
	private ActivityFacilityImpl parkingFacilities;

	public Ranking(ParkingPriceMapping parkingPriceMapping, IncomeRelevantForParking income, ActivityFacilityImpl parkingFacilities) {
		this.parkingPriceMapping = parkingPriceMapping;
		this.incomeRelevantForParking=income;
		this.parkingFacilities=parkingFacilities;
	}

	public double getScore(Coord targetCoord,Id parkingFacilityId, double startParkingTime, double endParkingTime, Id personId, double parkingDuration, double deparkingDuration){
		
		double parkingPriceScore=-1.0*ParkingRoot.getPriceScoreScalingFactor()*parkingPriceMapping.getParkingPrice(parkingFacilityId).getPrice(startParkingTime, endParkingTime);
		double income=incomeRelevantForParking.getIncome(personId);
		
		// TODO: instead of a linear impact, this could form a different function than the priceScore function (wip).
		// TODO: this value should be given back by some other function!!!!
		parkingPriceScore=parkingPriceScore/income;
		
		// impact of parking and un-parking durations
		double parkingActivityDurationPenalty=-1.0*ParkingRoot.getParkingActivityDurationPenaltyScalingFactor()*(parkingDuration+deparkingDuration);
		
		// TODO: the penalty could increase more than linear with the duration!!!! (wip).
		// make general concept for this! => provide a function for each, which can be used!
		// TODO: really important! => just change shape of main function to change the impact of whole system!!!!
		
		
		// impact of walking between parking and destination
		
		// TODO: allow defining general function, for which the parameter can be changed by individuals!
		// e.g. negative quadratic function (positiv utility in beginning, which can decrease with time).
		
		// e.g. if we have a model, that elderly or disabled people would like to walk much less, we should be able
		// to make that factor really important by changing the function parameters.
		// TODO: add here the aspect of the individual function!!!!!!
		// wip: propose a function, but say that others could be used.
		// wip: what one can do now, is to estimate a model, e.g. for walking distance and the other parts
		// after that one can tell the system, what the actual parking occupancy in an area is and then
		// let the system calibrate itself, what scaling would render best results.
		double walkingPenalty=-1.0* ClosestParkingMatrix.getDistance(targetCoord, parkingFacilities.getCoord());
		
		// TODO: question: should we have one scaling factor to scaling the whole parking thing with the other scores?
		// is this required per Person or in general???????
		// 
		// TODO: question: should we scale the different aspects of parking to each other per person or in general?
		// I think the importance between the different parts of the parking should come from the function.
		// therefore such a factor would not be required.
		
		// I think, we could avoid having all these parameters, if we would just change the original function
		// parameters of the people. 
		
		// this means, scaling in this case is not part of the simulation.
		// but this would make it difficult.
		
		// conclution: the scaling within the parking score should be done directly over the individual function paramters.
		// the scaling between parking and the rest should be done again on an individual basis. That parameter can again 
		// be based on the whole situation of the person. 
		// but we could for the beginning set this parameter on a central basis as default, which could be changed,
		// as models and parameters have been estiamted for that part.
		// =scalingFactorParking*(individual parts of parking defined as parameters for income, walking, etc. functions)
		
		// CAPACITY CONSTRAINTS
		
		double capacityConstraints;
		
		// wip: discussion, it is difficult to get the data for some areas, but in some cities such data is available.
		// the private parking data is a difficult part. but that can also be estimated (e.g. based on surveys).
		
		// wip: we can verfiy through different experiments these 4 components of the parking.
		// wip: what is missing: parking search. but this can be seen as an additional layer in the parking selection
		// process, because the four 4 factors we look here at, play a central role to parking any way.
		
		// wip: future work: parking search (add an individual penaltiy for driving there).
		// threre we try to create routes with maximal scores (parking utility and that of driving between them should be lowest)
		// because at each parking, which is full, we know about the parkings arround us (assumption for every day traffic) and
		// and the utility in terms of cost, travel distance, how much congestion is at a parking (although that needs to be clowded), so that
		// the agent has no perfect knowledge on the number of parkings before he gets to the street.
		// etc. is clear. so, we can select where to drive next.
		// 	
		
		return 0;
	}
}
