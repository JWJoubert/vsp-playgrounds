package air;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class MATSimAirport {
	
	public Coord coord;
	public Id id;
	
	public MATSimAirport(Id id, Coord coord) {
		this.id=id;
		this.coord=coord;
	}
	
	public void createRunways(Network network) {
		
//		Node mit IATA als Zentrales Element, Link mit IATA Code an Node,
//		Taxiway von IATA zu Runway_outbound und Runway_inbound
//		Runway_inbound, Runway_outbound
//		coord.getX und coord.getY gibt double, dann addieren 0.001 = 111m
		
//		create Ids for nodes and links
		
		Id idApron = this.id;													//Id for apron link and central airport node
		Id idApronEnd = new IdImpl(this.id+"apron");							//Id for end of apron node
		Id idTaxiIn = new IdImpl(this.id.toString()+"taxiInbound");				//Id for taxiway link and end of taxiway node
		Id idTaxiOut = new IdImpl(this.id.toString()+"taxiOutbound");			//Id for taxiway link and end of taxiway node
		Id idRunwayIn = new IdImpl(this.id.toString()+"runwayInbound");			//Id for runway link and end of runway node
		Id idRunwayOut = new IdImpl(this.id.toString()+"runwayOutbound");		//Id for runway link and end of runway node	
		
//		create Coords for nodes
		
		Coord coordApronEnd = new CoordImpl(this.coord.getX(), this.coord.getY()+0.001);			//shifting end of apron by 111 meters in Y direction
		Coord coordTaxiIn = new CoordImpl(this.coord.getX()-0.001, this.coord.getY()-0.001);		//shifting taxiway
		Coord coordTaxiOut = new CoordImpl(this.coord.getX()-0.001, this.coord.getY()+0.001);		//shifting taxiway
		Coord coordRunwayInEnd = new CoordImpl(coordTaxiIn.getX(), coordTaxiIn.getY()-0.01);		//shifting runway
		Coord coordRunwayOutEnd = new CoordImpl(coordTaxiOut.getX(), coordTaxiIn.getY()-0.01);		//shifting runway
		
//		create nodes
		
		Node nodeAirport = network.getFactory().createNode(idApron, this.coord);			//central node of any airport	
		Node nodeApron = network.getFactory().createNode(idApronEnd, coordApronEnd);		//end of apron node, apron is used for parking and as transit stop
		Node nodeTaxiIn = network.getFactory().createNode(idTaxiIn, coordTaxiIn);			//taxiway inbound start = runway inbound end
		Node nodeTaxiOut = network.getFactory().createNode(idTaxiOut, coordTaxiOut);		//taxiway outbound end = runway outbound start
		Node nodeRunwayIn = network.getFactory().createNode(idRunwayIn, coordRunwayInEnd);	//start of inbound runway
		Node nodeRunwayOut = network.getFactory().createNode(idRunwayOut, coordRunwayOutEnd);	//end of outbound runway
		
//		add nodes to network
		
		network.addNode(nodeAirport); 			
		network.addNode(nodeApron);
		network.addNode(nodeTaxiIn);
		network.addNode(nodeTaxiOut);
		network.addNode(nodeRunwayIn);
		network.addNode(nodeRunwayOut);
		
//		create links
		
		Link linkApron = network.getFactory().createLink(idApron, idApron, idApronEnd);
		Link linkTaxiIn = network.getFactory().createLink(idTaxiIn, idTaxiIn, idApron);
		Link linkTaxiOut = network.getFactory().createLink(idTaxiOut, idApron, idTaxiOut);
		Link linkRunwayIn = network.getFactory().createLink(idRunwayIn, idRunwayIn, idTaxiIn);
		Link linkRunwayOut = network.getFactory().createLink(idRunwayOut, idTaxiOut, idRunwayOut);
		
//		set capacity, freespeed and modes
		
//		add links to network
				
		network.addLink(linkApron);	
		network.addLink(linkTaxiIn);
		network.addLink(linkTaxiOut);		
		network.addLink(linkRunwayIn);	
		network.addLink(linkRunwayOut);
	}

}
