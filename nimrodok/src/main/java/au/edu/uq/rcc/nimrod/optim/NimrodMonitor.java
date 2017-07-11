package au.edu.uq.rcc.nimrod.optim;

import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

public class NimrodMonitor {

	private final JFrame m_Frame;
	private final TableModel m_Model;

	public NimrodMonitor() {
		m_Frame = new JFrame("Monitor");
		m_Frame.setSize(640, 480);

		JScrollPane sp = new JScrollPane();
		m_Model = new TableModel();
		JTable table = new JTable(m_Model);
		table.getTableHeader().setReorderingAllowed(false);
		sp.setViewportView(table);

		m_Frame.getContentPane().add(sp);
		m_Frame.setVisible(false);

	}

	public void addInstance(final BaseAlgorithm instance) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				m_Model.addInstance(instance);
			}
		});
	}

	public void update(final BaseAlgorithm algo) {
		if(!m_Frame.isVisible()) {
			return;
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				m_Model.update(algo);
			}
		});
	}

	public synchronized void reset() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				m_Model.reset();
			}
		});
	}

	public void open() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				m_Frame.setVisible(true);
			}
		});
	}

	public synchronized void close() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				m_Frame.setVisible(false);
			}
		});
	}

	private class TableModel extends AbstractTableModel {

		private final ArrayList<BaseAlgorithm> m_Instances;
		private final HashMap<BaseAlgorithm, Integer> m_RowMappings;

		private final int ROW_INSTANCE_ID = 0;
		private final int ROW_STATE = 1;

		public TableModel() {
			m_Instances = new ArrayList<>();
			m_RowMappings = new HashMap<>();
		}

		public void addInstance(BaseAlgorithm algo) {
			if(algo == null) {
				return;
			}

			if(m_RowMappings.keySet().contains(algo)) {
				return;
			}

			m_RowMappings.put(algo, m_Instances.size());
			m_Instances.add(algo);
			fireTableRowsInserted(getRowCount(), getRowCount());
		}

		public void update(BaseAlgorithm algo) {
			Integer row = m_RowMappings.get(algo);
			if(row == null) {
				return;
			}

			fireTableRowsUpdated(row, row);
		}

		public void reset() {
			fireTableRowsDeleted(0, getRowCount());
			m_Instances.clear();
			m_RowMappings.clear();
		}

		@Override
		public int getRowCount() {
			return m_Instances.size();
		}

		@Override
		public int getColumnCount() {
			return m_ColumnNames.length;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			BaseAlgorithm algo = m_Instances.get(rowIndex);

			if(columnIndex == ROW_INSTANCE_ID) {
				return algo.hashCode();
			} else if(columnIndex == ROW_STATE) {
				return algo.getState();
			}

			return null;
		}

		@Override
		public String getColumnName(int columnIndex) {
			return m_ColumnNames[columnIndex];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return m_ColumnTypes[columnIndex];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

		private final Class[] m_ColumnTypes = new Class[]{
			Integer.class, BaseAlgorithm.State.class
		};

		private final String[] m_ColumnNames = new String[]{
			"Instance", "State"
		};
	}
}
