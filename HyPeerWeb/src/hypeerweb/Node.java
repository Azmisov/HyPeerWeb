package hypeerweb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import validator.NodeInterface;

/**
 * The Node class
 * TODO: better searchForNode
 *		implement removeNeighbor syncing with InsertState (if necessary)
 * @author Guy
 */
public class Node implements NodeInterface{
	//NODE ATTRIBUTES
	private int webID;
	private int height;
	public static class Connections{
		public static enum ConnectionType {
			FOLD, SFOLD, ISFOLD, NEIGHBOR, SNEIGHBOR, ISNEIGHBOR
		}
		public Node fold;
		public Node surrogateFold;
		public Node inverseSurrogateFold;
		public TreeSet<Node> neighbors = new TreeSet<>();
		public TreeSet<Node> surrogateNeighbors = new TreeSet<>();
		public TreeSet<Node> inverseSurrogateNeighbors = new TreeSet<>();
	}
	private Connections C;
	//State machines
	private static final int recurseLevel = 2; //2 = neighbor's neighbors
	private FoldStateInterface foldState; 
	//Hash code prime
	private static long prime = Long.parseLong("2654435761");

	//CONSTRUCTORS
	/**
	 * Create a Node with only a WebID
	 *
	 * @param id The WebID of the Node
	 */
	public Node(int id, int height) {
		this.webID = id;
		this.height = height;
		
		NodeInit();
	}

	/**
	 * Create a Node with all of its data
	 *
	 * @param id The WebID of the Node
	 * @param Height The Height of the Node
	 * @param Fold The Fold of the Node
	 * @param sFold The Surrogate Fold of the Node
	 * @param isFold The Inverse Surrogate Fold of the Node
	 * @param Neighbors An ArrayList containing the Neighbors of the Node
	 * @param sNeighbors An ArrayList containing the Surrogate Neighbors of the
	 * Node
	 * @param isNeighbors An ArrayList containing the Inverse Surrogate
	 * Neighbors of the Node
	 */
	public Node(int id, int Height, Node Fold, Node sFold, Node isFold,
			ArrayList<Node> Neighbors, ArrayList<Node> sNeighbors,
			ArrayList<Node> isNeighbors) {
		webID = id;
		height = Height;
		C.fold = Fold;
		C.surrogateFold = sFold;
		C.inverseSurrogateFold = isFold;

		if (Neighbors != null)
			C.neighbors.addAll(Neighbors);
		if (sNeighbors != null)
			C.surrogateNeighbors.addAll(sNeighbors);
		if (isNeighbors != null)
			C.inverseSurrogateNeighbors.addAll(isNeighbors);
		
		NodeInit();
	}
	private void NodeInit(){
		foldState = new FoldStateStable();
		C = new Connections();
	}

