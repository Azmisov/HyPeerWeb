package chat.client;

import chat.server.ChatServer;
import chat.server.ChatServer.ChatUser;
import communicator.*;
import hypeerweb.NodeCache;
import hypeerweb.NodeCache.Node;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
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
	//Serialization
	public static final String className = ChatClient.class.getName();
	public static final int UID = Communicator.assignId();
	//Window title
	private static final String title = "HyPeerWeb Chat v0.3a";
	//Window dimensions
	private static final int width = 750, height = 700;
	//Action bar's pixel width
	private static final int actionBarWidth = 150;
	//Main pane (graph, list, chat) padding
	private static final int pad = 8;
	private static final EmptyBorder padding = new EmptyBorder(pad, pad, pad, pad);
	private static final Font bold = new Font("SansSerif", Font.BOLD, 12);
	
	//Data items
	private static RemoteAddress server;
	protected static ChatClient instance;
	protected static ChatUser activeUser;						//The user associated with this client
	protected static NodeCache nodeCache = null;				//List of all nodes in HyPeerWeb
	private static Node selected;								//The selected node
	private static String subnetName;
	//List of all chat users
	protected static HashMap<Integer, ChatUser> chatUsers = new HashMap();
	
	//GUI components
	private static final ChatTab chat = new ChatTab(padding, title);
	private static final GraphTab graph = new GraphTab();
	private static final JSpinner nodeSelect = new JSpinner(new SpinnerNumberModel(-1, -1, null, 1));
	private static final NodeInfo nodeInfo = new NodeInfo();
	private static final JTable connectList = new JTable(nodeInfo);
	private static final ListTab listTab = new ListTab();
	private static final JTextField
		txtSubnetName = new JTextField(),
		txtChatAlias = new JTextField(),
		txtIP = new JTextField("Address"),
		txtPort = new JTextField("Port");
	private static ArrayList<JPanel> boxes;
		
	private ChatClient(){
		initGUI();
		//Startup a communicator, to receive server events
		Communicator.startup(0);
	}
	public static ChatClient getInstance(){
		if (instance == null)
			instance = new ChatClient();
		return instance;
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
	private JPanel initActionBar(){
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
	private JPanel initNetworkBox(){
		//Create a network
		JButton btnCreate = new JButton("Create Network");
		btnCreate.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				ChatServer.startServerProcess(null, Communicator.getAddress());
			}
		});
		btnCreate.setPreferredSize(new Dimension(actionBarWidth, 25));
		
		//Connect to a network
		JButton btnSpawn = new JButton("Spawn");
		btnSpawn.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				RemoteAddress addr = parseGUIAddress();
				//Make sure spawning address exists
				if (addr != null){
					if (!Communicator.handshake(ChatServer.className, addr)){
						showPopup(
							JOptionPane.ERROR_MESSAGE,
							"This remote address does not refer to a chat server!"
						);
					}
					else ChatServer.startServerProcess(addr, Communicator.getAddress());
				}
			}
		});
		JButton btnLeech = new JButton("Leech");
		btnLeech.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				RemoteAddress addr = parseGUIAddress();
				//Make sure leeching address exists
				if (addr != null){
					if (!Communicator.handshake(ChatServer.className, addr)){
						showPopup(
							JOptionPane.ERROR_MESSAGE,
							"This remote address does not refer to a chat server!"
						);
					}
					//Register with this server
					else{
						Command register = new Command(
							ChatServer.className, "registerClient",
							new String[]{RemoteAddress.className},
							new Object[]{Communicator.getAddress()}
						);
						Communicator.request(addr, register, false);
					}
				}
			}
		});
		
		//Network connection configuration
		smartTextField(txtIP);
		smartTextField(txtPort);
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
		box.add(btnSpawn, c);
		c.gridx = 1;
		c.insets.right = 0;
		box.add(btnLeech, c);
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
	private JPanel initConnectionBox(){		
		//Segment name
		JLabel lblSeg = new JLabel("Subnet Name:");
		lblSeg.setFont(bold);
		txtSubnetName.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				String newName = txtSubnetName.getText();
				if (isConnected() && !newName.equals(subnetName)){
					Command namer = new Command(
						ChatServer.className, "changeNetworkName",
						new String[]{"java.lang.String"}, new Object[]{newName}
					);
					Communicator.request(server, namer, false);
				}
			}
		});
		
		//Username
		JLabel lblName = new JLabel("Chat Alias:");
		lblName.setFont(bold);
		txtChatAlias.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				String newAlias = txtChatAlias.getText();
				//Prevent empty names
				if (newAlias.length() == 0)
					txtChatAlias.setText(activeUser.name);
				//Otherwise, broadcast the name change to the server
				else if (isConnected() && !newAlias.equals(activeUser.name)){
					Command namer = new Command(
						ChatServer.className, "updateUser",
						new String[]{"int", "java.lang.String", "int"},
						new Object[]{activeUser.id, newAlias, activeUser.networkID}
					);
					Communicator.request(server, namer, false);
				}
			}
		});
		
		
		//Disconnect button
		JButton btnDisconnect = new JButton("Disconnect");
		btnDisconnect.setPreferredSize(new Dimension(actionBarWidth, 25));
		
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
		box.add(txtChatAlias, c);
		c.gridy++;
		box.add(btnDisconnect, c);
		c.gridy++;
		box.add(btnShutdown, c);
		// </editor-fold>
		
		return box;
	}
	private JPanel initNodeBox(){
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
				if (isConnected()){
					Command adder = new Command(ChatServer.className, "addNode");
					Communicator.request(server, adder, false);
				}
			}
		});
		deleteNode.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				if (isConnected() && selected != null){
					Command deleter = new Command(
					ChatServer.className, "removeNode",
						new String[]{"int"},
						new Object[]{selected.getWebId()}
					);
					Communicator.request(server, deleter, false);
				}
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
	//Utility methods
	private RemoteAddress parseGUIAddress(){
		String ip = txtIP.getText(), port = txtPort.getText();
		try{
			int portInt = Integer.parseInt(port);
			RemoteAddress addr = new RemoteAddress(ip, portInt, 0);
			return addr;
		} catch (NumberFormatException e){
			showPopup(JOptionPane.ERROR_MESSAGE, "Invalid port number");
		} catch (Exception e){
			showPopup(JOptionPane.ERROR_MESSAGE, e.getMessage());
		}
		return null;
	}
	private void smartTextField(final JTextField txt){
		final String defVal = txt.getText();
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
	private void showPopup(int popupType, String message){
		JOptionPane.showMessageDialog(
			instance, message, popupType == JOptionPane.ERROR_MESSAGE ? "Error" : "Notification", popupType
		);
	}
	//</editor-fold>
	
	//ACTIONS
	protected static boolean isConnected(){
		return nodeCache != null;
	}
	protected static void sendMessage(int userID, int recipientID, String message){
		if (isConnected()){
			Command sender = new Command(
				ChatServer.className, "sendMessage",
				new String[]{"int", "int", "java.lang.String"},
				new Object[]{userID, recipientID, message}
			);
			Communicator.request(server, sender, false);
		}
	}
	protected static void setSelectedNode(Node n){
		selected = n;
		nodeSelect.setValue(n == null ? -1 : n.getWebId());
		nodeInfo.updateInfo(n);
	}
	private static void setConnected(boolean connected){
		boxes.get(0).setVisible(!connected);
		boxes.get(1).setVisible(connected);
		boxes.get(2).setVisible(connected);
		if (!connected){
			nodeCache = null;
			chatUsers.clear();
			//TODO, reset other stuff here
		}
	}
	
	//LISTENERS
	public static void registerServer(RemoteAddress addr, NodeCache cache, ChatUser active, ChatUser[] users){
		server = addr;
		chatUsers.clear();
		activeUser = active;
		updateUser(active.id, active.name, active.networkID);
		for (ChatUser usr: users)
			updateUser(usr.id, usr.name, usr.networkID);
		//Update GUI components
		txtChatAlias.setText(active.name);
		setConnected(true);
		//Finally, connect to the HyPeerWeb cache
		nodeCache = cache;
	}
	public static void updateNetworkName(String newName){
		txtSubnetName.setText(newName);
		subnetName = newName;
		chat.writeStatus("Subnet renamed to <b>"+newName+"</b>");
	}
	public static void updateUser(int userid, String username, int networkid){
		chat.updateUser(userid, username, networkid);
	}
	public static void updateNodeCache(Node affectedNode, NodeCache.SyncType type, Node[] updatedNodes){
		if (nodeCache != null){
			switch (type){
				case ADD:
					nodeCache.addNode(affectedNode, false);
					break;
				case REMOVE:
					nodeCache.removeNode(affectedNode, false);
					break;
			}
			for (NodeCache.Node node : updatedNodes)
				nodeCache.addNode(node, false);
			//todo update listtab, graphtab, nodeinfo
			listTab.draw();
			
		}
	}
	public static void receiveMessage(int senderID, int recipientID, String mess){
		chat.receiveMessage(senderID, recipientID, mess);
	}
	
	private static class NodeInfo extends AbstractTableModel{
		ArrayList<String[]> data = new ArrayList();
		public void updateInfo(Node n){
			data.clear();
			if (n != null){
				Node temp;
				if ((temp = n.getFold()) != null)
					addInfo("F:",temp);
				if ((temp = n.getSurrogateFold()) != null)
					addInfo("SF:",temp);
				if ((temp = n.getInverseSurrogateFold()) != null)
					addInfo("ISF:",temp);
				Node[] temp2;
				if ((temp2 = n.getNeighbors()).length > 0)
					addInfo("Ns:",temp2);
				if ((temp2 = n.getSurrogateNeighbors()).length > 0)
					addInfo("SNs:",temp2);
				if ((temp2 = n.getInverseSurrogateNeighbors()).length > 0)
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
	
	//NETWORKING
	public static boolean handshake(){
		return instance != null;
	}
}
