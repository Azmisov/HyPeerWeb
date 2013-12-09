/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hypeerweb;

import communicator.Communicator;
import communicator.RemoteAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Gangsta
 * Every database segment has a database. It only saves to the database when the
 * InceptionWeb shuts down.
 */
public class SegmentDB {
	private HashSet<NodeProxy> proxies;
	private ArrayList<NodeCache> nodes;
	
	public SegmentDB(){
		proxies = new HashSet<>();
		nodes = new ArrayList<>();
	}
	
	/**
	 * Stores the InceptionWeb segment in a database.
	 * @param nodes The nodes in the segment to be saved 
	 */
	public void store(Set<Node> nodes){
		RemoteAddress here = Communicator.getAddress();
		for(Node n : nodes){
			NodeCache cache = new NodeCache(n, null);
			this.nodes.add(cache);
			for(Node link : n.L.getAllLinks()){
				if(!link.getAddress().onSameMachineAs(here)){
					proxies.add((NodeProxy)link);
				}
			}
		}
	}
	
	
}
