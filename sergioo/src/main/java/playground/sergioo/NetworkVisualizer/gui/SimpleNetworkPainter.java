package playground.sergioo.NetworkVisualizer.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;

public class SimpleNetworkPainter implements NetworkPainter {
	
	//Attributes
	private Color networkColor = Color.LIGHT_GRAY;
	private Stroke networkStroke = new BasicStroke(0.5f);
	private NetworkByCamera networkByCamera;
	
	//Methods
	public SimpleNetworkPainter(NetworkByCamera networkByCamera) {
		this.networkByCamera =  networkByCamera;
	}
	public SimpleNetworkPainter(NetworkByCamera networkByCamera, Color networkColor, Stroke networkStroke) {
		this.networkByCamera =  networkByCamera;
		this.networkColor = networkColor;
		this.networkStroke = networkStroke;
	}
	@Override
	public NetworkByCamera getNetworkByCamera() {
		return networkByCamera;
	}
	@Override
	public void paintNetwork(Graphics2D g2, Camera camera) throws Exception {
		networkByCamera.setCamera(camera);
		for(Link link:networkByCamera.getNetworkLinks())
			paintLink(g2,link, networkStroke, 0.5, networkColor);
	}
	private void paintLink(Graphics2D g2, Link link, Stroke stroke, double pointSize, Color color) throws Exception {
		paintLine(g2, new Tuple<Coord, Coord>(link.getFromNode().getCoord(), link.getToNode().getCoord()), stroke, color);
		paintCircle(g2,link.getToNode().getCoord(), pointSize, color);
	}
	private void paintLine(Graphics2D g2, Tuple<Coord,Coord> coords, Stroke stroke, Color color) throws Exception {
		g2.setStroke(stroke);
		g2.setColor(color);
		g2.drawLine(networkByCamera.getIntX(coords.getFirst().getX()),
				networkByCamera.getIntY(coords.getFirst().getY()),
				networkByCamera.getIntX(coords.getSecond().getX()),
				networkByCamera.getIntY(coords.getSecond().getY()));
	}
	private void paintCircle(Graphics2D g2, Coord coord, double pointSize, Color color) throws Exception {
		Shape circle = new Ellipse2D.Double(networkByCamera.getIntX(coord.getX())-pointSize,networkByCamera.getIntY(coord.getY())-pointSize,pointSize*2,pointSize*2);
		g2.fill(circle);
	}
	
}
