package chat;

import hypeerweb.HyPeerWebSegment;
import java.awt.*;
import static java.lang.Thread.sleep;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/**
 * The Graphical User Interface for the Chat Client
 * TODO:
 *	- listener on close to clean-up, delete InceptionSegment, etc.
 *  - add buttons for actions sidebar (e.g. addNode, disconnect, deleteNode, etc)
 *  - 
 *  - write code for GraphTab/ListTab
 * @author isaac
 */
public class ChatClient extends JFrame{
	//Window title
	private final String title = "HyPeerWeb Chat v0.1a";
	//Window dimensions
	private final int width = 750, height = 700;
	//Upper vertical split percentage
	private final double vsplitWeight = 0.8;
	//Action bar's pixel width
	private final int actionBarWidth = 150;
	//Main pane (graph, list, chat) padding
	private final int pad = 8;
	private final EmptyBorder padding = new EmptyBorder(pad, pad, pad, pad);
		
	public ChatClient(){
		initGUI();
	}
	
	// <editor-fold defaultstate="collapsed" desc="GUI INITIALIZATION">
	private void initGUI(){
		//Initialize the window
		setTitle(title);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(width, height);
		setLocationRelativeTo(null);
		
		//Divide the upper bar of the window into two halves
		//Left half will have node lists and the chat box; Right will be for actions
		JPanel hSplit = new JPanel();
		hSplit.setLayout(new BorderLayout());
		add(hSplit);
		
		//Right half will be an actions bar
		JPanel actionBar = new JPanel();
		actionBar.setBorder(padding);
		actionBar.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = 3;		c.ipady = 3;
		c.gridwidth = 2;	c.gridheight = 1;
		c.gridx = 0;		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0, 0, 4, 0);
		JButton btnCreate = new JButton("Create Network");
		actionBar.add(btnCreate, c);
		c.gridy++;
		c.gridwidth = 1;
		c.insets.right = 4;
		actionBar.add(new JButton("Join"), c);
		c.gridx = 1;
		c.insets.right = 0;
		actionBar.add(new JButton("Watch"), c);
		//Network connection settings
		JTextField txtIP = new JTextField("IP Address");
		JTextField txtPort = new JTextField("Port #");
		CompoundBorder txtPad = new CompoundBorder(
			txtIP.getBorder(),
			(new EmptyBorder(2, 2, 2, 2))
		);
		txtIP.setBorder(txtPad);
		txtPort.setBorder(txtPad);
		c.gridwidth = 2;
		c.gridy++;
		c.gridx = 0;
		c.insets.top = 2;
		actionBar.add(txtIP, c);
		c.gridy++;
		c.insets.top = 0;
		actionBar.add(txtPort, c);
		
		hSplit.add(actionBar, BorderLayout.EAST);
		
		//Left half will be a tabbed pane for graphing/chatting		
		GraphTab x = new GraphTab();
		JTabbedPane tabs = new JTabbedPane();
		hSplit.add(tabs, BorderLayout.CENTER);
		tabs.addTab("Chat", new ChatTab(padding, title));
		tabs.addTab("Node Graph", x);
		tabs.addTab("Node List", new ListTab());
		try {
			HyPeerWebSegment web = new HyPeerWebSegment(null, -1);
			for (int i=0; i<100; i++)
				web.addNode();
			//x.draw(web.getFirstNode(), height);
		} catch (Exception ex) {
			System.out.println("Cannot hypeerweb stuff");
		}
	}
	public static void main(String args[]) {
		//Load the look-and-feel
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
			System.out.println("Could not load look-and-feel to start the GUI");
		}

		//Start up the window
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				new ChatClient().setVisible(true);
			}
		});
	}
	//</editor-fold>
	
	//ACTIONS
	private void testChatRoom(){
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					sleep(500);
					ChatTab.ChatUser isaac = userUpdate(0, "user91023");
					sleep(200);
					writeMessage(isaac, null, "Hello world");
					sleep(200);
					userUpdate(0, "isaac");
					sleep(200);
					writeMessage(isaac, null, "blue babies");
					sleep(200);
					writeMessage(isaac, null, "Someone join me.... I'm getting bored");
					sleep(200);
					ChatTab.ChatUser john = userUpdate(1, "John");
					sleep(200);
					writeMessage(isaac, john, "Dude what is up");
					sleep(200);
					writeMessage(john, isaac, "Hey, I like your style bro");
					sleep(200);
					userUpdate(0, null);
					sleep(200);
					userUpdate(1, null);
				} catch (InterruptedException ex) {
					Logger.getLogger(ChatTab.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});
	}
	
}
