package playground.balac.freefloating.qsimParkingModule;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingCoordInfo;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.network.NetworkImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class ParkFFVehicles implements AgentSource {
	private Population population;
	private AgentFactory agentFactory;
	private QSim qsim;
	private Map<String, VehicleType> modeVehicleTypes;
	private Collection<String> mainModes;
	private boolean insertVehicles = true;
	private Collection<ParkingCoordInfo> freefloatingCars;
	private Scenario scenario;
	public ParkFFVehicles(Population population, AgentFactory agentFactory, QSim qsim,
			Collection<ParkingCoordInfo> freefloatingCars, Scenario scenario) {
		this.population = population;
		this.agentFactory = agentFactory;
		this.qsim = qsim;  
		this.scenario = scenario;
		this.modeVehicleTypes = new HashMap<String, VehicleType>();
		this.mainModes = qsim.getScenario().getConfig().qsim().getMainModes();
		this.freefloatingCars = freefloatingCars;
		for (String mode : mainModes) {
			modeVehicleTypes.put(mode, VehicleUtils.getDefaultVehicleType());
		}
	}
	
	@Override
	public void insertAgentsIntoMobsim() {
		// TODO Auto-generated method stub
		if (freefloatingCars != null)
		for (ParkingCoordInfo ffstation: freefloatingCars) {
			NetworkImpl net = (NetworkImpl) scenario.getNetwork();
			Link link = net.getNearestLink(ffstation.getParkingCoordinate());
			qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(new IdImpl("FF_"+(ffstation.getVehicleId().toString())), modeVehicleTypes.get("freefloating")), link.getId());

			
			
		}
		
		
	}

}
