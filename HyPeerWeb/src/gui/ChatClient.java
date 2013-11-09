package gui;

import gui.chat.ChatTab;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * The Graphical User Interface for the Chat Client
 * TODO:
 *	- listener on close to clean-up, delete InceptionSegment, etc.
 * 
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
		
		//Divide the window into an upper/lower bar
		//Upper will be for graphing and such; Lower will be a debug console
		JSplitPane vSplit = new JSplitPane();
		vSplit.setDividerLocation(vsplitWeight);
		vSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
		vSplit.setResizeWeight(vsplitWeight);
		add(vSplit);
		
		//Bottom half will be a debug console
		JScrollPane debugScroll = new JScrollPane(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
		);
		JTextPane debugConsole = new JTextPane();
		//debugConsole.setEditable(false);
		debugConsole.setText("Debugging Console...");
		debugScroll.setViewportView(debugConsole);
		vSplit.setBottomComponent(debugScroll);
		
		//Divide the upper bar of the window into two halves
		//Left half will have node lists and the chat box; Right will be for actions
		JPanel hSplit = new JPanel();
		hSplit.setLayout(new BorderLayout());
		vSplit.setTopComponent(hSplit);
		
		//Right half will be an actions bar
		JPanel actionBar = new JPanel();
		actionBar.setLayout(new BoxLayout(actionBar, BoxLayout.Y_AXIS));
		actionBar.add(new JButton("Add Node"));
		actionBar.add(new JButton("Delete Node"));
		actionBar.add(new JButton("New Network"));
		actionBar.add(new JButton("Join Network"));
		actionBar.add(new JButton("View Network"));
		hSplit.add(actionBar, BorderLayout.EAST);
		
		//Left half will be a tabbed pane for graphing/chatting
		JTabbedPane tabs = new JTabbedPane();
		hSplit.add(tabs, BorderLayout.CENTER);
		tabs.addTab("Chat", new ChatTab(padding, title));
		//tabs.addTab("Node Graph", initGraphTab());
		//tabs.addTab("Node List", initListTab());
	}
	//</editor-fold>
	
	//ACTIONS

	
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
}
