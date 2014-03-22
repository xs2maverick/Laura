package examples;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import org.jfree.ui.RefineryUtilities;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

public class Preprocess extends Snapshot{
	
	private static BufferedImage img;
	private static String extension = ".jpg";
	private static String filename = "test_053";
	private static final int numberofcandidates = 3;
	private static final String readfolder = "images/";
	private static final String savefolder = "images/results/";
	private static Vector<Mat> bands = new Vector<Mat>();
	private static boolean displayOn = false;
	
//	public static void main(String[] args) throws IOException{
//		int count = 5;
//		String filename = "test_00" + count;
//		File image = new File(readfolder + filename + ".jpg");
//		while(new File(readfolder + filename + ".jpg").exists()){
//			System.out.println("******** " + filename + "*******");
//			run(filename);
//			count++;
//			if(count >= 10){
//				
//				filename = "test_0" + count;
//			}else{
//				filename = "test_00" + count;
//			}
//		}
//	}
	
	
	public static void main(String args[]) throws IOException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		MatToBufferedImage conv = new MatToBufferedImage();
		
		Mat source = Highgui.imread(readfolder + filename + extension);
		double ratios = source.width()/source.height();
		
		Imgproc.resize(source, source, new Size(source.width(), source.height()));
		//Imgproc.resize(source, source, new Size(source.width()*0.5, source.height()*0.5));
		Mat processed = new Mat(source.width(), source.height(), CvType.CV_64FC2);
		Coordinates[] ycoords = new Coordinates[2];//0 is ypeak, 1 is yband
		Coordinates[] xcoords = new Coordinates[2];//0 is xpeak, 1 is xband
		//System.out.println("width: " + m.width() + "height: " + m.height());	//debugging purposes

		//Preprocess image
		Process(source, processed, 150, "vertical");
		img = conv.getImage(processed);
		System.out.println("original image");
		displayImage(source, new Coordinates(0, source.width()), new Coordinates(0, source.height()), "Original Image", filename);
		

		
		//Vertical Projection histogram & Image
		System.out.println("================   VERTICAL PROJECTION  ============");
		System.out.println("0");
		ycoords = displayHistogram(img, "Vertical Projection Histogram", "vertical", 0.55);
		displayImage(source, new Coordinates(0, source.width()), ycoords[1], "vertical projection 0th", filename);
		Mat band = source.submat(ycoords[1].getX(), ycoords[1].getY(), 0, source.width());
		if(bands.add(band))	System.out.println("band added");
		System.out.print("\n\n");
		
		
		//produce more possible plates
		if(numberofcandidates > 1){
			for(int i = 1; i < numberofcandidates; i++){
				System.out.println(i);
				Coordinates[] coords = displayHistogram(img, "Vertical Projection Histogram " + i + "th", "vertical", 0.55, i);
				band = source.submat(coords[1].getX(), coords[1].getY(), 0, source.width());
				if(bands.add(band)){
					System.out.println("band added");
				}
				displayImage(source, new Coordinates(0, source.width()), coords[1], "vertical projection " + i + "th", filename);
				System.out.print("\n\n");
			}

		}
		
	
		//Horizontal Projection
		Mat horizontal;
		
		Iterator it = bands.iterator();
		
		if(it.hasNext()){
			int count = 1;
			System.out.println("================ HORIZONTAL PROJECTION  ===============");
			while(it.hasNext()){
				horizontal = (Mat) it.next();
				Process(horizontal, processed, 150, "horizontal");
				img = conv.getImage(processed);
				xcoords = displayHistogram(img, "Horizontal Projection " + count + "th" + " Histogram", "horizontal", 0.1);
				displayImage(horizontal, xcoords[1], new Coordinates(0,horizontal.height()), "horizontal projection " + count + "th", filename);
				Mat possibleplate = horizontal.submat(0, horizontal.height(), xcoords[1].getX(), xcoords[1].getY());
				System.out.print(getMatDetail(possibleplate));
				System.out.println("The cropped image is " + verifySizes(possibleplate) + " possible plate" + "\n\n");
				count++;
			}
		}else{
			horizontal = source.submat(ycoords[1].getX(), ycoords[1].getY(), 0, source.width());
			Process(horizontal, processed, 150, "horizontal");
			img = conv.getImage(processed);
			xcoords = displayHistogram(img, "Horizontal Projection Histogram", "horizontal", 0.1);
			displayImage(horizontal, xcoords[1], new Coordinates(0,horizontal.height()), "horizontal projection", filename);
			Mat possibleplate = horizontal.submat(0, horizontal.height(), xcoords[1].getX(), xcoords[1].getY());
			System.out.print(getMatDetail(possibleplate));
			System.out.println("The cropped image is " + verifySizes(possibleplate) + " possible plate" + "\n\n");
		}
		
