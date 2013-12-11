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
	private final ArrayList<NodeImmutable> nodes;
	private final RemoteAddress oldAddress;
	
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
	/**
	 * Transfers the database to another segment.
	 * @param segment The segment to transfer the database to
	 */
	public void transferTo(Segment<Node> segment){
		for (NodeImmutable n : nodes){
			Node node = new Node(n);
			segment.nodes.put(node.getWebId(), node);
			segment.nodesByUID.put(node.UID, node);
			node.L.broadcastReplacement(new NodeProxy(n, oldAddress), node);
		}
	}
}
