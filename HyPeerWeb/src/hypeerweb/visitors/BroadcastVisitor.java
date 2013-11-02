package hypeerweb.visitors;

import hypeerweb.Node;
import java.util.List;

/**
 * Broadcast Visitor
 * @author Josh
 */
public class BroadcastVisitor extends SendVisitor{
	
	/**
	 * Begin broadcasting from this node; it will first run a
	 * SendVisitor operation to node with webID = 0; from there, it
	 * will begin the broadcast
	 * @param n a node to begin broadcasting from
	 */
	@Override
	final public void visit(Node n) {
		super.visit(n);
	}
	@Override
	final protected void performIntermediateOperation(Node node){}
	@Override
	final protected void performTargetOperation(Node node) {
		node.accept(new Broadcast());
	}
	
	/**
	 * Perform a broadcast operation
	 * @param n the node that has been broadcasted to
	 */
	protected void performOperation(Node n){
		
	}
	
	/**
	 * Private broadcast algorithm
	 * We don't want to expose our algorithm
	 * @author Josh
	 */
	private class Broadcast implements VisitorInterface{
		@Override
		public void visit(Node n) {
			performOperation(n);
			List<Node> broadcastNeighbors = n.getBroadcastNodes();
			for (Node neighbor : broadcastNeighbors)
				neighbor.accept(this);
		}
	}
}
