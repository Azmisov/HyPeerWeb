package hypeerweb;

import communicator.Communicator;
import communicator.NodeListener;
import communicator.RemoteAddress;
import hypeerweb.visitors.AbstractVisitor;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.*;

/**
 * The Node class
 * TODO:
 *  - make NodeProxy hold webID, height, changingKey, and L (LinksProxy) by default
 *  - make sure we can use == or .equals when we get to proxies
 * @author Guy
 */
public class Node implements Serializable, Comparable<Node>{
	public static final String
		className = Node.class.getName(),
		classNameArr = Node[].class.getName();
	//Serialization
	public final int UID = Communicator.assignId();
	//Node Attributes
	protected int webID, height;
	public transient Attributes data = new Attributes();
	//Node's connections
	public Links L;
	//State machines
	private static final int recurseLevel = 2; //2 = neighbor's neighbors (2+ for validator to validate)
	protected transient FoldState foldState = FoldState.STABLE;
	
	//CONSTRUCTORS
	/**
	 * Create a Node with only a WebID
	 * @param id the WebID of the node
	 * @param height  the height of the node
	 */
	public Node(int id, int height) {
		assert(id >= 0 && height >= 0);
		this.webID = id;
		this.height = height;
		L = new Links(UID);
	}
	/**
	 * Copy constructor
	 * @param node node to merge data with
	 */
	public Node(NodeImmutable node) {
		foldState = node.foldState;
		webID = node.webID;
		height = node.height;
		data = node.data;
		L = new Links(UID, node.L);
	}
	
