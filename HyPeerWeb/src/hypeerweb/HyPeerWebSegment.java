package hypeerweb;

import hypeerweb.visitors.SendVisitor;
import hypeerweb.visitors.BroadcastVisitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.TreeMap;

/**
 * The Great HyPeerWeb
 * @param <>> The Node type for this HyPeerWeb instance
 */
public class HyPeerWebSegment<T extends Node> extends Node{
	private TreeMap<Integer, Node> nodes;
	private HyPeerWebState state;
	//Random number generator for getting random nodes
	private static final Random rand = new Random();
	//Static list of all HWSegments in this process; they may not correspond to the same HyPeerWeb
	//This is used by NodeProxy to read-resolve
	public static ArrayList<HyPeerWebSegment> segmentList = new ArrayList();
	private TreeMap<Integer, Node> nodesByUID;
	
	/**
	 * Constructor for initializing the HyPeerWeb with default Node values
	 * @param seed the random seed number for getting random nodes; use -1
	 *	to get a pseudo-random seed
	 */
	public HyPeerWebSegment(long seed){
		this(seed, 0, 0);
	}
	/**
	 * Constructor for initializing the HyPeerWeb with defined Node values
	 * @param seed the random seed number for getting random nodes; use -1
	 *	to get a pseudo-random seed
	 * @param webID the node webID, if it has one
	 * @param height the node height, if it has one
	 */
	public HyPeerWebSegment(long seed, int webID, int height){
		super(0, 0);
		nodes = new TreeMap();
		nodesByUID = new TreeMap();
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
		//Don't do anything, if there are no nodes
		if (isEmpty())
			listener.callback(null);
		//If this segment is empty, execute method on non-empty segment
		else if (isSegmentEmpty())
			getNonemptySegment().removeNode(webid, listener);
		//If this segment has nodes, go find the node "webid"
		else{
			//TODO, fix this here
			Node n = nodes.get(webid);
			if (n != null)
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
		//TODO, if node is a proxy, execute this method on the proxy's host segment
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
				((HyPeerWebSegment) n).removeAllSegmentNodes();
				listener.callback(n);
			}
		})).visit(this);
	}
	/**
	 * Remove all node's from this particular segment
	 * Warning! This may leave the HyPeerWeb corrupt, if
	 * all segments are not cleared together
	 */
	protected void removeAllSegmentNodes(){
		nodes.clear();
		nodesByUID.clear();
	}
	
	/**
	 * Adds a new node to the HyPeerWeb
	 * @param listener event callback
	 */
	public void addNode(Node.Listener listener){
		//If there is a nonempty segment somewhere, go to it
		if (!isEmpty() && isSegmentEmpty())
			getNonemptySegment().addNode(listener);
		//Otherwise, run on this machine
		else state.addNode(this, listener);
	}
	protected void addDistantChild(Node child){
		nodes.put(child.getWebId(), child);
		nodesByUID.put(child.UID, child);
	}
	/**
	 * Holds the state of the entire HyPeerWeb, not just
	 * this individual segment. Handles special cases for
	 * add and remove node, as well as a corrupt HyPeerWeb.
	 */
	protected enum HyPeerWebState{
		//No nodes
		HAS_NONE {
			@Override
			public void addNode(final HyPeerWebSegment web, Node.Listener listener){
				//Use a proxy, if the request came from another segment
				Node first = new Node(0, 0);
				web.addDistantChild(first);
				//broadcast state change to HAS_ONE
				web.changeState(HAS_ONE);
				listener.callback(first);
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
				Node sec = new Node(1, 1),
					first = (Node) web.nodes.firstEntry().getValue();
				//Handle special case
				first.setHeight(1);
				first.L.setFold(sec);
				sec.L.setFold(first);
				first.L.addNeighbor(sec);
				sec.L.addNeighbor(first);
				web.addDistantChild(sec);
				//Broadcast state change
				web.changeState(HAS_MANY);
				listener.callback(sec);
			}
			@Override
			public void removeNode(final HyPeerWebSegment web, Node n, Node.Listener listener){
				//broadcast state change to HAS_NONE
				//handle special case
				web.removeAllSegmentNodes();
				web.nodes = new TreeMap<>();
				web.changeState(HAS_NONE);
				listener.callback(n);
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
	protected void changeState(final HyPeerWebState state){
		(new BroadcastVisitor(new Node.Listener(){
			@Override
			public void callback(Node n){
				((HyPeerWebSegment) n).state = state;
			}
		})).begin(this);
	}
	
	// <editor-fold defaultstate="collapsed" desc="SEGMENT GETTERS">
	/**
	 * Get a cached version of this HyPeerWeb segment
	 * @param networkID the ID for the new cache
	 * @return a node cache object
	 */
	public NodeCache getSegmentNodeCache(int networkID){
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
		//There are no non-empty segments
		if (isEmpty()) return null;
		//Hooray, this is a non-empty segment
		if (!isSegmentEmpty()) return this;
		else{
			//Recursively look through all neighbors, searching for a node
			//that is not empty; this is terribly inefficient, but we don't
			//know a better way to do it (at least not yet)
			HashSet<Node> visited = new HashSet();
			ArrayList<Node> parents = new ArrayList();
			HashSet<Node> friends = new HashSet();
			parents.add(this);
			visited.add(this);
			for (Node p: parents){
				friends.addAll(Arrays.asList(p.getNeighbors()));
				for (Node f: friends){
					if (!visited.c)
						//TODO, finish writing this method
				}
			}
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
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="HYPEERWEB GETTERS">
	/**
	 * Retrieves a random node in the HyPeerWeb
	 * @param listener retrieval callback
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
