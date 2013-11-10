package hypeerweb;

import hypeerweb.visitors.BroadcastVisitor;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Joshua
 */
public class ListNodesVisitor extends BroadcastVisitor{
	
	private List<Node> nodeList;
	
	public ListNodesVisitor(){
		nodeList = new ArrayList<>();
	}
	
	public List<Node> getNodeList() {
		return nodeList;
	}

	@Override
	public void performOperation(Node n) {
		nodeList.add(n);
	}
}
