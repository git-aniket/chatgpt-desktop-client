package nl.vu.psy.ams.suite.gui.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.freq.SmoothnessPriorMatrices;
import nl.vu.psy.ams.suite.gui.tabs.freq.FrequencyTab;
import nl.vu.psy.ams.suite.gui.tabs.label.LabelTab;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;

public class FrequencySettings extends SettingsPane {

	/**
	 * The settings for frequency analysis
	 */
	private static final long	serialVersionUID	= 1L;

	private NumberFormat		nf;

	private JFormattedTextField	lambdaTF, sigmaTF;
	private JCheckBox			drawHFCB, drawLFCB;

	private JFormattedTextField	lflbtf;

	private JFormattedTextField	lfubtf;

	private JFormattedTextField	hflbtf;

	private JFormattedTextField	hfubtf;

	@Override
	public void initializeSettings() {
		AppSettings set = AppSettings.getInstance();
		int val;
		val = set.getIntPropertyOrToBeSaved(Settings.FREQLAMBDA);
		lambdaTF.setValue(val /(Double) 100.);
		val = set.getIntPropertyOrToBeSaved(Settings.FREQSIGMA);
		sigmaTF.setValue(val / (Double)1000.);

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
		int val;
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

		if (CurrentOpenData.getInstance().isOpen() && CurrentOpenData.getInstance().channelExists("ECG") && !CurrentOpenData.getInstance().getBeatSet().getBeats().isEmpty())
		{
			FrequencyTab.getInstance().getLfhfDrawer().setDrawLF(drawLF);
			FrequencyTab.getInstance().getLfhfDrawer().setDrawHF(drawHF);
		}

	}

	@Override
	public void setupPanel() {
		nf = NumberFormat.getNumberInstance(Locale.US);
		nf.setGroupingUsed(false);

		lambdaTF = new JFormattedTextField(nf);
		lambdaTF.setColumns(10);

		sigmaTF = new JFormattedTextField(nf);
		sigmaTF.setColumns(10);

		pan.add(new JLabel("<html><h1>Frequency Analysis Settings</h1></html>"));
		pan.add(new JSeparator());
		JPanel tmp;
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
