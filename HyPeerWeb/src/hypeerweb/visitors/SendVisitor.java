package hypeerweb.visitors;

import hypeerweb.Attributes;
import hypeerweb.Node;

/**
 * Navigates from one node to another
 */
public class SendVisitor extends AbstractVisitor{
	/**
	 * The webID that we are searching for
	 */
	protected int targetWebId;
	/**
	 * Should we find an exact match for the targetWebID?
	 */
	protected boolean approximateMatch;
	private Node finalNode;
	
	/**
	 * Navigate to the node with webID = 0, with
	 * approximateMatch disabled
	 */
	public SendVisitor(){
		this(0, false);
	}
	/**
	 * Navigate to the specified node, with approximateMatch disabled
	 * @param targetWebId the WebID of the node to navigate to
	 */
	public SendVisitor(int targetWebId){
		this(targetWebId, false);
	}
	/**
	 * Creates a new Send operation visitor
	 * @param targetWebId the Node's WebID that we are searching for
	 * @param approximateMatch if false, it will try to find an exact match to targetWebId;
	 * if true, it will try to find a node that is close to targetWebID and may not
	 * get all the way to the node (e.g. use this to get random nodes)
	 */
	public SendVisitor(int targetWebId, boolean approximateMatch){
		this.targetWebId = targetWebId;
		this.approximateMatch = approximateMatch;
	}

	/**
	 * Visit a node
	 * @param n the node to visit
	 */
	@Override
	public void visit(Node n, Attributes a) {
		if (n.getWebId() == targetWebId){
			finalNode = n;
			performTargetOperation(n);
		}
		else{
			performIntermediateOperation(n);
			Node next = n.getCloserNode(targetWebId, approximateMatch);
			if (next != null)
				next.accept(this, a);
			else if (approximateMatch){
				finalNode = n;
				performTargetOperation(n);
			}
		}
	}
	
	/**
	 * Gets the last node that was visited
	 * @return the last node visited; null, if the last node was not found
	 */
	public Node getFinalNode(){
		return finalNode;
	}
	
	/**
	 * Perform an operation on the target node (the one we were searching for)
	 * @param node the node we are visiting
	 */
	protected void performTargetOperation(Node node){}
	/**
	 * Perform an operation on an intermediate node
	 * @param node the node we are visiting
	 */
	protected void performIntermediateOperation(Node node){}
}
