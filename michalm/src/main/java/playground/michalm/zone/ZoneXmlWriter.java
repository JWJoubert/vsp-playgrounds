package playground.michalm.zone;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;


public class ZoneXmlWriter
    extends MatsimXmlWriter
{
    private Map<Id, Zone> zones;


    public ZoneXmlWriter(Map<Id, Zone> zones)
    {
        this.zones = zones;
    }


    public void write(String fileName)
    {
        openFile(fileName);
        writeDoctype("zones", "http://matsim.org/files/dtd/zones_v1.dtd");
        writeStartTag("zones", Collections.<Tuple<String, String>>emptyList());
        writeZones();
        writeEndTag("zones");
        close();
    }


    private void writeZones()
    {
        for (Zone z : zones.values()) {
            List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();
            atts.add(new Tuple<String, String>("id", z.getId().toString()));
            atts.add(new Tuple<String, String>("type", z.getType()));
            writeStartTag("zone", atts, true);
        }
    }
}
