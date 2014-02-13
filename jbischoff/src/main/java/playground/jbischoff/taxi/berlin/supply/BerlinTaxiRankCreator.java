package playground.jbischoff.taxi.berlin.supply;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.michalm.taxi.model.TaxiRank;

public class BerlinTaxiRankCreator {
	private static final Logger log = Logger.getLogger(BerlinTaxiRankCreator.class);

	private final static String NETWORKFILE = "/Users/jb/tucloud/taxi/berlin/2kW.15.output_network.xml";
	private final static String RANKFILE = "/Users/jb/tucloud/taxi/taxiranks_greaterberlin-1.csv";
	private final static String OUTPUTFILE = "/Users/jb/tucloud/taxi/berlin_ranks.xml";
	private final static String OUTPUTSHP = "/Users/jb/tucloud/taxi/shp_berlin.shp";
	public static void main(String[] args) {
		BerlinTaxiRankCreator btrc = new BerlinTaxiRankCreator();
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile(NETWORKFILE);	
		List <TaxiRank> rankList = btrc.read(sc.getNetwork(), RANKFILE);
		btrc.writeRanks(rankList, OUTPUTFILE);
//		new Links2ESRIShape(sc.getNetwork(), OUTPUTSHP, ,
//				
//					).write();;
	}
	public List<TaxiRank>read (Network network, String rankFile){
		RankReader rr = new RankReader(network);
		TabularFileParserConfig config = new TabularFileParserConfig();
		log.info("parsing " + rankFile);
		config.setDelimiterTags(new String[]{"\t"});
		config.setFileName(rankFile);
		new TabularFileParser().parse(config, rr);
		log.info("done. (parsing " + rankFile + ")");
		return rr.getRanks();
		
		
	}

	public void writeRanks(List<TaxiRank> rankList, String outputFile){
		
		try {
			FileWriter fw = new FileWriter(new File(outputFile));
			fw.append("<?xml version=\"1.0\" ?>\n<!DOCTYPE ranks SYSTEM \"http://matsim.org/files/dtd/taxi_ranks_v1.dtd\">\n<ranks>\n");
			for (TaxiRank rank : rankList){
				fw.append("<rank id=\"" + rank.getId().toString() +"\" name=\""+rank.getName()+"\" link=\""+rank.getLink().getId().toString()+"\">\n"); 
				fw.append("</rank>\n");
			}
			
			fw.append("</ranks>");
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}



class RankReader implements TabularFileHandler{

	
	private NetworkImpl network;
	private List<TaxiRank> ranks = new ArrayList<TaxiRank>();
	private CoordinateTransformation ct = TransformationFactory
			.getCoordinateTransformation(
									TransformationFactory.WGS84,
									TransformationFactory.DHDN_GK4
						);
	RankReader(Network network){
		this.network = (NetworkImpl) network;
	}
	
	@Override
	public void startRow(String[] row) {
		Link link = network.getNearestRightEntryLink(stringtoCoord(row[2], row[1]));
		String name = row[4];
		Id id = new IdImpl(row[5]);
		TaxiRank rank = new TaxiRank(id, name, link);
		ranks.add(rank);
	}
	
	Coord stringtoCoord(String x, String y){
		String xcoordString = x.substring(2);
		double xc = 13. + Double.parseDouble("0."+xcoordString);
		String ycoordString = y.substring(2);
		double yc = 52. + Double.parseDouble("0."+ycoordString);
		Coord coord = new CoordImpl(xc, yc);
		Coord trans =ct.transform(coord); 
		System.out.println("Read x"+ x + " Read y "+ y + " coord read "+ coord + " transformed "+trans );
		return trans;
	}

	public List<TaxiRank> getRanks() {
		return ranks;
	}
	
}
