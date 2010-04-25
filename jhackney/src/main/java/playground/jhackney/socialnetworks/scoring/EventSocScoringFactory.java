package playground.jhackney.socialnetworks.scoring;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;



public class EventSocScoringFactory implements ScoringFunctionFactory {

	private String factype;
	private LinkedHashMap<Activity,ArrayList<Double>> actStats;
	private ScoringFunctionFactory factory;

	public EventSocScoringFactory(String factype, ScoringFunctionFactory sf, LinkedHashMap<Activity,ArrayList<Double>> actStats) {
		this.factype=factype;
		this.actStats=actStats;
		this.factory=sf;

	}

	public ScoringFunction createNewScoringFunction(final Plan plan) {
//		return new SNScoringMaxFriendFoeRatio(plan, this.factype, this.scorer);
		return new EventSocScoringFunction(plan, this.factory.createNewScoringFunction(plan), factype, actStats);
	}


}