		bands.clear();
	}
	
	private static Mat Process(Mat src, Mat dst, double Threshold, String sobeltype) throws IOException{
		Mat bw = new Mat(src.width(), src.height(), CvType.CV_64FC2);
		Imgproc.cvtColor(src, bw, Imgproc.COLOR_RGB2GRAY);
		Mat morphelem = new Mat(src.width(), src.height(), CvType.CV_64FC2);
		Mat threshold = new Mat(src.width(), src.height(), CvType.CV_8UC1);
		if(sobeltype == "vertical"){
			Imgproc.Sobel(bw, threshold, src.depth(), 1, 0);
		}else if(sobeltype == "horizontal"){
			Imgproc.Sobel(bw, threshold, src.depth(), 0, 2);
		}
		Imgproc.GaussianBlur(threshold, threshold, new Size(5,5), 0);
		Imgproc.threshold(threshold, threshold, Threshold, 255, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY);
		morphelem = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,5));
		Imgproc.morphologyEx(threshold, dst, 3, morphelem);
		displayImage(dst, new Coordinates(0, dst.width()), new Coordinates(0, dst.height()));
		
		return dst;
	}
	
	private static Coordinates[] displayHistogram(BufferedImage img, String title, String orientation, double bandthreshold){
		Coordinates[] coords = new Coordinates[2];	//coords[0] being peak coords, coords[1] being band coords
		Histogram hist = new Histogram(title, img, orientation);
		int[] arr = hist.getImgArr();
		Coordinates peak_coord = hist.getPeakCoord(arr);
		Coordinates band_coord;
		if(orientation == "vertical"){
			band_coord = hist.getBandCoords(arr, bandthreshold, "vertical");
		}else{
			band_coord = hist.getBandCoords(arr, bandthreshold, "horizontal");
		}
		if(peak_coord == null || band_coord == null){
			System.err.println("Error. unable to find peak coordinates or band coordinates");
		}
		System.out.println("peak coordinates are: " + peak_coord.toString());
		System.out.println("band coordinates are: " + band_coord.toString());
		coords[0] = peak_coord;
		coords[1] = band_coord;
		
		hist.pack();
		RefineryUtilities.centerFrameOnScreen(hist);
		if(displayOn)	hist.setVisible(true);
		
		return coords;
	}
	
	private static Coordinates[] displayHistogram(BufferedImage img, String title, String orientation, double bandthreshold, int newPeak){
		Coordinates[] coords = new Coordinates[2];//coords[0] being peak coords, coords[1] being band coords
		Histogram hist = new Histogram(title, img, orientation);
		int[] arr = hist.getImgArr();
		for(int i = 0; i < newPeak; i++){
			Coordinates peak = hist.getPeakCoord(arr);
			Coordinates band;
			if(orientation == "vertical"){
				band = hist.getBandCoords(arr, bandthreshold, "vertical");
			}else{
				band = hist.getBandCoords(arr, bandthreshold, "horizontal");
			}
			System.out.println("band to be zeroized: " + band.toString());
			arr = clearPeak(arr, band);
			System.out.println("band Zeroized");
		}
		Coordinates peak_coord = hist.getPeakCoord(arr);
		Coordinates band_coord;
		if(orientation == "vertical"){
			band_coord = hist.getBandCoords(arr, bandthreshold, "vertical");
		}else{
			band_coord = hist.getBandCoords(arr, bandthreshold, "horizontal");
		}
		if(peak_coord == null || band_coord == null){
			System.err.println("Error. unable to find peak coordinates or band coordinates");
		}
		System.out.println("peak coordinates are: " + peak_coord.toString());
		System.out.println("band coordinates are: " + band_coord.toString());
		coords[0] = peak_coord;
		coords[1] = band_coord;
		
		hist.pack();
		RefineryUtilities.centerFrameOnScreen(hist);
		if(displayOn)	hist.setVisible(true);
		
		return coords;
	}
	
	private static int[] clearPeak(int[] imgarr, Coordinates band_coord){
		for(int i = band_coord.getX(); i<band_coord.getY(); i++){
			imgarr[i] = 0;
		}
		
		return imgarr;
		
	}
	
	
	private static void displayImage(Mat mat, Coordinates axisX, Coordinates axisY) throws IOException{
		MatToBufferedImage conv = new MatToBufferedImage();
		BufferedImage image = conv.getImage(mat);
		BufferedImage result = getCropImage(image, axisX.getX(), axisX.getY(), axisY.getX(), axisY.getY());
		//System.out.println("image width: " + result.getWidth() + ", image height: " + result.getHeight());
		
		SetImage(result);
		//System.out.println(SaveImage(filename + "result"));
		if(displayOn)		ViewImage("Possible Plate");
	}
	
	private static void displayImage(Mat mat, Coordinates axisX, Coordinates axisY, String windowcaption, String filename) throws IOException{
		MatToBufferedImage conv = new MatToBufferedImage();
		BufferedImage image = conv.getImage(mat);
		BufferedImage result = getCropImage(image, axisX.getX(), axisX.getY(), axisY.getX(), axisY.getY());
		//System.out.println(windowcaption + "image width: " + result.getWidth() + ", image height: " + result.getHeight());
		
		SetImage(result);
		//creates a new folder for this test image
		File folder = new File(savefolder + "/" + filename);
		if(!folder.exists() && !folder.mkdir()){	//if the folder already exist and mkdir failed
			System.err.println("Error creating Directory " + folder.toString());
		}
		System.out.println("saving image..." + SaveImage(result, filename + windowcaption + "result", savefolder + "/" + filename + "/"));
		if(displayOn)		ViewImage(windowcaption);
	}
	
	private static boolean verifySizes(Mat possibleplate){
		//TODO:	hardcoded value. Please change for flexibility
		double error = 0.4;	
		double ratio = 130/20;	//6.5
		//current min size, 130 x 20
		
		//min and max aspect ratio with the given error relief
		double rmin = ratio - (ratio*error);
		double rmax = ratio + (ratio*error);
		
		//set a min and max area
		//TODO: 15 and 70 are hardcoded, baseless value. Please consider refactoring
		int min = 15*15*(int) ratio;//minimum area, current val is 
		int max = 70*70*(int) ratio;//maximum area
		
		int area = possibleplate.width()*possibleplate.height();
		double rplate = (double) possibleplate.width()/(double) possibleplate.height();
		if(rplate < 1){
			rplate = 1/rplate;
		}
		if((area < min || area > max) || (rplate < rmin || rplate > rmax)){
			return false;
		}else{
			return true;
		}
	}


}
