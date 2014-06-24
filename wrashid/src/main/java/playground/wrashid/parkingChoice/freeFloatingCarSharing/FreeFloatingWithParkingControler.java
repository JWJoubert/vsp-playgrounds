package playground.wrashid.parkingChoice.freeFloatingCarSharing;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.parkingChoice.carsharing.DummyParkingModuleWithFreeFloatingCarSharing;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingCoordInfo;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingModuleWithFreeFloatingCarSharing;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.balac.freefloating.config.FreeFloatingConfigGroup;
import playground.balac.freefloating.controler.listener.FFListener;
import playground.balac.freefloating.qsimParkingModule.FreeFloatingQsimFactory;
import playground.balac.freefloating.router.FreeFloatingParkingRoutingModule;
import playground.balac.freefloating.routerparkingmodule.FreeFloatingRoutingModule;
import playground.balac.freefloating.scoring.FreeFloatingScoringFunctionFactory;


public class FreeFloatingWithParkingControler extends Controler{
	
	
	public FreeFloatingWithParkingControler(Scenario scenario) {
		super(scenario);
	}


	public void init(Config config, Network network, Scenario sc) {
		FreeFloatingScoringFunctionFactory ffScoringFunctionFactory = new FreeFloatingScoringFunctionFactory(
				      config, 
				      network, sc);
	    this.setScoringFunctionFactory(ffScoringFunctionFactory); 	
				
		}
	
	@Override
	  protected void loadControlerListeners() {  
		  
	    super.loadControlerListeners();   
	   // this.addControlerListener(new FFListener( this.getConfig().getModule("FreeFloating").getValue("statsFileName")));
	  }
	public static void main(final String[] args) {
		
    	final Config config = ConfigUtils.loadConfig(args[0]);
    	FreeFloatingConfigGroup configGroup = new FreeFloatingConfigGroup();
    	config.addModule(configGroup);
		final Scenario sc = ScenarioUtils.loadScenario(config);
		
		
		final FreeFloatingWithParkingControler controler = new FreeFloatingWithParkingControler( sc );
		try {		
		
		final FreeFloatingConfigGroup configGroupff = (FreeFloatingConfigGroup)
				sc.getConfig().getModule( FreeFloatingConfigGroup.GROUP_NAME );
		
		
		BufferedReader reader;
		String s;
		
		
		
		
		controler.setTripRouterFactory(
				new TripRouterFactory() {
					@Override
					public TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext) {
						// this factory initializes a TripRouter with default modules,
						// taking into account what is asked for in the config
					
						// This allows us to just add our module and go.
						final TripRouterFactory delegate = DefaultTripRouterFactoryImpl.createRichTripRouterFactoryImpl(controler.getScenario());

						final TripRouter router = delegate.instantiateAndConfigureTripRouter(routingContext);
						
						// add our module to the instance
						router.setRoutingModule(
							"freefloating",
							new FreeFloatingRoutingModule());

						// we still need to provide a way to identify our trips
						// as being freefloating trips.
						// This is for instance used at re-routing.
						final MainModeIdentifier defaultModeIdentifier =
							router.getMainModeIdentifier();
						router.setMainModeIdentifier(
								new MainModeIdentifier() {
									@Override
									public String identifyMainMode(
											final List<PlanElement> tripElements) {
										for ( PlanElement pe : tripElements ) {
											if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "freefloating" ) ) {
												return "freefloating";
											}
										}
										// if the trip doesn't contain a freefloating leg,
										// fall back to the default identification method.
										return defaultModeIdentifier.identifyMainMode( tripElements );
									}
								});
						
						return router;
					}

					
				});
      controler.init(config, sc.getNetwork(), sc);	
      
      
      if (configGroupff.useFeeFreeFloating()) {
 		 reader = IOUtils.getBufferedReader(configGroupff.getvehiclelocations());
 		    s = reader.readLine();
 		    int i = 1;
 		    
 		    ArrayList<ParkingCoordInfo> freefloatingCars = new ArrayList<ParkingCoordInfo>();
 		    while(s != null) {
 		    	
 		    	String[] arr = s.split("\t", -1);
 		    
 		    	Link l = controler.getNetwork().getLinks().get(new IdImpl(arr[0]));	    	
 		    	
 		    	for (int k = 0; k < Integer.parseInt(arr[1]); k++) {
 		    		ParkingCoordInfo parkingInfo = new ParkingCoordInfo(new IdImpl(Integer.toString(i)), l.getCoord());
 		    		freefloatingCars.add(parkingInfo);
 		    		i++;
 		    	}
 		    	
 		    	s = reader.readLine();
 		    	
 		    }
 		    
 		   ParkingModuleWithFFCarSharingZH parkingModule = new ParkingModuleWithFFCarSharingZH(controler, freefloatingCars);
 		    //ParkingModuleWithFreeFloatingCarSharing parkingModule = new ParkingModuleWithFFCarSharing();
 		    
 		   
 		    
 		    controler.setMobsimFactory( new FreeFloatingQsimFactory(sc, controler, 
 		    		parkingModule,  freefloatingCars) );
 		}
      
      controler.setOverwriteFiles(true);
		
		controler.run();
} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

}
