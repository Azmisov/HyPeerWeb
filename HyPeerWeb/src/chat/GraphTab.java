package chat;

import hypeerweb.Links;
import hypeerweb.Node;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.JComboBox;

/**
 * Draws a directed graph of a HyPeerWeb node
 * @author isaac
 */
public class GraphTab extends JPanel{
	private static final int maxSizeX = 500, maxSizeY = 700;
	private final ChatClient container;
	private final JComboBox<GraphMode> modes = new JComboBox(GraphMode.values());
	private final Graph graph;
	
	public GraphTab(ChatClient container){
		this.container = container;
		setLayout(new BorderLayout(0, 0));
		
		//Graphing tab is split into a toolbar and graph
		JToolBar bar = new JToolBar(JToolBar.HORIZONTAL);
		bar.setFloatable(false);
		
		/**
		 * Stuff needed:
		 * Hide, Unhide All, Help, Viewing Mode, Go up a level
		 */
		bar.add(new JLabel("Mode: "));
		bar.add(modes);
		bar.add(new JButton("Hide Selected"));
		bar.add(new JButton("Unhide All"));
		bar.add(new JButton("Navigate Up"));		
		JButton btnHelp = new JButton("Help");
		btnHelp.setAlignmentX(RIGHT_ALIGNMENT);
		bar.add(btnHelp);
		
		//Bottom is the actual graph
		graph = new Graph();
		
		//Layout components
		//add(bar, BorderLayout.NORTH);
		add(graph, BorderLayout.CENTER);
	}
	
	public void draw(Node n){
		graph.draw(n);
	}
	
	private enum GraphMode{
		DEFAULT ("Default"){
			//Minimum margin between nodes (within allowed window space)
			private static final int margin = 3;
			private int levels = 3;
			
			@Override
			public void draw(Node n){
				nodes = new HashMap();
				links = new HashSet();
				HashSet<DrawLink> linksPot = new HashSet();
				
				//Start off with the active node
				int radius = (int) (maxSizeX/2-levels*margin)/2;
				Point2D origin = new Point2D.Double(maxSizeX/2, maxSizeY/2);
				DrawData active = new DrawData(n, null, null, -1, 0, origin);
				nodes.put(n, active);
				
				//Each level evenly distributes the nodes to the parents
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
							linksPot.add(new DrawLink(p.n, x, Links.Type.NEIGHBOR));
						potential.addAll(p.n.L.getSurrogateNeighborsSet());
						for (Node x: p.n.L.getSurrogateNeighborsSet())
							linksPot.add(new DrawLink(p.n, x, Links.Type.SNEIGHBOR));

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
						ArrayList<DrawData> childData = new ArrayList();
						int numChilds = childs.size();
						double theta = 2*Math.PI / (double)(numChilds == 2 ? 3 : numChilds);
						double angle = p.skew;
						adjRadius = radius + margin;
						for (Node c: childs){
							DrawData data = new DrawData(c, p, null, -1, level, new Point2D.Double(
								(adjRadius*Math.cos(angle)) + p.coord.getX(),
								(adjRadius*Math.sin(angle)) + p.coord.getY()
							));
							childData.add(data);
							nodes.put(c, data);
							angle += theta;
						}
						//Set references to children data
						p.children = childData;
						newParents.addAll(childData);
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
		};
		/*
		TREE ("Spanning Tree"),
		PETRIE ("Petrie Polygon"),
		SAND ("Sand Pile");
		*/
		
		public HashMap<Node, DrawData> nodes;
		public HashSet<DrawLink> links;
		
		private final String name;
		GraphMode(String name){
			this.name = name;
		}
		public abstract void draw(Node n);
		@Override
		public String toString(){
			return name;
		}
	}
	private static class DrawLink implements Comparable{
		public Node origin;
		public Node friend;
		public Links.Type type;

		public DrawLink(Node origin, Node friend, Links.Type type){
			this.origin = origin;
			this.friend = friend;
			this.type = type;
		}

