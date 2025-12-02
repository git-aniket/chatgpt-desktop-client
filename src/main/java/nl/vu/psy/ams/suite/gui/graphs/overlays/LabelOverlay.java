package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.AmsLabelConfiguration;
import nl.vu.psy.ams.suite.data.structures.LabelValue;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsPacket;
import nl.vu.psy.ams.suite.data.structures.sets.AmsLabelSet;
import nl.vu.psy.ams.suite.data.structures.sets.LabelSet;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.drawing.SignalPartDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.tabs.imp.ImpTab;
import nl.vu.psy.ams.suite.gui.tabs.label.LabelToolBar;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Overlay that shows and edits (if enabled) labels. Labels
 * are colored by categories. Also used to show and edit
 * ECG artefacts
 */
public class LabelOverlay extends Overlay {

	private static final int UNDER_LABEL = 0;

	private static final int LEFT_OF_LABEL = 1;

	private static final int RIGHT_OF_LABEL = 2;

	private static final int NOT_UNDER_LABEL = 3;

	private static final int NOT_IN_RANGE = 4;

	private LabelSet labels;

	private double fillFactor;
	private String title = null;

	private AmsLabel movingLabel = null;

	private int dragType = -1;

	private double prevTime, diffTime;

	private AmsLabelConfiguration labelConfig;
	private boolean isEditable = true, showTooltip = true;

	private ArrayList<Graph> graphs = new ArrayList<Graph>();

	private boolean isClicked = false;
	private boolean finalizeFinished;

	private int minValue = 1;
	private int secValue = 0;

	private boolean drawCursorTime = false;
	private int drawCursorPos;
	private int countlabel = 0;

	private TexturePaint errorPaint;

	private Map<String, String> copiedAttributes = new HashMap<String, String>();

	protected double leftTime;
	protected double rightTime;
	private double ltime, rtime, curtime = 0.0;

	public boolean setCancelled = false;

	public LabelOverlay(Graph graph) {
		super(graph, true);
		ToolTipManager.sharedInstance().registerComponent(graph);
		graph.setToolTipText("");
		graph.setToolTipText(null);
		fillFactor = 1;
		if (CurrentOpenData.getInstance().getLabels().getLabels().isEmpty() == false) {
			isClicked = true;
		}
		BufferedImage bi = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bi.createGraphics();
		// g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		// RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 5, 5);
		g.setColor(Color.red);
		g.drawLine(0, 0, 5, 5);
		g.drawLine(0, 5, 5, 0);
		errorPaint = new TexturePaint(bi, new Rectangle(0, 0, 5, 5));
	}

	public void addConnectedGraph(Graph graph) {
		graphs.add(graph);
	}

	@Override
	public void draw(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int w = graph.getWidth();
		int h = graph.getHeight();

		g.setColor(Color.BLACK);
		if (title != null) {
			if (isEditable == true && isClicked == false) {
				String text = "Click and Drag to add " + title;
				FontMetrics metrics = g.getFontMetrics();
				double txtW = metrics.getStringBounds(text, g).getWidth();
				double txtH = metrics.getStringBounds(text, g).getHeight();
				g.drawString(text, (float) (w / 2f - txtW / 2f), (float) (h / 2f + txtH / 3f));
			} else {
				FontMetrics metrics = g.getFontMetrics();
				double txtW = metrics.getStringBounds(title, g).getWidth();
				double txtH = metrics.getStringBounds(title, g).getHeight();
				g.drawString(title, (float) (w / 2f - txtW / 2f), (float) (h / 2f + txtH / 3f));
			}
		}

		double difftime = 0;
		int givelabelno = 0;
		double smalltime = Double.POSITIVE_INFINITY;

		for (AmsLabel l : labels.getLabels()) {

			if (l.equals(movingLabel) == false) {

				difftime = (l.getLeftTime() - CurrentOpenData.getInstance().getStartTimeInUS()) / 1000000;

				if (difftime < smalltime) {
					smalltime = difftime;
				}
				if (difftime == smalltime) {
					l.setLabelNo(1);
					givelabelno = 1;
					drawLabel(l, g);
				} else {
					l.setLabelNo(givelabelno + 1);
					drawLabel(l, g);
				}
			} else {

				difftime = (l.getLeftTime() - CurrentOpenData.getInstance().getStartTimeInUS()) / 1000000;

				if (difftime < smalltime) {
					smalltime = difftime;
				}
				if (difftime == smalltime) {
					l.setLabelNo(1);
					givelabelno = 1;
					drawLabel(l, g);
				} else {
					l.setLabelNo(givelabelno + 1);
					drawLabel(l, g);
				}
			}
			givelabelno = l.getLabelNo();
		}

		if (movingLabel != null) {
			drawLabel(movingLabel, g);

			double lTime = graph.getxAxis().getLeftTime();
			double rTime = graph.getxAxis().getRightTime();
			int lPos = Utils.getPixelCoordinate(movingLabel.getLeftTime(), lTime, rTime, w);
			int rPos = Utils.getPixelCoordinate(movingLabel.getRightTime(), lTime, rTime, w);
			int drawH = (int) (h * fillFactor);

			String lTxt = Utils.getTimeFromUS(movingLabel.getLeftTime());
			String rTxt = Utils.getTimeFromUS(movingLabel.getRightTime());
			FontMetrics metric = g.getFontMetrics();
			Rectangle2D lBounds = metric.getStringBounds(lTxt, g);
			Rectangle2D rBounds = metric.getStringBounds(rTxt, g);

			RoundRectangle2D lrr = new RoundRectangle2D.Double(lPos - lBounds.getWidth() - 10,
					drawH / 2. - lBounds.getHeight() / 2., lBounds.getWidth() + 9,
					lBounds.getHeight(), 10, 10);
			RoundRectangle2D rrr = new RoundRectangle2D.Double(rPos + 1, drawH / 2. - rBounds.getHeight() / 2.,
					rBounds.getWidth() + 9, rBounds.getHeight(),
					10, 10);
			g.setColor(Color.WHITE);
			g.fill(lrr);
			g.fill(rrr);

			g.setColor(Color.BLACK);
			g.draw(lrr);
			g.draw(rrr);

			g.drawString(lTxt, Math.round(lPos - lBounds.getWidth() - 5),
					Math.round(drawH / 2 + lBounds.getHeight() / 3));
			g.drawString(rTxt, rPos + 6, Math.round(drawH / 2 + rBounds.getHeight() / 3));
		}
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		int drawH = (int) (h * fillFactor);
		// for (AmsLabel l : labels.getLabels()) {
		// if (l.equals(movingLabel) == false) {
		// for (AmsLabel l2 : labels.getLabels()) {
		// if (l.equals(l2) == false) {
		// if (l.getLeftTime() > l2.getLeftTime() && l.getLeftTime() <
		// l2.getRightTime()) {
		// int lPos = Utils.getPixelCoordinate(l.getLeftTime(), lTime, rTime, w);
		// int rPos = 0;
		// if (l2.getRightTime() < l.getRightTime()) {
		// rPos = Utils.getPixelCoordinate(l2.getRightTime(), lTime, rTime, w);
		// } else {
		// rPos = Utils.getPixelCoordinate(l.getRightTime(), lTime, rTime, w);
		// }
		// g.setPaint(errorPaint);
		// g.fillRect(lPos, 0, rPos - lPos, drawH);
		// }
		// }
		// }
		// }
		// }
		if (movingLabel != null) {
			for (AmsLabel l2 : labels.getLabels()) {
				if (movingLabel.equals(l2) == false) {
					if (movingLabel.getLeftTime() > l2.getLeftTime() && movingLabel.getLeftTime() < l2.getRightTime()) {
						int lPos = Utils.getPixelCoordinate(movingLabel.getLeftTime(), lTime, rTime, w);
						int rPos = 0;
						if (l2.getRightTime() < movingLabel.getRightTime()) {
							rPos = Utils.getPixelCoordinate(l2.getRightTime(), lTime, rTime, w);
						} else {
							rPos = Utils.getPixelCoordinate(movingLabel.getRightTime(), lTime, rTime, w);
						}
						g.setPaint(errorPaint);
						g.fillRect(lPos, 0, rPos - lPos, drawH);
					}
					if (l2.getLeftTime() > movingLabel.getLeftTime() && l2.getLeftTime() < movingLabel.getRightTime()) {
						int lPos = Utils.getPixelCoordinate(l2.getLeftTime(), lTime, rTime, w);
						int rPos = 0;
						if (l2.getRightTime() < movingLabel.getRightTime()) {
							rPos = Utils.getPixelCoordinate(l2.getRightTime(), lTime, rTime, w);
						} else {
							rPos = Utils.getPixelCoordinate(movingLabel.getRightTime(), lTime, rTime, w);
						}
						g.setPaint(errorPaint);
						g.fillRect(lPos, 0, rPos - lPos, drawH);
					}
				}
			}
		}

		if (drawCursorTime) {

			int lPos = drawCursorPos, lPosDraw;

			String lTxt = Utils.getTimeFromUS(graph.getxAxis().getTimeFromPixel(lPos));
			FontMetrics metric = g.getFontMetrics();
			Rectangle2D lBounds = metric.getStringBounds(lTxt, g);

			if (lPos - lBounds.getWidth() < 10)
				lPosDraw = lPos;
			else
				lPosDraw = lPos - (int) lBounds.getWidth();
			RoundRectangle2D lrr = new RoundRectangle2D.Double(lPosDraw - 10, drawH / 2. - lBounds.getHeight() / 2.,
					lBounds.getWidth() + 9,
					lBounds.getHeight(), 10, 10);
			g.setColor(Color.WHITE);
			g.fill(lrr);
			g.setColor(Color.BLACK);
			g.draw(lrr);

			g.drawString(lTxt, Math.round(lPosDraw - 5), Math.round(drawH / 2 + lBounds.getHeight() / 3));
		}

	}

