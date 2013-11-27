package hypeerweb;

import hypeerweb.visitors.SendVisitor;
import hypeerweb.visitors.BroadcastVisitor;
import java.util.ArrayList;
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
	//Database name
	private final String dbname;
	
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
		super(0, 0);
		this.dbname = dbname;
		nodes = new TreeMap();
		nodesByUID = new TreeMap();
		if (seed != -1)
			rand.setSeed(seed);
		segmentList.add(this);
	}
	
	/**
	 * Removes the node of specified webid
	 * @param webid the webid of the node to remove
	 * @return the node that was removed
	 */
	public Node removeNode(int webid){
		//Execute removeNode on the segment that contains "webid"
		getNode(webid, false, new Node.Listener(){
			@Override
			public void callback(Node n) {
				//This node doesn't exist
				if (n == null)
					listener.callback(null);
				//Get the host segment to run removeNode on
				else{
					HyPeerWebSegment host = n.getHostSegment();
					host.state.removeNode(host, n, listener);
				}
			}
		});
	}
	/**
	 * Removes the node
	 * @param node the node to remove
	 * @param listener event callback
	 */
	public void removeNode(T node, Node.Listener listener){
		//Get the segment that has this node
		HyPeerWebSegment host = node.getHostSegment();
		//Current segment has this node; we're free to remove
		if (host == this)
			state.removeNode(this, node, listener);
		else{
			//If the segment is remote, delegate removal to that segment
			host.removeNode(node, listener);
		}
	}
	/**
	 * Removes all nodes from HyPeerWeb
	 * Warning! This may leave the HyPeerWeb corrupt, if
	 * all segments are not cleared together
	 * @param listener event callback
	 */
	public void removeAllNodes(final Node.Listener listener){
		(new BroadcastVisitor(new Node.Listener(){
			@Override
			public void callback(Node n){
				HyPeerWebSegment seg = (HyPeerWebSegment) n;
				//Clear node lists
				seg.nodes.clear();
				seg.nodesByUID.clear();
				listener.callback(n);
			}
		})).visit(this);
	}
	
	/**
	 * Adds a new node to the HyPeerWeb
	 * @param listener event callback
	 */
	public void addNode(Node.Listener listener){
		//We want the node to be added to this segment.
		//Since we might not have nodes ourselves, we may not be able
		//to compute links/connections; so we'll give the remote segment a
		//node-proxy to update for us
		Node proxy = new Node(0, 0);
		addNode(proxy, listener);
	}
	/**
	 * Adds a node to the HyPeerWeb, using a pre-initialized Node;
	 * Note: webID, height, and Links (L) will be altered; all other
	 * attributes will remain the same, however
	 * @param proxy a pre-initialized Node
	 * @param listener add node callback
	 */
	public void addNode(Node proxy, Node.Listener listener){
		//Add node to UID list, so the proxy can be resolved
		nodesByUID.put(proxy.UID, proxy);
		//If there is a nonempty segment somewhere, go to it
		if (!isEmpty() && isSegmentEmpty())
			getNonemptySegment().addNode(proxy, listener);
		//Otherwise, run on this machine
		else state.addNode(this, proxy, listener);
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
			public void addNode(HyPeerWebSegment web, Node n, Node.Listener listener){
				//When the entire HyPeerWeb is empty, we can guarantee that
				//we will be executing on n's machine (e.g. web is not a proxy)
				n = new Node(0, 0);
				web.nodes.put(0, n);
				//broadcast state change to HAS_ONE
				web.changeState(HAS_ONE);
				//run callback
				listener.callback(n);
			}
			@Override
			public void removeNode(HyPeerWebSegment web, Node n, Node.Listener listener){
				//Throw an error; this shouldn't happen
				web.changeState(CORRUPT);
				listener.callback(null);
			}
		},
		//Only one node
		HAS_ONE {
			@Override
			public void addNode(final HyPeerWebSegment web, final Node sec, final Node.Listener listener){
				final Node first = web.getFirstSegmentNode();
				//Always modify heights before you start changing links
				//Doing so will result in less network communications
				first.setHeight(1);
				sec.executeRemotely(new Node.Listener(){
					@Override
					public void callback(Node n){
						//Update data for the new node
						n.setHeight(1);
						n.setWebID(1);
						n.resetLinks();
						n.L.setFold(first);
						n.L.addNeighbor(first);
						n.getHostSegment().nodes.put(1, n);
						//Update data for the first node
						first.executeRemotely(new Node.Listener(){
							@Override
							public void callback(Node n){								
								n.L.setFold(sec);
								n.L.addNeighbor(sec);
								//Broadcast state change and execute callback
								web.changeState(HAS_MANY);
								listener.callback(sec);
							}
						});
					}
				});
			}
			@Override
			public void removeNode(HyPeerWebSegment web, Node n, Node.Listener listener){
				//only node left; both n and web will be on this machine
				web.nodes.clear();
				web.nodesByUID.clear();
				//broadcast state change to HAS_NONE
				web.changeState(HAS_NONE);
				listener.callback(n);
			}
		},
		//More than one node
		HAS_MANY {
			@Override
			public void addNode(HyPeerWebSegment web, final Node n, final Node.Listener listener){
				//Find a random node to start insertion
				web.getRandomNode(new Node.Listener(){
					@Override
					public void callback(Node ranNode){
						//Find a valid insertion point and add the child
						ranNode.findInsertionNode().addChild(n, new Node.Listener(){
							@Override
							public void callback(final Node insertNode){
								//Node has been successfully updated
								insertNode.getHostSegment().executeRemotely(new Node.Listener() {
									@Override
									public void callback(Node seg){
										//Add to the host's node list
										((HyPeerWebSegment) seg).nodes.put(n.getWebId(), n);
										listener.callback(n);
									}
								});
							}
						});
					}
				});
			}
			@Override
			public void removeNode(HyPeerWebSegment web, Node n, Node.Listener listener){
				//If the HyPeerWeb has more than two nodes, remove normally
				int size = web.getSegmentSize();
				Node last, first = null;
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
					//Find a disconnection point
					web.getRandomNode(new Node.Listener(){
						@Override
						public void callback(Node ranNode) {
							ranNode.findDisconnectNode().disconnectNode(new Node.Listener(){
								@Override
								public void callback(Node n){
									
									web.changeState(HAS_MANY);
									listener.callback(n);
								}
							});
								
							//Remove node from list of nodes
							web.nodes.remove(replace.getWebId());
							//Replace the node to be deleted
							if (!n.equals(replace)){
								int newWebID = n.getWebId();
								web.nodes.remove(newWebID);
								web.nodes.put(newWebID, replace);
								if (!replace.replaceNode(n))
									web.changeState(CORRUPT);
							}
							
							
						}
					});
				}
				//If the entire HyPeerWeb has only two nodes
				else{
					//removing node 0
					if(n.getWebId() == 0){
						Node replace = n.L.getFold(); //gets node 1
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
			public void addNode(HyPeerWebSegment web, Node n, Node.Listener listener){
				System.err.println("CORRUPT HYPEERWEB");
			}
			@Override
			public void removeNode(HyPeerWebSegment web, Node n, Node.Listener listener){
				System.err.println("CORRUPT HYPEERWEB");
			}
		};
		public abstract void addNode(HyPeerWebSegment web, Node n, Node.Listener listener);
		public abstract void removeNode(HyPeerWebSegment web, Node n, Node.Listener listener);
	}
	/**
	 * Change the state of the HyPeerWeb
	 * @param state the new state
	 */
	protected void changeState(final HyPeerWebState state){
		(new BroadcastVisitor("_changeState")).visit(this);
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
	 * Looks for a HyPeerWebSegment that is not empty
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
	protected T getSegmentNodeByUID(int UID) {
		return (T) nodesByUID.get(UID);
	}
	
	//HYPEERWEB GETTERS
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
	 * @return the node we found; null, if there were no nodes
	 */
	public Node getNode(int webId, boolean approximate){
		//There are no nodes; stop execution
		if (isEmpty()) return null;
		//Delegate this method to a segment that actually has nodes
		if (isSegmentEmpty())
			return getNonemptySegment().getNode(webId, approximate);
		else{
			Node n = nodes.get(webId);
			//If this segment has this node
			if (n != null)
				return n;
			//Otherwise, use send-visitor to get the node
			else{
				SendVisitor visitor = new SendVisitor(webId, approximate);
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
	
	//NETWORK OPERATIONS
	protected void _changeState(Node n, HyPeerWebState state){
		((HyPeerWebSegment) n).state = state;
	}
}
