package nl.vu.psy.ams.suite.gui.drawing;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
// import java.io.BufferedWriter;
// import java.io.File;
// import java.io.FileWriter;
// import java.io.IOException;
// import java.io.PrintWriter;
/*import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;*/
import java.util.ArrayList;
// import java.util.Iterator;

import javax.swing.JOptionPane;

// import com.google.gson.Gson;
// import com.google.gson.GsonBuilder;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.ECGBeat;
import nl.vu.psy.ams.suite.data.structures.SignalPart;
import nl.vu.psy.ams.suite.data.structures.sets.SignalPartSet;
import nl.vu.psy.ams.suite.gui.MainFrame;
//import nl.vu.psy.ams.suite.gui.SaveDialog;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.tabs.imp.ImpInfoPanel;
import nl.vu.psy.ams.suite.gui.tabs.imp.ImpTab;
import nl.vu.psy.ams.suite.gui.tabs.label.LabelTab;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.ThreadServer;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Draws a SignalPart, used mostly for ICG scoring.
 */
public class SignalPartDrawer extends DataDrawer {

	private SignalPartSet set;
	public ArrayList<SignalPart> parts;
	ArrayList<SignalPart> improvedParts;
	private int selPart = 0;
	private int selBeat = 0;
	private ArrayList<ECGBeat> curBeats;
	private SignalPart beatPart;
	// public Thread recalcThread;
	private boolean showIndiv = false;
	private boolean showRaw = false;

	private ImpInfoPanel iip;
	private Thread thread = null;
	// private Thread batchThread = null;
	private String chan;

	private boolean invert = false;
	private boolean filtered = true;
	private Color drawColor = Color.BLACK;

