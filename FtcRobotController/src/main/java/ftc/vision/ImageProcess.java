package ftc.vision;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ImageProcess implements Runnable {
	
	
	private static final int hue = 22; // the target hue
	private static final int radius = 8; // the variation in hue accepted
	private static final int minS = 150; // minimum saturation
	private static int minArea = 0;

	public static Mat stageHue = new Mat();
	
	private static CountDownLatch latch = new CountDownLatch(1); // use this to wait for a new frame
	public static Mat sourceImg; // image we want to process

	/**
	 * Last processed contour
	 */
	private static MatOfPoint lastProcessed;
	/**
	 * Whether the frame has been processed and can be read now
	 */
	private static AtomicBoolean frameProcessed = new AtomicBoolean();
	
	/**
	 * Last non-null contour
	 */
	private static MatOfPoint lastContour;
	/**
	 * Whether a new contour has been found and can be read now
	 */
	private static AtomicBoolean contourFound = new AtomicBoolean();
	
	private static ImageProcess p;
	
	private boolean running = false;
	
	public ImageProcess() {
		
	}

	/**
	 * Finds the cheddar in a given image, and returns the {@link MatOfPoint} that surrounds it
	 * 
	 * @param img The input image
	 * @return The contour around the cheddar
	 */
	public MatOfPoint processImage(Mat img) {
		long t = System.nanoTime();		
				
		minArea = img.cols() * img.rows() / 800; // what is the area of a contour that is 1/20th width and 1/20th
												// height of screen? Use that value as the minimum area
		
		Mat hsv = new Mat(img.rows(), img.cols(), CvType.CV_8UC3, new Scalar(0,0,0)); // a new matrix that will hold the image in HSV
		Imgproc.cvtColor(img, hsv, Imgproc.COLOR_RGB2HSV); // convert to HSV
		
		Mat v = new Mat(img.rows(), img.cols(), CvType.CV_8UC1, new Scalar(0)); // this is just the value part of the HSV image, will be used later
		Core.extractChannel(hsv, v, 2); // extract it (2 is the index of the value channel)
		
		Mat _binary = new Mat(img.rows(), img.cols(), CvType.CV_8UC1, new Scalar(0)); // the completely black and white image for the image after being filtered by hue and saturation
		// It's called a binary because every pixel is either completely black (0,0,0) or completely white (255, 255, 255)

        Core.inRange(hsv, new Scalar(hue-radius, minS, 0), new Scalar(hue+radius, 255, 255), _binary);
		// the Scalar go in the order H, S, V. The max is always 255

        stageHue = _binary.clone();

		Mat _binaryBlur = new Mat(img.rows(), img.cols(), CvType.CV_8UC1, new Scalar(0)); // we want to blur the binary to make the really small white parts go away
		Imgproc.blur(_binary, _binaryBlur, new Size(11,11)); // blur it
		Core.inRange(_binaryBlur, new Scalar(200, 0, 0), new Scalar(255, 0, 0), _binary); // only take parts that are still bright
		/*
		 * This part above is for performance reasons. When we blur the binary image, it basically spreads out the pixels.
		 * As a result, any parts of the image that are white, but very small, will become darker.
		 * However, parts of the image that are white but are bigger will only blur on the edge, while remaining
		 * bright in the center. Then we only take the very bright parts, effectively cutting out the small bits
		 * that don't matter.
		 */

		List<MatOfPoint> contours = new ArrayList<>(); // original list of contours
		ArrayList<MatContour> cs = new ArrayList<>(); // list of {@link MatContour} objects that will be created
		Imgproc.findContours(_binary, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		// Above function finds edges of the white blobs and puts them into a list

		for (MatOfPoint cont : contours) {
			cs.add(new MatContour(cont)); // create the MatContour objects
		}

		Collections.sort(cs); // sort them by area

		double minDensity = 0.4; // density is the area of the contour over the area of the convex hull (look up convex hull)
		int nCheck = 10; // number of contours the check
		nCheck = Math.min(cs.size(), nCheck); // if we have less than 10 contours, just check all of them

		double bestV = 0; // bestV is best value (in this case, I'm just choosing the largest value of HSV)
		MatContour bestMat = null; // the contour that has the best value
		for (int i = 0; i < nCheck; i++) {

			MatContour cont = cs.get(i);
			cont.findHull(); // generate the conxev hull

			if (cont.hullArea > minArea && cont.area / cont.hullArea > minDensity) { // if the hull area is more than min and the ratio is good
				Mat contourImg = Mat.zeros(_binary.size(), _binary.type()); // make an empty image of the same size and shape as the binary image

				// draw the contour on the image in pure white. This is basically making a binary image,
				// where 1s are inside the contour and 0s are outside the contour
				Imgproc.drawContours(contourImg, Arrays.asList(cont.mat), 0, new Scalar(255, 255, 255), -1);

				double meanV = Core.mean(v, contourImg).val[0];
				// find the mean value of the value image only inside the contour
				if (meanV > bestV) { // if this one is higher,
					bestV = meanV; // set the best value to the new value
					bestMat = cont; // save the contour for later
				}
			}
		}

//		System.out.println((System.nanoTime() - t) / 1000000.0);

		if (bestMat == null) {
//			System.out.println("box not found");
			return null;
		} else {
			return bestMat.mat; // we're done!
		}
	}

	@Override
	public void run() {
		running = true;
		while (true) {
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (!running) {
				break;
			}
			
			latch = new CountDownLatch(1);
//			System.out.println("Processing image");
			
			lastProcessed = processImage(sourceImg);
			if (lastProcessed != null) {
				lastContour = lastProcessed;
			}
			frameProcessed.set(true);
		}
	}
	
	public static void start() {
		if (p == null || !p.running) {
			p = new ImageProcess();
			p.running = true;
			Thread t = new Thread(p);
			t.start();
		}
	}
	
	public static void stop() {
		sourceImg = null;
		if (p != null) {
			p.running = false;
		}
		latch.countDown();
		frameProcessed.set(false);
	}
	
	public static void setSourceImage(Mat srcImg) {
		sourceImg = srcImg;
		latch.countDown();
	}
	
	/**
	 * Returns whether there is a new frame processed (could be null)
	 * @return True if there is a frame
	 */
	public static boolean canGrabFrame() {
		return frameProcessed.get();
	}
	
	/**
	 * Grab the most recent frame (could be null) and set canGrabFrame to false
	 * @return The most recent frame
	 */
	public static MatOfPoint grabFrame() {
		frameProcessed.set(false);
		return lastProcessed;
	}
	
	/**
	 * Returns whether there is a new non-null frame processed
	 * @return True if there is a non-null frame
	 */
	public static boolean canGrabContour() {
		return contourFound.get();
	}
	
	/**
	 * Grab the most recent non-null frame and set canGrabContour to false
	 * @return The most recent non-null frame
	 */
	public static MatOfPoint grabContour() {
		contourFound.set(false);
		return lastContour;
	}

}
