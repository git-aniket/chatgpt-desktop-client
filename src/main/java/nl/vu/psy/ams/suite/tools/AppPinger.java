package nl.vu.psy.ams.suite.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

//import javax.swing.JOptionPane;

//import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
/*
 * Thread that is started at the start of the program.
 * This thread writes a file with the current time to
 * the base temporary directory each minute. It is used to
 * detect temp directories that can be cleaned up after
 * an application crash: if, at the start of the program (before we have started pinging!),
 * a temporary directory is found with a ping file that is more
 * than 10 minutes in the past, we can assume that the DAMS instance
 * that created this ping has crashed, and we can delete the corresponding
 * temporary directory. Note that if the DAMS software exits normally, all
 * temporary files, including the ping file, will be cleaned up.
 */
public class AppPinger extends Thread {

	public static AppPinger getInstance() {
		if (instance == null)
			instance = new AppPinger();
		return instance;
	}
	private File				baseDir		= Utils.getBaseTemporaryDirectory();

	private File				pingFile	= new File(baseDir, "ping");

	private static AppPinger	instance;

	private AppPinger() {
		super();
	}

	private void cleanUpStaleTempData() {
		long curTime = Calendar.getInstance().getTimeInMillis();
		String tempDirStr = AppSettings.getInstance().getProperty(Settings.TEMPDIR);
		File tempDir = new File(tempDirStr, "VU-DAMS");
		File[] files = tempDir.listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				File pFile = new File(f, "ping");
				boolean delDir = true;
				if (pFile.exists()) {
					FileInputStream fis = null;
					DataInputStream dis = null;
					try {
						fis = new FileInputStream(pFile);
						fis.getChannel().lock(0, Long.MAX_VALUE, true);
						dis = new DataInputStream(new BufferedInputStream(fis));
						long pTime = dis.readLong();
						if (curTime - pTime < 600000) {
							delDir = false;
						}
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						if (dis != null) {
							try {
								dis.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						if (fis != null) {
							try {
								fis.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
				if (delDir == true) {
					//----------Delete Temporary Data without confirmation
					/*int res = JOptionPane
							.showConfirmDialog(
									MainFrame.getInstance().getMainFrame(),
									"Old temporary data found in directory:\n\n"
											+ f
											+ "\n\nThis can be the result of a previous application error, and this data is probably safe to delete.\nWould you like to delete this data?",
									"Old data found", JOptionPane.YES_NO_OPTION);
					if (res == JOptionPane.YES_OPTION)*/
						ExitApp.deleteDirectory(f);
				}
			}
		}

	}

	@Override
	public void run() {
		writePing();
		cleanUpStaleTempData();
		while (true) {
			writePing();
			try {
				sleep(60000);
			} catch (InterruptedException e) {
				break;
			}
			if (isInterrupted())
				break;
		}
	}

	private void writePing() {
		long pingTime = Calendar.getInstance().getTimeInMillis();
		FileOutputStream fos = null;
		DataOutputStream dos = null;
		try {
			fos = new FileOutputStream(pingFile);
			fos.getChannel().lock();
			dos = new DataOutputStream(new BufferedOutputStream(fos));
			dos.writeLong(pingTime);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (dos != null) {
				try {
					dos.close();
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
}
