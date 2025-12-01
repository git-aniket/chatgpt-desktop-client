package nl.vu.psy.ams.suite.gui.graphs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import javax.swing.BorderFactory;
import javax.swing.Box.Filler;
import javax.swing.JMenuItem;
import javax.swing.event.MouseInputListener;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import nl.vu.psy.ams.suite.gui.axes.XAxis;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.drawing.DataDrawer;
import nl.vu.psy.ams.suite.gui.graphs.overlays.Overlay;

public class Graph extends JPanel implements MouseWheelListener, MouseInputListener, ActionListener {

	/**
	 * Class that represents a graph. A graph always has a connected XAxis,
	 * which defines the left and right time of the graph. A graph can be
	 * connected to a DataDrawer, which defines what is drawn in the graph. An
	 * active Y-Axis can be set for a graph. Furthermore, a graph can have
	 * multiple overlays and underlays, that can draw additional information
	 * (for instance, event lines),and/or provide mouse input (for instance,
	 * editing beats in Beat detection tab).
	 */
	private static final long serialVersionUID = 1L;
	private XAxis xAxis;
	private YAxis activeYAxis;
	private YAxis yAxis;
	private JPanel panel = new JPanel(new BorderLayout());
	private ArrayList<DataDrawer> drawers = new ArrayList<DataDrawer>();
	private ArrayList<Overlay> overlays = new ArrayList<Overlay>();
	private ArrayList<Overlay> underlays = new ArrayList<Overlay>();
	private YAxis secondyAxis;
	private String name;
	private JPopupMenu popupMenu;
	private JMenuItem hideRawItem, hideFiltItem;

	public Graph(Color borderColor) {
		panel.add(this, BorderLayout.CENTER);
		this.setBackground(Color.WHITE);
		setBorder(BorderFactory.createLineBorder(borderColor));
		this.addMouseWheelListener(this);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);

