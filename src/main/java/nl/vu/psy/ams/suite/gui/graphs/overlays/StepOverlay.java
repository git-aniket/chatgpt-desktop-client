package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.SortedSet;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.Step;
import nl.vu.psy.ams.suite.data.structures.sets.StepSet;
import nl.vu.psy.ams.suite.gui.drawing.BinaryDataDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;
//import nl.vu.psy.ams.suite.tools.VUDAMSDebugSettings;
/*
 * Overlay that displays beat lines and allows editing beats.
 * Used during QRS complex editing.
 */
public class StepOverlay extends Overlay {

	private StepSet				beatSet	= CurrentOpenData.getInstance().getStepSet();
	private double				beatTime;
	private int					dragType;
	private BinaryDataDrawer	draw;

	public StepOverlay(Graph graph) {
		super(graph, true);
		draw = (BinaryDataDrawer) graph.getDrawers().get(0);
		beatSet	= CurrentOpenData.getInstance().getStepSet();
	}

	@Override
	public void draw(Graphics2D g) {
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		double botV = draw.getYAxis().getBottomValue();
		double topV = draw.getYAxis().getTopValue();
		int w = graph.getWidth();
		int h = graph.getHeight();
		if (rTime > lTime) {
		SortedSet<Step> beats = beatSet.subSet(lTime, rTime);
		if (beats.size() < 1000) {
			int xPos, yPos;
			for (Step beat : beats) {
				g.setColor(Color.MAGENTA);
				g.setStroke(new BasicStroke(1));
				xPos = Utils.getPixelCoordinate(beat.getTime(), lTime, rTime, w);
				yPos = Utils.getPixelCoordinate(draw.getValueAtTime(beat.getTime()), botV, topV, h);
				g.drawLine(xPos, h - yPos, xPos, h - 5);
				g.drawLine(xPos, h, xPos - 5, h - 5);
				g.drawLine(xPos - 5, h - 5, xPos + 5, h - 5);
				g.drawLine(xPos + 5, h - 5, xPos, h);
			}
		}
		}
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		if (dragType == 0) { // dragging a beat -  using left click
			double xPosTime = graph.getxAxis().getTimeFromPixel(arg0.getX());
			beatSet.changeStep(beatTime, xPosTime);
			beatTime = xPosTime;
			graph.getxAxis().repaintAll();
		}
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		int gridSize = AppSettings.getInstance().getIntProperty(Settings.SNAPSIZE);
		int xPos = arg0.getX();
		double lPixTime = graph.getxAxis().getTimeFromPixel(xPos - gridSize);
		double rPixTime = graph.getxAxis().getTimeFromPixel(xPos + gridSize);
		if (lPixTime > rPixTime)
			graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
		else if (beatSet.contains(lPixTime, rPixTime) == true) {
			double botV = draw.getYAxis().getBottomValue();
			double topV = draw.getYAxis().getTopValue();
			int h = graph.getHeight();
			double bTime = beatSet.getStepClosestToTime(graph.getxAxis().getTimeFromPixel(xPos)).getTime();
			int yPos = Utils.getPixelCoordinate(draw.getValueAtTime(bTime), botV, topV, h);
			if (arg0.getY() > (h - yPos)) {
				graph.setCursor(Utils.getCursor(Cursor.W_RESIZE_CURSOR));
			} else {
				graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
			}
		} else {
			graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		int gridSize = AppSettings.getInstance().getIntProperty(Settings.SNAPSIZE);
		int xPos = arg0.getX();
		double lPixTime = graph.getxAxis().getTimeFromPixel(xPos - gridSize);
		double rPixTime = graph.getxAxis().getTimeFromPixel(xPos + gridSize);
		if (beatSet.contains(lPixTime, rPixTime) == true) {
			beatTime = beatSet.subSet(lPixTime, rPixTime).first().getTime();
			if (arg0.getButton() == MouseEvent.BUTTON3 || arg0.isControlDown()) {  // right click
				beatSet.removeStep(beatTime);
				graph.getxAxis().repaintAll();
				dragType = 1;
			} else if (arg0.getButton() == MouseEvent.BUTTON1) {
				double botV = draw.getYAxis().getBottomValue();
				double topV = draw.getYAxis().getTopValue();
				int h = graph.getHeight();
				double bTime = beatSet.getStepClosestToTime(graph.getxAxis().getTimeFromPixel(xPos)).getTime();
				int yPos = Utils.getPixelCoordinate(draw.getValueAtTime(bTime), botV, topV, h);
				if (arg0.getY() > (h - yPos)) {
					dragType = 0;
				} else {
					dragType = -1;
				}
			}
		} else {
			if (arg0.getButton() == MouseEvent.BUTTON1) { // left click
				double xPosTime = graph.getxAxis().getTimeFromPixel(xPos);
				beatSet.addStep(xPosTime);
				beatTime = xPosTime;
				graph.getxAxis().repaintAll();
				dragType = 0;
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		int gridSize = AppSettings.getInstance().getIntProperty(Settings.SNAPSIZE);
		if (dragType == 0) {
			int xPos = arg0.getX();
			double lPixTime = graph.getxAxis().getTimeFromPixel(xPos - gridSize);
			double rPixTime = graph.getxAxis().getTimeFromPixel(xPos + gridSize);
			double vals[] = draw.getValuesBetweenTimes(lPixTime, rPixTime);
			if (vals != null) {
				double max = Double.NEGATIVE_INFINITY;
				int n = vals.length / 2;
				int maxi = -1;
				File curFile = new File(System.getProperty("user.dir"), "DoNotJumpToRpeakTop.txt");
				if(curFile.exists() == false){
					for (int i = 0; i < n; i++) {
						if (vals[2 * i + 1] > max) {
							max = vals[2 * i + 1];
							maxi = i;
						}
					}
				}
				beatSet.changeStep(beatTime, vals[2 * maxi]);
				graph.getxAxis().repaintAll();
			}
		}
	}

}
