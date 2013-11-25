/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chat;

import communicator.Command;
import communicator.Communicator;
import communicator.RemoteAddress;
import hypeerweb.Node;
import hypeerweb.NodeCache;
import java.io.ObjectStreamException;

/**
 *
 * @author gwerner
 */
public class ChatClientProxy extends ChatClient{
	private final RemoteAddress raddr;
	
	public ChatClientProxy(ChatClient client){
		raddr = new RemoteAddress(client.UID);
	}
	
	@Override
	public void updateNetworkName(String newName){
		request("updateNetworkName", new String[] {"java.lang.String"}, new Object[] {newName}, false);
	}
	
	@Override
	public void updateUser(int userid, String username, int networkid){
		request("updateUser", new String[] {"int","java.lang.String","int"}, new Object[] {userid, username, networkid}, false);
	}
	
	@Override
	public void updateNodeCache(NodeCache.Node affectedNode, NodeCache.SyncType type, NodeCache.Node[] updatedNodes){
		request("updateNodeCache", new String[] {"hypeerweb.NodeCache.Node","hypeerweb.NodeCache.SyncType","hypeerweb.NodeCache.Node[]"}, new Object[] {affectedNode, type, updatedNodes}, false);
	}
	
	@Override
	public void receiveMessage(int senderID, int recipientID, String mess){
		request("receiveMessage", new String[] {"int","int","java.lang.String"}, new Object[] {senderID, recipientID, mess}, false);
	}
	
	private Object request(String name){
		return request(name, null, null, true);
	}
	private Object request(String name, String[] paramTypes, Object[] paramVals, boolean sync){
		Command command = new Command("chat.ChatClient", name, paramTypes, paramVals);
		return Communicator.request(raddr, command, sync);
	}
	
	
	public Object writeReplace() throws ObjectStreamException {
		return this;
	}
	
	public Object readResolve() throws ObjectStreamException {
		if (raddr.onSameMachineAs(Communicator.getAddress()))
			Communicator.resolveId(ChatClient.class, raddr.UID);
		return this;
	}
}
