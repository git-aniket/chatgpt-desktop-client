package nl.vu.psy.ams.suite.gui.tabs.qrs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.ECGBeat;
import nl.vu.psy.ams.suite.data.structures.sets.ArtefactSet;
import nl.vu.psy.ams.suite.data.structures.sets.BeatSet;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.RescanECGDialog;
import nl.vu.psy.ams.suite.gui.SaveDialog;
import nl.vu.psy.ams.suite.gui.graphgen.GenerateEctopicBeatsDialog;
import nl.vu.psy.ams.suite.gui.tabs.inspect.InspectTab;
import nl.vu.psy.ams.suite.gui.toolbar.AmsToolBar;
import nl.vu.psy.ams.suite.tools.FileChooserWithSavedDir;
import nl.vu.psy.ams.suite.tools.FileDialogWithSavedDir;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * The detect r-peaks toolbar
 */
public class QRSToolBar extends AmsToolBar {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private JDialog diag;
	private QRSTab tab;
	protected BeatSet bSet = CurrentOpenData.getInstance().getBeatSet();
	private ArtefactSet aSet = CurrentOpenData.getInstance().getECGArtefacts();
	ArrayList<AmsLabel> retArray = new ArrayList<AmsLabel>();
	protected TreeSet<AmsLabel> lArtList = new TreeSet<AmsLabel>();
	protected JComboBox<String> cb;

