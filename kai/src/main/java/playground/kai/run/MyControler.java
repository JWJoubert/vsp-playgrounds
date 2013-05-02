package playground.kai.run;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.matsim4opus.analysis.KaiAnalysisListener;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.OTFVisConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNode;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFFileWriterFactory;
import org.matsim.vis.otfvis.OnTheFlyServer;

class MyControler {
	
	public static void main ( String[] args ) {
		Logger.getLogger("blabla").warn("here") ;
		
		// prepare the config:
		Config config = ConfigUtils.loadConfig( args[0] ) ;
		config.vspExperimental().setRemovingUnneccessaryPlanAttributes(true) ;
		
		// prepare the scenario
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		// prepare the control(l)er:
		Controler controler = new Controler( scenario ) ;
		controler.setOverwriteFiles(true) ;
		controler.addControlerListener(new KaiAnalysisListener()) ;
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
//		controler.setMobsimFactory(new OldMobsimFactory()) ;

		// run everything:
		controler.run();
	
	}
	
//	static class OldMobsimFactory implements MobsimFactory {
//		@Override
//		public Mobsim createMobsim( Scenario sc, EventsManager eventsManager ) {
//			return new QueueSimulation(sc, eventsManager);
//		}
//	}
	
	static class PatnaMobsimFactory implements MobsimFactory {
		private boolean useOTFVis = false ;

		@Override
		public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
			
	        QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
	        if (conf == null) {
	            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
	        }

	        // construct the QSim:
			QSim qSim = new QSim(sc, eventsManager);

			// add the actsim engine:
			ActivityEngine activityEngine = new ActivityEngine();
			qSim.addMobsimEngine(activityEngine);
			qSim.addActivityHandler(activityEngine);

			// add the netsim engine:
			NetsimNetworkFactory<QNode, QLinkImpl> netsimNetworkFactory = new NetsimNetworkFactory<QNode, QLinkImpl>() {
				@Override
				public QLinkImpl createNetsimLink(final Link link, final QNetwork network, final QNode toQueueNode) {
					return new QLinkImpl(link, network, toQueueNode, new PassingVehicleQ());
				}
				@Override
				public QNode createNetsimNode(final Node node, QNetwork network) {
					return new QNode(node, network);
				}
			};
			QNetsimEngine netsimEngine = new QNetsimEngine(qSim, netsimNetworkFactory) ;
//			QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim, MatsimRandom.getRandom());
			qSim.addMobsimEngine(netsimEngine);
			qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

			TeleportationEngine teleportationEngine = new TeleportationEngine();
			qSim.addMobsimEngine(teleportationEngine);
	        
			AgentFactory agentFactory;
//	        if (sc.getConfig().scenario().isUseTransit()) {
//	            agentFactory = new TransitAgentFactory(qSim);
//	            TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
//	            transitEngine.setUseUmlaeufe(true);
//	            transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
//	            qSim.addDepartureHandler(transitEngine);
//	            qSim.addAgentSource(transitEngine);
//	            qSim.addMobsimEngine(transitEngine);
//	        } else {
			agentFactory = new DefaultAgentFactory(qSim);
//	        }
//	        if (sc.getConfig().network().isTimeVariantNetwork()) {
//				qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
//			}

	        PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
	        Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();

	        VehicleType car = VehicleUtils.getFactory().createVehicleType(new IdImpl("car"));
	        car.setMaximumVelocity(60.0/3.6);
	        car.setPcuEquivalents(1.0);
	        modeVehicleTypes.put("car", car);
	        
	        VehicleType bike = VehicleUtils.getFactory().createVehicleType(new IdImpl("bike"));
	        bike.setMaximumVelocity(60.0/3.6);
	        bike.setPcuEquivalents(0.25);
	        modeVehicleTypes.put("bike", bike);
	        
	        VehicleType bicycles = VehicleUtils.getFactory().createVehicleType(new IdImpl("bicycle"));
	        bicycles.setMaximumVelocity(15.0/3.6);
	        bicycles.setPcuEquivalents(0.05);
	        modeVehicleTypes.put("bicycle", bicycles);

	        VehicleType walks = VehicleUtils.getFactory().createVehicleType(new IdImpl("walk"));
	        walks.setMaximumVelocity(1.5);
	        walks.setPcuEquivalents(0.10);  			// assumed pcu for walks is 0.1
	        modeVehicleTypes.put("walk", walks);
	        
			agentSource.setModeVehicleTypes(modeVehicleTypes);
	        
	        qSim.addAgentSource(agentSource);
			
			if ( useOTFVis ) {
				// otfvis configuration.  There is more you can do here than via file!
				final OTFVisConfigGroup otfVisConfig = qSim.getScenario().getConfig().otfVis();
				otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
				//				otfVisConfig.setShowParking(true) ; // this does not really work

				OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, eventsManager, qSim);
				OTFClientLive.run(sc.getConfig(), server);
			}
			return qSim ;
		}
	}
}
