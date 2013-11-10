package hypeerweb.visitors;

import hypeerweb.Attributes;
import hypeerweb.Node;
import java.util.List;

/**
 * Broadcast Visitor
 * @author Josh
 */
public class BroadcastVisitor extends AbstractVisitor{
	private static final String childOrigin = "BLACKLIST_NODE";
	/**
	 * Begin broadcasting from this node; it will first run a
	 * SendVisitor operation to node with webID = 0; from there, it
	 * will begin the broadcast
	 * @param n a node to begin broadcasting from
	 * @param a data to pass along
	 */
	@Override
	public void visit(Node n, Attributes a){
		performOperation(n);
		//Broadcast to children
		Node blacklist = (Node) (a != null ? a.getAttribute(childOrigin) : null);
		List<Node> broadcastNeighbors = n.getTreeChildren();
		for (Node neighbor : broadcastNeighbors){
			if (!neighbor.equals(blacklist))
				neighbor.accept(this, null);
		}
		//Broadcast to parent, if necessary
		if (blacklist != null && a != null){
			Node parent = n.getTreeParent();
			if (parent != null){
				a.setAttribute(childOrigin, n);
				parent.accept(this, a);
			}
		}
	}	
	/**
	 * Perform a broadcast operation
	 * @param n the node that has been broadcasted to
	 */
	public void performOperation(Node n){}
}
