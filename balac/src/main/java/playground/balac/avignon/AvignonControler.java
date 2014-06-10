package playground.balac.avignon;

import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;



public class AvignonControler extends Controler {
	
	private DestinationChoiceBestResponseContext dcContext;
	
	public AvignonControler(String[] args) {
		super(args);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		AvignonControler controler = new AvignonControler(args);
		//controler.setOverwriteFiles(true);
		controler.init();
    	controler.run();		   	
    	
	}
	@Override
	protected void loadControlerListeners() {
		this.dcContext.init(); // this is an ugly hack, but I somehow need to get the scoring function + context into the controler
		
		this.addControlerListener(new DestinationChoiceInitializer(this.dcContext));
		
		if (Double.parseDouble(super.getConfig().locationchoice().getRestraintFcnExp()) > 0.0 &&
				Double.parseDouble(super.getConfig().locationchoice().getRestraintFcnFactor()) > 0.0) {		
					this.addControlerListener(new FacilitiesLoadCalculator(this.dcContext.getFacilityPenalties()));
				}
		//this.addControlerListener(new RetailersLocationListener());
		//this.addControlerListener(new DistanceControlerListener(1.0));

		super.loadControlerListeners();
	}	
	
	public void init() {
		/*
		 * would be muuuuch nicer to have this in DestinationChoiceInitializer, but startupListeners are called after corelisteners are called
		 * -> scoringFunctionFactory cannot be replaced
		 */
		this.dcContext = new DestinationChoiceBestResponseContext(super.getScenario());	
		/* 
		 * add ScoringFunctionFactory to controler
		 *  in this way scoringFunction does not need to create new, identical k-vals by itself    
		 */
		AvignonScoringFunctionFactory dcScoringFunctionFactory = new AvignonScoringFunctionFactory(this.getConfig(), this, this.dcContext); 	
		super.setScoringFunctionFactory(dcScoringFunctionFactory);
		 
		 
		if (!super.getConfig().locationchoice().getPrefsFile().equals("null") &&
				!super.getConfig().facilities().getInputFile().equals("null")) {
			dcScoringFunctionFactory.setUsingConfigParamsForScoring(false);
		} else {
			dcScoringFunctionFactory.setUsingConfigParamsForScoring(true);
			log.info("external prefs are not used for scoring!");
		}	
		
	}
	public void run(String path) {			
			
			Config config = ConfigUtils.loadConfig(path);		
		
			Controler controler = new Controler(config);
		
			controler.run();
		
		
	}

}
