package chat;

import chat.client.ChatClient;
import chat.server.ChatServer;
import com.alee.laf.WebLookAndFeel;
import communicator.*;
import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command line startup
 * no arguments = startup GUI
 * -server/-s [machine:port] = startup server, with optional server to spawn from
 * @author isaac
 */
public class Main {
	private static final Exception syntax = new Exception(
		"Invalid arguments!\n"+
		"\tTo start a Client, don't pass any arguments"+
		"\tTo start a Server, use:"+
		"\t\t[-server|-s] [machine:port]?");
	public static void main(String args[]){
		try{
			//Running with no arguments starts client/GUI
			if (args.length == 0){
				//Load the look-and-feel
				WebLookAndFeel.install();

				//Start up the window
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						new ChatClient().setVisible(true);
					}
				});
			}
			//Otherwise start a server
			else if ("-s".equals(args[0]) || "-server".equals(args[0])){
				//New network
				if (args.length == 1){
					//Start up the window
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run() {
							new ChatServer(5200).setVisible(true);
						}
					});
					/*
					
					Communicator.startup(5200);
					System.out.println("This isn't implemented yet");
					String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
					System.out.println(path);
					//Something like: java -cp C:\Users\Matt\workspace\HelloWorld2\bin HelloWorld2
					//String[] term = new String[]{"/bin/bash","-c","java","-cp",path,Main.class.getName(),"-s","localhost:5200"};
					Runtime.getRuntime().exec("/bin/bash");
					//Process x = new ProcessBuilder("java","-cp",path,).start();
					
					/*
					CURRENT CLASSPATH:
					
					ClassLoader.getSystemClassLoader().getResource(".").getPath();
					
					CodeSource codeSource = YourMainClass.class.getProtectionDomain().getCodeSource();
					File jarFile = new File(codeSource.getLocation().toURI().getPath());
					String jarDir = jarFile.getParentFile().getPath();
					
					URLDecoder.decode(ClassLoader.getSystemClassLoader().getResource(".").getPath(‌​), "UTF-8");
					
					String path = Test.class.getProtectionDomain().getCodeSource().getLocation().getPath();
					String decodedPath = URLDecoder.decode(path, "UTF-8");
					
					return new File(MyClass.class.getProtectionDomain().getCodeSource().getLocation().toURI());‌
					
					Path path = Paths.get(Test.class.getProtectionDomain().getCodeSource().getLocation().toURI());
					
					IN TERMINAL:
					
					String path = "c:\\";
					Runtime.getRuntime().exec(new String[] { "cmd.exe", "/C", "\"start; cd "+path+"\"" });
					
					Runtime.getRuntime().exec("/bin/bash -c Your Command");
					
					*/
					//x.waitFor();
					//String jvm = new java.io.File(new java.io.File(System.getProperty("java.home"), "bin"), "java").getAbsolutePath();
				}
				//Spawn server
				else if (args.length == 2){
					Communicator.startup(5300);
					String[] addr = args[1].split(":");
					if (addr.length != 2)
						throw syntax;
					try{
						//Validate arguments
						int port = Integer.parseInt(addr[1]);
						if (port < RemoteAddress.MIN_PORT || port > RemoteAddress.MAX_PORT)
							throw new NumberFormatException();
						//Create a new remote address
						RemoteAddress spawn = new RemoteAddress(addr[0], port, 0);
						Command handshake = new Command(Communicator.className, "handshake");
						Object result = Communicator.request(spawn, handshake, true);
						if (!(result instanceof Boolean))
							throw new Exception("Cannot connect to spawning server!");
						//TODO here
						System.out.println("Connection successful; This next part isn't implemented");
						System.out.println("TODO, move this stuff to the ChatServer class");
					} catch (NumberFormatException e){
						System.err.println("Invalid port number!");
					}
				}
				else throw syntax;
			}
			else throw syntax;
		} catch (Exception e){
			System.err.println(e.getMessage());
		}
	}
}
