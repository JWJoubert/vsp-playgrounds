package playground.gregor.sim2d_v2.controller;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;

import com.vividsolutions.jts.geom.Envelope;

import playground.gregor.multidestpeds.denistyestimation.DensityEstimatorFactory;
import playground.gregor.multidestpeds.denistyestimation.NNGaussianKernelEstimator;
import playground.gregor.pedvis.PedVisPeekABot;
import playground.gregor.sims.msa.MSATravelTimeCalculatorFactory;

public class PeekabotVisController extends Controller2D{

	private PedVisPeekABot vis;

	public PeekabotVisController(String[] args) {
		super(args);
		setTravelTimeCalculatorFactory(new MSATravelTimeCalculatorFactory());
	}

	@Override
	protected void loadData() {
		super.loadData();
		this.vis = new PedVisPeekABot(10,this.scenarioData);
		//		this.vis.setOffsets(386128,5820182);
		this.vis.setOffsets(getNetwork());
		this.vis.setFloorShapeFile(this.getSim2dConfig().getFloorShapeFile());
		this.vis.drawNetwork(this.network);
		this.events.addHandler(this.vis);

		NNGaussianKernelEstimator est = new DensityEstimatorFactory(this.events).createDensityEstimator();
		this.events.addHandler(est);
		this.addControlerListener(this.vis);
	}

	public static void main(String[] args) {
		Controler controller = new PeekabotVisController(args);
		controller.run();

	}

}
