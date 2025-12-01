package nl.vu.psy.ams.suite.gui.settings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.freq.SmoothnessPriorMatrices;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.tabs.freq.FrequencyTab;
import nl.vu.psy.ams.suite.gui.tabs.label.LabelTab;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

public class LabelDataSettings extends SettingsPane {

	/**
	 * The settings for labeling data
	 */
	private static final long serialVersionUID = 1L;
	private JCheckBox drawRawHR;
	private JCheckBox drawAvHR;
	private JCheckBox drawRawMot;
	private JCheckBox drawAvMot;
	private JCheckBox drawRawRR;
	private JCheckBox drawAvRR;
	private JComboBox<?> cb;
	private Color avHraColor;
	private Color avMotColor;
	private Color avRrColor;

	private NumberFormat nf;
	private JFormattedTextField lambdaTF, sigmaTF;
	private JCheckBox drawHFCB, drawLFCB;

	private JFormattedTextField lflbtf;

	private JFormattedTextField lfubtf;

	private JFormattedTextField hflbtf;

	private JFormattedTextField hfubtf;

	@Override
	public void initializeSettings() {
		AppSettings set = AppSettings.getInstance();
		if (set.getIntPropertyOrToBeSaved(Settings.DRAWRAWHRLABELTAB) == 0) {
			drawRawHR.setSelected(false);
		} else {
			drawRawHR.setSelected(true);
		}
		if (set.getIntPropertyOrToBeSaved(Settings.DRAWAVHRLABELTAB) == 0) {
			drawAvHR.setSelected(false);
		} else {
			drawAvHR.setSelected(true);
		}
		if (set.getIntPropertyOrToBeSaved(Settings.DRAWRAWMOTLABELTAB) == 0) {
			drawRawMot.setSelected(false);
		} else {
			drawRawMot.setSelected(true);
		}
		if (set.getIntPropertyOrToBeSaved(Settings.DRAWAVMOTLABELTAB) == 0) {
			drawAvMot.setSelected(false);
		} else {
			drawAvMot.setSelected(true);
		}
		if (set.getIntPropertyOrToBeSaved(Settings.DRAWRAWRRLABELTAB) == 0) {
			drawRawRR.setSelected(false);
		} else {
			drawRawRR.setSelected(true);
		}
		if (set.getIntPropertyOrToBeSaved(Settings.DRAWAVRRLABELTAB) == 0) {
			drawAvRR.setSelected(false);
		} else {
			drawAvRR.setSelected(true);
		}

		int val = set.getIntPropertyOrToBeSaved(Settings.LABELAVHRACOLOR);
		avHraColor = new Color(val);
		val = set.getIntPropertyOrToBeSaved(Settings.LABELAVMOTCOLOR);
		avMotColor = new Color(val);
		val = set.getIntPropertyOrToBeSaved(Settings.LABELAVRRACOLOR);
		avRrColor = new Color(val);

		// ---------------- Label Between Markers----------------------------
		int value = set.getIntPropertyOrToBeSaved(Settings.LABELBETWEENMARKERS);
		cb.setSelectedItem(String.valueOf(value));
		// ------------------------------------------------------------------

		val = set.getIntPropertyOrToBeSaved(Settings.FREQLAMBDA);
		lambdaTF.setValue(val / (Double) 100.);
		val = set.getIntPropertyOrToBeSaved(Settings.FREQSIGMA);
		sigmaTF.setValue(val / (Double) 1000.);

		val = set.getIntPropertyOrToBeSaved(Settings.DRAWLFSIGNAL);
		if (val == 0) {
			drawLFCB.setSelected(false);
		} else {
			drawLFCB.setSelected(true);
		}

		val = set.getIntPropertyOrToBeSaved(Settings.DRAWHFSIGNAL);
		if (val == 0) {
			drawHFCB.setSelected(false);
		} else {
			drawHFCB.setSelected(true);
		}

		val = set.getIntPropertyOrToBeSaved(Settings.LFLB);
		lflbtf.setValue(val / 1000.);
		val = set.getIntPropertyOrToBeSaved(Settings.LFUB);
		lfubtf.setValue(val / 1000.);
		val = set.getIntPropertyOrToBeSaved(Settings.HFLB);
		hflbtf.setValue(val / 1000.);
		val = set.getIntPropertyOrToBeSaved(Settings.HFUB);
		hfubtf.setValue(val / 1000.);
	}

