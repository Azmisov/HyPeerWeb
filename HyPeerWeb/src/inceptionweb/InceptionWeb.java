package inceptionweb;

import hypeerweb.HyPeerWeb;
import hypeerweb.Node;

/**
 * Handles communications between all the HyPeerWeb segments
 */
public class InceptionWeb extends Node{
	/**
	 * Each node in InceptionWebSegment has an attribute
	 * "IWSegment" that points to a corresponding InceptionWeb
	 */
	private HyPeerWeb InceptionWebSegment;
	private HyPeerWeb HyPeerWebSegment;
	
	private InceptionWeb() throws Exception{
		InceptionWebSegment = new HyPeerWeb(false, false, -1);
		HyPeerWebSegment = new HyPeerWeb(false, false, -1);
	}
	
	/**
	 * Called by GUI;
	 * @return 
	 */
	public Node addNode(){
		return null;
	}
	
	public deleteNode(Node n){
		
	}
}
