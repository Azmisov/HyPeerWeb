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
		addParameter(Node.className);
	}
	
	/**
	 * Runs the callback on this node
	 * @param n the node to callback on
	 */
	public void callback(Node n){
		setParameter(0, n);
		execute();
		setParameter(0, null);
	}
}