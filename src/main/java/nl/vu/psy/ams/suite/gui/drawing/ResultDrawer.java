package nl.vu.psy.ams.suite.gui.drawing;

import java.awt.Graphics2D;

import java.awt.Color;
import nl.vu.psy.ams.suite.data.CurrentOpenData;
import nl.vu.psy.ams.suite.data.structures.AmsLabel;
import nl.vu.psy.ams.suite.data.structures.sets.AmsLabelSet;
import nl.vu.psy.ams.suite.gui.axes.YAxis;
import nl.vu.psy.ams.suite.gui.tabs.info.LabelInformationTab;
import nl.vu.psy.ams.suite.tools.Utils;

public class ResultDrawer extends DataDrawer {

    private String colName;

    public ResultDrawer(String colName, YAxis yAxis) {
        super(colName, yAxis);
        this.colName = colName;
    }

    public ResultDrawer(String colName) {
        this(colName, new YAxis(0, 1000.0, 0, 1000.0, colName));
    }

    @Override
    public void drawData(Graphics2D g) {
        double lTime = graph.getxAxis().getLeftTime();
        double rTime = graph.getxAxis().getRightTime();
        double bVal = yAxis.getBottomValue();
        double tVal = yAxis.getTopValue();
        int w = graph.getWidth();
        int h = graph.getHeight();
        g.setColor(Color.BLACK);
        AmsLabelSet ls = CurrentOpenData.getInstance().getLabels();
        int i = 0;
        if (ls.getLabels().size() < 2)
            return;
        for (AmsLabel l : ls.getLabels()) {
            if (i == 0) {
                i++;
                continue;
            }
            AmsLabel l2 = ls.getLabelBeforeTime(l.getLeftTime());
            double lPos = (l2.getRightTime() + l2.getLeftTime()) / 2.0;
            double rPos = (l.getRightTime() + l.getLeftTime()) / 2.0;
            if (rPos < lTime || !l.isTimeLabel() || !l2.isTimeLabel()) {
                i++;
                continue;
            }
            if (lPos > rTime)
                break;
            Double lVal, rVal;
            if (colName.equals("PEP [msec]")) {
                lVal = l2.getPEP();
                rVal = l.getPEP();
            } else if (colName.equals("LVET [msec]")) {
                lVal = l2.getLVET();
                rVal = l.getLVET();
            } else {
                Object[] values = LabelInformationTab.getInstance().getData().getValues(colName);
                if (lPos > rTime || i >= values.length)
                    break;
                if (values[i] instanceof Integer || values[i - 1] instanceof Integer) {
                    i++;
                    continue; // missing value
                }
                if (values[i] instanceof String || values[i - 1] instanceof String)
                    break; // still calculating
                lVal = (Double) values[i - 1];
                rVal = (Double) values[i];
            }

            if (lVal != null && rVal != null && lVal != -9999 && rVal != -9999) {
                int lPix = Utils.getPixelCoordinate(lPos, lTime, rTime, w);
                int rPix = Utils.getPixelCoordinate(rPos, lTime, rTime, w);
                int lPixVal = Utils.getPixelCoordinate(lVal, bVal, tVal, h);
                int rPixVal = Utils.getPixelCoordinate(rVal, bVal, tVal, h);
                g.drawLine(lPix, h - lPixVal, rPix, h - rPixVal);
            }
            i++;
        }
        this.getYAxis().autoScale();
    }

    @Override
    public double[] getBounds() {
        double lTime = graph.getxAxis().getLeftTime();
        double rTime = graph.getxAxis().getRightTime();
        double ret[] = new double[2];
        ret[0] = Double.POSITIVE_INFINITY;
        ret[1] = Double.NEGATIVE_INFINITY;
        AmsLabelSet ls = CurrentOpenData.getInstance().getLabels();
        int i = 0;
        for (AmsLabel l : ls.getLabels()) {
            double rPos = (l.getRightTime() + l.getLeftTime()) / 2.0;
            if (rPos < lTime || !l.isTimeLabel()) {
                i++;
                continue;
            }
            if (rPos > rTime)
                break;
            Double rVal;
            if (colName.equals("PEP [msec]")) {
                rVal = l.getPEP();
            } else if (colName.equals("LVET [msec]")) {
                rVal = l.getLVET();
            } else {
                Object[] values = LabelInformationTab.getInstance().getData().getValues(colName);
                if (rPos > rTime || i >= values.length)
                    break;
                if (values[i] instanceof Integer || values[i - 1] instanceof Integer) {
                    i++;
                    continue; // missing value
                }
                if (values[i] instanceof String || values[i - 1] instanceof String)
                    break; // still calculating
                rVal = (Double) values[i];
            }

            if (rVal != null && rVal != -9999) {
                if (rVal < ret[0])
                    ret[0] = rVal;
                if (rVal > ret[1])
                    ret[1] = rVal;
            }
            i++;
        }
        return ret;
    }

    @Override
    public double getAverageBetweenTimes(double lTime, double rTime) {
        return Double.NaN;
    }

    @Override
    public double getStdDevBetweenTimes(double lTime, double rTime) {
        return Double.NaN;
    }

}
