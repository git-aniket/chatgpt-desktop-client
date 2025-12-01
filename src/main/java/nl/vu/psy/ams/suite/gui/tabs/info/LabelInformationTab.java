package nl.vu.psy.ams.suite.gui.tabs.info;

//import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

// import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.sets.AmsLabelSet;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.TimeBar;
import nl.vu.psy.ams.suite.gui.tabs.AmsTab;
import nl.vu.psy.ams.suite.gui.tabs.imp.ImpTab;
import nl.vu.psy.ams.suite.gui.tabs.label.LabelTab;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
// import nl.vu.psy.ams.suite.tools.CachedThreadPool;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * The label information tab
 */
public class LabelInformationTab extends AmsTab {

	public class LITable extends JTable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public LITable(LabelInformationTableModel data) {
			super(data);
		}

		@Override
		public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {
			final Component prepareRenderer = super.prepareRenderer(renderer, row, column);
			final TableColumn tableColumn = getColumnModel().getColumn(column);
			// tableColumn.setHeaderValue(data.getHeader(column));
			if (prepareRenderer.getPreferredSize().width + 1 > tableColumn.getPreferredWidth()) {
				tableColumn.setPreferredWidth(prepareRenderer.getPreferredSize().width + 1);
			} else if (tableColumn.getPreferredWidth() < 80) {
				tableColumn.setPreferredWidth(80);
			}
			return prepareRenderer;
		}

	}

	class MyHeaderUI extends BasicTableHeaderUI {
		protected MouseInputListener createMouseInputListener() {
			return new MouseInputHandler();
		}

		public class MouseInputHandler extends BasicTableHeaderUI.MouseInputHandler {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (header.getDraggedDistance() != 0) {
				}
				super.mouseReleased(e);
			}
		}
	}

	// from https://stackoverflow.com/a/41145528
	class MultiLineTableHeaderRenderer extends JTextArea implements TableCellRenderer {
		public MultiLineTableHeaderRenderer() {
			setEditable(false);
			setLineWrap(true);
			setOpaque(false);
			setFocusable(false);
			setWrapStyleWord(true);
			LookAndFeel.installBorder(this, "TableHeader.cellBorder");
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			JTableHeader header = table.getTableHeader();
			int width = table.getColumnModel().getColumn(column).getWidth();
			int rowHeight = header.getHeight();
			setText((String) value);
			int prefHeight = getPreferredSize().height;
			setSize(width, prefHeight);
			prefHeight = getPreferredSize().height;
			if (prefHeight > rowHeight)
				header.setPreferredSize(new Dimension(header.getWidth(), prefHeight));
			return this;
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static LabelInformationTab instance;

	public static LabelInformationTab getInstance() {
		if (instance == null)
			instance = new LabelInformationTab();
		return instance;
	}

	public static LabelInformationTab getInstanceIfExist() {
		return instance;
	}

	public static LabelInformationTab getNewInstance() {
		instance = null;
		instance = new LabelInformationTab();
		return instance;
	}

	private LabelInformationTableModel model;
	private LITableData data;
	private DefaultTableColumnModel cm;
	private JTableHeader th;

	/*
	 * private Comparator<Object> comparator = new Comparator<Object>() {
	 * 
	 * @Override
	 * public int compare(Object s1, Object s2) {
	 * if (s1 instanceof java.lang.String || s2 instanceof java.lang.String) {
	 * String ss1 = s1.toString();
	 * String ss2 = s2.toString();
	 * if (ss1.length() != ss2.length()) {
	 * return ss1.length() - ss2.length();
	 * } else {
	 * return ss1.compareTo(ss2);
	 * }
	 * } else if (s1 instanceof java.lang.Number) {
	 * Number n1 = (Number) s1;
	 * Number n2 = (Number) s2;
	 * return Double.compare(n1.doubleValue(), n2.doubleValue());
	 * }
	 * return -1;
	 * }
	 * };
	 */

	private JTable table;

	private LabelInformationTab() {
		super();
		TimeBar.getInstance().connectToXAxis(mainXAxis);
		TimeBar.getInstance().connectToTab(this);
		setToolBar(new LabelInformationToolbar(this));
	}

	public LITableData getData() {
		return data;
	}

	public LabelInformationTableModel getTableModel() {
		return model;
	}

	@Override
	public boolean setActive() {
		super.setActive();
		if (!CurrentOpenData.getInstance().getLabels().getLabels().isEmpty())
			ImpTab.getInstance().redraw(0);
		data.recalculate();
		// model = new LabelInformationTableModel(data);
		// table.setModel(model);
		// table.setTableHeader(th);
		// table.createDefaultColumnsFromModel();
		// DefaultTableCellRenderer dtcr = new CustomCellRenderer();
		// dtcr.setHorizontalAlignment(SwingConstants.RIGHT);
		// for (int i = 0; i < data.getNCols(); i++) {
		// table.getColumnModel().getColumn(i).setHeaderValue(data.getHeader(i));
		// table.getColumnModel().getColumn(i).setCellRenderer(dtcr);
		// }

		return true;
	}

	@Override
	public void setupItems() {
		data = new LITableData();
		setUpTable();
	}

	public void setUpTable() {
		model = new LabelInformationTableModel(data);
		table = new LITable(model);
		cm = new DefaultTableColumnModel() {
			private static final long serialVersionUID = 1L;

			public void moveColumn(int columnIndex, int newIndex) {
				int n = columnIndex - newIndex;
				super.moveColumn(columnIndex, newIndex);
				data.moveColumn(columnIndex, -n);
				// if (n != 0)
				// data.recalculate(-1, false);
			}
		};
		table.setColumnModel(cm);
		th = new JTableHeader(cm) {
			private static final long serialVersionUID = 1L;
			private transient boolean updateInProgress;

			public void updateUI() {
				if (!updateInProgress) {
					updateInProgress = true;
					try {
						setUI(new MyHeaderUI());

						TableCellRenderer renderer = getDefaultRenderer();
						if (renderer instanceof Component) {
							SwingUtilities.updateComponentTreeUI((Component) renderer);
						}
					} finally {
						updateInProgress = false;
					}
				}
			}
		};
		th.addMouseListener(new java.awt.event.MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				int c = table.columnAtPoint(e.getPoint());
				if (c >= 0 && c < table.getColumnCount()) {
					table.setColumnSelectionInterval(c, c);
				} else {
					table.clearSelection();
				}
			}

		});
		JPopupMenu popupMenu = new JPopupMenu();
		// JMenuItem menuItemRemove = new JMenuItem("Remove current column");
		JMenuItem menuItemDefaults = new JMenuItem("Restore Factory Config");
		JMenuItem menuItemLoadConfig = new JMenuItem("Load Default Config");

		// menuItemRemove.addActionListener(new AbstractAction() {
		// private static final long serialVersionUID = 1L;

		// @Override
		// public void actionPerformed(ActionEvent e) {
		// int selectedColumn = table.getSelectedColumn();
		// if (selectedColumn == -1)
		// return;
		// data.disableColumn(selectedColumn);
		// table.removeColumn(cm.getColumn(selectedColumn));
		// cm.removeColumn(cm.getColumn(selectedColumn));
		// th.setDraggedColumn(null);
		// // data.recalculate(-1, false);
		// }
		// });
		menuItemDefaults.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				data.setDefault();
				getInstance().setUnactive();
				getInstance().setActive();
			}
		});
		menuItemLoadConfig.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				data.loadFromDisk();
				getInstance().setUnactive();
				getInstance().setActive();
			}
		});

		// popupMenu.add(menuItemRemove);
		popupMenu.add(menuItemDefaults);
		popupMenu.add(menuItemLoadConfig);

		// sets the popup menu for the tableheader
		th.setComponentPopupMenu(popupMenu);
		th.setDefaultRenderer(new MultiLineTableHeaderRenderer());
		table.setTableHeader(th);
		table.createDefaultColumnsFromModel();
		if (table.getColumnModel().getColumnCount() == 0)
			return;
		table.setAutoCreateRowSorter(true);
		/*
		 * TableRowSorter<TableModel> sorter = new
		 * TableRowSorter<TableModel>(table.getModel());
		 * for (int i = 0; i < table.getColumnCount(); i++)
		 * sorter.setComparator(i, comparator);
		 * table.setRowSorter(sorter);
		 */
		DefaultTableCellRenderer dtcr = new CustomCellRenderer();
		dtcr.setHorizontalAlignment(SwingConstants.RIGHT);
		for (int i = 0; i < data.getNCols(); i++) {
			table.getColumnModel().getColumn(i).setHeaderValue(data.getHeader(i));
			table.getColumnModel().getColumn(i).setCellRenderer(dtcr);
		}

		// final TableCellRenderer renderer =
		// table.getTableHeader().getDefaultRenderer();

		// for (int i = 0; i < table.getColumnCount(); ++i) {
		// table.getColumnModel()
		// .getColumn(i)
		// .setPreferredWidth(
		// renderer.getTableCellRendererComponent(table,
		// table.getColumnModel().getColumn(i).getHeaderValue(), false, false, 0, i)
		// .getPreferredSize().width);
		// }

		panel.removeAll();
		table.addMouseListener(new java.awt.event.MouseAdapter() {

			@Override
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				// find columns
				int[] columnIndexes = { -1, -1, -1, -1, -1 };
				for (int i = 0; i < table.getColumnCount(); i++) {
					switch (table.getColumnModel().getColumn(i).getHeaderValue().toString()) {
						case "Label ID":
							columnIndexes[0] = i;
							break;
						case "Start Date":
							columnIndexes[1] = i;
							break;
						case "Start Time":
							columnIndexes[2] = i;
							break;
						case "End Date":
							columnIndexes[3] = i;
							break;
						case "End Time":
							columnIndexes[4] = i;
							break;
					}
				}
				int row = table.rowAtPoint(evt.getPoint());
				int col = table.columnAtPoint(evt.getPoint());

				if (columnIndexes[1] == -1 || columnIndexes[2] == -1 || columnIndexes[3] == -1
						|| columnIndexes[4] == -1)
					return;
				String s1 = (table.getValueAt(row, columnIndexes[1]).toString())
						+ "/" + (table.getValueAt(row, columnIndexes[2]).toString());
				String s2 = (table.getValueAt(row, columnIndexes[3]).toString())
						+ "/" + (table.getValueAt(row, columnIndexes[4]).toString());

				String pattern = "dd-MM-yyyy/HH:mm:ss";
				SimpleDateFormat format = new SimpleDateFormat(pattern);

				Date date2 = Utils.parseDate(format, s1); // withMS);
				Date date3 = Utils.parseDate(format, s2); // withMS);
				if (date2 != null && date3 != null) {

					GregorianCalendar dd = CurrentOpenData.getInstance().getStartDate();
					double newlefttime = Utils.getTimeFromDate(date2, dd,
							CurrentOpenData.getInstance().getStartTimeInUS());
					double newrighttime = Utils.getTimeFromDate(date3, dd,
							CurrentOpenData.getInstance().getStartTimeInUS());

					LabelTab.getInstance().getXAxis().setTimes(newlefttime - 18000000, newrighttime + 18000000);
					LabelTab.getInstance().getXAxis().updateAll();
					if (col == columnIndexes[0]) {
						MainFrame.getInstance().getTabs().setSelectedComponent(LabelTab.getInstance());
						MainFrame.getInstance().getMainFrame().repaint();
					}
					MainFrame.getInstance().getTabs().repaint();
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				int r = table.rowAtPoint(e.getPoint());
				if (r >= 0 && r < table.getRowCount()) {
					table.setRowSelectionInterval(r, r);
				} else {
					table.clearSelection();
				}
			}

		});
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		// table.getTableHeader().setReorderingAllowed(false);
		JScrollPane pane = new JScrollPane(table);
		panel.add(pane, 1F); // BorderLayout.CENTER);
	}

	private class CustomCellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {

			Component rendererComp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
					row, column);

			// Make Label IDS texts blue to indicate they are clickable
			Integer misVal = AppSettings.getInstance().getIntProperty(Settings.MISSINGVALUE);
			if (table.getColumnModel().getColumn(column).getHeaderValue().equals("Label ID")) // column ==
																								// data.columns[1].pos)
																								// //data.columns[column].id
																								// == 1
				rendererComp.setForeground(Color.blue);
			else if (value instanceof Integer && misVal.equals(value))
				rendererComp.setForeground(Color.gray);
			else
				rendererComp.setForeground(null);
			if (isSelected == true)
				rendererComp.setBackground(new Color(255, 255, 153));
			else if (row == table.getRowCount() - 1
					&& AppSettings.getInstance().getIntProperty(Settings.SHOWENTIREDATA) == 1)
				rendererComp.setBackground(Color.lightGray);
			else
				rendererComp.setBackground(null);
			return rendererComp;
		}
	}

	public void selectLabel(double time) {
		CurrentOpenData cod = CurrentOpenData.getInstance();
		AmsLabelSet ls = cod.getLabels();
		AmsLabel lbl = ls.getLabelUnderTime(time);
		if (lbl != null) {
			int labelNo = lbl.getLabelNo();
			if (labelNo - 1 < table.getRowCount())
				table.setRowSelectionInterval(labelNo - 1, labelNo - 1);
		}
	}
}
