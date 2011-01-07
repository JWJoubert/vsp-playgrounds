package playground.mzilske.prognose2025;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;

import playground.mzilske.pipeline.PersonSink;
import playground.mzilske.pipeline.PersonSinkSource;

public class PersonVerschmiererTask implements PersonSinkSource {
	
	private static Logger logger = Logger.getLogger(PersonVerschmiererTask.class);
	
	private Verschmierer verschmierer;
	
	private PersonSink sink;
	
	int verschmiert = 0;
	int nichtVerschmiert = 0;

	public PersonVerschmiererTask(String shapeFilename) {
		this.verschmierer = new Verschmierer(shapeFilename);
	}

	@Override
	public void complete() {
		logger.info("Verschmiert: " + verschmiert + "  Nicht verschmiert: " + nichtVerschmiert);
		sink.complete();
	}

	@Override
	public void process(Person person) {
		Plan plan = person.getPlans().get(0);
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Activity) {
				ActivityImpl activity = (ActivityImpl) planElement;
				Coord oldCoord = activity.getCoord();
				Coord newCoord = verschmierer.shootIntoSameZoneOrLeaveInPlace(oldCoord);
				if (oldCoord.equals(newCoord)) {
					++nichtVerschmiert;
				} else {
					++verschmiert;
				}
				activity.setCoord(newCoord);
			}
		}
		sink.process(person);
	}

	@Override
	public void setSink(PersonSink sink) {
		this.sink = sink;
	}

}
