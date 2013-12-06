package chat.client;

import hypeerweb.NodeCache;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import hypeerweb.validator.Validator;

/**
 * List all nodes in HyPeerWeb, categorized by
 * their InceptionSegment
 * @author guy
 */
public class ListTab extends JPanel{
	private static JTable table;
	private static MyTableModel tabModel = new MyTableModel();
	private static JComboBox segmentBox;
	private static segmentModel segModel = new segmentModel();
	private static NodeCache[] nodeList;
	
	public ListTab(){
		super(new BorderLayout());
		
		JPanel segmentPanel = new JPanel();
		JLabel label = new JLabel("Segment:");
		segmentBox = new JComboBox(segModel);
		segmentBox.setPreferredSize(new Dimension(150, 30));
		segmentBox.setBorder(new EmptyBorder(4, 8, 4, 4));
		segmentPanel.add(label);
		segmentPanel.add(segmentBox);
		
		EmptyBorder btnBorder = new EmptyBorder(7, 10, 7, 10);
		JButton deleteAll = new JButton("Clear HyPeerWeb");
		deleteAll.setBorder(btnBorder);
		deleteAll.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				ChatClient.removeAllNodes();
			}
		});
		segmentPanel.add(deleteAll);
		
		JButton validateButton = new JButton("Validate");
		validateButton.setBorder(btnBorder);
        validateButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				boolean pass = !ChatClient.isConnected() || (new Validator(ChatClient.nodeCache)).validate();
				ChatClient.showPopup(
					pass ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE,
					pass ? "Validation Passed!" : "Validation Failed!"
				);
			}
        });
		segmentPanel.add(validateButton);
		this.add(segmentPanel, BorderLayout.NORTH);
		
        table = new JTable(tabModel);
        table.setFillsViewportHeight(true);
		TableColumnModel model = table.getColumnModel();
		
		ListSelectionModel lsm = table.getSelectionModel();
		lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lsm.addListSelectionListener(new selectionHandler());
		
		TableColumn col; 
        DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer();    
        dtcr.setHorizontalAlignment(SwingConstants.CENTER);  
		for(int i = 0; i < tabModel.getColumnCount(); i++){
			col = model.getColumn(i); 
			col.setCellRenderer(dtcr);
		}
		
		model.getColumn(0).setPreferredWidth(58);
		model.getColumn(1).setPreferredWidth(44);
		model.getColumn(2).setPreferredWidth(45);
		model.getColumn(3).setPreferredWidth(85);
		model.getColumn(4).setPreferredWidth(50);
		model.getColumn(5).setPreferredWidth(50);
		model.getColumn(6).setPreferredWidth(25);
		model.getColumn(7).setPreferredWidth(25);
		model.getColumn(8).setPreferredWidth(25);
		
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
	}
	
	public void draw(){
		nodeList = (NodeCache[]) ChatClient.nodeCache.getOrderedListOfNodes();
		table.repaint();
		segModel.updateSegments();
		segmentBox.repaint();
	}

	private static class MyTableModel implements TableModel {
		private final String[] columnNames = {"Segment",
										"WebID",
                                        "Height",
                                        "Ns",
                                        "SNs",
                                        "ISNs",
										"F",
										"SF",
										"ISF"};
		
		public MyTableModel() {}
		
		@Override
		public int getRowCount() {
			if(ChatClient.nodeCache == null)
				return 0;
			else
				return ChatClient.nodeCache.nodes.size();
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public String getColumnName(int columnIndex) {
			return columnNames[columnIndex];
		}

		@Override
		public Class getColumnClass(int columnIndex) {
			return String.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if(ChatClient.nodeCache == null)
				return null;
			
			String result = "";
			NodeCache node = nodeList[rowIndex];
			int selection = segModel.getSelection();
			
			if (selection == -1 || selection == node.getNetworkId()){
				switch(columnIndex){
					case 0:
						result = "1";
						break;
					case 1:
						result += node.getWebId();
						break;
					case 2:
						result += node.getHeight();
						break;
					case 3:
						for(int n : node.getRawNeighbors())
							result += n + " ";
						break;
					case 4:
						for(int n : node.getRawSurrogateNeighbors())
							result += n + " ";
						break;
					case 5:
						for(int n : node.getRawInverseSurrogateNeighbors())
							result += n + " ";
						break;
					case 6:
						if(node.getRawFold() != -1)
							result += node.getRawFold();
						break;
					case 7:
						if(node.getRawSurrogateFold() != -1)
							result += node.getRawSurrogateFold();
						break;
					case 8:
						if(node.getRawInverseSurrogateFold() != -1)
							result += node.getRawInverseSurrogateFold();
						break;
				}
			}	
			return result;
		}
		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {	
		}
		@Override
		public void addTableModelListener(TableModelListener l) {
		}
		@Override
		public void removeTableModelListener(TableModelListener l) {
		}
	}
	
	private static class segmentModel implements ComboBoxModel{
		//temporary
		
		int selection = -1;//-1 for all segments
		String[] segments = {"All"};
		
		private void updateSegments(){
			if(ChatClient.nodeCache == null)
				return;
			
			int size = ChatClient.nodeCache.segments.size();
			int index = 1;
			Integer[] seg = ChatClient.nodeCache.segments.keySet().toArray(new Integer[size]);
			size++;//All goes first
			segments = new String[size];
			segments[0] = "All";
			for(Integer i : seg){
				segments[index++] = i.toString();
			}
			
		}
		
		@Override
		public void setSelectedItem(Object anItem) {
			if(anItem == "All")
				selection = -1;
			else
				selection = Integer.parseInt((String) anItem);
		}

		@Override
		public Object getSelectedItem() {
			if(selection == -1)
				return "All";
			else
				return selection;
		}
		
		public int getSelection(){
			return selection;
		}
		
		@Override
		public int getSize() {
			//get number of segments
			return segments.length;
		}

		@Override
		public Object getElementAt(int index) {
			if(index < 0 || index >= getSize())
				return null;
			return segments[index];
		}

		@Override
		public void addListDataListener(ListDataListener l) {
		}

		@Override
		public void removeListDataListener(ListDataListener l) {
		}
		
	}
	
	private static class selectionHandler implements ListSelectionListener{
		//changes selected node if there is a click in the table
		@Override
		public void valueChanged(ListSelectionEvent e) {
			ListSelectionModel lsm = (ListSelectionModel)e.getSource();
			int index = lsm.getMinSelectionIndex();
			NodeCache n = (NodeCache) ChatClient.nodeCache.nodes.values().toArray()[index];
			ChatClient.setSelectedNode(n);	
		}
	}
}
