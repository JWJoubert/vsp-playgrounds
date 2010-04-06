/**
 * 
 */
package playground.yu.analysis.forMuc;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.xml.sax.SAXException;

import playground.yu.analysis.EnRouteModalSplit;
import playground.yu.utils.CollectionSum;
import playground.yu.utils.TollTools;

/**
 * compute daily En Route/ departures/ arrivals of Munich Network and Munich
 * Region respectively with through traffic
 * 
 * @author yu
 * 
 */
public class EnRouteModalSplit4Muc extends EnRouteModalSplit implements
		Analysis4Muc {
	private double[] rideDep = null, rideArr = null, rideStuck = null,
			rideEnRoute = null;

	/**
	 * @param scenario
	 * @param binSize
	 * @param nofBins
	 * @param plans
	 */
	public EnRouteModalSplit4Muc(String scenario, int binSize, int nofBins,
			PopulationImpl plans) {
		super(scenario, binSize, nofBins, plans);
		if (scenario.equals(MUNICH) || scenario.equals(ONLY_MUNICH)) {
			this.rideDep = new double[nofBins + 1];
			this.rideArr = new double[nofBins + 1];
			this.rideEnRoute = new double[nofBins + 1];
			this.rideStuck = new double[nofBins + 1];
		}
	}

	/**
	 * @param scenario
	 * @param binSize
	 * @param plans
	 */
	public EnRouteModalSplit4Muc(String scenario, int binSize,
			PopulationImpl plans) {
		this(scenario, binSize, 30 * 3600 / binSize + 1, plans);
	}

	/**
	 * @param scenario
	 * @param plans
	 */
	public EnRouteModalSplit4Muc(String scenario, PopulationImpl plans) {
		this(scenario, 300, plans);
	}

	/**
	 * @param scenario
	 * @param ppl
	 * @param toll
	 */
	public EnRouteModalSplit4Muc(String scenario, PopulationImpl ppl,
			RoadPricingScheme toll) {
		this(scenario, ppl);
		this.toll = toll;
	}

	protected void internalCompute(int binIdx, AgentEvent ae, Plan plan,
			double[] allCount, double[] carCount, double[] ptCount,
			double[] wlkCount, double[] bikeCount, double[] rideCount,
			double[] othersCount) {
		allCount[binIdx]++;
		Integer itg = legCounts.get(ae.getPersonId());
		if (itg != null) {
			switch (((LegImpl) plan.getPlanElements().get(2 * itg + 1))
					.getMode()) {
			case car:
				carCount[binIdx]++;
				break;
			case pt:
				if (ptCount != null)
					ptCount[binIdx]++;
				break;
			case walk:
				if (wlkCount != null)
					wlkCount[binIdx]++;
				break;
			case bike:
				if (bikeCount != null)
					bikeCount[binIdx]++;
				break;
			case ride:
				if (rideCount != null)
					rideCount[binIdx]++;
				break;
			default:
				if (othersCount != null)
					othersCount[binIdx]++;
				break;
			}
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		internalHandleEvent(event, this.arr, this.carArr, this.ptArr, wlkArr,
				bikeArr, rideArr, othersArr);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		Id id = event.getPersonId();
		Integer itg = legCounts.get(id);
		if (itg == null)
			itg = Integer.valueOf(-1);
		legCounts.put(id, itg.intValue() + 1);
		internalHandleEvent(event, this.dep, this.carDep, this.ptDep, wlkDep,
				bikeDep, rideDep, othersDep);
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		internalHandleEvent(event, this.stuck, this.carStuck, null, null, null,
				rideStuck, null);
	}

	protected void internalHandleEvent(AgentEvent ae, double[] allCount,
			double[] carCount, double[] ptCount, double[] wlkCount,
			double[] bikeCount, double[] rideCount, double[] othersCount) {
		int binIdx = getBinIndex(ae.getTime());
		Plan selectedPlan = plans.getPersons().get(ae.getPersonId())
				.getSelectedPlan();
		if (toll != null) {
			if (TollTools.isInRange(((PlanImpl) selectedPlan)
					.getFirstActivity().getLinkId(), toll)) {
				this.internalCompute(binIdx, ae, selectedPlan, allCount,
						carCount, ptCount, wlkCount, bikeCount, rideCount,
						othersCount);
			}
		} else {
			internalCompute(binIdx, ae, selectedPlan, allCount, carCount,
					ptCount, wlkCount, bikeCount, rideCount, othersCount);
		}
	}

	/**
	 * Writes the gathered data tab-separated into a text stream.
	 * 
	 * @param bw
	 *            The data stream where to write the gathered data.
	 */
	@Override
	public void write(final BufferedWriter bw) {
		calcOnRoute();
		try {
			bw
					.write("time\ttimeBin\t"
							+ "departures\tarrivals\tstuck\ton_route\t"
							+ "carDepartures\tcarArrivals\tcarStuck\tcarOnRoute\t"
							+ "ptDepartures\tptArrivals\tptStuck\tptOnRoute\t"
							+ "walkDepartures\twalkArrivals\twalkStuck\twalkOnRoute\t"
							+ "bikeDepartures\tbikeArrivals\tbikeStuck\tbikeOnRoute\t"
							+ "rideDepartures\trideArrivals\trideStuck\trideOnRoute\t"
							+ "othersDepartures\tothersArrivals\tothersStuck\tothersOnRoute\n");
			for (int i = 0; i < this.dep.length; i++) {
				bw.write(Time.writeTime(i * this.binSize) + "\t"
						+ i * this.binSize + "\t" + this.dep[i] + "\t"
						+ this.arr[i] + "\t" + this.stuck[i] + "\t"
						+ this.enRoute[i] + "\t" + this.carDep[i] + "\t"
						+ this.carArr[i] + "\t" + this.carStuck[i] + "\t"
						+ this.carEnRoute[i] + "\t" + this.ptDep[i] + "\t"
						+ this.ptArr[i] + "\t" + 0 + "\t" + this.ptEnRoute[i]
						+ "\t" + this.wlkDep[i] + "\t" + this.wlkArr[i] + "\t"
						+ 0 + "\t" + this.wlkEnRoute[i] + "\t"
						+ this.bikeDep[i] + "\t" + this.bikeArr[i] + "\t" + 0
						+ "\t" + this.bikeEnRoute[i] + "\t" + this.rideDep[i]
						+ "\t" + this.rideArr[i] + "\t" + this.rideStuck[i]
						+ "\t" + this.rideEnRoute[i] + "\t" + this.othersDep[i]
						+ "\t" + this.othersArr[i] + "\t" + 0 + "\t"
						+ this.othersEnRoute[i] + "\t");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* output methods */

	/**
	 * Writes the gathered data tab-separated into a text file.
	 * 
	 * @param filename
	 *            The name of a file where to write the gathered data.
	 */
	@Override
	public void write(final String filename) {
		BufferedWriter bw;
		try {
			bw = IOUtils.getBufferedWriter(filename);
			write(bw);
			bw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void writeCharts(final String filename) {
		int length = enRoute.length;
		double[] xs = new double[length];
		for (int j = 0; j < xs.length; j++) {
			xs[j] = ((double) j) * (double) this.binSize / 3600.0;
		}

		// enRoute chart
		XYLineChart enRouteChart = new XYLineChart("Leg Histogramm - En Route",
				"time", "agents en route from " + scenario);
		enRouteChart.addSeries("drivers", xs, carEnRoute);
		if (CollectionSum.getSum(ptEnRoute) > 0)
			enRouteChart.addSeries("public transit users", xs, ptEnRoute);
		if (CollectionSum.getSum(wlkEnRoute) > 0)
			enRouteChart.addSeries("pedestrians", xs, wlkEnRoute);
		if (CollectionSum.getSum(bikeEnRoute) > 0)
			enRouteChart.addSeries("cyclists", xs, bikeEnRoute);
		if (CollectionSum.getSum(rideEnRoute) > 0)
			enRouteChart.addSeries("ride", xs, rideEnRoute);
		if (CollectionSum.getSum(othersEnRoute) > 0)
			enRouteChart.addSeries("others", xs, othersEnRoute);
		enRouteChart.addSeries("all agents", xs, enRoute);
		enRouteChart.saveAsPng(filename + "enRoute.png", 1024, 768);

		// departures chart
		XYLineChart departChart = new XYLineChart(
				"Leg Histogramm - Departures", "time", "departing agents from "
						+ scenario);
		departChart.addSeries("drivers", xs, carDep);
		if (CollectionSum.getSum(ptDep) > 0)
			departChart.addSeries("public transit users", xs, ptDep);
		if (CollectionSum.getSum(wlkDep) > 0)
			departChart.addSeries("pedestrians", xs, wlkDep);
		if (CollectionSum.getSum(bikeDep) > 0)
			departChart.addSeries("cyclists", xs, bikeDep);
		if (CollectionSum.getSum(rideDep) > 0)
			departChart.addSeries("ride", xs, rideDep);
		if (CollectionSum.getSum(othersDep) > 0)
			departChart.addSeries("others", xs, othersDep);
		departChart.addSeries("all agents", xs, dep);
		departChart.saveAsPng(filename + "departures.png", 1024, 768);

		// arrivals chart
		XYLineChart arrChart = new XYLineChart("Leg Histogramm - Arrivals",
				"time", "arriving agents from " + scenario);
		arrChart.addSeries("drivers", xs, carArr);
		if (CollectionSum.getSum(ptArr) > 0)
			arrChart.addSeries("public transit users", xs, ptArr);
		if (CollectionSum.getSum(wlkArr) > 0)
			arrChart.addSeries("pedestrians", xs, wlkArr);
		if (CollectionSum.getSum(bikeArr) > 0)
			arrChart.addSeries("cyclists", xs, bikeArr);
		if (CollectionSum.getSum(rideArr) > 0)
			arrChart.addSeries("ride", xs, rideArr);
		if (CollectionSum.getSum(othersArr) > 0)
			arrChart.addSeries("others", xs, othersArr);
		arrChart.addSeries("all agents", xs, arr);
		arrChart.saveAsPng(filename + "arrivals.png", 1024, 768);
	}

	public static void main(final String[] args) {
		final String netFilename = args[0], // "../berlin data/osm/bb_osm_wip_cl.xml.gz";
		eventsFilename = args[1], // "../runs-svn/run756/it.1000/1000.events.txt.gz";
		plansFilename = args[2], // "../runs-svn/run756/it.1000/1000.plans.xml.gz";
		outputFilename = args[3], // "../matsimTests/analysis/enRoute.txt";
		chartFilename = args[4], // "../matsimTests/analysis/";
		tollFilename = args[5];

		ScenarioImpl scenario = new ScenarioImpl();

		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		PopulationImpl population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		RoadPricingScheme toll = scenario.getRoadPricingScheme();
		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(toll);
		try {
			tollReader.parse(tollFilename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		EventsManagerImpl events = new EventsManagerImpl();
		EnRouteModalSplit4Muc orms = new EnRouteModalSplit4Muc(MUNICH,
				population, toll);
		events.addHandler(orms);
		new MatsimEventsReader(events).readFile(eventsFilename);

		orms.write(outputFilename);
		orms.writeCharts(chartFilename);

		System.out.println("-> Done!");
		System.exit(0);
	}
}
