package nl.vu.psy.ams.suite.tools;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.OrientationFilesGenerator;
import nl.vu.psy.ams.suite.data.SubSetFileGenerator;
import nl.vu.psy.ams.suite.data.files.AmsDataFile;
import nl.vu.psy.ams.suite.data.posture.ActivityClassification;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.SaveDialog;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

/*
 * Class to let the app exit normally, deleting
 * all temporary data, and saving the data if needed.
 */
public class ExitApp {
	static public boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

	public static void NormalExit() {
		if (CurrentOpenData.getInstance().isOpen()
				&& (CurrentOpenData.getInstance().isDirty() || CurrentOpenData.getInstance().dirtyFiles.size() > 0)) {
			int result;
			if (new File(System.getProperty("user.dir"), "DoNotAskMeToSaveData.txt").exists())
				result = JOptionPane.NO_OPTION;
			else
				result = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
						"Would you like to save the current open data? It will be compressed so it opens faster next time.",
						"Save data",
						JOptionPane.YES_NO_CANCEL_OPTION);
			if (result == JOptionPane.YES_OPTION) {
				CurrentOpenData.getInstance().saveChangeablesToDisk();
				File datFile = CurrentOpenData.getInstance().getDataFile();
				if (datFile != null) {
					if (Utils.getExtension(datFile).equals("amsdata")
							|| Utils.getExtension(datFile).equals("amsdatai")) {
						AmsDataFile file = new AmsDataFile(true,
								CurrentOpenData.getInstance().getDataFile().getAbsolutePath());
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
						String extension = "";
						if (!Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("7fs")
								&& CurrentOpenData.getInstance().getFileHeader().getDwHardwareVersion() != 7)
							extension = "amsdata";
						else
							extension = "amsdatai";
						if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.SAVESIDINFILENAME) != 0) {
							retFile = SaveDialog.saveFileBrowserWithChanges(extension, "" + "_", "Ams Data File");
						} else {
							retFile = SaveDialog.saveFileBrowser(extension, "Ams Data File");
						}
						if (retFile != null) {
							AmsDataFile adf = new AmsDataFile(true, retFile.getAbsolutePath());
							adf.setCompressed(true);
							adf.setShowProgress(false);
							MainFrame.getInstance().getMainFrame().setVisible(false);
							JFrame saveInfoFrame = new JFrame();
							saveInfoFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
							saveInfoFrame.setResizable(false);
							saveInfoFrame.setTitle("Saving AMS Data. Please wait...");
							saveInfoFrame
									.setIconImage(new ImageIcon("src/main/resources/img/hearmysite.png").getImage());
							saveInfoFrame.setUndecorated(true);
							JLabel lbl = new JLabel("Saving AMS Data. Please wait...");
							lbl.setHorizontalAlignment(SwingConstants.CENTER);
							saveInfoFrame.add(lbl);
							Toolkit toolkit = Toolkit.getDefaultToolkit();
							Dimension scrnsize = toolkit.getScreenSize();
							Dimension frameSize = new Dimension(1280, 720);
							frameSize.width = (int) (1 * scrnsize.getWidth() / 4);
							frameSize.height = (int) (1 * scrnsize.getHeight() / 4);

							saveInfoFrame.setBounds(0, 0, frameSize.width, frameSize.height);
							saveInfoFrame.setLocationRelativeTo(null);
							saveInfoFrame.setVisible(true);
							lbl.paintImmediately(0, 0, lbl.getWidth(), lbl.getHeight());
							adf.start();
							try {
								adf.join();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							saveInfoFrame.setVisible(false);
						}
					}
				}
			} else if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
				return;
			}
		}

		ThreadServer.stopAllThreads();
		SubSetFileGenerator.getInstance().interrupt();
		try {
			SubSetFileGenerator.getInstance().join(1);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		OrientationFilesGenerator.getInstance().interrupt();
		try {
			OrientationFilesGenerator.getInstance().join(1);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		ActivityClassification.getInstance().interrupt();
		try {
			ActivityClassification.getInstance().join(1);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		AppPinger.getInstance().interrupt();
		try {
			AppPinger.getInstance().join(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (CurrentOpenData.getInstance().isOpen())
			CurrentOpenData.getInstance().ClearAllData();
		deleteDirectory(Utils.getBaseTemporaryDirectory());

		AppSettings.getInstance().saveSettingsToBeSaved();

		System.exit(0);
	}
}
