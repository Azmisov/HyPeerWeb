/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hypeerweb;

import communicator.Communicator;
import communicator.RemoteAddress;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 *
 * @author Gangsta
 * Every database segment has a database. It only saves to the database when the
 * InceptionWeb shuts down.
 */
public class SegmentDB implements Serializable {
	private ArrayList<NodeImmutable> nodes;
	private RemoteAddress oldAddress;
	
	public SegmentDB(){
		nodes = new ArrayList<>();
		oldAddress = Communicator.getAddress();
	}
	
	/**
	 * Stores the InceptionWeb segment in a database.
	 * @param nodes The nodes in the segment to be saved 
	 */
	public void store(Collection<Node> nodes){
		for(Node n : nodes)
			this.nodes.add(new NodeImmutable(n));
	}
	
	public void restore(Segment<Node> segment){
		for (NodeImmutable n : nodes){
			n.UID = Communicator.assignId();
			n.L.broadcastReplacement(n, );
		}
	}
}
