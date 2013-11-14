package hypeerweb.visitors;

import hypeerweb.Node;

/**
 * Navigates from one node to another
 */
public class SendVisitor extends AbstractVisitor{
	//Attributes we will store in the visitor data
	private static final String
		target = "TARGET",
		approx = "APPROX";
	
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
		data.setAttribute(target, targetWebId);
		data.setAttribute(approx, approximateMatch);
	}

	/**
	 * Visit a node
	 * @param n the node to visit
	 * @param o data to pass along
	 */
	@Override
	public final void visit(Node n) {
		int targetID = (int) data.getAttribute(target);
		boolean approxMatch = (boolean) data.getAttribute(approx);
		
		if (n.getWebId() == targetID)
			performTargetOperation(n);
		else{
			performIntermediateOperation(n);
			Node next = n.getCloserNode(targetID, approxMatch);
			if (next != null)
				next.accept(this);
			else if (approxMatch)
				performTargetOperation(n);
		}
	}
	
	/**
	 * Perform an operation on the target node (the one we were searching for)
	 * @param node the node we are visiting
	 */
	public void performTargetOperation(Node node){}
	/**
	 * Perform an operation on an intermediate node
	 * @param node the node we are visiting
	 */
	public void performIntermediateOperation(Node node){}
}
