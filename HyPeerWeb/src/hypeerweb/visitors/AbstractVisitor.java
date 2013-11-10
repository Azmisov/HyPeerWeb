package hypeerweb.visitors;

import hypeerweb.Attributes;
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
	 * Visit a particular node
	 * @param n the node to visit
	 * @param a parameters to pass along with each visit
	 */
	public abstract void visit(Node n, Attributes a);
}