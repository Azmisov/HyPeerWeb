package hypeerweb.visitors;

import hypeerweb.Attributes;
import hypeerweb.Node;
import java.io.Serializable;

/**
 * Visitor Pattern for the HyPeerWeb
 */
public abstract class AbstractVisitor implements Serializable{
	protected Attributes data = new Attributes();
	
	/**
	 * Visit a particular node
	 * @param n the node to visit
	 */
	public abstract void visit(Node n);
}