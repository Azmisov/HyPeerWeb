package communicator;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The deamon used to both send and receive commands from other applications that may be on this or other machines.
 * The PeerCommunicator is a singleton.  PeerCommunicator must be in the same package of all applications.
 * 
 * <pre>
 * <b>Class Domain:</b>
 *     singleton: PeerCommunicator -- the singleton
 *     NO_OP    : Command          -- a NO_OP command that may be sent to a PeerCommunicator. When a PeerCommunicator executes the "stopThisConnection()"
 *                                 -- command, it sets the attribute "stop" to true but the process won't stop until it receives and executes the next
 *                                 -- command.  So, we send a NO_OP command from itself to itself.
 *     
 *     <b>Invariant:</b>
 *         NO_OP &ne; null
 *         
 * <b>Instance Domain:</b>
 *     myGlobalObjectId: GlobalObjectId -- the globalObjectId for the class PeerCommunicator.  Notice that on creation it has no LobalObjectId,
 *                                      -- meaning all commands to be invoked on this object are to be "static" methods invoked on the class and not on
 *                                      -- an instance of the class.
 *     stop            : boolean        -- After it has finished handling an incoming command, the PeerCommunicator checks this variable to see if it should
 *                                      -- stop.  The suggested mechanism for stopping a process.  See the method "run()" below.
 * </pre>
 * 
 * @author Scott Woodfield
 *
 */
