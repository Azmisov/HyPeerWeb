package chat;

import hypeerweb.Links;
import hypeerweb.Node;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
import javax.swing.JPanel;

/**
 * Draws a directed graph of a HyPeerWeb node
 * @author isaac
 */
public class GraphTab extends JPanel{
	
	public GraphTab(){
		//Graphing tab is split into a 
	}
	
	private class Graph extends JPanel{
		//Graph stuff
		private String title;					//Graph title
		//Node stuff
		private String detail;					//Node details
		private Node n;							//Node we are going to draw
		private Node nParent;					//The node's parent
		private final int
			nodeSize = 10,						//Node size, in pixels
			margin = 0,							//Minimum margin between nodes (within allowed window space)
			selMargin = 5;						//Mimimum margin before a node is close enough to mouse for selection
		private int mx, my,	winSize;			//Mouse coordinates and window size
		private final TreeSet<Link> links;		//Set of links for the graph
		//Link types
		private final Links.Type[] Ntypes = {
			Links.Type.NEIGHBOR,
			Links.Type.SNEIGHBOR,
			Links.Type.ISNEIGHBOR
		};
		//Drawing modes
		private int levels;
		private final HashSet<Node> hide;
		private final HashMap<Node, DrawData> data;
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
			hide = new HashSet<>();
			data = new HashMap<>();
			links = new TreeSet<>();
			//Mouse listeners
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
					moveMouse(e.getX(), e.getY());
				}
			});
		}

		//MOUSE
		public void moveMouse(int x, int y){
			mx = x;
			my = y;
			highlightNode();
		}
		public void dragMouse(int x, int y){
			//Rotate, if a node is selected
			DrawData ddChild, ddParent;
			if (selected != null && (ddChild = selected.getValue()).parent != null){
				ddParent = data.get(ddChild.parent);
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
				draw(selected.getKey(), levels);
		}
		public void highlightNode(){
			//Highlight a node for selection
			Point2D temp;
			for (Map.Entry<Node, DrawData> datum: data.entrySet()){
				if (datum.getValue().coord.distance(mx, my) <= nodeSize){
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
				repaint();
			}
		}
		public void hideNode(){
			if (selected == null)
				return;
			//Hide all nodes descending from this one
			hide.add(selected.getKey());
			ArrayList<Node> nodes = selected.getValue().children;
			while (nodes != null && !nodes.isEmpty()){
				hide.addAll(nodes);
				ArrayList<Node> temp = new ArrayList<>();
				for (Node z: nodes){
					ArrayList<Node> childs = data.get(z).children;
					if (childs != null)
						temp.addAll(childs);
				}
				nodes = temp;
			}
			selected = null;
			redraw(false);
		}
		public void unhideNodes(){
			hide.clear();
			redraw(false);
		}

		//DRAWING
		public void draw(Node n, int levels){
			this.n = n;
			this.levels = levels;
			nParent = n.getParent();
			title = "Graph of Node #"+n.getWebId()+" ("+n.getHeight()+")";
			if (nParent != null)
				title += ", child of Node #"+nParent.getWebId()+" ("+nParent.getHeight()+")";
			detail = "Link Counts = N:"+n.getNeighbors().length+
						", SN:"+n.getSurrogateNeighbors().length+
						", ISN:"+n.getInverseSurrogateNeighbors().length+
						", F:"+(n.getFold() == null ? "0" : "1")+
						", SF:"+(n.getSurrogateFold()== null ? "0" : "1")+
						", ISF:"+(n.getInverseSurrogateFold()== null ? "0" : "1");
			System.out.println("Drawing Graph of..."+n);
			hide.clear();
			data.clear();
			links.clear();
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
				data.clear();
			}
			redraw = !force;
			repaint();
		}
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			//Redrawing
			if (buffer != null && !redraw){
				g2.drawImage(buffer, null, 0, 0);
				//Selected nodes
				if (active != null){
					DrawData temp = active.getValue();
					g2.setColor(Color.CYAN);
					repaintNode(g2, active.getKey());
					//Children of selection
					if (temp.children != null){
						g2.setColor(Color.YELLOW);
						for (Node child: temp.children)
							repaintNode(g2, child);
					}
					//Parent of selection
					if (temp.parent != null){
						g2.setColor(Color.GREEN);
						repaintNode(g2, temp.parent);
					}
				}
				drawSelected(g2);
				return;
			}
			//Draw a node
			if (n != null){
				buffer = new BufferedImage(winSize, winSize, BufferedImage.TYPE_INT_ARGB);
				Graphics2D gbi = buffer.createGraphics();
				//Graph title
				gbi.setColor(Color.BLACK);
				gbi.drawString(title, 20, 20);
				gbi.drawString(detail, 20, 35);
				//Get diameters of circles
				int radius = (int) (winSize/2-levels*margin);
				//Paint the starting node inthe center of the screen
				gbi.setColor(Color.BLUE);
				if (!redraw)
					addNodeData(n, null, null, -1, 0, null);
				paintNode(gbi, n, winSize/2, winSize/2);
				//Loop through each connection level and draw circles
				ArrayList<Node> parents = new ArrayList<>();
				ArrayList<Node> friends;
				parents.add(n);
				for (int level = 0; level < levels; level++){
					radius /= 2;
					friends = new ArrayList<>();
					for (Node parent: parents){
						if (!hide.contains(parent))
							friends.addAll(paintCircle(gbi, parent, radius+margin, redraw ? -1 : level));
					}
					if (friends.isEmpty()) break;
					parents = friends;
				}
				//Paint all links
				gbi.setComposite(compMode);
				paintLinks(gbi);
				g2.drawImage(buffer, null, 0, 0);
				drawSelected(g2);
			}
			redraw = false;
		}
		/**
		 * Paints a circle of nodes
		 * @param g graphics context
		 * @param n the parent node to draw around
		 * @param radius the radius of the circle
		 * @param level what is the depth of this graph circle? Use -1 when redrawing
		 * @return a list of nodes added children nodes (of n, the parent)
		 */
		private ArrayList<Node> paintCircle(Graphics2D g, Node n, int radius, int level){
			DrawData parentData = data.get(n);
			Point2D origin = parentData.coord;
			//Get a list of nodes that haven't been drawn already
			ArrayList<Node> friends;
			if (level != -1){
				friends = new ArrayList<>();
				Node[][] Nlinks = {
					n.getNeighbors(),
					n.getSurrogateNeighbors(),
					n.getInverseSurrogateNeighbors()
				};
				for (int i=0; i<Ntypes.length; i++){
					for (Node link: Nlinks[i]){
						if (!data.containsKey(link))
							friends.add(link);
						links.add(new Link(n, link, Ntypes[i]));
					}
				}
				parentData.children = friends;
			}
			else friends = parentData.children;
			//Remove hidden friends
			ArrayList<Node> temp = new ArrayList<>();
			for (Node z: friends){
				if (!hide.contains(z))
					temp.add(z);
			}
			friends = temp;
			//Draw these friends in a circle
			if (parentData.skew == -1)
				parentData.skew = Math.random()*2*Math.PI/friends.size();
			double theta = 2*Math.PI/(double) (friends.size() == 2 ? 3 : friends.size()),
					angle = parentData.skew;
			Point2D coord = new Point2D.Double();
			for (Node friend: friends){
				if (level != -1)
					addNodeData(friend, n, null, -1, level, null);
				coord.setLocation(
					radius*Math.cos(angle),
					radius*Math.sin(angle)
				);
				paintNode(g, friend,
					(int) (origin.getX()+coord.getX()),
					(int) (origin.getY()+coord.getY())
				);
				angle += theta;
			}
			return friends;
		}
		/**
		 * Draws an individual node
		 * @param g graphics context
		 * @param n the node to draw
		 * @param x x-coord location
		 * @param y y-coord location
		 */
		private void paintNode(Graphics2D g, Node n, int x, int y){
			data.get(n).coord.setLocation(x, y);
			drawNode(g, n, x, y);
		}
		private void repaintNode(Graphics2D g, Node n){
			Point2D p = data.get(n).coord;
			drawNode(g, n, (int) p.getX(), (int) p.getY());
		}
		private void drawNode(Graphics2D g, Node n, int x, int y){
			if (!hide.contains(n)){
				g.fillOval(x-nodeSize/2, y-nodeSize/2, nodeSize, nodeSize);
				g.drawString(n.getWebId()+" ("+n.getHeight()+")", x+5, y-5);
			}
		}
		private void drawSelected(Graphics2D g2){
			if (selected != null){
				DrawData d = selected.getValue();
				g2.setColor(Color.BLACK);
				//g2.setComposite(compMode);
				int s = nodeSize*2;
				g2.drawOval((int) d.coord.getX()-nodeSize, (int) d.coord.getY()-nodeSize, s, s);
			}
		}
		/**
		 * Draws all links between the nodes in the graph
		 * @param g graphics context
		 */
		private void paintLinks(Graphics2D g){
			DrawData d1, d2;
			Links.Type curType = null;
			ArrayList<Link> badLinks = new ArrayList<>();
			for (Link l: links){
				d1 = data.get(l.friend);
				d2 = data.get(l.origin);
				//Make sure references exist in the graph
				if (d1 != null && d2 != null){
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
				else badLinks.add(l);
			}
			links.removeAll(badLinks);
		}

		//DATA STRUCTURES
		private void addNodeData(Node n, Node parent, ArrayList<Node> children, double skew, int level, Point2D coord){
			if (coord == null)
				coord = new Point2D.Double();
			data.put(n, new DrawData(parent, children, skew, level, coord));
		}
		private class Link implements Comparable{
			public Node origin;
			public Node friend;
			public Links.Type type;

			public Link(Node origin, Node friend, Links.Type type){
				this.origin = origin;
				this.friend = friend;
				this.type = type;
			}

			@Override
			public boolean equals(Object n){
				if (n == null || getClass() != n.getClass())
					return false;
				Link l = (Link) n;
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
				Link l = (Link) o;
				if (l.type == type)
					return this.equals(l) ? 0 : 1;
				return l.type.compareTo(type);
			}
		}
		private class DrawData{
			ArrayList<Node> children;
			Node parent;		//Parent node (if level != 0)
			double skew;		//Rotation skew angle
			int level;			//Level that this was drawn at
			Point2D coord;		//Coordinates on screen

			public DrawData(Node parent, ArrayList<Node> children, double skew, int level, Point2D coord){
				this.parent = parent;
				this.children = children;
				this.skew = skew;
				this.level = level;
				this.coord = coord;
			}
		}
	}
}