		@Override
		public boolean equals(Object n){
			if (n == null || getClass() != n.getClass())
				return false;
			DrawLink l = (DrawLink) n;
			return type == l.type &&
					((origin == l.origin && friend == l.friend) ||
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
			if (l.type == type)
				return this.equals(l) ? 0 : 1;
			return l.type.compareTo(type);
		}
	}
	private static class DrawData{
		public static int nodeSize = 6;			//Node size, in pixels
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
		public void draw(Graphics2D g2){
			double x = coord.getX(), y = coord.getY();
			g2.fillOval((int) (x-nodeSize/2.0), (int) (y-nodeSize/2.0), nodeSize, nodeSize);
			g2.drawString(n.getWebId()+" ("+n.getHeight()+")", (int) x+5, (int) y-5);
		}
	}
	
	private class Graph extends JPanel{
		//Graph title
		private String title;					//Graph title
		//Node stuff
		private Node n;							//Node we are going to draw
		private final int selMargin = 15;		//Mimimum margin before a node is close enough to mouse for selection
		private int mx, my;						//Current mouse coordinates
		private final HashSet<Node> hide;		//Hidden subtrees
		GraphMode mode;							//Holds all data for the graph
		//Link types
		private final Links.Type[] Ntypes = {
			Links.Type.NEIGHBOR,
			Links.Type.SNEIGHBOR,
			Links.Type.ISNEIGHBOR
		};
		//Drawing modes
		private final AlphaComposite compMode = AlphaComposite.getInstance(AlphaComposite.DST_OVER);
		private final float[] dash1 = {4}, dash2 = {10};
		private final Stroke
			strokeN = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
			strokeSN = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, dash1, 10),
			strokeISN = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, dash2, 10);
		//Drawing buffer
		private BufferedImage buffer;
		private Map.Entry<Node, DrawData> active, selected;
		private boolean redraw = false;

		public Graph(){
			//Initialize data structures
			hide = new HashSet();
			//Initialize Mouse listeners
			addMouseListener(new MouseListener(){
				@Override
				public void mousePressed(MouseEvent e) {
					mouseClick(e.getButton());
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
					dragMouse(e.getX(), e.getY());
				}
				@Override
				public void mouseMoved(MouseEvent e) {
					mx = e.getX();
					my = e.getY();
					highlightNode();
				}
			});
		}

		//MOUSE
		public void dragMouse(int x, int y){
			//Rotate, if a node is selected
			DrawData ddChild, ddParent;
			if (selected != null && (ddChild = selected.getValue()).parent != null){
				ddParent = ddChild.parent;
				double px = ddParent.coord.getX(),
						py = ddParent.coord.getY();
				ddParent.skew += Math.atan2(y-py, x-px)-Math.atan2(my-py, mx-px);
				redraw(false);
			}
			mx = x;
			my = y;
		}
		public void mouseClick(int button){
			switch (button){
				case MouseEvent.BUTTON1:
					selectNode();
					break;
				case MouseEvent.BUTTON2:
					unhideNodes();
					break;
				case MouseEvent.BUTTON3:
					selectNode();
					hideNode();
					break;
			}
		}

		//ACTIONS
		public void viewSelected(){
			if (selected != null)
				draw(selected.getKey());
		}
		public void highlightNode(){
			if (mode == null || mode.nodes == null)
				return;
			//Highlight a node for selection
			Point2D temp;
			for (Map.Entry<Node, DrawData> datum: mode.nodes.entrySet()){
				if (datum.getValue().coord.distance(mx, my) <= selMargin){
					if (active != datum){
						active = datum;
						repaint();
					}
					return;
				}
			}
			if (active != null){
				active = null;
				repaint();
			}
		}
		public void selectNode(){
			if (active != null){
				selected = active;
				container.setSelectedNode(selected.getKey());
				repaint();
			}
		}
		public void hideNode(){
			if (selected == null)
				return;
			hide.add(selected.getKey());
			selected = null;
			redraw(false);
		}
		public void unhideNodes(){
			hide.clear();
			redraw(false);
		}

