/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v3.helper;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTimeCalculator;
import org.matsim.population.algorithms.PersonAlgorithm;

public class UTurnRemover implements PersonAlgorithm, IterationStartsListener{

	private static final Logger log = Logger.getLogger(UTurnRemover.class);

	private final NetworkLegRouter router;

	private final Scenario sc;
	private static int wrnCnt = 0;

	public UTurnRemover(Scenario sc){
		Network network = sc.getNetwork();
		FreeSpeedTravelTimeCalculator fs = new FreeSpeedTravelTimeCalculator();
		TravelDisutility cost = new TravelCostCalculatorFactoryImpl().createTravelDisutility(fs,sc.getConfig().planCalcScore() );
		LeastCostPathCalculator routeAlgo = new Dijkstra(network, cost, fs);
		this.router = new NetworkLegRouter(network, routeAlgo, ((PopulationFactoryImpl) sc.getPopulation().getFactory()).getModeRouteFactory());
		this.sc =sc;
	}

	private void run(Plan plan) {
		List<PlanElement> list = plan.getPlanElements();
		if (list.size() != 3 || !(list.get(0) instanceof Activity && list.get(1) instanceof Leg && list.get(2) instanceof Activity) ) {
			if (++wrnCnt <= 1) {
				log.warn("For now \"Act-Leg-Act\"-Plans only are allowed! This warning will only be shown once!");
			}
		}

		boolean needToReRoute = false;

		Activity a1 = (Activity) list.get(0);
		Id l0Id = a1.getLinkId();
		Id l0FromId = this.sc.getNetwork().getLinks().get(l0Id).getFromNode().getId();



		Leg l = (Leg) list.get(1);
		LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) l.getRoute();

		Id l1 = route.getLinkIds().get(0);
		Id l1ToId = this.sc.getNetwork().getLinks().get(l1).getToNode().getId();

		Activity a2 = (Activity) list.get(2);
		if (l0FromId.equals(l1ToId)) {
			((ActivityImpl)a1).setLinkId(l1);
			Link lin = this.sc.getNetwork().getLinks().get(l1);
			((ActivityImpl)a1).setCoord(lin.getCoord());
			needToReRoute = true;
		}
		//
		Id lmId = a2.getLinkId();
		Id lmToId = this.sc.getNetwork().getLinks().get(lmId).getToNode().getId();

		Id ln = route.getLinkIds().get(route.getLinkIds().size()-1);
		Id lnFromId = this.sc.getNetwork().getLinks().get(ln).getFromNode().getId();

		if (lmToId.equals(lnFromId)) {
			((ActivityImpl)a2).setLinkId(ln);
			needToReRoute = true;
		}


		if (needToReRoute) {
			((LegImpl)l).setRoute(null);
			this.router.routeLeg(plan.getPerson(), l, a1, a2, a1.getEndTime());
		}
	}

	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()){
			run(plan);
		}

	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (event.getIteration() == 0) {
			((PopulationImpl)this.sc.getPopulation()).addAlgorithm(this);
			((PopulationImpl)this.sc.getPopulation()).runAlgorithms();
			((PopulationImpl)this.sc.getPopulation()).removeAlgorithm(this);
		} else {
			event.getControler().removeControlerListener(this);
		}

	}


}
