package hypeerweb.visitors;

import hypeerweb.Node;

/**
 * Visitor Pattern for the HyPeerWeb
 */
public abstract class AbstractVisitor{
	/**
	 * Visit a particular node
	 * @param n the node to visit
	 */
	public void visit(Node n){
		visit(n, null);
	}
	/**
	 * Visit a particular node; use of this method is implementation
	 * dependent; you may need to use the default constructor
	 * @param n the node to visit
	 * @param a parameters to pass along with each visit
	 */
	public abstract void visit(Node n, Object a);
}