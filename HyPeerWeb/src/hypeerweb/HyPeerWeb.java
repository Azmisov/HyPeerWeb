package hypeerweb;

import hypeerweb.graph.DrawingThread;
import hypeerweb.visitors.SendVisitor;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;
import validator.HyPeerWebInterface;

/**
 * The Great HyPeerWeb Singleton
 * @author isaac
 */
public class HyPeerWeb<T extends Node> implements HyPeerWebInterface {
	private static Database db = null;
	private static TreeMap<Integer, T> nodes;
	//Random number generator for getting random nodes
	private static final Random rand = new Random();
	private static SendVisitor randVisitor;
	//Error messages
	private static final Exception
			addNodeErr = new Exception("Failed to add a new node"),
			removeNodeErr = new Exception("Failed to remove a node"),
			clearErr = new Exception("Failed to clear the HyPeerWeb"),
			replaceErr = new Exception("Failed to replace a node. Warning! Your HyPeerWeb is corrupted.");
	//Draw a graph of the HyPeerWeb
	private DrawingThread graph;
	
	/**
	 * Private constructor for initializing the HyPeerWeb
	 * @param useDatabase should we sync our HyPeerWeb to a database;
	 *	Warning! Database access can be very slow
	 * @param useGraph is graph drawing enabled {@link #drawGraph(hypeerweb.Node)}
	 * @param seed the random seed number for getting random nodes; use -1
	 *	to get a pseudo-random seed
	 * @return a reference to the initialized HyPeerWeb singleton
	 * @throws Exception if there was a database error
	 * @author isaac
	 */
	public HyPeerWeb(boolean useDatabase, boolean useGraph, long seed) throws Exception{
		if (useDatabase){
			db = Database.getInstance();
			nodes = db.getAllNodes();
		}
		else nodes = new TreeMap<>();
		if (seed != -1)
			rand.setSeed(seed);
		if (useGraph)
			graph = new DrawingThread(this);
	}
	
	/**
	 * Removes the node of specified webid
	 * @param webid the webid of the node to remove
	 * @return the removed node, or null if it doesn't exist
	 * @throws Exception if it fails to remove the node
	 * @author isaac
	 */
	public T removeNode(int webid) throws Exception{
		return this.removeNode(getNode(webid));
	}
	/**
	 * Removes the node
	 * @param n the node to remove
	 * @return the removed node, or null if it doesn't exist in the HyPeerWeb
	 * @throws Exception if it fails to remove the node
	 * @author isaac
	 */
	public T removeNode(T n) throws Exception{
		//Make sure Node exists in HyPeerWeb
		if (n == null || !nodes.containsValue(n))
			return null;
		
		//special case with 1/2 nodes in HyPeerWeb
		//There are two special cases:
		//1) One node
		if (nodes.size() == 1){
			removeAllNodes();
			return n;
		}
		//2) Two nodes
		if (nodes.size() == 2)
			return removeSecondNode(n);
		
		//Find a disconnection point
		T replace = getRandomNode().findDisconnectNode().disconnectNode(db);
		if (replace == null)
			throw removeNodeErr;
		//Remove node from list of nodes
		nodes.remove(replace.getWebId());
		//Replace the node to be deleted
		if (!n.equals(replace)){
			int newWebID = n.getWebId();
			nodes.remove(newWebID);
			nodes.put(newWebID, replace);
			if (!replace.replaceNode(db, n))
				throw replaceErr;
		}
		return n;
	}
	/**
	 * Remove the second to last node
	 * @return the removed node
	 * @throws Exception if it fails to modify the database
	 */
	private T removeSecondNode(T n) throws Exception{		
		T last = n.getNeighbors()[0];
		//Save the remaining node's attributes
		HashMap<String, Object> attrs = last.getAllAttributes();
		removeAllNodes();
		addFirstNode().setAllAttributes(attrs);
		return n;
	}
	/**
	 * Removes all nodes from HyPeerWeb
	 * @author isaac
	 * @throws Exception if it fails to clear the HyPeerWeb
	 */
	public void removeAllNodes() throws Exception{
		if (db != null && !db.clear())
			throw clearErr;
		nodes = new TreeMap<>();
	}
	
