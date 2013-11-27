package hypeerweb;

import communicator.*;
import hypeerweb.visitors.AbstractVisitor;
import java.io.ObjectStreamException;
import java.util.ArrayList;

public class NodeProxy extends Node{
    private final RemoteAddress raddr;

    public NodeProxy(Node node){
		super(node.webID, node.height);
		L = new LinksProxy(node.getLinks());
		raddr = new RemoteAddress(node.UID);
    }

	//NODE OPERATIONS
	@Override
	protected Node addChild(Node child) {
		return (Node) request("addChild",new String[] {Node.className}, new Object[] {child}, true);
	}
	@Override
	protected boolean replaceNode(Node toReplace) {
		return (boolean) request("replaceNode", new String[] {Node.className}, new Object[] {toReplace}, true);
	}
	@Override
	protected Node disconnectNode() {
		return (Node) request("disconnectNode", null, null, true);
	}
	@Override
	protected Node findValidNode(Criteria x, int levels, boolean recursive) {
		return (Node) request("findValidNode", new String[] {"hypeerweb.Node$Criteria","int","boolean"}, new Object[]{x, levels, recursive}, true);
	}
	@Override
	protected Node findInsertionNode() {
		return (Node) request("findInsertionNode");
	}
	@Override
	protected Node findDisconnectNode() {
		return (Node) request("findDisconnectNode");
	}
	@Override
    public void accept(AbstractVisitor p0){
		request("accept", new String[]{"hypeerweb.visitors.AbstractVisitor"}, new Object[]{p0}, false);
    }
	
	//GETTERS
	@Override
    public hypeerweb.Node[] getNeighbors(){
		return (Node[]) request("getNeighbors");
    }
	@Override
    public Node getFold(){
		return (Node) request("getFold");
    }
	@Override
    public ArrayList getTreeChildren(){
		return (ArrayList<Node>) request("getTreeChildren");
    }
	@Override
    public Node getTreeParent(){
		return (Node) request("getTreeParent");
    }
	@Override
    public Node[] getSurrogateNeighbors(){
		return (Node[]) request("getSurrogateNeighbors");
    }
	@Override
    public Node getCloserNode(int p0, boolean p1){
		return (Node) request("getCloserNode", new String[]{"int", "boolean"}, new Object[]{p0, p1}, true);
    }
	@Override
    public Node[] getInverseSurrogateNeighbors(){
		return (Node[]) request("getInverseSurrogateNeighbors");
	}
	@Override
    public Node getSurrogateFold(){
        return (Node) request("getSurrogateFold");
    }
	@Override
    public Node getInverseSurrogateFold(){
		return (Node) request("getInverseSurrogateFold");
    }
	@Override
	public Object getData(String key) {
		return request("getData", new String[] {"java.lang.String"}, new Object[] {key}, true);
	}
	@Override
	protected FoldState getFoldState() {
		return (FoldState) request("getFoldState");
	}
	@Override
	public HyPeerWebSegment getHostSegment(){
		return (HyPeerWebSegment) request("getHostSegment");
	}
	
	//SETTERS
	@Override
	public void setWebID(int id) {
		request("setWebID", new String[] {"int"}, new Object[] {id}, false);
	}
	@Override
	protected void setHeight(int h) {
		request("setHeight", new String[] {"int"}, new Object[] {h}, false);
	}
	@Override
	protected void setFoldState(FoldState state) {
		request("setFoldState", new String[] {"hypeerweb.Node$FoldState"}, new Object[] {state}, false);
	}
	@Override
	public void setData(String key, Object val) {
		request("setData", new String[] {"java.lang.String", "java.lang.Object"}, new Object[] {key, val}, false);
	}
	
	private Object request(String name){
		return request(name, null, null, true);
	}
	private Object request(String name, String[] paramTypes, Object[] paramVals, boolean sync){
		Command command = new Command(Node.className, name, paramTypes, paramVals);
		return Communicator.request(raddr, command, sync);
	}
	
	@Override
	public Object writeReplace() throws ObjectStreamException {
		return this;
	}
	@Override
	public Object readResolve() throws ObjectStreamException {
		if (raddr.onSameMachineAs(Communicator.getAddress()))
			return Communicator.resolveId(Node.class, raddr.UID);
		return this;
	}
}