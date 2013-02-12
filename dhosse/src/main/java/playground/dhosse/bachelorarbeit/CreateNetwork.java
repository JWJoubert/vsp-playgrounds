package playground.dhosse.bachelorarbeit;

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;


public class CreateNetwork {
	
	public static void main(String[] args) throws IOException {

	    String osm = "./input/berlin.osm";
	    Config config = ConfigUtils.createConfig();
	    Scenario sc = ScenarioUtils.createScenario(config);
	    Network net = sc.getNetwork();
	  	String crs = "PROJCS[\"ETRS89_UTM_Zone_33\"," +
	  				"GEOGCS[\"GCS_ETRS89\",DATUM[\"D_ETRS89\",SPHEROID[\"GRS_1980\"," +
	  				"6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\"," +
	  				"0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"]," +
	  				"PARAMETER[\"False_Easting\",3500000.0],PARAMETER[\"False_Northing\"," +
	  				"0.0],PARAMETER[\"Central_Meridian\",15.0],PARAMETER[\"Scale_Factor\"," +
	  				"0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]";
	  	CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84);
	  	
	  	OsmNetworkReader onr = new OsmNetworkReader(net,ct);
	    onr.parse(osm);
	    
	    new NetworkCleaner().run(net);
	    new NetworkWriter(net).write("./input/berlin_osm.xml");
	   }

}
