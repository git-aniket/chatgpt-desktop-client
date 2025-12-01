package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JPanel;

import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.TimeBar;
import nl.vu.psy.ams.suite.gui.axes.XAxis;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.tabs.AmsTab;
import nl.vu.psy.ams.suite.tools.CachedThreadPool;
import nl.vu.psy.ams.suite.tools.Utils;
/*
 * An overlay that adds a gray bar in the graph,
 * that can be moved and resized, and the area
 * under the bar defines the bounds of a different
 * X-Axis. Used for the TimeBar, and the selections
 * in the Ibi Drawers in the QRS Tab
 */
public class SelectBarOverlay extends Overlay {

	private Vector<XAxis>	connectedAxes	= new Vector<XAxis>();
	private Vector<SelectBarOverlay>	connectedOverlays	= new Vector<SelectBarOverlay>();
	private Vector<AmsTab>	connectedTabs	= new Vector<AmsTab>();
	private double			leftTime, rightTime;
	private double			prevTime;
	private byte			dragType;
	private double[]		animationTable	= Utils.getAnimationTable();
	private Color			borderColor	= new Color(0.5f, 0.5f, 0.5f);

	public SelectBarOverlay(Graph graph) {
		super(graph, true);
		//setPosition(graph.getxAxis().getLeftTime(), graph.getxAxis().getRightTime());
	}

	public SelectBarOverlay(Graph graph, Color borderColor) {
		super(graph, true);
		this.borderColor = borderColor;
		//setPosition(graph.getxAxis().getLeftTime(), graph.getxAxis().getRightTime());
	}

	public void connectToXAxis(XAxis xAxis) {
		connectedAxes.add(xAxis);
		xAxis.setSelectBarOverlay(this);
		if (rightTime > 0 ) {
			xAxis.setTimes(leftTime, rightTime);
		}
		connectedOverlays.add(null);
	}
	
	public void connectToXAxis(XAxis xAxis, SelectBarOverlay sbOverlay) {
		connectedAxes.add(xAxis);
		xAxis.setSelectBarOverlay(this);
		if (rightTime > 0 ) {
			xAxis.setTimes(leftTime, rightTime);
		}
		connectedOverlays.add(sbOverlay);
	}
	
	public void connectToTab(AmsTab tab) {
		connectedTabs.add(tab);
	}

	public void doAnimation(double newLTime, double newRTime) {
		final double lTime = getLeftTime();
		final double rTime = getRightTime();
		final double lWidth = newLTime - lTime;
		final double rWidth = newRTime - rTime;
		Runnable test = new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < 25; i++) {
					setPosition(lTime + animationTable[i] * lWidth,
						rTime + animationTable[i] * rWidth);
					graph.repaint();
					UpdateConnectedAxes();
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		CachedThreadPool.execute(test);
	}

	@Override
	public void draw(Graphics2D g) {
		int leftPix = graph.getxAxis().getPixelFromTime(getLeftTime());
		int rightPix = graph.getxAxis().getPixelFromTime(getRightTime());

		g.setColor(Color.BLACK);
		g.drawLine(leftPix, 0, leftPix, graph.getHeight());
		g.drawLine(rightPix, 0, rightPix, graph.getHeight());

		Composite prevComposite = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		g.setColor(borderColor);
		g.fillRect(leftPix, 0, rightPix - leftPix, graph.getHeight());
		g.setComposite(prevComposite);
	}

	public Graph getGraph() {
		return graph;
	}

	public int getLeftPixelPos() {
		return graph.getxAxis().getPixelFromTime(getLeftTime());
	}

	public double getLeftTime() {
		return leftTime;
	}

	public int getRightPixelPos() {
		return graph.getxAxis().getPixelFromTime(getRightTime());
	}

	public double getRightTime() {
		return rightTime;
	}

