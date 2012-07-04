/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.wrashid.parkingSearch.withindayFW.controllers.kti;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.TravelTimeFactoryWrapper;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.withinday.replanning.modules.ReplanningModule;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.IntegerValueHashMap;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingStrategy;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingStrategyManager;
import playground.wrashid.parkingSearch.withindayFW.impl.ParkingCostCalculatorFW;
import playground.wrashid.parkingSearch.withindayFW.impl.ParkingStrategyActivityMapperFW;
import playground.wrashid.parkingSearch.withindayFW.utility.ParkingPersonalBetas;
import playground.wrashid.parkingSearch.withindayFW.zhCity.CityZones;
import playground.wrashid.parkingSearch.withindayFW.zhCity.ParkingCostCalculatorZH;
import playground.wrashid.parkingSearch.withindayFW.zhCity.ParkingOccupancyAnalysis;
import playground.wrashid.parkingSearch.withindayFW.zhCity.ParkingInfrastructureZH;
import playground.wrashid.parkingSearch.withindayFW.zhCity.HUPC.HUPCIdentifier;
import playground.wrashid.parkingSearch.withindayFW.zhCity.HUPC.HUPCReplannerFactory;

public class HUPCControllerKTIzh extends KTIWithinDayControler  {
	private LinkedList<Parking> parkings;









	public HUPCControllerKTIzh(String[] args) {
		super(args);
	}
	
	@Override
	protected void startUpBegin() {
		HashMap<String, HashSet<Id>> parkingTypes=new HashMap<String, HashSet<Id>>();
		initParkingInfrastructure(this,parkingTypes);
		
		String cityZones="H:/data/experiments/TRBAug2012/parkings/zones.csv";
		parkingInfrastructure=new ParkingInfrastructureZH(this.scenarioData,parkingTypes, new ParkingCostCalculatorZH(parkingTypes, new CityZones(cityZones), scenarioData,parkings),parkings);
		
		
	}

	private HashMap<String, HashSet<Id>> initParkingTypes() {
		
		return null;
	}

	@Override
	protected void startUpFinishing() {
		
		ParkingPersonalBetas parkingPersonalBetas = new ParkingPersonalBetas(this.scenarioData, null);

		ParkingStrategyActivityMapperFW parkingStrategyActivityMapperFW = new ParkingStrategyActivityMapperFW();
		Collection<ParkingStrategy> parkingStrategies = new LinkedList<ParkingStrategy>();
		ParkingStrategyManager parkingStrategyManager = new ParkingStrategyManager(parkingStrategyActivityMapperFW,
				parkingStrategies, parkingPersonalBetas);
		parkingAgentsTracker.setParkingStrategyManager(parkingStrategyManager);

		LeastCostPathCalculatorFactory factory = new AStarLandmarksFactory(this.network, new FreespeedTravelTimeAndDisutility(
				this.config.planCalcScore()));
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) this.scenarioData.getPopulation().getFactory())
				.getModeRouteFactory();

		/*
		 * Initialize TravelTimeCollector and create a FactoryWrapper which will act as
		 * factory but returns always the same travel time object, which is possible since
		 * the TravelTimeCollector is not personalized.
		 */
		Set<String> analyzedModes = new HashSet<String>();
		analyzedModes.add(TransportMode.car);
		super.createAndInitTravelTimeCollector(analyzedModes);
		TravelTimeFactoryWrapper travelTimeCollectorWrapperFactory = new TravelTimeFactoryWrapper(this.getTravelTimeCollector());

//		// create a copy of the MultiModalTravelTimeWrapperFactory and set the
//		// TravelTimeCollector for car mode
//		MultiModalTravelTimeWrapperFactory timeFactory = new MultiModalTravelTimeWrapperFactory();
//		for (Entry<String, PersonalizableTravelTimeFactory> entry : this.getMultiModalTravelTimeWrapperFactory()
//				.getPersonalizableTravelTimeFactories().entrySet()) {
//			timeFactory.setPersonalizableTravelTimeFactory(entry.getKey(), entry.getValue());
//		}
//
//		timeFactory.setPersonalizableTravelTimeFactory(TransportMode.car, travelTimeCollectorWrapperFactory);
		
		TravelDisutilityFactory costFactory = new OnlyTimeDependentTravelCostCalculatorFactory();

