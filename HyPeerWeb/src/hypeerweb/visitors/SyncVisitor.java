package hypeerweb.visitors;

import hypeerweb.Attributes;
import hypeerweb.Node;

/**
 *
 * @author gwerner
 */
public class SyncVisitor extends AbstractVisitor{
	
	public SyncVisitor(SyncListener listener){
		
	}

	@Override
	public void visit(Node n, Object a) {
		//todo
	}
	
	/**
	 * Perform a broadcast operation
	 * @param n the node that has been broadcasted to
	 */
	public Object performOperation(Node n, Object a){
		return null;
	}
}
