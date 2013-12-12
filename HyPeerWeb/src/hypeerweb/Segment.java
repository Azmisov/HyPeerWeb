package hypeerweb;

import communicator.Communicator;
import communicator.NodeListener;
import hypeerweb.visitors.SendVisitor;
import hypeerweb.visitors.BroadcastVisitor;
import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Great HyPeerWeb
 * @param <T> The Node type for this HyPeerWeb instance
 */
public class Segment<T extends Node> extends Node{
	public static final String className = Segment.class.getName();
	//HyPeerWebSegment attributes
	protected final TreeMap<Integer, T> nodes, nodesByUID;
	public HyPeerWebState
		state = HyPeerWebState.HAS_NONE,
		inceptionState = HyPeerWebState.HAS_ONE;
	private boolean isInceptionWeb = false;
	//Random number generator for getting random nodes
	private static final Random rand = new Random();
	//Static list of all HWSegments in this JVM; they may not correspond to the same HyPeerWeb
	public static final ArrayList<Segment> segmentList = new ArrayList();
	//Segment settings
	protected final String dbname;
	protected final long seed;
	
	/**
	 * Constructor for initializing the HyPeerWeb with default Node values
	 * @param dbname filename for the database/node-cache
	 * @param seed the random seed number for getting random nodes; use -1
	 *	to get a pseudo-random seed
	 */
	protected Segment(String dbname, long seed){
		this(dbname, seed, 0, 0);
	}
	/**
	 * Constructor for initializing the HyPeerWeb with defined Node values
	 * @param dbname filename for the database/node-cache
	 * @param seed the random seed number for getting random nodes; use -1
	 *	to get a pseudo-random seed
	 * @param webID the node webID, if it has one
	 * @param height the node height, if it has one
	 */
	protected Segment(String dbname, long seed, int webID, int height){
		super(webID, height);
		this.dbname = dbname;
		this.seed = 2;
		nodes = new TreeMap();
		nodesByUID = new TreeMap();
		if (seed != -1)
			rand.setSeed(seed);
	}
	
	//SEGMENT OPS
	/**
	 * Creates a new segment
	 * @param dbname filename for the database/node-cache
	 * @param seed the random seed number for getting random nodes; use -1
	 *	to get a pseudo-random seed
	 */
	public static <K extends Node> Segment newSegment(String dbname, long seed){
		Segment<K> seg = new Segment(dbname, seed);
		segmentList.add(seg);
		return seg;
	}
	/**
	 * Adds a segment to the HyPeerWeb, using a pre-initialized Segment;
	 * Note: webID, height, Links (L), state, and inceptionState will be altered;
	 * all other attributes will remain the same however
	 * @param segment the pre-initialized segment
	 * @param listener add segment callback; this will execute on the machine
	 * the "segment" is on, unless remote execution is enabled
	 */
	public void addSegment(Segment<T> segment, NodeListener listener){
		//Create a temporary segment container
		Segment<Segment<T>> inceptionweb = new Segment(null, seed);
		inceptionweb.state = inceptionState;
		inceptionweb.isInceptionWeb = true;
		inceptionweb.nodes.put(this.webID, this);
		inceptionweb.nodesByUID.put(this.UID, this);
		//Temporarily add it to the segment list (so addNode can resolve the host)
		
		//The only extra data we need to initialize is the state
		segment.executeRemotely(new NodeListener(
			className, "_inheritState",
			new String[]{HyPeerWebState.className},
			new Object[]{state}
		));
		//Now run the add operation
		inceptionState.addNode(inceptionweb, segment, listener);
	}
	/**
	 * Removes a segment from the HyPeerWeb
	 * @param segment the segment to be removed
	 * @param listener remove segment callback; this will execute on the machine
	 * that removed segment is on, unless remote execution is enabled
	 */
	public void removeSegment(NodeListener listener){
		Segment<Segment<T>> inceptionweb = new Segment(null, seed);
		inceptionweb.state = inceptionState;
		inceptionweb.isInceptionWeb = true;
		inceptionweb.nodes.put(this.webID, this);
		inceptionweb.nodesByUID.put(this.UID, this);
		
		inceptionState.removeNode(inceptionweb, this, listener);
	}
	
