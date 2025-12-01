package nl.vu.psy.ams.suite.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
// import nl.vu.psy.ams.suite.data.OrientationFilesGenerator;
import nl.vu.psy.ams.suite.data.SubSetFileGenerator;
import nl.vu.psy.ams.suite.data.files.Ams5fsFile;
import nl.vu.psy.ams.suite.data.files.Ams7fsFile;
import nl.vu.psy.ams.suite.data.files.AmsAMSFile;
import nl.vu.psy.ams.suite.data.files.AmsDataFile;
import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.data.posture.ActivityClassification;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.data.structures.sets.AmsLabelSet;
import nl.vu.psy.ams.suite.gui.tabs.imp.ImpTab;
import nl.vu.psy.ams.suite.gui.tabs.info.LabelInformationTab;
import nl.vu.psy.ams.suite.gui.tabs.label.LabelTab;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.FileChooserWithSavedDir;
import nl.vu.psy.ams.suite.tools.Timer;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Dialog that can batch export .5fs, .7fs or .ams data to .amsdata(i) files,
 * or batch export excel files for .amsdata(i) files. 
 */
public class BatchDialog implements ClipboardOwner {

	private JTextField tf;
	private JCheckBox subCheck;
	private JDialog diag;
	private ArrayList<File> convertFiles = new ArrayList<File>();
	private JCheckBox overwriteCheck;
	private boolean stopped;
	private long totalFileSizeDone;
	private double curAverage;
	private long fileSizeLeft;
	private long totalFileSize;
	private int type;
	private JCheckBox xlsCheck, xlsTCheck;
	private JCheckBox signalfilescheck;
	private JCheckBox signalfilescheckE;
	private JCheckBox signalfilescheckB;
	private JCheckBox aggxlsCheck, aggxlsTCheck;
	private JCheckBox ascCheck, ascTCheck;
	private JCheckBox aggascCheck, aggascTCheck;
	private JCheckBox ascRSRCheck;
	private JCheckBox aggascRSRCheck;
	private JCheckBox exportraw;
	private JCheckBox exportrawE;
	private JCheckBox exportbeatFiletoASCII;
	private JCheckBox aggexportbeatFiletoASCII;
	private JCheckBox includeEventInformation;
	private JCheckBox automaticlabels;
	private JTextField xlstf, xlstfT;
	private JTextField asctf, asctfT;
	private JTextField asctfRSR;
	private JTextField asctfBeat;
	private JTextField samplingtime;
	private JTextField samplingtimeE;
	// private JTextField milliseconds;
	private JTextField autolabelsselectedFile;
	public JTextArea ta;
	private JTextArea errorta;
	private JButton batcherrorsbutton;
	private JButton cancButton;
	private JButton copyButton;
	private JComboBox<?> cb;
	private JComboBox<?> cb2;
	private JComboBox<?> cb3;
	private JComboBox<?> autolabelsformat;
	private File autoLabelsFile;