	//ADD OR REMOVE NODES
	/**
	 * Adds a child node to the current one
	 * @param child_proxy the Node to add as a child
	 * @param listener the add node callback
	 */
	protected void addChild(Node child_proxy, NodeListener listener){
		//Get new height and child's WebID
		int childHeight = height+1,
			childWebID = (1 << height) | webID;
		
		//Compile a list of updates for the child; the more things we
		//can group together, the less network communcations (and less failure)
		//New neighbors, including the parent node:
		Node[] child_n = L.getInverseSurrogateNeighbors();
		//New surrogate neighbors:
		//adds a neighbor of parent as a surrogate neighbor of child if
		//neighbor is childless and makes child an isn of neighbor
		ArrayList<Node> sn = new ArrayList();
		for (Node n: L.getNeighbors()){
			if (n.getHeight() < childHeight)
				sn.add(n);
		}
		Node[] child_sn = sn.toArray(new Node[sn.size()]);
		
		//Always set height/webID before changing links
		//Setting height is a little funky, in that it has to notify all
		//remote links to keep things valid. If you remove links, it can't notify them
		setHeight(childHeight);
		
		//Child has taken all isneighbors
		L.removeAllInverseSurrogateNeighbors();
		
		//Execute the update on the external segment
		child_proxy.executeRemotely(new NodeListener(
			className, "_addChild",
			new String[]{"int", "int", className, className+"$FoldState", classNameArr, classNameArr, NodeListener.className},
			new Object[]{childHeight, childWebID, this, foldState, child_n, child_sn, listener}
		));
	}
	protected static void _addChild(
		Node child, int childHeight, int childWebID, Node parent, FoldState foldState,
		Node[] child_n, Node[] child_sn, NodeListener listener
	){
		//Update height and webID first
		child.resetLinks();
		child.setHeight(childHeight);
		child.setWebID(childWebID);
		
		//Add neighbors
		//TODO, group these into mass remote updates
		child.L.addNeighbor(parent);
		parent.L.addNeighbor(child);
		for (Node friend: child_n){
			child.L.addNeighbor(friend);
			//Update friends
			friend.L.addNeighbor(child);
			//Remove surrogate reference to parent
			friend.L.removeSurrogateNeighbor(parent);
		}
		//Add surrogates
		for (Node friend: child_sn){
			child.L.addSurrogateNeighbor(friend);
			friend.L.addInverseSurrogateNeighbor(child);
		}
		
		//Set folds
		//TODO, group these into mass remote updates
		foldState.updateFolds(parent, child);
		
		//Child data has been set
		//TODO, the following needs to be moved to another callback, once
		//	we've grouped the other stuff into mass updates
		//Add to the host's node list
		Segment host = (Segment) child.getHostSegment();
		//Host could be null if we're adding a new segment (addSegment)
		if (host != null)
			host.nodes.put(child.getWebId(), child);
		if (listener != null)
			listener.callback(child);
	}
	/**
	 * Replaces a node with this node
	 * @param remove_proxy the node to replace
	 * @return true, if the replacement was successful
	 * @author isaac
	 */
	protected void replaceNode(Node remove_proxy, int newHeight, NodeListener listener){
		int oldWebID = this.webID;
		//Swap out connections
		L = new Links(UID, remove_proxy.L.convertToImmutable());
		//Inherit the node's fold state
		//TODO, should we cache this foldState?
		foldState = remove_proxy.getFoldState();
		//Change WebID/Height, this must come before updating connections
		//Otherwise, the Neighbor Sets will be tainted with incorrect webID's
		webID = remove_proxy.getWebId();
		//Update the node-map to reflect the new webID
		Segment host = getHostSegment();
		if (host != null){
			host.nodes.remove(oldWebID);
			host.nodes.put(webID, this);
		}
		height = newHeight == -1 ? remove_proxy.getHeight() : newHeight;
		
		//TODO, we may want to transfer data over; not sure how 
		//the whole get/setData stuff is going to be used, though

		//Notify all connections that their reference has changed
		L.broadcastReplacement(remove_proxy, this);
		remove_proxy.executeRemotely(new NodeListener(
			className, "_MANY_remove_finalize",
			new String[]{className, "int", NodeListener.className},
			new Object[]{this, oldWebID, listener}
		));
	}
	/**
	 * Disconnects an edge node to replace a node that will be deleted
	 * @param listener disconnection callback
	 * @author John, Brian, Guy
	 */
	protected void disconnectNode(int removeID, NodeListener listener){
		Node parent = getParent();
		int parentHeight = parent.getHeight()-1;
		//setHeight may conflict with the broadcastReplacement (in replaceNode),
		//if the parent happens to be the node we want to remove; the replacement
		//node needs to inherit the new height, so we'll have to pass it along in the listener
		if (parent.getWebId() != removeID){
			//reduce parent height by 1
			//TODO: I'm not sure if this will result in less network comms. if we set
			//the height before or after; this needs some experimentation
			parent.setHeight(parentHeight);
			parentHeight = -1;
		}
		
		//TODO, group these into mass node updates
		//all of the neighbors of this except parent will have parent as surrogateNeighbor instead of neighbor, and
		//parent will have all neighbors of this except itself as inverse surrogate neighbor
		for (Node neighbor: L.getNeighbors()){
			if (neighbor != parent){
				neighbor.L.addSurrogateNeighbor(parent);
				parent.L.addInverseSurrogateNeighbor(neighbor);
				neighbor.L.removeNeighbor(this);
			}
		}	
		//remove this from parent neighbor list
		parent.L.removeNeighbor(this);
		//all SNs of this will have this removed from their ISN list
		for (Node sn : L.getSurrogateNeighbors())
			sn.L.removeInverseSurrogateNeighbor(this);

		//Reverse the fold state; we will always have a fold - guaranteed
		assert(L.getFold() != null);
		L.getFold().getFoldState().reverseFolds(parent, this);
		
		//Execute callback, returning the new parentHeight
		listener.callback(this, parentHeight);
	}
	
