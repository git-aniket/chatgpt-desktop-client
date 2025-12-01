package nl.vu.psy.ams.suite.tools;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
/*
 * Extension of a normal Java AWT filedialog,
 * with the addition that it loads the path that was
 * previously used.
 */
public class FileDialogWithSavedDir extends FileDialog {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	
	public FileDialogWithSavedDir(Frame frame, int mode) {
		super(frame, "", mode);
		File file = new File(AppSettings.getInstance().getProperty(Settings.TEMPDIR));
		file = new File(file, "VU-DAMS/lastdir.txt");
		if (file.exists()) {
			String lastFile = Utils.readStringFromFile(file);
			setDirectory(lastFile);
		}
	}

	public FileDialogWithSavedDir(Frame frame) {
		this(frame, FileDialog.LOAD);
	}

	private void saveDirToDisk(String currentDirectory) {
		File file = new File(AppSettings.getInstance().getProperty(Settings.TEMPDIR));
		file = new File(file, "VU-DAMS/lastdir.txt");
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
			pw.print(currentDirectory);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}

	@Override
	public void setVisible(boolean bool) throws HeadlessException {
		super.setVisible(bool);
		if (bool)
			saveDirToDisk(getDirectory());
	}

}
