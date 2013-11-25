package hypeerweb;

import hypeerweb.visitors.SendVisitor;
import hypeerweb.visitors.BroadcastVisitor;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

/**
 * The Great HyPeerWeb
 * @param <T> The Node type for this HyPeerWeb instance
 * @author isaac
 */
public class HyPeerWebSegment<T extends Node> extends Node{
	private Database db = null;
	private TreeMap<Integer, Node> nodes;
	private HyPeerWebState state;
	//Random number generator for getting random nodes
	private static final Random rand = new Random();
	//Static list of all HWSegments in this process; they may not correspond to the same HyPeerWeb
	//This is used by NodeProxy to read-resolve
	public static ArrayList<HyPeerWebSegment> segmentList = new ArrayList();
	
	/**
	 * Constructor for initializing the HyPeerWeb with default Node values
	 * @param dbName should we sync our HyPeerWeb to a database;
	 *	Warning! Database access can be very slow (e.g. "HyPeerWeb.sqlite")
	 * @param seed the random seed number for getting random nodes; use -1
	 *	to get a pseudo-random seed
	 * @throws Exception if there was a database error
	 * @author isaac
	 */
	public HyPeerWebSegment(long seed) throws Exception{
		this(seed, 0, 0);
	}
	/**
	 * Constructor for initializing the HyPeerWeb with defined Node values
	 * @param dbName should we sync our HyPeerWeb to a database;
	 *	Warning! Database access can be very slow (e.g. "HyPeerWeb.sqlite")
	 * @param seed the random seed number for getting random nodes; use -1
	 *	to get a pseudo-random seed
	 * @param webID the node webID, if it has one
	 * @param height the node height, if it has one
	 * @throws Exception if there was a database error
	 * @author isaac
	 */
	public HyPeerWebSegment(long seed, int webID, int height) throws Exception{
		super(0, 0);
		nodes = new TreeMap();
		if (seed != -1)
			rand.setSeed(seed);
		segmentList.add(this);
	}
	
	/**
	 * Removes the node of specified webid
	 * @param webid the webid of the node to remove
	 * @param listener event callback
	 */
	public void removeNode(int webid, Node.Listener listener){
		//TODO, make get node take in a listener
		HyPeerWebSegment seg = getNonemptySegment();
		if (seg == null) return;
		if (isSegmentEmpty())
			seg.removeNode(webid, listener);
		else{
			Node n = nodes.get(webid);
			if(n != null)
				listener.callback(n);
			else{
				removeNode((T)n, listener);
				SendVisitor visitor = new SendVisitor(webid, listener);
				visitor.visit(getFirstSegmentNode());
			}
		}
	}
	/**
	 * Removes the node
	 * @param node the node to remove
	 * @param listener event callback
	 */
	public void removeNode(T node, Node.Listener listener){
		state.removeNode(this, node, listener);
	}
	/**
	 * Removes all nodes from HyPeerWeb
	 * @param listener event callback
	 */
	public void removeAllNodes(final Node.Listener listener){
		(new BroadcastVisitor(new Node.Listener() {
			@Override
			public void callback(Node n) {
				HyPeerWebSegment seg = (HyPeerWebSegment) n;
				//If we can't remove all nodes, HyPeerWeb is corrupt
				if (seg.db != null && !seg.db.clear())
					seg.changeState(HyPeerWebState.CORRUPT);
				seg.nodes = new TreeMap();
				//Call listener
				listener.callback(n);
			}
		})).visit(this);
	}
	