	//MASS NODE UPDATES
	protected static void _ONE_add_zero(Node zero, Node one_proxy, NodeListener listener){
		//Always modify heights before you start changing links
		//Doing so will result in less network communications
		//In this case, it doesn't matter much; but we're here anyways, so might as well...
		zero.setHeight(1);
		//We can't set zero's links yet, since "one" might be a proxy
		one_proxy.executeRemotely(new NodeListener(
			Node.className, "_ONE_add_one",
			new String[]{Node.className, NodeListener.className},
			new Object[]{zero, listener}
		));
	}
	protected static void _ONE_add_one(Node one, Node zero_proxy, NodeListener listener){
		//Update data for the new second node
		one.resetLinks();
		one.setHeight(1);
		one.setWebID(1);
		one.L.setFold(zero_proxy);
		one.L.addNeighbor(zero_proxy);
		//Host will be on executing machine
		//If we're doing an addSegment op, there will be no host
		Segment host = one.getHostSegment();
		if (host != null)
			host.nodes.put(1, one);
		//Update data for the first node
		zero_proxy.executeRemotely(new NodeListener(
			className, "_ONE_add_finalize",
			new String[]{className, NodeListener.className},
			new Object[]{one, listener}
		));
	}
	protected static void _ONE_add_finalize(Node zero, Node one_proxy, NodeListener listener){
		zero.L.setFold(one_proxy);
		zero.L.addNeighbor(one_proxy);
		if (listener != null)
			one_proxy.executeRemotely(listener);
	}
	protected static void _TWO_remove(Node tostay, Node remove_proxy, NodeListener listener){
		//Remove links before setting height, to avoid extraneous re-syncing
		tostay.L.removeAllNeighbors();
		tostay.L.setFold(null);
		//Note: tostay may already be webID = 0, if remove_proxy is webID = 1
		int oldWebID = tostay.getWebId();
		//We need to update the node-map, since we changed the webID
		if (oldWebID != 0){
			tostay.setWebID(0);
			Segment host = tostay.getHostSegment();
			if (host != null){
				host.nodes.remove(oldWebID);
				host.nodes.put(0, tostay);
			}
		}
		tostay.setHeight(0);

		//Remove "remove_proxy" from it's node-maps
		remove_proxy.executeRemotely(new NodeListener(
			className, "_TWO_remove_finalize",
			new String[]{className, "int", NodeListener.className},
			new Object[]{tostay, oldWebID, listener}
		));
	}
	protected static void _TWO_remove_finalize(Node remove, Node replace_proxy, int oldWebID, NodeListener listener){
		//The removed node will be on this machine; remove from the node maps
		//replace_proxy may have replaced it already, in which case we ignore this step
		Segment seg = remove.getHostSegment();
		int remID = remove.getWebId();
		if (seg != null && remID != 0){
			seg.nodes.remove(remID);
			seg.nodesByUID.remove(remove.UID);
		}
		if (listener != null)
			listener.callback(remove, replace_proxy, oldWebID);
	}
	protected static void _MANY_add_random(Node ranNode, Node child_proxy, NodeListener listener){
		//Find a valid insertion point and add the child
		ranNode.findInsertionNode().addChild(child_proxy, listener);
	}
	protected static void _MANY_remove_random(Node ranNode, Node remove_proxy, NodeListener listener){	
		//Find a valid disconnect point
		ranNode.findDisconnectNode().disconnectNode(remove_proxy.getWebId(), new NodeListener(
			className, "_MANY_remove_disconnect",
			new String[]{Node.className, NodeListener.className},
			new Object[]{remove_proxy, listener}
		));
	}
	protected static void _MANY_remove_disconnect(Node replacement, int newHeight, Node remove_proxy, NodeListener listener){
		//Replace the node to be deleted
		if (!replacement.equals(remove_proxy))
			replacement.replaceNode(remove_proxy, newHeight, listener);
		//If the replacement = remove_proxy, we don't need to do
		//any replacement; just call the finalize callback
		else{
			remove_proxy.executeRemotely(new NodeListener(
				className, "_MANY_remove_finalize",
				new String[]{className, "int", NodeListener.className},
				new Object[]{null, -1, listener}
			));
		}
	}
	protected static void _MANY_remove_finalize(Node removed, Node replace_proxy, int oldWebID, NodeListener listener){
		//Remove from node-maps
		Segment host = removed.getHostSegment();
		if (host != null){
			host.nodes.remove(removed.getWebId());
			host.nodesByUID.remove(removed.UID);
		}
		if (listener != null)
			listener.callback(removed, replace_proxy, oldWebID);
	}
	