	//ADD & REMOVE NODE
	/**
	 * Removes the node of specified webid
	 * @param webid the webid of the node to remove
	 * @param listener the removal callback; this will execute on the machine
	 * that the removed node is on, unless remote execution is enabled
	 */
	public void removeNode(int webid, NodeListener listener){
		//Execute removeNode on the segment that contains "webid"
		getNode(webid, false, new NodeListener(
			className, "_removeNode", listener
		));
	}
	/**
	 * Removes the node
	 * @param node the node to remove
	 * @param listener event callback; this will execute on the machine
	 * that the removed node is on, unless remote execution is enabled
	 */
	public void removeNode(T node, NodeListener listener){
		//Get the segment that has this node and execute the method there
		node.executeRemotely(new NodeListener(
			className, "_removeNode", listener
		));
	}
	protected static void _removeNode(Node n, NodeListener listener){
		Segment host;
		//This node doesn't exist or isn't attached to a Segment
		if (n == null || (host = n.getHostSegment()) == null)
			listener.callback(null, null, -1);
		//Get the host segment to run removeNode on
		else host.state.removeNode(host, n, listener);
	}
	/**
	 * Removes all nodes from HyPeerWeb
	 * @param listener event callback; executed on the each segment, unless
	 * remote execution is enabled
	 */
	public void removeAllNodes(NodeListener listener){
		(new BroadcastVisitor(new NodeListener(
			className, "_removeAllNodes", listener
		))).visit(this);
	}
	protected static void _removeAllNodes(Node n, NodeListener listener){
		Segment seg = (Segment) n;
		//Clear node lists
		seg.state = HyPeerWebState.HAS_NONE;
		seg.nodes.clear();
		seg.nodesByUID.clear();
		if (listener != null)
			listener.callback(n);
	}
	/**
	 * Adds a node to the HyPeerWeb, using a pre-initialized Node;
	 * Note: webID, height, and Links (L) will be altered; all other
	 * attributes will remain the same however
	 * @param node a pre-initialized Node; this is the only method that requires the
	 * input "node" be on same machine as the Segment
	 * @param listener add node callback; this will execute on the machine
	 * that "node" is on, unless remote execution is enabled
	 */
	public void addNode(T node, NodeListener listener){
		assert(node.getAddress().equals(Communicator.getAddress()));
		//Add node to UID list, so proxies can be resolved during the add process
		nodesByUID.put(node.UID, node);
		//The HyPeerWeb's state will handle everything else
		// (e.g. finding a nonempty segment to add from)
		state.addNode(this, node, listener);
	}
	
