package playground.andreas.utils.ana;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.vsp.analysis.VspAnalyzer;
import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.analysis.modules.act2mode.ActivityToModeAnalysis;
import playground.vsp.analysis.modules.boardingAlightingCount.BoardingAlightingCountAnalyzer;
import playground.vsp.analysis.modules.ptAccessibility.PtAccessibility;
import playground.vsp.analysis.modules.ptPaxVolumes.PtPaxVolumesAnalyzer;
import playground.vsp.analysis.modules.ptPaxVolumes.PtPaxVolumesHandler;
import playground.vsp.analysis.modules.ptTripAnalysis.traveltime.TTtripAnalysis;
import playground.vsp.analysis.modules.stuckAgents.GetStuckEventsAndPlans;
import playground.vsp.analysis.modules.transitSchedule2Shp.TransitSchedule2Shp;
import playground.vsp.analysis.modules.transitVehicleVolume.TransitVehicleVolumeAnalyzer;

import com.vividsolutions.jts.geom.Geometry;



public class AnalysisRunner {

	/**
	 * 
	 * @param args OutputDir RunId iteration gridSize shapeFile quadrantSegments
	 */
	public static void main(String[] args) {
		
		String outputDir = args[0];
		String runId = args[1];
		int iteration = Integer.parseInt(args[2]);
		int gridSize = Integer.valueOf(args[3]);
		String shapeFile = args[4];
		int quadrantSegments = Integer.parseInt(args[5]);
		
		
		String oldJavaIoTempDir = System.getProperty("java.io.tmpdir");
		String newJavaIoTempDir = outputDir;
		System.out.println("Setting java tmpDir from " + oldJavaIoTempDir + " to " + newJavaIoTempDir);
		System.setProperty("java.io.tmpdir", newJavaIoTempDir);
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().scenario().setUseTransit(true);
		
//		String targetCoordinateSystem = TransformationFactory.WGS84_UTM35S; // Gauteng
		String targetCoordinateSystem = TransformationFactory.WGS84_UTM33N; // Berlin
		
		OutputDirectoryHierarchy dir = new OutputDirectoryHierarchy(outputDir + "/" + runId + "/", runId, true, true);
		
		new TransitScheduleReader(sc).readFile(dir.getIterationFilename(iteration, "transitScheduleScored.xml.gz"));
		new MatsimNetworkReader(sc).readFile(dir.getOutputFilename(Controler.FILENAME_NETWORK));
		new MatsimFacilitiesReader((ScenarioImpl) sc).readFile(dir.getOutputFilename("output_facilities.xml.gz"));
		new MatsimPopulationReader(sc).readFile(dir.getIterationFilename(iteration, "plans.xml.gz"));
		
		List<Integer> cluster = new ArrayList<Integer>(){{
			add(100);
			add(500);
			add(1000);
		}};
		
		SortedMap<String, List<String>> activityCluster = BVG3ActsScheme.createBVG3ActsScheme();		
		
		Set<String> ptModes = new HashSet<String>(){{
			add("pt");
		}};
		
		Set<String> networkModes = new HashSet<String>(){{
			add("car");
		}};
		
		Set<Feature> features = new ShapeFileReader().readFileAndInitialize(shapeFile);
		Map<String, Geometry> zones =  new HashMap<String, Geometry>();
		for(Feature f: features){
			zones.put((String)f.getAttribute(2), (Geometry) f.getAttribute(0));
		}
		
		VspAnalyzer analyzer = new VspAnalyzer(dir.getOutputPath(), dir.getIterationFilename(iteration, Controler.FILENAME_EVENTS_XML));
		
		// works
		PtAccessibility ptAccessibility = new PtAccessibility(sc, cluster, quadrantSegments, activityCluster, targetCoordinateSystem, gridSize);
		analyzer.addAnalysisModule(ptAccessibility);
		
		GetStuckEventsAndPlans getStuckEventsAndPlans = new GetStuckEventsAndPlans(sc);
		analyzer.addAnalysisModule(getStuckEventsAndPlans);
		
		TransitVehicleVolumeAnalyzer transitVehicleVolumeAnalyzer = new TransitVehicleVolumeAnalyzer(sc, 3600., targetCoordinateSystem);
		analyzer.addAnalysisModule(transitVehicleVolumeAnalyzer);
		
		PtPaxVolumesAnalyzer ptPaxVolumesAnalyzer = new PtPaxVolumesAnalyzer(sc, 3600., targetCoordinateSystem);
		analyzer.addAnalysisModule(ptPaxVolumesAnalyzer);
		
		TTtripAnalysis ttTripAnalysis = new TTtripAnalysis(ptModes, networkModes, sc.getPopulation());	ttTripAnalysis.addZones(zones);
		analyzer.addAnalysisModule(ttTripAnalysis);
		
		TransitSchedule2Shp transitSchedule2Shp = new TransitSchedule2Shp(sc, targetCoordinateSystem);
		analyzer.addAnalysisModule(transitSchedule2Shp);
		
		ActivityToModeAnalysis activityToModeAnalysis = new ActivityToModeAnalysis(sc, null, 3600, targetCoordinateSystem);
		analyzer.addAnalysisModule(activityToModeAnalysis);
		
		BoardingAlightingCountAnalyzer boardingAlightingCountAnalyzes = new BoardingAlightingCountAnalyzer(sc, 3600, targetCoordinateSystem);
		boardingAlightingCountAnalyzes.setWriteHeatMaps(true, gridSize);
		
		analyzer.addAnalysisModule(boardingAlightingCountAnalyzes);
		analyzer.addAnalysisModule(new MyPtCount());

		analyzer.run();
		
		System.out.println("Setting java tmpDir from " + newJavaIoTempDir + " to " + oldJavaIoTempDir);
		System.setProperty("java.io.tmpdir", oldJavaIoTempDir);
	}
}

