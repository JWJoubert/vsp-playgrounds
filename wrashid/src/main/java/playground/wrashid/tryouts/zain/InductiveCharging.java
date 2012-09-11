package playground.wrashid.tryouts.zain;


import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.transEnergySim.charging.ChargingUponArrival;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.road.InductiveStreetCharger;
import org.matsim.contrib.transEnergySim.controllers.InductiveChargingController;
import org.matsim.contrib.transEnergySim.vehicles.VehicleUtils;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionTracker;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.galus.EnergyConsumptionModelGalus;
import org.matsim.contrib.transEnergySim.vehicles.impl.InductivelyChargableBatteryElectricVehicle;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

public class InductiveCharging {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Config config= ConfigUtils.loadConfig("C:/tmp/Inductive charging data/input files/config_Mini_Playground.xml");
	
		EnergyConsumptionModel ecm=new EnergyConsumptionModelGalus();
		
		HashMap<Id, Vehicle> vehicles=new HashMap<Id, Vehicle>();
		
		
		
		int batteryCapacityInJoules = 10*1000*3600;
		//int batteryCapacityInJoules = 26516889;   
		//int batteryCapacityInJoules = 18000000;
		//int batteryCapacityInJoules = 0;
		/*for (int i=0;i<100;i++){
			IdImpl agentId = new IdImpl("pid" + i);
			vehicles.put(agentId, new IC_BEV(ecm,batteryCapacityInJoules));
		}
		*/
		IdImpl agentId = new IdImpl("pid" + 0);
		vehicles.put(agentId, new InductivelyChargableBatteryElectricVehicle(ecm,batteryCapacityInJoules));
		
		InductiveChargingController controller = new InductiveChargingController(config,vehicles);

		EnergyConsumptionTracker energyConsumptionTracker = controller.getEnergyConsumptionTracker();
		InductiveStreetCharger inductiveCharger = controller.getInductiveCharger();
		ChargingUponArrival chargingUponArrival= controller.getChargingUponArrival();
		
		DoubleValueHashMap<Id> chargableStreets=new DoubleValueHashMap<Id>();
		chargableStreets.put(new IdImpl("2223"), 3500.0);
		chargableStreets.put(new IdImpl("2322"), 3500.0);
		chargableStreets.put(new IdImpl("1213"), 3500.0);
		chargableStreets.put(new IdImpl("1312"), 3500.0);
		
		inductiveCharger.setChargableStreets(chargableStreets);
		//inductiveCharger.setSamePowerAtAllStreets(3000);
		
		
		//chargingUponArrival.setPowerForNonInitializedActivityTypes(controller.getFacilities(), 3500);
		chargingUponArrival.getChargablePowerAtActivityTypes().put("h", 3312.0);
		chargingUponArrival.getChargablePowerAtActivityTypes().put("w", 3312.0);
		
		
		
		controller.setOverwriteFiles(true);
		controller.run();
		controller.printStatisticsToConsole();
		
	}

}
