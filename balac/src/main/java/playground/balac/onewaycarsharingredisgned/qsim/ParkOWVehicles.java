package playground.balac.onewaycarsharingredisgned.qsim;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;




public class ParkOWVehicles implements AgentSource {
	private Population population;
	private AgentFactory agentFactory;
	private QSim qsim;
	private Map<String, VehicleType> modeVehicleTypes;
	private Collection<String> mainModes;
	private boolean insertVehicles = true;
	private OneWayCarsharingRDVehicleLocation owvehiclesLocationqt;
	public ParkOWVehicles(Population population, AgentFactory agentFactory, QSim qsim,
			OneWayCarsharingRDVehicleLocation owvehiclesLocationqt) {
		this.population = population;
		this.agentFactory = agentFactory;
		this.qsim = qsim;  
		this.modeVehicleTypes = new HashMap<String, VehicleType>();
		this.mainModes = qsim.getScenario().getConfig().qsim().getMainModes();
		this.owvehiclesLocationqt = owvehiclesLocationqt;
		for (String mode : mainModes) {
			modeVehicleTypes.put(mode, VehicleUtils.getDefaultVehicleType());
		}
	}
	
	@Override
	public void insertAgentsIntoMobsim() {
		// TODO Auto-generated method stub
		if (owvehiclesLocationqt != null)
		for (OneWayCarsharingRDStation owstation: owvehiclesLocationqt.getQuadTree().values()) {
			
			for (String id:owstation.getIDs()) {
				qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(new IdImpl("OW_"+(id)), modeVehicleTypes.get("onewaycarsharing")), owstation.getLink().getId());

			}
			
		}
		
		
	}

}
