package nl.vu.psy.ams.suite.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import nl.vu.psy.ams.suite.tools.Utils;
 
public class ReadActigraphRawData {
		private static	double[] 	xData;
		private static double [] 	MXR;
		private static double [] 	MYR;
		private static double [] 	MZR;
		private static String		Xname;
		private static String		Yname;
		private static String		Zname;
		private static boolean		isCalculated	= false;
		
        public static void read() throws FileNotFoundException, ParseException {
        	if (isCalculated){
    			return;
        	}
    		isCalculated = true;
    		xData 								= null;
    		MXR 								= null;
    		MYR 								= null;
    		MZR 								= null;
    		
    		Xname								= "MXR";
    		Yname								= "MYR";
    		Zname								= "MZR";
        	File fl 							= new File(CurrentOpenData.getInstance().getFilePath(), "Actigraph_Motility.dat");
        	String fileToParse 					= fl.getAbsolutePath();
            BufferedReader fileReader 			= new BufferedReader(new FileReader(fileToParse));;    
           
            double sampleFrequency				= 0;
            
            String startDate					="";
            String startTime					="";
            
            ArrayList<Double> x_Axis_Count 		= new ArrayList<Double>(); 
            
            ArrayList<Double> xValues 			= new ArrayList<Double>(); 
            ArrayList<Double> yValues 			= new ArrayList<Double>();
            ArrayList<Double> zValues 			= new ArrayList<Double>();
            
            
            SimpleDateFormat dateformat 		= new SimpleDateFormat("dd-M-yyyy HH:mm:ss");
            Date startDate_actigraph		 	= null;
            Date startDate_amsdata 				= CurrentOpenData.getInstance().getStartDate().getTime();  
            Date endDate_amsdata				= CurrentOpenData.getInstance().getEndDate().getTime();
            
           
            long difference_in_ms_VUAMS			= endDate_amsdata.getTime() - startDate_amsdata.getTime(); // in milliseconds
            long difference						= 0;
            long skip 							= 0;
            double count						= 0;
            long endcount						= 0;
            
         try {

				 String data = fileReader.readLine(); // First Line contains sampling frequency
				 if(data.contains("30 Hz")){
					 sampleFrequency = 30;
				 }else if(data.contains("40 Hz")){
					 sampleFrequency = 40;
				 }else if(data.contains("50 Hz")){
					 sampleFrequency = 50;
				 }else if(data.contains("60 Hz")){
					 sampleFrequency = 60;
				 }else if(data.contains("70 Hz")){
					 sampleFrequency = 70;
				 }else if(data.contains("80 Hz")){
					 sampleFrequency = 80;
				 }else if(data.contains("90 Hz")){
					 sampleFrequency = 90;
				 }else{
					 sampleFrequency = 100;
				 }
				 data = fileReader.readLine(); // Serial Number of Actigraph Device
				 data = fileReader.readLine(); // Start time of actigraph recording
				 if(data.contains("Start Time")){
					 String[] s			=  data.split(" ");
					 startTime 			= s[2];			
				 }
				 data = fileReader.readLine(); // Start Date of actigraph recording
				 if(data.contains("Start Date")){
					 String[] s						=  data.split(" ");
					 startDate 						= s[2];
					 
					 if((!startTime.equals("")) && (!startDate.equals(""))){
						 String dateandtime 		= startDate.concat(" " +startTime);
						 startDate_actigraph 		= Utils.parseDate(dateformat, dateandtime);		
						 
						 if (startDate_actigraph != null) {
							difference 				= startDate_amsdata.getTime() - startDate_actigraph.getTime(); // In milliseconds. This helps to calculate skip and adjust the time scale
							skip 						= (long) (Math.round(difference/1000) * (sampleFrequency)); // Divide by 100o to convert to seconds and then multiply by sampling frequency
							endcount					= (long) (Math.round(difference_in_ms_VUAMS/1000) * (sampleFrequency)); // End count divided by sampling frequency and 60 (min) gives the duration of the recording (in min)
							
							/*
							* If difference is greater than zero, then Acti-graph recording started earlier
							* If difference is lesser than zero,  then VU-AMS recording started earlier
							*/
							System.out.println("VUAMSdata:_ " + startDate_amsdata +"==="+ endDate_amsdata); 
							System.out.println("Actigraph:_ " + startDate_actigraph); 
							
							System.out.println("Difference between amsdata and actigraph:_ " + difference +"===" + skip); 
							System.out.println("Duration of amsdata recording:_ " + difference_in_ms_VUAMS +"==="+ endcount); 
						}
					}
				 }
				 //--------------------------------------------

				 data = fileReader.readLine(); //  Epoch period
				 data = fileReader.readLine(); // Download Time
				 data = fileReader.readLine(); // Download Date
				 data = fileReader.readLine(); // Current Memory Address
				 data = fileReader.readLine(); // Current Battery Voltage
				 data = fileReader.readLine();
				 data = fileReader.readLine(); // Column header
				 data = fileReader.readLine();
					
				 while (data != null){
					 
					 String[] s				= data.split("\",");

					 //String time ;
					 String xval ;
					 String yval ;
					 String zval ;
					 
					 if((count >= skip ) && (count <= (skip + endcount))){
						
						 if(s.length > 3){
							 	//time 		= s[0].replaceAll("\"", "").replaceAll(",","."); // Replaces , with .
							 	xval 		= s[1].replaceAll("\"", "").replaceAll(",","."); // Replaces , with .
							 	yval 		= s[2].replaceAll("\"", "").replaceAll(",","."); // Replaces , with .
							 	zval 		= s[3].replaceAll("\"", "").replaceAll(",","."); // Replaces , with .
						 }else{
								xval 		= s[0].replaceAll("\"", "").replaceAll(",","."); // Replaces , with .
							 	yval 		= s[1].replaceAll("\"", "").replaceAll(",","."); // Replaces , with .
							 	zval 		= s[2].replaceAll("\"", "").replaceAll(",","."); // Replaces , with .
						 }
						 
						 
					 double xvalue 		= Double.parseDouble(xval);
					 double yvalue 		= Double.parseDouble(yval);
					 double zvalue 		= Double.parseDouble(zval);
					 
					 double cc			= (count + (count - skip) * 10);

					 x_Axis_Count.add(cc);				 
					
					 xValues.add(xvalue);
					 yValues.add(yvalue);
					 zValues.add(zvalue);
	
					 }
					 count++;
					 data = fileReader.readLine();
						
				 }
				 fileReader.close();
				 xData 	= new double[xValues.size()];
				 MXR 	= new double[xValues.size()];
				 MYR 	= new double[yValues.size()];
				 MZR 	= new double[zValues.size()];
				 
				 
				for (int i = 0; i < x_Axis_Count.size(); i++) {
					xData[i] = (CurrentOpenData.getInstance().getStartTimeInUS() + 1000. *  (x_Axis_Count.get(i))) ; 
				}
				for (int i = 0; i < xValues.size(); i++) {
					MXR[i] = xValues.get(i);
				}
				for (int i = 0; i < yValues.size(); i++) {
					MYR[i] = yValues.get(i);
				}
				for (int i = 0; i < zValues.size(); i++) {
					MZR[i] = zValues.get(i);
				}

		} catch (IOException e) {
			try {fileReader.close(); }
			catch (IOException f) {}
			e.printStackTrace();
		}
        }

        public static double[] getXData() {
    		return xData;
    	}

    	public static double[] getMXRData() {
    		return MXR;
    	}
    	public static double[] getMYRData() {
    		return MYR;
    	}
    	public static double[] getMZRData() {
    		return MZR;
    	}
    	
    	public static String getXName() {
    		try {
				read();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
    		return Xname;
    	}
    	public static String getYName() {
    		try {
				read();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
    		return Yname;
    	}
    	public static String getZName() {
    		try {
				read();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
    		return Zname;
    	}
}