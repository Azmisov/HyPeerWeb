package hypeerweb;

import communicator.Command;
import communicator.Communicator;
import communicator.RemoteAddress;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Maintains all node connections
 * @author isaac
 */
public class Links implements Serializable {
	public static final String
		className = Links.class.getName(),
		classNameArr = Links[].class.getName();
	//All the possible node link/connection types
	public static enum Type {
		FOLD, SFOLD, ISFOLD, NEIGHBOR, SNEIGHBOR, ISNEIGHBOR;
		public static String className = Type.class.getName();
	}
	//Serialization
	public final int UID;
	private boolean writeRealLinks = false;
	//Link data
	protected Node fold;
	protected Node surrogateFold;
	protected Node inverseSurrogateFold;
	protected TreeSet<Node> neighbors;
	protected TreeSet<Node> surrogateNeighbors;
	protected TreeSet<Node> inverseSurrogateNeighbors;
	protected TreeSet<Node> highest;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if(fold != null)
			builder.append("Fold: " + fold.getWebId()+ "\n");
		if(surrogateFold!=null)
			builder.append("Surrogate Fold: " + surrogateFold.getWebId()  + "\n");
		if(inverseSurrogateFold!=null)
			builder.append("Surrogate Fold: " + inverseSurrogateFold.getWebId()  + "\n");
		for(Node n : neighbors)
			builder.append("Neighbor:" + n.getWebId() + "\n");
		for(Node n : surrogateNeighbors)
			builder.append("Surrogate Neighbor:" + n.getWebId() + "\n");
		for(Node n : inverseSurrogateNeighbors)
			builder.append("Inverse Surrogate Neighbor:" + n.getWebId() + "\n");
		return builder.toString();
	}
	
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
	public Links(int UID, LinksImmutable l){
		this.UID = UID;		
		fold = l.fold;
		surrogateFold = l.surrogateFold;
		inverseSurrogateFold = l.inverseSurrogateFold;
		neighbors = l.neighbors;
		surrogateNeighbors = l.surrogateNeighbors;
		inverseSurrogateNeighbors = l.inverseSurrogateNeighbors;
		highest = l.highest;
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
		//Cannot have same reference in fold-set or neighbor-set
		/*
		boolean isFoldType = type == Type.FOLD || type == Type.SFOLD || type == Type.ISFOLD;
		if (oldNode != null && !(
			(!isFoldType &&
				(oldNode.equals(fold) || oldNode.equals(surrogateFold) || !oldNode.equals(inverseSurrogateFold))) ||
			(isFoldType &&
				(neighbors.contains(oldNode) || surrogateNeighbors.contains(oldNode) || inverseSurrogateNeighbors.contains(oldNode)))))
		{
			highest.remove(oldNode);
		}
		//*/
		//*
		//Update the highest connection list
		//Make sure this node isn't being referenced elsewhere
		if (oldNode != null && (!(fold == oldNode || surrogateFold == oldNode ||
			inverseSurrogateFold == oldNode || neighbors.contains(oldNode) ||
			surrogateNeighbors.contains(oldNode) || inverseSurrogateNeighbors.contains(oldNode))))
		{
			highest.remove(oldNode);
		}
		//*/
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
	protected void broadcastReplacement(Node oldPointer, Node newPointer){
		//NOTE: we reverse surrogate/inverse-surrogate connection types
		//In the case of folds, we do not have to search for an oldPointer
		//TODO: group these into mass updates
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
	/**
	 * Notifies all incoming pointers that the current node has
	 * changed its height and their lists need to be resorted
	 * @param n the node that was updated
	 * @param newHeight the node's
	 */
	protected void broadcastNewHeight(Node original, int newHeight){
		RemoteAddress hostAddr = Communicator.getAddress();
		//A list of nodes that are on this machine
		ArrayList<Links> here = new ArrayList();
		//A list of proxy nodes, mapped by their RemoteAddress
		HashMap<RemoteAddress, ArrayList<Links>> proxies = new HashMap();
		//Sort all links into here/proxies
		for (Node link: highest){
			//Not a proxy
			RemoteAddress laddr = link.getAddress();
			if (laddr == null || laddr.onSameMachineAs(hostAddr))
				here.add(link.L);
			//Is a proxy
			else{
				RemoteAddress generic = new RemoteAddress(laddr);
				ArrayList<Links> list = proxies.get(generic);
				if (list == null){
					list = new ArrayList();
					proxies.put(generic, list);
				}
				list.add(link.L);
			}
		}
				
		//Execute mass height update on each remote machine
		if (!proxies.isEmpty()){
			Command update = new Command(
				Links.className, "_resortLinks",
				new String[]{"int", "int", "int", Links.classNameArr},
				new Object[]{original.webID, original.height, newHeight, null}
			);
			for (Entry<RemoteAddress, ArrayList<Links>> proxy: proxies.entrySet()){
				update.setBaseParameter(3, proxy.getValue().toArray(new Links[proxy.getValue().size()]));
				Communicator.request(proxy.getKey(), update, false);
			}
		}
		
		//Change references on this computer (must come after remote stuff)
		if (!here.isEmpty())
			Links._resortLinks(original.webID, original.height, newHeight, here.toArray(new Links[here.size()]));
		//No references, we're safe to update
		else original.height = newHeight;
	}
	protected static void _resortLinks(int webId, int oldHeight, int newHeight, Links[] toUpdate){
		//All these links will be on this machine; if not, we did something wrong
		//We merge all duplicate proxy/real node references into one pointer
		ArrayList<HeightUpdate> reinsert = new ArrayList();
		for (Links l: toUpdate){
			if (l instanceof LinksProxy)
				System.err.println("_resortLinks will fail! This should not happen");
			reinsert.add(l._removeOutdatedLink(webId, oldHeight, newHeight));
		}
		
		//Re-insert the one pointer to rule them all
		Node pointer = null;
		for (int i=0, l=toUpdate.length; i<l; i++){
			HeightUpdate update = reinsert.get(i);
			if (update != null){
				//Change the height, if we haven't already
				if (pointer == null){
					pointer = update.pointer;
					pointer.height = newHeight;
				}
				//Re-insert
				if (update.foldRef != null)
					toUpdate[i].update(null, pointer, update.foldRef);
				if (update.neighborRef != null)
					toUpdate[i].update(null, pointer, update.neighborRef);
			}
		}
	}
	private HeightUpdate _removeOutdatedLink(int webID, int oldHeight, int newHeight){
		/* Since height makes up part of the key for the TreeSets, changing height
			poses a foreboding challenge. If the object is a reference/pointer in
			multiple TreeSets, changing the pointer in one will break retrieval
			from another. To fix this, we'll remove all items, change the key, and
			then re-insert.
			 
			Proxy nodes are serialized, so there may be multiple references that
			actually point to the same remote node. We merge all duplicate references
			and just use a single 'pointer'
		*/
		Node pointer = null;
		Type foldRef = null, neighRef = null;
		
		//Compile list of fold references; only one of them will have a reference
		if (fold != null && fold.webID == webID){
			pointer = fold;
			foldRef = Type.FOLD;
		}
		else if (surrogateFold != null && surrogateFold.webID == webID){
			pointer = surrogateFold;
			foldRef = Type.SFOLD;
		}
		else if (inverseSurrogateFold != null && inverseSurrogateFold.webID == webID){
			pointer = inverseSurrogateFold;
			foldRef = Type.ISFOLD;
		}
		
		//Compile list of neighbor references; only one of the lists will have a reference, if any
		//(neighbor differs by one bit, sneighbor differs by two bits,
		// isneighbor same as sneighbor but you can't have sneighbor and isneighbor)
		Node faux_node = pointer == null ? new Node(webID, oldHeight) : pointer;
		SortedSet<Node> search = neighbors.subSet(faux_node, true, faux_node, true);
		if (search.isEmpty()){
			search = surrogateNeighbors.subSet(faux_node, true, faux_node, true);
			if (search.isEmpty()){
				search = inverseSurrogateNeighbors.subSet(faux_node, true, faux_node, true);
				if (!search.isEmpty())
					neighRef = Type.ISNEIGHBOR;
			}
			else neighRef = Type.SNEIGHBOR;
		}
		else neighRef = Type.NEIGHBOR;
		//Remove from neighbor list; SortedSet is backed by the actual TreeSet
		if (!search.isEmpty()){
			pointer = search.first();
			search.remove(pointer);
		}
		
		//Remove the reference from the "all links" TreeSet
		if (pointer != null){
			highest.remove(pointer);
			return new HeightUpdate(foldRef, neighRef, pointer);
		}
		
		return null;
	}
	private class HeightUpdate{
		public Type foldRef, neighborRef;
		public Node pointer;
		public HeightUpdate(Type foldRef, Type neighborRef, Node pointer){
			this.foldRef = foldRef;
			this.neighborRef = neighborRef;
			this.pointer = pointer;
		}
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
	 * Removes all neighbors from the node
	 */
	protected void removeAllNeighbors(){
		highest.removeAll(neighbors);
		neighbors.clear();
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
	public void setWriteRealLinks(boolean writeRealLinks){
		this.writeRealLinks = writeRealLinks;
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
		return highest.isEmpty() ? null : highest.last();
	}
	/**
	 * Gets the lowest node out of all the connections
	 * @return a node
	 */
	public Node getLowestLink(){
		return highest.isEmpty() ? null : highest.first();
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
		if(writeRealLinks){
			setWriteRealLinks(false);
			return this;
		}
		return new LinksProxy(UID);
	}
	public Object readResolve() throws ObjectStreamException {
		return this;
	}
	public LinksImmutable convertToImmutable(){
		return new LinksImmutable(this);
	}
}