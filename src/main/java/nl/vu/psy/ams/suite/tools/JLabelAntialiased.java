package nl.vu.psy.ams.suite.tools;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JLabel;
/*
 * Simple class that forces a jlabel to be antialiased.
 * Used in the aboutdialog for instance
 */
public class JLabelAntialiased extends JLabel {
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	public JLabelAntialiased(String text) {
		super(text);
	}

	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		super.paintComponent(g2);
	}
}
