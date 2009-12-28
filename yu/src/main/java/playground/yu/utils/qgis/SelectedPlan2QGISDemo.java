package playground.yu.utils.qgis;

import java.io.IOException;

import org.geotools.referencing.CRS;
import org.jfree.util.Log;

import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * This class is a copy of main() from
 * org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape and can
 * convert a MATSim-population to a QGIS .shp-file (acts or legs)
 * 
 * @author ychen
 * 
 */
public class SelectedPlan2QGISDemo implements X2QGIS {
	public static void main(final String[] args) throws FactoryException {
		// final String populationFilename = "./examples/equil/plans100.xml";
		// final String populationFilename =
		// "../runs/run628/it.500/500.plans.xml.gz";
		// final String populationFilename = "output/bvg/245.xml.gz";
		final String populationFilename = "input/bse/760.plans.xml.gz";
		// final String networkFilename = "./examples/equil/network.xml";
		// final String networkFilename =
		// "test/scenarios/berlin/network.xml.gz";
		final String networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		// final String networkFilename =
		// "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		// final String outputDir = "../runs/run628/it.500/";
		// final String outputDir = "output/bvg";
		final String outputDir = "output/bse";

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFilename);

		PopulationImpl population = new PopulationImpl();
		new MatsimPopulationReader(population, network)
				.readFile(populationFilename);
		/*
		 * ----------------------------------------------------------------------
		 */
		CoordinateReferenceSystem crs = CRS.parseWKT(ch1903);
		SelectedPlans2ESRIShape sp = new SelectedPlans2ESRIShape(population,
				crs, outputDir);
		sp.setOutputSample(
		// 0.05
				0.2);
		sp.setActBlurFactor(100);
		sp.setLegBlurFactor(100);
		sp.setWriteActs(true);
		sp.setWriteLegs(true);

		try {
			sp.write();
		} catch (IOException e) {
			Log.error(e.getMessage(), e);
		}
	}
}
