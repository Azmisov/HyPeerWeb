package chat;

import hypeerweb.Node;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * List all nodes in HyPeerWeb, categorized by
 * their InceptionSegment
 * @author isaac
 */
public class ListTab extends JPanel{
	private static ChatClient container;
	private JTable table;
	
	public ListTab(ChatClient container) {
		super(new GridLayout(1,0));
		
		ListTab.container = container;
        table = new JTable(new MyTableModel());
        table.setFillsViewportHeight(true);
		
		TableColumn col; 
        DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer();    
        dtcr.setHorizontalAlignment(SwingConstants.CENTER);  
		for(int i = 0; i < 8; i++){
			col = table.getColumnModel().getColumn(i); 
			col.setCellRenderer(dtcr);
		}
		
		table.getColumnModel().getColumn(0).setPreferredWidth(50);
		table.getColumnModel().getColumn(1).setPreferredWidth(50);
		table.getColumnModel().getColumn(2).setPreferredWidth(100);
		table.getColumnModel().getColumn(5).setPreferredWidth(35);
		table.getColumnModel().getColumn(6).setPreferredWidth(35);
		table.getColumnModel().getColumn(7).setPreferredWidth(35);
		
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);
	}
	
	public void draw(){
		table.repaint();
	}

	private static class MyTableModel implements TableModel {
		private String[] columnNames = {"WebID",
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
			return container.nodeList.list.size();
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
			String result = "";
			Node node = container.nodeList.getNodes().get(rowIndex);
			switch(columnIndex){
				case 0:
					result += node.getWebId();
					break;
				case 1:
					result += node.getHeight();
					break;
				case 2:
					for(Node n : node.getNeighbors())
						result += n.getWebId() + " ";
					break;
				case 3:
					for(Node n : node.getSurrogateNeighbors())
						result += n.getWebId() + " ";
					break;
				case 4:
					for(Node n : node.getInverseSurrogateNeighbors())
						result += n.getWebId() + " ";
					break;
				case 5:
					if(node.getFold() != null)
						result += node.getFold().getWebId();
					break;
				case 6:
					if(node.getSurrogateFold() != null)
						result += node.getSurrogateFold().getWebId();
					break;
				case 7:
					if(node.getInverseSurrogateFold() != null)
						result += node.getInverseSurrogateFold().getWebId();
					break;
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
}