	public final static int CONVERT5FS = 0;
	public final static int EXPORTDATA = 1;
	public final static int BATCHANALYSIS = 2;
	public static int TYPEOFANALYSIS = 0;
	public String errorlog = "";
	private boolean ecgonly = false;
	private boolean ecgonlyE = false;
	final ArrayList<Ams7fsChannelInfo> chans = CurrentOpenData.getInstance().getChannelInfo();
	JFormattedTextField numberFieldX, numberFieldA;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public BatchDialog(int type) {

		this.type = type;
		String titleString = "";
		if (type == CONVERT5FS) {// JdH keep window titles the same as MainMenuBar.java options
			titleString = "Batch convert data files";
		} else if (type == EXPORTDATA) {
			titleString = "Batch export data";
		} else if (type == BATCHANALYSIS) {
			titleString = "Batch Analyze Data & Export";
		}
		diag = new JDialog(MainFrame.getInstance().getMainFrame(), titleString, true);
		diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));

		tf = new JTextField(64);
		tf.setEnabled(false);

		JPanel browsePanel = new JPanel(new BorderLayout());
		JButton browseButton = new JButton("Select Directory");
		browseButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// String OSname = System.getProperty("os.name");
				// File fl = null;
				// if(!OSname.contains("Mac")){
				FileChooserWithSavedDir fc = new FileChooserWithSavedDir();
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int retVal = fc.showOpenDialog(MainFrame.getInstance().getMainFrame());
				if (retVal == JFileChooser.APPROVE_OPTION)
					tf.setText(fc.getSelectedFile().getAbsolutePath());
				/*
				 * } else {
				 * FileDialogWithSavedDir fd = new
				 * FileDialogWithSavedDir(MainFrame.getInstance().getMainFrame());
				 * fd.setFilenameFilter(new FilenameFilter() {
				 * 
				 * @Override
				 * public boolean accept(File f, String s) {
				 * return false; //directories only
				 * }
				 * });
				 * fd.setVisible(true);
				 * String filename = fd.getDirectory() + File.separator + fd.getFile();
				 * tf.setText(filename);
				 * }
				 */
			}
		});

		browsePanel.add(tf, BorderLayout.CENTER);
		browsePanel.add(browseButton, BorderLayout.EAST);

		JPanel optionsPanel = new JPanel();
		subCheck = new JCheckBox("Include subdirectories");
		subCheck.setSelected(true);
		optionsPanel.add(subCheck);
		overwriteCheck = new JCheckBox("Overwrite existing files");
		overwriteCheck.setSelected(false);
		optionsPanel.add(overwriteCheck);

		diag.add(browsePanel);
		diag.add(optionsPanel);

		// ------------------- Auto Import
		// Labels-----------------------------------------
		JPanel panellabels = new JPanel(new GridLayout(0, 4));

		String[] labellingoptions = { "Default/Legacy [SM, EM, D1, D2, Label Code]",
				"Start_Marker, End_Marker, Duration(sec)_From_StartMarker, Duration(sec)_From_EndMarker, Label_Code",
				"Start_Marker, Duration(sec)_LabelStart_StartMarker, Duration(sec)_LabelEnd_StartMarker, Label_Code" };
		autolabelsformat = new JComboBox(labellingoptions);
		autolabelsformat.setEnabled(false);
		autolabelsformat.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

			}
		});

		autolabelsselectedFile = new JTextField(40);
		autolabelsselectedFile.setEnabled(false);

		final JButton selectFilebrowseButton = new JButton("Select File");
		selectFilebrowseButton.setEnabled(false);
		selectFilebrowseButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// String OSname = System.getProperty("os.name");
				File fl = null;
				// if(!OSname.contains("Mac")){
				FileChooserWithSavedDir fc = new FileChooserWithSavedDir();
				fc.setFileFilter(new FileFilter() {

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

				int retVal = fc.showSaveDialog(MainFrame.getInstance().getMainFrame());
				if (retVal == JFileChooser.APPROVE_OPTION)
					fl = fc.getSelectedFile();
				/*
				 * } else {
				 * FileDialogWithSavedDir fd = new
				 * FileDialogWithSavedDir(MainFrame.getInstance().getMainFrame(),
				 * FileDialog.SAVE);
				 * fd.setFilenameFilter(new FilenameFilter() {
				 * 
				 * @Override
				 * public boolean accept(File f, String s) {
				 * s = s.toLowerCase();
				 * if (s.endsWith("txt"))
				 * return true;
				 * return false;
				 * }
				 * });
				 * fd.setVisible(true);
				 * String filename = fd.getDirectory() + File.separator + fd.getFile();
				 * fl = new File(filename);
				 * }
				 */
				if (fl != null) {
					autoLabelsFile = fl;
					if (fl.exists()) {
						autolabelsselectedFile.setText(Utils.guaranteeExtension(fl.toString(), "txt"));
					} else {
						autolabelsselectedFile.setText(Utils.guaranteeExtension(fl.toString(), "txt"));
					}
				}

			}
		});

		automaticlabels = new JCheckBox("Automatic Labelling using Markers/Events");
		automaticlabels.setSelected(false);
		ActionListener CheckActionListner1 = (new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JCheckBox source = (JCheckBox) e.getSource();

				if (source == automaticlabels) {
					autolabelsformat.setEnabled(source.isSelected());
					selectFilebrowseButton.setEnabled(source.isSelected());
				}
			}
		});
		automaticlabels.addActionListener(CheckActionListner1);

		panellabels.add(automaticlabels);
		panellabels.add(autolabelsformat);
		panellabels.add(autolabelsselectedFile);
		panellabels.add(selectFilebrowseButton);
		if (type == 0) {
			diag.add(panellabels);
		}

		// ------------------------------------------------------------------------------------------------

		// -------------------- Batch
		// Analysis-------------------------------------------------------------
		JPanel analyzePanel = new JPanel(new GridLayout(1, 2));
		analyzePanel.setBorder(BorderFactory.createLoweredBevelBorder());
		analyzePanel.setBorder(BorderFactory.createTitledBorder("Batch Analyze Impedance & ECG Data"));

		String[] analyzeOptions = { "---Choose the file type---", "AMS Data Files (ECG & ICG Complexes to be Scored)",
				"Score only points which are missing", "Rescore all the complexes" };
		final JComboBox analyzecombobox = new JComboBox(analyzeOptions);
		analyzePanel.add(analyzecombobox);

		/*
		 * final JButton loopbutton = new JButton("Test");
		 * loopbutton.addActionListener(new ActionListener(){
		 * 
		 * @Override
		 * public void actionPerformed(ActionEvent arg0) {
		 * 
		 * listConvertFiles(new File(tf.getText()), subCheck.isSelected());
		 * diag.setVisible(false);
		 * for (File f : convertFiles) {
		 * AmsDataFile fileopen = new AmsDataFile(false, f.getAbsolutePath(),true);
		 * fileopen.start();
		 * 
		 * //CurrentOpenData.getInstance().saveChangeablesToDisk();
		 * 
		 * MainFrame.getInstance().getTabs().setSelectedComponent(ImpTab.getInstance());
		 * MainFrame.getInstance().getMainFrame().repaint();
		 * MainFrame.getInstance().getTabs().repaint();
		 * ImpTab.getInstance().getImpDrawer().LabelsflipLooping();
		 * ImpTab.getInstance().getECGDrawer().LabelsflipLooping();
		 * }
		 * //convertAllFiles();
		 * 
		 * MainFrame.getInstance().getTabs().setSelectedComponent(ImpTab.getInstance());
		 * MainFrame.getInstance().getMainFrame().repaint();
		 * MainFrame.getInstance().getTabs().repaint();
		 * ImpTab.getInstance().getImpDrawer().LabelsflipLooping();
		 * ImpTab.getInstance().getECGDrawer().LabelsflipLooping();
		 * 
		 * }
		 * 
		 * });
		 * 
		 * analyzePanel.add(loopbutton);
		 */

		// ----------------- End--------------------------
		JPanel xlsPanel = new JPanel(new GridLayout(0, 5));
		xlsCheck = new JCheckBox("Export label data files");
		xlsCheck.setSelected(false);

		aggxlsCheck = new JCheckBox("Aggregate label data to single Excel file");
		aggxlsCheck.setSelected(false);
		xlstf = new JTextField(40);
		xlstf.setEnabled(false);

		final JButton convButton = new JButton("Batch");

		final JButton aggxlsbrowseButton = new JButton("Select File");

		final JButton aggascbrowseButton = new JButton("Select File");

		final JButton aggascRSRbrowseButton = new JButton("Select File");

		final JButton aggascBeatbrowseButton = new JButton("Select File");

		final JButton aggxlsTbrowseButton = new JButton("Select File");

		final JButton aggascTbrowseButton = new JButton("Select File");

		aggxlsbrowseButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// String OSname = System.getProperty("os.name");
				File fl = null;
				// if(!OSname.contains("Mac")){
				FileChooserWithSavedDir fc = new FileChooserWithSavedDir();
				fc.setFileFilter(new FileFilter() {

					@Override
					public boolean accept(File f) {
						if (f.isDirectory())
							return true;
						String ext = Utils.getExtension(f);
						if (ext == null)
							return false;
						return Utils.getExtension(f).equals("xls");
					}

					@Override
					public String getDescription() {
						return "Excel files";
					}
				});

				int retVal = fc.showSaveDialog(MainFrame.getInstance().getMainFrame());
				if (retVal == JFileChooser.APPROVE_OPTION)
					fl = fc.getSelectedFile();
				/*
				 * } else {
				 * FileDialogWithSavedDir fd = new
				 * FileDialogWithSavedDir(MainFrame.getInstance().getMainFrame(),
				 * FileDialog.SAVE);
				 * fd.setFilenameFilter(new FilenameFilter() {
				 * 
				 * @Override
				 * public boolean accept(File f, String s) {
				 * s = s.toLowerCase();
				 * if (s.endsWith("xls"))
				 * return true;
				 * return false;
				 * }
				 * });
				 * fd.setVisible(true);
				 * String filename = fd.getDirectory() + File.separator + fd.getFile();
				 * fl = new File(filename);
				 * }
				 */
				if (fl != null) {
					if (fl.exists()) {
						int res = JOptionPane.showConfirmDialog(null, "Are you sure you want to add to existing file?",
								"Confirm File Append", JOptionPane.YES_NO_OPTION);
						if (res == JOptionPane.YES_OPTION) {
							xlstf.setText(Utils.guaranteeExtension(fl.toString(), "xls"));
							// JdH Sadly fl.canWrite() can't see if the file is opened by another process
							// (like Excel) so the trick below is used
							try {
								RandomAccessFile raf = new RandomAccessFile(fl, "rw");
								FileChannel channel = raf.getChannel();
								channel.close();// JdH or file will stay locked
								raf.close();
							} catch (FileNotFoundException e4) {
								JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
										"Can't open file for writing!\nMaybe it is open in Excel...");
								xlstf.setText("");
								e4.printStackTrace();
								return;
							} catch (IOException e1) {
								e1.printStackTrace();
							}
							/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
						} else
							xlstf.setText("");
						return;
					} else
						xlstf.setText(Utils.guaranteeExtension(fl.toString(), "xls"));
				}
			}
		});

		aggxlsbrowseButton.setEnabled(false);
		if ((type == EXPORTDATA) || (type == BATCHANALYSIS)) {
			convButton.setEnabled(false);
		} else {
			convButton.setEnabled(true);
		}

		ActionListener CheckActionListner = (new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JCheckBox source = (JCheckBox) e.getSource();
				if (source == exportraw) {
					ecgonly = source.isSelected();
				}

				if (source == exportrawE)
					ecgonlyE = source.isSelected();

				if (source == aggxlsCheck) {// XLS browse button
					aggxlsbrowseButton.setEnabled(source.isSelected());
				}
				if (source == aggxlsTCheck) {// XLS browse button
					aggxlsTbrowseButton.setEnabled(source.isSelected());
				}

				if (source == signalfilescheck) {
					exportraw.setEnabled(source.isSelected());
					cb.setEnabled(source.isSelected());
					samplingtime.setEnabled(source.isSelected());
				}

				if (source == signalfilescheckE) {
					exportrawE.setEnabled(source.isSelected());
					cb2.setEnabled(source.isSelected());
					samplingtimeE.setEnabled(source.isSelected());
				}

				if (source == signalfilescheckB) {
					cb3.setEnabled(source.isSelected());
				}

				if (source == aggascCheck) {// ASCCI browse button
					aggascbrowseButton.setEnabled(source.isSelected());
				}
				if (source == aggascTCheck) {// ASCCI browse button
					aggascTbrowseButton.setEnabled(source.isSelected());
				}
				if (source == aggascRSRCheck) {// ASCCI browse button
					aggascRSRbrowseButton.setEnabled(source.isSelected());
				}

				if (source == aggexportbeatFiletoASCII) {// ASCCI browse button
					aggascBeatbrowseButton.setEnabled(source.isSelected());
				}

				if (xlsCheck.isSelected() || aggxlsCheck.isSelected() || ascCheck.isSelected()
						|| ascRSRCheck.isSelected() ||
						aggascCheck.isSelected() || aggascRSRCheck.isSelected() || signalfilescheck.isSelected() ||
						signalfilescheckE.isSelected() || exportbeatFiletoASCII.isSelected()
						|| aggexportbeatFiletoASCII.isSelected() || (TYPEOFANALYSIS >= 1) ||
						xlsTCheck.isSelected() || aggxlsTCheck.isSelected() || ascTCheck.isSelected()
						|| aggascTCheck.isSelected() || signalfilescheckB.isSelected())
					convButton.setEnabled(true);// Batch button
				else
					convButton.setEnabled(false);
			}
		});

		aggxlsCheck.addActionListener(CheckActionListner);
		xlsCheck.addActionListener(CheckActionListner);

		xlsPanel.add(xlsCheck);
		xlsPanel.add(aggxlsCheck);
		xlsPanel.add(new JLabel(""));
		xlsPanel.add(xlstf);
		xlsPanel.add(aggxlsbrowseButton);
		xlsPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		xlsPanel.setBorder(BorderFactory.createTitledBorder("Export to Excel"));

		// JdH ASCII part:
		JPanel ascPanel = new JPanel();
		if (type == EXPORTDATA) {
			ascPanel.setLayout(new GridLayout(7, 1));
		} else if (type == BATCHANALYSIS) {
			ascPanel.setLayout(new GridLayout(1, 1));
		}
		JPanel panel1 = new JPanel(new GridLayout(0, 5));
		ascCheck = new JCheckBox("Export label data files");
		ascCheck.setSelected(false);

		signalfilescheck = new JCheckBox("Export Signals");
		signalfilescheck.setSelected(false);

		exportbeatFiletoASCII = new JCheckBox("Export Beats to ASCII File");
		exportbeatFiletoASCII.setSelected(false);

		includeEventInformation = new JCheckBox("Include Event Information");
		includeEventInformation.setSelected(false);

		aggexportbeatFiletoASCII = new JCheckBox("Aggregate Beats to single ASCII File");
		aggexportbeatFiletoASCII.setSelected(false);

		ascRSRCheck = new JCheckBox("Export breaths");
		ascRSRCheck.setSelected(false);

		aggascRSRCheck = new JCheckBox("Aggregate breaths to single ASCII file");
		aggascRSRCheck.setSelected(false);

		asctfRSR = new JTextField(40);
		asctfRSR.setEnabled(false);

		asctfBeat = new JTextField(40);
		asctfBeat.setEnabled(false);

		ArrayList<Ams7fsChannelInfo> channelInfo = CurrentOpenData.getInstance().getChannelInfo();
		List<String> optionList = new ArrayList<String>();
		ArrayList<String> addedChannels = new ArrayList<String>();
		CurrentOpenData cod = CurrentOpenData.getInstance();
		String[] channels = { "ECG", "DZDT", "SCL", "DZ", "Z0", "MXR", "XMT", "MYR", "YMT", "MZR", "ZMT", "MYA",
				"BAT" };
		for (String chan : channels) {
			if (cod.channelExists(chan)) {
				optionList.add(chan);
				addedChannels.add(chan);
			}
		}
		for (Ams7fsChannelInfo s : channelInfo) {
			if (addedChannels.contains(s.getSzID()) == false) {
				optionList.add(s.getSzID());
			}
		}
		String[] options = new String[optionList.size()]; // {"DZ", "ECG", "MZR", "MXR", "SCL", "BAT", "Z0", "MYA",
															// "MYR", "DZDT"};
		int i = 0;
		for (String option : optionList) {
			options[i] = option;
			i++;
		}
		if (options.length == 0)
			options = channels;

		cb = new JComboBox(options);
		cb.setSelectedIndex(0);
		cb.setEnabled(false);

		aggascCheck = new JCheckBox("Aggregate label data to single ASCII file");
		aggascCheck.setSelected(false);
		asctf = new JTextField(40);
		asctf.setEnabled(false);

		samplingtime = new JTextField(10);
		samplingtime.setEnabled(false);
		samplingtime.setEditable(false);
		samplingtime.setFont(new Font("Arial", Font.BOLD, 12));
		samplingtime.setText("1000 Hz");

		/*
		 * milliseconds = new JTextField(5);
		 * milliseconds.setText("Hertz");
		 * milliseconds.setForeground(Color.RED);
		 * milliseconds.setFont(new Font("Arial", Font.BOLD,12));
		 * milliseconds.setEditable(false);
		 */
		exportraw = new JCheckBox("Export only raw signal");
		exportraw.setEnabled(false);

		aggascbrowseButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// String OSname = System.getProperty("os.name");
				File fl = null;
				// if(!OSname.contains("Mac")){
				FileChooserWithSavedDir fc = new FileChooserWithSavedDir();
				fc.setFileFilter(new FileFilter() {

					@Override
					public boolean accept(File f) {
						if (f.isDirectory())
							return true;
						String ext = Utils.getExtension(f);
						if (ext == null)
							return false;
						return Utils.getExtension(f).equals("lbldat");
					}

					@Override
					public String getDescription() {
						return "ASCII files";
					}
				});

				int retVal = fc.showSaveDialog(MainFrame.getInstance().getMainFrame());
				if (retVal == JFileChooser.APPROVE_OPTION)
					fl = fc.getSelectedFile();
				/*
				 * } else {
				 * FileDialogWithSavedDir fd = new
				 * FileDialogWithSavedDir(MainFrame.getInstance().getMainFrame(),
				 * FileDialog.SAVE);
				 * fd.setFilenameFilter(new FilenameFilter() {
				 * 
				 * @Override
				 * public boolean accept(File f, String s) {
				 * s = s.toLowerCase();
				 * if (s.endsWith("lbldat"))
				 * return true;
				 * return false;
				 * }
				 * });
				 * fd.setVisible(true);
				 * String filename = fd.getDirectory() + File.separator + fd.getFile();
				 * fl = new File(filename);
				 * }
				 */
				if (fl != null) {
					if (fl.exists()) {
						int res = JOptionPane.showConfirmDialog(null, "Are you sure you want to add to existing file?",
								"Confirm File Append", JOptionPane.YES_NO_OPTION);
						if (res == JOptionPane.YES_OPTION)
							asctf.setText(Utils.guaranteeExtension(fl.toString(), "lbldat"));
						else
							return;
					} else
						asctf.setText(Utils.guaranteeExtension(fl.toString(), "lbldat"));
				}
			}
		});

		aggascRSRbrowseButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// String OSname = System.getProperty("os.name");
				File fl = null;
				// if(!OSname.contains("Mac")){
				FileChooserWithSavedDir fc = new FileChooserWithSavedDir();
				fc.setFileFilter(new FileFilter() {

					@Override
					public boolean accept(File f) {
						if (f.isDirectory())
							return true;
						String ext = Utils.getExtension(f);
						if (ext == null)
							return false;
						return Utils.getExtension(f).equals("rsr");
					}

					@Override
					public String getDescription() {
						return "ASCII files";
					}
				});

				int retVal = fc.showSaveDialog(MainFrame.getInstance().getMainFrame());
				if (retVal == JFileChooser.APPROVE_OPTION)
					fl = fc.getSelectedFile();
				/*
				 * } else {
				 * FileDialogWithSavedDir fd = new
				 * FileDialogWithSavedDir(MainFrame.getInstance().getMainFrame(),
				 * FileDialog.SAVE);
				 * fd.setFilenameFilter(new FilenameFilter() {
				 * 
				 * @Override
				 * public boolean accept(File f, String s) {
				 * s = s.toLowerCase();
				 * if (s.endsWith("rsr"))
				 * return true;
				 * return false;
				 * }
				 * });
				 * fd.setVisible(true);
				 * String filename = fd.getDirectory() + File.separator + fd.getFile();
				 * fl = new File(filename);
				 * }
				 */
				if (fl != null) {
					if (fl.exists()) {
						int res = JOptionPane.showConfirmDialog(null, "Are you sure you want to add to existing file?",
								"Confirm File Append", JOptionPane.YES_NO_OPTION);
						if (res == JOptionPane.YES_OPTION)
							asctfRSR.setText(Utils.guaranteeExtension(fl.toString(), "rsr"));
						else
							return;
					} else
						asctfRSR.setText(Utils.guaranteeExtension(fl.toString(), "rsr"));
				}
			}
		});

		aggascBeatbrowseButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// String OSname = System.getProperty("os.name");
				File fl = null;
				// if(!OSname.contains("Mac")){
				FileChooserWithSavedDir fc = new FileChooserWithSavedDir();
				fc.setFileFilter(new FileFilter() {

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
						return "ASCII files";
					}
				});

				int retVal = fc.showSaveDialog(MainFrame.getInstance().getMainFrame());
				if (retVal == JFileChooser.APPROVE_OPTION)
					fl = fc.getSelectedFile();
				/*
				 * } else {
				 * FileDialogWithSavedDir fd = new
				 * FileDialogWithSavedDir(MainFrame.getInstance().getMainFrame(),
				 * FileDialog.SAVE);
				 * fd.setFilenameFilter(new FilenameFilter() {
				 * 
				 * @Override
				 * public boolean accept(File f, String s) {
				 * s = s.toLowerCase();
				 * if (s.endsWith("txt"))
				 * return true;
				 * return false;
				 * }
				 * });
				 * fd.setVisible(true);
				 * String filename = fd.getDirectory() + File.separator + fd.getFile();
				 * fl = new File(filename);
				 * }
				 */
				if (fl != null) {
					if (fl.exists()) {
						int res = JOptionPane.showConfirmDialog(null, "Are you sure you want to add to existing file?",
								"Confirm File Append", JOptionPane.YES_NO_OPTION);
						if (res == JOptionPane.YES_OPTION)
							asctfBeat.setText(Utils.guaranteeExtension(fl.toString() + "_beat", "txt"));
						else
							return;
					} else
						asctfBeat.setText(Utils.guaranteeExtension(fl.toString() + "_beat", "txt"));
				}
			}
		});
		aggascBeatbrowseButton.setEnabled(false);
		aggascbrowseButton.setEnabled(false);
		aggascRSRbrowseButton.setEnabled(false);

		aggascCheck.addActionListener(CheckActionListner);
		ascCheck.addActionListener(CheckActionListner);
		ascRSRCheck.addActionListener(CheckActionListner);
		aggascRSRCheck.addActionListener(CheckActionListner);
		signalfilescheck.addActionListener(CheckActionListner);
		exportraw.addActionListener(CheckActionListner);
		exportbeatFiletoASCII.addActionListener(CheckActionListner);
		includeEventInformation.addActionListener(CheckActionListner);
		aggexportbeatFiletoASCII.addActionListener(CheckActionListner);

		panel1.add(ascCheck);
		panel1.add(aggascCheck);
		panel1.add(new JLabel(""));
		panel1.add(asctf);
		panel1.add(aggascbrowseButton);
		ascPanel.add(panel1);
		ascPanel.add(new JSeparator());

		JPanel panel2 = new JPanel(new GridLayout(0, 5));

		panel2.add(signalfilescheck);
		panel2.add(exportraw);
		panel2.add(cb);
		panel2.add(samplingtime);

		// panel2.add(milliseconds);

		JPanel panel3 = new JPanel(new GridLayout(0, 5));
		panel3.add(ascRSRCheck);
		panel3.add(aggascRSRCheck);
		panel3.add(new JLabel(""));
		panel3.add(asctfRSR);
		panel3.add(aggascRSRbrowseButton);

		JPanel panel4 = new JPanel(new GridLayout(0, 5));
		panel4.add(exportbeatFiletoASCII);
		panel4.add(includeEventInformation);
		panel4.add(aggexportbeatFiletoASCII);
		panel4.add(asctfBeat);
		panel4.add(aggascBeatbrowseButton);

		if (type == EXPORTDATA) {
			ascPanel.add(panel2);
			ascPanel.add(new JSeparator());
			ascPanel.add(panel3);
			ascPanel.add(new JSeparator());
			ascPanel.add(panel4);
		}
		cb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if ((cb.getSelectedIndex() == 0) || (cb.getSelectedIndex() == 1) || (cb.getSelectedIndex() == 2)
						|| (cb.getSelectedIndex() == 3) || (cb.getSelectedIndex() == 8)
						|| (cb.getSelectedIndex() == 9)) {
					samplingtime.setText("1000 Hz");
				} else if ((cb.getSelectedIndex() == 5) || (cb.getSelectedIndex() == 7)) {
					samplingtime.setText("1 Hz");
				} else if ((cb.getSelectedIndex() == 6)) {
					samplingtime.setText("250 Hz");
				} else {
					samplingtime.setText("10 Hz");
				}
			}
		});

		analyzecombobox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (analyzecombobox.getSelectedIndex() == 1) {
					setAnalysisType(1);
					TYPEOFANALYSIS = 1;
				} else if (analyzecombobox.getSelectedIndex() == 2) {
					TYPEOFANALYSIS = 2;
					setAnalysisType(2);
				} else if (analyzecombobox.getSelectedIndex() == 3) {
					TYPEOFANALYSIS = 3;
					setAnalysisType(3);
				} else {
					TYPEOFANALYSIS = 0;
				}

				if (TYPEOFANALYSIS >= 1) {
					convButton.setEnabled(true);
				} else {
					convButton.setEnabled(false);
				}
			}

		});

		ascPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		ascPanel.setBorder(BorderFactory.createTitledBorder("Export to ASCII"));

		// JdH END ASCII part

		// QH EDF part:
		JPanel edfPanel = new JPanel();
		if (type == EXPORTDATA) {
			edfPanel.setLayout(new GridLayout(1, 1));
		}

		JPanel panel5 = new JPanel(new GridLayout(0, 5));

		signalfilescheckE = new JCheckBox("Export Signals");
		signalfilescheckE.setSelected(false);
		signalfilescheckE.addActionListener(CheckActionListner);

		cb2 = new JComboBox(options);
		cb2.setSelectedIndex(0);
		cb2.setEnabled(false);
		cb2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if ((cb2.getSelectedIndex() == 0) || (cb2.getSelectedIndex() == 1) || (cb2.getSelectedIndex() == 2)
						|| (cb2.getSelectedIndex() == 3) || (cb2.getSelectedIndex() == 8)
						|| (cb2.getSelectedIndex() == 9)) {
					samplingtimeE.setText("1000 Hz");
				} else if ((cb2.getSelectedIndex() == 5) || (cb2.getSelectedIndex() == 7)) {
					samplingtimeE.setText("1 Hz");
				} else if ((cb2.getSelectedIndex() == 6)) {
					samplingtimeE.setText("250 Hz");
				} else {
					samplingtimeE.setText("10 Hz");
				}
			}
		});

		samplingtimeE = new JTextField(10);
		samplingtimeE.setEnabled(false);
		samplingtimeE.setEditable(false);
		samplingtimeE.setFont(new Font("Arial", Font.BOLD, 12));
		samplingtimeE.setText("1000 Hz");

		exportrawE = new JCheckBox("Export only raw signal");
		exportrawE.setEnabled(false);
		exportrawE.addActionListener(CheckActionListner);

		panel5.add(signalfilescheckE);
		panel5.add(exportrawE);
		panel5.add(cb2);
		panel5.add(samplingtimeE);

		edfPanel.add(panel5);
		edfPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		edfPanel.setBorder(BorderFactory.createTitledBorder("Export to EDF"));
		// QH END EDF part

		// QH bin file part:
		JPanel binPanel = new JPanel();
		if (type == EXPORTDATA) {
			binPanel.setLayout(new GridLayout(1, 1));
		}

		JPanel panel6 = new JPanel(new GridLayout(0, 5));

		signalfilescheckB = new JCheckBox("Export Signals");
		signalfilescheckB.setSelected(false);
		signalfilescheckB.addActionListener(CheckActionListner);

		cb3 = new JComboBox(options);
		cb3.setSelectedIndex(0);
		cb3.setEnabled(false);

		panel6.add(signalfilescheckB);
		panel6.add(cb3);

		binPanel.add(panel6);
		binPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		binPanel.setBorder(BorderFactory.createTitledBorder("Export to EDF"));
		// QH END bin file part

		// QH timed label Excel part
		JPanel xlsTPanel = new JPanel(new GridLayout(0, 5));
		xlsTCheck = new JCheckBox("Export timed label data");
		xlsTCheck.setSelected(false);

		aggxlsTCheck = new JCheckBox("Aggregate timed label data to single Excel file");
		aggxlsTCheck.setSelected(false);
		JPanel xlsTime = new JPanel(new GridLayout(0, 2));
		numberFieldX = new JFormattedTextField(60);
		numberFieldX.setColumns(10);
		xlsTime.add(new JLabel("Set time width (seconds): "));
		xlsTime.add(numberFieldX);
		xlstfT = new JTextField(40);
		xlstfT.setEnabled(false);

		aggxlsTbrowseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// String OSname = System.getProperty("os.name");
				File fl = null;
				// if(!OSname.contains("Mac")){
				FileChooserWithSavedDir fc = new FileChooserWithSavedDir();
				fc.setFileFilter(new FileFilter() {

					@Override
					public boolean accept(File f) {
						if (f.isDirectory())
							return true;
						String ext = Utils.getExtension(f);
						if (ext == null)
							return false;
						return Utils.getExtension(f).equals("xls");
					}

					@Override
					public String getDescription() {
						return "Excel files";
					}
				});

				int retVal = fc.showSaveDialog(MainFrame.getInstance().getMainFrame());
				if (retVal == JFileChooser.APPROVE_OPTION)
					fl = fc.getSelectedFile();
				if (fl != null) {
					if (fl.exists()) {
						int res = JOptionPane.showConfirmDialog(null, "Are you sure you want to add to existing file?",
								"Confirm File Append", JOptionPane.YES_NO_OPTION);
						if (res == JOptionPane.YES_OPTION) {
							xlstfT.setText(Utils.guaranteeExtension(fl.toString(), "xls"));
							/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
							// JdH Sadly fl.canWrite() can't see if the file is opened by another process
							// (like Excel) so the trick below is used
							try {
								RandomAccessFile raf = new RandomAccessFile(fl, "rw");
								FileChannel channel = raf.getChannel();
								channel.close();// JdH or file will stay locked
								raf.close();
							} catch (FileNotFoundException e4) {
								JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
										"Can't open file for writing!\nMaybe it is open in Excel...");
								xlstfT.setText("");
								e4.printStackTrace();
								return;
							} catch (IOException e1) {
								e1.printStackTrace();
							}
							/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
						} else
							xlstfT.setText("");
						return;
					} else
						xlstfT.setText(Utils.guaranteeExtension(fl.toString(), "xls"));
				}
			}
		});

		aggxlsTbrowseButton.setEnabled(false);

		aggxlsTCheck.addActionListener(CheckActionListner);
		xlsTCheck.addActionListener(CheckActionListner);

		xlsTPanel.add(xlsTCheck);
		xlsTPanel.add(aggxlsTCheck);
		xlsTPanel.add(xlsTime);
		xlsTPanel.add(xlstfT);
		xlsTPanel.add(aggxlsTbrowseButton);
		xlsTPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		xlsTPanel.setBorder(BorderFactory.createTitledBorder("Export timed labels to Excel"));
		// QH END timed label Excel part

		// QH timed label ASCII part
		JPanel ascTPanel = new JPanel(new GridLayout(0, 5));
		ascTCheck = new JCheckBox("Export timed label data");
		ascTCheck.setSelected(false);

		aggascTCheck = new JCheckBox("Aggregate timed label data to single ASCII file");
		aggascTCheck.setSelected(false);
		asctfT = new JTextField(40);
		asctfT.setEnabled(false);
		JPanel ascTime = new JPanel(new GridLayout(0, 2));
		numberFieldA = new JFormattedTextField(60);
		numberFieldA.setColumns(10);
		ascTime.add(new JLabel("Set time width (seconds): "));
		ascTime.add(numberFieldA);

		aggascTbrowseButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// String OSname = System.getProperty("os.name");
				File fl = null;
				// if(!OSname.contains("Mac")){
				FileChooserWithSavedDir fc = new FileChooserWithSavedDir();
				fc.setFileFilter(new FileFilter() {

					@Override
					public boolean accept(File f) {
						if (f.isDirectory())
							return true;
						String ext = Utils.getExtension(f);
						if (ext == null)
							return false;
						return Utils.getExtension(f).equals("lbldat");
					}

					@Override
					public String getDescription() {
						return "ASCII files";
					}
				});

				int retVal = fc.showSaveDialog(MainFrame.getInstance().getMainFrame());
				if (retVal == JFileChooser.APPROVE_OPTION)
					fl = fc.getSelectedFile();
				if (fl != null) {
					if (fl.exists()) {
						int res = JOptionPane.showConfirmDialog(null, "Are you sure you want to add to existing file?",
								"Confirm File Append", JOptionPane.YES_NO_OPTION);
						if (res == JOptionPane.YES_OPTION)
							asctfT.setText(Utils.guaranteeExtension(fl.toString(), "lbldat"));
						else
							return;
					} else
						asctfT.setText(Utils.guaranteeExtension(fl.toString(), "lbldat"));
				}
			}
		});

		aggascTbrowseButton.setEnabled(false);

		aggascTCheck.addActionListener(CheckActionListner);
		ascTCheck.addActionListener(CheckActionListner);

		ascTPanel.add(ascTCheck);
		ascTPanel.add(aggascTCheck);
		ascTPanel.add(ascTime);
		ascTPanel.add(asctfT);
		ascTPanel.add(aggascTbrowseButton);
		ascTPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		ascTPanel.setBorder(BorderFactory.createTitledBorder("Export timed labels to ASCII"));
		// QH END timed label ASCII part

		if (type == EXPORTDATA) {
			diag.add(xlsPanel);
			diag.add(ascPanel);
			diag.add(xlsTPanel);
			diag.add(ascTPanel);
			diag.add(edfPanel);
			diag.add(binPanel);
		} else if (type == BATCHANALYSIS) {
			diag.add(analyzePanel);
			diag.add(xlsPanel);
			diag.add(ascPanel);
		}

		JPanel butPanel = new JPanel();
		convButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// diag.setVisible(false);//JdH this is done after the first checking part of
				// convertAllFiles()
				convertFiles.clear();
				listConvertFiles(new File(tf.getText()), subCheck.isSelected());
				convertAllFiles();
			}
		});
		JButton cancButton = new JButton("Cancel");
		cancButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				diag.setVisible(false);
			}
		});

		butPanel.add(convButton);
		butPanel.add(cancButton);
		diag.add(butPanel);

		diag.pack();
		diag.setResizable(true);// JdH So it fits on MacOS
		diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
	}

	private void addFileToList(File file) {

		String baseName = file.getAbsolutePath();
		String amsdataName = "";
		if (type == CONVERT5FS) {
			// JdH 2012-01-10 BEGIN get subject ID from .5FS file because
			// "CurrentOpenData.getInstance().getFileHeader().getSzSubjectID()" doesn't work
			// at this point
			FileInputStream fis = null;
			InputStreamReader isr = null;
			int size5fsStart = 8, size7fsStart = 2;
			String ext = Utils.getExtension(file);
			String amsdataExt = "";
			String subID = "";
			try {
				fis = new FileInputStream(file.getAbsolutePath());
				isr = new InputStreamReader(fis, "UTF8");

				if (ext.equals("5fs")) {
					char[] FileStart = new char[size5fsStart];
					isr.read(FileStart, 0, size5fsStart);
					amsdataExt = "amsdata";
					// JdH 2012-01-10: there must be a better way to perform an equal test, but I
					// was done searching and this works!
					char check[] = { 'A', 'M', 'S', '2', 01, 02, 03, 04 };
					boolean bSame = false;
					for (int i = 0; i < size5fsStart; i++) {
						if (check[i] == FileStart[i])
							bSame = true;
						else
							bSame = false;
					}
					if (bSame) {
						char[] buffer = new char[128];
						isr.read(buffer, 0, 128);
						int SubIDoffset = 112;
						subID = "";
						for (int i = 0; i < 12 & buffer[i + SubIDoffset] != 0; i++)
							subID = subID + buffer[i + SubIDoffset];
					} else {
						System.out.println("ERROR: not a valid .5FS file to get Subject ID from. :ERROR");
						throw new IOException();
					}
				} else {
					char[] FileStart = new char[size7fsStart];
					isr.read(FileStart, 0, size7fsStart);
					amsdataExt = "amsdatai";
					// QH 2022-08-23: equal test copied from JdH!
					char check[] = { 'H', 'P' };
					boolean bSame = false;
					for (int i = 0; i < size7fsStart; i++) {
						if (check[i] == FileStart[i])
							bSame = true;
						else
							bSame = false;
					}
					if (bSame) {
						subID = "Dummy";
					} else {
						System.out.println("ERROR: not a valid .7FS file to batch. :ERROR");
						throw new IOException();
					}
				}
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
				if (isr != null) {
					try {
						isr.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			// JdH 2012-01-10 END get subject ID from .5FS file because
			// "CurrentOpenData.getInstance().getFileHeader().getSzSubjectID()" doesn't work
			// at this point

			if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.SAVESIDINFILENAME) != 0) {
				amsdataName = Utils.guaranteeExtensionAndPrepend(baseName, amsdataExt, subID + "_");
			} else {
				baseName = Utils.removeExtension(file.getAbsolutePath());
				amsdataName = Utils.guaranteeExtension(baseName, amsdataExt);
			}

		} else if ((type == EXPORTDATA) || (type == BATCHANALYSIS)) {
			amsdataName = Utils.guaranteeExtension(baseName, "xls");
		}
		File amsdataFile = new File(amsdataName);
		if (amsdataFile.exists() == false || overwriteCheck.isSelected() == true || aggxlsCheck.isSelected()) {
			convertFiles.add(file);
		}
	}

	protected synchronized void addNewAverageTime(double time, long fileSize) {
		totalFileSizeDone += fileSize;
		fileSizeLeft -= fileSize;
		curAverage = time / fileSize;
	}

	public void appendNewText(String txt, JTextArea tArea) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				tArea.setText(tArea.getText() + txt);
			}
		});
	}

	protected void convertAllFiles() {
		if (convertFiles.isEmpty()) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					"No files to batch!\nOr files might already exist.");
			return;
		}
		if (aggxlsCheck.isSelected() && xlstf.getText().compareTo("") == 0) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					"No name for single Excel file to aggregate data.");
			return;
		}
		if (aggascCheck.isSelected() && asctf.getText().compareTo("") == 0) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					"No name for single ASCII file to aggregate data.");
			return;
		}
		if (aggxlsTCheck.isSelected() && xlstfT.getText().compareTo("") == 0) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					"No name for single Excel file to aggregate time label data.");
			return;
		}
		if (aggascTCheck.isSelected() && asctfT.getText().compareTo("") == 0) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					"No name for single ASCII file to aggregate time label data.");
			return;
		}
		if (aggascRSRCheck.isSelected() && asctfRSR.getText().compareTo("") == 0) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					"No name for single ASCII file to aggregate data.");
			return;
		}
		if (aggexportbeatFiletoASCII.isSelected() && asctfBeat.getText().compareTo("") == 0) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					"No name for single ASCII file to aggregate data.");
			return;
		}
		diag.setVisible(false);// JdH Now close diag to give the user a chance to correct the problems above

		diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Batching files", true);
		final JProgressBar progressBar = new JProgressBar(0, 1000);
		String tmpString = "";
		for (int i = 0; i < 64; i++)
			tmpString += "0";

		JPanel errorPanel = new JPanel();
		errorPanel.setBorder(new TitledBorder(new EtchedBorder(), "Collective Warnings and Errors"));

		ta = new JTextArea(10, 80);
		ta.setEditable(false);
		ta.setBorder(new TitledBorder(new EtchedBorder(), "Warnings"));
		JScrollPane scroll = new JScrollPane(ta);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		errorta = new JTextArea(10, 80);
		errorta.setEditable(false);
		errorta.setBorder(new TitledBorder(new EtchedBorder(), "Errors"));
		errorta.setForeground(Color.RED);// JdH Error text always red
		JScrollPane scroll2 = new JScrollPane(errorta);
		scroll2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		errorPanel.add(scroll);
		errorPanel.add(scroll2);

		final JLabel timeLeftLabel = new JLabel("TEMPORARY");// JdH create timeLeftLabel here so it can be used in:
																// convThread run()

		JPanel notePanel = new JPanel();
		final JLabel note = new JLabel(tmpString);
		note.setHorizontalAlignment(SwingConstants.CENTER);
		notePanel.add(note);
		diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));
		diag.add(notePanel);
		diag.add(progressBar);
		diag.add(errorPanel);

		final Thread convThread = new Thread() {
			private Timer timer = new Timer();

			@SuppressWarnings("static-access")
			private void doBatch(File f) {

				if (automaticlabels.isSelected()) {
					System.out.println("Format Type:_ " + autolabelsformat.getSelectedIndex());
					if (autoLabelsFile == null) {
						JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
								"No Label Reference File is chosen.");
						setStop(true);
						diag.setVisible(false);
						return;
					}
					// The check below became unnecessary when moving away from ---Choose the import
					// labels format--- dropdown option 0. Just like the -1 in line 936
					/*
					 * if(autolabelsformat.getSelectedIndex() < 1){
					 * JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					 * "No Label Format is chosen.");
					 * setStop(true);
					 * diag.setVisible(false);
					 * return;
					 * }
					 */
				}

				if (type == CONVERT5FS) {
					String ext = Utils.getExtension(f);
					if (ext.equals("5fs")) {
						Ams5fsFile file = new Ams5fsFile(f.getAbsolutePath());
						file.setQuick(false);
						file.setProgressDialog(false);
						file.start();
						try {
							file.join();
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					} else if (ext.equals("7fs")) {
						Ams7fsFile file = new Ams7fsFile(f.getAbsolutePath());
						file.setQuick(false);
						file.setProgressDialog(false);
						file.start();
						try {
							file.join();
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					} else if (ext.equals("ams")) {
						AmsAMSFile file = new AmsAMSFile(f);
						file.start();
						try {
							file.join();
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
					SubSetFileGenerator.getInstance().interrupt();
					// OrientationFilesGenerator.getInstance().interrupt();
					ActivityClassification.getInstance().interrupt();
					String baseName = f.getAbsolutePath();
					String amsdataName = null, amsdataExt = null;
					if (ext.equals("5fs"))
						amsdataExt = "amsdata";
					else
						amsdataExt = "amsdatai";
					if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.SAVESIDINFILENAME) != 0) {
						amsdataName = Utils.guaranteeExtensionAndPrepend(baseName, amsdataExt,
								CurrentOpenData.getInstance().getFileHeader().getSzSubjectID() + "_");
					} else {
						baseName = Utils.removeExtension(f.getAbsolutePath());
						amsdataName = Utils.guaranteeExtension(baseName, amsdataExt);
					}

					if (automaticlabels.isSelected()) {
						System.out.println("Format Type:_ " + autolabelsformat.getSelectedIndex());
						if (autoLabelsFile != null) {
							MainFrame.getInstance().getTabs().setSelectedComponent(LabelTab.getInstance());
							AmsLabelSet lSet = CurrentOpenData.getInstance().getLabels();
							lSet.importFromFileusingMarkerinfo(autoLabelsFile, autolabelsformat.getSelectedIndex());
						}
						// CurrentOpenData.getInstance().saveChangeablesToDisk();
					}
					File amsdataFile = new File(amsdataName);
					AmsDataFile fl = new AmsDataFile(true, amsdataFile.getAbsolutePath(), true, 0);
					fl.setCompressed(true);
					fl.setShowProgress(false);
					CurrentOpenData.getInstance().setBatchAnalysis(true);// To prevent batch 'hang' because of No Beats
																			// Detected window
					fl.start();
					try {
						fl.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else if ((type == EXPORTDATA)) {
					AmsDataFile file = new AmsDataFile(false, f.getAbsolutePath(), true, 0);
					file.setQuick(true);
					CurrentOpenData.getInstance().setBatchAnalysis(true);// To prevent batch 'hang' because of No Beats
																			// Detected window
					file.setShowProgress(false);
					boolean fileStarted = false;

					SubSetFileGenerator.getInstance().interrupt();
					// OrientationFilesGenerator.getInstance().interrupt();
					ActivityClassification.getInstance().interrupt();

					String baseName = Utils.removeExtension(f.getAbsolutePath());
					String xlsName = Utils.guaranteeExtension(baseName, "xls");
					String ascName = Utils.guaranteeExtension(baseName, "lbldat");
					String ascRSRName = Utils.guaranteeExtension(baseName, "rsr");
					String asciiNameforSignals = Utils
							.guaranteeExtension(baseName + "_" + cb.getSelectedItem().toString(), "txt");
					String ascBeatFileName = Utils.guaranteeExtension(baseName + "_beat", "txt");
					String edfNameforSignals = Utils
							.guaranteeExtension(baseName + "_" + cb2.getSelectedItem().toString(), "edf");
					String binNameforSignals = Utils
							.guaranteeExtension(baseName + "_" + cb3.getSelectedItem().toString(), "edf");
					String xlsTName = Utils.guaranteeExtension(baseName + "_timed", "xls");
					String ascTName = Utils.guaranteeExtension(baseName + "_timed", "lbldat");

					File flxls = new File(xlsName);
					File flasc = new File(ascName);
					File flascRSR = new File(ascRSRName);
					File flsignalsasci = new File(asciiNameforSignals);
					File flBeat = new File(ascBeatFileName);
					File flsignalsedf = new File(edfNameforSignals);
					File flsignalsbin = new File(binNameforSignals);
					File flxlsT = new File(xlsTName);
					File flascT = new File(ascTName);
					// boolean recalcNeeded = true;

					if (CurrentOpenData.getInstance().isFileInfoMissing() == true) // File is corrupted
						appendNewText(CurrentOpenData.getInstance().getDataFile().getName() + " " + "file is corrupted"
								+ "\n", errorta);
					else {

						if (xlsCheck.isSelected()) {
							if (flxls.exists() && overwriteCheck.isSelected() == false)
								appendNewText(
										flxls.getName() + " exists (and 'Overwrite existing files' not checked)\n",
										errorta);
							else {
								if (!fileStarted) {
									file.start();
									try {
										file.join();
									} catch (InterruptedException e) {
									}
									fileStarted = true;
									LabelInformationTab.getInstance().getData().setAll();
								}
								LabelInformationTab.getInstance().getData().outputDataToXLS(flxls);
								// recalcNeeded = false;
							}
						}
						if (xlsTCheck.isSelected()) {
							if (flxlsT.exists() && overwriteCheck.isSelected() == false)
								appendNewText(
										flxlsT.getName() + " exists (and 'Overwrite existing files' not checked)\n",
										errorta);
							else {
								if (!fileStarted) {
									file.start();
									try {
										file.join();
									} catch (InterruptedException e) {
									}
									fileStarted = true;
									LabelInformationTab.getInstance().getData().setAll();
								}
								double tW = ((Number) numberFieldX.getValue()).doubleValue() * 1000000.;
								LabelInformationTab.getInstance().getData().outputDataToXLS(flxlsT, tW, false);
								// recalcNeeded = false;
							}
						}

						// Export Signals (_[channel].txt)
						if (signalfilescheck.isSelected()) {
							if (flsignalsasci.exists() && overwriteCheck.isSelected() == false)
								appendNewText(
										flsignalsasci.getName()
												+ " exists (and 'Overwrite existing files' not checked)\n",
										errorta);

							else {
								if (!fileStarted) {
									file.start();
									try {
										file.join();
									} catch (InterruptedException e) {
									}
									fileStarted = true;
								}
								if (CurrentOpenData.getInstance().channelExists(cb.getSelectedItem().toString())) {// JdH
																													// if
																													// selected
																													// channel
																													// exists
									try {
										long div = CurrentOpenData.getInstance()
												.getChannelInfoFromID(cb.getSelectedItem().toString()).getDwDivider();
										double sr = 1000. * div;
										BinaryFile bf = new BinaryFile((String) cb.getSelectedItem());
										bf.outputToASCII(flsignalsasci, sr, cb.getSelectedItem().toString(), ecgonly);
										bf.close();
									} catch (Exception e) {
										e.printStackTrace();
									}
								} else
									appendNewText("Channel " + cb.getSelectedItem().toString() + " not in file "
											+ CurrentOpenData.getInstance().getDataFile().getName() + "\n", errorta);
								if (cb.getSelectedItem().equals("ECG")
										&& CurrentOpenData.getInstance().getFileHeader().getsFileID().equals("AMS2"))
									appendNewText(CurrentOpenData.getInstance().getDataFile().getName()
											+ " As noted on our website the amplitude of the ECG needs to be divided by 3.6 The output of this conversion contains this correction.\n",
											ta);
							}
						}

						File tempFile = new File(xlstf.getText());
						if (aggxlsCheck.isSelected() && xlstf.getText().compareTo("") != 0) {
							if (!fileStarted) {
								file.start();
								try {
									file.join();
								} catch (InterruptedException e) {
								}
								fileStarted = true;
								LabelInformationTab.getInstance().getData().setAll();
							}
							LabelInformationTab.getInstance().getData().appendDataToXLS(tempFile, false, -1);
						}
						tempFile = new File(xlstfT.getText());
						double tW = ((Number) numberFieldX.getValue()).doubleValue() * 1000000.;
						if (aggxlsTCheck.isSelected() && xlstfT.getText().compareTo("") != 0) {
							if (!fileStarted) {
								file.start();
								try {
									file.join();
								} catch (InterruptedException e) {
								}
								fileStarted = true;
								LabelInformationTab.getInstance().getData().setAll();
							}
							LabelInformationTab.getInstance().getData().appendDataToXLS(tempFile, false, tW);
						}

						// Export label data files (.lbldat)
						if (ascCheck.isSelected()) {
							if (flasc.exists() && overwriteCheck.isSelected() == false)
								appendNewText(
										flasc.getName() + " exists (and 'Overwrite existing files' not checked)\n",
										errorta);

							else {
								if (!fileStarted) {
									file.start();
									try {
										file.join();
									} catch (InterruptedException e) {
									}
									fileStarted = true;
									LabelInformationTab.getInstance().getData().setAll();
								}
								LabelInformationTab.getInstance().getData().outputDataToASCII(flasc);
								// recalcNeeded = false;
							}
						}
						if (ascTCheck.isSelected()) {
							if (flascT.exists() && overwriteCheck.isSelected() == false)
								appendNewText(
										flascT.getName() + " exists (and 'Overwrite existing files' not checked)\n",
										errorta);
							else {
								if (!fileStarted) {
									file.start();
									try {
										file.join();
									} catch (InterruptedException e) {
									}
									fileStarted = true;
									LabelInformationTab.getInstance().getData().setAll();
								}
								tW = ((Number) numberFieldA.getValue()).doubleValue() * 1000000.;
								LabelInformationTab.getInstance().getData().outputDataToASCII(flascT, tW, false);
								// recalcNeeded = false;
							}
						}

						// Export Beats to ASCII File (_beat.txt)
						if (exportbeatFiletoASCII.isSelected()) {
							if (flBeat.exists() && overwriteCheck.isSelected() == false)
								appendNewText(
										flBeat.getName() + " exists (and 'Overwrite existing files' not checked)\n",
										errorta);

							else {
								if (!fileStarted) {
									file.start();
									try {
										file.join();
									} catch (InterruptedException e) {
									}
									fileStarted = true;
								}
								CurrentOpenData.getInstance().getBeatSet().exportToASCII(flBeat, false,
										includeEventInformation.isSelected());
								// recalcNeeded = false;
							}
						}
						tempFile = new File(asctfBeat.getText());
						if (aggexportbeatFiletoASCII.isSelected() && asctfBeat.getText().compareTo("") != 0) {
							if (!fileStarted) {
								file.start();
								try {
									file.join();
								} catch (InterruptedException e) {
								}
								fileStarted = true;
							}
							CurrentOpenData.getInstance().getBeatSet().exportToASCII(tempFile, true,
									includeEventInformation.isSelected());
						}

						// Export RSR files (.rsr)
						if (ascRSRCheck.isSelected()) {
							if (flascRSR.exists() && overwriteCheck.isSelected() == false)
								appendNewText(
										flascRSR.getName() + " exists (and 'Overwrite existing files' not checked)\n",
										errorta);

							else {
								if (!fileStarted) {
									file.start();
									try {
										file.join();
									} catch (InterruptedException e) {
									}
									fileStarted = true;
								}
								CurrentOpenData.getInstance().getRespSet().toRSRFile(flascRSR, false);
								// recalcNeeded = false;
							}
						}
						tempFile = new File(asctf.getText());
						if (aggascCheck.isSelected() && asctf.getText().compareTo("") != 0) {
							if (!fileStarted) {
								file.start();
								try {
									file.join();
								} catch (InterruptedException e) {
								}
								fileStarted = true;
								LabelInformationTab.getInstance().getData().setAll();
							}
							LabelInformationTab.getInstance().getData().outputDataToASCII(tempFile, -1, false,
									true);
						}
						tempFile = new File(asctfT.getText());
						tW = ((Number) numberFieldA.getValue()).doubleValue() * 1000000.;
						if (aggascTCheck.isSelected() && asctfT.getText().compareTo("") != 0) {
							if (!fileStarted) {
								file.start();
								try {
									file.join();
								} catch (InterruptedException e) {
								}
								fileStarted = true;
								LabelInformationTab.getInstance().getData().setAll();
							}
							LabelInformationTab.getInstance().getData().outputDataToASCII(tempFile, tW, false,
									true);
						}
						tempFile = new File(asctfRSR.getText());
						if (aggascRSRCheck.isSelected() && asctfRSR.getText().compareTo("") != 0) {
							if (!fileStarted) {
								file.start();
								try {
									file.join();
								} catch (InterruptedException e) {
								}
								fileStarted = true;
							}
							CurrentOpenData.getInstance().getRespSet().toRSRFile(tempFile, true);
						}

						// Export Signals (EDF)
						if (signalfilescheckE.isSelected()) {
							if (flsignalsedf.exists() && overwriteCheck.isSelected() == false)
								appendNewText(
										flsignalsedf.getName()
												+ " exists (and 'Overwrite existing files' not checked)\n",
										errorta);

							else {
								if (!fileStarted) {
									file.start();
									try {
										file.join();
									} catch (InterruptedException e) {
									}
									fileStarted = true;
								}
								if (CurrentOpenData.getInstance().channelExists(cb2.getSelectedItem().toString())) {// JdH
																													// if
																													// selected
																													// channel
																													// exists
									try {
										long div = CurrentOpenData.getInstance()
												.getChannelInfoFromID(cb2.getSelectedItem().toString()).getDwDivider();
										double sr = 1000. * div;
										BinaryFile bf = new BinaryFile((String) cb2.getSelectedItem());
										bf.outputToEDF(flsignalsedf, sr, cb2.getSelectedItem().toString(), ecgonlyE);
										bf.close();
									} catch (Exception e) {
										e.printStackTrace();
									}
								} else
									appendNewText("Channel " + cb2.getSelectedItem().toString() + " not in file "
											+ CurrentOpenData.getInstance().getDataFile().getName() + "\n", errorta);
								if (signalfilescheckE.isSelected() && cb2.getSelectedItem().equals("ECG")
										&& CurrentOpenData.getInstance().getFileHeader().getsFileID().equals("AMS2"))
									appendNewText(CurrentOpenData.getInstance().getDataFile().getName()
											+ " As noted on our website the amplitude of the ECG needs to be divided by 3.6 The output of this conversion contains this correction.\n",
											ta);
							}
						}

						// Export Signals (bin)
						if (signalfilescheckB.isSelected()) {
							if (flsignalsbin.exists()
									&& overwriteCheck.isSelected() == false)
								appendNewText(flsignalsbin.getName()
										+ " exists (and 'Overwrite existing files' not checked)\n", errorta);

							else {
								if (CurrentOpenData.getInstance().channelExists(cb3.getSelectedItem().toString())) {// JdH
																													// if
																													// selected
																													// channel
																													// exists
									File inFile = new File(CurrentOpenData.getInstance().getFilePath(),
											cb3.getSelectedItem() + ".bin");
									try {
										Files.copy(inFile.toPath(), flsignalsbin.toPath());
									} catch (Exception e) {
										e.printStackTrace();
									}
								} else
									appendNewText("Channel " + cb3.getSelectedItem().toString() + " not in file "
											+ CurrentOpenData.getInstance().getDataFile().getName() + "\n", errorta);
								if (signalfilescheckB.isSelected() && cb3.getSelectedItem().equals("ECG")
										&& CurrentOpenData.getInstance().getFileHeader().getsFileID().equals("AMS2"))
									appendNewText(CurrentOpenData.getInstance().getDataFile().getName()
											+ " As noted on our website the amplitude of the ECG needs to be divided by 3.6 The output of this conversion contains this correction.\n",
											ta);
							}
						}

						if (fileStarted && CurrentOpenData.getInstance().getLabels().getLabels().isEmpty())
							appendNewText(
									CurrentOpenData.getInstance().getDataFile().getName() + " " + "file has no labels"
											+ "\n",
									ta);
						if (fileStarted && CurrentOpenData.getInstance().channelExists("ECG")
								&& (CurrentOpenData.getInstance().getBeatSet().getBeats().isEmpty()))
							appendNewText(CurrentOpenData.getInstance().getDataFile().getName() + " "
									+ "file has no beats detected" + "\n", ta);
					}
				} else if (type == BATCHANALYSIS) {
					AmsDataFile file = new AmsDataFile(false, f.getAbsolutePath(), true, getAnalysisType());
					file.setQuick(false);
					file.setShowProgress(false);
					file.start();
					try {
						file.join();
					} catch (InterruptedException e) {
					}

					if (CurrentOpenData.getInstance().isFileInfoMissing() == true) // File is corrupted
						appendNewText(CurrentOpenData.getInstance().getDataFile().getName() + " " + "file is corrupted"
								+ "\n", errorta);
					else {

						// ECG Channel check
						if (!CurrentOpenData.getInstance().channelExists("ECG"))
							appendNewText(CurrentOpenData.getInstance().getDataFile().getName() + " "
									+ "file has no electrocardiogram recorded" + "\n", ta);

						// DZDT Channel check
						if (!CurrentOpenData.getInstance().channelExists("DZDT"))
							appendNewText(CurrentOpenData.getInstance().getDataFile().getName() + " "
									+ "file has no impedance cardiogram recorded" + "\n", ta);

						// --- ECG and DZDT channels present or not
						if (CurrentOpenData.getInstance().channelExists("ECG")
								&& CurrentOpenData.getInstance().channelExists("DZDT")) {
							// Beats detected - Yes/No
							if (CurrentOpenData.getInstance().channelExists("ECG")
									&& (CurrentOpenData.getInstance().getBeatSet().getBeats().isEmpty()))
								appendNewText(CurrentOpenData.getInstance().getDataFile().getName() + " "
										+ "file has no beats detected" + "\n", ta);

							if (CurrentOpenData.getInstance().getLabels().getLabels().isEmpty())
								appendNewText(CurrentOpenData.getInstance().getDataFile().getName() + " "
										+ "file has no labels" + "\n", ta);
							else {

								MainFrame.getInstance().getTabs().setSelectedComponent(ImpTab.getInstance());

								try {
									file.sleep(20000);
								} catch (InterruptedException e1) {
									e1.printStackTrace();
								}
								// file.resume();

								AmsDataFile file1 = new AmsDataFile(true, f.getAbsolutePath(), true, getAnalysisType());
								file1.setQuick(true);
								file1.setShowProgress(false);
								file1.start();
								try {
									file1.sleep(20000);
								} catch (InterruptedException e1) {
									e1.printStackTrace();
								}
								// file1.resume();
								try {
									file1.join();
								} catch (InterruptedException e) {
								}
							}
						}
						String baseName = Utils.removeExtension(f.getAbsolutePath());
						String xlsName = Utils.guaranteeExtension(baseName, "xls");
						String ascName = Utils.guaranteeExtension(baseName, "lbldat");

						File flxls = new File(xlsName);
						File flasc = new File(ascName);
						// boolean recalcNeeded = true;

						if (xlsCheck.isSelected() && flxls.exists() && overwriteCheck.isSelected() == false)
							appendNewText(flxls.getName() + " exists (and 'Overwrite existing files' not checked)\n",
									errorta);

						if (xlsCheck.isSelected()
								&& (flxls.exists() == false || (flxls.exists() && overwriteCheck.isSelected()))) {
							LabelInformationTab.getInstance().getData().outputDataToXLS(flxls);
							// recalcNeeded = false;
						}

						File tempFile = new File(xlstf.getText());
						if (aggxlsCheck.isSelected() && xlstf.getText().compareTo("") != 0) {
							LabelInformationTab.getInstance().getData().appendDataToXLS(tempFile, false, -1);
						}

						if (ascCheck.isSelected() && flasc.exists() && overwriteCheck.isSelected() == false)
							appendNewText(flasc.getName() + " exists (and 'Overwrite existing files' not checked)\n",
									errorta);

						if (ascCheck.isSelected()
								&& (flasc.exists() == false || (flasc.exists() && overwriteCheck.isSelected()))) {
							LabelInformationTab.getInstance().getData().outputDataToASCII(flasc);
							// recalcNeeded = false;
						}
						tempFile = new File(asctf.getText());
						if (aggascCheck.isSelected() && asctf.getText().compareTo("") != 0) {
							LabelInformationTab.getInstance().getData().outputDataToASCII(tempFile, -1, false,
									true);
						}
					}
				}
			}

			@Override
			public void run() {
				for (File f : convertFiles) {
					timer.start();
					note.setText(f.getName());
					progressBar.setValue(getPromilleDone());
					doBatch(f);
					CurrentOpenData.getInstance().Close();
					if (getStop()) {
						break;
					}
					timer.stop();
					addNewAverageTime(timer.getTime(), f.length());
				}

				// if (type == CONVERT5FS) {
				// File endFile = convertFiles.get(convertFiles.size() - 1);
				// if (Utils.getExtension(endFile).equals("ams")) {
				// AmsAMSFile file = new AmsAMSFile(endFile);
				// file.start();

				// } else if (Utils.getExtension(endFile).equals("5fs")) {
				// Ams5fsFile file = new Ams5fsFile(endFile.getAbsolutePath());
				// file.start();
				// } else {
				// Ams7fsFile file = new Ams7fsFile(endFile.getAbsolutePath());
				// file.start();
				// }
				// } else if ((type == EXPORTDATA) || (type == BATCHANALYSIS)) {
				// AmsDataFile file = new AmsDataFile(false,
				// convertFiles.get(convertFiles.size() - 1).getAbsolutePath(), true, 0);
				// file.start();
				// }

				setStop(true);
				note.setText("Batch Process Completed");
				progressBar.setValue(getPromilleDone());
				cancButton.setEnabled(false);
				batcherrorsbutton.setEnabled(true);
				diag.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);// To re-enable close by top right x
				timeLeftLabel.setText("Time remaining: 00:00:00");
				copyButton.setEnabled(true);
				CurrentOpenData.getInstance().setBatchAnalysis(false);
				batcherrorsbutton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						diag.setVisible(false);
					}
				});

			}
		};

		JPanel timeElPanel = new JPanel();
		final JLabel timeElLabel = new JLabel("TEMPORARY");
		timeElLabel.setHorizontalAlignment(SwingConstants.CENTER);
		timeElPanel.add(timeElLabel);

		JPanel timeLeftPanel = new JPanel();
		// final JLabel timeLeftLabel = new JLabel("TEMPORARY"); //JdH already created
		// so it can be used above in: convThread run()
		timeLeftLabel.setHorizontalAlignment(SwingConstants.CENTER);
		timeLeftPanel.add(timeLeftLabel);

		diag.add(timeElPanel);
		diag.add(timeLeftPanel);

		final Thread timeLeftThread = new Thread() {
			private long timeLeft = Long.MIN_VALUE;
			private long timeLeftCached = Long.MIN_VALUE;
			private long elapsedTime = 0;

			@Override
			public void run() {

				NumberFormat nf = NumberFormat.getInstance();
				nf.setMinimumIntegerDigits(2);

				while (true) {
					long curTimeLeft = getSTimeLeft();
					if (curTimeLeft != timeLeftCached) {
						timeLeft = curTimeLeft;
						timeLeftCached = curTimeLeft;
					}
					long tmpVal = timeLeft;
					long hoursLeft = tmpVal / 3600;
					tmpVal -= hoursLeft * 3600;
					long minLeft = tmpVal / 60;
					tmpVal -= minLeft * 60;
					long secLeft = tmpVal;

					tmpVal = elapsedTime;
					long elHour = tmpVal / 3600;
					tmpVal -= elHour * 3600;
					long elMin = tmpVal / 60;
					tmpVal -= elMin * 60;
					long elSec = tmpVal;
					String text = "Time elapsed: ";
					text += nf.format(elHour) + ":" + nf.format(elMin) + ":" + nf.format(elSec);
					timeElLabel.setText(text);

					text = "Time remaining: ";

					if (timeLeft > 0) {
						text += nf.format(hoursLeft) + ":" + nf.format(minLeft) + ":" + nf.format(secLeft);
					} else {
						text += "??:??:??";
					}

					timeLeftLabel.setText(text);

					try {
						sleep(1000);
					} catch (InterruptedException e) {
					}

					elapsedTime++;
					timeLeft--;
					if (getStop()) {
						break;
					}
				}

			}
		};
		copyButton = new JButton("Copy to clipboard");
		copyButton.setEnabled(false);
		copyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection ss = new StringSelection(
						"Warnings:\n" + ta.getText() + "\nErrors:\n" + errorta.getText());
				cb.setContents(ss, BatchDialog.this);
			}
		});

		cancButton = new JButton("Stop");
		cancButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				cancButton.setText("Stopping...");
				cancButton.setEnabled(false);
				setStop(true);
				appendNewText("batch stopped by user\n", errorta);
			}
		});

		batcherrorsbutton = new JButton("Close");
		batcherrorsbutton.setEnabled(false);

		cancButton.setFocusable(false);
		JPanel butPanel = new JPanel();
		butPanel.add(copyButton);
		butPanel.add(cancButton);
		butPanel.add(batcherrorsbutton);
		diag.add(butPanel);

		diag.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		diag.pack();
		diag.setResizable(false);
		diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());

		totalFileSizeDone = 0;
		fileSizeLeft = 0;
		totalFileSize = 0;
		curAverage = -1;
		for (File f : convertFiles) {
			fileSizeLeft += f.length();
			totalFileSize += f.length();
		}

		setStop(false);
		convThread.start();
		timeLeftThread.start();
		diag.setVisible(true);

		if ((type == EXPORTDATA)) {
			CurrentOpenData.getInstance().GenerateSubSampleFiles();
		}

	}

	protected synchronized int getPromilleDone() {
		return (int) Math.round(1000. * totalFileSizeDone / totalFileSize);
	}

	protected synchronized long getSTimeLeft() {
		return Math.round(curAverage * fileSizeLeft / 1000.);
	}

	protected synchronized boolean getStop() {
		return stopped;
	}

	protected void listConvertFiles(File path, boolean includeSubDirs) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory() && includeSubDirs == true) {
					listConvertFiles(files[i], true);
				} else {
					String ext = Utils.getExtension(files[i]);
					if (ext != null) {
						String compExt = "";
						String compExt2 = "";
						String compExt3 = "";
						if (type == CONVERT5FS) {
							compExt = "5fs";
							compExt2 = "ams";
							compExt3 = "7fs";
						} else if ((type == EXPORTDATA) || (type == BATCHANALYSIS)) {
							compExt = "amsdata";
							compExt2 = "amsdata";
							compExt3 = "amsdatai";
						}
						if (ext.equals(compExt) || ext.equals(compExt2) || ext.equals(compExt3)) {
							addFileToList(files[i]);
						}
					}
				}
			}
		}
	}

	protected synchronized void setStop(boolean b) {
		stopped = b;
	}

	public void setVisible(boolean b) {
		diag.setVisible(b);
	}

	public void setAnalysisType(int type) {
		TYPEOFANALYSIS = type;
	}

	public int getAnalysisType() {
		return TYPEOFANALYSIS;
	}

	@Override
	public void lostOwnership(Clipboard arg0, Transferable arg1) {
	}
}
