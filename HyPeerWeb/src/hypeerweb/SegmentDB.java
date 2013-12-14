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
import java.util.HashMap;
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
	
	public SegmentDB(Segment<Node> segment){
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
		HashMap<Integer, Node> node_map = new HashMap();
		//Add all proxies to node map
		for (Node n: proxies)
			node_map.put(n.getWebId(), n);
		//Rebuilt nodes
		for (NodeCache n: cache.nodes.values()){
			Node real = new Node(n.webID, n.height);
			node_map.put(n.webID, real);
		}
		//Now rebuild the links
		for (NodeCache n: cache.nodes.values()){
			Node real = node_map.get(n.webID);
			if (n.f != -1)
				real.L.setFold(node_map.get(n.f));
			if (n.sf != -1)
				real.L.setSurrogateFold(node_map.get(n.sf));
			if (n.isf != -1){
				real.foldState = Node.FoldState.UNSTABLE;
				real.L.setInverseSurrogateFold(node_map.get(n.isf));
			}
			for (int friend: n.n)
				real.L.addNeighbor(node_map.get(friend));
			for (int friend: n.sn)
				real.L.addSurrogateNeighbor(node_map.get(friend));
			for (int friend: n.isn)
				real.L.addInverseSurrogateNeighbor(node_map.get(friend));
		}
		//Transfer map to segment
		for (NodeCache n: cache.nodes.values()){
			Node real = node_map.get(n.webID);
			segment.nodes.put(n.webID, real);
			segment.nodesByUID.put(real.UID, real);
			RemoteAddress addr = new RemoteAddress(oldAddress, n.UID);
			real.L.broadcastReplacement(new NodeProxy(n, addr), real);
		}
	}
	
	public static void save(Segment segment){
		try{
			ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(segment.dbname));
			segment.setWriteRealSegment(true);
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
