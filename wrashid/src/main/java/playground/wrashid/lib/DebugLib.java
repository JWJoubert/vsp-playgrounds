package playground.wrashid.lib;

import org.matsim.api.core.v01.Id;

public class DebugLib {

	public static void traceAgent(Id personId){
		if (personId.toString().equalsIgnoreCase("303")){
			emptyFunctionForSettingBreakPoint();
		}
	}

	public static void assertTrue(boolean val, String errorString){
		if (!val){
			stopSystemAndReportInconsistency(errorString);	
		}
	}
	
	public static void startDebuggingInIteration(int iterationNumber){
		if (iterationNumber==18){
			System.out.println();
		}
	}
	
	public static void stopSystemAndReportInconsistency(String errorString){
		throw new Error("system is in inconsistent state: " + errorString);
	}
	
	public static void stopSystemAndReportInconsistency(){
		throw new Error("system is in inconsistent state");
	}
	
	public static void stopSystemWithError(){
		stopSystemAndReportInconsistency();
	}

	public static void criticalTODO(){
		stopSystemAndReportInconsistency("critical TODO still missing here");
	}
	
	public static void continueHere(){
		
	}

	public static void emptyFunctionForSettingBreakPoint(){
		
	}
	
	public static void haltSystemToPrintCrutialHint(String hintString){
		throw new Error(hintString);
	}
}
