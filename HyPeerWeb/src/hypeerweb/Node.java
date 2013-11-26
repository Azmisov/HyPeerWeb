package hypeerweb;

import communicator.Communicator;
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
public class Node implements Serializable {
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
	//Hash code prime
	private static long prime = 2654435761L;
	
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
	 * @return the new child node; null if the node couldn't be added
	 * @author Guy, Isaac, Brian
	 */
	protected Node addChild(Node child){
		//Get new height and child's WebID
		int childHeight = height+1,
			childWebID = (1 << height) | webID;
		setHeight(childHeight);
		child.setHeight(childHeight);
		child.setWebID(childWebID);
				
		//Set neighbours (Guy)
		//child neighbors
		this.L.addNeighbor(child);
		child.L.addNeighbor(this);
		for (Node n: L.getInverseSurrogateNeighbors()){
			child.L.addNeighbor(n);
			n.L.addNeighbor(child);
			//Remove surrogate reference to parent
			n.L.removeSurrogateNeighbor(this);
		}
		this.L.removeAllInverseSurrogateNeighbors();
		//adds a neighbor of parent as a surrogate neighbor of child if neighbor is childless
		//and makes child an isn of neighbor
		for (Node n: L.getNeighbors()){
			if (n.getHeight() < childHeight){
				child.L.addSurrogateNeighbor(n);
				n.L.addInverseSurrogateNeighbor(child);
			}
		}
		
		//Set folds (Brian/Isaac)
		foldState.updateFolds(this, child);

		return child;
	}
	/**
	 * Replaces a node with this node
	 * @param db a reference to the database singleton
	 * @param toReplace the node to replace
	 * @return true, if the replacement was successful
	 * @author isaac
	 */
	protected void replaceNode(Node toReplace){
		int oldWebID = this.webID;
		//Swap out connections
		//We're probably going to have to modify this so it works with proxies.
		L = toReplace.getLinks();
		L.UID = UID;
		//Inherit the node's fold state
		foldState = toReplace.getFoldState();
		//Change WebID/Height, this must come before updating connections
		//Otherwise, the Neighbor Sets will be tainted with incorrect webID's
		webID = toReplace.getWebId();
		height = toReplace.getHeight();
		//Notify all connections that their reference has changed
		L.broadcastUpdate(toReplace, this);
	}
	/**
	 * Disconnects an edge node to replace a node that
	 * will be deleted
	 * @param db the database connection
	 * @return the disconnected node
	 * @author John, Brian, Guy
	 */
	protected Node disconnectNode(){
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

		//Reverse the fold state; we will always have a fold - guaranteed
		L.getFold().getFoldState().reverseFolds(parent, this);

		return this;
	}
	
