package hypeerweb;

import hypeerweb.visitors.Broadcast;
import java.util.List;

/**
 *
 * @author Josh
 */
public class ListNodes extends Broadcast {
	
	ListNodesVisitor visitor;
	
	public ListNodes() {
		super(new ListNodesVisitor());
	}

	@Override
	protected void performTargetOperation(Node node) {
		visitor = new ListNodesVisitor();
		node.accept(visitor);
	}
	
	public List<Node> getNodeList(){
		return visitor.getNodeList();
	}
}
