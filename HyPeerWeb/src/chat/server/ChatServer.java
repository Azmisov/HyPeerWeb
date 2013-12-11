package chat.server;

import chat.Main;
import chat.client.ChatClient;
import communicator.*;
import hypeerweb.*;
import hypeerweb.validator.Validator;
import hypeerweb.visitors.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
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
	private static Segment<Node> segment;
	private static String networkName = "";
	//Node cache for the entire HyPeerWeb
	private static SegmentCache cache;
	private static final ArrayList<Integer> syncRequests = new ArrayList();
	private static final ArrayList<SyncRequest> syncResults = new ArrayList();
	//Cached list of all users
	private static final Random randomName = new Random();
	private static final HashMap<Integer, ChatUser> users = new HashMap();
	//List of all users and their GUI/Clients that are leeching on this network
	private static final HashMap<Integer, ChatUser> clients = new HashMap();
	
	private ChatServer(){
		segment = new Segment("HyPeerWeb.db", -1);
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
			new String[]{RemoteAddress.className, SegmentCache.className, ChatUser.className, ChatUser.classNameArr},
			new Object[]{Communicator.getAddress(), cache, user, users.values().toArray(new ChatUser[users.size()])}
		);
		Communicator.request(client, register, false);
		
		//Add new client to our list (this should come last)
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
					new String[]{RemoteAddress.className, Segment.className},
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
			cache = new SegmentCache();
		//Auto-register client
		if (leecher != null)
			registerClient(leecher);
	}
	protected static void _spawn(RemoteAddress rem, Segment seg){
		//Listener will execute on this segment/server (since we've enabled setRemote)
		NodeListener spawner = new NodeListener(ChatServer.className, "_spawnSendData");
		spawner.setRemote(true);
		segment.addSegment(seg, spawner);
	}
	protected static void _spawnSendData(Node new_seg){		
		new_seg.executeRemotely(new NodeListener(
			ChatServer.className, "_spawnReceiveData",
			new String[]{SegmentCache.className, ChatUser.classNameArr},
			new Object[]{cache, users.values().toArray(new ChatUser[users.size()])}
		));
	}
	protected static void _spawnReceiveData(Node n, SegmentCache spawn_cache, ChatUser[] spawn_users){
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
		//Send all data to the nearest connection
		Segment conn = (Segment) segment.L.getLowestLink();
		if (conn != null){
			//Disconnect from the inception web
			segment.removeSegment(new NodeListener(
				className, "_changeNetworkID"
			));
			//Send data to "conn"
			ChatUser[] rusers = clients.values().toArray(new ChatUser[clients.size()]);
			RemoteAddress[] raddress = new RemoteAddress[clients.size()];
			for (int i=0; i<rusers.length; i++)
				raddress[i] = rusers[i].client;
			conn.executeRemotely(new NodeListener(
				className, "_mergeServerData",
				new String[]{"hypeerweb.SegmentDB", ChatUser.classNameArr, RemoteAddress.classNameArr},
				new Object[]{segment.getDatabase(), rusers, raddress}
			));
		}
		//Disconnect all clients
		else{
			Command changeServer = new Command(
				ChatClient.className, "changeServer",
				new String[]{RemoteAddress.className},
				new Object[]{null}
			);
			for (ChatUser usr: clients.values())
				Communicator.request(usr.client, changeServer, false);
		}
	}
	public static void _changeNetworkID(Node removed, Node replaced, int oldWebID){
		//Change network ID's for the users
		int newID = replaced.getWebId();
		for (ChatUser usr: users.values()){
			if (usr.networkID == oldWebID)
				usr.networkID = newID;
		}
		//Change client stuff
		cache.changeNetworkID(oldWebID, replaced.getWebId());
		Command changeNetworkID = new Command(ChatClient.className, "changeNetworkID", 
				new String[]{"int","int"}, new Object[]{oldWebID, newID});
		for(ChatUser c : clients.values()){
			Communicator.request(c.client, changeNetworkID, false);
		}
	}
	public static void _mergeServerData(Node n, SegmentDB db, ChatUser[] rusers, RemoteAddress[] addresses){
		for(int i=0;i<rusers.length;i++){
			rusers[i].client = addresses[i];
			clients.put(rusers[i].id, rusers[i]);
		}
		db.transferTo(segment);
		Command changeServer = new Command(
			ChatClient.className, "changeServer",
			new String[]{RemoteAddress.className},
			new Object[]{Communicator.getAddress()}
		);
		for (int i=0; i<addresses.length; i++)
			Communicator.request(addresses[i], changeServer, false);
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
		segment.addNode(newNode, new NodeListener(
			className, "_addNode"
		));
	}
	protected static void _addNode(Node newNode){
		NodeCache clean = newNode.convertToCached();
		syncCache(cache.addNode(clean, true), clean, null);
	}
	/**
	 * Deletes a node from the HyPeerWeb and tells the nodeListeners about it.
	 * @param webID the webID of the node to delete
	 */
	public static void removeNode(int webID){
		if (segment.state != Segment.HyPeerWebState.HAS_NONE)
			{
				segment.removeNode(webID, new NodeListener(
					className, "_removeNode"
				));
			}
	}
	protected static void _removeNode(Node removed, Node replaced, int oldWebID){
		HashSet<Integer> dirty = cache.removeNode(removed.convertToCached(), true);
		//Replaced node is both an addedNode and a removedNode
		NodeCache cleanReplace = null;
		if (replaced != null){
			cleanReplace = replaced.convertToCached();
			dirty.addAll(cache.replaceNode(oldWebID, cleanReplace, true));
		}
		syncCache(dirty, cleanReplace, new int[]{removed.getWebId(), oldWebID});
	}
	public static void removeAllNodes(){
		segment.removeAllNodes(new NodeListener(
			className, "_removeAllNodes"
		));
	}
	public static void _removeAllNodes(Node n){
		cache = new SegmentCache();
		Command updater = new Command(
			ChatClient.className, "_removeAllNodes"
		);
		for (ChatUser usr: clients.values())
			Communicator.request(usr.client, updater, false);
	}
	/**
	 * Resyncs the node cache to the actual data in the HyPeerWeb
	 * @param addedNodes nodes that have been added or changed
	 * @param prefetched a node that has been updated, but has already been cached
	 * @param removedNodes nodes that have been removed
	 */
	private static void syncCache(HashSet<Integer> addedNodes, NodeCache prefetched, int[] removedNodes){
		//We need to retrieve cached versions of all addedNodes
		//Group them by networkID, and then delegate each segment to build a cache
		HashMap<Integer, ArrayList<Integer>> grouped = new HashMap();
		for (Integer id: addedNodes){
			//If the cache is missing an id, we're out of sync
			NodeCache cached = cache.nodes.get(id);
			if (cached == null)
				System.err.println("Warning! HyPeerWeb cache is out of sync!");
			else{
				int netID = cached.getNetworkId();
				ArrayList<Integer> lst = grouped.get(netID);
				if (lst == null){
					lst = new ArrayList();
					grouped.put(netID, lst);
				}
				lst.add(id);					
			}
		}
		
		//If there are no groups, we can execute the update now
		if (grouped.isEmpty()){
			NodeCache[] clean = prefetched == null ? null : new NodeCache[]{prefetched};
			_syncCache_broadcast(clean, removedNodes);
			return;
		}
		
		//Get an index to hold this sync request
		SyncRequest req = new SyncRequest(prefetched, removedNodes);
		int reqSize = syncRequests.size(),
			request_id = reqSize,
			numGroups = grouped.size();
		for (int i=0; i<reqSize; i++){
			if (syncRequests.get(i) == 0){
				syncRequests.set(i, numGroups);
				syncResults.set(i, req);
				request_id = i;
			}
		}
		if (request_id == reqSize){
			syncRequests.add(numGroups);
			syncResults.add(req);
		}
		
		//Retrieve all dirty nodes
		ArrayList<NodeCache> clean = new ArrayList();
		NodeListener retrieve = new NodeListener(
			className, "_syncCache_send",
			new String[]{RemoteAddress.className, "[I", "int"},
			new Object[]{Communicator.getAddress(), null, request_id}
		);
		for (Entry<Integer, ArrayList<Integer>> entry: grouped.entrySet()){
			//Convert parameters
			ArrayList<Integer> lst = entry.getValue();
			int netID = entry.getKey();
			int[] dirty = new int[lst.size()];
			for (int i=0, l=lst.size(); i<l; i++)
				dirty[i] = lst.get(i).intValue();
			//Execute the retrieval command
			retrieve.setBaseParameter(1, dirty);
			SendVisitor visitor = new SendVisitor(netID, retrieve);
			visitor.visit(segment);
		}
		
	}
	protected static void _syncCache_send(Node n, RemoteAddress origin, int[] dirty, int request_id){
		Communicator.request(origin, new Command(
			className, "_syncCache_retrieve",
			new String[]{SegmentCache.nodeClassNameArr, "int"},
			new Object[]{segment.getCache(dirty), request_id}
		), false);
	}
	protected static void _syncCache_retrieve(NodeCache[] clean, int request_id){
		SyncRequest req = syncResults.get(request_id);
		req.clean.addAll(Arrays.asList(clean));
		//If this is the last request, broadcast the updated cache
		int req_left = syncRequests.get(request_id);
		if (req_left == 1){
			//Build the update command
			NodeCache[] final_clean = req.clean.toArray(new NodeCache[req.clean.size()]);
			_syncCache_broadcast(final_clean, req.removed);
		}
		syncRequests.set(request_id, req_left-1);
	}
	protected static void _syncCache_broadcast(NodeCache[] clean, int[] removed){
		Command syncify = new Command(
			ChatClient.className, "updateNodeCache",
			new String[]{"[I", SegmentCache.nodeClassNameArr},
			new Object[]{removed, clean}
		);
		new BroadcastVisitor(new NodeListener(
			className, "_syncCache_update",
			new String[]{Command.className},
			new Object[]{syncify}
		)).visit(segment);
	}
	protected static void _syncCache_update(Node n, Command syncify){
		//Notify all listeners that the cache changed
		for (ChatUser user : clients.values())
			Communicator.request(user.client, syncify, false);
		//Update the server's cache
		int[] toRemove = (int[]) syncify.getParameter(0);		
		if (toRemove != null){
			for (int webID: toRemove)
				cache.removeNode(webID, false);
		}
		NodeCache[] toUpdate = (NodeCache[]) syncify.getParameter(1);
		if (toUpdate != null){
			for (NodeCache node: toUpdate)
				cache.addNode(node, false);
		}
	}
	private static class SyncRequest{
		public final ArrayList<NodeCache> clean = new ArrayList();
		public final int[] removed;
		public SyncRequest(NodeCache prefetched, int[] removed){
			if (prefetched != null)
				clean.add(prefetched);
			this.removed = removed;
		}
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
	public static void _debug(){
		SegmentCache actualCache = segment.getCache();
		Validator v = new Validator(actualCache);
		System.err.println("PRINTING SERVER DATA");
		System.out.println("ISTATE = "+segment.inceptionState);
		try{
			boolean valid = v.validate();
			System.out.println("Valid = "+valid);
			if (!valid){
				System.out.println(actualCache);
			}
		} catch (Exception e){
			System.err.println("Error validating:");
			System.err.println(e.getMessage());
		}
		System.err.println("--------------------");
	}
}