	public QRSToolBar(QRSTab tab) {
		super();
		this.tab = tab;
		bSet = CurrentOpenData.getInstance().getBeatSet();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String actionCommand = e.getActionCommand();
		if (actionCommand.equals("moveEcgLeft")) {
			double tWidth = tab.getXAxisECG().getTimeWidth();
			double midPoint = tab.getXAxisECG().getMidPoint();
			tab.getxAxisTopIBI().moveToTime(midPoint - 0.25 * tWidth);
			tab.getxAxisMidIBI().moveToTime(midPoint - 0.25 * tWidth);
			tab.getXAxisECG().moveToTime(midPoint - 0.25 * tWidth);
			// tab.getxAxisTopIBI().move(- 0.25 / tWidth);
			// tab.getxAxisMidIBI().move(- 0.25 / tWidth);
			// tab.getXAxisECG().move(-0.25);
		} else if (actionCommand.equals("moveEcgRight")) {
			double tWidth = tab.getXAxisECG().getTimeWidth();
			double midPoint = tab.getXAxisECG().getMidPoint();
			tab.getxAxisTopIBI().moveToTime(midPoint + 0.25 * tWidth);
			tab.getxAxisMidIBI().moveToTime(midPoint + 0.25 * tWidth);
			tab.getXAxisECG().moveToTime(midPoint + 0.25 * tWidth);
		} else if (actionCommand.equals("zoomEcgIn")) {
			tab.getXAxisECG().zoom(2);
		} else if (actionCommand.equals("zoomEcgOut")) {
			tab.getXAxisECG().zoom(0.5);
		} else if (actionCommand.equals("moveLeftOneScreen")) {
			// tab.getXAxisECG().move(-1);
			double tWidth = tab.getXAxisECG().getTimeWidth();
			double midPoint = tab.getXAxisECG().getMidPoint();
			tab.getxAxisTopIBI().moveToTime(midPoint - tWidth);
			tab.getxAxisMidIBI().moveToTime(midPoint - tWidth);
			tab.getXAxisECG().moveToTime(midPoint - tWidth);
		} else if (actionCommand.equals("moveRightOneScreen")) {
			tab.getXAxisECG().move(1);
			double tWidth = tab.getXAxisECG().getTimeWidth();
			double midPoint = tab.getXAxisECG().getMidPoint();
			tab.getxAxisTopIBI().moveToTime(midPoint + tWidth);
			tab.getxAxisMidIBI().moveToTime(midPoint + tWidth);
			tab.getXAxisECG().moveToTime(midPoint + tWidth);
		} else if (actionCommand.equals("zoomIn4X")) {
			tab.getXAxisECG().zoom(4);
		} else if (actionCommand.equals("zoomOut4X")) {
			tab.getXAxisECG().zoom(0.25);
		} else if (actionCommand.equals("moveMibiLeft")) {
			double tWidth = tab.getxAxisMidIBI().getTimeWidth();
			double midPoint = tab.getxAxisMidIBI().getMidPoint();
			tab.getxAxisTopIBI().moveToTime(midPoint - 0.25 * tWidth);
			tab.getxAxisMidIBI().moveToTime(midPoint - 0.25 * tWidth);
			tab.getXAxisECG().moveToTime(midPoint - 0.25 * tWidth);
		} else if (actionCommand.equals("moveMibiRight")) {
			double tWidth = tab.getxAxisMidIBI().getTimeWidth();
			double midPoint = tab.getxAxisMidIBI().getMidPoint();
			tab.getxAxisTopIBI().moveToTime(midPoint + 0.25 * tWidth);
			tab.getxAxisMidIBI().moveToTime(midPoint + 0.25 * tWidth);
			tab.getXAxisECG().moveToTime(midPoint + 0.25 * tWidth);
		} else if (actionCommand.equals("zoomMibiIn")) {
			tab.getxAxisMidIBI().zoom(2);
		} else if (actionCommand.equals("zoomMibiOut")) {
			tab.getxAxisMidIBI().zoom(0.5);
		} else if (actionCommand.equals("moveTibiLeft")) {
			double tWidth = tab.getxAxisTopIBI().getTimeWidth();
			double midPoint = tab.getxAxisTopIBI().getMidPoint();
			tab.getxAxisTopIBI().moveToTime(midPoint - 0.25 * tWidth);
			tab.getxAxisMidIBI().moveToTime(midPoint - 0.25 * tWidth);
			tab.getXAxisECG().moveToTime(midPoint - 0.25 * tWidth);
		} else if (actionCommand.equals("moveTibiRight")) {
			double tWidth = tab.getxAxisTopIBI().getTimeWidth();
			double midPoint = tab.getxAxisTopIBI().getMidPoint();
			tab.getxAxisTopIBI().moveToTime(midPoint + 0.25 * tWidth);
			tab.getxAxisMidIBI().moveToTime(midPoint + 0.25 * tWidth);
			tab.getXAxisECG().moveToTime(midPoint + 0.25 * tWidth);
		} else if (actionCommand.equals("zoomTibiIn")) {
			tab.getxAxisTopIBI().zoom(2);
		} else if (actionCommand.equals("zoomTibiOut")) {
			tab.getxAxisTopIBI().zoom(0.5);
		} else if (actionCommand.equals("autoscaleAll")) {
			tab.autoscale();
		} else if (actionCommand.equals("autoscaleAllRaw")) {
			tab.autoscale(0);
		} else if (actionCommand.equals("moveHighSusp")) {
			double time = bSet.getBeatSortedByIBISuspicion(0).getRPeakTime();
			bSet.setSelectedSuspiciousIBI(0);
			tab.getxAxisTopIBI().moveToTimeFast(time);
			tab.getxAxisMidIBI().moveToTimeFast(time);
			tab.getXAxisECG().moveToTimeFast(time);
			tab.autoscaleFast();
		} else if (actionCommand.equals("movePrevSusp")) {
			int curSel = bSet.getSelectedSuspiciousIBI();
			if (curSel > 0)
				curSel--;
			double time = bSet.getBeatSortedByIBISuspicion(curSel).getRPeakTime();
			bSet.setSelectedSuspiciousIBI(curSel);
			tab.getxAxisTopIBI().moveToTimeFast(time);
			tab.getxAxisMidIBI().moveToTimeFast(time);
			tab.getXAxisECG().moveToTimeFast(time);
			tab.autoscaleFast();
		} else if (actionCommand.equals("moveNextSusp")) {
			int curSel = bSet.getSelectedSuspiciousIBI();
			if (curSel < bSet.numberOfSuspiciousBeats() - 1)
				curSel++;
			double time = bSet.getBeatSortedByIBISuspicion(curSel).getRPeakTime();
			bSet.setSelectedSuspiciousIBI(curSel);
			tab.getxAxisTopIBI().moveToTimeFast(time);
			tab.getxAxisMidIBI().moveToTimeFast(time);
			tab.getXAxisECG().moveToTimeFast(time);
			tab.autoscaleFast();
		} else if (actionCommand.equals("recheckIBIs")) {
			bSet.recalculateAllSuspiciousLevels();
			CurrentOpenData.getInstance().setDirty(true);
			ECGBeat beat = bSet.getBeatSortedByIBISuspicion(0);
			if (beat != null) {
				double time = beat.getRPeakTime();
				bSet.setSelectedSuspiciousIBI(0);
				tab.getxAxisTopIBI().moveToTimeFast(time);
				tab.getxAxisMidIBI().moveToTimeFast(time);
				tab.getXAxisECG().moveToTimeFast(time);
				tab.autoscaleFast();
			}
		} else if (actionCommand.equals("delBeatsUnderArt")) {
			aSet.deleteBeatsUnderArtefacts();
			tab.getxAxisTopIBI().updateAll();
			tab.getxAxisMidIBI().updateAll();
			tab.getXAxisECG().updateAll();
		} else if (actionCommand.equals("importbeats")) {
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
						return Utils.getExtension(f).equals("beat");
					}

					@Override
					public String getDescription() {
						return "ECG beat files";
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
						if (s.endsWith("beat"))
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
				// File fl = new File(fileDialog.getDirectory(), fileName);
				bSet.importFromFile(fl);
			}
		} else if (actionCommand.equals("export_to_beat")) {
			File retFile = SaveDialog.saveFileBrowser("beat", "ECG Beat File");
			if (retFile != null) {
				bSet.saveToBeatFile(retFile);
			}
		} else if (actionCommand.equals("export_to_ascii")) {
			File retFile = SaveDialog.saveFileBrowserWithAdditionforQRS("txt", "_beat", "ECG Beat ASCII File");
			boolean state = SaveDialog.showEvents;
			if (retFile != null) {
				bSet.exportToASCII(retFile, false, state);
			}
		} else if (actionCommand.equals("deleteIBI")) {

			diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Delete diverging Beats", true);
			diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));

			JPanel questionpanel = new JPanel();
			JLabel question = new JLabel("Do you want to delete all the diverging beats?");
			questionpanel.add(question);
			diag.add(questionpanel);

			JPanel optionspanel = new JPanel();
			final JCheckBox high = new JCheckBox("Delete \"deviant\" beats");
			final JCheckBox medium = new JCheckBox("Delete beats \"worth checking\"");
			optionspanel.add(high);
			optionspanel.add(medium);
			diag.add(optionspanel);

			JPanel buttonpanel = new JPanel();
			JButton okbutton = new JButton("Delete");
			okbutton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent l) {
					SortedSet<ECGBeat> beatspresent = CurrentOpenData.getInstance().getBeatSet().getBeats();

					if (high.isSelected() && medium.isSelected()) {
						int count = 0;
						ECGBeat prevBeat = null;
						ECGBeat curBeat = null;
						for (ECGBeat beats : beatspresent) {
							if (count > 0) {
								prevBeat = bSet.getBeatBeforeTime(beats.getRPeakTime());
								curBeat = bSet.getBeatClosestToTime(beats.getRPeakTime());

								if (curBeat.getIBISuspicion() > 500) {

									if (prevBeat.getIBISuspicion() > 500) {
										aSet.getLabelUnderTime(prevBeat.getRPeakTime())
												.setRightTime(curBeat.getRPeakTime() + 250000);
									} else {
										aSet.add(AmsLabel.generateECGArtefact(beats.getRPeakTime() - 250000,
												beats.getRPeakTime() + 250000, false, 0.0, "Diverging beats"));
									}

								}
							}
							count++;
						}
						CurrentOpenData.getInstance().getECGArtefacts().deleteBeatsUnderArtefacts();
					} else if (high.isSelected()) {
						int count = 0;
						ECGBeat prevBeat = null;
						ECGBeat curBeat = null;
						for (ECGBeat beats : beatspresent) {
							if (count > 0) {
								prevBeat = bSet.getBeatBeforeTime(beats.getRPeakTime());
								curBeat = bSet.getBeatClosestToTime(beats.getRPeakTime());
								if (curBeat.getIBISuspicion() > 1000) {
									if (prevBeat.getIBISuspicion() > 1000) {
										aSet.getLabelUnderTime(prevBeat.getRPeakTime())
												.setRightTime(curBeat.getRPeakTime() + 250000);
									} else {
										aSet.add(AmsLabel.generateECGArtefact(beats.getRPeakTime() - 250000,
												beats.getRPeakTime() + 250000, false, 0.0, "Diverging beats"));
									}

								}
							}
							count++;
						}
						CurrentOpenData.getInstance().getECGArtefacts().deleteBeatsUnderArtefacts();
					} else if (medium.isSelected()) {
						int count = 0;
						ECGBeat prevBeat = null;
						ECGBeat curBeat = null;
						for (ECGBeat beats : beatspresent) {
							if (count > 0) {
								prevBeat = bSet.getBeatBeforeTime(beats.getRPeakTime());
								curBeat = bSet.getBeatClosestToTime(beats.getRPeakTime());
								if ((curBeat.getIBISuspicion() > 500) && (curBeat.getIBISuspicion() < 1000)) {
									if ((prevBeat.getIBISuspicion() > 500) && (prevBeat.getIBISuspicion() < 1000)) {
										aSet.getLabelUnderTime(prevBeat.getRPeakTime())
												.setRightTime(curBeat.getRPeakTime() + 250000);
									} else {
										aSet.add(AmsLabel.generateECGArtefact(beats.getRPeakTime() - 250000,
												beats.getRPeakTime() + 250000, false, 0.0, "Suspicious beats"));
									}
								}
							}
							count++;
						}
						CurrentOpenData.getInstance().getECGArtefacts().deleteBeatsUnderArtefacts();
					} else {
						diag.setVisible(false);
					}

					diag.setVisible(false);
					tab.repaint();
				}

			});

			JButton cancelbutton = new JButton("Cancel");
			cancelbutton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent l) {
					diag.setVisible(false);
				}
			});
			buttonpanel.add(okbutton);
			buttonpanel.add(cancelbutton);
			diag.add(buttonpanel);
			diag.pack();
			diag.setResizable(false);
			diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
			diag.setVisible(true);

		} else if (actionCommand.equals("importarts")) {
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
						return Utils.getExtension(f).equals("amsiv");
					}

					@Override
					public String getDescription() {
						return "AMS inventory files";
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
						if (s.endsWith("amsinv"))
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
				// File fl = new File(fileDialog.getDirectory(), fileName);
				CurrentOpenData.getInstance().getECGArtefacts().importFromFile(fl);
			}
		} else if (actionCommand.equals("goto")) {
			String input = JOptionPane.showInputDialog("Go to diverging Beat");
			Integer n = 0;
			if (input != null) {
				n = Utils.parseInt(input);
				if ((n != null) && (n > 0) && (n < CurrentOpenData.getInstance().getBeatSet().getBeats().size())) {
					ECGBeat sortedbeats = bSet.getBeatSortedByIBISuspicion(n - 1);
					double time = sortedbeats.getRPeakTime();
					bSet.setSelectedSuspiciousIBI(n - 1);
					tab.getxAxisTopIBI().moveToTimeFast(time);
					tab.getxAxisMidIBI().moveToTimeFast(time);
					tab.getXAxisECG().moveToTimeFast(time);
					tab.autoscaleFast();
				} else {
					JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
							"Please select a number between " + 1 + " and "
									+ CurrentOpenData.getInstance().getBeatSet().getBeats().size(),
							"Beat not found error", JOptionPane.ERROR_MESSAGE);
				}

			}
		} else if (actionCommand.equals("showectopicbeats")) {
			GenerateEctopicBeatsDialog diag = new GenerateEctopicBeatsDialog();
			diag.setVisible(true);
		} else if (actionCommand.equals("clrartefactsandrescanecg")) {
			aSet.clear();
			if (!Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("7fs")
					&& CurrentOpenData.getInstance().getFileHeader().getDwHardwareVersion() != 7)
				aSet.reCalculate(16);
			else
				aSet.reCalculate(24);
			double lTime = 0.0;
			double rTime = 10000.0 * (CurrentOpenData.getInstance().getEndDate().getTimeInMillis() -
					CurrentOpenData.getInstance().getStartDate().getTimeInMillis());
			JDialog diag = new RescanECGDialog(lTime, rTime);
			diag.setVisible(true);
			tab.getxAxisTopIBI().updateAll();
			tab.getxAxisMidIBI().updateAll();
			tab.getXAxisECG().updateAll();
		} else if (actionCommand.equals("moveFirstArt")) {
			aSet.setlabelindex(1);
			ArrayList<AmsLabel> artList = new ArrayList<AmsLabel>(aSet.getLabels());
			double time = artList.get(0).getLeftTime();
			tab.getxAxisTopIBI().moveToTimeFast(time);
			tab.getxAxisMidIBI().moveToTimeFast(time);
			tab.getXAxisECG().moveToTimeFast(time);
			tab.autoscaleFast();
		} else if (actionCommand.equals("movePrevArt")) {
			int index = aSet.getlabelindex() - 1;
			if (index < 1)
				index = 1;
			aSet.setlabelindex(index);
			ArrayList<AmsLabel> artList = new ArrayList<AmsLabel>(aSet.getLabels());
			double time = artList.get(index - 1).getLeftTime();
			tab.getxAxisTopIBI().moveToTimeFast(time);
			tab.getxAxisMidIBI().moveToTimeFast(time);
			tab.getXAxisECG().moveToTimeFast(time);
			tab.autoscaleFast();
		} else if (actionCommand.equals("moveNextArt")) {
			int index = aSet.getlabelindex() + 1;
			if (index > aSet.getLabels().size())
				index = aSet.getLabels().size();
			aSet.setlabelindex(index);
			ArrayList<AmsLabel> artList = new ArrayList<AmsLabel>(aSet.getLabels());
			double time = artList.get(index - 1).getLeftTime();
			tab.getxAxisTopIBI().moveToTimeFast(time);
			tab.getxAxisMidIBI().moveToTimeFast(time);
			tab.getXAxisECG().moveToTimeFast(time);
			tab.autoscaleFast();
		} else if (actionCommand.equals("moveLastArt")) {
			aSet.setlabelindex(aSet.getLabels().size());
			ArrayList<AmsLabel> artList = new ArrayList<AmsLabel>(aSet.getLabels());
			double time = artList.get(artList.size() - 1).getLeftTime();
			tab.getxAxisTopIBI().moveToTimeFast(time);
			tab.getxAxisMidIBI().moveToTimeFast(time);
			tab.getXAxisECG().moveToTimeFast(time);
			tab.autoscaleFast();
		} else if (actionCommand.equals("channel")) {
			CurrentOpenData.getInstance().setECGChannel(cb.getSelectedIndex());
			tab.setChannel(cb.getSelectedIndex());
		}

	}

	@Override
	public void addButtons() {
		String[] options = new String[CurrentOpenData.getInstance().ecgChannels()];
		String[] chanNames = { "ECG", "V2ecg", "V3ecg" };
		for (int i = 0; i < CurrentOpenData.getInstance().ecgChannels(); i++) {
			String prettyName = InspectTab.getInstance().getSignalName(chanNames[i]);
			// strio unit
			options[i] = prettyName.substring(0, prettyName.lastIndexOf(" "));
		}
		cb = new JComboBox<String>(options);
		this.add(new JLabel("Channel:"));
		this.add(cb);
		this.add(new JSeparator(SwingConstants.VERTICAL));
		cb.setActionCommand("channel");
		cb.addActionListener(this);
		setupButton("left", "Move ECG Left \u00BC Screen", "moveEcgLeft",
				KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true));
		setupButton("right", "Move ECG Right \u00BC Screen", "moveEcgRight",
				KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true));
		setupButton("zoomin", "Zoom ECG In 2X", "zoomEcgIn", KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true));
		setupButton("zoomout", "Zoom ECG Out 2X", "zoomEcgOut", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true));
		setupButton("left_left", "Move ECG Left 1 Screen", "moveLeftOneScreen",
				KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0, true));
		setupButton("right_right", "Move ECG Right 1 Screen", "moveRightOneScreen",
				KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0, true));
		setupButton("zoominin", "Zoom ECG In 4X", "zoomIn4X", KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0, true));
		setupButton("zoomoutout", "Zoom ECG Out 4X", "zoomOut4X",
				KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0, true));
		addNewSeparator();
		setupButton("left_left", "Move Middle IBI Left \u00BC Screen", "moveMibiLeft",
				KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, 0, true));
		setupButton("right_right", "Move Middle IBI Right \u00BC Screen", "moveMibiRight",
				KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, 0, true));
		setupButton("zoomin", "Zoom Middle IBI In 2X", "zoomMibiIn",
				KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0, true));
		setupButton("zoomout", "Zoom Middle IBI Out 2X", "zoomMibiOut",
				KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0, true));
		addNewSeparator();
		setupButton("first", "Move Top IBI Left \u00BC Screen", "moveTibiLeft",
				KeyStroke.getKeyStroke(KeyEvent.VK_O, 0, true));
		setupButton("last", "Move Top IBI Right \u00BC Screen", "moveTibiRight",
				KeyStroke.getKeyStroke(KeyEvent.VK_P, 0, true));
		setupButton("zoomin", "Zoom Top IBI In 2X", "zoomTibiIn", KeyStroke.getKeyStroke(KeyEvent.VK_0, 0, true));
		setupButton("zoomout", "Zoom Top IBI Out 2X", "zoomTibiOut", KeyStroke.getKeyStroke(KeyEvent.VK_9, 0, true));
		addNewSeparator();
		setupButton("autoscale_green", "Autoscale All", "autoscaleAll",
				KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
		setupButton("autoscale", "Autoscale All on Raw Data", "autoscaleAllRaw",
				KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0, true));
		addNewSeparator();
		setupButton("firstsusp", "Move to most diverging IBI", "moveHighSusp",
				KeyStroke.getKeyStroke(KeyEvent.VK_M, 0, true));
		setupButton("prevsusp", "Move to previous most diverging IBI", "movePrevSusp",
				KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, 0, true));
		setupButton("nextsusp", "Move to next most diverging IBI", "moveNextSusp",
				KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0, true));
		setupButton("rechecksusp", "Recheck for diverging IBIs", "recheckIBIs",
				KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK, true));
		addNewSeparator();
		setupButton("delete", "Delete Beats Under Artefacts", "delBeatsUnderArt",
				KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK, true));
		addNewSeparator();
		setupButton("import", "Import Beats From File", "importbeats",
				KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK, true));
		setupButton("export", "Export Beats To .beat File", "export_to_beat",
				KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK, true));
		setupButton("exportTXT", "Export Beats To ASCII File", "export_to_ascii",
				KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK, true));
		addNewSeparator();
		setupButton("import_art", "Import Artefacts From File", "importarts",
				KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK, true));
		setupButton("Trash", "Name diverging beats as Artefact", "deleteIBI",
				KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK, true));
		setupButton("search", "Go to diverging beat", "goto",
				KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK, true));
		setupButton("smallstar", "Show Ectopic Beats", "showectopicbeats",
				KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK, true));
		addNewSeparator();
		setupButton("mrbomb", "Clear all artefacts and rescan ECG", "clrartefactsandrescanecg",
				KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK, true));

		addNewSeparator();
		setupButton("first", "Move to first artefact", "moveFirstArt", KeyStroke.getKeyStroke(KeyEvent.VK_J, 0, true));
		setupButton("left", "Move to previous artefact", "movePrevArt", KeyStroke.getKeyStroke(KeyEvent.VK_K, 0, true));
		setupButton("right", "Move to next artefact", "moveNextArt", KeyStroke.getKeyStroke(KeyEvent.VK_L, 0, true));
		setupButton("last", "Move to last artefact", "moveLastArt",
				KeyStroke.getKeyStroke(KeyEvent.VK_SEMICOLON, 0, true));
	}
}
