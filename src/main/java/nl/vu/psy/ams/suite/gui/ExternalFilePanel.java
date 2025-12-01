package nl.vu.psy.ams.suite.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import javax.swing.JPanel;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.ExternalFile2Data;
import nl.vu.psy.ams.suite.data.ExternalFile3Data;
import nl.vu.psy.ams.suite.data.ExternalFileData;
import nl.vu.psy.ams.suite.gui.axes.XAxis;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.drawing.ExternalFileDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.graphs.overlays.GridOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.TimeShowOverlay;
import nl.vu.psy.ams.suite.gui.tabs.AmsTab;
/*
 * The panel between the main tabs and the TimeBar, that
 * shows an external signal if it is loaded. If no external file
 * is loaded, it hides itself from view.
 */
public class ExternalFilePanel extends JPanel {

	/**
	 * 
	 */
	private static final long			serialVersionUID	= 1L;
	private static ExternalFilePanel	instance			= null;
	private int  signalcount								= 0;
	
	public static ExternalFilePanel getInstance() {
		if (instance == null)
			instance = new ExternalFilePanel();
		return instance;
	}
	public static ExternalFilePanel getNewInstance() {
		instance = null;
		instance = new ExternalFilePanel();
		return instance;
	}
	private XAxis	xAxis;
	private Graph	extgraph;
	private Graph	extgraph2;
	private Graph	extgraph3;
	
	private boolean	isFile1Loaded;
	private boolean	isFile2Loaded;
	private boolean	isFile3Loaded;

	private ExternalFilePanel() {
		super();
		setLayout(new GridLayout(signalcount,1));
		resetLayout();
	}

	public void clear() {
		resetLayout();		
		new File(CurrentOpenData.getInstance().getFilePath(), "extfile.dat").delete();		
		new File(CurrentOpenData.getInstance().getFilePath(), "extfile2.dat").delete();
		new File(CurrentOpenData.getInstance().getFilePath(), "extfile3.dat").delete();
		ExternalFileData.clear();
		ExternalFile2Data.clear();		
		ExternalFile3Data.clear();		
		MainFrame.getInstance().getMainFrame().repaint();
		MainFrame.getInstance().getSouthPanel().repaint();
	}
	
	public void clear1() {
		resetLayout1();		
		new File(CurrentOpenData.getInstance().getFilePath(), "extfile.dat").delete();		
		ExternalFileData.clear();			
		MainFrame.getInstance().getMainFrame().repaint();
		MainFrame.getInstance().getSouthPanel().repaint();
	}
	
	public void clear2() {
		resetLayout2();		
		new File(CurrentOpenData.getInstance().getFilePath(), "extfile2.dat").delete();		
		ExternalFile2Data.clear();			
		MainFrame.getInstance().getMainFrame().repaint();
		MainFrame.getInstance().getSouthPanel().repaint();
	}
	
