package playground.wrashid.parkingSearch.withindayFW.core;

import org.matsim.api.core.v01.Id;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

import playground.wrashid.lib.obj.TwoKeyHashMapsWithDouble;

public class ParkingStrategy {


	private TwoKeyHashMapsWithDouble<Id, Integer> score;
	
	private final DuringLegIdentifier identifier;

	public ParkingStrategy(DuringLegIdentifier identifier) {
		this.identifier = identifier;
		score=new TwoKeyHashMapsWithDouble<Id, Integer>();
	}

	public void putScore(Id agentId, int legPlanElementIndex, double score){
		this.score.put(agentId, legPlanElementIndex, score);
	}
	
	public Double getScore(Id agentId, int legPlanElementIndex){
		return score.get(agentId, legPlanElementIndex);
	}
	
	public void removeScore(Id agentId, int legPlanElementIndex){
		this.score.get(agentId).remove(legPlanElementIndex);
	}

	public DuringLegIdentifier getIdentifier() {
		return identifier;
	}
	
	
}
