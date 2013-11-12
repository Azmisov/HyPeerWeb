package hypeerweb.visitors;

import hypeerweb.Attributes;
import hypeerweb.Node;

/**
 * Broadcast Visitor
 * @author Josh
 */
public class BroadcastVisitor extends AbstractVisitor{
	private static final String childOrigin = "BLACKLIST_NODE";
	
	/**
	 * Begin broadcasting from this node
	 * @param n a node to begin broadcasting from
	 */
	@Override
	public void visit(Node n){
		//Set the blacklist attribute to -1 to kick of the broadcast
		Attributes a = new Attributes();
		a.setAttribute(childOrigin, -1);
		visit(n, a);
	}
	
	/**
	 * Do not call this method! use visit(Node n) instead
	 * @param n a node to begin broadcasting from
	 * @param a data to pass along
	 */
	@Override
	public void visit(Node n, Attributes a){
		performOperation(n);
		//Broadcast to children
		Integer blacklist = (Integer) (a != null ? a.getAttribute(childOrigin) : null);
		for (Node child : n.getTreeChildren()){
			if (blacklist == null || child.getWebId() != blacklist)
				child.accept(this, null);
		}
		//Broadcast to parent, if necessary
		if (blacklist != null && a != null){
			Node parent = n.getTreeParent();
			if (parent != null){
				//Put child in blacklist, so we don't broadcast to it again
				a.setAttribute(childOrigin, n.getWebId());
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
