/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

/**
 * 
 */
package playground.southafrica.gauteng;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeUsingTollFactor;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import playground.southafrica.gauteng.roadpricingscheme.SanralTollFactor_Subpopulation;
import playground.southafrica.gauteng.routing.RandomizingTravelDisutilityWithTollFactory;
import playground.southafrica.gauteng.scoring.GautengScoringFunctionFactory;
import playground.southafrica.gauteng.scoring.GenerationOfMoneyEvents;
import playground.southafrica.utilities.Header;
import playground.vsp.planselectors.DiversityGeneratingPlansRemover;
import playground.vsp.planselectors.DiversityGeneratingPlansRemover.Builder;

/**
 * 
 * @author jwjoubert
 */
public class GautengControler_subpopulations {
	private final static Logger LOG = Logger
			.getLogger(GautengControler_subpopulations.class);

	static final String RE_ROUTE_AND_SET_VEHICLE = "ReRouteAndSetVehicle";
	public static final String VEH_ID = "TransportModeToVehicleIdMap" ;
	final static String DIVERSITY_GENERATING_PLANS_REMOVER = "DiversityGeneratingPlansRemover";

	public static enum User { johan, kai } ;
	private static User user = User.johan ;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		OutputDirectoryLogging.catchLogEntries();

		Header.printHeader(GautengControler_subpopulations.class.toString(),
				args);
		/* Config must be passed as an argument, everything else is optional. */
		final String configFilename = args[0];
		Config config = ConfigUtils.createConfig();
		if ( configFilename != null ) {
			ConfigUtils.loadConfig(config, configFilename);
		}

		/* Required argument:
		 * [0] - Config file;
		 * 
		 * Optional arguments:
		 * [1] - Population file;
		 * [2] - Person attribute file;
		 * [3] - Network file;
		 * [4] - Toll (road pricing) file.
		 * [5] - Base value of time ;
		 * [6] - Value-of-Time multiplier; and
		 * [7] - Number of threads. 
		 * [8] - user // should presumably be earlier in sequence?
		 * [9] - Output directory
		 * [10] - Counts file
		 * [11] - optional second config file
		 */

		setOptionalArguments(args, config);

		double baseValueOfTime = 110.;
		double valueOfTimeMultiplier = 4.;
		if (args.length > 5 && args[5] != null && args[5].length() > 0) {
			baseValueOfTime = Double.parseDouble(args[5]);
		}

		if (args.length > 6 && args[6] != null && args[6].length() > 0) {
			valueOfTimeMultiplier = Double.parseDouble(args[6]);
		}

		// ===========================================

		/* Set some other config parameters. */
		config.plans().setSubpopulationAttributeName("subpopulation");
		config.global().setCoordinateSystem("WGS84_SA_Albers");

		String[] modes ={"car","commercial"};
		config.qsim().setMainModes( Arrays.asList(modes) );
		config.plansCalcRoute().setNetworkModes(Arrays.asList(modes));

		if ( !config.controler().getMobsim().equals( ControlerConfigGroup.MobsimType.qsim.toString() ) ) {
			throw new RuntimeException("I doubt that the jdqsim honors _main modes_.  Either check, or do not use. Aborting. kai, jan'14") ;
		}

		config.qsim().setLinkDynamics( LinkDynamics.PassingQ.toString() );
		// yy is the mobsim really assigning the vehicles from the vehicles file?

		GautengUtils.assignSubpopulationStrategies(config);
		config.strategy().setPlanSelectorForRemoval(DIVERSITY_GENERATING_PLANS_REMOVER);

//		config.planCalcScore().setBrainExpBeta(1.0); // is now default
		config.controler().setWritePlansInterval(100);

		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);
		config.controler().setRoutingAlgorithmType( RoutingAlgorithmType.FastAStarLandmarks );
		config.controler().setLastIteration(1000);
		if ( user==User.kai ) {
			config.controler().setLastIteration(100);
		}
		
		final double sampleFactor = 0.01 ;


		config.qsim().setFlowCapFactor(sampleFactor);
//		config.qsim().setStorageCapFactor(Math.pow(sampleFactor, -0.25)); // interpolates between 0.03 @ 0.01 and 1 @ 1
		// yyyyyy was wrong!! Corrected version is
		config.qsim().setStorageCapFactor(sampleFactor * Math.pow(sampleFactor, -0.25)); // interpolates between 0.03 @ 0.01 and 1 @ 1

		config.counts().setCountsScaleFactor(1./sampleFactor);
		config.counts().setOutputFormat("all");

		config.qsim().setEndTime(36.*3600.);

		config.qsim().setStuckTime(10.);
//		config.qsim().setRemoveStuckVehicles(false); // this is now default

