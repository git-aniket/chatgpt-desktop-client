package nl.vu.psy.ams.suite.gui.tabs.info;

import javax.swing.table.AbstractTableModel;

/*
 * Small class that provides formatting for the
 * JTable that shows label information.
 */
public class LabelInformationTableModel extends AbstractTableModel {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	private LITableData			data;

	public LabelInformationTableModel(LITableData data) {
		super();
		this.data = data;
	}

	@Override
	public Class<?> getColumnClass(int col) {
		return data.getValue(0, col).getClass();
	}

	@Override
	public int getColumnCount() {
		return data.getNCols();
	}

	@Override
	public int getRowCount() {
		return data.getNRows();
	}

	@Override
	public Object getValueAt(int row, int col) {
		return data.getValue(row, col);
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return false;
	}

}