	//HYPEERWEB STATE
	/**
	 * Holds the state of the entire HyPeerWeb, not just
	 * this individual segment. Handles special cases for
	 * add and remove node, as well as a corrupt HyPeerWeb.
	 */
	public enum HyPeerWebState{
		//No nodes
		HAS_NONE {
			@Override
			public void addNode(Segment web, Node n, NodeListener listener){
				web.changeState(HAS_ONE);
				//Add to the current segment
				//execution namespace, web, and n, are all on the same machine
				n.resetLinks();
				n.setWebID(0);
				n.setHeight(0);
				web.nodes.put(0, n);
				//callback
				if (listener != null)
					listener.callback(n);
			}
			@Override
			public void removeNode(Segment web, Node n, NodeListener listener){
				//Throw an error; this shouldn't happen
				web.changeState(CORRUPT);
				//callback
				if (listener != null)
					listener.callback(null);
			}
		},
		//Only one node
		HAS_ONE {
			@Override
			public void addNode(Segment web, Node n, NodeListener listener){
				web.changeState(HAS_MANY);
				//Go get the segment that contains the first node, we'll start editing from there
				web.getNode(0, false, new NodeListener(
					Node.className, "_ONE_add_zero",
					new String[]{Node.className, NodeListener.className},
					new Object[]{n, listener}
				));
			}
			@Override
			public void removeNode(Segment web, Node n, NodeListener listener){
				//broadcast state change to HAS_NONE
				web.changeState(HAS_NONE);
				//only node left; both n and web will be on this machine
				web.nodes.clear();
				web.nodesByUID.clear();
				//callback
				if (listener != null)
					listener.callback(n, null, -1);
			}
		},
		//More than one node
		HAS_MANY {
			@Override
			public void addNode(Segment web, Node n, NodeListener listener){
				//Find a random node to start insertion search from
				web.getRandomNode(new NodeListener(
					Node.className, "_MANY_add_random",
					new String[]{Node.className, NodeListener.className},
					new Object[]{n, listener}
				));
			}
			@Override
			public void removeNode(Segment web, Node n, NodeListener listener){
				//If the HyPeerWeb has more than two nodes, remove normally
				int size = web.getSegmentSize();
				Node last;
				if (size > 2 ||
					//Basically, we're trying to find a node with webID > 1 or height > 1
					(last = (Node) web.getLastSegmentNode()).getWebId() > 1 ||
					//The only nodes left are 0 and 1; check their heights to see if they have children
					last.getHeight() > 1 ||
					(size == 2 && ((Node) web.getFirstSegmentNode()).getHeight() > 1) ||
					//The only other possibility is if we have one node, with a proxy child
					//Always execute this last, to avoid network communication if at all possible
					(size == 1 && last.L.getHighestLink().getWebId() > 1))
				{
					//Get a random node to start a disconnect search from
					web.getRandomNode(new NodeListener(
						Node.className, "_MANY_remove_random",
						new String[]{Node.className, NodeListener.className},
						new Object[]{n, listener}
					));
				}
				//If the entire HyPeerWeb has only two nodes
				else{
					web.changeState(HAS_ONE);
					//Make the other node be the HAS_ONE node, first
					//Afterwards, we'll remove the node from the node-maps
					n.L.getFold().executeRemotely(new NodeListener(
						Node.className, "_TWO_remove",
						new String[]{Node.className, NodeListener.className},
						new Object[]{n, listener}
					));
				}		
			}
		},
		//Network is corrupt; a segment failed to perform an operation
		CORRUPT {
			@Override
			public void addNode(Segment web, Node n, NodeListener listener){
				System.err.println("CORRUPT HYPEERWEB");
			}
			@Override
			public void removeNode(Segment web, Node n, NodeListener listener){
				System.err.println("CORRUPT HYPEERWEB");
			}
		};

		public static final String className = HyPeerWebState.class.getName();
		/**
		 * Add a node to the HyPeerWeb; we guarantee "n" will be on the same segment as "web"
		 * @param web the Segment
		 * @param n the node to remove
		 * @param listener removal callback
		 */
		public abstract void addNode(Segment web, Node n, NodeListener listener);
		/**
		 * Remove a node from the HyPeerWeb; we guarantee "n" will be on the same segment as "web"
		 * @param web the Segment
		 * @param n the node to remove
		 * @param listener removal callback
		 */
		public abstract void removeNode(Segment web, Node n, NodeListener listener);
	}
	/**
	 * Change the state of the HyPeerWeb
	 * @param state the new state
	 */
	protected void changeState(final HyPeerWebState state){
		(new BroadcastVisitor(new NodeListener(
			className, "_changeState",
			new String[]{HyPeerWebState.className},
			new Object[]{state}
		))).visit(this);
	}
	protected static void _changeState(Node n, HyPeerWebState state){
		Segment seg = (Segment) n;
		seg.state = state;
		//InceptionWeb will always have at least one node
		if (seg.isInceptionWeb){
			//changing the state for the first node will suffice
			if (state == HyPeerWebState.HAS_MANY || state == HyPeerWebState.HAS_ONE)
				((Segment) seg.getFirstSegmentNode()).inceptionState = state;
			//Corrupt state changes need to be broadcasted
			else if (state == HyPeerWebState.CORRUPT){
				(new BroadcastVisitor(new NodeListener(
					className, "_changeInceptionState",
					new String[]{HyPeerWebState.className},
					new Object[]{state}
				))).visit(seg.getFirstSegmentNode());
			}
		}
	}
	protected static void _changeInceptionState(Node n, HyPeerWebState state){
		((Segment) n).inceptionState = HyPeerWebState.HAS_MANY;
	}
	protected static void _inheritState(Node n, HyPeerWebState state){
		Segment seg = (Segment) n;
		seg.state = state;
		seg.inceptionState = HyPeerWebState.HAS_MANY;
	}
	