public class PeerCommunicator 
    extends Thread
{
	/**
	 * The Implementation of the singleton.
	 */
	private static PeerCommunicator singleton = null;
	
	/**
	 * A command that does nothing.  It is sent from a PeerCommunicator to itself when trying to stop a PeerCommunicator.
     * The PeerCommunicator is a long running process.  It is in a while loop that only exists if the variable "stop" is true.
     * But, after checking to see if it should stop, the process waits for the next command, executes it and then checks again
     * to see if "stop" is true.  Thus, after setting the variable "stop" to true we must wait for another command.  Therefore,
     * after setting "stop" to true the PeerCommunicator immediately sends the "NO_OP" command to itself.  It receives it,
     * executes it (doing nothing), checks the "stop" variable and exits the loop.
	 * 
	 * This class will probably be placed in a different package in a different implementation.  Be sure to insert the appropriate package name 
     * before "PeerCommunicator". For instance, if this is in the package x.y.z the parameter "PeerCommunicator" would become "x.y.z.PeerCommunicator".
     * This should be the same package name in all instantiations of PeerCommunicator on any machine.
	 */
	private static Command NO_OP = new Command(null, "PeerCommunicator", "noop", new String[0], new Object[0], false);

	/**
	 * The GlobalObjectId of the PeerCommunicator. The port number of this GlobalObjectId is the port number the PeerCommunicator will listen on.
     * It is also used to specify the PeerCommunicator (itself) this PeerCommunicator will send the "NO_OP" command to when trying to stop.
	 */
	private GlobalObjectId myGlobalObjectId;
	
	/**
	 * The socket this PeerCommunicator is listening on.
	 */
    private ServerSocket serverSocket;
    
    /**
     * The variable indicating whether this PeerCommunicator (a long running process) should stop.
     */
    private boolean stop = false;

    /**
     * The private constructor used to create the PeerCommunicator singleton listening on the default port number.
     * 
     * @pre The default port number is not in use
     * @post This single PeerCommunicator is created and listening on the default port number defined in <b>PortNumber</b>.  This starts the long running
     * process, see "run()" below.
     */
    private PeerCommunicator() {
    	try{
    		String myIPAddress = InetAddress.getLocalHost().getHostAddress();
       	    myGlobalObjectId = new GlobalObjectId(myIPAddress, PortNumber.DEFAULT_PORT_NUMBER, null);
    	    serverSocket = new ServerSocket(myGlobalObjectId.getPortNumber().getValue());
        	this.start();
    	} catch(Exception e){
    		System.err.println(e.getMessage());
    		System.err.println(e.getStackTrace());
    	}
    }
    
    /**
     * The private constructor used to create the PeerCommunicator singleton listening on the provided port number.
     * 
     * @pre port number is not in use
     * @post This single PeerCommunicator is created and listening on the indicated port number.  This starts the long running process, see "run()" below.
     */
    private PeerCommunicator(PortNumber port) {
    	try{
    		String myIPAddress = InetAddress.getLocalHost().getHostAddress();
       	    myGlobalObjectId = new GlobalObjectId(myIPAddress, port, null);
       	    serverSocket = new ServerSocket(myGlobalObjectId.getPortNumber().getValue());
        	this.start();
    	} catch(Exception e){
    		System.err.println(e.getMessage());
    		System.err.println(e.getStackTrace());
    	}
    }

    /**
     * The command sent to a PeerCommunicator to stop it.  This sets the stop variable in the singleton to true but nothing will happen until the
     * PeerCommunicator receives the next command (see the "run()" method below).  Therefore, we send the NO_OP command from this PeerCommunicator to itself.
     * 
     * @pre <i>None</i>
     * @post this PeerCommunicator stops and all objects in this application have been saved.
     */
    public static void stopThisConnection(){
    	singleton.stop = true;
    	singleton.sendASynchronous(singleton.myGlobalObjectId, NO_OP);
    }
    
    /**
     * Sends a stop command to the PeerCommunicator identified by the globalObjectId.
	 * This class will probably be placed in a different package in a different implementation.  Be sure to insert the appropriate package name 
     * before "PeerCommunicator". For instance, if this is in the package x.y.z the parameter "PeerCommunicator" would become "x.y.z.PeerCommunicator".
     * This should be the same package name in all instantiations of PeerCommunicator on any machine.
     * 
     * @param globalObjectId  the globalObjectId of the PeerCommunicator we are going to send the stop command to.
     * 
     * @pre globalObjectId &ne; null AND the globalObjectId is the id of a PeerCommunicator listening on globalObjectId.portNumber on machine
     *      globalObjectId.machineAddr
     * @post the PeerCommunicator listening on the port of the machine identified by "globalObjectId" is stopped.
     */
    public static void stopConnection(GlobalObjectId globalObjectId){
    	Command command = 
    		new Command(null, "PeerCommunicator", "stopThisConnection", new String[0], new Object[0], false);
    	singleton.sendASynchronous(globalObjectId, command);
    }
    
    /**
     * The NO_OP command sent from a PeerCommunicator to itself when stopping itself.
     * 
     * @pre <i>None</i>
     * @post true
     */
    public static void noop(){}
    
    /**
     * Starts a long running process listening for commands arriving on this PeerCommunicator's port.  When it receives a command it forks a
     * ServerThread that actually executes the command on a local object or class.  ServerThreads are used so the PeerCommunicator won't loose a
     * command that comes soon after this current command.
     * 
     * Before waiting for the arrival of a command, it checks to see if the stop variable is true.  If so, it stops and saves all the objects in
     * the objectDB.
     * 
     * @pre <i>None</i>
     * @post Continuously waits for a command to arrive.  Just before waiting for the command it checks the "stop" variable.
     *       If true it saves all objects in the ObjectDB and stops.  When a command arrives it forks a new ServerThread that
     *       handles or executes the command.
     */
    public void run() {
        while(!stop) {
          try {
           Socket client = serverSocket.accept();
           ServerThread serverThread = new ServerThread(client);
           serverThread.start();
          } catch(Exception e) {
        	  System.err.println(e.getMessage());
        	  System.err.println(e.getStackTrace());
          }
        }
        ObjectDB.getSingleton().save(null);
      }
    
    /**
     * The singleton getter for the singleton pattern.  This one is slightly different in that it requires the singleton to have been created before being
     * invoked. The singleton is created by the "createPeerCommunicator()" or "createPeerCommunicator(PortNumber)" methods below.
     * 
     * @pre singleton &ne; null
     * @post result = singleton
     */
	public static PeerCommunicator getSingleton() {
		assert(singleton != null);
		
		return singleton;
	}
	
	/**
	 * Creates the single PeerCommunicator listening on the default port.  Must be invoked before a singleton is retrieved.
	 * 
	 * @pre singleton = null
	 * @post the singleton has been created and is listening on the default port number.
	 */
	public static void createPeerCommunicator(){
		assert singleton == null;
		
		singleton = new PeerCommunicator(PortNumber.DEFAULT_PORT_NUMBER);
	}
	
	/**
	 * Creates the single PeerCommunicator listening on the indicated port number.  Must be invoked before a singleton is retrieved.
	 * 
	 * @param portNumber the port number the PeerCommunicator will be listening on.
	 * 
	 * @pre singleton = null
	 * @post the singleton has been created and is listening on the indicated port number.
	 */
	public static void createPeerCommunicator(PortNumber portNumber){
		assert singleton == null;
		
		singleton = new PeerCommunicator(portNumber);
	}
	
	/**
	 * Sends the indicated command to the target object indicated by the globalObjectId.  The target (which can be a class) executes the command and returns
     * a result.
	 * 
	 * @param globalObjectId the identifier of an object or class in an application listening on port globalObjectId.portNumber running on the machine
     *                       globalObjectId.machineAddr
	 * @param command  the command to be sent to the target object.
	 *
	 * @pre globalObjectId &ne; null AND command &ne; null AND the command must be a legal command for the target object and a PeerCommunicator is listening
     *      on the indicated port and machine.
	 * @post The command is executed on the remote object and the result is returned.  If there is an exception in the communication, the associated message
     *       and stack trace is printed.  If the remote execution of the method caused an error, the returned result will be of type Throwable or Exception.
     *       These should be checked by the original invoking method.
	 */
    public Object sendSynchronous(GlobalObjectId globalObjectId, Command command)
    {
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        Object result = null;
        try {
          //open a socket connection
          Socket socket = new Socket(globalObjectId.getMachineAddr(), globalObjectId.getPortNumber().getValue());
          //O streams for objects
          oos = new ObjectOutputStream(socket.getOutputStream());
          ois = new ObjectInputStream(socket.getInputStream());
          oos.writeObject(command);
          oos.flush();
     
          //read an object from the server
          result = ois.readObject();

          oos.close();
          ois.close();

        } catch(Exception e) {
          System.err.println(e.getMessage());
  		  System.err.println(e.getStackTrace());
        }  

        return result;
    }
    
	/**
	 * Sends the indicated command to the target object indicated by the globalObjectId.  The target (which can be a class) executes the command.
     * No result is expected.
	 * 
	 * @param globalObjectId the identifier of an object or class in an application listening on port globalObjectId.portNumber running on the machine
     * globalObjectId.machineAddr
     *
	 * @param command  the command to be sent to the target object.
	 *
	 * @pre globalObjectId &ne; null AND command &ne; null AND the command must be a legal command for the target object and a PeerCommunicator is listening
     *      on the indicated port and machine.
	 * @post The command is executed on the remote object.  No result is returned.
	 */
    public void sendASynchronous(GlobalObjectId globalObjectId, Command command) {
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        try {
          //open a socket connection
          Socket socket = new Socket(globalObjectId.getMachineAddr(), globalObjectId.getPortNumber().getValue());
          //O streams for objects
          oos = new ObjectOutputStream(socket.getOutputStream());
          ois = new ObjectInputStream(socket.getInputStream());

          oos.writeObject(command);
          oos.flush();

          oos.close();
          ois.close();
        } catch(Exception e) {
          System.out.println(e.getMessage());
  		  System.err.println(e.getStackTrace());
        }
    }
}