	/**
	 * Adds a new node to the HyPeerWeb
	 * @param listener event callback
	 */
	public void addNode(Node.Listener listener){
		if (isSegmentEmpty())
			getNonemptySegment().addNode(listener);
		else
			state.addNode(this, listener);
	}
	protected void addDistantChild(Node child)
	{
		nodes.put(child.getWebId(), child);
	}
	/**
	 * Holds the state of the entire HyPeerWeb, not just
	 * this individual segment. Handles special cases for
	 * add and remove node, as well as a corrupt HyPeerWeb.
	 */
	private enum HyPeerWebState{
		//No nodes
		HAS_NONE {
			@Override
			public void addNode(final HyPeerWebSegment web, Node.Listener listener){
				//Use a proxy, if the request came from another segment
				Node first = new Node(0, 0);
				if (web.db != null && !web.db.addNode(first))
					web.changeState(CORRUPT);
				else{
					web.addDistantChild(first);
					//broadcast state change to HAS_ONE
					web.changeState(HAS_ONE);
					listener.callback(first);
				}
			}
			@Override
			public void removeNode(final HyPeerWebSegment web, Node n, Node.Listener listener){
				//Throw an error; this shouldn't happen
				web.changeState(CORRUPT);
			}
		},
		//Only one node
		HAS_ONE {
			@Override
			public void addNode(final HyPeerWebSegment web, Node.Listener listener){
				//Use a proxy, if the request came from another segment
				//broadcast state change to HAS_MANY
				//handle special case
				Node sec = new Node(1, 1),
					first = (Node) web.nodes.firstEntry().getValue();
				//Update the database first
				if (web.db != null) {
					web.db.beginCommit();
					web.db.addNode(sec);
					web.db.setHeight(0, 1);
					//reflexive folds
					web.db.setFold(0, 1);
					web.db.setFold(1, 0);
					//reflexive neighbors
					web.db.addNeighbor(0, 1);
					web.db.addNeighbor(1, 0);
					if (!web.db.endCommit())
						web.changeState(CORRUPT);
				}
				//Update java struct
				{
					first.setHeight(1);
					first.L.setFold(sec);
					sec.L.setFold(first);
					first.L.addNeighbor(sec);
					sec.L.addNeighbor(first);
					web.addDistantChild(sec);
					web.changeState(HAS_MANY);
					listener.callback(sec);					
				}
			}
			@Override
			public void removeNode(final HyPeerWebSegment web, Node n, Node.Listener listener){
				//broadcast state change to HAS_NONE
				//handle special case
				if (web.db != null && !web.db.clear())
					web.changeState(CORRUPT);
				else{
					web.nodes = new TreeMap<>();
					web.changeState(HAS_NONE);
					listener.callback(n);
				}
			}
		},
		//More than one node
		HAS_MANY {
			@Override
			public void addNode(final HyPeerWebSegment web, final Node.Listener listener){
				//Use a proxy, if the request came from another segment
				web.getRandomNode(new Node.Listener() {
					@Override
					public void callback(Node n) {
						Node child = n.findInsertionNode().addChild(web.db, new Node(0,0));
						if (child == null)
							web.changeState(CORRUPT);
						else{
							//Node successfully added!
							//TODO: might need to change this here
							web.addDistantChild(child);
							listener.callback(child);
						}
					}
				});
			}
			@Override
			public void removeNode(final HyPeerWebSegment web, Node n, final Node.Listener listener){
				//If the HyPeerWeb has more than two nodes, remove normally
				int size = web.nodes.size();
				Node last, first = null;
				if (size > 2 ||
					//We can get rid of the rest of these checks if we end
					//up storing proxy nodes in "nodes"
					//Basically, we're trying to find a node with webID > 1 or height > 1
					(last = (Node) web.nodes.lastEntry().getValue()).getWebId() > 1 ||
					//The only nodes left are 0 and 1; check their heights to see if they have children
					last.getHeight() > 1 ||
					(size == 2 && ((Node) web.nodes.firstEntry().getValue()).getHeight() > 1) ||
					//The only other possibility is if we have one node, with a proxy child
					(size == 1 && last.L.getHighestLink().getWebId() > 1))
				{
					//Find a disconnection point
					web.getRandomNode(new Node.Listener(){
						@Override
						public void callback(Node n) {
							Node replace = n.findDisconnectNode().disconnectNode(web.db);
							if (replace == null)
								web.changeState(CORRUPT);
							else{
								//Remove node from list of nodes
								web.nodes.remove(replace.getWebId());
								//Replace the node to be deleted
								if (!n.equals(replace)){
									int newWebID = n.getWebId();
									web.nodes.remove(newWebID);
									web.nodes.put(newWebID, replace);
									if (!replace.replaceNode(web.db, n))
										web.changeState(CORRUPT);
								}
								web.changeState(HAS_MANY);
								listener.callback(n);
							}
						}
					});
				}
				//If the broadcastStateChangeentire HyPeerWeb has only two nodes
				else{
					//removing node 0
					if(n.getWebId() == 0){
						Node replace = n.getFold(); //gets node 1
						if (replace == null)
							web.changeState(CORRUPT);
						//Remove node from list of nodes
						web.nodes.remove(0);
						//Replace the node to be deleted
						replace.L.removeNeighbor(n);
						replace.L.setFold(null);
						replace.setWebID(0);
						replace.setHeight(0);
					}
					//removing node 1
					else{
						Node other = n.getFold();
						if (other == null)
							web.changeState(CORRUPT);
						web.nodes.remove(1);
						other.L.removeNeighbor(n);
						other.L.setFold(null);
						other.setHeight(0);
					}
					web.changeState(HAS_ONE);
				}				
			}
		},
		//Network is corrupt; a segment failed to perform an operation
		CORRUPT {
			@Override
			public void addNode(HyPeerWebSegment web, Node.Listener listener){
				System.err.println("CORRUPT HYPEERWEB");
			}
			@Override
			public void removeNode(HyPeerWebSegment web, Node n, Node.Listener listener){
				System.err.println("CORRUPT HYPEERWEB");
			}
		};
		public abstract void addNode(final HyPeerWebSegment web, Node.Listener listener);
		public abstract void removeNode(final HyPeerWebSegment web, Node n, Node.Listener listener);
	}
	/**
	 * Change the state of the HyPeerWeb
	 * @param state the new state
	 */
	private void changeState(final HyPeerWebState state){
		(new BroadcastVisitor(new Node.Listener(){
			@Override
			public void callback(Node n) {
				((HyPeerWebSegment) n).state = state;
			}
		})).visit(this);
	}
	
