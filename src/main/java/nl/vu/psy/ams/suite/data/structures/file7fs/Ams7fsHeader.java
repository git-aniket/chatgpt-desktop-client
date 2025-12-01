package nl.vu.psy.ams.suite.data.structures.file7fs;

import java.util.GregorianCalendar;
import java.util.HashMap;

import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsHeader;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsTime;
import nl.vu.psy.ams.suite.data.structures.file5fs.Ams5fsTimeZoneDate;
import nl.vu.psy.ams.suite.tools.Utils;
/*
 * Header info from 5fs file (copied from 5fs code)
 */
public class Ams7fsHeader extends Ams5fsHeader {

	public static final int	SIZE	= 512;

	// device info
	String		dwSoftwareVersion7 = "0";	// 0C - device software version
	String		dwSessionStr = "0";
	String		studyId = "";
	String		comment = "";
	protected Integer	wElectrodeDistance;		// 32 -
	
	public Ams7fsHeader() {

	}

	public Ams7fsHeader(HashMap<String, String> map, int nChannels) {
		GregorianCalendar cal = new GregorianCalendar();
	
		sFileID = "AMS7";
		// dwEndianTag = buff.getUInt();
		// wTag = buff.getUShort();
		// wSize = buff.getUShort();
		dwSoftwareVersion7 = map.get("App version");
		dwHardwareVersion = 7;
		dwSerialNumber = map.get("S/N");
		deviceId = map.get("SENSORNAME");
		if (dwSerialNumber == null || dwSerialNumber.equals("0"))
			dwSerialNumber = "MAC:" + map.get("MAC");
		szCompileDate = map.get("Compile time");
		szProducer = "AMS - Vrije Universiteit van Amsterdam - FPP - ITM";
		settStamp(new Ams5fsTime(cal));
		// dstBegin = new Ams5fsTimeZoneDate(buff);
		// dstEnd = new Ams5fsTimeZoneDate(buff);
		// lTimeZoneJump = buff.getInt();
		szSubjectID = map.get("SUBJECT_ID");
		if (szSubjectID == null)
			szSubjectID = map.get("NICKNAME");
		String distStr = map.get("E_DISTANCE");
		if (distStr != null)
			wElectrodeDistance = Utils.parseInt(distStr);
		setDwSampleTime_us(1000L);
		this.nChannels = nChannels;
		// dwFileVersion = buff.getUInt();
		// szReserved = buff.getString(364);
		studyId = map.get("STUDY_ID");
		comment = map.get("COMMENT");
		dwSessionStr = map.get("SESSION_ID");
		try {
			dwSession = Long.valueOf(dwSessionStr);
		} catch (NumberFormatException e) {
			dwSession = 0;
		}
	}

	public Ams5fsTimeZoneDate getDstBegin() {
		return dstBegin;
	}

	public Ams5fsTimeZoneDate getDstEnd() {
		return dstEnd;
	}

	public long getDwEndianTag() {
		return dwEndianTag;
	}

	public long getDwFileVersion() {
		return dwFileVersion;
	}

	public long getDwHardwareVersion() {
		return dwHardwareVersion;
	}

	public long getDwSampleTime_us() {
		return dwSampleTime_us;
	}

	public String getDwSerialNumber() {
		return dwSerialNumber;
	}

	public String getDwSession() {
		return dwSessionStr;
	}

	public String getStudyId() {
		return studyId;
	}

	public String getComment() {
		return comment;
	}

