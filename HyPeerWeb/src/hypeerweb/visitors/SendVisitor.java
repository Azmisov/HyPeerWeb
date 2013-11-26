package hypeerweb.visitors;

import hypeerweb.Node;

/**
 * Navigates from one node to another; listener callback is
 * guaranteed to run on the same machine the node is on
 */
public class SendVisitor extends AbstractVisitor{
	//Attributes we will store in the visitor data
	private static final String
		target = "TARGET",
		approx = "APPROX",
		listen = "LISTEN";
	private transient boolean hasListener = false;
	
	/**
	 * Navigate to the specified node, with approximateMatch disabled
	 * @param targetWebId the WebID of the node to navigate to
	 */
	public SendVisitor(int targetWebId, Node.Listener command){
		this(targetWebId, false, command);
	}
	/**
	 * Creates a new Send operation visitor
	 * @param targetWebId the Node's WebID that we are searching for
	 * @param approximateMatch if false, it will try to find an exact match to targetWebId;
	 * if true, it will try to find a node that is close to targetWebID and may not
	 * get all the way to the node (e.g. use this to get random nodes)
	 */
	public SendVisitor(int targetWebId, boolean approximateMatch, Node.Listener command){
		data.setAttribute(target, targetWebId);
		data.setAttribute(approx, approximateMatch);
		data.setAttribute(listen, command);
		hasListener = command == null;
	}

	/**
	 * Visit a node
	 * @param n the node to visit
	 * @param o data to pass along
	 */
	@Override
	public final void visit(Node n) {
		if (!hasListener) return;
		int targetID = (int) data.getAttribute(target);
		boolean approxMatch = (boolean) data.getAttribute(approx);
		
		if (n.getWebId() == targetID)
			((Node.Listener) data.getAttribute(listen)).callback(n);
		else{
			Node next = n.getCloserNode(targetID, approxMatch);
			if (next != null)
				next.accept(this);
			else{
				Node.Listener cbk = (Node.Listener) data.getAttribute(listen);
				//Pass null, if we couldn't find the node
				cbk.callback(approxMatch ? n : null);
			}
		}
	}
}