class MyPtCount extends AbstractAnalyisModule{

	PtPaxVolumesHandler handler;
	private ArrayList<Id> links;
	/**
	 * @param name
	 */
	public MyPtCount() {
		super(MyPtCount.class.getSimpleName());
		this.handler = new PtPaxVolumesHandler(3600.); 
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new ArrayList<EventHandler>();
		handler.add(this.handler);
		return handler;
	}

	@Override
	public void preProcessData() {
		this.links = new ArrayList<Id>();
		links.add(new IdImpl("90409-90411-90413-90415-90417-90419"));
		links.add(new IdImpl("90420-90418-90416-90414-90412-90410"));
		links.add(new IdImpl("20706-20707"));
		links.add(new IdImpl("72219-72220-72221"));
		links.add(new IdImpl("72241-72242-72243-72244"));
		links.add(new IdImpl("20726-20727-20728"));
		links.add(new IdImpl("24360-24361-24362-24363-24364"));
		links.add(new IdImpl("218-219-220-221-222"));
		links.add(new IdImpl("34580-34581-34582-34583-34584"));
		links.add(new IdImpl("73503-73504"));
		links.add(new IdImpl("53096-53097-53098"));
		links.add(new IdImpl("78332-78333-78334"));
		links.add(new IdImpl("18607-18605-18603-18601-18599-18597-18595-18593-18591-18589-18587-18585-18583-18581-18579-18577"));
		links.add(new IdImpl("18576-18578-18580-18582-18584-18586-18588-18590-18592-18594-18596-18598-18600-18602-18604"));
	}

	@Override
	public void postProcessData() {
		
	}

	@Override
	public void writeResults(String outputFolder) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder + "ptPaxVolumes.csv");
		try {
			//header
			writer.write("LinkId;total;");
			for(int i = 0; i < this.handler.getMaxInterval() + 1; i++){
					writer.write(String.valueOf(i) + ";");
			}
			writer.newLine();
			//content
			for(Id id: this.links){
				writer.write(id.toString() + ";");
				writer.write(this.handler.getPaxCountForLinkId(id) + ";");
				for(int i = 0; i < this.handler.getMaxInterval() + 1; i++){
					writer.write(this.handler.getPaxCountForLinkId(id, i) + ";");
				}
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}



