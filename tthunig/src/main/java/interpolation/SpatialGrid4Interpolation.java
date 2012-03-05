package interpolation;

import org.apache.log4j.Logger;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class SpatialGrid4Interpolation {
	
	// logger 
	private static final Logger logger = Logger.getLogger(SpatialGrid4Interpolation.class);

	private SpatialGrid<Double> spatialGrid = null;
	private double gridSizeInMeter = 1.;
	private double[] boundingBox = null;
	
	public SpatialGrid4Interpolation() {
		this.boundingBox = initBoundingBox();
		this.spatialGrid = new SpatialGrid<Double>(this.boundingBox, gridSizeInMeter);
		initTestSpatialGrid(this.spatialGrid);
	}
	
	private double[] initBoundingBox(){
		double[] box = new double[4];
		box[0] = 0; // xmin
		box[1] = 0; // ymin
		box[2] = 8; // xmax
		box[3] = 8; // ymax
		
		logger.info("Using bounding box with xmin=" + box[0] + ", ymin=" + box[1] + ", xmax=" + box[2] + ", ymax=" + box[3]);
		return box;
	}
	
	private void initTestSpatialGrid(final SpatialGrid<Double> spatialGrid){
		
		int rows = spatialGrid.getNumRows();
		int columns = spatialGrid.getNumCols(0);
		
		for(int row = 0; row < rows; row++){
			for(int col = 0; col < columns; col++){
				
				if(col == (columns / 2) && row == (rows / 2))
					spatialGrid.setValue(row, col, 100.);
				else
					spatialGrid.setValue(row, col, 1.);
			}
		}
	}
	
	public SpatialGrid<Double> getSpatialGrid(){
		return this.spatialGrid;
	}

	
	/**
	 * just for testing
	 * @param args
	 */
	public static void main(String args[]){
		
		SpatialGrid4Interpolation sg4i = new SpatialGrid4Interpolation();
		SpatialGrid<Double> sg = sg4i.getSpatialGrid();
		
		logger.info("The SpatialGrid looks like :");
			
		for(int row = 0; row < sg.getNumRows(); row++){
			for(int col = 0; col < sg.getNumCols(0); col++){
				System.out.print( sg.getValue(row, col) + " " );
			}
			System.out.println();
		}
		
		logger.info("These are the values for the correspondent coordinates ...");
		// coordinates
		GeometryFactory factory = new GeometryFactory();
		
		Point center = factory.createPoint(new Coordinate(4., 4.));
		Point corner = factory.createPoint(new Coordinate(0., 0.));
		
		logger.info("At coordinate x="+ center.getX() + " y="+ center.getY() + " the stored value is ="+ sg.getValue(center));
		logger.info("At coordinate x="+ corner.getX() + " y="+ corner.getY() + " the stored value is ="+ sg.getValue(corner));

		Object[][] test = sg.getMatrix();
		logger.info("...done");
	}
}
