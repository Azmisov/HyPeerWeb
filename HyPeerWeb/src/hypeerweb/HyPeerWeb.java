package hypeerweb;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;
import validator.HyPeerWebInterface;

/**
 * The Great HyPeerWeb Singleton
 * @author isaac
 */
public class HyPeerWeb implements HyPeerWebInterface {
	private static HyPeerWeb instance;
	private static Database db;
	private static TreeSet<Node> nodes;
	private static boolean disableDB = false;
	//Random number generator for getting random nodes
	private static Random rand = new Random();
	//Error messages
	private static Exception
			addNodeErr = new Exception("Failed to add a new node"),
			removeNodeErr = new Exception("Failed to remove a node"),
			clearErr = new Exception("Failed to clear the HyPeerWeb");
	//Trace random insertion for debugging purposes
	private static ArrayList<Long> randTrace;
	private static Iterator<Long> randTraceIter;
	private static enum TraceMode{ ON, OFF, READ }
	private static TraceMode traceMode = TraceMode.OFF;
	private static String traceLogName = "InsertionTrace.log";
	
	/**
	 * Private constructor for initializing the HyPeerWeb
	 * @author isaac
	 */
	private HyPeerWeb() throws Exception{
		db = Database.getInstance();
		nodes = db.getAllNodes();
	}
	/**
	 * Retrieve the HyPeerWeb singleton
	 * @return the singleton
	 * @author isaac
	 */
	public static HyPeerWeb getInstance() throws Exception{
		if (instance != null)
			return instance;
		instance = new HyPeerWeb();
		return instance;
	}
	/**
	 * Disables any writes to the database
	 * @author isaac
	 */
	public void disableDatabase(){
		disableDB = true;
	}
	
	/**
	 * Removes the node of specified webid
	 * @param webid the webid of the node to remove
	 * @return the removed node, or null if it doesn't exist
	 * @throws Exception if it fails to remove the node
	 * @author isaac
	 */
	public Node removeNode(int webid) throws Exception{
		return this.removeNode(getNode(webid));
	}
	/**
	 * Removes the node
	 * @param webid the node to remove
	 * @return the removed node, or null if it doesn't exist
	 * @throws Exception if it fails to remove the node
	 * @author isaac
	 */
	public Node removeNode(Node n) throws Exception{
		//Make sure Node exists in HyPeerWeb
		if (n == null || !nodes.contains(n))
			return null;
		
		//special case with 1/2 nodes in HyPeerWeb
		//There are two special cases:
		//1) One node
		if (nodes.size() == 1)
			return removeFirstNode();
		//2) Two nodes
		if (nodes.size() == 2)
			return removeSecondNode(n);
		
		//Find a disconnection point
		Node replace = this.getRandomNode().findDisconnectNode().disconnectNode(db);
		if (replace == null)
			throw removeNodeErr;
		//Remove node from list of nodes
		nodes.remove(replace);
		//Replace the node to be deleted
		if (!n.equals(replace))
			replace.replaceNode(n);
		return n;
	}
	/**
	 * 
	 * @return
	 * @throws Exception 
	 */
	private void removeFirstNode() throws Exception{
		if(!disableDB)
			removeAllNodes();
		else
			nodes = new TreeSet<>();
	}
	/**
	 * 
	 * @return
	 * @throws Exception 
	 */
	private Node removeSecondNode(Node n) throws Exception{
		Node last = n.getNeighbors()[0];
		last.setHeight(0);
		last.setFold(null);
		last.setSurrogateFold(null);
		last.setInverseSurrogateFold(n);
	}
	/**
	 * Removes all nodes from HyPeerWeb
	 * @author isaac
	 * @throws Exception if it fails to clear the HyPeerWeb
	 */
	public void removeAllNodes() throws Exception{
		if (!db.clear())
			throw clearErr;
		nodes = new TreeSet<>();
	}
	
