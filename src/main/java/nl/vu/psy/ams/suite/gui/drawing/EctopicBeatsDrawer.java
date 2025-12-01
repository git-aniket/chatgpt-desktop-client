package nl.vu.psy.ams.suite.gui.drawing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.SortedSet;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.files.BinaryFile;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.ECGBeat;
import nl.vu.psy.ams.suite.data.structures.sets.ArtefactSet;
import nl.vu.psy.ams.suite.data.structures.sets.BeatSet;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.tools.Utils;


public class EctopicBeatsDrawer extends DataDrawer {

private ArtefactSet aSet 				= CurrentOpenData.getInstance().getECGArtefacts();
private BeatSet		bSet				= CurrentOpenData.getInstance().getBeatSet();
ArrayList<AmsLabel> retArray 			= new ArrayList<AmsLabel>();
private				BinaryFile bf 		= new BinaryFile("ECG");
private int 		selPart 			= 0;
private double[] 	ectopicBeatValues;
	
	
	
public EctopicBeatsDrawer(String name, YAxis yAxis, String chan) {
	super(name, yAxis);
	getYAxis().setBottomValue(-6);
	getYAxis().setTopValue(10);				
}

	@Override
public void drawData(Graphics2D g) {		
	double bVal 			= yAxis.getBottomValue();
	double tVal 			= yAxis.getTopValue();
	int w 					= graph.getWidth();
	int h 					= graph.getHeight();
	String text 			= "";
	String beattype 		= "";
		
	if(retArray.size() > 0){
		AmsLabel l;
		if(getSelPart() < retArray.size()){
			l 			= retArray.get(getSelPart());
		}else{
			l 			= retArray.get(getSelPart()-1);
		}		
	
		SortedSet<ECGBeat> beats 	= bSet.getBeatBetweenTimes(l.getLeftTime(), l.getRightTime());
		double duration 			= 0;
		
		if(beats.size() > 1){
			// More than one beat under the current artefact is marked as an ectopic beat which is not possible. 
			g.setStroke(new BasicStroke(10));
			setValues(new double[1]);//to clear graph
			g.setColor(Color.RED);
			FontMetrics fm = g.getFontMetrics();
			int txtH = fm.getMaxAscent();
			g.drawString("More than one beat marked as ectopic beat. Please use one artefact per ectopic beat",200, 80 + txtH / 2);

		}else{
			ECGBeat pb,fb;
			if(beats.size()==0 ){//If beat is deleted. Which is very likely for beat(s) under artefacts!
				pb 				= bSet.getBeatBeforeTime(bSet.getBeatBeforeTime(l.getLeftTime()).getRPeakTime());//get beat 2 positions before
				fb 				= bSet.getBeatAfterTime(bSet.getBeatAfterTime(l.getRightTime()).getRPeakTime());//get beat 2 positions after
			}
			else{
				pb 				= bSet.getBeatBeforeTime(bSet.getBeatBeforeTime(beats.first().getRPeakTime()).getRPeakTime());//get beat 2 positions before
				fb 				= bSet.getBeatAfterTime(bSet.getBeatAfterTime(beats.first().getRPeakTime()).getRPeakTime());//get beat 2 positions after
			}
	
			duration  				= (fb.getRPeakTime() - pb.getRPeakTime()) ;				
			ectopicBeatValues 		= new double[(int)duration/1000];						
			if(l.getAttributes().containsValue("Premature Ventricular Contraction")){
				beattype 			= "Type : Premature Ventricular Contraction";
			}else{
				beattype 			= "Type : Premature Atrial Contraction";
			}	
			setValues(bf.getDataRun(pb.getRPeakTime(),duration));	
		}
		
		text = "Time :"+ Utils.getDateAndTimeFromUS(l.getLeftTime());
		g.setStroke(new BasicStroke(4));		
		g.setColor(Color.RED);
		FontMetrics fm = g.getFontMetrics();
		int txtH = fm.getMaxAscent();
		g.drawString(text,50, 20 + txtH / 2);		
		g.drawString(beattype,50, 40 + txtH / 2);
		drawPart(g, w, h, bVal, tVal);
  }else{
		Font font = new Font("MM", Font.BOLD, 14);
		g.setFont(font);
		g.setStroke(new BasicStroke(10));
		g.setColor(Color.RED);
		FontMetrics fm = g.getFontMetrics();
		int txtH = fm.getMaxAscent();
		g.drawString("No Ectopic Beats Detected",200, 80 + txtH / 2);
  }
}
	
 private void drawPart(Graphics2D g, int w, int h, double bVal, double tVal) {
		
	g.setStroke(new BasicStroke(2));
	g.setColor(Color.BLACK);
	double[] vals 	= getValues();	
	double lT 		= graph.getxAxis().getLeftTime();
	double rT 		= graph.getxAxis().getRightTime();
		
	if (vals == null) {
		g.drawLine(0, h - Utils.getPixelCoordinate(0, bVal, tVal, h), w, h - Utils.getPixelCoordinate(0, bVal, tVal, h));
	} else {
		for (int i = 1; i < vals.length; i++) {			
			double time;
			double ltime;
			time = i *1000 - 600000;
			ltime = (i - 1) *1000 - 600000;				
			int lPos = Utils.getPixelCoordinate(ltime, lT, rT, w);
			int rPos = Utils.getPixelCoordinate(time, lT, rT, w);
			int lVal = Utils.getPixelCoordinate(vals[i - 1], bVal, tVal, h);
			int rVal = Utils.getPixelCoordinate(vals[i], bVal, tVal, h);
			g.drawLine(lPos, h - lVal, rPos, h - rVal);
		}
	}
}
	@Override
public double[] getBounds() {
	if (selPart < 0)
		return null;

	double lT = graph.getxAxis().getLeftTime();
	double rT = graph.getxAxis().getRightTime();
	double[] ret = new double[2];
	ret[0] = Double.POSITIVE_INFINITY;
	ret[1] = Double.NEGATIVE_INFINITY;	
	double[] vals = getValues();
	
	for (int i = 0; i < vals.length; i++) {
		double time = i * 1000 - 1024000;
		if (time > lT && time < rT) {
			if (vals[i] < ret[0])
				ret[0] = vals[i];
			if (vals[i] > ret[1])
				ret[1] = vals[i];
		}
	}
	return ret;
}
	
public void setSelPart(int part) {
	selPart = part;
}
	
public int getSelPart(){
	return selPart;
}
	
public void setValues(double[] values){	
	ectopicBeatValues = values;
}
public double[] getValues(){
	return ectopicBeatValues;
}
	
public void nextPart() {				
	if (selPart < retArray.size()-1){
		setSelPart(selPart + 1);
	}
}
public void delete(){
	AmsLabel l  = retArray.get(getSelPart());
	retArray.remove(l);
	aSet.removeLabel(l);
	aSet.deleteArtefact(l);		
}
public void prevPart() {
	if (selPart > 0)
		setSelPart(selPart - 1);
}
public int getTotalectopicbeatsCount(){
	return retArray.size();
}
	
public void setretArray(){		
	for (AmsLabel l2 : aSet.getLabels()){		
		if((l2.getAttributes().toString().contains("Premature Ventricular Contraction")) || (l2.getAttributes().toString().contains("Premature Atrial Contraction"))){				
			retArray.add(l2);
		}
	}		
}

public double getAverageBetweenTimes(double lTime, double rTime) {
	return Double.NaN;
}

public double getStdDevBetweenTimes(double lTime, double rTime) {
	return Double.NaN;
}
}