	// <editor-fold defaultstate="collapsed" desc="SEGMENT GETTERS">
	/**
	 * Get a cached version of this HyPeerWeb segment
	 * @param networkID the ID for the new cache
	 * @return a node cache object
	 */
	public NodeCache getNodeCache(int networkID){
		NodeCache c = new NodeCache();
		for (Node n: nodes.values())
			c.addNode(n, false);
		return c;
	}
	/**
	 * Gets the first node in the HyPeerWeb
	 * @return node with webID = 0
	 */
	public T getFirstSegmentNode(){
		if (isSegmentEmpty()) return null;
		return (T) nodes.firstEntry().getValue();
	}
	/**
	 * Gets the last node in the HyPeerWeb
	 * @return node with greatest webID
	 */
	public T getLastSegmentNode(){
		if (isSegmentEmpty()) return null;
		return (T) nodes.lastEntry().getValue();
	}
	/**
	 * Get the size of the HyPeerWeb Segment
	 * @return the number of nodes in this particular segment
	 */
	public int getSegmentSize(){
		return nodes.size();
	}
	/**
	 * Is the HyPeerWeb segment empty? (not the entire HyPeerWeb, per se)
	 * @return true if it is empty
	 */
	public boolean isSegmentEmpty(){
		return nodes.isEmpty();
	}
	/**
	 * Looks for a HyPeerWebSegment that is not empty
	 * @return the segment found
	 */
	public HyPeerWebSegment getNonemptySegment(){
		if (isEmpty()) return null;
		if (!isSegmentEmpty()) return this;
		else{
			//TODO: don't think this is going to work here
			for (Node neighbor: L.getNeighbors())
				return ((HyPeerWebSegment)neighbor).getNonemptySegment();
		}
		//For Add Node method. If no segments are nonempty, 
		//this segment is as good a place to start as any.
		return this;
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="HYPEERWEB GETTERS">
	/**
	 * Retrieves a random node in the HyPeerWeb
	 * @return a random node; null, if there are no nodes
	 */
	public void getRandomNode(Node.Listener listener){
		getNode(rand.nextInt(Integer.MAX_VALUE), true, listener);
	}
	/**
	 * Retrieve a node with the specified webid
	 * @param webId the id of the node to retrieve
	 * @param approximate should we get the exact node with webID, or just the
	 * closest node to that webID
	 * @param listener retrieval callback
	 * @author isaac
	 */
	public void getNode(int webId, boolean approximate, Node.Listener listener){
		//There are no nodes; stop execution
		if (isEmpty()) listener.callback(null);
		//Delegate this method to a segment that actually has nodes
		if (isSegmentEmpty())
			getNonemptySegment().getNode(webId, approximate, listener);
		else{
			Node n = nodes.get(webId);
			//If this segment has this node
			if (n != null)
				listener.callback(n);
			//Otherwise, use send-visitor to get the node
			else{
				SendVisitor visitor = new SendVisitor(webId, approximate, listener);
				visitor.visit(getFirstSegmentNode());
			}
		}
	}
	/**
	 * Is the HyPeerWeb empty? (the entire HyPeerWeb, not just a segment)
	 * @return true if it is empty
	 */
	public boolean isEmpty(){
		return state == HyPeerWebState.HAS_NONE;
	}
	// </editor-fold>
}
