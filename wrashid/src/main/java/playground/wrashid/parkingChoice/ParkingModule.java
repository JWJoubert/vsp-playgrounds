package playground.wrashid.parkingChoice;

import java.util.LinkedList;

import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.parkingChoice.infrastructure.Parking;
import playground.wrashid.parkingChoice.scoring.ParkingScoreAccumulator;
import playground.wrashid.parkingChoice.scoring.ParkingScoreCollector;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;

public class ParkingModule {

	
	private final Controler controler;
	private ParkingScoreAccumulator parkingScoreAccumulator;

	public ParkingModule(Controler controler, LinkedList<Parking> parkingCollection){
		this.controler = controler;
		
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		controler.addControlerListener(eventHandlerAtStartupAdder);
		
		// TODO: remove this in refactoring, just here due to the output graph
		// class: playground.wrashid.parkingSearch.planLevel.analysis.ParkingWalkingDistanceMeanAndStandardDeviationGraph
		ParkingRoot.setParkingWalkingDistanceScalingFactorForOutput(1.0);
		
		ParkingManager parkingManager = new ParkingManager(controler, parkingCollection);
		ParkingSimulation parkingSimulation=new ParkingSimulation(parkingManager);
		ParkingScoreCollector parkingScoreCollector=new ParkingScoreCollector(controler);
		parkingSimulation.addParkingArrivalEventHandler(parkingScoreCollector);
		parkingSimulation.addParkingDepartureEventHandler(parkingScoreCollector);
		controler.addControlerListener(parkingManager);
		parkingScoreAccumulator = new ParkingScoreAccumulator(parkingScoreCollector);
		controler.addControlerListener(parkingScoreAccumulator);
		
		eventHandlerAtStartupAdder.addEventHandler(parkingSimulation);
	}
	
	public Double getAverageWalkingDistance(){
		return parkingScoreAccumulator.getAverageWalkingDistance();
	}
	
}
