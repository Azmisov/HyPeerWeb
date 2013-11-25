package hypeerweb;

import communicator.*;
import java.io.ObjectStreamException;

public class LinksProxy extends Links{
    private final RemoteAddress raddr;

    public LinksProxy(Links real){
		super(0);
		raddr = new RemoteAddress(real.UID);
    }

    //GETTERS
	@Override
	public Node[] getAllLinks(){
		return (Node[]) request("getAllLinks");
    }
		
        @Override
    public Node getHighestLink(){
                return (Node) request("getHighestLink");
    }
        @Override
    public Node getLowestLink(){
                return (Node) request("getLowestLink");
    }
        @Override
    public Node getFold(){
                return (Node) request("getFold");
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
    public Node[] getNeighbors(){
        return (Node[]) request("getNeighbors");
    }
        @Override
    public Node getHighestNeighbor(){
                return (Node) request("getHighestNeighbor");
    }
        @Override
    public Node getLowestNeighbor(){
        return (Node) request("getLowestNeighbor");
    }
        @Override
    public Node[] getSurrogateNeighbors(){
        return (Node[]) request("getSurrogateNeighbors");
    }
        @Override
    public Node getHighestSurrogateNeighbor(){
        return (Node) request("getHighestSurrogateNeighbor");
        }
        @Override
    public Node getLowestSurrogateNeighbor(){
        return (Node) request("getLowestSurrogateNeighbor");
    }
        @Override
    public Node[] getInverseSurrogateNeighbors(){
        return (Node[]) request("getInverseSurrogateNeighbors");
    }
        @Override
    public Node getHighestInverseSurrogateNeighbor(){
        return (Node) request("getHighestInverseSurrogateNeighbor");
    }
        @Override
    public Node getLowestInverseSurrogateNeighbor(){
		return (Node) request("getLowestInverseSurrogateNeighbor");
    }

    private Object request(String name){
		return request(name, null, null, true);
    }
    
	private Object request(String name, String[] paramTypes, Object[] paramVals, boolean sync){
		Command command = new Command("hypeerweb.Links", name, paramTypes, paramVals);
		return Communicator.request(raddr, command, sync);
    }
	
	@Override
	public Object writeReplace() throws ObjectStreamException {
		return this;
	}
	@Override
	public Object readResolve() throws ObjectStreamException {
		if (raddr.onSameMachineAs(Communicator.getAddress()))
			return ((Node)Communicator.resolveId(Node.class, raddr.UID)).L.UID;
		return this;
	}
}