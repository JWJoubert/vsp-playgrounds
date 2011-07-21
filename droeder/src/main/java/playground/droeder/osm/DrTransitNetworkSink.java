package playground.droeder.osm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.TagCollectionImpl;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.filter.common.IdTracker;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerFactory;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.IndexedObjectStore;
import org.openstreetmap.osmosis.core.store.IndexedObjectStoreReader;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class DrTransitNetworkSink implements Sink {
	
	static final Logger log = Logger.getLogger(DrTransitNetworkSink.class);

	private SimpleObjectStore<NodeContainer> allNodes;

	private IndexedObjectStore<NodeContainer> stopNodeStore;

	private IndexedObjectStore<WayContainer> routeSegmentStore;

	private SimpleObjectStore<WayContainer> allWays;

	private SimpleObjectStore<RelationContainer> transitLines;

	private IdTracker stopNodes;

	private IdTracker routeWays;

	private IdTracker allWaysTracker;

	private IdTracker allNodesTracker;

	private int count = 0;

	private Network network;

	private TreeSet<String> transitModes;

	private Map<Id, NetworkImpl> line2Network;

	public DrTransitNetworkSink(Network network, IdTrackerType idTrackerType) {
		this.network = network;
		stopNodes = IdTrackerFactory.createInstance(idTrackerType);
		routeWays = IdTrackerFactory.createInstance(idTrackerType);
		allWaysTracker = IdTrackerFactory.createInstance(idTrackerType);
		allNodesTracker = IdTrackerFactory.createInstance(idTrackerType);
		allNodes = new SimpleObjectStore<NodeContainer>(
				new SingleClassObjectSerializationFactory(NodeContainer.class),
				"afnd", true);
		stopNodeStore = new IndexedObjectStore<NodeContainer>(
				new SingleClassObjectSerializationFactory(NodeContainer.class),
		"stops");
		routeSegmentStore = new IndexedObjectStore<WayContainer>(
				new SingleClassObjectSerializationFactory(WayContainer.class),
		"routesegments");
		allWays = new SimpleObjectStore<WayContainer>(
				new SingleClassObjectSerializationFactory(WayContainer.class),
				"afwy", true);
		transitLines = new SimpleObjectStore<RelationContainer>(
				new SingleClassObjectSerializationFactory(RelationContainer.class),
				"afrl", true);
	}

	@Override
	public void process(EntityContainer entityContainer) {
		entityContainer.process(new EntityProcessor() {

			@Override
			public void process(BoundContainer arg0) {

			}

			@Override
			public void process(NodeContainer container) {

				// stuff all nodes into a file
				allNodesTracker.set(container.getEntity().getId());
				allNodes.add(container);

				// debug
				count++;
				if (count % 50000 == 0)
					log.info(count + " nodes processed so far");
			}

			@Override
			public void process(RelationContainer relationContainer) {
				Relation relation = relationContainer.getEntity();
				Map<String, String> tags = new TagCollectionImpl(relation.getTags()).buildMap();
				if ("route".equals(tags.get("type"))){
					if(tags.get("route") == null){
						log.info("Got an empty route tag for tags " + tags.toString());
					} else {
						if(getTransitModes().contains(tags.get("route"))) {//&& "tram".equals(tags.get("route"))) {
							transitLines.add(relationContainer);
						}
					}
				}
			}

			@Override
			public void process(WayContainer container) {
				allWaysTracker.set(container.getEntity().getId());
				allWays.add(container);
			}

		});
	}

	@Override
	public void complete() {
		ReleasableIterator<RelationContainer> transitLineIterator = transitLines.iterate();
		while (transitLineIterator.hasNext()) {
			Relation relation = transitLineIterator.next().getEntity();
			for (RelationMember relationMember : relation.getMembers()) {
				if (relationMember.getMemberType().equals(EntityType.Node)) {
					stopNodes.set(relationMember.getMemberId());
				} else if (relationMember.getMemberType().equals(EntityType.Way)) {
					routeWays.set(relationMember.getMemberId());
				} 
			}
		}
		transitLineIterator.release();

		ReleasableIterator<NodeContainer> nodeIterator = allNodes.iterate();
		while (nodeIterator.hasNext()) {
			NodeContainer nodeContainer = nodeIterator.next();
			Node node = nodeContainer.getEntity();
			if (stopNodes.get(node.getId())) {
				System.out.println(node.getId());
				stopNodeStore.add(node.getId(), nodeContainer);
			}
		}
		nodeIterator.release();
		stopNodeStore.complete();

		ReleasableIterator<WayContainer> wayIterator = allWays.iterate();
		while (wayIterator.hasNext()) {
			WayContainer wayContainer = wayIterator.next();
			Way way = wayContainer.getEntity();
			if (routeWays.get(way.getId())) {
				routeSegmentStore.add(way.getId(), wayContainer);
			}
		}
		wayIterator.release();
		routeSegmentStore.complete();

		transitLineIterator = transitLines.iterate();

		IndexedObjectStoreReader<NodeContainer> nodeReader = stopNodeStore.createReader();
		IndexedObjectStoreReader<WayContainer> wayReader = routeSegmentStore.createReader();
		log.info("Processing transit lines...");
		LineNetworkStore netStore = new LineNetworkStore(network);
		while (transitLineIterator.hasNext()) {
			Relation relation = transitLineIterator.next().getEntity();
			Map<String, String> tags = new TagCollectionImpl(relation.getTags()).buildMap();
			String route = tags.get("route");
			String ref = tags.get("ref");
			if(ref != null){
				ref = ref.replace('"', ' ').trim();
				ref = ref.replace('&', ' ').trim();
			}
			String operator = tags.get("operator");
			String name = tags.get("name");
			if(name != null){
				name = name.replace('"', ' ').trim();
				name = name.replace('&', ' ').trim();
			}
			Id lineId = new IdImpl(operator + "-" + route + "-" + ref );
			
			for (RelationMember relationMember : relation.getMembers()) {
				if (relationMember.getMemberType().equals(EntityType.Way)) {
					if (allWaysTracker.get(relationMember.getMemberId())) {
						Way way = wayReader.get(relationMember.getMemberId()).getEntity();
						String role = relationMember.getMemberRole();
						netStore.addWay(way, lineId);
					} else {
						log.info("--- Missing way: " + relationMember.getMemberId() + "on line " + lineId.toString());
					}
				} 
			}
			
		}
		transitLineIterator.release();
		nodeReader.release();
		wayReader.release();
//		netStore.clean();
		this.line2Network = netStore.getLine2Network();
	}

	public Map<Id, NetworkImpl> getLine2Network(){
		return this.line2Network;
	}
	@Override
	public void release() {

	}

	public void setTransitModes(String[] transitModes) {
		if(transitModes == null){
			this.transitModes = new TreeSet<String>();
		} else {
			TreeSet<String> modes = new TreeSet<String>();
			for (String string : transitModes) {
				modes.add(string);
			}
			this.transitModes = modes;
		}
	}



	public TreeSet<String> getTransitModes() {
		return this.transitModes;
	}

}