	/**
	 * Adds a new node to the HyPeerWeb
	 * @return the new node
	 * @author guy, brian, isaac
	 * @throws Exception if it fails to add a node
	 */
	public Node addNode() throws Exception{
		//There are two special cases:
		//1) No nodes
		if (nodes.isEmpty())
			return addFirstNode();
		//2) One node
		if (nodes.size() == 1)
			return addSecondNode();
		
		//Otherwise, use the normal insertion algorithm
		Node child = this.getRandomNode().findInsertionNode().addChild(disableDB ? null : db);
		if (child == null)
			throw addNodeErr;
		//Node successfully added!
		nodes.add(child);
		//System.out.println();
		return child;
	}
	/**
	 * Special case to handle adding the first node
	 * @return the new node
	 * @author isaac
	 */
	private Node addFirstNode() throws Exception{
		Node first = new Node(0, 0);
		if (!disableDB && !db.addNode(first))
			throw addNodeErr;
		nodes.add(first);
		return first;
	}
	/**
	 * Special case to handle adding the second node
	 * @return the new node
	 * @author isaac
	 */
	private Node addSecondNode() throws Exception{
		Node sec = new Node(1, 1),
			first = nodes.first();
		//Update the database first
		if (!disableDB) {
			int firstID = first.getWebId(),
				secID = sec.getWebId();
			db.beginCommit();
			db.addNode(sec);
			db.setHeight(firstID, 1);
			//reflexive folds
			db.setFold(firstID, secID);
			db.setFold(secID, firstID);
			//reflexive neighbors
			db.addNeighbor(firstID, secID);
			db.addNeighbor(secID, firstID);
			if (!db.endCommit())
				throw addNodeErr;
		}
		//Update java struct
		{
			first.setHeight(1);
			first.setFold(sec);
			sec.setFold(first);
			first.addNeighbor(sec);
			sec.addNeighbor(first);
			nodes.add(sec);
			return sec;
		}
	}
	
	/**
	 * Retrieves a random node in the HyPeerWeb that is a valid
	 * insertion point
	 * @return a random node that is a valid insertion point
	 * @author John, Josh
	 */
	public Node getRandomNode(){
		long index;
		if (traceMode == TraceMode.READ){
			index = randTraceIter.next();
			//We've reached the end of the log file; start recording
			if (!randTraceIter.hasNext())
				traceMode = TraceMode.ON;
		}
		else{
			index = rand.nextInt(Integer.MAX_VALUE);
			index *= Integer.MAX_VALUE;
			index += rand.nextInt(Integer.MAX_VALUE);
			//Record this insertion point, if tracing is enabled
			if (traceMode == TraceMode.ON)
				randTrace.add(index);
		}
		//Always start at Node with WebID = 0
		return nodes.first().searchForNode(index);
	}
	
	//DEBUGGING
	/**
	 * Begins tracing random insertion points
	 */
	public void startTrace(){
		traceMode = TraceMode.ON;
		randTrace = new ArrayList<>();
	}
	/**
	 * Stops tracing insertion points and saves it to a log file
	 * @return true on success
	 */
	public boolean endTrace(){
		if (traceMode != TraceMode.ON)
			return true;
		traceMode = TraceMode.OFF;
		try (FileOutputStream fos = new FileOutputStream(traceLogName);
			 ObjectOutputStream oos = new ObjectOutputStream(fos))
		{
			oos.writeObject(randTrace);
			return true;
		} catch (Exception e){
			return false;
		}
	}
	/**
	 * Loads a list of insertion points from the log file
	 * After calling, getRandomInsertionNode will follow the log's trace
	 * @return true on success
	 */
	public boolean loadTrace(){
		try (FileInputStream fis = new FileInputStream("InsertionTrace.log");
			 ObjectInputStream ois = new ObjectInputStream(fis))
		{
			randTrace = (ArrayList<Long>) ois.readObject();
			randTraceIter = randTrace.iterator();
			if (randTraceIter.hasNext())
				traceMode = TraceMode.READ;
			return true;
		} catch (Exception e){
			return false;
		}
	}
	
	//VALIDATION
	@Override
	public Node[] getOrderedListOfNodes() {
		return nodes.toArray(new Node[nodes.size()]);
	}
	/**
	 * Retrieve a node with the specified webid
	 * @param webid the webid of the node
	 * @return the node with the specified webid; otherwise null
	 * @author isaac
	 */
	@Override
	public Node getNode(int webId){
		Node n = nodes.floor(new Node(webId, 0));
		if (n.getWebId() != webId)
			return null;
		return n;
	}
}
