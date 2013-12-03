package chat.server;

import chat.Main;
import chat.client.ChatClient;
import communicator.*;
import hypeerweb.*;
import hypeerweb.visitors.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

/**
 * Handles communications in the chat network
 */
public class ChatServer{
	public static final String className = ChatServer.class.getName();
	public static final int UID = Communicator.assignId();
	//Chat servers are singletons
	private static ChatServer instance;
	private static boolean spawningComplete = false;
	//Inception web
	private static HyPeerWebSegment<Node> segment;
	private static String networkName = "";
	//Node cache for the entire HyPeerWeb
	private static NodeCache cache;
	//Cached list of all users
	private static final Random randomName = new Random();
	private static final HashMap<Integer, ChatUser> users = new HashMap();
	//List of all users and their GUI/Clients that are leeching on this network
	private static final HashMap<Integer, ChatUser> clients = new HashMap();
	
	private ChatServer(){
		segment = new HyPeerWebSegment("HyPeerWeb.db", -1);
		Communicator.startup(0);
	}
	/**
	 * Gets an instance of the server on this computer
	 * @return the server singleton
	 */
	public static ChatServer getInstance(){
		//Create the singleton
		if (instance == null)
			instance = new ChatServer();
		return instance;
	}
	
	//GUI
	/**
	 * Register a GUI/Client with this server (Watch/Leech)
	 * @param client the ChatClient to register with this server
	 * @return the ChatUser for this GUI/Client
	 */
	public static void registerClient(RemoteAddress client){
		//Perform a handshake with the client
		System.out.println("Registering client at "+client);
		if (!Communicator.handshake(ChatClient.className, client)){
			System.err.println("Client handshake failed");
			return;
		}
		
		//Generate a userID that has not been taken already
		int newUser;
		do{
			newUser = randomName.nextInt(9999);
		} while (users.containsKey(newUser));
		ChatUser user = new ChatUser(newUser, "user"+newUser, segment.getWebId());
		user.client = client;
		
		//Broadcast this user update to all segments & userListeners
		users.put(newUser, user);
		updateUser(user.id, user.name, user.networkID);
		
		//Send the new client the node cache and user list
		Command register = new Command(
			ChatClient.className, "registerServer",
			new String[]{RemoteAddress.className, NodeCache.className, ChatUser.className, ChatUser.classNameArr},
			new Object[]{Communicator.getAddress(), cache, user, users.values().toArray(new ChatUser[users.size()])}
		);
		Communicator.request(client, register, false);
		
		//Add new client to our list
		clients.put(newUser, user);
	}
	/**
	 * Unregisters a GUI/Client from the server
	 * @param userID the user ID associated with this client
	 */
	public static void unregisterClient(int userID){
		users.remove(userID);
		clients.remove(userID);
		//Broadcast user removal
		updateUser(userID, null, 0);
	}
	
