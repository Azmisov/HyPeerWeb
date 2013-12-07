package hypeerweb.visitors;

import communicator.NodeListener;
import hypeerweb.Node;

/**
 * Broadcast Visitor; listener callback is guaranteed
 * to run on the same machine the node is on
 */
public class BroadcastVisitor extends AbstractVisitor{
	private int blacklist = -2;

	/**
	 * Create a new broadcast visitor
	 * @param listener the visitor callback
	 */
	public BroadcastVisitor(NodeListener listener){
		super(listener);
	}

	/**
	 * Visit a node
	 * @param n a node to begin broadcasting from
	 */
	@Override
	public final void visit(Node n){
		//Run callback on this node
		callback.callback(n);
		
		//Reset blacklist flag
		int cur_blacklist = blacklist;
		blacklist = -1;
		
		//Broadcast to children
		for (Node child : n.getTreeChildren()){
			if (child.getWebId() != cur_blacklist)
				child.accept(this);
		}
		//Broadcast to parent, if necessary
		if (cur_blacklist != -1){
			Node parent = n.getTreeParent();
			if (parent != null){
				//Put child in blacklist, so we don't broadcast to it again
				blacklist = n.getWebId();
				parent.accept(this);
			}
		}
	}
}
