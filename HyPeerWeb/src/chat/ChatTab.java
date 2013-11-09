package chat;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/**
 * Handles all chatting
 * @author isaac
 */
public class ChatTab extends JPanel{
	//Active chat users and their associated colors
	private final HashMap<Integer, ChatUser> chatUsers = new HashMap();
	private final DefaultComboBoxModel<ChatUser> chatUsersLst = new DefaultComboBoxModel();
	//The last user that chatted something
	//If they send another message, we don't want to display their name twice
	private ChatUser lastUserTo, lastUserFrom;
	
	//Notify listeners of new message commands
	private final ArrayList<SendListener> listeners = new ArrayList();
	
	//Editing the chat log display
	private final HTMLDocument document;
	private final HTMLEditorKit editor;
	private final Element cursor;
	private boolean needBreak = false;
	
	//Different layouts depending on amount of users
	private static final String EMPTY = "Empty", FILLED = "Filled";
	
	public ChatTab(Border padding, String title){
		//The chat log is on top, the tools are on the bottom
		JScrollPane chatLogScroll = new JScrollPane(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
		);
		JTextPane chatLog = new JTextPane();
		chatLog.setEditable(false);
		chatLogScroll.setViewportView(chatLog);
		
		//The chat log uses an HTML renderer to display its stuff
		//Formatting is stored in styles.css and templates.html
		StyleSheet styles = new StyleSheet();
		styles.importStyleSheet(ChatTab.class.getResource("styles.css"));
		editor = new HTMLEditorKit();
		editor.setStyleSheet(styles);
		document = (HTMLDocument) editor.createDefaultDocument();
		chatLog.setEditorKit(editor);
		chatLog.setDocument(document);
		//Set the cursor to be the HTML body element
		cursor = document.getDefaultRootElement().getElement(0);
		
		//Welcome message
		String welcome = "Welcome to "+title+"!<br/>Join a network or create a new one from the right.";
		writeHTML(Tag.P, "title", welcome, null, null);
		//This is a bit hacky; but, I can't figure out a better way to get
		//the HTMLDocument to format correctly; needBreak will force the cursor
		//to drop out of the "welcome message" p-tag
		needBreak = true;
		writeTag(Tag.P);

		//Chat text box
		JScrollPane chatBoxScroll = new JScrollPane(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
		);
		chatBoxScroll.setPreferredSize(new Dimension(1, 60));
		JTextArea chatBox = new JTextArea();
		chatBox.setBorder(padding);
		chatBoxScroll.setViewportView(chatBox);
		
		//Action box
		JPanel btns = new JPanel(new CardLayout(5, 0));
		JLabel lblEmpty = new JLabel("The chatroom is empty. Go find some friends.");
		lblEmpty.setFont(new Font("SansSerif", Font.BOLD, 12));
		btns.add(lblEmpty, EMPTY);
		//Send a public message
		JButton btnSendPublic = new JButton("Send Public");
		btnSendPublic.setToolTipText("Broadcast message (Enter)");
		btns.add(btnSendPublic, FILLED);
		//Private message sending auto-hides based on the # of users
		final JButton btnSendPrivate = new JButton("Send Private");
		btnSendPrivate.setToolTipText("Send message (Ctrl+Enter)");
		btns.add(btnSendPrivate, FILLED);
		final JComboBox userList = new JComboBox();
		userList.setModel(chatUsersLst);
		btns.add(userList, FILLED);
		chatUsersLst.addListDataListener(new ListDataListener(){
			@Override
			public void intervalAdded(ListDataEvent e) {
				if (chatUsersLst.getSize() == 1){
					userList.setVisible(true);
					btnSendPrivate.setVisible(true);
					System.out.println("YES, this happens");
				}
			}
			@Override
			public void intervalRemoved(ListDataEvent e) {
				if (chatUsersLst.getSize() == 0){
					userList.setVisible(false);
					btnSendPrivate.setVisible(false);
					System.out.println("YES, this happens");
				}
			}
			@Override
			public void contentsChanged(ListDataEvent e){}
		});
		
		//Layout all the components in this tab
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		Insets pad = new Insets(8, 8, 8, 8);
		c.fill = GridBagConstraints.BOTH;
		c.insets = pad;
		c.weightx = 1;
		c.weighty = 1;
		c.gridy = 0;
		add(chatLogScroll, c);
		pad.top = 0;
		c.weighty = 0;
		c.gridy = 1;
		add(chatBoxScroll, c);
		c.gridy = 2;
		pad.top = 0;
		add(btns, c);
	}
	
