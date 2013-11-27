package hypeerweb;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeSet;

/**
 * Maintains all node connections
 * @author isaac
 */
public class Links implements Serializable {
	//All the possible node link/connection types
	public static enum Type {
		FOLD, SFOLD, ISFOLD, NEIGHBOR, SNEIGHBOR, ISNEIGHBOR
	}
	//Serialization
	public int UID;
	//Link data
	private Node fold;
	private Node surrogateFold;
	private Node inverseSurrogateFold;
	private TreeSet<Node> neighbors;
	private TreeSet<Node> surrogateNeighbors;
	private TreeSet<Node> inverseSurrogateNeighbors;
	private TreeSet<Node> highest;
	
	/**
	 * Creates an empty links object
	 */
	public Links(int UID){
		this.UID = UID;
		neighbors = new TreeSet();
		surrogateNeighbors = new TreeSet();
		inverseSurrogateNeighbors = new TreeSet();
		highest = new TreeSet();
	}
	/**
	 * Creates a links object with predefined connections
	 * @param f fold
	 * @param sf surrogate fold
	 * @param isf inverse surrogate fold
	 * @param n list of neighbors
	 * @param sn list of surrogate neighbors
	 * @param isn list of inverse surrogate neighbors
	 */
	public Links(Node f, Node sf, Node isf, ArrayList<Node> n, ArrayList<Node> sn, ArrayList<Node> isn){
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
	protected void update(Node oldNode, Node newNode, Type type){
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
				break;
			case SNEIGHBOR:
				if (oldNode != null)
					surrogateNeighbors.remove(oldNode);
				break;
			case ISNEIGHBOR:
				if (oldNode != null)
					inverseSurrogateNeighbors.remove(oldNode);
				break;
		}
		//Update the highest connection list
		//Make sure this node isn't being referenced elsewhere
		if (oldNode != null && (!(fold == oldNode || surrogateFold == oldNode ||
			inverseSurrogateFold == oldNode || neighbors.contains(oldNode) ||
			surrogateNeighbors.contains(oldNode) || inverseSurrogateNeighbors.contains(oldNode))))
		{
			highest.remove(oldNode);
		}
		//Add it to the appropriate structure
		//Change the key back to the changed value
		if (newNode != null){
			switch (type){
				case NEIGHBOR:
					neighbors.add(newNode);
					break;
				case SNEIGHBOR:
					surrogateNeighbors.add(newNode);
					break;
				case ISNEIGHBOR:
					inverseSurrogateNeighbors.add(newNode);
					break;
			}
			//Update the highest connection list
			highest.add(newNode);
		}
	}
	
	//BROADCAST AND NOTIFICATION
	/**
	 * Notifies all incoming pointers that the current node has
	 * changed and the references need to be updated
	 * @param oldPointer the old node pointer
	 * @param newPointer the new node pointer
	 * than a replacement of oldPointer
	 */
	protected void broadcastUpdate(Node oldPointer, Node newPointer){
		//NOTE: we reverse surrogate/inverse-surrogate connection types
		//In the case of folds, we do not have to search for an oldPointer
		if (fold != null)
			fold.L.update(null, newPointer, Type.FOLD);
		if (surrogateFold != null)
			surrogateFold.L.update(null, newPointer, Type.ISFOLD);
		if (inverseSurrogateFold != null)
			inverseSurrogateFold.L.update(null, newPointer, Type.SFOLD);
		for (Node n: neighbors)
			n.L.update(oldPointer, newPointer, Type.NEIGHBOR);
		for (Node n: surrogateNeighbors)
			n.L.update(oldPointer, newPointer, Type.ISNEIGHBOR);
		for (Node n: inverseSurrogateNeighbors)
			n.L.update(oldPointer, newPointer, Type.SNEIGHBOR);
	}
	
	//SETTERS
	/**
	 * Adds a Neighbor to the set of Neighbors
	 * @param n the neighbor node
	 */
	protected void addNeighbor(Node n) {
		update(null, n, Type.NEIGHBOR);
	}
	/**
	 * Removes a neighbor node
	 * @param n the node to remove
	 */
	protected void removeNeighbor(Node n){
		update(n, null, Type.NEIGHBOR);
	}
	/**
	 * Adds a Surrogate Neighbor
	 * @param sn the new node
	 */
	protected void addSurrogateNeighbor(Node sn) {
		update(null, sn, Type.SNEIGHBOR);
	}
	/**
	 * Removes a surrogate neighbor
	 * @param sn the node to remove
	 */
	protected void removeSurrogateNeighbor(Node sn){
		update(sn, null, Type.SNEIGHBOR);
	}
	/**
	 * Adds an Inverse Surrogate Neighbor
	 * @param isn the new node
	 */
	protected void addInverseSurrogateNeighbor(Node isn) {
		update(null, isn, Type.ISNEIGHBOR);
	}
	/**
	 * Removes the given node as an inverse surrogate neighbor
	 * @param isn Node to remove from inverse surrogate neighbor set
	 */
	protected void removeInverseSurrogateNeighbor(Node isn){
		update(isn, null, Type.ISNEIGHBOR);
	}
	/**
	 * Removes all the IS neighbors from the node
	 */
	protected void removeAllInverseSurrogateNeighbors(){
		highest.removeAll(inverseSurrogateNeighbors);
		inverseSurrogateNeighbors.clear();
	}
	/**
	 * Sets the the fold connection
	 * @param f the new fold node
	 */
	protected void setFold(Node f) {
		update(null, f, Type.FOLD);
	}
	/**
	 * Sets the surrogate fold of the node
	 * @param sf the new surrogate fold node
	 */
	protected void setSurrogateFold(Node sf) {
		update(null, sf, Type.SFOLD);
	}
	/**
	 * Sets the Inverse Surrogate Fold of the Node
	 * @param isf the new Inverse Surrogate Fold of the Node
	 */
	protected void setInverseSurrogateFold(Node isf) {
		update(null, isf, Type.ISFOLD);
	}
	
	//GETTERS
	/**
	 * Get a sorted array of all Node links
	 * @return an array of all connections
	 */
	public Node[] getAllLinks(){
		return highest.toArray(new Node[highest.size()]);
	}
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
	
	//NETWORKING
	public Object writeReplace() throws ObjectStreamException {
		//TODO: Figure out how to send real links in case of replacing node
		return new LinksProxy(this);
	}
	public Object readResolve() throws ObjectStreamException {
		return this;
	}
}