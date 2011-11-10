package air.scenario;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;


public class SfAirNetworkBuilder {

	public void createNetwork() throws IOException {
		
		int airportcounter = 0;
		int linkcounter = 0;
		
		String output = "/home/soeren/workspace/euroAirNetwork";
//		String output = "/home/soeren/workspace/germanAirNetwork";
		Set<String> allowedModes = new HashSet<String>();
		allowedModes.add("pt");
		allowedModes.add("car");

		NetworkImpl network = NetworkImpl.createNetwork();
		network.setCapacityPeriod(1.0);		//capacity period set to one second to allow storage capacity = 1 for runway with runway length of 450 meters and 1/60 flow capacity
		
		BufferedReader brAirports = new BufferedReader(new FileReader(new File("/home/soeren/workspace/osmEuroAirports.txt")));
		BufferedReader brRoutes = new BufferedReader(new FileReader(new File("/home/soeren/workspace/cityPairs.txt")));
		
//		BufferedReader brAirports = new BufferedReader(new FileReader(new File("/home/soeren/workspace/osmGermanAirports.txt")));
//		BufferedReader brRoutes = new BufferedReader(new FileReader(new File("/home/soeren/workspace/cityPairsGermany.txt")));
		
		CoordinateTransformation coordtransform =
			TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:3395");
		
		while (brAirports.ready()) {
			String oneLine = brAirports.readLine();
			String[] lineEntries = oneLine.split("\t");
			String airportCode = lineEntries[0];
			double xValue = Double.parseDouble(lineEntries[1]);
//				if (xValue<=-10.) System.out.println(oneLine);
			double yValue = Double.parseDouble(lineEntries[2]);		
			Coord coord = new CoordImpl(xValue, yValue);
			Coord airportCoord = coordtransform.transform(coord);
			
			airportcounter++;
			
			new SfMatsimAirport(new IdImpl(airportCode), airportCoord).createRunways(network);			
		}
		
		
		while (brRoutes.ready()) {
			String oneLine = brRoutes.readLine();
			String[] lineEntries = oneLine.split("\t");
			String[] airportCodes = lineEntries[0].split("_");
			double length = Double.parseDouble(lineEntries[1])*1000;	//distance between O&D in meters
			double flightTime = Double.parseDouble(lineEntries[2]);		
			if (flightTime>=3600.) flightTime-=600.;//flight time in seconds, assumption: 600secs for taxi/take-off/landing
			else if (flightTime>=1800 && flightTime<3600.) flightTime-=300.;
			else flightTime-=120.;
			double groundSpeed = length/flightTime;	
				if (groundSpeed<=50.) System.out.println(oneLine+"\t"+groundSpeed);
			String origin = airportCodes[0];
			String destination = airportCodes[1];
			
			
			//LÖSCHEN
//			if ((origin.equalsIgnoreCase("TXL") && destination.equalsIgnoreCase("ZRH")) || (origin.equalsIgnoreCase("ZRH") && destination.equalsIgnoreCase("TXL")) ) {
			
			Id originRunway = new IdImpl(origin+"runwayOutbound");
			Id destinationRunway = new IdImpl(destination+"runwayInbound");
			Link originToDestination = network.getFactory().createLink(new IdImpl(origin+destination), network.getNodes().get(originRunway), network.getNodes().get(destinationRunway));
			originToDestination.setAllowedModes(allowedModes);
			originToDestination.setCapacity(1.0);
			originToDestination.setFreespeed(groundSpeed);
			originToDestination.setLength(length);
			network.addLink(originToDestination);
			
			linkcounter++;
			
//			} // HIER LÖSCHEN
			
		}
			
		new NetworkWriter(network).write(output + ".xml");
		System.out.println("Done! Unprocessed MATSim Network saved as " + output + ".xml");
		
		System.out.println("Anzahl Flughäfen: "+airportcounter);
		System.out.println("Anzahl Links: "+linkcounter);
		
		brAirports.close();
		brRoutes.close();
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		SfAirNetworkBuilder builder = new SfAirNetworkBuilder();
		builder.createNetwork();
		
	}

	
}
