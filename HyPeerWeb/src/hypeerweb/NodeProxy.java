package hypeerweb;

import communicator.*;
import java.io.ObjectStreamException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import validator.NodeInterface;

public class NodeProxy
    extends Node
{
    private GlobalObjectId globalObjectId;

    public NodeProxy(GlobalObjectId globalObjectId){
		super(0, 0);
        this.globalObjectId = globalObjectId;
    }

	@Override
    public boolean equals(java.lang.Object p0){
        String[] parameterTypeNames = new String[1];
        parameterTypeNames[0] = "java.lang.Object";
        Object[] actualParameters = new Object[1];
        actualParameters[0] = p0;
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "equals", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (Boolean)result;
    }

	@Override
    public int hashCode(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "hashCode", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (Integer)result;
    }

	@Override
    public java.lang.String toString(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "toString", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (java.lang.String)result;
    }

	@Override
    public int compareTo(NodeInterface p0){
        String[] parameterTypeNames = new String[1];
        parameterTypeNames[0] = "interface validator.NodeInterface";
        Object[] actualParameters = new Object[1];
        actualParameters[0] = p0;
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "compareTo", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (Integer)result;
    }

	@Override
    public Node getParent(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getParent", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public void accept(hypeerweb.visitors.AbstractVisitor p0, hypeerweb.Attributes p1){
        String[] parameterTypeNames = new String[2];
        parameterTypeNames[0] = "hypeerweb.visitors.AbstractVisitor";
        parameterTypeNames[1] = "hypeerweb.Attributes";
        Object[] actualParameters = new Object[2];
        actualParameters[0] = p0;
        actualParameters[1] = p1;
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "accept", parameterTypeNames, actualParameters, false);
        PeerCommunicator.getSingleton().sendASynchronous(globalObjectId, command);
    }

	@Override
    public hypeerweb.Node[] getNeighbors(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getNeighbors", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node[])result;
    }

	@Override
    public int getWebId(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getWebId", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (Integer)result;
    }

	@Override
    public hypeerweb.Node getFold(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getFold", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public hypeerweb.Links getLinks(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getLinks", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Links)result;
    }

	@Override
    public int getHeight(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getHeight", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (Integer)result;
    }

	@Override
    public java.util.ArrayList getTreeChildren(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getTreeChildren", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (java.util.ArrayList)result;
    }

	@Override
    public int scoreWebIdMatch(int p0){
        String[] parameterTypeNames = new String[1];
        parameterTypeNames[0] = "int";
        Object[] actualParameters = new Object[1];
        actualParameters[0] = p0;
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "scoreWebIdMatch", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (Integer)result;
    }

	@Override
    public hypeerweb.Node getTreeParent(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getTreeParent", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public hypeerweb.Node[] getSurrogateNeighbors(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getSurrogateNeighbors", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
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
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public hypeerweb.Node[] getInverseSurrogateNeighbors(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getInverseSurrogateNeighbors", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node[])result;
    }

	@Override
    public hypeerweb.Node getSurrogateFold(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getSurrogateFold", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public hypeerweb.Node getInverseSurrogateFold(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Node", "getInverseSurrogateFold", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }
	
	@Override
	public Object writeReplace() throws ObjectStreamException {
		return this;
	}
	
	@Override
	public Object readResolve() throws ObjectStreamException {
			
		try {
			if(globalObjectId.getMachineAddr().equals(InetAddress.getLocalHost().getHostAddress()))
				// return the actual Node somehow?
				return null;
		} catch (UnknownHostException ex) {
			Logger.getLogger(NodeProxy.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		return this;
	}
}