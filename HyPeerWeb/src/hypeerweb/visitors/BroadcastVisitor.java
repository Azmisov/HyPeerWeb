package hypeerweb.visitors;

import hypeerweb.Node;

/**
 * Broadcast Visitor; listener callback is guaranteed
 * to run on the same machine the node is on
 */
public class BroadcastVisitor extends AbstractVisitor{
	private static final String
		childOrigin = "BLACKLIST_NODE",
		listen = "LISTEN";
	private transient boolean hasListener = false;
	
	public BroadcastVisitor(Node.Listener command){
		data.setAttribute(listen, command);
		hasListener = command == null;
	}
	
	/**
	 * Begin broadcasting from this node
	 * @param n a node to begin broadcasting from
	 */
	public final void begin(Node n){
		if (!hasListener) return;
		//Set the blacklist attribute to -1 to kick of the broadcast
		data.setAttribute(childOrigin, -1);
		visit(n);
	}
	
	/**
	 * Do not call this method! use begin(Node n) instead
	 * @param n a node to begin broadcasting from
	 */
	@Override
	public final void visit(Node n){
		((Node.Listener) data.getAttribute(listen)).callback(n);
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
}
