package playground.gregor.sim2d.otfdebug.readerwriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;

public class Agent2DWriter extends OTFDataWriter<List> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3182614793084430910L;
	public transient Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();

	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
		// TODO Auto-generated method stub
		
	}

	
	public void writeAgent(AgentSnapshotInfo pos, ByteBuffer out) {
		String id = pos.getId().toString();
		ByteBufferUtils.putString(out, id);
		out.putFloat((float)(pos.getEasting() - OTFServerQuad2.offsetEast));
		out.putFloat((float)(pos.getNorthing()- OTFServerQuad2.offsetNorth));
		out.putFloat((float)pos.getAzimuth());
	}
	
	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		// Write additional agent data
		if (this.src instanceof ArrayList) {
			this.positions = (Collection<AgentSnapshotInfo>) this.src;
		}
		
		out.putInt(this.positions.size());

		for (AgentSnapshotInfo pos : this.positions) {
			writeAgent(pos, out);
		}
		this.positions.clear();
		
	}

}
