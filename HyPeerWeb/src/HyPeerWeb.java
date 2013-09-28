
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;

/**
 * The Great HyPeerWeb Singleton
 * @author isaac
 */
public class HyPeerWeb {
	private static HyPeerWeb instance;
	Set<Node> nodes;
	Database db;
	
	/**
	 * Private constructor for initializing the HyPeerWeb
	 * @author isaac
	 */
	private HyPeerWeb() throws Exception{
		nodes = new TreeSet<>();
		db = Database.getInstance();
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
	public Node addNode(){
		//Handle hypeerweb with no nodes
		if (nodes.isEmpty()){
			Node first = new Node(0, 0);
			nodes.add(first);
			return first;
		}
		
		//Find parent node
		Node parent = findRandomNode().findInsertionNode();
		
		//Create child node and set its height
		int height = parent.getHeight()+1,
			webid = (int) (Math.pow(10, height-1) + parent.getWebID());
		Node child = new Node(webid, height);
		parent.setHeight(height);
		nodes.add(child);
		
		//Set neighbours (Guy)
		parent.hasChild(true);//sets parents hadChild value to true
                ArrayList<Node> list;
                //makes the parent's ISNs the child's neighbors
                list = parent.getInverseSurrogateNeighbors();
                for (Node n:list){
                    child.addNeighbor(n);
                }
                //adds a neighbor of parent as a surrogate neighbor of child if nieghbor is childless
                //and makes child an isn of neighbor
                list = parent.getNeighbors();
                for(Node n:list){
                    if(!n.hasChild()){ 
                        child.addSurrogateNeighbor(n);
                        n.addInverseSurrogateNeighbor(child);
                    }
                }
                parent.addNeighbor(child);
                child.addNeighbor(parent);
        
		//Set folds (Brian/Isaac)
		
		return child;
	}
	
	/**
	 * Picks a random node in the HyPeerWeb
	 * @return the random node
	 * @author john
	 */
	private Node findRandomNode(){
            if (nodes.isEmpty())
		return null;
            
            Random rand = new Random();
            long index = rand.nextInt(Integer.MAX_VALUE);
            index *= Integer.MAX_VALUE;
            index += rand.nextInt(Integer.MAX_VALUE);
            return ((Node)nodes.toArray()[0]).findInsertionStartPoint(index);
	}
}
