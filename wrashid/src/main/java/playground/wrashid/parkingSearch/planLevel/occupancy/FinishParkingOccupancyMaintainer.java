package playground.wrashid.parkingSearch.planLevel.occupancy;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import playground.wrashid.parkingSearch.planLevel.analysis.ParkingOccupancyAnalysis;
import playground.wrashid.parkingSearch.planLevel.analysis.ParkingWalkingTimesAnalysis;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.scoring.ParkingScoreExecutor;

public class FinishParkingOccupancyMaintainer implements AfterMobsimListener {

	private ParkingOccupancyMaintainer parkingOccupancyMaintainer;

	public void notifyAfterMobsim(AfterMobsimEvent event) {
		ParkingRoot.getParkingOccupancyMaintainer().closeAllLastParkings();

		// write also the occupancy statistics at this stage of the simulation

		ParkingOccupancyAnalysis poaWriter = new ParkingOccupancyAnalysis(ParkingRoot.getParkingOccupancyMaintainer()
				.getParkingOccupancyBins(), ParkingRoot.getParkingCapacity());
		String fileName = event.getControler().getControlerIO()
				.getIterationFilename(event.getControler().getIterationNumber(), "parkingOccupancyStatistics.txt");
		poaWriter.writeTxtFile(fileName);
		fileName = event.getControler().getControlerIO()
				.getIterationFilename(event.getControler().getIterationNumber(), "parkingOccupancyCoordinates.txt");
		poaWriter.writeFakeKMLFile(fileName);

		fileName = event.getControler().getControlerIO()
				.getIterationFilename(event.getControler().getIterationNumber(), "parkingWalkingTimes.txt");
		new ParkingWalkingTimesAnalysis(ParkingRoot.getParkingOccupancyMaintainer().getParkingRelatedWalkDistance())
				.writeTxtFile(fileName);

		
		
		
		new ParkingScoreExecutor().performScoring(event);
		
	}
}
