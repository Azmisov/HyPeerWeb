package chat;

import hypeerweb.NodeCache.Node;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * List all nodes in HyPeerWeb, categorized by
 * their InceptionSegment
 * @author isaac
 */
public class ListTab extends JPanel{
	private static ChatClient container;
	private static JTable table;
	private static MyTableModel tabModel = new MyTableModel();
	private static JComboBox segmentBox;
	private static segmentModel segModel = new segmentModel();
	
	public ListTab(ChatClient container) {
		super(new BorderLayout());
		ListTab.container = container;
		
		JPanel segmentPanel = new JPanel();
		JLabel label = new JLabel("Segment:");
		segmentBox = new JComboBox(segModel);
		segmentBox.setPreferredSize(new Dimension(150, 30));
		segmentBox.setBorder(new EmptyBorder(4, 8, 4, 4));
		segmentPanel.add(label);
		segmentPanel.add(segmentBox);
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
			if(container.nodeCache == null)
				return 0;
			else
				return container.nodeCache.nodes.size();
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
			if(container.nodeCache == null)
				return null;
			
			String result = "";
			Node node = (Node) container.nodeCache.nodes.values().toArray()[rowIndex];
			int selection = segModel.getSelection();
			
			if(selection == -1 || selection == node.getNetworkId()){
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
						for(Node n : node.getNeighbors())
							result += n.getWebId() + " ";
						break;
					case 4:
						for(Node n : node.getSurrogateNeighbors())
							result += n.getWebId() + " ";
						break;
					case 5:
						for(Node n : node.getInverseSurrogateNeighbors())
							result += n.getWebId() + " ";
						break;
					case 6:
						if(node.getFold() != null)
							result += node.getFold().getWebId();
						break;
					case 7:
						if(node.getSurrogateFold() != null)
							result += node.getSurrogateFold().getWebId();
						break;
					case 8:
						if(node.getInverseSurrogateFold() != null)
							result += node.getInverseSurrogateFold().getWebId();
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
			if(container.nodeCache == null)
				return;
			
			int size = container.nodeCache.segments.size();
			int index = 1;
			Integer[] seg = container.nodeCache.segments.toArray(new Integer[size]);
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
			Node n = (Node) container.nodeCache.nodes.values().toArray()[index];
			container.setSelectedNode(n);	
		}
	}
}
