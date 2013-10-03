
/**
 *
 * @author Guy
 */
import validator.NodeInterface;
import hypeerweb.Node;
import hypeerweb.Database;
import java.util.ArrayList;

public class Node_OLD implements NodeInterface{
	//NODE ATTRIBUTES	
	private int webID;
	private int height;
	private Node_OLD fold = null;
	private Node_OLD surrogateFold = null;
	private Node_OLD inverseSurrogateFold = null;
	private ArrayList<Node_OLD> neighbors = new ArrayList();
	private ArrayList<Node_OLD> surrogateNeighbors = new ArrayList();
	private ArrayList<Node_OLD> inverseSurrogateNeighbors = new ArrayList();

	//CONSTRUCTORS
	/**
	 * Create a Node with only a WebID
	 *
	 * @param id The WebID of the Node
	 */
	public Node_OLD(int id, int height) {
		webID = id;
		this.height = height;
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
	public Node_OLD(int id, int Height, Node_OLD Fold, Node_OLD sFold, Node_OLD isFold,
			ArrayList<Node_OLD> Neighbors, ArrayList<Node_OLD> sNeighbors,
			ArrayList<Node_OLD> isNeighbors) {
		webID = id;
		height = Height;
		fold = Fold;
		surrogateFold = sFold;
		inverseSurrogateFold = isFold;

		if (Neighbors != null) {
			neighbors = Neighbors;
		}
		if (sNeighbors != null) {
			surrogateNeighbors = sNeighbors;
		}
		if (isNeighbors != null) {
			inverseSurrogateNeighbors = isNeighbors;
		}
	}
	
	/**
	 * Finds the closest valid insertion point (the parent
	 * of the child to add) from a starting node
	 * @return the parent of the child to add
	 * @author josh
	 */
	public Node_OLD findInsertionNode(){

		Node_OLD result = findInsertionNode(2);

		if(result == null) {
			return this;
		}

		return result;
	}
		
	private Node_OLD findInsertionNode(int times){
			
			if (surrogateFold != null) {
				return surrogateFold;
			} else if (surrogateNeighbors != null && !surrogateNeighbors.isEmpty()) {
				return surrogateNeighbors.get(0);
			} else if (times == 0) {
				return null;
			}
			
			for (Node_OLD n : neighbors) {
				if (n.height < height) {
					Node_OLD result = n.findInsertionNode(times - 1);
					if (result != null) {
						return result;
					}
				}
			}
			
			return null;
	}
	
	//CHILD
	/**
	 * Finds out if this node has a child
	 * @return true if this node has a child, false otherwise
	 */
	//public boolean hasChild(){
	//	   return hasChild;
	//}
	/**
	 * Sets hasChild to true or false
	 * @param b new value for hasChild
	 */
//	public void hasChild(boolean b){
//		hasChild = b;
//	}

	//WEBID
	/**
	 * Gets the WebID of the Node
	 *
	 * @return The WebID of the Node
	 */
	@Override
	public int getWebId() {
		return webID;
	}

	//HEIGHT
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
	 * Sets the Height of the Node
	 *
	 * @param h The Height
	 */
	public void setHeight(int h) {
		height = h;
	}

	//FOLD
	/**
	 * Gets the WebId of the Node's Fold
	 *
	 * @return The WebID of the Node's Fold
	 */
		@Override
	public Node_OLD getFold() {
		return fold;
	}

	/**
	 * Sets the WebID of the Fold of the Node
	 *
	 * @param f The WebID of the Fold of the Node
	 */
	public void setFold(Node_OLD f) {
		fold = f;
	}

	//SURROGATE FOLD
	/**
	 * Gets the WebID of the Surrogate Fold of the Node
	 *
	 * @return The WebID of the Surrogate Fold of the Node
	 */
		@Override
	public Node_OLD getSurrogateFold() {
		return surrogateFold;
	}

	/**
	 * Sets the WebID of the Surrogate Fold of the Node
	 *
	 * @param sf The WebID of the Surrogate Fold of the Node
	 */
	public void setSurrogateFold(Node_OLD sf) {
		surrogateFold = sf;
	}

	//INVERSE SURROGATE FOLD
	/**
	 * Gets the WebID of the Inverse Surrogate Fold of the Node
	 *
	 * @return The WebID of the Inverse Surrogate Fold of the Node
	 */
	@Override
	public Node_OLD getInverseSurrogateFold() {
		return inverseSurrogateFold;
	}

	/**
	 * Sets the WebID of the Inverse Surrogate Fold of the Node
	 *
	 * @param sf The WebID of the Inverse Surrogate Fold of the Node
	 */
	public void setInverseSurrogateFold(Node_OLD sf) {
		inverseSurrogateFold = sf;
	}

	//NEIGHBORS
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
	 * Replaces the list of Neighbors with a new list
	 *
	 * @param al An ArrayList containing the new list of Neighbors. If al is
	 * null nothing will be changed
	 */
	public void setNeighbors(ArrayList<Node_OLD> al) {
		if (al != null) {
			neighbors = al;
		}
	}

	/**
	 * Adds a Neighbor WebID to the list of Neighbors if it is not already in
	 * the list
	 *
	 * @param n The WebID of the Neighbor
	 */
	public void addNeighbor(Node_OLD n) {
		if (!isNeighbor(n)) {
			neighbors.add(n);
		}
	}

	/**
	 * Checks to see if a WebID is in the list of Neighbors
	 *
	 * @param n The WebID to check
	 * @return True if found, false otherwise
	 */
	public boolean isNeighbor(Node_OLD n) {
		return neighbors.contains(n);
	}

	/**
	 * Deletes a WebID if it is in the list of Neighbors
	 *
	 * @param n The WebID to delete
	 */
	public void deleteNeighbor(int n) {
		neighbors.remove(n);
	}

	//SURROGATE NEIGHBORS
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
	 * Replaces the list of Surrogate Neighbors with a new list
	 *
	 * @param al An ArrayList containing the new list of Surrogate Neighbors If
	 * al is null nothing will be changed
	 */
	public void setSurrogateNeighbors(ArrayList<Node_OLD> al) {
		if (al != null) {
			surrogateNeighbors = al;
		}
	}

	/**
	 * Adds a Surrogate Neighbor WebID to the list of Surrogate Neighbors if it
	 * is not already in the list
	 *
	 * @param sn The WebID of the Surrogate Neighbor
	 */
	public void addSurrogateNeighbor(Node_OLD sn) {
		if (!isSurrogateNeighbor(sn)) {
			surrogateNeighbors.add(sn);
		}
	}

	/**
	 * Checks to see if a WebID is in the list of Surrogate Neighbors
	 *
	 * @param sn The WebID to check
	 * @return True if found, false otherwise
	 */
	public boolean isSurrogateNeighbor(Node_OLD sn) {
		return surrogateNeighbors.contains(sn);
	}

	/**
	 * Deletes a WebID if it is in the list of Surrogate Neighbors
	 *
	 * @param sn The WebID to delete
	 */
	public void deleteSurrogateNeighbor(int sn) {
		surrogateNeighbors.remove(sn);
	}

	//INVERSE SURROGATE NEIGHBORS
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

	/**
	 * Replaces the list of Inverse Surrogate Neighbors with a new list
	 *
	 * @param al An ArrayList containing the new list of Inverse Surrogate
	 * Neighbors. If al is null nothing will be changed
	 */
	public void setInverseSurrogateNeighbors(ArrayList<Node_OLD> al) {
		if (al != null) {
			inverseSurrogateNeighbors = al;
		}
	}

	/**
	 * Adds an Inverse Surrogate Neighbor WebID to the list of Inverse Surrogate
	 * Neighbors if it is not already in the list
	 *
	 * @param isn The WebID of the Inverse Surrogate Neighbor
	 */
	public void addInverseSurrogateNeighbor(Node_OLD isn) {
		if (!isInverseSurrogateNeighbor(isn)) {
			inverseSurrogateNeighbors.add(isn);
		}
	}

	/**
	 * Checks to see if a WebID is in the list of Inverse Surrogate Neighbors
	 *
	 * @param isn The WebID to check
	 * @return True if found, false otherwise
	 */
	public boolean isInverseSurrogateNeighbor(Node_OLD isn) {
		return inverseSurrogateNeighbors.contains(isn);
	}

	/**
	 * Deletes a WebID if it is in the list of Inverse Surrogate Neighbors
	 *
	 * @param isn The WebID to delete
	 */
	public void deleteInverseSurrogateNeighbor(Node_OLD isn) {
		inverseSurrogateNeighbors.remove(isn);
	}

	@Override
	public Node_OLD getParent() {

		Node_OLD lowest = this;

		for (Node_OLD n : neighbors) {
			if (n.webID < lowest.webID) {
				lowest = n;
			}
		}

		return lowest;
	}

	@Override
	public int compareTo(NodeInterface node) {
		if (webID < node.getWebId())
			return -1;
		else if (webID == node.getWebId())
			return 0;
		return 1;
	}

	public Node addChild(Database db){
		//Get new height and child's WebID
		int childHeight = height+1,
			childWebID = 1;
		for (int i=1; i<childHeight; i++)
			childWebID <<= 1;
		childWebID &= webID;
		Node child = new Node(childWebID, childHeight);
		
		//Set neighbours (Guy)
		NeighborDatabaseChanges ndc = new NeighborDatabaseChanges();
		//child neighbors
//		for (Node_OLD n: inverseSurrogateNeighbors)
//			ndc.updateDirect(child, n);
//		//adds a neighbor of parent as a surrogate neighbor of child if nieghbor is childless
//		//and makes child an isn of neighbor
//		for(Node_OLD n: neighbors){
//			//if (!n.hasChild){ 
//				ndc.updateSurrogate(child, n);
//				ndc.updateInverse(n, child);
//			//}
//		}
//		ndc.updateDirect(this, child);
//		ndc.updateDirect(child, this);
//		
//		//Set folds (Brian/Isaac)
//		FoldDatabaseChanges fdc = new FoldDatabaseChanges();
//		if (inverseSurrogateFold != null){
//			//Stable-state fold references
//			fdc.updateDirect(child, inverseSurrogateFold);
//			fdc.updateDirect(inverseSurrogateFold, child);
//			//Remove surrogate references
//			fdc.updateSurrogate(inverseSurrogateFold, null);
//			fdc.updateInverse(this, null);
//		}
//		else{
//			//Update reflexive folds
//			fdc.updateDirect(child, fold);
//			fdc.updateDirect(fold, child);
//			//Insert surrogates for non-existant node
//			fdc.updateSurrogate(this, fold);
//			fdc.updateInverse(fold, this);
//			//Remove stable state reference
//			fdc.updateDirect(this, null);
//		}
		
		//Attempt to add the node to the database
		//If it fails, we cannot proceed
		{
			db.beginCommit();
			//Create the child node
			db.addNode(childWebID, childHeight, null, null, null);
			//Set neighbors and folds
			ndc.commitToDatabase(db);
			//fdc.commitToDatabase(db);
			//Commit changes to database
			if (!db.endCommit())
				return null;
		}
		
		//Add the node to the Java structure
		{
			//Update parent
			this.height = childHeight;
			//this.hasChild = true;
			//Update neighbors and folds
			ndc.commitToHyPeerWeb();
			//fdc.commitToHyPeerWeb();
			return child;
		}
		
	}
	
	/**
	 * Finds and returns the node whose WebID is closest to the given long
	 * Assumed to always start with the node with WebID of zero
	 * @param index The value to get as close as possible to
	 * @author John
	 */
	public Node_OLD searchForNode(long index)
	{
		long closeness = index & webID, c;
		for (int i=0; i < neighbors.size(); i++){
			c = index & neighbors.get(i).webID;
			if (c > closeness)
				return neighbors.get(i).searchForNode(index);
		}
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
			public NodeUpdate(NodeUpdateType type, Node node, Node value){
				this.type = type;
				this.node = node;
				this.value = value;
			}
		}
		//constructor
		public DatabaseChanges(){
			updates = new ArrayList<>();
		}
		//add updates
		public void updateDirect(Node node, Node value){
			updates.add(new NodeUpdate(NodeUpdateType.DIRECT, node, value));
		}
		public void updateSurrogate(Node node, Node value){
			updates.add(new NodeUpdate(NodeUpdateType.SURROGATE, node, value));
		}
		public void updateInverse(Node node, Node value){
			updates.add(new NodeUpdate(NodeUpdateType.INVERSE, node, value));
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
//	private static class FoldDatabaseChanges extends DatabaseChanges implements DatabaseChangesInterface{
//		@Override
//		public void commitToDatabase(Database db) {
//			for (NodeUpdate nu: updates){
//				switch (nu.type){
//					case DIRECT:
//						db.setFold(nu.node.webID, nu.value.height);
//						break;
//					case SURROGATE:
//						db.setSurrogateFold(nu.node.webID, nu.value.webID);
//						break;
//					case INVERSE:
//						db.setInverseSurrogateFold(nu.node.webID, nu.value.webID);
//						break;
//				}
//			}
//		}

//		@Override
//		public void commitToHyPeerWeb() {
//			for (NodeUpdate nu: updates){
//				switch (nu.type){
//					case DIRECT:
//						nu.node.fold = nu.value;
//						break;
//					case SURROGATE:
//						nu.node.surrogateFold = nu.value;
//						break;
//					case INVERSE:
//						nu.node.inverseSurrogateFold = nu.value;
//						break;
//				}
//			}
//		}
//	}
	/**
	 * Extension of DatabaseChanges to handle neighbors
	 * @author guy
	 */
	private static class NeighborDatabaseChanges extends DatabaseChanges implements DatabaseChangesInterface{

		@Override
		public void commitToDatabase(Database db) {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public void commitToHyPeerWeb() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}
	
	}
}
