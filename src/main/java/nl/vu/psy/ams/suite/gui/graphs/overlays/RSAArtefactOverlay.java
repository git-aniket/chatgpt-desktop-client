package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.RespirationCycle;
import nl.vu.psy.ams.suite.data.structures.sets.RespirationSet;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Overlay used show and edit RSA artefacts. There are three types:
 * - Irregular IBI
 * - Clipping DZ
 * - Irregular Respiration Rate 
 */
public class RSAArtefactOverlay extends Overlay {

	public final static int TYPE_IRREGULAR_RR = 0;
	// public final static int TYPE_IRREGULAR_IBI = 1;
	public final static int TYPE_CLIPPING_DZ = 2;

	private RespirationSet rSet = CurrentOpenData.getInstance().getRespSet();
	private double curTime;
	private double prevTime;
	private boolean isDragging = false;
	private boolean isLeftButton;
	private int type;

	public RSAArtefactOverlay(Graph graph, int type) {
		super(graph, true);
		this.type = type;
		if (AppSettings.getInstance().getIntProperty(Settings.SHOWRSATOOLTIP) == 1)
			graph.setToolTipText("Use left mouse button and drag to add. Use right mouse button and drag to delete.");
	}

	@Override
	public void draw(Graphics2D g) {
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		int w = graph.getWidth();
		int h = graph.getHeight();
		// if (type == TYPE_IRREGULAR_IBI) {
		// for (RespirationCycle rc : rSet.getCycles()) {
		// if (rc.isExpEndSet()) {
		// if (rc.isIrregularIbi()) {
		// int lPos = Utils.getPixelCoordinate(rc.getInspStart(), lTime, rTime, w);
		// int rPos = Utils.getPixelCoordinate(rc.getExpEnd(), lTime, rTime, w);
		// if (rPos - lPos > 1) {
		// g.setColor(Color.ORANGE);
		// g.fillRect(lPos, 0, rPos - lPos, h);
		// g.setColor(Color.BLACK);
		// g.drawLine(lPos, 0, lPos, h);
		// g.drawLine(rPos, 0, rPos, h);
		// } else {
		// g.setColor(Color.ORANGE);
		// g.drawLine(lPos, 0, lPos, h);
		// g.drawLine(rPos, 0, rPos, h);
		// }
		// }
		// }
		// }
		if (type == TYPE_IRREGULAR_RR) {
			for (RespirationCycle rc : rSet.getCycles()) {
				if (rc.isExpEndSet()) {
					if (rc.isIrregularRR()) {
						int lPos = Utils.getPixelCoordinate(rc.getInspStart(), lTime, rTime, w);
						int rPos = Utils.getPixelCoordinate(rc.getExpEnd(), lTime, rTime, w);
						if (rPos - lPos > 1) {
							g.setColor(Color.BLUE);
							g.fillRect(lPos, 0, rPos - lPos, h);
							g.setColor(Color.BLACK);
							g.drawLine(lPos, 0, lPos, h);
							g.drawLine(rPos, 0, rPos, h);
						} else {
							g.setColor(Color.BLUE);
							g.drawLine(lPos, 0, lPos, h);
							g.drawLine(rPos, 0, rPos, h);
						}
					}
				}
			}
		} else if (type == TYPE_CLIPPING_DZ) {
			for (RespirationCycle rc : rSet.getCycles()) {
				if (rc.isExpEndSet()) {
					if (rc.isClippingDZ()) {
						int lPos = Utils.getPixelCoordinate(rc.getInspStart(), lTime, rTime, w);
						int rPos = Utils.getPixelCoordinate(rc.getExpEnd(), lTime, rTime, w);
						if (rPos - lPos > 1) {
							g.setColor(Color.PINK);
							g.fillRect(lPos, 0, rPos - lPos, h);
							g.setColor(Color.BLACK);
							g.drawLine(lPos, 0, lPos, h);
							g.drawLine(rPos, 0, rPos, h);
						} else {
							g.setColor(Color.PINK);
							g.drawLine(lPos, 0, lPos, h);
							g.drawLine(rPos, 0, rPos, h);
						}
					}
				}
			}
		}
		if (isDragging == true) {
			int lPos = Utils.getPixelCoordinate(prevTime, lTime, rTime, w);
			int rPos = Utils.getPixelCoordinate(curTime, lTime, rTime, w);
			if (rPos < lPos) {
				int tmp = rPos;
				rPos = lPos;
				lPos = tmp;
			}
			/*
			 * if (type == TYPE_IRREGULAR_IBI) {
			 * g.setColor(Color.RED);
			 * } else
			 */ if (type == TYPE_IRREGULAR_RR) {
				g.setColor(Color.BLUE);
			} else if (type == TYPE_CLIPPING_DZ) {
				g.setColor(Color.PINK);
			}
			if (isLeftButton == false) {
				g.setColor(Color.LIGHT_GRAY);
			}
			g.fillRect(lPos, 0, rPos - lPos, graph.getxAxis().getWidth());
			g.setColor(Color.BLACK);
			g.drawRect(lPos, 0, rPos - lPos, graph.getxAxis().getWidth());
		}

		String title = "";
		/*
		 * if (type == TYPE_IRREGULAR_IBI) {
		 * title = "Irregular IBI";
		 * } else
		 */ if (type == TYPE_IRREGULAR_RR) {
			title = "Irregular Respiration";
		} else if (type == TYPE_CLIPPING_DZ) {
			title = "Clipping DZ";
		}
		g.setColor(Color.BLACK);
		FontMetrics fm = g.getFontMetrics();
		int sW = fm.stringWidth(title);
		int sH = fm.getMaxAscent();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.drawString(title, w / 2f - sW / 2f, h / 2f + sH / 2f);
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		if (((arg0.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == InputEvent.BUTTON1_DOWN_MASK)
				|| (arg0.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) == InputEvent.BUTTON3_DOWN_MASK) {
			curTime = graph.getxAxis().getTimeFromPixel(arg0.getX());
			graph.repaint();
		}
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		if (arg0.getButton() == MouseEvent.BUTTON3 || arg0.isControlDown()) {
			prevTime = graph.getxAxis().getTimeFromPixel(arg0.getX());
			curTime = prevTime;
			isDragging = true;
			isLeftButton = false;
		} else if (arg0.getButton() == MouseEvent.BUTTON1) {
			prevTime = graph.getxAxis().getTimeFromPixel(arg0.getX());
			curTime = prevTime;
			isDragging = true;
			isLeftButton = true;
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		isDragging = false;
		double lTime = prevTime;
		double rTime = curTime;
		if (rTime < lTime) {
			double tmp = rTime;
			rTime = lTime;
			lTime = tmp;
		}
		boolean setTrueOrFalse = isLeftButton;
		ArrayList<RespirationCycle> afterSet = new ArrayList<RespirationCycle>(
				rSet.getCycles().tailSet(new RespirationCycle(lTime), true));
		for (int i = 0; i < afterSet.size(); i++) {
			if (afterSet.get(i).getExpStart() < rTime) {
				// if (type == TYPE_IRREGULAR_IBI) {
				// afterSet.get(i).setIrregularIBI(setTrueOrFalse);
				if (type == TYPE_IRREGULAR_RR) {
					afterSet.get(i).setIrregularRR(setTrueOrFalse);
				} else if (type == TYPE_CLIPPING_DZ) {
					afterSet.get(i).setClippingDZ(setTrueOrFalse);
				}
			}
		}
		graph.getxAxis().updateAll();
		graph.repaint();
	}

}
