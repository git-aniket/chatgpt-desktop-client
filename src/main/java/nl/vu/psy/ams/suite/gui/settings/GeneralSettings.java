package nl.vu.psy.ams.suite.gui.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.FileChooserWithSavedDir;
import nl.vu.psy.ams.suite.tools.FileDialogWithSavedDir;
import nl.vu.psy.ams.suite.tools.Utils;

public class GeneralSettings extends SettingsPane implements ItemListener, ActionListener {

	/**
	 * The general settings for the entire suite
	 */
	private static final long serialVersionUID = 1L;
	private JTextField tDir;
	private JCheckBox useSDir;
	private JCheckBox autoscaleontabchange;
	private JButton browseBut;
	private JPanel tmpPan;
	private JSlider displayPointsSlider;
	private JSlider buffSizeSlider;
	// private JSlider gridSizeSlider; //JdH because unused since dynamic grid
	// (1089)
	private JSlider snapSizeSlider;
	private JCheckBox gridEnabledCB;
	private JCheckBox drawBATSignal;
	private JCheckBox hideClinicalWarningCB;
	private JCheckBox insertSIDCB;
	private JCheckBox appendSubIDtoTitleBarCB;

	@Override
	public void actionPerformed(ActionEvent arg0) {
		String ac = arg0.getActionCommand();
		if (ac.equals("Browse")) {
			String OSname = System.getProperty("os.name");
			// File fl = null;
			if (!OSname.contains("Mac")) {
				FileChooserWithSavedDir fc = new FileChooserWithSavedDir();
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int retVal = fc.showOpenDialog(MainFrame.getInstance().getMainFrame());
				if (retVal == JFileChooser.APPROVE_OPTION)
					tDir.setText(fc.getSelectedFile().getAbsolutePath());
			} else {
				FileDialogWithSavedDir fd = new FileDialogWithSavedDir(MainFrame.getInstance().getMainFrame());
				fd.setFilenameFilter(new FilenameFilter() {
					@Override
					public boolean accept(File f, String s) {
						return false; // directories only
					}
				});
				fd.setVisible(true);
				if (fd.getFile() != null) {
					String filename = fd.getDirectory() + File.separator + fd.getFile();
					tDir.setText(filename);
				}
			}
		}
	}

	@Override
	public void initializeSettings() {
		AppSettings set = AppSettings.getInstance();
		String tempDir = set.getPropertyOrToBeSaved(Settings.TEMPDIR);
		if (tempDir.equals(System.getProperty("java.io.tmpdir"))) {
			useSDir.setSelected(true);
			browseBut.setEnabled(false);
		} else {
			useSDir.setSelected(false);
			browseBut.setEnabled(true);
		}
		tDir.setText(tempDir);

		int nDisp = set.getIntPropertyOrToBeSaved(Settings.MAXDISPLAYPOINTS);
		displayPointsSlider.setValue(nDisp);

		int nBuff = set.getIntPropertyOrToBeSaved(Settings.BINARYBUFFERSIZE);
		buffSizeSlider.setValue(nBuff * 4);

		// int nGrid = set.getIntPropertyOrToBeSaved(Settings.GRIDSIZE);
		// gridSizeSlider.setValue(nGrid);

		int nSnap = set.getIntPropertyOrToBeSaved(Settings.SNAPSIZE);
		snapSizeSlider.setValue(nSnap);

		int as = set.getIntPropertyOrToBeSaved(Settings.AUTOSCALEUPONTABCHANGE);
		if (as == 0) {
			autoscaleontabchange.setSelected(false);
		} else {
			autoscaleontabchange.setSelected(true);
		}

		int val = set.getIntPropertyOrToBeSaved(Settings.GRIDENABLED);
		if (val == 0) {
			gridEnabledCB.setSelected(false);
		} else {
			gridEnabledCB.setSelected(true);
		}

		val = set.getIntPropertyOrToBeSaved(Settings.SHOWBATSIGNAL);
		if (val == 0) {
			drawBATSignal.setSelected(false);
		} else {
			drawBATSignal.setSelected(true);
		}

		val = set.getIntPropertyOrToBeSaved(Settings.SHOWCLINICALWARNING);
		if (val == 0) {
			hideClinicalWarningCB.setSelected(true);
		} else {
			hideClinicalWarningCB.setSelected(false);
		}

		val = set.getIntPropertyOrToBeSaved(Settings.SAVESIDINFILENAME);
		if (val == 0) {
			insertSIDCB.setSelected(false);
		} else {
			insertSIDCB.setSelected(true);
		}

		val = set.getIntPropertyOrToBeSaved(Settings.APPENDSUBIDTOTITLEBAR);
		if (val == 0) {
			appendSubIDtoTitleBarCB.setSelected(false);
		} else {
			appendSubIDtoTitleBarCB.setSelected(true);
		}

	}