	@Override
	public void save() {
		AppSettings set = AppSettings.getInstance();
		boolean isOpen = CurrentOpenData.getInstance().isOpen();
		boolean beatsAbsent = (CurrentOpenData.getInstance().getBeatSet() == null
				|| CurrentOpenData.getInstance().getBeatSet().getBeats().isEmpty());
		boolean motAbsent = CurrentOpenData.getInstance().channelExists("MYA") == false;
		if (drawRawHR.isSelected()) {
			set.setIntProperty(Settings.DRAWRAWHRLABELTAB, 1);
		} else {
			set.setIntProperty(Settings.DRAWRAWHRLABELTAB, 0);
		}
		if (drawAvHR.isSelected()) {
			set.setIntProperty(Settings.DRAWAVHRLABELTAB, 1);
		} else {
			set.setIntProperty(Settings.DRAWAVHRLABELTAB, 0);
		}
		if (isOpen && beatsAbsent == false) {
			LabelTab.getInstance().getHRDrawer().drawRawHR(drawRawHR.isSelected());
			LabelTab.getInstance().getHRDrawer().drawAverageHR(drawAvHR.isSelected());
		}
		if (drawRawMot.isSelected()) {
			set.setIntProperty(Settings.DRAWRAWMOTLABELTAB, 1);
		} else {
			set.setIntProperty(Settings.DRAWRAWMOTLABELTAB, 0);
		}
		if (drawAvMot.isSelected()) {
			set.setIntProperty(Settings.DRAWAVMOTLABELTAB, 1);
		} else {
			set.setIntProperty(Settings.DRAWAVMOTLABELTAB, 0);
		}
		if (isOpen && motAbsent == false) {
			LabelTab.getInstance().getMotDrawer().setDrawRaw(drawRawMot.isSelected());
			LabelTab.getInstance().getMotDrawer().setDrawAv(drawAvMot.isSelected());
		}
		if (drawRawRR.isSelected()) {
			set.setIntProperty(Settings.DRAWRAWRRLABELTAB, 1);
		} else {
			set.setIntProperty(Settings.DRAWRAWRRLABELTAB, 0);
		}
		if (drawAvRR.isSelected()) {
			set.setIntProperty(Settings.DRAWAVRRLABELTAB, 1);
		} else {
			set.setIntProperty(Settings.DRAWAVRRLABELTAB, 0);
		}
		if (isOpen && beatsAbsent == false) {
			LabelTab.getInstance().getRRDrawer().drawRaw(drawRawHR.isSelected());
			LabelTab.getInstance().getRRDrawer().drawAv(drawAvHR.isSelected());
		}

		int val = avHraColor.getRGB();
		set.setIntProperty(Settings.LABELAVHRACOLOR, val);
		val = avMotColor.getRGB();
		set.setIntProperty(Settings.LABELAVMOTCOLOR, val);
		val = avRrColor.getRGB();
		set.setIntProperty(Settings.LABELAVRRACOLOR, val);

		// ---------------- Label Between Markers----------------------------
		set.setIntProperty(Settings.LABELBETWEENMARKERS, Integer.valueOf(cb.getSelectedItem().toString()));
		// ------------------------------------------------------------------

		if (isOpen && beatsAbsent == false) {
			LabelTab.getInstance().getHRDrawer().setAvColor(avHraColor);
			MainFrame.getInstance().getMainFrame().repaint();
		}

		if (isOpen && motAbsent == false) {
			LabelTab.getInstance().getMotDrawer().setAvColor(avMotColor);
			MainFrame.getInstance().getMainFrame().repaint();
		}

		if (isOpen && beatsAbsent == false) {
			LabelTab.getInstance().getRRDrawer().setAvColor(avRrColor);
			MainFrame.getInstance().getMainFrame().repaint();
		}

		try {
			lambdaTF.commitEdit();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		try {
			sigmaTF.commitEdit();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		boolean recalc = false;

		val = (int) (((Number) lambdaTF.getValue()).doubleValue() * 100);
		if (val != set.getIntPropertyOrToBeSaved(Settings.FREQLAMBDA)) {
			set.setIntProperty(Settings.FREQLAMBDA, val);
			recalc = true;
		}
		val = (int) (((Number) sigmaTF.getValue()).doubleValue() * 1000);
		if (val != set.getIntPropertyOrToBeSaved(Settings.FREQSIGMA)) {
			set.setIntProperty(Settings.FREQSIGMA, val);
			recalc = true;
		}

		try {
			lflbtf.commitEdit();
		} catch (ParseException e) {
		}

		val = (int) Math.round(((Number) lflbtf.getValue()).doubleValue() * 1000);
		if (val != set.getIntPropertyOrToBeSaved(Settings.LFLB)) {
			set.setIntProperty(Settings.LFLB, val);
			recalc = true;
		}

		val = (int) Math.round(((Number) lfubtf.getValue()).doubleValue() * 1000);
		if (val != set.getIntPropertyOrToBeSaved(Settings.LFUB)) {
			set.setIntProperty(Settings.LFUB, val);
			recalc = true;
		}

		val = (int) Math.round(((Number) hflbtf.getValue()).doubleValue() * 1000);
		if (val != set.getIntPropertyOrToBeSaved(Settings.HFLB)) {
			set.setIntProperty(Settings.HFLB, val);
			recalc = true;
		}

		val = (int) Math.round(((Number) hfubtf.getValue()).doubleValue() * 1000);
		if (val != set.getIntPropertyOrToBeSaved(Settings.HFUB)) {
			set.setIntProperty(Settings.HFUB, val);
			recalc = true;
		}

		if (recalc) {
			SmoothnessPriorMatrices.getInstance().reGenerate();
			if (CurrentOpenData.getInstance().isOpen()) {
				FrequencyTab tab = FrequencyTab.getInstance();
				if (tab.isActive()) {
					tab.setUnactive();
					tab.setActive();
				}
				LabelTab tabl = LabelTab.getInstance();
				if (tabl.isActive()) {
					tabl.setUnactive();
					tabl.setActive();
				}
			}
		}

		boolean drawLF = drawLFCB.isSelected();
		if (drawLF) {
			set.setIntProperty(Settings.DRAWLFSIGNAL, 1);
		} else {
			set.setIntProperty(Settings.DRAWLFSIGNAL, 0);
		}
		boolean drawHF = drawHFCB.isSelected();
		if (drawHF) {
			set.setIntProperty(Settings.DRAWHFSIGNAL, 1);
		} else {
			set.setIntProperty(Settings.DRAWHFSIGNAL, 0);
		}

		if (CurrentOpenData.getInstance().isOpen() && CurrentOpenData.getInstance().channelExists("ECG")
				&& !CurrentOpenData.getInstance().getBeatSet().getBeats().isEmpty()) {
			LabelTab.getInstance().getLfhfDrawer().setDrawLF(drawLF);
			LabelTab.getInstance().getLfhfDrawer().setDrawHF(drawHF);
		}

	}

	@Override
	public void setupPanel() {
		pan.add(new JLabel("<html><h1>Label Data</h1></html>"));
		pan.add(new JSeparator());
		drawRawHR = new JCheckBox("Draw raw heartrate data");
		drawRawHR.setAlignmentX(Component.LEFT_ALIGNMENT);
		drawAvHR = new JCheckBox("Draw average heartrate data");
		drawAvHR.setAlignmentX(Component.LEFT_ALIGNMENT);
		drawRawMot = new JCheckBox("Draw raw motility data");
		drawRawMot.setAlignmentX(Component.LEFT_ALIGNMENT);
		drawAvMot = new JCheckBox("Draw average motility data");
		drawAvMot.setAlignmentX(Component.LEFT_ALIGNMENT);
		drawRawRR = new JCheckBox("Draw raw respiration rate data");
		drawRawRR.setAlignmentX(Component.LEFT_ALIGNMENT);
		drawAvRR = new JCheckBox("Draw average respiration rate data");
		drawAvRR.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel tmp = new JPanel();
		tmp.setLayout(new BoxLayout(tmp, BoxLayout.Y_AXIS));
		tmp.setBorder(BorderFactory.createTitledBorder("Drawing options"));
		tmp.add(drawRawHR);
		tmp.add(drawAvHR);
		tmp.add(drawRawMot);
		tmp.add(drawAvMot);
		tmp.add(drawRawRR);
		tmp.add(drawAvRR);

		JButton but;
		but = new JButton("Set filtered data color");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog(LabelDataSettings.this, "Set filtered data color",
						avHraColor);
				if (newColor != null) {
					avHraColor = newColor;
				}
			}
		});
		tmp.add(but);

