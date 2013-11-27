package hypeerweb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.TreeMap;
import hypeerweb.validator.HyPeerWebInterface;
import hypeerweb.validator.NodeInterface;

/**
 * Lightweight cache of a HyPeerWeb's nodes
 * Node objects may not reflect what the actual values are
 * @author inygaard
 */
public class NodeCache implements HyPeerWebInterface, Serializable{
	public enum SyncType {ADD, REMOVE, REPLACE}
	public TreeMap<Integer, Node> nodes = new TreeMap();
	public HashSet<Integer> segments = new HashSet();
	
	public void merge(NodeCache cache){
		nodes.putAll(cache.nodes);
	}
	
	public int[] addNode(hypeerweb.Node real,  boolean sync){
		Node n = new Node(real);
		return addNode(n, sync);
	}
	public int[] addNode(Node faux, boolean sync){
		int[] syncNodes = null;
		if (sync)
			 syncNodes = sync(faux, SyncType.ADD);
		nodes.put(faux.webID, faux);
		return syncNodes;
	}
	
	public int[] removeNode(hypeerweb.Node real, boolean sync){
		return removeNode(nodes.get(real.webID), sync);
	}
	public int[] removeNode(Node faux, boolean sync){
		int[] syncNodes = null;
		if (sync)
			syncNodes = sync(faux, SyncType.REMOVE);
		nodes.remove(faux.webID);
		return syncNodes;
	}
	
	private int[] sync(Node faux, SyncType type){
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
		
		//Don't fetch "faux.webID" or "-1"
		dirty.remove(faux.webID);
		dirty.remove(-1);
		
		//TODO: account for different SyncType's
		//i.e., replace sync type?
				//todo remove dirty nodes
		
		Integer[] obj = dirty.toArray(new Integer[dirty.size()]);
		int[] ret = new int[obj.length];
		for (int i=0; i<obj.length; i++)
			ret[i] = obj[i].intValue();
		
		return ret;
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
		@Override
		public Node getSurrogateFold(){
			return nodes.get(sf);
		}
		@Override
		public Node getInverseSurrogateFold(){
			return nodes.get(isf);
		}
		@Override
		public Node[] getNeighbors(){
			return mapToCached(n);
		}
		@Override
		public Node[] getSurrogateNeighbors(){
			return mapToCached(sn);
		}
		@Override
		public Node[] getInverseSurrogateNeighbors(){
			return mapToCached(isn);
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
