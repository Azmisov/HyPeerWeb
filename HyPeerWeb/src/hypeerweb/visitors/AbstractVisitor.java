package hypeerweb.visitors;

import communicator.Command;
import hypeerweb.Node;
import java.io.Serializable;

/**
 * Visitor Pattern for the HyPeerWeb
 * Takes a static method as a callback
 */
public abstract class AbstractVisitor implements Serializable{
	public static final String nodeClass = Node.class.getCanonicalName();
	protected final Command callback;
	
	public AbstractVisitor(Command listener){
		callback = listener;
		callback.addParameter(nodeClass);
	}
	
	/**
	 * Visit a particular node
	 * @param n the node to visit
	 */
	public abstract void visit(Node n);
	/**
	 * Runs the callback on this node
	 * @param n the node to callback on
	 */
	protected void callback(Node n){
		callback.setParameter(0, n);
		callback.execute();
		callback.setParameter(0, null);
	}
}