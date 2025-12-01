package nl.vu.psy.ams.suite.gui.settings;

import java.awt.BorderLayout;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public abstract class SettingsPane extends JScrollPane {

	/**
	 * Base abstract class of a SettingsPane, that can show and save settings.
	 * If you create a new settings catagory, extend this class.
	 */
	private static final long	serialVersionUID	= 1L;
	private JPanel				outerPanel;
	protected JPanel			pan;
	public SettingsPane() {
		super();
		outerPanel = new JPanel(new BorderLayout());
		pan = new JPanel();
		pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));

		setupPanel();

		initializeSettings();

		outerPanel.add(pan, BorderLayout.NORTH);
		outerPanel.add(new JPanel(), BorderLayout.CENTER);

		setViewportView(outerPanel);
	}

	public abstract void initializeSettings();
	public abstract void save();
	public abstract void setupPanel();
}
