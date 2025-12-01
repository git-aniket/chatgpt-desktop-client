package nl.vu.psy.ams.suite.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
//import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import com.google.gson.Gson;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.files.Ams5fsFile;
import nl.vu.psy.ams.suite.data.files.Ams7fsFile;
import nl.vu.psy.ams.suite.data.files.AmsAMSFile;
// import nl.vu.psy.ams.suite.data.files.BinDirTest;
import nl.vu.psy.ams.suite.data.files.AmsDataFile;
import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.sets.BinaryFileSet;
import nl.vu.psy.ams.suite.device.AmsDevice;
import nl.vu.psy.ams.suite.device7.AmsDevice7;
import nl.vu.psy.ams.suite.gui.settings.SettingsDialog;
import nl.vu.psy.ams.suite.gui.tabs.actigraph.ActigraphTab;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
// import nl.vu.psy.ams.suite.tools.ExitApp;
import nl.vu.psy.ams.suite.tools.FileChooserWithSavedDir;
import nl.vu.psy.ams.suite.tools.FileDialogWithSavedDir;
import nl.vu.psy.ams.suite.tools.TestFunction;
import nl.vu.psy.ams.suite.tools.Utils;
import nl.vu.psy.ams.suite.tools.VUDAMSDebugSettings;

/**
 * The main menubar on the top of the screen. Button logic is also provided here
 * (should be refactored out for nicer source code). Is a normal menubar for
 * most items, except for the Action menu, which is changed each time the user
 * changes tabs.
 */
public class MainMenuBar implements ActionListener {

	public static MainMenuBar getInstance() {
		if (instance == null) {
			instance = new MainMenuBar();
		}
		return instance;
	}

	private JMenuBar menuBar;

	private JMenu actionMenu;

	private JMenu fileMenu;

	private JMenu helpMenu;

	private JMenu editMenu;

	private JMenuItem saveItem;

	private JMenuItem saveAsItem;

	// private JMenuItem batchConvertItem;

	// private JMenuItem batchanalyze;

	// private JMenuItem merge7fsItem;

	private JMenuItem recInfoItem;

	// private JMenuItem quickOpenItem;

	private JMenuItem clearExtItem;

	private JMenuItem externalItem;

	private JMenuItem exportSignal;

	private JMenuItem exportSignalEDF;

	private JMenuItem exportSignalBin;

	private JMenuItem addPhysiologicalData;

	private JMenuItem importActigraphData;

	private static MainMenuBar instance;

	private long timeintervalinsec;

	private File file1;
	private File file2;
	private File file3;
	private File actigraphfile;

	boolean clear1;
	boolean clear2;
	boolean clear3;
	boolean clear; // clear ALL

