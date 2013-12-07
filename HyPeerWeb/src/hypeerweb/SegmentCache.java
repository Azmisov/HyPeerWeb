package hypeerweb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import hypeerweb.validator.HyPeerWebInterface;
import hypeerweb.validator.NodeInterface;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Lightweight cache of a HyPeerWeb's nodes
 * Node objects may not reflect what the actual values are
 * @author isaac
 */
public class SegmentCache implements HyPeerWebInterface, Serializable{
	public static final String className = SegmentCache.class.getName();
	public static final String nodeClassNameArr = NodeCache[].class.getName();
	public enum SyncType{ADD, REMOVE, REPLACE}
	public final HashMap<Integer, NodeCache> nodes = new HashMap();
	public final HashMap<Integer, HashSet<NodeCache>> segments = new HashMap();
	
	/**
	 * Merge a cache with this cache; the merging cache will
	 * overwrite any data of the same networkID
	 * @param cache the cache to merge with
	 */
	public void merge(SegmentCache cache){
		//Overwrite data with same network ID
		for (Integer netID: cache.segments.keySet()){
			HashSet<NodeCache> refs = segments.get(netID);
			if (refs != null){
				for (NodeCache n: refs)
					nodes.remove(n.webID);
			}
		}
		for (Entry<Integer, NodeCache> entry: cache.nodes.entrySet()){
			NodeCache n = entry.getValue();
			n.parent = this;
			nodes.put(entry.getKey(), n);
		}
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
		return addNode(new NodeCache(real, this), sync);
	}
	/**
	 * Adds a cached node to the cache
	 * @param faux a SegmentCache.Node
	 * @param sync whether to gather a list of modified nodes,
	 * based on the new links shown of the added node
	 * @return a list of nodes that need to be synced, if "sync" was enabled
	 */
	public HashSet<Integer> addNode(NodeCache faux, boolean sync){
		faux.parent = this;
		HashSet<Integer> syncNodes = sync ? sync(faux) : null;
		nodes.put(faux.webID, faux);
		//Add to segments list
		HashSet<NodeCache> set = segments.get(faux.networkID);
		if (set == null){
			set = new HashSet();
			segments.put(faux.networkID, set);
		}
		set.add(faux);	
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
	 * @param faux a SegmentCache.Node
	 * @param sync whether to gather a list of modified nodes,
	 * based on the links of the removed node
	 * @return a list of nodes that need to be synced, if "sync" was enabled
	 */
	public HashSet<Integer> removeNode(NodeCache faux, boolean sync){
		HashSet<Integer> syncNodes = sync ? sync(faux) : null;
		nodes.remove(faux.webID);
		//Remove from segments list
		HashSet<NodeCache> set = segments.get(faux.networkID);
		set.remove(faux);
		if (set.isEmpty())
			segments.remove(faux.networkID);
		//Return a list of dirty nodes
		return syncNodes;
	}
	public void removeAllNodes(){
		nodes.clear();
	}
	/**
	 * Replaces the node of "webID" with the faux node
	 * @param webID the webID of the node to replace
	 * @param faux the replacement node
	 * @param sync whether to gather a list of modified nodes,
	 * based on the links of the replaced node
	 * @return a list of nodes that need to be synced, if "sync" was enabled
	 */
	public HashSet<Integer> replaceNode(int webID, NodeCache faux, boolean sync){
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
	public HashSet<Integer> replaceNode(NodeCache old, NodeCache faux, boolean sync){		
		//Replace node is special in that the webID has changed
		//So, instead of using sync() to get the symmetric difference,
		//all of the old node's links are dirty
		HashSet<Integer> dirty = sync ? syncAll(old) : null;
		
		//Replace the node
		removeNode(old, false);
		addNode(faux, false);
		
		return dirty;
	}
	
	//Gather a list of nodes that need to be updated
	private HashSet<Integer> sync(NodeCache faux){
		NodeCache cache = nodes.get(faux.webID);
		//If this is a new node, sync all links
		if (cache == null)
			return syncAll(faux);
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
	private HashSet<Integer> syncAll(NodeCache faux){
		//Sync all connections
		HashSet<Integer> dirty = new HashSet();
		dirty.add(faux.f);
		dirty.add(faux.sf);
		dirty.add(faux.isf);
		for (int x: faux.n) dirty.add(x);
		for (int x: faux.sn) dirty.add(x);
		for (int x: faux.isn) dirty.add(x);
		dirty.remove(-1);
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
		HashSet<NodeCache> oldSeg = segments.get(oldID), newSeg;
		if (oldSeg == null) return;
		newSeg = segments.get(newID);
		//Create a new hashset object, if newSeg doesn't exist
		if (newSeg == null){
			newSeg = new HashSet();
			segments.put(newID, newSeg);
		}
		//Change the network ID
		for (NodeCache n: oldSeg){
			n.networkID = newID;
			newSeg.add(n);
		}			
	}
	//Helper method for creating a cached node; should not be run with proxy nodes
	protected NodeCache createCachedNode(hypeerweb.Node real){
		return new NodeCache(real, this);
	}
	
	//VALIDATOR
	@Override
	public NodeInterface[] getOrderedListOfNodes() {
		NodeCache[] unsorted = nodes.values().toArray(new NodeCache[nodes.size()]);
		Arrays.sort(unsorted);
		return unsorted;
	}
	@Override
	public NodeInterface getNode(int webId) {
		return nodes.get(webId);
	}
	@Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (NodeCache n : nodes.values())
            builder.append(n);
        return builder.toString();
    }
}
