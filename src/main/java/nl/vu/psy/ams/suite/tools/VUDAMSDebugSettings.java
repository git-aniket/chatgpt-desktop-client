package nl.vu.psy.ams.suite.tools;

public class VUDAMSDebugSettings {

	int onlinehiddenchannels			= 0;
	int showPVC							= 0;//JdH 20160105 Seems to be doing nothing
	int showVariance					= 0;
	int enableECGFilter					= 0;//JdH 20160614 Seems to be doing nothing
	int useNewPCBZ0Signal				= 0;//JdH 20160105 Seems to be doing nothing
	int enableVO2Estimation				= 0;//JdH 20160614 Seems to be doing nothing
	int addPhysiologicalData			= 0;//JdH 20160614 Seems to be doing nothing ("Add Demographic Data" always visible)
	int importactigraphData				= 0;
	int recalculateFiltDZ				= 0;
	
	double ECGMin						= 0;
	double ECGMax						= 0;
	
	double DZMin						= 0;
	double DZMax						= 0;
	
	double DZDTMin						= 0;
	double DZDTMax						= 0;
	
	double Z0Min						= 0;
	double Z0Max						= 0;
	
	double MotilityXYZMin				= 0;
	double MotilityXYZMax				= 0;
	
	double MotilityRawMin				= 0;
	double MotilityRawMax				= 0;
	
	double BATMin						= 0;
	double BATMax						= 0;
	
	double SCLMin						= 0;
	double SCLMax						= 0;
	
	double PCGMin						= 0;
	double PCGMax						= 0;
	
	
	public double getECGMax() {
		return ECGMax;
	}

	public void setECGMax(double eCGMax) {
		ECGMax = eCGMax;
	}

	public double getDZMin() {
		return DZMin;
	}

	public void setDZMin(double dZMin) {
		DZMin = dZMin;
	}

	public double getDZMax() {
		return DZMax;
	}

	public void setDZMax(double dZMax) {
		DZMax = dZMax;
	}

	public double getDZDTMin() {
		return DZDTMin;
	}

	public void setDZDTMin(double dZDTMin) {
		DZDTMin = dZDTMin;
	}

	public double getDZDTMax() {
		return DZDTMax;
	}

	public void setDZDTMax(double dZDTMax) {
		DZDTMax = dZDTMax;
	}

	public double getZ0Min() {
		return Z0Min;
	}

	public void setZ0Min(double z0Min) {
		Z0Min = z0Min;
	}

	public double getZ0Max() {
		return Z0Max;
	}

	public void setZ0Max(double z0Max) {
		Z0Max = z0Max;
	}

	public double getMotilityXYZMin() {
		return MotilityXYZMin;
	}

	public void setMotilityXYZMin(double motilityXYZMin) {
		MotilityXYZMin = motilityXYZMin;
	}

	public double getMotilityXYZMax() {
		return MotilityXYZMax;
	}

	public void setMotilityXYZMax(double motilityXYZMax) {
		MotilityXYZMax = motilityXYZMax;
	}

	public double getMotilityRawMin() {
		return MotilityRawMin;
	}

	public void setMotilityRawMin(double motilityRawMin) {
		MotilityRawMin = motilityRawMin;
	}

	public double getMotilityRawMax() {
		return MotilityRawMax;
	}

	public void setMotilityRawMax(double motilityRawMax) {
		MotilityRawMax = motilityRawMax;
	}

	public void setImportactigraphData(int importactigraphData) {
		this.importactigraphData = importactigraphData;
	}


	public double getBATMin() {
		return BATMin;
	}

	public void setBATMin(double bATMin) {
		BATMin = bATMin;
	}

	public double getBATMax() {
		return BATMax;
	}

	public void setBATMax(double bATMax) {
		BATMax = bATMax;
	}

	public double getSCLMin() {
		return SCLMin;
	}

	public void setSCLMin(double sCLMin) {
		SCLMin = sCLMin;
	}

	public double getSCLMax() {
		return SCLMax;
	}

	public void setSCLMax(double sCLMax) {
		SCLMax = sCLMax;
	}

	public double getPCGMin() {
		return PCGMin;
	}