	public SignalPartDrawer(String name, YAxis yAxis, String chan) {
		super(name, yAxis);
		this.chan = chan;
		set = new SignalPartSet(chan, true);
		parts = set.getParts();
		improvedParts = set.getImpParts();
		drawColor = new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR));
	}

	public SignalPartDrawer(String name, YAxis yAxis, String chan, boolean filtered) {
		super(name, yAxis);
		this.chan = chan;
		this.filtered = filtered;
		set = new SignalPartSet(chan, filtered);
		if (filtered)
			drawColor = new Color(AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR));
		parts = set.getParts();
		improvedParts = set.getImpParts();
	}

	public void clear() {
		parts.clear();
		improvedParts.clear();
		setSelPart(0);
	}

	public void setChannel(String chan) {
		this.chan = chan;
	}

	@Override
	public void drawData(Graphics2D g) {

		if (selPart < 0 || selPart >= parts.size())
			return;

		SignalPart prt = improvedParts.get(selPart);

		if (prt.getCalculating()) {
			g.setColor(drawColor);
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
		if (prt.getValues() == null) {
			g.setColor(Color.RED);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			String errorText = "No beats detected!";
			g.setFont(new Font("Arial", Font.BOLD, 20));
			FontMetrics met = g.getFontMetrics();
			int strWidth = met.stringWidth(errorText);
			int strHeight = met.getMaxAscent();
			g.drawString(errorText, (float) (graph.getWidth() / 2. - strWidth / 2.),
					(float) (graph.getHeight() / 2. + strHeight / 2.));
			return;
		}

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		double bVal = yAxis.getBottomValue();
		double tVal = yAxis.getTopValue();
		int w = graph.getWidth();
		int h = graph.getHeight();

		g.setColor(Color.GRAY);
		int yPos = Utils.getPixelCoordinate(0, bVal, tVal, h);
		g.drawLine(0, h - yPos, w, h - yPos);

		if (showIndiv) {
			if (curBeats != null && getBeatPart() != null) {
				if (getSelBeat() < 0 || getSelBeat() >= curBeats.size()) {
				} else {
					g.setColor(Color.GRAY);
					drawPart(getBeatPart(), g, w, h, bVal, tVal);
				}
			}
		}

		if (showRaw) {
			g.setColor(Color.RED);
			SignalPart impP = parts.get(selPart);
			if (impP.getCalculating()) {
				g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				String calcText = "Calculating raw average...";
				g.setFont(new Font("Arial", Font.BOLD, 12));
				g.drawString(calcText, 5, (float) (graph.getHeight() - 5));
			} else {
				drawPart(impP, g, w, h, bVal, tVal);
			}
		}

		g.setColor(drawColor);
		// Stroke oldStroke = g.getStroke();
		// g.setStroke(new BasicStroke(2));
		drawPart(prt, g, w, h, bVal, tVal);
		// g.setStroke(oldStroke);

	}

	private void drawPart(SignalPart prt, Graphics2D g, int w, int h, double bVal, double tVal) {
		double[] vals = prt.getValues();
		double lT = graph.getxAxis().getLeftTime();
		double rT = graph.getxAxis().getRightTime();
		if (vals == null) {
			g.drawLine(0, h - Utils.getPixelCoordinate(0, bVal, tVal, h), w,
					h - Utils.getPixelCoordinate(0, bVal, tVal, h));
		} else {
			for (int i = 1; i < vals.length; i++) {
				double time = i * prt.getSampleTimeInUS() - 256000;
				double ltime = (i - 1) * prt.getSampleTimeInUS() - 256000;
				int lPos = Utils.getPixelCoordinate(ltime, lT, rT, w);
				int rPos = Utils.getPixelCoordinate(time, lT, rT, w);
				int lVal = Utils.getPixelCoordinate(vals[i - 1], bVal, tVal, h);
				int rVal = Utils.getPixelCoordinate(vals[i], bVal, tVal, h);
				g.drawLine(lPos, h - lVal, rPos, h - rVal);
			}
		}
	}

	public synchronized void firstBeat() {
		setSelBeat(0);
	}

	public void firstPart() {
		setSelPart(0);
	}

	public void flipLooping() {
		if (thread != null) {
			stopLooping();
		} else {
			startLooping();
		}
	}

	// public void batchLooping() {
	// if (batchThread != null) {
	// stopBatchLoop();
	// } else {
	// startBatchLoop();
	// }
	// }

	public void flipShowHideIndiv() {
		if (showIndiv == false) {
			showIndiv = true;
		} else {
			showIndiv = false;
		}
	}

	public void flipShowHideRaw() {
		if (showRaw == false) {
			showRaw = true;
		} else {
			showRaw = false;
		}
	}

	public synchronized SignalPart getBeatPart() {
		return beatPart;
	}

	@Override
	public double[] getBounds() {
		if (selPart < 0 || selPart >= parts.size())
			return null;

		SignalPart prt = improvedParts.get(selPart);
		double lT = graph.getxAxis().getLeftTime();
		double rT = graph.getxAxis().getRightTime();
		double[] ret = new double[2];
		ret[0] = Double.POSITIVE_INFINITY;
		ret[1] = Double.NEGATIVE_INFINITY;
		if (prt.getCalculating() == false && prt.getValues() != null) {
			double[] vals = prt.getValues();
			for (int i = 0; i < vals.length; i++) {
				double time = i * prt.getSampleTimeInUS() - 256000;
				if (time > lT && time < rT) {
					if (vals[i] < ret[0])
						ret[0] = vals[i];
					if (vals[i] > ret[1])
						ret[1] = vals[i];
				}
			}
		}

		return ret;

	}

	public int getNBeats() {
		if (curBeats == null)
			return 0;
		return curBeats.size();
	}

	public int getNParts() {
		if (parts == null)
			return 0;
		return parts.size();
	}

	public synchronized int getSelBeat() {
		return selBeat;
	}

	public int getSelIndex() {
		return selPart;
	}

	public SignalPart getSelPart() {
		if (improvedParts == null)
			return null;
		if (improvedParts.isEmpty())
			return null;
		if (selPart < 0 || selPart >= improvedParts.size())
			return null;
		return improvedParts.get(selPart);
	}

	public boolean isInvert() {
		return invert;
	}

	public synchronized void lastBeat() {
		if (curBeats == null)
			return;
		setSelBeat(curBeats.size() - 1);
	}

	public void lastPart() {
		setSelPart(parts.size() - 1);
	}

	public synchronized void nextBeat() {
		if (curBeats == null)
			return;
		if (selBeat < curBeats.size() - 1) {
			setSelBeat(selBeat + 1);
		} else {
			setSelBeat(0);
		}
	}

	public void nextPart() {
		if (selPart < parts.size() - 1) {
			setSelPart(selPart + 1);
		}

		if (CurrentOpenData.getInstance().isBatchAnalysis() == true) {
			getSelPart().getLabel().setECGBeenScored(true);
			getSelPart().getLabel().setICGBeenScored(true);
			getSelPart().getLabel().setScoringBeenSaved(true);
		}
	}

	public synchronized void prevBeat() {
		if (selBeat > 0) {
			setSelBeat(selBeat - 1);
		} else {
			setSelBeat(curBeats.size() - 1);
		}
	}

	public void prevPart() {
		if (selPart > 0)
			setSelPart(selPart - 1);
	}

	public void restoreMarkers(String ret) {
		for (SignalPart p : improvedParts) {
			p.restoreScoring(ret);
		}
	}

	public void saveICGScoring(String name) {
		AmsLabel l = getSelPart().getLabel();
		if (l != null) {
			if (l.savedICGNameExists(name)) {
				int res = JOptionPane.showConfirmDialog(MainFrame.getInstance().getMainFrame(),
						"A saved scoring with this name already exists.\nAre you sure you want to use this name?",
						"Existing name", JOptionPane.YES_NO_OPTION);
				if (res == JOptionPane.NO_OPTION)
					return;
			}
			for (SignalPart p : improvedParts) {
				p.saveICGScoring(name);
			}
		}
	}

	public synchronized void setBeatPart(SignalPart beatPart) {
		this.beatPart = beatPart;
	}

	public void setIip(ImpInfoPanel iip) {
		this.iip = iip;
	}

	public void setInvert(boolean invert) {
		this.invert = invert;
	}

	public synchronized void setSelBeat(int bt) {
		selBeat = bt;
		if (curBeats != null) {
			if (curBeats.isEmpty() == false) {
				AmsLabel l = parts.get(selPart).getLabel();
				setBeatPart(new SignalPart(curBeats.get(selBeat).getRPeakTime(), chan, l, filtered));
				if (isInvert()) {
					if (getBeatPart().getValues() != null) {
						for (int i = 0; i < getBeatPart().getValues().length; i++)
							getBeatPart().getValues()[i] *= -1;
					}
				}
			}
		}
	}

	public void setSelPart(int sel) {
		selPart = sel;
		if (parts.isEmpty()) {
			curBeats = null;
		} else {
			AmsLabel l = parts.get(sel).getLabel();
			curBeats = new ArrayList<ECGBeat>(
					CurrentOpenData.getInstance().getBeatSet().subSet(l.getLeftTime(), l.getRightTime()));
			double newlefttime = l.getLeftTime();
			double newrighttime = l.getRightTime();
			// ImpTab.getInstance().getAEXAxis().setTimes(-256000, l.getAverage(false) *
			// 1000.);
			if (ImpTab.getInstance().isActive()) {
				LabelTab.getInstance().getXAxis().setTimes(newlefttime - 18000000, newrighttime + 18000000);
				LabelTab.getInstance().getXAxis().updateAll();
				MainFrame.getInstance().getTabs().repaint();
			}
		}
		setSelBeat(0);
	}

	private void startLooping() {
		thread = new Thread() {
			@Override
			public void run() {
				if (curBeats == null)
					return;
				if (curBeats.size() < 2)
					return;
				while (true) {
					long ms = 1000;
					if (getSelBeat() != curBeats.size() - 1) {
						ms = Math.round((curBeats.get((getSelBeat() + 1)).getRPeakTime()
								- curBeats.get(getSelBeat()).getRPeakTime()) / 1000);
					}
					try {
						Thread.sleep(ms);
					} catch (InterruptedException e) {
						return;
					}
					if (isInterrupted())
						return;
					nextBeat();
					showIndiv = true;
					graph.repaint();
					if (iip != null)
						iip.updateLabelTexts();
					if (isInterrupted())
						return;
				}
			}
		};
		ThreadServer.addNewThread(thread);
		thread.start();
	}

	private void stopLooping() {
		showIndiv = false;
		thread.interrupt();
		try {
			thread.join();
		} catch (InterruptedException e) {
		}
		ThreadServer.removeThread(thread);
		thread = null;
	}

	// private void startBatchLoop() {
	// batchThread = new Thread() {

	// public void run() {
	// if (selPart == (parts.size() - 1))
	// return;
	// while (true) {
	// long ms = 1000;
	// if (getSelIndex() != (parts.size() - 1)) {
	// ms = 300;
	// }
	// try {
	// Thread.sleep(ms);
	// } catch (InterruptedException e) {
	// return;
	// }
	// if (isInterrupted())
	// return;
	// if (getSelPart().getCalculating() == true) {
	// try {
	// Thread.sleep(3000);
	// } catch (InterruptedException e) {
	// return;
	// }
	// }
	// if (selPart == 0) {
	// try {
	// Thread.sleep(5000);
	// } catch (InterruptedException e) {
	// return;
	// }
	// }
	// nextPart();
	// graph.repaint();

	// if (iip != null)
	// iip.updateLabelTexts();
	// if (isInterrupted())
	// return;

	// }
	// }
	// };
	// ThreadServer.addNewThread(batchThread);
	// batchThread.start();
	// }

	// private void stopBatchLoop() {
	// // batchThread.interrupt();
	// try {
	// batchThread.join();
	// } catch (InterruptedException e) {

	// }
	// ThreadServer.removeThread(batchThread);
	// batchThread = null;
	// }

	public double getAverageBetweenTimes(double lTime, double rTime) {
		if (selPart < 0 || selPart >= parts.size())
			return Double.NaN;

		double av = 0;
		long nvals = 0;
		SignalPart prt = improvedParts.get(selPart);
		double lT = graph.getxAxis().getLeftTime();
		double rT = graph.getxAxis().getRightTime();
		if (prt.getCalculating() == false && prt.getValues() != null) {
			double[] vals = prt.getValues();
			for (int i = 0; i < vals.length; i++) {
				double time = i * prt.getSampleTimeInUS() - 256000;
				if (time > lT && time < rT) {
					av += vals[i];
					nvals++;
				}
			}
			return av / nvals;
		}
		return Double.NaN;
	}

	public double getStdDevBetweenTimes(double lTime, double rTime) {
		if (selPart < 0 || selPart >= parts.size())
			return Double.NaN;

		double m = 0;
		double s = 0;
		double tmpM = 0;
		int k = 1;
		double realVal;
		SignalPart prt = improvedParts.get(selPart);
		double lT = graph.getxAxis().getLeftTime();
		double rT = graph.getxAxis().getRightTime();
		double[] ret = new double[2];
		ret[0] = Double.POSITIVE_INFINITY;
		ret[1] = Double.NEGATIVE_INFINITY;
		if (prt.getCalculating() == false && prt.getValues() != null) {
			double[] vals = prt.getValues();
			for (int i = 0; i < vals.length; i++) {
				double time = i * prt.getSampleTimeInUS() - 256000;
				if (time > lT && time < rT) {
					realVal = vals[i];
					tmpM = m;
					m += (realVal - tmpM) / k;
					s += (realVal - tmpM) * (realVal - m);
					k++;
				}
			}
			if (k < 2)
				return -1;
			return Math.sqrt(s / (k - 2));
		}
		return Double.NaN;
	}

	// public void signalPartsToJSON(File file) {
	// PrintWriter writer;
	// Gson gson = new
	// GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
	// try {
	// writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
	// writer.print(gson.toJson(parts));
	// writer.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }

	// public void impPartsToJSON(File file) {
	// PrintWriter writer;
	// Gson gson = new
	// GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
	// try {
	// writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
	// writer.print(gson.toJson(improvedParts));
	// writer.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }

	public void setSignalParts(ArrayList<SignalPart> parts) {
		this.parts = parts;
	}

	public void setImpParts(ArrayList<SignalPart> parts) {
		this.improvedParts = parts;
	}

	public SignalPartSet getSignalPartSet() {
		return set;
	}

	public void setSignalPartSet(SignalPartSet set) {
		this.set = set;
		parts = set.getParts();
		improvedParts = set.getImpParts();
	}
}
