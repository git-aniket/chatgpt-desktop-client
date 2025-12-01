package nl.vu.psy.ams.suite.gui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import nl.vu.psy.ams.suite.data.files.Ams5fsFile;
import nl.vu.psy.ams.suite.data.files.Ams7fsFile;
import nl.vu.psy.ams.suite.data.files.AmsAMSFile;
import nl.vu.psy.ams.suite.data.files.AmsDataFile;
import nl.vu.psy.ams.suite.gui.tabs.StartTab;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.AppPinger;
import nl.vu.psy.ams.suite.tools.AppUpdater;
import nl.vu.psy.ams.suite.tools.ExitApp;
import nl.vu.psy.ams.suite.tools.Utils;

/**
 * The main application window. Is implemented as a singleton, so you can always
 * get a reference to it if you open a JDialog.
 */
public class MainFrame {


	private static MainFrame	instance;

	public static MainFrame getInstance() {
		if (instance == null) {
			instance = new MainFrame(null);
		}
		return instance;
	}

	public static MainFrame getInstance(String fn) {
		if (instance == null) {
			instance = new MainFrame(fn);
		}
		return instance;
	}

	private JFrame		mainFrame;

	private MainTabs	tabs;

	private JPanel		southPanel;

	/**
	 * Instantiates a new main frame.
	 */
	private MainFrame(final String fn) {

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				mainFrame = new JFrame(Utils.APPNAME);

				mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
				mainFrame.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent ev) {
						ExitApp.NormalExit();
					}
				});
				mainFrame.setIconImage(new ImageIcon(getClass().getResource("/img/hearmysite.png")).getImage());

				MainMenuBar mainMenuBar = MainMenuBar.getInstance();

				mainFrame.setJMenuBar(mainMenuBar.getMenuBar());
				
			//	Toolkit toolkit = Toolkit.getDefaultToolkit();
				//Dimension scrnsize = toolkit.getScreenSize();
			//	Dimension frameSize = new Dimension(1280, 720);
				
				
				//frameSize.width = (int) (3 * scrnsize.getWidth() / 4);
				//frameSize.height = (int) (3 * scrnsize.getHeight() / 4);
				
			//	mainFrame.setBounds(0, 0, frameSize.width, frameSize.height);
				

				// --------- Application to fit the screen------------------------------
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				int framewidth = (int) screenSize.getWidth();
				int frameheight=(int) screenSize.getHeight()-40;
			    mainFrame.setBounds(0, 0, framewidth, frameheight);	    
			    //-------------------------------------------------------------
				
			   
			    tabs = new MainTabs();
				tabs.setFocusable(false);
				tabs.setHideTabBarWhenSingleTab(true);
				tabs.addAmsTab("", StartTab.getNewInstance());
				// tabs.addChangeListener(new TabListener(tabs));

				mainFrame.setLayout(new BorderLayout());

				mainFrame.add(tabs, BorderLayout.CENTER);

				southPanel = new JPanel();
				southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));

				mainFrame.add(southPanel, BorderLayout.SOUTH);
				mainFrame.setLocationRelativeTo(null);
				mainFrame.setFocusable(true);
				/*mainFrame.addFocusListener(new FocusListener() {
					 
                     public void focusGained(FocusEvent e) {
                             System.out.println("focusGained " + e);
                     }

                     public void focusLost(FocusEvent e) {
                             System.out.println("focusLost " + e);
                     }
				});*/

				if (AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.SHOWCLINICALWARNING) != 0)
					new ClinicalWarningDialog(mainFrame);

				AppPinger.getInstance().start();
				
				String arch 			= System.getProperty("os.arch");								
				String realArch 		= "null";	
			    realArch 				= arch.endsWith("64")	                  
			                          		? "64" : "32";
				
				String JavaJVM	  		= System.getProperty("sun.arch.data.model");
				
				if(realArch.contains("64")){ // 64 bit operating system
					if(JavaJVM.contains("64")){
						if (Utils.isCorrectJavaVersion() == false) {
							
							String[] options = {"OK", "Visit java.com"};
							int res = JOptionPane.showOptionDialog(mainFrame,
									"You are using an old version of Java, please upgrade!\nFor upgrade information, see http://www.java.com.\nCurrent Java version: "
											+ System.getProperty("java.version") + "\nMinimum Java version: 1.6.0_22", "Old Java Version", JOptionPane.YES_NO_OPTION,
									JOptionPane.ERROR_MESSAGE, null, options, options[0]);
							if (res == JOptionPane.NO_OPTION) {
								try {
									Desktop.getDesktop().browse(new URI("http://www.java.com"));
								} catch (IOException e) {
									e.printStackTrace();
								} catch (URISyntaxException e) {
									e.printStackTrace();
								}
							}
							ExitApp.NormalExit();
						}
					}/*else {
						String[] options = {"Download", "Exit Program"};
						String setupURLString = "http://www.vu-ams.nl/software/jre-windows-x64.exe";
						int res = JOptionPane.showOptionDialog(mainFrame,
								"You have a 64 bit Operating System. Please download 64 bit Java for optimal performance."
										,"Performance Test", JOptionPane.YES_NO_OPTION,
								JOptionPane.ERROR_MESSAGE, null, options, options[0]);
						if (res == 0) {						
							try {
								Desktop.getDesktop().browse(new URI(setupURLString));
							} catch (IOException e) {
								e.printStackTrace();
							} catch (URISyntaxException e) {
								e.printStackTrace();
							}
						}else{
							ExitApp.NormalExit();
						}
						ExitApp.NormalExit();
					}*/
					
				}else{
					if (Utils.isCorrectJavaVersion() == false) {
					String[] options = {"OK", "Visit java.com"};
					int res = JOptionPane.showOptionDialog(mainFrame,
							"You are using an old version of Java, please upgrade!\nFor upgrade information, see http://www.java.com.\nCurrent Java version: "
									+ System.getProperty("java.version") + "\nMinimum Java version: 1.6.0_22", "Old Java Version", JOptionPane.YES_NO_OPTION,
							JOptionPane.ERROR_MESSAGE, null, options, options[0]);
					if (res == JOptionPane.NO_OPTION) {
						try {
							Desktop.getDesktop().browse(new URI("http://www.java.com"));
						} catch (IOException e) {
							e.printStackTrace();
						} catch (URISyntaxException e) {
							e.printStackTrace();
						}
					}
					ExitApp.NormalExit();
				}
				}
				AppUpdater.updateAll();

				if (fn != null) {
					File fl = new File(fn);
					if (Utils.getExtension(fl).equals("5fs")) {
						Ams5fsFile file = new Ams5fsFile(fl.getAbsolutePath());
						file.start();
					} else if (Utils.getExtension(fl).equals("amsdata") || Utils.getExtension(fl).equals("amsdatai")) {
						AmsDataFile datafile = new AmsDataFile(false, fl.getAbsolutePath());
						datafile.start();
					} else if (Utils.getExtension(fl).equals("ams")) {
						AmsAMSFile file = new AmsAMSFile(fl);
						file.start();
					} else if (Utils.getExtension(fl).equals("7fs")) {
						Ams7fsFile file = new Ams7fsFile(fl.getAbsolutePath());
						file.start();
					}
				}

				/* For debugging add from nl.vu.psy.ams.suite.gui.MainMenuBar.java here */
				//BatchDialog bdg = new BatchDialog(BatchDialog.EXPORTDATA);
				//bdg.setVisible(true);
			}
		});

	}

	public JFrame getMainFrame() {
		return mainFrame;
	}

	public JPanel getSouthPanel() {
		return southPanel;
	}

	public MainTabs getTabs() {
		return tabs;
	}

}
