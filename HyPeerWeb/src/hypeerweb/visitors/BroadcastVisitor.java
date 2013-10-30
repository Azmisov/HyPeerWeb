package hypeerweb.visitors;

import hypeerweb.Node;
import java.util.List;

/**
 *
 * @author Josh
 */
public class BroadcastVisitor implements VisitorInterface{
	
	public BroadcastVisitor(){
		
	}

	@Override
	public void visit(Node n) {
		performOperation(n);
		List<Node> broadcastNeighbors = n.getBroadcastNodes();
		for(Node neighbor : broadcastNeighbors){
			neighbor.accept(this);
		}
	}
	
	protected void performOperation(Node n){
		
	}
}
