package hypeerweb;

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
	private static boolean disableDB;
	//Random number generator for getting random nodes
	private static Random rand;
	//Error messages
	private static Exception addNodeErr;
	
	/**
	 * Private constructor for initializing the HyPeerWeb
	 * @author isaac
	 */
	private HyPeerWeb() throws Exception{
		db = Database.getInstance();
		nodes = db.getAllNodes();
		rand = new Random();
		addNodeErr = new Exception("Failed to add a new node");
		disableDB = false;
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
	 * Removes all nodes from HyPeerWeb
	 * @author isaac
	 */
	public void deleteAllNodes(){
		if (!disableDB)
			db.clear();
		nodes = new TreeSet<>();
	}
	
	/**
	 * Adds a new node to the HyPeerWeb
	 * @return the new node
	 * @author guy, brian, isaac
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
		Node child = this.getRandomInsertionNode().addChild(disableDB ? null : db);
		if (child == null)
			throw addNodeErr;
		//Node successfully added!
		nodes.add(child);
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
	private Node getRandomInsertionNode(){
		long index = rand.nextInt(Integer.MAX_VALUE);
		index *= Integer.MAX_VALUE;
		index += rand.nextInt(Integer.MAX_VALUE);
		//Always start at Node with WebID = 0
		return nodes.first().searchForNode(index).findInsertionNode();
	}
	
	//VALIDATION
	@Override
	public Node[] getOrderedListOfNodes() {
		return nodes.toArray(new Node[nodes.size()]);
	}
	@Override
	public Node getNode(int webId){
		Node n = nodes.floor(new Node(webId, 0));
		if (n.getWebId() != webId)
			return null;
		return n;
	}
}
