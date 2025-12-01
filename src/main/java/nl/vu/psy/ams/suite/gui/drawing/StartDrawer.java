package nl.vu.psy.ams.suite.gui.drawing;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class StartDrawer extends DataDrawer {

	public StartDrawer() {
		super();
	}

	@Override
	public void drawData(Graphics2D g) {
		g.setColor(Color.BLACK);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		String startText = "Go to File -> Open/Import Data or Connect to get started";
		g.setFont(new Font("Arial", Font.PLAIN, 15));
		FontMetrics met = g.getFontMetrics();
		int strWidth = met.stringWidth(startText);
		int strHeight = met.getMaxAscent();
		g.drawString(startText, (float) (graph.getWidth() / 2. - strWidth / 2.),
				(float) (graph.getHeight() / 2. + strHeight / 2.));
	}

	@Override
	public double[] getBounds() {
		return null;
	}

	public double getAverageBetweenTimes(double lTime, double rTime) {
		return Double.NaN;
	}

	public double getStdDevBetweenTimes(double lTime, double rTime) {
		return Double.NaN;
	}
}
