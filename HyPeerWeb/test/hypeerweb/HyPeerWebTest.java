/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hypeerweb;

import hypeerweb.visitors.SendVisitor;
import java.util.Random;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import validator.Validator;

/**
 * HyPeerWeb testing
 */
public class HyPeerWebTest {
	//Testing variables
	private final int
		MAX_SIZE = 500,				//Maximum HyPeerWeb size for tests
		TEST_EVERY = 1,				//How often to validate the HyPeerWeb for add/delete
		SEND_TESTS = 2000,			//How many times to test send operation
		RAND_SEED = 5;				//Seed for getting random nodes (use -1 for a random seed)
	private final boolean
		USE_DATABASE = false,		//Enables database syncing
		USE_GRAPH = true,			//Starts a new thread for drawing the HyPeerWeb
		TEST_DELETE = false;		//Tests deletion from the HyPeerWeb
	private HyPeerWeb web;
	
	public HyPeerWebTest() throws Exception{
		web = HyPeerWeb.getInstance(USE_DATABASE, USE_GRAPH, RAND_SEED);
	}
	
	/**
	 * Populates the HyPeerWeb with some nodes
	 */
	private String curTest;
	private boolean hasRestored = false, hasPopulated = false;
	@Before
	public void populate() throws Exception{
		//Restore the database, if we haven't already
		if (!hasRestored && USE_DATABASE){
			System.out.println("Restoring...");
			assertTrue((new Validator(web)).validate());
			web.removeAllNodes();
			hasRestored = true;
		}
		//Populate the DB with nodes, if needed
		//Add a bunch of nodes if it validates afterwards, methods should be working
		if (!hasPopulated || web.isEmpty()){
			System.out.println("Populating...");
			web.removeAllNodes();
			Node temp;
			int old_size = 0;
			for (int i=1; i<=MAX_SIZE; i++){
				if ((temp = web.addNode()) == null)
					throw new Exception("Added node should not be null!");
				if (web.getSize() != ++old_size)
					throw new Exception("HyPeerWeb is not the correct size");
				if (i % TEST_EVERY == 0)
					assertTrue((new Validator(web)).validate());
			}
			hasPopulated = true;
		}
	}
	public void begin(String type){
		curTest = type;
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
	}
	
	/**
	 * Test of removeNode method (from zero, every time)
	 */
	@Test
	public void testRemoveZero() throws Exception {
		begin("REMOVING ZERO");
		Node temp;
		int old_size = web.getSize();
		assert(old_size == MAX_SIZE);
		for (int i=1; i<=MAX_SIZE; i++){
			if ((temp = web.removeNode(web.getFirstNode())) == null)
				throw new Exception("Removed node should not be null!");
			if (web.getSize() != --old_size)
				throw new Exception("HyPeerWeb is not the correct size");
			if (i % TEST_EVERY == 0)
				assertTrue((new Validator(web)).validate());
		}
	}
	
	/**
	 * Test of removeNode method (picking randomly)
	 */
	@Test
	public void testRemoveRandom() throws Exception {
		begin("REMOVING RANDOM");
		Node temp, rand;
		int old_size = web.getSize();
		for (int i=1; i<=MAX_SIZE; i++){
			rand = web.getRandomNode();
			if ((temp = web.removeNode(rand)) == null)
				throw new Exception("Removed node should not be null!");
			if (web.getSize() != --old_size)
				throw new Exception("HyPeerWeb is not the correct size");
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
			f1 = web.getRandomNode();
			do{
				f2 = web.getRandomNode();
			} while (f2 == f1);
			SendVisitor x = new SendVisitor(f1.getWebId());
			x.visit(f2);
			assertTrue(x.wasFound());
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
			while (web.getNode(bad_id) != null)
				bad_id *= 3;
			SendVisitor x = new SendVisitor(bad_id);
			x.visit(web.getFirstNode());
			assertFalse(x.wasFound());
		}
	}
}
