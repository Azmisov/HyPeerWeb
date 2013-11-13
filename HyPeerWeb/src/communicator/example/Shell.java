package communicator.example;

import java.io.IOException;

/**
 * Not really needed for the example but if you need to execute from within java a command on the command line of
 * another machine, this shows you how to do it.  You will have to set up ssh so no password is needed.  See
 * http://www.csua.berkeley.edu/~ranga/notes/ssh_nopass.html
 * @author Scott
 *
 */
public class Shell {
	public void executeCommand(String command){
		try {
			Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Shell(){}
	
	public static void main(String[] args){
		System.out.println("Begin");
		Shell shell = new Shell();
		shell.executeCommand("ssh woodfiel@woodfiel.cs.byu.edu ls > t");
		System.out.println("End");
	}
}