//		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true); // test.  I don't think that this is needed. kai, aug'14

		config.controler().setWriteSnapshotsInterval(0);
		
		if ( user==User.kai ) {
			config.parallelEventHandling().setNumberOfThreads(1); // I don't think that it helps to have more than one here.  
			config.qsim().setNumberOfThreads(6);
		} else if(user == User.johan){
			config.parallelEventHandling().setNumberOfThreads(1); 
		}

		config.vspExperimental().setRemovingUnneccessaryPlanAttributes(true);
		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.ABORT ) ;
		config.vspExperimental().setWritingOutputEvents(true);
		
		// This should (in theory) allow you to add a second config file, overwriting config entries.  Meant for work with jars on the server,
		// where you don't want to re-create the jar if you just want to change a config option.   Currently untested. kai, feb'14
		if(args.length > 11 && args[11] != null && args[11].length() > 0 && !args[11].equals("null") ) {
			ConfigUtils.loadConfig(config, args[11]);
		}

		config.addConfigConsistencyChecker( new VspConfigConsistencyCheckerImpl() );
		config.checkConsistency();
		

		// ===========================================

		final Scenario sc = ScenarioUtils.loadScenario(config);

		((ScenarioImpl)sc).createVehicleContainer() ;
		
		/* CREATE VEHICLES. */
		createVehiclePerPerson(sc);

		// ===========================================

		final Controler controler = new Controler(sc);

		// SET VEHICLES ...
		// ... at beginning:
		controler.addControlerListener(new IterationStartsListener() {
			@Override
			public void notifyIterationStarts(IterationStartsEvent event) {
				if(event.getIteration() == sc.getConfig().controler().getFirstIteration()){
					for(Person p : sc.getPopulation().getPersons().values()){
						for(Plan plan : p. getPlans()){
							new SetVehicleInAllNetworkRoutes(sc).handlePlan(plan);
						}
					}
				}
			}
		});

		// ... during replanning (this also needs to be registered as strategy in the config):
		controler.addPlanStrategyFactory(RE_ROUTE_AND_SET_VEHICLE, new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(final Scenario scenario, EventsManager eventsManager) {
				PlanStrategyImpl planStrategy = new PlanStrategyImpl( new RandomPlanSelector<Plan, Person>() ) ; 
				planStrategy.addStrategyModule( new ReRoute( scenario ) ); 
				planStrategy.addStrategyModule( new SetVehicleInAllNetworkRoutes(scenario));
				return planStrategy ;
			}
		});

		// ROAD PRICING AND SCORING:
		setUpRoadPricingAndScoring(baseValueOfTime, valueOfTimeMultiplier, sc, controler);
		
		// PLANS REMOVAL
		Builder builder = new DiversityGeneratingPlansRemover.Builder() ;
		builder.setActTypeWeight(5.);
		builder.setLocationWeight(5.);
		builder.setSameModePenalty(5.);
		builder.setSameRoutePenalty(5.);
		builder.setActTimeParameter(0.);
		controler.addPlanSelectorFactory(DIVERSITY_GENERATING_PLANS_REMOVER, builder );
		// yyyy needs to be tested.  But in current runs, all plans of an agent are exactly identical at end of 1000it.  kai, mar'13
		
		// ADDITIONAL ANALYSIS:
		controler.addControlerListener(new KaiAnalysisListener());

		// RUN:

		controler.run();

		Header.printFooter();
	}


	private static void setUpRoadPricingAndScoring(double baseValueOfTime, double valueOfTimeMultiplier, final Scenario sc,
			final Controler controler) {
		// ROAD PRICING:
        final RoadPricingConfigGroup roadPricingConfig = ConfigUtils.addOrGetModule(
        		sc.getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class
        		);
		if (roadPricingConfig.isUsingRoadpricing()) {
			throw new RuntimeException(
					"roadpricing must NOT be enabled in config.scenario in order to use special "
							+ "road pricing features.  aborting ...");
		}

		// CONSTRUCT VEH-DEP ROAD PRICING SCHEME:
        RoadPricingScheme sanralTollScheme = new RoadPricingSchemeUsingTollFactor(
        		roadPricingConfig.getTollLinksFile(), new SanralTollFactor_Subpopulation(sc)
        		);

		// INSTALL ROAD PRICING (in the longer run, re-merge with RoadPricing
		// class):
		// insert into scoring:
		controler.addControlerListener(new GenerationOfMoneyEvents(
				sc.getNetwork(), sc.getPopulation(), sanralTollScheme
				));

		controler.setScoringFunctionFactory(new GautengScoringFunctionFactory( 
				sc, baseValueOfTime, valueOfTimeMultiplier 
				) );

		
		final RandomizingTravelDisutilityWithTollFactory travelDisutilityFactory = 
				new RandomizingTravelDisutilityWithTollFactory( sc );
		// ---
		travelDisutilityFactory.setRoadPricingScheme( sanralTollScheme ); 
		travelDisutilityFactory.setRandomness(3);
		// ---
		controler.setTravelDisutilityFactory( travelDisutilityFactory );

	}

	/**
	 * @param sc
	 */
	public static void createVehiclePerPerson(final Scenario sc) {
		// (public:  access for analysis. kai, feb'14)
		
		/* Create vehicle types. */
		VehiclesFactory vf = VehicleUtils.getFactory();
		LOG.info("Creating vehicle types.");
		VehicleType vehicle_A2 = vf.createVehicleType(new IdImpl("A2"));
		vehicle_A2.setDescription("Light vehicle with SANRAL toll class `A2'");
		sc.getVehicles().addVehicleType(vehicle_A2);

		VehicleType vehicle_B = vf.createVehicleType(new IdImpl("B"));
		vehicle_B.setDescription("Short commercial vehicle with SANRAL toll class `B'");
		vehicle_B.setMaximumVelocity(100.0 / 3.6);
		vehicle_B.setLength(10.0);
		sc.getVehicles().addVehicleType(vehicle_B);

		VehicleType vehicle_C = vf.createVehicleType(new IdImpl("C"));
		vehicle_C.setDescription("Medium/long commercial vehicle with SANRAL toll class `C'");
		vehicle_C.setMaximumVelocity(80.0 / 3.6);
		vehicle_C.setLength(15.0);
		sc.getVehicles().addVehicleType(vehicle_C);

		/* Create a vehicle per person. */
		Vehicles vehicles = ((ScenarioImpl) sc).getVehicles();
		for (Person p : sc.getPopulation().getPersons().values()) {
			String vehicleType = (String) sc.getPopulation()
					.getPersonAttributes()
					.getAttribute(p.getId().toString(), SanralTollFactor_Subpopulation.VEH_TOLL_CLASS_ATTRIB_NAME);
			Boolean eTag = (Boolean) sc.getPopulation().getPersonAttributes()
					.getAttribute(p.getId().toString(), SanralTollFactor_Subpopulation.E_TAG_ATTRIBUTE_NAME);

			/* Create the vehicle. */
			Vehicle v = null;
			if (vehicleType.equalsIgnoreCase("A2")) {
				v = vf.createVehicle(p.getId(), vehicle_A2);
			} else if (vehicleType.equalsIgnoreCase("B")) {
				v = vf.createVehicle(p.getId(), vehicle_B);
			} else if (vehicleType.equalsIgnoreCase("C")) {
				v = vf.createVehicle(p.getId(), vehicle_C);
			} else {
				throw new RuntimeException("Unknown vehicle toll class: "
						+ vehicleType);
			}
			vehicles.addVehicle(v);

			vehicles.getVehicleAttributes().putAttribute(v.getId().toString(), SanralTollFactor_Subpopulation.E_TAG_ATTRIBUTE_NAME, eTag);

			sc.getPopulation().getPersonAttributes().putAttribute( p.getId().toString(), VEH_ID, v.getId() );
		}
	}

	/**
	 * @param args
	 * @param config
	 */
	private static void setOptionalArguments(String[] args, Config config) {
		/* Optional arguments. */
		String plansFilename = null;
		if (args.length > 1 && args[1] != null && args[1].length() > 0) {
			plansFilename = args[1];
			config.plans().setInputFile(plansFilename);
		}

		String personAttributeFilename = null;
		if (args.length > 2 && args[2] != null && args[2].length() > 0) {
			personAttributeFilename = args[2];
			config.plans().setInputPersonAttributeFile(personAttributeFilename);
		}

		String networkFilename = null;
		if (args.length > 3 && args[3] != null && args[3].length() > 0) {
			networkFilename = args[3];
			config.network().setInputFile(networkFilename);
		}

		String tollFilename = null;
		if (args.length > 4 && args[4] != null && args[4].length() > 0) {
			tollFilename = args[4];
            ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setTollLinksFile(tollFilename);
		}
		
		int numberOfThreads = 1;
		if (args.length > 7 && args[7] != null && args[7].length() > 0) {
			numberOfThreads = Integer.parseInt(args[7]);
			config.global().setNumberOfThreads(numberOfThreads);
		}

		if (args.length > 8 && args[8] != null && args[8].length() > 0) {
			user = User.valueOf( args[8] ) ;
		}
		
		String outputDirectory = null;
		if(args.length > 9 && args[9] != null && args[9].length() > 0) {
			outputDirectory = args[9];
			config.controler().setOutputDirectory(outputDirectory);
		}

		String countsFilename = null;
		if(args.length > 10 && args[10] != null && args[10].length() > 0) {
			countsFilename = args[10];
			config.counts().setCountsFileName(countsFilename);
		}
	}

	private static final class SetVehicleInAllNetworkRoutes implements PlanStrategyModule {
		private final Scenario scenario;

		private SetVehicleInAllNetworkRoutes(Scenario scenario) {
			this.scenario = scenario;
		}

		@Override
		public void prepareReplanning(ReplanningContext replanningContext) {}

		@Override
		public void handlePlan(Plan plan) {
			Id vehId = (Id) scenario.getPopulation().getPersonAttributes()
					.getAttribute(plan.getPerson().getId().toString(), VEH_ID ) ;
			for ( Leg leg : PopulationUtils.getLegs(plan) ) {
				if ( leg.getRoute()!=null && leg.getRoute() instanceof NetworkRoute ) {
					((NetworkRoute)leg.getRoute()).setVehicleId(vehId);
				}
			}
		}

		@Override
		public void finishReplanning() {}
	}

}