	public long getDwSoftwareVersion7() {
		return 0;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public String getFirmwareVersion() {
		if (dwHardwareVersion != 7) {
			if (super.dwSoftwareVersion > 6) {
				return ((super.dwSoftwareVersion >> 16) & 0xFF) + "." + ((super.dwSoftwareVersion >> 8) & 0xFF) + "." + (super.dwSoftwareVersion & 0xFF);
			}
			return Long.toString(super.dwSoftwareVersion);
		} else	
			return dwSoftwareVersion7;
	}

	public int getlTimeZoneJump() {
		return lTimeZoneJump;
	}

	public long getnChannels() {
		return nChannels;
	}

	public String getsFileID() {
		return sFileID;
	}

	public String getSzCompileDate() {
		return szCompileDate;
	}

	public String getSzProducer() {
		return szProducer;
	}

	public String getSzReserved() {
		return szReserved;
	}

	public String getSzSubjectID() {
		return szSubjectID;
	}

	public Ams5fsTime gettStamp() {
		return tStamp;
	}

	public int getwSize() {
		return wSize;
	}

	public int getwTag() {
		return wTag;
	}

	public Integer getwElectrodeDistance() {
		return wElectrodeDistance;
	}

	public void setDstBegin(Ams5fsTimeZoneDate dstBegin) {
		this.dstBegin = dstBegin;
	}

	public void setDstEnd(Ams5fsTimeZoneDate dstEnd) {
		this.dstEnd = dstEnd;
	}

	public void setDwEndianTag(long dwEndianTag) {
		this.dwEndianTag = dwEndianTag;
	}

	public void setDwFileVersion(long dwFileVersion) {
		this.dwFileVersion = dwFileVersion;
	}

	public void setDwHardwareVersion(long dwHardwareVersion) {
		this.dwHardwareVersion = dwHardwareVersion;
	}

	public void setDwSampleTime_us(long dwSampleTime_us) {
		this.dwSampleTime_us = dwSampleTime_us;
	}

	public void setDwSerialNumber(String dwSerialNumber) {
		this.dwSerialNumber = dwSerialNumber;
	}

	public void setDwSession(String dwSession) {
		this.dwSessionStr = dwSession;
	}

	public void setDwSoftwareVersion7(String dwSoftwareVersion) {
		this.dwSoftwareVersion7 = dwSoftwareVersion;
	}

	public void setlTimeZoneJump(int lTimeZoneJump) {
		this.lTimeZoneJump = lTimeZoneJump;
	}

	public void setnChannels(long nChannels) {
		this.nChannels = nChannels;
	}

	public void setsFileID(String sFileID) {
		this.sFileID = sFileID;
	}

	public void setSzCompileDate(String szCompileDate) {
		this.szCompileDate = szCompileDate;
	}

	public void setSzProducer(String szProducer) {
		this.szProducer = szProducer;
	}

	public void setSzReserved(String szReserved) {
		this.szReserved = szReserved;
	}

	public void setSzSubjectID(String szSubjectID) {
		this.szSubjectID = szSubjectID;
	}

	public void settStamp(Ams5fsTime tStamp) {
		this.tStamp = tStamp;
	}

	public void setwSize(int wSize) {
		this.wSize = wSize;
	}

	public void setwTag(int wTag) {
		this.wTag = wTag;
	}

	public void setwElectrodeDistance(Integer wElectrodeDistance) {
		this.wElectrodeDistance = wElectrodeDistance;
	}
	public void setAmsDataFileVersion(String d){
		this.AMSDataFileVersion = d;
	}
	public String getAmsDataFileVersion(){
		return AMSDataFileVersion;
	}
	
	public void setVUDAMSSoftwareVersion(String SoftwareVersion){
		this.VUDAMSSoftwareVersion = SoftwareVersion;
	}
	public String getVUDAMSSoftwareVersion(){
		return VUDAMSSoftwareVersion;
	}
	
	
	public double getSpecificBloodResistivity() {
		return SpecificBloodResistivity;
	}

	public void setSpecificBloodResistivity(double specificBloodResistivity) {
		SpecificBloodResistivity = specificBloodResistivity;
	}
	public double getAge() {
		return age;
	}

	public void setAge(double age) {
		this.age = age;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public int getGender() {
		return gender;
	}

	public void setGender(int gender) {
		this.gender = gender;
	}

	@Override
	public String toString() {
		return "Ams5fsHeader [" + (sFileID != null ? "sFileID=" + sFileID + ", " : "") + "dwEndianTag=" + dwEndianTag + ", wTag=" + wTag + ", wSize=" + wSize
				+ ", dwSoftwareVersion=" + dwSoftwareVersion7 + ", dwHardwareVersion=" + dwHardwareVersion + ", dwSerialNumber=" + dwSerialNumber + ", "
				+ (szCompileDate != null ? "szCompileDate=" + szCompileDate + ", " : "") + (szProducer != null ? "szProducer=" + szProducer + ", " : "")
				+ (tStamp != null ? "tStamp=" + tStamp + ", " : "") + (dstBegin != null ? "dstBegin=" + dstBegin + ", " : "")
				+ (dstEnd != null ? "dstEnd=" + dstEnd + ", " : "") + "lTimeZoneJump=" + lTimeZoneJump + ", "
				+ (szSubjectID != null ? "szSubjectID=" + szSubjectID + ", " : "") + "dwSession=" + dwSessionStr + ", dwSampleTime_us=" + dwSampleTime_us
				+ ", nChannels=" + nChannels + ", dwFileVersion=" + dwFileVersion + ", " 
				+ (szReserved != null ? "szReserved=" + szReserved : "") 
				+ ", AmsDataFileVersion="+ 			  AMSDataFileVersion 
				+", VUDAMSSoftwareVersion		= "	+ VUDAMSSoftwareVersion 
				+", SpecificBloodResistivity	= " + SpecificBloodResistivity
				+", gender						= "	+ gender
				+", age							= "	+ age
				+", weight						= "	+ weight
				+", height						= "	+ height			
				+",]";
	}

}
