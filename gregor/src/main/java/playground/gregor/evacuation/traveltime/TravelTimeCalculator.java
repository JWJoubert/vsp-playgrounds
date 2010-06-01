package playground.gregor.evacuation.traveltime;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.xml.sax.SAXException;

public class TravelTimeCalculator implements AgentArrivalEventHandler {
	
	int count = 0;
	double time = 0;
	
	public static void main(String [] args) {
		String events = "/home/laemmel/devel/allocation/output/ITERS/it.150/150.events.xml.gz";
		TravelTimeCalculator tt = new TravelTimeCalculator();
		
		EventsManagerImpl ev = new EventsManagerImpl();
		ev.addHandler(tt);
		try {
			new EventsReaderXMLv1(ev).parse(events);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(tt.time/tt.count);
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.count++;
		this.time += event.getTime()-3*3600;
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

}
