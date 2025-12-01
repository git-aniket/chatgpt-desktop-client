package nl.vu.psy.ams.suite.tools;

import java.awt.Component;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import javax.swing.JFileChooser;

import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

/*
 * Extension of a normal Java Swing filechooser,
 * with the addition that it loads the path that was
 * previously used.
 */
public class FileChooserWithSavedDir extends JFileChooser {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String lastdirFile;

	public FileChooserWithSavedDir() {
		this("VU-DAMS/lastdir.txt");
	}

	public FileChooserWithSavedDir(String lastdirFile) {
		super();
		this.lastdirFile = lastdirFile;
		File file = new File(AppSettings.getInstance().getProperty(Settings.TEMPDIR));
		file = new File(file, lastdirFile);
		if (file.exists()) {
			String lastFile = Utils.readStringFromFile(file);
			setCurrentDirectory(new File(lastFile));
		}
	}

	private void saveDirToDisk(File currentDirectory) {
		File file = new File(AppSettings.getInstance().getProperty(Settings.TEMPDIR));
		file = new File(file, lastdirFile);
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
			pw.print(currentDirectory.getAbsolutePath());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}

	@Override
	public int showOpenDialog(Component parent) throws HeadlessException {
		int retVal = super.showOpenDialog(parent);
		saveDirToDisk(getCurrentDirectory());
		return retVal;
	}

	@Override
	public int showSaveDialog(Component parent) throws HeadlessException {
		int retVal = super.showSaveDialog(parent);
		saveDirToDisk(getCurrentDirectory());
		return retVal;
	}

}
