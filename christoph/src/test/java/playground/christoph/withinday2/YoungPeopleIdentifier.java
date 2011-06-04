package playground.christoph.withinday2;

import java.util.HashSet;
import java.util.Set;

import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.population.PersonImpl;
import org.matsim.ptproject.qsim.agents.WithinDayAgent;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;
import org.matsim.ptproject.qsim.qnetsimengine.QVehicle;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

public class YoungPeopleIdentifier extends DuringLegIdentifier {

	private Netsim queueSim;

	@Override
	public Set<WithinDayAgent> getAgentsToReplan(double time) {
		
		Set<WithinDayAgent> set = new HashSet<WithinDayAgent>();

		// don't handle the agent, if time != 12 o'clock
		if (Math.floor(time) !=  22000.0) {
			return set;
		}
		
//		Collection<QVehicle> tmpList=queueSim.getQNetwork().getLinks().get(new IdImpl("6")).getAllNonParkedVehicles();

		// select agents, which should be replanned within this time step
		for (NetsimLink link:queueSim.getNetsimNetwork().getNetsimLinks().values()){
//			for (QVehicle vehicle : link.getVehQueue()) {
				for (QVehicle vehicle : link.getAllNonParkedVehicles()) {
				PersonDriverAgent agent=vehicle.getDriver();
				System.out.println(agent.getId());
				if (((PersonImpl) ((HasPerson)agent).getPerson()).getAge() == 18) {
					System.out.println("found agent");
					set.add((WithinDayAgent)agent);
				}
			}
		}

		return set;
	}
	
	YoungPeopleIdentifier(Netsim queueSim) {
		this.queueSim = queueSim;
	}
}