		popupMenu = new JPopupMenu();
		hideRawItem = new JMenuItem("Hide Raw Data");
		hideRawItem.addActionListener(this);
		popupMenu.add(hideRawItem);
		hideFiltItem = new JMenuItem("Hide Filtered Data");
		hideFiltItem.addActionListener(this);
		popupMenu.add(hideFiltItem);
	}

	public Graph() {
		this(Color.BLACK);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource() == hideRawItem) {
			drawers.get(0).showorhide();
			String txt = hideRawItem.getText();
			if (txt.startsWith("Hide"))
				hideRawItem.setText("Show Raw Data");
			if (txt.startsWith("Show"))
				hideRawItem.setText("Hide Raw Data");
		}
		if (arg0.getSource() == hideFiltItem) {
			drawers.get(1).showorhide();
			String txt = hideFiltItem.getText();
			if (txt.startsWith("Hide"))
				hideFiltItem.setText("Show Filtered Data");
			if (txt.startsWith("Show"))
				hideFiltItem.setText("Hide Filtered Data");
		}
	}

	public ArrayList<Integer> getXMajorTicks() {
		return this.getxAxis().getXMajorTicks();
	}

	public ArrayList<Integer> getYMajorTicks() {
		return this.getActiveYAxis().getYMajorTicks();
	}

	public Graph(XAxis xAxis) {
		this();
		this.xAxis = xAxis;
		xAxis.connectToGraph(this);
	}

	public Graph(XAxis xAxis, Color borderColor) {
		this(borderColor);
		this.xAxis = xAxis;
		xAxis.connectToGraph(this);
	}

	public void addDataDrawer(DataDrawer drawer) {
		drawers.add(drawer);
	}

	public void removeDataDrawer(DataDrawer drawer) {
		drawers.remove(drawer);
	}

	public void addOverlay(Overlay overlay) {
		overlays.add(overlay);
	}

	public void removeOverlay(Overlay overlay) {
		overlays.remove(overlay);
	}

	public void addUnderlay(Overlay overlay) {
		underlays.add(overlay);
	}

	public void autoScale() {
		if (activeYAxis != null) {
			activeYAxis.autoScale();
		}
		if (yAxis != null) {
			yAxis.autoScale();
		}

	}

	public void defaultScale() {
		if (activeYAxis != null) {
			activeYAxis.defaultScale();
		}
		if (yAxis != null) {
			yAxis.defaultScale();
		}

	}

	public void autoScale(int d) {
		if (activeYAxis != null) {
			activeYAxis.autoScale(d);
		}
		if (yAxis != null) {
			yAxis.autoScale(d);
		}

	}

	public void autoScale(boolean forceDebugMinMax) {
		if (activeYAxis != null) {
			activeYAxis.autoScale(forceDebugMinMax);
		}
		if (yAxis != null) {
			yAxis.autoScale(forceDebugMinMax);
		}
	}

	public void autoScaleFast() {
		if (activeYAxis != null) {
			activeYAxis.autoScaleFast();
		}
		if (yAxis != null) {
			yAxis.autoScale();
		}
	}

	public ArrayList<DataDrawer> getDrawers() {
		return drawers;
	}

	public JPanel getPanel() {
		return panel;
	}

	@Override
	public String getToolTipText(MouseEvent e) {
		String toolText;
		for (Overlay over : overlays) {
			toolText = over.getToolTipText(e);
			if (toolText != null)
				return toolText;
		}
		for (Overlay under : underlays) {
			toolText = under.getToolTipText(e);
			if (toolText != null)
				return toolText;
		}
		return super.getToolTipText(e);
	}

	public XAxis getxAxis() {
		return xAxis;
	}

	public YAxis getyAxis() {
		return yAxis;
	}

	public YAxis getActiveYAxis() {
		return activeYAxis;
	}

	public String getName() {
		return name;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0) {
		if (getxAxis() != null) {
			int wheelRotation = arg0.getWheelRotation();
			double time = getxAxis().getTimeFromPixel(arg0.getX());
			// String OSname = System.getProperty("os.name");

			// if (arg0.getModifiersEx() == 0) && !OSname.contains("Mac"))
			if (wheelRotation < 0) {
				getxAxis().moveAndZoomToTime(time, 2);
				// getxAxis().zoom(2);
			} else if (wheelRotation > 0) {
				getxAxis().moveAndZoomToTime(time, 0.5);
				// getxAxis().zoom(0.5);
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
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		try {
			for (Overlay under : underlays)
				under.draw(g2);
			for (DataDrawer drawer : drawers)
				drawer.draw(g2);
			for (Overlay over : overlays)
				over.draw(g2);
		} catch (ConcurrentModificationException e) {
			e.printStackTrace();

		}
	}

	public void setActiveYAxis(YAxis yAxis) {
		if (activeYAxis != null)
			panel.remove(activeYAxis);
		activeYAxis = yAxis;
		panel.add(yAxis, BorderLayout.WEST);
	}

	public void setYAxis(YAxis yAxis1) {
		if (yAxis != null)
			panel.remove(yAxis);
		yAxis = yAxis1;
		panel.add(yAxis1, BorderLayout.WEST);
	}

	public void setEmptyYAxis() {
		Filler filler = new Filler(new Dimension(50, 1), new Dimension(50, 50), new Dimension(50, 50));
		filler.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		panel.add(filler, BorderLayout.WEST);
	}

	public void setSecondYAxis(YAxis yAxis) {
		if (secondyAxis != null) {
			panel.remove(secondyAxis);
		}
		this.secondyAxis = yAxis;
		panel.add(secondyAxis, BorderLayout.EAST);
	}

	public void setxAxis(XAxis xAxis) {
		if (this.xAxis != null) {
			this.xAxis.removeGraph(this);
		}
		this.xAxis = xAxis;
		xAxis.connectToGraph(this);
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		if (arg0.getButton() == MouseEvent.BUTTON3 || arg0.isControlDown()) { // right click
			maybeShowPopup(arg0);
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		if (arg0.getButton() != MouseEvent.BUTTON1) {
			maybeShowPopup(arg0);
		}
	}

	private void maybeShowPopup(MouseEvent e) {
		if (e.isPopupTrigger() && drawers.size() > 1) {
			popupMenu.show(this, e.getX(), e.getY());
		}
	}
}
