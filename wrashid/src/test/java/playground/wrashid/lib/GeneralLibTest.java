package playground.wrashid.lib;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.testcases.MatsimTestCase;

public class GeneralLibTest extends MatsimTestCase {


	public void testReadWritePopulation(){
		Scenario scenario = GeneralLib.readPopulation("test/scenarios/equil/plans2.xml","test/scenarios/equil/network.xml" );

		GeneralLib.writePopulation(scenario.getPopulation(), scenario.getNetwork(), getOutputDirectory() + "plans2.xml");

		scenario = GeneralLib.readPopulation(getOutputDirectory() + "plans2.xml","test/scenarios/equil/network.xml" );

		assertEquals(2, scenario.getPopulation().getPersons().size());
	}

	public void testReadWriteFacilities(){
		ActivityFacilitiesImpl facilities=GeneralLib.readActivityFacilities("test/scenarios/equil/facilities.xml");

		GeneralLib.writeActivityFacilities(facilities, getOutputDirectory() + "facilities.xml");

		facilities=GeneralLib.readActivityFacilities(getOutputDirectory() + "facilities.xml");

		assertEquals(23, facilities.getFacilities().size());

	}

	public void testReadWriteMatrix(){
		double[][] hubPriceInfoOriginal=GeneralLib.readMatrix(96, 4, false, "test/input/playground/wrashid/PSF/data/hubPriceInfo.txt");

		GeneralLib.writeMatrix(hubPriceInfoOriginal, getOutputDirectory() + "hubPriceInfo.txt", null);

		double[][] hubPriceInfoRead=GeneralLib.readMatrix(96, 4, false, getOutputDirectory() + "hubPriceInfo.txt");

		assertTrue(isEqual(hubPriceInfoOriginal, hubPriceInfoRead));
	}

	private boolean isEqual(double[][] matrixA, double[][] matrixB){
		for (int i=0;i<matrixA.length;i++){
			for (int j=0;j<matrixA[0].length;j++){
				if (matrixA[i][j]!=matrixB[i][j]){
					return false;
				}
			}
		}

		return true;
	}

	public void testScaleMatrix(){
		double[][] matrix=new double[1][1];
		matrix[0][0]=1.0;
		matrix=GeneralLib.scaleMatrix(matrix, 2);

		assertEquals(2.0, matrix[0][0]);
	}
	
	
	public void testProjectTimeWithin24Hours(){
		assertEquals(10.0,GeneralLib.projectTimeWithin24Hours(10.0));
		assertEquals(0.0,GeneralLib.projectTimeWithin24Hours(60*60*24.0));
		assertEquals(1.0,GeneralLib.projectTimeWithin24Hours(60*60*24.0+1),0.1);
	}
	
	
	public void testIsIn24HourInterval(){
		assertEquals(true,GeneralLib.isIn24HourInterval(0.0, 10.0, 9.0));
		assertEquals(false,GeneralLib.isIn24HourInterval(0.0, 10.0, 11.0));
		assertEquals(true,GeneralLib.isIn24HourInterval(0.0, 10.0, 0.0));
		assertEquals(true,GeneralLib.isIn24HourInterval(0.0, 10.0, 10.0));
		
		assertEquals(false,GeneralLib.isIn24HourInterval(10.0, 3.0, 9.0));
		assertEquals(true,GeneralLib.isIn24HourInterval(10.0, 3.0, 11.0));
		assertEquals(true,GeneralLib.isIn24HourInterval(10.0, 3.0, 2.0));
	}
}