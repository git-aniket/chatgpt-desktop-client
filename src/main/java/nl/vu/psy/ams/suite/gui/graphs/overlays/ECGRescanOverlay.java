package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.RescanECGDialog;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.tools.Utils;
/*
 * Overlay that allows the user to 'paint' a part of the ECG
 * data to select that part to rescan. Used in the small bar above the ECG
 * screen in QRS tab.
 */
public class ECGRescanOverlay extends Overlay {

	private double				prevTime;
	private double				curTime;
	private boolean				isDragging	= false;
	private static final String	title		= "Click and Drag to Rescan ECG";

	public ECGRescanOverlay(Graph graph) {
		super(graph, true);
	}

	@Override
	public void draw(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		int w = graph.getWidth();
		int h = graph.getHeight();
		if (isDragging == true) {
			double lTime = graph.getxAxis().getLeftTime();
			double rTime = graph.getxAxis().getRightTime();
			int lPos = Utils.getPixelCoordinate(prevTime, lTime, rTime, w);
			int rPos = Utils.getPixelCoordinate(curTime, lTime, rTime, w);
			if (rPos < lPos) {
				int tmp = rPos;
				rPos = lPos;
				lPos = tmp;
			}
			g.setColor(Color.GREEN);
			g.fillRect(lPos, 0, rPos - lPos, graph.getxAxis().getWidth());
			g.setColor(Color.BLACK);
			g.drawRect(lPos, 0, rPos - lPos, graph.getxAxis().getWidth());
		}
		FontMetrics metrics = g.getFontMetrics();
		double txtW = metrics.getStringBounds(title, g).getWidth();
		double txtH = metrics.getStringBounds(title, g).getHeight();
		g.drawString(title, (float) (w / 2f - txtW / 2f), (float) (h / 2f + txtH / 3f));
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		if ((arg0.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == InputEvent.BUTTON1_DOWN_MASK) {
			curTime = graph.getxAxis().getTimeFromPixel(arg0.getX());
			graph.repaint();
		}
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		if (arg0.getButton() == MouseEvent.BUTTON1) {
			prevTime = graph.getxAxis().getTimeFromPixel(arg0.getX());
			curTime = prevTime;
			isDragging = true;
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
		if ((rTime - lTime) < 10000000) {
			JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(), "Please select at least 10 seconds to rescan!", "Rescan too short",
					JOptionPane.ERROR_MESSAGE);
		} else {
			JDialog diag = new RescanECGDialog(lTime, rTime);
			diag.setVisible(true);
			graph.getxAxis().updateAll();
		}
		graph.repaint();
	}

}
