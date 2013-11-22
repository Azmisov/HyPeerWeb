package hypeerweb;

import communicator.*;
import java.io.ObjectStreamException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NodeProxy
    extends Node
{
    private GlobalObjectId globalObjectId;

    public NodeProxy(Node node){
		super(node.getWebId(), 0);
		try {
			this.globalObjectId = new GlobalObjectId(PortNumber.getApplicationsPortNumber(), node.getLocalObjectId());
			L = new LinksProxy(PortNumber.getApplicationsPortNumber(), node.L.getLocalObjectId()));
		} catch (UnknownHostException ex) {
			Logger.getLogger(NodeProxy.class.getName()).log(Level.SEVERE, null, ex);
		}
    }

	@Override
    public java.lang.String toString(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "toString", parameterTypeNames, actualParameters, true);
        Object result = Communicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (java.lang.String)result;
    }

	@Override
    public void accept(hypeerweb.visitors.AbstractVisitor p0){
        String[] parameterTypeNames = new String[1];
        parameterTypeNames[0] = "hypeerweb.visitors.AbstractVisitor";
        Object[] actualParameters = new Object[1];
        actualParameters[0] = p0;
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "accept", parameterTypeNames, actualParameters, false);
        Communicator.getSingleton().sendASynchronous(globalObjectId, command);
    }

	@Override
    public hypeerweb.Node[] getNeighbors(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getNeighbors", parameterTypeNames, actualParameters, true);
        Object result = Communicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node[])result;
    }

	@Override
    public hypeerweb.Node getFold(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getFold", parameterTypeNames, actualParameters, true);
        Object result = Communicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public int getHeight(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getHeight", parameterTypeNames, actualParameters, true);
        Object result = Communicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (Integer)result;
    }

	@Override
    public java.util.ArrayList getTreeChildren(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getTreeChildren", parameterTypeNames, actualParameters, true);
        Object result = Communicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (java.util.ArrayList)result;
    }

	@Override
    public hypeerweb.Node getTreeParent(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getTreeParent", parameterTypeNames, actualParameters, true);
        Object result = Communicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public hypeerweb.Node[] getSurrogateNeighbors(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getSurrogateNeighbors", parameterTypeNames, actualParameters, true);
        Object result = Communicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node[])result;
    }

	@Override
    public hypeerweb.Node getCloserNode(int p0, boolean p1){
        String[] parameterTypeNames = new String[2];
        parameterTypeNames[0] = "int";
        parameterTypeNames[1] = "boolean";
        Object[] actualParameters = new Object[2];
        actualParameters[0] = p0;
        actualParameters[1] = p1;
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getCloserNode", parameterTypeNames, actualParameters, true);
        Object result = Communicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public hypeerweb.Node[] getInverseSurrogateNeighbors(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getInverseSurrogateNeighbors", parameterTypeNames, actualParameters, true);
        Object result = Communicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node[])result;
    }

	@Override
    public hypeerweb.Node getSurrogateFold(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getSurrogateFold", parameterTypeNames, actualParameters, true);
        Object result = Communicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public hypeerweb.Node getInverseSurrogateFold(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Object result = Communicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }
	
	private Object request(String name, String[] paramTypes, String[] paramVals, boolean sync){
		Command command = new Command("hypeerweb.Node", "getInverseSurrogateFold", paramTypes, paramVals, sync);
		return Communicator.request(globalObjectId, null, sync);
	}
	
	@Override
	public Object writeReplace() throws ObjectStreamException {
		return this;
	}
	@Override
	public Object readResolve() throws ObjectStreamException {
			
		try {
			if(globalObjectId.getMachineAddr().equals(InetAddress.getLocalHost().getHostAddress())
					&& globalObjectId.getPortNumber().equals(PortNumber.getApplicationsPortNumber()))
				
				for(HyPeerWebSegment segment : HyPeerWebSegment.segmentList) {
					Node node = segment.getNode(webID, globalObjectId.getLocalObjectId());
					if (node != null)
						return node;
				}
				return null;
		} catch (UnknownHostException ex) {
			Logger.getLogger(NodeProxy.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		return this;
	}
}