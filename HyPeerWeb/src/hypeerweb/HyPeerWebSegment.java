package hypeerweb;

import hypeerweb.visitors.SendVisitor;
import hypeerweb.visitors.BroadcastVisitor;
import java.util.Random;
import java.util.TreeMap;
import validator.HyPeerWebInterface;

/**
 * The Great HyPeerWeb
 * @param <T> The Node type for this HyPeerWeb instance
 * @author isaac
 */
public class HyPeerWebSegment<T extends Node> extends Node implements HyPeerWebInterface{
	private Database db = null;
	private TreeMap<Integer, Node> nodes;
	private HyPeerWebState state;
	//Random number generator for getting random nodes
	private static final Random rand = new Random();
	private static SendVisitor randVisitor;
	//Error messages
	private static final Exception
			addNodeErr = new Exception("Failed to add a new node"),
			removeNodeErr = new Exception("Failed to remove a node"),
			clearErr = new Exception("Failed to clear the HyPeerWeb"),
			replaceErr = new Exception("Failed to replace a node. Warning! Your HyPeerWeb is corrupted.");
	
	/**
	 * Constructor for initializing the HyPeerWeb with default Node values
	 * @param dbName should we sync our HyPeerWeb to a database;
	 *	Warning! Database access can be very slow (e.g. "HyPeerWeb.sqlite")
	 * @param seed the random seed number for getting random nodes; use -1
	 *	to get a pseudo-random seed
	 * @throws Exception if there was a database error
	 * @author isaac
	 */
	public HyPeerWebSegment(String dbName, long seed) throws Exception{
		this(dbName, seed, 0, 0);
	}
	/**
	 * Constructor for initializing the HyPeerWeb with defined Node values
	 * @param dbName should we sync our HyPeerWeb to a database;
	 *	Warning! Database access can be very slow (e.g. "HyPeerWeb.sqlite")
	 * @param seed the random seed number for getting random nodes; use -1
	 *	to get a pseudo-random seed
	 * @param webID the node webID, if it has one
	 * @param height the node height, if it has one
	 * @throws Exception if there was a database error
	 * @author isaac
	 */
	public HyPeerWebSegment(String dbName, long seed, int webID, int height) throws Exception{
		super(0, 0);
		if (dbName != null){
			db = Database.getInstance(dbName);
			nodes = db.getAllNodes();
		}
		else nodes = new TreeMap();
		if (seed != -1)
			rand.setSeed(seed);
	}
	
	/**
	 * Removes the node of specified webid
	 * @param webid the webid of the node to remove
	 * @return the removed node, or null if it doesn't exist
	 * @throws Exception if it fails to remove the node
	 */
	public T removeNode(int webid) throws Exception{
		return removeNode(getNode(webid));
	}
	/**
	 * Removes the node
	 * @param n the node to remove
	 * @return the removed node, or null if it doesn't exist in the HyPeerWeb
	 * @throws Exception if it fails to remove the node
	 */
	public T removeNode(T n) throws Exception{
		//Make sure Node exists in HyPeerWeb
		//TODO, make this here work for more stuff
		if (n == null || !nodes.containsValue(n))
			return null;
		state.removeNode(this, n);
		return n;
	}
	/**
	 * Removes all nodes from HyPeerWeb
	 * @throws Exception if it fails to clear the HyPeerWeb
	 */
	public void removeAllNodes(){
		(new BroadcastVisitor(){
			@Override
			public void performOperation(Node n) {
				HyPeerWebSegment seg = (HyPeerWebSegment) n;
				if (seg.db != null && !seg.db.clear())
					throw clearErr;
				seg.nodes = new TreeMap<>();
			}
		}).visit(this);
	}
	
	/**
	 * Adds a new node to the HyPeerWeb
	 * @return the new node
	 * @throws Exception if it fails to add a node
	 */
	public T addNode() throws Exception{
		return (T) state.addNode(this);
	}
		
