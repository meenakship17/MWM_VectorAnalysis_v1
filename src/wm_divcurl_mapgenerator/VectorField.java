/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wm_divcurl_mapgenerator;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.ZProjector;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

/**
 *
 * @author Meenakshi
 */
public class VectorField {

    private ImagePlus xImp = null;
    private ImagePlus yImp = null;
    private ImagePlus resImp = null;
    private boolean isStack = true;
    private ImagePlus sum_xImp = null;
    private ImagePlus sd_xImp = null;
    private ImagePlus sum_yImp = null;
    private ImagePlus sd_yImp = null;
    private Roi sampledPixelsROI = null;
    private ImagePlus SF_xImp = null;
    private ImagePlus SF_yImp = null;
    private ImagePlus xFracErr = null;
    private ImagePlus yFracErr = null;
    private ImagePlus fieldProperty = null;//divergence or curl - not sure if this is the way we want to execute this
    
    
    

    public VectorField(ImagePlus xImg, ImagePlus yImg, ImagePlus resImg) {
        xImp = xImg;
        yImp = yImg;
        resImp = resImg;
        isStack = resImp.isStack();
    }

    private void setMeanAndSDImages() {

        this.rtCorrAndByteConversion(xImp, resImp);
        this.rtCorrAndByteConversion(yImp, resImp);

        sum_xImp = ZProjector.run(xImp, "sum");
        sum_yImp = ZProjector.run(yImp, "sum");

        if (isStack) {
            sd_xImp = ZProjector.run(xImp, "sd");
            sd_yImp = ZProjector.run(yImp, "sd");
        }
//            this.saveTiff(sum_xImp, "C:\\Users\\Meenakshi\\Desktop\\results2", "01.sum_xImg.tiff");
//            this.saveTiff(sd_xImp, "C:\\Users\\Meenakshi\\Desktop\\results2", "02.sd_xImg.tiff");
//            this.saveTiff(sum_yImp, "C:\\Users\\Meenakshi\\Desktop\\results2", "01.sum_yImg.tiff");
//            this.saveTiff(sd_yImp, "C:\\Users\\Meenakshi\\Desktop\\results2", "02.sd_yImg.tiff");

    }

    private void setSampledPixelsROI() {
        //make ROI of sampled points
        resImp = ZProjector.run(resImp, "sum");
        resImp.getProcessor().convertToByteProcessor();
        resImp.getProcessor().setThreshold(1, 255, 3);       //255 is max for 8-bit image 
        ThresholdToSelection tts = new ThresholdToSelection();
        sampledPixelsROI = tts.convert(resImp.getProcessor());
    }

    private void setSurfaceFitImages(int polyOrder) {
        //xImg
        sum_xImp.setRoi(sampledPixelsROI);
        SF_xImp = this.surfaceFit(sum_xImp, polyOrder);
//            this.saveTiff(SF_xImp, "C:\\Users\\Meenakshi\\Desktop\\results2", "03.SF_xImg.tiff");
        //yImg
        sum_yImp.setRoi(sampledPixelsROI);
        SF_yImp = this.surfaceFit(sum_yImp, polyOrder);
//            this.saveTiff(SF_yImp, "C:\\Users\\Meenakshi\\Desktop\\results2", "03.SF_yImg.tiff");

        if (isStack) {
            //for err image generation
            xFracErr = this.getFracErrImage(sum_xImp, sd_xImp, SF_xImp.duplicate());
            yFracErr = this.getFracErrImage(sum_yImp, sd_yImp, SF_yImp.duplicate());
        }
    }

    private void rtCorrAndByteConversion(ImagePlus xImp, ImagePlus resImp) {
        //Divide by restime to account for number of times a pixel has been sampled
        ImageCalculator ic = new ImageCalculator();
        ic.run("divide stack", xImp, resImp);
//            this.saveTiff(sum_xImp, "C:\\Users\\Meenakshi\\Desktop\\results2", "ImgAfterDivision.tiff");

        //convert result stack to 8 bit - to remove zeros and scale the values
        ImageConverter imgconv = new ImageConverter(xImp);
        imgconv.convertToGray8();
//            this.saveTiff(sum_xImp, "C:\\Users\\Meenakshi\\Desktop\\results2", "ImgAfterByteProcessorConversion.tiff");
    }

    private ImagePlus surfaceFit(ImagePlus image, int num) {
        //polynomial fit function -
        Polynomial_Surface_Fit psf = new Polynomial_Surface_Fit(image);
//        image.show();
        image = psf.run(image.getProcessor(), num);
//        image.show();
        return image;
    }

