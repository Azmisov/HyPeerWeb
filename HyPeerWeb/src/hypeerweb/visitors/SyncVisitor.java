package hypeerweb.visitors;

import hypeerweb.Node;

/**
 *
 * @author gwerner
 */
public class SyncVisitor extends AbstractVisitor{
	private static final String cbk = "CALLBACK";
	
	public SyncVisitor(SyncListener listener){
		//TODO: convert listener to a proxy
		data.setAttribute(cbk, listener);
	}

	@Override
	public void visit(Node n) {
		
		//Broadcast to children
		Integer blacklist = (Integer) (a != null ? a.getAttribute(childOrigin) : null);
		for (Node child : n.getTreeChildren()){
			if (blacklist == null || child.getWebId() != blacklist)
				child.accept(this, null);
		}
		//Broadcast to parent, if necessary
		if (blacklist != null && a != null){
			Node parent = n.getTreeParent();
			if (parent != null){
				//Put child in blacklist, so we don't broadcast to it again
				a.setAttribute(childOrigin, n.getWebId());
				parent.accept(this, a);
			}
		}
		//todo
	}
	
	/**
	 * Perform a broadcast operation
	 * @param n the node that has been broadcasted to
	 * @param a a parameter object
	 * @return the modified object
	 */
	public Object performOperation(Node n, Object a){
		return null;
	}
}
