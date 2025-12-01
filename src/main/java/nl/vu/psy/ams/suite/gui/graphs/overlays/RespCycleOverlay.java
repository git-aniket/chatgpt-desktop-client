package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.NavigableSet;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.data.structures.RespirationCycle;
import nl.vu.psy.ams.suite.data.structures.sets.RespirationSet;
import nl.vu.psy.ams.suite.gui.drawing.BinaryDataDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Show the start of expiration and inspiration of all
 * cycles with triangles. Used in RSA tab.
 */
public class RespCycleOverlay extends Overlay {

	private RespirationSet rSet = CurrentOpenData.getInstance().getRespSet();
	private ArrayList<RespirationCycle> cycleArray = CurrentOpenData.getInstance().getRespSet().getCycleArray();
	private int dragType;
	private int isInsp = -1;
	private RespirationCycle rc, addedCycle;
	private BinaryDataDrawer draw;
	private boolean firstClick = false;
	private BinaryFile bff = new BinaryFile("FILTDZ"), bf;

	public RespCycleOverlay(Graph graph) {
		super(graph, true);
		draw = (BinaryDataDrawer) graph.getDrawers().get(graph.getDrawers().size() - 1);
		CurrentOpenData cod = CurrentOpenData.getInstance();
		if (Utils.getExtension(cod.getDataFile()).equals("7fs") || cod.fileHeader.getDwHardwareVersion() == 7)
			bf = new BinaryFile("Z0");
		else
			bf = new BinaryFile("DZ");
	}

