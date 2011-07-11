package playground.gregor.multidestpeds.densityestimation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.scenario.MyDataContainer;
import playground.gregor.sim2d_v2.simulation.floor.StaticEnvironmentDistancesField;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class DensityEstimatorFactory {

	private final EventsManager events;
	private final Scenario sc;
	private final double res;

	public DensityEstimatorFactory(EventsManager events, Scenario sc) {
		this(events,sc,1);
	}

	public DensityEstimatorFactory(EventsManager events, Scenario sc, double res) {
		this.events = events;
		this.sc = sc;
		this.res = res;
	}

	public NNGaussianKernelEstimator createDensityEstimator() {
		NNGaussianKernelEstimator ret = new NNGaussianKernelEstimator();
		ret.addGroupId("r");
		ret.addGroupId("g");
		ret.setResolution(this.res);


		ret.setLambda(1);
		ret.setMinDist(.300);
		ret.setEventsManager(this.events);

		StaticEnvironmentDistancesField sedf = this.sc.getScenarioElement(StaticEnvironmentDistancesField.class);
		ret.setStaticEnvironmentDistancesField(sedf);
		double maxX = Double.NEGATIVE_INFINITY;
		double minX = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		for (Node node : this.sc.getNetwork().getNodes().values()) {
			double x = node.getCoord().getX();
			double y = node.getCoord().getY();
			if (x > maxX) {
				maxX = x;
			}
			if (x < minX) {
				minX = x;
			}

			if (y > maxY) {
				maxY = y;
			}

			if (y < minY) {
				minY = y;
			}


		}

		Coordinate c1 = new Coordinate(minX,minY);
		Coordinate c2 = new Coordinate(maxX,maxY);
		ret.setEnvelope(new Envelope(c1,c2));


		return ret;
	}
}