	//FIND VALID NODES
	/**
	 * Finds a valid node, given a set of criteria
	 * @param type the Criteria that denotes a valid node
	 * @param levels how many neighbor levels out to search;
	 *	a value less than zero will search forever until there are no more nodes to search
	 * @param recursive should this be run recursively, once a valid node is found?
	 *	Warning! depending on how you implement Criteria, if levels is less than 0 you
	 *  may enter an infinite loop
	 * @return a valid node
	 */
	protected Node findValidNode(Criteria.Type type, int levels, boolean recursive){
		int level = levels;
		//Nodes we've checked already
		TreeSet<Node> visited = new TreeSet();
		//Nodes we are currently checking
		ArrayList<Node> parents = new ArrayList();
		//Neighbors of the parents
		ArrayList<Node> friends;
		//Start by checking the current node
		parents.add(this);
		visited.add(this);
		Node temp;
		while(true){
			//Check for valid nodes
			for (Node parent: parents){
				if ((temp = Criteria.check(type, this, parent)) != null)
					return recursive ? temp.findValidNode(type, levels, recursive) : temp;
			}
			//If this was the last level, don't go down any further
			if (level-- != 0){
				//Get a list of neighbors (friends)
				friends = new ArrayList<>();
				for (Node parent: parents)
					friends.addAll(Arrays.asList(parent.L.getNeighbors()));
				//Set non-visited friends as the new parents
				parents = new ArrayList<>();
				for (Node friend: friends){
					if (visited.add(friend)){
						parents.add(friend);
					}
				}
				//Nothing else to check
				if (parents.isEmpty())
					return this;
			}
			//No friend nodes out to "recurseLevel" connections is valid
			else return this;
		}
	}
	/**
	 * Finds the closest valid insertion point (the parent
	 * of the child to add) from a startuping node, automatically deals with
	 * the node's holes and insertable state
	 * @return the parent of the child to add
	 * @author josh
	 */
	protected Node findInsertionNode() {
		return findValidNode(Criteria.Type.INSERT, recurseLevel, true);
	}
	/**
	 * Finds an edge node that can replace a node to be deleted
	 * @return a Node that can be disconnected
	 * @author Josh
	 */
	protected Node findDisconnectNode(){
		return findValidNode(Criteria.Type.DISCONNECT, recurseLevel, true);
	}
	
