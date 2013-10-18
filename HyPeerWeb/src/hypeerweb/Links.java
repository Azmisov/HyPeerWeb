package hypeerweb;

import java.util.ArrayList;
import java.util.TreeSet;

/**
 * Maintains all node connections
 * @author isaac
 */
public class Links{
	public static enum Type {
		FOLD, SFOLD, ISFOLD, NEIGHBOR, SNEIGHBOR, ISNEIGHBOR
	}
	private Node fold;
	private Node surrogateFold;
	private Node inverseSurrogateFold;
	private TreeSet<Node> neighbors;
	private TreeSet<Node> surrogateNeighbors;
	private TreeSet<Node> inverseSurrogateNeighbors;
	private TreeSet<Node> highest;
	
	public Links(){
		neighbors = new TreeSet<>();
		surrogateNeighbors = new TreeSet<>();
		inverseSurrogateNeighbors = new TreeSet<>();
		highest = new TreeSet<>();
	}
	public Links(Node f, Node sf, Node isf, ArrayList<Node> n, ArrayList<Node> sn, ArrayList<Node> isn){
		this();
		//Add everything to the highest set as well
		//Add folds
		fold = f;
		surrogateFold = sf;
		inverseSurrogateFold = isf;
		if (f != null) highest.add(f);
		if (sf != null) highest.add(sf);
		if (isf != null) highest.add(isf);
		//Add neighbors
		if (n != null){
			neighbors.addAll(n);
			highest.addAll(n);
		}
		if (sn != null){
			surrogateNeighbors.addAll(sn);
			highest.addAll(sn);
		}
		if (isn != null){
			inverseSurrogateNeighbors.addAll(isn);
			highest.addAll(isn);
		}
	}
	
	/**
	 * Updates the connection
	 * @param oldNode the old Node reference (if there was one)
	 * @param newNode the new Node reference
	 * @param type the type of connection (Links.Type)
	 */
	private void update(Node oldNode, Node newNode, Type type){
		//Add it to the appropriate structure
		switch (type){
			case FOLD:
				oldNode = fold;
				fold = newNode;
				break;
			case SFOLD:
				oldNode = surrogateFold;
				surrogateFold = newNode;
				break;
			case ISFOLD:
				oldNode = inverseSurrogateFold;
				inverseSurrogateFold = newNode;
				break;
			case NEIGHBOR:
				if (oldNode != null)
					neighbors.remove(oldNode);
				if (newNode != null)
					neighbors.add(newNode);
				break;
			case SNEIGHBOR:
				if (oldNode != null)
					surrogateNeighbors.remove(oldNode);
				if (newNode != null)
					surrogateNeighbors.add(newNode);
				break;
			case ISNEIGHBOR:
				if (oldNode != null)
					inverseSurrogateNeighbors.remove(oldNode);
				if (newNode != null)
					inverseSurrogateNeighbors.add(newNode);
				break;
		}
		//Update the highest connection list
		if (oldNode != null)
			highest.remove(oldNode);
		if (newNode != null)
			highest.add(newNode);
	}
	
	//BROADCAST AND NOTIFICATION
	/**
	 * Notifies all incoming pointers that the current node has
	 * changed and the references need to be updated
	 * @param n the new node pointer
	 */
	public void updateIncomingPointers(Node oldPointer, Node newPointer){
		//NOTE: we reverse surrogate/inverse-surrogate connections
		//Fold connections do not need a reference to the old node (pass null)
		if (fold != null)
			fold.updateConnection(null, newPointer, Type.FOLD);
		if (surrogateFold != null)
			surrogateFold.updateConnection(null, newPointer, Type.ISFOLD);
		if (inverseSurrogateFold != null)
			inverseSurrogateFold.updateConnection(null, newPointer, Type.SFOLD);
		for (Node n: neighbors)
			n.updateConnection(oldPointer, newPointer, Type.NEIGHBOR);
		for (Node n: surrogateNeighbors)
			n.updateConnection(oldPointer, newPointer, Type.ISNEIGHBOR);
		for (Node n: inverseSurrogateNeighbors)
			n.updateConnection(oldPointer, newPointer, Type.SNEIGHBOR);
	}
	
