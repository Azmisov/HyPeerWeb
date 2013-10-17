package hypeerweb;

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
import java.util.Objects;
import java.util.TreeSet;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Draws a directed graph of a HyPeerWeb node
 * @author isaac
 */
public class HyPeerWebGraph extends JFrame{
	private HyPeerWeb web;
	private Graph draw;
	private static int winSize = 500;
	private int levels;
	
	public HyPeerWebGraph() throws Exception{
		web = HyPeerWeb.getInstance();
		
		//Initialize window
		setTitle("HyPeerWeb Directed Graph");
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setSize(500, 500);
		draw = new Graph();
		add(draw);
		
		//Mouse listeners
		addMouseListener(new MouseListener(){
			@Override
			public void mousePressed(MouseEvent e) {
				throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				
			}
			@Override
			public void mouseClicked(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
		});
		addMouseMotionListener(new MouseMotionListener(){
			@Override
			public void mouseDragged(MouseEvent e) {
				
			}
			@Override
			public void mouseMoved(MouseEvent e) {
				
			}
		});
	}
	
	/**
	 * Draws the specified node out to levels
	 * @param n 
	 * @param level number of levels to go out
	 */
	public void drawNode(Node n, int level){
		levels = level;
		draw.draw(n);
	}
	
	//INNER DRAWING CLASS:
	private class Graph extends JPanel{
		private Node n;							//Node we are going to draw
		private int nodeSize = 10,				//Node size, in pixels
					margin = 20,				//Minimum margin between nodes (within allowed window space)
					mx, my;						//Mouse coordinates
		private HashMap<Node, Point2D> coords;	//Screen coords of nodes
		private HashMap<Node, Double> skews;	//Skew angles for each circle
		private TreeSet<Link> links;			//Set of links for the graph
		//Link types
		private final Node.Connections.ConnectionType[] Ntypes = {
			Node.Connections.ConnectionType.NEIGHBOR,
			Node.Connections.ConnectionType.SNEIGHBOR,
			Node.Connections.ConnectionType.ISNEIGHBOR
		};
		//Drawing modes
		private final AlphaComposite compMode = AlphaComposite.getInstance(AlphaComposite.DST_OVER);
		private final float[] dash1 = {4}, dash2 = {10};
		private final Stroke
			strokeN = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
			strokeSN = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, dash1, 10),
			strokeISN = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, dash2, 10);
		
		/**
		 * Draws the specified node
		 */
		public void draw(Node n){
			this.n = n;
			skews = new HashMap<>();
			repaint();
		}
		/**
		 * Draws the mouse cursor
		 * @param x x-coordinate
		 * @param y y-coordinate
		 */
		public void drawMouse(int x, int y){
			mx = x;
			my = y;
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			//Draw a node
			if (n != null){
				coords = new HashMap<>();
				links = new TreeSet<>();
				BufferedImage buffer = new BufferedImage(winSize, winSize, BufferedImage.TYPE_INT_ARGB);
				Graphics2D gbi = buffer.createGraphics();
				//Get diameters of circles
				int radius = winSize/2-levels*margin;
				//Paint the starting node inthe center of the screen
				gbi.setColor(Color.BLUE);
				paintNode(gbi, n, winSize/2, winSize/2);
				//Loop through each connection level and draw circles
				ArrayList<Node> parents = new ArrayList<>();
				ArrayList<Node> friends;
				parents.add(n);
				int level = 10;
				while (level-- != 0){
					radius /= 2;
					friends = new ArrayList<>();
					for (Node parent: parents)
						friends.addAll(paintCircle(gbi, parent, radius+margin));
					if (friends.isEmpty()) break;
					parents = friends;
				}
				//Paint all links
				gbi.setComposite(compMode);
				paintLinks(g2);
				g2.drawImage(buffer, null, 0, 0);
			}
		}
		private ArrayList<Node> paintCircle(Graphics2D g, Node n, int radius){
			Point2D origin = coords.get(n);
			//Get a list of nodes that haven't been drawn already
			ArrayList<Node> friends = new ArrayList<>();
			Node[][] Nlinks = {
				n.getNeighbors(),
				n.getSurrogateNeighbors(),
				n.getInverseSurrogateNeighbors()
			};
			for (int i=0; i<Ntypes.length; i++){
				for (Node link: Nlinks[i]){
					if (!coords.containsKey(link)){
						friends.add(link);
						links.add(new Link(n, link, Ntypes[i]));
					}
				}
			}
			//Draw these as friends in a circle
			double theta = 2*Math.PI/(double) friends.size();
			//Offset rotation for this circle
			Double angle = skews.get(n);
			if (angle == null){
				angle = Math.random()*2*Math.PI/friends.size();
				skews.put(n, angle);
			}
			Point2D coord = new Point2D.Double();
			for (Node friend: friends){
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
		private void paintNode(Graphics2D g, Node n, int x, int y){
			g.fillOval(x-nodeSize/2, y-nodeSize/2, nodeSize, nodeSize);
			g.drawString(n.getWebId()+" ("+n.getHeight()+")", x+5, y-5);
			coords.put(n, new Point2D.Double(x, y));
		}
		private void paintLinks(Graphics2D g){			
			Point2D n1, n2;
			Node.Connections.ConnectionType curType = null;
			for (Link l: links){
				n1 = coords.get(l.friend);
				n2 = coords.get(l.origin);
				//Make sure references exist in the graph
				if (n1 != null && n2 != null){
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
					g.drawLine((int) n1.getX(), (int) n1.getY(), (int) n2.getX(), (int) n2.getY());
				}
			}
		}
		
		private class Link implements Comparable{
			public Node origin;
			public Node friend;
			public Node.Connections.ConnectionType type;
			
			public Link(Node origin, Node friend, Node.Connections.ConnectionType type){
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
				return type.compareTo(l.type);
			}
		}
	};
}