	//GETTERS
	/**
	 * Gets stored data in this node
	 * @param key key for this data
	 * @return data associated with this key
	 */
	public Object getData(String key){
		return data.getAttribute(key);
	}
	/**
	 * Gets all the data stored in this node
	 */
	public Attributes getAllData(){
		return data;
	}
	/**
	 * Gets the WebID of the Node
	 *
	 * @return The WebID of the Node
	 */
	public int getWebId() {
		return webID;
	}
	/**
	 * Gets the Height of the Node
	 *
	 * @return The Height of the Node
	 */
	public int getHeight() {
		return height;
	}
	/**
	 * Gets this node's parent
	 * @return the neighbor with webID lower than this node
	 */
	public Node getParent() {
		//NOTE: Any changes should be duplicated in NodeCache.Node
		if (webID == 0)
			return null;
		int parID = webID & ~Integer.highestOneBit(webID);
		for (Node n : L.getNeighbors()) {
			if (parID == n.getWebId())
				return n;
		}
		return null;
	}
	/**
	 * Get all child nodes of HyPeerWeb spanning tree
	 * @return a list of children nodes
	 */
	public ArrayList<Node> getTreeChildren(){
		//NOTE: Any changes should be duplicated in NodeCache.Node
		HashSet<Integer>
				generatedNeighbors = new HashSet(),
				generatedInverseSurrogates = new HashSet();
		ArrayList<Node> found = new ArrayList<>();
		int id = this.getWebId(),
			//Add a one bit to left-end of id, to get neighbor's children
			id_surr = id | ((1 << (height - 1)) << 1),
			trailingZeros = Integer.numberOfTrailingZeros(id);
		//Flip each of the trailing zeros, one at a time
		int bitShifter = 1;
		for(int i = 0; i < trailingZeros; i++){
			generatedNeighbors.add(id | bitShifter);
			generatedInverseSurrogates.add(id_surr | bitShifter);
			bitShifter <<= 1;
		}
		//If any of the neighbors match these webId's, we should broadcast to them
		for(Node node : L.getNeighbors()){
			if (generatedNeighbors.contains(node.getWebId()))
				found.add(node);
		}
		//Broadcast to any of our neighbor's children, if we have links to them
		for(Node node : L.getInverseSurrogateNeighbors()){
			if (generatedInverseSurrogates.contains(node.getWebId()))
				found.add(node);
		}
		return found;
	}
	/**
	 * Get parent node of HyPeerWeb spanning tree
	 * @return null if there is no parent, 
	 */
	public Node getTreeParent(){
		//NOTE: Any changes should be duplicated in NodeCache.Node
		if (webID == 0) return null;
		//This algorithm is just the reverse of getTreeChildren()
		//First check for a neighbor with the correct ID
		int neighborID = webID & ~Integer.lowestOneBit(webID);
		for (Node n: L.getNeighbors()){
			if (n.getWebId() == neighborID)
				return n;
		}
		//Otherwise, there must be a surrogate tree parent
		for (Node sn: L.getSurrogateNeighbors()){
			if (sn.getWebId() == (neighborID & ~((1 << (sn.getHeight() - 1)) << 1)))
				return sn;
		}
		//This should never happen in a valid HyPeerWeb
		assert(false);
		return null;
	}
	/**
	 * Get a closer Link to a target WebID
	 * @param target the WebID we're searching for
	 * @param mustBeCloser if false, it will get surrogate neighbors of equal
	 * closeness, provided no other link is closer
	 * @return a Node that is closer to the target WebID; null, if there are
	 * no closer nodes or if the target is negative
	 */
	public Node getCloserNode(int target, boolean mustBeCloser){
		//Trying to find a negative node is a waste of time
		if (target < 0) return null;
		//Try to find a link with a webid that is closer to the target
		//Keep track of highest scoring match; not as greedy, but less network
		//communications should make up for the slowness
		Node closest = null;
		int base = this.scoreWebIdMatch(target), high = base, temp;
		for (Node n: L.getAllLinks()){
			if ((temp = n.scoreWebIdMatch(target)) > high){
				high = temp;
				closest = n;
			}
		}
		if (closest != null)
			return closest;
		//If none are closer, get a SNeighbor
		if (!mustBeCloser){
			for (Node sn: L.getSurrogateNeighbors()){
				if (sn.scoreWebIdMatch(target) == base)
					return sn;
			}
		}
		//Otherwise, that node doesn't exist
		return null;
	}
	/**
	 * Scores how well a webID matches a search key compared to a base score
	 * @param idSearch the query result webID
	 * @return how many bits are set in the number
	 */
	private int scoreWebIdMatch(int idSearch){
		return Integer.bitCount(~(webID ^ idSearch));
	}
	
	//SETTERS
	/**
	 * Sets the value of stored data
	 * @param key string to associate to this data
	 * @param val data for this key
	 */
	public void setData(String key, Object val){
		data.setAttribute(key, val);
	}
	/**
	 * Sets the WebID of the Node
	 * @param id the new webID
	 */
	protected void setWebID(int id){
		webID = id;
	}
	/**
	 * Sets the Height of the Node and updates all pointers
	 * @param h The new height
	 */
	protected void setHeight(int h){
		//Links will handle all the gruesome pain it is to change height
		L.broadcastNewHeight(this, h);
	}
	/**
	 * Switches the Fold State pattern state
	 * @param state whether or not to switch to the stable state
	 */
	protected void setFoldState(FoldState state){
		foldState = state;
	}
	/**
	 * Resets links so we can assure we're working
	 * with a clean copy of the Node; this is used when
	 * we pass in a NodeProxy to addChild/replaceNode etc to
	 * assure the NodeProxy doesn't have any pre-existing links
	 */
	protected void resetLinks(){
		L = new Links(UID);
	}
	
