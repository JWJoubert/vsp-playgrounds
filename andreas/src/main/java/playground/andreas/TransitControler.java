package playground.andreas;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.mobsim.framework.ObservableMobsim;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.pt.qsim.ComplexTransitStopHandlerFactory;
import org.matsim.pt.qsim.TransitQSimEngine;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.AgentFactory;
import org.matsim.ptproject.qsim.agents.DefaultAgentFactory;
import org.matsim.ptproject.qsim.agents.PopulationAgentSource;
import org.matsim.ptproject.qsim.agents.TransitAgentFactory;
import org.matsim.ptproject.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.ptproject.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.ptproject.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.andreas.fixedHeadway.FixedHeadwayControler;
import playground.andreas.fixedHeadway.FixedHeadwayCycleUmlaufDriverFactory;

/**
 * @author aneumann
 */
public class TransitControler extends Controler {

	private final static Logger log = Logger.getLogger(TransitControler.class);

	private boolean useOTFVis = true;
	private boolean useHeadwayControler = false;
	
	public TransitControler(Config config) {
		super(config);
	}
	
	@Override
	protected void runMobSim() {
		
		log.info("Overriding runMobSim()");

        Scenario sc = this.getScenario();EventsManager eventsManager = this.getEvents();

        QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }

        // Get number of parallel Threads
        int numOfThreads = conf.getNumberOfThreads();
        QNetsimEngineFactory netsimEngFactory;
        if (numOfThreads > 1) {
            eventsManager = new SynchronizedEventsManagerImpl(eventsManager);
            netsimEngFactory = new ParallelQNetsimEngineFactory();
        } else {
            netsimEngFactory = new DefaultQSimEngineFactory();
        }
        QSim qSim = new QSim(sc, eventsManager, netsimEngFactory);
        AgentFactory agentFactory = new TransitAgentFactory(qSim);
        TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
        transitEngine.setUseUmlaeufe(true);
        transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
        qSim.addDepartureHandler(transitEngine);
        qSim.addAgentSource(transitEngine);


        PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
        qSim.addAgentSource(agentSource);

        transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
//		this.events.addHandler(new LogOutputEventHandler());

		

		if(this.useHeadwayControler){
			transitEngine.setAbstractTransitDriverFactory(new FixedHeadwayCycleUmlaufDriverFactory());
			this.events.addHandler(new FixedHeadwayControler(qSim, transitEngine));
		}

        for (MobsimListener l : this.getQueueSimulationListener()) {
            qSim.addQueueSimulationListeners(l);
        }
        if (this.useOTFVis) {
			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(config,getScenario(), events, qSim);
			OTFClientLive.run(config, server);
		}
		qSim.run();
	}	
	
	void setUseOTFVis(boolean useOTFVis) {
		this.useOTFVis = useOTFVis;
	}

	public static void main(final String[] args) {
		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile(args[0]);
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		
				
		TransitControler tc = new TransitControler(config);
		if(args.length > 1 && args[1].equalsIgnoreCase("true")){
			tc.setUseOTFVis(true);
		}
		tc.setOverwriteFiles(true);
//		tc.setCreateGraphs(false);
		tc.run();
	}
}
