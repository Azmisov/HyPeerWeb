package hypeerweb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.TreeMap;
import hypeerweb.validator.HyPeerWebInterface;
import hypeerweb.validator.NodeInterface;
import java.util.HashMap;

/**
 * Lightweight cache of a HyPeerWeb's nodes
 * Node objects may not reflect what the actual values are
 * @author isaac
 */
public class HyPeerWebCache implements HyPeerWebInterface, Serializable{
	public static final String className = HyPeerWebCache.class.getName();
	public static final String nodeClassNameArr = Node[].class.getName();
	public enum SyncType{ADD, REMOVE, REPLACE}
	public final TreeMap<Integer, Node> nodes = new TreeMap();
	public final HashMap<Integer, HashSet<Node>> segments = new HashMap();
	
	/**
	 * Merge a cache with this cache; the merging cache will
	 * overwrite any data of the same networkID
	 * @param cache the cache to merge with
	 */
	public void merge(HyPeerWebCache cache){
		//Overwrite data with same network ID
		for (Integer netID: cache.segments.keySet()){
			HashSet<Node> refs = segments.get(netID);
			if (refs != null){
				for (Node n: refs)
					nodes.remove(n.webID);
			}
		}
		nodes.putAll(cache.nodes);
		segments.putAll(cache.segments);
	}
	
	/**
	 * Adds a node to the cache; this should not be a proxy node!!! you
	 * should convert the proxy to a cached node first
	 * @param real a node (should not be a proxy)
	 * @param sync whether to gather a list of modified nodes,
	 * based on the new links shown of the added node
	 * @return a list of nodes that need to be synced, if "sync" was enabled
	 */
	public HashSet<Integer> addNode(hypeerweb.Node real, boolean sync){
		return addNode(new Node(real), sync);
	}
	/**
	 * Adds a cached node to the cache
	 * @param faux a HyPeerWebCache.Node
	 * @param sync whether to gather a list of modified nodes,
	 * based on the new links shown of the added node
	 * @return a list of nodes that need to be synced, if "sync" was enabled
	 */
	public HashSet<Integer> addNode(Node faux, boolean sync){
		HashSet<Integer> syncNodes = sync ? sync(faux) : null;
		nodes.put(faux.webID, faux);
		//Add to segments list
		HashSet<Node> set = segments.get(faux.networkID);
		boolean create = set == null;
		if (create)
			set = new HashSet();
		set.add(faux);
		if (create)
			segments.put(faux.networkID, set);
		//Return list of dirty nodes
		return syncNodes;
	}
	
	/**
	 * Removes a node with the specified webID
	 * @param webID the webID of the node to remove
	 * @param sync whether to gather a list of modified nodes,
	 * based on the links of the removed node
	 * @return a list of nodes that need to be synced, if "sync" was enabled
	 */
	public HashSet<Integer> removeNode(int webID, boolean sync){
		return removeNode(nodes.get(webID), sync);
	}
	/**
	 * Removes a cached node from the cache
	 * @param faux a HyPeerWebCache.Node
	 * @param sync whether to gather a list of modified nodes,
	 * based on the links of the removed node
	 * @return a list of nodes that need to be synced, if "sync" was enabled
	 */
	public HashSet<Integer> removeNode(Node faux, boolean sync){
		HashSet<Integer> syncNodes = sync ? sync(faux) : null;
		nodes.remove(faux.webID);
		//Remove from segments list
		HashSet<Node> set = segments.get(faux.networkID);
		set.remove(faux);
		if (set.isEmpty())
			segments.remove(faux.networkID);
		//Return a list of dirty nodes
		return syncNodes;
	}
	
	/**
	 * Replaces the node of "webID" with the faux node
	 * @param webID the webID of the node to replace
	 * @param faux the replacement node
	 * @param sync whether to gather a list of modified nodes,
	 * based on the links of the replaced node
	 * @return a list of nodes that need to be synced, if "sync" was enabled
	 */
	public HashSet<Integer> replaceNode(int webID, Node faux, boolean sync){
		return replaceNode(nodes.get(webID), faux, sync);
	}
	/**
	 * Replaces a cached node with another
	 * @param old the the cached node to replace
	 * @param faux the replacement node
	 * @param sync whether to gather a list of modified nodes,
	 * based on the links of the replaced node
	 * @return a list of nodes that need to be synced, if "sync" was enabled
	 */
	public HashSet<Integer> replaceNode(Node old, Node faux, boolean sync){		
		//Replace node is special in that the webID has changed
		//So, instead of using sync() to get the symmetric difference,
		//all of the old node's links are dirty
		HashSet<Integer> dirty = null;
		if (sync){
			dirty = new HashSet();
			dirty.add(old.f);
			dirty.add(old.sf);
			dirty.add(old.isf);
			for (int x: old.n) dirty.add(x);
			for (int x: old.sn) dirty.add(x);
			for (int x: old.isn) dirty.add(x);
			dirty.remove(-1);
		}
		
		//Replace the node
		removeNode(old, false);
		addNode(faux, false);
		
		return dirty;
	}
	
