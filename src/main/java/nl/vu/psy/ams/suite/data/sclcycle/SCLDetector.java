package nl.vu.psy.ams.suite.data.sclcycle;

import java.util.SortedSet;

import javax.swing.ProgressMonitor;
import nl.vu.psy.ams.suite.data.structures.SCLCycle;
import nl.vu.psy.ams.suite.tools.ProgressInterface;
/*
 * Interface that can detect SCL cycle from FilteredSCL file.
 * 
*/
public interface SCLDetector {
	
	public SortedSet<SCLCycle> findCycle(double leftTime, double rightTime, ProgressInterface pi);
	public SortedSet<SCLCycle> findCycle(double leftTime, double rightTime);

	public void setMonitor(ProgressMonitor mon);
}
