package chat;

import chat.client.ChatClient;
import chat.server.ChatServer;
import com.alee.laf.WebLookAndFeel;
import communicator.*;
import java.awt.EventQueue;
import javax.swing.JFrame;

/**
 * Command line startup
 * @author isaac
 */
public class Main {
	public static String executable, jvm;
	private static final Exception syntax = new Exception(
		"Invalid arguments!\n"+
		" - To start a client, don't pass any arguments\n"+
		" - To start a new network, use:\n"+
		"      [-new|-n]\n"+
		" - To spawn a server from an existing network, use:\n"+
		"      [-spawn|-s] machine:port\n"+
		" - Optionally, you can include a client to auto-connect to (for -new or -spawn):\n"+
		"      [-leech|-l] machine:port");
	public static void main(final String args[]){
		/* Get the location of this executable and JVM; Here are alternatives:
		  - URLDecoder.decode(ClassLoader.getSystemClassLoader().getResource(".").getPath(‌​), "UTF-8");
		  - String path = Test.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			String decodedPath = URLDecoder.decode(path, "UTF-8");
		*/
		executable = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		jvm = new java.io.File(new java.io.File(System.getProperty("java.home"), "bin"), "java").getAbsolutePath();
		try{
			//Parse terminal arguments
			boolean new_network = false;
			RemoteAddress spawner = null, leecher = null;
			if (args.length > 4)
				throw syntax;
			for (int i=0; i<args.length; i++){
				switch(args[i]){
					case "-n":
					case "-new":
						new_network = true;
						break;
					case "-s":
					case "-spawn":
						spawner = parseAddress(args[++i]);
						break;
					case "-l":
					case "-leech":
						leecher = parseAddress(args[++i]);
						break;
					default:
						throw syntax;
				}
			}
			if ((spawner != null && new_network) || (leecher != null && args.length == 2))
				throw syntax;
			//Create a new client or server, depending on the arguments
			final boolean f_new = new_network;
			final RemoteAddress f_spawn = spawner, f_leech = leecher;
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run(){
					boolean server = f_new || f_spawn != null;
					JFrame win;
					if (server){
						win = ChatServer.getInstance();
						((ChatServer) win).initialize(f_spawn, f_leech);
					}
					else{
						//Only load the look-and-feel for clients
						WebLookAndFeel.install();
						win = new ChatClient();
					}
					win.setVisible(true);
				}
			});
		} catch (Exception e){
			System.err.println(e.getMessage());
		}
	}
	
	public static RemoteAddress parseAddress(String raw) throws Exception{
		String[] addr = raw.split(":");
		if (addr.length != 2)
			throw new Exception("Invalid address format!\nExpected 'machine:port'");
		try{
			//Validate arguments
			int port = Integer.parseInt(addr[1]);
			if (port < RemoteAddress.MIN_PORT || port > RemoteAddress.MAX_PORT)
				throw new NumberFormatException();
			//Create a new remote address
			return new RemoteAddress(addr[0], port, 0);
		} catch (NumberFormatException e){
			throw new Exception("Invalid port number!\nMust be between "+RemoteAddress.MIN_PORT+" and "+RemoteAddress.MAX_PORT);
		} catch (Exception e){
			throw new Exception("Invalid machine/ip address!");
		}
	}
}
