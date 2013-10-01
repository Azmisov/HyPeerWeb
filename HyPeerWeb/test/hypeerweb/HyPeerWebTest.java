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
	private int MAX_TESTS = 50;
	private Validator v;

	/**
	 * Test of addNode method, of class HyPeerWeb.
	 */
	@Test
	public void testAddNode() throws Exception {
		System.out.println("addNode");
		HyPeerWeb web = HyPeerWeb.getInstance();
		web.deleteAllNodes();
		
		//Add a bunch of nodes; if it validates afterwards, addNode should be working
		//We cannot do simulated tests, since addNode inserts at arbitrary places
		for (int i=0; i<MAX_TESTS; i++){
			web.addNode();
			System.out.println("Adding node #"+i);
			assertTrue((new Validator(web)).validate());
		}
		
	}
	
}
