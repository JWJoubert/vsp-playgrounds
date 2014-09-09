package playground.pieter.pseudosimulation.controler.listeners;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.population.PersonImpl;

import playground.pieter.pseudosimulation.controler.PSimControler;

/**
 * @author fouriep
 *         <P>
 *         Because psim is set to only execute newly formed plans from plan
 *         mutators, all other selected plans in the population get a score of
 *         zero when scoring is done, because these plans don't generate any
 *         events.
 * 
 *         <P>
 *         The current solution to this is to record the plan scores of agents
 *         not selected for psim in a map in the {@link PSimControler} when
 *         a psim iteration starts (see
 *         {@link BeforePSimSelectedPlanScoreRecorder}), then restore the scores
 *         after scoring is run (to get an idea of the impact of the psim
 *         iteration on the score).

 */
public class AfterScoringSelectedPlanScoreRestoreListener implements
		ScoringListener {
	PSimControler c;

	public AfterScoringSelectedPlanScoreRestoreListener(PSimControler c) {
		super();
		this.c = c;
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		if (MobSimSwitcher.isQSimIteration)
			return;
		restoreScores();
	}


	private void restoreScores() {
		HashMap<IdImpl, Double> nSASS = c
				.getNonSimulatedAgentSelectedPlanScores();
		Map<Id<Person>, ? extends Person> persons = c.getMATSimControler().getPopulation().getPersons();
		// double selectedPlanScoreAvg = 0;
		// int i = 0;
		for (IdImpl id : nSASS.keySet()) {
			// i++;
			// try {
			// selectedPlanScoreAvg += ((PersonImpl) persons.get(id))
			// .getSelectedPlan().getScore();
			// } catch (NullPointerException e) {
			// selectedPlanScoreAvg += 0;
			// }
			((PersonImpl) persons.get(id)).getSelectedPlan().setScore(
					nSASS.get(id));
		}
		// Logger.getLogger(getClass()).error(selectedPlanScoreAvg / (double) i
		// + "from " + i);
	}

}
