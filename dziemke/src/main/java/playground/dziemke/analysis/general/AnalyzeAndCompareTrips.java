package playground.dziemke.analysis.general;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import playground.dziemke.analysis.GnuplotUtils;
import playground.dziemke.analysis.general.matsim.Events2TripsParser;
import playground.dziemke.analysis.general.matsim.MatsimTrip;
import playground.dziemke.analysis.general.matsim.MatsimTripFilterImpl;
import playground.dziemke.analysis.general.srv.SrvTrip;
import playground.dziemke.analysis.general.srv.SrvTripFilterImpl;
import playground.dziemke.analysis.general.srv.Srv2MATSimPopulation;

import java.io.File;
import java.util.List;

/**
 * With this class, one can
 * - analyze a MATSim simulation (based on events),
 * - analyze SrV survey statistics (based on original SrV data or prepared statistics file),
 * - do both at the same time,
 * - compare the results of the above analyses, and
 * - plot part of or all the above into PDF file using Gnuplot.
 * 
 * Analysis can be done with regard to
 * - travel distance distributions,
 * - travel time distributions,
 * - travel speeds distributions,
 * - departure time distributions, and
 * - activity participation distributions.
 * 
 * Filters can be applied to only include
 * - specific modes ('activateMode')
 * - trips starting or ending in a given area defined by a shapefile ('activateStartsOrEndsIn'; take care of correct CRS!)
 * - trips within a specific distance range ('activateDist')
 * - trips within a specific departure time range ('activateDepartureTimeRange')
 * - trips that precede/follow a specific activity ('activateCertainActBefore', 'activateCertainActAfter')
 * - trips performed by people within a given age range ('activateAge')
 * - etc.
 * 
 * Note that filters for MATSim trips and SrV trip have to be set independently, i.e. the filters can differ between MATSim
 * and SrV. This is implemented like this on purpose because in some cases a comparison has to be "asymmetric" to be meaningful,
 * e.g. if the simulation area is bigger than that of the survey, there is only a need for an area filter on the simulation side.
 * 
 * @author gthunig, dziemke
 */
public class AnalyzeAndCompareTrips {
	public static final Logger LOG = Logger.getLogger(AnalyzeAndCompareTrips.class);

	// Parameters
	private static final String RUN_DIRECTORY_ROOT = "../../runs-svn/open_berlin_scenario"; // To be adjusted
	private static final String RUN_ID = "be400mt_58_v6"; // To be adjusted

	// Input and output
	private static final String NETWORK_FILE = RUN_DIRECTORY_ROOT + "/" + RUN_ID + "/" + RUN_ID + ".output_network.xml.gz";
	private static final String CONFIG_FILE = RUN_DIRECTORY_ROOT + "/" + RUN_ID + "/" + RUN_ID + ".output_config.xml";
	private static final String EVENTS_FILE = RUN_DIRECTORY_ROOT + "/" + RUN_ID + "/" + RUN_ID + ".output_events.xml.gz";
	private static final String AREA_SHAPE_FILE = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2013/Berlin_DHDN_GK4.shp";
	private static String analysisOutputDirectory = RUN_DIRECTORY_ROOT + "/" + RUN_ID + "/analysis";

	// SrV parameters
	private static final String SRV_BASE_DIR = "../../shared-svn/studies/countries/de/open_berlin_scenario/analysis/srv/input/"; // THis folder needs to be checked out
	private static final String SRV_PERSON_FILE_PATH = SRV_BASE_DIR + "P2008_Berlin2.dat";
	private static final String SRV_TRIP_FILE_PATH = SRV_BASE_DIR + "W2008_Berlin_Weekday.dat";
//	private static String srvOutputDirectory = "../../shared-svn/studies/countries/de/open_berlin_scenario/analysis/srv/output2/"; // needs nto be adjusted if applied
//	private static final String OUTPUT_POPULATION_FILE_PATH = srvOutputDirectory + "testOutputPopulation.xml";


