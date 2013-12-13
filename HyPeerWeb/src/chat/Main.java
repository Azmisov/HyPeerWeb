package chat;

import chat.client.ChatClient;
import chat.server.ChatServer;
import chat.server.ChatServerGUI;
import com.alee.laf.WebLookAndFeel;
import communicator.*;
import java.awt.EventQueue;
import java.net.URLDecoder;
import java.util.Arrays;
import javax.swing.JFrame;

/**
 * Command line startup
 * @author isaac
 */
public class Main {
	public static String className = Main.class.getName(), executable, jvm;
	private static final Exception syntax = new Exception(
		"Invalid arguments!\n"+
		" - To start a client, don't pass any arguments\n"+
		" - To start a new network, use:\n"+
		"      [-new|-n]\n"+
		" - To spawn a server from an existing network, use:\n"+
		"      [-spawn|-s] machine:port\n"+
		" - Optionally, you can include a client to auto-connect to (for -new or -spawn):\n"+
		"      [-leech|-l] machine:port\n"+
		" - You can also start a GUI console for the server using:\n"+
		"      [-gui|-g]");
	public static void main(final String args[]){
		try{
			//Get the location of this executable and JVM
			executable = URLDecoder.decode(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
			jvm = new java.io.File(new java.io.File(System.getProperty("java.home"), "bin"), "java").getAbsolutePath();
			//Parse terminal arguments
			int port = 0;
			boolean new_network = false, use_gui = false;
			RemoteAddress spawner = null, leecher = null;
			if (args.length > 7)
				throw syntax;
			for (int i=0; i<args.length; i++){
				switch(args[i]){
					case "-n":
					case "-new":
						new_network = true;
						break;
					case "-g":
					case "-gui":
						use_gui = true;
						break;
					case "-s":
					case "-spawn":
						if (args.length == ++i)
							throw syntax;
						spawner = parseAddress(args[i]);
						break;
					case "-l":
					case "-leech":
						if (args.length == ++i)
							throw syntax;
						leecher = parseAddress(args[i]);
						break;
					case "-p":
					case "-port":
						if (args.length == ++i)
							throw syntax;
						port = Integer.parseInt(args[i]);
						break;
					default:
						throw syntax;
				}
			}
			if ((spawner != null && new_network) ||
				(leecher != null && args.length == 2) ||
				(use_gui && !new_network && spawner == null))
			{
				throw syntax;
			}
			//Create a new client or server, depending on the arguments
			final int f_port = port;
			final boolean f_new = new_network, f_gui = use_gui;
			final RemoteAddress f_spawn = spawner, f_leech = leecher;
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run(){
					if (f_new || f_spawn != null){
						ChatServer server = ChatServer.getInstance(f_port);
						//Start GUI, if necessary
						if (f_gui){
							JFrame win = new ChatServerGUI();
							win.setVisible(true);
						}
						server.initialize(f_spawn, f_leech);
					}
					else{
						//Only load the look-and-feel for clients
						WebLookAndFeel.install();
						JFrame win = ChatClient.getInstance();
						win.setVisible(true);
					}
				}
			});
		} catch (Exception e){
			System.err.println("Error with arguments "+Arrays.toString(args));
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
