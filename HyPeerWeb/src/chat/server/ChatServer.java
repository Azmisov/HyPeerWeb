package chat.server;

import chat.client.ChatClient;
import communicator.Communicator;
import hypeerweb.HyPeerWebSegment;
import hypeerweb.Node;
import hypeerweb.NodeCache;
import hypeerweb.visitors.BroadcastVisitor;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;

/**
 * Handles communications in the chat network
 */
public class ChatServer implements Serializable{
	public final int UID = Communicator.assignId();
	private final HyPeerWebSegment<HyPeerWebSegment<Node>> segment;
	private String networkName = "";
	//Node cache for the entire HyPeerWeb
	private NodeCache cache;
	//Cached list of all users
	private static final Random randomName = new Random();
	private final HashMap<Integer, ChatUser> users = new HashMap();
	//List of all users and their GUI/Clients that are leeching on this network
	private final HashMap<Integer, ChatUser> clients = new HashMap();
	
	public ChatServer() throws Exception{
		segment = new HyPeerWebSegment("InceptionWeb.db", -1);
		segment.setData("ChatServer", this);
		/* TODO:
			Join the network by creating another node in "segment"
			Fetch the following from another segment in the InceptionWeb
			- cache
			- chatUsers
		*/
	}
	
	//GUI
	/**
	 * Register a GUI/Client with this server (Watch/Leech)
	 * @param client the ChatClient to register with this server
	 * @return the ChatUser for this GUI/Client
	 */
	public ChatUser registerClient(ChatClient client){
		//Generate a userID that has not been taken already
		int newUser;
		do{
			newUser = randomName.nextInt(9999);
		} while (users.containsKey(newUser));
		ChatUser user = new ChatUser(newUser, "user"+newUser, segment.getWebId());
		user.client = client;
		users.put(newUser, user);
		clients.put(newUser, user);
		//broadcast this user update to all segments & userListeners
		updateUser(user.id, user.name, user.networkID);
		//TODO, send the client the nodecache, userlist, etc, through the listeners
		
		return user;
	}
	/**
	 * Unregisters a GUI/Client from the server
	 * @param userID the user ID associated with this client
	 */
	public void unregisterClient(int userID){
		users.remove(userID);
		clients.remove(userID);
		//TODO, broadcast this user update to all other segments
	}
	
	//NETWORK
	/**
	 * Spawn a new server off of this one
	 */
	public void spawnNewServer(){
		//We may need to write our own communication thing
		//instead of calling this method
	}
	/**
	 * Disconnect from the network
	 */
	public void disconnect(){
		//this one looks tough
		//I think this is the part where Dr. Woodfield said that if one segment
		//wanted to quit, all of the segments would have to quit.  Now I can 
		//see why.  Sending all of the nodes on this segment to live somewhere
		//else would be difficult.
	}
	/**
	 * Shutdown all servers in this network
	 */
	public void shutdown(){}
	/**
	 * Change the ChatServer's name
	 * @param name 
	 */
	public void changeNetworkName(String name){
		networkName = name;
		//broadcast to all network name listeners
	}
	
	//NODES
	/**
	 * Adds a node to the HyPeerWeb and tells the nodeListeners about it.
	 */
	public void addNode(){
		segment.getFirstSegmentNode().addNode(new Node.Listener() {
			@Override
			public void callback(Node n) {
				resyncCache(n, NodeCache.SyncType.ADD);
			}
		});
	}
	/**
	 * Deletes a node from the HyPeerWeb and tells the nodeListeners about it.
	 * @param webID the webID of the node to delete
	 */
	public void removeNode(int webID){
		HyPeerWebSegment hws = segment.getFirstSegmentNode();
		hws.removeNode(webID, new Node.Listener(){
			@Override
			public void callback(Node n) {
				resyncCache(n, NodeCache.SyncType.REMOVE);
			}
		});
	}
	/**
	 * Resyncs the node cache to the actual data in the HyPeerWeb
	 * @param n the Node that changed
	 * @param type the change type
	 */
	private void resyncCache(Node n, NodeCache.SyncType type){
		//These are a list of dirty nodes in our cache
		int[] dirty = new int[1];
		switch (type){
			case ADD:
				dirty = cache.addNode(n, true);
				break;
			case REMOVE:
				dirty = cache.removeNode(n, true);
				break;
		}
		//Retrieve all dirty nodes
		NodeCache.Node clean[] = new NodeCache.Node[dirty.length];
		//populate clean array
		for(NodeCache.Node node : clean)
			cache.addNode(node, false);
		//Notify all listeners that the cache changed
		for(ChatUser user : clients.values())
			user.client.updateNodeCache(cache.nodes.get(n.getWebId()), type, clean);
	}
	
	//CHAT
	/**
	 * Sends a message to another ChatUser
	 * @param senderID who sent this message
	 * @param recipientID who should receive the message (-1 to give to everyone)
	 * @param message the message
	 */
	public void sendMessage(final int senderID, final int recipientID, final String message){
		if(recipientID == -1){
			(new BroadcastVisitor(new Node.Listener() {
				@Override
				public void callback(Node n) {
					ChatServer server = (ChatServer) n.getData("ChatServer");
					for(ChatUser user : server.clients.values())
						user.client.receiveMessage(senderID, -1, message);
				}
			})).begin(segment);
		}
		else{
			segment.getNode(users.get(recipientID).networkID, new Node.Listener(){
				@Override
				public void callback(Node n) {
					ChatServer server = (ChatServer) n.getData("ChatServer");
					ChatClient client = server.clients.get(recipientID).client;
					client.receiveMessage(senderID, recipientID, message);
				}
			});
		}
	}
	
	//USERS
	/**
	 * Changes a user's name
	 * @param userID the user's id we want to update
	 * @param name new name for this user
	 */
	public void updateUser(final int userid, String username, final int networkid){
		if (username != null && users.containsKey(userid)){
			users.get(userid).name = username;
			//broadcast name change
			new BroadcastVisitor(new Node.Listener() {
				@Override
				public void callback(Node n) {
					ChatServer server = (ChatServer) n.getData("ChatServer");
					for(ChatUser user : server.clients.values())
						user.client.updateUser(userid, networkName, networkid);
				}
			}).begin(segment);
		}
	}
	public static class ChatUser implements Serializable{
		//Random color generator
		private static final Random rand = new Random();
		private static final int minRGB = 30, maxRGB = 180;
		//User attributes
		public String color, name;
		public int id;
		//Server that owns this user
		public int networkID;
		public transient ChatClient client;
		
		/**
		 * Create a new chat user
		 * @param id a unique id for this user
		 * @param name the user's name
		 * @param networkid the network that contains this user
		 */
		public ChatUser(int id, String name, int networkid){
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
			this.networkID = networkid;
		}
		@Override
		public String toString(){
			return name;
		}
	}
	
	/**
	 *
	 * @return
	 * @throws Exception
	 */
	public Object writeReplace() throws Exception {
		return new ChatServerProxy(this);
	}
	public Object readResolve() throws ObjectStreamException {
		return this;
	}
}
