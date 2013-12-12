package chat.server;

import communicator.Communicator;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

/**
 * GUI to display console messages from server
 * Create GUI after starting the ChatServer
 * @author isaac
 */
public class ChatServerGUI extends JFrame{
	public ChatServerGUI(){
		setTitle("HyPeerWeb Chat Server: "+Communicator.getAddress().port);
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
		
		System.out.println("Server is listening on "+Communicator.getAddress());
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				ChatServer.disconnect();
			}
		});
	}
}
