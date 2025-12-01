package nl.vu.psy.ams.suite.data.structures.sets;

import java.util.ArrayList;
import java.util.Iterator;

import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.SignalPart;
import nl.vu.psy.ams.suite.gui.tabs.imp.ImpInfoPanel;
import nl.vu.psy.ams.suite.gui.tabs.imp.ImpTab;

public class SignalPartSet {

    private ArrayList<SignalPart> parts = new ArrayList<SignalPart>();
    private ArrayList<SignalPart> improvedParts = new ArrayList<SignalPart>();
    public boolean filtered = true;
    private String chan;

    public SignalPartSet(String chan) {
        this.chan = chan;
    }

    public SignalPartSet(String chan, boolean filtered) {
        this.chan = chan;
        this.filtered = filtered;
    }

    public ArrayList<SignalPart> getParts() {
        return parts;
    }

    public ArrayList<SignalPart> getImpParts() {
        return improvedParts;
    }

    public void setParts(ArrayList<SignalPart> parts) {
        this.parts = parts;
    }

    public void setImpParts(ArrayList<SignalPart> parts) {
        this.improvedParts = parts;
    }

    public void addPart(AmsLabel lab, ArrayList<Integer> removeIndices) {
        SignalPart part = new SignalPart();
        part.setLabel(lab);
        parts.add(part);
        part.setCalculating(true);
        SignalPart impP = new SignalPart();
        impP.setLabel(lab);
        impP.setCalculating(true);
        improvedParts.add(impP);
        int sampleTimeInUS = (int) CurrentOpenData.getInstance().getFileHeader().getDwSampleTime_us();
        try {
            sampleTimeInUS *= CurrentOpenData.getInstance().getChannelInfoFromID(chan).getDwDivider();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        impP.setSampleTimeInUS(sampleTimeInUS);
        SignalPart[] pt = impP.getLabel().getAverageSignalsUnderLabel(chan, filtered, removeIndices);
        if (pt == null) {
            impP.setCalculating(false);
            part.setCalculating(false);
            return;
        }
        if (pt[1] != null) {
            impP.setValues(pt[1].getValues());
            impP.setNumberOfComplexes(pt[1].getnumberOfComplexes());
        } else {
            double[] zero = new double[512000 / sampleTimeInUS];
            for (int i = 0; i < zero.length; i++)
                zero[i] = 0;
            impP.setValues(zero);
            impP.setNumberOfComplexes(0);
        }
        if (isInvert())
            for (int i = 0; i < impP.getValues().length; i++)
                impP.getValues()[i] *= -1;

        // --------------To smooth the complexes - Written by
        // Menaka--------------------------
        // double[] filt = movingAverage(impP.getValues());
        // impP.setValues(filt);
        // ------------------------------------------------
        if (chan.equals("ECG") && impP.isECGMissing() == false) {
            double avheartrate = impP.getLabel().getAverage(true);
            boolean dirty = ImpTab.getInstance().getEso().isDirty();
            if (avheartrate > 0
                    && (dirty || impP.getECGRPoint() == Double.NEGATIVE_INFINITY)) {
                impP.calcECGPoints(avheartrate, false);
            }
        }
        if (chan.equals("DZDT") && impP.isICGMissing() == false) {
            double avheartrate = impP.getLabel().getAverage(true);
            boolean dirty = ImpTab.getInstance().getIso().isDirty();
            if (avheartrate > 0
                    && (dirty || impP.getcPoint() == Double.NEGATIVE_INFINITY)) {
                impP.calcICGPoints(avheartrate);
            }
        }
        impP.setCalculating(false);
        part.setSampleTimeInUS(sampleTimeInUS);
        if (pt[0] != null) {
            part.setValues(pt[0].getValues());
            part.setNumberOfComplexes(pt[0].getnumberOfComplexes());
        } else {
            double[] zero = new double[part.getValues().length];
            for (int i = 0; i < zero.length; i++)
                zero[i] = 0;
            part.setValues(zero);
            part.setNumberOfComplexes(0);
        }
        if (isInvert())
            for (int i = 0; i < part.getValues().length; i++)
                part.getValues()[i] *= -1;
        part.setCalculating(false);
    }

    public boolean isInvert() {
        return (chan.equals("DZDT"));
    }

    public void removePart(AmsLabel lab) {
        Iterator<SignalPart> iter = parts.iterator();

        while (iter.hasNext()) {
            SignalPart p = iter.next();

            if (p.getLabel() == lab)
                iter.remove();

        }
        iter = improvedParts.iterator();

        while (iter.hasNext()) {
            SignalPart p = iter.next();

            if (p.getLabel() == lab)
                iter.remove();

        }
    }

    public void editPart(AmsLabel lab, ArrayList<Integer> removeIndices) {
        removePart(lab);
        addPart(lab, removeIndices);
    }

    public void recalculate(ArrayList<Integer>[] outer) {
        parts.clear();
        improvedParts.clear();
        // try {
        // if (CurrentOpenData.getInstance().getLoadLabelThread() != null)
        // CurrentOpenData.getInstance().getLoadLabelThread().join();
        // if (CurrentOpenData.getInstance().getTimeLabelThread() != null)
        // CurrentOpenData.getInstance().getTimeLabelThread().join();
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }
        for (AmsLabel l : CurrentOpenData.getInstance().getLabels().getLabels()) {
            SignalPart prt = new SignalPart();
            prt.setLabel(l);
            parts.add(prt);
        }
        for (SignalPart p : parts) {
            p.setCalculating(true);
            SignalPart impP = new SignalPart();
            impP.setLabel(p.getLabel());
            impP.setCalculating(true);
            improvedParts.add(impP);
        }
        // if (recalcThread != null) {
        // // recalcThread.interrupt();
        // try {
        // recalcThread.join();
        // } catch (InterruptedException e) {
        // }
        // ThreadServer.removeThread(recalcThread);
        // }
        // recalcThread = new Thread() {
        // @Override
        // public void run() {
        int sampleTimeInUS = (int) CurrentOpenData.getInstance().getFileHeader().getDwSampleTime_us();
        try {
            sampleTimeInUS *= CurrentOpenData.getInstance().getChannelInfoFromID(chan).getDwDivider();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        for (int j = 0; j < parts.size(); j++) {
            improvedParts.get(j).setSampleTimeInUS(sampleTimeInUS);
            SignalPart[] pt = improvedParts.get(j).getLabel().getAverageSignalsUnderLabel(chan, filtered,
                    outer[j]);
            if (pt == null) {
                improvedParts.get(j).setCalculating(false);
                parts.get(j).setCalculating(false);
                continue;
            }
            if (pt[1] != null) {
                improvedParts.get(j).setValues(pt[1].getValues());
                improvedParts.get(j).setNumberOfComplexes(pt[1].getnumberOfComplexes());
            } else {
                double[] zero = new double[512000 / sampleTimeInUS];
                for (int i = 0; i < zero.length; i++)
                    zero[i] = 0;
                improvedParts.get(j).setValues(zero);
                improvedParts.get(j).setNumberOfComplexes(0);
            }
            if (isInvert())
                for (int i = 0; i < improvedParts.get(j).getValues().length; i++)
                    improvedParts.get(j).getValues()[i] *= -1;

            // --------------To smooth the complexes - Written by
            // Menaka--------------------------
            // double[] filt = movingAverage(improvedParts.get(j).getValues());
            // improvedParts.get(j).setValues(filt);
            // ------------------------------------------------
            if (chan.equals("ECG") && improvedParts.get(j).isECGMissing() == false) {
                double avheartrate = improvedParts.get(j).getLabel().getAverage(true);
                boolean dirty = ImpTab.getInstance().getEso().isDirty();
                if (avheartrate > 0
                        && (dirty || improvedParts.get(j).getECGRPoint() == Double.NEGATIVE_INFINITY)) {
                    improvedParts.get(j).calcECGPoints(avheartrate, false);
                }
            }
            if (chan.equals("DZDT") && improvedParts.get(j).isICGMissing() == false) {
                double avheartrate = improvedParts.get(j).getLabel().getAverage(true);
                boolean dirty = ImpTab.getInstance().getIso().isDirty();
                if (avheartrate > 0
                        && (dirty || improvedParts.get(j).getcPoint() == Double.NEGATIVE_INFINITY)) {
                    improvedParts.get(j).calcICGPoints(avheartrate);
                }
            }
            improvedParts.get(j).setCalculating(false);
            parts.get(j).setSampleTimeInUS(sampleTimeInUS);
            if (pt[0] != null) {
                parts.get(j).setValues(pt[0].getValues());
                parts.get(j).setNumberOfComplexes(pt[0].getnumberOfComplexes());
            } else {
                double[] zero = new double[parts.get(j).getValues().length];
                for (int i = 0; i < zero.length; i++)
                    zero[i] = 0;
                parts.get(j).setValues(zero);
                parts.get(j).setNumberOfComplexes(0);
            }
            if (isInvert())
                for (int i = 0; i < parts.get(j).getValues().length; i++)
                    parts.get(j).getValues()[i] *= -1;
            parts.get(j).setCalculating(false);
            // if (iip != null)
            // iip.updateLabelTexts();
            // if (isInterrupted())
            // return;

        }
        // }

        // };ThreadServer.addNewThread(recalcThread);
        // recalcThread.start();
        ImpInfoPanel iip = ImpTab.getInstance().getIip();
        if (iip != null)
            iip.updateLabelTexts();
    }

    public void recalculatePoints() {
        for (SignalPart prt : improvedParts) {
            if (chan.equals("ECG") && prt.isECGMissing() == false) {
                double avheartrate = prt.getLabel().getAverage(true);
                if (avheartrate > 0) {
                    prt.getLabel().clearICGScoring();
                    prt.calcECGPoints(avheartrate, false);
                }
            }
            if (chan.equals("DZDT") && prt.isICGMissing() == false) {
                double avheartrate = prt.getLabel().getAverage(true);
                if (avheartrate > 0) {
                    prt.calcICGPoints(avheartrate);
                }
            }
        }
    }
}
