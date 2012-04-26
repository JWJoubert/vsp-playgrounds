package playground.andreas.P2.replanning.modules;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;

import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.replanning.PPlanStrategy;
import playground.andreas.P2.replanning.PStrategy;

/**
 * 
 * Changes the end time of operation randomly within a certain range, leaving a minimal operating time.
 * 
 * @author aneumann
 *
 */
public class RandomEndTimeAllocator extends PStrategy implements PPlanStrategy{
	
	private final static Logger log = Logger.getLogger(RandomEndTimeAllocator.class);
	
	public static final String STRATEGY_NAME = "RandomEndTimeAllocator";
	
	private final int mutationRange;

	public RandomEndTimeAllocator(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 1){
			log.error("Missing parameter: 1 - Mutation range in seconds");
		}
		this.mutationRange = Integer.parseInt(parameter.get(0));
	}
	
	@Override
	public PPlan run(Cooperative cooperative) {
		if (cooperative.getBestPlan().getNVehicles() <= 1) {
			return null;
		}
		
		// profitable route, change startTime
		PPlan newPlan = new PPlan(new IdImpl(cooperative.getCurrentIteration()));
		newPlan.setStopsToBeServed(cooperative.getBestPlan().getStopsToBeServed());
		newPlan.setStartTime(cooperative.getBestPlan().getStartTime());
		
		// get a valid new end time
		double newEndTime = Math.min(24 * 3600.0, cooperative.getBestPlan().getEndTime() + (-0.5 + MatsimRandom.getRandom().nextDouble()) * this.mutationRange);
		newEndTime = Math.max(newEndTime, cooperative.getBestPlan().getStartTime() + cooperative.getMinOperationTime());
		newPlan.setEndTime(newEndTime);
		
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan.getStartTime(), newPlan.getEndTime(), 1, newPlan.getStopsToBeServed(), new IdImpl(cooperative.getCurrentIteration())));
		
		return newPlan;
	}

	@Override
	public String getName() {
		return RandomEndTimeAllocator.STRATEGY_NAME;
	}

}
