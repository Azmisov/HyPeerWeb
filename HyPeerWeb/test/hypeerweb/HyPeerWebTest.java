/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hypeerweb;

import hypeerweb.visitors.SendVisitor;
import org.junit.Test;
import static org.junit.Assert.*;
import validator.Validator;

/**
 * HyPeerWeb testing
 */
public class HyPeerWebTest {
	//Testing variables
	private final int
		MAX_TESTS = 500,			//How many times to test add/delete
		TEST_EVERY = 1,				//How often to validate the HyPeerWeb for add/delete
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
	 * Test of addNode/removeNode method, of class HyPeerWeb.
	 */
	@Test
	public void testNodeAddRemove() throws Exception {
		int t = 0, i = 1;
		try{
			if (USE_DATABASE){
				System.out.println("Testing restore");
				assertTrue((new Validator(web)).validate()); //comment out this line to get new HyPeerWeb
				System.out.println("Done testing restore");
			}

			//Add a bunch of nodes, then remove them; if it validates afterwards, methods should be working
			web.removeAllNodes();
			boolean valid;
			int old_size = 0;
			Node temp;
			for (; t<(TEST_DELETE ? 2 : 1); t++){
				System.out.println("BEGIN "+(t == 0 ? "ADDING" : "DELETING")+" NODES");
				for (i=1; i<=MAX_TESTS; i++){
					//Add nodes first time around
					if (t == 0){
						if ((temp = web.addNode()) == null)
							throw new Exception("Added node should not be null!");
						if (web.getSize() != ++old_size)
							throw new Exception("HyPeerWeb is not the correct size");
					}
					//Then delete all nodes
					else{
						if ((temp = web.removeNode(web.getFirstNode())) == null)
							throw new Exception("Removed node should not be null!");
						if (web.getSize() != --old_size)
							throw new Exception("HyPeerWeb is not the correct size");
					}
					if (i % TEST_EVERY == 0){
						valid = (new Validator(web)).validate();
						assertTrue(valid);
					}
				}
				//After insertion graph
				System.out.println("DONE "+(t == 0 ? "ADDING" : "DELETING")+" NODES");
			}
			
			//Test send node
			Node f1, f2;
			for (int j=0; j<2000; j++){
				f1 = web.getRandomNode();
				do{
					f2 = web.getRandomNode();
				} while (f2 == f1);
				SendVisitor x = new SendVisitor(f1.getWebId());
				x.visit(f2);
				if (!x.wasFound()){
					System.out.println("sad face :(");
					System.out.println("From "+f2.getWebId()+" to "+f1.getWebId());
				}
			}
			
		} catch (Exception e){
			System.out.println("Fatal Error from HyPeerWeb:");
			System.out.println(e);
			System.out.println(e.getMessage());
			e.printStackTrace();
			fail();
		} finally{
			System.out.println("ADDED "+(t > 0 ? MAX_TESTS : i)+" NODES");
			System.out.println("DELETED "+(t == 1 ? i : t == 2 ? MAX_TESTS : 0)+" NODES");
		}
	}
}
