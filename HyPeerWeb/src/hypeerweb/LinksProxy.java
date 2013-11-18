package hypeerweb;

import communicator.*;

public class LinksProxy
    extends Links
{
    private GlobalObjectId globalObjectId;

    public LinksProxy(GlobalObjectId globalObjectId){
        this.globalObjectId = globalObjectId;
    }

	@Override
    public hypeerweb.Node getHighestLink(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getHighestLink", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public hypeerweb.Node getLowestLink(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getLowestLink", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public hypeerweb.Node getFold(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getFold", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public hypeerweb.Node getSurrogateFold(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getSurrogateFold", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public hypeerweb.Node getInverseSurrogateFold(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getInverseSurrogateFold", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public hypeerweb.Node[] getNeighbors(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getNeighbors", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node[])result;
    }

	@Override
    public hypeerweb.Node getHighestNeighbor(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getHighestNeighbor", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public hypeerweb.Node getLowestNeighbor(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getLowestNeighbor", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public hypeerweb.Node[] getSurrogateNeighbors(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getSurrogateNeighbors", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node[])result;
    }

	@Override
    public hypeerweb.Node getHighestSurrogateNeighbor(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getHighestSurrogateNeighbor", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public hypeerweb.Node getLowestSurrogateNeighbor(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getLowestSurrogateNeighbor", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public hypeerweb.Node[] getInverseSurrogateNeighbors(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getInverseSurrogateNeighbors", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node[])result;
    }

	@Override
    public hypeerweb.Node getHighestInverseSurrogateNeighbor(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getHighestInverseSurrogateNeighbor", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public hypeerweb.Node getLowestInverseSurrogateNeighbor(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getLowestInverseSurrogateNeighbor", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (hypeerweb.Node)result;
    }

	@Override
    public java.util.TreeSet getNeighborsSet(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getNeighborsSet", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (java.util.TreeSet)result;
    }

	@Override
    public java.util.TreeSet getSurrogateNeighborsSet(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getSurrogateNeighborsSet", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (java.util.TreeSet)result;
    }

	@Override
    public java.util.TreeSet getInverseSurrogateNeighborsSet(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getInverseSurrogateNeighborsSet", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (java.util.TreeSet)result;
    }

	@Override
    public java.util.TreeSet getAllLinks(){
        String[] parameterTypeNames = new String[0];
        Object[] actualParameters = new Object[0];
        Command command = new Command(globalObjectId.getLocalObjectId(), "hypeerweb.Links", "getAllLinks", parameterTypeNames, actualParameters, true);
        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);
        return (java.util.TreeSet)result;
    }
}