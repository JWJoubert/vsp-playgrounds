package playground.singapore.springcalibration.run;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;

public class RunSingapore {
	
	private final static Logger log = Logger.getLogger(RunSingapore.class);

	public static void main(String[] args) {
		log.info("Running SingaporeControlerRunner");
		Controler controler = new Controler(args[0]);
			
		// GlobalConfigGroup globalConfigGroup, Network network, ControlerConfigGroup controlerConfigGroup, CountsConfigGroup countsConfigGroup, VolumesAnalyzer volumesAnalyzer, IterationStopWatch iterationStopwatch, OutputDirectoryHierarchy controlerIO
		controler.addControlerListener(
				new CountsControlerListenerSingapore(
						controler.getConfig().global(), 
						controler.getScenario().getNetwork(), 
						controler.getConfig().controler(), 
						controler.getConfig().counts(), 
						controler.getVolumes(),
						controler.getStopwatch(),
						controler.getControlerIO()));
				
		controler.run();
		log.info("finished SingaporeControlerRunner");
	}

}
