package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;

public class TestVehicleCollectorSetUp {

	
	public  TestVehicleCollectorSetUp(){
		
	}
	
	/**
	 * defines and sets up the gas types for the simulation
	 * @return
	 */
	public VehicleTypeCollector setUp(){
		
		/*
		 * GAS TYPES
		 * - Specify gas types and their characteristics
		 * 
		 * - Gas Price [currency]
		 * - Joules per liter in gas [J]
		 * - emissions of Co2 per liter gas [kg]
		 
		 */
		double gasPricePerLiter= 0.25; 
		double gasJoulesPerLiter = 43.0*1000000.0;// Benzin 42,7–44,2 MJ/kg
		double emissionPerLiter = 23.2/10; // 23,2kg/10l= xx/mass   1kg=1l
		//--> gas Price per second for PHEV 4.6511627906976747E-4
		
		//0.24*10^-4 EUR per second
		GasType normalGas=new GasType("normal gas", 
				gasJoulesPerLiter, 
				gasPricePerLiter, 
				emissionPerLiter);
		
		
		/*
		 * Define battery types (e.g. EV mode, PHEV model)
		 * 
		 * Battery characteristics:
		 * - full capacity [J]
		 * e.g. common size is 24kWh = 24kWh*3600s/h*1000W/kW = 24*3600*1000Ws= 24*3600*1000J
		 * - minimum level of state of charge, avoid going below this SOC= batteryMin
		 * (0.1=10%)
		 * - maximum level of state of charge, avoid going above = batteryMax
		 * (0.9=90%)
		 * 
		 * Create desired Battery Types
		 */
		double batterySizeEV= 24*3600*1000; 
		double batterySizePHEV= 24*3600*1000; 
		double batteryMinEV= 0.1; 
		
		double batteryMinPHEV= 0.1; 
		double batteryMaxEV= 0.9; 
		double batteryMaxPHEV= 0.9; 		
		
		Battery EVBattery = new Battery(batterySizeEV, batteryMinEV, batteryMaxEV);
		Battery PHEVBattery = new Battery(batterySizePHEV, batteryMinPHEV, batteryMaxPHEV);
		
		/*
		 * Specify vehicle types </br>
		 * 
		 * each vehicle has 
		 * <ul>
		 * <li> a name [String]
		 * <li> a battery type
		 * <li> a gas type ( EVs do not need a gas type--> leave it null )
		 * <li> a reference/dummy vehicle object to check the classes of vehicles find the corresponding Vehicle type for it
		 * <li> energy burn rate of the engine [W] 
		 * </ul>
		 */
		VehicleType EVTypeStandard= new VehicleType("standard EV", 
				EVBattery, 
				null, 
				new ElectricVehicle(null, new IdImpl(1)),
				80000);// Nissan leaf 80kW Engine
		
		VehicleType PHEVTypeStandard= new VehicleType("standard PHEV", 
				PHEVBattery, 
				normalGas, 
				new PlugInHybridElectricVehicle(new IdImpl(1)),
				80000);
		
		/*
		 * The vehicle types are saved within the VehicleTypeCollector
		 * which is then passed into the Decentralized Smart Charger
		 */
		VehicleTypeCollector myVehicleTypes= new VehicleTypeCollector();
		myVehicleTypes.addVehicleType(EVTypeStandard);
		myVehicleTypes.addVehicleType(PHEVTypeStandard);		
	
		
		return myVehicleTypes;
	}
	
}
