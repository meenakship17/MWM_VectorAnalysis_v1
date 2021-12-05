/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wm_divcurl_mapgenerator;

import ij.ImagePlus;
import ij.measure.CurveFitter;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.util.ArrayList;

/**
 *
 * @author Meenakshi
 */
public class GaussFit2D {

    private static ImagePlus imp = null;
    private int size_of_patch = 20; //D61G,Bl6,DBA = 20, Balbc = 41, Swiss = 100,98 
    private static int xPt = 0;
    private static int yPt = 0;
    private double amplitude = Double.NaN; //A, amplitude of the Gaussian function
    private double a_ = Double.NaN; //a, 
    private double x0 = Double.NaN; //x0, x-position in patch
    private double b_ = Double.NaN; //b
    private double y0 = Double.NaN; //y0, y-position in patch
    private double FWHMx = Double.NaN; //FWHM in x
    private double FWHMy = Double.NaN; //FWHM in y
    private double rsq = Double.NaN;
    private boolean success = false;

    public GaussFit2D(ImagePlus IMP, int x, int y) {
        imp = IMP.duplicate();
        xPt = x;
        yPt = y;
    }

    public double getAmplitude(){
    return amplitude;
    }
    
    public double geta_(){
    return a_;
    }
    
    public double getb_(){
    return b_;
    }
    
    public double getx0(){
    return x0;
    }
    
    public double gety0(){
    return y0;
    }
    
    public double getFWHMx(){
    return FWHMx;
    }
    
    public double getFWHMy(){
    return FWHMy;
    }    
    
    public double getRsq(){
    return rsq;
    }
    
    public boolean getSuccess(){
    return success;
    }
    
    public void doIterativeFit() {
        ImageProcessor ip = imp.getProcessor();
        int fitCounter = 0;
        while (!success && fitCounter < 1) { //no iteration at the moment
            double[] x = new double[size_of_patch * size_of_patch];
            double[] o = new double[size_of_patch * size_of_patch];
            int count = 0;
            for (int j = 0; j < size_of_patch; j++) {
                for (int i = 0; i < size_of_patch; i++) {
                    int xpix = i + xPt - Math.round(size_of_patch / 2);
                    int ypix = j + yPt - Math.round(size_of_patch / 2);
                    o[count] = ip.getPixelValue(xpix, ypix);
                    x[count] = count;
                    count = count + 1;
                }
            }
            //Calls the function which fits the distribution within the patch.
            success = fit_gaussian_diag_cov(size_of_patch, size_of_patch, x, o);
            success = true; //comment out once you incorportate iterative fitting
            if(success){
            x0 = x0 + xPt - Math.round(size_of_patch/2)+ 0.5;
            y0 = y0 + yPt - Math.round(size_of_patch/2)+ 0.5;
            return;
            }
//            size_of_patch = size_of_patch + 10;
            fitCounter = fitCounter + 1;
        }
    }
    
    private boolean fit_gaussian_diag_cov(int wid, int hei, double[] x, double[] o) {
        boolean success = true;
        //Get the statistics from the array.
        //Array.getStatistics(o, min, max, mean, stdDev);
        double[] stats = this.getStats(o);
        double max = stats[1];

        //2D Gaussian equation.
        String eq = "y =a*exp(-(b*(pow(((x-(floor(x/" + wid + ")*" + wid + "))-c),2))+d*pow(((floor(x/" + wid + "))-e),2)))";
        CurveFitter Fit = new CurveFitter(x, o);
        //Fit.doFit(f, x, o,newArray(max,0.1,wid/2,0.1,hei/2));

        double r_sq = 0;
        double[] initialParams = {max, 0.1, wid / 2, 0.1, hei / 2};
        Fit.doCustomFit(eq, initialParams, false); //custom-fit
        r_sq = Fit.getRSquared(); //r-sqr
        System.out.println("2D GaussFit R-sq: " + r_sq);
        System.out.println("2D GaussFit Status int: " + Fit.getStatus());
        rsq = r_sq;
        if (r_sq < 0.9) {
            success = false;
//            return success; //uncomment once iterative fitting has been implemented
        }
        double[] params = Fit.getParams();
        amplitude = params[0]; //A, amplitude of the Gaussian function
        a_ = params[1]; //a, 
        x0 = params[2]; //x0, x-position in patch 
        b_ = params[3]; //b
        y0 = params[4]; //y0, y-position in patch
        //sigma_x = sqrt(1/(b*2), sigma_y = sqrt(1/(d*2)
        //"FWHMx" = sqrt(1/(2*b))*2.3548, "FWHMy" = sqrt(1/(2*d))*2.3548
        FWHMx = Math.sqrt(1 / (2 * a_)) * 2.3548;
        FWHMy = Math.sqrt(1 / (2 * b_)) * 2.3548;
//        success = true;
        return success;
    }

    private static double[] getStats(double[] o) {
//Array.getStatistics(o, min, max, mean, stdDev);
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double sum = 0d;
        double stdDev = 0d; //forgot how to calculate programatically - DO LATER AFTER CHECKING NOTES    
        for (int i = 0; i < o.length; i++) {
            double value = o[i];
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
            sum = sum + value;
        }
        double mean = sum / o.length;

        double[] results = new double[4];
        results[0] = min;
        results[1] = max;
        results[2] = mean;
        results[3] = stdDev;

        return results;
    }
}
