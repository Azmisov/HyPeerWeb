package hypeerweb;

import communicator.NodeListener;
import hypeerweb.visitors.SendVisitor;
import hypeerweb.visitors.BroadcastVisitor;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

/**
 * The Great HyPeerWeb
 * @param <T> The Node type for this HyPeerWeb instance
 */
public class HyPeerWebSegment<T extends Node> extends Node{
	public static final String className = HyPeerWebSegment.class.getName();
	//HyPeerWebSegment attributes
	protected final TreeMap<Integer, T> nodes;
	private final TreeMap<Integer, T> nodesByUID;
	public HyPeerWebState
		state = HyPeerWebState.HAS_NONE,
		inceptionState = HyPeerWebState.HAS_ONE;
	private boolean isInceptionWeb = false;
	//Random number generator for getting random nodes
	private static final Random rand = new Random();
	//Static list of all HWSegments in this JVM; they may not correspond to the same HyPeerWeb
	public static final ArrayList<HyPeerWebSegment> segmentList = new ArrayList();
	//Segment settings
	protected final transient String dbname;
	protected final transient long seed;
	
	/**
	 * Constructor for initializing the HyPeerWeb with default Node values
	 * @param dbname filename for the database/node-cache
	 * @param seed the random seed number for getting random nodes; use -1
	 *	to get a pseudo-random seed
	 */
	public HyPeerWebSegment(String dbname, long seed){
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
	public HyPeerWebSegment(String dbname, long seed, int webID, int height){
		super(webID, height);
		this.dbname = dbname;
		this.seed = seed;
		nodes = new TreeMap();
		nodesByUID = new TreeMap();
		if (seed != -1)
			rand.setSeed(seed);
		segmentList.add(this);
	}
	
	//ADD & REMOVE NODE
	/**
	 * Removes the node of specified webid
	 * @param webid the webid of the node to remove
	 * @param listener the removal callback
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
	 * @param listener event callback
	 */
	public void removeNode(T node, NodeListener listener){
		//Get the segment that has this node and execute the method there
		node.executeRemotely(new NodeListener(
			className, "_removeNode", listener
		));
	}
	protected static void _removeNode(Node n, NodeListener listener){
		HyPeerWebSegment host;
		//This node doesn't exist or isn't attached to a HyPeerWebSegment
		if (n == null || (host = n.getHostSegment()) == null)
			listener.callback(null);
		//Get the host segment to run removeNode on
		else host.state.removeNode(host, n, listener);
	}
	/**
	 * Removes all nodes from HyPeerWeb
	 * Warning! This may leave the HyPeerWeb corrupt, if
	 * all segments are not cleared together
	 * @param listener event callback
	 */
	public void removeAllNodes(NodeListener listener){
		(new BroadcastVisitor(new NodeListener(
			className, "_removeAllNodes", listener
		))).visit(this);
	}
	protected static void _removeAllNodes(Node n, NodeListener listener){
		HyPeerWebSegment seg = (HyPeerWebSegment) n;
		//Clear node lists
		seg.nodes.clear();
		seg.nodesByUID.clear();
		listener.callback(n);
	}
	/**
	 * Adds a node to the HyPeerWeb, using a pre-initialized Node;
	 * Note: webID, height, and Links (L) will be altered; all other
	 * attributes will remain the same, however
	 * @param node a pre-initialized Node
	 * @param listener add node callback
	 */
	public void addNode(T node, NodeListener listener){
		//Add node to UID list, so proxies can be resolved during the add process
		nodesByUID.put(node.UID, node);
		//The HyPeerWeb's state will handle everything else
		state.addNode(this, node, listener);
	}
	/**
	 * Adds a segment to the HyPeerWeb, using a pre-initialized Segment;
	 * Note: webID, height, Links (L), state, and inceptionState will be altered;
	 * all other attributes will remain the same however
	 * @param segment the pre-initialized segment
	 * @param listener add segment callback
	 */
	public void addSegment(HyPeerWebSegment<T> segment, NodeListener listener){
		//Create a temporary segment container
		HyPeerWebSegment<HyPeerWebSegment<T>> inceptionweb = new HyPeerWebSegment(null, seed);
		inceptionweb.state = inceptionState;
		inceptionweb.isInceptionWeb = true;
		inceptionweb.nodes.put(this.webID, this);
		inceptionweb.nodesByUID.put(this.UID, this);
		//The only extra data we need to initialize is the state
		segment.executeRemotely(new NodeListener(
			HyPeerWebSegment.className, "_inheritState",
			new String[]{HyPeerWebState.className},
			new Object[]{state}
		));
		//Change the inception's state, since we're adding another node
		inceptionState = HyPeerWebState.HAS_MANY;
		//Now run the add operation
		inceptionweb.addNode(segment, listener);
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
			public void addNode(HyPeerWebSegment web, Node n, NodeListener listener){
				//When the entire HyPeerWeb is empty, we can guarantee that
				//we will be executing on n's machine (e.g. web is not a proxy)
				n.resetLinks();
				n.setWebID(0);
				n.setHeight(0);
				web.nodes.put(0, n);
				//broadcast state change to HAS_ONE
				web.changeState(HAS_ONE);
				//run callback
				if (listener != null)
					listener.callback(n);
			}
			@Override
			public void removeNode(HyPeerWebSegment web, Node n, NodeListener listener){
				//Throw an error; this shouldn't happen
				web.changeState(CORRUPT);
				if (listener != null)
					listener.callback(null);
			}
		},
		//Only one node
		HAS_ONE {
			@Override
			public void addNode(HyPeerWebSegment web, Node sec, NodeListener listener){
				Node first = web.getFirstSegmentNode();
				//Always modify heights before you start changing links
				//Doing so will result in less network communications
				first.setHeight(1);
				sec.executeRemotely(new NodeListener(
					Node.className, "_ONE_editSecondNode",
					new String[]{Node.className, NodeListener.className},
					new Object[]{first, listener}
				));
			}
			@Override
			public void removeNode(HyPeerWebSegment web, Node n, NodeListener listener){
				//only node left; both n and web will be on this machine
				web.nodes.clear();
				web.nodesByUID.clear();
				//broadcast state change to HAS_NONE
				web.changeState(HAS_NONE);
				if (listener != null)
					listener.callback(n);
			}
		},
		//More than one node
		HAS_MANY {
			@Override
			public void addNode(HyPeerWebSegment web, Node n, NodeListener listener){
				//Find a random node to start insertion search from
				System.out.println("HAS_MANY executing");
				web.getRandomNode(new NodeListener(
					Node.className, "_MANY_add_random",
					new String[]{Node.className, NodeListener.className},
					new Object[]{n, listener}
				));
			}
			@Override
			public void removeNode(HyPeerWebSegment web, Node n, NodeListener listener){
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
					//removing node 0
					if (n.getWebId() == 0){
						Node replace = n.L.getFold(); //gets node 1
						if (replace == null)
							web.changeState(CORRUPT);
						//Remove node from list of nodes
						web.nodes.remove(0);
						//Replace the node to be deleted
						replace.executeRemotely(listener);
						replace.L.removeNeighbor(n);
						replace.L.setFold(null);
						replace.setWebID(0);
						replace.setHeight(0);
					}
					//removing node 1
					else{
						Node other = n.L.getFold();
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
			public void addNode(HyPeerWebSegment web, Node n, NodeListener listener){
				System.err.println("CORRUPT HYPEERWEB");
			}
			@Override
			public void removeNode(HyPeerWebSegment web, Node n, NodeListener listener){
				System.err.println("CORRUPT HYPEERWEB");
			}
		};

		public static final String className = HyPeerWebState.class.getName();
		/**
		 * Add a node to the HyPeerWeb; we guarantee "n" will be on the same segment as "web"
		 * @param web the HyPeerWebSegment
		 * @param n the node to remove
		 * @param listener removal callback
		 */
		public abstract void addNode(HyPeerWebSegment web, Node n, NodeListener listener);
		/**
		 * Remove a node from the HyPeerWeb; we guarantee "n" will be on the same segment as "web"
		 * @param web the HyPeerWebSegment
		 * @param n the node to remove
		 * @param listener removal callback
		 */
		public abstract void removeNode(HyPeerWebSegment web, Node n, NodeListener listener);
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
		HyPeerWebSegment seg = (HyPeerWebSegment) n;
		seg.state = state;
		//InceptionWeb will always have at least one node
		if (seg.isInceptionWeb){
			//changing the state for the first node will suffice
			if (state == HyPeerWebState.HAS_MANY || state == HyPeerWebState.HAS_ONE)
				((HyPeerWebSegment) seg.getFirstSegmentNode()).state = state;
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
	protected static void _inheritState(Node n, HyPeerWebState state){
		HyPeerWebSegment seg = (HyPeerWebSegment) n;
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
	 * Criteria for a non-empty HyPeerWeb segment
	 */
	private static final Criteria nonemptyCriteria = new Criteria(){
		@Override
		public Node check(Node origin, Node friend) {
			return ((HyPeerWebSegment) friend).isSegmentEmpty() ? null : friend;
		}
	};
	/**
	 * Looks for a Segment that is not empty
	 * @return the segment found
	 */
	public HyPeerWebSegment getNonemptySegment(){
		//There are no non-empty segments
		if (isEmpty()) return null;
		else{
			//Recursively look through all neighbors, searching for a node
			//that is not empty; this is terribly inefficient, but we don't
			//know a better way to do it (at least not yet)
			//findValidNode will always check current node first
			return (HyPeerWebSegment) findValidNode(nonemptyCriteria, -1, false);
		}
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
	public NodeCache getCache(int networkID){
		NodeCache c = new NodeCache();
		for (Node n: nodes.values())
			c.addNode(n, false);
		return c;
	}
	public void store() throws Exception{
		//NOT IMPLEMENTED
	}
	public void restore() throws Exception{
		//NOT IMPLEMENTED
	}
	
	//CLASS OVERRIDES
	@Override
	public Object writeReplace() throws ObjectStreamException {
		return new HyPeerWebSegmentProxy(this);
	}
	@Override
	public Object readResolve() throws ObjectStreamException {
		return this;
	}
}
