package playground.mzilske.neo;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.MobsimConfigGroupI;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.SnapshotGenerator;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;
import org.neo4j.graphdb.Transaction;

public class NeoLiveMain {

	public static void main(String[] args) throws InterruptedException, InvocationTargetException {

		String directory =  "output/neo";
		Map<String,String> config = new HashMap<String,String>();
		config.put("neostore.nodestore.db.mapped_memory","80M");
		config.put("neostore.relationshipstore.db.mapped_memory","750M");
		config.put("neostore.propertystore.db.mapped_memory","0M");
		config.put("neostore.propertystore.db.strings.mapped_memory","0M");
		config.put("neostore.propertystore.db.arrays.mapped_memory","0M");

		String eventsFileName = "../../matsim/output/example5/ITERS/it.10/10.events.xml.gz";

		double snapshotPeriod = 60;
		MobsimConfigGroupI simulationConfigGroup = new SimulationConfigGroup();
		NeoScenario scenario = new NeoScenario(directory, config);
		try {
			Transaction tx = scenario.beginTx();
			try {
				EventsManager events = (EventsManager) EventsUtils.createEventsManager();


				final OnTheFlyServer server = OnTheFlyServer.createInstance(scenario, events);
				SnapshotGenerator snapshotGenerator = new SnapshotGenerator(scenario.getNetwork(), (int) snapshotPeriod, simulationConfigGroup); 
				snapshotGenerator.addSnapshotWriter(server.getSnapshotReceiver());
				events.addHandler(snapshotGenerator);

				NeoOTFLiveServerTransactionWrapper wrappedServer = new NeoOTFLiveServerTransactionWrapper(server, scenario);

				OTFClientLive.run(scenario.getConfig(), wrappedServer);

				System.out.println("Reading...");
				new MatsimEventsReader(events).readFile(eventsFileName);
	
				tx.success();
			} finally {
				tx.finish();
			}
		} finally {
			scenario.shutdown();
		}
	}

}
