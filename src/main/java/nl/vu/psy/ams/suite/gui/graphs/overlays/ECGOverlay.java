package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.SortedSet;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.ECGBeat;
import nl.vu.psy.ams.suite.data.structures.sets.ArtefactSet;
import nl.vu.psy.ams.suite.data.structures.sets.BeatSet;
import nl.vu.psy.ams.suite.data.structures.sets.RespirationSet;
import nl.vu.psy.ams.suite.data.structures.sets.SubSetIbiSet;
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
public class ECGOverlay extends Overlay {

	private BeatSet				beatSet	= CurrentOpenData.getInstance().getBeatSet();
	private RespirationSet		rSet	= CurrentOpenData.getInstance().getRespSet();
	private ArtefactSet			aSet	= CurrentOpenData.getInstance().getECGArtefacts();
	private double				beatTime;
	private int					dragType;
	private BinaryDataDrawer	draw;
	private SubSetIbiSet		ssSet	= CurrentOpenData.getInstance().getIbiSubSet(0);

	public ECGOverlay(Graph graph) {
		super(graph, true);
		draw = (BinaryDataDrawer) graph.getDrawers().get(graph.getDrawers().size() - 1);
		beatSet	= CurrentOpenData.getInstance().getBeatSet();
		ssSet	= CurrentOpenData.getInstance().getIbiSubSet(CurrentOpenData.getInstance().getECGChannel());
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
		SortedSet<ECGBeat> beats = beatSet.subSet(lTime, rTime);
		if (beats.size() < 1000) {
			int xPos, yPos;
			for (ECGBeat beat : beats) {
				double susp = beat.getIBISuspicion();
				if (susp > 1000) {
					g.setColor(Color.RED);
					g.setStroke(new BasicStroke(2));
				} else if (susp > 500) {
					g.setColor(Color.ORANGE);
					g.setStroke(new BasicStroke(2));
				} else {
					g.setColor(Color.BLUE);
					g.setStroke(new BasicStroke(1));
				}
				xPos = Utils.getPixelCoordinate(beat.getRPeakTime(), lTime, rTime, w);
				yPos = Utils.getPixelCoordinate(draw.getValueAtTime(beat.getRPeakTime()), botV, topV, h);
				g.drawLine(xPos, h - yPos, xPos, h - 5);
				g.drawLine(xPos, h, xPos - 5, h - 5);
				g.drawLine(xPos - 5, h - 5, xPos + 5, h - 5);
				g.drawLine(xPos + 5, h - 5, xPos, h);
			}
		}
		}
		g.setStroke(new BasicStroke(1));
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(new Font("Arial", Font.BOLD, 16));
		g.setColor(Color.BLUE);


		String txt			 	= "No of beats : "+CurrentOpenData.getInstance().getBeatSet().getBeats().size();
		FontMetrics metrics 	= g.getFontMetrics();
		Rectangle2D bounds 		= metrics.getStringBounds(txt, g);
		float yPos 				= (float) bounds.getHeight();
		g.drawString(txt, 5f, yPos);

		g.setColor(Color.BLACK);
		txt 					= "Number of diverging IBIs:";
		bounds 					= metrics.getStringBounds(txt, g);
		yPos 					+= (float) bounds.getHeight();
		g.drawString(txt, 5f, yPos);

		g.setColor(Color.RED);
		txt 					= "Deviant: " + beatSet.getnHigh();
		bounds 					= metrics.getStringBounds(txt, g);
		yPos 					+= (float) bounds.getHeight();
		g.drawString(txt, 5f, yPos);

		g.setColor(Color.ORANGE);
		txt 					= "Worth checking: " + beatSet.getnMed();
		bounds 					= metrics.getStringBounds(txt, g);
		yPos 					+= (float) bounds.getHeight();
		g.drawString(txt, 5f, yPos);

		g.setColor(Color.RED);
		txt 					= "Number of artefacts: " + aSet.getLabels().size();
		bounds 					= metrics.getStringBounds(txt, g);
		yPos 					+= (float) bounds.getHeight();
		g.drawString(txt, 5f, yPos);

		g.setColor(Color.BLACK);
		txt = "Currently selected IBI: " + (beatSet.getSelectedSuspiciousIBI() + 1);
		bounds = metrics.getStringBounds(txt, g);
		yPos += (float) bounds.getHeight();
		g.drawString(txt, 5f, yPos);
		float xPos = (float) (5f + bounds.getWidth());
		ECGBeat beat = beatSet.getBeatAtSelectedSuspiciousIBI();
		if (beat != null) {
			double susp = beatSet.getBeatAtSelectedSuspiciousIBI().getIBISuspicion();
			if (susp > 1000) {
				g.setColor(Color.RED);
				txt = "(Deviant)";
			} else if (susp > 500) {
				g.setColor(Color.ORANGE);
				txt = "(Worth checking)";
			} else {
				g.setColor(new Color(0, 128, 0));
				txt = "(Non-diverging)";
			}
			g.drawString(txt, xPos + 5f, yPos);
		}
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		if (dragType == 0) { // dragging a beat -  using left click
			double xPosTime = graph.getxAxis().getTimeFromPixel(arg0.getX());
			beatSet.changeBeat(beatTime, xPosTime);
			ssSet.changeAroundTime(beatTime);
			ssSet.changeAroundTime(xPosTime);
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
			double bTime = beatSet.getBeatClosestToTime(graph.getxAxis().getTimeFromPixel(xPos)).getRPeakTime();
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
			beatTime = beatSet.subSet(lPixTime, rPixTime).first().getRPeakTime();
			if (arg0.getButton() == MouseEvent.BUTTON3 || arg0.isControlDown()) {  // right click
				beatSet.removeBeat(beatTime);
				ssSet.changeAroundTime(beatTime);
				//---- Update Respiration cycle------
				rSet.rescan(beatTime-5000000, beatTime + 5000000);
				//-----------------------------------
				graph.getxAxis().repaintAll();
				dragType = 1;
			} else if (arg0.getButton() == MouseEvent.BUTTON1) {
				double botV = draw.getYAxis().getBottomValue();
				double topV = draw.getYAxis().getTopValue();
				int h = graph.getHeight();
				double bTime = beatSet.getBeatClosestToTime(graph.getxAxis().getTimeFromPixel(xPos)).getRPeakTime();
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
				beatSet.addBeat(xPosTime);
				ssSet.changeAroundTime(xPosTime);
				//---- Update Respiration cycle------
				rSet.rescan(xPosTime - 5000000, xPosTime + 5000000);
				//-----------------------------------
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
				beatSet.changeBeat(beatTime, vals[2 * maxi]);
				ssSet.changeAroundTime(beatTime);
				ssSet.changeAroundTime(vals[2 * maxi]);
				//---- Update Respiration cycle------
				rSet.rescan(beatTime- 5000000, beatTime+ 5000000);
				//-----------------------------------
				graph.getxAxis().repaintAll();
			}
		}
	}

}
