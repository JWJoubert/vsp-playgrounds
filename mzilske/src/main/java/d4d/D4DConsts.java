package d4d;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class D4DConsts {
	
	public static final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "PROJCS[\"WGS 84 / World Mercator\",\r\n    GEOGCS[\"WGS 84\",\r\n        DATUM[\"WGS_1984\",\r\n            SPHEROID[\"WGS 84\",6378137,298.257223563,\r\n                AUTHORITY[\"EPSG\",\"7030\"]],\r\n            AUTHORITY[\"EPSG\",\"6326\"]],\r\n        PRIMEM[\"Greenwich\",0,\r\n            AUTHORITY[\"EPSG\",\"8901\"]],\r\n        UNIT[\"degree\",0.01745329251994328,\r\n            AUTHORITY[\"EPSG\",\"9122\"]],\r\n        AUTHORITY[\"EPSG\",\"4326\"]],\r\n    UNIT[\"metre\",1,\r\n        AUTHORITY[\"EPSG\",\"9001\"]],\r\n    PROJECTION[\"Mercator_1SP\"],\r\n    PARAMETER[\"central_meridian\",0],\r\n    PARAMETER[\"scale_factor\",1],\r\n    PARAMETER[\"false_easting\",0],\r\n    PARAMETER[\"false_northing\",0],\r\n    AUTHORITY[\"EPSG\",\"3395\"],\r\n    AXIS[\"Easting\",EAST],\r\n    AXIS[\"Northing\",NORTH]]");
	static final String D4D_DIR = "/Users/michaelzilske/d4d/input/";
	static final String WORK_DIR = "/Users/michaelzilske/d4d/output/";


}
