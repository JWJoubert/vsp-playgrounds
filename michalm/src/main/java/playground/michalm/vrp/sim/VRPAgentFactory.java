package playground.michalm.vrp.sim;

import org.matsim.api.core.v01.population.*;
import org.matsim.core.mobsim.framework.*;
import org.matsim.ptproject.qsim.agents.*;
import org.matsim.ptproject.qsim.interfaces.*;

import playground.michalm.vrp.data.*;
import playground.michalm.vrp.supply.*;
import playground.mzilske.withinday.*;


public class VRPAgentFactory
    implements AgentFactory
{
    private Netsim netsim;
    private MATSimVRPData data;

    public final static String VRP_DRIVER_PREFIX = "vrpDriver_";


    public VRPAgentFactory(Netsim netsim, MATSimVRPData data)
    {
        this.netsim = netsim;
        this.data = data;
    }


    @Override
    public MobsimAgent createMobsimAgentFromPerson(Person p)
    {
        if (p instanceof VRPDriverPerson) {
            VRPDriverPerson driverPerson = (VRPDriverPerson)p;

            RealAgent vrpAgent = new LightweightVRPVehicleAgent(
                    driverPerson.getVrpVehicle(), data.getShortestPaths());
            
            AdapterAgent adapterAgent = new AdapterAgent(p.getSelectedPlan(), netsim, vrpAgent);
            netsim.addQueueSimulationListeners(adapterAgent);

            return adapterAgent;
        }
        // else if ()// other possible agents
        // {}
        else {// default agents (according to DefaultAgentFactory)
            return new PersonDriverAgentImpl(p, netsim);
        }
    }
}
