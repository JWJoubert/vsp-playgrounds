package playground.pieter.pseudosimulation.replanning.modules;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.ReplanningContext;

import playground.pieter.pseudosimulation.controler.PSimControler;

public class PSimPlanMarkerModule implements PlanStrategyModule {

	private PSimControler controler;

	public PSimPlanMarkerModule(PSimControler controler) {
		this.controler = controler;
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {

	}

	@Override
	public void handlePlan(Plan plan) {
		controler.addPlanForPseudoSimulation(plan);
	}

	@Override
	public void finishReplanning() {

	}

}
