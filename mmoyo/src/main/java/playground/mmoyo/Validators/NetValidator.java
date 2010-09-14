package playground.mmoyo.Validators;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mmoyo.PTRouter.PTTravelTime;
import playground.mmoyo.PTRouter.PTValues;
import playground.mmoyo.PTRouter.Station;

/**
 * Validates all links in a network for correct cost values and lengths
 */
public class NetValidator {

	private NetworkImpl net;
	private PTTravelTime ptTravelTime;

	public NetValidator (final NetworkImpl net, final PTTravelTime ptTraveltime){
		this.net = net;
		this.ptTravelTime = ptTraveltime;
	}

	public void printNegativeStandardCosts(){
		boolean found=false;
		for(Link link: net.getLinks().values()){
			if(((LinkImpl) link).getType().equals("Standard")){
				double cost= ptTravelTime.getLinkTravelTime(link,0);
				if (cost<0){
					System.out.println(link.getId() + " link has negative cost: " + cost);
					found=true;
				}
			}
		}
		if (!found)
			System.out.println("No negative costs found");
	}

	public void printNegativeTransferCosts(double time){
		int x=0;
		for(Link link: net.getLinks().values()){
			if(((LinkImpl) link).getType().equals(PTValues.TRANSFER_STR)){
				double cost= ptTravelTime.getTransferTime((Station)link.getToNode(), time);
				if (cost<0){
					System.out.println(link.getId() + " link has negative transfer time: " + cost);
					x++;
				}
			}
		}
		System.out.println("negative costs found: " + x);
	}

	public boolean validLinkLengths(){
		for (Link link : net.getLinks().values()){
			Coord from = link.getFromNode().getCoord();
			Coord to = link.getToNode().getCoord();
			double distance= CoordUtils.calcDistance(from, to);
			if (link.getLength()!= distance ){
				return false;
			}
		}
		return true;
	}
}
