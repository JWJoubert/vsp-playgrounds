package playground.florian.OTFVis.tests;

import org.matsim.vis.otfvis.OTFClientSwing;

public class T6_Swing_QSim {
	private static final String mviFile = "./Output/OTFVisTests/QSim/ITERS/it.1/1.otfvis.mvi";
	
	public static void main(String[] args) {
		new OTFClientSwing(mviFile).run();
	}

}
