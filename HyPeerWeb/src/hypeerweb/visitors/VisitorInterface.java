package hypeerweb.visitors;

import hypeerweb.Node;

/**
 * Visitor Pattern for the HyPeerWeb
 */
public interface VisitorInterface{
	/**
	 * Visit a particular node
	 * @param n the node to visit
	 */
	public void visit(Node n);
}