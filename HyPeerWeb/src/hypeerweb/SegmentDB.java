/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hypeerweb;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import communicator.Communicator;
import communicator.RemoteAddress;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

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
	
	public void save(Segment segment){
		try{
			ObjectOutputStream bstream = new ObjectOutputStream(new FileOutputStream(segment.dbname));
			bstream.write("Chibbi".getBytes());
			bstream.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public Segment load(File file) throws IOException, ClassNotFoundException{
		return (Segment) new ObjectInputStream(new FileInputStream(file)).readObject();
	}
}
