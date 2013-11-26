/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hypeerweb;


import hypeerweb.visitors.SendVisitor;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import validator.NodeInterface;
import validator.Validator;

/**
 * HyPeerWeb testing
 */
public class HyPeerWebTest {
	//Testing variables
	private static final int
		MAX_SIZE = 600,				//Maximum HyPeerWeb size for tests
		TEST_EVERY = 1,					//How often to validate the HyPeerWeb for add/delete
		SEND_TESTS = 2000,				//How many times to test send operation
		BROADCAST_TESTS = 120,			//How many times to test broadcast operation
		RAND_SEED = -1;					//Seed for getting random nodes (use -1 for a random seed)
	private static final String
		DB_NAME = null;					//Enables database syncing
	private static HyPeerWebSegment web;
	private static String curTest;
	private static boolean
		useDatabase = false,
		useGraph = true;
	
	//TODO: Fill in callback methods
	private Node.Listener RemoveAllListener = new Node.Listener () 
	{@Override
	public void callback(Node n)
	{}};
	private Node.Listener SendVisitorListener = new Node.Listener () 
	{@Override
	public void callback(Node n)
	{}};
	private Node.Listener ListNodesVisitorListener = new Node.Listener () 
	{@Override
	public void callback(Node n)
	{}};
	private Node.Listener RemoveListener = new Node.Listener () 
	{@Override
	public void callback(Node n)
	{}};
	private Node.Listener AddListener = new Node.Listener () 
	{@Override
	public void callback(Node n)
	{}};
	private Node.Listener GetListener = new Node.Listener () 
	{@Override
	public void callback(Node n)
	{}};
	
	
	public HyPeerWebTest() throws Exception{
		web = new HyPeerWebSegment(DB_NAME, RAND_SEED);
	}
	
	public int getSize()
	{
		return web.getSegmentNodeCache(1).nodes.size();
	}
	public NodeCache.Node getRandom()
	{
		NodeInterface[] nodeList = web.getSegmentNodeCache(1).getOrderedListOfNodes();
		Random random = new Random();
		return (NodeCache.Node) nodeList[random.nextInt()];
	}
	public NodeCache.Node getFirst()
	{
		return (NodeCache.Node) web.getSegmentNodeCache(1).getOrderedListOfNodes()[0];
	}
	public NodeCache.Node getAddedNode()
	{
		NodeInterface[] nodeList = web.getSegmentNodeCache(1).getOrderedListOfNodes();
		web.addNode(AddListener);
		NodeInterface[] nodeList2 = web.getSegmentNodeCache(1).getOrderedListOfNodes();
		for (int i=0; i < nodeList2.length ; i++)
		{
			NodeInterface other = null;
			for (int j=0; j < nodeList.length && other == null; j++)
				if (nodeList[j] == nodeList2[i])
					other = nodeList[j];
			if (other == null)
				return (NodeCache.Node)nodeList2[i];
		}
		
		return null;
	}
	
