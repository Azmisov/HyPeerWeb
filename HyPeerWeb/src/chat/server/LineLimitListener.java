package chat.server;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

/*
 *  A class to control the maximum number of lines to be stored in a Document
 *  Excess lines can be removed from the start of the Document
 */
public class LineLimitListener implements DocumentListener{
	private final int maximumLines;

	/*
	 *  Specify the number of lines to be stored in the Document.
	 *  Extra lines will be removed from the start or end of the Document,
	 *  depending on the boolean value specified.
	 */
	public LineLimitListener(int maximumLines){
		assert (maximumLines >= 1);
		this.maximumLines = maximumLines;
	}

	// Handle insertion of new text into the Document
	@Override
	public void insertUpdate(final DocumentEvent e){
		//  Changes to the Document can not be done within the listener
		//  so we need to add the processing to the end of the EDT
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run(){
				removeLines(e);
			}
		});
	}

	@Override
	public void removeUpdate(DocumentEvent e) {}
	@Override
	public void changedUpdate(DocumentEvent e) {}

	/*
	 *  Remove lines from the Document when necessary
	 */
	private void removeLines(DocumentEvent e){
		//  The root Element of the Document will tell us the total number
		//  of line in the Document.

		Document document = e.getDocument();
		Element root = document.getDefaultRootElement();

		while (root.getElementCount() > maximumLines){
			Element line = root.getElement(0);
			int end = line.getEndOffset();
			try{
				document.remove(0, end);
			} catch(BadLocationException ble){
				System.out.println(ble);
			}
		}
	}
}
