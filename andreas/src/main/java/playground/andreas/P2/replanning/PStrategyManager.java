package playground.andreas.P2.replanning;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.MatsimRandom;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PConfigGroup.PStrategySettings;

/**
 * Loads strategies from config and chooses strategies according to their weights.
 * 
 * @author aneumann
 *
 */
public class PStrategyManager {
	
	private final static Logger log = Logger.getLogger(PStrategyManager.class);
	
	private final ArrayList<PPlanStrategy> strategies = new ArrayList<PPlanStrategy>();
	private final ArrayList<Double> weights = new ArrayList<Double>();
	private double totalWeights = 0.0;
	
	private String pIdentifier;
	private TimeReduceDemand timeReduceDemand = null;	
	
	public PStrategyManager(PConfigGroup pConfig){
		this.pIdentifier = pConfig.getPIdentifier();
	}
	// TODO[an] always initialize TimeReduceDemand
	public void init(PConfigGroup pConfig, EventsManager eventsManager) {
		for (PStrategySettings settings : pConfig.getStrategySettings()) {
			double rate = settings.getProbability();
			if (rate == 0.0) {
				continue;
			}
			String classname = settings.getModuleName();
			PPlanStrategy strategy = loadStrategy(classname, settings, eventsManager);
			this.addStrategy(strategy, rate);
		}
		
		log.info("enabled with " + this.strategies.size()  + " strategies");
	}

	private PPlanStrategy loadStrategy(final String name, final PStrategySettings settings, EventsManager eventsManager) {
		PPlanStrategy strategy = null;
		
		if (name.equals(RemoveAllVehiclesButOne.STRATEGY_NAME)) {
			strategy = new RemoveAllVehiclesButOne(settings.getParametersAsArrayList());
		} else if (name.equals(RandomStartTimeAllocator.STRATEGY_NAME)) {
			strategy = new RandomStartTimeAllocator(settings.getParametersAsArrayList());
		} else if (name.equals(RandomEndTimeAllocator.STRATEGY_NAME)) {
			strategy = new RandomEndTimeAllocator(settings.getParametersAsArrayList());
		} else if (name.equals(MaxRandomStartTimeAllocator.STRATEGY_NAME)) {
			strategy = new MaxRandomStartTimeAllocator(settings.getParametersAsArrayList());
		} else if (name.equals(MaxRandomEndTimeAllocator.STRATEGY_NAME)) {
			strategy = new MaxRandomEndTimeAllocator(settings.getParametersAsArrayList());
		} else if (name.equals(IncreaseNumberOfVehicles.STRATEGY_NAME)) {
			strategy = new IncreaseNumberOfVehicles(settings.getParametersAsArrayList());
		} else if (name.equals(AddRandomStop.STRATEGY_NAME)) {
			strategy = new AddRandomStop(settings.getParametersAsArrayList());
		} else if (name.equals(AggressiveIncreaseNumberOfVehicles.STRATEGY_NAME)) {
			strategy = new AggressiveIncreaseNumberOfVehicles(settings.getParametersAsArrayList());
		} else if (name.equals(TimeReduceDemand.STRATEGY_NAME)) {
			TimeReduceDemand strat = new TimeReduceDemand(settings.getParametersAsArrayList());
			strat.setPIdentifier(this.pIdentifier);
			eventsManager.addHandler(strat);
			strategy = strat;
			this.timeReduceDemand = strat;
		} else if (name.equals(StopReduceDemand.STRATEGY_NAME)) {
			StopReduceDemand strat = new StopReduceDemand(settings.getParametersAsArrayList());
			strat.setPIdentifier(this.pIdentifier);
			eventsManager.addHandler(strat);
			strategy = strat;
//			this.timeReduceDemand = strat;
		}
		
		if (strategy == null) {
			log.error("Could not initialize strategy named " + name);
		}
		
		return strategy;
	}

	private void addStrategy(final PPlanStrategy strategy, final double weight) {
		this.strategies.add(strategy);
		this.weights.add(Double.valueOf(weight));
		this.totalWeights += weight;
	}

	public PPlanStrategy chooseStrategy() {
		double rnd = MatsimRandom.getRandom().nextDouble() * this.totalWeights;

		double sum = 0.0;
		for (int i = 0, max = this.weights.size(); i < max; i++) {
			sum += this.weights.get(i).doubleValue();
			if (rnd <= sum) {
				return this.strategies.get(i);
			}
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append("Strategies: ");
		strBuffer.append(this.strategies.get(0).getName()); strBuffer.append(" ("); strBuffer.append(this.weights.get(0)); strBuffer.append(")");
		
		for (int i = 1; i < this.strategies.size(); i++) {
			strBuffer.append(", "); strBuffer.append(this.strategies.get(i).getName()); strBuffer.append(" ("); strBuffer.append(this.weights.get(i)); strBuffer.append(")");
		}
		return strBuffer.toString();
	}

	public TimeReduceDemand getTimeReduceDemand() {
		return this.timeReduceDemand;
	}	
}