	private void drawLabel(AmsLabel l, Graphics2D g) {
		double lTime = graph.getxAxis().getLeftTime();
		double rTime = graph.getxAxis().getRightTime();
		int w = graph.getWidth();
		int h = graph.getHeight();
		int drawH = (int) (h * fillFactor);
		int lPos = Utils.getPixelCoordinate(l.getLeftTime(), lTime, rTime, w);
		int rPos = Utils.getPixelCoordinate(l.getRightTime(), lTime, rTime, w);
		int nCat = labelConfig.getCategories().size();
		// boolean isTimeLabel = false;
		if (nCat == 0)
			nCat = 1; // prevent div by zero exception when categories are not yet saved

		// ----------------------- Include type of artefact while creating ECG/SCL
		// Artefact-----------------------------
		if ((labelConfig.getCategories().toString().equals("[ECG Artefact]"))
				|| (labelConfig.getCategories().toString().equals("[SCL Artefact]"))) {
			g.setColor(Color.RED);
			g.fillRect(lPos, 0, rPos - lPos, drawH);
		} else {
			// ---------------------------------------------------------------------------------------------------------
			if (labelConfig != null) {
				int index = 0;
				for (String cat : labelConfig.getCategories()) {
					ArrayList<LabelValue> vals = labelConfig.getValuesForCategory(cat);
					if (vals != null) {
						int i = 0;
						for (LabelValue v : vals) {
							if (v.getName().equals(l.getAttributes().get(cat)))
								break;
							i++;
						}
						if (i < vals.size()) {
							g.setColor(Utils.getColorFromInteger((float) i / vals.size()));
							g.fillRect(lPos, index * drawH / nCat, rPos - lPos, drawH / nCat);
						} else {
							if (!l.isTimeLabel()) {
								g.setPaint(errorPaint);
								g.fillRect(lPos, index * drawH / nCat, rPos - lPos, drawH / nCat);
							} else if (l.getRightTime() - l.getLeftTime() != 60000000) { // not minute label
								g.setColor(new Color(204, 204, 204));
								// isTimeLabel = true;
								g.fillRect(lPos, index * drawH / nCat, rPos - lPos, drawH / nCat);
							}
						}
					}
					index++;
				}
			}
		}
		if (rPos - lPos > 1)
			g.setColor(Color.BLACK);
		g.drawRect(lPos, 0, rPos - lPos, drawH);
		g.setColor(Color.DARK_GRAY);

		// ------ If SCL or ECG Artefact do not add label no-------
		if (labelConfig != null && !labelConfig.getCategories().toString().equals("[ECG Artefact]")
				&& !labelConfig.getCategories().toString().equals("[SCL Artefact]")) {
			ArrayList<String> cats = labelConfig.getCategories();
			String[] labelStrings = new String[cats.size()];
			Map<String, String> att = l.getAttributes();

			att.put("Label No", Integer.toString(l.getLabelNo()));
			for (int j = 0; j < cats.size(); j++) {
				String val = att.get(cats.get(j));
				for (LabelValue lv : labelConfig.getConfig().get(cats.get(j))) {
					if (lv.getName().equals(val)) {
						labelStrings[j] = lv.getName(); // Integer.toString(lv.getCode());
					}
				}
			}
			int hLine = (int) (h / nCat * fillFactor);
			// int strW = g.getFontMetrics().stringWidth(labelString);
			for (int j = 0; j < cats.size(); j++) {
				if (labelStrings[j] == null) // to prevent exception in getFontMetrics on new label
					// if (isTimeLabel)
					// labelStrings[j] = l.getAttributes().get("Time label");
					// else
					labelStrings[j] = "temp";
				double strW = g.getFontMetrics().getStringBounds(labelStrings[j], g).getWidth();
				double strH = g.getFontMetrics().getStringBounds(labelStrings[j], g).getHeight();
				// g.drawString(title, (float) (w / 2f - txtW / 2f), (float) (h / 2f + txtH /
				// 3f));
				if (strW < rPos - lPos) {
					// int strH = g.getFontMetrics().getMaxAscent();
					g.drawString(labelStrings[j], (float) ((rPos + lPos) / 2f - strW / 2f),
							(float) ((hLine / 2f) + j * hLine + strH / 3f));
				}
			}
		}
	}

