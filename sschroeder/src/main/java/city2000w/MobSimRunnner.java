/**
 * 
 */
package city2000w;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.mzilske.city2000w.City2000WMobsimFactory;
import playground.mzilske.freight.CarrierAgentFactory;
import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.Carriers;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TransportServiceProviders;
import freight.AnotherCarrierAgentFactory;
import freight.CarrierPlanReader;
import freight.CarrierPlanWriter;
import freight.CarrierUtils;
import freight.TSPPlanReader;

/**
 * @author schroeder
 *
 */
public class MobSimRunnner implements StartupListener, ScoringListener, ReplanningListener, BeforeMobsimListener, AfterMobsimListener, IterationEndsListener, ShutdownListener {

	static class ConfigData {
		private String propertyName;
		private String value;
		public ConfigData(String propertyName, String value) {
			super();
			this.propertyName = propertyName;
			this.value = value;
		}
		public String getPropertyName() {
			return propertyName;
		}
		public String getValue() {
			return value;
		}
		
		
	}
	
	private static Logger logger = Logger.getLogger(MobSimRunnner.class);

	private Carriers carriers;

	private CarrierAgentTracker carrierAgentTracker;
	
	private TSPAgentTracker tspAgentTracker;
	
	private TransportServiceProviders tsProviders;

	private ScenarioImpl scenario;
	
	private String NETWORK_FILENAME;

	private String CARRIERPLANFILE;
	
	private static final String TSPPLANFILE = "../Diplomarbeit_Matthias/output/tspPlans.xml";
	
	private boolean liveModus = true;
	
	private boolean carrierMode = true;
	
	private boolean tspMode = false;
	
	private String outputDirectory = "./output/";
	
	private List<ConfigData> configs = new ArrayList<ConfigData>();


	public void setTspMode(boolean tspMode) {
		this.tspMode = tspMode;
		if(tspMode){
			carrierMode = false;
		}
		else{
			carrierMode = true;
		}
	}


	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		MobSimRunnner runner = new MobSimRunnner();
		runner.setTspMode(false);
		runner.init(args[0]);
		runner.run();
	}

	
	private void init(String filename) throws FileNotFoundException, IOException {
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		String line = null;
		while((line = reader.readLine()) != null){
			String[] tokens = line.split("=");
			ConfigData configDate = new ConfigData(tokens[0], tokens[1]);
			configs.add(configDate);
		}
		reader.close();
		NETWORK_FILENAME = getConfigFor("NETWORKFILENAME");
		CARRIERPLANFILE = getConfigFor("CARRIERPLANFILENAME");
		
	}


	private String getConfigFor(String string) {
		for(ConfigData date : configs){
			if(date.getPropertyName().equals(string)){
				return date.getValue();
			}
		}
		return null;
	}


	public void notifyStartup(StartupEvent event) {
//		logger.getRootLogger().setLevel(Level.ERROR);
		Controler controler = event.getControler();
		
		readCarrierPlans();
		
		if(tspMode){
			readTSPPlans();
			tspAgentTracker = new TSPAgentTracker(tsProviders.getTransportServiceProviders());
			createCarrierPlans();
		}
		else{
			
		}
		
		CarrierAgentFactory carrierAgentFactory = new AnotherCarrierAgentFactory(scenario.getNetwork(), controler.createRoutingAlgorithm());
		carrierAgentTracker = new CarrierAgentTracker(carriers.getCarriers().values(), controler.createRoutingAlgorithm(), scenario.getNetwork(), carrierAgentFactory);
		carrierAgentTracker.getShipmentStatusListeners().add(tspAgentTracker);
		carrierAgentTracker.getCostListeners().add(tspAgentTracker);

			
		City2000WMobsimFactory mobsimFactory = new City2000WMobsimFactory(0, carrierAgentTracker);
		mobsimFactory.setUseOTFVis(liveModus);
		event.getControler().setMobsimFactory(mobsimFactory);

	}


	private void createCarrierPlans() {
		Collection<Contract> contracts = tspAgentTracker.createCarrierContracts();
		for(Contract c : contracts){
			Id carrierId = c.getOffer().getCarrierId();
			CarrierImpl carrier = carriers.getCarriers().get(carrierId);
			if(carrier == null){
				throw new NullPointerException("carrier " + carrierId.toString() + " does not exist in carrierList");
			}
			CarrierUtils.createAndAddContract(carrier, c.getShipment(), c.getOffer());
		}
		for(CarrierImpl carrier : carriers.getCarriers().values()){
			SimpleCarrierPlanBuilder planBuilder = new SimpleCarrierPlanBuilder();
			carrier.setSelectedPlan(planBuilder.buildPlan(carrier));
		}
	}

//
	private void readTSPPlans() {
		tsProviders = new TransportServiceProviders();
		TSPPlanReader planReader = new TSPPlanReader(tsProviders.getTransportServiceProviders());
		planReader.read(TSPPLANFILE);
	}


	private void readCarrierPlans() {
		carriers = new Carriers();
		Collection<CarrierImpl> carrierCol = new ArrayList<CarrierImpl>();
		CarrierPlanReader planReader = new CarrierPlanReader(carrierCol);
		planReader.read(CARRIERPLANFILE);
		for(CarrierImpl c : carrierCol){
			carriers.getCarriers().put(c.getId(), c);
		}
	}


	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		carrierAgentTracker.createPlanAgents();
		Controler controler = event.getControler();
		controler.getEvents().addHandler(carrierAgentTracker);
	}

	public void notifyAfterMobsim(AfterMobsimEvent event) {
		Controler controler = event.getControler();
		controler.getEvents().removeHandler(carrierAgentTracker);
	}


	public void notifyScoring(ScoringEvent event) {

	}

	public void notifyIterationEnds(IterationEndsEvent event) {
//		Collection<Plan> plans = new ArrayList<Plan>();
//		String activityShpFilename = outputDirectory + event.getIteration() + ".activities.shp";
//		String legShpFilename = outputDirectory + event.getIteration() + ".legs.shp";
	}

	public void notifyReplanning(ReplanningEvent event) {
		
	}


	public void notifyShutdown(ShutdownEvent event)  {
		CarrierPlanWriter planWriter = new CarrierPlanWriter(carriers.getCarriers().values());
		planWriter.write(outputDirectory + "newCarrierPlans.xml");
	}

	private void run(){
		Config config = new Config();
		config.addCoreModules();
		
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		scenario = (ScenarioImpl)ScenarioUtils.loadScenario(config);
		
		readNetwork(NETWORK_FILENAME);
		Controler controler = new Controler(scenario);
		/*
		 * muss ich auf 'false' setzen, da er mir sonst eine exception wirft, weil er das matsim-logo nicht finden kann
		 * ich hab keine ahnung wo ich den pfad des matsim-logos setzen kann
		 * 
		 */
		
		controler.setCreateGraphs(false);
		controler.addControlerListener(this);
		controler.setOverwriteFiles(true);
		controler.run();
	}

	private void readNetwork(String networkFilename) {
		new MatsimNetworkReader(scenario).readFile(networkFilename);
	}

}
