package hypeerweb;

import communicator.*;
import hypeerweb.visitors.AbstractVisitor;
import java.io.ObjectStreamException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class NodeProxy extends Node{
    private final RemoteAddress raddr;

    public NodeProxy(Node node){
		super(node.webID, node.height);
		raddr = new RemoteAddress(node.UID);
		L = new LinksProxy(raddr);
    }

	@Override
    public void accept(AbstractVisitor p0){
		request("accept", new String[]{"hypeerweb.visitors.AbstractVisitor"}, new Object[]{p0}, false);
    }
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
	
	private Object request(String name){
		return request(name, null, null, true);
	}
	private Object request(String name, String[] paramTypes, Object[] paramVals, boolean sync){
		Command command = new Command("hypeerweb.Node", name, paramTypes, paramVals);
		return Communicator.request(raddr, command, sync);
	}
	
	@Override
	public Object writeReplace() throws ObjectStreamException {
		return this;
	}
	@Override
	public Object readResolve() throws ObjectStreamException {
		RemoteAddress app = Communicator.getAddress();
		if (raddr.ip.equals(app.ip) && raddr.port.equals(app.port)){
			for (HyPeerWebSegment segment : HyPeerWebSegment.segmentList) {
				Node node = segment.getNode(webID, raddr.UID);
				if (node != null)
					return node;
			}
		}
		return this;
	}
}