	//Gather a list of nodes that need to be updated
	private HashSet<Integer> sync(Node faux){
		Node cache = nodes.get(faux.webID);
		//Compare the cached version and the faux/proxy/real version
		HashSet<Integer> dirty = new HashSet();
		//Compare folds
		int[] cacheF = new int[]{cache.f, cache.sf, cache.isf};
		int[] fauxF = new int[]{faux.f, faux.sf, faux.isf};
		for (int i=0; i<3; i++){
			if (cacheF[i] != fauxF[i]){
				dirty.add(cacheF[i]);
				dirty.add(fauxF[i]);
			}
		}
		//Compare neighbors
		dirty.addAll(syncNeighbors(cache.n, faux.n));
		dirty.addAll(syncNeighbors(cache.sn, faux.sn));
		dirty.addAll(syncNeighbors(cache.isn, faux.isn));
		
		//Don't fetch "faux.webID" since we already have it; -1 is a placeholder
		dirty.remove(faux.webID);
		dirty.remove(-1);
		return dirty;
	}
	private ArrayList<Integer> syncNeighbors(int[] cacheN, int[] fauxN){
		//Assuming the two arrays are sorted,
		//get the symmetric difference of the two
		ArrayList<Integer> dirty = new ArrayList();
		int ci = 0, fi = 0;
		while (ci < cacheN.length || fi < fauxN.length){
			//We've come to the end of an array; add the rest of the elements
			if (ci == cacheN.length)
				dirty.add(fauxN[fi++]);
			else if (fi == fauxN.length)
				dirty.add(cacheN[ci++]);
			else{
				//Otherwise, compare the id's at our pointer locations
				if (cacheN[ci] == fauxN[fi]){
					ci++;
					fi++;
				}
				else if (cacheN[ci] < fauxN[fi])
					dirty.add(cacheN[ci++]);
				else dirty.add(fauxN[fi++]);
			}
		}
		return dirty;
	}
	
	/**
	 * Update the cache to reflect a new network ID; if the new ID already
	 * exists in the cache, the entries in the segment map will be merged
	 * @param oldID the old id
	 * @param newID the new id
	 */
	public void changeNetworkID(int oldID, int newID){
		//Retrieve the old and new segment entries
		if (oldID == newID) return;
		HashSet<Node> oldSeg = segments.get(oldID), newSeg;
		if (oldSeg == null) return;
		newSeg = segments.get(newID);
		//Create a new hashset object, if newSeg doesn't exist
		boolean create = newSeg == null;
		if (create)
			newSeg = new HashSet();
		//Change the network ID
		for (Node n: oldSeg){
			n.networkID = newID;
			newSeg.add(n);
		}
		//Add to segment list, if it's a new entry
		if (create)
			segments.put(newID, newSeg);
	}
	
	public class Node implements NodeInterface, Serializable{
		//Network id
		private int networkID;
		//Node attributes
		private int webID, height;
		//Node links
		private int f=-1, sf=-1, isf=-1;
		private int[] n, sn, isn;

		public Node(hypeerweb.Node real){
			webID = real.getWebId();
			height = real.getHeight();
			Segment host = real.getHostSegment();
			networkID = host != null ? host.getWebId() : -1;
			//Folds
			hypeerweb.Node temp;
			if ((temp = real.L.getFold()) != null)
				f = temp.getWebId();
			if ((temp = real.L.getSurrogateFold()) != null)
				sf = temp.getWebId();
			if ((temp = real.L.getInverseSurrogateFold()) != null)
				isf = temp.getWebId();
			//Neighbors
			n = convertToCached(real.L.getNeighbors());
			sn = convertToCached(real.L.getSurrogateNeighbors());
			isn = convertToCached(real.L.getInverseSurrogateNeighbors());
		}

		//BASIC GETTERS
		public int getNetworkId(){
			return networkID;
		}
		@Override
		public int getWebId(){
			return webID;
		}
		@Override
		public int getHeight(){
			return height;
		}
		@Override
		public Node getFold(){
			return nodes.get(f);
		}
		public int getRawFold(){
			return f;
		}
		@Override
		public Node getSurrogateFold(){
			return nodes.get(sf);
		}
		public int getRawSurrogateFold(){
			return sf;
		}
		@Override
		public Node getInverseSurrogateFold(){
			return nodes.get(isf);
		}
		public int getRawInverseSurrogateFold(){
			return isf;
		}
		@Override
		public Node[] getNeighbors(){
			return mapToCached(n);
		}
		public int[] getRawNeighbors(){
			return n;
		}
		@Override
		public Node[] getSurrogateNeighbors(){
			return mapToCached(sn);
		}
		public int[] getRawSurrogateNeighbors(){
			return sn;
		}
		@Override
		public Node[] getInverseSurrogateNeighbors(){
			return mapToCached(isn);
		}
		public int[] getRawInverseSurrogateNeighbors(){
			return isn;
		}
		