	/**
	 * Instantiates a new main menu bar.
	 */
	private MainMenuBar() {
		menuBar = new JMenuBar();
		String newtitle = "";
		if (Utils.isIsprerelease()) {
			menuBar.setBackground(Color.RED);
			newtitle = MainFrame.getInstance().getMainFrame().getTitle() + "_" + "beta_version";
			MainFrame.getInstance().getMainFrame().setTitle(newtitle);
		} else {
			newtitle = MainFrame.getInstance().getMainFrame().getTitle();
			MainFrame.getInstance().getMainFrame().setTitle(newtitle);
		}
		addItems();

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String comm = e.getActionCommand();

		if (comm.equals("Open processed data")) {
			if (CurrentOpenData.getInstance().isOpen()) {
				CurrentOpenData.getInstance().Close();
			}
			String OSname = System.getProperty("os.name");
			File fl = null;
			if (!OSname.contains("Mac")) {
				FileChooserWithSavedDir fc = new FileChooserWithSavedDir();
				fc.setFileFilter(new FileFilter() {
					@Override
					public boolean accept(File f) {
						if (f.isDirectory())
							return true;
						String ext = Utils.getExtension(f).toLowerCase();
						if (ext == null)
							return false;
						if (ext.equals("5fs") || ext.equals("amsdata") || ext.equals("amsdatai") || ext.equals("ams")
								|| ext.equals("7fs"))
							return true;
						return false;
					}

					@Override
					public String getDescription() {
						return "VU-AMS data";
					}
				});
				int retVal = fc.showOpenDialog(MainFrame.getInstance().getMainFrame());
				if (retVal == JFileChooser.APPROVE_OPTION)
					fl = fc.getSelectedFile();
			} else {
				FileDialogWithSavedDir fd = new FileDialogWithSavedDir(MainFrame.getInstance().getMainFrame());
				fd.setFilenameFilter(new FilenameFilter() {
					@Override
					public boolean accept(File f, String s) {
						s = s.toLowerCase();
						if (s.endsWith("5fs") || s.endsWith("amsdata") || s.endsWith("amsdatai") || s.endsWith("ams")
								|| s.endsWith("7fs"))
							return true;
						return false;
					}
				});
				fd.setVisible(true);
				if (fd.getFile() != null) {
					String filename = fd.getDirectory() + File.separator + fd.getFile();
					fl = new File(filename);
				}
			}
			if (fl != null) {
				if (Utils.getExtension(fl).equals("5fs")) {
					File amsDataFile = new File(Utils.removeExtension(fl.getAbsolutePath()) + ".amsdata");
					if (amsDataFile.exists()) {
						int retVal2 = JOptionPane
								.showConfirmDialog(
										MainFrame.getInstance().getMainFrame(),
										"There is an existing .amsdata for this 5fs file. All previous scoring is saved in this file.\nWould you like to open the .amsdata file instead of the .5fs file?",
										"Existing .amsdata file found!", JOptionPane.YES_NO_OPTION);
						if (retVal2 == JOptionPane.YES_OPTION) {
							AmsDataFile datafile = new AmsDataFile(false, amsDataFile.getAbsolutePath());
							datafile.start();
						} else {
							Ams5fsFile file = new Ams5fsFile(fl.getAbsolutePath());
							file.start();
						}
					} else {
						Ams5fsFile file = new Ams5fsFile(fl.getAbsolutePath());
						file.start();
					}
				} else if (Utils.getExtension(fl).equals("7fs")) {
					File amsDataFile = new File(Utils.removeExtension(fl.getAbsolutePath()) + ".amsdatai");
					Ams7fsFile file = new Ams7fsFile(fl.getAbsolutePath());
					try {
						File parentDir = new File(amsDataFile.getParent());
						String mergedFile[] = new String[1];
						ArrayList<File> toMerge = Merge7fsFilesDialog.listMergeFiles(parentDir, mergedFile);
						if (toMerge.size() > 1 && toMerge.get(0).equals(fl)) {
							Object[] options = { "Yes, please", "No, use single data file" };
							int res = JOptionPane.showOptionDialog(MainFrame.getInstance().getMainFrame(),
									"Your selected input file is the first of a larger recording.\n Do you want to merge all files of the recording and open the merged file?",
									"Multiple 7fs files found", JOptionPane.YES_NO_OPTION,
									JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
							if (res == JOptionPane.YES_OPTION) {
								file = new Ams7fsFile(toMerge, amsDataFile);
							}
						}
					} catch (Exception e1) {
						e1.printStackTrace();
					} finally {
						if (amsDataFile.exists()) {
							int retVal2 = JOptionPane
									.showConfirmDialog(
											MainFrame.getInstance().getMainFrame(),
											"There is an existing .amsdatai for this 7fs file. All previous scoring is saved in this file.\nWould you like to open the .amsdatai file instead of the .7fs file?",
											"Existing .amsdatai file found!", JOptionPane.YES_NO_OPTION);
							if (retVal2 == JOptionPane.YES_OPTION) {
								AmsDataFile datafile = new AmsDataFile(false, amsDataFile.getAbsolutePath());
								datafile.start();
							} else {
								file.start();
							}
						} else {
							file.start();
						}
					}
				} else if (Utils.getExtension(fl).equals("amsdata") || Utils.getExtension(fl).equals("amsdatai")) {
					AmsDataFile datafile = new AmsDataFile(false, fl.getAbsolutePath());
					datafile.start();
				} else if (Utils.getExtension(fl).equals("ams")) {
					AmsAMSFile datafile = new AmsAMSFile(fl);
					datafile.start();
				}

			}
		} else if (comm.equals("Import raw data")) {
			if (CurrentOpenData.getInstance().isOpen()) {
				CurrentOpenData.getInstance().Close();
			}
			Import7fsFilesDialog mdg = new Import7fsFilesDialog();
			mdg.setVisible(true);
			// } else if (comm.equals("Open bin dir (test)")) {
			// if (CurrentOpenData.getInstance().isOpen()) {
			// int result;
			// if (new File(System.getProperty("user.dir"),
			// "DoNotAskMeToSaveData.txt").exists())
			// result = JOptionPane.NO_OPTION;
			// else
			// result =
			// JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
			// "Would you like to save the current open data? It will be compressed so it
			// opens faster next time.",
			// "Save data",
			// JOptionPane.YES_NO_CANCEL_OPTION);
			// if (result == JOptionPane.YES_OPTION) {
			// saveData();
			// } else if (result == JOptionPane.CANCEL_OPTION || result ==
			// JOptionPane.CLOSED_OPTION) {
			// return;
			// }
			// }
			// String OSname = System.getProperty("os.name");
			// File fl = null;
			// if (!OSname.contains("Mac")) {
			// FileChooserWithSavedDir fc = new FileChooserWithSavedDir();
			// fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			// fc.setFileFilter(new FileFilter() {
			// @Override
			// public boolean accept(File f) {
			// if (f.isDirectory())
			// return true;
			// return false;
			// }

			// @Override
			// public String getDescription() {
			// return "VU-AMS data";
			// }
			// });
			// int retVal = fc.showOpenDialog(MainFrame.getInstance().getMainFrame());
			// if (retVal == JFileChooser.APPROVE_OPTION)
			// fl = fc.getSelectedFile();
			// } else {
			// FileDialogWithSavedDir fd = new
			// FileDialogWithSavedDir(MainFrame.getInstance().getMainFrame());
			// fd.setFilenameFilter(new FilenameFilter() {
			// @Override
			// public boolean accept(File f, String s) {
			// return false; // directories only
			// }
			// });
			// fd.setVisible(true);
			// if (fd.getFile() != null) {
			// String filename = fd.getDirectory() + File.separator + fd.getFile();
			// fl = new File(filename);
			// }
			// }
			// if (fl != null) {
			// if (fl.isDirectory()) {
			// BinDirTest datafile = new BinDirTest(fl);
			// datafile.start();
			// }
			// }
			// } else if (comm.equals("Exit program")) {
			// ExitApp.NormalExit();
		} else if (comm.equals("Save")) {
			saveData();
		} else if (comm.equals("Save a copy")) {
			File datFile = CurrentOpenData.getInstance().getDataFile();
			String extension = "";
			if (!Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("7fs")
					&& CurrentOpenData.getInstance().getFileHeader().getDwHardwareVersion() != 7)
				extension = "amsdata";
			else
				extension = "amsdatai";
			if (datFile != null) {
				if (Utils.getExtension(datFile).equals(extension)) {
					File retFile = SaveDialog.saveFileBrowserWithAddition(extension, "_copy", "Ams Data File");
					if (retFile != null) {
						AmsDataFile fl = new AmsDataFile(true, retFile.getAbsolutePath());
						fl.setCompressed(true);
						fl.setForceReSave(true);
						fl.start();
					}
				} else {
					File retFile = null;
					if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.SAVESIDINFILENAME) != 0) {
						retFile = SaveDialog.saveFileBrowserWithPrepend(extension,
								CurrentOpenData.getInstance().getFileHeader().getSzSubjectID() + "_",
								"Ams Data File");
					} else {
						retFile = SaveDialog.saveFileBrowser(extension, "Ams Data File");
					}
					if (retFile != null) {
						AmsDataFile fl = new AmsDataFile(true, retFile.getAbsolutePath());
						fl.setCompressed(true);
						fl.start();
					}
				}
			}
		} else if (comm.equals("Close")) {
			CurrentOpenData.getInstance().Close();
		} else if (comm.equals("About")) {
			AboutDialog diag = new AboutDialog();
			diag.setVisible(true);
		} else if (comm.equals("Report problem or request feature")) {
			Utils.submitBug();
		} else if (comm.equals("VU-AMS website")) {
			Utils.OpenWebsite("http://www.vu-ams.nl");
		} else if (comm.equals("VU-AMS manual")) {
			Utils.OpenWebsite("http://www.vu-ams.nl/support/instruction-manual");
		} else if (comm.equals("Settings")) {
			SettingsDialog diag = new SettingsDialog();
			diag.setVisible(true);
			// } else if (comm.equals("Batch convert data files")) {
			// BatchDialog bdg = new BatchDialog(BatchDialog.CONVERT5FS);
			// bdg.setVisible(true);
		} else if (comm.equals("Batch export data")) {
			BatchDialog bdg = new BatchDialog(BatchDialog.EXPORTDATA);
			bdg.setVisible(true);
			// }else if (comm.equals("Batch Analyze Data & Export")) {
			// BatchDialog bdg = new BatchDialog(BatchDialog.BATCHANALYSIS);
			// bdg.setVisible(true);
			// }else if (comm.equals("Merge 7fs files")) {
			// Merge7fsFilesDialog mdg = new Merge7fsFilesDialog();
			// mdg.setVisible(true);
		} else if (comm.equals("TestFunction")) {
			TestFunction.testFunction();
		}

	}

	public void saveData() {
		File datFile = CurrentOpenData.getInstance().getDataFile();
		String extension = "";
		if (!Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("7fs")
				&& CurrentOpenData.getInstance().getFileHeader().getDwHardwareVersion() != 7)
			extension = "amsdata";
		else
			extension = "amsdatai";
		if (datFile != null) {
			if (Utils.getExtension(datFile).equals(extension)) {
				AmsDataFile file = new AmsDataFile(true, CurrentOpenData.getInstance().getDataFile().getAbsolutePath());
				file.setCompressed(true);
				file.setShowProgress(false);
				file.start();
				try {
					file.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				File retFile = null;
				if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.SAVESIDINFILENAME) != 0) {

					String currentfilename = Utils.removeExtension(datFile.getName());
					String newfilename = CurrentOpenData.getInstance().getFileHeader().getSzSubjectID();
					File newnamefor5fs = new File(datFile.getParent(),
							CurrentOpenData.getInstance().getFileHeader().getSzSubjectID() + "_" + datFile.getName());

					if (currentfilename.contains(newfilename)) {
						// Do nothing
					} else {
						datFile.renameTo(newnamefor5fs);
					}

					if (currentfilename.contains(newfilename)) {
						retFile = SaveDialog.saveFileBrowserWithPrepend(extension, "" + "",
								"Ams Data File");
					} else {
						retFile = SaveDialog.saveFileBrowserWithPrepend(extension,
								CurrentOpenData.getInstance().getFileHeader().getSzSubjectID() + "_",
								"Ams Data File");
					}

				} else {
					retFile = SaveDialog.saveFileBrowser(extension, "Ams Data File");
				}
				if (retFile != null) {
					AmsDataFile fl = new AmsDataFile(true, retFile.getAbsolutePath());
					fl.setCompressed(true);
					fl.start();
				}
			}
		}
	}

	/**
	 * Fill component vectors.
	 */
	private void addItems() {

		fileMenu = new JMenu("File");
		helpMenu = new JMenu("Help");
		editMenu = new JMenu("Edit");
		JMenu deviceMenu = new JMenu("Connect");

		JMenuItem mi;
		// mi = new JMenuItem("Open bin dir (test)");
		// mi.addActionListener(this);
		// fileMenu.add(mi);
		mi = new JMenuItem("Import raw data");
		mi.addActionListener(this);
		fileMenu.add(mi);

		mi = new JMenuItem("Open processed data");
		mi.addActionListener(this);
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		fileMenu.add(mi);

		// quickOpenItem = new JMenuItem("Inspect AMS Data file");
		// quickOpenItem.addActionListener(new ActionListener() {

		// @Override
		// public void actionPerformed(ActionEvent e) {
		// String OSname = System.getProperty("os.name");
		// File fl = null;
		// if(!OSname.contains("Mac")){
		// FileChooserWithSavedDir fc = new FileChooserWithSavedDir();
		// fc.setFileFilter(new FileFilter() {

		// @Override
		// public boolean accept(File f) {
		// if (f.isDirectory())
		// return true;
		// String ext = Utils.getExtension(f);
		// if (ext == null)
		// return false;
		// return Utils.getExtension(f).equals("amsdata") ||
		// Utils.getExtension(f).equals("amsdatai");
		// }

		// @Override
		// public String getDescription() {
		// return "AMS data";
		// }
		// });
		// int retVal = fc.showOpenDialog(MainFrame.getInstance().getMainFrame());
		// if (retVal == JFileChooser.APPROVE_OPTION)
		// fl = fc.getSelectedFile();
		// } else {
		// FileDialogWithSavedDir fd = new
		// FileDialogWithSavedDir(MainFrame.getInstance().getMainFrame());
		// fd.setFilenameFilter(new FilenameFilter() {
		// @Override
		// public boolean accept(File f, String s) {
		// s = s.toLowerCase();
		// if (s.endsWith("amsdata") || s.endsWith("amsdatai"))
		// return true;
		// return false;
		// }
		// });
		// fd.setVisible(true);
		// if (fd.getFile() != null) {
		// String filename = fd.getDirectory() + File.separator + fd.getFile();
		// fl = new File(filename);
		// }
		// }
		// if (fl != null) {
		// QuickOpenDialog qod = new QuickOpenDialog(fl);
		// qod.setVisible(true);
		// }
		// }
		// });
		// fileMenu.add(quickOpenItem);
		saveItem = new JMenuItem("Save");
		saveItem.addActionListener(this);
		saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		saveItem.setEnabled(false);
		fileMenu.add(saveItem);
		saveAsItem = new JMenuItem("Save a copy");
		saveAsItem.addActionListener(this);
		saveAsItem
				.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
		saveAsItem.setEnabled(false);
		fileMenu.add(saveAsItem);
		mi = new JMenuItem("Close");
		mi.addActionListener(this);
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
		fileMenu.add(mi);

		// batchConvertItem = new JMenuItem("Batch convert data files");
		// batchConvertItem.addActionListener(this);
		// fileMenu.add(batchConvertItem);

		// batchanalyze = new JMenuItem("Batch Analyze Data & Export");
		// batchanalyze.addActionListener(this);
		// fileMenu.add(batchanalyze);

		// merge7fsItem = new JMenuItem("Merge 7fs files");
		// merge7fsItem.addActionListener(this);
		// fileMenu.add(merge7fsItem);

		// mi = new JMenuItem("Exit program");
		// mi.addActionListener(this);
		// fileMenu.add(mi);

		mi = new JMenuItem("Report problem or request feature");
		mi.addActionListener(this);
		helpMenu.add(mi);
		mi = new JMenuItem("VU-AMS website");
		mi.addActionListener(this);
		helpMenu.add(mi);
		mi = new JMenuItem("VU-AMS manual");
		mi.addActionListener(this);
		helpMenu.add(mi);
		mi = new JMenuItem("About");
		mi.addActionListener(this);
		helpMenu.add(mi);
		mi = new JMenuItem("Settings");
		mi.addActionListener(this);
		editMenu.add(mi);

		JMenuItem serConn = new JMenuItem("VU-AMS 5fs");
		serConn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));

		serConn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				AmsDevice dev = new AmsDevice();
				dev.connectToSerialPort();
			}
		});
		deviceMenu.add(serConn);

		// JMenuItem bluetoothConn = new JMenuItem("Connect using Bluetooth");
		// bluetoothConn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B,
		// ActionEvent.ALT_MASK));

		// bluetoothConn.addActionListener(new ActionListener() {

		// @Override
		// public void actionPerformed(ActionEvent e) {
		// AmsDevice dev = new AmsDevice();
		// dev.connectToBlueTooth();
		// }
		// });
		// deviceMenu.add(bluetoothConn);

		// JMenuItem udpConn = new JMenuItem("VU-AMS Core using UDP");
		// udpConn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U,
		// ActionEvent.ALT_MASK));

		// udpConn.addActionListener(new ActionListener() {

		// @Override
		// public void actionPerformed(ActionEvent e) {
		// AmsDevice7 dev = new AmsDevice7(false);
		// dev.connectToUDP();
		// }
		// });
		// deviceMenu.add(udpConn);

		JMenuItem bleConn = new JMenuItem("VU-AMS Core");
		bleConn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, ActionEvent.ALT_MASK));

		bleConn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					AmsDevice7 dev = new AmsDevice7(true);
					dev.connectToBLE();
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
							"Matlab not installed. It is adviced to use the Conroller app (iOS) to connect to the device",
							"Matlab BLE not found",
							JOptionPane.WARNING_MESSAGE);
				}
			}
		});
		deviceMenu.add(bleConn);

		JMenu dataMenu = new JMenu("Data");
		recInfoItem = new JMenuItem("Recording info");
		recInfoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.ALT_MASK));
		recInfoItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new RecordingInfoDialog().setVisible(true);
			}
		});
		recInfoItem.setEnabled(false);
		dataMenu.add(recInfoItem);

		exportSignal = new JMenuItem("Export Signal To ASCII");
		exportSignal.addActionListener(new ActionListener() {
			boolean ecgonly = false;

			@Override
			public void actionPerformed(ActionEvent e) {
				final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Export Signal To ASCII",
						true);
				JPanel panel1 = new JPanel();
				JPanel panel2 = new JPanel();
				FlowLayout flow = new FlowLayout();
				panel1.setLayout(flow);
				panel2.setLayout(flow);
				diag.add(panel1);
				diag.add(panel2);
				diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));
				final ArrayList<Ams7fsChannelInfo> chans = CurrentOpenData.getInstance().getChannelInfo();
				if (chans.isEmpty()) {
					JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "No channels present",
							"Export Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				String[] options = new String[chans.size()];
				for (int i = 0; i < chans.size(); i++) {
					options[i] = chans.get(i).getSzID();
				}
				final JComboBox<String> cb = new JComboBox<String>(options);
				panel1.add(new JLabel("Channel:"));
				panel1.add(cb);
				panel1.add(Box.createRigidArea(new Dimension(5, 0)));
				panel1.add(new JSeparator(SwingConstants.VERTICAL));
				panel1.add(Box.createRigidArea(new Dimension(5, 0)));
				panel1.add(new JLabel("Output"));

				NumberFormat nff = NumberFormat.getInstance(Locale.US);
				nff.setMaximumFractionDigits(0);
				nff.setGroupingUsed(false);
				NumberFormatter nf = new NumberFormatter(nff);
				DefaultFormatterFactory dff = new DefaultFormatterFactory(nf);
				final JFormattedTextField ftf = new JFormattedTextField(dff);
				try {
					ftf.setValue(1000 / CurrentOpenData.getInstance()
							.getChannelInfoFromID(cb.getSelectedItem().toString()).getDwDivider());
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				ActionListener l = (new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						JComboBox<?> source = (JComboBox<?>) e.getSource();
						try {
							ftf.setValue(1000 / CurrentOpenData.getInstance()
									.getChannelInfoFromID(source.getSelectedItem().toString()).getDwDivider());
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
				});

				cb.addActionListener(l);
				ftf.setColumns(10);
				panel1.add(ftf);
				panel1.add(new JLabel("Hz"));
				panel1.add(Box.createRigidArea(new Dimension(10, 0)));
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						diag.setVisible(false);
						try {
							ftf.commitEdit();
						} catch (ParseException e1) {
						}

						if (cb.getSelectedItem().equals("ECG")
								&& CurrentOpenData.getInstance().getFileHeader().getsFileID().equals("AMS2"))
							JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
									"As noted on our website the amplitude of the ECG needs to be divided by 3.6\nThe output of this conversion contains this correction.");

						timeintervalinsec = 1000 / (Long) ftf.getValue();
						double sr = 1000. * timeintervalinsec;
						File outFile = SaveDialog.saveFileBrowserWithAddition("txt",
								"_" + (String) cb.getSelectedItem(), "ASCII Signal File");
						if (outFile != null) {
							BinaryFile bf = new BinaryFile((String) cb.getSelectedItem());
							bf.outputToASCII(outFile, sr, cb.getSelectedItem().toString(), ecgonly);
							try {
								bf.close();
							} catch (Exception e2) {
								e2.printStackTrace();
							}
						}
					}
				});
				panel2.add(okButton);
				JButton cancButton = new JButton("Cancel");
				cancButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						diag.setVisible(false);
					}
				});
				final JCheckBox defaultcb = new JCheckBox("Advanced Settings");
				defaultcb.setSelected(false);

				/*
				 * ActionListener ad = (new ActionListener() {
				 * 
				 * @Override
				 * public void actionPerformed(ActionEvent e) {
				 * JCheckBox source = (JCheckBox) e.getSource();
				 * if(source.isSelected() && source == defaultcb){
				 * 
				 * //JOptionPane.showMessageDialog(source,
				 * "It is not advised to change the Sampling Frequency for ECG and ICG Signals",
				 * "Sampling Frequency Warning", JOptionPane.OK_OPTION);
				 * JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
				 * "It is not advised to change the Sampling Frequency for ECG and ICG Signals"
				 * );
				 * 
				 * ftf.setEnabled(true);
				 * ftf.setEditable(true);
				 * 
				 * }
				 * else{
				 * ftf.setEnabled(false);
				 * }
				 * }
				 * });
				 * defaultcb.addActionListener(ad);
				 */
				panel2.add(cancButton);
				ecgonly = false;
				final JCheckBox raw = new JCheckBox("Export only raw Signal");
				panel1.add(raw, BorderLayout.WEST);
				raw.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent arg0) {
						ecgonly = raw.isSelected();
					}

				});
				// panel2.add(defaultcb);
				diag.pack();
				diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
				diag.setVisible(true);
			}
		});
		exportSignal.setEnabled(false);
		dataMenu.add(exportSignal);

		exportSignalEDF = new JMenuItem("Export Signals To EDF");
		exportSignalEDF.addActionListener(new ActionListener() {
			boolean ecgonly = false;

			@Override
			public void actionPerformed(ActionEvent e) {
				final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Export Signals To EDF", true);
				JPanel panel1 = new JPanel();
				JPanel panel2 = new JPanel();
				FlowLayout flow = new FlowLayout();
				panel1.setLayout(flow);
				panel2.setLayout(flow);
				diag.add(panel1);
				diag.add(panel2);
				diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));
				final ArrayList<Ams7fsChannelInfo> chans = CurrentOpenData.getInstance().getChannelInfo();
				if (chans.isEmpty()) {
					JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "No channels present",
							"Export Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				String[] options = new String[chans.size()];
				for (int i = 0; i < chans.size(); i++) {
					options[i] = chans.get(i).getSzID();
				}
				JList<String> cb = new JList<String>(options);
				cb.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
				panel1.add(new JLabel("Channel:"));
				panel1.add(cb);
				panel1.add(Box.createRigidArea(new Dimension(5, 0)));
				panel1.add(new JSeparator(SwingConstants.VERTICAL));
				panel1.add(Box.createRigidArea(new Dimension(5, 0)));
				panel1.add(new JLabel("Output"));

				NumberFormat nff = NumberFormat.getInstance(Locale.US);
				nff.setMaximumFractionDigits(0);
				nff.setGroupingUsed(false);
				long[] freqs = new long[chans.size()];
				String[] freqStr = new String[chans.size()];
				try {
					for (int i = 0; i < chans.size(); i++) {
						freqStr[i] = String.valueOf(
								1000 / CurrentOpenData.getInstance().getChannelInfoFromID(options[i]).getDwDivider())
								+ " Hz";
						freqs[i] = 1000 / CurrentOpenData.getInstance().getChannelInfoFromID(options[i]).getDwDivider();
					}
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				JList<String> ftf = new JList<String>(freqStr);
				// ftf.setColumns(10);
				panel1.add(ftf);
				panel1.add(Box.createRigidArea(new Dimension(10, 0)));
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						diag.setVisible(false);
						for (Object item : cb.getSelectedValuesList())
							if (item.toString().equals("ECG")
									&& CurrentOpenData.getInstance().getFileHeader().getsFileID().equals("AMS2"))
								JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
										"As noted on our website the amplitude of the ECG needs to be divided by 3.6\nThe output of this conversion contains this correction.");
						long[] timeintervalsinsec = new long[cb.getSelectedIndices().length];
						for (int i = 0; i < timeintervalsinsec.length; i++)
							timeintervalsinsec[i] = 1000 / (Long) freqs[cb.getSelectedIndices()[i]];
						ArrayList<Double> srs = new ArrayList<Double>();
						for (int i = 0; i < timeintervalsinsec.length; i++)
							srs.add(1000. * timeintervalsinsec[i]);
						String addition;
						if (cb.getSelectedValuesList().size() == 1)
							addition = "_" + (String) cb.getSelectedValuesList().get(0);
						else
							addition = "_multiplesigs";
						File outFile = SaveDialog.saveFileBrowserWithAddition("edf", addition, "EDF Signal File");
						if (outFile != null) {
							String[] bfList = new String[cb.getSelectedValuesList().size()];
							for (int i = 0; i < cb.getSelectedValuesList().size(); i++)
								bfList[i] = cb.getSelectedValuesList().get(i);
							new BinaryFileSet(bfList).outputsToEDF(outFile, srs, cb.getSelectedValuesList(), ecgonly);
						}
					}
				});
				panel2.add(okButton);
				JButton cancButton = new JButton("Cancel");
				cancButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						diag.setVisible(false);
					}
				});
				final JCheckBox defaultcb = new JCheckBox("Advanced Settings");
				defaultcb.setSelected(false);

				/*
				 * ActionListener ad = (new ActionListener() {
				 * 
				 * @Override
				 * public void actionPerformed(ActionEvent e) {
				 * JCheckBox source = (JCheckBox) e.getSource();
				 * if(source.isSelected() && source == defaultcb){
				 * 
				 * //JOptionPane.showMessageDialog(source,
				 * "It is not advised to change the Sampling Frequency for ECG and ICG Signals",
				 * "Sampling Frequency Warning", JOptionPane.OK_OPTION);
				 * JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
				 * "It is not advised to change the Sampling Frequency for ECG and ICG Signals"
				 * );
				 * 
				 * ftf.setEnabled(true);
				 * //ftf.setEditable(true);
				 * 
				 * }
				 * else{
				 * ftf.setEnabled(false);
				 * }
				 * }
				 * });
				 * defaultcb.addActionListener(ad);
				 */
				panel2.add(cancButton);
				ecgonly = false;
				final JCheckBox raw = new JCheckBox("Export only raw Signal");
				panel1.add(raw, BorderLayout.WEST);
				raw.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent arg0) {
						ecgonly = raw.isSelected();
					}

				});
				// panel2.add(defaultcb);
				diag.pack();
				diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
				diag.setVisible(true);
			}
		});
		exportSignalEDF.setEnabled(false);
		dataMenu.add(exportSignalEDF);

		exportSignalBin = new JMenuItem("Export Signals To bin file");
		exportSignalBin.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Export Signals To bin file",
						true);
				JPanel panel1 = new JPanel();
				JPanel panel2 = new JPanel();
				FlowLayout flow = new FlowLayout();
				panel1.setLayout(flow);
				panel2.setLayout(flow);
				diag.add(panel1);
				diag.add(panel2);
				diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));
				final ArrayList<Ams7fsChannelInfo> chans = CurrentOpenData.getInstance().getChannelInfo();
				if (chans.isEmpty()) {
					JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "No channels present",
							"Export Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				String[] options = new String[chans.size()];
				for (int i = 0; i < chans.size(); i++) {
					options[i] = chans.get(i).getSzID();
				}
				final JComboBox<String> cb = new JComboBox<String>(options);
				panel1.add(new JLabel("Channel:"));
				panel1.add(cb);
				panel1.add(Box.createRigidArea(new Dimension(5, 0)));
				panel1.add(new JSeparator(SwingConstants.VERTICAL));
				panel1.add(Box.createRigidArea(new Dimension(5, 0)));
				panel1.add(new JLabel("Output"));

				panel1.add(Box.createRigidArea(new Dimension(10, 0)));
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						diag.setVisible(false);
						if (cb.getSelectedItem().toString().equals("ECG")
								&& CurrentOpenData.getInstance().getFileHeader().getsFileID().equals("AMS2"))
							JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
									"As noted on our website the amplitude of the ECG needs to be divided by 3.6\nThe output of this conversion contains this correction.");
						String addition;
						addition = "_" + (String) cb.getSelectedItem();
						File outFile = SaveDialog.saveFileBrowserWithAddition("bin", addition, "Binary Signal File");
						if (outFile != null) {
							File inFile = new File(CurrentOpenData.getInstance().getFilePath(),
									cb.getSelectedItem() + ".bin");
							try {
								Files.copy(inFile.toPath(), outFile.toPath());
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					}
				});
				panel2.add(okButton);
				JButton cancButton = new JButton("Cancel");
				cancButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						diag.setVisible(false);
					}
				});
				final JCheckBox defaultcb = new JCheckBox("Advanced Settings");
				defaultcb.setSelected(false);

				/*
				 * ActionListener ad = (new ActionListener() {
				 * 
				 * @Override
				 * public void actionPerformed(ActionEvent e) {
				 * JCheckBox source = (JCheckBox) e.getSource();
				 * if(source.isSelected() && source == defaultcb){
				 * 
				 * //JOptionPane.showMessageDialog(source,
				 * "It is not advised to change the Sampling Frequency for ECG and ICG Signals",
				 * "Sampling Frequency Warning", JOptionPane.OK_OPTION);
				 * JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
				 * "It is not advised to change the Sampling Frequency for ECG and ICG Signals"
				 * );
				 * 
				 * ftf.setEnabled(true);
				 * //ftf.setEditable(true);
				 * 
				 * }
				 * else{
				 * ftf.setEnabled(false);
				 * }
				 * }
				 * });
				 * defaultcb.addActionListener(ad);
				 */
				panel2.add(cancButton);
				diag.pack();
				diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
				diag.setVisible(true);
			}
		});
		exportSignalBin.setEnabled(false);
		dataMenu.add(exportSignalBin);

		externalItem = new JMenuItem("Load external signals");
		externalItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Load External Signals", true);
				diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));

				JPanel butPanel = new JPanel();
				final JButton finalloadbutton = new JButton("Load");

				// ----- Cancel Button-----------------------
				JButton cancButton = new JButton("Cancel");
				cancButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						diag.setVisible(false);
					}
				});
				// ------------------------------------------
				butPanel.add(finalloadbutton);
				butPanel.add(cancButton);

				// ---------------------- Signals Panel-----------------------------
				JPanel signalsPanel = new JPanel(new GridLayout(3, 3));
				final JButton loadfile1 = new JButton("Select File");
				loadfile1.setEnabled(false);

				final JButton loadfile2 = new JButton("Select File");
				loadfile2.setEnabled(false);

				final JButton loadfile3 = new JButton("Select File");
				loadfile3.setEnabled(false);

				final JTextField textfield1 = new JTextField(40);
				textfield1.setEnabled(false);

				final JTextField textfield2 = new JTextField(40);
				textfield2.setEnabled(false);

				final JTextField textfield3 = new JTextField(40);
				textfield3.setEnabled(false);

				final JCheckBox signal1 = new JCheckBox("Load Signal 1");
				signal1.setSelected(false);

				final JCheckBox signal2 = new JCheckBox("Load Signal 2");
				signal2.setSelected(false);

				final File curFile1 = new File(CurrentOpenData.getInstance().getFilePath(), "extfile.dat");
				final File curFile2 = new File(CurrentOpenData.getInstance().getFilePath(), "extfile2.dat");
				final File curFile3 = new File(CurrentOpenData.getInstance().getFilePath(), "extfile3.dat");

				final JCheckBox signal3 = new JCheckBox("Load Signal 3");
				signal3.setSelected(false);

				if (curFile1.exists() && curFile2.exists() && curFile3.exists()) {
					signal1.setEnabled(false);
					signal2.setEnabled(false);
					signal3.setEnabled(false);
					textfield1.setText("External Signal 1 is already loaded");
					textfield2.setText("External Signal 2 is already loaded");
					textfield3.setText("External Signal 3 is already loaded");
				} else if (curFile1.exists() && curFile2.exists()) {
					signal1.setEnabled(false);
					signal2.setEnabled(false);
					signal3.setEnabled(true);
					textfield1.setText("External Signal 1 is already loaded");
					textfield2.setText("External Signal 2 is already loaded");
				} else if (curFile1.exists()) {
					signal1.setEnabled(false);
					signal2.setEnabled(true);
					signal3.setEnabled(false);
					textfield1.setText("External Signal 1 is already loaded");
				} else {
					signal1.setEnabled(true);
					signal2.setEnabled(false);
					signal3.setEnabled(false);
				}

				loadfile1.addActionListener(new ActionListener() { // Select File Button

					@Override
					public void actionPerformed(ActionEvent e) {

						String OSname = System.getProperty("os.name");
						File fl = null;
						if (!OSname.contains("Mac")) {
							FileChooserWithSavedDir fc1 = new FileChooserWithSavedDir();
							fc1.setFileFilter(new FileFilter() {
								@Override
								public boolean accept(File f) {
									if (f.isDirectory())
										return true;
									String ext = Utils.getExtension(f);
									if (ext == null)
										return false;
									return Utils.getExtension(f).equals("txt");
								}

								@Override
								public String getDescription() {
									return "Text files";
								}
							});
							int retVal = fc1.showOpenDialog(MainFrame.getInstance().getMainFrame());
							if (retVal == JFileChooser.APPROVE_OPTION)
								fl = fc1.getSelectedFile();
						} else {
							FileDialogWithSavedDir fd = new FileDialogWithSavedDir(
									MainFrame.getInstance().getMainFrame());
							fd.setFilenameFilter(new FilenameFilter() {
								@Override
								public boolean accept(File f, String s) {
									s = s.toLowerCase();
									if (s.endsWith("txt"))
										return true;
									return false;
								}
							});
							fd.setVisible(true);
							if (fd.getFile() != null) {
								String filename = fd.getDirectory() + File.separator + fd.getFile();
								fl = new File(filename);
							}
						}
						if (fl != null) {
							textfield1.setText(Utils.guaranteeExtension(fl.toString(), "txt"));
							setSelectedExternalFile1(fl);
						}

						File curFile1 = new File(CurrentOpenData.getInstance().getFilePath(), "extfile.dat");
						boolean state = false;
						if (signal1.isEnabled() && !textfield1.getText().equals("")) {
							state = true;
						}

						if (curFile1.exists() || (state == true)) {
							signal2.setEnabled(true);
						}

					}
				});

				loadfile2.addActionListener(new ActionListener() { // Select File Button 2

					@Override
					public void actionPerformed(ActionEvent e) {

						String OSname = System.getProperty("os.name");
						File fl2 = null;
						if (!OSname.contains("Mac")) {
							FileChooserWithSavedDir fc2 = new FileChooserWithSavedDir();
							fc2.setFileFilter(new FileFilter() {

								@Override
								public boolean accept(File f) {
									if (f.isDirectory())
										return true;
									String ext = Utils.getExtension(f);
									if (ext == null)
										return false;
									return Utils.getExtension(f).equals("txt");
								}

								@Override
								public String getDescription() {
									return "Text files";
								}
							});
							int retVal = fc2.showOpenDialog(MainFrame.getInstance().getMainFrame());
							if (retVal == JFileChooser.APPROVE_OPTION)
								fl2 = fc2.getSelectedFile();
						} else {
							FileDialogWithSavedDir fd = new FileDialogWithSavedDir(
									MainFrame.getInstance().getMainFrame());
							fd.setFilenameFilter(new FilenameFilter() {
								@Override
								public boolean accept(File f, String s) {
									s = s.toLowerCase();
									if (s.endsWith("txt"))
										return true;
									return false;
								}
							});
							fd.setVisible(true);
							if (fd.getFile() != null) {
								String filename = fd.getDirectory() + File.separator + fd.getFile();
								fl2 = new File(filename);
							}
						}
						if (fl2 != null) {
							textfield2.setText(Utils.guaranteeExtension(fl2.toString(), "txt"));
							setSelectedExternalFile2(fl2);
						}
						File curFile2 = new File(CurrentOpenData.getInstance().getFilePath(), "extfile2.dat");
						boolean state = false;
						if (signal2.isEnabled() && !textfield2.getText().equals("")) {
							state = true;
						}
						if (curFile2.exists() || (state == true)) {
							signal3.setEnabled(true);
						}
					}

				});

				loadfile3.addActionListener(new ActionListener() { // Select File3 Button

					@Override
					public void actionPerformed(ActionEvent e) {
						String OSname = System.getProperty("os.name");
						File fl = null;
						if (!OSname.contains("Mac")) {
							FileChooserWithSavedDir fc3 = new FileChooserWithSavedDir();
							fc3.setFileFilter(new FileFilter() {

								@Override
								public boolean accept(File f) {
									if (f.isDirectory())
										return true;
									String ext = Utils.getExtension(f);
									if (ext == null)
										return false;
									return Utils.getExtension(f).equals("txt");
								}

								@Override
								public String getDescription() {
									return "Text files";
								}
							});
							int retVal = fc3.showOpenDialog(MainFrame.getInstance().getMainFrame());
							if (retVal == JFileChooser.APPROVE_OPTION)
								fl = fc3.getSelectedFile();
						} else {
							FileDialogWithSavedDir fd = new FileDialogWithSavedDir(
									MainFrame.getInstance().getMainFrame());
							fd.setFilenameFilter(new FilenameFilter() {
								@Override
								public boolean accept(File f, String s) {
									s = s.toLowerCase();
									if (s.endsWith("txt"))
										return true;
									return false;
								}
							});
							fd.setVisible(true);
							if (fd.getFile() != null) {
								String filename = fd.getDirectory() + File.separator + fd.getFile();
								fl = new File(filename);
							}
						}
						if (fl != null) {
							textfield3.setText(Utils.guaranteeExtension(fl.toString(), "txt"));
							setSelectedExternalFile3(fl);
						}
					}
				});
				ActionListener CheckActionListner = (new ActionListener() { // Check box listener

					@Override
					public void actionPerformed(ActionEvent e) {
						JCheckBox source = (JCheckBox) e.getSource();

						if (source == signal1) {
							loadfile1.setEnabled(source.isSelected());
						}

						if (source == signal2) {
							loadfile2.setEnabled(source.isSelected());
						}

						if (source == signal3) {
							loadfile3.setEnabled(source.isSelected());
						}
					}
				});

				finalloadbutton.addActionListener(new ActionListener() { // Final Load Button
					@Override
					public void actionPerformed(ActionEvent e) {

						int count = 0;
						boolean state1 = signal1.isSelected() && !textfield1.getText().equals("");
						boolean state2 = signal2.isSelected() && !textfield2.getText().equals("");
						boolean state3 = signal3.isSelected() && !textfield3.getText().equals("");

						if (state1 == true && state2 == true && state3 == true) {
							count = 3;
							ExternalFilePanel.getInstance().setnumberofExternalSignals(count);
							ExternalFilePanel.getInstance().setFile(file1);
							ExternalFilePanel.getInstance().setFile2(file2);
							ExternalFilePanel.getInstance().setFile3(file3);
						} else if (state1 == true && state2 == true) {
							count = 2;
							ExternalFilePanel.getInstance().setnumberofExternalSignals(count);
							ExternalFilePanel.getInstance().setFile(file1);
							ExternalFilePanel.getInstance().setFile2(file2);
						} else if (state2 == true && state3 == true) {
							count = 2;
							ExternalFilePanel.getInstance().setnumberofExternalSignals(count);
							ExternalFilePanel.getInstance().setFile2(file2);
							ExternalFilePanel.getInstance().setFile3(file3);
						} else if (state1 == true) {
							count = 1;
							ExternalFilePanel.getInstance().setnumberofExternalSignals(count);
							ExternalFilePanel.getInstance().setFile(file1);
						} else if (state2 == true) {
							if (curFile1.exists()) {
								count = 2;
								ExternalFilePanel.getInstance().loadFileFromTempDir();
							} else {
								count = 1;
							}
							ExternalFilePanel.getInstance().setnumberofExternalSignals(count);
							ExternalFilePanel.getInstance().setFile2(file2);
						} else if (state3 == true) {

							if (curFile1.exists() && curFile2.exists()) {
								count = 3;
								ExternalFilePanel.getInstance().loadFileFromTempDir();
								ExternalFilePanel.getInstance().loadFile2FromTempDir();
							} else {
								count = 1;
							}
							ExternalFilePanel.getInstance().setnumberofExternalSignals(count);
							ExternalFilePanel.getInstance().setFile3(file3);
						}
						if (count > 0)
							CurrentOpenData.getInstance().setDirty(true);
						diag.setVisible(false);
					}
				});

				signal1.addActionListener(CheckActionListner);
				signal2.addActionListener(CheckActionListner);
				signal3.addActionListener(CheckActionListner);

				signalsPanel.add(signal1);
				signalsPanel.add(textfield1);
				signalsPanel.add(loadfile1);
				signalsPanel.add(signal2);
				signalsPanel.add(textfield2);
				signalsPanel.add(loadfile2);
				signalsPanel.add(signal3);
				signalsPanel.add(textfield3);
				signalsPanel.add(loadfile3);

				signalsPanel.setBorder(BorderFactory.createLoweredBevelBorder());
				signalsPanel.setBorder(BorderFactory.createTitledBorder("Load External Signals"));

				diag.add(signalsPanel);
				diag.add(butPanel);

				diag.pack();
				diag.setResizable(false);
				diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
				diag.setVisible(true);

			}
		});
		dataMenu.add(externalItem);

		clearExtItem = new JMenuItem("Clear external signals");
		clearExtItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				clear1 = false;
				clear2 = false;
				clear3 = false;
				clear = false;

				final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Clear External Signals",
						true);
				diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));

				JPanel signalsPanel = new JPanel(new GridLayout(5, 1));

				final JCheckBox signal1 = new JCheckBox("Clear Signal 1");
				signal1.setSelected(false);
				if (!ExternalFilePanel.getInstance().isFile1Loaded())
					signal1.setEnabled(false);

				final JCheckBox signal2 = new JCheckBox("Clear Signal 2");
				signal2.setSelected(false);
				if (!ExternalFilePanel.getInstance().isFile2Loaded())
					signal2.setEnabled(false);

				final JCheckBox signal3 = new JCheckBox("Clear Signal 3");
				signal3.setSelected(false);
				if (!ExternalFilePanel.getInstance().isFile3Loaded())
					signal3.setEnabled(false);

				final JCheckBox clearall = new JCheckBox("Clear ALL");
				signal3.setSelected(false);

				signalsPanel.add(signal1);
				signalsPanel.add(signal2);
				signalsPanel.add(signal3);
				signalsPanel.add(new JSeparator());
				signalsPanel.add(clearall);

				ActionListener CheckActionListner = (new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						JCheckBox source = (JCheckBox) e.getSource();

						if (source == signal1)
							clear1 = source.isSelected();

						if (source == signal2)
							clear2 = source.isSelected();

						if (source == signal3)
							clear3 = source.isSelected();

						if (source.isSelected() && source == clearall) {
							signal1.setEnabled(false);
							signal2.setEnabled(false);
							signal3.setEnabled(false);
							clear = true;
						}
						if (!source.isSelected() && source == clearall) {
							if (ExternalFilePanel.getInstance().isFile1Loaded())
								signal1.setEnabled(true);
							if (ExternalFilePanel.getInstance().isFile2Loaded())
								signal2.setEnabled(true);
							if (ExternalFilePanel.getInstance().isFile3Loaded())
								signal3.setEnabled(true);
							clear = false;
						}
					}
				});
				signal1.addActionListener(CheckActionListner);
				signal2.addActionListener(CheckActionListner);
				signal3.addActionListener(CheckActionListner);
				clearall.addActionListener(CheckActionListner);

				JPanel butPanel = new JPanel();
				final JButton load = new JButton("Ok");
				load.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (clear1)
							ExternalFilePanel.getInstance().clear1();
						if (clear2)
							ExternalFilePanel.getInstance().clear2();
						if (clear3)
							ExternalFilePanel.getInstance().clear3();
						if (clear)
							ExternalFilePanel.getInstance().clear();
						else if (!(ExternalFilePanel.getInstance().isFile1Loaded()
								|| ExternalFilePanel.getInstance().isFile2Loaded()
								|| ExternalFilePanel.getInstance().isFile3Loaded()))
							ExternalFilePanel.getInstance().clear(); // Also clear all when no external data is shown
																		// (=last graph is cleared) to remove empty
																		// panel
						CurrentOpenData.getInstance().setDirty(true);
						diag.setVisible(false);
					}
				});

				JButton cancButton = new JButton("Cancel");
				cancButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						diag.setVisible(false);
					}
				});

				butPanel.add(load);
				butPanel.add(cancButton);

				diag.add(signalsPanel);
				diag.add(butPanel);
				diag.pack();
				diag.setResizable(false);
				diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
				diag.setVisible(true);

			}
		});

		dataMenu.add(clearExtItem);

		addPhysiologicalData = new JMenuItem("Add Demographic Data");
		addPhysiologicalData.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Add Demographic Data", true);
				JPanel panel1 = new JPanel();
				JPanel panel2 = new JPanel();
				FlowLayout flow = new FlowLayout();
				GridLayout grid = new GridLayout(4, 4);
				panel1.setLayout(grid);
				panel2.setLayout(flow);
				diag.add(panel1);
				diag.add(panel2);
				diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));

				String[] options = new String[2];
				options[0] = "Female";
				options[1] = "Male";

				final JComboBox<String> cb = new JComboBox<String>(options);
				cb.setSelectedIndex(CurrentOpenData.getInstance().getFileHeader().getGender() - 1);

				panel1.add(new JLabel("	"));
				panel1.add(new JLabel("Gender:"));
				panel1.add(cb);
				panel1.add(new JLabel("	"));

				// --- Age Spinner----------------------------
				SpinnerModel agespinnerModel = new SpinnerNumberModel(10, 0, 100, 1);// step
				final JSpinner agespinner = new JSpinner(agespinnerModel);
				// --------------------------------------------

				// --- Weight Spinner----------------------------
				SpinnerModel weightspinnerModel = new SpinnerNumberModel(2, 0, 200, 0.5);// step
				final JSpinner weightspinner = new JSpinner(weightspinnerModel);
				// --------------------------------------------

				// --- Height Spinner----------------------------
				SpinnerModel heightspinnerModel = new SpinnerNumberModel(20, 0, 250, 1);// step
				final JSpinner heightspinner = new JSpinner(heightspinnerModel);
				// --------------------------------------------

				try {
					agespinner.setValue(CurrentOpenData.getInstance().getFileHeader().getAge());
					weightspinner.setValue(CurrentOpenData.getInstance().getFileHeader().getWeight());
					heightspinner.setValue(CurrentOpenData.getInstance().getFileHeader().getHeight());
				} catch (Exception e2) {
					e2.printStackTrace();
				}

				panel1.add(new JLabel("	"));
				panel1.add(new JLabel("Age:"));
				panel1.add(agespinner);
				panel1.add(new JLabel("	"));

				panel1.add(new JLabel("	"));
				panel1.add(new JLabel("Weight [kg]"));
				panel1.add(weightspinner);
				panel1.add(new JLabel("	"));

				panel1.add(new JLabel("	"));
				panel1.add(new JLabel("Height [cm]"));
				panel1.add(heightspinner);
				panel1.add(new JLabel("	"));

				// ---------------- Panel2 ------------------------
				JButton okButton = new JButton("Save");
				okButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						System.out.println("Gender:_ " + cb.getSelectedItem().toString());
						System.out.println("Age:_ " + agespinner.getValue());
						System.out.println("Weight:_ " + weightspinner.getValue());
						System.out.println("Height:_ " + heightspinner.getValue());
						CurrentOpenData.getInstance().getFileHeader().setGender(cb.getSelectedIndex() + 1);
						CurrentOpenData.getInstance().getFileHeader()
								.setAge(Double.valueOf(agespinner.getValue().toString()));
						CurrentOpenData.getInstance().getFileHeader()
								.setWeight(Double.valueOf(weightspinner.getValue().toString()));
						CurrentOpenData.getInstance().getFileHeader()
								.setHeight(Double.valueOf(heightspinner.getValue().toString()));
						// CurrentOpenData.getInstance().saveChangeablesToDisk();
						CurrentOpenData.getInstance().setDirty(true);
						diag.setVisible(false);
					}
				});
				panel2.add(okButton);
				JButton cancButton = new JButton("Cancel");
				cancButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						diag.setVisible(false);
					}
				});
				panel2.add(cancButton);

				diag.pack();
				diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
				diag.setVisible(true);
			}
		});

		addPhysiologicalData.setVisible(true);
		dataMenu.add(addPhysiologicalData);

		importActigraphData = new JMenuItem("Import Actigraph Raw Data");
		importActigraphData.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String OSname = System.getProperty("os.name");
				File fl = null;
				if (!OSname.contains("Mac")) {
					FileChooserWithSavedDir fc = new FileChooserWithSavedDir();
					fc.setAcceptAllFileFilterUsed(false);
					fc.setFileFilter(new FileFilter() {

						@Override
						public boolean accept(File f) {
							if (f.isDirectory())
								return true;
							String ext = Utils.getExtension(f);
							if (ext == null)
								return false;
							return Utils.getExtension(f).equals("csv");
						}

						@Override
						public String getDescription() {
							return "Actigraph Raw Data CSV File ";
						}
					});

					int retVal = fc.showOpenDialog(MainFrame.getInstance().getMainFrame());

					if (retVal == JFileChooser.APPROVE_OPTION)
						fl = fc.getSelectedFile();
				} else {
					FileDialogWithSavedDir fd = new FileDialogWithSavedDir(MainFrame.getInstance().getMainFrame());
					fd.setFilenameFilter(new FilenameFilter() {
						@Override
						public boolean accept(File f, String s) {
							s = s.toLowerCase();
							if (s.endsWith("csv"))
								return true;
							return false;
						}
					});
					fd.setVisible(true);
					if (fd.getFile() != null) {
						String filename = fd.getDirectory() + File.separator + fd.getFile();
						fl = new File(filename);
					}
				}
				if (fl != null) {
					setActigraphFile(fl);
					CurrentOpenData.getInstance().setDirty(true);
					ActigraphTab.getInstance().setFile(actigraphfile);
					CurrentOpenData.getInstance().SetUpActigraphPanel();
					MainFrame.getInstance().getMainFrame().repaint();
				}
			}
		});
		importActigraphData.setEnabled(false);

		Gson gson = new Gson();
		File curFile = new File(System.getProperty("user.dir"), "VUDAMSDebug.json");
		if (curFile.exists()) {
			String inString = Utils.readStringFromFile(curFile);
			VUDAMSDebugSettings debugSettings = gson.fromJson(inString, VUDAMSDebugSettings.class);
			if (debugSettings.getImportactigraphData() == 1) {
				// if(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.IMPORTACTIGRAPHDATA)
				// == 1){ // Import Actigraph Data - Enable/Disable
				dataMenu.add(importActigraphData);
			}
		}

		JMenuItem jmi = new JMenuItem("Batch export data");
		jmi.addActionListener(this);
		dataMenu.add(jmi);

		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(dataMenu);
		menuBar.add(deviceMenu);
		// menuBar.add(Box.createHorizontalGlue());
		menuBar.add(helpMenu);

		if (Utils.isIsprerelease()) {
			JButton testFunction = new JButton("TestFunction");
			testFunction.addActionListener(this);
			menuBar.add(testFunction);
		}

		setFileOpen(false);

	}

	public JMenuBar getMenuBar() {
		return menuBar;
	}

	public JComponent getSaveAsItem() {
		return saveAsItem;
	}

	public JComponent getSaveItem() {
		return saveItem;
	}

	public void setActionMenu(JMenu inMenu) {
		if (actionMenu != null)
			menuBar.remove(actionMenu);
		actionMenu = inMenu;
		menuBar.add(actionMenu, 2);
	}

	public void setFileOpen(boolean open) {
		recInfoItem.setEnabled(open);
		saveAsItem.setEnabled(open);
		saveItem.setEnabled(open);
		externalItem.setEnabled(open);
		clearExtItem.setEnabled(open);
		exportSignal.setEnabled(open);
		exportSignalEDF.setEnabled(open);
		exportSignalBin.setEnabled(open);
		addPhysiologicalData.setEnabled(open);
		importActigraphData.setEnabled(open);
	}

	public void setSelectedExternalFile1(File fl) {
		file1 = fl;
	}

	public void setSelectedExternalFile2(File fl) {
		file2 = fl;
	}

	public void setSelectedExternalFile3(File fl) {
		file3 = fl;
	}

	public void setActigraphFile(File fl) {
		actigraphfile = fl;
	}

	public File getActiFile() {
		return actigraphfile;
	}
}
