package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsPacket;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Draw a line for each user event from the 5fs file.
 */
public class EventOverlay extends Overlay {

	private static final int UNDER_EVENT = 0;

	private static final int NOT_UNDER_EVENT = 1;

	private static final int NOT_IN_RANGE = 2;

	private Ams5fsPacket movingEvent = null;

	private int dragType = -1;

	private double prevTime, diffTime;

	ArrayList<Ams5fsPacket> events = CurrentOpenData.getInstance().getEvents();
	private boolean showCode = false;
	private boolean isEditable = false;
	boolean finalizeFinished = false;

	public EventOverlay(Graph graph) {
		super(graph, true);
	}

	@Override
	public void draw(Graphics2D g) {
		double lTime = (graph.getxAxis().getLeftTime()) / 1000;
		double rTime = (graph.getxAxis().getRightTime()) / 1000;
		int w = graph.getWidth();

		for (Ams5fsPacket p : events) {
			long diffTime = 0;
			if (graph.getDrawers().size() > 0)
				diffTime = graph.getDrawers().get(0).getCurTick();
			if (p.getDwClockTick_ms() - diffTime > lTime && p.getDwClockTick_ms() - diffTime < rTime) {
				if (p.getlType() <= 2 || p.getlType() == null || p.getlType() == 100) {
					if (p.getlType() == 100)
						g.setColor(new Color(255, 100, 100));
					else
						g.setColor(new Color(100, 100, 255));
					int xPos = Utils.getPixelCoordinate(p.getDwClockTick_ms() - diffTime, lTime, rTime, w);
					g.drawLine(xPos, 0, xPos, graph.getHeight());
				}
				if (p.getlType() == 200
						&& AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.SHOWEOFEVENTS) == 1) {
					g.setColor(new Color(100, 255, 100));
					int xPos = Utils.getPixelCoordinate(p.getDwClockTick_ms() - diffTime, lTime, rTime, w);
					g.drawLine(xPos, 0, xPos, graph.getHeight());
				}
			}
		}
		if (showCode) {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			for (Ams5fsPacket p : events) {
				long diffTime = 0;
				if (graph.getDrawers().size() > 0)
					diffTime = graph.getDrawers().get(0).getCurTick();
				if (p.getDwClockTick_ms() - diffTime > lTime && p.getDwClockTick_ms() - diffTime < rTime) {
					if (p.getlType() <= 2 || p.getlType() == null || p.getlType() == 100) {
						g.setColor(Color.WHITE);
						int xPos = Utils.getPixelCoordinate(p.getDwClockTick_ms() - diffTime, lTime, rTime, w);

						String lTxt = p.getSzMessage();
						if (p.getlCode() == 0 && p.getlType() != 100)
							lTxt = "Button";
						if (lTxt.equals(""))
							lTxt = Integer.toString(p.getlCode());
						FontMetrics metric = g.getFontMetrics();
						Rectangle2D lBounds = metric.getStringBounds(lTxt, g);

						RoundRectangle2D lrr = new RoundRectangle2D.Double(xPos - lBounds.getWidth() / 2 - 5,
								10 - lBounds.getHeight() / 2.,
								lBounds.getWidth() + 9, lBounds.getHeight(), 10, 10);
						g.setColor(Color.WHITE);
						g.fill(lrr);
						if (p.getlType() == 100)
							g.setColor(new Color(255, 100, 100));
						else
							g.setColor(new Color(100, 100, 255));
						g.draw(lrr);

						g.drawString(lTxt, Math.round(xPos - lBounds.getWidth() / 2),
								Math.round(10 + lBounds.getHeight() / 3));
					}
					if (p.getlType() == 200
							&& AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.SHOWEOFEVENTS) == 1) {
						g.setColor(Color.WHITE);
						int xPos = Utils.getPixelCoordinate(p.getDwClockTick_ms() - diffTime, lTime, rTime, w);

						String lTxt = p.getSzMessage();
						if (lTxt.equals(""))
							lTxt = Integer.toString(p.getlCode());
						FontMetrics metric = g.getFontMetrics();
						Rectangle2D lBounds = metric.getStringBounds(lTxt, g);

						RoundRectangle2D lrr = new RoundRectangle2D.Double(xPos - lBounds.getWidth() / 2 - 5,
								10 - lBounds.getHeight() / 2.,
								lBounds.getWidth() + 9, lBounds.getHeight(), 10, 10);
						g.setColor(Color.WHITE);
						g.fill(lrr);
						g.setColor(new Color(100, 255, 100));
						g.draw(lrr);

						g.drawString(lTxt, Math.round(xPos - lBounds.getWidth() / 2),
								Math.round(10 + lBounds.getHeight() / 3));
					}
				}
			}
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		}
	}

	private Ams5fsPacket getEventUnderMouse(int mouseX, int mouseY) {
		int snapSize = AppSettings.getInstance().getIntProperty(Settings.SNAPSIZE);
		double mouseTimeL = graph.getxAxis().getTimeFromPixel(mouseX - snapSize) / 1000;
		double mouseTimeR = graph.getxAxis().getTimeFromPixel(mouseX + snapSize) / 1000;
		int drawH = graph.getHeight();
		for (Ams5fsPacket l : events) {
			if (mouseTimeL < l.getDwClockTick_ms() && mouseTimeR > l.getDwClockTick_ms() && mouseY < drawH) {
				return l;
			}
		}
		return null;
	}

	private int isMouseUnderEvent(int mouseX, int mouseY) {
		int snapSize = AppSettings.getInstance().getIntProperty(Settings.SNAPSIZE);
		double mouseTimeL = graph.getxAxis().getTimeFromPixel(mouseX - snapSize) / 1000;
		double mouseTimeR = graph.getxAxis().getTimeFromPixel(mouseX + snapSize) / 1000;
		int drawH = graph.getHeight();
		if (mouseY > drawH)
			return NOT_IN_RANGE;
		for (Ams5fsPacket l : events) {
			if (mouseTimeL < l.getDwClockTick_ms() && mouseTimeR > l.getDwClockTick_ms()) {
				return UNDER_EVENT;
			}
		}
		return NOT_UNDER_EVENT;
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		if (isEditable == false)
			return;
		if ((arg0.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == InputEvent.BUTTON1_DOWN_MASK) {
			double curTime = graph.getxAxis().getTimeFromPixel(arg0.getX());
			diffTime = (curTime - prevTime) / 1000.0;
			if (dragType == UNDER_EVENT && movingEvent.getlType() == 100) {
				movingEvent.setDwClockTick_ms(movingEvent.getDwClockTick_ms() + (int) diffTime);
				prevTime = curTime;
				graph.repaint();
			}
		}
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		switch (isMouseUnderEvent(arg0.getX(), arg0.getY())) {
			case UNDER_EVENT:
				graph.setToolTipText("");
				if (isEditable)
					graph.setCursor(Utils.getCursor(Cursor.HAND_CURSOR));
				break;
			case NOT_UNDER_EVENT:
				if (isEditable)
					graph.setCursor(Utils.getCursor(Cursor.DEFAULT_CURSOR));
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

			if (isMouseUnderEvent(arg0.getX(), arg0.getY()) == UNDER_EVENT) {
				final int xMousePos = arg0.getX();
				final int yMousePos = arg0.getY();

				JPopupMenu popup = new JPopupMenu();
				JMenuItem editItem = new JMenuItem("Edit");
				popup.add(editItem);
				editItem.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						movingEvent = getEventUnderMouse(xMousePos, yMousePos);
						if (movingEvent.getlType() != 100) // only events added afterwards are editable
							return;
						finalizeFinished = false;
						final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Set event message",
								true);
						diag.setLayout(new BorderLayout());

						JPanel chPanel = new JPanel();
						chPanel.setLayout(new GridLayout(4, 1));
						JTextField message = new JTextField(5);
						message.setText(movingEvent.getSzMessage());
						JTextField code = new JTextField(5);
						code.setText(movingEvent.getlCode().toString());
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
						GregorianCalendar date1 = (GregorianCalendar) CurrentOpenData.getInstance().getStartDate()
								.clone();
						left.setText(Utils.getDateAndTimeFromUSforLabels(movingEvent.getDwClockTick_ms() * 1000, date1,
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
										CurrentOpenData.getInstance().getStartTimeInUS()) / 1000.0;

								movingEvent.setDwClockTick_ms((long) newlefttime);
								;
							}
							Integer codeInt = Utils.parseInt(code.getText());
							String messageTxt = message.getText();
							if (codeInt == null)
								codeInt = -1;
							movingEvent.setlType(100);
							movingEvent.setlCode(codeInt);
							movingEvent.setSzMessage(messageTxt);
							graph.getxAxis().updateAll();
						}
						// finalizeFinished = false;
						// finalizeAddingNewLabel();
						movingEvent = null;
						graph.repaint();
					}
				});
				JMenuItem delItem = new JMenuItem("Delete");
				popup.add(delItem);
				delItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						Ams5fsPacket l = getEventUnderMouse(xMousePos, yMousePos);
						if (l.getlType() != 100) // only events added afterwards are editable
							return;
						events.remove(l);
						graph.repaint();
						graph.getxAxis().updateAll();
					}
				});

				popup.show(graph, arg0.getX(), arg0.getY());
			}
			// ------------------------------------ Mouse is not under
			// label--------------------------------------
		} else if (arg0.getButton() == MouseEvent.BUTTON1) {
			dragType = isMouseUnderEvent(arg0.getX(), arg0.getY());
			prevTime = graph.getxAxis().getTimeFromPixel(arg0.getX());
			if (dragType != NOT_IN_RANGE) {
				if (dragType != NOT_UNDER_EVENT) {
					movingEvent = getEventUnderMouse(arg0.getX(), arg0.getY());
				} else {
					movingEvent = new Ams5fsPacket();
				}
			}
			graph.repaint();
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		if (isEditable == false)
			return;
		dragType = -1;
		movingEvent = null;
		graph.repaint();
		graph.getxAxis().updateAll();
	}

	public void setShowCode(boolean showCode) {
		this.showCode = showCode;
	}

	public void setEvents(ArrayList<Ams5fsPacket> events) {
		this.events = events;
	}

	public void setEditable(boolean editable) {
		this.isEditable = editable;
	}
}
