package chat;

import hypeerweb.Node;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Draws a directed graph of a HyPeerWeb node
 * TODO:
 *	- hide should remove selection
 *	- spanning tree fanning
 *  - default graph evenly distribute non-children
 *  - graph-mode options toolbar
 * @author isaac
 */
public class GraphTab extends JPanel{
	private static ChatClient container;
	private Node activeNode;	//the selected node; may not be in graph
	//Drawing stuff
	private static final int maxSizeX = 550, maxSizeY = 600;
	private boolean DIRTY_BUFFER = false;	//do we need to redraw?
	private boolean useBinary = false;	
	//Graphing stuff
	private final JComboBox<GraphMode> modes = new JComboBox(GraphMode.values());
	private int previousMode = 0;
	private static Graph graph;
	
	public GraphTab(ChatClient container){
		GraphTab.container = container;
		setLayout(new BorderLayout(0, 0));
		
		//Graphing tab is split into a toolbar and graph
		JToolBar bar = new JToolBar(JToolBar.HORIZONTAL);
		bar.setFloatable(false);
		
		/**
		 * Stuff needed:
		 * Hide, Unhide All, Help, Viewing Mode, Go up a level
		 */
		bar.add(new JLabel("Mode: "));
		//Redraw when the mode changes
		modes.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				int newMode = modes.getSelectedIndex();
				if (previousMode != newMode){
					previousMode = newMode;
					graph.draw();
				}
			}
		});
		bar.add(modes);
		final JCheckBox chckBinary = new JCheckBox("Binary", false);
		chckBinary.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				useBinary = chckBinary.isSelected();
				graph.redraw(true);
			}
		});
		bar.add(chckBinary);
		bar.add(Box.createHorizontalGlue());
		JButton hideBtn = new JButton("Hide");
		hideBtn.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if (graph.selected != null){
					graph.active = graph.selected;
					graph.hideNode();
				}
			}
		});
		hideBtn.setToolTipText("Right click");
		bar.add(hideBtn);
		JButton unhideBtn = new JButton("Unhide All");
		unhideBtn.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.unhideNodes();
			}
		});
		unhideBtn.setToolTipText("Middle click");
		bar.add(unhideBtn);
		
		//Bottom is the actual graph
		graph = new Graph();
		
		//Layout components
		JPanel toolbars = new JPanel();
		toolbars.setLayout(new BorderLayout(0, 0));		
		toolbars.add(bar, BorderLayout.NORTH);
		toolbars.add(((GraphMode) modes.getSelectedItem()).getToolbar(), BorderLayout.CENTER);
		add(toolbars, BorderLayout.NORTH);
		add(graph, BorderLayout.CENTER);
	}
	
	public void draw(){
		graph.draw();
	}
	public void select(Node n){
		/*
		//This node is already selected
		if (graph.selected.getKey() == n)
			return;
		//Can show selection
		if (GraphMode.nodes.containsKey(n)){
			graph.selected = GraphMode.nodes
			graph.redraw(false);
		}
		//Can't show selection
		else if (graph.selected != null){
			graph.selected = null;
			graph.redraw(false);
		}
		*/
	}
	
	private enum GraphMode{
		DEFAULT ("Default", RotateMode.TRANSLATE){
			//Minimum margin between nodes (within allowed window space)
			private static final int margin = 3;
			private JSpinner select;
			private JSpinner levelSelect;
			
			@Override
			public void draw(){				
				nodes = new HashMap();
				links = new TreeSet();
				helpers = new ArrayList();
				
				Node n = container.nodeList.list.get((int) select.getValue());
				if (n == null)
					return;
				
				//Start off with the active node
				int levels = (int) levelSelect.getValue();
				int radius = (int) (maxSizeX/2-levels*margin)/2;
				Point2D origin = new Point2D.Double(maxSizeX/2, maxSizeY/2);
				DrawData active = new DrawData(n, null, null, -1, 0, origin);
				helpers.add(active);
				nodes.put(n, active);
				
				//Each level evenly distributes the nodes to the parents
				HashSet<DrawLink> linksPot = new HashSet();
				ArrayList<DrawData> parents = new ArrayList();
				parents.add(active);
				for (int level = 0, adjRadius; level < levels; level++, radius /= 2){
					//Generate a hashset of all parents for quick lookup
					HashSet<Integer> allParentIDs = new HashSet();
					for (DrawData p: parents)
						allParentIDs.add(p.n.getWebId());
					//Generate all children of the parent
					ArrayList<DrawData> newParents = new ArrayList();
					for (final DrawData p: parents){
						//Set skew for this parent
						if (p.skew == -1)
							p.skew = Math.random()*Math.PI;						
						int parID = p.n.getWebId();
						ArrayList<Node> childs = new ArrayList();
						ArrayList<Node> potential = new ArrayList();

						//Create links
						potential.addAll(p.n.L.getNeighborsSet());
						for (Node x: p.n.L.getNeighborsSet())
							linksPot.add(new DrawLink(p.n, x));
						potential.addAll(p.n.L.getSurrogateNeighborsSet());
						for (Node x: p.n.L.getSurrogateNeighborsSet())
							linksPot.add(new DrawLink(p.n, x, DrawLink.Type.DOTTED));

						//Only add potential children if they are a direct child
						//of the parent or not a direct child of any other parent
						for (Node c: potential){
							//Make sure this hasn't been drawn already
							if (!nodes.containsKey(c)){
								int webID = c.getWebId(),
									childsParID = webID & ~Integer.highestOneBit(webID);
								if (parID == childsParID || !allParentIDs.contains(childsParID))
									childs.add(c);
							}
						}

						//Now we have a list of children, build the draw data for each
						int numChilds = childs.size();
						double theta = 2*Math.PI / (double)(numChilds == 2 ? 3 : numChilds);
						//Set references to children data
						p.children = drawCircle(childs, p, p.coord, p.skew, theta, radius+margin, level);
						newParents.addAll(p.children);
					}
					if (newParents.isEmpty())
						break;
					parents = newParents;
				}
				
				//Filter out links
				for (DrawLink l: linksPot){
					if (nodes.containsKey(l.friend) && nodes.containsKey(l.origin))
						links.add(l);
				}
			}
			
			@Override
			public JToolBar getToolbar(){
				JToolBar bar = new JToolBar();
				bar.setFloatable(false);
				bar.add(new JLabel("Graph Node: "));
				select = new JSpinner(new SpinnerNumberModel(0, 0, null, 1));
				select.setPreferredSize(new Dimension(70, 30));
				select.addChangeListener(new ChangeListener(){
					@Override
					public void stateChanged(ChangeEvent e){
						graph.draw();
					}
				});
				bar.add(select);
				bar.add(new JLabel("Levels: "));
				levelSelect = new JSpinner(new SpinnerNumberModel(3, 0, 99, 1));
				levelSelect.addChangeListener(new ChangeListener(){
					@Override
					public void stateChanged(ChangeEvent e){
						graph.draw();
					}
				});
				bar.add(levelSelect);
				bar.add(Box.createHorizontalGlue());
				return bar;
			}
		};
		/*
		TREE ("Spanning Tree", RotateMode.ROTATE){
			private int levels = 4, radiusDelta;
			private Point2D origin;
			
			@Override
			public void draw(Node n) {
				nodes = new HashMap();
				links = new TreeSet();
				helpers = new ArrayList();
				
				//Start out with node in middle
				origin = new Point2D.Double(maxSizeX/2, maxSizeY/2);
				DrawData active = new DrawData(n, null, null, -1, 0, origin);
				nodes.put(n, active);
				
				//Draw tree in a circle; evenly distributing all branches around circle
				if (levels > 0){
					radiusDelta = (maxSizeX/2-20)/levels;
					drawBranch(active, 0, Math.PI*2, radiusDelta, levels);
				}
			}
			private void drawBranch(DrawData p, double angleOffset, double angle, int radius, int level){
				//Create links
				int idx = Integer.highestOneBit(p.n.getWebId());
				ArrayList<Node> childs = p.n.getTreeChildren();
				for (Node c: childs){
					boolean isSurr = Integer.highestOneBit(c.getWebId()) < idx;
					links.add(new DrawLink(p.n, c, isSurr ? DrawLink.Type.DOTTED : DrawLink.Type.SOLID));
				}
				if (!childs.isEmpty()){
					//Draw children
					angleOffset -= angle/2.0;
					double delta = angle/(double) childs.size();
					p.children = drawCircle(childs, p, origin, angleOffset, delta, radius, level);
					//Recurse
					if (--level != 0){
						radius += radiusDelta;
						for (DrawData d: p.children){
							drawBranch(d, angleOffset, delta, radius, level);
							angleOffset += delta;
						}
					}
				}
			}
			
			@Override
			public JToolBar getToolbar(){
				JToolBar bar = new JToolBar();
				bar.add(new JLabel("Root Node:"));
				bar.add(new JSpinner(new SpinnerNumberModel(0, 0, null, 1)));
				bar.add(Box.createHorizontalGlue());
				return bar;
			}
		},
		SAND ("Sand Pile", RotateMode.ROTATE){
			private int minDim = 2, maxDim = 6;
			
			@Override
			public void draw(Node n){
				nodes = new HashMap();
				links = new TreeSet();
				TreeMap<Integer, Node> all = container.nodeList.list;
				Node[] vals = all.values().toArray(new Node[all.size()]);
				Integer[] keys = all.keySet().toArray(new Integer[all.size()]);
				
				Point2D origin = new Point2D.Double(maxSizeX/2, maxSizeY/2);
				
				Node temp;
				//Hue counters
				double hueDelta = 1/(double)(maxDim-minDim);
				float hue = 0;
				//Radius counters
				int radDelta = (maxSizeX/2-20)/(maxDim-minDim+1), radius = 0;
				//Each circle corresponds to another Hypercube dimension
				//Dimension counters
				int dID = (int) Math.pow(2, minDim),
					maxDID = dID,
					d = minDim;
					//Get starting node
				int index = Arrays.binarySearch(keys, dID);
				if (index != -1){
					while (d <= maxDim){
						//Increment counters
						maxDID *= 2;
						radius += radDelta;
						hue += hueDelta;
						//Give each dimension its own color
						Color linkCol = new Color(Color.HSBtoRGB(hue, 1, 0.5f));
						//Loop through all nodes in this dimension
						ArrayList<DrawData> dimData = new ArrayList();
						double delta = Math.PI*2 / (double)(maxDID - dID);
						while (keys[index] < maxDID){
							//dimNodes.add(vals[index]);
							double angle = (maxDID-keys[index])*delta;
							/*
							DrawData data = new DrawData(vals[index], , null, 0, d, new Point2D.Double(
								(radius*Math.cos(angle)) + origin.getX(),
								(radius*Math.sin(angle)) + origin.getY()
							));
							nodes.put(vals[index], data);
							*
							index++;
						}
						d++;
					}
				}
			}
			
			@Override
			public JToolBar getToolbar(){
				JToolBar bar = new JToolBar();
				bar.add(new JLabel("Low Dimension:"));
				final JSpinner loDim = new JSpinner(new SpinnerNumberModel(2, 2, null, 1));
				loDim.addChangeListener(new ChangeListener(){
					@Override
					public void stateChanged(ChangeEvent e){
						minDim = (int) loDim.getValue();
					}
				});
				bar.add(loDim);
				bar.add(new JLabel("High Dimension:"));
				final JSpinner hiDim = new JSpinner(new SpinnerNumberModel(2, 2, null, 1));
				hiDim.addChangeListener(new ChangeListener(){
					@Override
					public void stateChanged(ChangeEvent e) {
						SpinnerNumberModel lo = (SpinnerNumberModel) loDim.getModel();
						lo.setMaximum((Comparable) hiDim.getValue());
						if ((int) lo.getValue() > (int) lo.getMaximum())
							lo.setValue(lo.getMaximum());
					}
				});
				bar.add(hiDim);
				bar.add(Box.createHorizontalGlue());
				return bar;
			}
		};
		/*
		PETRIE ("Petrie Polygon", RotateMode.ROTATE),
		*/
		
		public static HashMap<Node, DrawData> nodes;
		public static TreeSet<DrawLink> links;
		public static ArrayList<DrawData> helpers;
		
		public static enum RotateMode {TRANSLATE, ROTATE}
		
		private final String name;
		private final RotateMode rotateMode;
		
		GraphMode(String name, RotateMode rotateMode){
			this.name = name;
			this.rotateMode = rotateMode;
		}
		
		public abstract void draw();	
		public abstract JToolBar getToolbar();
		private static ArrayList<DrawData> drawCircle(
			ArrayList<Node> childs, DrawData parent, Point2D origin,
			double angle, double delta, double radius, int level
		){
			ArrayList<DrawData> res = new ArrayList();
			for (Node c: childs){
				DrawData data = new DrawData(c, parent, null, -1, level, new Point2D.Double(
					(radius*Math.cos(angle)) + origin.getX(),
					(radius*Math.sin(angle)) + origin.getY()
				));
				res.add(data);
				nodes.put(c, data);
				angle += delta;
			}
			return res;
		}
		
		@Override
		public String toString(){
			return name;
		}
	}
	private static class DrawLink implements Comparable{
		public static enum Type {SOLID, DOTTED};
		public Node origin;
		public Node friend;
		public Type type;
		public Color color;

		public DrawLink(Node origin, Node friend){
			this(origin, friend, Type.SOLID);
		}
		public DrawLink(Node origin, Node friend, Type type){
			this(origin, friend, type, type == Type.SOLID ? Color.RED : Color.GREEN);
		}
		public DrawLink(Node origin, Node friend, Type type, Color color){
			this.origin = origin;
			this.friend = friend;
			this.type = type;
			this.color = color;
		}

		@Override
		public boolean equals(Object n){
			if (n == null || getClass() != n.getClass())
				return false;
			DrawLink l = (DrawLink) n;
			return ((origin == l.origin && friend == l.friend) ||
					(friend == l.origin && origin == l.friend));
		}
		@Override
		public int hashCode() {
			int hash = 7;
			hash = 29 * hash + Objects.hashCode(this.origin);
			hash = 29 * hash + Objects.hashCode(this.friend);
			hash = 29 * hash + (this.type != null ? this.type.hashCode() : 0);
			return hash;
		}
		@Override
		public int compareTo(Object o) {
			if (o == null || getClass() != o.getClass())
				return 1;
			DrawLink l = (DrawLink) o;
			if (this.equals(l))
				return 0;
			if (l.type == type)
				return l.color.getRGB() > color.getRGB() ? 1 : -1;
			return l.type.compareTo(type) > 0 ? 1 : -1;
		}
	}
	private static class DrawData{
		public static int nodeSize = 7;			//Node size, in pixels
		public ArrayList<DrawData> children;		//Children of current drawdata
		public DrawData parent;					//Parent of current drawdata (if level != 0)
		public Node n;							//Node associated with this data
		public double skew;						//Rotation skew angle
		public int level;						//Level that this was drawn at
		public Point2D coord;					//Coordinates on screen

		public DrawData(Node n, DrawData parent, ArrayList<DrawData> children, double skew, int level, Point2D coord){
			this.n = n;
			this.parent = parent;
			this.children = children;
			this.skew = skew;
			this.level = level;
			this.coord = coord;
		}
		public void draw(Graphics2D g2, boolean useBinary){
			if (n != null){
				double x = coord.getX(), y = coord.getY();
				g2.fillOval((int) (x-nodeSize/2.0), (int) (y-nodeSize/2.0), nodeSize, nodeSize);
				String name = useBinary ? Integer.toBinaryString(n.getWebId()) : String.valueOf(n.getWebId());
				g2.drawString(name+" ("+n.getHeight()+")", (int) x+5, (int) y-5);
			}
		}
	}
	
	/**
	 * Handles rendering of GraphMode data
	 * Also handles all user interaction with the graph
	 */
	private class Graph extends JPanel{
		//Graph title
		private String title;					//Graph title
		//Node stuff
		private int mx, my;						//Current mouse coordinates
		private final int selMargin = 15;		//Mimimum margin before a node is close enough to mouse for selection
		private final HashSet<DrawData> hide;	//Hidden subtrees
		GraphMode mode;							//Holds all data for the graph
		//Drawing modes
		private final AlphaComposite compMode = AlphaComposite.getInstance(AlphaComposite.DST_OVER);
		private final float[] dash1 = {4};
		private final Stroke
			strokeSolid = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
			strokeDotted = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, dash1, 10);			
		//Drawing buffer
		private BufferedImage buffer;
		private DrawData active, selected;

		/**
		 * Create a new graph and bind mouse listeners
		 * to the component
		 */
		public Graph(){
			//Initialize data structures
			hide = new HashSet();
			//Initialize Mouse listeners
			addMouseListener(new MouseListener(){
				@Override
				public void mousePressed(MouseEvent e) {
					switch (e.getButton()){
						case MouseEvent.BUTTON1:
							selectNode();
							break;
						case MouseEvent.BUTTON2:
							unhideNodes();
							break;
						case MouseEvent.BUTTON3:
							hideNode();
							break;
					}
				}
				@Override
				public void mouseClicked(MouseEvent e) {}
				@Override
				public void mouseReleased(MouseEvent e) {}
				@Override
				public void mouseEntered(MouseEvent e) {}
				@Override
				public void mouseExited(MouseEvent e) {}
			});
			addMouseMotionListener(new MouseMotionListener(){
				@Override
				public void mouseDragged(MouseEvent e) {
					int x = e.getX(), y = e.getY();
					//Rotate, if a node is selected
					DrawData par;
					if (selected != null && (par = selected.parent) != null){
						double px = par.coord.getX(), py = par.coord.getY(),
								theta = Math.atan2(y-py, x-px)-Math.atan2(my-py, mx-px);
						//Rotate all the immediate children
						AffineTransform tr = new AffineTransform();
						tr.rotate(theta, px, py);
						for (DrawData c: par.children){
							//Rotate all descendants
							if (mode.rotateMode == GraphMode.RotateMode.ROTATE)
								transform(c, tr);
							//Rotate child, shift descendents
							else{
								Point2D rot = new Point2D.Double();
								tr.transform(c.coord, rot);
								AffineTransform tt = new AffineTransform();
								tt.translate(
									rot.getX()-c.coord.getX(),
									rot.getY()-c.coord.getY()
								);
								//Shift immediate children's descendants
								transform(c, tt);
							}
						}
						redraw(true);
					}
					mx = x;
					my = y;
				}
				@Override
				public void mouseMoved(MouseEvent e) {
					mx = e.getX();
					my = e.getY();
					highlightNode();
				}
			});
		}

		//ACTIONS
		public void viewSelected(){
			/*
			if (selected != null)
				draw(selected.n);
			*/
		}
		public void highlightNode(){
			if (mode == null || GraphMode.nodes == null)
				return;
			//Highlight a node for selection
			Point2D temp;
			for (DrawData datum: GraphMode.nodes.values()){
				if (!hide.contains(datum) && datum.n != null &&
					datum.coord.distance(mx, my) <= selMargin)
				{
					if (active != datum){
						active = datum;
						redraw(false);
					}
					return;
				}
			}
			if (active != null){
				active = null;
				redraw(false);
			}
		}
		public void selectNode(){
			if (active != null){
				selected = active;
				container.setSelectedNode(selected.n);
				redraw(false);
			}
		}
		public void hideNode(){
			if (active == null)
				return;
			//Hide all nodes descending from this one
			hide.add(active);
			ArrayList<DrawData> childs = active.children;
			while (childs != null && !childs.isEmpty()){
				hide.addAll(childs);
				ArrayList<DrawData> temp = new ArrayList<>();
				for (DrawData z: childs){
					if (z.children != null)
						temp.addAll(z.children);
				}
				childs = temp;
			}
			active = null;
			redraw(true);			
		}
		public void unhideNodes(){
			hide.clear();
			redraw(true);
		}
		private void transform(DrawData d, AffineTransform t){
			t.transform(d.coord, d.coord);
			if (d.children != null){
				for (DrawData c: d.children)
					transform(c, t);
			}
		}

		//DRAWING
		/**
		 * Performs a full refresh on the graph
		 */
		public void draw(){
			//Get drawing data from the graph's mode
			hide.clear();
			mode = (GraphMode) modes.getSelectedItem();
			mode.draw();
			//See if the selected node is in the new graph
			if (selected != null){
				DrawData d = GraphMode.nodes.get(selected.n);
				selected = d;
				if (d == null)
					container.setSelectedNode(null);
			}
			active = null;
			buffer = null;
			redraw(false);
		}
		/**
		 * Uses pre-calculated draw-data to redraw the graph
		 * @param clearBuffer should we clear the screen's buffer
		 */
		private void redraw(boolean clearBuffer){
			if (clearBuffer)
				buffer = null;
			repaint();
		}
		/**
		 * Draws a node in the graph, recursively
		 * @param g graphics context
		 * @param d the node's draw data
		 */
		private void drawNode(Graphics2D g, DrawData d){
			d.draw(g, useBinary);
			if (d.children != null){
				for (DrawData c: d.children){
					if (!hide.contains(c))
						drawNode(g, c);
				}
			}
		}
		/**
		 * Draws a blue circle around the selected node
		 * @param g2 graphics context
		 */
		private void drawSelected(Graphics2D g2){
			if (selected != null){
				g2.setColor(Color.BLUE);
				g2.setStroke(strokeSolid);
				int d = DrawData.nodeSize*4,
					r = d/2;
				g2.drawOval((int) selected.coord.getX()-r, (int) selected.coord.getY()-r, d, d);
			}
		}
		/**
		 * Draws all links between the nodes in the graph
		 * @param g graphics context
		 */
		private void paintLinks(Graphics2D g){
			DrawData d1, d2;
			DrawLink.Type curType = null;
			Color curCol = null;
			for (DrawLink l: GraphMode.links){
				d1 = GraphMode.nodes.get(l.friend);
				d2 = GraphMode.nodes.get(l.origin);
				//Make sure references exist in the graph
				if (hide.contains(d1) || hide.contains(d2))
					continue;
				//Switch drawing mode
				//TreeSet is ordered by type/color, so we'll only switch modes once in a while
				if (curType != l.type){
					curType = l.type;
					switch (l.type){
						case SOLID:
							g.setStroke(strokeSolid);
							break;
						case DOTTED:
							g.setStroke(strokeDotted);
							break;
					}
				}
				if (curCol != l.color){
					curCol = l.color;
					g.setColor(curCol);
				}
				//Draw a line
				g.drawLine((int) d1.coord.getX(), (int) d1.coord.getY(), (int) d2.coord.getX(), (int) d2.coord.getY());
			}
		}
		@Override
		protected void paintComponent(Graphics g){
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			//Empty graph
			if (GraphMode.nodes == null || GraphMode.nodes.isEmpty()){
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setFont(new Font("Sans Serif", Font.BOLD, 30));
				g2.drawString("EMPTY GRAPH", maxSizeX/2-100, maxSizeY/2-15);
				return;
			}
			//Draw a node
			if (buffer == null){
				buffer = new BufferedImage(maxSizeX, maxSizeY, BufferedImage.TYPE_INT_ARGB);
				Graphics2D gbi = buffer.createGraphics();
				gbi.setRenderingHints(new HashMap<RenderingHints.Key, Object>(){{
					put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
					put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				}});

				//Paint all links and nodes, recursively
				paintLinks(gbi);
				gbi.setColor(Color.BLACK);
				for (DrawData d: GraphMode.helpers)
					drawNode(gbi, d);
			}
			//Draw buffer to screen
			g2.drawImage(buffer, null, 0, 0);
			//Selected nodes
			if (active != null){
				g2.setColor(Color.CYAN);
				active.draw(g2, useBinary);
				//Children of selection
				if (active.children != null){
					g2.setColor(Color.YELLOW);
					for (DrawData child: active.children){
						if (!hide.contains(child))
							child.draw(g2, useBinary);
					}
				}
				//Parent of selection
				if (active.parent != null){
					g2.setColor(Color.GREEN);
					active.parent.draw(g2, useBinary);
				}
			}
			//Set cursor
			container.setCursor(active != null ?
				Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) :
				Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
			);
			drawSelected(g2);
		}
	}
}
