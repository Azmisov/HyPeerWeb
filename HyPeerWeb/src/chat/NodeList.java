package chat;

import hypeerweb.Node;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Keep a local copy of all the nodes in the HyPeerWeb
 * @author isaac
 */
public class NodeList {
	public TreeMap<Integer, Node> list;
	
	public NodeList(TreeMap<Integer, Node> initialList){
		if (initialList == null)
			list = new TreeMap();
		else list = initialList;
	}
	
	/**
	 * Syncs the list of nodes with the added node
	 * @param n the new node
	 * @return a list of affected nodes
	 */
	public ArrayList<Node> addNode(Node n){
		//TODO, don't update nodes that aren't proxies
		list.put(n.getWebId(), n);
		return null;
	}
	/**
	 * Syncs the list of nodes with the removed node
	 * @param n the new node
	 * @return a list of affected nodes
	 */
	public ArrayList<Node> removeNode(Node n){
		//TODO, don't update nodes that aren't proxies
		list.remove(n.getWebId());
		return null;
	}
}
