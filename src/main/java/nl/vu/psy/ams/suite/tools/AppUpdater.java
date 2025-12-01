package nl.vu.psy.ams.suite.tools;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitorInputStream;

import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

/*
 * Checks for updates on the internet for the application and
 * the bluetooth table.
 * Checks the rev.txt files first, and if it is newer than the local
 * rev, does the update. The application updater kicks in upon the next
 * app launch after a new version has been found.
 */
public class AppUpdater {

	// public static final String BTDBREVURLString =
	// "http://www.vu-ams.nl/software/BTDBRev.txt";
	// public static final String BTDBURLString =
	// "http://www.vu-ams.nl/software/BTDB.json";

	public static final String revURLString = "https://vu-ams.nl/wp-content/uploads/curRevB.txt";
	// public static final String setupURLString =
	// String.format("http://www.vu-ams.nl/software/VU-DAMSSetup%s.exe",
	// AppSettings.getInstance().getProperty(Settings.UPDATEVERSTRING));
	public static final String setupURLPrefix = "https://vu-ams.nl/wp-content/uploads/";

	public static void checkForUpdate() {
		// Runnable run = new Runnable() {
		// @Override
		// public void run() {
		try {
			URL revURL = (new URI(revURLString)).toURL();
			BufferedReader in = new BufferedReader(new InputStreamReader(revURL.openStream()));
			String revString = in.readLine();
			Integer newRev = Utils.parseInt(revString);
			if (newRev != null)
				AppSettings.getInstance().setIntProperty(Settings.UPDATEREVNUMBER, newRev);
			String revDate = in.readLine();
			AppSettings.getInstance().setProperty(Settings.UPDATEREVDATE, revDate);
			String verString = in.readLine();
			AppSettings.getInstance().setProperty(Settings.UPDATEVERSTRING, verString);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		// }
		// };
		// Thread thrd = ThreadServer.getNewThread(run);
		// thrd.start();
	}

	public static void updateAll() {
		checkForUpdate();
		updateIfNeeded();
		// updateBlueToothDBFromWebsite();
	}

	// private static void updateBlueToothDBFromWebsite() {
	// Runnable run = new Runnable() {
	// @Override
	// public void run() {
	// try {
	// URL revURL = new URL(BTDBREVURLString);
	// BufferedReader in = new BufferedReader(new
	// InputStreamReader(revURL.openStream()));
	// String revString = in.readLine();
	// int newRev = Integer.parseInt(revString);
	// int curRev =
	// AppSettings.getInstance().getIntProperty(Settings.BTDBREVNUMBER);
	// if (newRev > curRev) {
	// URL dbURL = new URL(BTDBURLString);
	// InputStream inStream = new BufferedInputStream(dbURL.openStream());

	// /*
	// //----------------- Previous code to save the BTDB.json to Temp
	// Directory-----------------
	// File file = new
	// File(AppSettings.getInstance().getProperty(Settings.TEMPDIR));
	// file = new File(file, "VU-DAMS/BTDB.json");
	// */

	// //----------------Code to save BTDB.json to App Data
	// directory----------------------------
	// String OSname = System.getProperty("os.name");
	// String path ="";
	// if(OSname.contains("Mac")){
	// path = System.getProperty("user.home") + "/Library/Application "+ "Support";
	// }else{
	// path = System.getenv("APPDATA");
	// }
	// File file;
	// file = new File(path, "VU-DAMS");

	// if(file.exists() == false){
	// file.mkdir();
	// }
	// file = new File(file, "BTDB.json");
	// //-----------------------------------------------------------------------------------------------

	// BufferedOutputStream outStream = null;
	// try {
	// outStream = new BufferedOutputStream(new FileOutputStream(file));
	// byte[] buffer = new byte[1024];
	// int i = inStream.read(buffer);
	// while (i > 0) {
	// outStream.write(buffer, 0 ,i);
	// i = inStream.read(buffer);
	// }
	// AppSettings.getInstance().setIntProperty(Settings.BTDBREVNUMBER, newRev);
	// } catch (IOException e) {
	// } finally {
	// if (outStream != null) {
	// outStream.close();
	// }
	// }
	// }
	// } catch (MalformedURLException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }

	// }
	// };
	// Thread thrd = ThreadServer.getNewThread(run);
	// thrd.start();
	// }

	public static void updateIfNeeded() {

		if (AppSettings.getInstance().getIntProperty(Settings.SHOWUPDATEAVAILABLE) == 0)
			return;

		int curRev = Utils.getRevisionNumber();
		int newRev = AppSettings.getInstance().getIntProperty(Settings.UPDATEREVNUMBER);
		String newVersion = AppSettings.getInstance().getProperty(Settings.UPDATEVERSTRING);
		if (curRev >= newRev)
			return;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate modDate = LocalDate.parse(AppSettings.getInstance().getProperty(Settings.UPDATEREVDATE), formatter);
		int modMonth = modDate.getMonthValue();
		String setupURLString = setupURLPrefix + modDate.getYear() + "/" + String.format("%02d", modMonth);
		String OSname = System.getProperty("os.name");
		String OSarch = System.getProperty("os.arch");
		OSname = "Windows 11";
		if (OSname.contains("Mac OS") || OSname.contains("MacOS")) {
			if (OSarch.equals("aarch64"))
				setupURLString += "/VU-DAMS" + newVersion + ".dmg";
			else
				setupURLString += "/VU-DAMS" + newVersion + "_intel.dmg";
		} else if (OSname.contains("Linux")) {
			setupURLString += "/AmsSuiteJ" + newVersion + ".jar";
		} else {
			setupURLString += "/VU-DAMSSetup" + newVersion + ".exe";
		}
		OutputStream outExe = null;
		try {
			Object[] options = { "Yes, please", "No, thank you", "Never ask me again" };
			int res = JOptionPane.showOptionDialog(MainFrame.getInstance().getMainFrame(),
					"A new version of the VU-DAMS suite is available!\nWould you like to update now?", "New Version!",
					JOptionPane.YES_NO_CANCEL_OPTION,
					// "A new version of the VU-DAMS suite is available!\nWould you like to download
					// the new version to your Downloads folder?", "New Version!",
					// JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
			if (res == JOptionPane.YES_OPTION) {
				URL exeURL = (new URI(setupURLString)).toURL();
				HttpURLConnection conn = null;
				conn = (HttpURLConnection) exeURL.openConnection();
				conn.setRequestMethod("HEAD");
				// System.out.println(conn.getContentLengthLong());
				if (conn != null) {
					conn.disconnect();
				}

				JFrame frame = MainFrame.getInstance().getMainFrame();
				InputStream inExe = new ProgressMonitorInputStream(frame, "Downloading new version",
						new BufferedInputStream(
								exeURL.openStream()));
				File setupFile;
				if (OSname.contains("Mac OS") || OSname.contains("MacOS")) {
					if (OSarch.equals("aarch64"))
						setupFile = new File((AppSettings.getInstance().getProperty(Settings.TEMPDIR)),
								String.format("VU-DAMS/VU-DAMS%s.dmg", newVersion));
					else
						setupFile = new File((AppSettings.getInstance().getProperty(Settings.TEMPDIR)),
								String.format("VU-DAMS/VU-DAMS%s_intel.dmg", newVersion));
				} else if (OSname.contains("Linux")) {
					setupFile = new File((AppSettings.getInstance().getProperty(Settings.TEMPDIR)),
							String.format("/AmsSuiteJ%s.jar", newVersion));
				} else {
					setupFile = new File((AppSettings.getInstance().getProperty(Settings.TEMPDIR)),
							String.format("VU-DAMS/VU-DAMSSetup%s.exe", newVersion));
				}
				outExe = new BufferedOutputStream(new FileOutputStream(setupFile));
				byte[] buffer = new byte[1024];
				int i = inExe.read(buffer);
				while (i > 0) {
					outExe.write(buffer, 0, i);
					i = inExe.read(buffer);
				}

				outExe.close();
				inExe.close();
				/*
				 * frame.toFront();
				 * frame.requestFocus();
				 */
				Desktop.getDesktop().open(setupFile);
				ExitApp.NormalExit();

			}
			if (res == JOptionPane.CANCEL_OPTION) {
				AppSettings.getInstance().setIntProperty(Settings.SHOWUPDATEAVAILABLE, 0);
			}
		} catch (MalformedURLException e) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					"An error has occured during updating.\nPlease try again later or visit the VU-AMS website.",
					"Update Error", JOptionPane.ERROR_MESSAGE);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
					"An error has occured during updating.\nPlease try again later or visit the VU-AMS website.",
					"Update Error", JOptionPane.ERROR_MESSAGE);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} finally {
			if (outExe != null) {
				try {
					outExe.close();
				} catch (IOException e) {
				}
			}
		}

	}

}
