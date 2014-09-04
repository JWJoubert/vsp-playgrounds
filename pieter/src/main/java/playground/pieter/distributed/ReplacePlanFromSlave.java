package playground.pieter.distributed;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;

public class ReplacePlanFromSlave implements PlanStrategy {

	private HashMap<String, Plan> plans;

	public ReplacePlanFromSlave(HashMap<String, Plan> plans) {
		this.plans=plans;
	}

	@Override
	public void run(HasPlansAndId<Plan,Person> person) {
		person.removePlan(person.getSelectedPlan());
		Plan plan = plans.get(person.getId().toString());
		person.addPlan(plan);	
		person.setSelectedPlan(plan);

	}

	@Override
	public void init(ReplanningContext replanningContext) {

	}

	@Override
	public void finish() {

	}

}