	//VISITOR METHODS
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
	public int scoreWebIdMatch(int idSearch){
		return Integer.bitCount(~(webID ^ idSearch));
	}
	/**
	 * Get all child nodes of HyPeerWeb spanning tree
	 * @return a list of children nodes
	 */
	public ArrayList<Node> getTreeChildren(){
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
	
	//FIND VALID NODES
	/**
	 * Defines a set of criteria for a valid node point
	 * (whether that be for an insertionPoint or disconnectPoint)
	 */
	protected static interface Criteria{
		/**
		 * Checks to see if the "friend" of the "origin" node fits some criteria
		 * @param origin the originating node
		 * @param friend a node connected to the origin within "level" neighbor connections
		 * @return a Node that fits the criteria, otherwise null
		 */
		public Node check(Node origin, Node friend);
	}
	/**
	 * Finds a valid node, given a set of criteria
	 * @param x the Criteria that denotes a valid node
	 * @return a valid node
	 */
	protected Node findValidNode(Criteria x){
		int level = recurseLevel;
		//Nodes we've checked already
		TreeSet<Node> visited = new TreeSet<>();
		//Nodes we are currently checking
		ArrayList<Node> parents = new ArrayList<>();
		//Neighbors of the parents
		ArrayList<Node> friends;
		//Start by checking the current node
		parents.add(this);
		visited.add(this);
		Node temp;
		while(true){
			//Check for valid nodes
			for (Node parent: parents){
				if ((temp = x.check(this, parent)) != null)
					return temp.findValidNode(x);
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
	 * Criteria for a valid insertion point node
	 */
	private static final Criteria insertCriteria = new Criteria(){
		@Override
		public Node check(Node origin, Node friend){
			//Insertion point is always the lowest point within recurseLevel connections
			Node low = friend.L.getLowestLink();
			if (low != null && low.getHeight() < origin.getHeight())
				return low;
			return null;
		}
	};
	/**
	 * Finds the closest valid insertion point (the parent
 of the child to add) from a startuping node, automatically deals with
 the node's holes and insertable state
	 * @return the parent of the child to add
	 * @author josh
	 */
	protected Node findInsertionNode() {
		return findValidNode(insertCriteria);
	}
	/**
	 * Criteria for a valid disconnect node
	 */
	private static final Criteria disconnectCriteria = new Criteria(){
		@Override
		public Node check(Node origin, Node friend){
			/* Check all nodes out to "recurseLevel" for higher nodes
				Any time we find a "higher" node, we go up to it
				We keep walking up the ladder until we can go no farther
				We don't need to keep track of visited nodes, since visited nodes
				will always be lower on the ladder We also never want to delete
				from a node with children
			*/
			//Check for higher nodes
			Node high = friend.L.getHighestLink();
			if (high != null && high.getHeight() > origin.getHeight())
				return high;
			//Then go up to children, if it has any
			if (origin == friend){
				Node child = origin.L.getHighestNeighbor();
				if (child.getWebId() > origin.getWebId())
					return child;
			}
			return null;
		}
	};
	/**
	 * Finds an edge node that can replace a node to be deleted
	 * @return a Node that can be disconnected
	 * @author Josh
	 */
	protected Node findDisconnectNode(){
		return findValidNode(disconnectCriteria);
	}
	
	//GETTERS
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
	 * Get the node's neighbors
	 * @return a list of nodes
	 */
	public Node[] getNeighbors() {
		return L.getNeighbors();
	}
	/**
	 * Get this node's surrogate neighbors
	 * @return a list of nodes
	 */
	public Node[] getSurrogateNeighbors() {
		return L.getSurrogateNeighbors();
	}
	/**
	 * Get this node's inverse surrogate neighbors
	 * @return a list of nodes
	 */
	public Node[] getInverseSurrogateNeighbors() {
		return L.getInverseSurrogateNeighbors();
	}
	/**
	 * Get this node's fold
	 * @return a single node
	 */
	public Node getFold() {
		return L.getFold();
	}
	/**
	 * Get this node's surrogate fold
	 * @return a single node
	 */
	public Node getSurrogateFold() {
		return L.getSurrogateFold();
	}
	/**
	 * Get this node's inverse surrogate fold
	 * @return a single node
	 */
	public Node getInverseSurrogateFold() {
		return L.getInverseSurrogateFold();
	}
	/**
	 * Gets all the nodes connections
	 * @return a Links class
	 */
	public Links getLinks(){
		return L;
	}
	/**
	 * Gets stored data in this node
	 * @param key key for this data
	 * @return data associated with this key
	 */
	public Object getData(String key){
		return data.getAttribute(key);
	}
	
	//SETTERS
	/**
	 * Sets the WebID of the Node
	 * @param id the new webID
	 */
	public void setWebID(int id){
		webID = id;
	}
	/**
	 * Sets the Height of the Node and updates all pointers
	 * @param h The new height
	 */
	protected void setHeight(int h) {
		//First remove the old key
		L.broadcastUpdate(this, null);
		height = h;
		//Now add back in with the new key
		L.broadcastUpdate(null, this);
	}
	/**
	 * Switches the Fold State pattern state
	 * @param state whether or not to switch to the stable state
	 */
	protected void setFoldState(FoldState state){
		foldState = state;
	}
	/**
	 * Sets the value of stored data
	 * @param key string to associate to this data
	 * @param val data for this key
	 */
	public void setData(String key, Object val){
		data.setAttribute(key, val);
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
			public void updateFolds(Node caller, Node child) {
				Node fold = caller.getFold();
				//Update reflexive folds
				child.L.setFold(fold);
				fold.L.setFold(child);
				//Insert surrogates for non-existant node
				caller.L.setSurrogateFold(fold);
				fold.L.setInverseSurrogateFold(caller);
				fold.setFoldState(FoldState.UNSTABLE);
				//Remove stable state reference
				caller.L.setFold(null);
			}
			@Override
			public void reverseFolds(Node parent, Node child) {
				/* To reverse from a stable state:
				 * parent.isf = child.f
				 * child.f.sf = parent
				 * child.f.f = null
				 */
				Node fold = child.getFold();
				parent.L.setInverseSurrogateFold(fold);
				parent.setFoldState(FoldState.UNSTABLE);
				fold.L.setSurrogateFold(parent);
				fold.L.setFold(null);
			}
		},
		UNSTABLE{
			//After running, we should be in a stable state
			@Override
			public void updateFolds(Node caller, Node child) {
				//Stable-state fold references
				Node isfold = caller.getInverseSurrogateFold();
				child.L.setFold(isfold);
				isfold.L.setFold(child);
				//Remove surrogate references
				isfold.L.setSurrogateFold(null);
				caller.L.setInverseSurrogateFold(null);
				caller.setFoldState(FoldState.STABLE);
			}
			@Override
			public void reverseFolds(Node parent, Node child) {
				/* To reverse from an unstable state:
				 * parent.f = child.f
				 * child.f.f = parent
				 * parent.sf = null
				 * child.f.isf = null
				 */
				Node fold = child.getFold();
				parent.L.setFold(fold);
				fold.L.setFold(parent);
				parent.L.setSurrogateFold(null);
				fold.L.setInverseSurrogateFold(null);
				fold.setFoldState(FoldState.STABLE);
			}
		};
		
		public abstract void updateFolds(Node caller, Node child);
		public abstract void reverseFolds(Node caller, Node child);
	}
	
	//VISITOR PATTERN
	/**
	 * Accept a visitor for traversal
	 * @param v a HyPeerWeb visitor
	 */
	public void accept(AbstractVisitor v){
		v.visit(this);
	}
	
	//CLASS OVERRIDES
	public int compareTo(Node node) {
		int id = node.getWebId();
		if (webID == id)
			return 0;
		int nh = node.getHeight();
		return (height == nh ? webID < id : height < nh) ? -1 : 1;
	}
	@Override
	public int hashCode(){
		return (int) ((this.webID * prime) % Integer.MAX_VALUE);
	}
	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass())
			return false;
		return this.webID == ((Node) obj).getWebId();
	}
	
	//NETWORKING
	public Object writeReplace() throws ObjectStreamException {
		return new NodeProxy(this);
	}
	public Object readResolve() throws ObjectStreamException {
		return this;
	}
	public static abstract class Listener implements Serializable{
		public abstract void callback(Node n);
	}
}
