/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chat.server;

import chat.client.ChatClient;
import communicator.Command;
import communicator.Communicator;
import communicator.RemoteAddress;
import java.io.ObjectStreamException;

/**
 *
 * @author briands
 */
public class ChatServerProxy extends ChatServer{
	private final RemoteAddress raddr;
	
	public ChatServerProxy(ChatServer server) throws Exception{
		raddr = new RemoteAddress(server.UID);
    }
	
	@Override
	public ChatUser registerClient(ChatClient client){
		return (ChatUser) request("registerClient", new String[]{"chat.ChatClient"}, new Object[]{client}, true);
	}
	@Override
	public void unregisterClient(int userID){
		request("unregisterClient", new String[]{"int"}, new Object[]{userID}, false);
	}
	@Override
	public void addNode(){
		request("addNode", null, null, false);
	}
	@Override
	public void removeNode(int webID){
		request("removeNode", new String[]{"int"}, new Object[]{webID}, false);
	}
	@Override
	public void sendMessage(final int senderID, final int recipientID, final String message){
		request("sendMessage", new String[]{"int", "int", "java.lang.String"}, new Object[]{senderID, recipientID, message}, false);
	}
	@Override
	public void updateUser(int userID, String username, final int networkid){
		request("changeUserName", new String[]{"int", "java.lang.String", "int"}, new Object[]{userID, username, networkid}, false);
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
		if (raddr.onSameMachineAs(Communicator.getAddress()))
			return Communicator.resolveId(ChatServer.class, raddr.UID);
		return this;
	}
}
