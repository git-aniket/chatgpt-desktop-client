package nl.vu.psy.ams.suite.gui.graphgen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.gui.SaveDialog;
import nl.vu.psy.ams.suite.gui.axes.XAxis;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.drawing.BinaryDataDrawer;
import nl.vu.psy.ams.suite.gui.drawing.HRDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.graphs.overlays.DrawerOverlay;
import nl.vu.psy.ams.suite.gui.graphs.overlays.TimeShowOverlay;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

public class GeneratedHRMOTGraphPanel extends JPanel {

	/**
	 * JPanel that can draw and save a HR+MOT graph of the entire recording.
	 */
	private static final long serialVersionUID = 1L;
	private XAxis xAxis;
	private HRDrawer hrDraw;
	private BinaryDataDrawer motDrawer;
	private YAxis motYAxis;
	private YAxis hrYAxis;
	private JPanel titlePanel;
	private boolean drawTitle = false;
	private String title;

	public GeneratedHRMOTGraphPanel() {
		super();
		setPreferredSize(new Dimension(600, 300));
		setMinimumSize(new Dimension(600, 300));
		setMaximumSize(new Dimension(600, 300));
		setBackground(Color.WHITE);
		xAxis = new XAxis();
		xAxis.setTimes(CurrentOpenData.getInstance().getStartTimeInUS(),
				CurrentOpenData.getInstance().getEndTimeInUS());
		Graph graph = new Graph(xAxis);
		hrDraw = new HRDrawer();
		hrYAxis = new YAxis(5, 195, 5, 195, "HeartRate");
		hrDraw.setYAxis(hrYAxis);
		hrDraw.getYAxis().setBottomValue(-5);
		hrDraw.getYAxis().setTopValue(195);

		hrDraw.connectToGraph(graph);
		hrDraw.drawRawHR(false);
		hrDraw.drawAverageHR(true);
		hrDraw.setAvColor(Color.BLACK);

		if (CurrentOpenData.getInstance().channelExists("MYA")) {
			Ams7fsChannelInfo s;
			try {
				s = CurrentOpenData.getInstance().getChannelInfoFromID("MYA");
				motYAxis = new YAxis((int) -Math.pow(2, s.getnBits() - 1), (int) Math.pow(2, s.getnBits() - 1) - 1,
						(double) s.getlMinValue()
								/ s.getlMinMaxDivider(),
						(double) s.getlMaxValue() / s.getlMinMaxDivider(), s.getSzID() + " [" + s.getSzUnit() + "]");
				motYAxis.setBottomValue(motYAxis.getSampleValueFromRealValue(-0.05));
				motYAxis.setTopValue(motYAxis.getSampleValueFromRealValue(1.05));
				motDrawer = new BinaryDataDrawer(s.getSzID(), motYAxis);
				motDrawer.setDrawAv(true);
				motDrawer.setAvColor(new Color(0, 205, 0));
				motDrawer.setClipAtZero(true);
				motDrawer.setDrawRaw(false);
				// motDrawer.connectToGraph(graph);
				graph.addUnderlay(new DrawerOverlay(graph, motDrawer, motYAxis));
				graph.setSecondYAxis(motYAxis);
				motYAxis.setFlipped(true);
				// yAxis.setMinimumSize(new Dimension(50,1));
				// add(yAxis,BorderLayout.EAST);
			} catch (Exception e) {
			}
		}

		graph.addOverlay(new TimeShowOverlay(graph));
		graph.setActiveYAxis(hrDraw.getYAxis());

		/*
		 * hrYAxis.setDrawBlocks(false); motYAxis.setDrawBlocks(false);
		 * xAxis.setDrawBlocks(false);
		 */

		setLayout(new BorderLayout());
		add(graph.getPanel(), BorderLayout.CENTER);
		add(xAxis.getDoubleFillerPanel(), BorderLayout.SOUTH);

		setTitle(AppSettings.getInstance().getPropertyOrToBeSaved(Settings.GENGRAPHTITLE));

		titlePanel = new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D) g;
				g2.setColor(Color.BLACK);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setFont(new Font("Arial", Font.BOLD, 24));
				FontMetrics fm = g2.getFontMetrics();
				String name = title;
				int wdth = fm.stringWidth(name);
				int hth = fm.getAscent();
				g2.drawString(name, getWidth() / 2f - wdth / 2f, getHeight() / 2f + hth / 3);
			}
		};
		titlePanel.setMinimumSize(new Dimension(1, 50));
		titlePanel.setPreferredSize(new Dimension(50, 50));
		titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		titlePanel.setBackground(Color.WHITE);

		hrYAxis.setAxisTitle(AppSettings.getInstance().getPropertyOrToBeSaved(Settings.GENGRAPHHRYAXIS));
		if (CurrentOpenData.getInstance().channelExists("MYA"))
			motYAxis.setAxisTitle(AppSettings.getInstance().getPropertyOrToBeSaved(Settings.GENGRAPHMOTYAXIS));
		setXAxisTitle(AppSettings.getInstance().getPropertyOrToBeSaved(Settings.GENGRAPHXAXIS));
	}

	public String getGraphTitle() {
		return title;
	}

	public String getHRAAxisTitle() {
		return hrYAxis.getAxisTitle();
	}

	public HRDrawer getHRDrawer() {
		return hrDraw;
	}

	public String getMOTAxisTitle() {
		if (motYAxis != null)
			return motYAxis.getAxisTitle();
		else
			return null;
	}

	public BinaryDataDrawer getMOTDrawer() {
		return motDrawer;
	}

	public XAxis getxAxis() {
		return xAxis;
	}

	public String getXAxisTitle() {
		return xAxis.getAxisTitle();
	}

	public void saveToFile() {
		BufferedImage bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		hrYAxis.setDrawBlocks(false);
		motYAxis.setDrawBlocks(false);
		xAxis.setDrawBlocks(false);
		Graphics2D ig2 = bi.createGraphics();
		ig2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		paintAll(ig2);
		File retFile = SaveDialog.saveFileBrowserWithAddition("png", "_HRAMOT", "HRA+MOT graph");
		if (retFile != null) {
			try {
				ImageIO.write(bi, "PNG", retFile);
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(this, "Error saving image", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		hrYAxis.setDrawBlocks(true);
		if (motYAxis != null)
			motYAxis.setDrawBlocks(true);
		xAxis.setDrawBlocks(true);
	}

	public void setDrawTitle(boolean drawTitle) {
		if (drawTitle == this.drawTitle)
			return;
		this.drawTitle = drawTitle;
		if (drawTitle) {
			add(titlePanel, BorderLayout.NORTH);
		} else {
			remove(titlePanel);
		}
		revalidate();
		repaint();
	}

	public void setHRAAxisTitle(String title) {
		hrYAxis.setAxisTitle(title);
		repaint();
	}

	public void setHRATitle(String tit) {
		hrYAxis.setAxisTitle(tit);
		repaint();
	}

	public void setMOTAxisTitle(String title) {
		if (motYAxis != null)
			motYAxis.setAxisTitle(title);
		repaint();
	}

	public void setMOTTitle(String tit) {
		if (motYAxis != null)
			motYAxis.setAxisTitle(tit);
		repaint();
	}

	public void setTitle(String title) {
		if (title.length() == 0) {
			this.title = CurrentOpenData.getInstance().getFileHeader().getSzSubjectID();
		} else {
			this.title = title;
		}
		repaint();
	}

	public void setXAxisTitle(String title) {
		if (title.length() == 0) {
			xAxis.setAxisTitle(null);
		} else {
			xAxis.setAxisTitle(title);
		}
		repaint();
	}

}
