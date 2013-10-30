package hypeerweb.visitors;

import hypeerweb.Node;

/**
 *
 * @author Josh
 */
public class Broadcast extends SendVisitor {
	
	BroadcastVisitor broadcastVisitor = null;
	
	public Broadcast(BroadcastVisitor broadcastVisitor){
		this.broadcastVisitor = broadcastVisitor;
	}
	
	@Override
	protected void performTargetOperation(Node node) {
		node.accept(broadcastVisitor);
	}
}
