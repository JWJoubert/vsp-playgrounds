package playground.mzilske.freight;

import java.util.Collection;

/**
 * 
 * Ein CarrierPlan kann sinnvoll nur dem Carrier hinzugefügt werden, nach dessen CarrierCapabilities und mit dessen Shipments er erzeugt worden ist.
 * Trotzdem hat er keinen expliziten Verweis auf seinen Carrier. Naja. Mal sehen.
 * 
 * @author michaz
 *
 */
public class CarrierPlan {
	
	private Double score = null;
	
	private Collection<ScheduledTour> scheduledTours;

	public CarrierPlan(Collection<ScheduledTour> scheduledTours) {
		this.scheduledTours = scheduledTours;
	}

	Double getScore() {
		return score;
	}

	void setScore(Double score) {
		this.score = score;
	}

	public Collection<ScheduledTour> getScheduledTours() {
		return scheduledTours;
	}
	
}
