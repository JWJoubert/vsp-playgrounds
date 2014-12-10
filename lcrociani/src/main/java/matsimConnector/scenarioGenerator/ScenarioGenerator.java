package matsimConnector.scenarioGenerator;

import matsimConnector.utility.Constants;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

import pedCA.context.Context;
import pedCA.output.Log;
import scenarios.ContextGenerator;

public class ScenarioGenerator {
	private static String inputDir = Constants.INPUT_PATH;
	private static String outputDir = Constants.OUTPUT_PATH;

	private static final Double DOOR_WIDTH = Constants.FAKE_LINK_WIDTH;
	private static final Double CA_LENGTH = Constants.CA_LINK_LENGTH;
	private static final int CA_ROWS = (int)Math.round((DOOR_WIDTH/Constants.CA_CELL_SIDE));
	private static final int CA_COLS = (int)Math.round((CA_LENGTH/Constants.CA_CELL_SIDE));
	private static Double TOTAL_DENSITY = 4.;
	private static int POPULATION_SIZE = 600;//(int)((CA_ROWS*Constants.CA_CELL_SIDE) * (CA_COLS*Constants.CA_CELL_SIDE) * TOTAL_DENSITY);
	
	public static void main(String [] args) {
		if (args.length>0){
			TOTAL_DENSITY = Double.parseDouble(args[0]);
			inputDir = Constants.FD_TEST_PATH+args[0]+"/input";
			outputDir = Constants.FD_TEST_PATH+args[0]+"/output";
			POPULATION_SIZE = (int)((CA_ROWS*Constants.CA_CELL_SIDE) * (CA_COLS*Constants.CA_CELL_SIDE) * TOTAL_DENSITY);
		}
		
		Config c = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(c);
		
		Context contextCA = createCAScenario();
		NetworkGenerator.createNetwork(scenario, contextCA);
		
		c.network().setInputFile(inputDir + "/network.xml.gz");

		//		c.strategy().addParam("Module_1", "playground.gregor.sim2d_v4.replanning.Sim2DReRoutePlanStrategy");
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().addParam("ModuleProbability_1", ".1");
		c.strategy().addParam("ModuleDisableAfterIteration_1", "100");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", ".9");

		c.controler().setOutputDirectory(outputDir);
		c.controler().setLastIteration(200);
		c.controler().setRoutingAlgorithmType(ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks);

		c.plans().setInputFile(inputDir + "/population.xml.gz");

		ActivityParams pre = new ActivityParams("origin");
		// needs to be geq 49, otherwise when running a simulation one gets "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in ActivityUtilityParameters.java (gl)
		pre.setTypicalDuration(49); 
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);

		ActivityParams post = new ActivityParams("destination");
		post.setTypicalDuration(49); 
		post.setMinimalDuration(49);
		post.setClosingTime(49);
		post.setEarliestEndTime(49);
		post.setLatestStartTime(49);
		post.setOpeningTime(49);
		scenario.getConfig().planCalcScore().addActivityParams(pre);
		scenario.getConfig().planCalcScore().addActivityParams(post);
		scenario.getConfig().planCalcScore().setLateArrival_utils_hr(0.);
		scenario.getConfig().planCalcScore().setPerforming_utils_hr(0.);


		QSimConfigGroup qsim = scenario.getConfig().qsim();
		qsim.setEndTime(20*60);
		qsim.setStuckTime(100000);
		c.controler().setMobsim(Constants.CA_MOBSIM_MODE);
		c.global().setCoordinateSystem(Constants.COORDINATE_SYSTEM);
		c.qsim().setEndTime(60*10);

		PopulationGenerator.createPopulation(scenario, POPULATION_SIZE);

		new ConfigWriter(c).write(inputDir+ "/config.xml");
		new NetworkWriter(scenario.getNetwork()).write(c.network().getInputFile());
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(c.plans().getInputFile());
	}
	
	private static Context createCAScenario() {
		Log.log("CA Scenario generation");
		//ContextGenerator.createAndSaveBidCorridorContext(inputDir+"/CAScenario", CA_ROWS, CA_COLS, 2);
		return ContextGenerator.loadEnvironmentAndSaveContext(inputDir+"/CAScenario");
	}
}