	@Override
	public void itemStateChanged(ItemEvent arg0) {
		Object source = arg0.getItemSelectable();
		if (source == useSDir) {
			if (arg0.getStateChange() == ItemEvent.DESELECTED) {
				browseBut.setEnabled(true);
			} else if (arg0.getStateChange() == ItemEvent.SELECTED) {
				browseBut.setEnabled(false);
				tDir.setText(System.getProperty("java.io.tmpdir"));
			}
		}
	}

	@Override
	public void save() {
		AppSettings set = AppSettings.getInstance();
		String tempDir = tDir.getText();
		if (tempDir.equals(System.getProperty("java.io.tmpdir"))) {
			set.setProperty(Settings.TEMPDIR, "-1");
		} else {
			set.setProperty(Settings.TEMPDIR, tempDir);
		}

		set.setIntProperty(Settings.MAXDISPLAYPOINTS, displayPointsSlider.getValue());
		set.setIntProperty(Settings.BINARYBUFFERSIZE, buffSizeSlider.getValue() / 4);
		// set.setIntProperty(Settings.GRIDSIZE, gridSizeSlider.getValue());
		set.setIntProperty(Settings.SNAPSIZE, snapSizeSlider.getValue());

		if (autoscaleontabchange.isSelected()) {
			set.setIntProperty(Settings.AUTOSCALEUPONTABCHANGE, 1);
		} else {
			set.setIntProperty(Settings.AUTOSCALEUPONTABCHANGE, 0);
		}

		int val = set.getIntPropertyOrToBeSaved(Settings.GRIDENABLED);
		if (gridEnabledCB.isSelected()) {
			set.setIntProperty(Settings.GRIDENABLED, 1);
			if (val == 0)
				MainFrame.getInstance().getMainFrame().repaint();
		} else {
			set.setIntProperty(Settings.GRIDENABLED, 0);
			if (val != 0)
				MainFrame.getInstance().getMainFrame().repaint();
		}
		if (drawBATSignal.isSelected()) {
			set.setIntProperty(Settings.SHOWBATSIGNAL, 1);
		} else {
			set.setIntProperty(Settings.SHOWBATSIGNAL, 0);
		}
		if (hideClinicalWarningCB.isSelected()) {
			set.setIntProperty(Settings.SHOWCLINICALWARNING, 0);
		} else {
			set.setIntProperty(Settings.SHOWCLINICALWARNING, 1);
		}
		if (insertSIDCB.isSelected()) {
			set.setIntProperty(Settings.SAVESIDINFILENAME, 1);
		} else {
			set.setIntProperty(Settings.SAVESIDINFILENAME, 0);
		}
		if (appendSubIDtoTitleBarCB.isSelected()) {
			set.setIntProperty(Settings.APPENDSUBIDTOTITLEBAR, 1);
		} else {
			set.setIntProperty(Settings.APPENDSUBIDTOTITLEBAR, 0);
		}
		MainFrame.getInstance().getTabs().repaint();
	}

