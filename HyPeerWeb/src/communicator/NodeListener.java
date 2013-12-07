package communicator;

import hypeerweb.Node;

/**
 * Node operation callback
 * TODO:
 * Mimics RMI or AJAX; performs a remote operation that takes
 * a single parameter, a Node
 */
public class NodeListener extends Command{
	public static final String className = NodeListener.class.getName();
	
	/**
	 * Create a new node listener
	 * @param cname the class name
	 * @param mname a static method callback in that class
	 */
	public NodeListener(String cname, String mname){
		this(cname, mname, null, null);
	}
	/**
	 * Create a new node listener with an embedded callback
	 * @param cname the class name
	 * @param mname a static method callback in that class
	 * @param param an embedded callback
	 */
	public NodeListener(String cname, String mname, NodeListener cbk){
		this(cname, mname, new String[]{NodeListener.className}, new Object[]{cbk});
	}
	/**
	 * Create a new node listener with parameters
	 * @param cname the class name
	 * @param mname a static method callback in that class
	 * @param ptypes parameter class types
	 * @param pvals parameter values
	 */
	public NodeListener(String cname, String mname, String[] ptypes, Object[] pvals){
		super(cname, mname, ptypes, pvals);
	}
	
	/**
	 * Runs the callback on this node
	 * @param n the node to callback on
	 */
	public void callback(Node n){
		if (addedParamCount != 1)
			prependParameter(Node.className, n);
		else setParameter(0, n);
		//Should we execute this callback remotely?
		execute(false);		
	}
	/**
	 * Runs the callback on this node, with extra
	 * parameters for remove-node methods
	 * @param n1 the removed node
	 * @param n2 the node that replaced it
	 * @param i the old webID of the replacing node
	 */
	public void callback(Node n1, Node n2, int i){
		if (addedParamCount != 3){
			prependParameter("int", i);
			prependParameter(Node.className, n2);
			prependParameter(Node.className, n1);
		}
		else{
			setParameter(0, n1);
			setParameter(1, n2);
			setParameter(2, i);
		}
		execute(false);
	}
}
