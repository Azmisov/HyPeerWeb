/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chat;

import communicator.Command;
import communicator.Communicator;
import communicator.RemoteAddress;
import hypeerweb.HyPeerWebSegment;
import hypeerweb.Node;
import java.io.ObjectStreamException;

/**
 *
 * @author briands
 */
public class ChatServerProxy extends ChatServer{
	private final RemoteAddress raddr;
	
	public ChatServerProxy(ChatServer server) throws Exception{
		super();
		raddr = new RemoteAddress(server.UID);
    }
	
	private Object request(String name){
		return request(name, null, null, true);
	}
	private Object request(String name, String[] paramTypes, Object[] paramVals, boolean sync){
		Command command = new Command("chat.ChatServer", name, paramTypes, paramVals);
		return Communicator.request(raddr, command, sync);
	}
	
	@Override
	public Object writeReplace() throws ObjectStreamException {
		return this;
	}
	@Override
	public Object readResolve() throws ObjectStreamException {
		if (raddr.onSameMachineAs(Communicator.getAddress())){
			for (HyPeerWebSegment segment : HyPeerWebSegment.segmentList) {
				Node node = segment.getNode(webID, raddr.UID);
				if (node != null)
					return node;
			}
		}
		return this;
	}
}
