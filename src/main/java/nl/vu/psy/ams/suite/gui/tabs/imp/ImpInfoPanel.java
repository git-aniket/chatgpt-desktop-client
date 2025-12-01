package nl.vu.psy.ams.suite.gui.tabs.imp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.LayoutManager2;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
//import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsHeader;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsPacket;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsHeader;
import nl.vu.psy.ams.suite.gui.MainFrame;
import nl.vu.psy.ams.suite.gui.drawing.SignalPartDrawer;
import nl.vu.psy.ams.suite.gui.tabs.ExpandingPanel;
import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Utils;

public class ImpInfoPanel extends JPanel {

	/**
	 * Panel showing impedance information about the selected label.
	 */
	private ArrayList<Ams5fsPacket> packet = CurrentOpenData.getInstance().getSettings();
	private Ams7fsHeader fileHeader = CurrentOpenData.getInstance().getFileHeader();
	// private Ams5fsHeader header;
	private static final long serialVersionUID = 1L;
	private JPanel complexPan;
	private JPanel paraPan;
	private JPanel calcParaPan;
	private JPanel scorePan;
	private JLabel paraLabL, paraLabR;
	private JLabel complexLabL, complexLabR;

	private SignalPartDrawer id;
	private double rhoVal = 135;
	protected double edVal = 20;
	// private JLabel scoreLabL;
	private NumberFormat nf;
	private NumberFormat nf1;
	private JLabel calcParaLabL;
	private double avZ0;
	private double avZ0_c;
	private double hra;
	private JLabel calcParaLabR;
	// private JLabel scoreLabR;
	private JCheckBox cb;
	private JCheckBox qonsetmissing;
	private JCheckBox qmissing;
	private JCheckBox ecgmissing;
	private JCheckBox showMot;
	final int misVal = AppSettings.getInstance().getIntProperty(Settings.MISSINGVALUE);

	public ImpInfoPanel() {
		super();

		JPanel tP;

		JPanel ScrollPanel = new JPanel();
		ScrollPanel.setLayout(new BoxLayout(ScrollPanel, BoxLayout.Y_AXIS));

		nf = NumberFormat.getInstance(Locale.US);
		nf.setGroupingUsed(false);
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(2);

		nf1 = NumberFormat.getInstance(Locale.US);
		nf1.setGroupingUsed(false);
		nf1.setMaximumFractionDigits(1);
		nf1.setMinimumFractionDigits(1);

		ArrayList<Ams5fsPacket> settings = CurrentOpenData.getInstance().getSettings();
		if (settings.isEmpty() == false)
			edVal = settings.get(0).getwElectrodeDistance() / 10.;
		else if (fileHeader.getwElectrodeDistance() != null)
			edVal = fileHeader.getwElectrodeDistance() / 10.;

		rhoVal = CurrentOpenData.getInstance().getBloodresistivity();// Always use rhoVal from CurrentOpenData which is
																		// either default: 135 or specific from .amsdata
																		// file

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createLineBorder(Color.BLACK));

