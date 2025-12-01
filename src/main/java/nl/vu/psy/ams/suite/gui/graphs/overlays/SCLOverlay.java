package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.sets.EventRelatedSCLSet;
import nl.vu.psy.ams.suite.gui.drawing.BinaryDataDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.tabs.scl.SCLTab;
import nl.vu.psy.ams.suite.tools.Utils;

// To mark currently selected SCL Cycle - Only in Event based design

public class SCLOverlay extends Overlay {
	
	
	private EventRelatedSCLSet				SCRSet			= CurrentOpenData.getInstance().getEventSCLSet();
	private BinaryDataDrawer				draw;
	private double  						selTime 		= 0;
	private double  						YselTime		= 0;
	
	private SCLTab			tab;
	
	public SCLOverlay(Graph graph, SCLTab tab) {
		super(graph, true);
		this.tab = tab;
		graph.setToolTipText(null);
		setDraw((BinaryDataDrawer) graph.getDrawers().get(0));
	}

	@Override
	public void draw(Graphics2D g) {
		
			double		lfwindow 		= SCRSet.getleftWindow();
			double		rfwindow		= SCRSet.getSearchWindow();
			double 		lTime 			= graph.getxAxis().getLeftTime();
			double 		rTime 			= graph.getxAxis().getRightTime();		
			int 		w 		 		= graph.getWidth();
			int 		h 		 		= graph.getHeight();
		
			Color customColor = new Color(246, 187, 227, 128);
			g.setColor(customColor);
			int x = Utils.getPixelCoordinate((SCRSet.getCycleClosestToTime(selTime).getTime()- lfwindow), lTime, rTime, w);
			int wi = Utils.getPixelCoordinate((SCRSet.getCycleClosestToTime(selTime).getTime()+rfwindow), lTime, rTime, w);	
			g.fillRect(x, 0, (wi-x), h);

	}
		
	@Override
	public void mouseDragged(MouseEvent arg0) {
		
	}
	
	@Override
	public void mouseMoved(MouseEvent arg0) {
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		double selTime = graph.getxAxis().getTimeFromPixel(arg0.getX());
		double yselTime = graph.getxAxis().getTimeFromPixel(arg0.getY());
		setSelTime(selTime);
		setYselTime(yselTime);
		graph.repaint();
		
		tab.getEventInfoPanel().setSelEvent(SCRSet.getCycleIndexClosestToTime(selTime));		
		tab.getEventInfoPanel().updateLabelTexts(SCRSet.getCycleClosestToTime(selTime),false);
		tab.repaint();
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {

	}
	
	public void setSelTime(double time){
		this.selTime = time;
	}
	
	public double getSelTime(){
		return selTime;
	}

	public double getYselTime() {
		return YselTime;
	}

	public void setYselTime(double yselTime) {
		YselTime = yselTime;
	}
	
	public void stepNext(double time) {		
		graph.getxAxis().moveToTimeFast(time);
		graph.repaint();
	}

	public void stepPrev(double time) {
		graph.getxAxis().moveToTimeFast(time);
		graph.repaint();
	}

	public BinaryDataDrawer getDraw() {
		return draw;
	}

	public void setDraw(BinaryDataDrawer draw) {
		this.draw = draw;
	}
}
