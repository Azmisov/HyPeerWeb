/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hypeerweb;

import hypeerweb.visitors.BroadcastVisitor;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Joshua
 */
public class ListNodesVisitor extends BroadcastVisitor {
	
	private List<Node> nodeList;
	
	public ListNodesVisitor(){
		nodeList = new ArrayList<>();
	}
	
	public List<Node> getNodeList() {
		return nodeList;
	}

	@Override
	protected void performOperation(Node n) {
		nodeList.add(n);
	}
}
