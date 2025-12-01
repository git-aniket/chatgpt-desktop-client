package nl.vu.psy.ams.suite.gui.tabs.info;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.LabelValue;
import nl.vu.psy.ams.suite.data.structures.sets.AmsLabelSet;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.tabs.imp.ImpTab;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.ThreadServer;
import nl.vu.psy.ams.suite.tools.Utils;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/*
 * Class that provides data for the output table. If you want to add
 * a new value to the table, add it to allNames, and add the value at the
 * end of the switch block in getDataValues().
 */
public class LITableData {

	static class DataColumn {
		String header;
		boolean enabled = true;
		int pos = 0;
		int id;

		public DataColumn() {

		}

		public DataColumn(String header, int id, boolean enabled) {
			this.header = header;
			this.id = id;
			this.enabled = enabled;
			this.pos = id;
		}

		public DataColumn(String header, int id, boolean enabled, int pos) {
			this.header = header;
			this.id = id;
			this.enabled = enabled;
			this.pos = pos;
		}

		public String toString() {
			return header + "\t" + enabled + "\t" + id + "\t" + pos;
		}
	}

	private class DataValue {
		private Object data;

		public DataValue(Object val) {
			data = val;
		}

		public Object getTableValue() {
			if (data instanceof java.lang.Double) {
				return nf.format(data);
			}
			return data;
		}

		public Object getValue() {
			return data;
		}

	}

	private NumberFormat nf = NumberFormat.getInstance(Locale.US);

	DataColumn[] columns;

	private String[] factoryNames = {
			"Subject ID", "Label ID", "Start Date", "Start Time",
			"End Date", "End Time", "Label Duration [s]", "Total Motility [mg]", "Average IBI [msec]",
			"Average HR [bpm]", "Respiration Rate [bpm]", "Tidal Volume [m\u2126]",
			"RSA-0 [msec]", "SDNN [msec]", "RMSSD [msec]", "LF [ms\u00B2]", "HF [ms\u00B2]", "PEP [msec]",
			"LVET [msec]", "T-Wave amplitude [mV]", "Stroke Volume (Nederend 2017) [cc]",
			"Minute Volume (Nederend 2017) [l/min]", "Average SCL [uS]", "nsSCRs per minute [ppm]",
			"Average step impact [g]", "steps per minute [spm]", "Metabolic Equivalent []"
	};

	private ArrayList<DataColumn> enabledColumns = new ArrayList<DataColumn>();

	private DataValue[][] data;
	private DataValue[][] dataCopy;
	private int nRows;
	private int nCols;
	private AmsLabelConfiguration config;
	private Thread recalcThread;

	public LITableData() {
		setDefault();
		loadFromDisk();
		recalculateSimulation();
	}

	final Comparator<DataColumn> byPosEnable = new Comparator<DataColumn>() {
		@Override
		public int compare(DataColumn o2, DataColumn o1) {
			int retVal = Boolean.compare(o1.enabled, o2.enabled);
			if (retVal != 0)
				return retVal;
			return o2.pos - o1.pos;
		}
	};
	final Comparator<DataColumn> byPos = new Comparator<DataColumn>() {
		@Override
		public int compare(DataColumn o2, DataColumn o1) {
			return o2.pos - o1.pos;
		}
	};
	final Comparator<DataColumn> byId = new Comparator<DataColumn>() {
		@Override
		public int compare(DataColumn o2, DataColumn o1) {
			return o2.id - o1.id;
		}
	};
	// public ArrayList<DataColumn> sList = new ArrayList<LITableData.DataColumn>();