	public void setPCGMin(double pCGMin) {
		PCGMin = pCGMin;
	}

	public double getPCGMax() {
		return PCGMax;
	}

	public void setPCGMax(double pCGMax) {
		PCGMax = pCGMax;
	}

	public int getEnableECGFilter() {
		return enableECGFilter;
	}
	public int getEnableVO2Estimation() {
		return enableVO2Estimation;
	}

	public void setEnableECGFilter(int enableECGFilter) {
		this.enableECGFilter = enableECGFilter;
	}

	public void setUseNewPCBZ0Signal(int newsignalenable){
		useNewPCBZ0Signal = newsignalenable;		
	}
	
	public int getUseNewPCBZ0Signal() {
		return useNewPCBZ0Signal;
	}
	
	public double getECGMin() {
		return ECGMin;
	}

	public void setECGMin(double eCGMin) {
		ECGMin = eCGMin;
	}


	public void setECGMaax(double eCGMax) {
		ECGMax = eCGMax;
	}

	public VUDAMSDebugSettings() {

	}

	public int getOnlinehiddenchannels() {
		return onlinehiddenchannels;
	}


	public void setOnlinehiddenchannels(int onlinehiddenchannels) {
		this.onlinehiddenchannels = onlinehiddenchannels;
	}


	public int getShowPVC() {
		return showPVC;
	}


	public void setShowPVC(int showPVC) {
		this.showPVC = showPVC;
	}


	public int getShowVariance() {
		return showVariance;
	}


	public void setShowVariance(int showVariance) {
		this.showVariance = showVariance;
	}

	public void setEnableVO2Estimation(int enableVO2Estimation) {
		this.enableVO2Estimation = enableVO2Estimation;
	}

	
	public int getAddPhysiologicalData() {
		return addPhysiologicalData;
	}

	public void setAddPhysiologicalData(int addPhysiologicalData) {
		this.addPhysiologicalData = addPhysiologicalData;
	}
	
	public int getImportactigraphData() {
		return importactigraphData;
	}
	
	public int getRecalculateFiltDZ() {
		return recalculateFiltDZ;
	}

	public void setRecalculateFiltDZ(int recalculateFiltDZ) {
		this.recalculateFiltDZ = recalculateFiltDZ;
	}

	@Override
	public String toString() {
		return "VUDAMSDebugSetings [" + ", onlinehiddenchannels=" + onlinehiddenchannels + ", showPVC=" + showPVC
				+ ", showVariance	=" + showVariance 		+ ", enableECGFilter=" + enableECGFilter +  ", " + ", useNewPCBZ0Signal=" + useNewPCBZ0Signal +  ", "		
				+ ", ECGMin			=" + ECGMin 			+ ", ECGMax			=" + ECGMax 		 +  ", "
				+ ", DZMin			=" + DZMin 				+ ", DZMax			=" + DZMax 		 	 +  ", "
				+ ", DZDTMin		=" + DZDTMin 			+ ", DZDTMax		=" + DZDTMax 		 +  ", "
				+ ", Z0Min			=" + Z0Min 				+ ", Z0Max			=" + Z0Max 		 	 +  ", "
				+ ", MotilityXYZMin	=" + MotilityXYZMin 	+ ", MotilityXYZMax	=" + MotilityXYZMax  +  ", "
				+ ", MotilityRawMin	=" + MotilityRawMin 	+ ", MotilityRawMax	=" + MotilityRawMax  +  ", "
				+ ", BATMin			=" + BATMin 			+ ", BATMax			=" + BATMax 		 +  ", "
				+ ", SCLMin			=" + SCLMin 			+ ", SCLMax			=" + SCLMax 		 +  ", "
				+ ", PCGMin			=" + PCGMin 			+ ", PCGMax			=" + PCGMax 		 +  ", "
				+ ", enableVO2Estimation	=" + enableVO2Estimation 								+  ", "	
				+ ", addPhysiologicalData	=" + addPhysiologicalData 								+  ","
				+ ", importactigraphData	=" + importactigraphData 								+  ","
				+ ", recalculateFiltDZ		=" + recalculateFiltDZ
				+",]";
	}

}