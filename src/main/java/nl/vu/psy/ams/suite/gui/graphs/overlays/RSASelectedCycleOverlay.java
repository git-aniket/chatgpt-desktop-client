package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.RespirationCycle;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.tabs.rsa.RespirationCycleInfoPanel;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Overlay that shows the length of inspiration and expiration plus the 
 * phase shift (e.g. the window where smallest and longest ibis are searched for)
 * for a single selected respiration cycle.
 */
public class RSASelectedCycleOverlay extends Overlay {

	private ArrayList<RespirationCycle> cycleArray = CurrentOpenData.getInstance().getRespSet().getCycleArray();
	private int selCycle = 0;
	private ArrayList<RSASelectedCycleOverlay> otherOverlays = new ArrayList<RSASelectedCycleOverlay>();
	private RespirationCycleInfoPanel infoPanel;

	public RSASelectedCycleOverlay(Graph graph) {
		super(graph, true);
	}

	private void addOverlay(RSASelectedCycleOverlay ov) {
		otherOverlays.add(ov);
	}

	public void connectToEachOther(RSASelectedCycleOverlay ov) {
		addOverlay(ov);
		ov.addOverlay(this);
	}

	@Override
	public void draw(Graphics2D g) {
		if (selCycle >= cycleArray.size() || selCycle < 0)
			return;
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		int w = graph.getWidth();
		int h = graph.getHeight();

		double inspTime = cycleArray.get(selCycle).getInspStart();
		double expTime = cycleArray.get(selCycle).getExpStart();

		double afterShortest = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAAFTERSHORTEST) * 1000.;
		double afterLongest = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.RSAAFTERLONGEST) * 1000.;

		int xInspPos = Utils.getPixelCoordinate(inspTime, lTime, rTime, w);
		int xExpPos = Utils.getPixelCoordinate(expTime, lTime, rTime, w);
		int xExpBuffPos = Utils.getPixelCoordinate(expTime + afterShortest, lTime, rTime, w);
		g.setColor(Color.RED);
		g.drawLine(xInspPos, 0, xInspPos, h);
		Composite prevComposite = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		g.fillRect(xInspPos, 0, xExpBuffPos - xInspPos, h);
		g.setComposite(prevComposite);

		g.setColor(Color.BLUE);
		g.drawLine(xExpPos, 0, xExpPos, h);
		if (cycleArray.get(selCycle).isExpEndSet()) {
			int xExpEndPos = Utils.getPixelCoordinate(cycleArray.get(selCycle).getExpEnd() + afterLongest, lTime, rTime,
					w);
			prevComposite = g.getComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
			g.fillRect(xExpPos, 0, xExpEndPos - xExpPos, h);
			g.setComposite(prevComposite);
		}
	}

	public RespirationCycle getSelectedCycle() {
		if (cycleArray.isEmpty())
			return null;
		if (selCycle < 0 || selCycle > cycleArray.size() - 1)
			return null;
		return cycleArray.get(selCycle);
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		double time = graph.getxAxis().getTimeFromPixel(arg0.getX());
		setSelectedToTime(time, false);
		for (RSASelectedCycleOverlay ov : otherOverlays) {
			ov.setSelectedToTime(time, false);
		}
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		if (arg0.getButton() == MouseEvent.BUTTON1) {
			double time = graph.getxAxis().getTimeFromPixel(arg0.getX());
			setSelectedToTime(time, false);
			for (RSASelectedCycleOverlay ov : otherOverlays) {
				ov.setSelectedToTime(time, false);
			}
		}
	}

	public void setInfoPanel(RespirationCycleInfoPanel respirationCycleInfoPanel) {
		infoPanel = respirationCycleInfoPanel;
	}

	public void setSelectedToTime(double time, boolean center) {
		if (cycleArray.isEmpty())
			return;
		selCycle = cycleArray.size() / 2;
		int curWidth = cycleArray.size() / 4;
		while (curWidth > 0) {
			double probeVal = cycleArray.get(selCycle).getExpStart();
			if (time < probeVal) {
				selCycle -= curWidth;
			} else {
				selCycle += curWidth;
			}
			curWidth /= 2;
		}
		boolean done = false;
		while (done == false) {
			if (cycleArray.get(selCycle).isExpEndSet() == false) {
				done = true;
			} else {
				if (cycleArray.get(selCycle).getInspStart() < time && cycleArray.get(selCycle).getExpEnd() > time) {
					done = true;
				} else if (cycleArray.get(selCycle).getInspStart() > time
						&& cycleArray.get(selCycle - 1).getExpEnd() < time) { // prevent infinite loop when breaths are
																				// not following each other
					done = true;
				} else {
					if (time < cycleArray.get(selCycle).getInspStart()) {
						if (selCycle > 0) {
							selCycle--;
						} else {
							done = true;
						}
					} else if (time > cycleArray.get(selCycle).getInspStart()) {
						if (selCycle < cycleArray.size() - 1) {
							selCycle++;
						} else {
							done = true;
						}
					} else {
						done = true;
					}
				}
			}
		}
		if (infoPanel != null)
			infoPanel.updateText();
		if (center) {
			double expTime = cycleArray.get(selCycle).getExpStart();
			graph.getxAxis().moveToTimeFast(expTime);
		} else {
			graph.repaint();
		}
	}

	public void stepNext() {
		if (selCycle < cycleArray.size() - 1)
			selCycle++;
		double expTime = cycleArray.get(selCycle).getExpStart();
		if (infoPanel != null)
			infoPanel.updateText();
		graph.getxAxis().moveToTimeFast(expTime);
	}

	public void stepPrev() {
		if (selCycle > 0)
			selCycle--;
		double expTime = cycleArray.get(selCycle).getExpStart();
		if (infoPanel != null)
			infoPanel.updateText();
		graph.getxAxis().moveToTimeFast(expTime);
	}

}