	/**
	 * Populates the HyPeerWeb with some nodes
	 */
	public void populate() throws Exception{
		//Restore the database, if we haven't already
		//*
		if (DB_NAME != null){
			System.out.println("Restoring...");
			try{
				if (!(new Validator(web.getSegmentNodeCache(0))).validate())
					throw new Exception("FATAL ERROR: Could not restore the old database");
			} catch (Exception e){
				System.out.println("The database <"+DB_NAME+"> must be corrupt. Did you previously force execution to stop?");
				System.out.println("Deleting the old database... Rerun the tests two more times to verify it works");
				Database badDB = Database.getInstance(DB_NAME);
				badDB.clear();
				throw e;
			}
			web.removeAllNodes(RemoveAllListener);
		}
		//*/
		//Populate the DB with nodes, if needed
		//Add a bunch of nodes if it validates afterwards, methods should be working
		if (web.isEmpty()){
			System.out.println("Populating...");
			web.removeAllNodes(RemoveAllListener);
			NodeCache.Node temp;
			int old_size = 0;
			for (int i=1; i<=MAX_SIZE; i++){
				if ((temp = getAddedNode()) == null)
					throw new Exception("Added node should not be null!");
				if (getSize() != ++old_size)
					throw new Exception("HyPeerWeb is not the correct size");
				if (i % TEST_EVERY == 0)
					assertTrue((new Validator(web.getSegmentNodeCache(0))).validate());
			}
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
	 * Test of addNode method
	 */
	@Test
	public void testAdd() throws Exception {
		//This is a dummy method for populate()
		//Don't remove this method
		begin("ADDING");
	}
	
	/**
	 * Test of removeNode method (from zero, every time)
	 */
	@Test
	public void testRemoveZero() throws Exception {
		begin("REMOVING ZERO");
		Node temp;
		int old_size = getSize();
		assert(old_size == MAX_SIZE);
		for (int i=1; i<=MAX_SIZE; i++){
			web.removeNode(0, RemoveListener);
			if (getSize() != --old_size)
				throw new Exception("HyPeerWeb is not the correct size");
			if (i % TEST_EVERY == 0)
				assertTrue((new Validator(web.getSegmentNodeCache(0))).validate());
		}
	}
	
	/**
	 * Test of removeNode method (picking randomly)
	 */
	@Test
	public void testRemoveRandom() throws Exception {
		begin("REMOVING RANDOM");
		NodeCache.Node temp, rand;
		int old_size = getSize();
		for (int i=1; i<=MAX_SIZE; i++){
			rand = getRandom();
			web.removeNode(rand.getWebId(), RemoveListener);
			if (getSize() != --old_size)
				throw new Exception("HyPeerWeb is not the correct size");
			if (i % TEST_EVERY == 0)
				assertTrue((new Validator(web.getSegmentNodeCache(0))).validate());
		}
	}
	
	/**
	 * Test of send visitor with valid target node
	 */
	@Test
	public void testSendValid() throws Exception {
		//Test send node
		begin("SENDING VALID");
		NodeCache.Node f1, f2, found;
		for (int j=0; j<SEND_TESTS; j++){
			f1 = getRandom();
			do{
				f2 = getRandom();
			} while (f2 == f1);
			SendVisitor x = new SendVisitor(f1.getWebId(), SendVisitorListener);
			//TODO: Figure out how to get a node
			x.visit(f2);
			found = x.getFinalNode();
			if (found == null){
				System.out.println("f1 = " + f1);
				System.out.println("f2 = " + f2);
			}
			assertNotNull(found);
			assert(found.getWebId() == f1.getWebId());
		}
	}
	
	/**
	 * Test of send visitor with invalid target node
	 */
	@Test
	public void testSendInvalid() throws Exception {
		begin("SENDING INVALID");
		Random r = new Random();
		for (int i=0; i<SEND_TESTS; i++){
			int bad_id = r.nextInt();
			while (web.getSegmentNodeCache(0).getNode(bad_id) != null)
				bad_id *= 3;
			SendVisitor x = new SendVisitor(bad_id, SendVisitorListener);
			//TODO: Figure out how to get a node
			x.visit(web.getFirstNode());
			assertNull(x.getFinalNode());
		}
	}
	
	@Test
	public void testBroadcast() throws Exception {
		begin("TESTING BROADCAST");
		for (int i=0; i<BROADCAST_TESTS; i++){
			NodeCache.Node origin = getRandom();
			//System.out.println("Starting with:"+origin);
			ListNodesVisitor x = new ListNodesVisitor(ListNodesVisitorListener);
			//TODO: Figure out how to get a node
			x.visit(web.getNode(origin.getWebId(), GetListener));
			if(x.getNodeList().size() < getSize()) {
				for(Node n : web.getAllSegmentNodes()) {
					if(!x.getNodeList().contains(n)){
						System.out.println("Missing: " + n);
					}
				}
			}
			assertTrue(x.getNodeList().size() == getSize());
			for(NodeCache.Node n : x.getNodeList()) {
				//TODO: Figure out how to get a node
				assertTrue(web.getNode(n.getWebId()) != null);
			}
			Set<NodeCache.Node> set = new HashSet<>(x.getNodeList());
			assertTrue(set.size() == x.getNodeList().size());
		}
	}
}
