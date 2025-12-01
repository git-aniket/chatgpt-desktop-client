package nl.vu.psy.ams.suite.device7;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import nl.vu.psy.ams.suite.data.ComplementaryFilter;
import nl.vu.psy.ams.suite.data.structures.file7fs.Ams7fsChannelInfo;
import nl.vu.psy.ams.suite.gui.drawing.OnlineDrawer;
import nl.vu.psy.ams.suite.gui.tabs.inspect.YawPitchRollVisualizer;
import nl.vu.psy.ams.suite.device7.onlineFiltering.ButterworthFilter;
import nl.vu.psy.ams.suite.device7.onlineFiltering.ButterworthFilter2Order;
import nl.vu.psy.ams.suite.device7.onlineFiltering.ButterworthFilter3Order;
// import nl.vu.psy.ams.suite.device7.GaitParametersGenerator;

/*
 * Thread that repeatedly asks for data from the
 * AMS device in online mode.
 */
public class OnlineDataGetter7 extends Thread {

    private AmsDevice7 ams;
    private boolean started;
    private ArrayList<OnlineDrawer> odList, odListF;
    private YawPitchRollVisualizer vis;
    private int chan = 3;
    private boolean dt = false;

    private int curDiv;
    @SuppressWarnings("unused")
    private boolean firstrun = true;

    private boolean isDZ;
    private boolean autoScaled = false;
    private static final int WAIT_TIME_FOR_AUTOSCALE = 2000; // do automatic autoScale after channel switch after x ms

    private List<ButterworthFilter> bfList = new ArrayList<ButterworthFilter>();
    private final String[] filteredChans = { "ECG", "V2ecg", "V3ecg", "magX", "magY", "magZ", "MXR", "MYR", "MZR",
            "GyroX", "GyroY", "GyroZ", "SCL" };
    // private GaitParametersGenerator GPG = new GaitParametersGenerator();
    // MadgwickFilter DeviceOrientation = new MadgwickFilter(1000);
    ComplementaryFilter DeviceOrientation = new ComplementaryFilter();

