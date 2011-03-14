package playground.demandde.pendlermatrix;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.mzilske.pipeline.PopulationWriterTask;
import playground.mzilske.pipeline.RoutePersonTask;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;




public class PendlerMatrixReader {

	private static final Logger log = Logger.getLogger(PendlerMatrixReader.class);

	//	private static final String PV_EINPENDLERMATRIX = "../../shared-svn/studies/countries/de/pendler_nach_gemeinden/CD_Pendler_Gemeindeebene_30_06_2009/einpendler-muenchen.csv";
	private static final String PV_EINPENDLERMATRIX = "../../detailedEval/eingangsdaten/Pendlermatrizen/EinpendlerMUC_843_062004.csv";

	private static final String PV_AUSPENDLERMATRIX = "../../detailedEval/eingangsdaten/Pendlermatrizen/AuspendlerMUC_843_062004.csv";

	private static final String NODES = "../../shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2004/strasse/knoten_wgs84.csv";

	private Map<Integer, Zone> zones = new HashMap<Integer, Zone>();

	//	private static final String FILENAME = "/Users/michaelzilske/workspace/prognose_2025/demand/naechster_versuch.xml";
	//
	//	private static final String NETWORK_FILENAME = "/Users/michaelzilske/osm/motorway_germany.xml";
	//
	//	private static final String FILTER_FILENAME = "/Users/michaelzilske/workspace/prognose_2025/demand/filter.shp";

	private TripFlowSink flowSink;

	public void run() {
		readNodes();
		readMatrix(PV_EINPENDLERMATRIX);
		readMatrix(PV_AUSPENDLERMATRIX);
		flowSink.complete();
	}

	private void readNodes() {
		final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(NODES);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});
		try {
			new TabularFileParser().parse(tabFileParserConfig,
					new TabularFileHandler() {
				@Override
				public void startRow(String[] row) {
					if (row[0].startsWith("Knoten")) {
						return;
					}
					int zone = Integer.parseInt(row[5]);
					double x = Double.parseDouble(row[2]);
					double y = Double.parseDouble(row[3]);
					Zone zone1 = new Zone(zone, 1, 1, coordinateTransformation.transform(new CoordImpl(x,y)));
					zones.put(zone, zone1);
				}

			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void readMatrix(final String filename) {
		System.out.println("======================" + "\n"
						   + "Start reading " + filename + "\n"
						   + "======================" + "\n");
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {","});
		try {
			new TabularFileParser().parse(tabFileParserConfig,
					new TabularFileHandler() {

				@Override
				public void startRow(String[] row) {
					if (row[0].startsWith("#")) {
						return;
					}
					Integer quelle = null ;
					Integer ziel = 0;
					// car market share for commuter work/education trips (taken from "Regionaler Nahverkehrsplan-Fortschreibung, MVV 2007)
					double carMarketShare = 0.67;
					// scale factor, since Pendlermatrix only considers "sozialversicherungspflichtige Arbeitnehmer" (taken from GuthEtAl2005)
					double scaleFactor = 1.29;

					if (filename.equals(PV_EINPENDLERMATRIX)){
						try {
							quelle = Integer.parseInt(row[2]);
							ziel = 9162 ;
							
							int totalTrips = (int) (scaleFactor * Integer.parseInt(row[4]));
							int workPt = (int) ((1 - carMarketShare) * totalTrips) ;
							int educationPt = 0 ;
							int workCar = (int) (carMarketShare * totalTrips);
							int educationCar = 0 ;
							String label = row[3] ;
							if ( !label.contains("brige ") && !quelle.equals(ziel)) {
								process(quelle, ziel, workPt, educationPt, workCar, educationCar);
							} else {
								System.out.println( " uebrige? : " + label ) ;
							}
						} catch ( Exception ee ) {
							System.err.println("we are trying to read quelle: " + quelle ) ;
							//						System.exit(-1) ;
						}
					}
					else if (filename.equals(PV_AUSPENDLERMATRIX)){
						try {
							quelle = 9162;
							ziel = Integer.parseInt(row[2]);

							int totalTrips = (int) (scaleFactor * Integer.parseInt(row[4]));
							int workPt = (int) ((1 - carMarketShare) * totalTrips) ;
							int educationPt = 0 ;
							int workCar = (int) (carMarketShare * totalTrips);
							int educationCar = 0 ;
							String label = row[3] ;
							if ( !label.contains("brige ") && !quelle.equals(ziel)) {
								process(quelle, ziel, workPt, educationPt, workCar, educationCar);
							} else {
								System.out.println( " uebrige? : " + label ) ;
							}
						} catch ( Exception ee ) {
							System.err.println("we are trying to read quelle: " + quelle ) ;
							//						System.exit(-1) ;
						}
					}
					else{
						System.err.println("ATTENTION: check filename!") ;
					}
				}

			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean isCoordInShape(Coord linkCoord, Set<Feature> features, GeometryFactory factory) {
		boolean found = false;
		Geometry geo = factory.createPoint(new Coordinate(linkCoord.getX(), linkCoord.getY()));
		for (Feature ft : features) {
			if (ft.getDefaultGeometry().contains(geo)) {
				found = true;
				break;
			}
		}
		return found;
	}

	private void process(int quelle, int ziel, int workPt, int educationPt, int workCar, int educationCar) {
		Zone source = zones.get(quelle);
		Zone sink = zones.get(ziel);
		if (source == null) {
			log.error("Unknown source: " + quelle);
			return;
		}
		if (sink == null) {
			log.error("Unknown sink: " + ziel);
			return;
		}
		int carQuantity = workCar + educationCar ;
		int ptQuantity = workPt + educationPt;
		int scaledCarQuantity = scale(carQuantity);
		int scaledPtQuantity = scale(ptQuantity);
		
		if (scaledCarQuantity != 0) {
			log.info(quelle + "->" + ziel + ": " + scaledCarQuantity + " car trips");
			flowSink.process(zones.get(quelle), zones.get(ziel), scaledCarQuantity, TransportMode.car, "pvWork", 0.0);
		}
		if (scaledPtQuantity != 0){
			log.info(quelle + "->" + ziel + ": " + scaledPtQuantity + " pt trips");
			flowSink.process(zones.get(quelle), zones.get(ziel), scaledPtQuantity, TransportMode.pt, "pvWork", 0.0);
		}
	}

	//	private int getCarQuantity(Zone source, Zone sink, int carWorkTripsPerDay) {
	//		double outWeight = ((double) source.workingPopulation * sink.workplaces) /  ((double) source.workplaces * sink.workingPopulation);
	//		double inWeight = ((double) source.workplaces * sink.workingPopulation) /  ((double) source.workingPopulation * sink.workplaces);
	//		double outShare = outWeight / (inWeight + outWeight);
	//		int amount = (int) (outShare * carWorkTripsPerDay * 0.5);
	//		return amount;
	//	}

	private int scale(int quantityOut) {
		int scaled = (int) (quantityOut * 0.1 );
		return scaled;
	}

	void setFlowSink(TripFlowSink flowSink) {
		this.flowSink = flowSink;
	}

}
