package hypeerweb.visitors;

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
	public final void begin(Node n){
		//Set the blacklist attribute to -1 to kick of the broadcast
		data.setAttribute(childOrigin, -1);
		visit(n);
	}
	
	/**
	 * Do not call this method! use visit(Node n) instead
	 * @param n a node to begin broadcasting from
	 */
	@Override
	public final void visit(Node n){
		performOperation(n);
		//Broadcast to children
		Integer blacklist = (Integer) data.getAttribute(childOrigin);
		for (Node child : n.getTreeChildren()){
			if (blacklist == null || child.getWebId() != blacklist)
				child.accept(this);
		}
		//Broadcast to parent, if necessary
		if (blacklist != null){
			Node parent = n.getTreeParent();
			if (parent != null){
				//Put child in blacklist, so we don't broadcast to it again
				data.setAttribute(childOrigin, n.getWebId());
				parent.accept(this);
			}
		}
	}
	
	/**
	 * Perform a broadcast operation
	 * @param n the node that has been broadcasted to
	 */
	public void performOperation(Node n){}
}