    public OnlineDataGetter7(AmsDevice7 ams, ArrayList<OnlineDrawer> od, ArrayList<OnlineDrawer> odF,
            OnlineDialog7 dialog, YawPitchRollVisualizer vis) {
        this.ams = ams;
        this.odList = od;
        this.odListF = odF;
        this.vis = vis;
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

    public synchronized boolean isDZ() {
        return isDZ;
    }

    public synchronized boolean isStarted() {
        return started;
    }

    @Override
    public void run() {
        for (OnlineDrawer od : odList) {
            od.setConnectionStatus(OnlineDrawer.CONNECTED);
        }

        // Declare butterworth filters of required cufoff-freq and sampling rate
        for (int i = 0; i < 13; i++) {
            // if (i == 0) // for DZDT
            // bfList.add(new ButterworthFilter3Order(1000, 60));
            if (i <= 2) { // for ECG
                if (ams.isBLE) // correct for 10x downsampling in device
                    bfList.add(new ButterworthFilter3Order(100, 2));
                else
                    bfList.add(new ButterworthFilter3Order(1000, 2));
            }
            if (i == 3) // SCL; may need fixing for sample freq
                bfList.add(new ButterworthFilter3Order(100, 2));
            if (i > 3 && i < 7) // for magnetometer sampling rate is 50 Hz
                bfList.add(new ButterworthFilter2Order(50, 5));
            if (i >= 7) { // for other sensors, sampling rate is 1000 Hz
                if (ams.isBLE) // correct for 10x downsampling in device
                    bfList.add(new ButterworthFilter2Order(100, 5));
                else
                    bfList.add(new ButterworthFilter2Order(1000, 5));
            }
        }
        long startTime = System.currentTimeMillis();

        // helper variables for parameter calculations
        double[] AX = new double[1], AY = new double[1], AZ = new double[1], GX = new double[1], GY = new double[1],
                GZ = new double[1], MX = new double[1], MY = new double[1], MZ = new double[1],
                meanMot = new double[1], meanMotF = new double[1];

        // Infinite Loop for loading data into online plot streams
        while (true) {
            ams.getOnlineData();

            // real time plotter variables
            int[] data, ticks, dataFiltered, ticksG = new int[1];
            double[] dataD, dataFilteredD;
            int i = 0, j = 0, k = 0;
            int Asize = ams.recordedChannels.get(0).size();
            int Bsize = ams.recordedChannels.get(1).size();
            int Dsize = ams.recordedChannels.get(2).size();
            int Gsize = ams.recordedChannels.get(3).size();
            int Msize = ams.recordedChannels.get(4).size();
            ticks = new int[1];
            // scan data from all of device channels
            for (Ams7fsChannelInfo chan : ams.channelInfo) {

                // ADC data
                if (i < Asize) {
                    // break on empty listOfListsA when device disconnects
                    if (ams.listOfListsA.size() == 0) {
                        i++;
                        if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals))
                            k++;
                        j = 0;
                        continue;
                    }
                    odList.get(i).setLastGetTime(Calendar.getInstance().getTimeInMillis());
                    odList.get(i).setLastGetTick(ams.listOfListsA.get(0).get(Asize + 1));
                    if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals)) {
                        odListF.get(k).setLastGetTime(Calendar.getInstance().getTimeInMillis());
                        odListF.get(k).setLastGetTick(ams.listOfListsA.get(0).get(Asize + 1));
                    }
                    data = new int[ams.listOfListsA.size()];
                    dataFiltered = new int[ams.listOfListsA.size()];
                    ticks = new int[ams.listOfListsA.size()];
                    if (chan.getSzID().equals("SCL")) {
                        dataD = new double[ams.listOfListsA.size()];
                        dataFilteredD = new double[ams.listOfListsA.size()];
                        Set<String> variables = new HashSet<String>(chan.getConstants().keySet());
                        variables.add(chan.getSzID());
                        Expression e = new ExpressionBuilder(chan.getFormula()).variables(variables).build()
                                .setVariables(chan.getConstants());
                        for (List<Integer> innerList : ams.listOfListsA) {
                            double val = innerList.get(6);
                            e.setVariable(chan.getSzID(), val);
                            dataD[j] = e.evaluate();
                            if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals)) {
                                double temp = ((ButterworthFilter3Order) bfList.get(k)).process(dataD[j]);
                                dataFilteredD[j] = temp;
                            }
                            ticks[j] = innerList.get(Asize + 1);
                            j++;
                        }
                        odList.get(i).addData(dataD);
                        odList.get(i).addTicks(ticks);
                        if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals)) {
                            odListF.get(k).addData(dataFilteredD);
                            odListF.get(k).addTicks(ticks);
                            k++;
                        }
                    } else {
                        for (List<Integer> innerList : ams.listOfListsA) {
                            data[j] = innerList.get(i);
                            if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals)) {
                                double temp = ((ButterworthFilter3Order) bfList.get(k)).process((double) data[j]);
                                dataFiltered[j] = (int) temp;
                            }
                            ticks[j] = innerList.get(Asize + 1);
                            j++;
                            // if (j == Asize)
                            // break;
                        }
                        odList.get(i).addData(data);
                        odList.get(i).addTicks(ticks);
                        if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals)) {
                            odListF.get(k).addData(dataFiltered);
                            odListF.get(k).addTicks(ticks);
                            k++;
                        }
                    }
                }

                // Battery data
                else if (i < Asize + Bsize) {
                    if (ams.listOfListsB.size() == 0) {
                        i++;
                        if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals))
                            k++;
                        j = 0;
                        continue;
                    }
                    odList.get(i).setLastGetTime(Calendar.getInstance().getTimeInMillis());
                    odList.get(i).setLastGetTick(ams.listOfListsB.get(0).get(Bsize));
                    data = new int[ams.listOfListsB.size()];
                    ticks = new int[ams.listOfListsB.size()];
                    for (List<Integer> innerList : ams.listOfListsB) {
                        data[j] = innerList.get(i - Asize);
                        ticks[j] = innerList.get(Bsize);
                        j++;
                    }
                    odList.get(i).addData(data);
                    odList.get(i).addTicks(ticks);
                }

                // (Druk)Pressure+ temperature data
                else if (i < Asize + Bsize + Dsize) {
                    if (ams.listOfListsD.size() == 0) {
                        i++;
                        if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals))
                            k++;
                        j = 0;
                        continue;
                    }
                    odList.get(i).setLastGetTime(Calendar.getInstance().getTimeInMillis());
                    odList.get(i).setLastGetTick(ams.listOfListsD.get(0).get(Dsize));
                    data = new int[ams.listOfListsD.size()];
                    ticks = new int[ams.listOfListsD.size()];
                    Set<String> variables = new HashSet<String>(chan.getConstants().keySet());
                    variables.add(chan.getSzID());
                    if (chan.getSzID().equals("P_sc")) {
                        variables.add("T_sc");
                    }
                    Expression e = new ExpressionBuilder(chan.getFormula()).variables(variables).build()
                            .setVariables(chan.getConstants());
                    if (chan.getSzID().equals("P_sc")) {
                        Ams7fsChannelInfo chanT = null;
                        for (Ams7fsChannelInfo s : ams.channelInfo) {
                            if (s.getSzID().equals("T_sc")) {
                                chanT = s;
                            }
                        }
                        for (List<Integer> innerList : ams.listOfListsD) {
                            double val = innerList.get(0) * chan.getRealSlope();
                            double valT = innerList.get(1) * chanT.getRealSlope();
                            e.setVariable("P_sc", val);
                            e.setVariable("T_sc", valT);
                            data[j] = (int) e.evaluate();
                            ticks[j] = innerList.get(Dsize);
                            j++;
                        }
                    } else {
                        for (List<Integer> innerList : ams.listOfListsD) {
                            double val = innerList.get(1) * chan.getRealSlope();
                            e.setVariable(chan.getSzID(), val);
                            data[j] = (int) e.evaluate();
                            ticks[j] = innerList.get(Dsize);
                            j++;
                        }
                    }
                    odList.get(i).addData(data);
                    odList.get(i).addTicks(ticks);
                }

                // Data from Magnetometer
                else if (i < Asize + Bsize + Dsize + Gsize) {
                    if (ams.listOfListsG.size() == 0) {
                        i++;
                        if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals))
                            k++;
                        j = 0;
                        continue;
                    }
                    odList.get(i).setLastGetTime(Calendar.getInstance().getTimeInMillis());
                    odList.get(i).setLastGetTick(ams.listOfListsG.get(0).get(ams.listOfListsG.get(0).size() - 1));
                    if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals)) {
                        odListF.get(k).setLastGetTime(Calendar.getInstance().getTimeInMillis());
                        odListF.get(k)
                                .setLastGetTick(ams.listOfListsG.get(0).get(ams.listOfListsG.get(0).size() - 1));
                    }
                    data = new int[ams.listOfListsG.size()];
                    if (chan.getSzID().equals("magX"))
                        MX = new double[ams.listOfListsG.size()];
                    else if (chan.getSzID().equals("magY"))
                        MY = new double[ams.listOfListsG.size()];
                    else if (chan.getSzID().equals("magZ"))
                        MZ = new double[ams.listOfListsG.size()];
                    dataFiltered = new int[ams.listOfListsG.size()];
                    ticksG = new int[ams.listOfListsG.size()];
                    for (List<Integer> innerList : ams.listOfListsG) {
                        data[j] = innerList.get(i - (Asize + Bsize + Dsize));
                        if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals)) {
                            double temp = ((ButterworthFilter2Order) bfList.get(k)).process((double) data[j]);
                            dataFiltered[j] = (int) temp;

                            if (chan.getSzID().equals("magX"))
                                MX[j] = temp * chan.getRealSlope();
                            else if (chan.getSzID().equals("magY"))
                                MY[j] = temp * chan.getRealSlope();
                            else if (chan.getSzID().equals("magZ"))
                                MZ[j] = temp * chan.getRealSlope();
                        }
                        ticksG[j] = innerList.get(innerList.size() - 1);
                        j++;
                    }
                    odList.get(i).addData(data);
                    odList.get(i).addTicks(ticksG);
                    if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals)) {
                        odListF.get(k).addData(dataFiltered);
                        odListF.get(k).addTicks(ticksG);
                        k++;
                    }
                }

                // Data from Motility(Accel+Gyro) sensors
                else if (i < Asize + Bsize + Dsize + Gsize + Msize - 1) {
                    if (ams.listOfListsM.size() == 0) {
                        i++;
                        if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals))
                            k++;
                        j = 0;
                        continue;
                    }
                    odList.get(i).setLastGetTime(Calendar.getInstance().getTimeInMillis());
                    odList.get(i).setLastGetTick(ams.listOfListsM.get(0).get(Msize));
                    if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals)) {
                        odListF.get(k).setLastGetTime(Calendar.getInstance().getTimeInMillis());
                        odListF.get(k).setLastGetTick(ams.listOfListsM.get(0).get(Msize));
                    }

                    data = new int[ams.listOfListsM.size()];
                    if (chan.getSzID().equals("MXR"))
                        AX = new double[ams.listOfListsG.size()];
                    else if (chan.getSzID().equals("MYR"))
                        AY = new double[ams.listOfListsG.size()];
                    else if (chan.getSzID().equals("MZR")) {
                        AZ = new double[ams.listOfListsG.size()];
                        meanMot = new double[ams.listOfListsM.size()];
                        meanMotF = new double[ams.listOfListsM.size()];
                    } else if (chan.getSzID().equals("GyroX"))
                        GX = new double[ams.listOfListsG.size()];
                    else if (chan.getSzID().equals("GyroY"))
                        GY = new double[ams.listOfListsG.size()];
                    else if (chan.getSzID().equals("GyroZ"))
                        GZ = new double[ams.listOfListsG.size()];

                    dataFiltered = new int[ams.listOfListsM.size()];
                    ticks = new int[ams.listOfListsM.size()];

                    for (List<Integer> innerList : ams.listOfListsM) {
                        data[j] = innerList.get(i - (Asize + Bsize + Dsize + Gsize));
                        ticks[j] = innerList.get(Msize);
                        if (chan.getSzID().equals("GyroZ"))
                            data[j] = (-1) * data[j];
                        if (chan.getSzID().equals("MZR"))
                            data[j] = (-1) * data[j];
                        if (chan.getSzID().equals("magZ"))
                            data[j] = (-1) * data[j];

                        if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals)) {
                            double temp = ((ButterworthFilter2Order) bfList.get(k)).process((double) data[j]);
                            dataFiltered[j] = (int) temp;
                        }
                        j++;
                    }
                    for (int l = 0; l < ticksG.length; l++) {
                        for (int m = 0; m < ticks.length; m++) {
                            if (ticks[m] >= ticksG[l]) {
                                try {
                                    if (chan.getSzID().equals("MXR"))
                                        AX[l] = dataFiltered[m] * chan.getRealSlope();
                                    else if (chan.getSzID().equals("MYR"))
                                        AY[l] = dataFiltered[m] * chan.getRealSlope();
                                    else if (chan.getSzID().equals("MZR"))
                                        AZ[l] = dataFiltered[m] * chan.getRealSlope();
                                    else if (chan.getSzID().equals("GyroX"))
                                        GX[l] = dataFiltered[m] * chan.getRealSlope();
                                    else if (chan.getSzID().equals("GyroY"))
                                        GY[l] = dataFiltered[m] * chan.getRealSlope();
                                    else if (chan.getSzID().equals("GyroZ"))
                                        GZ[l] = dataFiltered[m] * chan.getRealSlope();
                                    break;
                                } catch (ArrayIndexOutOfBoundsException e) {
                                    // System.err.println("Error: the array is empty");
                                }
                            }
                        }
                    }
                    for (int m = 0; m < ticks.length; m++) {
                        if (chan.getSzID().equals("MXR")) {
                            meanMotF[m] += Math.pow(dataFiltered[m] * chan.getRealSlope(), 2);
                            meanMot[m] += Math.pow(data[m] * chan.getRealSlope(), 2);
                            meanMotF[m] = Math.sqrt(meanMotF[m]);
                            meanMot[m] = Math.sqrt(meanMot[m]);
                        } else if (chan.getSzID().equals("MYR")) {
                            meanMotF[m] += Math.pow(dataFiltered[m] * chan.getRealSlope(), 2);
                            meanMot[m] += Math.pow(data[m] * chan.getRealSlope(), 2);
                        } else if (chan.getSzID().equals("MZR")) {
                            meanMotF[m] += Math.pow(dataFiltered[m] * chan.getRealSlope(), 2);
                            meanMot[m] += Math.pow(data[m] * chan.getRealSlope(), 2);
                        }
                    }
                    odList.get(i).addData(data);
                    odList.get(i).addTicks(ticks);
                    if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals)) {
                        odListF.get(k).addData(dataFiltered);
                        odListF.get(k).addTicks(ticks);
                        k++;
                    }
                    // stepinstances
                    if (chan.getSzID().equals("MXR")) {
                        odList.get(odList.size() - 1).addTicks(ticks);
                        odList.get(odList.size() - 1).setLastGetTick(ticks[ticks.length - 1]);
                        odList.get(odList.size() - 1).setLastGetTime(Calendar.getInstance().getTimeInMillis());
                        odList.get(odList.size() - 1).addData(meanMot);
                        odListF.get(odListF.size() - 1).addTicks(ticks);
                        odListF.get(odListF.size() - 1).setLastGetTick(ticks[ticks.length - 1]);
                        odListF.get(odListF.size() - 1).setLastGetTime(Calendar.getInstance().getTimeInMillis());
                        odListF.get(odListF.size() - 1).addData(meanMotF);
                    }
                }

                // ADC tick diff
                else if (i == Asize + Bsize + Dsize + Gsize + Msize - 1) {
                    if (ams.listOfListsA.size() == 0) {
                        i++;
                        if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals))
                            k++;
                        j = 0;
                        continue;
                    }
                    odList.get(i).setLastGetTick(ams.listOfListsA.get(0).get(Asize + 1));
                    odList.get(i).setLastGetTime(Calendar.getInstance().getTimeInMillis());
                    data = new int[ams.listOfListsA.size()];
                    for (List<Integer> innerList : ams.listOfListsA) {
                        data[j] = innerList.get(Asize);
                        j++;
                    }
                    odList.get(i).addData(data);
                    odList.get(i).addTicks(ticks);
                }

                // roll, pitch, yaw
                else {
                    if (i == Asize + Bsize + Dsize + Gsize + Msize) {
                        if (ams.listOfListsM.size() == 0 || ams.listOfListsG.size() == 0) {
                            i++;
                            if (Arrays.stream(filteredChans).anyMatch(chan.getSzID()::equals))
                                k++;
                            j = 0;
                            continue;
                        }
                        odList.get(i).addTicks(ticksG);
                        odList.get(i).setLastGetTick(ticksG[ticksG.length - 1]);
                        odList.get(i).setLastGetTime(Calendar.getInstance().getTimeInMillis());
                        odList.get(i + 1).addTicks(ticksG);
                        odList.get(i + 1).setLastGetTick(ticksG[ticksG.length - 1]);
                        odList.get(i + 1).setLastGetTime(Calendar.getInstance().getTimeInMillis());
                        odList.get(i + 2).addTicks(ticksG);
                        odList.get(i + 2).setLastGetTick(ticksG[ticksG.length - 1]);
                        odList.get(i + 2).setLastGetTime(Calendar.getInstance().getTimeInMillis());
                        double[] roll = new double[ams.listOfListsG.size()];
                        double[] pitch = new double[ams.listOfListsG.size()];
                        double[] yaw = new double[ams.listOfListsG.size()];
                        int timeDiff = 20; // 50 Hz
                        for (int l = 0; l < ams.listOfListsG.size(); l++) {
                            // GPG.processIMUDataMultiAxis(AX[l], AY[l], AZ[l]);
                            if (l > 0)
                                timeDiff = ticksG[l] - ticksG[l - 1];
                            DeviceOrientation.update(GX[l], GY[l], GZ[l], 9.8 * AX[l], 9.8 * AY[l], 9.8 *
                                    AZ[l],
                                    1e2 * MX[l],
                                    1e2 * MY[l], 1e2 * MZ[l], timeDiff / 1000.0);
                            // DeviceOrientation.updateIMU(GX[l], GY[l], GZ[l], 9.8 * AX[l], 9.8 * AY[l],
                            // 9.8 * AZ[l]);
                            roll[l] = DeviceOrientation.getRollDegrees(); // - DeviceOrientation.thetaInit;
                            pitch[l] = DeviceOrientation.getPitchDegrees(); // - DeviceOrientation.phiInit;
                            yaw[l] = DeviceOrientation.getYawDegrees(); // - DeviceOrientation.shiInit;
                            // vis.setRoll(roll[l]);
                            vis.setPitch(pitch[l]);
                            // vis.setYaw(yaw[l]);
                        }
                        odList.get(i).addData(roll);
                        odList.get(i + 1).addData(pitch);
                        odList.get(i + 2).addData(yaw);
                        // stepinstances
                        odList.get(i + 3).addTicks(ticks);
                        odList.get(i + 3).setLastGetTick(ticks[ticks.length - 1]);
                        odList.get(i + 3).setLastGetTime(Calendar.getInstance().getTimeInMillis());
                        odList.get(i + 3).addData(meanMot);
                        odListF.get(odListF.size() - 1).addTicks(ticks);
                        odListF.get(odListF.size() - 1).setLastGetTick(ticks[ticks.length - 1]);
                        odListF.get(odListF.size() - 1).setLastGetTime(Calendar.getInstance().getTimeInMillis());
                        odListF.get(odListF.size() - 1).addData(meanMotF);
                    }
                }
                j = 0;
                i++;
            }
            // end of channel scans

            if (isStarted() == false) {
                return;
            }
            if (System.currentTimeMillis() - startTime > WAIT_TIME_FOR_AUTOSCALE && autoScaled == false) {
                for (int i1 = 0; i1 < odList.size(); i1++) {
                    odList.get(i1).getYAxis().autoScale();
                }
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

    public void setNewAxisTitle(String axisTitle) {
    }

    public void setNewSlopeAndConstant(double lowerBound, double upperBound, double lowerValue, double upperValue) {
    }

    public synchronized void setDZ(boolean b) {
        isDZ = b;
    }

    public synchronized void setStarted(boolean started) {
        this.started = started;
    }

}
