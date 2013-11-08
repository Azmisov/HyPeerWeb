package jamie;

import jamie.gui.SendVisitor;
import jamie.model.HyperWeb;
import jamie.model.Node;
import jamie.model.WebID;
import jamie.simulation.NodeInterface;
import jamie.simulation.Validator;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases for Jamie's group's HyPeerWeb
 * NOTES FOR THE OTHER GROUP:
 *		- no good way to get HyperWeb's size
 *		- addNode/removeNode should return the node
 *		- removeNode() does not let us specify which node we want to remove
 *		- no way to test if database works
 *		- your static Node methods should probably go in the HyperWeb class
 * @author isaac
 */
public class JamieHyPeerWebTest {
	//Testing variables
	private static final int
		MAX_SIZE = 500,					//Maximum HyPeerWeb size for tests
		TEST_EVERY = 1,					//How often to validate the HyPeerWeb for add/delete
		SEND_TESTS = 500;				//How many times to test send operation
	private static HyperWeb web;
	private static String curTest;
	private static boolean hasPopulated;
	private boolean sendFound = false;
	private Node found;
	
	public JamieHyPeerWebTest() throws Exception{
		web = new HyperWeb();
		hasPopulated = false;
	}
	
	/**
	 * Populates the HyPeerWeb with some nodes
	 */
	public void populate() throws Exception{
		//Populate the DB with nodes, if needed
		//Add a bunch of nodes if it validates afterwards, methods should be working
		if (!hasPopulated || web.getOrderedListOfNodes().length == 0){
			System.out.println("Populating...");
			while (web.getOrderedListOfNodes().length > 0)
				web.removeNode();
			Node temp;
			int old_size = 0, new_size;
			for (int i=1; i<=MAX_SIZE; i++){
				web.addNode();
				new_size = web.getOrderedListOfNodes().length;
				if (new_size != ++old_size)
					throw new Exception("HyPeerWeb is not the correct size: expected "+old_size+" but found "+new_size);
				//if (i % TEST_EVERY == 0)
					//assertTrue((new Validator(web)).validate());
			}
			hasPopulated = true;
		}
	}
	public void begin(String type) throws Exception{
		curTest = type;
		populate();
		System.out.println("BEGIN:\t"+type);
	}
	@After
	public void end(){
		if (curTest != null){
			System.out.println("END:\t"+curTest);
			curTest = null;
		}
	}
	
	/**
	 * Equivalent Partitions:
	 * 1. Send to node not in hypeerweb. (Tested with testSendInvalid()).
	 * 2. Send to self.
	 * 3. Send to immediate connection.
	 * 4. Send to node in another sand pile, where the send method could get stuck
	 *	  in a local maxima. (Tested with testSendValid())
	 */
	@Test
	public void testSendToEquivalentPartitions() throws Exception{
		begin("Send to equivalent partitions");
		Node node = (Node) web.getNode(0);
		testSend(node, node);//partition 2
		testSend(node, (Node) node.getNeighbors()[0]);//partition 3
	}
	
	public int scoreWebIdMatch(Node node, int idSearch){
		return Integer.bitCount(~(node.getWebId() ^ idSearch));
	}
	
	/**
	 * Test of addNode method
	 */
	//@Test
	public void testAdd() throws Exception {
		//This is a dummy method for populate()
		//Don't remove this method
		begin("ADDING");
	}
	
	/**
	 * Test of removeNode method (from zero, every time)
	 */
	//@Test
	public void testRemoveRandom() throws Exception {
		begin("REMOVING RANDOM");
		Node temp;
		int old_size = web.getOrderedListOfNodes().length,
			new_size;
		assert(old_size == MAX_SIZE);
		for (int i=1; i<=MAX_SIZE; i++){
			web.removeNode();
			new_size = web.getOrderedListOfNodes().length;
			if (new_size != --old_size)
				throw new Exception("HyPeerWeb is not the correct size: expected "+old_size+" but found "+new_size);
			if (i % TEST_EVERY == 0)
				assertTrue((new Validator(web)).validate());
		}
	}
	
	/**
	 * Test of send visitor with valid target node
	 */
	@Test
	public void testSendValid() throws Exception {
		//Test send node
		begin("SENDING VALID");
		Node f1, f2;
		for (int j=0; j<SEND_TESTS; j++){
			
			f1 = Node.getRandomNode();
			do{
				f2 = Node.getRandomNode();
			} while (f2 == f1);
			testSend(f1, f2);
		}
	}
	
	private void testSend(Node f1, Node f2){
		sendFound = false;
		found = null;
		
		SendVisitor x = new SendVisitor(f1.getWebID()){
			protected void targetOperation(Node node) {
				sendFound = true;
				found = node;
			}
		};
		x.visit(f2);
		assertTrue(sendFound);
		assertNotNull(found);
		assert(found.getWebId() == f1.getWebId());
	}
	
	/**
	 * Test of send visitor with invalid target node
	 */
	@Test
	public void testSendInvalid() throws Exception {
		begin("SENDING INVALID");
		Random r = new Random();
		for (int i=0; i<SEND_TESTS; i++){
			sendFound = false;
			int bad_id = r.nextInt() % 100;
			if(bad_id < 0)
				bad_id *= -1;
			while (web.getNode(bad_id) != null){
				bad_id *= 3;
			}
			SendVisitor x = new SendVisitor(new WebID(bad_id)){
				@Override
				protected void targetOperation(Node node) {
					sendFound = true;
				}
			};
			x.visit((Node) web.getNode(0));
			assertFalse(sendFound);
		}
	}
	
	/**
	 * The testBroadcast method uses visit starting at a specified node to visit
	 * node 0 and tell it to begin broadcasting.  This test method tests according
	 * to boundary value analysis by changing which node this process starts at.
	 * Boundary values:
	 * Starting visit at node 0.
	 * Starting visit at node 1.
	 * Starting visit at the second highest node.
	 * Starting visit at the highest node.
	 * @throws Exception 
	 */
	@Test
	public void testBroadcastWithBVA() throws Exception{
		begin("BroadCast with boundary value analysis");
		Node[] nodes = (Node[]) web.getOrderedListOfNodes();
		int result;
		int sizeOfWeb = nodes.length;
		
		result = testBroadcast(nodes[0]);
		assert(result == sizeOfWeb);
		result = testBroadcast(nodes[1]);
		assert(result == sizeOfWeb);
		result = testBroadcast(nodes[sizeOfWeb - 2]);
		assert(result == sizeOfWeb);
		result = testBroadcast(nodes[sizeOfWeb - 1]);
		assert(result == sizeOfWeb);
	}
	
	private int testBroadcast(Node node){
		ListNodesVisitor x = new ListNodesVisitor();
		x.visit(node);
		return x.getNodeList().size();
	}
}