//		AbstractMultithreadedModule router = new ReplanningModule(config, network, costFactory, timeFactory, factory,
//				routeFactory);
		
		AbstractMultithreadedModule router = new ReplanningModule(config, network, costFactory, travelTimeCollectorWrapperFactory, factory,
				routeFactory);
		
		// adding hight utility parking choice algo
		HUPCReplannerFactory hupcReplannerFactory = new HUPCReplannerFactory(this.getReplanningManager(),
				router, 1.0, this.scenarioData, parkingAgentsTracker);
		HUPCIdentifier hupcSearchIdentifier = new HUPCIdentifier(parkingAgentsTracker, (ParkingInfrastructureZH) parkingInfrastructure);
		this.getFixedOrderSimulationListener().addSimulationListener(hupcSearchIdentifier);
		hupcReplannerFactory.addIdentifier(hupcSearchIdentifier);
		ParkingStrategy parkingStrategy = new ParkingStrategy(hupcSearchIdentifier);
		parkingStrategies.add(parkingStrategy);
		this.getReplanningManager().addDuringLegReplannerFactory(hupcReplannerFactory);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "home", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "work_sector2", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "work_sector3", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "shop", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "leisure", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "education_other", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "education_kindergarten", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "education_primary", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "education_secondary", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "education_higher", parkingStrategy);

		
		
		this.addControlerListener(parkingStrategyManager);
		this.getFixedOrderSimulationListener().addSimulationListener(parkingStrategyManager);

		this.getReplanningManager().setEventsManager(this.getEvents());
	

		initParkingFacilityCapacities();
		
		
		cleanNetwork();
		
		parkingAgentsTracker.setParkingOccupancyHandler(new ParkingOccupancyAnalysis(this));
		
	}

	private void cleanNetwork() {
		//network cleaning
		// set min length of link to 10m (else replanning does not function, because vehicle arrives
		// before it can be replanned).
		// case where this happend: although p1 and p2 are on different links, if both have length zero
		// then replanning does not happen and walk leg, etc. is not set of link.
		
		//TODO: alternative to handle this (make check where getting alternative parking (not on same link)
		// there check, if link length is zero of current and next link (and if this is the case), exclude both links
		// until link with length non-zero in the set.
		
		int minLinkLength = 40;
		for (Link link:network.getLinks().values()){
			if (link.getLength()<minLinkLength){
				link.setLength(minLinkLength);
			}
		}
	}

	private void initParkingFacilityCapacities() {
		IntegerValueHashMap<Id> facilityCapacities=new IntegerValueHashMap<Id>();
		parkingInfrastructure.setFacilityCapacities(facilityCapacities);
		
		for (Parking parking:parkings){
			facilityCapacities.incrementBy(parking.getId(),(int) Math.round(parking.getCapacity()));
		}
	}
	
	
	
	private void initParkingInfrastructure(Controler controler, HashMap<String, HashSet<Id>> parkingTypes) {
		parkings = getParkingsForScenario(controler);
		
		for (Parking parking:parkings){
			
			ActivityFacility parkingFacility = this.scenarioData.getActivityFacilities().createFacility(parking.getId(), parking.getCoord());
			Link nearestLink = ((NetworkImpl)this.scenarioData.getNetwork()).getNearestLink(parking.getCoord());
			
			((ActivityFacilityImpl)parkingFacility).setLinkId(nearestLink.getId());
			
			ActivityOption activityOption = ((ActivityFacilityImpl)parkingFacility).createActivityOption("parking");
			activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 0*3600, 24*3600));
			activityOption.setCapacity(Double.MAX_VALUE);
		
		}

	}

	public static LinkedList<Parking> getParkingsForScenario(Controler controler) {
		String parkingDataBase;
		String isRunningOnServer = controler.getConfig().findParam("parking", "isRunningOnServer");
		if (Boolean.parseBoolean(isRunningOnServer)) {
			parkingDataBase = "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/wrashid/data/experiments/TRBAug2011/parkings/flat/";
			ParkingHerbieControler.isRunningOnServer = true;
		} else {
			parkingDataBase = "H:/data/experiments/TRBAug2011/parkings/flat/";
			ParkingHerbieControler.isRunningOnServer = false;
		}
		
		
		double parkingsOutsideZHCityScaling = Double.parseDouble(controler.getConfig().findParam("parking",
				"publicParkingsCalibrationFactorOutsideZHCity"));

		LinkedList<Parking> parkingCollection = getParkingCollectionZHCity(controler,parkingDataBase);
		String streetParkingsFile = null;
		//if (isKTIMode) {
			streetParkingsFile = parkingDataBase + "publicParkingsOutsideZHCity_v0_dilZh30km_10pct.xml";
		//} else {
		//	streetParkingsFile = parkingDataBase + "publicParkingsOutsideZHCity_v0.xml";
		//}

		readParkings(parkingsOutsideZHCityScaling, streetParkingsFile, parkingCollection);

		return parkingCollection;
	}
	
	public static LinkedList<Parking> getParkingCollectionZHCity(Controler controler,String parkingDataBase) {
		double streetParkingCalibrationFactor = Double.parseDouble(controler.getConfig().findParam("parking",
				"streetParkingCalibrationFactorZHCity"));
		double garageParkingCalibrationFactor = Double.parseDouble(controler.getConfig().findParam("parking",
				"garageParkingCalibrationFactorZHCity"));
		double privateParkingCalibrationFactorZHCity = Double.parseDouble(controler.getConfig().findParam("parking",
				"privateParkingCalibrationFactorZHCity"));
		// double
		// privateParkingsOutdoorCalibrationFactor=Double.parseDouble(controler.getConfig().findParam("parking",
		// "privateParkingsOutdoorCalibrationFactorZHCity"));

		LinkedList<Parking> parkingCollection = new LinkedList<Parking>();

		String streetParkingsFile = parkingDataBase + "streetParkings.xml";
		readParkings(streetParkingCalibrationFactor, streetParkingsFile, parkingCollection);

		String garageParkingsFile = parkingDataBase + "garageParkings.xml";
		readParkings(garageParkingCalibrationFactor, garageParkingsFile, parkingCollection);

		String privateIndoorParkingsFile = null;
		//if (isKTIMode) {
			privateIndoorParkingsFile = parkingDataBase + "privateParkings_v1_kti.xml";
		//} else {
		//	privateIndoorParkingsFile = parkingDataBase + "privateParkings_v1.xml";
		//}

		readParkings(privateParkingCalibrationFactorZHCity, privateIndoorParkingsFile, parkingCollection);

		return parkingCollection;
	}
	
	
	public static void readParkings(double parkingCalibrationFactor, String parkingsFile, LinkedList<Parking> parkingCollection) {
		ParkingHerbieControler.readParkings(parkingCalibrationFactor, parkingsFile, parkingCollection);
	}
	
	
	
	
	
	
	
	
	
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println("using default config");
		//	args = new String[] { "test/input/playground/wrashid/parkingSearch/withinday/chessboard/config.xml" };
			args = new String[] { "H:/data/experiments/TRBAug2011/runs/ktiRun1/configRunLocal2.xml" };
			
		
		}
		final HUPCControllerKTIzh controller = new HUPCControllerKTIzh(args);

		controller.setOverwriteFiles(true);
		GeneralLib.controler=controller;

		controller.run();

		
		System.exit(0);
	}

}
