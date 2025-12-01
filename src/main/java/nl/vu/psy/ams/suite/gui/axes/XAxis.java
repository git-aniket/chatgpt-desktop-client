package nl.vu.psy.ams.suite.gui.axes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.MouseInputListener;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.graphs.overlays.SelectBarOverlay;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Jpanel that draws and handles mouse input for an X-Axis.
 * Can also be used to get the left time and the right time of
 * the connected graph.
 */
public class XAxis extends JPanel implements MouseInputListener, ActionListener, MouseWheelListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<Graph> graphs = new ArrayList<Graph>();
	private double leftTime, rightTime;
	private double prevTime;
	private double timeFormatFactor;
	private int dragType = -1;
	private DecimalFormat decFormat = new DecimalFormat("#####.###", new DecimalFormatSymbols(Locale.of("us")));
	private DecimalFormat intFormat = new DecimalFormat("########", new DecimalFormatSymbols(Locale.of("us")));
	private JPopupMenu popupMenu = new JPopupMenu();
	private JRadioButtonMenuItem timeFormatDate = new JRadioButtonMenuItem("Show time in hh:mm:ss", true);
	private JRadioButtonMenuItem timeFormatSeconds = new JRadioButtonMenuItem("Show time in seconds", false);
	private JRadioButtonMenuItem timeFormatTicks = new JRadioButtonMenuItem("Show time in ticks", false);
	private NumberFormat nf = NumberFormat.getInstance();
	private JPanel panel = new JPanel(new BorderLayout());
	private boolean isSmall, isEditable;
	private List<SelectBarOverlay> sBars = new ArrayList<SelectBarOverlay>();
	// private double[] animationTable = Utils.getAnimationTable();
	private boolean isAnimating;
	private boolean popupEnabled = true;
	private boolean drawBlocks = true;
	private String axisTitle = null;
	private ArrayList<Integer> XMajorTicks = new ArrayList<Integer>();
	private double minTimeWidth = 1000000.0;
	private boolean trueSeconds = true, isLive = false, isTicks = false;
	private int dataStartEnd = 1;

	public XAxis() {
		this(false, true, 1);
	}

	public ArrayList<Integer> getXMajorTicks() {
		return this.XMajorTicks;
	}

	public XAxis(boolean isSmall, boolean isEditable, int dataStartEnd) {
		leftTime = CurrentOpenData.getInstance().getStartTimeInUS();
		rightTime = CurrentOpenData.getInstance().getEndTimeInUS();
		this.addMouseListener(this);
		this.addMouseWheelListener(this);
		if (isEditable == true) {
			this.addMouseMotionListener(this);
		}
		setBackground(new Color(0.9f, 0.9f, 0.9f));
		setBorder(BorderFactory.createLineBorder(Color.BLACK));

		timeFormatDate.addActionListener(this);
		timeFormatSeconds.addActionListener(this);
		timeFormatTicks.addActionListener(this);
		ButtonGroup group = new ButtonGroup();
		group.add(timeFormatDate);
		group.add(timeFormatSeconds);
		group.add(timeFormatTicks);
		popupMenu.add(timeFormatDate);
		popupMenu.add(timeFormatSeconds);
		popupMenu.add(timeFormatTicks);
		nf.setMinimumIntegerDigits(2);
		if (timeFormatSeconds.isSelected()) {
			timeFormatFactor = 100.0;
		} else {
			timeFormatFactor = 500.0;
		}

		panel.add(this, BorderLayout.CENTER);
		JPanel filler = new JPanel();
		filler.setMinimumSize(new Dimension(50, 1));
		filler.setPreferredSize(new Dimension(50, 50));
		filler.setMaximumSize(new Dimension(50, 50));
		filler.setBackground((new Color(0.9f, 0.9f, 0.9f)));
		filler.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		panel.add(filler, BorderLayout.WEST);

		if (isSmall == false) {
			this.setMinimumSize(new Dimension(1, 50));
			this.setMaximumSize(new Dimension(100000, 50));
			this.setPreferredSize(new Dimension(1, 50));
			panel.setMinimumSize(new Dimension(1, 50));
			panel.setMaximumSize(new Dimension(100000, 50));
			panel.setPreferredSize(new Dimension(1, 50));
		} else {
			this.setMinimumSize(new Dimension(1, 25));
			this.setMaximumSize(new Dimension(100000, 25));
			this.setPreferredSize(new Dimension(1, 25));
			panel.setMinimumSize(new Dimension(1, 25));
			panel.setMaximumSize(new Dimension(100000, 25));
			panel.setPreferredSize(new Dimension(1, 25));
		}

		this.isSmall = isSmall;
		this.isEditable = isEditable;
		this.dataStartEnd = dataStartEnd;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource() == timeFormatDate) {
			timeFormatFactor = 500.0;
			displayAllGraphs();
			repaint();
		}
		if (arg0.getSource() == timeFormatSeconds || arg0.getSource() == timeFormatTicks) {
			timeFormatFactor = 100.0;
			displayAllGraphs();
			repaint();
		}
	}

	public synchronized void autoscaleConnectedGraphs() {
		for (Graph g : graphs) {
			g.autoScale();
		}
	}

	public synchronized void defaultscaleConnectedGraphs() {
		for (Graph g : graphs) {
			g.defaultScale();
		}
	}

	public synchronized void autoscaleConnectedGraphs(int d) {
		for (Graph g : graphs) {
			g.autoScale(d);
		}
	}

	public synchronized void autoscaleConnectedGraphs(boolean forceDebugMinMax) {
		for (Graph g : graphs) {
			g.autoScale(forceDebugMinMax);
		}
	}

	public synchronized void autoscaleConnectedGraphsFast() {
		for (Graph g : graphs) {
			g.autoScaleFast();
		}
	}

	public synchronized void connectToGraph(Graph graph) {
		graphs.add(graph);
	}

	public void displayAllGraphs() {
		for (Graph g : graphs)
			g.repaint();
	}

	// private void doAnimation(double newLTime, double newRTime) {
	// final double lTime = getLeftTime();
	// final double rTime = getRightTime();
	// final double lWidth = newLTime - lTime;
	// final double rWidth = newRTime - rTime;
	// if (lWidth == 0 && rWidth == 0) {
	// setTimes(newLTime, newRTime);
	// updateAll(newLTime, newRTime);
	// return;
	// }
	// setAnimating(true);
	// // Runnable test = new Runnable() {
	// // @Override
	// // public void run() {
	// for (int i = 0; i < 25; i++) {
	// setTimes(lTime + animationTable[i] * lWidth, rTime + animationTable[i] *
	// rWidth);
	// updateAll(newLTime, newRTime);
	// try {
	// Thread.sleep(10);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// }
	// setAnimating(false);
	// // }
	// // };
	// // CachedThreadPool.execute(test);
	// }

	private void drawDateTicks(Graphics2D g) {

		double expon = Math.round(Math.log10(getTimeWidth()));
		double tickSize = Math.pow(10, expon);
		int lSnap = (int) Math.ceil(getLeftTime() / tickSize);
		int rSnap = (int) Math.floor(getRightTime() / tickSize);
		int nTicks = rSnap - lSnap;

		if (nTicks < 2) {
			expon--;
			tickSize = Math.pow(10, expon);
			lSnap = (int) Math.ceil(getLeftTime() / tickSize);
			rSnap = (int) Math.floor(getRightTime() / tickSize);
			nTicks = rSnap - lSnap;
		}
		// to make ticks on round secs/minutes
		if (tickSize > 10000000) { // 10 s
			if (nTicks > 16) {
				tickSize = tickSize * 1.8;
				lSnap = (int) Math.ceil(getLeftTime() / tickSize);
				rSnap = (int) Math.floor(getRightTime() / tickSize);
				nTicks = rSnap - lSnap;
			} else {
				tickSize = tickSize * 0.9;
				lSnap = (int) Math.ceil(getLeftTime() / tickSize);
				rSnap = (int) Math.floor(getRightTime() / tickSize);
				nTicks = rSnap - lSnap;
			}
		}
		double dtickSizeInS = tickSize / 1000000.0;

		double xDraw;
		int w = getWidth();
		int h = getHeight();
		int xPos;
		FontMetrics metrics = g.getFontMetrics();
		double curTime;
		double txtHeight, txtWidth;
		XMajorTicks.clear();
		for (int i = 0; i <= nTicks; i++) {
			curTime = (lSnap + i) * tickSize;
			xDraw = Utils.getRelDrawCoordinate(curTime, getLeftTime(), getRightTime());
			xPos = (int) Math.round(xDraw * w);
			XMajorTicks.add(xPos);
			g.drawLine(xPos, 0, xPos, h / 8);

			GregorianCalendar date = Utils.getCalendarFromUS(curTime);
			String tickText = nf.format(date.get(Calendar.HOUR_OF_DAY)) + ":" + nf.format(date.get(Calendar.MINUTE));
			nf.setMinimumIntegerDigits(2);
			if (dtickSizeInS < 0.0015) {
				tickText += ":" + nf.format(date.get(Calendar.SECOND));
				nf.setMinimumIntegerDigits(3);
				tickText += "." + nf.format(date.get(Calendar.MILLISECOND));
				nf.setMinimumIntegerDigits(2);
			} else if (dtickSizeInS < 0.1) {
				tickText += ":" + nf.format(date.get(Calendar.SECOND));
				tickText += "." + nf.format(date.get(Calendar.MILLISECOND) / 10);
			} else if (dtickSizeInS < 1.0) {
				tickText += ":" + nf.format(date.get(Calendar.SECOND));
				nf.setMinimumIntegerDigits(1);
				tickText += "." + nf.format(date.get(Calendar.MILLISECOND) / 100);
				nf.setMinimumIntegerDigits(2);
			} else if (dtickSizeInS < 21600.0) {
				tickText += ":" + nf.format(date.get(Calendar.SECOND));
			}

			txtWidth = metrics.stringWidth(tickText);
			txtHeight = metrics.getMaxAscent();

			g.drawString(tickText, Math.round(xPos - txtWidth / 2.), Math.round(h / 8 + txtHeight));
		}

		if (isSmall == false) {
			String axisTitle = this.axisTitle;
			if (axisTitle == null) {
				axisTitle = "Time";
				GregorianCalendar date = Utils.getCalendarFromUS(getMidPoint());
				axisTitle += " (Date: " + nf.format(date.get(Calendar.DAY_OF_MONTH)) + "-"
						+ nf.format(date.get(Calendar.MONTH) + 1) + "-"
						+ date.get(Calendar.YEAR) + ")";
			}
			txtWidth = metrics.stringWidth(axisTitle);
			g.drawString(axisTitle, Math.round(getWidth() / 2 - txtWidth / 2.), Math.round(3 * getHeight() / 4));

		}
	}

	private void DrawResizeBlocks(Graphics2D g) {
		g.setColor(new Color(0.8f, 0.8f, 0.8f));
		g.fillRect(0, 0, getWidth() / 4, getHeight());
		g.fillRect(3 * getWidth() / 4, 0, getWidth(), getHeight());
	}

	private void drawSecondTicks(Graphics2D g) {

		double expon = Math.floor(Math.log10(getTimeWidth()));
		double tickSize = Math.pow(10, expon);
		int lSnap = (int) Math.ceil(getLeftTime() / tickSize);
		int rSnap = (int) Math.floor(getRightTime() / tickSize);
		int nTicks = rSnap - lSnap;

		if (nTicks < 2) {
			expon--;
			tickSize = Math.pow(10, expon);
			lSnap = (int) Math.ceil(getLeftTime() / tickSize);
			rSnap = (int) Math.floor(getRightTime() / tickSize);
			nTicks = rSnap - lSnap;
		}
		double xDraw;
		int w = getWidth();
		int h = getHeight();
		int xPos;
		FontMetrics metrics = g.getFontMetrics();
		double curTime;
		String tickText;
		double txtHeight, txtWidth;
		XMajorTicks.clear();
		for (int i = 0; i <= nTicks; i++) {
			curTime = (lSnap + i) * tickSize;
			xDraw = Utils.getRelDrawCoordinate(curTime, getLeftTime(), getRightTime());
			xPos = (int) Math.round(xDraw * w);
			XMajorTicks.add(xPos);
			g.drawLine(xPos, 0, xPos, h / 8);

			if (isTicks) {
				tickText = intFormat.format(curTime / 1000);
			} else {
				if (isLive) {
					if (trueSeconds)
						tickText = decFormat.format(curTime / 1000000);
					else
						tickText = decFormat.format(curTime / 1000);
				} else {
					if (trueSeconds)
						tickText = decFormat
								.format((curTime - CurrentOpenData.getInstance().getStartTimeInUS()) / 1000000);
					else
						tickText = decFormat
								.format((curTime - CurrentOpenData.getInstance().getStartTimeInUS()) / 1000);
				}
			}
			txtWidth = metrics.stringWidth(tickText);
			txtHeight = metrics.getMaxAscent();

			g.drawString(tickText, Math.round(xPos - txtWidth / 2.), Math.round(h / 8 + txtHeight));
		}

		if (isSmall == false) {
			String axisTitle = this.axisTitle;
			if (axisTitle == null) {
				axisTitle = "Time";
				if (isTicks) {
					axisTitle += " (ticks)";
				} else {
					if (trueSeconds)
						axisTitle += " (seconds)";
					else
						axisTitle += " (milliseconds)";
				}
			}
			txtWidth = metrics.stringWidth(axisTitle);
			txtHeight = metrics.getMaxAscent();
			g.drawString(axisTitle, Math.round(getWidth() / 2 - txtWidth / 2), Math.round(3 * getHeight() / 4));

		}

	}

	public void setTrueSeconds(boolean secs) {
		trueSeconds = secs;
	}

	public boolean getTrueSeconds() {
		return trueSeconds;
	}

	public void setIsLive(boolean live) {
		isLive = live;
	}

	public boolean getIsLive() {
		return isLive;
	}

	public boolean getIsTicks() {
		return isTicks;
	}

	public String getAxisTitle() {
		return axisTitle;
	}

	public JPanel getDoubleFillerPanel() {
		JPanel filler = new JPanel();
		filler.setMinimumSize(new Dimension(50, 1));
		filler.setPreferredSize(new Dimension(50, 50));
		filler.setMaximumSize(new Dimension(50, 50));
		filler.setBackground((new Color(0.9f, 0.9f, 0.9f)));
		filler.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		panel.add(filler, BorderLayout.EAST);
		return panel;
	}

	public synchronized double getLeftTime() {
		return leftTime;
	}

	public double getMidPoint() {
		return (getLeftTime() + getRightTime()) / 2;
	}

	public JPanel getPanel() {
		return panel;
	}

	public int getPixelFromTime(double time) {
		double relPos;
		relPos = (time - getLeftTime()) / getTimeWidth();
		if (this.getWidth() == 0)
			return (int) Math.round(relPos * 1500);
		else
			return (int) Math.round(relPos * this.getWidth());
	}

	public synchronized double getRightTime() {
		return rightTime;
	}

	public JRadioButtonMenuItem getTimeFormatSeconds() {
		return timeFormatSeconds;
	}

	public double getTimeFromPixel(int xPos) {
		double relPos;
		if (this.getWidth() == 0)
			relPos = (double) xPos / 1500;
		else
			relPos = (double) xPos / this.getWidth();
		return getLeftTime() + relPos * getTimeWidth();
	}

	public double getTimeWidth() {
		return getRightTime() - getLeftTime();
	}

	private void maybeShowPopup(MouseEvent e) {
		if (popupEnabled == false)
			return;
		if (e.isPopupTrigger()) {
			popupMenu.show(this, e.getX(), e.getY());
		}
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		if ((arg0.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == InputEvent.BUTTON1_DOWN_MASK) {
			double newTime = getTimeFromPixel(arg0.getX());
			double diffTime = newTime - prevTime;
			double rScaleFactor = (getTimeWidth() - diffTime) / getTimeWidth();
			double lScaleFactor = (getTimeWidth() + diffTime) / getTimeWidth();

			if (dragType == 0) {
				setTimes(getLeftTime() - diffTime, getRightTime() - diffTime);
			} else if (dragType == 1) {
				if (minTimeWidth >= getTimeWidth() * lScaleFactor * timeFormatFactor) {
					setLeftTime(getLeftTime());
				} else {
					setLeftTime(getLeftTime() - diffTime);
				}
			} else if (dragType == 2) {
				if (minTimeWidth >= getTimeWidth() * rScaleFactor * timeFormatFactor) {
					setRightTime(getRightTime());
				} else {
					setRightTime(getRightTime() - diffTime);
				}
			}
			prevTime = getTimeFromPixel(arg0.getX());
			updateAll();
		}
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		if (arg0.getX() <= 0.25 * getWidth()) {
			setCursor(Utils.getCursor(Cursor.W_RESIZE_CURSOR));
		} else if (arg0.getX() >= 0.75 * getWidth()) {
			setCursor(Utils.getCursor(Cursor.E_RESIZE_CURSOR));
		} else {
			setCursor(Utils.getCursor(Cursor.HAND_CURSOR));
		}
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		if (arg0.getButton() == MouseEvent.BUTTON1) {
			prevTime = getTimeFromPixel(arg0.getX());
			if (arg0.getX() <= 0.25 * getWidth()) {
				dragType = 1;
			} else if (arg0.getX() >= 0.75 * getWidth()) {
				dragType = 2;
			} else {
				dragType = 0;
			}
		} else {
			maybeShowPopup(arg0);
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		if (arg0.getButton() != MouseEvent.BUTTON1) {
			maybeShowPopup(arg0);
		}
		/*
		 * For autoscale after mouse release if(dragType>=0){ dragType=-1; for
		 * (Graph g : graphs) g.autoScale(); }
		 */
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0) {
		int wheelRotation = arg0.getWheelRotation();
		double time = getTimeFromPixel(arg0.getX());
		if (wheelRotation < 0) {
			moveAndZoomToTime(time, 2);
		} else if (wheelRotation > 0) {
			moveAndZoomToTime(time, 0.5);
		}
		// If wheel rotation value is a negative it means rotate up, while
		// positive value means rotate down
		// if (arg0.getWheelRotation() < 0) {
		// System.out.println("modEx: " + arg0.getModifiersEx() + " val:" +
		// arg0.getPreciseWheelRotation() );
		// System.out.println("Rotated Up... " + arg0.getWheelRotation());
		// } else if (arg0.getWheelRotation() > 0) {
		// System.out.println("modEx: " + arg0.getModifiersEx() + " val:" +
		// arg0.getPreciseWheelRotation() );
		// System.out.println("Rotated Down... " + arg0.getWheelRotation());
		// }
	}

	public void move(double screenFactor) {
		if (!isEditable || isAnimating)
			return;
		double lTime = getLeftTime();
		double rTime = getRightTime();
		double width = getTimeWidth();
		// doAnimation(lTime + screenFactor * width, rTime + screenFactor * width);
		setTimes(lTime + screenFactor * width, rTime + screenFactor * width);
		updateAll();
	}

	public void moveAndZoomToTime(double time, double factor) {
		if (!isEditable || isAnimating)
			return;
		double width = getTimeWidth() / factor;

		if (minTimeWidth >= width * timeFormatFactor) {
			width = getTimeWidth();
		}
		/*
		 * double relTime = (time - leftTime) / (getTimeWidth());
		 * doAnimation(time - relTime * width, time + (1 - relTime) * width);
		 */
		// doAnimation(time - width / 2.0, time + width / 2.0);
		setTimes(time - width / 2.0, time + width / 2.0);
		updateAll();
		// System.out.println("lefttime: " + (time - width / 2.0) + " righttime: " +
		// (time + width / 2.0));
	}

	public void moveToTime(double time) {
		if (!isEditable || isAnimating)
			return;
		// double width = getTimeWidth();
		// doAnimation(time - width / 2.0, time + width / 2.0);
		moveToTimeFast(time);
	}

	public void moveToTimeFast(double time) {
		if (!isEditable || isAnimating)
			return;
		double width = getTimeWidth();
		setTimes(time - width / 2.0, time + width / 2.0);
		updateAll();
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		if (isEditable && drawBlocks)
			DrawResizeBlocks(g2);
		g2.setColor(Color.BLACK);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
		if (timeFormatSeconds.isSelected()) {
			isTicks = false;
			drawSecondTicks(g2);
		} else if (timeFormatDate.isSelected()) {
			isTicks = false;
			drawDateTicks(g2);
		} else { // timeFormatTicks
			isTicks = true;
			drawSecondTicks(g2);
		}

		displayAllGraphs();
	}

	public synchronized void removeGraph(Graph graph) {
		graphs.remove(graph);
	}

	public void repaintAll() {
		displayAllGraphs();
		for (SelectBarOverlay sBar : sBars) {
			sBar.getGraph().getxAxis().repaintAll();
		}
		repaint();
	}

	public void setAnimating(boolean isAni) {
		isAnimating = isAni;
	}

	public void setAxisTitle(String title) {
		this.axisTitle = title;
	}

	public void setDrawBlocks(boolean draw) {
		drawBlocks = draw;
	}

	public synchronized void setLeftTime(double leftTime) {
		if (leftTime < CurrentOpenData.getInstance().getStartTimeInUS() && dataStartEnd == 1)
			leftTime = CurrentOpenData.getInstance().getStartTimeInUS();
		if (leftTime < -256000 && dataStartEnd == 0) // ICG tab
			leftTime = -256000;
		double width = getRightTime() - leftTime;
		if (minTimeWidth >= width * timeFormatFactor)
			return;
		this.leftTime = leftTime;
	}

	public synchronized void setLeftTimeF(double leftTime) {
		this.leftTime = leftTime;
	}

	public void setPopupEnabled(boolean popupEnabled) {
		this.popupEnabled = popupEnabled;
	}

	public synchronized void setRightTime(double rightTime) {
		if (rightTime > CurrentOpenData.getInstance().getEndTimeInUS() && dataStartEnd == 1)
			rightTime = CurrentOpenData.getInstance().getEndTimeInUS();
		if (rightTime > 750000 && dataStartEnd == 0) // ICG tab
			rightTime = 750000;
		double width = rightTime - getLeftTime();
		if (minTimeWidth >= width * timeFormatFactor)
			return;
		this.rightTime = rightTime;
	}

	public synchronized void setRightTimeF(double rightTime) {
		this.rightTime = rightTime;
	}

	public synchronized void setTimes(double leftTime, double rightTime) {
		if (leftTime < CurrentOpenData.getInstance().getStartTimeInUS() && dataStartEnd == 1) {
			double diff = leftTime - CurrentOpenData.getInstance().getStartTimeInUS();
			leftTime -= diff;
			rightTime -= diff;
		}
		if (leftTime < -256000 && dataStartEnd == 0) // ICG tab
			leftTime = -256000;
		if (rightTime > CurrentOpenData.getInstance().getEndTimeInUS() && dataStartEnd == 1) {
			double diff = rightTime - CurrentOpenData.getInstance().getEndTimeInUS();
			leftTime -= diff;
			rightTime -= diff;
		}
		if (rightTime > 750000 && dataStartEnd == 0) // ICG tab
			rightTime = 750000;
		double width = rightTime - leftTime;
		if (minTimeWidth >= width * timeFormatFactor)
			return;
		this.leftTime = leftTime;
		this.rightTime = rightTime;
	}

	public void setSelectBarOverlay(SelectBarOverlay sBar) {
		this.sBars.add(sBar);
	}

	public void updateAll() {
		displayAllGraphs();
		for (SelectBarOverlay sBar : sBars) {
			// sBar.setLeftTime(leftTime);
			// sBar.setRightTime(rightTime);
			sBar.setPosition(leftTime, rightTime);
			sBar.getGraph().getxAxis().repaintAll();
			sBar.getGraph().repaint();
		}
		repaint();
	}

	public void updateAll(double lTime, double rTime) {
		displayAllGraphs();
		for (SelectBarOverlay sBar : sBars) {
			sBar.setPosition(lTime, rTime);
			sBar.getGraph().getxAxis().repaintAll();
			sBar.getGraph().repaint();
		}
		repaint();
	}

	public void zoom(double factor) {
		if (!isEditable || isAnimating)
			return;
		double midPoint = getMidPoint();
		double width = getTimeWidth();
		if (minTimeWidth >= (width / factor) * timeFormatFactor) {
			return;
		} else {
			// doAnimation(midPoint - width / (2 * factor), midPoint + width / (2 *
			// factor));
			setTimes(midPoint - width / (2 * factor), midPoint + width / (2 * factor));
			updateAll();
		}
	}

}
