package playground.christoph.withinday;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.PersonImpl;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;

public class OldPeopleIdentifier extends DuringActivityIdentifier {

	private Netsim mobsim;

	@Override
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {

		Set<PlanBasedWithinDayAgent> set = new HashSet<PlanBasedWithinDayAgent>();

		// don't handle the agent, if time != 12 o'clock
		if (time != 12 * 3600) {
			return set;
		}

		// select agents, which should be replanned within this time step
		for (MobsimAgent pa : mobsim.getActivityEndsList()) {
//			PersonAgent agent = (PersonAgent) pa ;
//			if (((PersonImpl) agent.getPerson()).getAge() == 56) {
			if ( pa instanceof HasPerson ) {
				Person person = ((HasPerson)pa).getPerson() ;
				if ( ((PersonImpl)person).getAge() == 56 ) {
					System.out.println("found agent");
					set.add((PlanBasedWithinDayAgent)pa);
				}
			}
		}

		return set;
	}

	/*package*/ OldPeopleIdentifier(Netsim mobsim) {
		this.mobsim = mobsim;
	}

}
