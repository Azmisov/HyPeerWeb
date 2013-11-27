package hypeerweb.visitors;

import communicator.Command;
import hypeerweb.Node;

/**
 * Navigates from one node to another; listener callback is
 * guaranteed to run on the same machine the node is on
 */
public class SendVisitor extends AbstractVisitor{
	//Attributes we will store in the visitor data
	private final int target;
	private final boolean approximate;
	
	/**
	 * Navigate to the specified node, with approximateMatch disabled
	 * @param targetWebId the WebID of the node to navigate to
	 * @param listener the command to execute on the target node
	 */
	public SendVisitor(int targetWebId, Command listener){
		this(targetWebId, false, listener);
	}
	/**
	 * Creates a new Send operation visitor
	 * @param targetWebId the Node's WebID that we are searching for
	 * @param approximateMatch if false, it will try to find an exact match to targetWebId;
	 * if true, it will try to find a node that is close to targetWebID and may not
	 * get all the way to the node (e.g. use this to get random nodes)
	 * @param listener the command to execute on the target node
	 */
	public SendVisitor(int targetWebId, boolean approximateMatch, Command listener){
		super(listener);
		target = targetWebId;
		approximate = approximateMatch;
	}

	/**
	 * Visit a node
	 * @param n the node to visit
	 */
	@Override
	public final void visit(Node n){
		//We found a match!
		if (n.getWebId() == target)
			callback(n);
		//Otherwise, get the next closest node
		else{
			Node next = n.getCloserNode(target, approximate);
			if (next != null) next.accept(this);
			//Pass null, if we couldn't find the node
			else callback(approximate ? n : null);
		}
	}
}