    private ImagePlus getResidualSumSquareImage(ImagePlus sum_xImp, ImagePlus SF_xImp) {
        //resize surface fit image
       // SF_xImp = this.resizeImage(SF_xImp, sampledPixelsROI, true);
//        this.saveTiff(SF_xImp, "C:\\Users\\Meenakshi\\Desktop\\results2", "04.1ExpectedImage_SF_xImg_resized.tiff");

        //apply roi and clear outside - aka set black - check value of background
        SF_xImp.getProcessor().setValue(0d);
        SF_xImp.getProcessor().fillOutside(sampledPixelsROI);
//          SF_xImp.show();
//          this.saveTiff(SF_xImp, "C:\\Users\\Meenakshi\\Desktop\\results2", "04.1ExpectedImage_SF_xImg_resized_Bgdstate.tiff");

        //deviations i.e. observed subtract expected
        ImageCalculator ic = new ImageCalculator();
        ImagePlus RSS = ic.run("subtract create", sum_xImp, SF_xImp);
//        this.saveTiff(RSS, "C:\\Users\\Meenakshi\\Desktop\\results2", "04.2.deviations.tiff");
        //variance i.e. devation-sq image
        RSS.getProcessor().sqr();
//        this.saveTiff(RSS, "C:\\Users\\Meenakshi\\Desktop\\results2", "04.3.RSS.tiff");
        return RSS;
    }

    private void getReciprocal(ImageProcessor ip) {
        float[] pixels = (float[]) ip.getPixels();
        for (int i = 0; i < ip.getWidth() * ip.getHeight(); i++) {
            if (pixels[i] == 0f) {
                pixels[i] = Float.NaN;
            } else {
                pixels[i] = 1f / pixels[i];
            }
        }
        ip.resetMinAndMax();
    }

    private ImagePlus getWeight(ImagePlus sd_xImp) {
        //sd-sq
        sd_xImp.getProcessor().sqr();
//            this.saveTiff(weight, "C:\\Users\\Meenakshi\\Desktop\\results2", "05.sd_sq.tiff");
//            weight.show();

        //Inversely weight error to value
        this.getReciprocal(sd_xImp.getProcessor());
//            this.saveTiff(weight, "C:\\Users\\Meenakshi\\Desktop\\results2", "06.reciprocal_sd_sq.tiff");

        //get integrated density value to make the weights fractional
        Analyzer.setMeasurements(Measurements.INTEGRATED_DENSITY);
        RoiManager roiMan = new RoiManager(false);
        roiMan.addRoi(sampledPixelsROI);
        ResultsTable resultsTable = roiMan.multiMeasure(sd_xImp);
//            resultsTable.show("");
        double sum = resultsTable.getValueAsDouble(0, 0);
//        System.out.println("Weights - Sum: " + sum);
        //Math divide - SD-sq reciprocal image w int density value
        sd_xImp.getProcessor().multiply(1 / sum);
        //check int den = 1
        resultsTable = roiMan.multiMeasure(sd_xImp);
//            resultsTable.show("");
        sum = resultsTable.getValueAsDouble(0, 0);
        System.out.println("Check weights - Normalised sum (1): " + sum);
        return sd_xImp;
    }

    private ImagePlus getFracErr(ImagePlus RSS, ImagePlus weight, ImagePlus sum_xImp) {
        //Image calculator - residual multiply by weight, i.e. RSS image in 7.2
        ImageCalculator ic = new ImageCalculator();
        ImagePlus resultImp = ic.run("multiply create", RSS, weight);
        //step 9.3
        //observed-sq
        sum_xImp.getProcessor().sqr();
        //image calculator divide by observed-sq image
        ic.run("divide", resultImp, sum_xImp);
        //math sq root
        resultImp.getProcessor().sqrt();
        return resultImp;
    }

    private ImagePlus getFracErrImage(ImagePlus sum_xImp, ImagePlus sd_xImp, ImagePlus SF_xImp) {
        //xImg
        ImagePlus RSS = this.getResidualSumSquareImage(sum_xImp, SF_xImp);
        ImagePlus weight = this.getWeight(sd_xImp);
        ImagePlus xFracErr = this.getFracErr(RSS, weight, sum_xImp);
//            this.saveTiff(RSS, "C:\\Users\\Meenakshi\\Desktop\\results2", "04.RSS_xImg.tiff");
//            this.saveTiff(weight, "C:\\Users\\Meenakshi\\Desktop\\results2", "05.weight_xImg.tiff");            
//            this.saveTiff(resultImp, "C:\\Users\\Meenakshi\\Desktop\\results2", "06.fracErr_xImg.tiff");
        return xFracErr;
    }
}
