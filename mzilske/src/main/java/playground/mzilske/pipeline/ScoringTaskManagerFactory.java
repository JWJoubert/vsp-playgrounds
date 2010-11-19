package playground.mzilske.pipeline;

import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;

public class ScoringTaskManagerFactory extends TaskManagerFactory {

	@Override
	protected TaskManager createTaskManagerImpl(Config config) {
		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory(config.charyparNagelScoring());
		return new ScoringTaskManager(scoringFunctionFactory, config.charyparNagelScoring().getLearningRate());
	}

}
