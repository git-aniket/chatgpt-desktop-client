package nl.vu.psy.ams.suite.gui.drawing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Path2D;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.sets.BeatSet;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Draws LF and HF signals, using 1 value for every
 * 10 seconds, and a window of 4 minutes (2 min before, 2 min
 * after the value).
 */
public class LFHFDrawer extends DataDrawer {

	private double[] lf;

	private double[] hf;
	private int nSteps;
	private long stepWidth = 10000000L;
	// private long freqWidth = 300000000L;
	private long freqWidth = 240000000L; // 4 minutes
	private double lTime;
	private boolean drawRatio = false;
	private boolean isCalculating;
	private Thread thread;
	private boolean drawLF = false, drawHF = true;

	private Color lfColor = Color.BLUE;
	private Color hfColor = new Color(0, 128, 0);

	private boolean drawLegend = true;

	public LFHFDrawer(String name, YAxis yAxis) {
		super(name, yAxis);
		int val;
		val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.DRAWLFSIGNAL);
		if (val != 0) {
			drawLF = true;
		} else {
			drawLF = false;
		}
		val = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.DRAWHFSIGNAL);
		if (val != 0) {
			drawHF = true;
		} else {
			drawHF = false;
		}
	}

	@Override
	public void drawData(Graphics2D g) {

		if (drawRatio) {
			drawRatio(g);
		} else {
			drawSignals(g);
			drawLegend(g);
		}
		if (getCalculating() == true) {
			g.setColor(Color.BLACK);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			String calcText = "Calculating...";
			g.setFont(new Font("Arial", Font.BOLD, 20));
			FontMetrics met = g.getFontMetrics();
			int strWidth = met.stringWidth(calcText);
			int strHeight = met.getMaxAscent();
			g.drawString(calcText, (float) (graph.getWidth() / 2. - strWidth / 2.),
					(float) (graph.getHeight() / 2. + strHeight / 2.));
			return;
		}
	}

	private void drawLegend(Graphics2D g) {

		if (drawLegend == false)
			return;

		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		FontMetrics fm = g.getFontMetrics();

		int txtWidth = fm.stringWidth("HF");
		int txtH = fm.getMaxAscent();

		if (drawHF && drawLF) {
			g.setColor(Color.WHITE);
			g.fillRect(20, 20, 60 + txtWidth, 40);

			Stroke oldStroke = g.getStroke();
			g.setStroke(new BasicStroke(2));
			g.setColor(lfColor);
			g.drawLine(30, 30, 60, 30);
			g.setColor(hfColor);
			g.drawLine(30, 50, 60, 50);
			g.setStroke(oldStroke);

			g.setColor(Color.BLACK);
			g.drawRect(20, 20, 60 + txtWidth, 40);

			g.drawString("LF", 70, 30 + txtH / 2);
			g.drawString("HF", 70, 50 + txtH / 2);
		} else if (drawHF || drawLF) {
			g.setColor(Color.WHITE);
			g.fillRect(20, 20, 60 + txtWidth, 20);
			g.setColor(Color.BLACK);
			g.drawRect(20, 20, 60 + txtWidth, 20);

			if (drawLF) {
				g.setColor(lfColor);
				Stroke oldStroke = g.getStroke();
				g.setStroke(new BasicStroke(2));
				g.drawLine(30, 30, 60, 30);
				g.setStroke(oldStroke);
				g.setColor(Color.BLACK);
				g.drawString("LF", 70, 30 + txtH / 2);
			} else {
				Stroke oldStroke = g.getStroke();
				g.setStroke(new BasicStroke(2));
				g.setColor(hfColor);
				g.drawLine(30, 30, 60, 30);
				g.setStroke(oldStroke);
				g.setColor(Color.BLACK);
				g.drawString("HF", 70, 30 + txtH / 2);
			}
		}

	}

	private void drawRatio(Graphics2D g) {
		double leftTime = graph.getxAxis().getLeftTime();
		double rightTime = graph.getxAxis().getRightTime();

		double bVal = yAxis.getBottomValue();
		double tVal = yAxis.getTopValue();

		int w = graph.getWidth();
		int h = graph.getHeight();

		if (nSteps < 2)
			return;
		g.setColor(Color.BLACK);
		Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD, nSteps);
		int nPointsInPath = 0;
		int xPos, yPos;
		int[] bnds = getLeftRightIndex();
		for (int i = bnds[0]; i <= bnds[1]; i++) {
			xPos = Utils.getPixelCoordinate(lTime + (double) i * stepWidth + freqWidth / 2., leftTime, rightTime, w);
			if (lf[i] > 0 && hf[i] > 0) {
				yPos = h - Utils.getPixelCoordinate(lf[i] / hf[i], bVal, tVal, h);
				if (nPointsInPath == 0) {
					path.moveTo(xPos, yPos);
				} else {
					path.lineTo(xPos, yPos);
				}
				nPointsInPath++;
			} else {
				if (nPointsInPath > 0) {
					g.draw(path);
					path = new Path2D.Double(Path2D.WIND_EVEN_ODD, nSteps - i);
					nPointsInPath = 0;
				}
			}
		}
		if (nPointsInPath > 0) {
			g.draw(path);
		}
	}

	private synchronized void drawSignals(Graphics2D g) {
		Stroke oldStroke = g.getStroke();
		g.setStroke(new BasicStroke(2));
		double leftTime = graph.getxAxis().getLeftTime();
		double rightTime = graph.getxAxis().getRightTime();

		double bVal = yAxis.getBottomValue();
		double tVal = yAxis.getTopValue();

		int w = graph.getWidth();
		int h = graph.getHeight();

		if (nSteps < 2)
			return;

		Path2D.Double lfPath = new Path2D.Double(Path2D.WIND_EVEN_ODD, nSteps);
		Path2D.Double hfPath = new Path2D.Double(Path2D.WIND_EVEN_ODD, nSteps);
		int nPointsInLFPath = 0;
		int nPointsInHFPath = 0;
		int xPos, yPos;
		int[] bnds = getLeftRightIndex();
		for (int i = bnds[0]; i <= bnds[1]; i++) {
			xPos = Utils.getPixelCoordinate(lTime + (double) i * stepWidth + freqWidth / 2., leftTime, rightTime, w);
			if (drawLF) {
				if (lf[i] > 0) {
					yPos = h - Utils.getPixelCoordinate(lf[i], bVal, tVal, h);
					if (nPointsInLFPath == 0) {
						lfPath.moveTo(xPos, yPos);
					} else {
						lfPath.lineTo(xPos, yPos);
					}
					nPointsInLFPath++;
				} else {
					if (nPointsInLFPath > 0) {
						g.setColor(lfColor);
						g.draw(lfPath);
						lfPath = new Path2D.Double(Path2D.WIND_EVEN_ODD, nSteps - i);
						nPointsInLFPath = 0;
					}
				}
			}
			if (drawHF) {
				if (hf[i] > 0) {
					yPos = h - Utils.getPixelCoordinate(hf[i], bVal, tVal, h);
					if (nPointsInHFPath == 0) {
						hfPath.moveTo(xPos, yPos);
					} else {
						hfPath.lineTo(xPos, yPos);
					}
					nPointsInHFPath++;
				} else {
					if (nPointsInHFPath > 0) {
						g.setColor(hfColor);
						g.draw(hfPath);
						hfPath = new Path2D.Double(Path2D.WIND_EVEN_ODD, nSteps - i);
						nPointsInHFPath = 0;
					}
				}
			}
		}
		if (nPointsInLFPath > 0) {
			g.setColor(lfColor);
			g.draw(lfPath);
		}
		if (nPointsInHFPath > 0) {
			g.setColor(hfColor);
			g.draw(hfPath);
		}
		g.setStroke(oldStroke);
	}

	@Override
	public synchronized double[] getBounds() {
		double[] ret = new double[2];
		ret[0] = Double.POSITIVE_INFINITY;
		ret[1] = Double.NEGATIVE_INFINITY;
		int[] bnds = getLeftRightIndex();
		if (drawRatio) {
			for (int i = bnds[0]; i <= bnds[1]; i++) {
				if (lf[i] > 0 && hf[i] > 0) {
					double val = lf[i] / hf[i];
					if (val < ret[0])
						ret[0] = val;
					if (val > ret[1])
						ret[1] = val;
				}
			}
		} else {
			for (int i = bnds[0]; i <= bnds[1]; i++) {
				if (drawLF) {
					if (lf[i] > 0) {
						if (lf[i] < ret[0])
							ret[0] = lf[i];
						if (lf[i] > ret[1])
							ret[1] = lf[i];
					}
				}
				if (drawHF) {
					if (hf[i] > 0) {
						if (hf[i] < ret[0])
							ret[0] = hf[i];
						if (hf[i] > ret[1])
							ret[1] = hf[i];
					}
				}
			}
		}
		return ret;
	}

	private synchronized boolean getCalculating() {
		return isCalculating;
	}

	private int[] getLeftRightIndex() {
		double leftTime = graph.getxAxis().getLeftTime();
		double rightTime = graph.getxAxis().getRightTime();

		// int lIndex = (int) ((leftTime - lTime) / stepWidth - 0.5);
		int lIndex = (int) ((leftTime - lTime - freqWidth / 2) / stepWidth);
		if (lIndex < 0)
			lIndex = 0;
		int rIndex = (int) Math.ceil(((rightTime - lTime - freqWidth / 2) / stepWidth));
		if (rIndex >= nSteps)
			rIndex = nSteps - 1;

		int[] ret = new int[2];
		ret[0] = lIndex;
		ret[1] = rIndex;
		return ret;
	}

	public synchronized double getLTime() {
		return lTime;
	}

	public void recalculate() {

		if (thread != null) {
			// thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		setCalculating(true);

		thread = new Thread() {

			@Override
			public void run() {
				BeatSet bSet = CurrentOpenData.getInstance().getBeatSet();
				if (bSet.getBeats().size() < 3) {
					nSteps = -1;
					return;
				}
				setLTime(bSet.getBeats().first().getRPeakTime());
				double rTime = bSet.getPrevBeat(bSet.getBeats().last()).getRPeakTime();
				double tWidth = rTime - lTime;
				resetLFHF((int) ((tWidth - freqWidth) / stepWidth)); // This would give no of samples
				for (int i = 0; i < nSteps; i++) { // nSteps = no of samples
					if (isInterrupted())
						return;
					double curTime = getLTime() + i * (double) stepWidth;
					AmsLabel lbl = new AmsLabel(curTime, curTime + freqWidth, false, 0.0, "");
					double[] freq = lbl.getFreqPowers(false, 230); // minute with rounding
					if (freq == null) {
						setLFHF(-1, -1, i);
					} else {
						setLFHF(freq[0], freq[1], i);
					}
					graph.repaint();
				}
				setCalculating(false);
			}

		};

		thread.start();

	}

	public synchronized void resetLFHF(int nSamples) {
		nSteps = nSamples;
		if (nSteps < 0) {
			nSteps = 0;
		}
		lf = new double[nSteps];
		hf = new double[nSteps];
		for (int i = 0; i < nSteps; i++) {
			lf[i] = -1;
			hf[i] = -1;
		}
	}

	private synchronized void setCalculating(boolean calc) {
		isCalculating = calc;
	}

	public void setDrawHF(boolean drawHF) {
		this.drawHF = drawHF;
		graph.repaint();
	}

	public void setDrawLegend(boolean b) {
		this.drawLegend = b;
		graph.repaint();
	}

	public void setDrawLF(boolean drawLF) {
		this.drawLF = drawLF;
		graph.repaint();
	}

	public void setDrawRatio(boolean drawRatio) {
		this.drawRatio = drawRatio;
	}

	private synchronized void setLFHF(double lfVal, double hfVal, int i) {
		lf[i] = lfVal;
		hf[i] = hfVal;
	}

	public synchronized void setLTime(double lTime) {
		this.lTime = lTime;
	}

	public double getAverageBetweenTimes(double lTime, double rTime) {
		return Double.NaN;
	}

	public double getStdDevBetweenTimes(double lTime, double rTime) {
		return Double.NaN;
	}
}
