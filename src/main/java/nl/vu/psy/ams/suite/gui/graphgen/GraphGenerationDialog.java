package nl.vu.psy.ams.suite.gui.graphgen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import nl.vu.psy.ams.suite.gui.CombinedLabelEditor;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.SaveDialog;

public class GraphGenerationDialog extends JDialog implements ActionListener {

	/**
	 * Dialog that shows a generated bar graph, and has buttons to save it to a
	 * file.
	 */
	private static final long serialVersionUID = 1L;
	private GeneratedGraphPanel gengraph;

	public GraphGenerationDialog() {
		super(MainFrame.getInstance().getMainFrame(), "Label Information Graph Generator", true);
		setLayout(new BorderLayout());
		gengraph = new GeneratedGraphPanel();
		add(gengraph, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		JButton setTit = new JButton("Change Graph Title");
		setTit.addActionListener(this);
		setTit.setAlignmentX(Component.CENTER_ALIGNMENT);

		JButton setXTit = new JButton("Change X Axis Title");
		setXTit.addActionListener(this);
		setXTit.setAlignmentX(Component.CENTER_ALIGNMENT);

		JButton setYTit = new JButton("Change Y Axis Title");
		setYTit.addActionListener(this);
		setYTit.setAlignmentX(Component.CENTER_ALIGNMENT);

		JButton combBut = new JButton("Edit Combined Labels");
		combBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CombinedLabelEditor diag = new CombinedLabelEditor();
				diag.setVisible(true);
				gengraph.recalcAverages();
				gengraph.repaint();
			}
		});
		combBut.setAlignmentX(Component.CENTER_ALIGNMENT);

		buttonPanel.add(setTit);
		buttonPanel.add(setXTit);
		buttonPanel.add(setYTit);
		buttonPanel.add(combBut);
		buttonPanel.add(Box.createVerticalGlue());

		JButton saveButton = new JButton("Save to file");
		saveButton.addActionListener(this);
		saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(this);
		closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		buttonPanel.add(saveButton);
		buttonPanel.add(closeButton);

		buttonPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

		add(buttonPanel, BorderLayout.EAST);

		pack();
		setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String ac = e.getActionCommand();
		if (ac.equals("Close")) {
			setVisible(false);
		} else if (ac.equals("Change Graph Title")) {
			String ret = JOptionPane.showInputDialog(this, "Enter the new graph title:");
			if (ret != null)
				gengraph.setGraphTitle(ret);
		} else if (ac.equals("Change X Axis Title")) {
			String ret = JOptionPane.showInputDialog(this, "Enter the new X axis title:");
			if (ret != null)
				gengraph.setXAxisTitle(ret);
		} else if (ac.equals("Change Y Axis Title")) {
			String ret = JOptionPane.showInputDialog(this, "Enter the new Y axis title:");
			if (ret != null)
				gengraph.setYAxisTitle(ret);
		} else if (ac.equals("Save to file")) {
			BufferedImage bi = new BufferedImage(gengraph.getWidth(), gengraph.getHeight(),
					BufferedImage.TYPE_INT_ARGB);
			Graphics2D ig2 = bi.createGraphics();
			gengraph.drawGraph(ig2);
			File retFile = SaveDialog.saveFileBrowserWithAddition("png", "_LABELS", "HRA Bar graph");
			if (retFile != null) {
				try {
					ImageIO.write(bi, "PNG", retFile);
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(this, "Error saving image", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}
}
