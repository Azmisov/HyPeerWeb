package hypeerweb;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.HashSet;
import validator.NodeInterface;

/**
 * The Node class
 * TODO:
 *  - find insert/disconnect point is broken again
 *  - find disconnect point is slow
 *  - disconnect node doesn't sync with database
 *  - make NodeProxy hold webID, height, changingKey, and L (LinksProxy) by default
 *  - make sure we can use == or .equals when we get to proxies
 * @author Guy
 */
public class Node implements NodeInterface, Comparable<NodeInterface>{
	//NODE ATTRIBUTES
	private int webID, height;
	public Links L;
	//State machines
	private static final int recurseLevel = 2; //2 = neighbor's neighbors
	private FoldStateInterface foldState = new FoldStateStable(); 
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
		L = new Links();
	}
	/**
	 * Create a Node with all of its data
	 *
	 * @param id The WebID of the Node
	 * @param h The Height of the Node
	 * @param f The Fold of the Node
	 * @param sf The Surrogate Fold of the Node
	 * @param isf The Inverse Surrogate Fold of the Node
	 * @param n An ArrayList containing the Neighbors of the Node
	 * @param sn An ArrayList containing the Surrogate Neighbors of the Node
	 * @param isn An ArrayList containing the Inverse Surrogate Neighbors of the Node
	 */
	public Node(int id, int h, Node f, Node sf, Node isf, ArrayList<Node> n, ArrayList<Node> sn, ArrayList<Node> isn){
		webID = id;
		height = h;
		L =  new Links(f, sf, isf, n, sn, isn);		
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
		for (Node n: L.getInverseSurrogateNeighborsSet()){
			ndc.updateDirect(child, n);
			ndc.updateDirect(n, child);
			//Remove surrogate reference to parent
			ndc.removeSurrogate(n, this);
			ndc.removeInverse(this, n);
		}
		//adds a neighbor of parent as a surrogate neighbor of child if neighbor is childless
		//and makes child an isn of neighbor
		for (Node n: L.getNeighborsSet()){
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
			setHeight(childHeight);
			L.removeAllInverseSurrogateNeighbors();
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
		L = toReplace.getLinks();
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
	protected Node disconnectNode(Database db){
		NeighborDatabaseChanges ndc = new NeighborDatabaseChanges();
		FoldDatabaseChanges fdc = new FoldDatabaseChanges();
		Node parent = getParent();
		int parentHeight = parent.getHeight()-1;

		//all of the neighbors of this except parent will have parent as surrogateNeighbor instead of neighbor, and
		//parent will have all neighbors of this except itself as inverse surrogate neighbor
		for (Node neighbor: L.getNeighborsSet()){
			if(neighbor != parent){
				ndc.updateSurrogate(neighbor, parent);
				ndc.updateInverse(parent, neighbor);
				ndc.removeDirect(neighbor, this);
			}
		}	

		//remove this from parent neighbor list
		ndc.removeDirect(parent, this);

		//all SNs of this will have this removed from their ISN list
		for (Node sn : L.getSurrogateNeighborsSet())
			ndc.removeInverse(sn, this);

		//Reverse the fold state; we will always have a fold - guaranteed
		L.getFold().getFoldState().reverseFolds(fdc, parent, this);

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
	public Node searchForNode(int index, boolean exactMatch){
		//Exact matches will have a score of 32
		int base = scoreWebIdMatch(index), score;
		if (base == 32) return this;
		//Check fold first, since it may be faster
		Node fold_ref = L.getFold();
		if (fold_ref == null)
			fold_ref = L.getSurrogateFold();
		if (fold_ref != null && (score = fold_ref.scoreWebIdMatch(index)) > base)
			return score == 32 ? fold_ref : fold_ref.searchForNode(index, exactMatch);
		//Otherwise, check neighbors
		for (Node n: L.getNeighborsSet()){
			if ((score = n.scoreWebIdMatch(index)) > base)
				return score == 32 ? n : n.searchForNode(index, exactMatch);
		}
		//If we're looking for an exact node, go down to sneighbors
		if (exactMatch && webID != index){
			for (Node n: L.getSurrogateNeighborsSet()){
				if ((score = n.scoreWebIdMatch(index)) >= base)
					return score == 32 ? n : n.searchForNode(index, exactMatch);
			}
		}
		return this;
	}
	/**
	 * Scores how well a webID matches a search key compared to a base score
	 * @param idKey the webId search key
	 * @param idSearch the query result webID
	 * @param base the base score
	 * @return how many bits are set in the number
	 */
	private int scoreWebIdMatch(int idSearch){
		return Integer.bitCount(~(webID ^ idSearch));
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
					friends.addAll(parent.L.getNeighborsSet());
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
			Node low = friend.L.getLowestLink();
			if (low != null && low.getHeight() < origin.getHeight())
				return low;
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
			Node high = friend.L.getHighestLink();
			if (high != null && high.getHeight() > origin.getHeight())
				return high;
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
						nu.node.L.setFold(value);
						break;
					case SURROGATE:
						nu.node.L.setSurrogateFold(value);
						break;
					case INVERSE:
						nu.node.L.setInverseSurrogateFold(value);
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
							nu.node.L.removeNeighbor(nu.value);
						else nu.node.L.addNeighbor(nu.value);
						break;
					case SURROGATE:
						if (nu.delete)
							nu.node.L.removeSurrogateNeighbor(nu.value);
						else nu.node.L.addSurrogateNeighbor(nu.value);
						break;
					case INVERSE:
						if (nu.delete)
							nu.node.L.removeInverseSurrogateNeighbor(nu.value);
						else nu.node.L.addInverseSurrogateNeighbor(nu.value);
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
	 * Gets this node's parent
	 * @return the neighbor with webID lower than this node
	 */
	@Override
	public Node getParent() {
		Node lowest = this;
		int lowID = this.webID, temp;
		for (Node n : L.getNeighborsSet()) {
			if ((temp = n.getWebId()) < lowID){
				lowID = temp;
				lowest = n;
			}
		}
		return lowest == this ? null : lowest;
	}
	/**
	 * Get the node's neighbors
	 * @return a list of nodes
	 */
	@Override
	public Node[] getNeighbors() {
		return L.getNeighbors();
	}
	/**
	 * Get this node's surrogate neighbors
	 * @return a list of nodes
	 */
	@Override
	public Node[] getSurrogateNeighbors() {
		return L.getSurrogateNeighbors();
	}
	/**
	 * Get this node's inverse surrogate neighbors
	 * @return a list of nodes
	 */
	@Override
	public Node[] getInverseSurrogateNeighbors() {
		return L.getInverseSurrogateNeighbors();
	}
	/**
	 * Get this node's fold
	 * @return a single node
	 */
	@Override
	public Node getFold() {
		return L.getFold();
	}
	/**
	 * Get this node's surrogate fold
	 * @return a single node
	 */
	@Override
	public Node getSurrogateFold() {
		return L.getSurrogateFold();
	}
	/**
	 * Get this node's inverse surrogate fold
	 * @return a single node
	 */
	@Override
	public Node getInverseSurrogateFold() {
		return L.getInverseSurrogateFold();
	}
	
	//LINKS & CONNECTIONS
	/**
	 * Gets all the nodes connections
	 * @return a Links class
	 */
	public Links getLinks(){
		return L;
	}
	
	//Setters
	/**
	 * Sets the node's webid
	 * @param id the new webid
	 */
	protected void setWebID(int id){
		//We don't need to broadcast a key change here, since this is only
		//called when there is one node left
		this.webID = id;
	}
	/**
	 * Sets the Height of the Node and updates all pointers
	 * @param h The new height
	 */
	public void setHeight(int h) {
		//First remove the old key
		L.broadcastUpdate(this, null);
		height = h;
		//Now add back in with the new key
		L.broadcastUpdate(null, this);
	}
	
	//CLASS OVERRIDES
	@Override
	public int compareTo(NodeInterface node) {
		//If we're trying to remove the key in an ordered collection, changingKey = true
		//In that case, check the old key value, before it was changed
		//Currently, we only cache the old height, but we may cache old webId in the future
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
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append("\nNode: ").append(webID).append("(").append(height).append(")");
		Node f;
		//Folds
		if ((f = L.getFold()) != null)
			builder.append("\n\tFold: ").append(f.getWebId()).
					append("(").append(f.getHeight()).append(")");
		if ((f = L.getSurrogateFold()) != null)
			builder.append("\n\tSFold: ").append(f.getWebId()).
					append("(").append(f.getHeight()).append(")");
		if ((f = L.getInverseSurrogateFold()) != null)
			builder.append("\n\tISFold: ").append(f.getWebId()).
					append("(").append(f.getHeight()).append(")");
		//Neighbors
		TreeSet<Node> temp;
		if (!(temp = L.getNeighborsSet()).isEmpty()){
			builder.append("\n\tNeighbors: ");
			for (Node n : temp)
				builder.append(n.getWebId()).append("(").append(n.getHeight()).append("), ");
		}
		if (!(temp = L.getSurrogateNeighborsSet()).isEmpty()){
			builder.append("\n\tSNeighbors: ");
			for (Node n : temp)
				builder.append(n.getWebId()).append("(").append(n.getHeight()).append("), ");
		}
		if (!(temp = L.getInverseSurrogateNeighborsSet()).isEmpty()){
			builder.append("\n\tISNeighbors: ");
			for (Node n : temp)
				builder.append(n.getWebId()).append("(").append(n.getHeight()).append("), ");
		}
		return builder.toString();
	}
		
	//FOLD STATE PATTERN
	/**
	 * Switches the Fold State pattern state
	 * @param stable whether or not to switch to the stable state
	 */
	private void setFoldState(boolean stable){
		foldState = stable ? new Node.FoldStateStable() : new Node.FoldStateUnstable();
	}
	/**
	 * Gets this node's fold state
	 * @return a FoldState
	 */
	private FoldStateInterface getFoldState(){
		return foldState;
	}
	private static interface FoldStateInterface{
		public void updateFolds(Node.FoldDatabaseChanges fdc, Node caller, Node child);
		public void reverseFolds(Node.FoldDatabaseChanges fdc, Node parent, Node child);
	}
	private static class FoldStateStable implements FoldStateInterface{
		@Override
		//After running we should be in an unstable state
		public void updateFolds(Node.FoldDatabaseChanges fdc, Node caller, Node child) {
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
		public void reverseFolds(Node.FoldDatabaseChanges fdc, Node parent, Node child) {
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
		@Override
		//After running, we should be in a stable state
		public void updateFolds(Node.FoldDatabaseChanges fdc, Node caller, Node child) {
			//Stable-state fold references
			Node isfold = caller.getInverseSurrogateFold();
			fdc.updateDirect(child, isfold);
			fdc.updateDirect(isfold, child);
			//Remove surrogate references
			fdc.removeSurrogate(isfold, null);
			fdc.removeInverse(caller, null);
		}
		@Override
		public void reverseFolds(Node.FoldDatabaseChanges fdc, Node parent, Node child) {
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
	
	//VISITOR PATTERN
	private interface VisitorInterface{
		public void visit(Node n);
	}
	private class VisitorSend implements VisitorInterface{
		@Override
		public void visit(Node n){
			//Do something here...
		}
	}
	public void accept(VisitorInterface v){
		v.visit(this);
	}
        private class VisitorBroadcast implements VisitorInterface{
            @Override
            public void visit(Node n){
                int trailingZeros = Integer.numberOfTrailingZeros(webID);
                HashSet<Integer> generatedNeighbors = new HashSet();
                HashSet<Integer> generatedInverseSurrogates = new HashSet();
                int neighbors = webID;
                int inverseSurrogates = webID | (Integer.highestOneBit(webID) << 1);
                int bitShifter = 1;
                for(int i = 0; i < trailingZeros; i++){
                    generatedNeighbors.add(neighbors | bitShifter);
                    generatedInverseSurrogates.add(inverseSurrogates | bitShifter);
                    bitShifter <<= 1;
                }
                for(Node node : L.getNeighborsSet()){
                    if(generatedNeighbors.contains(node.getWebId())){
                        //do something
                    }
                }
                for(Node node : L.getInverseSurrogateNeighborsSet()){
                    if(generatedInverseSurrogates.contains(node.getWebId())){
                        //do something
                    }
                }
            }
        }
}