		//DRAWING
		public void draw(Node n){
			this.n = n;
			Node parent = n.getParent();
			//Graph Title
			title = "Graph of Node #"+n.getWebId()+" ("+n.getHeight()+")";
			if (parent != null)
				title += ", child of Node #"+parent.getWebId()+" ("+parent.getHeight()+")";
			//Get drawing data from the graph's mode
			hide.clear();
			mode = (GraphMode) modes.getSelectedItem();
			mode.draw(n);
			selected = null;
			active = null;
			buffer = null;
			repaint();
		}
		private void redraw(boolean force){
			if (force){
				selected = null;
				active = null;
				buffer = null;
				mode.nodes.clear();
			}
			redraw = !force;
			repaint();
		}
		@Override
		protected void paintComponent(Graphics g){
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			//Redrawing
			if (buffer != null && !redraw){
				g2.drawImage(buffer, null, 0, 0);
				//Selected nodes
				if (active != null){
					DrawData temp = active.getValue();
					g2.setColor(Color.CYAN);
					temp.draw(g2);
					//Children of selection
					if (temp.children != null){
						g2.setColor(Color.YELLOW);
						for (DrawData child: temp.children)
							child.draw(g2);
					}
					//Parent of selection
					if (temp.parent != null){
						g2.setColor(Color.GREEN);
						temp.parent.draw(g2);
					}
				}
				//Set cursor
				container.setCursor(active != null ?
					Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) :
					Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
				);
				drawSelected(g2);
				return;
			}
			//Draw a node
			if (n != null){
				buffer = new BufferedImage(maxSizeX, maxSizeY, BufferedImage.TYPE_INT_ARGB);
				Graphics2D gbi = buffer.createGraphics();
				gbi.setRenderingHint(
					RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON
				);

				//Graph title
				gbi.setColor(Color.BLACK);
				gbi.drawString(title, 20, 20);

				//Draw all nodes, recursively
				drawNode(gbi, mode.nodes.get(n));
				
				//Paint all links
				gbi.setComposite(compMode);
				paintLinks(gbi);
				
				//Draw buffer to screen
				g2.drawImage(buffer, null, 0, 0);
				drawSelected(g2);
			}
			redraw = false;
		}
		
		private void drawNode(Graphics2D g, DrawData d){
			d.draw(g);
			if (d.children != null){
				for (DrawData c: d.children){
					if (!hide.contains(c.n))
						drawNode(g, c);
				}
			}
		}
		private void drawSelected(Graphics2D g2){
			if (selected != null){
				DrawData sel = selected.getValue();
				g2.setColor(Color.BLUE);
				g2.setStroke(strokeN);
				int d = DrawData.nodeSize*4,
					r = d/2;
				g2.drawOval((int) sel.coord.getX()-r, (int) sel.coord.getY()-r, d, d);
			}
		}
		
		
		/**
		 * Draws all links between the nodes in the graph
		 * @param g graphics context
		 */
		
		private void paintLinks(Graphics2D g){
			DrawData d1, d2;
			Links.Type curType = null;
			for (DrawLink l: mode.links){
				d1 = mode.nodes.get(l.friend);
				d2 = mode.nodes.get(l.origin);
				//Make sure references exist in the graph
				if (hide.contains(l.friend) || hide.contains(l.origin))
					continue;
				//Switch drawing mode
				//TreeSet is ordered by type, so we'll only switch modes thrice
				if (curType != l.type){
					curType = l.type;
					switch (l.type){
						case NEIGHBOR:
							g.setStroke(strokeN);
							g.setColor(Color.RED);
							break;
						case SNEIGHBOR:
							g.setStroke(strokeSN);
							g.setColor(Color.GREEN);
							break;
						case ISNEIGHBOR:
							g.setStroke(strokeISN);
							g.setColor(Color.MAGENTA);
							break;
					}
				}
				//Draw a line
				g.drawLine((int) d1.coord.getX(), (int) d1.coord.getY(), (int) d2.coord.getX(), (int) d2.coord.getY());
			}
		}
	}
}
