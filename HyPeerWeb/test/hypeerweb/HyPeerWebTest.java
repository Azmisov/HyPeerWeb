/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hypeerweb;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import validator.Validator;

/**
 * HyPeerWeb testing
 */
public class HyPeerWebTest {
	//Validation variables
	private int MAX_TESTS = 10;
	private int TEST_EVERY = 5;
	private HyPeerWeb web;
	
	public HyPeerWebTest() throws Exception{
		web = HyPeerWeb.getInstance();
	}
	
	/*
	@Test
	public void testHyPeerWeb(){
		//Validate the web prior to adding to make sure
		//it is extracting from the Database correctly
		System.out.println("Testing restore");
		assertTrue((new Validator(web)).validate());
		System.out.println("Done testing restore");
	}
	*/
	
	/**
	 * Test of addNode method, of class HyPeerWeb.
	 */
	@Test
	public void testAddNode() throws Exception {
		HyPeerWeb web = HyPeerWeb.getInstance();
		Node n;
		//I put the testHyPeerWeb code here because it was always running after testAddNode and so wasn't testing anything.
		System.out.println("Testing restore");
		assertTrue((new Validator(web)).validate());//comment out this line to get new HyPeerWeb
		System.out.println("Done testing restore");
		
		//Add a bunch of nodes; if it validates afterwards, addNode should be working
		//We cannot do simulated tests, since addNode inserts at arbitrary places
		web.deleteAllNodes();
		boolean valid;
		for (int i=1; i<=MAX_TESTS; i++){
			n = web.addNode();
			System.out.println("Added node #" + n.getWebId());
			if (i % TEST_EVERY == 0){
				valid = (new Validator(web)).validate();
				assertTrue(valid);
			}
			
		}
		
	}
	
}