		but = new JButton("Set average motility color");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog(LabelDataSettings.this, "Set average motility color",
						avMotColor);
				if (newColor != null) {
					avMotColor = newColor;
				}
			}
		});
		tmp.add(but);

		but = new JButton("Set average respiration rate color");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog(LabelDataSettings.this, "Set average motility color",
						avMotColor);
				if (newColor != null) {
					avRrColor = newColor;
				}
			}
		});
		tmp.add(but);

		pan.add(tmp);
		pan.add(new JSeparator());
		pan.add(new JLabel("<html><h1>Label Categories</h1></html>"));

		JPanel overlapPanel = new JPanel();
		overlapPanel.setLayout(new BoxLayout(overlapPanel, BoxLayout.Y_AXIS));
		overlapPanel.setBorder(BorderFactory.createTitledBorder("Label Options"));

		JLabel label = new JLabel(
				"Provide Experimental Categories to Time Labels only if the overlap is greater than:");
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		JTextField tf = new JTextField();
		tf.setColumns(10);
		tf.setText("70%");
		tf.setEnabled(false);
		tf.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel emptypanel = new JPanel();
		emptypanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel labeldur = new JLabel("Minimum label duration time for labels between markers");
		labeldur.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel tfpanel = new JPanel();
		tfpanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		String[] options = { "5", "10", "15", "20", "30", "60", "90" };
		cb = new JComboBox<Object>(options);

		JTextField tf2 = new JTextField();
		tf2.setColumns(10);
		tf2.setText("sec");
		tf2.setEnabled(false);
		tf2.setAlignmentX(Component.CENTER_ALIGNMENT);

		tfpanel.add(cb);
		tfpanel.add(tf2);

		overlapPanel.add(label);
		overlapPanel.add(tf);
		overlapPanel.add(emptypanel);
		overlapPanel.add(labeldur);
		overlapPanel.add(tfpanel);

		pan.add(overlapPanel);

		nf = NumberFormat.getNumberInstance(Locale.US);
		nf.setGroupingUsed(false);

		lambdaTF = new JFormattedTextField(nf);
		lambdaTF.setColumns(10);

		sigmaTF = new JFormattedTextField(nf);
		sigmaTF.setColumns(10);

		pan.add(new JLabel("<html><h1>Frequency Analysis Settings</h1></html>"));
		pan.add(new JSeparator());
		tmp = new JPanel();
		tmp.setLayout(new BoxLayout(tmp, BoxLayout.Y_AXIS));

		JPanel tmp2 = new JPanel();
		tmp2.setLayout(new BorderLayout());
		JPanel tmp3 = new JPanel();
		tmp3.setLayout(new BoxLayout(tmp3, BoxLayout.X_AXIS));
		JLabel lab = new JLabel("Smoothness prior \u03BB value:");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp3.add(lab);
		tmp3.add(lambdaTF);

		tmp2.add(tmp3, BorderLayout.WEST);

		tmp.add(tmp2);

		tmp2 = new JPanel();
		tmp2.setLayout(new BorderLayout());
		tmp3 = new JPanel();
		tmp3.setLayout(new BoxLayout(tmp3, BoxLayout.X_AXIS));
		lab = new JLabel("Artefact removal \u03C3 value:");
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		tmp3.add(lab);
		tmp3.add(sigmaTF);

		tmp2.add(tmp3, BorderLayout.WEST);

		tmp.add(tmp2);

		tmp.setBorder(BorderFactory.createTitledBorder("Signal preprocessing settings"));

		pan.add(tmp);

		tmp = new JPanel();
		tmp.setLayout(new BoxLayout(tmp, BoxLayout.Y_AXIS));
		tmp.setBorder(BorderFactory.createTitledBorder("Frequency bands"));

		lflbtf = new JFormattedTextField(NumberFormat.getInstance(Locale.US));
		lfubtf = new JFormattedTextField(NumberFormat.getInstance(Locale.US));
		hflbtf = new JFormattedTextField(NumberFormat.getInstance(Locale.US));
		hfubtf = new JFormattedTextField(NumberFormat.getInstance(Locale.US));

		JPanel tfPan;

		tfPan = new JPanel(new BorderLayout());
		tfPan.add(new JLabel("LF lower bound (Hz): "), BorderLayout.WEST);
		tfPan.add(lflbtf, BorderLayout.CENTER);
		tmp.add(tfPan);
		tfPan = new JPanel(new BorderLayout());
		tfPan.add(new JLabel("LF upper bound (Hz): "), BorderLayout.WEST);
		tfPan.add(lfubtf, BorderLayout.CENTER);
		tmp.add(tfPan);
		tfPan = new JPanel(new BorderLayout());
		tfPan.add(new JLabel("HF lower bound (Hz): "), BorderLayout.WEST);
		tfPan.add(hflbtf, BorderLayout.CENTER);
		tmp.add(tfPan);
		tfPan = new JPanel(new BorderLayout());
		tfPan.add(new JLabel("HF upper bound (Hz): "), BorderLayout.WEST);
		tfPan.add(hfubtf, BorderLayout.CENTER);
		tmp.add(tfPan);

		pan.add(tmp);

		tmp = new JPanel();
		tmp.setLayout(new BoxLayout(tmp, BoxLayout.Y_AXIS));
		tmp.setBorder(BorderFactory.createTitledBorder("Frequency drawing"));

		drawLFCB = new JCheckBox("Draw LF Signal");
		drawHFCB = new JCheckBox("Draw HF Signal");
		tmp.add(drawLFCB);
		tmp.add(drawHFCB);

		pan.add(tmp);
	}

}
