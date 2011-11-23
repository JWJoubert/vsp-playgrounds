package playground.christoph.withinday;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.population.PersonImpl;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.ptproject.qsim.interfaces.MobsimVehicle;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.ptproject.qsim.qnetsimengine.NetsimLink;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

public class YoungPeopleIdentifier extends DuringLegIdentifier {

	private Netsim mobsim;

	@Override
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {
		
		Set<PlanBasedWithinDayAgent> set = new HashSet<PlanBasedWithinDayAgent>();

		// don't handle the agent, if time != 12 o'clock
		if (Math.floor(time) !=  22000.0) {
			return set;
		}
		
		NetsimLink tmpLink = mobsim.getNetsimNetwork().getNetsimLinks().get(new IdImpl("6"));
//		Collection<QVehicle> tmpList=queueSim.getQNetwork().getLinks().get(new IdImpl("6")).getVehQueue();
		Collection<MobsimVehicle> tmpList=mobsim.getNetsimNetwork().getNetsimLinks().get(new IdImpl("6")).getAllNonParkedVehicles();

		// select agents, which should be replanned within this time step
		for (NetsimLink link:mobsim.getNetsimNetwork().getNetsimLinks().values()){
//			for (QVehicle vehicle : link.getVehQueue()) {
				for (MobsimVehicle vehicle : link.getAllNonParkedVehicles()) {
				MobsimDriverAgent agent=vehicle.getDriver();
				System.out.println(agent.getId());
				if (((PersonImpl) ((HasPerson)agent).getPerson()).getAge() == 18) {
					System.out.println("found agent");
					set.add((PlanBasedWithinDayAgent)agent);
				}
			}
		}

		return set;
	}
	
	/*package*/ YoungPeopleIdentifier(Netsim mobsim) {
		this.mobsim = mobsim;
	}


}