	private String[] finalizeAddingNewLabel() {

		ArrayList<String> categories = labelConfig.getCategories();
		if (categories.isEmpty()) {
			finalizeFinished = true;
			return null;
		}
		boolean isArtefact = labelConfig.getCategories().toString().equals("[ECG Artefact]");
		String[] output = new String[categories.size() * 2];
		ArrayList<JList<String>> lists = new ArrayList<JList<String>>();
		String artefactCat = "";

		JPanel timepanel = new JPanel(new GridLayout(4, 1));
		JTextField left = new JTextField(5);
		GregorianCalendar date1 = (GregorianCalendar) CurrentOpenData.getInstance().getStartDate().clone();
		left.setText(Utils.getDateAndTimeFromUSforLabels(movingLabel.getLeftTime(), date1,
				CurrentOpenData.getInstance().getStartTimeInUS()));
		JTextField right = new JTextField(5);
		right.setText(Utils.getDateAndTimeFromUSforLabels(movingLabel.getRightTime(), date1,
				CurrentOpenData.getInstance().getStartTimeInUS()));
		timepanel.add(new JLabel("Start time"));
		timepanel.add(left);
		timepanel.add(new JLabel("End time"));
		timepanel.add(right);
		if (!isArtefact) {

			setcancelled(false);
			final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Set label categories", true);
			diag.setLayout(new BorderLayout());
			diag.setResizable(true);
			Dimension d = MainFrame.getInstance().getMainFrame().getSize();
			diag.setMaximumSize(new Dimension(600, d.height));

			JPanel chPanel = new JPanel();
			chPanel.setLayout(new BoxLayout(chPanel, BoxLayout.X_AXIS));
			for (String s : categories) {
				JPanel panel = new JPanel(new BorderLayout());
				panel.add(new JLabel(s + ":"), BorderLayout.NORTH);
				ArrayList<LabelValue> values = labelConfig.getValuesForCategory(s);
				String[] listValues = new String[values.size()];
				for (int i = 0; i < listValues.length; i++) {
					listValues[i] = values.get(i).getName();
				}
				JList<String> list = new JList<String>(listValues);
				list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				list.setLayoutOrientation(JList.VERTICAL);
				list.setBorder(BorderFactory.createEtchedBorder());
				String curVal = movingLabel.getAttributes().get(s);
				if (curVal != null) {
					list.setSelectedValue(curVal, true);
				} else {
					list.setSelectedIndex(0);
				}
				panel.add(list, BorderLayout.WEST);
				chPanel.add(panel);
				lists.add(list);
			}
			diag.add(new JScrollPane(chPanel), BorderLayout.NORTH);
			JPanel butPanel = new JPanel();
			JButton kButton = new JButton("OK");
			kButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					countlabel++;
					movingLabel.setLabelNo(countlabel);
					finalizeFinished = true;
					diag.setVisible(false);
				}
			});
			butPanel.add(kButton);
			diag.getRootPane().setDefaultButton(kButton);// ENTER will hit button OK

			JButton cButton = new JButton("Cancel");
			cButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					setcancelled(true);
					diag.setVisible(false);
				}
			});
			butPanel.add(cButton);
			diag.add(butPanel, BorderLayout.SOUTH);
			if (labelConfig != null && !labelConfig.getCategories().toString().equals("[ECG Artefact]")
					&& !labelConfig.getCategories().toString().equals("[SCL Artefact]")) {
				diag.add(timepanel, BorderLayout.CENTER);
			}
			diag.pack();
			diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
			diag.setVisible(true);
		} else {
			countlabel++;
			movingLabel.setLabelNo(countlabel);
			finalizeFinished = true;
			artefactCat = "Manually added";
		}
		if (finalizeFinished) {
			// ---------------- To check if this is a real label or some kind of
			// artefact------------------
			if (labelConfig != null && !labelConfig.getCategories().toString().equals("[ECG Artefact]")
					&& !labelConfig.getCategories().toString().equals("[SCL Artefact]")) {
				// ---------------------------------------------------------------------------------------------
				// labels.setCustomizedLabels(false);
				labels.setlabelindex(1);

				String s1 = left.getText();
				String s2 = right.getText();

				// --- To add milli seconds----
				// String s1withMS = s1.concat(actuallefttext.substring(19,
				// actuallefttext.length()));
				// String s2withMS = s2.concat(actualrighttext.substring(19,
				// actualrighttext.length()));
				// ----------------

				String patternwithMS = "dd-MM-yyyy/HH:mm:ss.SSS";

				// String pattern = "dd-MM-yyyy/HH:mm:ss";
				SimpleDateFormat format = new SimpleDateFormat(patternwithMS);
				Date date2 = Utils.parseDate(format, s1); // withMS);
				Date date3 = Utils.parseDate(format, s2); // withMS);
				if (date2 != null && date3 != null) {
					if (date2.after(date3)) {
						finalizeFinished = false;
						JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
								"Start time has to be before End time", "Label error", JOptionPane.ERROR_MESSAGE);
					}

					GregorianCalendar dd = CurrentOpenData.getInstance().getStartDate();
					double newlefttime = Utils.getTimeFromDate(date2, dd,
							CurrentOpenData.getInstance().getStartTimeInUS());
					double newrighttime = Utils.getTimeFromDate(date3, dd,
							CurrentOpenData.getInstance().getStartTimeInUS());

					movingLabel.setLeftTime(newlefttime);
					movingLabel.setRightTime(newrighttime);
					graph.repaint();
				}
			}
			// ----------------------------If Artefact-include milliseconds
			// also---------------
			else {
				movingLabel.setLeftTime(movingLabel.getLeftTime());
				movingLabel.setRightTime(movingLabel.getRightTime());
				graph.repaint();
			}
			// --------------------------------------------------------------------------------
		}

		movingLabel.getAttributes().clear();
		for (int i = 0; i < categories.size(); i++) {
			String s = categories.get(i);
			String val;
			if (!isArtefact)
				val = (String) lists.get(i).getSelectedValue();
			else
				val = artefactCat;
			movingLabel.getAttributes().put(s, val);
			output[2 * i] = s;
			output[2 * i + 1] = val;
		}
		// CurrentOpenData.getInstance().saveChangeablesToDisk();
		return output;
	}

	private AmsLabel getLabelUnderMouse(int mouseX, int mouseY) {
		int snapSize = AppSettings.getInstance().getIntProperty(Settings.SNAPSIZE);
		double mouseTime = graph.getxAxis().getTimeFromPixel(mouseX);
		double mouseTimeL = graph.getxAxis().getTimeFromPixel(mouseX - snapSize);
		double mouseTimeR = graph.getxAxis().getTimeFromPixel(mouseX + snapSize);
		int h = graph.getHeight();
		int drawH = (int) (h * fillFactor);
		for (AmsLabel l : labels.getLabels()) {
			if (mouseTime > l.getLeftTime() && mouseTime < l.getRightTime() && mouseY < drawH) {
				return l;
			} else if (mouseTimeR > l.getLeftTime() && mouseTimeR < l.getRightTime()) {
				return l;
			} else if (mouseTimeL > l.getLeftTime() && mouseTimeL < l.getRightTime()) {
				return l;
			}
		}
		return null;
	}

	public AmsLabel getMovingLabel() {
		return movingLabel;
	}

	@Override
	public String getToolTipText(MouseEvent arg0) {
		double mouseTime = graph.getxAxis().getTimeFromPixel(arg0.getX());
		int h = graph.getHeight();
		int drawH = (int) (h * fillFactor);
		for (AmsLabel l : labels.getLabels()) {
			if (mouseTime > l.getLeftTime() && mouseTime < l.getRightTime() && arg0.getY() < drawH) {
				// Calculate duration in microseconds and convert to milliseconds and seconds
				double durationUS = l.getRightTime() - l.getLeftTime();
				double durationMS = durationUS / 1000.0;
				double durationS = durationMS / 1000.0;

				String labelText = "<html><b>Label Times:</b><br>";
				labelText += "<b>Start:</b> " + Utils.getTimeFromUS(l.getLeftTime()) + "<br>";
				labelText += "<b>End:</b> " + Utils.getTimeFromUS(l.getRightTime()) + "<br>";
				labelText += "<b>Duration:</b> " + String.format("%.2f ms / %.2f s", durationMS, durationS) + "<br>";
				labelText += "<br><b>Attributes:</b><br><table border=\"1\">";
				for (Map.Entry<String, String> entry : l.getAttributes().entrySet()) {
					labelText += "<tr><td>" + entry.getKey() + ":</td><td>" + entry.getValue() + "</td></tr>";
				}
				labelText += "</table></html>";
				return labelText;
			}
		}
		return null;
	}

	private int isMouseUnderLabel(int mouseX, int mouseY) {
		int snapSize = AppSettings.getInstance().getIntProperty(Settings.SNAPSIZE);
		double mouseTime = graph.getxAxis().getTimeFromPixel(mouseX);
		double mouseTimeL = graph.getxAxis().getTimeFromPixel(mouseX - snapSize);
		double mouseTimeR = graph.getxAxis().getTimeFromPixel(mouseX + snapSize);
		int h = graph.getHeight();
		int drawH = (int) (h * fillFactor);
		if (mouseY > drawH)
			return NOT_IN_RANGE;
		for (AmsLabel l : labels.getLabels()) {
			if (mouseTime > l.getLeftTime() && mouseTime < l.getRightTime()) {
				return UNDER_LABEL;
			} else if (mouseTimeR > l.getLeftTime() && mouseTimeR < l.getRightTime()) {
				return LEFT_OF_LABEL;
			} else if (mouseTimeL > l.getLeftTime() && mouseTimeL < l.getRightTime()) {
				return RIGHT_OF_LABEL;
			}
		}
		return NOT_UNDER_LABEL;
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		if (isEditable == false)
			return;
		if ((arg0.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == InputEvent.BUTTON1_DOWN_MASK) {
			double curTime = graph.getxAxis().getTimeFromPixel(arg0.getX());
			diffTime = curTime - prevTime;
			if (dragType == UNDER_LABEL) {
				movingLabel.setLeftTime(movingLabel.getLeftTime() + diffTime);
				movingLabel.setRightTime(movingLabel.getRightTime() + diffTime);
				prevTime = curTime;
				graph.repaint();

			} else if (dragType == LEFT_OF_LABEL) {
				if (curTime < movingLabel.getRightTime())
					movingLabel.setLeftTime(curTime);
				prevTime = curTime;
			} else if (dragType == RIGHT_OF_LABEL) {
				if (curTime > movingLabel.getLeftTime())
					movingLabel.setRightTime(curTime);
				prevTime = curTime;
			} else if (dragType == NOT_UNDER_LABEL) {
				if (curTime > prevTime) {
					movingLabel.setLeftTime(prevTime);
					movingLabel.setRightTime(curTime);
				} else {
					movingLabel.setLeftTime(curTime);
					movingLabel.setRightTime(prevTime);
				}
			}
			graph.repaint();
			updateConnectedGraphs();
		}
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		drawCursorTime = false;
		// graph.repaint();
	}

	private void displayToolTip(MouseEvent event) {
		final ToolTipManager ttm = ToolTipManager.sharedInstance();
		final int oldDelay = ttm.getInitialDelay();
		ttm.setInitialDelay(0);
		ttm.setDismissDelay(10000);
		ttm.mouseMoved(event);

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				ttm.setInitialDelay(oldDelay);
			}
		}, ttm.getDismissDelay());
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		drawCursorTime = false;
		switch (isMouseUnderLabel(arg0.getX(), arg0.getY())) {
			case UNDER_LABEL:
				graph.setToolTipText(this.getToolTipText(arg0));
				if (showTooltip)
					displayToolTip(arg0);
				if (isEditable)
					graph.setCursor(Utils.getCursor(Cursor.HAND_CURSOR));
				break;
			case LEFT_OF_LABEL:
				if (isEditable)
					graph.setCursor(Utils.getCursor(Cursor.W_RESIZE_CURSOR));
				graph.setToolTipText(null);
				break;
			case RIGHT_OF_LABEL:
				if (isEditable)
					graph.setCursor(Utils.getCursor(Cursor.E_RESIZE_CURSOR));
				graph.setToolTipText(null);
				break;
			case NOT_UNDER_LABEL:
				if (isEditable) {
					graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
					drawCursorTime = true;
					drawCursorPos = arg0.getX();
				}
				graph.setToolTipText(null);
				break;
			case NOT_IN_RANGE:
				if (isEditable)
					graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
				graph.setToolTipText(null);
				break;
		}
		// graph.repaint();
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		if (isEditable == false)
			return;
		if (arg0.getButton() == MouseEvent.BUTTON3 || arg0.isControlDown()) {

			if (isMouseUnderLabel(arg0.getX(), arg0.getY()) == UNDER_LABEL) {
				final int xMousePos = arg0.getX();
				final int yMousePos = arg0.getY();

				JPopupMenu popup = new JPopupMenu();
				JMenuItem editItem = new JMenuItem("Edit");
				if (labelConfig.getCategories().isEmpty() == false
						&& !labelConfig.getCategories().toString().equals("[ECG Artefact]")
						&& !labelConfig.getCategories().toString().equals("[SCL Artefact]"))
					popup.add(editItem);
				editItem.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						movingLabel = getLabelUnderMouse(xMousePos, yMousePos);
						movingLabel.getAttributes();
						finalizeFinished = false;
						finalizeAddingNewLabel();
						ArrayList<Integer> removeIndices = new ArrayList<Integer>();
						ImpTab.getInstance().getECGDrawer().getSignalPartSet().addPart(movingLabel, removeIndices);
						ImpTab.getInstance().getImpDrawer().getSignalPartSet().addPart(movingLabel, removeIndices);
						if (ImpTab.getInstance().getDrawers() != null)
							for (SignalPartDrawer d : ImpTab.getInstance().getDrawers())
								d.getSignalPartSet().addPart(movingLabel, removeIndices);
						ImpTab.getInstance().getECGRawDrawer().getSignalPartSet().addPart(movingLabel, removeIndices);
						ImpTab.getInstance().getImpRawDrawer().getSignalPartSet().addPart(movingLabel, removeIndices);
						if (ImpTab.getInstance().getFiltDrawers() != null)
							for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
								prt.getSignalPartSet().addPart(movingLabel, removeIndices);
						movingLabel = null;
						graph.repaint();
						updateConnectedGraphs();
					}
				});
				JMenuItem timeitem = new JMenuItem("Divide this label into time intervals");
				if (labelConfig.getCategories().isEmpty() == false
						&& !labelConfig.getCategories().toString().equals("[ECG Artefact]")
						&& !labelConfig.getCategories().toString().equals("[SCL Artefact]"))
					popup.add(timeitem);
				timeitem.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						ltime = getLabelUnderMouse(xMousePos, yMousePos).getLeftTime();
						rtime = getLabelUnderMouse(xMousePos, yMousePos).getRightTime();
						curtime = ltime;
						// double tw = 60000000;

						final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Select time width",
								true);
						final JFormattedTextField timeWidth = new JFormattedTextField(60);
						timeWidth.setColumns(10);
						diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.X_AXIS));
						diag.add(new JLabel("Set time width (seconds): "));
						diag.add(timeWidth);
						final JButton okButton = new JButton("OK");
						diag.getRootPane().setDefaultButton(okButton);
						okButton.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent ev) {
								diag.setVisible(false);
								try {
									timeWidth.commitEdit();
								} catch (ParseException e1) {
								}
								double tw = ((Number) timeWidth.getValue()).doubleValue() * 1000000.;
								double intEnd = curtime + tw;
								if (curtime + tw >= rtime)
									return;

								AmsLabelConfiguration lblcfg = CurrentOpenData.getInstance().getLabelConfig();
								ArrayList<String> categories = lblcfg.getCategories();
								AmsLabel l1 = getLabelUnderMouse(xMousePos, yMousePos);

								labels.removeLabel(getLabelUnderMouse(xMousePos, yMousePos));
								while (curtime + tw < rtime) {
									String[] values = new String[categories.size()];
									String[] attributes = new String[2 * categories.size()];
									if (curtime >= l1.getLeftTime() && intEnd <= l1.getRightTime()) {
										for (int i = 0; i < categories.size(); i++) {
											values[i] = l1.getAttributes().get(categories.get(i));
											attributes[2 * i] = categories.get(i);
											attributes[(2 * i) + 1] = values[i];
										}
										AmsLabelSet alabels = (AmsLabelSet) labels;
										alabels.add(
												new AmsLabel(curtime, (curtime + tw), true, tw / 1000000, attributes),
												true); // was
										// customized
									}
									curtime += tw;

								}
								String[] values = new String[categories.size()];
								String[] attributes = new String[2 * categories.size()];
								if (curtime >= l1.getLeftTime() && rtime <= l1.getRightTime()) {
									for (int i = 0; i < categories.size(); i++) {
										values[i] = l1.getAttributes().get(categories.get(i));
										attributes[2 * i] = categories.get(i);
										attributes[(2 * i) + 1] = values[i];
									}
								}
								AmsLabelSet alabels = (AmsLabelSet) labels;
								alabels.add(new AmsLabel(curtime, (rtime), true, tw / 1000000, attributes), true); // was
								// customized
								labels.setlabelindex(2);
								LabelToolBar.labelchooser.setSelectedIndex(0);
							}
						});

						diag.add(okButton);
						diag.getRootPane().setDefaultButton(okButton);// ENTER will hit button OK

						JButton cButton = new JButton("Cancel");
						cButton.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								diag.setVisible(false);
							}
						});

						diag.add(cButton);
						diag.pack();
						diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
						diag.setVisible(true);
						// graph.repaint();
						// updateConnectedGraphs();
					}
				});

				if (!getLabelUnderMouse(xMousePos, yMousePos).getAttributes().toString().equals("{}")
						&& !getLabelUnderMouse(xMousePos, yMousePos).getAttributes().toString()
								.equals("{ECGArtefact=Clipping ECG values}")
						&& !labelConfig.getCategories().toString().equals("[ECG Artefact]")
						&& !labelConfig.getCategories().toString().equals("[SCL Artefact]")) {
					JMenuItem copyItem = new JMenuItem("Copy");
					popup.add(copyItem);
					copyItem.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							movingLabel = getLabelUnderMouse(xMousePos, yMousePos);
							copiedAttributes.putAll(movingLabel.getAttributes());
							leftTime = movingLabel.getLeftTime();
							rightTime = movingLabel.getRightTime();
						}
					});

					JMenuItem delItem = new JMenuItem("Delete");
					popup.add(delItem);
					delItem.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							labels.removeLabel(getLabelUnderMouse(xMousePos, yMousePos));
							graph.repaint();
							updateConnectedGraphs();
						}
					});
				} else {
					JMenuItem delItem = new JMenuItem("Delete");
					popup.add(delItem);
					delItem.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							AmsLabel l = getLabelUnderMouse(xMousePos, yMousePos);
							labels.removeLabel(l);
							if (labelConfig.getCategories().toString().equals("[SCL Artefact]"))
								rescanSCLCycles(l);
							else
								rescanBeats(l);
							graph.repaint();
							graph.getxAxis().updateAll();
							updateConnectedGraphs();
						}
					});
				}

				popup.show(graph, arg0.getX(), arg0.getY());
			}
			// ------------------------------------ Mouse is not under
			// label--------------------------------------
			else {
				final int xMousePos = arg0.getX();

				JPopupMenu popup = new JPopupMenu();
				if (copiedAttributes.toString() != "{}") {// Only show Paste option if label available/copied
					JMenuItem Paste = new JMenuItem("Paste");
					popup.add(Paste);
					Paste.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							// graph.repaint();
							// updateConnectedGraphs();
							double mouseTime = graph.getxAxis().getTimeFromPixel(xMousePos);
							movingLabel = new AmsLabel(mouseTime, mouseTime + rightTime - leftTime, false, 0.0, "");
							movingLabel.getAttributes().putAll(copiedAttributes);
							if (!labelConfig.getCategories().toString().equals("[ECG Artefact]")
									&& !labelConfig.getCategories().toString().equals("[SCL Artefact]")) {
								AmsLabelSet alabels = (AmsLabelSet) labels;
								alabels.add(movingLabel, true);
							} else
								labels.add(movingLabel);
							movingLabel = null;
							graph.repaint();
							updateConnectedGraphs();
						}
					});
				}
				// --------------------------Option 1 ---------------------------------------
				JMenuItem addBetweenMarkers = new JMenuItem("Add label between markers");
				popup.add(addBetweenMarkers);
				addBetweenMarkers.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						double mouseTime = graph.getxAxis().getTimeFromPixel(xMousePos);
						double lDiff = Double.POSITIVE_INFINITY;
						double rDiff = Double.POSITIVE_INFINITY;
						for (Ams5fsPacket event : CurrentOpenData.getInstance().getEvents()) {
							if (event.getlType() <= 2 || event.getlType() == 100) {
								double curTime = 1000. * event.getDwClockTick_ms();
								if (mouseTime < curTime) {
									double curDiff = curTime - mouseTime;
									if (curDiff < rDiff)
										rDiff = curDiff;
								} else {
									double curDiff = mouseTime - curTime;
									if (curDiff < lDiff)
										lDiff = curDiff;
								}
							}
						}
						if (rDiff == Double.POSITIVE_INFINITY) {
							JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
									"No marker after position", "Error",
									JOptionPane.ERROR_MESSAGE);
							return;
						}
						if (lDiff == Double.POSITIVE_INFINITY) {
							JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
									"No marker before position", "Error",
									JOptionPane.ERROR_MESSAGE);
							return;
						}
						// graph.repaint();
						// updateConnectedGraphs();
						movingLabel = new AmsLabel(mouseTime - lDiff, mouseTime + rDiff, false, 0.0, "");
						finalizeFinished = false;
						finalizeAddingNewLabel();
						if (finalizeFinished == true) {
							if (!labelConfig.getCategories().toString().equals("[ECG Artefact]")
									&& !labelConfig.getCategories().toString().equals("[SCL Artefact]")) {
								AmsLabelSet alabels = (AmsLabelSet) labels;
								alabels.add(movingLabel, true);
							} else
								labels.add(movingLabel);
						}
						movingLabel = null;
						graph.repaint();
						updateConnectedGraphs();
					}
				});
				// ------------------------------------------------------------------------------------

				// -----------------Option 2--------------------------
				JMenuItem addBeforeMarker = new JMenuItem("Add fixed-time label at previous marker");
				popup.add(addBeforeMarker);
				addBeforeMarker.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						double mouseTime = graph.getxAxis().getTimeFromPixel(xMousePos);
						double lDiff = Double.POSITIVE_INFINITY;
						for (Ams5fsPacket event : CurrentOpenData.getInstance().getEvents()) {
							if (event.getlType() <= 2 || event.getlType() == 100) {
								double curTime = 1000. * event.getDwClockTick_ms();
								if (mouseTime > curTime) {
									double curDiff = mouseTime - curTime;
									if (curDiff < lDiff)
										lDiff = curDiff;
								}
							}
						}
						if (lDiff == Double.POSITIVE_INFINITY) {
							JOptionPane.showMessageDialog(MainFrame.getInstance().getMainFrame(),
									"No marker before position", "Error",
									JOptionPane.ERROR_MESSAGE);
							return;
						}
						final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Set label length",
								true);
						diag.setLayout(new FlowLayout());
						diag.add(new JLabel("Minutes:"));
						JFormattedTextField minField = new JFormattedTextField();
						minField.setValue(minValue);
						minField.setColumns(3);
						diag.add(minField);
						diag.add(new JLabel("Seconds:"));
						JFormattedTextField secField = new JFormattedTextField();
						secField.setValue(secValue);
						secField.setColumns(3);
						diag.add(secField);
						JButton okBut = new JButton("OK");
						okBut.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								diag.setVisible(false);
							}
						});
						diag.getRootPane().setDefaultButton(okBut);
						diag.add(okBut);
						diag.pack();
						diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
						diag.setVisible(true);

						try {
							minField.commitEdit();
							secField.commitEdit();
						} catch (ParseException e1) {
						}

						minValue = (Integer) minField.getValue();
						secValue = (Integer) secField.getValue();

						double tW = (minValue * 60. + secValue) * 1000000.;

						if (tW > 0) {
							// graph.repaint();
							// updateConnectedGraphs();
							movingLabel = new AmsLabel(mouseTime - lDiff, mouseTime - lDiff + tW, false, 0.0, "");
							finalizeFinished = false;
							finalizeAddingNewLabel();
							if (finalizeFinished == true) {
								if (!labelConfig.getCategories().toString().equals("[ECG Artefact]")
										&& !labelConfig.getCategories().toString().equals("[SCL Artefact]")) {
									AmsLabelSet alabels = (AmsLabelSet) labels;
									alabels.add(movingLabel, true);
								} else
									labels.add(movingLabel);
							}
							movingLabel = null;
							graph.repaint();
							updateConnectedGraphs();
						}
					}
				});
				// ------------------------------------------------------------------------------

				// ------------------------------- Option
				// 3--------------------------------------
				JMenuItem addLabelsBetweenMarkers = new JMenuItem("Add labels between all markers");
				popup.add(addLabelsBetweenMarkers);
				addLabelsBetweenMarkers.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {

						int noofevents = 0;
						int count = 1;
						double time_between_markers = 0;

						// AmsLabelConfiguration lblcfg =
						// CurrentOpenData.getInstance().getLabelConfig();
						ArrayList<String> categories = labelConfig.getCategories();

						String[] output = new String[categories.size() * 2];

						// String[] values = new String[categories.size()];
						// String[] attributes = new String[(2*categories.size())+2];

						for (Ams5fsPacket event : CurrentOpenData.getInstance().getEvents()) {
							if (event.getlType() <= 2 || event.getlType() == 100) {
								noofevents++;
							}
						}

						double[] timeEvents = new double[noofevents + 2];
						timeEvents[0] = CurrentOpenData.getInstance().getStartTimeInUS() / 1000;
						for (Ams5fsPacket event : CurrentOpenData.getInstance().getEvents()) {
							if (event.getlType() <= 2 || event.getlType() == 100) {
								timeEvents[count] = event.getDwClockTick_ms();

								count++;
							}
						}
						timeEvents[count] = CurrentOpenData.getInstance().getEndTimeInUS() / 1000;

						for (int q = 0; q < timeEvents.length; q++) {

							graph.repaint();
							updateConnectedGraphs();

							if (q > 0) {

								time_between_markers = timeEvents[q] - timeEvents[q - 1];

								// --------------------Label Between Markers------------------------
								double length = AppSettings.getInstance().getIntProperty(Settings.LABELBETWEENMARKERS)
										* 1000;
								// ------------------------------------------------------------------

								double startTime = 1000. * timeEvents[q - 1];
								double endTime = 1000. * timeEvents[q];
								movingLabel = new AmsLabel(startTime, endTime, false, 0.0, "");
								finalizeFinished = true;

								if (q < 2) {
									output = finalizeAddingNewLabel();
								}
								if (time_between_markers > length) {

									if (setCancelled == false) {
										movingLabel = new AmsLabel(startTime, endTime, false, 0.0, "");
										movingLabel.getAttributes().clear();
										for (int m = 0; m < categories.size(); m++) {
											movingLabel.getAttributes().put(output[2 * m], output[2 * m + 1]);
										}

										if (finalizeFinished == true) {
											if (!labelConfig.getCategories().toString().equals("[ECG Artefact]")
													&& !labelConfig.getCategories().toString()
															.equals("[SCL Artefact]")) {
												AmsLabelSet alabels = (AmsLabelSet) labels;
												alabels.add(movingLabel, true);
											} else
												labels.add(movingLabel);
										}

										graph.repaint();
										updateConnectedGraphs();

									}
								}
							}

						}
						movingLabel = null;
					}

				});

				// ------------------------------------------------------------------------------

				// --------------------------------Option
				// 4----------------------------------------
				JMenuItem addAfterallMarkers = new JMenuItem("Add fixed-time labels after all markers");
				popup.add(addAfterallMarkers);

				addAfterallMarkers.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						int noofevents = 0;
						int count = 0;

						ArrayList<String> categories = labelConfig.getCategories();
						String[] output = new String[categories.size() * 2];

						for (Ams5fsPacket event : CurrentOpenData.getInstance().getEvents()) {
							if (event.getlType() <= 2 || event.getlType() == 100) {
								noofevents++;
							}
						}

						double[] timeEvents = new double[noofevents];
						for (Ams5fsPacket event : CurrentOpenData.getInstance().getEvents()) {
							if (event.getlType() <= 2 || event.getlType() == 100) {
								timeEvents[count] = event.getDwClockTick_ms();

								count++;
							}
						}

						final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Set label length",
								true);
						diag.setLayout(new FlowLayout());
						diag.add(new JLabel("Minutes:"));
						JFormattedTextField minField = new JFormattedTextField();
						minField.setValue(minValue);
						minField.setColumns(3);
						diag.add(minField);
						diag.add(new JLabel("Seconds:"));
						JFormattedTextField secField = new JFormattedTextField();
						secField.setValue(secValue);
						secField.setColumns(3);
						diag.add(secField);
						JButton okBut = new JButton("OK");
						okBut.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								diag.setVisible(false);
							}
						});
						diag.getRootPane().setDefaultButton(okBut);
						diag.add(okBut);
						diag.pack();
						diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
						diag.setVisible(true);

						try {
							minField.commitEdit();
							secField.commitEdit();
						} catch (ParseException e1) {
						}

						minValue = (Integer) minField.getValue();
						secValue = (Integer) secField.getValue();

						double tW = (minValue * 60. + secValue) * 1000000.;

						double time_between_markers = 0;

						for (int q = 0; q < timeEvents.length; q++) {

							if (tW > 0) {

								if (q > 0) {
									time_between_markers = timeEvents[q] - timeEvents[q - 1];
								} else {
									time_between_markers = timeEvents[q];
								}

								graph.repaint();
								updateConnectedGraphs();

								double curTime = 1000. * timeEvents[q];
								movingLabel = new AmsLabel(curTime, curTime + tW, false, 0.0, "");
								finalizeFinished = true;

								if (q < 1) {
									output = finalizeAddingNewLabel();
								}

								if (time_between_markers > (tW / 1000)) {

									if (setCancelled == false) {
										movingLabel = new AmsLabel(curTime, curTime + tW, false, 0.0, "");
										for (int m = 0; m < categories.size(); m++) {
											movingLabel.getAttributes().put(output[2 * m], output[2 * m + 1]);
										}

										if (finalizeFinished == true) {
											if (!labelConfig.getCategories().toString().equals("[ECG Artefact]")
													&& !labelConfig.getCategories().toString()
															.equals("[SCL Artefact]")) {
												AmsLabelSet alabels = (AmsLabelSet) labels;
												alabels.add(movingLabel, true);
											} else
												labels.add(movingLabel);
										}
										graph.repaint();
										updateConnectedGraphs();
									}
								}
							}
						}
						movingLabel = null;
					}
				});
				// ------------------------------------------------------------------------------------

				// -------------------------Option 5 ----------------------------------
				JMenuItem addBeforeallMarkers = new JMenuItem("Add fixed-time labels before all markers");
				popup.add(addBeforeallMarkers);

				addBeforeallMarkers.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						int noofevents = 0;
						int count = 0;
						ArrayList<String> categories = labelConfig.getCategories();
						String[] output = new String[categories.size() * 2];

						for (Ams5fsPacket event : CurrentOpenData.getInstance().getEvents()) {
							if (event.getlType() <= 2 || event.getlType() == 100) {
								noofevents++;
							}
						}

						double[] timeEvents = new double[noofevents];
						for (Ams5fsPacket event : CurrentOpenData.getInstance().getEvents()) {
							if (event.getlType() <= 2 || event.getlType() == 100) {
								timeEvents[count] = event.getDwClockTick_ms();

								count++;
							}
						}

						final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Set label length",
								true);
						diag.setLayout(new FlowLayout());
						diag.add(new JLabel("Minutes:"));
						JFormattedTextField minField = new JFormattedTextField();
						minField.setValue(minValue);
						minField.setColumns(3);
						diag.add(minField);
						diag.add(new JLabel("Seconds:"));
						JFormattedTextField secField = new JFormattedTextField();
						secField.setValue(secValue);
						secField.setColumns(3);
						diag.add(secField);
						JButton okBut = new JButton("OK");
						okBut.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								diag.setVisible(false);
							}
						});
						diag.getRootPane().setDefaultButton(okBut);
						diag.add(okBut);
						diag.pack();
						diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
						diag.setVisible(true);

						try {
							minField.commitEdit();
							secField.commitEdit();
						} catch (ParseException e1) {
						}

						minValue = (Integer) minField.getValue();
						secValue = (Integer) secField.getValue();

						double tW = (minValue * 60. + secValue) * 1000000.;

						double time_between_markers = 0;

						for (int q = 0; q < timeEvents.length; q++) {

							if (tW > 0) {

								if (q > 0) {
									time_between_markers = timeEvents[q] - timeEvents[q - 1];
								} else {
									time_between_markers = timeEvents[q];
								}

								graph.repaint();
								updateConnectedGraphs();

								double curTime = 1000. * timeEvents[q];
								movingLabel = new AmsLabel(curTime - tW, curTime, false, 0.0, "");
								finalizeFinished = true;

								if (q < 1) {
									output = finalizeAddingNewLabel();
								}

								if (time_between_markers > (tW / 1000)) {

									if (setCancelled == false) {
										movingLabel = new AmsLabel(curTime - tW, curTime, false, 0.0, "");
										for (int m = 0; m < categories.size(); m++) {
											movingLabel.getAttributes().put(output[2 * m], output[2 * m + 1]);
										}
										if (finalizeFinished == true) {
											if (!labelConfig.getCategories().toString().equals("[ECG Artefact]")
													&& !labelConfig.getCategories().toString()
															.equals("[SCL Artefact]")) {
												AmsLabelSet alabels = (AmsLabelSet) labels;
												alabels.add(movingLabel, true);
											} else
												labels.add(movingLabel);
										}

										graph.repaint();
										updateConnectedGraphs();
									}
								}
							}
						}
						movingLabel = null;
					}
				});
				// ------------------------------------------------------------------------------------
				if (!labelConfig.getCategories().toString().equals("[ECG Artefact]")
						&& !labelConfig.getCategories().toString().equals("[SCL Artefact]")) {
					popup.show(graph, arg0.getX(), arg0.getY());
				}
			}
		} else if (arg0.getButton() == MouseEvent.BUTTON1) {
			dragType = isMouseUnderLabel(arg0.getX(), arg0.getY());
			prevTime = graph.getxAxis().getTimeFromPixel(arg0.getX());
			if (dragType != NOT_IN_RANGE) {
				if (dragType != NOT_UNDER_LABEL) {
					movingLabel = getLabelUnderMouse(arg0.getX(), arg0.getY());
				} else {
					movingLabel = new AmsLabel(prevTime, prevTime + 1, false, 0.0, "");
				}
			}
			graph.repaint();
			updateConnectedGraphs();
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		if (isEditable == false)
			return;
		double mlLT, mlRT;
		int lPix = 0, rPix = 0;
		ArrayList<Integer> removeIndices = new ArrayList<Integer>();
		if (dragType >= 0) {
			mlLT = movingLabel.getLeftTime();
			mlRT = movingLabel.getRightTime();
			lPix = graph.getxAxis().getPixelFromTime(mlLT);
			rPix = graph.getxAxis().getPixelFromTime(mlRT);
		}
		if (rPix - lPix > 5) {
			if (dragType == NOT_UNDER_LABEL) {
				finalizeFinished = false;
				finalizeAddingNewLabel();
				if (finalizeFinished == true) {
					if (!labelConfig.getCategories().toString().equals("[ECG Artefact]")
							&& !labelConfig.getCategories().toString().equals("[SCL Artefact]")) {
						AmsLabelSet alabels = (AmsLabelSet) labels;
						alabels.add(movingLabel, true);
					} else
						labels.add(movingLabel);
					isClicked = true;
					if (labelConfig.getCategories().toString().equals("[ECG Artefact]"))
						CurrentOpenData.getInstance().getECGArtefacts().deleteBeatsUnderArtefacts();
					if (labelConfig.getCategories().toString().equals("[SCL Artefact]"))
						CurrentOpenData.getInstance().getSCLArtefacts().deleteSCLCyclesUnderArtefacts();
					graph.getxAxis().updateAll();
				}
			} else if (dragType == LEFT_OF_LABEL) {
				if (labelConfig.getCategories().toString().equals("[ECG Artefact]")) {
					if (diffTime < 0)
						CurrentOpenData.getInstance().getECGArtefacts().deleteBeatsUnderArtefacts();
					else
						rescanBeats(movingLabel);
					graph.getxAxis().updateAll();
				} else if (labelConfig.getCategories().toString().equals("[SCL Artefact]")) {
					if (diffTime < 0)
						CurrentOpenData.getInstance().getSCLArtefacts().deleteSCLCyclesUnderArtefacts();
					else
						rescanSCLCycles(movingLabel);
					graph.getxAxis().updateAll();
				} else {
					ImpTab.getInstance().getECGDrawer().getSignalPartSet().editPart(movingLabel, removeIndices);
					ImpTab.getInstance().getImpDrawer().getSignalPartSet().editPart(movingLabel, removeIndices);
					if (ImpTab.getInstance().getDrawers() != null)
						for (SignalPartDrawer d : ImpTab.getInstance().getDrawers())
							d.getSignalPartSet().editPart(movingLabel, removeIndices);
					ImpTab.getInstance().getECGRawDrawer().getSignalPartSet().editPart(movingLabel, removeIndices);
					ImpTab.getInstance().getImpRawDrawer().getSignalPartSet().editPart(movingLabel, removeIndices);
					if (ImpTab.getInstance().getFiltDrawers() != null)
						for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
							prt.getSignalPartSet().editPart(movingLabel, removeIndices);
				}
			} else if (dragType == RIGHT_OF_LABEL) {
				if (labelConfig.getCategories().toString().equals("[ECG Artefact]")) {
					if (diffTime > 0)
						CurrentOpenData.getInstance().getECGArtefacts().deleteBeatsUnderArtefacts();
					else
						rescanBeats(movingLabel);
					graph.getxAxis().updateAll();
				} else if (labelConfig.getCategories().toString().equals("[SCL Artefact]")) {
					if (diffTime > 0)
						CurrentOpenData.getInstance().getSCLArtefacts().deleteSCLCyclesUnderArtefacts();
					else
						rescanSCLCycles(movingLabel);
					graph.getxAxis().updateAll();
				} else {
					ImpTab.getInstance().getECGDrawer().getSignalPartSet().editPart(movingLabel, removeIndices);
					ImpTab.getInstance().getImpDrawer().getSignalPartSet().editPart(movingLabel, removeIndices);
					if (ImpTab.getInstance().getDrawers() != null)
						for (SignalPartDrawer d : ImpTab.getInstance().getDrawers())
							d.getSignalPartSet().editPart(movingLabel, removeIndices);
					ImpTab.getInstance().getECGRawDrawer().getSignalPartSet().editPart(movingLabel, removeIndices);
					ImpTab.getInstance().getImpRawDrawer().getSignalPartSet().editPart(movingLabel, removeIndices);
					if (ImpTab.getInstance().getFiltDrawers() != null)
						for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
							prt.getSignalPartSet().editPart(movingLabel, removeIndices);
				}
			} else if (dragType == UNDER_LABEL) {
				if (labelConfig.getCategories().toString().equals("[ECG Artefact]")) {
					CurrentOpenData.getInstance().getECGArtefacts().deleteBeatsUnderArtefacts();
					rescanBeats(movingLabel);
					graph.getxAxis().updateAll();
				} else if (labelConfig.getCategories().toString().equals("[SCL Artefact]")) {
					CurrentOpenData.getInstance().getSCLArtefacts().deleteSCLCyclesUnderArtefacts();
					rescanSCLCycles(movingLabel);
					graph.getxAxis().updateAll();
				} else {
					ImpTab.getInstance().getECGDrawer().getSignalPartSet().editPart(movingLabel, removeIndices);
					ImpTab.getInstance().getImpDrawer().getSignalPartSet().editPart(movingLabel, removeIndices);
					if (ImpTab.getInstance().getDrawers() != null)
						for (SignalPartDrawer d : ImpTab.getInstance().getDrawers())
							d.getSignalPartSet().editPart(movingLabel, removeIndices);
					ImpTab.getInstance().getECGRawDrawer().getSignalPartSet().editPart(movingLabel, removeIndices);
					ImpTab.getInstance().getImpRawDrawer().getSignalPartSet().editPart(movingLabel, removeIndices);
					if (ImpTab.getInstance().getFiltDrawers() != null)
						for (SignalPartDrawer prt : ImpTab.getInstance().getFiltDrawers())
							prt.getSignalPartSet().editPart(movingLabel, removeIndices);
				}
			}
		} else if (!labelConfig.getCategories().toString().equals("[ECG Artefact]")
				&& !labelConfig.getCategories().toString().equals("[SCL Artefact]") && movingLabel != null
				&& !(arg0.getButton() == MouseEvent.BUTTON3 || arg0.isControlDown())) {
			finalizeFinished = false;
			final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Set event message", true);
			diag.setLayout(new BorderLayout());

			JPanel chPanel = new JPanel();
			chPanel.setLayout(new GridLayout(4, 1));
			JTextField message = new JTextField(5);
			JTextField code = new JTextField(5);
			code.setText("0");
			chPanel.add(new JLabel("Marker message"));
			chPanel.add(message);
			chPanel.add(new JLabel("Event code"));
			chPanel.add(code);
			diag.add(chPanel, BorderLayout.NORTH);
			JPanel butPanel = new JPanel();
			JButton kButton = new JButton("OK");
			kButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					finalizeFinished = true;
					diag.setVisible(false);
				}
			});
			butPanel.add(kButton);
			diag.getRootPane().setDefaultButton(kButton);// ENTER will hit button OK

			JButton cButton = new JButton("Cancel");
			cButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					diag.setVisible(false);
				}
			});
			butPanel.add(cButton);
			diag.add(butPanel, BorderLayout.SOUTH);

			JPanel timepanel = new JPanel(new GridLayout(2, 1));
			JTextField left = new JTextField(5);
			GregorianCalendar date1 = (GregorianCalendar) CurrentOpenData.getInstance().getStartDate().clone();
			left.setText(Utils.getDateAndTimeFromUSforLabels(movingLabel.getLeftTime(), date1,
					CurrentOpenData.getInstance().getStartTimeInUS()));
			timepanel.add(new JLabel("Event time"));
			timepanel.add(left);
			diag.add(timepanel, BorderLayout.CENTER);
			diag.pack();
			diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
			diag.setResizable(false);
			diag.setVisible(true);
			if (finalizeFinished) {
				String s1 = left.getText();

				String patternwithMS = "dd-MM-yyyy/HH:mm:ss.SSS";
				SimpleDateFormat format = new SimpleDateFormat(patternwithMS);
				Date date2 = Utils.parseDate(format, s1); // withMS);
				if (date2 != null) {
					GregorianCalendar dd = CurrentOpenData.getInstance().getStartDate();
					double newlefttime = Utils.getTimeFromDate(date2, dd,
							CurrentOpenData.getInstance().getStartTimeInUS());

					movingLabel.setLeftTime(newlefttime);
				}
				Integer codeInt = Utils.parseInt(code.getText());
				String messageTxt = message.getText();
				if (codeInt == null)
					codeInt = -1;
				Ams5fsPacket event = new Ams5fsPacket();
				Double clockTick = movingLabel.getLeftTime() / 1000.0;
				event.setDwClockTick_ms(clockTick.longValue());
				event.setlType(100);
				event.setlCode(codeInt);
				event.setSzMessage(messageTxt);
				CurrentOpenData.getInstance().getEvents().add(event);
				graph.getxAxis().updateAll();
			}
		}
		dragType = -1;
		movingLabel = null;
		graph.repaint();
		graph.getxAxis().updateAll();
		updateConnectedGraphs();
	}

	private void rescanBeats(AmsLabel l) {
		double lTime = l.getLeftTime();
		double rTime = l.getRightTime();
		if ((rTime - lTime) < 10000000) {
			double mTime = (lTime + rTime) / 2;
			lTime = mTime - 5000000;
			rTime = mTime + 5000000;
		}
		double ptpw = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.PTPW), 0, 100) / 100.;
		double upw = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.UPW), 0, 100) / 100.;
		double downw = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.DOWNW), 0, 100) / 100.;
		double hT = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.HIGHTHRESHOLD), 0, 100)
				/ 100.;
		double lT = Utils.setInsideBounds(AppSettings.getInstance().getIntProperty(Settings.LOWTHRESHOLD), 0, 100)
				/ 100.;
		CurrentOpenData cod = CurrentOpenData.getInstance();
		cod.getBeatSet().rescan(lTime, rTime, hT, lT, ptpw, upw, downw, cod.getECGFile());
	}

	private void rescanSCLCycles(AmsLabel l) {
		if (AppSettings.getInstance().getIntProperty(Settings.EVENTBASED) == 1)
			CurrentOpenData.getInstance().getEventSCLSet().recalculate();
		else {
			double lTime = l.getLeftTime();
			double rTime = l.getRightTime();
			if ((rTime - lTime) < 120000000) {
				double mTime = (lTime + rTime) / 2;
				lTime = mTime - 60000000;
				rTime = mTime + 60000000;
			}
			CurrentOpenData.getInstance().getSCLSet().rescan(lTime, rTime);
		}
	}

	public void setEditable(boolean editable) {
		this.isEditable = editable;
	}

	public void setShowTooltip(boolean showTooltip) {
		this.showTooltip = showTooltip;
	}

	public void setFillFactor(double fillFactor) {
		this.fillFactor = fillFactor;
	}

	public void setLabelConfig(AmsLabelConfiguration labelConfig) {
		this.labelConfig = labelConfig;
	}

	public void setLabels(LabelSet labels) {
		this.labels = labels;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void updateConnectedGraphs() {
		for (Graph g : graphs)
			g.repaint();
	}

	public void updateLabelConfig() {
		labelConfig = CurrentOpenData.getInstance().getLabelConfig();
	}

	public void setcancelled(boolean state) {
		setCancelled = state;
	}
}
