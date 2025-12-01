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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.SaveDialog;
import nl.vu.psy.ams.suite.gui.tabs.qrs.QRSTab;

public class GenerateEctopicBeatsDialog extends JDialog implements ActionListener{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6100874583825002303L;
	private GenerateEctopicBeatsGraph	gengraph;
	private JLabel 						countofectopicbeats;
	
	public  GenerateEctopicBeatsDialog(){
		super(MainFrame.getInstance().getMainFrame(), "Ectopic beats", true);
		setLayout(new BorderLayout());
		
		gengraph = new GenerateEctopicBeatsGraph();
		add(gengraph, BorderLayout.CENTER);
		
		countofectopicbeats = new JLabel();
		countofectopicbeats.setText("Ectopic beats No:_ " +String.valueOf(gengraph.getDrawer().getSelPart()+1)+ " " + "of" +" " +gengraph.getDrawer().getTotalectopicbeatsCount());
		countofectopicbeats.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		JButton setTit = new JButton("Next Ectopic beat");
		setTit.addActionListener(this);
		setTit.setAlignmentX(Component.CENTER_ALIGNMENT);

		JButton setXTit = new JButton("Previous Ectopic beat");
		setXTit.addActionListener(this);
		setXTit.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		JButton template = new JButton("Delete Ectopic beat");
		template.addActionListener(this);
		template.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		buttonPanel.add(countofectopicbeats);
		buttonPanel.add(setTit);
		buttonPanel.add(setXTit);
		buttonPanel.add(template);
		
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
		} else if (ac.equals("Next Ectopic beat")) {
			gengraph.getDrawer().nextPart();
			countofectopicbeats.setText("Ectopic beat No:_ " +String.valueOf(gengraph.getDrawer().getSelPart()+1)+  " " + "of" +" " +gengraph.getDrawer().getTotalectopicbeatsCount());
			repaint();
		} else if (ac.equals("Previous Ectopic beat")) {
			gengraph.getDrawer().prevPart();
			countofectopicbeats.setText("Ectopic beat No:_ " +String.valueOf(gengraph.getDrawer().getSelPart()+1)+  " " + "of" +" " +gengraph.getDrawer().getTotalectopicbeatsCount());
			repaint();
		}else if (ac.equals("Delete Ectopic beat")) {			
			gengraph.getDrawer().delete();
			if(gengraph.getDrawer().getSelPart() < gengraph.getDrawer().getTotalectopicbeatsCount()){
				countofectopicbeats.setText("Ectopic beat No:_ " +String.valueOf(gengraph.getDrawer().getSelPart()+1)+  " " + "of" +" " +gengraph.getDrawer().getTotalectopicbeatsCount());				
			}else{
				countofectopicbeats.setText("Ectopic beat No:_ " +String.valueOf(gengraph.getDrawer().getSelPart())+  " " + "of" +" " +gengraph.getDrawer().getTotalectopicbeatsCount());
			}
			QRSTab.getInstance().repaint();		
			repaint();						
		} else if (ac.equals("Save to file")) {
			BufferedImage bi = new BufferedImage(gengraph.getWidth(), gengraph.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D ig2 = bi.createGraphics();
			gengraph.getDrawer().drawData(ig2);
			File retFile = SaveDialog.saveFileBrowserWithAddition("png", "_ectopicbeats", "Ectopic Beats");
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