	public void appendDataToXLS(File fl, boolean recalc, double tW) {
		if (fl.exists() == false) {
			outputDataToXLS(fl, tW, false);
		} else {
			recalculate(tW);
			BufferedInputStream fis = null;
			BufferedOutputStream fos = null;
			if (recalcThread != null) {
				try {
					recalcThread.join();
				} catch (InterruptedException e) {
				}
			}
			try {
				fis = new BufferedInputStream(new FileInputStream(fl));
				HSSFWorkbook wb = new HSSFWorkbook(fis);
				fis.close();
				Sheet sh = wb.getSheet("AMSdata");
				int lastRow = sh.getLastRowNum();
				for (int i = 0; i < nRows; i++) {
					Row row = sh.createRow(lastRow + i + 1);
					for (int j = 0; j < nCols; j++) {
						Object val = data[i][j].getValue();
						if (val instanceof java.lang.String) {
							row.createCell(j, CellType.STRING).setCellValue((String) val);
						} else {
							row.createCell(j, CellType.NUMERIC).setCellValue(((Number) val).doubleValue());
						}
					}
				}
				fos = new BufferedOutputStream(new FileOutputStream(fl));
				for (int i = 0; i < nCols; i++)
					sh.autoSizeColumn(i);
				wb.write(fos);
				wb.close();
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}
		if (recalc)
			recalculate();
	}

	public void editConfig() {
		final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Output config editor", true);
		diag.setLayout(new BorderLayout());
		final String[] columnNames = { "Name", "Factory Name", "Enabled" };
		java.util.Arrays.sort(columns, byPosEnable);
		final AbstractTableModel atm = new AbstractTableModel() {

			private static final long serialVersionUID = 1L;

			@Override
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public Class getColumnClass(int c) {
				return getValueAt(0, c).getClass();
			}

			@Override
			public int getColumnCount() {
				return columnNames.length;
			}

			@Override
			public String getColumnName(int col) {
				return columnNames[col];
			}

			@Override
			public int getRowCount() {
				return columns.length;
			}

			@Override
			public Object getValueAt(int row, int col) {
				if (col == 0) {
					return columns[row].header;
				} else if (col == 1) {
					int nameCount = allNames.values().length - 1;
					// return
					// LabelInformationTab.getInstance().getTable().getColumnModel().getColumn(sList.get(row).id);
					if (columns[row].id < nameCount)
						return columns[row].header; // allNames.valueOf(columns[row].header);
					else if (columns[row].id < nameCount + config.getCategories().size())
						return config.getCategories().get(columns[row].id - nameCount);
					else
						return config.getCategories().get(columns[row].id - nameCount - config.getCategories().size())
								+ " Code";
				} else {
					return columns[row].enabled;
				}
			}

			@Override
			public boolean isCellEditable(int row, int col) {
				if (col == 1)
					return false;
				return true;
			}

			@Override
			public void setValueAt(Object value, int row, int col) {
				if (col == 0) {
					columns[row].header = (String) value;
				} else {
					if ((Boolean) value)
						enableColumn(row);
					if (!(Boolean) value)
						disableColumn(row);
					java.util.Arrays.sort(columns, byPosEnable);
					fireTableDataChanged();
				}
				fireTableCellUpdated(row, col);
			}
		};
		final JTable table = new JTable(atm);

		table.getTableHeader().setReorderingAllowed(false);

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// drag and drop rows
		final class MouseHandler implements MouseListener, MouseMotionListener {

			private Integer row = null;

			private final JTable table;
			private final AbstractTableModel tableModel;

			public MouseHandler(JTable table, AbstractTableModel model) {
				this.table = table;
				this.tableModel = model;
			}

			@Override
			public void mouseClicked(MouseEvent event) {
			}

			@Override
			public void mousePressed(MouseEvent event) {
				if (table == null) {
					return;
				}
				int viewRowIndex = table.rowAtPoint(event.getPoint());
				row = table.convertRowIndexToModel(viewRowIndex);
			}

			@Override
			public void mouseReleased(MouseEvent event) {
				row = null;
			}

			@Override
			public void mouseEntered(MouseEvent event) {
			}

			@Override
			public void mouseExited(MouseEvent event) {
			}

			@Override
			public void mouseDragged(MouseEvent event) {
				if (table == null || tableModel == null) {
					return;
				}

				int viewRowIndex = table.rowAtPoint(event.getPoint());
				int selRow = table.convertRowIndexToModel(viewRowIndex);

				int n = selRow - row;
				moveColumn(row, n);
				java.util.Arrays.sort(columns, byPosEnable);
				atm.fireTableDataChanged();
				row = selRow;
				table.setRowSelectionInterval(viewRowIndex, viewRowIndex);
			}

			@Override
			public void mouseMoved(MouseEvent event) {
			}

		}
		MouseHandler handler = new MouseHandler(table, atm);
		table.addMouseListener(handler);
		table.addMouseMotionListener(handler);

		diag.add(new JScrollPane(table), BorderLayout.CENTER);

		JPanel butPan = new JPanel();
		butPan.setLayout(new BoxLayout(butPan, BoxLayout.Y_AXIS));

		butPan.add(Box.createVerticalGlue());

		JButton but;

		but = new JButton("Selection \u21E7");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int selRow = table.getSelectedRow();
				if (selRow != -1 && selRow != 0) {
					moveColumn(selRow, -1);
					java.util.Arrays.sort(columns, byPosEnable);
					atm.fireTableDataChanged();
					table.getSelectionModel().setSelectionInterval(selRow - 1, selRow - 1);
				}
			}
		});
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		butPan.add(but);

		but = new JButton("Selection \u21E9");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int selRow = table.getSelectedRow();
				if (selRow != -1 && selRow != columns.length - 1) {
					moveColumn(selRow, 1);
					java.util.Arrays.sort(columns, byPosEnable);
					atm.fireTableDataChanged();
					table.getSelectionModel().setSelectionInterval(selRow + 1, selRow + 1);
				}
			}
		});
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		butPan.add(but);

		but = new JButton("Select all");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < table.getRowCount(); i++) {
					columns[i].enabled = true;
				}
				java.util.Arrays.sort(columns, byPosEnable);
				atm.fireTableDataChanged();
			}
		});
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		butPan.add(but);

		but = new JButton("Deselect all");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < table.getRowCount(); i++) {
					columns[i].enabled = false;
				}
				java.util.Arrays.sort(columns, byPosEnable);
				atm.fireTableDataChanged();
			}
		});
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		butPan.add(but);

		butPan.add(Box.createVerticalGlue());

		but = new JButton("Save Config As Default");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveToDisk();
			}
		});
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		butPan.add(but);
		but = new JButton("Load Default Config");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadFromDisk();
				java.util.Arrays.sort(columns, byPosEnable);
				atm.fireTableDataChanged();
			}
		});
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		butPan.add(but);

		butPan.add(Box.createVerticalGlue());

		but = new JButton("Restore Factory Config");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setDefault();
				java.util.Arrays.sort(columns, byPosEnable);
				atm.fireTableDataChanged();
			}
		});
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		butPan.add(but);

		but = new JButton("Show All Columns");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setAll();
				java.util.Arrays.sort(columns, byPosEnable);
				atm.fireTableDataChanged();
			}
		});
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		butPan.add(but);

		but = new JButton("Close");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				diag.setVisible(false);
			}
		});
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		butPan.add(but);

		butPan.add(Box.createVerticalGlue());

		diag.add(butPan, BorderLayout.EAST);

		diag.pack();
		diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());

		diag.setVisible(true);

		recalculate();
		LabelInformationTab.getInstance().setUpTable();
		MainFrame.getInstance().getMainFrame().repaint();
	}

	private DataValue[] getDataValues(AmsLabel lbl, ArrayList<DataColumn> enabledColumns, int rowIndex) {
		int misVal = AppSettings.getInstance().getIntProperty(Settings.MISSINGVALUE);
		DataValue[] vals = new DataValue[enabledColumns.size()];
		CurrentOpenData cod = CurrentOpenData.getInstance();
		String chan = "";
		double leftTime = lbl.getLeftTime();
		double rightTime = lbl.getRightTime();
		double[] freq = lbl.getFreqPowers(false, 230); // minute with rounding for beats
		double avZ0 = lbl.getAverageZ0();
		double sdZ0 = lbl.getStdDevZ0();
		double[] valuesZ0 = lbl.getMinMaxZ0();
		double rhoVal = ImpTab.getInstance().getIip().getRhoVal();
		double edVal = ImpTab.getInstance().getIip().getEdVal();
		double lTime = lbl.getICGbPoint() / 1000000.;
		double rTime = lbl.getICGxPoint() / 1000000.;
		double sv = rhoVal * edVal * edVal * lbl.getICGcVal() * (rTime - lTime) / (avZ0 * avZ0);
		double avZ0Corrected = 7.337 - 6.208 * -lbl.getICGcVal(); // SV_average, c.f. Table 6 in Nederend et al. 2017
																	// (http://dx.doi.org/10.1016/j.ijpsycho.2017.07.015)
		double svc = rhoVal * edVal * edVal * lbl.getICGcVal() * (rTime - lTime) / (avZ0Corrected * avZ0Corrected);
		double valuesSCL[] = null;
		int sclcycles = -1;
		if (cod.channelExists("SCL")) {
			valuesSCL = lbl.getMinMaxSCL();
			sclcycles = lbl.getSCLCycleCount();
		}
		Double totalTime = lbl.getTotalTimeUnderLabel();
		double HR = lbl.getAverage(true);
		double[] rrVals = lbl.getRRVals();
		double[] rsaVals = lbl.getRSAVals();
		double[] rsa0Vals = lbl.getRSA0Vals();
		double[] rsaAddedVals = lbl.getRSAAddedVals();
		double[] rsa0AddedVals = lbl.getRSA0AddedVals();
		double[] tidalVolumeVals = lbl.getTidalVolumeVals();
		double[] tidalVolumeRawVals = lbl.getTidalVolumeRawVals();
		double[] inspVals = lbl.getInspVals();
		double[] expVals = lbl.getExpVals();
		double[] inspExpVals = lbl.getInspExpVals();
		double[] shortestIBIVals = lbl.getShortestVals();
		double[] longestIBIVals = lbl.getLongestVals();
		double lblLength = (rightTime - leftTime);
		double valuesSteps[] = null;
		int steps = -1;
		if (cod.channelExists("StepInstances")) {
			valuesSteps = lbl.getMinMaxStep();
			steps = lbl.getStepCount();
		}
		double valuesECG[] = null;
		if (cod.channelExists("ECG")) {
			valuesECG = lbl.getMinMaxECG();
		}
		double valuesECG2[] = null;
		if (cod.channelExists("V2ecg")) {
			valuesECG2 = lbl.getMinMaxECG2();
		}
		double vl = 0;
		for (int i = 0; i < (enabledColumns.size()); i++) {
			Optional<allNames> env = allNames.get(enabledColumns.get(i).header);
			allNames id = allNames.CAT;
			if (env.isPresent())
				id = env.get();
			switch (id) {
				case Subj: // subject id
					vals[i] = new DataValue(cod.getFileHeader().getSzSubjectID());
					break;
				case Label: // label id
					if (rowIndex > 0)
						vals[i] = new DataValue(String.valueOf(rowIndex));
					else
						vals[i] = new DataValue("Entire data");
					break;
				case Date1: // start date
					vals[i] = new DataValue(Utils.getDateFromUS(leftTime));
					break;
				case Time1: // start time
					vals[i] = new DataValue(Utils.getTimeFromUS(leftTime));
					break;
				case Date2: // end date
					vals[i] = new DataValue(Utils.getDateFromUS(rightTime));
					break;
				case Time2: // end time
					vals[i] = new DataValue(Utils.getTimeFromUS(rightTime));
					break;
				case IBI: // IBI count
					vl = lbl.getNumberOfIBIs();
					if (vl < 0) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(vl);
					}
					break;
				case avIBI: // average IBI
					vl = lbl.getAverage(false);
					if (vl < 0) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(vl);
					}
					break;
				case sdIBI: // SDNN (sd IBI)
					vl = lbl.getStddev(false);
					if (vl < 0) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(vl);
					}
					break;
				case minIBI: // min IBI
					vl = lbl.getMin(false);
					if (vl < 0) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(vl);
					}
					break;
				case maxIBI: // max IBI
					vl = lbl.getMax(false);
					if (vl < 0) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(vl);
					}
					break;
				case avHR: // average HR
					if (HR < 0) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(HR);
					}
					break;
				case sdHR: // std HR
					vl = lbl.getStddev(true);
					if (vl < 0) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(vl);
					}
					break;
				case minHR: // min HR
					vl = lbl.getMin(true);
					if (vl < 0) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(vl);
					}
					break;
				case maxHR: // max HR
					vl = lbl.getMax(true);
					if (vl < 0) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(vl);
					}
					break;
				case RMSSD: // RMSSD
					vl = lbl.getRMSSD();
					if (vl < 0) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(vl);
					}
					break;
				case MYA: // average motility
					if (cod.channelExists("MYA") == false) {
						vals[i] = new DataValue(misVal);
					} else {
						vl = lbl.getAvMot();
						if (Double.isNaN(vl) || vl < 0) {
							vals[i] = new DataValue(misVal);
						} else {
							vals[i] = new DataValue(vl);
						}
					}
					break;
				case LF: // LF
					if (freq == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(freq[0]);
					}
					break;
				case HF: // HF
					if (freq == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(freq[1]);
					}
					break;
				case LFHF: // LF/HF
					if (freq == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(freq[0] / freq[1]);
					}
					break;
				case PEP: // Calculation of PEP
					double pep = Double.NEGATIVE_INFINITY;
					if (lbl.isICGMissing() == false) {
						if (lbl.getICGbPoint() != Double.NEGATIVE_INFINITY
								&& lbl.getECGQOnsetPoint() != Double.NEGATIVE_INFINITY) {
							if (lbl.isECGQOnsetMissing() == false) { // Q-onset is present
								pep = (lbl.getICGbPoint() - lbl.getECGQPoint()) / 1000;
								vals[i] = new DataValue(pep);
							} else { // Q-onset is missing
								if (lbl.isECGQPointMissing() == false) { // Q Point present
									pep = ((lbl.getICGbPoint() - lbl.getECGQOnsetPoint()) / 1000) + 12;
									vals[i] = new DataValue(pep);
								} else {
									pep = ((lbl.getICGbPoint() - lbl.getECGRPoint()) / 1000) + 41;
									vals[i] = new DataValue(pep);
								}
							}
						} else {
							vals[i] = new DataValue(misVal);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case LVET: // LVET
					double lvet = lbl.getICGxPoint() - lbl.getICGbPoint();
					if (lbl.getICGxPoint() == Double.NEGATIVE_INFINITY || lbl.getICGbPoint() == Double.NEGATIVE_INFINITY
							|| lbl.isICGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						lvet /= 1000;
						vals[i] = new DataValue(lvet);
					}
					break;
				case TWave: // T-wave amplitude
					double TValue = lbl.getECGTVal();
					double TOffsetValue = lbl.getECGTOffsetVal();
					if (TValue == Double.NEGATIVE_INFINITY || TOffsetValue == Double.NEGATIVE_INFINITY
							|| lbl.isECGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(TValue - TOffsetValue);
					}
					break;
				case QT: // QT interval
					double QT = Double.NEGATIVE_INFINITY;
					if (lbl.isECGMissing() == false) {
						if (lbl.getECGTOffsetPoint() != Double.NEGATIVE_INFINITY
								&& lbl.getECGQOnsetPoint() != Double.NEGATIVE_INFINITY) {
							if (lbl.isECGQOnsetMissing() == false) { // Q-onset is present
								QT = (lbl.getECGTOffsetPoint() - lbl.getECGQPoint()) / 1000;
								vals[i] = new DataValue(QT);
							} else { // Q-onset is missing
								if (lbl.isECGQPointMissing() == false) { // Q Point present
									QT = ((lbl.getECGTOffsetPoint() - lbl.getECGQOnsetPoint()) / 1000) + 12;
									vals[i] = new DataValue(QT);
								} else {
									vals[i] = new DataValue(misVal);
								}
							}
						} else {
							vals[i] = new DataValue(misVal);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case Stroke1: // stroke volume (K)
					if (lbl.getICGxPoint() == Double.NEGATIVE_INFINITY || lbl.getICGbPoint() == Double.NEGATIVE_INFINITY
							|| lbl.getICGcVal() == Double.NEGATIVE_INFINITY || lbl.isICGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(sv);
					}
					break;
				case Stroke2: // stroke volume (N)
					if (lbl.getICGxPoint() == Double.NEGATIVE_INFINITY || lbl.getICGbPoint() == Double.NEGATIVE_INFINITY
							|| lbl.getICGcVal() == Double.NEGATIVE_INFINITY || lbl.isICGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(svc);
					}
					break;
				case Minute1: // minute volume (K)
					double mv = sv * HR / 1000.;
					if (lbl.getICGxPoint() == Double.NEGATIVE_INFINITY || lbl.getICGbPoint() == Double.NEGATIVE_INFINITY
							|| lbl.getICGcVal() == Double.NEGATIVE_INFINITY || lbl.isICGMissing()
							|| lblLength < 60 * 1000000) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(mv);
					}
					break;
				case Minute2: // minute volume (N)
					double mvol = svc * HR / 1000.;
					if (lbl.getICGxPoint() == Double.NEGATIVE_INFINITY || lbl.getICGbPoint() == Double.NEGATIVE_INFINITY
							|| lbl.getICGcVal() == Double.NEGATIVE_INFINITY || lbl.isICGMissing()
							|| lblLength < 60 * 1000000) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(mvol);
					}
					break;
				case Distance: // electrode distance
					vals[i] = new DataValue(edVal);
					break;
				case Heather: // heather index
					double mt = lbl.getICGcPoint() / 1000000.;
					double hi = -lbl.getICGcVal() / (mt);
					if (lbl.getICGcVal() == Double.NEGATIVE_INFINITY || lbl.getICGcPoint() == Double.NEGATIVE_INFINITY
							|| lbl.isICGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(hi);
					}
					break;
				case Bpos: // B position
					double val = lbl.getICGbPoint() / 1000.;
					if (val == Double.NEGATIVE_INFINITY || lbl.isICGMissing() || lbl.isECGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(val);
					}
					break;
				case Bval: // B value
					val = lbl.getICGbVal();
					if (val == Double.NEGATIVE_INFINITY || lbl.isICGMissing() || lbl.isECGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(-val);
					}
					break;
				case Cpos: // C position
					val = lbl.getICGcPoint() / 1000.;
					if (val == Double.NEGATIVE_INFINITY || lbl.isICGMissing() || lbl.isECGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(val);
					}
					break;
				case Cval: // C value
					val = lbl.getICGcVal();
					if (val == Double.NEGATIVE_INFINITY || lbl.isICGMissing() || lbl.isECGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(-val);
					}
					break;
				case Xpos: // X position
					val = lbl.getICGxPoint() / 1000.;
					if (val == Double.NEGATIVE_INFINITY || lbl.isICGMissing() || lbl.isECGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(val);
					}
					break;
				case Xval: // X value
					val = lbl.getICGxVal();
					if (val == Double.NEGATIVE_INFINITY || lbl.isICGMissing() || lbl.isECGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(-val);
					}
					break;
				case BeatsDisc: // beats discarded
					val = lbl.getNumberOfBeats();
					if (val == 0 || !cod.channelExists("DZDT"))
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(((double) lbl.getNotInAv() / val) * 100);
					break;
				case avSCL: // average SCL
					if (cod.channelExists("SCL")) {
						vl = lbl.getAvSCL();
						if (Double.isNaN(vl)) {
							vals[i] = new DataValue(misVal);
						} else {
							vals[i] = new DataValue(vl);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case minSCL: // min SCL
					if (cod.channelExists("SCL")) {
						if (Double.isNaN(valuesSCL[0])) {
							vals[i] = new DataValue(misVal);
						} else {
							vals[i] = new DataValue(valuesSCL[0]);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case maxSCL: // max SCL
					if (cod.channelExists("SCL")) {
						if (Double.isNaN(valuesSCL[1])) {
							vals[i] = new DataValue(misVal);
						} else {
							vals[i] = new DataValue(valuesSCL[1]);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case nsSCR: // nsSCR
					if (cod.channelExists("SCL")) {
						if (sclcycles == -1)
							vals[i] = new DataValue(misVal);
						else
							vals[i] = new DataValue(sclcycles);
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case nsSCRmin: // nsSCR / minute
					if (cod.channelExists("SCL")) {
						if (sclcycles == -1 || lblLength < 60 * 1000000)
							vals[i] = new DataValue(misVal);
						else
							vals[i] = new DataValue((double) sclcycles * 60. / (lblLength / 1000000.));
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case ArtefSCL: // artefact free SCL
					if (cod.channelExists("SCL")) {
						Double totalTimeSCL = lbl.getTotalTimeUnderLabelSCL();
						vals[i] = new DataValue(((double) totalTimeSCL / lblLength) * 100.);
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case avStep: // average step
					if (cod.channelExists("StepInstances")) {
						vl = lbl.getAvStep();
						if (Double.isNaN(vl)) {
							vals[i] = new DataValue(misVal);
						} else {
							vals[i] = new DataValue(vl);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case minStep: // min step
					if (cod.channelExists("StepInstances")) {
						if (Double.isNaN(valuesSteps[0])) {
							vals[i] = new DataValue(misVal);
						} else {
							vals[i] = new DataValue(valuesSteps[0]);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case maxStep: // max step
					if (cod.channelExists("StepInstances")) {
						if (Double.isNaN(valuesSteps[1])) {
							vals[i] = new DataValue(misVal);
						} else {
							vals[i] = new DataValue(valuesSteps[1]);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case nStep: // step count
					if (cod.channelExists("StepInstances")) {
						if (steps == -1)
							vals[i] = new DataValue(misVal);
						else
							vals[i] = new DataValue(steps);
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case nStepMin: // steps / minute
					if (cod.channelExists("StepInstances")) {
						if (steps == -1 || lblLength < 60 * 1000000)
							vals[i] = new DataValue(misVal);
						else
							vals[i] = new DataValue((double) steps * 60. / (lblLength / 1000000.));
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case avMETBNB: // metabolic equivalent
					Double MET = lbl.getAverageMETBrageN();
					if (MET == null || lblLength < 60 * 1000000)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(MET);
					break;
				case avMETBB: // metabolic equivalent
					Double METB = lbl.getAverageMETBrageB();
					if (METB == null || lblLength < 60 * 1000000)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(METB);
					break;
				case avMETF: // metabolic equivalent
					Double METF = lbl.getAverageMETFreedson();
					if (METF == null || lblLength < 60 * 1000000)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(METF);
					break;
				case avMADxyz: // mean absolute deviation xyz
					Double MADxyz = lbl.getAverageMADxyz();
					if (MADxyz == null || lblLength < 60 * 1000000)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(MADxyz);
					break;
				case avMAD: // mean absolute deviation
					Double MAD = lbl.getAverageMAD();
					if (MAD == null || lblLength < 60 * 1000000)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(MAD);
					break;
				case avSpeech: // mean absolute deviation xyz
					Double speech = lbl.getAverageSpeech();
					if (speech == null || lblLength < 60 * 1000000)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(speech);
					break;
				case avPosture: // mean absolute deviation xyz
					String posture = lbl.getPosture();
					if (posture == null || lblLength < 30 * 1000000)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(posture);
					break;
				case avX: // average X mot
					if (cod.channelExists("MXR") == true) {
						chan = "MXR";
					} else {
						if (cod.channelExists("XMT") == true) {
							chan = "XMT";
						} else {
							vals[i] = new DataValue(misVal);
							break;
						}
					}
					vl = lbl.getAvMot(chan);
					if (Double.isNaN(vl) || vl < 0) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(1000 * vl);
					}
					break;
				case avY: // average Y mot
					if (cod.channelExists("MYR") == true) {
						chan = "MYR";
					} else {
						if (cod.channelExists("YMT") == true) {
							chan = "YMT";
						} else {
							vals[i] = new DataValue(misVal);
							break;
						}
					}
					vl = lbl.getAvMot(chan);
					if (Double.isNaN(vl) || vl < 0) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(1000 * vl);
					}
					break;
				case avZ: // average Z mot
					if (cod.channelExists("MZR") == true) {
						chan = "MZR";
					} else {
						if (cod.channelExists("ZMT") == true) {
							chan = "ZMT";
						} else {
							vals[i] = new DataValue(misVal);
							break;
						}
					}
					vl = lbl.getAvMot(chan);
					if (Double.isNaN(vl) || vl < 0) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(1000 * vl);
					}
					break;
				case Duration: // label duration
					vals[i] = new DataValue((rightTime - leftTime) / 1000000.);
					break;
				case avRSA: // RSA
					Double rsa = lbl.getRSA();
					if (rsa == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(rsa);
					}
					break;
				case sdRSA: // sd RSA
					if (rsaVals[0] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(rsaVals[0]);
					break;
				case minRSA: // min RSA
					if (rsaVals[1] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(rsaVals[1]);
					break;
				case maxRSA: // max RSA
					if (rsaVals[2] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(rsaVals[2]);
					break;
				case avRSA0: // RSA-0
					Double rsa0 = lbl.getRSA0();
					if (rsa0 == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(rsa0);
					}
					break;
				case sdRSA0: // sd RSA-0
					if (rsa0Vals[0] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(rsa0Vals[0]);
					break;
				case minRSA0: // min RSA-0
					if (rsa0Vals[1] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(rsa0Vals[1]);
					break;
				case maxRSA0: // max RSA
					if (rsa0Vals[2] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(rsa0Vals[2]);
					break;
				case avRSAA: // RSA Added
					rsa = lbl.getRSAAdded();
					if (rsa == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(rsa);
					}
					break;
				case sdRSAA: // sd RSA Added
					if (rsaVals[0] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(rsaAddedVals[0]);
					break;
				case minRSAA: // min RSA Added
					if (rsaVals[1] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(rsaAddedVals[1]);
					break;
				case maxRSAA: // max RSA Added
					if (rsaVals[2] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(rsaAddedVals[2]);
					break;
				case avRSA0A: // RSA-0 Added
					rsa0 = lbl.getRSA0Added();
					if (rsa0 == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(rsa0);
					}
					break;
				case sdRSA0A: // sd RSA-0 Added
					if (rsa0Vals[0] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(rsa0AddedVals[0]);
					break;
				case minRSA0A: // min RSA-0 Added
					if (rsa0Vals[1] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(rsa0AddedVals[1]);
					break;
				case maxRSA0A: // max RSA Added
					if (rsa0Vals[2] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(rsa0AddedVals[2]);
					break;
				case avRR: // respiration rate
					Double rr = lbl.getRespirationRate();
					if (rr == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(rr);
					}
					break;
				case sdRR: // sd RR
					if (rrVals[0] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(rrVals[0]);
					break;
				case minRR: // min RR
					if (rrVals[1] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(rrVals[1]);
					break;
				case maxRR: // max RR
					if (rrVals[2] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(rrVals[2]);
					break;
				case avTidal: // tidal volume
					Double tidalVolume = lbl.getTidalVolume();
					if (tidalVolume == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(1000. * tidalVolume);
					}
					break;
				case sdTidal: // sd tidal volume
					if (tidalVolumeVals[0] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(1000. * tidalVolumeVals[0]);
					break;
				case minTidal: // min tidal volume
					if (tidalVolumeVals[1] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(1000. * tidalVolumeVals[1]);
					break;
				case maxTidal: // max tidal volume
					if (tidalVolumeVals[2] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(1000. * tidalVolumeVals[2]);
					break;
				case avTidalRaw: // tidal volume
					tidalVolume = lbl.getTidalVolumeRaw();
					if (tidalVolume == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(1000. * tidalVolume);
					}
					break;
				case sdTidalRaw: // sd tidal volume
					if (tidalVolumeRawVals[0] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(1000. * tidalVolumeRawVals[0]);
					break;
				case minTidalRaw: // min tidal volume
					if (tidalVolumeRawVals[1] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(1000. * tidalVolumeRawVals[1]);
					break;
				case maxTidalRaw: // max tidal volume
					if (tidalVolumeVals[2] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(1000. * tidalVolumeRawVals[2]);
					break;
				case ArtefResp: // artefact free respiration
					Double artefactFree = lbl.getArtefactFreeLength();
					// if (totalTime == 0) {
					// vals[i] = new DataValue(misVal);
					// } else {
					vals[i] = new DataValue(artefactFree * 100.);
					// }
					break;
				case RejectedResp: // artefact free respiration
					Double rejected = lbl.getPercentRejected();
					// if (totalTime == 0) {
					// vals[i] = new DataValue(misVal);
					// } else {
					vals[i] = new DataValue(rejected * 100.);
					// }
					break;
				case avInsp: // inspiration duration
					Double insp = lbl.getInsp();
					if (insp == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(insp / 1000);
					}
					break;
				case sdInsp: // sd insp
					if (inspVals[0] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(inspVals[0] / 1000);
					break;
				case minInsp: // min insp
					if (inspVals[1] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(inspVals[1] / 1000);
					break;
				case maxInsp: // max insp
					if (inspVals[2] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(inspVals[2] / 1000);
					break;
				case avExp: // expiration duration
					Double exp = lbl.getExp();
					if (exp == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(exp / 1000);
					}
					break;
				case sdExp: // sd exp
					if (expVals[0] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(expVals[0] / 1000);
					break;
				case minExp: // min exp
					if (expVals[1] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(expVals[1] / 1000);
					break;
				case maxExp: // max exp
					if (expVals[2] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(expVals[2] / 1000);
					break;
				case avInspExp: // inspiration/expiration ratio
					Double inspExp = lbl.getInspExp();
					if (inspExp == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(inspExp);
					}
					break;
				case sdInspExp: // sd insp/exp
					if (inspExpVals[0] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(inspExpVals[0]);
					break;
				case minInspExp: // min insp/exp
					if (inspExpVals[1] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(inspExpVals[1]);
					break;
				case maxInspExp: // max insp
					if (inspExpVals[2] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(inspExpVals[2]);
					break;
				case avShortest: // shortest IBi inspiration
					Double shortest = lbl.getShortest();
					if (shortest == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(shortest / 1000);
					}
					break;
				case sdShortest: // sd shortest
					if (shortestIBIVals[0] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(shortestIBIVals[0] / 1000);
					break;
				case minShortest: // min shortest
					if (shortestIBIVals[1] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(shortestIBIVals[1] / 1000);
					break;
				case maxShortest: // max shortest
					if (shortestIBIVals[2] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(shortestIBIVals[2] / 1000);
					break;
				case avLongest: // longest IBI expiration
					Double longest = lbl.getLongest();
					if (longest == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(longest / 1000);
					}
					break;
				case sdLongest: // sd longest
					if (longestIBIVals[0] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(longestIBIVals[0] / 1000);
					break;
				case minLongest: // min longest
					if (longestIBIVals[1] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(longestIBIVals[1] / 1000);
					break;
				case maxLongest: // max longest
					if (longestIBIVals[2] == -1)
						vals[i] = new DataValue(misVal);
					else
						vals[i] = new DataValue(longestIBIVals[2] / 1000);
					break;
				case avMot: // total motility
					double ee1 = lbl.getTotalMotility();
					if (ee1 < 0) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(1000. * ee1);
					}
					break;
				case Ext1: // external signal 1
					Double extSig = lbl.getExternalFileAverage();
					if (extSig == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(extSig);
					}
					break;
				case ArtefECG: // artefact free ECG seconds
					// if (totalTime == 0) {
					// vals[i] = new DataValue(misVal);
					// } else {
					vals[i] = new DataValue(totalTime / 1000000.);
					// }
					break;
				case ArtefPerc: // artefact free ECG
					// if (totalTime == 0) {
					// vals[i] = new DataValue(misVal);
					// } else {
					vals[i] = new DataValue(((double) totalTime / lblLength) * 100.);
					// }
					break;
				case avZ0: // average Z0
					if (Double.isNaN(avZ0)) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(avZ0);
					}
					break;
				case sdZ0: // sd Z0
					if (Double.isNaN(sdZ0)) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(sdZ0);
					}
					break;
				case minZ0: // min Z0
					if (cod.channelExists("Z0")) {
						if (Double.isNaN(valuesZ0[0])) {
							vals[i] = new DataValue(misVal);
						} else {
							vals[i] = new DataValue(valuesZ0[0]);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case maxZ0: // max Z0
					if (cod.channelExists("Z0")) {
						if (Double.isNaN(valuesZ0[1])) {
							vals[i] = new DataValue(misVal);
						} else {
							vals[i] = new DataValue(valuesZ0[1]);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case POnsetPos: // P onset position
					double POnsetPoint = lbl.getECGPPoint() / 1000;
					if (lbl.isECGMissing() || lbl.getECGPPoint() == Double.NEGATIVE_INFINITY) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(POnsetPoint);
					}
					break;
				case POnsetVal: // P onset value
					double pValue = lbl.getECGPVal();
					if ((pValue == Double.NEGATIVE_INFINITY) || lbl.isECGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(pValue);
					}
					break;
				case Ppos: // P position
					double PPoint = lbl.getECGPOnsetPoint() / 1000;
					if (lbl.isECGMissing() || lbl.getECGPOnsetPoint() == Double.NEGATIVE_INFINITY) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(PPoint);
					}
					break;
				case Pval: // P value
					double PValue = lbl.getECGPOnsetVal();
					if (PValue == Double.NEGATIVE_INFINITY || lbl.isECGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(PValue);
					}
					break;
				case QOnsetPos: // Q onset position
					double QOnsetPoint = lbl.getECGQPoint() / 1000;
					if (lbl.isECGMissing() || lbl.getECGQPoint() == Double.NEGATIVE_INFINITY
							|| lbl.isECGQOnsetMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(QOnsetPoint);
					}
					break;
				case QOnsetVal: // Q onset value
					double qValue = lbl.getECGQVal();
					if ((qValue == Double.NEGATIVE_INFINITY) || (lbl.isECGQOnsetMissing() || lbl.isECGMissing())) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(qValue);
					}
					break;
				case Qpos: // Q position
					double QPoint = lbl.getECGQOnsetPoint() / 1000;
					if (lbl.isECGMissing() || lbl.getECGQOnsetPoint() == Double.NEGATIVE_INFINITY
							|| lbl.isECGQPointMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(QPoint);
					}
					break;
				case Qval: // Q value
					double QValue = lbl.getECGQOnsetVal();
					if (QValue == Double.NEGATIVE_INFINITY || lbl.isECGQPointMissing() || lbl.isECGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(QValue);
					}
					break;
				case Rval: // R value
					double RValue = lbl.getECGRVal();
					if (RValue == Double.NEGATIVE_INFINITY || lbl.isECGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(RValue);
					}
					break;
				case Spos: // S position
					double SPoint = lbl.getECGSPoint() / 1000;
					if (lbl.isECGMissing() || lbl.getECGSPoint() == Double.NEGATIVE_INFINITY) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(SPoint);
					}
					break;
				case Sval: // S value
					double SValue = lbl.getECGSVal();
					if (SValue == Double.NEGATIVE_INFINITY || lbl.isECGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(SValue);
					}
					break;
				case SOffsetPos: // S offset position
					double SOffsetPoint = lbl.getECGSOffsetPoint() / 1000;
					if (lbl.isECGMissing() || lbl.getECGSOffsetPoint() == Double.NEGATIVE_INFINITY) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(SOffsetPoint);
					}
					break;
				case SOffsetVal: // S offset value
					double SOffsetValue = lbl.getECGSOffsetVal();
					if (SOffsetValue == Double.NEGATIVE_INFINITY || lbl.isECGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(SOffsetValue);
					}
					break;
				case Tpos: // T position
					double TPoint = lbl.getECGTPoint() / 1000;
					if (lbl.isECGMissing() || lbl.getECGTPoint() == Double.NEGATIVE_INFINITY) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(TPoint);
					}
					break;
				case Tval: // T value
					TValue = lbl.getECGTVal();
					if (TValue == Double.NEGATIVE_INFINITY || lbl.isECGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(TValue);
					}
					break;
				case TOffsetPos: // T offset position
					double TOffsetPoint = lbl.getECGTOffsetPoint() / 1000;
					if (lbl.isECGMissing() || lbl.getECGTOffsetPoint() == Double.NEGATIVE_INFINITY) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(TOffsetPoint);
					}
					break;
				case TOffsetVal: // T offset value
					TOffsetValue = lbl.getECGTOffsetVal();
					if (TOffsetValue == Double.NEGATIVE_INFINITY || lbl.isECGMissing()) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(TOffsetValue);
					}
					break;
				case PVC: // PVCs
					int noofPVC = lbl.getNoofPrematureVentricularContractions();
					vals[i] = new DataValue(noofPVC);
					break;
				case PAC: // PACs
					int noofPAC = lbl.getNoofPrematureAtrialContractions();
					vals[i] = new DataValue(noofPAC);
					break;
				case Ext2: // external signal 2
					Double extSig2 = lbl.getExternalFile2Average();
					if (extSig2 == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(extSig2);
					}
					break;
				case Ext3: // external signal 3
					Double extSig3 = lbl.getExternalFile3Average();
					if (extSig3 == null) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(extSig3);
					}
					break;
				case avECG: // ECG
					if (cod.channelExists("ECG")) {
						Double ecg = lbl.getAverageECG();
						if (Double.isNaN(ecg)) {
							vals[i] = new DataValue(misVal);
						} else {
							vals[i] = new DataValue(ecg);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case sdECG: // sd ECG
					if (cod.channelExists("ECG")) {
						double sdECG = lbl.getStdDevECG();
						if (Double.isNaN(sdECG)) {
							vals[i] = new DataValue(misVal);
						} else {
							vals[i] = new DataValue(sdECG);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case minECG: // min ECG
					if (cod.channelExists("ECG")) {
						if (Double.isNaN(valuesECG[0])) {
							vals[i] = new DataValue(misVal);
						} else {
							vals[i] = new DataValue(valuesECG[0]);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case maxECG: // max ECG
					if (cod.channelExists("ECG")) {
						if (Double.isNaN(valuesECG[1])) {
							vals[i] = new DataValue(misVal);
						} else {
							vals[i] = new DataValue(valuesECG[1]);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case avECG2: // ECG2
					if (cod.channelExists("V2ecg")) {
						Double ecg2 = lbl.getAverageECG2();
						if (Double.isNaN(ecg2)) {
							vals[i] = new DataValue(misVal);
						} else {
							vals[i] = new DataValue(ecg2);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case sdECG2: // sd ECG2
					if (cod.channelExists("V2ecg")) {
						double sdECG2 = lbl.getStdDevECG2();
						if (Double.isNaN(sdECG2)) {
							vals[i] = new DataValue(misVal);
						} else {
							vals[i] = new DataValue(sdECG2);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case minECG2: // min ECG2
					if (cod.channelExists("V2ecg")) {
						if (Double.isNaN(valuesECG[0])) {
							vals[i] = new DataValue(misVal);
						} else {
							vals[i] = new DataValue(valuesECG2[0]);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case maxECG2: // max ECG2
					if (cod.channelExists("V2ecg")) {
						if (Double.isNaN(valuesECG2[1])) {
							vals[i] = new DataValue(misVal);
						} else {
							vals[i] = new DataValue(valuesECG2[1]);
						}
					} else {
						vals[i] = new DataValue(misVal);
					}
					break;
				case avRpeak: // average R peak height
					vl = lbl.getAverageRHeight();
					if (vl < 0) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(vl);
					}
					break;
				case sdRpeak: // sd R peak height
					vl = lbl.getStddevRHeight();
					if (vl < 0) {
						vals[i] = new DataValue(misVal);
					} else {
						vals[i] = new DataValue(vl);
					}
					break;
				default:
					int nameCount = allNames.values().length - 1;
					int j = (enabledColumns.get(i).id - nameCount);
					if (j >= config.getCategories().size())
						j = j - config.getCategories().size();
					String cat = lbl.getAttributes().get(config.getCategories().get(j));
					if (cat != null) {
						if (enabledColumns.get(i).header.endsWith("Code")) {
							ArrayList<LabelValue> lvals = config.getValuesForCategory(config.getCategories().get(j));
							vals[i] = new DataValue(misVal);
							for (LabelValue v : lvals) {
								if (v.getName().equals(cat)) {
									vals[i] = new DataValue(v.getCode());
								}
							}
						} else
							vals[i] = new DataValue(cat);
					} else if (rowIndex == 0 && !enabledColumns.get(i).header.endsWith("Code"))
						vals[i] = new DataValue("Total Registration");
					else {
						vals[i] = new DataValue(misVal);
					}
			}
		}
		return vals;

	}

	public String getHeader(int col) {
		if (col < enabledColumns.size()) {
			return enabledColumns.get(col).header;
		}
		if (col < enabledColumns.size() + config.getCategories().size()) {
			return config.getCategories().get(col - enabledColumns.size());
		}
		return config.getCategories().get(col - enabledColumns.size() - config.getCategories().size()) + " Code";
	}

	public int getCol(String header) {
		int ret = -1;
		for (int i = 0; i < enabledColumns.size(); i++) {
			if (header.equals(enabledColumns.get(i).header)) {
				ret = i;
				break;
			}
		}
		return ret;
	}

	private int getMaxStringLengthInColumn(int col) {
		int maxLength = sanitizeString(getHeader(col)).length();
		for (int i = 0; i < nRows; i++) {
			int curLength = data[i][col].getTableValue().toString().length();
			if (curLength > maxLength)
				maxLength = curLength;
		}
		return maxLength;
	}

	public int getNCols() {
		return nCols;
	}

	public int getNRows() {
		return nRows;
	}

	public Object getValue(int row, int col) {
		if (data.length == 0)
			return 0;
		return data[row][col].getTableValue();
	}

	public Object[] getValues(String header) {
		Object[] ret = new Object[nRows];
		int col = getCol(header);
		if (col > 0) {
			for (int i = 0; i < nRows; i++)
				ret[i] = data[i][col].getValue();
		}
		return ret;
	}

	public Thread getRecalcThread() {
		return recalcThread;
	}

	public void loadFromDisk() {
		Gson gson = new Gson();
		LinkedList<DataColumn> tempLList;
		Type collectionType;
		/*
		 * //----------------- Previous code to save the outputconfig.json to Temp
		 * Directory-----------------
		 * File curFile = new
		 * File(AppSettings.getInstance().getProperty(Settings.TEMPDIR));
		 * curFile = new File(curFile, "VU-DAMS/outputconfig.json");
		 */

		// ----------------Code to save outputconfig.json to App Data
		// directory----------------------------
		String OSname = System.getProperty("os.name");
		String path = "";
		if (OSname.contains("Mac")) {
			path = System.getProperty("user.home") + "/Library/Application " + "Support";
		} else if (OSname.contains("Linux")) {
			path = System.getProperty("user.home") + "/.local/share/applications";
		} else {
			path = System.getenv("APPDATA");
		}
		File curFile;
		curFile = new File(path, "VU-DAMS");

		if (curFile.exists() == false) {
			curFile.mkdir();
		}
		curFile = new File(curFile, "outputconfig.json");
		// -------------------------------------------------------------------------------------------------
		if (curFile.exists() == false)
			return;
		String inString;
		inString = Utils.readStringFromFile(curFile);
		if (inString != null) {
			collectionType = new TypeToken<LinkedList<DataColumn>>() {
			}.getType();
			tempLList = gson.fromJson(inString, collectionType);
			DataColumn[] tempList = new DataColumn[tempLList.size()];
			int k = 0;
			for (DataColumn d : tempLList) {
				tempList[k] = d;
				k++;
			}
			int nameCount = allNames.values().length - 1;
			boolean scl = CurrentOpenData.getInstance().channelExists("SCL");
			if (tempList.length == nameCount + 2 * config.getCategories().size() && distinctValues(tempList)) {
				for (DataColumn o : tempList) {
					for (int i = 0; i < columns.length; i++) {
						if (o.id == columns[i].id) {
							if (!scl && (columns[i].header.equals("Average SCL [\u00B5S]")
									|| columns[i].header.equals("nsSCRs per minute [ppm]"))) {
							} else
								columns[i] = o;
						}
					}
				}
			} else { // When more variables are added to the output
				saveToDisk();
			}
		}

	}

	public void outputDataToASCII(File fl) {
		outputDataToASCII(fl, -1, false, false);
	}

	public void outputDataToASCII(File fl, double tW, boolean recalc) {
		outputDataToASCII(fl, tW, recalc, false);
	}

	public void outputDataToASCII(File fl, double tW, boolean recalc, boolean append) {
		recalculate(tW);
		int nRowsPrint = nRows;
		int showEntireData = AppSettings.getInstance().getIntProperty(Settings.SHOWENTIREDATA);
		if (showEntireData == 1 && tW <= 0)
			nRowsPrint--; // don't export row with entire registration
		PrintWriter write = null;
		if (recalcThread != null) {
			try {
				recalcThread.join();
			} catch (InterruptedException e) {
			}
		}
		try {
			write = new PrintWriter(new BufferedWriter(new FileWriter(fl, append)));
			String line = "";
			int[] maxLengths = new int[nCols];
			for (int i = 0; i < nCols; i++)
				maxLengths[i] = getMaxStringLengthInColumn(i);
			if (fl.length() == 0) {// header only when starting with empty file
				for (int i = 0; i < nCols; i++) {
					String headerVal = sanitizeString(getHeader(i));
					String value = new String(headerVal);
					while (value.length() <= maxLengths[i])
						value += " ";
					value += "\t";
					line += value;
				}
				write.println(line);
			}
			for (int i = 0; i < nRowsPrint; i++) {
				line = "";
				for (int j = 0; j < nCols; j++) {
					String value = "";
					value += data[i][j].getTableValue().toString();
					while (value.length() <= maxLengths[j])
						value += " ";
					value += "\t";
					line += value;
				}
				write.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (write != null) {
				write.close();
			}
		}
		if (recalc)
			recalculate();
	}

	public void outputDataToXLS(File fl) {
		outputDataToXLS(fl, -1, false);
	}

	public void outputDataToXLS(File fl, boolean recalc) {
		outputDataToXLS(fl, -1, recalc);
	}

	public void outputDataToXLS(File fl, double tW, boolean recalc) {
		recalculate(tW);
		int nRowsPrint = nRows;
		int showEntireData = AppSettings.getInstance().getIntProperty(Settings.SHOWENTIREDATA);
		if (showEntireData == 1 && tW <= 0)
			nRowsPrint--; // don't export row with entire registration
		if (recalcThread != null) {
			try {
				recalcThread.join();
			} catch (InterruptedException e) {
			}
		}
		try {
			FileOutputStream fos = new FileOutputStream(fl);
			Workbook wb = new HSSFWorkbook();
			Sheet sh = wb.createSheet("AMSdata");
			Row row = sh.createRow(0);
			for (int i = 0; i < nCols; i++) {
				String headerVal = sanitizeString(getHeader(i));
				row.createCell(i, CellType.STRING).setCellValue(headerVal);
			}
			for (int i = 0; i < nRowsPrint; i++) {
				row = sh.createRow(i + 1);
				for (int j = 0; j < nCols; j++) {
					Object val = data[i][j].getValue();
					if (val instanceof java.lang.String) {
						row.createCell(j, CellType.STRING).setCellValue((String) val);
					} else {
						row.createCell(j, CellType.NUMERIC).setCellValue(((Number) val).doubleValue());
					}
				}
			}
			for (int i = 0; i < nCols; i++)
				sh.autoSizeColumn(i);
			wb.write(fos);
			wb.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (recalc)
			recalculate();
	}

	public void recalculate() {
		recalculate(-1, true);
	}

	public void recalculate(double tW) {
		recalculate(tW, true);
	}

	public void recalculate(double tW, boolean calcAll) {

		CurrentOpenData cod = CurrentOpenData.getInstance();
		AmsLabelSet ls = cod.getLabels();
		if (tW > 0) {
			AmsLabelSet curLab = cod.getLabels();
			ls = new AmsLabelSet();
			double curTime = cod.getStartTimeInUS();
			double endTime = cod.getEndTimeInUS();
			while (curTime + tW < endTime) {
				AmsLabel lab = new AmsLabel(curTime, curTime + tW, true, 0.0);
				AmsLabel llab = curLab.getLabelUnderTime(curTime);
				AmsLabel rlab = curLab.getLabelUnderTime(curTime + tW);
				if (llab != null && rlab != null) {
					if (llab.equals(rlab)) {
						for (Entry<String, String> entry : llab.getAttributes().entrySet()) {
							lab.getAttributes().put(entry.getKey(), entry.getValue());
						}
					}
				}
				ls.add(lab);
				curTime += tW;
			}
			calcAll = true;
		}

		try {
			if (CurrentOpenData.getInstance().getLoadLabelThread() != null)
				CurrentOpenData.getInstance().getLoadLabelThread().join();
			if (CurrentOpenData.getInstance().getTimeLabelThread() != null)
				CurrentOpenData.getInstance().getTimeLabelThread().join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		List<AmsLabel> list = new ArrayList<AmsLabel>(ls.getLabels());
		for (int i = 0; i < list.size(); i++) {
			AmsLabel l = list.get(i);
			double leftTime = l.getLeftTime();
			double rightTime = l.getRightTime();
			if ((leftTime < cod.getStartTimeInUS() && rightTime < cod.getStartTimeInUS())
					|| (leftTime > cod.getEndTimeInUS() && rightTime > cod.getEndTimeInUS()))
				ls.removeLabel(l);
			else if (leftTime < cod.getStartTimeInUS())
				l.setLeftTime(cod.getStartTimeInUS());
			else if (rightTime > cod.getEndTimeInUS())
				l.setRightTime(cod.getEndTimeInUS());
		}

		config = cod.getLabelConfig();

		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(2);
		nf.setGroupingUsed(false);

		enabledColumns.clear();
		java.util.Arrays.sort(columns, byId);

		Set<Integer> intSet = new TreeSet<Integer>();
		for (DataColumn dc : columns)
			if (dc.enabled)
				intSet.add(dc.pos);

		for (int i : intSet) {
			for (DataColumn dc : columns)
				if (dc.pos == i && dc.enabled)
					enabledColumns.add(dc);
		}

		int showEntireData = AppSettings.getInstance().getIntProperty(Settings.SHOWENTIREDATA);

		nRows = ls.getLabels().size();
		if (showEntireData == 1 && tW <= 0)
			nRows++;
		nCols = enabledColumns.size();// + 2 * config.getCategories().size();
		int totalCols = columns.length;

		data = new DataValue[nRows][nCols];
		if (calcAll) {
			dataCopy = new DataValue[nRows][totalCols];
			for (int i = 0; i < nRows; i++) {
				for (int j = 0; j < nCols; j++) {
					data[i][j] = new DataValue("Calculating...");
				}
			}

			// int misVal = AppSettings.getInstance().getIntProperty(Settings.MISSINGVALUE);

			ArrayList<DataColumn> listColumns = new ArrayList<DataColumn>();
			Collections.addAll(listColumns, columns);
			if (recalcThread != null) {
				// recalcThread.interrupt();
				try {
					recalcThread.join();
				} catch (InterruptedException e) {
				}
				ThreadServer.removeThread(recalcThread);
			}
			final AmsLabelSet lsr = ls;
			recalcThread = new Thread() {
				@Override
				public void run() {
					int q = 0;
					for (AmsLabel l : lsr.getLabels()) {
						/*
						 * for (int i = 0; i < enabledColumns.size(); i++) { data[q][i] =
						 * getDataValue(enabledColumns.get(i).id, l, k); }
						 */
						DataValue[] datVals = getDataValues(l, enabledColumns, q + 1);
						for (int i = 0; i < enabledColumns.size(); i++) {
							dataCopy[q][columns[i].id] = datVals[i];
						}
						for (int j = 0; j < nCols; j++) {
							data[q][j] = datVals[j];
						}
						q++;
					}
					if (showEntireData == 1 && tW <= 0) {

						AmsLabel lbl = new AmsLabel(cod.getStartTimeInUS(), cod.getEndTimeInUS(), false, 0.0, "");
						DataValue[] datVals = getDataValues(lbl, listColumns, 0);

						for (int i = 0; i < listColumns.size(); i++) {
							// data[0][i] = getDataValue(enabledColumns.get(i).id, lbl, 0);
							dataCopy[q][columns[i].id] = datVals[i];
						}
						for (int j = 0; j < nCols; j++) {
							data[q][j] = datVals[enabledColumns.get(j).id];
						}
					}
					java.util.Arrays.sort(columns, byPosEnable);
					MainFrame.getInstance().getMainFrame().repaint();
				}
			};
			ThreadServer.addNewThread(recalcThread);
			recalcThread.start();
		} else {
			for (int i = 0; i < nRows; i++) {
				for (int j = 0; j < nCols; j++) {
					data[i][j] = dataCopy[i][enabledColumns.get(j).id];
				}
			}
			java.util.Arrays.sort(columns, byPosEnable);
		}
	}

	private void recalculateSimulation() {
		CurrentOpenData cod = CurrentOpenData.getInstance();

		AmsLabelSet ls = cod.getLabels();

		config = cod.getLabelConfig();

		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(2);
		nf.setGroupingUsed(false);

		enabledColumns.clear();

		Set<Integer> intSet = new TreeSet<Integer>();
		for (DataColumn dc : columns)
			if (dc.enabled)
				intSet.add(dc.pos);

		for (int i : intSet) {
			for (DataColumn dc : columns)
				if (dc.pos == i && dc.enabled)
					enabledColumns.add(dc);
		}

		int showEntireData = AppSettings.getInstance().getIntProperty(Settings.SHOWENTIREDATA);

		nRows = ls.getLabels().size();
		if (showEntireData == 1)
			nRows++;
		nCols = enabledColumns.size();// + 2 * config.getCategories().size();

		data = new DataValue[nRows][nCols];
		for (int i = 0; i < nRows; i++) {
			for (int j = 0; j < nCols; j++) {
				data[i][j] = new DataValue("N/A");
			}
		}
	}

	private String sanitizeString(String header) {
		String str = header;
		str = str.trim();
		str = str.replaceAll("[^\\p{ASCII}]", "");
		str = str.replace(" ", "_");
		str = str.replace("[", "");
		str = str.replace("]", "");
		str = str.replace("!", "");
		str = str.replace("?", "");
		str = str.replace("*", "");
		str = str.replace("/", "");
		str = str.replace("-", "");
		return str;
	}

	public void saveToDisk() {
		/*
		 * //----------------- Previous code to save the outputconfig.json to Temp
		 * Directory-----------------
		 * File file = new
		 * File(AppSettings.getInstance().getProperty(Settings.TEMPDIR));
		 * file = new File(file, "VU-DAMS/outputconfig.json");
		 */

		// ----------------Code to save outputconfig.json to App Data
		// directory----------------------------
		String OSname = System.getProperty("os.name");
		String path = "";
		if (OSname.contains("Mac")) {
			path = System.getProperty("user.home") + "/Library/Application " + "Support";
		} else if (OSname.contains("Linux")) {
			path = System.getProperty("user.home") + "/.local/share/applications";
		} else {
			path = System.getenv("APPDATA");
		}
		File file;
		file = new File(path, "VU-DAMS");

		if (file.exists() == false) {
			file.mkdir();
		}
		file = new File(file, "outputconfig.json");
		// -------------------------------------------------------------------------------------------------

		PrintWriter writer = null;
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
			writer.print(gson.toJson(columns));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	public void setAll() {
		if (recalcThread != null) {
			try {
				recalcThread.join();
			} catch (InterruptedException e) {
			}
		}
		int nameCount = allNames.values().length - 1;
		if (config == null)
			config = CurrentOpenData.getInstance().getLabelConfig();
		columns = new DataColumn[nameCount + 2 * config.getCategories().size()];
		for (int i = 0; i < columns.length; i++) {
			if (i < nameCount)
				columns[i] = new DataColumn(allNames.values()[i].getName(), i, true);
			else if (i < nameCount + config.getCategories().size())
				columns[i] = new DataColumn(config.getCategories().get(i - nameCount), i, true);
			else
				columns[i] = new DataColumn(
						config.getCategories().get(i - nameCount - config.getCategories().size()) + " Code", i, true);
		}

	}

	public void setDefault() {
		setAll();
		int j = 0;
		int nameCount = allNames.values().length - 1;
		boolean scl = CurrentOpenData.getInstance().channelExists("SCL");
		for (int i = nameCount; i < columns.length; i++) {
			columns[i].pos = j;
			j++;
		}
		for (int i = 0; i < nameCount; i++) {
			columns[i].pos = columns[i].pos + j;
		}
		for (int i = 0; i < nameCount; i++) {
			int pos = find(factoryNames, columns[i].header);
			if (!scl && (columns[i].header.equals("Average SCL [\u00B5S]")
					|| columns[i].header.equals("nsSCRs per minute [ppm]")))
				pos = -1;
			if (pos == -1)
				disableColumn(columns[i].id);
			else
				moveColumn(columns[i].id, pos + j - columns[i].pos);
			java.util.Arrays.sort(columns, byId);
		}
		distinctValues(columns);
		/*
		 * for (int i = 0; i < columns.length; i++) {
		 * System.out.println("Default col " + i + ": " + columns[i].toString());
		 * }
		 */
		/*
		 * for (DataColumn dc : columns)
		 * sList.add(dc);
		 * 
		 * columns[0] = new DataColumn(allNames[0], 0, true); columns[1] =
		 * new DataColumn(allNames[1], 1, true); columns[2] = new
		 * DataColumn(allNames[2], 2, true); columns[3] = new
		 * DataColumn(allNames[3], 3, true); columns[4] = new
		 * DataColumn("End Date", 4, true); columns[5] = new
		 * DataColumn("End Time", 5, true); columns[6] = new
		 * DataColumn("Number of IBIs", 6, true); columns[7] = new
		 * DataColumn("Average IBI [msec]", 7, true); columns[8] = new
		 * DataColumn("StdDev IBI [msec]", 8, true); columns[9] = new
		 * DataColumn("Min IBI [msec]", 9, true); columns[10] = new
		 * DataColumn("Max IBI [msec]", 10, true); columns[11] = new
		 * DataColumn("Average HR [bpm]", 11, true); columns[12] = new
		 * DataColumn("StdDev HR [bpm]", 12, true); columns[13] = new
		 * DataColumn("Min HR [bpm]", 13, true); columns[14] = new
		 * DataColumn("Max HR [bpm]", 14, true); columns[15] = new
		 * DataColumn("RMSSD [msec]", 15, true); columns[16] = new
		 * DataColumn("Average Mot [mg]", 16, true); columns[17] = new
		 * DataColumn("LF [ms\u00B2]", 17, true); columns[18] = new
		 * DataColumn("HF [ms\u00B2]", 18, true); columns[19] = new
		 * DataColumn("LF/HF", 19, true); columns[20] = new
		 * DataColumn("PEP [msec]",20,true);
		 */
	}

	public static boolean distinctValues(DataColumn[] arr) {
		Set<Integer> foundNumbers = new HashSet<Integer>();
		for (int i = 0; i < arr.length; i++) {
			int num = arr[i].pos;
			if (num < 0 || num >= arr.length) {
				return false;
			}
			if (foundNumbers.contains(num)) {
				return false;
			}
			foundNumbers.add(num);
		}
		return true;
	}

	public void disableColumn(int col) {
		int colId = columns[col].id;
		boolean enabled = columns[col].enabled;
		java.util.Arrays.sort(columns, byPos);
		if (!enabled)
			return;
		boolean movePos = false;
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].id == colId) {
				columns[i].pos = columns.length - 1;
				// System.out.println("Disable col " + i + ": " + columns[i].toString());
				movePos = true;
				continue;
			}
			if (movePos) {
				columns[i].pos -= 1;
				// System.out.println("Disable col " + i + ": " + columns[i].toString());
			}
		}
		// java.util.Arrays.sort(columns, byId);
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].id == colId) {
				columns[i].enabled = false;
				break;
			}
		}
		distinctValues(columns);
	}

	public void enableColumn(int col) {
		int colId = columns[col].id;
		int tempPos = columns[col].pos, tempPos2 = -1;
		boolean enabled = columns[col].enabled;
		java.util.Arrays.sort(columns, byPos);
		if (enabled)
			return;
		boolean movePos = false;
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].enabled == false && movePos == false && columns[i].pos != tempPos) {
				tempPos2 = columns[i].pos;
				columns[i].pos = tempPos;
				// System.out.println("Enable col " + i + ": " + columns[i].toString());
				movePos = true;
				break;
			}
		}
		if (movePos) // stays false when all columns are enabled
			for (int i = 0; i < columns.length; i++) {
				if (columns[i].id == colId) {
					columns[i].pos = tempPos2;
					// System.out.println("Enable col " + i + ": " + columns[i].toString());
					break;
				}
			}
		// java.util.Arrays.sort(columns, byId);
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].id == colId) {
				columns[i].enabled = true;
				break;
			}
		}
		distinctValues(columns);
	}

	public void moveColumn(int column, int positions) {
		if (positions == 0)
			return;
		int oldPos = columns[column].pos;
		java.util.Arrays.sort(columns, byPosEnable);
		if (positions > 0) {
			for (int i = 0; i < columns.length; i++) {
				if (columns[i].pos == oldPos) {
					columns[i].pos = oldPos + positions;
					// System.out.println("Move col " + i + ": " + columns[i].toString());
					continue;
				}
				if (columns[i].pos > oldPos && columns[i].pos <= oldPos + positions) {
					columns[i].pos -= 1;
					// System.out.println("Move col " + i + ": " + columns[i].toString());
				}
			}
		}
		if (positions < 0) {
			for (int i = 0; i < columns.length; i++) {
				if (columns[i].pos >= oldPos + positions && columns[i].pos < oldPos) {
					columns[i].pos += 1;
					// System.out.println("Moveb col " + i + ": " + columns[i].toString());
					continue;
				}
				if (columns[i].pos == oldPos) {
					columns[i].pos = oldPos + positions;
					// System.out.println("Moveb col " + i + ": " + columns[i].toString());
				}
			}
		}
		distinctValues(columns);
		// java.util.Arrays.sort(columns, byId);
	}

	public void updateLabelConfig() {
		config = CurrentOpenData.getInstance().getLabelConfig();
		nCols = enabledColumns.size();
		updateColumns();
		// recalculate(-1.0);// JdH 20120710 Force recalculation after label.cfg import
		// to prevent hanging
		// when saving right after importing a label configuration with a higher number
		// of categories/levels
		LabelInformationTab.getInstance().setUpTable();
		MainFrame.getInstance().getMainFrame().repaint();
	}

	private void updateColumns() {
		if (recalcThread != null) {
			try {
				recalcThread.join();
			} catch (InterruptedException e) {
			}
		}
		java.util.Arrays.sort(columns, byId);
		DataColumn[] tempColumns = new DataColumn[columns.length];
		int catSize = 2 * config.getCategories().size();
		int nameCount = allNames.values().length - 1;
		int catSizeOld = columns.length - nameCount;
		int diff = catSizeOld - catSize;
		int[] freePos = new int[catSize];
		for (int i = 0; i < columns.length; i++) {
			tempColumns[i] = columns[i];
			if (i >= nameCount && i - nameCount < catSize)
				freePos[i - nameCount] = columns[i].pos;
		}
		columns = new DataColumn[nameCount + 2 * config.getCategories().size()];
		if (diff < 0) // categories have been added
			for (int j = 0; j < columns.length - tempColumns.length; j++)
				freePos[freePos.length + diff + j] = tempColumns.length + j;
		for (int i = 0; i < columns.length; i++) {
			if (i < nameCount) {
				columns[i] = tempColumns[i];
				if (diff > 0) // categories have been removed
					columns[i].pos -= diff;
			} else if (i < nameCount + config.getCategories().size())
				columns[i] = new DataColumn(config.getCategories().get(i - nameCount), i, true, freePos[i - nameCount]);
			else
				columns[i] = new DataColumn(
						config.getCategories().get(i - nameCount - config.getCategories().size()) + " Code", i, true,
						freePos[i - nameCount]);
		}
		distinctValues(columns);
		// java.util.Arrays.sort(columns, byPos);
	}

	public static int find(String[] a, String target) {
		for (int i = 0; i < a.length; i++)
			if (a[i] == target)
				return i;

		return -1;
	}
}

enum allNames {
	Subj("Subject ID"), Label("Label ID"), Date1("Start Date"), Time1("Start Time"), Date2("End Date"),
	Time2("End Time"),
	IBI("Number of IBIs"), avIBI("Average IBI [msec]"), sdIBI("SDNN [msec]"), minIBI("Min IBI [msec]"),
	maxIBI("Max IBI [msec]"),
	avHR("Average HR [bpm]"), sdHR("StdDev HR [bpm]"), minHR("Min HR [bpm]"), maxHR("Max HR [bpm]"),
	RMSSD("RMSSD [msec]"),
	MYA("Average Mot [mg]"), LF("LF [ms\u00B2]"), HF("HF [ms\u00B2]"), LFHF("LF/HF"),
	PEP("PEP [msec]"), LVET("LVET [msec]"), TWave("T-Wave amplitude [mV]"), QT("QT Interval [msec]"),
	Stroke1("Stroke Volume (Kubicek 1966) [cc]"), Stroke2("Stroke Volume (Nederend 2017) [cc]"),
	Minute1("Minute Volume (Kubicek 1966) [l/min]"), Minute2("Minute Volume (Nederend 2017) [l/min]"),
	Distance("Electrode distance [cm]"), Heather("Heather Index [\u2126/s*s]"),
	Bpos("B-Point position [msec]"), Bval("B-Point value [\u2126/sec]"),
	Cpos("dZ/dt min position [msec]"), Cval("dZ/dt min value [\u2126/sec]"),
	Xpos("X-Point position [msec]"), Xval("X-Point value [\u2126/sec]"), BeatsDisc("Beats Discarded ICG [percent]"),
	avSCL("Average SCL [\u00B5S]"), minSCL("Min SCL [\u00B5S]"), maxSCL("Max SCL [\u00B5S]"), nsSCR("nsSCR [count]"),
	nsSCRmin("nsSCRs per minute [ppm]"), ArtefSCL("Artefact Free SCL [percent]"),
	avStep("Average step impact [g]"), minStep("Min step impact [g]"), maxStep("Max step impact [g]"),
	nStep("steps [count]"), nStepMin("steps per minute [spm]"), avMETBNB("MET BrageNB []"), avMETBB("MET BrageB []"),
	avMETF("MET Freedson []"),
	avMADxyz("MAD xyz [millig]"), avMAD("MAD [millig]"), avSpeech("Speech"), avPosture("Posture"),
	avX("Average X Motility [mg]"), avY("Average Y Motility [mg]"), avZ("Average Z Motility [mg]"),
	Duration("Label Duration [s]"),
	avRSA("RSA [msec]"), sdRSA("StdDev RSA [msec]"), minRSA("Min RSA [msec]"), maxRSA("Max RSA [msec]"),
	avRSA0("RSA-0 [msec]"), sdRSA0("StdDev RSA-0 [msec]"), minRSA0("Min RSA-0 [msec]"), maxRSA0("Max RSA-0 [msec]"),
	avRSAA("RSA Added [msec]"), sdRSAA("StdDev RSA Added [msec]"), minRSAA("Min RSA Added [msec]"),
	maxRSAA("Max RSA Added [msec]"),
	avRSA0A("RSA-0 Added [msec]"), sdRSA0A("StdDev RSA-0 Added [msec]"), minRSA0A("Min RSA-0 Added [msec]"),
	maxRSA0A("Max RSA-0 Added [msec]"),
	avRR("Respiration Rate [bpm]"), sdRR("StdDev RR [bpm]"), minRR("Min RR [bpm]"), maxRR("Max RR [bpm]"),
	avTidal("Tidal Volume [m\u2126]"), sdTidal("StdDev Tidal [m\u2126]"), minTidal("Min Tidal [m\u2126]"),
	maxTidal("Max Tidal [m\u2126]"),
	avTidalRaw("Tidal Volume Raw [m\u2126]"), sdTidalRaw("StdDev Tidal Raw [m\u2126]"),
	minTidalRaw("Min Tidal Raw [m\u2126]"),
	maxTidalRaw("Max Tidal Raw [m\u2126]"), ArtefResp("Artefact Free Respiration [percent]"),
	RejectedResp("Rejected breaths [percent]"),
	avInsp("Inspiration Duration [ms]"), sdInsp("StdDev Inspiration [ms]"), minInsp("Min Inspiration [ms]"),
	maxInsp("Max Inspiration [ms]"),
	avExp("Expiration Duration [ms]"), sdExp("StdDev Expiration [ms]"), minExp("Min Exppiration [ms]"),
	maxExp("Max Exspiration [ms]"),
	avInspExp("Insp/exp ratio"), sdInspExp("StdDev Insp/exp"), minInspExp("Min Insp/exp"), maxInspExp("Max Insp/exp"),
	avShortest("Shortest IBI (inspiration) [ms]"), sdShortest("StdDev Shortest IBI [ms]"),
	minShortest("Min Shortest IBI [ms]"), maxShortest("Max Shortest IBI [ms]"),
	avLongest("Longest IBI (expiration) [ms]"), sdLongest("StdDev Longest IBI [ms]"),
	minLongest("Min Longest IBI [ms]"), maxLongest("Max Longest IBI [ms]"),
	avMot("Total Motility [mg]"), Ext1("External Signal Average"),
	ArtefECG("Artefact Free ECG [s]"), ArtefPerc("Artefact Free ECG [percent]"), avZ0("Z0 Average [\u2126]"),
	sdZ0("Z0 StdDev [\u2126]"), minZ0("Min Z0 (\u2126)"), maxZ0("Max Z0 (\u2126)"),
	POnsetPos("P-Onset point [msec]"), POnsetVal("P-Onset-value [mV]"),
	Ppos("P-Point [msec]"), Pval("P-value [mV]"),
	QOnsetPos("Q-Onset point [msec]"), QOnsetVal("Q-Onset-value [mV]"),
	Qpos("Q-Point [msec]"), Qval("Q-value [mV]"),
	Rval("R-Value [mV]"),
	Spos("S-Point [msec]"), Sval("S-Value [mV]"),
	SOffsetPos("S-Offset point [msec]"), SOffsetVal("S-Offset Val [mV]"),
	Tpos("T-Point [msec]"), Tval("T-Value [mV]"),
	TOffsetPos("T-Offset point [msec]"), TOffsetVal("T-Offset Val [mV]"),
	PVC("No of Premature Ventricular Contractions"), PAC("No of Premature Atrial Contractions"),
	Ext2("External Signal Average_2"), Ext3("External Signal Average_3"),
	avECG("ECG Average [mV]"), sdECG("ECG StdDev [mV]"), minECG("Min ECG (mV)"), maxECG("Max ECG (mV)"),
	avECG2("ECG2 Average [mV]"), sdECG2("ECG2 StdDev [mV]"), minECG2("Min ECG2 (mV)"), maxECG2("Max ECG2 (mV)"),
	avRpeak("R peak height Average (mV)"), sdRpeak("R peak height StdDev (mV)"),
	CAT("Label Categories");

	private String title;

	allNames(String title) {
		this.title = title;
	}

	public String getName() {
		return title;
	}
	// ****** Reverse Lookup ************//

	public static Optional<allNames> get(String title) {
		return Arrays.stream(allNames.values())
				.filter(env -> env.title.equals(title))
				.findFirst();
	}
}
