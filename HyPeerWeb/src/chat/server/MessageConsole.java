package chat.server;

import java.io.*;
import java.awt.*;
import javax.swing.event.*;
import javax.swing.text.*;

/*
 *  Create a simple console to display text messages.
 *
 *  Messages can be directed here from different sources. Each source can
 *  have its messages displayed in a different color.
 *
 *  Messages can either be appended to the console or inserted as the first
 *  line of the console
 *
 *  You can limit the number of lines to hold in the Document.
 */
public class MessageConsole{
	private final JTextComponent textComponent;
	private final Document document;
	private DocumentListener limitLinesListener;

	/*
	 *	Use the text component specified as a simply console to display
	 *  text messages.
	 *
	 *  The messages can either be appended to the end of the console or
	 *  inserted as the first line of the console.
	 */
	public MessageConsole(JTextComponent textComponent){
		this.textComponent = textComponent;
		this.document = textComponent.getDocument();
		textComponent.setEditable( false );
	}

	/*
	 *  Redirect the output from the standard output to the console
	 *  using the default text color and null PrintStream
	 */
	public void redirectOut(){
		redirectOut(Color.BLACK, null);
	}

	/*
	 *  Redirect the output from the standard output to the console
	 *  using the specified color and PrintStream. When a PrintStream
	 *  is specified the message will be added to the Document before
	 *  it is also written to the PrintStream.
	 */
	public void redirectOut(Color textColor, PrintStream printStream){
		ConsoleOutputStream cos = new ConsoleOutputStream(textColor, printStream);
		System.setOut( new PrintStream(cos, true) );
	}

	/*
	 *  Redirect the output from the standard error to the console
	 *  using the default text color and null PrintStream
	 */
	public void redirectErr(){
		redirectErr(Color.RED, null);
	}

	/*
	 *  Redirect the output from the standard error to the console
	 *  using the specified color and PrintStream. When a PrintStream
	 *  is specified the message will be added to the Document before
	 *  it is also written to the PrintStream.
	 */
	public void redirectErr(Color textColor, PrintStream printStream){
		ConsoleOutputStream cos = new ConsoleOutputStream(textColor, printStream);
		System.setErr( new PrintStream(cos, true) );
	}

	/*
	 *  To prevent memory from being used up you can control the number of
	 *  lines to display in the console
	 *
	 *  This number can be dynamically changed, but the console will only
	 *  be updated the next time the Document is updated.
	 */
	public void setMessageLines(int lines){
		if (limitLinesListener != null)
			document.removeDocumentListener( limitLinesListener );

		limitLinesListener = new LineLimitListener(lines);
		document.addDocumentListener( limitLinesListener );
	}

	/*
	 *	Class to intercept output from a PrintStream and add it to a Document.
	 *  The output can optionally be redirected to a different PrintStream.
	 *  The text displayed in the Document can be color coded to indicate
	 *  the output source.
	 */
	class ConsoleOutputStream extends ByteArrayOutputStream{
		private final String EOL = System.getProperty("line.separator");
		private SimpleAttributeSet attributes;
		private final PrintStream printStream;
		private final StringBuffer buffer = new StringBuffer(80);
		private boolean isFirstLine;

		/*
		 *  Specify the option text color and PrintStream
		 */
		public ConsoleOutputStream(Color textColor, PrintStream printStream){
			if (textColor != null){
				attributes = new SimpleAttributeSet();
				StyleConstants.setForeground(attributes, textColor);
			}

			this.printStream = printStream;
			isFirstLine = true;
		}

		/*
		 *  Override this method to intercept the output text. Each line of text
		 *  output will actually involve invoking this method twice:
		 *
		 *  a) for the actual text message
		 *  b) for the newLine string
		 *
		 *  The message will be treated differently depending on whether the line
		 *  will be appended or inserted into the Document
		 */
		@Override
		public void flush(){
			String message = toString();
			if (message.length() == 0)
				return;
			handleAppend(message);
			reset();
		}

		/*
		 *	We don't want to have blank lines in the Document. The first line
		 *  added will simply be the message. For additional lines it will be:
		 *
		 *  newLine + message
		 */
		private void handleAppend(String message){
			if (EOL.equals(message))
				buffer.append(message);
			else{
				buffer.append(message);
				clearBuffer();
			}

		}

		/*
		 *  The message and the newLine have been added to the buffer in the
		 *  appropriate order so we can now update the Document and send the
		 *  text to the optional PrintStream.
		 */
		private void clearBuffer(){
			//  In case both the standard out and standard err are being redirected
			//  we need to insert a newline character for the first line only

			if (isFirstLine && document.getLength() != 0)
			    buffer.insert(0, "\n");

			isFirstLine = false;
			String line = buffer.toString();

			try{
				int offset = document.getLength();
				document.insertString(offset, line, attributes);
				textComponent.setCaretPosition( document.getLength() );
			}
			catch (BadLocationException ble) {}

			if (printStream != null)
				printStream.print(line);

			buffer.setLength(0);
		}
	}
}
