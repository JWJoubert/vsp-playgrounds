package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.scenario.MyDataContainer;
import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.Floor;
import playground.gregor.sim2d_v2.simulation.floor.forces.ForceModule;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.operation.distance.DistanceOp;

public class CollisionPredictionEnvironmentForceModule implements ForceModule {


	private final Scenario sc;

	private final double EventHorizonTime = 10;
	private final GeometryFactory geofac = new GeometryFactory();
	private final double Bi;
	private final double Ai;
	private QuadTree<Coordinate> quad;

	private final double maxSensingRange;

	/**
	 * @param floor
	 * @param scenario
	 */
	public CollisionPredictionEnvironmentForceModule(Floor floor, Scenario scenario) {
		this.sc = scenario;
		Sim2DConfigGroup conf = (Sim2DConfigGroup) scenario.getConfig().getModule("sim2d");
		this.Bi = conf.getBi();
		this.Ai = conf.getAi();
		this.maxSensingRange = conf.getMaxSensingRange();
	}

	@Override
	public void run(Agent2D agent) {
		double fx = 0;
		double fy = 0;

		Collection<Coordinate> ed = this.quad.get(agent.getPosition().x, agent.getPosition().y, this.maxSensingRange);

		double t_i = getTi(ed,agent);
		if (t_i == Double.POSITIVE_INFINITY) {
			return;
		}
		double v_i = Math.sqrt(agent.getVx()*agent.getVx() + agent.getVy()*agent.getVy());
		double stopDist = v_i/ t_i;

		for (Coordinate c : ed) {
			double dist = c.distance(agent.getPosition());
			double term1 = this.Ai * stopDist * Math.exp(-dist/this.Bi);
			Vector vecDPrime_ij_t_i = getDistVector(agent,c,t_i);
			double dPrime_ij_t_i = Math.sqrt(vecDPrime_ij_t_i.x*vecDPrime_ij_t_i.x+vecDPrime_ij_t_i.y*vecDPrime_ij_t_i.y);



			double dx =vecDPrime_ij_t_i.x / dPrime_ij_t_i;
			double dy =vecDPrime_ij_t_i.y / dPrime_ij_t_i;
			fx += -term1 * dx;
			fy += -term1 * dy;



		}

		agent.getForce().incrementX(fx);
		agent.getForce().incrementY(fy);

	}

	private Vector getDistVector(Agent2D agent, Coordinate c, double t_i) {
		double projectedX = agent.getPosition().x + agent.getVx() * t_i;
		double projectedY = agent.getPosition().y + agent.getVy() * t_i;

		double dPrimeX_ij = c.x - projectedX;
		double dPrimeY_ij = c.y - projectedY;
		Vector v = new Vector();
		v.x = dPrimeX_ij;
		v.y = dPrimeY_ij;



		return v;
	}

	private double getTi(Collection<Coordinate> ed, Agent2D agent) {
		double t_i = Double.POSITIVE_INFINITY;

		for (Coordinate c : ed) {
			double tmp = getTi(c,agent);
			if (tmp < t_i) {
				t_i = tmp;
			}
		}

		return t_i;
	}

	private double getTi(Coordinate c, Agent2D agent) {
		Coordinate c2 = new Coordinate(c.x - agent.getVx()*this.EventHorizonTime, c.y - agent.getVy()*this.EventHorizonTime,0);


		LineString ls = this.geofac.createLineString(new Coordinate [] {c,c2});

		DistanceOp op =  new DistanceOp(ls, this.geofac.createPoint(agent.getPosition()));
		double ti = op.closestPoints()[0].distance(c);
		double tanPhi = op.distance()/ti;
		double phi = Math.atan(tanPhi);
		if (phi > Math.PI) {
			phi = Math.PI*2 - phi;
		}

		if (phi > Math.PI/4){
			return Double.POSITIVE_INFINITY;
		}

		double v = Math.sqrt(agent.getVx()*agent.getVx()+ agent.getVy()*agent.getVy());
		ti /= v;


		return ti;
	}

	@Override
	public void init() {
		this.quad = this.sc.getScenarioElement(MyDataContainer.class).getQuadTree();
	}

	private static final class Vector {
		double x;

		double y;
	}

}
