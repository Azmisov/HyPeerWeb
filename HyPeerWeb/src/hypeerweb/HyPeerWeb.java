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
	 * Adds a new node to the HyPeerWeb
	 * @return the new node
	 * @author guy, brian, isaac
	 */
	public Node addNode() throws Exception{
		//There are two special cases:
		//1) No nodes
		if (nodes.isEmpty()){
			Node first = new Node(0, 0);
			if (!db.addNode(first))
				throw addNodeErr;
			nodes.add(first);
			return first;
		}
		//2) One node
		if (nodes.size() == 1){
			Node sec = new Node(1, 1),
				first = nodes.first();
			sec.fold = first;
			//Update the database first
			db.beginCommit();
			db.addNode(sec);
			db.setFold(first.webID, sec.webID);
			db.setHeight(first.webID, 1);
			if (!db.endCommit())
				throw addNodeErr;
			//Update java struct
			first.height = 1;
			first.fold = sec;
			nodes.add(sec);
			return sec;
		}
		
		//Otherwise, use the normal insertion algorithm
		Node child = this.getRandomInsertionNode().addChild(db);
		if (child == null)
			throw addNodeErr;
		//Node successfully added!
		nodes.add(child);
		return child;
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
