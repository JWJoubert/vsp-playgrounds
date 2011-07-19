package playground.wrashid.parkingChoice;

import java.util.LinkedList;

import junit.framework.TestCase;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import playground.wrashid.parkingChoice.api.ReservedParkingManager;
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.ReservedParking;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;

public class ReservedParkingTest extends TestCase {

	public void testBaseTestCase(){
		ReservedParkingManager reservedParkingManager = new ReservedParkingManager() {

			@Override
			public boolean considerForChoiceSet(ReservedParking reservedParking, Id personId, double OPTIONALtimeOfDayInSeconds,
					ActInfo targetActInfo) {
				if (personId.equals(new IdImpl(1)) && reservedParking.getAttributes().contains("EV")) {
					return true;
				}
				return false;
			}
		};
		
		assertEquals(2744, walkingDistanceFor3CarScenario(reservedParkingManager,1),5.0);
	}
	
	public void testCaseWithHigherParkingCapacityAllAgentsAllowedToUseReservedParking(){
		ReservedParkingManager reservedParkingManager = new ReservedParkingManager() {

			@Override
			public boolean considerForChoiceSet(ReservedParking reservedParking, Id personId, double OPTIONALtimeOfDayInSeconds,
					ActInfo targetActInfo) {
				if (personId.equals(new IdImpl(1)) && reservedParking.getAttributes().contains("EV")) {
					return true;
				}
				if (personId.equals(new IdImpl(2)) && reservedParking.getAttributes().contains("disabled")) {
					return true;
				}
				if (personId.equals(new IdImpl(3)) && reservedParking.getAttributes().contains("EV")) {
					return true;
				}
				return false;
			}
		};
		
		assertEquals(998, walkingDistanceFor3CarScenario(reservedParkingManager,10),5.0);
	}
	
	
	private double walkingDistanceFor3CarScenario(ReservedParkingManager reservedParkingManager, int parkingCapacity) {
		ParkingChoiceLib.isTestCaseRun=true;
		Config config = ConfigUtils.loadConfig("test/input/playground/wrashid/parkingChoice/chessConfig.xml");
		Controler controler = new Controler(config);

		// setup parking infrastructure
		LinkedList<Parking> parkingCollection = new LinkedList<Parking>();

		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				ParkingImpl parking = new ParkingImpl(new CoordImpl(i * 1000 + 500, j * 1000 + 500));
				parking.setMaxCapacity(parkingCapacity);
				parkingCollection.add(parking);
			}
		}

		ReservedParking reservedParking = new ReservedParking(new CoordImpl(8500.0, 9000), "disabled, EV");
		reservedParking.setMaxCapacity(parkingCapacity);
		parkingCollection.add(reservedParking);

		ParkingModule parkingModule = new ParkingModule(controler, parkingCollection);

		parkingModule.setReservedParkingManager(reservedParkingManager);

		controler.setOverwriteFiles(true);

		controler.run();

		return parkingModule.getAverageWalkingDistance();
	}
	
	
	
	

}
