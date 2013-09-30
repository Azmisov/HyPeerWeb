
/**
 *
 * @author Guy
 */
import java.util.ArrayList;

public class Node implements NodeInterface{
	//NODE ATTRIBUTES	
	private boolean hasChild;
	private int webID;
	private int height;
	private Node fold = null;
	private Node surrogateFold = null;
	private Node inverseSurrogateFold = null;
	private ArrayList<Node> neighbors = new ArrayList();
	private ArrayList<Node> surrogateNeighbors = new ArrayList();
	private ArrayList<Node> inverseSurrogateNeighbors = new ArrayList();

	//CONSTRUCTORS
	/**
	 * Create a Node with only a WebID
	 *
	 * @param id The WebID of the Node
	 */
	public Node(int id, int height) {
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
	public Node(int id, int Height, Node Fold, Node sFold, Node isFold,
			ArrayList<Node> Neighbors, ArrayList<Node> sNeighbors,
			ArrayList<Node> isNeighbors) {
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
	 * Adds a child node to the current one
	 * @param db the Database associated with the HyPeerWeb
	 * @return the new child node; null if the node couldn't be added
	 */
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
		for (Node n: inverseSurrogateNeighbors)
			ndc.updateDirect(child, n);
		//adds a neighbor of parent as a surrogate neighbor of child if nieghbor is childless
		//and makes child an isn of neighbor
		for(Node n: neighbors){
			if (!n.hasChild){ 
				ndc.updateSurrogate(child, n);
				ndc.updateInverse(n, child);
			}
		}
		ndc.updateDirect(this, child);
		ndc.updateDirect(child, this);
		
		//Set folds (Brian/Isaac)
		FoldDatabaseChanges fdc = new FoldDatabaseChanges();
		if (inverseSurrogateFold != null){
			//Stable-state fold references
			fdc.updateDirect(child, inverseSurrogateFold);
			fdc.updateDirect(inverseSurrogateFold, child);
			//Remove surrogate references
			fdc.updateSurrogate(inverseSurrogateFold, null);
			fdc.updateInverse(this, null);
		}
		else{
			//Update reflexive folds
			fdc.updateDirect(child, fold);
			fdc.updateDirect(fold, child);
			//Insert surrogates for non-existant node
			fdc.updateSurrogate(this, fold);
			fdc.updateInverse(fold, this);
			//Remove stable state reference
			fdc.updateDirect(this, null);
		}
		
		//Attempt to add the node to the database
		//If it fails, we cannot proceed
		{
			db.beginCommit();
			//Create the child node
			db.addNode(childWebID, childHeight, null, null, null);
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
			this.height = childHeight;
			this.hasChild = true;
			//Update neighbors and folds
			ndc.commitToHyPeerWeb();
			fdc.commitToHyPeerWeb();
			return child;
		}
		
	}
	
	/**
	 * Finds and returns the node whose WebID is closest to the given long
	 * Assumed to always start with the node with WebID of zero
	 * @param index The value to get as close as possible to
	 * @author John
	 */
	public Node searchForNode(long index){
		long closeness = index & webID, c;
		for (int i=0; i < neighbors.size(); i++){
			c = index & neighbors.get(i).webID;
			if (c > closeness)
				return neighbors.get(i).searchForNode(index);
		}
		return this;
	}
	
	/**
	 * Finds the closest valid insertion point (the parent
	 * of the child to add) from a starting node
	 * @return the parent of the child to add
	 * @author josh
	 */
	public Node findInsertionNode(){

		Node result = findInsertionNode(2);

		if(result == null) {
			return this;
		}

		return result;
	}
	private Node findInsertionNode(int times){
			
			if (surrogateFold != null) {
				return surrogateFold;
			} else if (surrogateNeighbors != null && !surrogateNeighbors.isEmpty()) {
				return surrogateNeighbors.get(0);
			} else if (times == 0) {
				return null;
			}
			
			for (Node n : neighbors) {
				if (n.height < height) {
					Node result = n.findInsertionNode(times - 1);
					if (result != null) {
						return result;
					}
				}
			}
			
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
	private static class FoldDatabaseChanges extends DatabaseChanges implements DatabaseChangesInterface{
		@Override
		public void commitToDatabase(Database db) {
			for (NodeUpdate nu: updates){
				switch (nu.type){
					case DIRECT:
						db.setFold(nu.node.webID, nu.value.height);
						break;
					case SURROGATE:
						db.setSurrogateFold(nu.node.webID, nu.value.webID);
						break;
					case INVERSE:
						db.setInverseSurrogateFold(nu.node.webID, nu.value.webID);
						break;
				}
			}
		}

		@Override
		public void commitToHyPeerWeb() {
			for (NodeUpdate nu: updates){
				switch (nu.type){
					case DIRECT:
						nu.node.fold = nu.value;
						break;
					case SURROGATE:
						nu.node.surrogateFold = nu.value;
						break;
					case INVERSE:
						nu.node.inverseSurrogateFold = nu.value;
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
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public void commitToHyPeerWeb() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}
	
	}
	
		//ACCESS METHODS FOR 
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
	 * Gets an ArrayList containing the Surrogate Neighbors of the Node
	 *
	 * @return An ArrayList containing the Surrogate Neighbors of the Node
	 */
	@Override
	public Node[] getSurrogateNeighbors() {
		return surrogateNeighbors.toArray(new Node[0]);
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
}
