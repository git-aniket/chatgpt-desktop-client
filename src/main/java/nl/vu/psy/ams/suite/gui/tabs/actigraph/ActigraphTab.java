package nl.vu.psy.ams.suite.gui.tabs.actigraph;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Vector;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.gui.MainMenuBar;
import nl.vu.psy.ams.suite.gui.TimeBar;
import nl.vu.psy.ams.suite.gui.axes.XAxis;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.drawing.ActigraphDataDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.graphs.overlays.GridOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.LabelOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.TimeShowOverlay;
import nl.vu.psy.ams.suite.gui.tabs.AmsTab;
/*
 * The frequency analasys tab
 */
public class ActigraphTab extends AmsTab {

	private static final long	serialVersionUID	= 1L;
	private static ActigraphTab	instance;
	
	private File actifile;
	public static ActigraphTab getInstance() {
		if (instance == null) {
			instance = new ActigraphTab();
		}
		return instance;
	}
	public static ActigraphTab getNewInstance() {
		instance = null;
		instance = new ActigraphTab();
		return instance;
	}

	private LabelOverlay	lov;
	private boolean	isFile1Loaded;
	private Graph actigraph_MXR ;
	private Graph actigraph_MYR ;
	private Graph actigraph_MZR ;
	
	private ActigraphTab() {
		super();
		TimeBar.getInstance().connectToXAxis(mainXAxis);
		setToolBar(new ActigraphToolbar(this));
	}
	@Override
	public void autoscale() {
		mainXAxis.autoscaleConnectedGraphs();
		mainXAxis.autoscaleConnectedGraphsFast();
	}

	public XAxis getXAxis() {
		return mainXAxis;
	}
	@Override
	public boolean setActive() {
		
		super.setActive();		
		return true;
	}

	@Override
	public void setupItems() {
		
		if(MainMenuBar.getInstance().getActiFile() != null){
			File outFile1 				= new File(CurrentOpenData.getInstance().getFilePath(), "Actigraph_Motility.dat");		
			copyFileToTempDir(MainMenuBar.getInstance().getActiFile(), outFile1);		
			loadFileFromTempDir();
		}
		Vector<TimeShowOverlay>	tsOverlays = new Vector<TimeShowOverlay>();		
		
		Graph tGraph = new Graph(mainXAxis);
		lov = new LabelOverlay(tGraph);
		lov.setEditable(false);
		lov.setLabelConfig(CurrentOpenData.getInstance().getLabelConfig());
		lov.setLabels(CurrentOpenData.getInstance().getLabels());
		lov.setTitle("Labels");
		tGraph.addOverlay(lov);
		tGraph.setEmptyYAxis();
		ArrayList<String> cats = CurrentOpenData.getInstance().getLabelConfig().getCategories();
		tGraph.getPanel().setPreferredSize(new Dimension(1, 20 * cats.size()));
		tGraph.getPanel().setMaximumSize(new Dimension(Integer.MAX_VALUE, 20 * cats.size()));
		panel.add(tGraph.getPanel());

		actigraph_MXR = new Graph(mainXAxis);
		actigraph_MXR.addUnderlay(new GridOverlay(actigraph_MXR));
		ActigraphDataDrawer a_MXR = new ActigraphDataDrawer("Actigraph_MXR", new YAxis(0, 1, 0, 1,"MXR"));
		a_MXR.getYAxis().setBottomValue(0);
		a_MXR.getYAxis().setTopValue(1);
		a_MXR.connectToGraph(actigraph_MXR);
		actigraph_MXR.setActiveYAxis(a_MXR.getYAxis());				
		TimeShowOverlay tso = new TimeShowOverlay(actigraph_MXR);
		actigraph_MXR.addOverlay(tso);
		tsOverlays.add(tso);
		panel.add(actigraph_MXR.getPanel());
		
		actigraph_MYR = new Graph(mainXAxis);
		actigraph_MYR.addUnderlay(new GridOverlay(actigraph_MYR));
		ActigraphDataDrawer a_MYR = new ActigraphDataDrawer("Actigraph_MYR", new YAxis(0, 1, 0, 1, "MYR"));
		a_MYR.getYAxis().setBottomValue(0);
		a_MYR.getYAxis().setTopValue(1);
		a_MYR.connectToGraph(actigraph_MYR);
		actigraph_MYR.setActiveYAxis(a_MYR.getYAxis());				
		tso = new TimeShowOverlay(actigraph_MYR);
		actigraph_MYR.addOverlay(tso);
		tsOverlays.add(tso);
		panel.add(actigraph_MYR.getPanel());
		
		actigraph_MZR = new Graph(mainXAxis);
		actigraph_MZR.addUnderlay(new GridOverlay(actigraph_MZR));
		ActigraphDataDrawer a_MZR = new ActigraphDataDrawer("Actigraph_MZR", new YAxis(0, 1, 0, 1, "MZR"));
		a_MZR.getYAxis().setBottomValue(0);
		a_MZR.getYAxis().setTopValue(1);
		a_MZR.connectToGraph(actigraph_MZR);
		actigraph_MZR.setActiveYAxis(a_MZR.getYAxis());				
		tso = new TimeShowOverlay(actigraph_MZR);
		actigraph_MZR.addOverlay(tso);
		tsOverlays.add(tso);
		panel.add(actigraph_MZR.getPanel());
		
		for (TimeShowOverlay ts : tsOverlays)
			ts.setConnectedOverlays(tsOverlays);
		panel.add(mainXAxis.getPanel());

	}
	public void updateLabelConfig() {
		lov.updateLabelConfig();
	}
	
	public void loadFileFromTempDir() {
		this.isFile1Loaded = true;
		//setupItems();
	}
	public boolean isLoaded() {
		return isFile1Loaded;
	}
	public void setFile(File fl) {
		actifile 					= fl;
		//File outFile1 				= new File(CurrentOpenData.getInstance().getFilePath(), "Actigraph_Motility.dat");		
		//copyFileToTempDir(fl, outFile1);		
		//loadFileFromTempDir();
	}
	
	public File getFile(){
		return actifile;
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
	
	
}
