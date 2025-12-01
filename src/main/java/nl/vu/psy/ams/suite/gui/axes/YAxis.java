package nl.vu.psy.ams.suite.gui.axes;

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
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.event.MouseInputListener;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.gui.drawing.DataDrawer;
import nl.vu.psy.ams.suite.tools.CachedThreadPool;
import nl.vu.psy.ams.suite.tools.Utils;
/*
 * Jpanel that draws and handles mouse input for an Y-Axis.
 * Can also be used to get 'real' sample values from a sample value.
 * Sample values are drawn using the raw 16-bit values from the 5fs
 * file, so a linear function is used to convert these values to 'real'
 * values.
 */
public class YAxis extends JPanel implements MouseInputListener, ActionListener {
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	private ArrayList<DataDrawer>			drawers = new ArrayList<DataDrawer>();

	private double				bottomValue, topValue;

	private double				realSlope;

	private double				realConstant;

	private String				axisTitle;

	private double				prevValue;

	private byte				dragType;

	private JPopupMenu			popupMenu;

	private JMenuItem			autoScaleItem, defaultScaleItem, autoScaleFiltItem;

	private boolean				isAnimating;

	private double[]			animationTable		= Utils.getAnimationTable();

	private boolean				flipped				= false;

	private boolean				drawBlocks			= true;
	
	private ArrayList<Integer>	YMajorTicks			= new ArrayList<Integer>();
	
	private double				lowerValue, upperValue;

	private AffineTransform 	at;

	public YAxis(double realSlope, double realConstant, String axisTitle) {
		this.setMinimumSize(new Dimension(50, 1));
		this.setMaximumSize(new Dimension(50, 100000));
		this.setPreferredSize(new Dimension(50, 1));
		this.realSlope = realSlope;
		this.realConstant = realConstant;
		this.axisTitle = axisTitle;
		
		this.lowerValue = -Integer.MIN_VALUE / 131072;
		this.upperValue = Integer.MAX_VALUE / 131072;
		//----------------------------------------------------------------------
		setBackground(new Color(0.9f, 0.9f, 0.9f));
		setBorder(BorderFactory.createLineBorder(Color.BLACK));
		this.addMouseListener(this);
		this.addMouseMotionListener(this);

		popupMenu = new JPopupMenu();
		autoScaleItem = new JMenuItem("Autoscale on Raw");
		autoScaleItem.addActionListener(this);
		popupMenu.add(autoScaleItem);
		autoScaleFiltItem = new JMenuItem("Autoscale on Filtered");
		autoScaleFiltItem.addActionListener(this);
		defaultScaleItem = new JMenuItem("Default scale");
		defaultScaleItem.addActionListener(this);
		popupMenu.add(defaultScaleItem);
		at = AffineTransform.getRotateInstance(-Math.PI / 2);
	}

	public YAxis(double lowerBound, double upperBound, double lowerValue, double upperValue, String axisTitle) {
		this.setMinimumSize(new Dimension(50, 1));
		this.setMaximumSize(new Dimension(50, 100000));
		this.setPreferredSize(new Dimension(50, 1));
		setSlopeAndConstant(lowerBound, upperBound, lowerValue, upperValue);
		this.axisTitle = axisTitle;
		
		// ----- For AMS Files - Files recorded using old VU-AMS Device---------
		if(this.axisTitle.equals("Z0"+ " [" + "Ohm" + "]")){
		if(Utils.getExtension(CurrentOpenData.getInstance().getDataFile()).equals("ams")|| (CurrentOpenData.getInstance().getFileHeader().getsFileID().equals("AMS1"))){			
				lowerBound 		= 0;
				upperBound 		= 1024;
				lowerValue 		= 0.39;
				upperValue 		= 24.25;
				realSlope 		= 0.0233;
				realConstant 	= 0.39;					
			}
		}
		this.lowerValue = lowerValue;
		this.upperValue = upperValue;
		//----------------------------------------------------------------------
		setBackground(new Color(0.9f, 0.9f, 0.9f));
		setBorder(BorderFactory.createLineBorder(Color.BLACK));
		this.addMouseListener(this);
		this.addMouseMotionListener(this);

		popupMenu = new JPopupMenu();
		autoScaleItem = new JMenuItem("Autoscale");
		autoScaleItem.addActionListener(this);
		popupMenu.add(autoScaleItem);
		autoScaleFiltItem = new JMenuItem("Autoscale on Filtered");
		autoScaleFiltItem.addActionListener(this);
		defaultScaleItem = new JMenuItem("Default scale");
		defaultScaleItem.addActionListener(this);
		popupMenu.add(defaultScaleItem);
		at = AffineTransform.getRotateInstance(-Math.PI / 2);
	}
	
