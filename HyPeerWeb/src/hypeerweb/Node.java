package hypeerweb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
	private static final int recurseLevel = 2;
	private InsertState insertState;
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
		insertState = new InsertState();
		foldState = new FoldStateStable();
	}

	/**
	 * Adds a child node to the current one
	 * @param db the Database associated with the HyPeerWeb
	 * @return the new child node; null if the node couldn't be added
	 */
	public Node addChild(Database db){
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
		/* WARNING:
			updateDirect must come before surr/inv_surr neighbor definitions
			that way, all child's neighbors will recieve state change
			broadcast signals (if surrogateNeighbors/folds/etc. change)
		*/
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
			/* WARNING:
				must come before ndc/fdc commits
				for broadcastInsertStateChange to work (I think)
			*/
			this.setHeight(childHeight);
			this.removeAllInverseSurrogateNeighbors();
			//Update neighbors and folds
			ndc.commitToHyPeerWeb();
			fdc.commitToHyPeerWeb();
			return child;
		}
	}
	
        public void disconnectNode(Node node){
            Node parent = getParent();
            parent.setHeight(parent.getHeight()-1);
            
            //all of the neighbors of node except parent will have parent as surrogateNeighbor and
            //parent will have all neighbors except itself as isn
            for(Node neighbor: neighbors){
                if(!neighbor.equals(parent)){
                    neighbor.addSurrogateNeighbor(parent);
                    parent.addInverseSurrogateNeighbor(neighbor);
                }
            }    
            
            //remove node from parent neighbor list
            parent.re
            
            //all SNs of node will have node removed from their ISN list
            for (int i=0; i < surrogateNeighbors.size(); i++)
            {
                surrogateNeighbors.get(i).removeInverseSurrogateNeighbor(this);
            }
            
            //fold stuff
        }
        
	/**
	 * Finds and returns the node whose WebID is closest to the given long
	 * Assumed to always start with the node with WebID of zero
	 * @param index The value to get as close as possible to
	 * @author John
	 */
	public Node searchForNode(long index){
		//THIS NEEDS FIXING HERE; account for folds, count bits
		long closeness = index & this.getWebId(), c;
		for (int i=0; i < neighbors.size(); i++){
			c = index & neighbors.get(i).getWebId();
			if (c > closeness)
				return neighbors.get(i).searchForNode(index);
		}
		return this;
	}
	
	/**
	 * Finds the closest valid insertion point (the parent
	 * of the child to add) from a starting node, automatically deals with
     * the node's holes and insertable state
	 * @return the parent of the child to add
	 * @author josh
	 */
	public Node findInsertionNode() {
		Node temp = findInsertionNode(recurseLevel);
		//System.out.println("Adding at point = "+temp);
		return temp;
	}
	private Node findInsertionNode(int level){
		//*
		TreeSet<Node> set = this.getRecursiveNeighbors(level+1);
		Iterator<Node> iter = set.iterator();
		Node temp, temp2;
		while (iter.hasNext()){
			temp = iter.next();
			if (temp.getHeight() < height)
				return temp;
			temp2 = temp.getSurrogateFold();
			if (temp2 != null)
				return temp2;
			temp2 = temp.getFirstSurrogateNeighbor();
			if (temp2 != null)
				return temp2;
		}
		return this;
		//*/
		//return insertState.findInsertionNode(this, level);
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
						//Surrogate fold has changed; update InsertState
						nu.node.broadcastInsertStateChange(!nu.delete, InsertState.InsertStateHoley.MASK_FOLD);
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
						if (nu.delete){
							nu.node.surrogateNeighbors.remove(nu.value);
							//We no longer have surrogate neighbors; update InsertState
							if (!nu.node.surrogateNeighbors.isEmpty())
								nu.node.broadcastInsertStateChange(false, InsertState.InsertStateHoley.MASK_NEIGHBOR);
						}
						else{
							//We now have surrogate neighbors; updateInsertState
							if (nu.node.surrogateNeighbors.isEmpty())
								nu.node.broadcastInsertStateChange(true, InsertState.InsertStateHoley.MASK_NEIGHBOR);
							nu.node.surrogateNeighbors.add(nu.value);
						}
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
	/**
	 * Gets a set of neighbors out to a certain level
	 * Note, the set does not include the current node
	 * @param levels number of levels to go out
	 * @return a TreeSet of neighbors out to level
	 */
	public TreeSet<Node> getRecursiveNeighbors(int levels){
		ArrayList<ArrayList<Node>> full = getRecursiveNeighborsList(levels);
		full.remove(0);
		TreeSet<Node> set = new TreeSet<>();
		for (ArrayList<Node> level: full)
			set.addAll(level);
		return set;
	}
	/**
	 * Gets a list of neighbors per level;
	 * index 0 = 0th level, index 1 = 1st level etc.
	 * @param levels how many levels to go out
	 * @return a list of lists of neighbors, of size = levels
	 */
	public ArrayList<ArrayList<Node>> getRecursiveNeighborsList(int levels){
		ArrayList<ArrayList<Node>> full = new ArrayList<>();
		ArrayList<Node> parents = new ArrayList<>();
		ArrayList<Node> friends;
		parents.add(this);
		full.add(parents);
		do{
			friends = new ArrayList<>();
			for (Node parent: parents)
				friends.addAll(parent.getNeighborsList());
			parents = friends;
			full.add(parents);
		} while (--levels != 0);
		return full;
	}
	/**
	 * Gets the type-mask/hole-mask for what type of holes this
	 * node has (will return 0, if not a holey node)
	 * @param checkHeight the node's height to check against, otherwise -1
	 * @return the hole type mask
	 */
	public int getHoleMask(int checkHeight){
		int mask = 0;
		if (checkHeight != -1 && height < checkHeight)
			mask |= InsertState.InsertStateHoley.MASK_HEIGHT;
		if (surrogateFold != null)
			mask |= InsertState.InsertStateHoley.MASK_FOLD;
		if (!surrogateNeighbors.isEmpty())
			mask |= InsertState.InsertStateHoley.MASK_NEIGHBOR;
		return mask;
	}
	
	//Setters
	/**
	 * Adds a Neighbor WebID to the list of Neighbors if it is not already in
	 * the list
	 *
	 * @param n The WebID of the Neighbor
	 */
	public void addNeighbor(Node n) {
		if (!isNeighbor(n)){
			//Update holey nodes list to reflect added neighbor
			ArrayList<ArrayList<Node>> addTree = n.getRecursiveNeighborsList(recurseLevel-1);
			ArrayList<ArrayList<Node>> connectTree = this.getRecursiveNeighborsList(recurseLevel-1);
			ArrayList<ArrayList<Integer>> addTreeMask = new ArrayList<>();
			ArrayList<ArrayList<Integer>> connectTreeMask = new ArrayList<>();
			ArrayList<ArrayList<Integer>> addTreeHeight = new ArrayList<>();
			ArrayList<ArrayList<Integer>> connectTreeHeight = new ArrayList<>();
			//Build mask tables
			ArrayList<Integer> temp, temp2;
			for (ArrayList<Node> addTreeEntry: addTree){
				temp = new ArrayList<>();
				temp2 = new ArrayList<>();
				for (Node entryNode: addTreeEntry){
					temp.add(entryNode.getHoleMask(-1));
					temp2.add(entryNode.getHeight());
				}
				addTreeMask.add(temp);
				addTreeHeight.add(temp2);
			}
			for (ArrayList<Node> connectTreeEntry: connectTree){
				temp = new ArrayList<>();
				temp2 = new ArrayList<>();
				for (Node entryNode: connectTreeEntry){
					temp.add(entryNode.getHoleMask(-1));
					temp2.add(entryNode.getHeight());
				}
				connectTreeMask.add(temp);
				connectTreeHeight.add(temp2);
			}
			//Update references
			int maxIters = connectTree.size(),
				maxAdd = addTree.size(),
				maxAddLevel, maxConnectLevel,
				maskAdd, maskConnect,
				heightAdd, heightConnect,
				maskAddHeight, maskConnectHeight;
			Node addNode, connectNode;
			ArrayList<Node> addLevel, connectLevel;
			//each add level
			for (int i=0; i<maxAdd; i++){
				//a corresponding connect level
				for (int j=0; j<maxIters; j++){
					//Loop through all permutations of add/connect nodes
					addLevel = addTree.get(i);
					connectLevel = connectTree.get(j);
					maxAddLevel = addLevel.size();
					maxConnectLevel = connectLevel.size();
					for (int ii=0; ii<maxAddLevel; ii++){
						for (int jj=0; jj<maxConnectLevel; jj++){
							maskAdd = addTreeMask.get(i).get(ii);
							maskConnect = connectTreeMask.get(j).get(jj);
							addNode = addLevel.get(ii);
							connectNode = connectLevel.get(jj);
							//Account for heights in masks
							heightAdd = addTreeHeight.get(i).get(ii);
							heightConnect = connectTreeHeight.get(j).get(jj);
							//System.out.println("Comparing "+addNode+" to "+connectNode);
							if (heightAdd < heightConnect){
								maskAdd |= InsertState.InsertStateHoley.MASK_HEIGHT;
							//	System.out.println("at least it is running");
							}
							else if (heightConnect < heightAdd){
								maskConnect |= InsertState.InsertStateHoley.MASK_HEIGHT;
							//	System.out.println("at least it is running");
							}
							//Exchange masks
							if (maskAdd != 0){
								connectNode.updateInsertState(
									addNode, heightAdd, true, maskAdd
								);
							}
							if (maskConnect != 0){
								addNode.updateInsertState(
									connectNode, heightConnect, true, maskConnect
								);
							}
						}
					}
				}
				maxIters--;
			}
			//Add the actual node
			neighbors.add(n);
		}
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
		public void removeInverseSurrogateNeighbor(Node isn)
	{
		for (int i=0; i < inverseSurrogateNeighbors.size(); i++)
		{
			if (isn == inverseSurrogateNeighbors.get(i))
			{
				inverseSurrogateNeighbors.remove(i);
				return;
			}
		}
	}
	/**
	 * Sets the Height of the Node
	 *
	 * @param h The new height
	 */
	public void setHeight(int h) {
		height = h;
		//Height has changed, update InsertState, if we aren't already holey
		this.broadcastInsertStateChange(false, InsertState.InsertStateHoley.MASK_HEIGHT);
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
	/**
	 * Changes the Insertable state pattern's state
	 * @param isHole is this node holey; NOTE: this will still check the
	 * node's height, even if isHole is false
	 */
	public void broadcastInsertStateChange(boolean isHole, int typemask){
		//Get a list of neighbors two levels out
		TreeSet<Node> updateNodes = getRecursiveNeighbors(2);
		Iterator<Node> iter = updateNodes.iterator();
		//Tell neighbors we have updated holeyness
		Node temp;
		boolean rev_comp;
		while (iter.hasNext()){
			temp = iter.next();
			rev_comp = temp.updateInsertState(this, this.height, isHole, typemask);
			//For height changes, we need to check reverse
			if ((typemask & InsertState.InsertStateHoley.MASK_HEIGHT) != 0)
				insertState.updateState(temp, rev_comp, InsertState.InsertStateHoley.MASK_HEIGHT);
		}
	}
	/**
	 * Updates the insert state to account for the new node
	 * @param check the new node to account for
	 * @param checkHeight the "check" node's height
	 * @param isHole whether it is a hole or not
	 * @param cache a list of valid node insertion points (optional)
	 * @return true if this node's height is less than checkHeight
	 */
	public boolean updateInsertState(Node check, int checkHeight, boolean isHole, int typemask){
		//If we don't know if it is holey, we need to compare the node's height
		if (!isHole || checkHeight < this.height)
			typemask |= InsertState.InsertStateHoley.MASK_HEIGHT;
		insertState.updateState(check, isHole || checkHeight < this.height, typemask);
		return this.height < checkHeight;
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

	//INSERT STATE PATTERN
	private static class InsertState{
		private InsertStateInterface state;
		/**
		 * Constructs a new state machine
		 * The default state is "full"
		 */
		public InsertState(){
			state = new InsertStateFull();
		}
		/**
		 * Updates the Insert State, depending on the
		 * whether the updated node is a hole or not
		 * @param node the updated node
		 * @param isHole whether it is a hole or not
		 */
		public void updateState(Node node, boolean isHole, int typemask){
			//Change to holey state
			if (isHole && !state.addHole(node, typemask))
				state = new InsertStateHoley(node, typemask);
			//Change to full state
			else if (!isHole && !state.removeHole(node, typemask))
				state = new InsertStateFull();
		}
		/**
		 * Returns a valid parent node that can add a child
		 * @param node the starting point node (given by HyPeerWeb class)
		 * @param level how far to recurse down looking for holey state nodes
		 */
		public Node findInsertionNode(Node node, int level){
			return state.findInsertionNode(node, level);
		}
		/**
		 * Check whether we're in the holey state
		 * @return true, if we're holey
		 */
		public boolean isHoley(){
			return state.getClass() == InsertStateHoley.class;
		}
		
		/**
		 * Generic state for this state machine
		 */
		private static interface InsertStateInterface{
			public Node findInsertionNode(Node node, int level);
			/**
			 * Adds a holey node to the optimized cache; This can be a node with
			 *  - surrogate neighbors
			 *  - a surrogate fold
			 *  - height less than neighbor
			 * @param hole the holey node that can be used for insertion
			 * @param type a bitmask type (see InsertStateHoley for details)
			 * @return false, if this state does not have a cache
			 */
			public boolean addHole(Node hole, int type);
			/**
			 * Removes a holey node from the optimized cache
			 * @param node the holey node to remove
			 * @param type bitmask type
			 * @return false, if there are no more nodes in the cache
			 */
			public boolean removeHole(Node node, int type);
		}
		/**
		 * The "full" state; no surrogate neighbors/folds or
		 * nodes less than parent height
		 */
		private static class InsertStateFull implements InsertStateInterface{
			@Override
			public Node findInsertionNode(Node node, int level) {
				if (level-1 == 0)
					return node;
				//Recursively search for valid nodes
				Node temp;
				for (Node n: node.neighbors){
					temp = n.findInsertionNode(level-1);
					if (temp != n)
						return temp;
				}
				return node;
			}
			@Override
			public final boolean addHole(Node node, int type){
				return false;
			}
			@Override
			public boolean removeHole(Node node, int type){
				return true;
			}
		}
		/**
		 * The "holey" state; there are surrogate neighbors/folds or
		 * nodes less than parent height
		 */
		private class InsertStateHoley implements InsertStateInterface{
			/**
			 * What kind of hole are we?
			 * - height less than neighbor
			 * - has surrogate neighbors
			 * - has fold
			 */
			public static final int MASK_HEIGHT = 1, MASK_NEIGHBOR = 2, MASK_FOLD = 4;
			private TreeMap<Node, Integer> holes;
			public InsertStateHoley(Node start_hole, int type){
				holes = new TreeMap<>();
				this.addHole(start_hole, type);
			}
			@Override
			public Node findInsertionNode(Node node, int level) {
				Entry<Node, Integer> hole = holes.firstEntry();
				Node parent = hole.getKey();
				int mask = hole.getValue();
				if ((mask & MASK_FOLD) != 0)
					return parent.getFold();
				if ((mask & MASK_NEIGHBOR) != 0)
					return parent.getFirstSurrogateNeighbor();
				//Height mask is all that's left
				return parent;
			}
			@Override
			public boolean addHole(Node hole, int type){
				//Type is actually a bitmask of all types
				Integer old = holes.get(hole);
				if (old != null)
					type |= old;
				holes.put(hole, type);
				return true;
			}
			@Override
			public boolean removeHole(Node node, int type) {
				Integer mask = holes.get(node);
				if (mask != null){
					mask &= ~type;
					if (mask == 0)
						holes.remove(node);
					else holes.put(node, mask);
				}
				return !holes.isEmpty();
			}
		}
	}
	
	//FOLD STATE PATTERN
	private static interface FoldStateInterface{
		public void updateFolds(Node.FoldDatabaseChanges fdc, Node caller, Node child);
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
	}
}
