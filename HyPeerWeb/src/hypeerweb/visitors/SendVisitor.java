package hypeerweb.visitors;

import hypeerweb.HyPeerWeb;
import hypeerweb.Node;

public class SendVisitor implements VisitorInterface{
	private int targetWebId;
	private boolean found = false;
	
	/**
	 * Creates a new Send operation visitor
	 * @param targetWebId the Node's WebID that we are searching for
	 */
	public SendVisitor(int targetWebId){
		this.targetWebId = targetWebId;
	}
	
	public boolean wasFound(){
		return found;
	}

	@Override
	public void visit(Node n) {
		if (n.getWebId() == targetWebId){
			//Do something productive here
			found = true;
			return;
		}
		Node next = n.getCloserNode(targetWebId);
		if (next != null)
			next.accept(this);
	}
}
