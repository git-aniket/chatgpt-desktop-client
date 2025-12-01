package nl.vu.psy.ams.suite.gui.graphgen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Map.Entry;

import javax.swing.JPanel;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.LabelValue;
import nl.vu.psy.ams.suite.data.structures.MetaLabel;
import nl.vu.psy.ams.suite.data.structures.sets.AmsLabelSet;
import nl.vu.psy.ams.suite.data.structures.sets.MetaLabelSet;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

public class GeneratedGraphPanel extends JPanel {

	/**
	 * JPanel that can draw and save a bar graph of combined labels.
	 */
	private static final long serialVersionUID = 1L;
	private AmsLabelConfiguration lConf = CurrentOpenData.getInstance().getLabelConfig();
	private AmsLabelSet lblSet = CurrentOpenData.getInstance().getLabels();
	private MetaLabelSet mSet = CurrentOpenData.getInstance().getMetaLabels();
	private ArrayList<MetaLabel> mL = mSet.getMetaLabels();

	private ArrayList<Double> avs = new ArrayList<Double>();

	private String graphTitle = AppSettings.getInstance().getProperty(Settings.BARGRAPHTITLE);
	private String xAxisLabel = AppSettings.getInstance().getProperty(Settings.BARGRAPHXTITLE);
	private String yAxisLabel = AppSettings.getInstance().getProperty(Settings.BARGRAPHYTITLE);
	private int maxR = 150;
	private int nCats;

	public GeneratedGraphPanel() {
		super();
		setPreferredSize(new Dimension(600, 300));
		setMinimumSize(new Dimension(600, 300));
		setMaximumSize(new Dimension(600, 300));
		setBackground(Color.WHITE);
		if (mL.size() == 0)
			generateDefault();
		recalcAverages();
	}

	public void generateDefault() {
		ArrayList<String> lst = new ArrayList<String>();
		for (Entry<String, ArrayList<LabelValue>> entry : lConf.getConfig().entrySet()) {
			for (LabelValue lv : entry.getValue()) {
				lst.add(lv.getName());
				lst.add(Integer.toString(lv.getCode()));
			}
		}

		for (int j = 0; j < lst.size(); j += 2) {
			String cat = lst.get(j);
			String val = lst.get(j + 1);
			MetaLabel mLabel = new MetaLabel();
			mLabel.setName(cat);
			mLabel.addConnectedCode(Integer.parseInt(val));
			mSet.addMetaLabel(mLabel);
		}
	}

	public void drawGraph(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setStroke(new BasicStroke(2));
		g2.setFont(new Font("Arial", Font.PLAIN, 16));
		g2.setColor(Color.WHITE);
		int w = getWidth();
		int h = getHeight();
		g2.fillRect(0, 0, w, h);
		g2.setColor(Color.LIGHT_GRAY);
		g2.fillRect(w / 5, h / 5, 15 * w / 20, 3 * h / 5);
		g2.setColor(Color.BLACK);
		g2.drawRect(w / 5, h / 5, 15 * w / 20, 3 * h / 5);
		int pos = h / 5;
		FontMetrics fm = g2.getFontMetrics();
		int txtH = fm.getMaxAscent();
		int nPixPerCat = 0;
		if (mL.isEmpty() == false)
			nPixPerCat = 3 * h / (5 * nCats);

		for (int j = 0; j < mL.size(); j++) {
			if (avs.get(j) != Double.NEGATIVE_INFINITY) {
				String txt = mL.get(j).getName();
				int txtW = fm.stringWidth(txt);
				int yPos = pos + nPixPerCat / 2 + txtH / 2;
				g2.drawString(txt, w / 5 - txtW - 3, yPos);

				double relVal = avs.get(j) / maxR;
				int barWidth = (int) (relVal * 15 * w / 20);
				g2.setColor(Color.RED);
				g2.fillRect(w / 5, pos + nPixPerCat / 2 - 10, barWidth, 20);
				g2.setColor(Color.BLACK);
				g2.drawRect(w / 5, pos + nPixPerCat / 2 - 10, barWidth, 20);

				String hrTxt = Long.toString(Math.round(avs.get(j)));
				g2.drawString(hrTxt, w / 5 + barWidth + 2, yPos);

				pos += nPixPerCat;
			}
		}

		for (int i = 0; i <= maxR; i += 25) {
			double relVal = (double) i / maxR;
			int linePos = (int) (w / 5 + relVal * 15 * w / 20);
			g2.drawLine(linePos, 4 * h / 5, linePos, 4 * h / 5 + 5);
			String txt = Integer.toString(i);
			int txtW = fm.stringWidth(txt);
			g2.drawString(txt, linePos - txtW / 2, 4 * h / 5 + 7 + txtH);
		}

		g2.setFont(new Font("Arial", Font.BOLD, 20));
		fm = g2.getFontMetrics();
		txtH = fm.getMaxAscent();

		int txtW = fm.stringWidth(graphTitle);
		g2.drawString(graphTitle, w / 2 - txtW / 2, 10 + txtH);

		txtW = fm.stringWidth(xAxisLabel);
		g2.drawString(xAxisLabel, w / 5 + 15 * w / 40 - txtW / 2, h - 10);

		txtW = fm.stringWidth(yAxisLabel);
		g2.translate(w / 2., h / 2.);
		g2.rotate(-Math.PI / 2);
		g2.translate(-h / 2., -w / 2.);
		g2.drawString(yAxisLabel, h / 2 - txtW / 2, txtH + 5);

	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		drawGraph(g2);
	}

	public void recalcAverages() {
		avs.clear();
		nCats = 0;
		for (int i = 0; i < mL.size(); i++) {
			ArrayList<Double> av = new ArrayList<Double>();
			ArrayList<Double> times = new ArrayList<Double>();
			MetaLabel mLabel = mL.get(i);
			ArrayList<String> lst = new ArrayList<String>();
			for (Entry<String, ArrayList<LabelValue>> entry : lConf.getConfig().entrySet()) {
				for (LabelValue lv : entry.getValue()) {
					if (mLabel.getConnectedCodes().contains(lv.getCode())) {
						lst.add(entry.getKey());
						lst.add(lv.getName());
					}
				}
			}

			for (AmsLabel l : lblSet.getLabels()) {
				boolean isCorrectLabel = false;
				for (int j = 0; j < lst.size(); j += 2) {
					String cat = lst.get(j);
					String val = lst.get(j + 1);
					if (l.getAttributes().get(cat) != null && l.getAttributes().get(cat).equals(val) == true) {
						isCorrectLabel = true;
						break;
					}
				}
				if (isCorrectLabel) {
					times.add(l.getTotalTimeUnderLabel());
					av.add(l.getAverage(true));
				}
			}
			if (av.isEmpty()) {
				avs.add(Double.NEGATIVE_INFINITY);
			} else {
				double ttime = 0;
				double avVal = 0;
				for (int j = 0; j < times.size(); j++) {
					ttime += times.get(j);
					avVal += times.get(j) * av.get(j);
				}
				avVal /= ttime;
				avs.add(avVal);
				nCats++;
			}
		}
	}

	public void setGraphTitle(String ret) {
		this.graphTitle = ret;
		repaint();
	}

	public void setXAxisTitle(String ret) {
		this.xAxisLabel = ret;
		repaint();
	}

	public void setYAxisTitle(String ret) {
		this.yAxisLabel = ret;
		repaint();
	}

}
