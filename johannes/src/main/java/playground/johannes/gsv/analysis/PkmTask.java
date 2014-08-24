package playground.johannes.gsv.analysis;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;

import playground.johannes.coopsim.analysis.LegModeCondition;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.analysis.TrajectoryUtils;
import playground.johannes.coopsim.analysis.TripDistanceTotal;
import playground.johannes.coopsim.pysical.Trajectory;

public class PkmTask extends TrajectoryAnalyzerTask {

	private final ActivityFacilities facilities;
	
	public PkmTask(ActivityFacilities facilities) {
		this.facilities = facilities;
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Set<String> modes = TrajectoryUtils.getModes(trajectories);
		modes.add(null);
		
		TripDistanceTotal dist = new TripDistanceTotal(facilities);
		for(String mode : modes) {
			if(mode != null) {
				dist.setCondition(new LegModeCondition(mode));
			}
			TObjectDoubleHashMap<Trajectory> dists = dist.values(trajectories);
			TObjectDoubleIterator<Trajectory> it = dists.iterator();
			double sum = 0;
			for(int i = 0; i < dists.size(); i++) {
				it.advance();
				sum += it.value();
			}
			
			if(mode == null) {
				mode = "all";
			}
			
			DescriptiveStatistics stats = new DescriptiveStatistics();
			stats.addValue(sum);
			results.put(String.format("pkm.%s", mode), stats);
		}
	}

}
