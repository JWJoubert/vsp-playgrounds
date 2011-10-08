package freight.vrp.vrpTestInstances;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.mzilske.freight.carrier.Carrier;
import playground.mzilske.freight.carrier.CarrierPlan;
import playground.mzilske.freight.carrier.CarrierPlanWriter;
import playground.mzilske.freight.carrier.CarrierShipment;
import playground.mzilske.freight.carrier.CarrierVehicle;
import vrp.algorithms.ruinAndRecreate.constraints.CapacityConstraint;
import vrp.algorithms.ruinAndRecreate.factories.StandardRuinAndRecreateFactory;
import vrp.api.Constraints;
import vrp.basics.CrowFlyDistance;
import vrp.basics.SingleDepotSolutionFactoryImpl;
import vrp.basics.Tour;
import vrp.basics.Vehicle;
import city2000w.VRPCarrierPlanBuilder;
import freight.vrp.ChartListener;
import freight.vrp.RRSingleDepotVRPSolver;
import freight.vrp.VRPSolver;
import freight.vrp.VRPSolverFactory;

public class ChristophidesMingozziTothPlanCreator {
	
	static class MySolverFactory implements VRPSolverFactory {

		@Override
		public VRPSolver createSolver(Collection<CarrierShipment> shipments,Collection<CarrierVehicle> carrierVehicles, Network network) {
			RRSingleDepotVRPSolver solver = new RRSingleDepotVRPSolver(shipments, carrierVehicles, network);
			CrowFlyDistance costs = new CrowFlyDistance();
			costs.speed = 1;
			StandardRuinAndRecreateFactory ruinAndRecreateFactory = new StandardRuinAndRecreateFactory();
			ChartListener chartListener = new ChartListener();
			chartListener.setFilename("vrp/vrp.png");
			ruinAndRecreateFactory.addRuinAndRecreateListener(chartListener);
			solver.setRuinAndRecreateFactory(ruinAndRecreateFactory);
			solver.setCosts(costs);
			solver.setConstraints(new CapacityConstraint());
			solver.setIniSolutionFactory(new SingleDepotSolutionFactoryImpl());
			solver.setnOfWarmupIterations(20);
			solver.setnOfIterations(500);
			return solver;
		}
		
	}
	
	
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(Level.INFO);
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile("vrp/grid1000.xml");
		Collection<Carrier> carriers = new ArrayList<Carrier>();
		ChristophidesMingozziTothCarrierCreator carrierCreator = new ChristophidesMingozziTothCarrierCreator(carriers, scenario.getNetwork(), 1000);
		carrierCreator.createCarriers("/Users/stefan/Documents/workspace/VehicleRouting/instances/vrp_christofides_mingozzi_toth/vrpnc2.txt");
		
		for(Carrier carrier : carriers){
			VRPCarrierPlanBuilder planBuilder = new VRPCarrierPlanBuilder(carrier.getCarrierCapabilities(), carrier.getContracts(), scenario.getNetwork());
			planBuilder.setVrpSolverFactory(new MySolverFactory());
			CarrierPlan plan = planBuilder.buildPlan();
			carrier.setSelectedPlan(plan);
		}
		
		
		new CarrierPlanWriter(carriers).write("vrp/christophidesMingozziToth_vrpnc2_plans.xml");
	}

}
