package nl.vu.psy.ams.suite.gui.tabs.label;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.util.Iterator;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.sets.AmsLabelSet;
import nl.vu.psy.ams.suite.gui.LabelConfigurationEditor;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.SaveDialog;
import nl.vu.psy.ams.suite.gui.TimeBar;
import nl.vu.psy.ams.suite.gui.toolbar.AmsToolBar;
import nl.vu.psy.ams.suite.tools.FileChooserWithSavedDir;
import nl.vu.psy.ams.suite.tools.FileDialogWithSavedDir;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * The label data toolbar
 */
public class LabelToolBar extends AmsToolBar {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private LabelTab tab;

	protected AmsLabel l;
	public static JComboBox<?> labelchooser;

	public LabelToolBar(LabelTab tab) {
		super();
		this.tab = tab;

		// String[] labeltype = {"Default","Experiment labels", "Customized labels",
		// "Exp + Time labels"};
		// String[] labeltype = {"Default","Experiment labels","Exp + Time labels"};
		String[] labeltype = { "All labels", "Experiment labels", "Time labels" };
		labelchooser = new JComboBox<String>(labeltype);
		AmsLabelSet labels = tab.getLabelsShow();
		labels.setlabelindex(1);
		labelchooser.setSelectedIndex(1);
		if (labels.getLabels().size() == 0 && labels.getAllLabels().size() > 0) {
			labels.setlabelindex(2);
			labelchooser.setSelectedIndex(2);
		}

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String actionCommand = e.getActionCommand();
		if (actionCommand.equals("moveLeft")) {
			tab.getXAxis().move(-0.25);
		} else if (actionCommand.equals("moveRight")) {
			tab.getXAxis().move(0.25);
		} else if (actionCommand.equals("zoomIn")) {
			tab.getXAxis().zoom(2);
		} else if (actionCommand.equals("zoomOut")) {
			tab.getXAxis().zoom(0.5);
		} else if (actionCommand.equals("autoscaleAll")) {
			tab.getXAxis().autoscaleConnectedGraphs();
		} else if (actionCommand.equals("moveLeftOneScreen")) {
			tab.getXAxis().move(-1);
		} else if (actionCommand.equals("moveRightOneScreen")) {
			tab.getXAxis().move(1);
		} else if (actionCommand.equals("zoomIn4X")) {
			tab.getXAxis().zoom(4);
		} else if (actionCommand.equals("zoomOut4X")) {
			tab.getXAxis().zoom(0.25);
		} else if (actionCommand.equals("editLabels")) {
			JDialog diag = new LabelConfigurationEditor();
			diag.setVisible(true);
		} else if (actionCommand.equals("import")) {
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
						return Utils.getExtension(f).equals("lbl");
					}

					@Override
					public String getDescription() {
						return "Ams Label Files";
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
						if (s.endsWith("lbl"))
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
				int res = JOptionPane.YES_OPTION;
				if (CurrentOpenData.getInstance().getLabels().getAllLabels().size() > 0) {
					res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
							"There are existing labels; the scoring for all the ECG & ICG Complexes will be lost. Do you want to continue?",
							"Existing labels", JOptionPane.YES_NO_OPTION);
				}
				if (res == JOptionPane.YES_OPTION) {
					CurrentOpenData.getInstance().getLabels().importFromFile(fl);
					tab.repaint();
					TimeBar.getInstance().getPanel().repaint();
					CurrentOpenData.getInstance().setDirty(true);
					// CurrentOpenData.getInstance().saveChangeablesToDisk();
				} else
					return;
			}
		} else if (actionCommand.equals("export")) {
			File fl = new File("");
			fl = SaveDialog.saveFileBrowserWithAddition("lbl", "_icg", "LBL File");
			if (fl != null) {
				CurrentOpenData.getInstance().getLabels().exportToLBLFile(fl);
			}
		} else if (actionCommand.equals("importfromMarkers")) {

			final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Choose import type", true);

			JPanel panel1 = new JPanel();
			JPanel panel2 = new JPanel();

			String[] options = { "Default/Legacy [SM, EM, D1, D2, Label Code]",
					"Start_Marker, End_Marker, Duration(sec)_From_StartMarker, Duration(sec)_From_EndMarker, Label_Code",
					"Start_Marker, Duration(sec)_LabelStart_StartMarker, Duration(sec)_LabelEnd_StartMarker, Label_Code" };
			final JComboBox<String> cb = new JComboBox<String>(options);

			JButton browseButton = new JButton("Select File to Import");
			browseButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String OSname = System.getProperty("os.name");
					File fl = null;
					if (!OSname.contains("Mac")) {
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
								return "Markers to Labels files";
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
						int res = JOptionPane.YES_OPTION;
						if (CurrentOpenData.getInstance().getLabels().getAllLabels().size() > 0) {
							res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
									"There are existing labels; the scoring for all the ECG & ICG Complexes will be lost. Do you want to continue?",
									"Existing labels", JOptionPane.YES_NO_OPTION);
						}
						if (res == JOptionPane.YES_OPTION) {
							CurrentOpenData.getInstance().getLabels().importFromFileusingMarkers(fl,
									cb.getSelectedIndex());
							diag.setVisible(false);
							tab.repaint();
							TimeBar.getInstance().getPanel().repaint();
							CurrentOpenData.getInstance().setDirty(true);
							// CurrentOpenData.getInstance().saveChangeablesToDisk();
						} else
							return;
					}
				}
			});

			JButton cButton = new JButton("Cancel");
			cButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					diag.setVisible(false);
				}
			});
			panel1.add(cb);

			panel2.add(browseButton);
			panel2.add(cButton);
			diag.setLayout(new GridLayout(2, 1));
			diag.add(panel1);
			diag.add(panel2);

			diag.pack();
			diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
			diag.setVisible(true);

		} else if (actionCommand.equals("nextPan")) {
			tab.nextPanel();
		} else if (actionCommand.equals("prevPan")) {
			tab.prevPanel();

		} else if (actionCommand.equals("addlbls")) {

			final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Select time width", true);
			final JFormattedTextField timeWidth = new JFormattedTextField(60);
			timeWidth.setColumns(10);
			diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.X_AXIS));
			diag.add(new JLabel("Set time width (seconds): "));
			diag.add(timeWidth);
			final JButton okButton = new JButton("OK");
			diag.getRootPane().setDefaultButton(okButton);
			okButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {

					diag.setVisible(false);
					try {
						timeWidth.commitEdit();
					} catch (ParseException e1) {
					}
					double tW = ((Number) timeWidth.getValue()).doubleValue() * 1000000.;
					if (tW < 1) {
						JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
								"Label width must be greater than 0", "Time WidthError",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					AmsLabelSet lSet = CurrentOpenData.getInstance().getLabels();

					if (lSet.getLabels().isEmpty() == false) {
						// int res =
						// JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
						// "Would you like to include experiment label information in your time based
						// labels?",
						// "Confirm Label Type", JOptionPane.YES_NO_OPTION);
						// if (res == JOptionPane.YES_OPTION) {
						lSet.setStateoflabels(true);
						// } else {
						// lSet.setStateoflabels(false);
						// }
					} else {
						lSet.setStateoflabels(false);
					}
					lSet.setToTimeLabels(tW);
					tab.repaint();
					TimeBar.getInstance().getPanel().repaint();
					// CurrentOpenData.getInstance().saveChangeablesToDisk();
					labelchooser.setSelectedIndex(2);
					lSet.setlabelindex(2);
				}
			});

			diag.add(okButton);
			diag.getRootPane().setDefaultButton(okButton);// ENTER will hit button OK

			JButton cButton = new JButton("Cancel");
			cButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					diag.setVisible(false);
				}
			});

			diag.add(cButton);
			diag.pack();
			diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
			diag.setVisible(true);
		} else if (actionCommand.equals("deletelabels")) {

			int res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
					"Are you sure you want to delete all existing labels in this view?",
					"Delete labels", JOptionPane.YES_NO_OPTION);
			if (res == JOptionPane.YES_OPTION) {
				AmsLabelSet labels = CurrentOpenData.getInstance().getLabels();
				int i = labels.getlabelindex();
				if (i == 0)
					labels.clear();
				else if (i == 1) {
					Iterator<AmsLabel> iter = labels.getLabels().iterator();

					while (iter.hasNext()) {
						AmsLabel l = iter.next();

						if (!l.isTimeLabel())
							labels.removeLabel(l);
					}
				} else {
					Iterator<AmsLabel> iter = labels.getLabels().iterator();

					while (iter.hasNext()) {
						AmsLabel l = iter.next();

						if (l.isTimeLabel())
							labels.removeLabel(l);
					}
				}
				// CurrentOpenData.getInstance().saveChangeablesToDisk();
				labels.setlabelindex(0);
				tab.repaint();
			} else {
				return;
			}

		}
	}

	@Override
	public void addButtons() {
		setupButton("left", "Move Left \u00BC Screen", "moveLeft", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true));
		setupButton("right", "Move Right \u00BC Screen", "moveRight",
				KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true));
		setupButton("zoomin", "Zoom In 2X", "zoomIn", KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true));
		setupButton("zoomout", "Zoom Out 2X", "zoomOut", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true));
		addNewSeparator();
		setupButton("left_left", "Move Left 1 Screen", "moveLeftOneScreen",
				KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0, true));
		setupButton("right_right", "Move Right 1 Screen", "moveRightOneScreen",
				KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0, true));
		setupButton("zoominin", "Zoom In 4X", "zoomIn4X", KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0, true));
		setupButton("zoomoutout", "Zoom Out 4X", "zoomOut4X", KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0, true));
		addNewSeparator();
		setupButton("autoscale", "Autoscale All", "autoscaleAll", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
		addNewSeparator();
		setupButton("config", "Edit Label Codes/Names", "editLabels", KeyStroke.getKeyStroke(KeyEvent.VK_E, 0, true));
		addNewSeparator();
		setupButton("import", "Import Labeling/Markers From File", "import",
				KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK, true));
		setupButton("export", "Export Labeling To lbl File", "export",
				KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK, true));
		setupButton("import_Events", "Create Labels using Events", "importfromMarkers",
				KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK, true));
		addNewSeparator();
		setupButton("first", "Previous Panel", "prevPan", KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, 0, true));
		setupButton("last", "Next Panel", "nextPan", KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0, true));
		addNewSeparator();
		setupButton("addlbls3", "Add Time-Based Labels", "addlbls",
				KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK, true));
		setupButton("Trash", "Delete all Labels", "deletelabels",
				KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK, true));

		JPanel pan = new JPanel();
		pan.setSize(10, 5);

		pan.add(labelchooser);
		AmsLabelSet labels = tab.getLabelsShow();
		this.add(pan);
		labelchooser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				labels.setlabelindex(labelchooser.getSelectedIndex());
				tab.repaint();
				TimeBar.getInstance().getPanel().repaint();
			}
		});

	}

}
