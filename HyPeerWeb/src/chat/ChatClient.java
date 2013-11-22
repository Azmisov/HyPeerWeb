package chat;

import chat.ChatServer.*;
import com.alee.laf.WebLookAndFeel;
import hypeerweb.NodeCache.Node;
import hypeerweb.NodeCache;
import java.awt.*;
import java.awt.event.*;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

/**
 * The Graphical User Interface for the Chat Client
 * TODO:
 *	- listener on close to clean-up, delete InceptionSegment, etc.
 *  - add buttons for actions sidebar (e.g. addNode, disconnect, deleteNode, etc)
 *  - write code for GraphTab/ListTab
 * @author isaac
 */
public class ChatClient extends JFrame{
	//Window title
	private static final String title = "HyPeerWeb Chat v0.2a";
	//Window dimensions
	private static final int width = 750, height = 700;
	//Action bar's pixel width
	private static final int actionBarWidth = 150;
	//Main pane (graph, list, chat) padding
	private static final int pad = 8;
	private static final EmptyBorder padding = new EmptyBorder(pad, pad, pad, pad);
	private static final Font bold = new Font("SansSerif", Font.BOLD, 12);
	
	//Data items
	private ChatServer server;							//Server reference
	protected HashMap<Integer, ChatUser> chatUsers;		//List of all chat users
	protected ChatUser activeUser;						//The user associated with this client
	protected NodeCache nodeCache = null;				//List of all nodes in HyPeerWeb
	private Node selected;								//The selected node
	private String subnetName;
	
	//GUI components
	private final ChatTab chat = new ChatTab(this, padding, title);
	private final GraphTab graph = new GraphTab(this);
	private final JSpinner nodeSelect = new JSpinner(new SpinnerNumberModel(-1, -1, null, 1));
	private final NodeInfo nodeInfo = new NodeInfo();
	private final JTable connectList = new JTable(nodeInfo);
	private final ListTab listTab = new ListTab(this);
	private final JTextField txtSubnetName = new JTextField();
	private ArrayList<JPanel> boxes;
		
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
		hSplit.add(initActionBar(), BorderLayout.EAST);
		
