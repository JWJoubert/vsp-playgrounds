package gis.arcgis;

import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransformFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import facilities.Facility;

public class FacilityShapeFileWriter {
	
	private static Logger logger = Logger.getLogger(FacilityShapeFileWriter.class);
	
	private FeatureCollection<SimpleFeatureType,SimpleFeature> facilityFeatures = FeatureCollections.newCollection();
	
	private Network network;
	
	private Collection<? extends Facility> facilities;
	
	private CoordinateReferenceSystem referenceSystem;

	public FacilityShapeFileWriter(Collection<? extends Facility> facilities, Network network) {
		super();
		setDefaultReferenceSystem();
		this.facilities = facilities;
		this.network = network;
	}
	
	public FacilityShapeFileWriter(Collection<? extends Facility> facilities) {
		super();
		setDefaultReferenceSystem();
		this.facilities = facilities;
	}
	
	
	private void setDefaultReferenceSystem() {
		try {
			referenceSystem = CRS.decode("EPSG:32633");
		} catch (NoSuchAuthorityCodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		
	}
	
	public void setReferenceSystem(CoordinateReferenceSystem referenceSystem) {
		this.referenceSystem = referenceSystem;
	}

	public void write(String filename) {
		createFeatures();
		try {
			writeFeatures(filename);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e);
			System.exit(1);
		}
	}

	private void writeFeatures(String filename) throws IOException {
		ShapeFileWriter shpWriter = new ShapeFileWriter(facilityFeatures);
		shpWriter.writeFeatures(filename);
	}

	private void createFeatures() {
		SimpleFeatureType featureType = createFeatureType();
		for(Facility f : facilities){
			SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
			
			Coordinate facilityCoord = null;
			if(network == null){
				facilityCoord = f.getCoordinate();
				if(facilityCoord == null){
					throw new IllegalStateException("network is null and facilityCoord is null. this should not happen.");
				}
			}
			else{
				if(f.getLocationId() == null){
					throw new IllegalStateException("locationId/linkId must be set.");
				}
				facilityCoord = makeCoordinate(network.getLinks().get(f.getLocationId()).getCoord());
			}
			Point point = new GeometryFactory().createPoint(facilityCoord);
			featureBuilder.add(point);
			logger.debug("point=" + point.getCoordinate());
//			logger.debug("transformedPoint=" + new DefaultMathTransformFactory().createAffineTransform(matrix));
			featureBuilder.add(f.getId());
			featureBuilder.add(f.getLocationId());
			featureBuilder.add(f.getType());
			facilityFeatures.add(featureBuilder.buildFeature(null));
		}	
	}

	private Coordinate makeCoordinate(Coord faciltiyCoord) {
		return new Coordinate(faciltiyCoord.getX(),faciltiyCoord.getY());
	}

	private SimpleFeatureType createFeatureType() {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Facility");
        builder.setCRS(referenceSystem);
        builder.add("Facility", Point.class);
        builder.add("FacilityId", String.class); 
        builder.add("LocationId", String.class);
        builder.add("Type", String.class);
        logger.info(referenceSystem.toString());
        // build the type
        final SimpleFeatureType featureType = builder.buildFeatureType();

        return featureType;
	}
}
