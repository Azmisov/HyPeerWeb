package hypeerweb.visitors;

import hypeerweb.Node;

public class SendVisitor implements VisitorInterface{
	private int targetWebId;
	
	/**
	 * Creates a new Send operation visitor
	 * @param targetWebId the Node's WebID that we are searching for
	 */
	public SendVisitor(int targetWebId){
		this.targetWebId = targetWebId;
	}

	@Override
	public void visit(Node n) {
		if (n.getWebId() == targetWebId){
			System.out.println("Hooray! "+n.getWebId());
			return;
		}
		Node next = n.getSendNode(targetWebId);
		if (next == null)
			System.out.println("You are an idiot");
		else next.accept(this);
	}
}