	public static void main(String[] args) {
		// MATSim/Simulation
		Events2TripsParser events2TripsParser = new Events2TripsParser(CONFIG_FILE, EVENTS_FILE, NETWORK_FILE);
		List<MatsimTrip> matsimTrips = events2TripsParser.getTrips();
		
		// Set filters if desired
		MatsimTripFilterImpl matsimTripFilter = new MatsimTripFilterImpl();
//		matsimTripFilter.activateMode(TransportMode.car);
//		matsimTripFilter.activateMode("pt", "ptSlow");
		matsimTripFilter.activateMode(TransportMode.pt);
//		matsimTripFilter.activateMode("ptSlow");
//		matsimTripFilter.activateMode("bicycle");
//		matsimTripFilter.activateMode(TransportMode.walk);
		matsimTripFilter.activateStartsOrEndsIn(events2TripsParser.getNetwork(), AREA_SHAPE_FILE, 11000000);
		matsimTripFilter.activateDist(0, 100);
//		matsimTripFilter.activateDepartureTimeRange(7. * 3600, 9. * 3600);
//		matsimTripFilter.activateDepartureTimeRange(16. * 3600, 22. * 3600);
		List<Trip> filteredMatsimTrips = TripFilter.castTrips(matsimTripFilter.filter(matsimTrips));

		analysisOutputDirectory = matsimTripFilter.adaptOutputDirectory(analysisOutputDirectory);
		new File(analysisOutputDirectory).mkdirs();
		GeneralTripAnalyzer.analyze(filteredMatsimTrips, events2TripsParser.getNoPreviousEndOfActivityCounter(),
				events2TripsParser.getPersonStuckCounter(), analysisOutputDirectory);


		// SrV/Survey
		Srv2MATSimPopulation srv2MATSimPopulation = new Srv2MATSimPopulation(SRV_PERSON_FILE_PATH, SRV_TRIP_FILE_PATH);
//		srv2MATSimPopulation.writePopulation(OUTPUT_POPULATION_FILE_PATH);
		List<SrvTrip> srvTrips = srv2MATSimPopulation.getTrips();
		
		// Set filters if desired
		SrvTripFilterImpl srvTripFilter = new SrvTripFilterImpl();
//		srvTripFilter.activateMode(TransportMode.car);
		srvTripFilter.activateMode(TransportMode.pt);
//		srvTripFilter.activateMode(TransportMode.bike);
//		srvTripFilter.activateMode(TransportMode.ride);
		srvTripFilter.activateDist(0, 100);
//		srvTripFilter.activateDepartureTimeRange(7. * 3600, 9. * 3600);
//		srvTripFilter.activateDepartureTimeRange(16. * 3600, 22. * 3600);

		String srvOutputDirectory = srvTripFilter.adaptOutputDirectory("analysis_srv");
		String srvOutputDirectoryFullPath = analysisOutputDirectory + "/" + srvOutputDirectory;
		new File(srvOutputDirectoryFullPath).mkdirs();
		if (!GeneralTripAnalyzer.doesExist(srvOutputDirectoryFullPath)) {
			//filter
			List<Trip> filteredSrvTrips = TripFilter.castTrips(srvTripFilter.filter(srvTrips));
			//write output
			GeneralTripAnalyzer.analyze(filteredSrvTrips, srvOutputDirectoryFullPath);
		}

		// Gnuplot
		String gnuplotScriptName = "plot_abs_path_run.gnu";
		String relativePathToGnuplotScript = "../../../../shared-svn/studies/countries/de/open_berlin_scenario/analysis/gnuplot/" + gnuplotScriptName;
		GnuplotUtils.runGnuplotScript(analysisOutputDirectory, relativePathToGnuplotScript, srvOutputDirectory);

		deleteFolder(new File(srvOutputDirectoryFullPath)); // Delete temporary SrV statistics folder
	}

	private static void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if(files!=null) {
			for(File f: files) {
				if(f.isDirectory()) {
					deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}
}