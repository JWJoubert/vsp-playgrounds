/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.vrp.taxi;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;

import pl.poznan.put.vrp.dynamic.data.model.*;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.model.TaxiCustomer;
import playground.michalm.vrp.data.network.*;


public class TaxiModeDepartureHandler
    implements DepartureHandler
{
    private static final String TAXI_MODE = "taxi";

    private MatsimVrpData data;
    private TaxiSimEngine taxiSimEngine;


    public TaxiModeDepartureHandler(TaxiSimEngine taxiSimEngine, MatsimVrpData data)
    {
        this.taxiSimEngine = taxiSimEngine;
        this.data = data;
    }


    @Override
    public boolean handleDeparture(double now, MobsimAgent agent, Id linkId)
    {
        if (agent.getMode().equals(TAXI_MODE)) {
            // mobsim.getEventsManager().processEvent(
            // new TaxiRequestEventImpl(now, agent.getId(), linkId, agent.getMode()));

            MatsimVrpGraph vrpGraph = data.getVrpGraph();

            // TODO: Works for FixedSizeVrpGraph!
            // in case of GrowingVrpGraph: 'addVertex' must be called if 'getVertex' returns 'null'
            MatsimVertex fromVertex = vrpGraph.getVertex(linkId);

            Id toLinkId = agent.getDestinationLinkId();
            //TODO: as above (works for FixedSizeVrpGraph, won't work for GrowingVrpGraph)
            MatsimVertex toVertex = vrpGraph.getVertex(toLinkId);

            List<Customer> customers = data.getVrpData().getCustomers();
            List<Request> requests = data.getVrpData().getRequests();

            int id = requests.size();

            // notify the DVRP Optimizer
            // agent -> customerId -> Customer
            Customer customer = new TaxiCustomer(id, fromVertex, agent);// TODO

            int duration = 120; // approx. 120 s for entering the taxi
            int t0 = (int)now;
            int t1 = t0 + 0; // hardcoded values!
            Request request = new RequestImpl(id, customer, fromVertex, toVertex, 1, 1, duration,
                    t0, t1, false);

            customers.add(customer);
            requests.add(request);

            // taxiSimEngine.getMobsim().registerAdditionalAgentOnLink(agent);
            taxiSimEngine.internalInterface.registerAdditionalAgentOnLink(agent);

            taxiSimEngine.taxiRequestSubmitted(request, now);

            return true;
        }
        else {
            return false;
        }
    }
}