	//SEGMENT GETTERS
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
	 * Looks for a node with this UID in this segment
	 * @param UID the UID of the node to search for
	 * @return the node with this UID; null, if it doesn't exist
	 */
	public T getSegmentNodeByUID(int UID) {
		return (T) nodesByUID.get(UID);
	}
	
	//HYPEERWEB GETTERS
	/**
	 * Retrieves a random node in the HyPeerWeb
	 * @param listener retrieval callback
	 */
	public void getRandomNode(NodeListener listener){
		getNode(rand.nextInt(Integer.MAX_VALUE), true, listener);
	}
	/**
	 * Retrieve a node with the specified webid
	 * @param webId the id of the node to retrieve
	 * @param approximate should we get the exact node with webID, or just the
	 * closest node to that webID
	 * @param listener retrieval callback
	 */
	public void getNode(int webId, boolean approximate, NodeListener listener){
		//There are no nodes; stop execution
		if (isEmpty())
			listener.callback(null);
		//Delegate this method to a segment that actually has nodes
		else if (isSegmentEmpty())
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
	 * Retrieves the segment with the specified webid
	 * @param webId the id of the segment to retrieve
	 * @param approximate should we get the exact segment with webID, or just the
	 * closest segment to that webID
	 * @param listener retrieval callback
	 */
	public void getSegment(int webId, boolean approximate, NodeListener listener){
		if (this.webID == webId)
			listener.callback(this);
		else{
			SendVisitor visitor = new SendVisitor(webId, approximate, listener);
			visitor.visit(this);
		}
	}
	/**
	 * Looks for a Segment that is not empty
	 * @return the segment found
	 */
	public Segment getNonemptySegment(){
		//There are no non-empty segments
		if (isEmpty()) return null;
		else{
			//Recursively look through all neighbors, searching for a node
			//that is not empty; this is terribly inefficient, but we don't
			//know a better way to do it (at least not yet)
			//findValidNode will always check current node first
			return (Segment) findValidNode(Criteria.Type.NONEMPTY, -1, false);
		}
	}
	/**
	 * Is the HyPeerWeb empty? (the entire HyPeerWeb, not just a segment)
	 * @return true if it is empty
	 */
	public boolean isEmpty(){
		return state == HyPeerWebState.HAS_NONE;
	}
	
	//CACHE & DATABASE
	/**
	 * Get a cached version of this HyPeerWeb segment
	 * @param networkID the ID for the new cache
	 * @return a node cache object
	 */
	public SegmentCache getCache(){
		SegmentCache c = new SegmentCache();
		for (Node n: nodes.values())
			c.addNode(n, false);
		return c;
	}
	/**
	 * Fetch a list of cached nodes
	 * @param fetch the list of webID's to cache
	 * @return a list of cached nodes
	 */
	public NodeCache[] getCache(int[] fetch){
		SegmentCache temp = new SegmentCache();
		for (int id: fetch){
			Node n = nodes.get(id);
			if (n != null)
				temp.addNode(n, false);
		}
		return temp.nodes.values().toArray(new NodeCache[temp.nodes.size()]);
	}
	public SegmentDB getDatabase(){
		SegmentDB db = new SegmentDB();
		db.store((Collection<Node>) nodes.values());
		return db;
	}
	public void store(){
		System.out.println("Storing in database");
		getDatabase().save(this);
	}
	public void restore() throws Exception{
		//TODO: NOT IMPLEMENTED
	}
	
	//CLASS OVERRIDES
	@Override
	public void setWriteRealNode(boolean writeRealNode) {
		this.writeRealNode = writeRealNode;
		for(Node n : nodes.values()){
			n.setWriteRealNode(writeRealNode);
		}
	}
	@Override
	public Object writeReplace() throws ObjectStreamException {
		if(writeRealNode){
			setWriteRealNode(false);
			return this;
		}
		return new SegmentProxy(this);
	}
	@Override
	public Object readResolve() throws ObjectStreamException {
		return this;
	}
}
