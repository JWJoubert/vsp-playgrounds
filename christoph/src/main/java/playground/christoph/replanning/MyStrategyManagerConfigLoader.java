package playground.christoph.replanning;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManagerImpl;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.replanning.modules.ExternalModule;
import org.matsim.core.replanning.modules.PlanomatModule;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.ReRouteLandmarks;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.replanning.selectors.PathSizeLogitSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.locationchoice.LocationChoice;

import playground.christoph.replanning.modules.MyReRouteDijkstra;

public class MyStrategyManagerConfigLoader {

	private static final Logger log = Logger.getLogger(StrategyManagerConfigLoader.class);

	public static void load(final Controler controler, final Config config, final StrategyManagerImpl manager) {

		Network network = controler.getNetwork();
		PersonalizableTravelCost travelCostCalc = controler.createTravelCostCalculator();
		PersonalizableTravelTime travelTimeCalc = controler.getTravelTimeCalculator();

		manager.setMaxPlansPerAgent(config.strategy().getMaxAgentPlanMemorySize());

		int externalCounter = 0;

		for (StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings()) {
			double rate = settings.getProbability();
			if (rate == 0.0) {
				continue;
			}
			String classname = settings.getModuleName();

			if (classname.startsWith("org.matsim.demandmodeling.plans.strategies.")) {
				classname = classname.replace("org.matsim.demandmodeling.plans.strategies.", "");
			}
			PlanStrategyImpl strategy = null;
			if (classname.equals("KeepLastSelected")) {
				strategy = new PlanStrategyImpl(new KeepSelected());
			} else if (classname.equals("ReRoute") || classname.equals("threaded.ReRoute")) {
				strategy = new PlanStrategyImpl(new RandomPlanSelector());
				strategy.addStrategyModule(new ReRoute(controler));
			} else if (classname.equals("ReRoute_Dijkstra")) {
				strategy = new PlanStrategyImpl(new RandomPlanSelector());
				strategy.addStrategyModule(new MyReRouteDijkstra(config, network, travelCostCalc, travelTimeCalc));
			} else if (classname.equals("ReRoute_Landmarks")) {
				strategy = new PlanStrategyImpl(new RandomPlanSelector());
				strategy.addStrategyModule(new ReRouteLandmarks(config, network, travelCostCalc, travelTimeCalc, new FreespeedTravelTimeCost(config.charyparNagelScoring())));
			} else if (classname.equals("TimeAllocationMutator") || classname.equals("threaded.TimeAllocationMutator")) {
				strategy = new PlanStrategyImpl(new RandomPlanSelector());
				TimeAllocationMutator tam = new TimeAllocationMutator(config);
				strategy.addStrategyModule(tam);
			} else if (classname.equals("TimeAllocationMutator7200_ReRouteLandmarks")) {
				strategy = new PlanStrategyImpl(new RandomPlanSelector());
				strategy.addStrategyModule(new TimeAllocationMutator(config, 7200));
				strategy.addStrategyModule(new ReRouteLandmarks(config, network, travelCostCalc, travelTimeCalc, new FreespeedTravelTimeCost(config.charyparNagelScoring())));
			} else if (classname.equals("ExternalModule")) {
				externalCounter++;
				strategy = new PlanStrategyImpl(new RandomPlanSelector());
				String exePath = settings.getExePath();
				ExternalModule em = new ExternalModule(exePath, "ext" + externalCounter, controler, controler.getScenario());
				em.setIterationNumber(controler.getIterationNumber());
				strategy.addStrategyModule(em);
			} else if (classname.equals("Planomat")) {
				strategy = new PlanStrategyImpl(new RandomPlanSelector());
				PlanStrategyModule planomatStrategyModule = new PlanomatModule(controler, controler.getEvents(), controler.getNetwork(), controler.getScoringFunctionFactory(), controler.createTravelCostCalculator(), controler.getTravelTimeCalculator());
				strategy.addStrategyModule(planomatStrategyModule);
			} else if (classname.equals("PlanomatReRoute")) {
				strategy = new PlanStrategyImpl(new RandomPlanSelector());
				PlanStrategyModule planomatStrategyModule = new PlanomatModule(controler, controler.getEvents(), controler.getNetwork(), controler.getScoringFunctionFactory(), controler.createTravelCostCalculator(), controler.getTravelTimeCalculator());
				strategy.addStrategyModule(planomatStrategyModule);
				strategy.addStrategyModule(new ReRoute(controler));
			} else if (classname.equals("BestScore")) {
				strategy = new PlanStrategyImpl(new BestPlanSelector());
			} else if (classname.equals("SelectExpBeta")) {
				strategy = new PlanStrategyImpl(new ExpBetaPlanSelector(config.charyparNagelScoring()));
			} else if (classname.equals("ChangeExpBeta")) {
				strategy = new PlanStrategyImpl(new ExpBetaPlanChanger(config.charyparNagelScoring().getBrainExpBeta()));
			} else if (classname.equals("SelectRandom")) {
				strategy = new PlanStrategyImpl(new RandomPlanSelector());
			} else if (classname.equals("ChangeLegMode")) {
				strategy = new PlanStrategyImpl(new RandomPlanSelector());
				strategy.addStrategyModule(new ChangeLegMode(config));
				strategy.addStrategyModule(new ReRoute(controler));
			} else if (classname.equals("SelectPathSizeLogit")) {
				strategy = new PlanStrategyImpl(new PathSizeLogitSelector(controler.getNetwork(), config.charyparNagelScoring()));
//				// JH
			} else if (classname.equals("KSecLoc")){
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				PlanStrategyModule socialNetStrategyModule= new RandomFacilitySwitcherK(network, travelCostCalc, travelTimeCalc);
//				strategy.addStrategyModule(socialNetStrategyModule);
				log.warn("jhackney: No replanning module available in the core for keywords KSecLoc, FSecLoc,SSecloc. The modules have moved to the playground.");
			} else if (classname.equals("FSecLoc")){
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				PlanStrategyModule socialNetStrategyModule= new RandomFacilitySwitcherF(network, travelCostCalc, travelTimeCalc, facilities);
//				strategy.addStrategyModule(socialNetStrategyModule);
				log.warn("jhackney: No replanning module available in the core for keywords KSecLoc, FSecLoc,SSecloc. The modules have moved to the playground.");
			} else if (classname.equals("SSecLoc")){
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				PlanStrategyModule socialNetStrategyModule= new SNPickFacilityFromAlter(network,travelCostCalc,travelTimeCalc);
//				strategy.addStrategyModule(socialNetStrategyModule);
				log.warn("jhackney: No replanning module available in the core for keywords KSecLoc, FSecLoc,SSecloc. The modules have moved to the playground.");
//				// JH
			} else if (classname.equals("LocationChoice")) {
				strategy = new PlanStrategyImpl(new ExpBetaPlanSelector(config.charyparNagelScoring()));
				strategy.addStrategyModule(new LocationChoice(controler.getNetwork(), controler, (controler.getScenario()).getKnowledges()));
				strategy.addStrategyModule(new ReRoute(controler));
				strategy.addStrategyModule(new TimeAllocationMutator(config));
				/* not really happy about the following line. Imagine what happens if everybody does
				 * this, that one doesn't know at the end which removal-strategy is really used.
				 * The removal strategy must not be tight to a replanning-strategy, but is a general
				 * option that should be set somewhere else.  marcel/9jun2009/CLEANUP
				 */
				// not yet working correctly:
				// compreh. tests needed
				// ah/27jun2009
				// manager.setPlanSelectorForRemoval(new ExpBetaPlanForRemovalSelector());
			}
			//if none of the strategies above could be selected we try to load the class by name
			else {
				//classes loaded by name must not be part of the matsim core
				if (classname.startsWith("org.matsim")) {
					log.error("Strategies in the org.matsim package must not be loaded by name!");
				}
				else {
					try {
						Class<? extends PlanStrategyImpl> klas = (Class<? extends PlanStrategyImpl>) Class.forName(classname);
						Class[] args = new Class[1];
						args[0] = Scenario.class;
						Constructor<? extends PlanStrategyImpl> c = null;
						try{
							c = klas.getConstructor(args);
							strategy = c.newInstance(controler.getScenario());
						} catch(NoSuchMethodException e){
							log.warn("Cannot find Constructor in PlanStrategy " + classname + " with single argument of type Scenario. " +
									"This is not fatal, trying to find other constructor, however a constructor expecting Scenario as " +
									"single argument is recommented!" );
						}
						if (c == null){
							args[0] = Controler.class;
							c = klas.getConstructor(args);
							strategy = c.newInstance(controler);
						}
						log.info("Loaded PlanStrategy from class " + classname);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (SecurityException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			}

			if (strategy == null) {
				Gbl.errorMsg("Could not initialize strategy named " + classname);
			}

			manager.addStrategy(strategy, rate);

			// now check if this modules should be disabled after some iterations
			if (settings.getDisableAfter() >= 0) {
				int maxIter = settings.getDisableAfter();
				if (maxIter >= config.controler().getFirstIteration()) {
					manager.addChangeRequest(maxIter + 1, strategy, 0.0);
				} else {
					/* The controler starts at a later iteration than this change request is scheduled for.
					 * make the change right now.					 */
					manager.changeWeightOfStrategy(strategy, 0.0);
				}
			}
		}
	}
}
