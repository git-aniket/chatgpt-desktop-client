package nl.vu.psy.ams.suite.gui.drawing;

import java.awt.Color;
import java.awt.Graphics2D;

import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.graphs.Graph;

/*
 * Generic class that can be added to a graph in order
 * to draw things. If you want to draw a kind of signal in a
 * graph, this class should be extended, and the abstract methods
 * implemented.
 * Can also be used to get the current bounds of a graph (eg max value
 * and min value), which is used for autoscaling a graph.
 */
public abstract class DataDrawer {

	protected YAxis yAxis;
	protected Color color;
	protected Graph graph;
	protected String name;
	protected Double average = 0.0;
	private boolean showpoints = true;

	public DataDrawer() {
		this("", null, Color.BLACK);
	}

	public DataDrawer(String name, YAxis yAxis) {
		this(name, yAxis, Color.BLACK);
	}

	public DataDrawer(String name, YAxis yAxis, Color color) {
		this.yAxis = yAxis;
		if (yAxis != null)
			yAxis.connectToDrawer(this);
		this.color = color;
		this.name = name;
	}

	public void connectToGraph(Graph graph) {
		this.graph = graph;
		graph.addDataDrawer(this);
	}

	public void disconnectFromGraph(Graph graph) {
		this.graph = graph;
		graph.removeDataDrawer(this);
	}

	public void draw(Graphics2D g) {
		if (showpoints)
			drawData(g);
	}

	public abstract void drawData(Graphics2D g);

	public abstract double[] getBounds();

	public double[] getBounds(boolean forceDebugMinMax) {
		// JdH This shouldn't be called, but has to exist???
		new Throwable().printStackTrace();
		return new double[2];
	}

	public abstract double getAverageBetweenTimes(double lTime, double rTime);

	public abstract double getStdDevBetweenTimes(double lTime, double rTime);

	public Color getColor() {
		return color;
	}

	public Graph getGraph() {
		return graph;
	}

	public YAxis getYAxis() {
		return yAxis;
	}

	public void setGraph(Graph graph2) {
		this.graph = graph2;
	}

	public void setYAxis(YAxis yAxis) {
		this.yAxis = yAxis;
		yAxis.setDrawer(this);
	}

	public void setAverage(double av) {
		this.average = av;
	}

	public int getCurTick() {
		return 0;
	}

	public String getName() {
		return name;
	}

	public void showorhide() {
		if (showpoints) {
			showpoints = false;
		} else {
			showpoints = true;
		}
		graph.repaint();
	}

	public double getValueAtTime(double time) {
		return 0;
	}
}
