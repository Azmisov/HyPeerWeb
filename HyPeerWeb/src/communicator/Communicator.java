package communicator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The deamon used to both send and receive commands from other applications that may be on this or other machines.
 * The Communicator is a singleton. Communicator must be in the same package of all applications.
 * @author Scott Woodfield
 *
 */
public class Communicator extends Thread{
	private static Communicator instance = null;
	//Connection info for this server
	private static GlobalObjectId myGlobalObjectId;
	//The socket this Communicator is listening on.
	private static ServerSocket socket;
	//The variable indicating whether this PeerCommunicator (a long running process) should stop.
	private static boolean stop = false;
	
	/**
	 * Starts up the communicator
	 */
	private Communicator(PortNumber port) {
		try{
			String myIPAddress = InetAddress.getLocalHost().getHostAddress();
	   		myGlobalObjectId = new GlobalObjectId(myIPAddress, port, null);
	   		socket = new ServerSocket(myGlobalObjectId.getPortNumber().getValue());
			this.start();
		} catch(IOException e){
			System.err.println(e.getMessage());
			System.err.println(e.getStackTrace());
		}
	}

	/**
	 * Shuts down the communicator thread
	 * Also saves all Objects in the object database
	 */
	public static void shutdown(){
		try {
			stop = true;
			socket.close();
		} catch (IOException ex) {
			System.err.println("Failed to close socket connection");
		}
	}
	
	/**
	 * Initializes the server listener thread
	 * When it receives a command it forks a ServerThread that actually executes
	 * the command on a local object or class.  ServerThreads are used so the
	 * Communicator won't lose a command that comes soon after this current command.
	 */
	@Override
	public void run() {
		while (!stop){
			try {
				Socket client = socket.accept();
				ServerThread serverThread = new ServerThread(client);
				serverThread.start();
			} catch(IOException e) {
				System.err.println(e.getMessage());
				System.err.println(e.getStackTrace());
			}
		}
		ObjectDB.getSingleton().save(null);
	}
	
	/**
	 * Creates the single PeerCommunicator listening on the indicated port number.
	 * Must be invoked before a singleton is retrieved.
	 * @param portNumber port to listen on; null to use the default port
	 */
	public static void createPeerCommunicator(PortNumber portNumber){
		instance = new Communicator(portNumber);
	}
	
	/**
	 * Sends the indicated command to the target object indicated by the globalObjectId
	 * @param globalObjectId the identifier of the remote object
	 * @param command the command to be sent to the remote object
	 * @param sync should we wait for a response? (is this a synchronous command)
	 * @return the results of the command, if sync is true
	 */
	public static Object request(GlobalObjectId globalObjectId, Command command, boolean sync){
		Object result = null;
		try {
			command.sync = sync;
			command.localObjectId = globalObjectId.getLocalObjectId();
			//open a socket connection
			Socket ext_socket = new Socket(globalObjectId.getMachineAddr(), globalObjectId.getPortNumber().getValue());
			//object streams for java objects
			ObjectOutputStream oos = new ObjectOutputStream(ext_socket.getOutputStream());
			ObjectInputStream ois = new ObjectInputStream(ext_socket.getInputStream());
			oos.writeObject(command);
			oos.flush();
			//get the remote server's response message (if necessary)
			if (sync) result = ois.readObject();
			//close connection
			oos.close();
			ois.close();
		} catch(IOException | ClassNotFoundException e) {
			System.err.println(e.getMessage());
			System.err.println(e.getStackTrace());
		}  
		return result;
	}
}