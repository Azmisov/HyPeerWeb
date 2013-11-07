package jamie;

import jamie.gui.BroadcastVisitor;
import jamie.model.Node;
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
	protected void intermediateOperation(Node node) {
		nodeList.add(node);
	}
}
