package hypeerweb;

import communicator.Communicator;
import communicator.RemoteAddress;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;

/**
 *
 * @author Gangsta
 * Every database segment has a database. It only saves to the database when the
 * InceptionWeb shuts down.
 */
public class SegmentDB implements Serializable {
	private HashSet<Node> proxies;
	private SegmentCache cache;
	private final RemoteAddress oldAddress;
	
	public SegmentDB(Segment segment){
		oldAddress = Communicator.getAddress();
		proxies = new HashSet();
		cache = segment.getCache();
		//Get a list of proxies that are missing from the cache
		//I don't really know a better way to do this
		for (Object n : segment.nodes.values())
			proxies.addAll(((Node) n).L.getProxies());
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
	
	public static void save(Segment segment){
		try{
			ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(segment.dbname));
			segment.setWriteRealNode(true);
			stream.writeObject(segment);
			stream.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	public static Segment load(String file) throws IOException, ClassNotFoundException{
		ObjectInputStream stream = new ObjectInputStream(new FileInputStream(new File(file)));
		Segment segment = (Segment) stream.readObject();
		Segment.segmentList.add(segment);
		stream.close();
		return segment;
	}
}
