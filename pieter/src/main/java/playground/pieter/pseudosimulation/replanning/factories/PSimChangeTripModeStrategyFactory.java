package playground.pieter.pseudosimulation.replanning.factories;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ChangeTripModeStrategyFactory;

import playground.pieter.pseudosimulation.controler.PSimControler;
import playground.pieter.pseudosimulation.replanning.modules.PSimPlanMarkerModule;

public class PSimChangeTripModeStrategyFactory extends
		ChangeTripModeStrategyFactory {

	public PSimChangeTripModeStrategyFactory(PSimControler controler) {
		this.controler = controler;
	}

	private PSimControler controler;

	@Override
	public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager ) {
		PlanStrategyImpl strategy = (PlanStrategyImpl) super.createPlanStrategy(scenario,eventsManager);
		strategy.addStrategyModule(new PSimPlanMarkerModule(controler));
		return strategy;
	}


}
