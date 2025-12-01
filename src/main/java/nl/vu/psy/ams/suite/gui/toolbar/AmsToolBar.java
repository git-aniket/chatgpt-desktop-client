package nl.vu.psy.ams.suite.gui.toolbar;

import java.awt.Color;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
/*
 * Base abstract class to implement a toolbar. Also updates
 * the action menu. If you want to add a toolbar to a AmsTab,
 * extend this class and implement addButtons(). Use setupButton
 * to add a button to the toolbar and action menu, and use addNewSeparator to
 * add a separator to the toolbar.
 */
public abstract class AmsToolBar extends JToolBar implements ActionListener {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	private JMenu				actionMenu			= new JMenu("Actions");

	public AmsToolBar() {
		super();
		this.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		this.setFloatable(false);
	}

	public abstract void addButtons();

	protected void addNewSeparator() {
		this.addSeparator();
		actionMenu.addSeparator();
	}

	public void clearAllButtons() {
		actionMenu.removeAll();
		this.removeAll();
	}

	public JMenu getActionMenu() {
		return actionMenu;
	}

	protected void setupButton(String iconName, String text, String actionCommand, KeyStroke hotKey) {
		JButton button = new JButton(new ImageIcon(getClass().getResource("/img/" + iconName + ".png")));
		button.setToolTipText(text + " (" + hotKey.toString().replaceAll("released ", "") + ")");
		button.setActionCommand(actionCommand);
		button.addActionListener(this);
		button.setFocusable(false);
		this.add(button);
		JMenuItem menuItem = new JMenuItem(text);
		menuItem.setActionCommand(actionCommand);
		menuItem.setAccelerator(hotKey);
		menuItem.addActionListener(this);
		actionMenu.add(menuItem);
	}

}
