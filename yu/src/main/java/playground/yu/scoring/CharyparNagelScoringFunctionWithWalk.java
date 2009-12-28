/**
 * 
 */
package playground.yu.scoring;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;

/**
 * change scoring function, because "walk"-mode will be implemented
 * 
 * @author yu
 * 
 */
public class CharyparNagelScoringFunctionWithWalk extends LegScoringFunction {
	private static double offsetWlk = 6.0;

	public CharyparNagelScoringFunctionWithWalk(Plan plan,
			final CharyparNagelScoringParameters params) {
		super(plan, params);
	}

	@Override
	protected double calcLegScore(double departureTime, double arrivalTime,
			LegImpl leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in
		// seconds
		// double dist = 0.0; // distance in meters

		// in our current tests, marginalUtilityOfDistance always == 0.0.
		// if (marginalUtilityOfDistance != 0.0) {
		// /* we only as for the route when we have to calculate a distance
		// cost,
		// * because route.getDist() may calculate the distance if not yet
		// * available, which is quite an expensive operation
		// */
		// Route route = leg.getRoute();
		// dist = route.getDist();
		// /* TODO the route-distance does not contain the length of the first
		// or
		// * last link of the route, because the route doesn't know those.
		// Should
		// * be fixed somehow, but how? MR, jan07
		// */
		// /* TODO in the case of within-day replanning, we cannot be sure that
		// the
		// * distance in the leg is the actual distance driven by the agent.
		// */
		// }

		if (TransportMode.car.equals(leg.getMode())) {
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling;
		} else if (TransportMode.pt.equals(leg.getMode())) {
			tmpScore += travelTime * (-3.0) / 3600.0;
		} else if (TransportMode.walk.equals(leg.getMode())) {
			tmpScore += offsetWlk + travelTime * (-18.0) / 3600.0;
		} else {
			// use the same values as for "car"
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling;
		}

		return tmpScore;
	}

}