	//NETWORK
	/**
	 * Create a new JVM process to run a server
	 * @param spawner address of the spawning server; null to create a new network
	 * @param leecher address of the leeching client; null to skip pre-loading a client
	 */
	public static void startServerProcess(RemoteAddress spawner, RemoteAddress leecher){
		try {
			ArrayList<String> args = new ArrayList(Arrays.asList(
				new String[]{Main.jvm, "-cp", Main.executable, Main.className, "-gui"}
			));
			if (spawner == null)
				args.add("-new");
			else{
				args.add("-spawn");
				args.add(spawner.toString());
			}
			if (leecher != null){
				args.add("-leech");
				args.add(leecher.toString());
			}
			System.out.println("Starting a new child server process...");
			Process x = new ProcessBuilder(args).start();
			new StreamGobbler(x.getErrorStream(), "Server Error").start();
		} catch (IOException e) {
			System.err.println("Failed to start a new JVM process!");
		}
	}
	private static class StreamGobbler extends Thread {
		InputStream is;
		String type;
		private StreamGobbler(InputStream is, String type) {
			this.is = is;
			this.type = type;
		}
		@Override
		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				while ((line = br.readLine()) != null)
					System.err.println(type + " > " + line);
			}
			catch (IOException ioe){
				System.err.println("Failed to capture server's error stream");
			}
		}
	}
	/**
	 * Initialize this server from an existing server
	 * @param spawner address of the spawning server (null to create new network)
	 * @param leecher address of the leeching client
	 */
	public void initialize(RemoteAddress spawner, RemoteAddress leecher){
		//Spawn this network from another
		if (spawner != null){
			//Make sure the spawner exists
			if (!Communicator.handshake(ChatServer.className, spawner)){
				System.err.println("Spawning address does not refer to a chat server!");
				System.err.println("Creating a new network instead...");
				spawner = null;
			}
			else{
				Command spawn = new Command(
					ChatServer.className, "_spawn",
					new String[]{RemoteAddress.className, HyPeerWebSegment.className},
					new Object[]{Communicator.getAddress(), segment}
				);
				Communicator.request(spawner, spawn, false);
				//Wait until the data comes back before proceeding				
				synchronized (ChatServer.instance){
					while (!spawningComplete){
						try {
							ChatServer.instance.wait();
						} catch (InterruptedException ex) {
							System.err.println("Error waiting for spawn completion");
						}
					}
				}
			}
		}
		//This is a new network; no spawning necessary
		if (spawner == null)
			cache = new NodeCache();
		//Auto-register client
		if (leecher != null)
			registerClient(leecher);
	}
	protected static void _spawn(RemoteAddress rem, HyPeerWebSegment seg){
		//Listener will execute on this segment/server
		segment.addSegment(seg, new NodeListener(
			ChatServer.className, "_spawnSendData",
			new String[]{RemoteAddress.className},
			new Object[]{rem}
		));
	}
	protected static void _spawnSendData(Node n, RemoteAddress rem){		
		Command transfer = new Command(
			ChatServer.className, "_spawnReceiveData",
			new String[]{NodeCache.className, ChatUser.classNameArr},
			new Object[]{cache, users.values().toArray(new ChatUser[users.size()])}
		);
		Communicator.request(rem, transfer, false);
	}
	protected static void _spawnReceiveData(NodeCache spawn_cache, ChatUser[] spawn_users){
		cache = spawn_cache;
		for (ChatUser usr: spawn_users)
			users.put(usr.id, usr);
		
		//Notify thread that spawning is complete
		spawningComplete = true;
		synchronized (ChatServer.instance){
			ChatServer.instance.notify();
		}
	}
	/**
	 * Disconnect from the network
	 */
	public static void disconnect(){
		//this one looks tough
		//I think this is the part where Dr. Woodfield said that if one segment
		//wanted to quit, all of the segments would have to quit.  Now I can 
		//see why.  Sending all of the nodes on this segment to live somewhere
		//else would be difficult.
	}
	/**
	 * Shutdown all servers in this network
	 */
	public static void shutdown(){}
	/**
	 * Change the ChatServer's name
	 * @param name 
	 */
	public static void changeNetworkName(String name){
		networkName = name;
		//broadcast to all network name listeners
	}
	
	//NODES
	/**
	 * Adds a node to the HyPeerWeb and tells the nodeListeners about it.
	 */
	public static void addNode(){
		hypeerweb.Node newNode = new hypeerweb.Node(0, 0);
		segment.addNode(newNode, new NodeListener() {
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
	public static void removeNode(int webID){
		segment.removeNode(webID, new NodeListener(){
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
	private static void resyncCache(Node n, NodeCache.SyncType type){
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
	public static void sendMessage(int senderID, int recipientID, String message){
		if (recipientID == -1){
			//Public message
			new BroadcastVisitor(new NodeListener(
				ChatServer.className, "_sendMessagePublic",
				new String[]{"int", "java.lang.String"},
				new Object[]{senderID, message}
			)).visit(segment);
		}
		else{
			//Private message
			new SendVisitor(
				users.get(recipientID).networkID, false,
				new NodeListener(
					ChatServer.className, "_sendMessagePrivate",
					new String[]{"int", "int", "java.lang.String"},
					new Object[]{senderID, recipientID, message}
				)
			).visit(segment);
		}
	}
	protected static void _sendMessagePublic(Node n, int senderID, String message){
		Command receiver = new Command(
			ChatClient.className, "receiveMessage",
			new String[]{"int", "int", "java.lang.String"},
			new Object[]{senderID, -1, message}
		);
		for (ChatUser user: clients.values())
			Communicator.request(user.client, receiver, false);
	}
	protected static void _sendMessagePrivate(Node n, int senderID, int recipientID, String message){
		RemoteAddress client = clients.get(recipientID).client;
		Command receiver = new Command(
			ChatClient.className, "receiveMessage",
			new String[]{"int", "int", "java.lang.String"},
			new Object[]{senderID, recipientID, message}
		);
		Communicator.request(client, receiver, false);
	}
	
	//USERS
	/**
	 * Changes a user's name
	 * @param userID the user's id we want to update
	 * @param name new name for this user (null to remove user)
	 */
	public static void updateUser(int userid, String username, int networkid){
		new BroadcastVisitor(new NodeListener(
			ChatServer.className, "_updateUser",
			new String[]{"int", "java.lang.String", "int"},
			new Object[]{userid, username, networkid}
		)).visit(segment);
	}
	protected static void _updateUser(Node n, int userid, String username, int networkid){
		ChatUser user = users.get(userid);
		//If this is a new user, create it
		if (user == null){
			user = new ChatUser(userid, username, networkid);
			users.put(userid, user);
		}
		//Update this user's info
		else{
			user.name = username;
			user.networkID = networkid;
		}
		//Send this update to GUI clients
		Command updater = new Command(
			ChatClient.className, "updateUser",
			new String[]{"int", "java.lang.String", "int"},
			new Object[]{userid, username, networkid}
		);
		for (ChatUser usr: clients.values())
			Communicator.request(usr.client, updater, false);
	}
	public static class ChatUser implements Serializable{
		public static final String
			className = ChatUser.class.getName(),
			classNameArr = ChatUser[].class.getName();
		//Random color generator
		private static final Random rand = new Random();
		private static final int minRGB = 30, maxRGB = 180;
		//User attributes
		public String color, name;
		public int id;
		//Server that owns this user
		public int networkID;
		public transient RemoteAddress client;
		
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
	
	//NETWORKING
	public static boolean handshake(){
		return instance != null;
	}
}
