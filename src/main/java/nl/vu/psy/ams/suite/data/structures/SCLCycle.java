package nl.vu.psy.ams.suite.data.structures;

/*
 * Structure that holds information for
 * a single SCL Cycle
 */
public class SCLCycle  implements Comparable<SCLCycle>{
	
	private boolean stimulus;
	private boolean	isPeak;
	private boolean isOnset;
	
	private double  stimulustime;
	private double  latencytime;
	private double  onsettime;
	private double  onsetvalue;
	private double  peaktime;
	private double  peakvalue;
	private double	SCLValue;
	private double	Time;
	
	public SCLCycle() {

	}

	public SCLCycle(double Time) {
		this.Time = Time;
	}
	
	//------------------- Getters------------------------------
	
	public boolean isStimulusApplied(){
		return stimulus;
	}
	
	public boolean isOnset() {
		return isOnset;
	}
	
	public boolean isPeak() {
		return isPeak;
	}
	
	public double getStimulusTime() {
		return stimulustime;
	}
	
	public double getLatencyTime() {
		return latencytime;
	}
	
	public double getOnsetTime() {
		return onsettime;
	}
	
	public double getPeakTime() {
		return peaktime;
	}
	
	public double getOnsetValue(){
		return onsetvalue;
	}
	
	public double getPeakValue(){
		return peakvalue;
	}
	
	public double getSCLValue(){
		return SCLValue;
	}
	
	public double getTime(){
		return Time;
	}
	
	//--------------------- Setters---------------------------------
	public void setStimulus(boolean state){
		stimulus = state;
	}
	
	public void setOnset(boolean state) {
		isOnset = state;
	}
	
	public void setPeak(boolean state){
		isPeak = state;
	}
	
	public void setStimulusTime(double time){
		stimulustime = time;
	}
	
	public void setLatencyTime(double time){
		latencytime = time;
	}
	
	public void setOnsetTime(double time){
		onsettime = time;
	}
	public void setPeakTime(double time){
		peaktime = time;
	}
	
	public void setOnsetValue(double value){
		onsetvalue = value;
	}
	
	public void setPeakValue(double value){
		peakvalue = value;
	}
	

	public void setSCLValue(double value){
		SCLValue = value;
	}

	public void setTime(double time){
		Time = time;
	}

	@Override
	public int compareTo(SCLCycle o) {
		return Double.compare(Time, o.Time);
	}

}
