package chat.server;

import chat.Main;
import chat.client.ChatClient;
import communicator.*;
import hypeerweb.*;
import hypeerweb.visitors.BroadcastVisitor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

/**
 * Handles communications in the chat network
 */
public class ChatServer extends JFrame{
	public static final String className = ChatServer.class.getName();
	public static final int UID = Communicator.assignId();
	//Chat servers are singletons
	private static ChatServer instance;
	//Inception web
	private static HyPeerWebSegment<HyPeerWebSegment<Node>> segment;
	private static String networkName = "";
	//Node cache for the entire HyPeerWeb
	private static NodeCache cache;
	//Cached list of all users
	private static final Random randomName = new Random();
	private static final HashMap<Integer, ChatUser> users = new HashMap();
	//List of all users and their GUI/Clients that are leeching on this network
	private static final HashMap<Integer, ChatUser> clients = new HashMap();
	
	private ChatServer(){
		segment = new HyPeerWebSegment("InceptionWeb.db", -1);

		setSize(500, 350);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		//Console GUI
		JScrollPane consoleScroll = new JScrollPane(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
		);
		JTextPane console = new JTextPane();
		consoleScroll.setViewportView(console);
		this.setVisible(true);
		
		MessageConsole m = new MessageConsole(console);
		m.redirectErr();
		m.redirectOut();
		m.setMessageLines(500);

		add(consoleScroll);
		
		Communicator.startup(0);
		setTitle("HyPeerWeb Chat Server: "+Communicator.getAddress().port);
	}
	/**
	 * Gets an instance of the server on this computer
	 * @return the server singleton
	 */
	public static ChatServer getInstance(){
		//Create the singleton
		if (instance == null){
			instance = new ChatServer();
			segment.setData("ChatServer", instance);
		}
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
		updateUser(user.id, user.name, user.networkID);
		//Send the new client the node cache and user list
		Command register = new Command(
			ChatClient.className, "registerServer",
			new String[]{RemoteAddress.className, NodeCache.className, ChatUser.className, ChatUser.classNameArr},
			new Object[]{Communicator.getAddress(), cache, user, users.values().toArray(new ChatUser[users.size()])}
		);
		Communicator.request(client, register, false);
		//Add new client to our list
		users.put(newUser, user);
		clients.put(newUser, user);
	}
	/**
	 * Unregisters a GUI/Client from the server
	 * @param userID the user ID associated with this client
	 */
	public static void unregisterClient(int userID){
		users.remove(userID);
		clients.remove(userID);
		//TODO, broadcast this user update to all other segments
	}
	
	//NETWORK
	/**
	 * Create a new JVM process to run a server
	 * @param spawner address of the spawning server; null to create a new network
	 * @param leecher address of the leeching client; null to skip pre-loading a client
	 */
	public static void startServerProcess(RemoteAddress spawner, RemoteAddress leecher){
		try {
			ArrayList<String> args = new ArrayList(Arrays.asList(new String[]{Main.jvm, "-cp", Main.executable, Main.className}));
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
		} catch (Exception e) {
			System.err.println("Failed to start a new JVM process!");
			System.err.println(e);
			System.err.println(e.getMessage());
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
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
	/**
	 * Initialize this server from an existing server
	 * @param spawner address of the spawning server
	 * @param leecher address of the leeching client
	 */
	public void initialize(RemoteAddress spawner, RemoteAddress leecher){
		if (spawner != null){
			System.out.println("TODO, intialize spawner here");
			/* TODO:
				Join the network by creating another node in "segment"
				Fetch the following from another segment in the InceptionWeb
				- cache
				- chatUsers
			*/
		}
		else cache = new NodeCache();
		//Auto-register client
		if (leecher != null)
			registerClient(leecher);
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
		segment.getFirstSegmentNode().addNode(newNode, new NodeListener() {
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
		HyPeerWebSegment hws = segment.getFirstSegmentNode();
		hws.removeNode(webID, new NodeListener(){
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
	public static void sendMessage(final int senderID, final int recipientID, final String message){
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
	public static void updateUser(final int userid, String username, final int networkid){
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