	@Override
	public void draw(Graphics2D g) {
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		double bVal = graph.getDrawers().get(0).getYAxis().getBottomValue();
		double tVal = graph.getDrawers().get(0).getYAxis().getTopValue();
		int w = graph.getWidth();
		int h = graph.getHeight();
		NavigableSet<RespirationCycle> cyc = rSet.getSubset(lTime, rTime);
		int xPos, yPos;
		for (RespirationCycle rc : cyc) {
			g.setColor(Color.RED);
			xPos = Utils.getPixelCoordinate(rc.getInspStart(), lTime, rTime, w);
			yPos = Utils.getPixelCoordinate(draw.getValueAtTime(rc.getInspStart()) - this.average, bVal, tVal, h);
			g.drawLine(xPos, h - yPos, xPos - 5, h - (yPos - 10));
			g.drawLine(xPos - 5, h - (yPos - 10), xPos + 5, h - (yPos - 10));
			g.drawLine(xPos + 5, h - (yPos - 10), xPos, h - yPos);
		}
		for (RespirationCycle rc : cyc) {
			g.setColor(Color.BLUE);
			xPos = Utils.getPixelCoordinate(rc.getExpStart(), lTime, rTime, w);
			yPos = Utils.getPixelCoordinate(draw.getValueAtTime(rc.getExpStart()) - this.average, bVal, tVal, h);
			g.drawLine(xPos, h - yPos, xPos - 5, h - (yPos + 10));
			g.drawLine(xPos - 5, h - (yPos + 10), xPos + 5, h - (yPos + 10));
			g.drawLine(xPos + 5, h - (yPos + 10), xPos, h - yPos);
		}
		if (firstClick && addedCycle != null) {
			g.setColor(Color.DARK_GRAY);
			g.setStroke(new BasicStroke(3));
			xPos = Utils.getPixelCoordinate(addedCycle.getInspStart(), lTime, rTime, w);
			// yPos = Utils.getPixelCoordinate(addedCycle.getLowValue() - this.average,
			// bVal, tVal, h);
			yPos = Utils.getPixelCoordinate(draw.getValueAtTime(addedCycle.getInspStart()) - this.average, bVal, tVal,
					h);
			g.drawLine(xPos, h - yPos, xPos - 5, h - (yPos - 10));
			g.drawLine(xPos - 5, h - (yPos - 10), xPos + 5, h - (yPos - 10));
			g.drawLine(xPos + 5, h - (yPos - 10), xPos, h - yPos);
		}
		g.setStroke(new BasicStroke(1));
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(new Font("Arial", Font.BOLD, 16));
		g.setColor(Color.BLUE);

		String txt = "No of cycles : " + CurrentOpenData.getInstance().getRespSet().getCycles().size();
		FontMetrics metrics = g.getFontMetrics();
		Rectangle2D bounds = metrics.getStringBounds(txt, g);
		float yPosf = (float) bounds.getHeight();
		g.drawString(txt, 5f, yPosf);

		// g.setColor(Color.BLACK);
		// txt = "Number of diverging cycles:";
		// bounds = metrics.getStringBounds(txt, g);
		// yPosf += (float) bounds.getHeight();
		// g.drawString(txt, 5f, yPosf);

		g.setColor(Color.RED);
		txt = "Deviant cycles: " + rSet.getnHigh();
		bounds = metrics.getStringBounds(txt, g);
		yPosf += (float) bounds.getHeight();
		g.drawString(txt, 5f, yPosf);

		g.setColor(Color.ORANGE);
		txt = "Cycles worth checking: " + rSet.getnMed();
		bounds = metrics.getStringBounds(txt, g);
		yPosf += (float) bounds.getHeight();
		g.drawString(txt, 5f, yPosf);

		g.setColor(Color.BLACK);
		txt = "Currently selected cycle: " + (rSet.getSelectedSuspiciousRR() + 1);
		bounds = metrics.getStringBounds(txt, g);
		yPosf += (float) bounds.getHeight();
		g.drawString(txt, 5f, yPosf);
		float xPosf = (float) (5f + bounds.getWidth());
		RespirationCycle cycle = rSet.getBeatAtSelectedSuspiciousRR();
		double maxDiff = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSARRATEMAX) / 100.;
		if (cycle != null) {
			double susp = rSet.getBeatAtSelectedSuspiciousRR().getRRSuspicion();
			if (susp > maxDiff * 2) {
				g.setColor(Color.RED);
				txt = "(Deviant)";
			} else if (susp > maxDiff) {
				g.setColor(Color.ORANGE);
				txt = "(Worth checking)";
			} else {
				g.setColor(new Color(0, 128, 0));
				txt = "(Non-diverging)";
			}
			g.drawString(txt, xPosf + 5f, yPosf);
		}
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		double time = graph.getxAxis().getTimeFromPixel(arg0.getX());
		if (cycleArray.isEmpty())
			return;
		rc = rSet.getCyclesBetweenTimes(time, time).pollFirst();
		RespirationCycle next = rSet.getNextCycle(rc);
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		double bVal = graph.getDrawers().get(0).getYAxis().getBottomValue();
		double tVal = graph.getDrawers().get(0).getYAxis().getTopValue();
		int w = graph.getWidth();
		int h = graph.getHeight();
		int xPosInsp = Utils.getPixelCoordinate(rc.getInspStart(), lTime, rTime, w);
		int yPosInsp = Utils.getPixelCoordinate(draw.getValueAtTime(rc.getInspStart()) - this.average, bVal, tVal, h);
		int xPosExp = Utils.getPixelCoordinate(rc.getExpStart(), lTime, rTime, w);
		int yPosExp = Utils.getPixelCoordinate(draw.getValueAtTime(rc.getExpStart()) - this.average, bVal, tVal, h);
		int xPosInspn = Utils.getPixelCoordinate(next.getInspStart(), lTime, rTime, w);
		int yPosInspn = Utils.getPixelCoordinate(draw.getValueAtTime(next.getInspStart()) - this.average, bVal, tVal,
				h);
		boolean clickOnDraw = false;
		if (arg0.getX() > xPosInsp - 10 && arg0.getX() < xPosInsp + 10 && arg0.getY() > h - yPosInsp - 5
				&& arg0.getY() < h - (yPosInsp - 15)) {
			clickOnDraw = true;
			isInsp = 1;
		}
		if (arg0.getX() > xPosExp - 10 && arg0.getX() < xPosExp + 10 && arg0.getY() > h - (yPosExp + 15)
				&& arg0.getY() < h - yPosExp + 5) {
			clickOnDraw = true;
			isInsp = 0;
		}
		if (arg0.getX() > xPosInspn - 10 && arg0.getX() < xPosInspn + 10 && arg0.getY() > h - yPosInspn - 5
				&& arg0.getY() < h - (yPosInspn - 15)) {
			clickOnDraw = true;
			isInsp = 2;
		}
		if (clickOnDraw) {
			if (arg0.getButton() == MouseEvent.BUTTON3 || arg0.isControlDown()) { // right click
				RespirationCycle prev = rSet.getPrevCycle(rc);
				if (prev != null)
					prev.setExpEnd(rc.getExpEnd());
				// prev.delExpEnd();
				cycleArray.remove(rc);
				rSet.getCycles().remove(rc);
				rSet.rescan(time - 5000000, time + 5000000);
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
				// add breath
				double xPosTime = graph.getxAxis().getTimeFromPixel(arg0.getX());
				int yPos = Utils.getPixelCoordinate(draw.getValueAtTime(time) - this.average, bVal, tVal, h);
				if (arg0.getY() > (h - yPos) - 10 && arg0.getY() < (h - yPos) + 10) {
					firstClick = !firstClick;
					if (firstClick) {
						if (xPosTime < rc.getExpStart())
							firstClick = false;
						else {
							addedCycle = new RespirationCycle(xPosTime);
							addedCycle.setLowValue(bf.getDataAtTime(xPosTime));
							addedCycle.setLowRealValue(bff.getDataAtTime(xPosTime));
							isInsp = 1;
						}
					} else {
						if (xPosTime > addedCycle.getInspStart()
								&& xPosTime < rSet.getNextCycle(addedCycle).getInspStart()) {
							addedCycle.setExpStart(xPosTime);
							addedCycle.setHighValue(bf.getDataAtTime(xPosTime));
							addedCycle.setHighRealValue(bff.getDataAtTime(xPosTime));
							rSet.getPrevCycle(addedCycle).setExpEnd(addedCycle.getInspStart());
							addedCycle.setExpEnd(rSet.getNextCycle(addedCycle).getInspStart());
							cycleArray.add(addedCycle);
							rSet.getCycles().add(addedCycle);
							rSet.rescan(time - 5000000, time + 5000000);
							isInsp = 0;
						} else {
							firstClick = true;
						}
					}
					rc = addedCycle;
					graph.getxAxis().repaintAll();
					dragType = 0;
				} else if (firstClick) {
					addedCycle = null;
					firstClick = false;
					graph.repaint();
					dragType = -1;
				}
			}
		}
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		if (dragType == 0 && isInsp >= 0) { // dragging an insp or exsp time - using left click
			double[] boundaries = new double[2];
			if (isInsp == 0) {
				boundaries[0] = rc.getInspStart();
				boundaries[1] = rSet.getNextCycle(rc).getInspStart();
			} else if (isInsp == 1) {
				boundaries[0] = rSet.getPrevCycle(rc).getExpStart();
				boundaries[1] = rc.getExpStart();
			} else {
				boundaries[0] = rc.getExpStart();
				boundaries[1] = rSet.getNextCycle(rc).getExpStart();
			}
			double xPosTime = graph.getxAxis().getTimeFromPixel(arg0.getX());
			if (xPosTime > boundaries[0] && xPosTime < boundaries[1]) {
				if (isInsp == 0) {
					rc.setExpStart(xPosTime);
					rc.setHighValue(bf.getDataAtTime(xPosTime));
					rc.setHighRealValue(bff.getDataAtTime(xPosTime));
				} else if (isInsp == 1) {
					rSet.getPrevCycle(rc).setExpEnd(xPosTime);
					rc.setInspStart(xPosTime);
					rc.setLowValue(bf.getDataAtTime(xPosTime));
					rc.setLowRealValue(bff.getDataAtTime(xPosTime));
				} else {
					rc.setExpEnd(xPosTime);
					rSet.getNextCycle(rc).setInspStart(xPosTime);
					rSet.getNextCycle(rc).setLowValue(bf.getDataAtTime(xPosTime));
					rSet.getNextCycle(rc).setLowRealValue(bff.getDataAtTime(xPosTime));
				}
				rSet.rescan(xPosTime - 5000000, xPosTime + 5000000);
				graph.getxAxis().repaintAll();
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		int gridSize = AppSettings.getInstance().getIntProperty(Settings.SNAPSIZE) * 5;
		if (dragType == 0) {
			int xPos = arg0.getX();
			double lPixTime = graph.getxAxis().getTimeFromPixel(xPos - gridSize);
			double rPixTime = graph.getxAxis().getTimeFromPixel(xPos + gridSize);
			double vals[] = draw.getValuesBetweenTimes(lPixTime, rPixTime);
			if (vals != null) {
				double max = Double.NEGATIVE_INFINITY;
				double min = Double.POSITIVE_INFINITY;
				int n = vals.length / 2;
				int maxi = -1, mini = 0;
				File curFile = new File(System.getProperty("user.dir"), "DoNotJumpToRpeakTop.txt");
				if (curFile.exists() == false) {
					for (int i = 0; i < n; i++) {
						if (vals[2 * i + 1] > max) {
							max = vals[2 * i + 1];
							maxi = i;
						}
						if (vals[2 * i + 1] < min) {
							min = vals[2 * i + 1];
							mini = i;
						}
					}
				}
				if (isInsp == 0) {
					rc.setExpStart(vals[2 * maxi]);
					rc.setHighValue(bf.getDataAtTime(vals[2 * maxi]));
					rc.setHighRealValue(bff.getDataAtTime(vals[2 * maxi]));
					rSet.rescan(vals[2 * maxi] - 5000000, vals[2 * maxi] + 5000000);
				} else if (isInsp == 1) {
					rSet.getPrevCycle(rc).setExpEnd(vals[2 * mini]);
					rc.setInspStart(vals[2 * mini]);
					rc.setLowValue(bf.getDataAtTime(vals[2 * mini]));
					rc.setLowRealValue(bff.getDataAtTime(vals[2 * mini]));
					rSet.rescan(vals[2 * mini] - 5000000, vals[2 * mini] + 5000000);
				} else {
					rc.setExpEnd(vals[2 * maxi]);
					rSet.getNextCycle(rc).setInspStart(vals[2 * maxi]);
					rSet.getNextCycle(rc).setLowValue(bf.getDataAtTime(vals[2 * maxi]));
					rSet.getNextCycle(rc).setLowRealValue(bff.getDataAtTime(vals[2 * maxi]));
					rSet.rescan(vals[2 * maxi] - 5000000, vals[2 * maxi] + 5000000);
				}
				graph.getxAxis().repaintAll();
				CurrentOpenData.getInstance().setDirty(true);
			}
		}
	}
}
