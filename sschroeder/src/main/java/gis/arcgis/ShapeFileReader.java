package gis.arcgis;

import java.io.File;
import java.io.IOException;

import kid.filter.SimpleFeatureFilter;

import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class ShapeFileReader {

	
	private FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;


	public FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatureCollection() {
		 return featureCollection;
	 }

	 public void readFileAndInitialize(String linkFileName) {
		 FileDataStore store;
		try {
			store = FileDataStoreFinder.getDataStore(new File(linkFileName));
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = store.getFeatureSource();
			featureCollection = featureSource.getFeatures();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	 }
}