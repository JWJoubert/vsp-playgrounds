package playground.balac.onewaycarsharingredisgned.qsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import playground.balac.onewaycarsharingredisgned.config.OneWayCarsharingRDConfigGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

public class OneWayCarsharingRDQsimFactory implements MobsimFactory{

	private final static Logger log = Logger.getLogger(QSimFactory.class);

	private final Scenario scenario;
	private final Controler controler;
	private final ArrayList<OneWayCarsharingRDStation> owvehiclesLocation;

	public OneWayCarsharingRDQsimFactory(final Scenario scenario, final Controler controler) throws IOException {
		
		this.scenario = scenario;
		this.controler = controler;
		owvehiclesLocation = new ArrayList<OneWayCarsharingRDStation>();
		readVehicleLocations();
	}
	public void readVehicleLocations() throws IOException {
		
		
		final OneWayCarsharingRDConfigGroup configGroupow = (OneWayCarsharingRDConfigGroup)
				scenario.getConfig().getModule( OneWayCarsharingRDConfigGroup.GROUP_NAME );
		
		
		BufferedReader reader;
		String s;
		
		LinkUtils linkUtils = new LinkUtils(controler.getNetwork());
		
	
		if (configGroupow.useOneWayCarsharing()) {
		reader = IOUtils.getBufferedReader(configGroupow.getvehiclelocations());
		s = reader.readLine();
		    s = reader.readLine();
		    int i = 1;
		    while(s != null) {
		    	
		    	String[] arr = s.split("\t", -1);
		    
		    	CoordImpl coordStart = new CoordImpl(arr[2], arr[3]);
				Link l = linkUtils.getClosestLink(coordStart);		    	
				ArrayList<String> vehIDs = new ArrayList<String>();
		    	
		    	for (int k = 0; k < Integer.parseInt(arr[6]); k++) {
		    		vehIDs.add(Integer.toString(i));
		    		i++;
		    	}
		    	OneWayCarsharingRDStation f = new OneWayCarsharingRDStation(l, Integer.parseInt(arr[6]), vehIDs);
		    	
		    	owvehiclesLocation.add(f);
		    	s = reader.readLine();
		    	
		    }	 
		}
		
	}
	@Override
	public Netsim createMobsim(Scenario sc, EventsManager eventsManager) {
		
		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		QSim qSim = new QSim(sc, eventsManager);
		
		ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
        QNetsimEngineModule.configure(qSim);
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);

		AgentFactory agentFactory = null;
		OneWayCarsharingRDVehicleLocation owvehiclesLocationqt = null;

		
		if (sc.getConfig().scenario().isUseTransit()) {
			agentFactory = new TransitAgentFactory(qSim);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		} else {
			
			
			try {
				owvehiclesLocationqt = new OneWayCarsharingRDVehicleLocation(controler, owvehiclesLocation);

				agentFactory = new OneWayCarsharingRDAgentFactory(qSim, scenario, controler, owvehiclesLocationqt);
			
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		
		ParkOWVehicles parkSource = new ParkOWVehicles(sc.getPopulation(), agentFactory, qSim,  owvehiclesLocationqt);
		
		qSim.addAgentSource(agentSource);
		
		qSim.addAgentSource(parkSource);
		
		return qSim;
	}
	
	private class LinkUtils {
		
		Network network;
		public LinkUtils(Network network) {
			
			this.network = network;		}
		
		public LinkImpl getClosestLink(Coord coord) {
			
			double distance = (1.0D / 0.0D);
		    Id closestLinkId = new IdImpl(0L);
		    for (Link link : network.getLinks().values()) {
		      LinkImpl mylink = (LinkImpl)link;
		      Double newDistance = Double.valueOf(mylink.calcDistance(coord));
		      if (newDistance.doubleValue() < distance) {
		        distance = newDistance.doubleValue();
		        closestLinkId = link.getId();
		      }

		    }

		    return (LinkImpl)network.getLinks().get(closestLinkId);
			
			
		}
	}
	
	
}
