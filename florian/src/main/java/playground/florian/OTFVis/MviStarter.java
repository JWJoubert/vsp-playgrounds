package playground.florian.OTFVis;

import org.matsim.run.OTFVis;


public class MviStarter {

	private static String config = "./src/main/java/playground/florian/Equil/config_mvi.xml";
	private static String mviFile = "./src/main/java/playground/florian/Equil/Output_mvi/ITERS/it.0/0.otfvis.mvi";
	
	public static void main(String[] args) {
//		Controler con = new Controler(config);
//		con.run();
		OTFVis ot = new OTFVis();
		ot.main(args);
	}

}
