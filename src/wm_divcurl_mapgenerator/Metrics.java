/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wm_divcurl_mapgenerator;

import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.MaximumFinder;
import ij.process.ImageStatistics;
import java.awt.Polygon;
import java.awt.Rectangle;

/**
 *
 * @author Meenakshi
 */
public class Metrics {

    private ImagePlus imp = null;
    private Polygon peaksList = null;
    private PointRoi[] pcsROI = null;
    private double[] pcsXList = null;
    private double[] pcsYList = null;
    private OvalRoi[] searchAreaROI = null;
    private double[] searchAreaXList = null;
    private double[] searchAreaYList = null;
    private double[] intensity = null;
    private double[] rsqList = null;
    private boolean[] successArr = null;
    private Roi sampledPixelsROI = null;
    
    public Metrics(ImagePlus IMP, Roi r) {
        //initialize
        imp = IMP.duplicate();
        sampledPixelsROI = (Roi) r.clone();
        //normalise to max intensity
        ImageStatistics is = ImageStatistics.getStatistics(imp.getProcessor());
        double maxInt = is.max;
        imp.getProcessor().multiply(1/maxInt);
        //get peaks, calculate metric
        getPeaks();        
        setMetric();
    }

    public PointRoi[] getMetricPcsROI() {
        return pcsROI;
    }

    public OvalRoi[] getSearchAreaROI() {
        return searchAreaROI;
    }

    public double[] getIntensity() {
        return intensity;
    }

    public ResultsTable getMetricsAsResultsTable() {
        ResultsTable rt = new ResultsTable();
        for (int i = 0; i < intensity.length; i++) {
            boolean success = successArr[i];
            double PcsX = pcsXList[i];
            double PcsY = pcsYList[i];
            double SAX = searchAreaXList[i];
            double SAY = searchAreaYList[i];
            double intValue = intensity[i];
            double rsq = rsqList[i];
            System.out.println("No.: " + i);
            System.out.println("FitStatus: " + success);
            System.out.println("PcsX: " + PcsX);
            System.out.println("PcsY: " + PcsY);
            double pcsxVal = PcsX + sampledPixelsROI.getBounds().x;
            double pcsyVal = PcsY + sampledPixelsROI.getBounds().y;
            System.out.println("PcsX_corr: " + pcsxVal);
            System.out.println("PcsY_corr: " + pcsyVal);
            System.out.println("BoundX: " + sampledPixelsROI.getBounds().x);
            System.out.println("BoundY: " + sampledPixelsROI.getBounds().y);            
            System.out.println("SearchAreaX: " + SAX);
            System.out.println("SearchAreaY: " + SAY);
            System.out.println("Intensity: " + intValue);
            System.out.println("R-sq: " + rsq);
            //create new file, make line tab, and then output the file
            //setValue​(java.lang.String column, int row, double value)
            rt.setValue​("Pcs No.", i, i+1);
            rt.setValue​("PcsX", i, pcsxVal);
            rt.setValue​("PcsY", i, pcsyVal);
            rt.setValue​("SearchAreaX", i, SAX);
            rt.setValue​("SearchAreaY", i, SAY);
            rt.setValue​("Intensity", i, intValue);
            rt.setValue​("R-sq", i, rsq);
        }
        return rt;
    }

    private void setMetric() {
        pcsROI = new PointRoi[peaksList.npoints];
        pcsXList = new double[peaksList.npoints];
        pcsYList = new double[peaksList.npoints];        
        searchAreaROI = new OvalRoi[peaksList.npoints];
        intensity = new double[peaksList.npoints];
        searchAreaXList = new double[peaksList.npoints];
        searchAreaYList = new double[peaksList.npoints];
        successArr = new boolean[peaksList.npoints];
        rsqList = new double[peaksList.npoints];

        for (int i = 0; i < peaksList.npoints; i++) {
            int xPt = peaksList.xpoints[i];
            int yPt = peaksList.ypoints[i];
            //code to do 2d gauss fit
            GaussFit2D gf = new GaussFit2D(imp, xPt, yPt);
            gf.doIterativeFit();
            //success of fit. Determines whether to print the results
            successArr[i] = gf.getSuccess();

            //populate the metric arrays
            //for accuracy
            PointRoi pcsEstimate = new PointRoi(gf.getx0(), gf.gety0());
            pcsROI[i] = (PointRoi) pcsEstimate.clone();
            pcsXList[i] = gf.getx0();
            pcsYList[i] = gf.gety0();

            //for uncertainty in search
            OvalRoi SA = new OvalRoi(0, 0, gf.getFWHMx(), gf.getFWHMy());
            searchAreaROI[i] = (OvalRoi) SA.clone();
            searchAreaXList[i] = gf.getFWHMx();
            searchAreaYList[i] = gf.getFWHMy();

            //for intensity
            intensity[i] = gf.getAmplitude();
            
            //for r-sq
            rsqList[i] = gf.getRsq();
        }
    }

    private static double[] getStats(double[] o) {
        int minIdx = 0;
        double min = Double.MAX_VALUE;
        int maxIdx = 0;
        double max = Double.MIN_VALUE;
        double sum = 0d;
        double stdDev = 0d; //forgot how to calculate programatically - DO LATER AFTER CHECKING NOTES    
        for (int i = 0; i < o.length; i++) {
            double value = o[i];
            if (value < min) {
                min = value;
                minIdx = i;
            }
            if (value > max) {
                max = value;
                maxIdx = i;
            }
            sum = sum + value;
        }
        double mean = sum / o.length;

        double[] results = new double[4];
        results[0] = minIdx;
        results[1] = min;
        results[2] = maxIdx;
        results[3] = max;
        results[4] = mean;
        results[5] = stdDev;
        return results;
    }

    private void getPeaks() {
        Rectangle bounds = sampledPixelsROI.getBounds();
        MaximumFinder mf = new MaximumFinder();
        Polygon points = mf.getMaxima(imp.getProcessor(), 0.01, true, true); //strict, excludes edges, 1%
        peaksList = new Polygon();

        for (int i = 0; i < points.npoints; i++) {
            int xPt = points.xpoints[i];
            int yPt = points.ypoints[i];
            int boundX = bounds.x;
            int boundY = bounds.y;

            if (gui_wErr.pool.containsPoint(xPt + boundX, yPt + boundY)) {
                peaksList.addPoint(xPt, yPt);
                System.out.println("xPt on cropped image: " + xPt);
                System.out.println("yPt on cropped image: " + yPt);
            }
        }
    }
    
}