	private double getTimeWidth() {
		return getRightTime() - getLeftTime();
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {

	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		double newTime = graph.getxAxis().getTimeFromPixel(arg0.getX());
		if (dragType == 0) {
			double diffTime = newTime - prevTime;
			setPosition(getLeftTime() + diffTime, getRightTime() + diffTime);
		} else if (dragType == 1) {
			if (newTime < getRightTime())
				setLeftTime(newTime);
		} else if (dragType == 2) {
			if (newTime > getLeftTime())
				setRightTime(newTime);
		}
		prevTime = newTime;
		graph.repaint();
		UpdateConnectedAxes();
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		int leftPixPos = getLeftPixelPos();
		int rightPixPos = getRightPixelPos();
		int mousePos = arg0.getX();
		if (mousePos > leftPixPos && mousePos < rightPixPos) {
			graph.setCursor(Utils.getCursor(Cursor.HAND_CURSOR));
		} else if (leftPixPos - mousePos <= 10 && mousePos < rightPixPos) {
			graph.setCursor(Utils.getCursor(Cursor.W_RESIZE_CURSOR));
		} else if (mousePos - rightPixPos <= 10 && mousePos > leftPixPos) {
			graph.setCursor(Utils.getCursor(Cursor.E_RESIZE_CURSOR));
		} else {
			graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		int leftPixPos = getLeftPixelPos();
		int rightPixPos = getRightPixelPos();
		int clickPos = arg0.getX();
		prevTime = graph.getxAxis().getTimeFromPixel(clickPos);
		for (AmsTab tab : connectedTabs)
			tab.selectLabel(prevTime);
		if (clickPos > leftPixPos && clickPos < rightPixPos) {
			dragType = 0;
		} else if (leftPixPos - clickPos <= 10 && clickPos < rightPixPos) {
			dragType = 1;
		} else if (clickPos - rightPixPos <= 10 && clickPos > leftPixPos) {
			dragType = 2;
		} else {
			MoveToPixelPosition(clickPos);
			graph.repaint();
			UpdateConnectedAxes();
			dragType = 0;
			graph.setCursor(Utils.getCursor(Cursor.HAND_CURSOR));
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		dragType = -1;
	}

	public void MoveToPixelPosition(int clickPos) {
		double time = graph.getxAxis().getTimeFromPixel(clickPos);
		double timeWidth = getTimeWidth();
		//doAnimation(time - timeWidth / 2, time + timeWidth / 2);
		setPosition(time - timeWidth / 2, time + timeWidth / 2);
	}

	public void setLeftTime(double leftTime) {
		if (leftTime < graph.getxAxis().getLeftTime())
			leftTime = graph.getxAxis().getLeftTime();
		this.leftTime = leftTime;
		ArrayList<Integer> pixels = new ArrayList<Integer>();
		int i = 0, j = 0;
		for (XAxis axis : connectedAxes) {
			if(connectedOverlays.elementAt(i) != null)
				pixels.add(axis.getPixelFromTime(connectedOverlays.elementAt(i).getLeftTime()));
			axis.setLeftTime(leftTime);
		if (axis.getRightTime() < axis.getLeftTime())
			System.out.println("Oopsl");
			i++;
		}
		i = 0;
		for (SelectBarOverlay sbOverlay : connectedOverlays) {
			if(connectedOverlays.elementAt(i) != null) {
				sbOverlay.setLeftTime(connectedAxes.elementAt(i).getTimeFromPixel(pixels.get(j)));
				sbOverlay.getGraph().getxAxis().repaintAll();
				sbOverlay.getGraph().repaint();
				sbOverlay.UpdateConnectedAxes();
				j++;
			}
			i++;
		}
	}

	public void setPosition(double leftTime, double rightTime) {
		try {
			if (rightTime < leftTime)
				throw new Exception();
		} catch (Exception e) {
			
		}
		if (leftTime < graph.getxAxis().getLeftTime())
			leftTime = graph.getxAxis().getLeftTime();
		if (rightTime > graph.getxAxis().getRightTime())
			rightTime = graph.getxAxis().getRightTime();
		double timeWidth = graph.getxAxis().getTimeWidth();
		if (rightTime - leftTime == timeWidth && graph.getxAxis() != TimeBar.getInstance().getXaxis()) {
			graph.getxAxis().setTimes(leftTime - (timeWidth / 3),rightTime + (timeWidth / 3));
		}
		this.leftTime = leftTime;
		this.rightTime = rightTime;
		ArrayList<Integer> pixelsl = new ArrayList<Integer>();
		ArrayList<Integer> pixelsr = new ArrayList<Integer>();
		int i = 0, j = 0;
		for (XAxis axis : connectedAxes) {
			if(connectedOverlays.elementAt(i) != null) {
				pixelsl.add(axis.getPixelFromTime(connectedOverlays.elementAt(i).getLeftTime()));
				pixelsr.add(axis.getPixelFromTime(connectedOverlays.elementAt(i).getRightTime()));
			}
			axis.setTimes(leftTime, rightTime);
		if (axis.getRightTime() < axis.getLeftTime())
			System.out.println("Oops");
			i++;
		}
		i = 0;
		for (SelectBarOverlay sbOverlay : connectedOverlays) {
			if(connectedOverlays.elementAt(i) != null) {
				int w = connectedAxes.elementAt(i).getWidth();
				if (w == 0)
					w = 1500;
				double margin = w / 150;
				int pixell = pixelsl.get(j);
				int pixelr = pixelsr.get(j);
				int ow = pixelr - pixell;
				if (pixell < margin) {
					pixell = (int) Math.round(margin);
					pixelr = pixell + ow;
				}
				if (pixelr > w - margin) {
					pixelr = (int) Math.round(w - margin);
					pixell = pixelr - ow;
				}
				if (pixell >= pixelr) {
					pixell = (int) Math.round(margin);
					pixelr = (int) Math.round(w - margin);
				}
				sbOverlay.setPosition(connectedAxes.elementAt(i).getTimeFromPixel(pixell),
						connectedAxes.elementAt(i).getTimeFromPixel(pixelr));
				sbOverlay.getGraph().getxAxis().repaintAll();
				sbOverlay.getGraph().repaint();
				sbOverlay.UpdateConnectedAxes();
				j++;
			}
			i++;
		}
	}

	public void setRightTime(double rightTime) {
		if (rightTime > graph.getxAxis().getRightTime())
			rightTime = graph.getxAxis().getRightTime();
		this.rightTime = rightTime;
		ArrayList<Integer> pixels = new ArrayList<Integer>();
		int i = 0, j = 0;
		for (XAxis axis : connectedAxes) {
			if(connectedOverlays.elementAt(i) != null)
				pixels.add(axis.getPixelFromTime(connectedOverlays.elementAt(i).getRightTime()));
			axis.setRightTime(rightTime);
		if (axis.getRightTime() < axis.getLeftTime())
			System.out.println("Oopsr");
			i++;
		}
		i = 0;
		for (SelectBarOverlay sbOverlay : connectedOverlays) {
			if(connectedOverlays.elementAt(i) != null) {
				sbOverlay.setRightTime(connectedAxes.elementAt(i).getTimeFromPixel(pixels.get(j)));
				sbOverlay.getGraph().getxAxis().repaintAll();
				sbOverlay.getGraph().repaint();
				sbOverlay.UpdateConnectedAxes();
				j++;
			}
			i++;
		}
	}

	public void UpdateConnectedAxes() {
		//setLeftTime(leftTime);
		//setRightTime(rightTime);
		for (XAxis axis : connectedAxes) {
			axis.setTimes(leftTime, rightTime);
			JPanel curPanel = (JPanel) MainFrame.getInstance().getTabs().getSelectedComponent();
			if (curPanel.isAncestorOf(axis)) {
				axis.updateAll();
			}
		}
	}

}