	//FOLD STATE PATTERN
	/**
	 * Gets this node's fold state
	 * @return a FoldState
	 */
	protected FoldState getFoldState(){
		return foldState;
	}
	protected enum FoldState{
		STABLE{
			//After running we should be in an unstable state
			@Override
			public void updateFolds(Node parent, Node child){
				Node fold = parent.L.getFold();
				//Update reflexive folds
				child.L.setFold(fold);
				fold.L.setFold(child);
				//Insert surrogates for non-existant node
				parent.L.setSurrogateFold(fold);
				fold.L.setInverseSurrogateFold(parent);
				fold.setFoldState(FoldState.UNSTABLE);
				//Remove stable state reference
				parent.L.setFold(null);
			}
			@Override
			public void reverseFolds(Node parent, Node child){
				/* To reverse from a stable state:
				 * parent.isf = child.f
				 * child.f.sf = parent
				 * child.f.f = null
				 */
				Node fold = child.L.getFold();
				parent.L.setInverseSurrogateFold(fold);
				parent.setFoldState(FoldState.UNSTABLE);
				fold.L.setSurrogateFold(parent);
				fold.L.setFold(null);
			}
		},
		UNSTABLE{
			//After running, we should be in a stable state
			@Override
			public void updateFolds(Node parent, Node child){
				//Stable-state fold references
				Node isfold = parent.L.getInverseSurrogateFold();
				child.L.setFold(isfold);
				isfold.L.setFold(child);
				//Remove surrogate references
				isfold.L.setSurrogateFold(null);
				parent.L.setInverseSurrogateFold(null);
				parent.setFoldState(FoldState.STABLE);
			}
			@Override
			public void reverseFolds(Node parent, Node child){
				/* To reverse from an unstable state:
				 * parent.f = child.f
				 * child.f.f = parent
				 * parent.sf = null
				 * child.f.isf = null
				 */
				Node fold = child.L.getFold();
				parent.L.setFold(fold);
				fold.L.setFold(parent);
				parent.L.setSurrogateFold(null);
				fold.L.setInverseSurrogateFold(null);
				fold.setFoldState(FoldState.STABLE);
			}
		};
		
		public abstract void updateFolds(Node parent, Node child);
		public abstract void reverseFolds(Node parent, Node child);
	}
	
	//VISITOR PATTERN
	/**
	 * Accept a visitor for traversal
	 * @param v a HyPeerWeb visitor
	 */
	public void accept(AbstractVisitor v){
		v.visit(this);
	}
	
	//NETWORKING
	/**
	 * Get the segment that holds this node, if any
	 * @return a Segment containing this node
	 */
	public Segment getHostSegment(){
		for (Segment s: Segment.segmentList){
			if (s.getSegmentNodeByUID(UID) != null)
				return s;
		}
		return null;
	}
	/**
	 * Executes a callback on the machine this node is on
	 * @param listener a command/callback to execute
	 */
	public void executeRemotely(NodeListener listener){
		listener.callback(this);
	}
	/**
	 * Convert this node to a cached one; to convert many nodes
	 * at once, use Segment.getCache()
	 * @return a cached version of this node
	 */
	public NodeCache convertToCached(){
		return new NodeCache(this, null);
	}
	/**
	 * Get the address this node is on
	 * @return RemoteAddress specifying the host machine
	 */
	public RemoteAddress getAddress(){
		return Communicator.getAddress();
	}
	
	//CLASS OVERRIDES
	public Object writeReplace() throws ObjectStreamException {
		return new NodeProxy(this);
	}
	public Object readResolve() throws ObjectStreamException {
		return this;
	}
	@Override
	public int compareTo(Node node){
		int id = node.getWebId();
		if (webID == id)
			return 0;
		int nh = node.getHeight();
		return (height == nh ? webID < id : height < nh) ? -1 : 1;
	}
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Node))
			return false;
		return this.webID == ((Node) obj).getWebId();
	}
	@Override
	public int hashCode() {
		return new Integer(this.webID).hashCode();
	}
	/*
	@Override
	public String toString(){
		return "Node-"+webID+" ("+height+")";
	}*/
}
