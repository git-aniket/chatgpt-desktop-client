package nl.vu.psy.ams.suite.data.structures.file5fs;

import nl.vu.psy.ams.suite.tools.UnsignedByteBuffer;
/*
 * Header info from 5fs file (copied from 5fs code)
 */
public class Ams5fsHeader {

	public static final int	SIZE	= 512;

	protected String					sFileID;			// 00 - 'A' 'M' 'S' '2' or 'I',
												// 'N', 'V', '2'
	protected long					dwEndianTag;		// 04 - 0x04030201
	protected int						wTag;				// 06 - type tag to identify
												// header structure
	protected int						wSize;				// 08 - total nr of bytes of
												// this structure, excl. channel
												// info
	// device info
	public long					dwSoftwareVersion;	// 0C - device software version
	protected long					dwHardwareVersion;	// 10 - device revision
	protected String					dwSerialNumber;	// 14 - device serial number
	protected String					szCompileDate;		// 18 - __DATE__
	// recording info
	protected String					szProducer;		// 24 - AMS5fs - Vrije
												// Universiteit van Amsterdam -
												// FPP -
												// ITM
	protected Ams5fsTime				tStamp;			// 64 - file creation time and
												// date
	protected Ams5fsTimeZoneDate		dstBegin;			// 6C
	protected Ams5fsTimeZoneDate		dstEnd;			// 70
	protected int						lTimeZoneJump;		// 74
	protected String					szSubjectID;		// 78 - subject/recording ID
	protected long					dwSession;			// 84 - recording session
												// sequence number
	protected long					dwSampleTime_us;	// 88 - sample interval time in
												// microseconds
	// channel info
	protected long					nChannels;			// 8C - # recorded channels
	protected long					dwFileVersion;		// 90 - file version, field
												// added after 1.0.6.7 because
												// (amsinv) files can change
												// even if device software
												// doesn't
	protected String					szReserved;		// 94
	protected String					AMSDataFileVersion 			= "0" ;
	protected String 					VUDAMSSoftwareVersion 		= "0" ;
	protected double					SpecificBloodResistivity 	= 0;
	
	// Add PhysiologicalData of the Subject
	protected double					age							= 0;
	protected double					height						= 0; // cm
	protected double					weight						= 0; // kg
	protected int						gender						= 0; // 1 is Male and 0 is Female
	protected String					deviceId = "";
	
	
	public Ams5fsHeader() {

	}

	public Ams5fsHeader(UnsignedByteBuffer buff) {
		sFileID = buff.getString(4);
		dwEndianTag = buff.getUInt();
		wTag = buff.getUShort();
		wSize = buff.getUShort();
		dwSoftwareVersion = buff.getUInt();
		dwHardwareVersion = buff.getUInt();
		dwSerialNumber = Long.toString(buff.getUInt());
		szCompileDate = buff.getString(12);
		szProducer = buff.getString(64);
		tStamp = new Ams5fsTime(buff);
		dstBegin = new Ams5fsTimeZoneDate(buff);
		dstEnd = new Ams5fsTimeZoneDate(buff);
		lTimeZoneJump = buff.getInt();
		szSubjectID = buff.getString(12);
		dwSession = buff.getUInt();
		dwSampleTime_us = buff.getUInt();
		nChannels = buff.getUInt();
		dwFileVersion = buff.getUInt();
		szReserved = buff.getString(364);
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

	public String getDeviceId() {
		return deviceId;
	}

	public String getDwSession() {
		return String.valueOf(dwSession);
	}

	public String getStudyId() {
		return "N/A";
	}

	public String getComment() {
		return "N/A";
	}

	public long getDwSoftwareVersion() {
		return dwSoftwareVersion;
	}

	public String getFirmwareVersion() {
		if (dwSoftwareVersion > 6) {
			return ((dwSoftwareVersion >> 16) & 0xFF) + "." + ((dwSoftwareVersion >> 8) & 0xFF) + "." + (dwSoftwareVersion & 0xFF);
		}
		return Long.toString(dwSoftwareVersion);
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

	public void setDwSession(long dwSession) {
		this.dwSession = dwSession;
	}

	public void setDwSoftwareVersion(long dwSoftwareVersion) {
		this.dwSoftwareVersion = dwSoftwareVersion;
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
				+ ", dwSoftwareVersion=" + dwSoftwareVersion + ", dwHardwareVersion=" + dwHardwareVersion + ", dwSerialNumber=" + dwSerialNumber + ", "
				+ (szCompileDate != null ? "szCompileDate=" + szCompileDate + ", " : "") + (szProducer != null ? "szProducer=" + szProducer + ", " : "")
				+ (tStamp != null ? "tStamp=" + tStamp + ", " : "") + (dstBegin != null ? "dstBegin=" + dstBegin + ", " : "")
				+ (dstEnd != null ? "dstEnd=" + dstEnd + ", " : "") + "lTimeZoneJump=" + lTimeZoneJump + ", "
				+ (szSubjectID != null ? "szSubjectID=" + szSubjectID + ", " : "") + "dwSession=" + dwSession + ", dwSampleTime_us=" + dwSampleTime_us
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