		showMot = new JCheckBox("Show motility graph (PCG)");
		showMot.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ExpandingPanel exp = ImpTab.getInstance().getExp();
				Container parent = exp.getParent();
				LayoutManager2 layout = null;
				if (parent != null)
					layout = (LayoutManager2) parent.getLayout();
				if (((JCheckBox) e.getSource()).isSelected()) {
					exp.setVisible(true);
					if (layout != null)
						layout.addLayoutComponent(exp, 1F);
				} else {
					exp.setVisible(false);
					if (layout != null)
						layout.addLayoutComponent(exp, null);
				}
				exp.revalidate();
				exp.repaint();
			}
		});
		showMot.setSelected(false);

		complexPan = new JPanel(new BorderLayout());
		complexPan.setBorder(BorderFactory.createTitledBorder("ICG Complex"));
		complexLabL = new JLabel();
		complexLabR = new JLabel();
		complexPan.add(complexLabL, BorderLayout.WEST);
		complexPan.add(complexLabR, BorderLayout.EAST);
		complexPan.add(showMot, BorderLayout.SOUTH);
		ScrollPanel.add(complexPan);

		scorePan = new JPanel(new BorderLayout());
		scorePan.setBorder(BorderFactory.createTitledBorder("Marker Missingness"));
		// String lblText = "<html><body>Upstroke position(B-Point):<br>Dz/dt minimum
		// position (C-Point):<br>Incisura position (X-Point):<br>P-onset position:<br>P
		// Time<br>Q-onset position:<br>Q position:<br>S Time:<br>S Offset Time:<br>T
		// position:<br>T value:<br>T Offset Time:<br></body></html>";
		String lblText = "<html><body></body></html>";
		cb = new JCheckBox("Set ICG As Missing");

		cb.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				id.getSelPart().getLabel().setICGMissing(((JCheckBox) e.getSource()).isSelected());
				updateLabelTexts();
			}
		});

		qonsetmissing = new JCheckBox("Set Q-Onset As Missing");

		qonsetmissing.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (AmsLabel l : CurrentOpenData.getInstance().getLabels().getLabels()) {
					l.setECGQOnsetMissing(((JCheckBox) e.getSource()).isSelected());
				}
				// id.getSelPart().getLabel().setECGQOnsetMissing(((JCheckBox)
				// e.getSource()).isSelected());
				updateLabelTexts();
			}
		});

		qmissing = new JCheckBox("Set Q As Missing");

		qmissing.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (AmsLabel l : CurrentOpenData.getInstance().getLabels().getLabels()) {
					l.setECGQPOintMissing(((JCheckBox) e.getSource()).isSelected());
				}
				// id.getSelPart().getLabel().setECGQPOintMissing(((JCheckBox)
				// e.getSource()).isSelected());
				updateLabelTexts();
			}
		});

		ecgmissing = new JCheckBox("Set ECG and ICG as Missing");

		ecgmissing.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				id.getSelPart().getLabel().setECGMissing(((JCheckBox) e.getSource()).isSelected());
				id.getSelPart().getLabel().setICGMissing(((JCheckBox) e.getSource()).isSelected());
				updateLabelTexts();
			}
		});

		tP = new JPanel();
		tP.add(cb);
		tP.add(qonsetmissing);
		tP.add(qmissing);
		tP.add(ecgmissing);
		tP.setLayout(new GridLayout(4, 1));
		scorePan.add(tP, BorderLayout.SOUTH);

		calcParaPan = new JPanel(new BorderLayout());
		calcParaPan.setBorder(BorderFactory.createTitledBorder("Calculated Parameters"));
		calcParaLabL = new JLabel();
		lblText = "<html><body>PEP:<br>HR Average:<br>LVET:<br>Z0 Average:<br>T-wave Amplitude</body></html>";
		calcParaLabL.setText(lblText);
		calcParaLabR = new JLabel();
		calcParaPan.add(calcParaLabL, BorderLayout.WEST);
		calcParaPan.add(calcParaLabR, BorderLayout.EAST);
		ScrollPanel.add(calcParaPan);
		ScrollPanel.add(scorePan);

		paraPan = new JPanel(new BorderLayout());
		paraPan.setBorder(BorderFactory.createTitledBorder("Stroke Volume"));
		paraLabL = new JLabel();
		lblText = "<html><body>\u03C1 value:<br>Electrode distance:<br>Stroke volume (Kubicek 1966):<br>Stroke volume (Nederend 2017):</body></html>";
		paraLabL.setText(lblText);
		paraLabR = new JLabel();
		paraPan.add(paraLabL, BorderLayout.WEST);
		paraPan.add(paraLabR, BorderLayout.EAST);
		JButton editBut = new JButton("Edit");
		editBut.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				final JDialog diag = new JDialog(MainFrame.getInstance().getMainFrame(), "Edit Parameters", true);
				diag.setLayout(new BoxLayout(diag.getContentPane(), BoxLayout.Y_AXIS));
				JPanel pan = new JPanel();
				pan.setLayout(new BoxLayout(pan, BoxLayout.X_AXIS));
				pan.add(new JLabel("\u03F1 value [\u2126/cm]:"));
				pan.add(Box.createHorizontalGlue());
				final JFormattedTextField rhoField = new JFormattedTextField(nf1);
				rhoField.setColumns(6);
				// Gson gson1 = new Gson();
				// File curFile = new File(CurrentOpenData.getInstance().getFilePath(),
				// "header.json");
				// String inString = Utils.readStringFromFile(curFile);
				// header = gson1.fromJson(inString, Ams5fsHeader.class);
				rhoField.setValue(CurrentOpenData.getInstance().getBloodresistivity());
				// rhoField.setValue(header.getSpecificBloodResistivity());
				pan.add(rhoField);
				diag.add(pan);

				pan = new JPanel();
				pan.setLayout(new BoxLayout(pan, BoxLayout.X_AXIS));
				pan.add(new JLabel("Electrode distance [cm]:"));
				pan.add(Box.createHorizontalGlue());
				final JFormattedTextField edField = new JFormattedTextField(nf1);
				edField.setColumns(6);
				edField.setValue(edVal);
				pan.add(edField);
				diag.add(pan);

				pan = new JPanel();
				JButton okBut = new JButton("OK");
				okBut.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							rhoField.commitEdit();
							edField.commitEdit();
						} catch (ParseException e1) {
							e1.printStackTrace();
						}
						rhoVal = Utils.convertDouble(rhoField.getValue());
						edVal = Utils.convertDouble(edField.getValue());
						CurrentOpenData.getInstance().setBloodresistivity(rhoVal);

						// ------------------ Save changes to settings structure------------------
						if (packet.size() > 0)
							packet.get(0).setwElectrodeDistance((int) (edVal * 10));
						else
							fileHeader.setwElectrodeDistance((int) (edVal * 10));
						CurrentOpenData.getInstance().setDirty(true);
						// CurrentOpenData.getInstance().saveChangeablesToDisk();
						PrintWriter writer = null;
						Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues()
								.create();

						try {
							writer = new PrintWriter(new BufferedWriter(new FileWriter(
									new File(CurrentOpenData.getInstance().getFilePath(), "settings.json"))));
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						writer.print(gson.toJson(packet));
						writer.close();
						CurrentOpenData.getInstance().setDirty(true);
						// ------------------ Save changes to settings structure------------------

						updateLabelTexts();
						diag.setVisible(false);
					}

				});
				JButton canBut = new JButton("Cancel");
				canBut.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						diag.setVisible(false);
					}
				});
				pan.add(okBut);
				pan.add(canBut);

				diag.add(pan);

				diag.pack();
				diag.setLocationRelativeTo(MainFrame.getInstance().getMainFrame());
				diag.setVisible(true);
			}
		});
		tP = new JPanel();
		tP.add(editBut);
		paraPan.add(tP, BorderLayout.SOUTH);
		ScrollPanel.add(paraPan);

		add(new JScrollPane(ScrollPanel));

		setMinimumSize(new Dimension(300, 0));
	}

	public double getEdVal() {
		return edVal;
	}

	public double getRhoVal() {
		return rhoVal;
	}

	public void setImpDrawer(SignalPartDrawer id) {
		this.id = id;
		id.setIip(this);
	}

	public void setEcgDrawer(SignalPartDrawer id) {
		this.id = id;
		id.setIip(this);
	}

	public synchronized void updateLabelTexts() {
		String lblText;
		AmsLabel l = id.getSelPart().getLabel();
		lblText = "<html><body>";
		for (Map.Entry<String, String> entry : l.getAttributes().entrySet()) {
			if (!entry.getKey().equals("Label No"))
				lblText += entry.getKey() + ":<br>";
		}
		lblText += "Average Complex:<br>Individual Complex:<br>Start Time:<br>End Time:<br>Ensembling Period:<br>Percentage of beats discarded:<br>Total motility:</body></html>";
		complexLabL.setText(lblText);

		lblText = "<html><body>";
		for (Map.Entry<String, String> entry : l.getAttributes().entrySet()) {
			if (!entry.getKey().equals("Label No"))
				lblText += entry.getValue() + "<br>";
		}

		cb.setSelected(id.getSelPart().getLabel().isICGMissing());
		qonsetmissing.setSelected(id.getSelPart().getLabel().isECGQOnsetMissing());
		qmissing.setSelected(id.getSelPart().getLabel().isECGQPointMissing());
		ecgmissing.setSelected(id.getSelPart().getLabel().isECGMissing());

		if (id.getSelPart().getCalculating() || id.getSelPart().getValues() == null ||
				id.getSelPart().getLabel().isICGMissing() || id.getSelPart().getLabel().isECGMissing()) {
			calcParaLabR.setText("");
			paraLabR.setText("");
			// scoreLabR.setText("");
			updateScoringTexts(lblText);
		} else if (id.getSelPart().getCalculating() || id.getSelPart().getValues() == null ||
				id.getSelPart().getLabel().isICGMissing()) {
			// calcParaLabR.setText("");
			paraLabR.setText("");
			updateScoringTexts(lblText);
		} else {

			avZ0 = l.getAverageZ0();
			// Choose correction flavor:
			// ----------------------------
			// avZ0_c = -1.291 + 0.304*avZ02 - 6.695*-l.getICGmVal() + 0.442*edVal; //
			// SV_biplane, c.f. Table 6 in Nederend et al. 2017
			// (http://dx.doi.org/10.1016/j.ijpsycho.2017.07.015)
			// avZ0_c = 4.619 + 0.227*avZ02 - 5.363*-l.getICGmVal(); // SV_VTI, c.f. Table 6
			// in Nederend et al. 2017 (http://dx.doi.org/10.1016/j.ijpsycho.2017.07.015)
			// avZ0_c = 7.978 - 6.359*-lbl.getICGmVal(); // SV_3D, c.f. Table 6 in Nederend
			// et al. 2017 (http://dx.doi.org/10.1016/j.ijpsycho.2017.07.015)
			avZ0_c = 7.337 - 6.208 * -l.getICGcVal(); // SV_average, c.f. Table 6 in Nederend et al. 2017
														// (http://dx.doi.org/10.1016/j.ijpsycho.2017.07.015)
			hra = l.getAverage(true);
			updateScoringTexts(lblText);
		}
		repaint();
		id.getGraph().getxAxis().updateAll();
		id.getGraph().repaint();
	}

	public synchronized void updateScoringTexts(String lblText) {
		// String lblText;

		double bTime = id.getSelPart().getbPoint() / 1000000.;
		// double cTime = id.getSelPart().getcPoint() / 1000000.;
		double xTime = id.getSelPart().getxPoint() / 1000000.;
		//
		// double pOnsetTime = id.getSelPart().getECGPPoint() / 1000000.; // returns P -
		// Onset Time
		// double pTime = id.getSelPart().getECGPOnsetPoint()/1000000.; // returns P -
		// Time
		double qOnsetTime = id.getSelPart().getECGQPoint() / 1000000.; // returns Q - Onset Time
		double qTime = id.getSelPart().getECGQOnsetPoint() / 1000000.; // returns Q - Time
		// //double qamplitude = Math.ceil(id.getSelPart().getECGQVal() *1000)/1000;
		// //double rAmplitude = Math.ceil(id.getSelPart().getECGRVal() * 100)/100;
		// double STime = id.getSelPart().getECGSPoint() / 1000000.;
		// double SOffsetTime = id.getSelPart().getECGSOffsetPoint() / 1000000.;
		// double tTime = id.getSelPart().getECGTPoint() / 1000000.;
		// double TOffsetTime = id.getSelPart().getECGTOffsetPoint() / 1000000.;

		// double QTInterval = (Math.abs(qTime) + Math.abs(TOffsetTime));
		// double RRInterval = 60/id.getSelPart().getLabel().getAverage(true);

		double minVal = -id.getSelPart().getcVal();
		double sv = -CurrentOpenData.getInstance().getBloodresistivity() * edVal * edVal * minVal * (xTime - bTime)
				/ (avZ0 * avZ0);
		double sv_c = -CurrentOpenData.getInstance().getBloodresistivity() * edVal * edVal * minVal * (xTime - bTime)
				/ (avZ0_c * avZ0_c);
		// double correctedQTC = (QTInterval)/Math.sqrt(RRInterval);

		AmsLabel l = id.getSelPart().getLabel();
		// lblText = "<html><body>";
		lblText += (id.getSelIndex() + 1) + " of " + id.getNParts() + "<br>";
		lblText += (id.getSelBeat() + 1) + " of " + id.getNBeats() + "<br>";
		lblText += Utils.getDateAndTimeFromUS(l.getLeftTime()) + "<br>";
		lblText += Utils.getDateAndTimeFromUS(l.getRightTime()) + "<br>";
		long nSec = Math.round(((l.getRightTime() - l.getLeftTime()) / 1000000.));
		lblText += nSec + " [sec]<br>";
		int ninAv = id.getSelPart().getnumberOfComplexes();

		int nBeats = l.getNumberOfBeats();
		double relDisc = (double) ninAv / nBeats;
		if ((1 - relDisc) * 100 > 40) {
			lblText += "<font color=red>";
			lblText += nf.format((1 - relDisc) * 100) + " \u0025<br>";
			lblText += "</font>";
		} else {
			lblText += nf.format((1 - relDisc) * 100) + " \u0025<br>";
		}
		double motility = l.getTotalMotility();
		lblText += nf.format(motility) + " mg<br>";

		lblText += "</body></html>";
		complexLabR.setText(lblText);

		if (id.getSelPart().getCalculating() == false && id.getSelPart().getValues() != null &&
				id.getSelPart().getLabel().isICGMissing() == false
				&& id.getSelPart().getLabel().isECGMissing() == false) {

			lblText = "<html><body>";
			// -------------Calculation of PEP -----
			if (id.getSelPart().getLabel().isECGQOnsetMissing() == false) {
				// If Q-onset is present
				lblText += nf.format(((bTime) * 1000. + (Math.abs(qOnsetTime) * 1000))) + " [msec]<br>";

			} else if (id.getSelPart().getLabel().isECGQOnsetMissing() == true
					&& (id.getSelPart().getLabel().isECGQPointMissing() == false)) {
				// Q onset missing, but Q-Point present
				lblText += nf.format(((bTime) * 1000. + (Math.abs(qTime) * 1000)) + 12)
						+ " [msec](From Q-to-B + 12ms)<br>";
			} else if (id.getSelPart().getLabel().isECGQOnsetMissing() == true
					&& (id.getSelPart().getLabel().isECGQPointMissing() == true)) {
				// Q-onset and Q-Point are absent
				lblText += nf.format(((bTime) * 1000.) + 41) + " [msec](From R-to-B + 41ms)<br>";
			}
			// --------------------------------------
			double TValue = id.getSelPart().getLabel().getECGTVal();
			double TOffsetValue = id.getSelPart().getLabel().getECGTOffsetVal();

			lblText += nf.format(hra) + " [bpm]<br>";
			lblText += nf.format((xTime - bTime) * 1000.) + " [msec]<br>";
			lblText += nf.format(avZ0) + " [\u2126]<br>";
			lblText += nf.format(TValue - TOffsetValue) + " [mV]<br>";
			lblText += "</body></html>";
			calcParaLabR.setText(lblText);

			/*
			 * lblText = "<html><body>";
			 * lblText += nf.format(bTime * 1000) + " [msec]<br>";
			 * lblText += nf.format(cTime * 1000) + " [msec]<br>";
			 * lblText += nf.format(xTime * 1000) + " [msec]<br>";
			 * lblText += nf.format(pOnsetTime * 1000) + " [msec]<br>";
			 * lblText += nf.format(pTime * 1000) + " [msec]<br>";
			 * 
			 * if(id.getSelPart().getLabel().isECGQOnsetMissing() == true){
			 * lblText += misVal+ "<br>";
			 * }else{
			 * lblText += nf.format(qOnsetTime * 1000) + " [msec]<br>";
			 * }
			 * 
			 * if(id.getSelPart().getLabel().isECGQPointMissing() == true){
			 * lblText += misVal+ "<br>";
			 * }else{
			 * lblText += nf.format(qTime * 1000) + " [msec]<br>";
			 * }
			 * 
			 * lblText += nf.format( STime * 1000) + " [msec]<br>";
			 * lblText += nf.format(SOffsetTime* 1000) + " [msec]<br>";
			 * lblText += nf.format(tTime * 1000) + " [msec]<br>";
			 * lblText += nf.format(id.getSelPart().getECGTVal()) + " [mV]<br>";
			 * lblText += nf.format(TOffsetTime* 1000) + " [msec]<br>";
			 * 
			 * //rAmplitude = Math.ceil(id.getSelPart().getECGRVal() * 100)/100;
			 * lblText += "</body></html>";
			 * scoreLabR.setText(lblText);
			 */

			lblText = "<html><body>";
			lblText += nf1.format(CurrentOpenData.getInstance().getBloodresistivity()) + " [\u2126/cm]<br>";
			lblText += nf1.format(edVal) + " [cm]<br>";
			lblText += nf.format(sv) + " [cc]<br>";
			lblText += nf.format(sv_c) + " [cc]<br>";
			lblText += "</body></html>";
			paraLabR.setText(lblText);

		} else if (id.getSelPart().getCalculating() == false && id.getSelPart().getValues() != null
				&& id.getSelPart().getLabel().isECGMissing() == false) {

			lblText = "<html><body>";

			lblText += "" + "<br>";
			lblText += nf.format(hra) + " [bpm]<br>";
			lblText += "" + "<br>";
			lblText += "" + "<br>";
			lblText += "" + "<br>";
			lblText += "" + "<br>";
			lblText += "</body></html>";
			calcParaLabR.setText(lblText);

			/*
			 * lblText = "<html><body>";
			 * lblText += "<br>";
			 * lblText += "<br>";
			 * lblText += "<br>";
			 * lblText += nf.format(pOnsetTime * 1000) + " [msec]<br>";
			 * lblText += nf.format(pTime * 1000) + " [msec]<br>";
			 * 
			 * if(id.getSelPart().getLabel().isECGQOnsetMissing()==true){
			 * lblText += misVal+ "<br>";
			 * }else{
			 * lblText += nf.format(id.getSelPart().getECGQPoint()/ 1000) + " [msec]<br>";
			 * }
			 * 
			 * if(id.getSelPart().getLabel().isECGQPointMissing()==true){
			 * lblText += misVal+ "<br>";
			 * } else {
			 * lblText += nf.format(id.getSelPart().getECGQOnsetPoint()/ 1000) +
			 * " [msec]<br>";
			 * }
			 * 
			 * lblText += nf.format( STime * 1000) + " [msec]<br>";
			 * lblText += nf.format(SOffsetTime* 1000) + " [msec]<br>";
			 * lblText += nf.format(tTime * 1000) + " [msec]<br>";
			 * lblText += nf.format(id.getSelPart().getECGTVal()) + " [mV]<br>";
			 * lblText += nf.format(TOffsetTime* 1000) + " [msec]<br>";
			 * lblText += "</body></html>";
			 * scoreLabR.setText(lblText);
			 */
		}
	}

}
