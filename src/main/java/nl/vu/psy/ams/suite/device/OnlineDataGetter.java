package nl.vu.psy.ams.suite.device;

import java.util.Calendar;

import javax.swing.JButton;

import nl.vu.psy.ams.suite.gui.drawing.OnlineDrawer;
import nl.vu.psy.ams.suite.tools.SCLValueConvertor;
/*
 * Thread that repeatedly asks for data from the
 * AMS device in online mode. If connection is lost,
 * it enables the reconnect button in the online dialog.
 */
public class OnlineDataGetter extends Thread {

	private static final int	AMSII_ONLINE_TAG_DATA	= 1;
	private AmsDevice			ams;
	private boolean				started;
	private OnlineDrawer		od;
	private int					chan					= 3;
	private boolean				dt						= false;

	private int					curChannel				= -1;
	private int					curDiv;
	@SuppressWarnings("unused")
	private JButton				button;

	private boolean				firstrun				= true;

	private boolean				isMYA;

	private String				axisTitle				= "TEST";

	private double				realSlope				= 1;

	private double				realConstant			= 1;
	
	private boolean				isScl; 
	private boolean				isDZ;
	private boolean				isDT					= false;
	private OnlineDialog 		dialog;
	private int 				dat[];
	
	private boolean				autoScaled 				= false;
	private static final int	WAIT_TIME_AFTER_SWITCH	= 1000; // after channel switch wait for artifact peak before starting graph
	private static final int	WAIT_TIME_FOR_AUTOSCALE	= 2000; // do automatic autoScale after channel switch after x ms
	
	public OnlineDataGetter(AmsDevice ams, OnlineDrawer od, JButton reconnectBut, OnlineDialog dialog) {
		this.ams = ams;
		this.od = od;
		this.button = reconnectBut;
		this.dialog = dialog;
	}
	
	public synchronized int getChannel() {
		return chan;
	}
	public synchronized boolean getDT() {
		return dt;
	}
	public synchronized int getCurDiv() {
		return curDiv;
	}
	private synchronized boolean isMYA() {
		return isMYA;
	}
	public synchronized boolean isSCL() {
		return isScl;
	}
	public synchronized boolean isDZ() {
		return isDZ;
	}

	public synchronized boolean isStarted() {
		return started;
	}

