package nl.vu.psy.ams.suite.gui.graphs.overlays;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Vector;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.gui.drawing.BinaryDataDrawer;
import nl.vu.psy.ams.suite.gui.drawing.BinaryDoubleDataDrawer;
import nl.vu.psy.ams.suite.gui.drawing.DataDrawer;
import nl.vu.psy.ams.suite.gui.drawing.FormulaDrawer;
import nl.vu.psy.ams.suite.gui.drawing.HRTachogramDrawer;
import nl.vu.psy.ams.suite.gui.drawing.RSADrawer;
import nl.vu.psy.ams.suite.gui.drawing.RespTachogramDrawer;
import nl.vu.psy.ams.suite.gui.drawing.RespirationRateDrawer;
import nl.vu.psy.ams.suite.gui.drawing.TotalMotilityDrawer;
import nl.vu.psy.ams.suite.gui.graphs.Graph;
import nl.vu.psy.ams.suite.gui.tabs.inspect.InspectTab;
import nl.vu.psy.ams.suite.gui.tabs.inspect.YawPitchRollVisualizer;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

/*
 * Shows the time of the mouse position.
 */
public class TimeShowOverlay extends Overlay {

	int prevX, prevY, newX, newY;
	double newXVal, prevXVal, newYVal, prevYVal;
	boolean isDragging = false, drawBox = false;
	private Vector<TimeShowOverlay> connectedOverlays;
	private YawPitchRollVisualizer vis;

	public TimeShowOverlay(Graph graph) {
		super(graph, true);
	}

	public TimeShowOverlay(Graph graph, YawPitchRollVisualizer vis) {
		super(graph, true);
		this.vis = vis;
	}

	public void setConnectedOverlays(Vector<TimeShowOverlay> overlays) {
		this.connectedOverlays = overlays;
	}

	protected void setNewX(int newX) {
		this.newX = newX;
	}

	protected void setPrevX(int prevX) {
		this.prevX = prevX;
	}

	protected void setDragging(boolean isDragging) {
		this.isDragging = isDragging;
	}

	protected void setDrawBox(boolean drawBox) {
		this.drawBox = drawBox;
	}

	public Graph getGraph() {
		return graph;
	}