	//CHAT ACTIONS
	/**
	 * Updates a user in the chatroom
	 * @param userid the ID of the user we want to update
	 * @param username if null, it will remove the user; if the
	 * user has already been added, it updates their alias; otherwise,
	 * it adds them as a new user in the chatroom
	 */
	public void updateUser(int userid, String username){
		ChatUser cu = chatUsers.get(userid);
		//Create a new user
		if (cu == null){
			cu = new ChatUser(username);
			chatUsers.put(userid, cu);
			chatUsersLst.addElement(cu);
			writeStatus("<b>"+cu.name+"</b> is online");
		}
		//Remove a user
		else if (username == null){
			writeStatus("<b>"+cu.name+"</b> is offline");
			chatUsers.remove(userid);
			chatUsersLst.removeElement(cu);
		}
		//Update existing username
		else if (!cu.name.equals(username)){
			writeStatus("<b>"+cu.name+"</b> is now known as <b>"+username+"</b>");
			cu.name = username;
			//Notify the user list
			int index = chatUsersLst.getIndexOf(cu);
			ListDataEvent updateEvt = new ListDataEvent(
				chatUsersLst, ListDataEvent.CONTENTS_CHANGED, index, index
			);
			for (ListDataListener dl: chatUsersLst.getListDataListeners())
				dl.contentsChanged(updateEvt);
		}
	}
	/**
	 * Notifies the chat tab that a message has been received
	 * @param userID who sent the message
	 * @param recipientID who should receive the message (-1 if it was sent to everyone)
	 * @param message the message
	 */
	public void receiveMessage(int userID, int recipientID, String message){
		writeMessage(
			chatUsers.get(userID),
			recipientID == -1 ? null : chatUsers.get(recipientID),
			message
		);
	}
	/**
	 * Register yourself as wanting sendMessage notifications
	 * @param listener the sendMessage callback
	 */
	public void addSendListener(SendListener listener){
		listeners.add(listener);
	}
	/**
	 * Notify all listeners of a message should be sent
	 * @param userID the sender's id
	 * @param recipientID the recipient's id (-1 to send to everyone)
	 * @param message the message to send
	 */
	private void sendMessage(int userID, int recipientID, String message){
		receiveMessage(userID, recipientID, message);
		for (SendListener l: listeners)
			l.callback(userID, recipientID, message);
	}
	
	//CHAT HTML MACROS
	/**
	 * Write a single chat message's HTML
	 * @param userFrom who sent the message
	 * @param userTo who is it to (or null)
	 * @param message the message they sent
	 */
	private void writeMessage(ChatUser userFrom, ChatUser userTo, String message){
		//We don't know who sent this message; they never sent an updateUser command
		if (userFrom == null)
			userFrom = new ChatUser("anonymous");
		//This person was the last one to chat something
		if (userFrom == lastUserFrom && userTo == lastUserTo)
			writePlain(message+"<br/>");
		else{
			writeUser(userFrom);
			if (userTo != null){
				writePlain("&nbsp;&raquo;&nbsp;");
				writeUser(userTo);
			}
			writePlain(": "+message+"<br/>");
			lastUserFrom = userFrom;
			lastUserTo = userTo;
		}
	}
	/**
	 * Write HTML for displaying a user's name
	 * @param user the ChatUser for the user to display
	 */
	private void writeUser(ChatUser user){
		writeHTML(Tag.FONT, "user", user.name, new String[]{"color"}, new String[]{user.color});
	}
	/**
	 * Write generic status information
	 * @param message status message
	 */
	private void writeStatus(String message){
		writeHTML(Tag.FONT, "status", message+"<br/>", null, null);
		lastUserFrom = null;
	}
	