	//SETTERS
	/**
	 * Adds a Neighbor to the set of Neighbors
	 * @param n the neighbor node
	 */
	public void addNeighbor(Node n) {
		update(null, n, Type.NEIGHBOR);
	}
	/**
	 * Removes a neighbor node
	 * @param n the node to remove
	 */
	public void removeNeighbor(Node n){
		update(n, null, Type.NEIGHBOR);
	}
	/**
	 * Adds a Surrogate Neighbor
	 * @param sn the new node
	 */
	public void addSurrogateNeighbor(Node sn) {
		update(null, sn, Type.SNEIGHBOR);
	}
	/**
	 * Removes a surrogate neighbor
	 * @param sn the node to remove
	 */
	public void removeSurrogateNeighbor(Node sn){
		update(sn, null, Type.SNEIGHBOR);
	}
	/**
	 * Adds an Inverse Surrogate Neighbor
	 * @param isn the new node
	 */
	public void addInverseSurrogateNeighbor(Node isn) {
		update(null, isn, Type.ISNEIGHBOR);
	}
	/**
	 * Removes the given node as an inverse surrogate neighbor
	 * @param isn Node to remove from inverse surrogate neighbor set
	 */
	public void removeInverseSurrogateNeighbor(Node isn){
		update(isn, null, Type.ISNEIGHBOR);
	}
	/**
	 * Removes all the IS neighbors from the node
	 */
	public void removeAllInverseSurrogateNeighbors(){
		highest.removeAll(inverseSurrogateNeighbors);
		inverseSurrogateNeighbors.clear();
	}
	/**
	 * Sets the the fold connection
	 * @param f the new fold node
	 */
	public void setFold(Node f) {
		update(null, f, Type.FOLD);
	}
	/**
	 * Sets the surrogate fold of the node
	 * @param sf the new surrogate fold node
	 */
	public void setSurrogateFold(Node sf) {
		update(null, sf, Type.SFOLD);
	}
	/**
	 * Sets the Inverse Surrogate Fold of the Node
	 * @param isf the new Inverse Surrogate Fold of the Node
	 */
	public void setInverseSurrogateFold(Node isf) {
		update(null, isf, Type.ISFOLD);
	}
	
	//GETTERS
	/**
	 * Gets the highest node out of all the connections
	 * @return a node
	 */
	public Node getHighestLink(){
		return highest.last();
	}
	/**
	 * Gets the lowest node out of all the connections
	 * @return a node
	 */
	public Node getLowestLink(){
		return highest.first();
	}
	/**
	 * Gets the node's fold
	 * @return
	 */
	public Node getFold() {
		return fold;
	}
	/**
	 * Gets the Surrogate Fold of the Node
	 * @return
	 */
	public Node getSurrogateFold() {
		return surrogateFold;
	}
	/**
	 * Gets the Inverse Surrogate Fold of the Node
	 * @return
	 */
	public Node getInverseSurrogateFold() {
		return inverseSurrogateFold;
	}
	/**
	 * Gets an ordered list (ascending) containing the Neighbors of the Node
	 * @return
	 */
	public Node[] getNeighbors() {
		return neighbors.toArray(new Node[neighbors.size()]);
	}
	/**
	 * Gets the neighbor of greatest height
	 * @return a neighbor
	 */
	public Node getHighestNeighbor(){
		if (neighbors.isEmpty())
			return null;
		return neighbors.last();
	}
	/**
	 * Gets the neighbor of smallest height
	 * @return a neighbor
	 */
	public Node getLowestNeighbor(){
		if (neighbors.isEmpty())
			return null;
		return neighbors.first();
	}
	/**
	 * Gets an ordered (ascending) list containing the Surrogate Neighbors of the Node
	 * @return a list of nodes
	 */
	public Node[] getSurrogateNeighbors() {
		return surrogateNeighbors.toArray(new Node[surrogateNeighbors.size()]);
	}		
	/**
	 * Gets the last surrogate neighbor of the node
	 * @return the last surrogate neighbor
	 */
	public Node getHighestSurrogateNeighbor(){
		if (surrogateNeighbors.isEmpty())
			return null;
		return surrogateNeighbors.last();
	}
	/**
	 * Gets the first surrogate neighbor of the node
	 * @return the first surrogate neighbor
	 */
	public Node getLowestSurrogateNeighbor(){
		if (surrogateNeighbors.isEmpty())
			return null;
		return surrogateNeighbors.first();
	}
	/**
	 * Gets an ordered (ascending) list of Inverse Surrogate Neighbors of the Node
	 * @return a list of nodes
	 */
	public Node[] getInverseSurrogateNeighbors() {
		return inverseSurrogateNeighbors.toArray(new Node[inverseSurrogateNeighbors.size()]);
	}
	/**
	 * Gets the first inverse surrogate neighbor of the node
	 * @return the first inverse surrogate neighbor
	 */
	public Node getHighestInverseSurrogateNeighbor(){
		if (inverseSurrogateNeighbors.isEmpty())
			return null;
		return inverseSurrogateNeighbors.last();
	}
	/**
	 * Gets the smallest isneighbor
	 * @return 
	 */
	public Node getLowestInverseSurrogateNeighbor(){
		if (inverseSurrogateNeighbors.isEmpty())
			return null;
		return inverseSurrogateNeighbors.first();
	}
	
	//PROTECTED GETTERS (WARNING, DO NOT MODIFY RETURNED VALUES)
	/**
	 * Gets neighbors as a collection
	 * Implementor must not modify the values
	 * @return a set of neighbors
	 */
	protected TreeSet<Node> getNeighborsSet(){
		return neighbors;
	}
	/**
	 * Gets surrogate neighbors as a collection
	 * Implementor must not modify the values
	 * @return a set of surrogate neighbors
	 */
	protected TreeSet<Node> getSurrogateNeighborsSet(){
		return surrogateNeighbors;
	}
	/**
	 * Gets inverse surrogate neighbors as a collection
	 * Implementor must not modify the values
	 * @return a set of inverse surrogate neighbors
	 */
	protected TreeSet<Node> getInverseSurrogateNeighborsSet(){
		return inverseSurrogateNeighbors;
	}
}