package hypeerweb;

import communicator.Communicator;
import communicator.NodeListener;
import communicator.RemoteAddress;
import static hypeerweb.Segment.HyPeerWebState.*;
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
	public Attributes data = new Attributes();
	//Node's connections
	public Links L;
	//State machines
	private static final int recurseLevel = 2; //2 = neighbor's neighbors
	private FoldState foldState = FoldState.STABLE; 
	
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

	//ADD OR REMOVE NODES
	/**
	 * Adds a child node to the current one
	 * @param db the Database associated with the HyPeerWeb
	 * @param child the Node to add as a child
	 * @param listener the add node callback
	 */
	protected void addChild(Node child, NodeListener listener){
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
		child.executeRemotely(new NodeListener(
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
		child.L.addNeighbor(parent);
		for (Node friend: child_n)
			child.L.addNeighbor(friend);
		//Add surrogates
		for (Node friend: child_sn)
			child.L.addSurrogateNeighbor(friend);

		//Update parent node's connections
		//TODO, group these into mass remote updates
		parent.L.addNeighbor(child);
		for (Node friend: child_n){
			friend.L.addNeighbor(child);
			//Remove surrogate reference to parent
			friend.L.removeSurrogateNeighbor(parent);
		}
		for (Node friend: child_sn)
			friend.L.addInverseSurrogateNeighbor(child);
		
		//Set folds
		//TODO, group these into mass remote updates
		foldState.updateFolds(parent, child);
		
		//Child data has been set, we can call the listener now
		listener.callback(child);
	}
	/**
	 * Replaces a node with this node
	 * @param db a reference to the database singleton
	 * @param removed the node to replace
	 * @return true, if the replacement was successful
	 * @author isaac
	 */
	protected void replaceNode(Node removed, NodeListener listener){
		int oldWebID = this.webID;
		//Swap out connections
		L = new Links(removed.L.convertToImmutable());
		L.UID = UID;
		//Inherit the node's fold state
		foldState = removed.getFoldState();
		//Change WebID/Height, this must come before updating connections
		//Otherwise, the Neighbor Sets will be tainted with incorrect webID's
		webID = removed.getWebId();
		height = removed.getHeight();
		//Notify all connections that their reference has changed
		L.broadcastReplacement(removed, this);
		listener.callback(removed, this, oldWebID);
	}
	/**
	 * Disconnects an edge node to replace a node that
	 * will be deleted
	 * @param db the database connection
	 * @return the disconnected node
	 * @author John, Brian, Guy
	 */
	protected void disconnectNode(NodeListener listener){
		Node parent = getParent();
		int parentHeight = parent.getHeight()-1;
		//reduce parent height by 1
		parent.setHeight(parentHeight);

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

		//TODO, group these into mass node updates
		//Reverse the fold state; we will always have a fold - guaranteed
		assert(L.getFold() != null);
		L.getFold().getFoldState().reverseFolds(parent, this);
		listener.callback(this);
	}
	
	//MASS NODE UPDATES
	protected static void _ONE_add_zero(Node zero, Node one, NodeListener listener){
		//Always modify heights before you start changing links
		//Doing so will result in less network communications
		//In this case, it doesn't matter much; but we're here anyways, so might as well...
		zero.setHeight(1);
		one.executeRemotely(new NodeListener(
			Node.className, "_ONE_add_one",
			new String[]{Node.className, NodeListener.className},
			new Object[]{zero, listener}
		));
	}
	protected static void _ONE_add_one(Node one, Node zero, NodeListener listener){
		//Update data for the new second node
		one.resetLinks();
		one.setHeight(1);
		one.setWebID(1);
		one.L.setFold(zero);
		one.L.addNeighbor(zero);
		//Host will be on executing machine
		//If we're doing an addSegment op, there will be no host
		Segment host = one.getHostSegment();
		if (host != null)
			host.nodes.put(1, one);
		//Update data for the first node
		zero.executeRemotely(new NodeListener(
			className, "_ONE_add_finalize",
			new String[]{className, NodeListener.className},
			new Object[]{one, listener}
		));
	}
	protected static void _ONE_add_finalize(Node zero, Node one, NodeListener listener){
		zero.L.setFold(one);
		zero.L.addNeighbor(one);
		if (listener != null)
			one.executeRemotely(listener);
	}
	protected static void _TWO_remove(Node tostay, Node removed, NodeListener listener){
		tostay.L.removeAllNeighbors();
		tostay.L.setFold(null);
		tostay.setWebID(0);
		tostay.setHeight(0);
		tostay.getHostSegment().changeState(HAS_ONE);
		listener.callback(removed, tostay, tostay.getWebId());
	}
	protected static void _MANY_add_random(Node ranNode, Node child, NodeListener listener){
		//Find a valid insertion point and add the child
		ranNode.findInsertionNode().addChild(child, new NodeListener(
			className, "_MANY_add_insert", listener
		));
	}
	protected static void _MANY_add_insert(Node child, NodeListener listener){
		child.executeRemotely(new NodeListener(
			className, "_MANY_add_finalize", listener
		));
	}
	protected static void _MANY_add_finalize(Node child, NodeListener listener){
		//Add to the host's node list
		Segment host = (Segment) child.getHostSegment();
		//Host could be null if we're adding a new segment (addSegment)
		if (host != null)
			host.nodes.put(child.getWebId(), child);
		if (listener != null)
			listener.callback(child);
	}
	protected static void _MANY_remove_random(Node ranNode, Node remove, NodeListener listener){	
		//Find a valid disconnect point
		ranNode.findDisconnectNode().disconnectNode(new NodeListener(
			className, "_MANY_remove_disconnect",
			new String[]{Node.className, NodeListener.className},
			new Object[]{remove, listener}
		));
	}
	protected static void _MANY_remove_disconnect(Node replaced, Node removed, NodeListener listener){
		int oldWebId = replaced.webID;
		//Remove node from list of nodes
		//This remove is called on the same machine
		removed.getHostSegment().nodes.remove(replaced.getWebId());
		//Replace the node to be deleted
		if (!removed.equals(replaced)){
			int newWebID = removed.getWebId();
			removed.getHostSegment().nodes.remove(newWebID);
			removed.getHostSegment().nodes.put(newWebID, replaced);
			replaced.replaceNode(removed, listener);
		}
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
}