	//ADD OR REMOVE NODES
	/**
	 * Adds a child node to the current one
	 * @param db the Database associated with the HyPeerWeb
	 * @return the new child node; null if the node couldn't be added
	 * @author Guy, Isaac, Brian
	 */
	protected Node addChild(Database db){
		//Get new height and child's WebID
		int childHeight = this.getHeight()+1,
			childWebID = 1;
		for (int i=1; i<childHeight; i++)
			childWebID <<= 1;
		childWebID |= this.getWebId();
		Node child = new Node(childWebID, childHeight);
				
		//Set neighbours (Guy)
		NeighborDatabaseChanges ndc = new NeighborDatabaseChanges();
		//child neighbors
		ndc.updateDirect(this, child);
		ndc.updateDirect(child, this);
		for (Node n: C.inverseSurrogateNeighbors){
			ndc.updateDirect(child, n);
			ndc.updateDirect(n, child);
			//Remove surrogate reference to parent
			ndc.removeSurrogate(n, this);
			ndc.removeInverse(this, n);
		}
		//adds a neighbor of parent as a surrogate neighbor of child if neighbor is childless
		//and makes child an isn of neighbor
		for (Node n: C.neighbors){
			if (n.getHeight() < childHeight){
				ndc.updateSurrogate(child, n);
				ndc.updateInverse(n, child);
			}
		}
		
		//Set folds (Brian/Isaac)
		FoldDatabaseChanges fdc = new FoldDatabaseChanges();
		foldState.updateFolds(fdc, this, child);
		
		//Attempt to add the node to the database
		//If it fails, we cannot proceed
		if (db != null) {
			db.beginCommit();
			//Create the child node
			db.addNode(child);
			//Update parent
			db.setHeight(webID, childHeight);
			db.removeAllInverseSurrogateNeighbors(webID);
			//Set neighbors and folds
			ndc.commitToDatabase(db);
			fdc.commitToDatabase(db);
			//Commit changes to database
			if (!db.endCommit())
				return null;
		}
		
		//Add the node to the Java structure
		{
			//Update parent
			this.setHeight(childHeight);
			this.removeAllInverseSurrogateNeighbors();
			//Update neighbors and folds
			ndc.commitToHyPeerWeb();
			fdc.commitToHyPeerWeb();
			return child;
		}
	}
	/**
	 * Replaces a node with this node
	 * @param toReplace the node to replace
	 * @author isaac
	 */
	protected void replaceNode(Node toReplace){
		//Swap out connections
		C = toReplace.getConnections();
		//Change WebID/Height, this must come before updating connections
		//Otherwise, the Neighbor Sets will be tainted with incorrect webID's
		webID = toReplace.getWebId();
		height = toReplace.getHeight();
		//Notify all connections that their reference has changed
		//NOTE: we reverse surrogate/inverse-surrogate connections
		//Fold connections do not need a reference to the old node (pass null)
		if (C.fold != null)
			C.fold.updateConnection(null, this, Connections.ConnectionType.FOLD);
		if (C.surrogateFold != null)
			C.surrogateFold.updateConnection(null, this, Connections.ConnectionType.ISFOLD);
		if (C.inverseSurrogateFold != null)
			C.inverseSurrogateFold.updateConnection(null, this, Connections.ConnectionType.SFOLD);
		for (Node n: C.neighbors)
			n.updateConnection(toReplace, this, Connections.ConnectionType.NEIGHBOR);
		for (Node n: C.surrogateNeighbors)
			n.updateConnection(toReplace, this, Connections.ConnectionType.ISNEIGHBOR);
		for (Node n: C.inverseSurrogateNeighbors)
			n.updateConnection(toReplace, this, Connections.ConnectionType.SNEIGHBOR);
	}
	/**
	 * Disconnects an edge node to replace a node that
	 * will be deleted
	 * @param db the database connection
	 * @return the disconnected node
	 * @author John, Brian, Guy
	 */
	protected Node disconnectNode(Database db){
		NeighborDatabaseChanges ndc = new NeighborDatabaseChanges();
		FoldDatabaseChanges fdc = new FoldDatabaseChanges();
		Node parent = getParent();
		int parentHeight = parent.getHeight()-1;

		//all of the neighbors of this except parent will have parent as surrogateNeighbor instead of neighbor, and
		//parent will have all neighbors of this except itself as inverse surrogate neighbor
		for (Node neighbor: C.neighbors){
			if(neighbor != parent){
				ndc.updateSurrogate(neighbor, parent);
				ndc.updateInverse(parent, neighbor);
				ndc.removeDirect(neighbor, this);
			}
		}	

		//remove this from parent neighbor list
		ndc.removeDirect(parent, this);

		//all SNs of this will have this removed from their ISN list
		for (Node sn : C.surrogateNeighbors){
			ndc.removeInverse(sn, this);
		}

		//Reverse the fold state; we will always have a fold - guaranteed
		C.fold.foldState.reverseFolds(fdc, parent, this);

		//Attempt to update the database
		//If it fails, we cannot proceed
		if (db != null) {
			db.beginCommit();
			//reduce parent height by 1
			db.setHeight(parent.getWebId(), parentHeight);
			ndc.commitToDatabase(db);
			fdc.commitToDatabase(db);
			//Commit changes to database
			if (!db.endCommit())
				return null;
		}

		//Update the Java structure
		{
			//reduce parent height by 1
			parent.setHeight(parentHeight);
			ndc.commitToHyPeerWeb();
			fdc.commitToHyPeerWeb();
			return this;
		}
	}
	
