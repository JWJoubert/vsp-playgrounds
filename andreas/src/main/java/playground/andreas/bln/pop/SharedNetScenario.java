package playground.andreas.bln.pop;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Provides a real scenario, but exchanges the population.
 * Still, network and facilities can be reused that way.
 *
 * @author mrieser
 */
public class SharedNetScenario implements Scenario {

	private final ScenarioImpl scenario;
	private Population myPopulation;

	public SharedNetScenario(final ScenarioImpl scenario, final Population population) {
		this.scenario = scenario;
		this.myPopulation = population;
	}

	@Override
	public Population getPopulation() {
		return this.myPopulation;
	}
	
	@Override
	public TransitSchedule getTransitSchedule() {
		return this.scenario.getTransitSchedule();
	}
	
	@Override
	public ActivityFacilities getActivityFacilities() {
		return this.scenario.getActivityFacilities();
	}

	@Override
	public Coord createCoord(double x, double y) {
		return this.scenario.createCoord(x, y);
	}

	@Override
	public Id createId(String string) {
		return this.scenario.createId(string);
	}

	@Override
	public Config getConfig() {
		return this.scenario.getConfig();
	}

	@Override
	public Network getNetwork() {
		return this.scenario.getNetwork();
	}

	@Override
	public void addScenarioElement(String name, Object o) {
		this.scenario.addScenarioElement(name , o);
	}

	@Override
	public Object getScenarioElement(String name) {
		return this.scenario.getScenarioElement(name);
	}

	@Override
	public Object removeScenarioElement(String name) {
		return this.scenario.removeScenarioElement( name );
	}

}