	public void clear3() {
		resetLayout3();		
		new File(CurrentOpenData.getInstance().getFilePath(), "extfile3.dat").delete();		
		ExternalFile3Data.clear();			
		MainFrame.getInstance().getMainFrame().repaint();
		MainFrame.getInstance().getSouthPanel().repaint();
	}
	private void copyFileToTempDir(File fl, File outFile) {
		FileChannel ifc = null;
		FileChannel ofc = null;
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(fl);
			fos = new FileOutputStream(outFile);
			ifc = fis.getChannel();
			ofc = fos.getChannel();
			ifc.transferTo(0, fl.length(), ofc);
		} catch (IOException e) {

		} finally {
			if (ifc != null) {
				try {
					ifc.close();
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (ofc != null) {
				try {
					ofc.close();
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public XAxis getXAxis() {
		return xAxis;
	}

	public boolean isFile1Loaded() {
		return isFile1Loaded;
	}
	public boolean isFile2Loaded() {
		return isFile2Loaded;
	}
	public boolean isFile3Loaded() {
		return isFile3Loaded;
	}
	public void loadFileFromTempDir() {
		removeAll();
		this.isFile1Loaded = true;
		xAxis = new XAxis();
		xAxis.setTimes(0, 100);
		extgraph = new Graph(xAxis);
		extgraph.addUnderlay(new GridOverlay(extgraph));		
		extgraph.addOverlay(new TimeShowOverlay(extgraph));
		
		ExternalFileData.clear();
	
		ExternalFileDrawer efd = new ExternalFileDrawer("External", new YAxis(0, 1, 0, 1, ExternalFileData.getName()));
		efd.getYAxis().setBottomValue(0);
		efd.getYAxis().setTopValue(1);
		efd.connectToGraph(extgraph);
		extgraph.setActiveYAxis(efd.getYAxis());
		
		this.add(extgraph.getPanel(), BorderLayout.CENTER);
		
		setMinimumSize(new Dimension(1, 75));
		setMaximumSize(new Dimension(100000, 75));
		setPreferredSize(new Dimension(1, 75));
		AmsTab curTab = ((AmsTab) MainFrame.getInstance().getTabs().getSelectedComponent());
		if (curTab != null) {
			curTab.setUnactive();
			curTab.setActive();
		}
		MainFrame.getInstance().getMainFrame().revalidate();
		MainFrame.getInstance().getMainFrame().repaint();
	}
	public void loadFile2FromTempDir() {
		
		this.isFile2Loaded = true;		
		xAxis = new XAxis();
		xAxis.setTimes(0, 100);
		
		extgraph2 = new Graph(xAxis);
		extgraph2.addUnderlay(new GridOverlay(extgraph2));
		extgraph2.addOverlay(new TimeShowOverlay(extgraph2));
		
		ExternalFile2Data.clear();
		ExternalFileDrawer efd2 = new ExternalFileDrawer("External2", new YAxis(0, 1, 0, 1, ExternalFile2Data.getName()));
		efd2.getYAxis().setBottomValue(0);
		efd2.getYAxis().setTopValue(1);
		efd2.connectToGraph(extgraph2);
		extgraph2.setActiveYAxis(efd2.getYAxis());
				
		this.add(extgraph2.getPanel(), BorderLayout.CENTER);
		
		setMinimumSize(new Dimension(1, 150));
		setMaximumSize(new Dimension(100000, 150));
		setPreferredSize(new Dimension(1, 150));
		AmsTab curTab = ((AmsTab) MainFrame.getInstance().getTabs().getSelectedComponent());
		if (curTab != null) {
			curTab.setUnactive();
			curTab.setActive();
		}
		MainFrame.getInstance().getMainFrame().revalidate();
		MainFrame.getInstance().getMainFrame().repaint();
	}
	public void loadFile3FromTempDir() {
		
		this.isFile3Loaded = true;		
		xAxis = new XAxis();
		xAxis.setTimes(0, 100);
		
		extgraph3 = new Graph(xAxis);
		extgraph3.addUnderlay(new GridOverlay(extgraph3));
		extgraph3.addOverlay(new TimeShowOverlay(extgraph3));
		
		ExternalFile3Data.clear();
		ExternalFileDrawer efd2 = new ExternalFileDrawer("External3", new YAxis(0, 1, 0, 1, ExternalFile3Data.getName()));
		efd2.getYAxis().setBottomValue(0);
		efd2.getYAxis().setTopValue(1);
		efd2.connectToGraph(extgraph3);
		extgraph3.setActiveYAxis(efd2.getYAxis());
				
		this.add(extgraph3.getPanel(), BorderLayout.CENTER);
		
		setMinimumSize(new Dimension(1, 225));
		setMaximumSize(new Dimension(100000, 225));
		setPreferredSize(new Dimension(1, 225));
		AmsTab curTab = ((AmsTab) MainFrame.getInstance().getTabs().getSelectedComponent());
		if (curTab != null) {
			curTab.setUnactive();
			curTab.setActive();
		}
		MainFrame.getInstance().getMainFrame().revalidate();
		MainFrame.getInstance().getMainFrame().repaint();
	}
	public void resetLayout() {
		removeAll();
		this.isFile1Loaded = false;
		this.isFile2Loaded = false;
		this.isFile3Loaded = false;
		setMinimumSize(new Dimension(1, 1));
		setMaximumSize(new Dimension(100000, 1));
		setPreferredSize(new Dimension(1, 1));
		MainFrame.getInstance().getMainFrame().repaint();
		MainFrame.getInstance().getSouthPanel().repaint();
	}
	public void resetLayout1() {		
		this.isFile1Loaded = false;
		this.remove(extgraph.getPanel());
		MainFrame.getInstance().getMainFrame().repaint();
		MainFrame.getInstance().getSouthPanel().revalidate();
		MainFrame.getInstance().getSouthPanel().repaint();		
	}
	public void resetLayout2() {		
		this.isFile2Loaded = false;
		this.remove(extgraph2.getPanel());
		MainFrame.getInstance().getMainFrame().repaint();
		MainFrame.getInstance().getSouthPanel().revalidate();
		MainFrame.getInstance().getSouthPanel().repaint();		
	}
	public void resetLayout3() {		
		this.isFile3Loaded = false;
		this.remove(extgraph3.getPanel());
		MainFrame.getInstance().getMainFrame().repaint();
		MainFrame.getInstance().getSouthPanel().revalidate();
		MainFrame.getInstance().getSouthPanel().repaint();		
	}
	public void setFile(File fl) {
		File outFile1 = new File(CurrentOpenData.getInstance().getFilePath(), "extfile.dat");
		copyFileToTempDir(fl, outFile1);
		loadFileFromTempDir();
	}

	public void setFile2(File fl) {
		File outFile2 = new File(CurrentOpenData.getInstance().getFilePath(), "extfile2.dat");
		copyFileToTempDir(fl, outFile2);		
		loadFile2FromTempDir();
	}
	
	public void setFile3(File fl) {
		File outFile3 = new File(CurrentOpenData.getInstance().getFilePath(), "extfile3.dat");
		copyFileToTempDir(fl, outFile3);
		loadFile3FromTempDir();
	}
	
	public void setXAxis(XAxis xAxis) {
		if (extgraph != null) {
			extgraph.setxAxis(xAxis);
		}
		if (extgraph2 != null) {
			extgraph2.setxAxis(xAxis);
		}
		if (extgraph3 != null) {
			extgraph3.setxAxis(xAxis);
		}
	}
	
	public void setnumberofExternalSignals(int no){
		signalcount = no;
	}
}
