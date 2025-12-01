package nl.vu.psy.ams.suite.gui.settings;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.tools.ExitApp;

public class SettingsDialog extends JDialog implements TreeSelectionListener, ActionListener {

	/**
	 * Dialog that shows all setting catagories on the left in a tree, and the
	 * corresponding settingsPane of the selected catagory on the left
	 */
	private static final long				serialVersionUID	= 1L;
	private JTree							tree;
	private SettingsPane					rightPanel			= new EmptySettings();
	private HashMap<String, SettingsPane>	map					= new HashMap<String, SettingsPane>();
	private JSplitPane						sp;
	private boolean 						showExpertMode		= true;
	public SettingsDialog() {
		super(MainFrame.getInstance().getMainFrame(), "VU-DAMS Suite Settings", true);
		setLayout(new BorderLayout());
		setupTree();
		setupPanels();
		// add(new JScrollPane(tree), BorderLayout.WEST);
		sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(tree), rightPanel);
		// add(rightPanel,BorderLayout.CENTER);
		add(sp, BorderLayout.CENTER);
		JPanel butPanel = new JPanel();
		JButton restoreButton = new JButton("Restore Defaults and Exit");
		restoreButton.addActionListener(this);
		JButton saveButton = new JButton("Save Settings");
		saveButton.addActionListener(this);
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		butPanel.add(restoreButton);
		butPanel.add(saveButton);
		butPanel.add(cancelButton);
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
		JPanel textPanel = new JPanel();
		JLabel textLabel = new JLabel("Note: some settings will only take effect after restarting the VU-DAMS Suite.");
		textLabel.setHorizontalAlignment(SwingConstants.CENTER);
		textPanel.add(textLabel);
		bottomPanel.add(textPanel);
		bottomPanel.add(butPanel);

		add(bottomPanel, BorderLayout.SOUTH);
		setPreferredSize(new Dimension(1000, 675));
		pack();
		setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String ac = e.getActionCommand();
		if (ac.equals("Cancel")) {
			setVisible(false);
		} else if (ac.equals("Save Settings")) {
			for (SettingsPane sp : map.values()) {
				sp.save();
			}
			setVisible(false);
			MainFrame.getInstance().getMainFrame().repaint();
		} else if (ac.equals("Restore Defaults and Exit")) {
			int res = JOptionPane.showConfirmDialog(this, "Are you sure you want to restore the default settings?\nThis will exit the VU-DAMS Suite!",
					"Restore default settings", JOptionPane.YES_NO_OPTION);
			if (res == JOptionPane.NO_OPTION || res == JOptionPane.CLOSED_OPTION)
				return;
			AppSettings.getInstance().restoreDefaults();
			ExitApp.NormalExit();
		}

	}

	public void expandAll(JTree tree, boolean expand) {
		TreeNode root = (TreeNode) tree.getModel().getRoot();

		// Traverse tree from root
		expandAll(tree, new TreePath(root), expand);
	}

	private void expandAll(JTree tree, TreePath parent, boolean expand) {
		// Traverse children
		TreeNode node = (TreeNode) parent.getLastPathComponent();
		if (node.getChildCount() >= 0) {
			for (@SuppressWarnings("rawtypes")
			Enumeration e = node.children(); e.hasMoreElements();) {
				TreeNode n = (TreeNode) e.nextElement();
				TreePath path = parent.pathByAddingChild(n);
				expandAll(tree, path, expand);
			}
		}

		// Expansion or collapse must be done bottom-up
		if (expand) {
			tree.expandPath(parent);
		} else {
			tree.collapsePath(parent);
		}
	}

	private void setupPanels() {
		map.put("Settings", new EmptySettings());
		map.put("General", new GeneralSettings());
		map.put("QRS Detection", new QRSDetectionSettings());
		map.put("Label Data", new LabelDataSettings());
		map.put("SCL Data", new EventDataSettings());
		map.put("Label Information", new LabelInfoSettings());
		map.put("Graph Generation", new GenGraphSettings());
		//map.put("Frequency Analysis", new FrequencySettings());
		map.put("Respiration Scoring", new RSASettings());
		if(showExpertMode == true){
			map.put("Expert Mode", new ExpertModeSettings());
		}
	}

	private void setupTree() {
		DefaultMutableTreeNode top = new DefaultMutableTreeNode("Settings");
		DefaultMutableTreeNode temp;

		temp = new DefaultMutableTreeNode("General");
		top.add(temp);
		top.add(new DefaultMutableTreeNode("QRS Detection"));
		top.add(new DefaultMutableTreeNode("Label Data"));
		top.add(new DefaultMutableTreeNode("SCL Data"));
		temp = new DefaultMutableTreeNode("Label Information");
		temp.add(new DefaultMutableTreeNode("Graph Generation"));
		top.add(temp);
		//top.add(new DefaultMutableTreeNode("Frequency Analysis"));
		top.add(new DefaultMutableTreeNode("Respiration Scoring"));
		
		if(showExpertMode == true){
			top.add(new DefaultMutableTreeNode("Expert Mode"));
		}
		
		
		tree = new JTree(top);
		expandAll(tree, true);
		DefaultTreeCellRenderer render = new DefaultTreeCellRenderer();
		render.setLeafIcon(null);
		render.setOpenIcon(null);
		render.setClosedIcon(null);
		tree.setCellRenderer(render);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
	}
	@Override
	public void valueChanged(TreeSelectionEvent arg0) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

		if (node == null)
			// Nothing is selected.
			return;

		String nodeInfo = (String) node.getUserObject();
		sp.remove(rightPanel);
		rightPanel = map.get(nodeInfo);
		sp.setRightComponent(rightPanel);
		revalidate();
		repaint();
	}
}
