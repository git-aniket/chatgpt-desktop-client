package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.SortedSet;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.SCLCycle;
import nl.vu.psy.ams.suite.data.structures.sets.SkinConductanceSet;
import nl.vu.psy.ams.suite.gui.drawing.DataDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.tools.Utils;

// To draw all SCL Cycles
public class SCLabelSelectedOverlay extends Overlay {

	private SkinConductanceSet SCRSet = CurrentOpenData.getInstance().getSCLSet();
	private DataDrawer draw;
	private double selTime = 0;
	private double YselTime = 0;
	private int dragType;
	private int isOnset = -1;
	private SCLCycle rc, addedCycle;

	public SCLabelSelectedOverlay(Graph graph) {
		super(graph, true);
		graph.setToolTipText(null);
		draw = graph.getDrawers().get(graph.getDrawers().size() - 1);
		// bf= new BinaryFile("FILTSCL");
	}

	@Override
	public void draw(Graphics2D g) {
		int xPos, yPos, xPos1, yPos1;
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		double botV = graph.getDrawers().get(0).getYAxis().getBottomValue();
		double topV = graph.getDrawers().get(0).getYAxis().getTopValue();
		int w = graph.getWidth();
		int h = graph.getHeight();

		SortedSet<SCLCycle> scrsubset = SCRSet.subSet(lTime, rTime);

		for (SCLCycle beat : scrsubset) {

			g.setColor(Color.BLUE);
			xPos = Utils.getPixelCoordinate(beat.getPeakTime(), lTime, rTime, w);
			yPos = Utils.getPixelCoordinate(draw.getValueAtTime(beat.getPeakTime()), botV, topV, h);
			g.setStroke(new BasicStroke(2));
			g.drawLine(xPos, h - yPos - 5, xPos, h - yPos + 5);
			g.drawLine(xPos - 5, h - yPos, xPos + 5, h - yPos);

			g.setColor(Color.RED);
			xPos1 = Utils.getPixelCoordinate(beat.getOnsetTime(), lTime, rTime, w);
			yPos1 = Utils.getPixelCoordinate(draw.getValueAtTime(beat.getOnsetTime()), botV, topV, h);
			g.setStroke(new BasicStroke(2));
			g.drawLine(xPos1, h - yPos1 - 5, xPos1, h - yPos1 + 5);
			g.drawLine(xPos1 - 5, h - yPos1, xPos1 + 5, h - yPos1);
		}
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		double time = graph.getxAxis().getTimeFromPixel(arg0.getX());
		rc = SCRSet.getCycleClosestToTime(time);
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		double bVal = graph.getDrawers().get(0).getYAxis().getBottomValue();
		double tVal = graph.getDrawers().get(0).getYAxis().getTopValue();
		int w = graph.getWidth();
		int h = graph.getHeight();
		int xPosOnset = Utils.getPixelCoordinate(rc.getOnsetTime(), lTime, rTime, w);
		int yPosOnset = Utils.getPixelCoordinate(draw.getValueAtTime(rc.getOnsetTime()) - this.average, bVal, tVal, h);
		int xPosPeak = Utils.getPixelCoordinate(rc.getPeakTime(), lTime, rTime, w);
		int yPosPeak = Utils.getPixelCoordinate(draw.getValueAtTime(rc.getPeakTime()) - this.average, bVal, tVal, h);
		boolean clickOnDraw = false;
		if (arg0.getX() > xPosOnset - 10 && arg0.getX() < xPosOnset + 10 && arg0.getY() > h - yPosOnset - 10
				&& arg0.getY() < h - (yPosOnset - 10)) {
			clickOnDraw = true;
			isOnset = 1;
		}
		if (arg0.getX() > xPosPeak - 10 && arg0.getX() < xPosPeak + 10 && arg0.getY() > h - yPosPeak - 10
				&& arg0.getY() < h - (yPosPeak - 10)) {
			clickOnDraw = true;
			isOnset = 0;
		}
		if (clickOnDraw) {
			if (arg0.getButton() == MouseEvent.BUTTON3 || arg0.isControlDown()) { // right click
				SCRSet.getSCLCycles().remove(rc);
				graph.getxAxis().repaintAll();
				dragType = 1;
			} else if (arg0.getButton() == MouseEvent.BUTTON1) {
				int yPos = Utils.getPixelCoordinate(draw.getValueAtTime(time) - this.average, bVal, tVal, h);
				if (arg0.getY() > (h - yPos) - 10 && arg0.getY() < (h - yPos) + 10) {
					dragType = 0;
				} else {
					dragType = -1;
				}
			}
		} else {
			if (arg0.getButton() == MouseEvent.BUTTON1) { // left click
				// add cycle
				int yPos = Utils.getPixelCoordinate(draw.getValueAtTime(time) - this.average, bVal, tVal, h);
				if (arg0.getY() > (h - yPos) - 10 && arg0.getY() < (h - yPos) + 10) {
					addedCycle = new SCLCycle(time);
					if (SCRSet.getPrevCycle(addedCycle) == null || SCRSet.getPrevCycle(addedCycle).isPeak()) {
						addedCycle.setOnsetTime(time);
						addedCycle.setOnsetValue(draw.getValueAtTime(time) - this.average);
						addedCycle.setPeakTime(-9999);
						addedCycle.setPeakValue(-9999);
						addedCycle.setOnset(true);
						addedCycle.setPeak(false);
						addedCycle.setSCLValue(draw.getValueAtTime(time) - this.average);
					} else {
						addedCycle.setPeakTime(time);
						addedCycle.setPeakValue(draw.getValueAtTime(time) - this.average);
						addedCycle.setOnsetTime(-9999);
						addedCycle.setOnsetValue(-9999);
						addedCycle.setOnset(false);
						addedCycle.setPeak(true);
						addedCycle.setSCLValue(draw.getValueAtTime(time) - this.average);
					}
					SCRSet.getSCLCycles().add(addedCycle);
					graph.getxAxis().repaintAll();
				}
				dragType = 0;
				isOnset = -1;
			}
		}
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		if (dragType == 0 && isOnset >= 0) { // dragging a cycle time - using left click
			double[] boundaries = new double[2];
			boundaries[0] = SCRSet.getPrevCycle(rc).getTime();
			boundaries[1] = SCRSet.getNextCycle(rc).getTime();
			double xPosTime = graph.getxAxis().getTimeFromPixel(arg0.getX());
			if (xPosTime > boundaries[0] && xPosTime < boundaries[1]) {
				if (isOnset == 0) {
					rc.setPeakTime(xPosTime);
					rc.setTime(xPosTime);
				} else if (isOnset == 1) {
					rc.setOnsetTime(xPosTime);
					rc.setTime(xPosTime);
				}
			}
			graph.getxAxis().repaintAll();
		}
	}

	public void setSelTime(double time) {
		this.selTime = time;
	}

	public double getSelTime() {
		return selTime;
	}

	public double getYselTime() {
		return YselTime;
	}

	public void setYselTime(double yselTime) {
		YselTime = yselTime;
	}

}
