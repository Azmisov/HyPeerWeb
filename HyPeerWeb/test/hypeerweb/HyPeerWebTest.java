/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hypeerweb;

import org.junit.Test;
import static org.junit.Assert.*;
import validator.Validator;

/**
 * HyPeerWeb testing
 */
public class HyPeerWebTest {
	//Validation variables
	private final int MAX_TESTS = 1000;
	private final int TEST_EVERY = 1;
	private final boolean TEST_DATABASE = false;
	private HyPeerWeb web;
	
	public HyPeerWebTest() throws Exception{
		web = HyPeerWeb.getInstance();
		if (!TEST_DATABASE)
			web.disableDatabase();
	}
	
	/**
	 * Test of addNode method, of class HyPeerWeb.
	 */
	@Test
	public void testAddNode() throws Exception {
		try{
			if (TEST_DATABASE){
				//I put the testHyPeerWeb code here because it was always running after testAddNode and so wasn't testing anything.
				System.out.println("Testing restore");
				assertTrue((new Validator(web)).validate());//comment out this line to get new HyPeerWeb
				System.out.println("Done testing restore");
			}

			//Add a bunch of nodes; if it validates afterwards, addNode should be working
			//We cannot do simulated tests, since addNode inserts at arbitrary places
			web.deleteAllNodes();
			boolean valid;
			for (int i=1; i<=MAX_TESTS; i++){
				web.addNode();
				//System.out.println("added node "+i);
				if (i % TEST_EVERY == 0){
					valid = (new Validator(web)).validate();
					assertTrue(valid);
				}
			}
		} catch (Exception e){
			System.out.println("Fatal Error from HyPeerWeb:");
			System.out.println(e);
			System.out.println(e.getMessage());
			fail();
		}
	}
	
}