	public ArrayList<Integer> getYMajorTicks() {
		return this.YMajorTicks;
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource() == autoScaleItem) {
			autoScale(0);
		}
		if (arg0.getSource() == defaultScaleItem) {
			defaultScale();
		}
		if (arg0.getSource() == autoScaleFiltItem) {
			autoScale(1);
		}
	}

	public void autoScale() {
		autoScale(1);
	}

	public void autoScale(int d) {
		if (d >= drawers.size())
			d = 0;
		double[] bounds = drawers.get(d).getBounds();
		if (bounds == null) {
			defaultScale();
			return;
		}
		if (!Double.isFinite(bounds[0]) || !Double.isFinite(bounds[1])) {
			defaultScale();
			return;
		} else if (bounds[1] > bounds[0]) {
			double diff = bounds[1] - bounds[0];
			setValues(bounds[0] - diff / 4, bounds[1] + diff / 4);
		} else if (bounds[1] == bounds[0]) {
			setValues(bounds[0] - 1, bounds[1] + 1);
		}
	}
	
	public void autoScale(boolean forceDebugMinMax) {
		int d = 1;
		if (d >= drawers.size())
			d = 0;
		double[] bounds = drawers.get(d).getBounds(forceDebugMinMax);
		if (bounds == null) {
			defaultScale();
			return;
		}
		if (!Double.isFinite(bounds[0]) || !Double.isFinite(bounds[1])) {
			defaultScale();
			return;
		} else if (bounds[1] > bounds[0]) {
			double diff = bounds[1] - bounds[0];
			setValues(bounds[0] - diff / 4, bounds[1] + diff / 4);
		} else if (bounds[1] == bounds[0]) {
			setValues(bounds[0] - 1, bounds[1] + 1);
		}
	}
	
	public void autoScaleFast() {
		int d = 1;
		if (d >= drawers.size())
			d = 0;
		double[] bounds = drawers.get(d).getBounds();
		if (bounds == null) {
			defaultScale();
			return;
		}
		if (!Double.isFinite(bounds[0]) || !Double.isFinite(bounds[1])) {
			defaultScale();
			return;
		} else if (bounds[1] > bounds[0]) {
			double diff = bounds[1] - bounds[0];
			setValuesFast(bounds[0] - diff / 4, bounds[1] + diff / 4);
		} else if (bounds[1] == bounds[0]) {
			setValues(bounds[0] - 1, bounds[1] + 1);
		}
	}
	
	public void defaultScale() {
		setValues(getSampleValueFromRealValue(lowerValue), getSampleValueFromRealValue(upperValue));
	}

	public void connectToDrawer(DataDrawer drawer) {
		if (drawers.size() == 0 || !(drawers.get(drawers.size() -1 ).equals(drawer)))
			this.drawers.add(drawer);
		if (drawers.size() > 1) {
			autoScaleFiltItem.addActionListener(this);
			popupMenu.add(autoScaleFiltItem);	
		}
	}

	private void doAnimation(double newBVal, double newTVal) {
		final double bVal = getBottomValue();
		final double tVal = getTopValue();
		final double bWidth = newBVal - bVal;
		final double tWidth = newTVal - tVal;
		if (Double.isNaN(bVal) || Double.isNaN(tVal) || Double.isInfinite(bVal) || Double.isInfinite(tVal)) {
			setBottomValue(newBVal);
			setTopValue(newTVal);
			return;
		}
		setAnimating(true);
		Runnable test = new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < 25; i++) {
					setBottomValue(bVal + animationTable[i] * bWidth);
					setTopValue(tVal + animationTable[i] * tWidth);
					if (drawers.get(0).getGraph() != null)
						drawers.get(0).getGraph().repaint();
					repaint();
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				setAnimating(false);
			}
		};
		CachedThreadPool.execute(test);
	}

	private void DrawResizeBlocks(Graphics2D g) {
		g.setColor(new Color(0.8f, 0.8f, 0.8f));
		g.fillRect(0, 0, getWidth(), getHeight() / 4);
		g.fillRect(0, 3 * getHeight() / 4, getWidth(), getHeight());
	}

	private void DrawTicks(Graphics2D g) {
		YMajorTicks.clear();
		g.setColor(Color.BLACK);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(new Font("SansSerif", Font.PLAIN, 11));
		double expon = Math.floor(Math.log10(getRealValueWidth()));
		double tickSize = Math.pow(10, expon);
		double bottom = getRealBottomValue();
		double top = getRealTopValue();
		int lSnap = (int) Math.ceil(bottom / tickSize);
		int rSnap = (int) Math.floor(top / tickSize);
		int nTicks = rSnap - lSnap;
		int maxTicks = getHeight() / 100;
		if (nTicks < maxTicks) {
			expon--;
			tickSize = Math.pow(10, expon);
			lSnap = (int) Math.ceil(bottom / tickSize);
			rSnap = (int) Math.floor(top / tickSize);
			nTicks = rSnap - lSnap;
		}
		double yDraw;
		int yPos;
		FontMetrics metrics = g.getFontMetrics();
		double tickValue;
		String tickText;
		DecimalFormat numberFormat;
		if (Math.abs(top) >= 0.001 && Math.abs(bottom) <= 1000) {
			numberFormat = new DecimalFormat("####.####", new DecimalFormatSymbols(Locale.of("us")));
		} else {
			numberFormat = new DecimalFormat("0.####E0", new DecimalFormatSymbols(Locale.of("us")));
			//System.out.println(bottom + " " + top);
		}
		double txtWidth, txtHeight;
		for (int i = -1; i <= nTicks + 1; i++) {
			tickValue = (lSnap + i) * tickSize;
			tickText = numberFormat.format(tickValue);
			yDraw = Utils.getRelDrawCoordinate((lSnap + i) * tickSize, bottom, top);
			yPos = (int) Math.round(getHeight() - getHeight() * yDraw);
			YMajorTicks.add(yPos);

			txtWidth = metrics.getStringBounds(tickText, g).getWidth();
			txtHeight = metrics.getStringBounds(tickText, g).getHeight();
			if (flipped) {
				g.drawLine(0, yPos, 1 * getWidth() / 8, yPos);
				g.drawString(tickText, Math.round(3 * getWidth() / 16), Math.round(yPos + (txtHeight / 3.)));
			} else {
				g.drawLine(7 * getWidth() / 8, yPos, getWidth(), yPos);
				g.drawString(tickText, Math.round(7 * getWidth() / 8 - txtWidth), Math.round(yPos + (txtHeight / 3.)));
			}
		}
		g.transform(at);
		txtWidth = metrics.getStringBounds(axisTitle, g).getWidth();
		if (flipped) {
			g.drawString(axisTitle, (int) (-getHeight() / 2 - txtWidth / 2), 7 * getWidth() / 8);
		} else {
			g.drawString(axisTitle, (int) (-getHeight() / 2 - txtWidth / 2), getWidth() / 4);
		}
	}

	public String getAxisTitle() {
		return axisTitle;
	}

	public double getBottomValue() {
		return bottomValue;
	}

	private double getRealBottomValue() {
		return getRealValueFromSampleValue(getBottomValue());
	}

	private double getRealTopValue() {
		return getRealValueFromSampleValue(getTopValue());
	}

	public double getRealValueFromSampleValue(double sampleValue) {
		return realSlope * sampleValue + realConstant;
	}

	private double getRealValueWidth() {
		return getRealTopValue() - getRealBottomValue();
	}

	public double getSampleValueFromRealValue(double realValue) {
		return (realValue - realConstant) / realSlope;
	}

	public double getTopValue() {
		return topValue;
	}

	public double getValueFromPixel(int yPos) {
		double relPos = 1 - (double) yPos / this.getHeight();
		return getBottomValue() + relPos * getValueWidth();
	}

	private double getValueWidth() {
		return getTopValue() - getBottomValue();
	}

	private void maybeShowPopup(MouseEvent e) {
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
			double newValue = getValueFromPixel(arg0.getY());
			double diffValue = newValue - prevValue;
			if (dragType == 0) {
				setBottomValue(getBottomValue() - diffValue);
				setTopValue(getTopValue() - diffValue);
			} else if (dragType == 1) {
				setBottomValue(getBottomValue() - diffValue);
			} else if (dragType == 2) {
				setTopValue(getTopValue() - diffValue);
			}
			prevValue = getValueFromPixel(arg0.getY());
			drawers.get(0).getGraph().repaint();
			repaint();
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
		if (arg0.getY() <= 0.25 * getHeight()) {
			setCursor(Utils.getCursor(Cursor.N_RESIZE_CURSOR));
		} else if (arg0.getY() >= 0.75 * getHeight()) {
			setCursor(Utils.getCursor(Cursor.S_RESIZE_CURSOR));
		} else {
			setCursor(Utils.getCursor(Cursor.HAND_CURSOR));
		}
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		if (arg0.getButton() == MouseEvent.BUTTON1) {
			prevValue = getValueFromPixel(arg0.getY());
			if (arg0.getY() <= 0.25 * getHeight()) {
				dragType = 2;
			} else if (arg0.getY() >= 0.75 * getHeight()) {
				dragType = 1;
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
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		if (drawBlocks)
			DrawResizeBlocks(g2);
		DrawTicks(g2);
	}

	private void setAnimating(boolean b) {
		isAnimating = b;
	}

	public void setAxisTitle(String axisTitle) {
		this.axisTitle = axisTitle;
	}

	public void setBottomValue(double bottomValue) {
		this.bottomValue = bottomValue;
	}

	public void setDrawBlocks(boolean drawBlock) {
		this.drawBlocks = drawBlock;
	}

	public void setDrawer(DataDrawer d) {
		if (drawers.size() == 0 || !(drawers.get(drawers.size() -1 ).equals(d)))
			this.drawers.add(d);
		if (drawers.size() > 1) {
			autoScaleFiltItem.addActionListener(this);
			popupMenu.add(autoScaleFiltItem);	
		}
	}

	public void removeDrawer(DataDrawer d) {
		this.drawers.remove(d);
	}

	public void setFlipped(boolean flip) {
		flipped = flip;
	}

	public void setSlopeAndConstant(double lowerBound, double upperBound, double lowerValue, double upperValue) {
		realSlope = (upperValue - lowerValue) / (upperBound - lowerBound);
		realConstant = lowerValue - realSlope * lowerBound;
	}

	public void setTopValue(double topValue) {
		this.topValue = topValue;
	}

	public void setValues(double bVal, double tVal) {
		if (!isAnimating)
			doAnimation(bVal, tVal);
	}

	private void setValuesFast(double bVal, double tVal) {
		setBottomValue(bVal);
		setTopValue(tVal);
		drawers.get(0).getGraph().repaint();
		repaint();
	}

}
