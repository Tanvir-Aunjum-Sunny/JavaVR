/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hstar.javavr.aparapi;

import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opencv.core.Mat;
import org.opencv.highgui.VideoCapture;
import static org.opencv.core.Core.*;
import static org.opencv.core.CvType.*;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import static org.opencv.imgproc.Imgproc.*;
import static org.opencv.calib3d.Calib3d.*;
import static com.hstar.javavr.aparapi.tools.Utilities.*;
/**
 *
 * @author Saswat
 */
public class JavaVRAparapiV2 {

    

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        printMem();

        librarySetup();
        
        final VideoCapture capture = new VideoCapture(0);

        final AtomicBoolean wait = new AtomicBoolean(false);

        BID.getFrame(0).addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                capture.release();
                System.exit(0);
                super.windowClosing(e);
            }
        });
        BID.setOnClick(new Runnable() {
            @Override
            public void run() {
                wait.set(!wait.get());
            }
        }, 0);

        Mat web_img = new Mat();

        if (capture.isOpened()) {
            capture.read(web_img);
            BID.display(web_img, 0);
            BID.getFrame(0).setSize(640, 480);
            BID.getFrame(0).setLocationRelativeTo(null);
            BID.getFrame(0).setLocation(BID.getFrame(0).getLocation().x, Toolkit.getDefaultToolkit().getScreenSize().height - BID.getFrame(0).getSize().height);

            int fps = 0;
            long time = System.currentTimeMillis();
            Mat test = web_img.clone();
            while (capture.read(web_img)) {

                flip(web_img, web_img, 1);
                Mat ori = web_img.clone();
                BID.display(ori, 3);
                Mat gray = new Mat();

                //<editor-fold defaultstate="collapsed" desc="Edge detect">
                //edge detect (Sobel)
                //erode(web_img, gray, getStructuringElement(MORPH_RECT, new Size(5, 5)));
                GaussianBlur(web_img, test, new Size(0, 0), 3);
                addWeighted(web_img, 1.5, test, -0.5, 0, test);

                cvtColor(test, test, COLOR_RGB2GRAY);//convert to grayscale

                Mat gradx = new Mat();
                Mat grady = new Mat();

                Sobel(test, gradx, CV_16S, 1, 0, 3, 1, 0, BORDER_DEFAULT);//dx
                convertScaleAbs(gradx, gradx);

                Sobel(test, grady, CV_16S, 0, 1, 3, 1, 0, BORDER_DEFAULT);//dy
                convertScaleAbs(grady, grady);

                addWeighted(gradx, 0.5, grady, 0.5, 0, test);// add

                //dilate(gray, gray, getStructuringElement(MORPH_RECT, new Size(2, 2)));
                ///GaussianBlur(test, test, new Size(5, 5), 5, 5);
                //test = test.clone();
                threshold(test, gray, 70, 255, THRESH_BINARY);// *///threshold

                //gray = test.clone();
                //--------------------------------------------------------------------------------
                //edge detect (Canny)
                //dilate(web_img, test, new Mat(), new Point(-1, -1), 3);
                //GaussianBlur(web_img, gray, new Size(0, 0), 3);
                //addWeighted(web_img, 1.5, gray, -0.5, 0, gray);
                //Canny(web_img, test, 200, 200);
                ///Canny(web_img, gray, 200, 200);
                //test = gray.clone();
                //</editor-fold>
                //<editor-fold defaultstate="collapsed" desc="Blob detection">
                List<MatOfPoint> contours = new ArrayList<>();
                findContours(gray.clone(), contours, new Mat(), RETR_LIST, CHAIN_APPROX_SIMPLE);

                drawContours(web_img, contours, -1, new Scalar(0, 255, 0), -1);

                ArrayList<Point[]> possibleRect = new ArrayList();//all the possible convex quads
                ArrayList<Point[]> possibleHull = new ArrayList();//the respective hulls for these quads
                ArrayList<Double> possibleArea = new ArrayList();//the respective areas for these quads

                MatOfPoint2f approx = new MatOfPoint2f();
                for (MatOfPoint mop : contours) {
                    MatOfPoint2f mop2f = new MatOfPoint2f(mop.toArray());
                    approxPolyDP(mop2f, approx, arcLength(mop2f, true) * 0.02, true);

                    Point points[] = approx.toArray();

                    double area;

                    if (points.length == 4 /*&& contourArea(mop) > 300*/ && distance(points[0], points[2]) > 20 && distance(points[1], points[3]) > 20) {
                        Point hull[];
                        if (//distance(points[0], points[1]) > 2 * distance(points[2], points[1])
                                //|| distance(points[2], points[1]) > 2 * distance(points[0], points[1]) ||
                                (hull = giftWrapping(points)).length != 4) {
                            continue;
                        }

                        //throw out tiny rectangles
                        if ((area = areaQuad(hull[0], hull[1], hull[2], hull[3])) < 500) {//TODO make resolution independant
                            //System.out.println("area: "+area);
                            continue;
                        }

                        List<MatOfPoint> temp = new ArrayList();
                        temp.add(new MatOfPoint(hull));

                        possibleRect.add(points);
                        possibleHull.add(hull);
                        possibleArea.add(area);
                        drawContours(web_img, temp, 0, new Scalar(255, 0, 255), 2);
                    }

                }
                //</editor-fold>

                ArrayList<Point[]> insideSquares = new ArrayList<>();
                ArrayList<Point[]> outsideSquares = new ArrayList<>();

                //<editor-fold defaultstate="collapsed" desc="Awesome Rectangle isolater and angle computer">
                //*
                for (int i = 0; i < possibleRect.size(); i++) {
                    for (int j = 0; j < possibleRect.size(); j++) {//i = outside, j = inside
                        String print = "";

                        Mat test_t = test.clone();
                        Mat gray_t = gray.clone();

                        // check if same rect and if one rectangle is inside the other
                        if (j == i || !isQuadInQuad(possibleHull.get(i), possibleHull.get(j))) {
                            continue;
                        }

                        //Check if the squares are too large or small relative to each other
                        double ratio = possibleArea.get(i) / possibleArea.get(j);
                        if (ratio > 2 || ratio < 1.1) {//usually around 1.6
                            continue;
                        }

                        //check for duplicate squares
                        boolean hit = false;
                        for (int k = 0; k < outsideSquares.size(); k++) {
                            Point outpts[] = outsideSquares.get(k);
                            Point newpts[] = possibleHull.get(i);

                            Point ncenter = midpoint(newpts[0], newpts[2]);

                            Point ocenter = midpoint(outpts[0], outpts[2]);
                            double radius1 = distance(ocenter, outpts[1]);
                            double radius2 = distance(ocenter, outpts[2]);

                            double dis = distance(ocenter, ncenter);
                                //System.out.println("nc: "+ncenter.toString()+" | oc: "+ocenter.toString());
                            //System.out.println("dis: "+dis+"\t rad1: "+radius1+" | rad2: "+radius2);

                            if (dis < radius1 || dis < radius2) {
                                hit = true;
                            }
                        }
                        if (hit) {
                            continue;
                        }
                        /*print += (Math.abs(distance(possibleHull.get(i)[0], possibleHull.get(i)[1])/distance(possibleHull.get(j)[0], possibleHull.get(j)[1])));
                         print += "\n";
                         print += ("\td1:" + distance(possibleHull.get(i)[0], possibleHull.get(i)[1]) + "\n\td2:" + distance(possibleHull.get(j)[0], possibleHull.get(j)[1]));
                         print += "\n";*/
                        //check if the ratios between the small and big sides aren't extreme
                        /*if (Math.abs(distance(possibleHull.get(i)[0], possibleHull.get(i)[1]) / distance(possibleHull.get(j)[0], possibleHull.get(j)[1])) < .9 ||//TODO find a better solution to false A than extreme thresholds
                         Math.abs(distance(possibleHull.get(i)[1], possibleHull.get(i)[2]) / distance(possibleHull.get(j)[1], possibleHull.get(j)[2])) < .9
                         || Math.abs(distance(possibleHull.get(i)[2], possibleHull.get(i)[3]) / distance(possibleHull.get(j)[2], possibleHull.get(j)[3])) < .9
                         || Math.abs(distance(possibleHull.get(i)[3], possibleHull.get(i)[0]) / distance(possibleHull.get(j)[3], possibleHull.get(j)[0])) < .9) {
                            
                         continue;
                         }*/
                        Mat sqrp = new Mat(4, 1, CV_32FC2);
                        Mat sqrf = new Mat(4, 1, CV_32FC2);
                        for (int k = 1; k <= 4; k++) {
                            sqrp.put(k, 1, possibleRect.get(i)[k - 1].x, possibleRect.get(i)[k - 1].y);

                        }

                        sqrf.put(1, 1, 0, 0);
                        sqrf.put(2, 1, 500, 0);
                        sqrf.put(3, 1, 500, 500);
                        sqrf.put(4, 1, 0, 500);

                        Mat trans = getPerspectiveTransform(new MatOfPoint2f(possibleHull.get(i)), new MatOfPoint2f(new Point(0, 0), new Point(0, 500), new Point(500, 500), new Point(500, 0)));

                        //computing rect scale :)
                        Point mp1 = midpoint(possibleHull.get(i)[0], possibleHull.get(i)[1]);
                        Point mp2 = midpoint(possibleHull.get(i)[1], possibleHull.get(i)[2]);
                        Point mp3 = midpoint(possibleHull.get(i)[2], possibleHull.get(i)[3]);
                        Point mp4 = midpoint(possibleHull.get(i)[3], possibleHull.get(i)[0]);

                        double scale1 = distance(mp3, mp1) / 500;//TODO change 500 to a constant
                        double scale2 = distance(mp2, mp4) / 500;//TODO change 500 to a constant

                        print += ("s1: " + scale1 + ", s2: " + scale2);
                        print += "\n";

                        print += ("trans: " + trans.dump());
                        print += "\n";
                        //Calib3d.

                        //decomposeProjectionMatrix(trans, new Mat(), new Mat(), new Mat());
                        //System.out.println(Arrays.toString(RQDecomp3x3(trans, new Mat(), new Mat())));
                        Mat homography = findHomography(new MatOfPoint2f(possibleHull.get(i)), new MatOfPoint2f(new Point(0, 0), new Point(0, 500), new Point(500, 500), new Point(500, 0)));

                        print += (homography.dump());
                        print += "\n";

                        warpPerspective(ori, test_t, trans, new Size(500, 500));
                        gray_t = test_t.clone();
                        cvtColor(test_t, test_t, COLOR_RGB2GRAY);

                        int whiteref = (int) mean(test_t.submat(80, 90, 80, 90)).val[0];
                        int blackref = (int) mean(test_t.submat(20, 30, 20, 30)).val[0];

                        //System.out.println("w:" + whiteref + ", b:" + blackref);
                        if (whiteref - blackref < 30) {
                            continue;
                        }

                        int avg = (whiteref + blackref) / 2;

                        threshold(test_t, test_t, avg, 255, THRESH_BINARY);

                        int angle = 0;

                        Point pointi[] = possibleHull.get(i);
                        Point pointj[] = possibleHull.get(j);

                        //check rotations
                        //<editor-fold defaultstate="collapsed" desc="Angles">
                        /*check if triangle is upright
                         * 245, 150
                         * 255, 160
                         * 10x10
                         * 
                         * check if triangle is pointed left
                         * 155, 245
                         * 165, 255
                         * 10x10
                         * 
                         * check if triangle is pointed right
                         * 335, 250
                         * 345, 260
                         * 10x10
                         * 
                         * check if triangle is pointed down
                         * 245, 335
                         * 255, 345
                         * 10x10
                         * 
                         * white point(for threshold)
                         * 80, 80
                         * 90, 90
                         * 10x10
                         * 
                         * black point
                         * 20, 20
                         * 30, 30
                         * 10x10
                         */
                        //</editor-fold>
                        //<editor-fold defaultstate="collapsed" desc="Image Rotator">
                        //rotates image// and reorders points
                        if (mean(test_t.submat(150, 160, 245, 255)).val[0] > avg) {//upright, continue if area is bright
                            if (mean(test_t.submat(245, 255, 140, 150)).val[0] < avg) {//left, rotate right if area is dark
                                angle = 270;
                                /*Point pt = pointi[0];
                                 pointi[0] = pointi[3];
                                 pointi[3] = pointi[2];
                                 pointi[2] = pointi[1];
                                 pointi[1] = pt;

                                 pt = pointj[0];
                                 pointj[0] = pointj[1];
                                 pointj[1] = pointj[2];
                                 pointj[2] = pointj[3];
                                 pointj[3] = pt;
                                 System.out.println("rotating right");*/
                            } else if (mean(test_t.submat(245, 255, 335, 345)).val[0] < avg) {//right, rotate left if area is dark
                                angle = 90;
                                /*Point pt = pointi[0];
                                 pointi[0] = pointi[1];
                                 pointi[1] = pointi[2];
                                 pointi[2] = pointi[3];
                                 pointi[3] = pt;

                                 pt = pointj[0];
                                 pointj[0] = pointj[1];
                                 pointj[1] = pointj[2];
                                 pointj[2] = pointj[3];
                                 pointj[3] = pt;
                                 System.out.println("rotating left");*/
                            } else if (mean(test_t.submat(335, 345, 245, 255)).val[0] < avg) {//down, flip up if area is dark
                                angle = 180;
                                /*Point pt = pointi[0];
                                 pointi[0] = pointi[2];
                                 pointi[2] = pt;
                                 pt = pointi[1];
                                 pointi[1] = pointi[3];
                                 pointi[3] = pt;

                                 pt = pointj[0];
                                 pointj[0] = pointj[2];
                                 pointj[2] = pt;
                                 pt = pointj[1];
                                 pointj[1] = pointj[3];
                                 pointj[3] = pt;
                                 System.out.println("fliping up");*/
                            }
                        }
                        print += (angle);
                        print += "\n";
                        if (angle != 0) {
                            warpAffine(test_t, test_t, getRotationMatrix2D(new Point(250, 250), (360 - angle) * -1, 1), new Size(500, 500));
                        }
                        //</editor-fold>

                        List<MatOfPoint> temp = new ArrayList();
                        temp.add(new MatOfPoint(pointi));
                        List<MatOfPoint> temp2 = new ArrayList();
                        temp2.add(new MatOfPoint(pointj));
                        insideSquares.add(pointj);
                        outsideSquares.add(pointi);

                        drawContours(web_img, temp, -1, new Scalar(255, 0, 0), 3);
                        drawContours(web_img, temp2, -1, new Scalar(0, 0, 255), 3);

                        test = test_t;
                        gray = gray_t;
                        //done = true;
                        System.out.println(print);
                        //break;
                    }
                    //if (done) {
                    //    break;
                    //}
                }
                // */
                //</editor-fold>

                if (outsideSquares.size() > 0) {
                    System.out.println("squares: " + outsideSquares.size());
                }

                /*for(Point[] square : outsideSquares) {
                    double x = 
                    
                }*/
                
                BID.display(web_img, 0);
                BID.display(gray, 1);
                BID.display(test, 2);

                //<editor-fold defaultstate="collapsed" desc="fps counter">
                if (System.currentTimeMillis() - time > 1000) {
                    time = System.currentTimeMillis();
                    System.out.println("FPS: " + fps);
                    fps = 0;
                }
                fps++;
                //</editor-fold>
                while (wait.get()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(JavaVRAparapiV2.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

}
