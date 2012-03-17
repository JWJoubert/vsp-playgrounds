package playground.mmoyo.zz_archive.TransitSimulation;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.run.OTFVis;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

public class MMoyoTransitControler extends Controler {
	boolean launchOTFDemo=false;
	private Config config;
	
	public MMoyoTransitControler(final ScenarioImpl scenario, boolean launchOTFDemo){
		super(scenario);
		this.config = scenario.getConfig();
		this.setOverwriteFiles(true);   
		this.launchOTFDemo = launchOTFDemo;
	}
	
	@Override
	protected void runMobSim() {
		QSim sim = QSim.createQSimAndAddAgentSource(this.scenarioData, this.events);
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(this.scenarioData.getConfig(), this.scenarioData, events, sim);
		OTFClientLive.run(this.scenarioData.getConfig(), server);

		sim.run();
		/*
		TransitQueueSimulation sim = new TransitQueueSimulation(this.scenarioData, this.events);
		sim.startOTFServer("livesim");
		new OnTheFlyClientQuad("rmi:127.0.0.1:4019:" + "livesim").start();
		sim.run();
		*/
	}

	@Override
	public PlanAlgorithm createRoutingAlgorithm(final PersonalizableTravelDisutility travelCosts, final PersonalizableTravelTime travelTimes) {
		return new MMoyoPlansCalcTransitRoute(this.config.plansCalcRoute(), this.network, travelCosts, travelTimes,
				this.getLeastCostPathCalculatorFactory(), ((PopulationFactoryImpl) this.population.getFactory()).getModeRouteFactory(), this.scenarioData.getTransitSchedule(), new TransitConfigGroup());
	}
	
	public static void main(final String[] args) {
		if (args.length > 0) {
			ScenarioLoaderImpl scenarioLoader = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(args[0]); //load from configFile
			ScenarioImpl scenario = (ScenarioImpl) scenarioLoader.getScenario();
			scenarioLoader.loadScenario();
			new MMoyoTransitControler(scenario, true).run();
		} 
	}
	
}
