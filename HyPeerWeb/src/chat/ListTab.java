package chat;

import hypeerweb.Node;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 * List all nodes in HyPeerWeb, categorized by
 * their InceptionSegment
 * @author isaac
 */
public class ListTab extends JPanel{
	public ListTab() {
		super(new GridLayout(1,0));
 
        JTable table = new JTable(new MyTableModel());
        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        table.setFillsViewportHeight(true);
 
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
 
        //Add the scroll pane to this panel.
        add(scrollPane);
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
		private ArrayList<Node> nodes = new ArrayList();
		
		public MyTableModel() {
		}

		@Override
		public int getRowCount() {
			return nodes.size();
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
			return Integer.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return 1;
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
