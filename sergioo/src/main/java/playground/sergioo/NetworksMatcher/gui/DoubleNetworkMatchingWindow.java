package playground.sergioo.NetworksMatcher.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import playground.sergioo.NetworksMatcher.gui.MatchingsPainter.MatchingOptions;
import playground.sergioo.NetworksMatcher.kernel.CrossingMatchingStep;
import playground.sergioo.NetworksMatcher.kernel.core.MatchingProcess;
import playground.sergioo.NetworksMatcher.kernel.core.NodesMatching;
import playground.sergioo.Visualizer2D.Camera;
import playground.sergioo.Visualizer2D.LayersWindow;

public class DoubleNetworkMatchingWindow extends LayersWindow implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Enumerations
	private enum PanelIds implements LayersWindow.PanelIds {
		A,
		B,
		ACTIVE,
		DOUBLE;
	}
	public enum Options implements LayersWindow.Options {
		SELECT_LINK("<html>L<br/>I<br/>N<br/>K</html>"),
		SELECT_NODE("<html>N<br/>O<br/>D<br/>E</html>"),
		SELECT_NODES("<html>N<br/>O<br/>D<br/>E<br/>S</html>"),
		ZOOM("<html>Z<br/>O<br/>O<br/>M</html>");
		private String caption;
		private Options(String caption) {
			this.caption = caption;
		}
		@Override
		public String getCaption() {
			return caption;
		}
	}
	public enum Tool {
		MATCH("Match",0,0,2,1,"match"),
		DELETE_MATCH("Delete match",0,1,2,1,"deleteMatch"),
		SELECT_MATCH("Select match",2,0,2,1,"selectMatch"),
		MODIFY_MATCH("Modify match",2,1,2,1,"modifyMatch"),
		VERIFY_MATCHINGS("Verify matchings",4,0,2,1,"verifyMatchings"),
		SAVE("Save",4,1,2,1,"save");
		String caption;
		int gx;int gy;
		int sx;int sy;
		String function;
		private Tool(String caption, int gx, int gy, int sx, int sy, String function) {
			this.caption = caption;
			this.gx = gx;
			this.gy = gy;
			this.sx = sx;
			this.sy = sy;
			this.function = function;
		}
	}
	public enum Labels implements LayersWindow.Labels {
		LINK("Link"),
		NODE("Node"),
		NODES("Nodes"),
		ACTIVE("Active");
		private String text;
		private Labels(String text) {
			this.text = text;
		}
		@Override
		public String getText() {
			return text;
		}
	}
	
	//Attributes
	private JButton readyButton;
	private boolean networksSeparated = true;
	private JPanel panelsPanel;
	private MatchingProcess matchingProcess;
	private int step;
	private List<Color> colors;
	private Set<NodesMatching> nodesMatchings;
	private NodesMatching selectedNodesMatching;
	
	//Methods
	private DoubleNetworkMatchingWindow(String title) {
		setTitle(title);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		this.setLocation(0,0);
		this.setLayout(new BorderLayout());
		option = Options.ZOOM;
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new GridLayout(Options.values().length,1));
		for(Options option:Options.values()) {
			JButton optionButton = new JButton(option.caption);
			optionButton.setActionCommand(option.getCaption());
			optionButton.addActionListener(this);
			buttonsPanel.add(optionButton);
		}
		this.add(buttonsPanel, BorderLayout.EAST);
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BorderLayout());
		readyButton = new JButton("Ready to exit");
		readyButton.addActionListener(this);
		readyButton.setActionCommand(READY_TO_EXIT);
		infoPanel.add(readyButton, BorderLayout.WEST);
		JPanel labelsPanel = new JPanel();
		labelsPanel.setLayout(new FlowLayout());
		labelsPanel.setBorder(new TitledBorder("Information"));
		labels = new JTextField[Labels.values().length];
		for(int i=0; i<Labels.values().length; i++) {
			labels[i]=new JTextField("");
			labels[i].setEditable(false);
			labels[i].setBackground(null);
			labelsPanel.add(labels[i]);
		}
		infoPanel.add(labelsPanel, BorderLayout.CENTER);
		JPanel coordsPanel = new JPanel();
		coordsPanel.setLayout(new GridLayout(1,2));
		coordsPanel.setBorder(new TitledBorder("Coordinates"));
		coordsPanel.add(lblCoords[0]);
		coordsPanel.add(lblCoords[1]);
		infoPanel.add(coordsPanel, BorderLayout.EAST);
		this.add(infoPanel, BorderLayout.SOUTH);
		setSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
	}
	public DoubleNetworkMatchingWindow(String title, NetworkNodesPainter networkPainterA, NetworkNodesPainter networkPainterB) {
		this(title);
		layersPanels.put(PanelIds.A, new NetworkNodesPanel(this, networkPainterA));
		layersPanels.put(PanelIds.B, new NetworkNodesPanel(this, networkPainterB));
		layersPanels.get(PanelIds.A).setBorder(new LineBorder(Color.BLACK, 5));
		layersPanels.get(PanelIds.B).setBorder(new LineBorder(Color.BLACK, 5));
		layersPanels.put(PanelIds.ACTIVE, layersPanels.get(PanelIds.A));
		layersPanels.get(PanelIds.ACTIVE).requestFocus();
		layersPanels.put(PanelIds.DOUBLE, new DoubleNetworkMatchingPanel(this, networkPainterA, networkPainterB));
		panelsPanel = new JPanel();
		panelsPanel.setLayout(new GridLayout());
		panelsPanel.add(layersPanels.get(PanelIds.A));
		panelsPanel.add(layersPanels.get(PanelIds.B));
		this.add(panelsPanel, BorderLayout.CENTER);
	}
	public DoubleNetworkMatchingWindow(String title, MatchingProcess matchingProcess) {
		this(title);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.matchingProcess = matchingProcess;
		this.step = matchingProcess.getNumSteps();
		nodesMatchings = matchingProcess.getFinalMatchings();
		colors = MatchingsPainter.generateRandomColors(nodesMatchings.size());
		layersPanels.put(PanelIds.A, new NetworkNodesPanel(nodesMatchings, MatchingOptions.A, this, new NetworkNodesPainter(matchingProcess.getFinalNetworkA()), colors));
		layersPanels.put(PanelIds.B, new NetworkNodesPanel(nodesMatchings, MatchingOptions.B, this, new NetworkNodesPainter(matchingProcess.getFinalNetworkB(), Color.BLACK, Color.CYAN), colors));
		layersPanels.get(PanelIds.A).setBorder(new LineBorder(Color.BLACK, 5));
		layersPanels.get(PanelIds.B).setBorder(new LineBorder(Color.BLACK, 5));
		layersPanels.put(PanelIds.ACTIVE, layersPanels.get(PanelIds.A));
		layersPanels.get(PanelIds.ACTIVE).requestFocus();
		layersPanels.put(PanelIds.DOUBLE, new DoubleNetworkMatchingPanel(nodesMatchings, this, new NetworkNodesPainter(matchingProcess.getFinalNetworkA()), new NetworkNodesPainter(matchingProcess.getFinalNetworkB(), Color.BLACK, Color.CYAN)));
		panelsPanel = new JPanel();
		panelsPanel.setLayout(new GridLayout());
		panelsPanel.add(layersPanels.get(PanelIds.A));
		panelsPanel.add(layersPanels.get(PanelIds.B));
		this.add(panelsPanel, BorderLayout.CENTER);
	}
	public DoubleNetworkMatchingWindow(String title, Network networkA, Network networkB, Set<NodesMatching> nodesMatchings) {
		this(title);
		option = Options.SELECT_NODES;
		JPanel toolsPanel = new JPanel();
		toolsPanel.setLayout(new GridBagLayout());
		for(Tool tool:Tool.values()) {
			JButton toolButton = new JButton(tool.caption);
			toolButton.setActionCommand(tool.name());
			toolButton.addActionListener(this);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = tool.gx;
			gbc.gridy = tool.gy;
			gbc.gridwidth = tool.sx;
			gbc.gridheight = tool.sy;
			toolsPanel.add(toolButton,gbc);
		}
		this.add(toolsPanel, BorderLayout.NORTH);
		this.nodesMatchings = nodesMatchings;
		colors = MatchingsPainter.generateRandomColors(nodesMatchings.size());
		layersPanels.put(PanelIds.A, new NetworkNodesPanel(nodesMatchings, MatchingOptions.A, this, new NetworkNodesPainter(networkA), colors));
		layersPanels.put(PanelIds.B, new NetworkNodesPanel(nodesMatchings, MatchingOptions.B, this, new NetworkNodesPainter(networkB, Color.BLACK, Color.CYAN), colors));
		layersPanels.get(PanelIds.A).setBorder(new LineBorder(Color.BLACK, 5));
		layersPanels.get(PanelIds.B).setBorder(new LineBorder(Color.BLACK, 5));
		layersPanels.put(PanelIds.ACTIVE, layersPanels.get(PanelIds.A));
		layersPanels.get(PanelIds.ACTIVE).requestFocus();
		layersPanels.put(PanelIds.DOUBLE, new DoubleNetworkMatchingPanel(nodesMatchings, this, new NetworkNodesPainter(networkA), new NetworkNodesPainter(networkB, Color.BLACK, Color.CYAN)));
		panelsPanel = new JPanel();
		panelsPanel.setLayout(new GridLayout());
		panelsPanel.add(layersPanels.get(PanelIds.A));
		panelsPanel.add(layersPanels.get(PanelIds.B));
		this.add(panelsPanel, BorderLayout.CENTER);
	}
	private void setStep(boolean finalStep) {
		if(matchingProcess!=null) {
			Collection<NodesMatching> nodesMatchings = null;
			Network networkA = null;
			Network networkB = null;
			if(finalStep) {
				nodesMatchings = matchingProcess.getFinalMatchings();
				networkA = matchingProcess.getFinalNetworkA();
				networkB = matchingProcess.getFinalNetworkB();
			}
			else {
				nodesMatchings = matchingProcess.getNodesMatchings(step);
				networkA = matchingProcess.getNetworkA(step);
				networkB = matchingProcess.getNetworkB(step);
			}
			if(nodesMatchings!= null && !(colors.size()==nodesMatchings.size()))
				colors = MatchingsPainter.generateRandomColors(nodesMatchings.size());
			((NetworkNodesPanel)layersPanels.get(PanelIds.A)).setMatchings(nodesMatchings, MatchingOptions.A, colors);
			((NetworkNodesPanel)layersPanels.get(PanelIds.B)).setMatchings(nodesMatchings, MatchingOptions.B, colors);
			((DoubleNetworkMatchingPanel)layersPanels.get(PanelIds.DOUBLE)).setMatchings(nodesMatchings);
			((NetworkNodesPanel)layersPanels.get(PanelIds.A)).setNetwork(networkA);
			((NetworkNodesPanel)layersPanels.get(PanelIds.B)).setNetwork(networkB);
			((DoubleNetworkMatchingPanel)layersPanels.get(PanelIds.DOUBLE)).setNetworks(networkA, networkB);
			setVisible(true);
			repaint();
		}
	}
	public void setNetworksSeparated() {
		networksSeparated = !networksSeparated;
		if(networksSeparated) {
			layersPanels.put(PanelIds.ACTIVE, layersPanels.get(PanelIds.A));
			this.remove(layersPanels.get(PanelIds.DOUBLE));
			this.add(panelsPanel);
		}
		else {
			this.remove(panelsPanel);
			this.add(layersPanels.get(PanelIds.DOUBLE), BorderLayout.CENTER);
		}
		setVisible(true);
		repaint();
		if(networksSeparated) {
			layersPanels.get(PanelIds.ACTIVE).requestFocus();
			layersPanels.get(PanelIds.A).getCamera().copyCenter(layersPanels.get(PanelIds.DOUBLE).getCamera());
			layersPanels.get(PanelIds.B).getCamera().copyCenter(layersPanels.get(PanelIds.DOUBLE).getCamera());
		}
		else {
			layersPanels.get(PanelIds.DOUBLE).requestFocus();
			layersPanels.get(PanelIds.DOUBLE).getCamera().copyCenter(layersPanels.get(PanelIds.A).getCamera());
		}
		
	}
	public void nextNetwork() {
		if(matchingProcess!=null) {
			step++;
			if(step>=matchingProcess.getNumSteps())
				step = 0;
			setStep(false);
		}
	}
	public void previousNetwork() {
		if(matchingProcess!=null) {
			step--;
			if(step<0)
				step = matchingProcess.getNumSteps()-1;
			setStep(false);
		}
	}
	public void finalNetworks() {
		if(matchingProcess!=null) {
			step = matchingProcess.getNumSteps();
			setStep(true);
		}
	}
	public void cameraChange(Camera camera) {
		if(networksSeparated) {
			if(layersPanels.get(PanelIds.ACTIVE)==layersPanels.get(PanelIds.A)) {
				layersPanels.get(PanelIds.B).getCamera().copyCamera(camera);
				layersPanels.get(PanelIds.B).repaint();
			}
			else {
				layersPanels.get(PanelIds.A).getCamera().copyCamera(camera);
				layersPanels.get(PanelIds.A).repaint();
			}
		}
	}
	public void setActivePanel(NetworkNodesPanel panel) {
		layersPanels.put(PanelIds.ACTIVE, panel);
	}
	public NodesMatching getSelectedNodesMatching() {
		return selectedNodesMatching;
	}
	public void match() {
		if(matchingProcess == null) {
			nodesMatchings.add(new NodesMatching(((NetworkNodesPanel)layersPanels.get(PanelIds.A)).getSelectedNodes(),((NetworkNodesPanel)layersPanels.get(PanelIds.B)).getSelectedNodes()));
			selectedNodesMatching = null;
			float r = 0,g = 0,b = 0;
			while(r+b+g<1) {
				r = (float) Math.random();
				b = (float) Math.random();
				g = (float) Math.random();
			}
			colors.add(new Color(r, g, b));
		}
	}
	public void deleteMatch() {
		if(matchingProcess == null) {
			selectMatch();
			if(selectedNodesMatching!=null)
				nodesMatchings.remove(selectedNodesMatching);
			selectedNodesMatching = null;
		}
	}
	public void selectMatch() {
		if(matchingProcess == null) {
			selectedNodesMatching = null;
			Iterator<Node> nodeAI = ((NetworkNodesPanel)layersPanels.get(PanelIds.A)).getSelectedNodes().iterator();
			Iterator<Node> nodeBI = ((NetworkNodesPanel)layersPanels.get(PanelIds.B)).getSelectedNodes().iterator();
			Node nodeA = nodeAI.hasNext()?nodeAI.next():null;
			Node nodeB = nodeBI.hasNext()?nodeBI.next():null;
			for(NodesMatching nodesMatching:nodesMatchings)
				if((nodeA!=null && nodesMatching.getComposedNodeA().getNodes().contains(nodeA)) || (nodeB!=null && nodesMatching.getComposedNodeB().getNodes().contains(nodeB)))
					selectedNodesMatching = nodesMatching;
			if(selectedNodesMatching!=null) {
				((NetworkNodesPanel)layersPanels.get(PanelIds.A)).selectNodes(selectedNodesMatching.getComposedNodeA().getNodes());
				((NetworkNodesPanel)layersPanels.get(PanelIds.B)).selectNodes(selectedNodesMatching.getComposedNodeB().getNodes());
			}
		}
	}
	public void modifyMatch() {
		if(matchingProcess == null) {
			if(selectedNodesMatching!=null) {
				nodesMatchings.remove(selectedNodesMatching);
				match();
			}
		}
	}
	public void verifyMatchings() {
		if(matchingProcess == null) {
			Set<Link> wrongLinks = new HashSet<Link>();
			for(Link link:((NetworkNodesPanel)layersPanels.get(PanelIds.A)).getLinks())
				if(!(isInNetworkAMatchings(link.getFromNode()) && isInNetworkAMatchings(link.getToNode()))) {
					wrongLinks.add(link);
				}
			if(wrongLinks.size()==0)
				JOptionPane.showMessageDialog(this, "Yes!!!");
			else {
				JOptionPane.showMessageDialog(this, "No, "+wrongLinks.size()+" links");
				((NetworkNodesPanel)layersPanels.get(PanelIds.A)).setLinksLayer(wrongLinks);
				double x = wrongLinks.iterator().next().getCoord().getX(), y = wrongLinks.iterator().next().getCoord().getY();
				((NetworkNodesPanel)layersPanels.get(PanelIds.A)).centerCamera(x, y);
				((NetworkNodesPanel)layersPanels.get(PanelIds.B)).centerCamera(x, y);
			}
		}
	}
	public void save() {
		try {
			PrintWriter writer = new PrintWriter(CrossingMatchingStep.MATCHINGS_FILE);
			for(NodesMatching nodesMatching:nodesMatchings)
				writer.println(nodesMatching.toString());
			writer.close();
			JOptionPane.showMessageDialog(this, "Saved");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	private boolean isInNetworkAMatchings(Node networkNode) {
		for(NodesMatching nodesMatching:nodesMatchings)
			for(Node node:nodesMatching.getComposedNodeA().getNodes())
				if(node.equals(networkNode))
					return true;
		return false;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		for(Options option:Options.values())
			if(e.getActionCommand().equals(option.getCaption()))
				this.option = option;
		for(Tool tool:Tool.values())
			if(e.getActionCommand().equals(tool.name())) {
				try {
					Method m = DoubleNetworkMatchingWindow.class.getMethod(tool.function, new Class[] {});
					m.invoke(this, new Object[]{});
				} catch (SecurityException e1) {
					e1.printStackTrace();
				} catch (NoSuchMethodException e1) {
					e1.printStackTrace();
				} catch (IllegalArgumentException e1) {
					e1.printStackTrace();
				} catch (IllegalAccessException e1) {
					e1.printStackTrace();
				} catch (InvocationTargetException e1) {
					e1.printStackTrace();
				}
				setVisible(true);
				repaint();
			}
		if(e.getActionCommand().equals(READY_TO_EXIT)) {
			setVisible(false);
			readyToExit = true;
		}
	}
	@Override
	public void refreshLabel(playground.sergioo.Visualizer2D.LayersWindow.Labels label) {
		if(label.equals(Labels.ACTIVE))
			labels[label.ordinal()].setText(layersPanels.get(PanelIds.ACTIVE)==layersPanels.get(PanelIds.A)?"A":"B");
		else
			labels[label.ordinal()].setText(((NetworkNodesPanel)layersPanels.get(PanelIds.ACTIVE)).getLabelText(label));
		repaint();
	}
	
}
