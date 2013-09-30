
import java.util.Random;
import java.util.TreeSet;

/**
 * The Great HyPeerWeb Singleton
 * @author isaac
 */
public class HyPeerWeb implements HyPeerWebInterface {
	
	private static HyPeerWeb instance;
	TreeSet<Node> nodes;
	Database db;
	//Random number generator for getting random nodes
	Random rand;
	
	/**
	 * Private constructor for initializing the HyPeerWeb
	 * @author isaac
	 */
	private HyPeerWeb() throws Exception{
		nodes = new TreeSet<>();
		db = Database.getInstance();
		rand = new Random();
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
			nodes.add(first);
			db.addNode(first);
			return first;
		}
		//2) One node
		if (nodes.size() == 1){
			Node sec = new Node(1, 1),
				first = nodes.first();
			first.setHeight(1);
			first.setFold(sec);
			sec.setFold(first);
			nodes.add(sec);
			return sec;
		}
		
		//Otherwise, use the normal insertion algorithm
		Node child = this.getRandomInsertionNode().addChild(db);
		if (child == null)
			throw new Exception("Failed to add a new node");
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
