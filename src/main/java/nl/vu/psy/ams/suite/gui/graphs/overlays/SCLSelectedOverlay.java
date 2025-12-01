package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.SortedSet;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
//import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.data.structures.EventRelatedSCLCycle;
import nl.vu.psy.ams.suite.data.structures.sets.EventRelatedSCLSet;
import nl.vu.psy.ams.suite.gui.drawing.BinaryDataDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.tools.Utils;

// To draw event based SCL Cycles
public class SCLSelectedOverlay extends Overlay {
	
	private EventRelatedSCLSet				SCRSet			= CurrentOpenData.getInstance().getEventSCLSet();
	private BinaryDataDrawer				draw;
	private double  						selTime 		= 0;
	private double  						YselTime		= 0;
	//private BinaryFile 						bf;

	
	public SCLSelectedOverlay(Graph graph) {
		super(graph, true);
		graph.setToolTipText(null);
		draw = (BinaryDataDrawer) graph.getDrawers().get(0);
		//bf= new BinaryFile("FILTSCL");
	}

	@Override
	public void draw(Graphics2D g) {
		int xPos, yPos, xPos1, yPos1;
		double lTime 								= graph.getxAxis().getLeftTime();
		double rTime 								= graph.getxAxis().getRightTime();
		double botV  								= graph.getDrawers().get(0).getYAxis().getBottomValue();
		double topV  								= graph.getDrawers().get(0).getYAxis().getTopValue();
		int w 		 								= graph.getWidth();
		int h 										= graph.getHeight();
	
		SortedSet<EventRelatedSCLCycle> scrsubset 	= SCRSet.subSet(lTime, rTime);
			
			for (EventRelatedSCLCycle beat : scrsubset) {				
				
				g.setColor(Color.BLUE);			
				xPos = Utils.getPixelCoordinate(beat.getPeakTime(), lTime, rTime, w);
				yPos = Utils.getPixelCoordinate(draw.getValueAtTime(beat.getPeakTime()), botV, topV, h);								
				g.setStroke( new BasicStroke(2));
				g.drawLine(xPos, h - yPos - 5, xPos, h - yPos + 5);
				g.drawLine(xPos - 5, h - yPos, xPos + 5, h - yPos);
				
				g.setColor(Color.RED);				
				xPos1 = Utils.getPixelCoordinate(beat.getOnset_Time(), lTime, rTime, w);
				yPos1 = Utils.getPixelCoordinate(draw.getValueAtTime(beat.getOnset_Time()), botV, topV, h);							
				g.setStroke( new BasicStroke(2));
				g.drawLine(xPos1, h - yPos1 - 5, xPos1, h - yPos1 + 5);
				g.drawLine(xPos1 - 5, h - yPos1, xPos1 + 5, h - yPos1);
			}		
	}
		
	@Override
	public void mouseDragged(MouseEvent arg0) {
		
	}
	
	@Override
	public void mouseMoved(MouseEvent arg0) {
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
	
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
	
}