		//Left half will be a tabbed pane for graphing/chatting
		JTabbedPane tabs = new JTabbedPane();
		hSplit.add(tabs, BorderLayout.CENTER);
		tabs.addTab("Chat", chat);
		tabs.addTab("Node Graph", graph);
		tabs.addTab("Node List", listTab);
	}
	public JPanel initActionBar(){
		JPanel bar = new JPanel();
		boxes = new ArrayList(){{
			add(initNetworkBox());
			add(initConnectionBox());
			add(initNodeBox());
		}};
		setConnected(false);
		
		// <editor-fold defaultstate="collapsed" desc="Layout components in a stack">
		GroupLayout stack = new GroupLayout(bar);
		bar.setLayout(stack);
		ParallelGroup hgroup = stack.createParallelGroup();
		SequentialGroup vgroup = stack.createSequentialGroup();
		Iterator<JPanel> it = boxes.iterator();
		int i=0;
		while (it.hasNext()){
			JPanel box = it.next();
			box.setBorder(padding);
			hgroup.addComponent(box);
			vgroup.addComponent(box);
			if (i++ == 1)
				vgroup.addGap(50);
		}
		vgroup.addContainerGap(1000, Short.MAX_VALUE);
		stack.setHorizontalGroup(hgroup);
        stack.setVerticalGroup(stack.createParallelGroup(GroupLayout.Alignment.CENTER).addGroup(vgroup));
		// </editor-fold>
		
		return bar;
	}
	public JPanel initNetworkBox(){
		//Create a network
		JButton btnCreate = new JButton("Create Network");
		
		//Connect to a network
		JButton btnJoin = new JButton("Join");
		btnJoin.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				setConnected(true);
			}
		});
		JButton btnWatch = new JButton("Watch");
		
		//Network connection configuration
		final String t1 = "IP Address", t2 = "Port #";
		final JTextField txtIP = new JTextField(t1);
		final JTextField txtPort = new JTextField(t2);
		smartTextField(t1, txtIP);
		smartTextField(t2, txtPort);
		CompoundBorder txtPad = new CompoundBorder(
			txtIP.getBorder(),
			(new EmptyBorder(2, 2, 2, 2))
		);
		txtIP.setBorder(txtPad);
		txtPort.setBorder(txtPad);
		
		// <editor-fold defaultstate="collapsed" desc="Layout components in grid">
		JPanel box = new JPanel();
		box.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = 3;		c.ipady = 3;
		c.gridwidth = 2;	c.gridheight = 1;
		c.gridx = 0;		c.gridy = 0;
		c.weightx = .5;		c.weightx = .5;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 0, 4, 0);
		box.add(btnCreate, c);
		c.gridy++;
		c.gridwidth = 1;
		c.insets.right = 4;
		box.add(btnJoin, c);
		c.gridx = 1;
		c.insets.right = 0;
		box.add(btnWatch, c);
		c.gridwidth = 2;
		c.gridy++;
		c.gridx = 0;
		c.insets.top = 2;
		box.add(txtIP, c);
		c.gridy++;
		c.insets.top = 0;
		box.add(txtPort, c);
		c.gridy++;
		c.insets.top = 20;
		// </editor-fold>
		
		return box;
	}
	public JPanel initConnectionBox(){		
		//Segment name
		JLabel lblSeg = new JLabel("Subnet Name:");
		lblSeg.setFont(bold);
		txtSubnetName.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				String newName = txtSubnetName.getText();
				if (server != null && !newName.equals(subnetName))
					server.changeNetworkName(newName);
			}
		});
		
		//Username
		JLabel lblName = new JLabel("Chat Alias:");
		lblName.setFont(bold);
		JTextField txtName = new JTextField();
		
		//Disconnect button
		JButton btnDisconnect = new JButton("Disconnect");
		btnDisconnect.setPreferredSize(new Dimension(150, 25));
		
		//Shutdown button
		JButton btnShutdown = new JButton("Shutdown");
		
		// <editor-fold defaultstate="collapsed" desc="Layout components in grid">
		JPanel box = new JPanel();
		box.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = 3;		c.ipady = 3;
		c.gridwidth = 2;	c.gridheight = 1;
		c.gridx = 0;		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 0, 2, 0);
		box.add(lblSeg, c);
		c.gridy++;
		box.add(txtSubnetName, c);
		c.gridy++;
		box.add(lblName, c);
		c.gridy++;
		c.insets.bottom = 4;
		box.add(txtName, c);
		c.gridy++;
		box.add(btnDisconnect, c);
		c.gridy++;
		box.add(btnShutdown, c);
		// </editor-fold>
		
		return box;
	}
	public JPanel initNodeBox(){
		//Initialize elements
		JButton addNode = new JButton("Add");
		JButton deleteNode = new JButton("Delete");
		final JSpinner addCount = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
		connectList.setFocusable(false);
		connectList.setRowSelectionAllowed(false);
		final JLabel L = new JLabel("Selected:");
		L.setFont(bold);
		
		
		addNode.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				server.addNode();
			}
		});
		deleteNode.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				if (selected != null)
					server.removeNode(selected.getWebId());
			}
		});
		
		
		// <editor-fold defaultstate="collapsed" desc="Layout components in grid">
		JPanel box = new JPanel();
		box.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = 3;		c.ipady = 3;
		c.gridwidth = 1;	c.gridheight = 1;
		c.gridx = 0;		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		box.add(L, c);
		c.gridx++;
		c.gridwidth = 1;
		box.add(nodeSelect, c);
		c.gridwidth = 2;
		c.gridx = 0;
		c.weightx = 1;
		c.gridy++;
		c.insets = new Insets(6, 0, 6, 0);
		box.add(connectList, c);
		c.insets.set(0, 0, 0, 0);
		c.weightx = 0;
		c.weighty = 0;
		c.gridy++;
		c.gridx = 0;
		box.add(deleteNode, c);
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = .3;
		box.add(addNode, c);
		c.gridx++;
		c.weightx = .7;
		box.add(addCount, c);
		// </editor-fold>
		
		return box;
	}
	private void smartTextField(final String defVal, final JTextField txt){
		txt.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {
				if (txt.getText().equals(defVal))
					txt.setText(null);
			}
			@Override
			public void focusLost(FocusEvent e) {
				if (txt.getText().isEmpty())
					txt.setText(defVal);
			}
		});
	}
	
	public static void main(String args[]) {
		//Load the look-and-feel
		/*
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
			System.out.println("Could not load look-and-feel to start the GUI");
		}
		//*/
		WebLookAndFeel.install();

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
	protected void setConnected(boolean connected){
		boxes.get(0).setVisible(!connected);
		boxes.get(1).setVisible(connected);
		boxes.get(2).setVisible(connected);
	}
	protected void sendMessage(int userID, int recipientID, String message){
		if (server != null)
			server.sendMessage(userID, recipientID, message);
	}
	protected void setSelectedNode(Node n){
		selected = n;
		nodeSelect.setValue(n == null ? -1 : n.getWebId());
		nodeInfo.updateInfo(n);
	}
	
	//LISTENERS
	public void updateNetworkName(String newName){
		txtSubnetName.setText(newName);
		subnetName = newName;
	}
	public void updateUser(int userid, String username, int networkid){
		chat.updateUser(userid, username, networkid);
	}
	public void updateNodeCache(Node affectedNode, NodeCache.SyncType type, Node[] updatedNodes){
		
	}
	public void receiveMessage(int senderID, int recipientID, String mess){
		chat.receiveMessage(senderID, recipientID, mess);
	}
	
	private class NodeInfo extends AbstractTableModel{
		ArrayList<String[]> data = new ArrayList();
		public void updateInfo(Node n){
			data.clear();
			if (n != null){
				Node temp;
				if ((temp = n.getFold()) != null)
					addInfo("F:",temp);
				if ((temp = n.getSFold()) != null)
					addInfo("SF:",temp);
				if ((temp = n.getISFold()) != null)
					addInfo("ISF:",temp);
				Node[] temp2;
				if ((temp2 = n.getNeighbors()).length > 0)
					addInfo("Ns:",temp2);
				if ((temp2 = n.getSNeighbors()).length > 0)
					addInfo("SNs:",temp2);
				if ((temp2 = n.getISNeighbors()).length > 0)
					addInfo("ISNs:",temp2);
			}
			fireTableStructureChanged();
			connectList.getColumnModel().getColumn(0).setPreferredWidth(12);
		}
		private void addInfo(String name, Node n){
			data.add(new String[]{boldify(name), String.valueOf(n.getWebId())});
		}
		private void addInfo(String name, Node[] n){
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<n.length; i++){
				sb.append(n[i].getWebId());
				if (i+1 != n.length)
					sb.append(",");
			}
			data.add(new String[]{boldify(name), sb.toString()});
		}
		private String boldify(String name){
			return "<html><b>"+name;
		}
		@Override
		public int getRowCount() {
			return data.isEmpty() ? 1 : data.size();
		}
		@Override
		public int getColumnCount() {
			return data.isEmpty() ? 1 : 2;
		}
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (data.isEmpty())
				return boldify("No node selected");
			return data.get(rowIndex)[columnIndex];
		}
	}
}
