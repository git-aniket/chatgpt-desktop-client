package nl.vu.psy.ams.suite.gui.graphgen;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.gui.MainFrame;

public class GeneratedHRMOTDialog extends JDialog {

	/**
	 * Dialog that shows a generated HR+MOT graph, and has buttons to save it to
	 * a file.
	 */
	private static final long			serialVersionUID	= 1L;
	private GeneratedHRMOTGraphPanel	genGraph;

	public GeneratedHRMOTDialog() {
		super(MainFrame.getInstance().getMainFrame(), "Label Information Graph Generator", true);
		setLayout(new BorderLayout());
		genGraph = new GeneratedHRMOTGraphPanel();
		add(genGraph, BorderLayout.CENTER);
		JPanel butPanel = new JPanel();
		butPanel.setLayout(new BoxLayout(butPanel, BoxLayout.Y_AXIS));

		JButton but;

		but = new JButton("Set graph to first half of data");
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		but.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				double startTime = CurrentOpenData.getInstance().getStartTimeInUS();
				double endTime = CurrentOpenData.getInstance().getEndTimeInUS();
				genGraph.getxAxis().setTimes(startTime, (startTime + endTime) / 2.);
				genGraph.repaint();
			}
		});
		butPanel.add(but);

		but = new JButton("Set graph to second half of data");
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		but.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				double startTime = CurrentOpenData.getInstance().getStartTimeInUS();
				double endTime = CurrentOpenData.getInstance().getEndTimeInUS();
				genGraph.getxAxis().setTimes((startTime + endTime) / 2., endTime);
				genGraph.repaint();
			}
		});
		butPanel.add(but);

		but = new JButton("Change HRA average length");
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		but.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				final JDialog diag = new JDialog(GeneratedHRMOTDialog.this, "Set HRA average length", true);
				diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.X_AXIS));
				final JFormattedTextField ftf = new JFormattedTextField(60);
				int oldVal = (int) Math.round(genGraph.getHRDrawer().getAvLength() / 1000000.);
				ftf.setValue(Integer.valueOf(oldVal));
				diag.add(new JLabel("HRA average length (seconds):"));
				diag.add(ftf);
				ftf.setColumns(10);
				JButton but = new JButton("OK");
				but.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							ftf.commitEdit();
							double newVal = ((Integer) ftf.getValue()) * 1000000.;
							genGraph.getHRDrawer().setAvLength(newVal);
							diag.setVisible(false);
						} catch (ParseException e1) {
						}
					}
				});
				diag.add(but);

				but = new JButton("Cancel");
				but.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						diag.setVisible(false);
					}
				});
				diag.add(but);
				diag.pack();
				diag.setLocationRelativeTo(GeneratedHRMOTDialog.this);
				diag.setVisible(true);
			}
		});
		butPanel.add(but);

		but = new JButton("Change MOT average length");
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		but.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				final JDialog diag = new JDialog(GeneratedHRMOTDialog.this, "Set MOT average length", true);
				diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.X_AXIS));
				final JFormattedTextField ftf = new JFormattedTextField(60);
				int oldVal = (int) Math.round(genGraph.getMOTDrawer().getAvLength() / 1000000.);
				ftf.setValue(Integer.valueOf(oldVal));
				diag.add(new JLabel("MOT average length (seconds):"));
				diag.add(ftf);
				ftf.setColumns(10);
				JButton but = new JButton("OK");
				but.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							ftf.commitEdit();
							double newVal = ((Integer) ftf.getValue()) * 1000000.;
							genGraph.getMOTDrawer().setAvLength(newVal);
							genGraph.repaint();
							diag.setVisible(false);
						} catch (ParseException e1) {
						}
					}
				});
				diag.add(but);

				but = new JButton("Cancel");
				but.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						diag.setVisible(false);
					}
				});
				diag.add(but);
				diag.pack();
				diag.setLocationRelativeTo(GeneratedHRMOTDialog.this);
				diag.setVisible(true);
			}
		});
		butPanel.add(but);

		but = new JButton("Change HRA Axis Title");
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newString = JOptionPane.showInputDialog("Set HRA Axis Title", genGraph.getHRAAxisTitle());
				if (newString != null) {
					genGraph.setHRAAxisTitle(newString);
				}
			}
		});
		butPanel.add(but);

		but = new JButton("Change MOT Axis Title");
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newString = JOptionPane.showInputDialog("Set MOT Axis Title", genGraph.getMOTAxisTitle());
				if (newString != null) {
					genGraph.setMOTAxisTitle(newString);
				}
			}
		});
		butPanel.add(but);

		but = new JButton("Change X-Axis Title");
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String oldString = genGraph.getXAxisTitle();
				String newString;
				if (oldString != null) {
					newString = JOptionPane.showInputDialog("Set Graph Title (empty for default)", oldString);
				} else {
					newString = JOptionPane.showInputDialog("Set Graph Title (empty for default)");
				}
				if (newString != null) {
					genGraph.setXAxisTitle(newString);
				}
			}
		});
		butPanel.add(but);

		but = new JButton("Change Graph Title");
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newString = JOptionPane.showInputDialog("Set Graph Title (empty for subject ID)", genGraph.getGraphTitle());
				if (newString != null) {
					genGraph.setTitle(newString);
				}
			}
		});
		butPanel.add(but);

		JCheckBox drawTitleCB = new JCheckBox("Draw graph title");
		drawTitleCB.setSelected(false);
		drawTitleCB.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				genGraph.setDrawTitle(((JCheckBox) e.getSource()).isSelected());
				Dimension curSize = getSize();
				if (((JCheckBox) e.getSource()).isSelected()) {
					setSize(curSize.width, curSize.height + 50);
				} else {
					setSize(curSize.width, curSize.height - 50);
				}
				revalidate();
				genGraph.revalidate();
			}
		});
		drawTitleCB.setAlignmentX(Component.CENTER_ALIGNMENT);
		butPanel.add(drawTitleCB);

		butPanel.add(Box.createVerticalGlue());

		but = new JButton("Save");
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		but.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				genGraph.saveToFile();
			}
		});
		butPanel.add(but);

		but = new JButton("Close");
		but.setAlignmentX(Component.CENTER_ALIGNMENT);
		but.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		butPanel.add(but);

		add(butPanel, BorderLayout.EAST);

		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension scrnsize = toolkit.getScreenSize();
		Dimension frameSize = new Dimension(1280, 720);
		frameSize.width = (int) (3 * scrnsize.getWidth() / 4);
		frameSize.height = (int) (1 * scrnsize.getHeight() / 3);

		setBounds(0, 0, frameSize.width, frameSize.height);

		setLocationRelativeTo(MainFrame.getInstance().getMainFrame());

	}
}
