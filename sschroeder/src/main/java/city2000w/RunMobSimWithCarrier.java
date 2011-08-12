package city2000w;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mzilske.city2000w.City2000WMobsimFactory;
import playground.mzilske.freight.CarrierAgent;
import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.api.CarrierAgentFactory;
import freight.CarrierPlanReader;


public class RunMobSimWithCarrier implements StartupListener, BeforeMobsimListener{
	
	static class SimpleCarrierAgentFactory implements CarrierAgentFactory {

		private PlanAlgorithm router;
		
		public void setRouter(PlanAlgorithm router){
			this.router = router;
		}
		
		@Override
		public CarrierAgent createAgent(CarrierAgentTracker tracker,CarrierImpl carrier) {
			CarrierAgent agent = new CarrierAgent(tracker, carrier, router);
			return agent;
		}
		
	}
	private static Logger logger = Logger.getLogger(RunMobSimWithCarrier.class);
	
	private static String NETWORK_FILENAME;
	
	private static String PLAN_FILENAME;
	
	private ScenarioImpl scenario;
	
	private CarrierAgentTracker carrierAgentTracker;
	
	public static void main(String[] args) {
		RunMobSimWithCarrier mobSim = new RunMobSimWithCarrier();
		mobSim.run();
	}
	
	private void run(){
		logger.info("run");
		init();
		Config config = new Config();
		config.addCoreModules();
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.addSimulationConfigGroup(new SimulationConfigGroup());
		config.simulation().setEndTime(12*3600);
		scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		readNetwork(NETWORK_FILENAME);
		Controler controler = new Controler(scenario);
		controler.setCreateGraphs(false);
		controler.addControlerListener(this);
		controler.setOverwriteFiles(true);
		
		controler.run();
	}

	private void init() {
		logger.info("initialise model");
		NETWORK_FILENAME = "../playgrounds/sschroeder/networks/grid.xml";
	}

	private void readNetwork(String networkFilename) {
		new MatsimNetworkReader(scenario).readFile(networkFilename);
	}

	public void notifyStartup(StartupEvent event) {
		Collection<CarrierImpl> carrierImpls = new ArrayList<CarrierImpl>();
		new CarrierPlanReader(carrierImpls).read("../playgrounds/sschroeder/output/testCarriers.xml");
		PlanAlgorithm router = event.getControler().createRoutingAlgorithm();
		SimpleCarrierAgentFactory agentFactory = new SimpleCarrierAgentFactory();
		agentFactory.setRouter(router);
		carrierAgentTracker = new CarrierAgentTracker(carrierImpls, router, scenario.getNetwork(), agentFactory);
		City2000WMobsimFactory mobsimFactory = new City2000WMobsimFactory(0, carrierAgentTracker);
		mobsimFactory.setUseOTFVis(true);
		event.getControler().setMobsimFactory(mobsimFactory);
	}
	
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Controler controler = event.getControler();
		controler.getEvents().addHandler(carrierAgentTracker);
		carrierAgentTracker.createPlanAgents();
	}

}
