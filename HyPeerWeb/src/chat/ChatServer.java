package chat;

import hypeerweb.HyPeerWebSegment;
import hypeerweb.Node;
import hypeerweb.visitors.SendVisitor;
import java.util.ArrayList;
import java.util.Random;

/**
 * Handles communications in the chat network
 */
public class ChatServer{
	private HyPeerWebSegment<HyPeerWebSegment<Node>> segment;
	private ChatUser user;
	//private String networkName = "";
	private ArrayList<SendListener> sendListeners = new ArrayList();
	private ArrayList<UserListener> userListeners = new ArrayList();
	private ArrayList<NodeListener> nodeListeners = new ArrayList();
	private ArrayList<NetworkNameListener> networkNameListeners = new ArrayList();
	
	public ChatServer(String dbName) throws Exception{
		segment = new HyPeerWebSegment(dbName, -1, this);
	}
	
	/**
	 * Adds a node to the HyPeerWeb and tells the nodeListeners about it.
	 */
	public void addNode() throws Exception{
		Node node = segment.addNode();
		for(NodeListener listener : nodeListeners)
			listener.callback(node, true);
	}
	
	/**
	 * Deletes a node from the HyPeerWeb and tells the nodeListeners about it.
	 * @param node the node to delete
	 */
	public void deleteNode(Node node){
		node = segment.deleteNode(node);
		for(NodeListener listener : nodeListeners)
			listener.callback(node, false);
	}
	
	/**
	 * 
	 * @return 
	 */
	public ArrayList<Node> getAllNodes(){
		ArrayList<Node> nodes = new ArrayList();
		//use broadcast to make a list of all nodes.
			
		return nodes;
	}
	/**
	 * Gets the name of the ChatUser
	 * @return the name
	 */
	public ChatUser getUser(){
		return user;
	}
	
	/**
	 * 
	 * @return 
	 */
	public ArrayList<ChatUser> getAllUsers(){
		ArrayList<ChatUser>users = new ArrayList();
		//use broadcast to get all users
		return users;
	}
	
	/**
	 * 
	 * @param listener 
	 */
	public void addSendListener(SendListener listener){
		sendListeners.add(listener);
	}
	
	/**
	 * 
	 * @param listener
	 */
	public void addUserListener(UserListener listener){
		userListeners.add(listener);
	}
	
	/**
	 * 
	 * @param listener 
	 */
	public void addNodeListener(NodeListener listener){
		nodeListeners.add(listener);
	}
	
	/**
	 * 
	 * @param listener 
	 */
	public void addNetworkNameListener(NetworkNameListener listener){
		networkNameListeners.add(listener);
	}
	
	/**
	 * Sends a message to another ChatUser
	 * @param user the destination
	 * @param message the message
	 */
	public void sendMessage(ChatUser user, String message){
		SendVisitor visitor = new SendVisitor(user.getWebId());
		visitor.visit(segment);
	}
	/**
	 * Method called by sendVisitor to display message destined for this user
	 * @param message the message to display
	 */
	public void receiveMessage(String message){
		for(SendListener listener : sendListeners)
			listener.callback(user.getWebId(), message);
	}
	
	/**
	 * 
	 */
	public void disconnect(){
		//this one looks tough
		//I think this is the part where Dr. Woodfield said that if one segment
		//wanted to quit, all of the segments would have to quit.  Now I can 
		//see why.  Sending all of the nodes on this segment to live somewhere
		//else would be difficult.
	}
	
	/**
	 * 
	 * @param name 
	 */
	public void updateUserName(String name){
		user.changeName(name);
	}
	
	/**
	 * 
	 * @param name 
	 */
	public void updateNetworkName(String name){
		networkName = name;
	}
	
	static public class ChatUser {
	//Random color generator
		private static final Random rand = new Random();
		private static final int minRGB = 30, maxRGB = 200;
		//User attributes
		public String color, name;
		public int id;
		
		/**
		 * Create a new chat user
		 * @param name the user's name
		 */
		public ChatUser(int id, String name){
			//Random username color
			//RGB values between 100-250
			int delta = maxRGB-minRGB;
			color = String.format(
				"#%02x%02x%02x",
				rand.nextInt(delta)+minRGB,
				rand.nextInt(delta)+minRGB,
				rand.nextInt(delta)+minRGB
			);
			this.name = name;
			this.id = id;
		}
		/**
		 * Changes the name of this chatUser.
		 * @param newName The new name.
		 */
		public void changeName(String newName){
			name = newName;
		}
		@Override
		public String toString(){
			return name;
		}
		public int getWebId(){
			return id;
		}
	}
	
	public abstract class SendListener{
		abstract void callback(int recipientID, String mess);
	}
	
	public abstract class UserListener{
		abstract void callback();
	}
	
	public abstract class NodeListener{
		abstract void callback(Node affectedNode, boolean adding);
	}
	
	public abstract class NetworkNameListener{
		abstract void callback();
	}
	
	
}
