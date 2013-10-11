package hypeerweb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
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
	private Node fold;
	private Node surrogateFold;
	private Node inverseSurrogateFold;
	private ArrayList<Node> neighbors = new ArrayList();
	private ArrayList<Node> surrogateNeighbors = new ArrayList();
	private ArrayList<Node> inverseSurrogateNeighbors = new ArrayList();
	//State machines
	private static final int recurseLevel = 3; //3 = neighbor's neighbors
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
		fold = Fold;
		surrogateFold = sFold;
		inverseSurrogateFold = isFold;

		if (Neighbors != null)
			neighbors = Neighbors;
		if (sNeighbors != null)
			surrogateNeighbors = sNeighbors;
		if (isNeighbors != null)
			inverseSurrogateNeighbors = isNeighbors;
		
		NodeInit();
	}
	private void NodeInit(){
		foldState = new FoldStateStable();
	}

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
		for (Node n: inverseSurrogateNeighbors){
			ndc.updateDirect(child, n);
			ndc.updateDirect(n, child);
			//Remove surrogate reference to parent
			ndc.removeSurrogate(n, this);
			ndc.removeInverse(this, n);
		}
		//adds a neighbor of parent as a surrogate neighbor of child if neighbor is childless
		//and makes child an isn of neighbor
		for (Node n: neighbors){
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
		//TODO
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
		for (Node neighbor: neighbors){
			if(neighbor != parent){
				ndc.updateSurrogate(neighbor, parent);
				ndc.updateInverse(parent, neighbor);
				ndc.removeDirect(neighbor, this);
			}
		}	

		//remove this from parent neighbor list
		ndc.removeDirect(parent, this);

		//all SNs of this will have this removed from their ISN list
		for (Node sn : surrogateNeighbors){
			ndc.removeInverse(sn, this);
		}

		//determine fold state
		//if stable
		
		//if unstable

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
		
	/**
	 * Finds and returns the node whose WebID is closest to the given long
	 * Assumed to always start with the node with WebID of zero
	 * @param index The value to get as close as possible to
	 * @author John
	 */
	public Node searchForNode(long index){
		long closeness = countSetBits(index & this.webID);
		//Check fold first, since it may be faster
		Node fold_ref = this.fold == null ? this.surrogateFold : this.fold;
		if (fold_ref != null && countSetBits(index & fold_ref.getWebId()) > closeness)
			return fold_ref.searchForNode(index);
		//Otherwise, check neighbors
		for (Node n: neighbors){
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
	
	/**
	 * Finds the closest valid insertion point (the parent
	 * of the child to add) from a starting node, automatically deals with
	 * the node's holes and insertable state
	 * @return the parent of the child to add
	 * @author josh
	 */
	protected Node findInsertionNode() {
		return findInsertionNode(recurseLevel);
	}
	private Node findInsertionNode(int level){
		//For some reason, HyPeerWeb only validates if we
		//increase the recurse level; don't ask me why...
		level++;
		//Nodes we've checked for holes already
		TreeSet<Node> visited = new TreeSet<>();
		//Nodes we are currently checking for holes
		ArrayList<Node> parents = new ArrayList<>();
		//Neighbors of the parents
		ArrayList<Node> friends;
		//Start by checking the current node
		parents.add(this);
		visited.add(this);
		Node temp;
		while(true){
			//Check parents for valid insertion points
			for (Node parent: parents){
				if (parent.getHeight() < height)
					return parent;
				temp = parent.getSurrogateFold();
				if (temp != null)
					return temp;
				temp = parent.getFirstSurrogateNeighbor();
				if (temp != null)
					return temp;
			}
			//If this was the last level, don't go down any further
			if (level-- != 0){
				//Get a list of neighbors (friends)
				friends = new ArrayList<>();
				for (Node parent: parents)
					friends.addAll(parent.getNeighborsList());
				//Set non-visited friends as the new parents
				parents = new ArrayList<>();
				for (Node friend: friends){
					if (visited.add(friend)){
						parents.add(friend);
					}
				}
			}
			else return this;
		}
	}
	
	/**
	 * Finds an edge node that can replace a node to be deleted
	 * @return a Node that can be disconnected
	 * @author Josh
	 */
	protected Node findDisconnectNode(){
		return null;
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
							nu.node.neighbors.remove(nu.value);
						else nu.node.neighbors.add(nu.value);
						break;
					case SURROGATE:
						if (nu.delete)
							nu.node.surrogateNeighbors.remove(nu.value);
						else nu.node.surrogateNeighbors.add(nu.value);
						break;
					case INVERSE:
						if (nu.delete)
							nu.node.inverseSurrogateNeighbors.remove(nu.value);
						else nu.node.inverseSurrogateNeighbors.add(nu.value);
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
		return fold;
	}
	/**
	 * Gets the WebID of the Surrogate Fold of the Node
	 *
	 * @return The WebID of the Surrogate Fold of the Node
	 */
	@Override
	public Node getSurrogateFold() {
		return surrogateFold;
	}
	/**
	 * Gets the WebID of the Inverse Surrogate Fold of the Node
	 *
	 * @return The WebID of the Inverse Surrogate Fold of the Node
	 */
	@Override
	public Node getInverseSurrogateFold() {
		return inverseSurrogateFold;
	}
	/**
	 * Gets an ArrayList containing the Neighbors of the Node
	 *
	 * @return An ArrayList containing the Neighbors of the Node
	 */
	@Override
	public Node[] getNeighbors() {
		return neighbors.toArray(new Node[0]);
	}
	/**
	 * Gets neighbors as type list
	 * Users of the method are "on their honor" to not modify the original list
	 * @return a list of neighbors
	 */
	private ArrayList<Node> getNeighborsList(){
		return neighbors;
	}
	/**
	 * Gets an ArrayList containing the Surrogate Neighbors of the Node
	 *
	 * @return An ArrayList containing the Surrogate Neighbors of the Node
	 */
	@Override
	public Node[] getSurrogateNeighbors() {
		return surrogateNeighbors.toArray(new Node[0]);
	}		
	/**
	 * Gets the first surrogate neighbor of the node
	 * @return the first surrogate neighbor
	 */
	public Node getFirstSurrogateNeighbor(){
		if (this.surrogateNeighbors.isEmpty())
			return null;
		return this.surrogateNeighbors.get(0);
	}
	/**
	 * Gets an ArrayList containing the Inverse Surrogate Neighbors of the Node
	 *
	 * @return An ArrayList containing the Inverse Surrogate Neighbors of the
	 * Node
	 */
	@Override
	public Node[] getInverseSurrogateNeighbors() {
		return inverseSurrogateNeighbors.toArray(new Node[0]);
	}
	@Override
	public Node getParent() {
		Node lowest = this;
		for (Node n : neighbors) {
			if (n.webID < lowest.webID)
				lowest = n;
		}
		return lowest == this ? null : lowest;
	}
	
	//Setters
	/**
	 * Adds a Neighbor WebID to the list of Neighbors if it is not already in
	 * the list
	 *
	 * @param n The WebID of the Neighbor
	 */
	public void addNeighbor(Node n) {
		if (!isNeighbor(n))
			neighbors.add(n);
	}
	public void removeNeighbor(Node n){
		if(isNeighbor(n))
			neighbors.remove(n);
	}
	/**
	 * Checks to see if a WebID is in the list of Neighbors
	 *
	 * @param n The WebID to check
	 * @return True if found, false otherwise
	 */
	private boolean isNeighbor(Node n) {
		return neighbors.contains(n);
	}
	/**
	 * Adds a Surrogate Neighbor WebID to the list of Surrogate Neighbors if it
	 * is not already in the list
	 *
	 * @param sn The WebID of the Surrogate Neighbor
	 */
	public void addSurrogateNeighbor(Node sn) {
		if (!isSurrogateNeighbor(sn))
			surrogateNeighbors.add(sn);
	}
	/**
	 * Checks to see if a WebID is in the list of Surrogate Neighbors
	 *
	 * @param sn The WebID to check
	 * @return True if found, false otherwise
	 */
	private boolean isSurrogateNeighbor(Node sn) {
		return surrogateNeighbors.contains(sn);
	}
	/**
	 * Adds an Inverse Surrogate Neighbor WebID to the list of Inverse Surrogate
	 * Neighbors if it is not already in the list
	 *
	 * @param isn The WebID of the Inverse Surrogate Neighbor
	 */
	public void addInverseSurrogateNeighbor(Node isn) {
		if (!isInverseSurrogateNeighbor(isn))
			inverseSurrogateNeighbors.add(isn);
	}
	/**
	 * Checks to see if a WebID is in the list of Inverse Surrogate Neighbors
	 *
	 * @param isn The WebID to check
	 * @return True if found, false otherwise
	 */
	private boolean isInverseSurrogateNeighbor(Node isn) {
		return inverseSurrogateNeighbors.contains(isn);
	}
	/**
	 * Removes the given node as an inverse surrogate neighbor
	 * 
	 * @param isn Node to remove from inverse surrogate neighbor list
	 */
	public void removeInverseSurrogateNeighbor(Node isn){
		inverseSurrogateNeighbors.remove(isn);
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
		this.inverseSurrogateNeighbors.clear();
	}
	/**
	 * Sets the WebID of the Fold of the Node
	 *
	 * @param f The WebID of the Fold of the Node
	 */
	public void setFold(Node f) {
		fold = f;
	}
	/**
	 * Sets the WebID of the Surrogate Fold of the Node
	 *
	 * @param sf The WebID of the Surrogate Fold of the Node
	 */
	public void setSurrogateFold(Node sf) {
		surrogateFold = sf;
	}
	/**
	 * Sets the WebID of the Inverse Surrogate Fold of the Node
	 *
	 * @param sf The WebID of the Inverse Surrogate Fold of the Node
	 */
	public void setInverseSurrogateFold(Node sf) {
		inverseSurrogateFold = sf;
	}
	/**
	 * Switches the Fold State pattern state
	 * @param stable whether or not to switch to the stable state
	 */
	public void setFoldState(boolean stable){
		foldState = stable ? new FoldStateStable() : new FoldStateUnstable();
	}
	
	//CLASS OVERRIDES
	@Override
	public int compareTo(NodeInterface node) {
		if (webID < node.getWebId())
			return -1;
		else if (webID == node.getWebId())
			return 0;
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
		return this.webID == ((Node) obj).webID;
	}
	@Override
	public String toString(){
		return String.valueOf(webID)+"("+String.valueOf(height)+")";
	}
	
	//FOLD STATE PATTERN
	private static interface FoldStateInterface{
		public void updateFolds(Node.FoldDatabaseChanges fdc, Node caller, Node child);
		public void reverseFolds(Node.FoldDatabaseChanges fdc, Node caller, Node child);
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
		public void reverseFolds(FoldDatabaseChanges fdc, Node caller, Node child) {
			//parent.isf = child.fold
			
			//parent.isf.sfold = parent
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
		public void reverseFolds(FoldDatabaseChanges fdc, Node caller, Node child) {
			//give parent fold back
			
			//remove parent sfold and the corresponding isfold
			
			//child.fold.fold = parent
		}
	}
}
