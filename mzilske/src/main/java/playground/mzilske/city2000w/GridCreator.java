/**
 * 
 */
package playground.mzilske.city2000w;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;

/**
 * @author schroeder
 *
 */
public class GridCreator {

	private static final int GRID_SIZE = 8;

	private static final String NETWORK_FILENAME = "output/grid.xml";

	private Scenario scenario;

	private void createGrid(int size) {
		Config config = new Config();
		config.addCoreModules();
		scenario = new ScenarioImpl(config);
		Network network = scenario.getNetwork();
		for (int i = 0; i <= size; i++) {
			for (int j = 0; j <= size; j++) {
				Node node = network.getFactory().createNode(makeId(i, j), makeCoord(i, j));
				network.addNode(node);
				if (i != 0) {
					Link iLink = network.getFactory().createLink(makeLinkId(i, j), makeId(i-1, j), makeId(i, j));
					iLink.setLength(1000);
					Link iLinkR = network.getFactory().createLink(scenario.createId("i("+i+","+j+")"+"R"), makeId(i, j),makeId(i-1, j));
					iLinkR.setLength(1000);
					network.addLink(iLink);
					network.addLink(iLinkR);
				}
				if (j != 0) {
					Link jLink = network.getFactory().createLink(scenario.createId("j("+i+","+j+")"), makeId(i, j-1), makeId(i, j));
					jLink.setLength(1000);
					Link jLinkR = network.getFactory().createLink(scenario.createId("j("+i+","+j+")"+"R"), makeId(i, j), makeId(i, j-1));
					jLinkR.setLength(1000);
					network.addLink(jLink);
					network.addLink(jLinkR);
				}
			}
		}
	}

	private Id makeLinkId(int i, int j) {
		return scenario.createId("i("+i+","+j+")");
	}

	private Coord makeCoord(int i, int j) {
		return scenario.createCoord(i * 1000, j * 1000);
	}

	private Id makeId(int i, int j) {
		return scenario.createId("("+i+","+j+")");
	}
	
	private void writeNetwork() {
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		new NetworkWriter(network).write(NETWORK_FILENAME);
	}
	
	void run() {
		createGrid(8);
		writeNetwork();
	}
	
	public static void main(String[] args) {
		GridCreator gridCreator = new GridCreator();
		gridCreator.run();
	}
	
}
