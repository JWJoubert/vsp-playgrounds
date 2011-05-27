package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC.TimeDataCollector;
import junit.framework.TestCase;

public class TimeDataCollectorTest extends TestCase{

	public TimeDataCollectorTest() {		
	}
	
	public void testExtrapolateValueAtTime( ){
		TimeDataCollector myCollector =setUpTimeDataCollector();
		
		double ex1= myCollector.extrapolateValueAtTime(0.5*60.0);
		assertEquals(7.5, ex1);
		
		double ex2= myCollector.extrapolateValueAtTime(1.2*60.0);
		assertEquals(12.0, ex2);
		
		
	}
	
	
	public void testIncreaseDatPoint(){
		TimeDataCollector myCollector= setUpTimeDataCollector();
		
		myCollector.increaseYEntryAtEntryByDouble(0, 10.0);
		assertEquals(15.0, myCollector.getYAtEntry(0));
		
		myCollector.increaseYEntryAtEntryByDouble(1, -20.0);
		assertEquals(-10.0, myCollector.getYAtEntry(1));
		
		
	}
	
	public TimeDataCollector setUpTimeDataCollector(){
		TimeDataCollector myCollector = new TimeDataCollector(3);
		
		myCollector.addDataPoint(0, 0*60.0, 5);
		myCollector.addDataPoint(1, 1*60.0, 10);
		myCollector.addDataPoint(2, 2*60.0, 20);
		return myCollector;
	}
	
	
}
