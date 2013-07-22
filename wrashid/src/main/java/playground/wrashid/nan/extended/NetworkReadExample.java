package playground.wrashid.nan.extended;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.xml.sax.SAXException;
import org.matsim.core.utils.io.MatsimXmlParser;
public class NetworkReadExample {

	public static void main(String[] args) {
		//getFilteredEquilNetLinks();
	}
	public static Map<Id,Link> getNetworkLinks(String networkFile, Coord center, double radius){ //read network
		// read the network file
		NetworkImpl network=(NetworkImpl) GeneralLib.readNetwork(networkFile);
		
		
		if (center==null){
			return network.getLinks(); //return all links without filtering, do it later
		} else {
			return getLinksWithinRadius(network.getLinks(),radius,center); //set radius of the targeted area
		}
		
	}
	public static Map<Id, Link> getLinksWithinRadius(Map<Id, Link> links, double radius, Coord center){
		HashMap<Id,Link> filteredLinks=new HashMap<Id, Link>();
			
		for (Id linkId:links.keySet()){
			Link link=links.get(linkId); //find all the links
						
			if (getDistance(link.getCoord(),center)<radius){ // filter links
				filteredLinks.put(linkId, link);
			}
		}
		return filteredLinks;
		
	}
	public static double getDistance(Coord coordA, Coord coordB){
		return Math.sqrt(((coordA.getX()-coordB.getX())*(coordA.getX()-coordB.getX()) + (coordA.getY()-coordB.getY())*(coordA.getY()-coordB.getY())));
	} //filter algorithm
	

}
