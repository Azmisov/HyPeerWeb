package hypeerweb.visitors;

import communicator.NodeListener;
import hypeerweb.Node;
import java.io.Serializable;

/**
 * Visitor Pattern for the HyPeerWeb
 * Takes a static method as a callback
 */
public abstract class AbstractVisitor implements Serializable{
	protected final NodeListener callback;
	
	public AbstractVisitor(NodeListener listener){
		callback = listener;
	}
	
	/**
	 * Visit a particular node
	 * @param n the node to visit
	 */
	public abstract void visit(Node n);
}