	@Override
	public void run() {
		od.setConnectionStatus(OnlineDrawer.CONNECTED);
		AmsHelper h = new AmsHelper();
		long switchTime = 0;
		while (true) {
			int sel 	= getChannel();
			boolean selDT	= getDT();
			int oldDiv 	= getCurDiv();
			int newDiv 	= 0;
			int ci 		= (int)((ams.getSettings().channels[sel].getDivider()*3)/4);
			if (curChannel != sel || isDT != selDT) {
				newDiv = oldDiv;
				ams.startOnline(sel, newDiv);
				switchTime = System.currentTimeMillis();
				autoScaled = false;
				// System.out.println("Divider:_ " + ci);
				//----------- SCL Online mode - Connection Lost Problem - Solved with divider---------------							
				/*try {
					Thread.sleep(ci);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				dat = ams.getOnlineDataAndContinue();*/
				//------------------------------------------------------------------------------------------
				firstrun = true;
				od.setSlopeAndConstant(realSlope, realConstant);
				od.getYAxis().setAxisTitle(axisTitle);
				//int datInit = h.GetUnsignedShortFromArrayOffset(dat, 10);
				//if (curChannel != -1)
				od.setDT(selDT);
				curChannel = sel;
				isDT = selDT;
			}
			//----------- SCL Online mode - Connection Lost Problem - Solved with divider---------------
			//else{
				try {
					Thread.sleep(ci);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				dat = ams.getOnlineDataAndContinue();
			
			//}		
			//------------------------------------------------------------------------------------------
			

			boolean done = (dat != null) && (h.GetUnsignedShortFromArrayOffset(dat, 2) == AMSII_ONLINE_TAG_DATA);

			long startTime = System.currentTimeMillis();
			
			while (done == false) {
				
				//------------------- When no data is received- add zero to the graph. This will create a gap and can be used to visualize data lost----------------------
				int[] datazero = new int[1];
				for(int j = 0; j< 1; j++){
					datazero[j] = 0;
				}
				od.addData(datazero);
				//--------------------------------------------------------------------------------------------------------------------------------------------------------
				
				dat = ams.getOnlineDataAndContinue();
				done = (dat != null) && (h.GetUnsignedShortFromArrayOffset(dat, 2) == AMSII_ONLINE_TAG_DATA);
				if (((System.currentTimeMillis() - startTime)>=2000) && (done==false)) {
					od.setConnectionStatus(OnlineDrawer.CONNECTIONLOST);
					dialog.reconnect();
					if(isStarted() == true){
						done = true;
						return;
					}
					startTime = System.currentTimeMillis();
					return;
					}
			}
			
				h.GetUnsignedShortFromArrayOffset(dat, 4);
				h.GetUnsignedLongFromArrayOffset(dat, 6);
				int nSamples = (dat.length - 10) / 2;
				
				if (firstrun) {
					od.setLastGetTime(Calendar.getInstance().getTimeInMillis());
					firstrun = false;
				} else {
					long newTime = od.getLastGetTime() + nSamples * oldDiv;
					if (newTime > Calendar.getInstance().getTimeInMillis()) {
						nSamples = (int) (Calendar.getInstance().getTimeInMillis() - od.getLastGetTime()) / oldDiv;
						newTime = od.getLastGetTime() + nSamples * oldDiv;
					}
					od.setLastGetTime(newTime);
				}
				if (nSamples != 0) {
					
					int[] data = new int[nSamples];
					int q = 0;  
					for (int i = 10; i < dat.length; i += 2) {
						data[q] = h.GetUnsignedShortFromArrayOffset(dat, i);
						
						q++;
						
						if (q == nSamples)
							break;
					}
					if (isMYA()) {
						long nSamplesInSum = ams.getSettings().channels[AmsDeviceConstants.CHX_MYA].dwDivider;
						for (int i = 0; i < data.length; i++) {
							long shift = data[i] & 0x0F;
							data[i] = (data[i] >> 4);
							data[i] = (data[i] << shift);
							data[i] = (int) Math.round((double) data[i] / nSamplesInSum);
						}
					}
					if (isSCL()) {
						for (int i = 0; i < data.length; i++) {
							if(data[i] == 65535)  data[i] = 65534;//JdH Ugly way to fix ArrayIndexOutOfBoundsException when only SCL is enabled
							data[i] = (SCLValueConvertor.convertValue(data[i] + Short.MIN_VALUE) - Short.MIN_VALUE);
						}
					}
					
					if (System.currentTimeMillis() - switchTime > WAIT_TIME_AFTER_SWITCH) {
						od.addData(data);
					} else {
						/*for (int i = 0; i < data.length; i++)
							System.out.print(data[i]+" ");
						System.out.print("\r\n");*/
						od.clear(data[data.length - 1]);
					}
				}
				
				startTime = od.getLastGetTime();
			
				if(isStarted() == false){
					ams.stopOnline();
					od.setConnectionStatus(OnlineDrawer.CONNECTIONLOST);
					return;
				}
				
				if (System.currentTimeMillis() - switchTime > WAIT_TIME_FOR_AUTOSCALE && autoScaled == false) {
					if (isDZ())
						dialog.yAxis.setValues(dialog.yAxis.getSampleValueFromRealValue(-1.1), dialog.yAxis.getSampleValueFromRealValue(1.1));
					else
						dialog.yAxis.autoScale();
					autoScaled = true;
				}
		}
		
		
	

	}

	public synchronized void setChannel(int chan) {
		this.chan = chan;
	}

	public synchronized void setDT(boolean dt) {
		this.dt = dt;
	}

	public synchronized void setCurDiv(int curDiv) {
		this.curDiv = curDiv;
	}

	public synchronized void setMYA(boolean mya) {
		this.isMYA = mya;
	}

	public void setNewAxisTitle(String axisTitle) {
		this.axisTitle = axisTitle;
	}

	public void setNewSlopeAndConstant(double lowerBound, double upperBound, double lowerValue, double upperValue) {
		realSlope = (upperValue - lowerValue) / (upperBound - lowerBound);
		realConstant = lowerValue - realSlope * lowerBound;
	}
	public synchronized void setSCL(boolean b) {
		isScl = b;
	}
	
	public synchronized void setDZ(boolean b) {
		isDZ = b;
	}

	public synchronized void setStarted(boolean started) {
		this.started = started;
	}
	
}