	/**
	 * Adds a new node to the HyPeerWeb
	 * @return the new node
	 * @author guy, brian, isaac
	 * @throws Exception if it fails to add a node
	 */
	public T addNode() throws Exception{
		//There are two special cases:
		//1) No nodes
		if (nodes.isEmpty())
			return addFirstNode();
		//2) One node
		if (nodes.size() == 1)
			return addSecondNode();
		
		//Otherwise, use the normal insertion algorithm
		T child = getRandomNode().findInsertionNode().addChild(db);
		if (child == null)
			throw addNodeErr;
		//Node successfully added!
		nodes.put(child.getWebId(), child);
		return child;
	}
	/**
	 * Special case to handle adding the first node
	 * @return the new node
	 * @author isaac
	 */
	private T addFirstNode() throws Exception{
		T first = new Node(0, 0);
		if (db != null && !db.addNode(first))
			throw addNodeErr;
		nodes.put(0, first);
		return first;
	}
	/**
	 * Special case to handle adding the second node
	 * @return the new node
	 * @author isaac
	 */
	private T addSecondNode() throws Exception{
		T sec = new Node(1, 1),
			first = nodes.firstEntry().getValue();
		//Update the database first
		if (db != null) {
			db.beginCommit();
			db.addNode(sec);
			db.setHeight(0, 1);
			//reflexive folds
			db.setFold(0, 1);
			db.setFold(1, 0);
			//reflexive neighbors
			db.addNeighbor(0, 1);
			db.addNeighbor(1, 0);
			if (!db.endCommit())
				throw addNodeErr;
		}
		//Update java struct
		{
			first.setHeight(1);
			first.L.setFold(sec);
			sec.L.setFold(first);
			first.L.addNeighbor(sec);
			sec.L.addNeighbor(first);
			nodes.put(1, sec);
			return sec;
		}
	}
	
	/**
	 * Retrieves a random node in the HyPeerWeb
	 * @return a random node; null, if there are no nodes
	 * @author John, Josh
	 */
	public T getRandomNode(){
		//Always start at Node with WebID = 0
		if (nodes.isEmpty())
			return null;
		T first = nodes.firstEntry().getValue();
		randVisitor = new SendVisitor(rand.nextInt(Integer.MAX_VALUE), true);
		randVisitor.visit(first);
		return randVisitor.getFinalNode();
	}
	
	//GRAPHING
	/**
	 * Draws a graph of the HyPeerWeb at a node
	 * @param n the node to start at
	 * @throws Exception 
	 */
	public void drawGraph(Node n) throws Exception{
		if (graph == null){
			System.out.println("HyPeerWeb graphing is disabled");
			return;
		}
		if (n == null) return;
		graph.start(n);
		synchronized (this){
			this.wait();
		}
	}
		
	//VALIDATION
	@Override
	public T[] getOrderedListOfNodes() {
		return nodes.values().toArray(new T[nodes.size()]);
	}
	/**
	 * Retrieve a node with the specified webid
	 * @return the node with the specified webid; otherwise null
	 * @author isaac
	 */
	@Override
	public T getNode(int webId){
		return nodes.get(webId);
	}
	/**
	 * Gets the first node in the HyPeerWeb
	 * @return node with webID = 0
	 */
	public T getFirstNode(){
		if (nodes.isEmpty())
			return null;
		return nodes.firstEntry().getValue();
	}
	/**
	 * Gets the last node in the HyPeerWeb
	 * @return 
	 */
	public T getLastNode(){
		if (nodes.isEmpty())
			return null;
		return nodes.lastEntry().getValue();
	}
	/**
	 * Get the size of the HyPeerWeb
	 * @return the number of nodes in the web
	 */
	public int getSize(){
		return nodes.size();
	}
	/**
	 * Is the HyPeerWeb empty?
	 * @return true if it is empty
	 */
	public boolean isEmpty(){
		return nodes.isEmpty();
	}
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (T n : nodes.values())
            builder.append(n);
        return builder.toString();
    }
}