	//HTML EDITING
	/**
	 * Write a single empty HTML tag
	 * @param tag the HTML tag
	 */
	private void writeTag(Tag tag){
		writeHTML(Tag.P, null, null, null, null);
	}
	/**
	 * Write plain, unformatted text
	 * @param plain the text to write
	 */
	private void writePlain(String plain){
		writeHTML(Tag.SPAN, null, plain, null, null);
	}
	/**
	 * Write an HTML element
	 * @param tag the HTML element tag name
	 * @param className optional class name
	 * @param html optional content
	 * @param styles optional style definitions
	 * @param styleVals optional style values
	 */
	private void writeHTML(Tag tag, String className, String html, String styles[], String styleVals[]){
		try {
			String data = createHTMLElement(tag, className, html, styles, styleVals);
			if (document.getLength() == 0)
				document.insertAfterStart(cursor, data);
			else{
				//System.out.println(document.getLength());
				editor.insertHTML(document, document.getLength(), data, needBreak ? 1 : 0, 0, tag);
				needBreak = false;
			}
		} catch (BadLocationException | IOException ex) {
			System.out.println("Chat HTML document is corrupt");
		}
	}
	/**
	 * Create an HTML element string
	 * @param tag tag name
	 * @param className optional class name
	 * @param content optional innerHTML
	 * @param styles optional style names
	 * @param styleVals optional style values for each style name
	 * @return a valid HTML string for this element
	 */
	private String createHTMLElement(Tag tag, String className, String content, String styles[], String styleVals[]){
		assert((styles == null || styles.length == styleVals.length) && tag != null);
		StringBuilder html = new StringBuilder();
		//Openingtag
		String tagName = tag.toString();
		html.append("<").append(tagName);
		//Classname
		if (className != null)
			html.append(" class='").append(className).append("'");
		//Styles
		if (styles != null && styles.length > 0){
			html.append(" style='");
			for (int i=0; i<styles.length; i++)
				html.append(styles[i]).append(":").append(styleVals[i]).append(";");
			html.append("'");
		}
		html.append(">");
		if (content != null)
			html.append(content);
		//Closing tag
		html.append("</").append(tagName).append(">");
		return html.toString();
	}
	/**
	 * Dump the current HTML document to a string
	 * Intended for debugging purposes
	 * @return a formatted HTML document string
	 */
	private String dumpHTML(){
		StringWriter writer = new StringWriter();
		try {
			editor.write(writer, document, 0, document.getLength());
		} catch (IOException | BadLocationException ex) {
			System.out.println("Chat HTML document is corrupt");
		}
		String s = writer.toString();
		return s;
	}
	
	//CHAT USERS
	private static class ChatUser{
		//Random color generator
		private static final Random rand = new Random();
		private static final int minRGB = 30, maxRGB = 200;
		//User attributes
		public String color, name;
		
		/**
		 * Create a new chat user
		 * @param name the user's name
		 */
		public ChatUser(String name){
			//Random username color
			//RGB values between 100-250
			int delta = maxRGB-minRGB;
			color = String.format(
				"#%02x%02x%02x",
				rand.nextInt(delta)+minRGB,
				rand.nextInt(delta)+minRGB,
				rand.nextInt(delta)+minRGB
			);
			this.name = name;
		}
		@Override
		public String toString(){
			return name;
		}
	}
	
	//SEND NOTIFICATION HANDLERS
	public static abstract class SendListener{
		abstract void callback(int senderID, int recipientID, String message);
	}
}