	@Override
	public void draw(Graphics2D g) {
		if (isDragging) {
			g.setColor(Color.BLACK);
			// hairline
			g.drawLine(newX, 0, newX, graph.getHeight());
			g.drawLine(prevX, 0, prevX, graph.getHeight());
			if (drawBox) {
				if (connectedOverlays != null) {
					for (TimeShowOverlay tsOverlay : connectedOverlays) {
						if (tsOverlay != this) {
							tsOverlay.setNewX(newX);
							tsOverlay.setPrevX(prevX);
							tsOverlay.setDragging(true);
							tsOverlay.setDrawBox(false);
							tsOverlay.getGraph().repaint();
						}
					}
				}
				// box
				g.drawLine(prevX, prevY, newX, prevY);
				g.drawLine(newX, prevY, newX, newY);
				g.drawLine(newX, newY, prevX, newY);
				g.drawLine(prevX, newY, prevX, prevY);
				String lTxt;
				String rTxt;
				String diffXTxt;
				int w = graph.getWidth();
				int h = graph.getHeight();
				newXVal = graph.getxAxis().getTimeFromPixel(newX);
				prevXVal = graph.getxAxis().getTimeFromPixel(prevX);
				double diffXVal = Math.abs(newXVal - prevXVal);
				DecimalFormat decFormat = new DecimalFormat("#####.###", new DecimalFormatSymbols(Locale.of("us")));
				DecimalFormat intFormat = new DecimalFormat("########", new DecimalFormatSymbols(Locale.of("us")));
				if (graph.getxAxis().getIsTicks()) { // ticks x-axis
					rTxt = intFormat.format(newXVal / 1000.0);
					lTxt = intFormat.format(prevXVal / 1000.0);
				} else if (!graph.getxAxis().getTimeFormatSeconds().isSelected()) { // datetime x-axis
					rTxt = Utils.getTimeFromUS(newXVal);
					lTxt = Utils.getTimeFromUS(prevXVal);
				} else if (graph.getxAxis().getTrueSeconds()) { // seconds x-axis
					if (!graph.getxAxis().getIsLive()) {
						newXVal -= CurrentOpenData.getInstance().getStartTimeInUS();
						prevXVal -= CurrentOpenData.getInstance().getStartTimeInUS();
					}
					rTxt = decFormat.format(newXVal / 1000000.0);
					lTxt = decFormat.format(prevXVal / 1000000.0);
				} else { // milliseconds x-axis
					if (!graph.getxAxis().getIsLive()) {
						newXVal -= CurrentOpenData.getInstance().getStartTimeInUS();
						prevXVal -= CurrentOpenData.getInstance().getStartTimeInUS();
					}
					rTxt = decFormat.format(newXVal / 1000.0);
					lTxt = decFormat.format(prevXVal / 1000.0);
				}
				diffXVal /= 1000.0; // us to ms
				if (diffXVal > 300000)
					diffXTxt = " (" + decFormat.format(diffXVal / (1000.0 * 60.0)) + " min)";
				else if (diffXVal > 10000)
					diffXTxt = " (" + decFormat.format(diffXVal / 1000.0) + " s)";
				else
					diffXTxt = " (" + intFormat.format(diffXVal) + " ms)";
				newYVal = graph.getActiveYAxis().getValueFromPixel(newY);
				prevYVal = graph.getActiveYAxis().getValueFromPixel(prevY);
				double diffYVal = Math.abs(newYVal - prevYVal);
				String bTxt = String.format("%.4g", graph.getActiveYAxis().getRealValueFromSampleValue(newYVal));
				String tTxt = String.format("%.4g", graph.getActiveYAxis().getRealValueFromSampleValue(prevYVal));
				String diffYTxt = " ("
						+ String.format("%.4g", graph.getActiveYAxis().getRealValueFromSampleValue(diffYVal)) + ")";
				// get average and sd
				double av, sd;
				boolean useFiltered = false;
				if (!(graph.getDrawers().get(0) instanceof BinaryDataDrawer)
						&& !(graph.getDrawers().get(0) instanceof RespirationRateDrawer)
						&& !(graph.getDrawers().get(0) instanceof HRTachogramDrawer)
						&& !(graph.getDrawers().get(0) instanceof RSADrawer)
						&& !(graph.getDrawers().get(0) instanceof RespTachogramDrawer)
						&& !(graph.getDrawers().get(0) instanceof BinaryDoubleDataDrawer)
						&& !(graph.getDrawers().get(0) instanceof TotalMotilityDrawer)) {
					prevXVal /= 1000.0;
					newXVal /= 1000.0;
				}
				if ((graph.getDrawers().get(0) instanceof FormulaDrawer)) {
					prevXVal *= 1000.0;
					newXVal *= 1000.0;
				}
				DataDrawer draw = graph.getDrawers().get(0);
				if (prevXVal < newXVal) {
					av = draw.getAverageBetweenTimes(prevXVal, newXVal);
					sd = draw.getStdDevBetweenTimes(prevXVal, newXVal);
				} else {
					av = draw.getAverageBetweenTimes(newXVal, prevXVal);
					sd = draw.getStdDevBetweenTimes(newXVal, prevXVal);
				}
				if (graph.getDrawers().size() > 1) {
					double avf, sdf;
					DataDrawer drawf = graph.getDrawers().get(1);
					if (prevXVal < newXVal) {
						avf = drawf.getAverageBetweenTimes(prevXVal, newXVal);
						sdf = drawf.getStdDevBetweenTimes(prevXVal, newXVal);
					} else {
						avf = drawf.getAverageBetweenTimes(newXVal, prevXVal);
						sdf = drawf.getStdDevBetweenTimes(newXVal, prevXVal);
					}
					double realVal = graph.getActiveYAxis().getRealValueFromSampleValue(prevYVal);
					if (Math.abs(realVal - avf) < Math.abs(realVal - av)) {
						useFiltered = true;
						sd = sdf;
						av = avf;
					}
				}
				String avTxt = "mean: " + String.format("%.4g", av);
				String sdTxt = "sd: " + String.format("%.4g", sd);

				if (prevY < newY) {
					bTxt = bTxt + diffYTxt;
				} else {
					tTxt = tTxt + diffYTxt;
				}
				if (prevX < newX) {
					rTxt = rTxt + diffXTxt;
				} else {
					lTxt = lTxt + diffXTxt;
				}

				FontMetrics metric = g.getFontMetrics();

				Rectangle2D bounds = metric.getStringBounds(lTxt, g);
				double height = bounds.getHeight();
				double width = bounds.getWidth();
				int yPos = 0;
				int xPos = 0;
				if (prevY < newY) {
					yPos = (int) Math.round(prevY + (newY - prevY) / 2 - height / 2);
				} else {
					yPos = (int) Math.round(newY + (prevY - newY) / 2 - height / 2);
				}
				if (prevX < newX) {
					if (prevX > width) {
						xPos = (int) Math.round(prevX - width);
					} else
						xPos = prevX + 1;
				} else {
					if (prevX < w - width) {
						xPos = prevX + 1;
					} else {
						xPos = (int) Math.round(prevX - width);
						yPos += height;
					}
				}
				g.setColor(Color.WHITE);
				g.fillRect(xPos, yPos, (int) Math.round(width), (int) Math.round(height));
				g.setColor(Color.BLACK);
				g.drawRect(xPos, yPos, (int) Math.round(width), (int) Math.round(height));
				g.drawString(lTxt, xPos, yPos + (int) height - 3);

				bounds = metric.getStringBounds(rTxt, g);
				height = bounds.getHeight();
				width = bounds.getWidth();
				if (prevY < newY) {
					yPos = (int) Math.round(prevY + (newY - prevY) / 2 - height / 2);
				} else {
					yPos = (int) Math.round(newY + (prevY - newY) / 2 - height / 2);
				}
				if (prevX >= newX) {
					if (newX > width) {
						xPos = (int) Math.round(newX - width);
					} else
						xPos = newX + 1;
				} else {
					if (newX < w - width) {
						xPos = newX + 1;
					} else {
						xPos = (int) Math.round(newX - width);
						yPos += height;
					}
				}
				g.setColor(Color.WHITE);
				g.fillRect(xPos, yPos, (int) Math.round(width), (int) Math.round(height));
				g.setColor(Color.BLACK);
				g.drawRect(xPos, yPos, (int) Math.round(width), (int) Math.round(height));
				g.drawString(rTxt, xPos, yPos + (int) height - 3);

				bounds = metric.getStringBounds(bTxt, g);
				height = bounds.getHeight();
				width = bounds.getWidth();
				if (prevX < newX) {
					xPos = (int) Math.round(prevX + (newX - prevX) / 2 - width / 2);
				} else {
					xPos = (int) Math.round(newX + (prevX - newX) / 2 - width / 2);
				}
				if (prevY >= newY) {
					if (newY > height)
						yPos = (int) Math.round(newY - height);
					else
						yPos = newY + 1;
				} else {
					height *= 3;
					if (newY < h - height)
						yPos = newY + 1;
					else
						yPos = (int) Math.round(newY - height);
				}
				g.setColor(Color.WHITE);
				g.fillRect(xPos, yPos, (int) Math.round(width), (int) Math.round(height));
				g.setColor(Color.BLACK);
				g.drawRect(xPos, yPos, (int) Math.round(width), (int) Math.round(height));
				if (prevY < newY) {
					g.drawString(bTxt, xPos, yPos + (int) (height / 3) - 3);
					if (useFiltered)
						g.setColor(new Color(
								AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR)));
					g.drawString(avTxt, xPos, yPos + 2 * (int) (height / 3) - 3);
					g.drawString(sdTxt, xPos, yPos + 3 * (int) (height / 3) - 3);
					if (useFiltered)
						g.setColor(Color.BLACK);
				} else {
					g.drawString(bTxt, xPos, yPos + (int) height - 3);
				}

				bounds = metric.getStringBounds(tTxt, g);
				height = bounds.getHeight();
				width = bounds.getWidth();
				if (prevX < newX) {
					xPos = (int) Math.round(prevX + (newX - prevX) / 2 - width / 2);
				} else {
					xPos = (int) Math.round(newX + (prevX - newX) / 2 - width / 2);
				}
				if (prevY < newY) {
					if (prevY > height)
						yPos = (int) Math.round(prevY - height);
					else
						yPos = prevY + 1;
				} else {
					height *= 3;
					if (prevY < h - height)
						yPos = prevY + 1;
					else
						yPos = (int) Math.round(prevY - height);
				}
				g.setColor(Color.WHITE);
				g.fillRect(xPos, yPos, (int) Math.round(width), (int) Math.round(height));
				g.setColor(Color.BLACK);
				g.drawRect(xPos, yPos, (int) Math.round(width), (int) Math.round(height));
				if (prevY >= newY) {
					g.drawString(tTxt, xPos, yPos + (int) (height / 3) - 3);
					if (useFiltered)
						g.setColor(new Color(
								AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR)));
					g.drawString(avTxt, xPos, yPos + 2 * (int) (height / 3) - 3);
					g.drawString(sdTxt, xPos, yPos + 3 * (int) (height / 3) - 3);
					if (useFiltered)
						g.setColor(Color.BLACK);
				} else {
					g.drawString(tTxt, xPos, yPos + (int) height - 3);
				}
			}
		}
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		if (isDragging) {
			newX = arg0.getX();
			newY = arg0.getY();
			double time = graph.getxAxis().getTimeFromPixel(newX);
			if (vis == null) {
				if (InspectTab.getInstance().getVisualizer() != null)
					InspectTab.getInstance().getVisualizer().setRotation(time);
			} else
				vis.setRotation(time);
			graph.repaint();
		}
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		if (arg0.getButton() == MouseEvent.BUTTON1) {
			if (!isDragging) {
				prevX = arg0.getX();
				prevY = arg0.getY();
				double time = graph.getxAxis().getTimeFromPixel(prevX);
				if (vis == null) {
					if (InspectTab.getInstance().getVisualizer() != null)
						InspectTab.getInstance().getVisualizer().setRotation(time);
				} else
					vis.setRotation(time);
				newX = prevX;
				newY = prevY;
				isDragging = true;
			} else {
				boolean xInBoundaries = false, yInBoundaries = false;
				if (newX > prevX) {
					if (arg0.getX() > prevX && arg0.getX() < newX)
						xInBoundaries = true;
				} else {
					if (arg0.getX() > newX && arg0.getX() < prevX)
						xInBoundaries = true;
				}
				if (newY > prevY) {
					if (arg0.getY() > prevY && arg0.getY() < newY)
						yInBoundaries = true;
				} else {
					if (arg0.getY() > newY && arg0.getY() < prevY)
						yInBoundaries = true;
				}
				if (xInBoundaries && yInBoundaries) {
					if (newXVal > prevXVal)
						graph.getxAxis().setTimes(prevXVal, newXVal);
					else if (newXVal < prevXVal)
						graph.getxAxis().setTimes(newXVal, prevXVal);
					if (newYVal > prevYVal)
						graph.getActiveYAxis().setValues(prevYVal, newYVal);
					else if (newYVal < prevYVal)
						graph.getActiveYAxis().setValues(newYVal, prevYVal);
					graph.getxAxis().updateAll();
				}
				isDragging = false;
				graph.repaint();
			}
			drawBox = true;
			if (connectedOverlays != null) {
				for (TimeShowOverlay tsOverlay : connectedOverlays) {
					if (tsOverlay != this) {
						tsOverlay.setDragging(false);
						tsOverlay.setDrawBox(false);
						tsOverlay.getGraph().repaint();
					}
				}
			}
		}
	}
}