	@Override
	public void setupPanel() {
		JPanel tmp;
		pan.add(new JLabel("<html><h1>General</h1></html>"));
		pan.add(new JSeparator());
		tDir = new JTextField();
		tDir.setEnabled(false);
		useSDir = new JCheckBox("Use system temporary directory");
		useSDir.addItemListener(this);
		pan.add(useSDir);
		tmp = new JPanel();
		tmp.setLayout(new BoxLayout(tmp, BoxLayout.Y_AXIS));
		tmp.setBorder(BorderFactory.createTitledBorder("Temporary Directory"));
		useSDir.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(useSDir);
		tmpPan = new JPanel(new BorderLayout());
		tmpPan.add(new JLabel("Temporary Directory:"), BorderLayout.WEST);
		tmpPan.add(tDir, BorderLayout.CENTER);
		browseBut = new JButton("Browse");
		browseBut.addActionListener(this);
		tmpPan.add(browseBut, BorderLayout.EAST);
		tmpPan.setAlignmentX(Component.LEFT_ALIGNMENT);

		tmp.add(tmpPan);

		pan.add(tmp);

		tmp = new JPanel();
		tmp.setLayout(new BoxLayout(tmp, BoxLayout.Y_AXIS));
		tmp.setBorder(BorderFactory.createTitledBorder("Drawing options"));
		JLabel lab;
		drawBATSignal = new JCheckBox("Show battery signal");
		tmp.add(drawBATSignal);
		autoscaleontabchange = new JCheckBox("Autoscale signals upon tab change");
		autoscaleontabchange.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(autoscaleontabchange);
		lab = new JLabel("Maximum number of display points:");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		displayPointsSlider = new JSlider(1000, 50000);
		displayPointsSlider.setMajorTickSpacing(9800);
		displayPointsSlider.setMinorTickSpacing(1960);
		displayPointsSlider.setPaintTicks(true);
		displayPointsSlider.setPaintLabels(true);
		displayPointsSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(new JSeparator());
		tmp.add(lab);
		tmp.add(displayPointsSlider);
		lab = new JLabel("Binary buffer size (bytes):");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		buffSizeSlider = new JSlider(8000, 400000);
		buffSizeSlider.setMajorTickSpacing(78400);
		buffSizeSlider.setMinorTickSpacing(15680);
		buffSizeSlider.setPaintTicks(true);
		buffSizeSlider.setPaintLabels(true);
		buffSizeSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp.add(new JSeparator());
		tmp.add(lab);
		tmp.add(buffSizeSlider);
		/*
		 * lab = new JLabel("Grid size (pixels):");
		 * lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		 * gridSizeSlider = new JSlider(1, 50);
		 * gridSizeSlider.setMajorTickSpacing(7);
		 * gridSizeSlider.setMinorTickSpacing(1);
		 * gridSizeSlider.setPaintTicks(true);
		 * gridSizeSlider.setPaintLabels(true);
		 * gridSizeSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
		 */ tmp.add(new JSeparator());
		gridEnabledCB = new JCheckBox("Show grid");
		tmp.add(gridEnabledCB);
		// tmp.add(lab);
		// tmp.add(gridSizeSlider);
		lab = new JLabel("Snap size (pixels):");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		snapSizeSlider = new JSlider(1, 25);
		snapSizeSlider.setMajorTickSpacing(3);
		snapSizeSlider.setMinorTickSpacing(1);
		snapSizeSlider.setPaintTicks(true);
		snapSizeSlider.setPaintLabels(true);
		snapSizeSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
		// tmp.add(new JSeparator());
		tmp.add(lab);
		tmp.add(snapSizeSlider);

		pan.add(tmp);

		pan.add(new JSeparator(SwingConstants.HORIZONTAL));
		insertSIDCB = new JCheckBox("Insert subject ID at start of filename when saving amsdata file");
		pan.add(insertSIDCB);
		hideClinicalWarningCB = new JCheckBox("Hide clinical warning message on startup");
		pan.add(hideClinicalWarningCB);
		appendSubIDtoTitleBarCB = new JCheckBox("Append [Subject ID] to Title bar");
		pan.add(appendSubIDtoTitleBarCB);

		JButton clearAllButton = new JButton("Clear user configuration files");
		clearAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int resVal = JOptionPane.showConfirmDialog(GeneralSettings.this,
						"Are you sure you want to clear the user configuration files?\nNote: this does not delete any data",
						"Confirm clear user configuration", JOptionPane.YES_NO_OPTION);
				if (resVal == JOptionPane.YES_OPTION) {
					for (String s : Utils.USERFILES) {
						File curFile = new File(AppSettings.getInstance().getProperty(Settings.TEMPDIR));
						curFile = new File(curFile, "VU-DAMS/" + s);
						curFile.delete();
					}
					CurrentOpenData.getInstance().setDirty(true);
				}
			}
		});
		pan.add(clearAllButton);

		JButton ForceBTupdateButton = new JButton("Force Bluetooth database update");
		ForceBTupdateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AppSettings.getInstance().setIntProperty(Settings.BTDBREVNUMBER, 1);
			}
		});
		pan.add(ForceBTupdateButton);
	}
}