	//SEARCH FOR NODE
	/**
	 * Finds and returns the node whose WebID is closest to the given long
	 * Assumed to always start with the node with WebID of zero
	 * @param index The value to get as close as possible to
	 * @author John
	 */
	public Node searchForNode(long index){
		long closeness = countSetBits(index & this.webID);
		//Check fold first, since it may be faster
		Node fold_ref = C.fold == null ? C.surrogateFold : C.fold;
		if (fold_ref != null && countSetBits(index & fold_ref.getWebId()) > closeness)
			return fold_ref.searchForNode(index);
		//Otherwise, check neighbors
		for (Node n: C.neighbors){
			if (countSetBits(index & n.getWebId()) > closeness)
				return n.searchForNode(index);
		}
		return this;
	}
	/**
	 * Voodoo magic... don't touch
	 * @param i a number
	 * @return how many bits are set in the number
	 */
	private long countSetBits(long i){
		i = i - ((i >> 1) & 0x55555555);
		i = (i & 0x33333333) + ((i >> 2) & 0x33333333);
		return (((i + (i >> 4)) & 0x0F0F0F0F) * 0x01010101) >> 24;
	}
	
	//FIND VALID NODES
	/**
	 * This is the Command Pattern
	 * Yeah. That's right.
	 */
	private static interface Criteria{
		/**
		 * Checks to see if the "friend" of the "origin" node fits some criteria
		 * @param origin the originating node
		 * @param friend a node connected to the origin within "level" neighbor connections
		 * @param level how far out the friend is from origin
		 * @return a Node that fits the criteria, otherwise null
		 */
		public Node check(Node origin, Node friend, int level);
	}
	/**
	 * Finds a valid node, given a set of criteria
	 * @param x the Criteria that denotes a valid node
	 * @return a valid node
	 */
	private Node findValidNode(Criteria x){
		//For some reason, HyPeerWeb only validates if we
		//increase the recurse level; don't ask me why...
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
				if ((temp = x.check(this, parent, recurseLevel-level)) != null)
					return temp.findValidNode(x);
			}
			//If this was the last level, don't go down any further
			if (level-- != 0){
				//Get a list of neighbors (friends)
				friends = new ArrayList<>();
				for (Node parent: parents)
					friends.addAll(parent.getNeighborsSet());
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
	private static Criteria insertCriteria = new Criteria(){
		@Override
		public Node check(Node origin, Node friend, int level){
			int originHeight = origin.getHeight();
			//Friends cannot have height less than origin
			if (friend.getHeight() < originHeight)
				return friend;
			//Friend's fold cannot have smaller height
			Node temp = friend.getSurrogateFold();
			if (temp != null && temp.getHeight() < originHeight)
				return temp;
			//Friends cannot have surrogate neighbors of less height
			temp = friend.getLowestSurrogateNeighbor();
			if (temp != null && temp.getHeight() < originHeight)
				return temp;
			return null;
		}
	};
	/**
	 * Finds the closest valid insertion point (the parent
	 * of the child to add) from a starting node, automatically deals with
	 * the node's holes and insertable state
	 * @return the parent of the child to add
	 * @author josh
	 */
	protected Node findInsertionNode() {
		return findValidNode(insertCriteria);
	}
	/**
	 * Criteria for a valid disconnect node
	 */
	private static Criteria disconnectCriteria = new Criteria(){
		@Override
		public Node check(Node origin, Node friend, int level){
			//Folds
			Node temp = friend.getFold();
			if (temp != null && temp.compareTo(origin) > 0)
				return temp;
			
			temp = friend.getHighestInverseSurrogateNeighbor();
			if (temp != null && temp.compareTo(origin) > 0)
				return temp;
			//*			
			//Friends
			if (level < 2){
				temp = friend.getHighestNeighbor();
				if (temp != null && temp.getHeight() > origin.getHeight())
					return friend;
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
		/* Check all nodes out to "recurseLevel" for higher nodes
			Any time we find a "higher" node, we go up to it
			We keep walking up the ladder until we can go no farther
			We don't need to keep track of visited nodes, since visited nodes will always be lower on the ladder
		*/
		return findValidNode(disconnectCriteria);
		//return getHighestRelation(true);
	}
	
	private Node getHighestRelation(boolean originalCall){
	    Node highest = this;
	    Node neighborHighest;
	    if(!C.inverseSurrogateNeighbors.isEmpty()){
		for(Node n : C.inverseSurrogateNeighbors){
		    if(n.getWebId() > highest.getWebId())
			highest = n;
		}
	    }
	    if(!C.neighbors.isEmpty()){
		for(Node n : C.neighbors){
		if(n.getWebId() > highest.getWebId())
		highest = n;
		}
	    }
	    if(originalCall && !C.neighbors.isEmpty()){//neighbor's neighbors
		for(Node n : C.neighbors){
		    for(Node m : n.getInverseSurrogateNeighbors()){
			neighborHighest = m.getHighestRelation(false);
			if(neighborHighest.getWebId() > highest.getWebId())
			    highest = neighborHighest;
		    }
		}
	    }
	    if(C.fold != null)
	    if(C.fold.getWebId() > highest.getWebId())
		highest = C.fold;
	    if(C.inverseSurrogateFold != null)
	    if(C.inverseSurrogateFold.getWebId() > highest.getWebId())
		highest = C.inverseSurrogateFold;
	    
	    if(highest.getWebId() > webID)
		return highest.getHighestRelation(true);
	    
	    return this;
	}
	//EN-MASSE DATABASE CHANGE HANDLING
	/**
	 * Sub-Class to keep track of Fold updates
	 * @author isaac
	 */
	private static class DatabaseChanges{
		//Valid types of changes
		protected enum NodeUpdateType{
			DIRECT, SURROGATE, INVERSE
		}
		//List of changes
		protected ArrayList<NodeUpdate> updates;
		//Holds all change information
		protected class NodeUpdate{
			public NodeUpdateType type;
			public Node node;
			public Node value;
			public boolean delete;
			public NodeUpdate(NodeUpdateType type, Node node, Node value, boolean delete){
				this.type = type;
				this.node = node;
				this.value = value;
				this.delete = delete;
			}
		}
		//constructor
		public DatabaseChanges(){
			updates = new ArrayList<>();
		}
		//add updates
		public void updateDirect(Node node, Node value){
			newUpdate(NodeUpdateType.DIRECT, node, value, false);
		}
		public void updateSurrogate(Node node, Node value){
			newUpdate(NodeUpdateType.SURROGATE, node, value, false);
		}
		public void updateInverse(Node node, Node value){
			newUpdate(NodeUpdateType.INVERSE, node, value, false);
		}
		//remove updates
		public void removeDirect(Node node, Node value){
			newUpdate(NodeUpdateType.DIRECT, node, value, true);
		}
		public void removeSurrogate(Node node, Node value){
			newUpdate(NodeUpdateType.SURROGATE, node, value, true);
		}
		public void removeInverse(Node node, Node value){
			newUpdate(NodeUpdateType.INVERSE, node, value, true);
		}
		
		//general constructor
		private void newUpdate(NodeUpdateType type, Node n, Node v, boolean del){
			updates.add(new NodeUpdate(type, n, v, del));
		}
	}
	/**
	 * Interface for implementing node-specific commit actions
	 * @author isaac
	 */
	private interface DatabaseChangesInterface{
		public void commitToDatabase(Database db);
		public void commitToHyPeerWeb();
	}
	/**
	 * Extension of DatabaseChanges class to handle folds
	 * @author isaac
	 */
	private static class FoldDatabaseChanges extends DatabaseChanges implements DatabaseChangesInterface{
		@Override
		public void commitToDatabase(Database db) {
			for (NodeUpdate nu: updates){
				int value = nu.delete ? -1 : nu.value.webID;
				switch (nu.type){
					case DIRECT:
						db.setFold(nu.node.webID, value);
						break;
					case SURROGATE:
						db.setSurrogateFold(nu.node.webID, value);
						break;
					case INVERSE:
						db.setInverseSurrogateFold(nu.node.webID, value);
						break;
				}
			}
		}
		@Override
		public void commitToHyPeerWeb() {
			for (NodeUpdate nu: updates){
				Node value = nu.delete ? null : nu.value;
				switch (nu.type){
					case DIRECT:
						nu.node.setFold(value);
						break;
					case SURROGATE:
						nu.node.setSurrogateFold(value);
						break;
					case INVERSE:
						nu.node.setInverseSurrogateFold(value);
						//Update node FoldState; nu.delete corresponds directly to a Stable state
						nu.node.setFoldState(nu.delete);
						break;
				}
			}
		}
	}
	/**
	 * Extension of DatabaseChanges to handle neighbors
	 * @author guy
	 */
	private static class NeighborDatabaseChanges extends DatabaseChanges implements DatabaseChangesInterface{
		@Override
		public void commitToDatabase(Database db) {
			for (NodeUpdate nu: updates){
				switch (nu.type){
					case DIRECT:
						if (nu.delete)
							db.removeNeighbor(nu.node.webID, nu.value.webID);
						else db.addNeighbor(nu.node.webID, nu.value.webID);
						break;
					case SURROGATE:
						if (nu.delete)
							db.removeSurrogateNeighbor(nu.node.webID, nu.value.webID);
						else db.addSurrogateNeighbor(nu.node.webID, nu.value.webID);
						break;
					//Surrogate/Inverse are reflexive; DB will handle the rest
					case INVERSE: break;
				}
			}
		}
		@Override
		public void commitToHyPeerWeb() {
			for (NodeUpdate nu: updates){
				switch (nu.type){
					case DIRECT:
						if (nu.delete)
							nu.node.removeNeighbor(nu.value);
						else nu.node.addNeighbor(nu.value);
						break;
					case SURROGATE:
						if (nu.delete)
							nu.node.removeSurrogateNeighbor(nu.value);
						else nu.node.addSurrogateNeighbor(nu.value);
						break;
					case INVERSE:
						if (nu.delete)
							nu.node.removeInverseSurrogateNeighbor(nu.value);
						else nu.node.addInverseSurrogateNeighbor(nu.value);
						break;
				}
			}
		}
	}
	
	//GETTERS
	/**
	 * Gets the WebID of the Node
	 *
	 * @return The WebID of the Node
	 */
	@Override
	public int getWebId() {
		return webID;
	}
	/**
	 * Gets the Height of the Node
	 *
	 * @return The Height of the Node
	 */
	@Override
	public int getHeight() {
		return height;
	}
	/**
	 * Gets the WebId of the Node's Fold
	 *
	 * @return The WebID of the Node's Fold
	 */
	@Override
	public Node getFold() {
		return C.fold;
	}
	/**
	 * Gets the WebID of the Surrogate Fold of the Node
	 *
	 * @return The WebID of the Surrogate Fold of the Node
	 */
	@Override
	public Node getSurrogateFold() {
		return C.surrogateFold;
	}
	/**
	 * Gets the WebID of the Inverse Surrogate Fold of the Node
	 *
	 * @return The WebID of the Inverse Surrogate Fold of the Node
	 */
	@Override
	public Node getInverseSurrogateFold() {
		return C.inverseSurrogateFold;
	}
	/**
	 * Gets an ArrayList containing the Neighbors of the Node
	 *
	 * @return An ArrayList containing the Neighbors of the Node
	 */
	@Override
	public Node[] getNeighbors() {
		return C.neighbors.toArray(new Node[0]);
	}
	/**
	 * Gets neighbors as type list
	 * Users of the method are "on their honor" to not modify the original list
	 * @return a list of neighbors
	 */
	private TreeSet<Node> getNeighborsSet(){
		return C.neighbors;
	}
	/**
	 * Gets the neighbor of greatest height
	 * @return a neighbor
	 */
	private Node getHighestNeighbor(){
		if (C.neighbors.isEmpty())
			return null;
		return C.neighbors.last();
	}
	/**
	 * Gets the neighbor of smallest height
	 * @return a neighbor
	 */
	private Node getLowestNeighbor(){
		if (C.neighbors.isEmpty())
			return null;
		return C.neighbors.first();
	}
	/**
	 * Gets an ArrayList containing the Surrogate Neighbors of the Node
	 *
	 * @return An ArrayList containing the Surrogate Neighbors of the Node
	 */
	@Override
	public Node[] getSurrogateNeighbors() {
		return C.surrogateNeighbors.toArray(new Node[0]);
	}		
	/**
	 * Gets the last surrogate neighbor of the node
	 * @return the last surrogate neighbor
	 */
	public Node getHighestSurrogateNeighbor(){
		if (C.surrogateNeighbors.isEmpty())
			return null;
		return C.surrogateNeighbors.last();
	}
	/**
	 * Gets the first surrogate neighbor of the node
	 * @return the first surrogate neighbor
	 */
	public Node getLowestSurrogateNeighbor(){
		if (C.surrogateNeighbors.isEmpty())
			return null;
		return C.surrogateNeighbors.first();
	}
	/**
	 * Gets an ArrayList containing the Inverse Surrogate Neighbors of the Node
	 *
	 * @return An ArrayList containing the Inverse Surrogate Neighbors of the
	 * Node
	 */
	@Override
	public Node[] getInverseSurrogateNeighbors() {
		return C.inverseSurrogateNeighbors.toArray(new Node[0]);
	}
	/**
	 * Gets the first inverse surrogate neighbor of the node
	 * @return the first inverse surrogate neighbor
	 */
	public Node getHighestInverseSurrogateNeighbor(){
		if (C.inverseSurrogateNeighbors.isEmpty())
			return null;
		return C.inverseSurrogateNeighbors.last();
	}
	/**
	 * Gets the smallest isneighbor
	 * @return 
	 */
	public Node getLowestInverseSurrogateNeighbor(){
		if (C.inverseSurrogateNeighbors.isEmpty())
			return null;
		return C.inverseSurrogateNeighbors.first();
	}
	public boolean hasInverseSurrogateNeighbors(){
	    if(C.inverseSurrogateNeighbors.isEmpty())
		return false;
	    else
		return true;
	}
	@Override
	public Node getParent() {
		Node lowest = this;
		int lowID = this.webID, temp;
		for (Node n : C.neighbors) {
			if ((temp = n.getWebId()) < lowID){
				lowID = temp;
				lowest = n;
			}
		}
		return lowest == this ? null : lowest;
	}
	/**
	 * Gets all the nodes connections
	 * @return a Connections class
	 */
	private Connections getConnections(){
		return C;
	}
	
	//Setters
	/**
	 * Sets the node's webid
	 * @param id the new webid
	 */
	protected void setWebID(int id){
		this.webID = id;
	}
	/**
	 * Adds a Neighbor WebID to the list of Neighbors if it is not already in
	 * the list
	 *
	 * @param n The WebID of the Neighbor
	 */
	public void addNeighbor(Node n) {
		C.neighbors.add(n);
	}
	/**
	 * Removes a neighbor node
	 * @param n the node to remove
	 */
	public void removeNeighbor(Node n){
		C.neighbors.remove(n);
	}
	/**
	 * Adds a Surrogate Neighbor WebID to the list of Surrogate Neighbors if it
	 * is not already in the list
	 *
	 * @param sn The WebID of the Surrogate Neighbor
	 */
	public void addSurrogateNeighbor(Node sn) {
		C.surrogateNeighbors.add(sn);
	}
	/**
	 * Removes a surrogate neighbor
	 * @param sn the node to remove
	 */
	public void removeSurrogateNeighbor(Node sn){
		C.surrogateNeighbors.remove(sn);
	}
	/**
	 * Adds an Inverse Surrogate Neighbor WebID to the list of Inverse Surrogate
	 * Neighbors if it is not already in the list
	 *
	 * @param isn The WebID of the Inverse Surrogate Neighbor
	 */
	public void addInverseSurrogateNeighbor(Node isn) {
		C.inverseSurrogateNeighbors.add(isn);
	}
	/**
	 * Removes the given node as an inverse surrogate neighbor
	 * 
	 * @param isn Node to remove from inverse surrogate neighbor list
	 */
	public void removeInverseSurrogateNeighbor(Node isn){
		C.inverseSurrogateNeighbors.remove(isn);
	}
	/**
	 * Sets the Height of the Node
	 *
	 * @param h The new height
	 */
	public void setHeight(int h) {
		height = h;
	}
	/**
	 * Removes all the IS neighbors from the node
	 */
	public void removeAllInverseSurrogateNeighbors(){
		C.inverseSurrogateNeighbors.clear();
	}
	/**
	 * Sets the WebID of the Fold of the Node
	 *
	 * @param f The WebID of the Fold of the Node
	 */
	public void setFold(Node f) {
		C.fold = f;
	}
	/**
	 * Sets the WebID of the Surrogate Fold of the Node
	 *
	 * @param sf The WebID of the Surrogate Fold of the Node
	 */
	public void setSurrogateFold(Node sf) {
		C.surrogateFold = sf;
	}
	/**
	 * Sets the WebID of the Inverse Surrogate Fold of the Node
	 *
	 * @param sf The WebID of the Inverse Surrogate Fold of the Node
	 */
	public void setInverseSurrogateFold(Node sf) {
		C.inverseSurrogateFold = sf;
	}
	/**
	 * Switches the Fold State pattern state
	 * @param stable whether or not to switch to the stable state
	 */
	public void setFoldState(boolean stable){
		foldState = stable ? new FoldStateStable() : new FoldStateUnstable();
	}
	/**
	 * Updates a node's connections when a previous connection is replaced
	 * @param old_node the old connection Node
	 * @param new_node the new connection Node
	 * @param type the type of the connection
	 */
	protected void updateConnection(Node old_node, Node new_node, Connections.ConnectionType type){
		switch (type){
			case FOLD:
				C.fold = new_node;
				break;
			case SFOLD:
				C.surrogateFold = new_node;
				break;
			case ISFOLD:
				C.inverseSurrogateFold = new_node;
				break;
			case NEIGHBOR:
				C.neighbors.remove(old_node);
				C.neighbors.add(new_node);
				break;
			case SNEIGHBOR:
				C.surrogateNeighbors.remove(old_node);
				C.surrogateNeighbors.add(new_node);
				break;
			case ISNEIGHBOR:
				C.inverseSurrogateNeighbors.remove(old_node);
				C.inverseSurrogateNeighbors.add(new_node);
				break;
		}
	}
	
	//CLASS OVERRIDES
	@Override
	public int compareTo(NodeInterface node) {
		if (webID == node.getWebId())
			return 0;
		if (height < node.getHeight() || webID < node.getWebId())
			return -1;
		return 1;
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
	@Override
	public String toString(){
            StringBuilder builder = new StringBuilder();
            builder.append("\nWebId: " + webID + " Height: " + height);
	    if(C.fold != null)
		builder.append(" Fold: " + C.fold.getWebId());
	    if(C.surrogateFold != null)
                builder.append(" SFold: " + C.surrogateFold.getWebId());
	    if(C.inverseSurrogateFold != null)
                builder.append(" ISFold: " + C.inverseSurrogateFold.getWebId());
            
	    builder.append("\nNs: ");
	    for(Node n : C.neighbors) {
		builder.append(n.getWebId() + " ");
	    }
	    builder.append("SNs: ");
	    for(Node n : C.surrogateNeighbors) {
		builder.append(n.getWebId() + " ");
	    }
	    builder.append("ISNs: ");
	    for(Node n : C.inverseSurrogateNeighbors) {
		builder.append(n.getWebId() + " ");
	    }
	    builder.append("\n");
	    
            return builder.toString();
	}
	
	//FOLD STATE PATTERN
	private static interface FoldStateInterface{
		public void updateFolds(Node.FoldDatabaseChanges fdc, Node caller, Node child);
		public void reverseFolds(Node.FoldDatabaseChanges fdc, Node parent, Node child);
	}
	private static class FoldStateStable implements FoldStateInterface{
		/*
		private static FoldStateInterface instance = new FoldStateStable();
		public static FoldStateInterface getInstance(){
			return instance;
		}
		*/
		@Override
		//After running we should be in an unstable state
		public void updateFolds(FoldDatabaseChanges fdc, Node caller, Node child) {
			Node fold = caller.getFold();
			//Update reflexive folds
			fdc.updateDirect(child, fold);
			fdc.updateDirect(fold, child);
			//Insert surrogates for non-existant node
			fdc.updateSurrogate(caller, fold);
			fdc.updateInverse(fold, caller);
			//Remove stable state reference
			fdc.removeDirect(caller, null);
		}
		@Override
		public void reverseFolds(FoldDatabaseChanges fdc, Node parent, Node child) {
			/* To reverse from a stable state:
			 * parent.isf = child.f
			 * child.f.sf = parent
			 * child.f.f = null
			 */
			Node fold = child.getFold();
			fdc.updateInverse(parent, fold);
			fdc.updateSurrogate(fold, parent);
			fdc.removeDirect(fold, null);
		}
	}
	private static class FoldStateUnstable implements FoldStateInterface{
		/*
		private static FoldStateInterface instance = new FoldStateStable();
		public static FoldStateInterface getInstance(){
			return instance;
		}
		*/
		@Override
		//After running, we should be in a stable state
		public void updateFolds(FoldDatabaseChanges fdc, Node caller, Node child) {
			//Stable-state fold references
			Node isfold = caller.getInverseSurrogateFold();
			fdc.updateDirect(child, isfold);
			fdc.updateDirect(isfold, child);
			//Remove surrogate references
			fdc.removeSurrogate(isfold, null);
			fdc.removeInverse(caller, null);
		}
		@Override
		public void reverseFolds(FoldDatabaseChanges fdc, Node parent, Node child) {
			/* To reverse from an unstable state:
			 * parent.f = child.f
			 * child.f.f = parent
			 * parent.sf = null
			 * child.f.isf = null
			 */
			Node fold = child.getFold();
			fdc.updateDirect(parent, fold);
			fdc.updateDirect(fold, parent);
			fdc.removeSurrogate(parent, null);
			fdc.removeInverse(fold, null);
		}
	}
}