		//SPECIALIZED GETTERS
		@Override
		public Node getParent(){
			//See comments in hypeerweb.Node class for documentation
			if (webID == 0) return null;
			int parID = webID & ~Integer.highestOneBit(webID);
			return Arrays.binarySearch(n, parID) < 0 ? null : nodes.get(parID);
		}
		public Node[] getTreeChildren(){
			//See comments in hypeerweb.Node class for documentation
			HashSet<Integer>
					generatedNeighbors = new HashSet(),
					generatedInverseSurrogates = new HashSet();
			ArrayList<Node> found = new ArrayList();
			int id_surr = webID | ((1 << (height - 1)) << 1),
				trailingZeros = Integer.numberOfTrailingZeros(webID);
			int bitShifter = 1;
			for(int i = 0; i < trailingZeros; i++){
				generatedNeighbors.add(webID | bitShifter);
				generatedInverseSurrogates.add(id_surr | bitShifter);
				bitShifter <<= 1;
			}
			for (int id: n){
				if (generatedNeighbors.contains(id))
					found.add(nodes.get(id));
			}
			for (int id: isn){
				if (generatedInverseSurrogates.contains(id))
					found.add(nodes.get(id));
			}
			return found.toArray(new Node[found.size()]);
		}
		public Node getTreeParent(){
			//See comments in hypeerweb.Node class for documentation
			if (webID == 0) return null;			
			int neighborID = webID & ~Integer.lowestOneBit(webID);
			int idx = Arrays.binarySearch(n, neighborID);			
			if (idx < 0){
				Node temp;
				for (int snID: sn){
					temp = nodes.get(snID);
					if (temp != null && temp.webID == (neighborID & ~((1 << (temp.height - 1)) << 1)))
						return temp;
				}
			}
			else return nodes.get(n[idx]);
			return null;
		}
		
		private Node[] mapToCached(int[] idList){
			Node[] cached = new Node[idList.length];
			for (int i=0; i<idList.length; i++)
				cached[i] = nodes.get(idList[i]);
			return cached;
		}
		private int[] convertToCached(hypeerweb.Node[] realList){
			int[] temp = new int[realList.length];
			for (int i=0; i<realList.length; i++)
				temp[i] = realList[i].getWebId();
			Arrays.sort(temp);
			return temp;
		}

		@Override
		public int compareTo(NodeInterface node) {
			int id = node.getWebId();
			if (webID == id)
				return 0;
			int nh = node.getHeight();
			return (height == nh ? webID < id : height < nh) ? -1 : 1;
		}
		@Override
		public boolean equals(Object obj){
			if (obj == null || !(obj instanceof Node))
				return false;
			return this.webID == ((Node) obj).webID;
		}
		@Override
		public int hashCode(){
			return new Integer(this.webID).hashCode();
		}
		@Override
		public String toString(){
			StringBuilder builder = new StringBuilder();
			builder.append("\nNode: ").append(webID).append("(").append(height).append(")");
			//Folds
			if (f != -1)
				builder.append("\n\tFold: ").append(f).
						append("(").append(getNode(f).getHeight()).append(")");
			if (sf != -1)
				builder.append("\n\tSFold: ").append(sf).
						append("(").append(getNode(sf).getHeight()).append(")");
			if (isf != -1)
				builder.append("\n\tISFold: ").append(isf).
						append("(").append(getNode(isf).getHeight()).append(")");
			//Neighbors
			if (n.length > 0){
				builder.append("\n\tNeighbors: ");
				for (int a_n : n)
					builder.append(a_n).append("(").append(getNode(a_n).getHeight()).append("), ");
			}
			if (sn.length > 0){
				builder.append("\n\tSNeighbors: ");
				for (int a_sn : sn)
					builder.append(a_sn).append("(").append(getNode(a_sn).getHeight()).append("), ");
			}
			if (isn.length > 0){
				builder.append("\n\tISNeighbors: ");
				for (int a_isn : isn)
					builder.append(a_isn).append("(").append(getNode(a_isn).getHeight()).append("), ");
			}
			return builder.toString();
		}
	}
	//Helper method for creating a cached node; should not be run with proxy nodes
	protected Node createCachedNode(hypeerweb.Node real){
		return new Node(real);
	}
	
	//VALIDATOR
	@Override
	public NodeInterface[] getOrderedListOfNodes() {
		return nodes.values().toArray(new Node[nodes.size()]);
	}
	@Override
	public NodeInterface getNode(int webId) {
		return nodes.get(webId);
	}
	@Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Node n : nodes.values())
            builder.append(n);
        return builder.toString();
    }
}
