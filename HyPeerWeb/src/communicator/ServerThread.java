package communicator;

import chat.server.ChatServer;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * This thread executes a command that arrived at the PeerCommunicator.  
 * @author Scott Woodfield
 */
public class ServerThread extends Thread {
	//The socket over which we communicate with the sending PeerCommunicator.
	private Socket client = null;
	//The ObjectInputStream from which we read the command.
	private ObjectInputStream ois = null;
	//The ObjectOutputStream over which we send the result of executing the command.
	private ObjectOutputStream oos = null;

	/**
	 * The ServerThread constructor.  It establishes the connection with the sending PeerCommunicator.
	 * @param clientSocket the socket from the PeerCommunicator used to communicate with the PeerCommunicator.
	 */
	public ServerThread(Socket clientSocket) {
	    client = clientSocket;
	    try {
	        ois = new ObjectInputStream(client.getInputStream());
	        oos = new ObjectOutputStream(client.getOutputStream());
	    } catch(IOException e1) {
	        try {
	           client.close();
	        }catch(IOException e) {
	           System.err.println(e.getMessage());
	        }
	    }
	}
	
	/**
	 * Reads the command and executes it.  If it is a synchronous command it returns a result.
	 */
	@Override
	public void run() {
	    try {
	        Command command = (Command) ois.readObject();
			System.out.println("> "+command.clazz+"."+command.methodName);
			//Detect server shutdwon
			if (ChatServer.isShutDown() && command.clazz.equals(ChatServer.className) && command.methodName.equals("startup")){
				System.err.println("The network has been shut down; Refusing to perform operation");
				return;
			}
	        if (command.commSync){
	            Object result = command.execute(true);
	            oos.writeObject(result);
	            oos.flush();
	            oos.close();
		        ois.close();
	            client.close();
	        } else {
	            oos.close();
		        ois.close();
	            client.close();
	            command.execute(false);
	        }
	    } catch(IOException | ClassNotFoundException e) {}       
	}
}