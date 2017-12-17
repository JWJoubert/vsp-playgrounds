package signals.laemmer.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.lanes.data.Lane;

public class SignalPhase {
	// TODO ist doch sinnvoll, dass eine Signalphase ihre Groups kennt. Muss beim Erstellen der Phase gefuellt werden (Konstruktor)! tt, dez'17
	private List<SignalGroup> signalGroups = new LinkedList<>();
	private Map<Id<SignalGroup>, List<Id<Lane>>> greenSignalsToLanes = new HashMap<>();
	private Set<Id<Lane>> lanes = new HashSet<>();
	private Id<SignalPhase> id;
	
	public SignalPhase() {
		
	}
	
	public SignalPhase(Map<Id<SignalGroup>, LinkedList<Id<Lane>>> greenSignalesToLanes) {
		this.greenSignalsToLanes.putAll(greenSignalesToLanes);
		StringBuilder idStringBuilder = new StringBuilder();
		for (Id<SignalGroup> group : greenSignalesToLanes.keySet())
			idStringBuilder.append(group+"-");
		idStringBuilder.deleteCharAt(idStringBuilder.length()-1);
		this.id = Id.create(idStringBuilder.toString(), SignalPhase.class);
		//TODO prüfen ob das funktioniert:
		greenSignalesToLanes.values().forEach(lanes::addAll);
	}
	
	public void addGreenSignalGroupsAndLanes(Id<SignalGroup> signal, List<Id<Lane>> lanes) {
		greenSignalsToLanes.put(signal, lanes);
		this.lanes.addAll(lanes);
	}
	
	public Set<Id<SignalGroup>> getGreenSignalGroups(){
		return greenSignalsToLanes.keySet();
	}
	
	public Set<Id<Lane>> getGreenLanes(){
		return this.lanes;
	}
	
	public boolean equals(SignalPhase other) {
		if(this.getGreenSignalGroups().equals(other.getGreenSignalGroups())
				&& this.getGreenLanes().equals(other.getGreenLanes()))
			return true;
		else
			return false;
	}

	//  @deprecated not working yet. 
	public SignalGroupState getState() {
		boolean allGreen = true;
		for (SignalGroup sg : signalGroups)
			allGreen &= sg.getState().equals(SignalGroupState.GREEN);
		return (allGreen ? SignalGroupState.GREEN : SignalGroupState.RED);
	}
	// @deprecated should replaced by by getState(). beforehand create field for list of signalgroup-objects in constructor.
	public SignalGroupState getState(SignalSystem signalSystem) {
		boolean allGreen = true;
		for (SignalGroup sg : signalSystem.getSignalGroups().values()) {
			if (this.greenSignalsToLanes.containsKey(sg.getId()))
				allGreen &= sg.getState().equals(SignalGroupState.GREEN);
		}
		return (allGreen ? SignalGroupState.GREEN : SignalGroupState.RED);
	}
	
	
	public Id<SignalPhase> getId() {
		return this.id;
	}
	
	@Override
	public String toString() {
		StringBuilder string = new StringBuilder();
		for (Id<SignalGroup> sg : getGreenSignalGroups()) {
			string.append(sg.toString()+"; ");
		}
		return string.toString();
	}
}
