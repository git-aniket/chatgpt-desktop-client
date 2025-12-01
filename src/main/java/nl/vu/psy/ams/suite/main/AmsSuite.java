/**
 * 
 */
package nl.vu.psy.ams.suite.main;

import java.io.File;

import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.tools.Utils;

//import org.apache.log4j.PropertyConfigurator;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.LogManager;

import java.awt.*;
import java.awt.desktop.OpenFilesHandler;
import java.awt.desktop.OpenFilesEvent;

import com.intel.bluetooth.BlueCoveConfigProperties;
import com.intel.bluetooth.BlueCoveImpl;

/**
 * 
 * This is the base AmsSuite java class, consisting of a single public method:
 * <code>main</code>. The <code>main</code> method is the first method that is
 * called when the AmsSuite program is launched.
 * 
 *
 * @author dm.pelt
 * 
 */
// TODO traagheid
// TODO algoritme Shiek ICG score
// TODO skin conductance algo

public class AmsSuite {
	private static String fL = null;

	/**
	 * Creates the GUI and shows it.
	 */
	private static void createAndShowGUI(String fn) {
		final MainFrame mainFrame = MainFrame.getInstance(fn);
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				mainFrame.getMainFrame().setVisible(true);
			}
		});
	}

	/**
	 * Main entry point of the AmsSuite program. Should only be used to start
	 * the User Interface.
	 * 
	 * @param args
	 *             the command line arguments
	 */
	public static void main(final String[] args) {
		/*
		 * try {
		 * UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		 * } catch (ClassNotFoundException | InstantiationException |
		 * IllegalAccessException
		 * | UnsupportedLookAndFeelException e1) {
		 * e1.printStackTrace();
		 * }
		 */
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_OBEX_TIMEOUT, String.valueOf(1000));
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_CONNECT_TIMEOUT, String.valueOf(10000));

		String OSname = System.getProperty("os.name");
		String path = "";

		if (OSname.contains("Mac")) {
			path = System.getProperty("user.home") + "/Library/Application Support";
		} else if (OSname.contains("Linux")) {
			path = System.getProperty("user.home") + "/.local/share/applications";
		} else {
			path = System.getenv("APPDATA");
		}
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("userApp.root", path);

		LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
		File file = new File(System.getProperty("user.dir"), "log4j2.properties");

		// this will force a reconfiguration
		if (file.exists())
			context.setConfigLocation(file.toURI());
		// PropertyConfigurator.configure((new File(System.getProperty("user.dir"),
		// "log4jconfig.properties")).toString());
		ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

		System.out.println("App Version: " + Utils.getAppVersion());
		System.out.println("Java version: " + System.getProperty("java.version"));
		String name = "os.name";
		String version = "os.version";
		String architecture = "os.arch";

		System.out.println("OS Name: " + System.getProperty(name));
		System.out.println("OS Version: " + System.getProperty(version));
		System.out.println("OS Architecture: " + System.getProperty(architecture));
		System.out.println("JVM:_ " + System.getProperty("sun.arch.data.model"));

		if (OSname.contains("Mac")) {
			Desktop.getDesktop().setOpenFileHandler(new OpenFilesHandler() {

				public void openFiles(OpenFilesEvent e) {
					for (File file : e.getFiles()) {
						fL = file.getAbsolutePath();
					}
				}

			});

		} else {
			if (args.length > 0) {
				for (String arg : args) {
					fL = arg;
					System.out.println("Command line arg:" + fL);
				}
			}
		}
		// Create the GUI in a separate thread
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// Try setting the look and feel to the System l&f
				// Set System L&F
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (UnsupportedLookAndFeelException e) {
					e.printStackTrace();
				}

				createAndShowGUI(fL);
			}
		});

		// Disable XFileDialog debug messages
		// XFileDialog.setTraceLevel(0);
	}

}
