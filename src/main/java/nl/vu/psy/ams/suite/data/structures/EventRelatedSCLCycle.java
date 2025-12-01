package nl.vu.psy.ams.suite.data.structures;
/*
 * Structure that holds information for
 * a single EventSCL Cycle
 */
public class EventRelatedSCLCycle  implements Comparable<EventRelatedSCLCycle>{
	
	private double	Time;
	private int  	eventcode;
	
	private double  SCL_Stimulus;	
	private double  baseline_SCL;
		
	private boolean isSCRPresent;
	
	private double 	Onset_Time;
	private double  Onset_SCL;
	
	private double  peak_Time;
	private double  peak_SCL;
	
	private int     HR; 
	
	public EventRelatedSCLCycle() { 

	}

	public EventRelatedSCLCycle(double Time) {
		this.Time = Time;
	}
	
	//------------------- Getters------------------------------
	
	public double getBaselineSCL() {
		return baseline_SCL;
	}
	
	public double getPeakSCL(){
		return peak_SCL;
	}
	
	public double getPeakTime(){
		return peak_Time;
	}
	
	public double getTime(){
		return Time;
	}
	
	public int getHeartRate(){
		return HR;
	}
	
	public int getEventCode(){
		return eventcode;
	}
	
	public double getSCL_Stimulus() {
		return SCL_Stimulus;
	}

	public boolean isSCRPresent() {
		return isSCRPresent;
	}

	public double getOnset_Time() {
		return Onset_Time;
	}

	
	public double getOnset_SCL() {
		return Onset_SCL;
	}
	
	//--------------------- Setters---------------------------------
	
	
	public void setBaselineSCL(double value){
		baseline_SCL = value;
	}
	
	public void setPeakValue(double value){
		peak_SCL = value;
	}

	public void setPeakTime(double time){
		peak_Time= time;
	}
	
	public void setHeartRate(int heartrate){
		HR = heartrate;
	}
	
	public void setEventCode(int code){
		eventcode = code;
	}
	
	@Override
	public int compareTo(EventRelatedSCLCycle o) {
		return Double.compare(Time, o.Time);
	}
	public void setSCL_Stimulus(double sCL_Stimulus) {
		SCL_Stimulus = sCL_Stimulus;
	}

	public void setSCRPresent(boolean isSCRPresent) {
		this.isSCRPresent = isSCRPresent;
	}
	public void setOnset_Time(double onset_Time) {
		Onset_Time = onset_Time;
	}

	
	public void setOnset_SCL(double onset_SCL) {
		Onset_SCL = onset_SCL;
	}

}
