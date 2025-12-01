package nl.vu.psy.ams.suite.gui;

import java.awt.CardLayout;
import java.awt.LayoutManager;
import java.util.ArrayList;

import javax.swing.JTabbedPane;

import nl.vu.psy.ams.suite.gui.tabs.AmsTab;
/*
 * The main tabs handler. Makes sure that the previous
 * tab is deactivated, and the new tab is activated after
 * tab change.
 */
public class MainTabs extends JTabbedPane {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	private ArrayList<AmsTab>	tabs				= new ArrayList<AmsTab>();
	private boolean hideTabBarWhenSingleTab = false;
	private LayoutManager defaultLayout;

	public void addAmsTab(String title, AmsTab tab) {
		tabs.add(tab);
		add(title, tab);
		//updateLayout();
	}

	public void clear() {
		tabs.clear();
		removeAll();
		//updateLayout();	
	}

	public ArrayList<AmsTab> getAmsTabs() {
		return tabs;
	}

	@Override
	public void setSelectedIndex(int index) {
		int selectedTab = getSelectedIndex();
		if (selectedTab != -1) {
			if (tabs.get(selectedTab).setUnactive() == false) {
				return;
			}
		}
		if (index != -1) {
			if (tabs.get(index).setActive() == false) {
				tabs.get(selectedTab).setActive();
				return;
			}
		}
		super.setSelectedIndex(index);
	}
	/**
	 * Sets whether the tab bar should be shown when there is only one tab.
	 * taken from: https://github.com/sing-group/GC4S/blob/master/gc4s/src/main/java/org/sing_group/gc4s/ui/tabbedpane/ExtendedJTabbedPane.java
	 * @param hide {@code true} if the tab bar should be hidden when there is
	 * only one tab and {@code false} otherwise.
	 */
	public void setHideTabBarWhenSingleTab(boolean hide) {
		if (hideTabBarWhenSingleTab != hide) {
			this.hideTabBarWhenSingleTab = hide;
			defaultLayout = this.getLayout();
			this.updateLayout();
		}
	}

	private void updateLayout() {
		this.setLayout(getProperLayout());
	}

	private LayoutManager getProperLayout() {
		if (getTabCount() == 1 && hideTabBarWhenSingleTab) {
			return new CardLayout();
		} else {
			return defaultLayout;
		}
	}
}