	private enum HyPeerWebState{
		//No nodes
		HAS_NONE {
			@Override
			public Node addNode(HyPeerWebSegment web) throws Exception{
				//Use a proxy, if the request came from another segment
				Node first = new Node(0, 0);
				if (web.db != null && !web.db.addNode(first))
					throw HyPeerWebSegment.addNodeErr;
				web.nodes.put(0, first);
				//broadcast state change to HAS_ONE
				web.changeState(HAS_ONE);
				return first;
			}
			@Override
			public void removeNode(HyPeerWebSegment web, Node n) throws Exception{
				//Throw an error; this shouldn't happen
				throw removeNodeErr;
			}
		},
		//Only one node
		HAS_ONE {
			@Override
			public Node addNode(HyPeerWebSegment web) throws Exception{
				//Use a proxy, if the request came from another segment
				//broadcast state change to HAS_MANY
				//handle special case
				Node sec = new Node(1, 1),
					first = (Node) web.nodes.firstEntry().getValue();
				//Update the database first
				if (web.db != null) {
					web.db.beginCommit();
					web.db.addNode(sec);
					web.db.setHeight(0, 1);
					//reflexive folds
					web.db.setFold(0, 1);
					web.db.setFold(1, 0);
					//reflexive neighbors
					web.db.addNeighbor(0, 1);
					web.db.addNeighbor(1, 0);
					if (!web.db.endCommit())
						throw addNodeErr;
				}
				//Update java struct
				{
					first.setHeight(1);
					first.L.setFold(sec);
					sec.L.setFold(first);
					first.L.addNeighbor(sec);
					sec.L.addNeighbor(first);
					web.nodes.put(1, sec);
					web.changeState(HAS_MANY);
					return sec;
				}
			}
			@Override
			public void removeNode(HyPeerWebSegment web, Node n) throws Exception{
				//broadcast state change to HAS_NONE
				//handle special case
				if (web.db != null && !web.db.clear())
					throw clearErr;
				web.nodes = new TreeMap<>();
				web.changeState(HAS_NONE);
			}
		},
		//More than one node
		HAS_MANY {
			@Override
			public Node addNode(HyPeerWebSegment web) throws Exception{
				//Use a proxy, if the request came from another segment
				Node child = web.getRandomNode().findInsertionNode().addChild(web.db);
				if (child == null)
					throw addNodeErr;
				//Node successfully added!
				web.nodes.put(child.getWebId(), child);
				return child;
			}
			@Override
			public void removeNode(HyPeerWebSegment web, Node n) throws Exception{
				//If the HyPeerWeb has more than two nodes, remove normally
				int size = web.nodes.size();
				Node last, first = null;
				if (size > 2 ||
					//We can get rid of the rest of these checks if we end
					//up storing proxy nodes in "nodes"
					//Basically, we're trying to find a node with webID > 1 or height > 1
					(last = (Node) web.nodes.lastEntry().getValue()).getWebId() > 1 ||
					//The only nodes left are 0 and 1; check their heights to see if they have children
					last.getHeight() > 1 ||
					(size == 2 && ((Node) web.nodes.firstEntry().getValue()).getHeight() > 1) ||
					//The only other possibility is if we have one node, with a proxy child
					(size == 1 && last.L.getHighestLink().getWebId() > 1))
				{
					//Find a disconnection point
					Node replace = web.getRandomNode().findDisconnectNode().disconnectNode(web.db);
					if (replace == null)
						throw removeNodeErr;
					//Remove node from list of nodes
					web.nodes.remove(replace.getWebId());
					//Replace the node to be deleted
					if (!n.equals(replace)){
						int newWebID = n.getWebId();
						web.nodes.remove(newWebID);
						web.nodes.put(newWebID, replace);
						if (!replace.replaceNode(web.db, n))
							throw replaceErr;
					}
					web.changeState(HAS_MANY);
				}
				//If the broadcastStateChangeentire HyPeerWeb has only two nodes
				else{
					//handle special case
					//broadcast state change to HAS_ONE
					last = n.getNeighbors()[0];
					//Save the remaining node's attributes
					Attributes attrs = last.data;
					web.removeAllNodes();
					web.addFirstNode().data = attrs;
					web.changeState(HAS_ONE);
				}				
			}
		};
		public abstract Node addNode(HyPeerWebSegment web) throws Exception;
		public abstract void removeNode(HyPeerWebSegment web, Node n) throws Exception;
	}
	private void changeState(final HyPeerWebState state){
		(new BroadcastVisitor(){
			@Override
			public void performOperation(Node n) {
				((HyPeerWebSegment) n).state = state;
			}
		}).visit(this);
	}
	
	// <editor-fold defaultstate="collapsed" desc="GETTERS">
	/**
	 * Retrieves a random node in the HyPeerWeb
	 * @return a random node; null, if there are no nodes
	 * @author John, Josh
	 */
	public T getRandomNode(){
		//Always start at Node with WebID = 0
		if (nodes.isEmpty())
			return null;
		Node first = nodes.firstEntry().getValue();
		randVisitor = new SendVisitor(rand.nextInt(Integer.MAX_VALUE), true);
		randVisitor.visit(first);
		return (T) randVisitor.getFinalNode();
	}
	@Override
	public T[] getOrderedListOfNodes() {
		return (T[]) nodes.values().toArray(new Node[nodes.size()]);
	}
	/**
	 * Retrieve a node with the specified webid
	 * @return the node with the specified webid; otherwise null
	 * @author isaac
	 */
	@Override
	public T getNode(int webId){
		return (T) nodes.get(webId);
	}
	/**
	 * Gets the first node in the HyPeerWeb
	 * @return node with webID = 0
	 */
	public T getFirstNode(){
		if (nodes.isEmpty())
			return null;
		return (T) nodes.firstEntry().getValue();
	}
	/**
	 * Gets the last node in the HyPeerWeb
	 * @return 
	 */
	public T getLastNode(){
		if (nodes.isEmpty())
			return null;
		return (T) nodes.lastEntry().getValue();
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
        for (Node n : nodes.values())
            builder.append(n);
        return builder.toString();
    }
	// </editor-fold>
}
