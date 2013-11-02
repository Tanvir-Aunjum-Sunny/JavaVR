/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hstar.javavr.aparapi;

import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;
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

/**
 *
 * @author Saswat
 */
public class JavaVRAparapiV2 {

    private static Path libsDir;
    
    static {
        try {
            libsDir = Files.createTempDirectory("JVR libs");
        } catch (IOException ex) {
            libsDir = null;
            Logger.getLogger(JavaVRAparapiV2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        printMem();
        
        
        
        try {
            InputStream is = JavaVRAparapiV2.class.getResource("opencv_java246.dll").openStream();
            //addDir("C:\\Users\\Saswat\\Dropbox\\Documents\\Java\\libraries\\opencv");
            //addDir("C:\\Users\\Saswat\\Dropbox\\Documents\\Java\\libraries\\Aparapi");
            
            Files.copy(is, libsDir.resolve("opencv_java246.dll"));
            
            System.out.println(libsDir.toString());
            
            addDir(libsDir.toString());
            
        } catch (IOException ex) {
            Logger.getLogger(JavaVRAparapiV2.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.loadLibrary("opencv_java246");

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

                    if (points.length == 4 && (area = contourArea(mop)) > 300 && distance(points[0], points[2]) > 200 && distance(points[1], points[3]) > 200) {
                        Point hull[];
                        if (distance(points[0], points[1]) > 2 * distance(points[2], points[1])
                                || distance(points[2], points[1]) > 2 * distance(points[0], points[1])
                                || (hull = giftWrapping(points)).length != 4) {
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

                //<editor-fold defaultstate="collapsed" desc="Awesome Rectangle isolater and angle computer">
                //*
                ArrayList<Point[]> insideSquares = new ArrayList<>();
                ArrayList<Point[]> outsideSquares = new ArrayList<>();

                for (int i = 0; i < possibleRect.size(); i++) {

                    for (int j = 0; j < possibleRect.size(); j++) {
                        Mat test_t = test.clone();
                        Mat gray_t = gray.clone();

                        // check if same rect, the area between rectangles and if one is inside the other
                        if (j == i || possibleArea.get(i) - possibleArea.get(j) < 100 || !isQuadInQuad(possibleHull.get(i), possibleHull.get(j))) {
                            continue;
                        }


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

                        //Calib3d.

                        //decomposeProjectionMatrix(trans, new Mat(), new Mat(), new Mat());
                        System.out.println(Arrays.toString(RQDecomp3x3(trans, new Mat(), new Mat())));

                        warpPerspective(ori, test_t, trans, new Size(500, 500));
                        gray_t = test_t.clone();
                        cvtColor(test_t, test_t, COLOR_RGB2GRAY);

                        int whiteref = (int) mean(test_t.submat(80, 90, 80, 90)).val[0];
                        int blackref = (int) mean(test_t.submat(20, 30, 20, 30)).val[0];

                        System.out.println("w:" + whiteref + ", b:" + blackref);

                        if (whiteref - blackref < 30) {
                            continue;
                        }

                        int avg = (whiteref + blackref) / 2;

                        threshold(test_t, test_t, avg, 255, THRESH_BINARY);

                        int angle = 0;

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
                        if (mean(test_t.submat(150, 160, 245, 255)).val[0] > avg) {//upright, continue if area is bright
                            if (mean(test_t.submat(245, 255, 140, 150)).val[0] < avg) {//left, rotate right if area is dark
                                angle = 270;
                            } else if (mean(test_t.submat(245, 255, 335, 345)).val[0] < avg) {//right, rotate left if area is dark
                                angle = 90;
                            } else if (mean(test_t.submat(335, 345, 245, 255)).val[0] < avg) {//down, flip up if area is dark
                                angle = 180;
                            }
                        }
                        System.out.println(angle);

                        if (angle != 0) {
                            warpAffine(test_t, test_t, getRotationMatrix2D(new Point(250, 250), (360 - angle) * -1, 1), new Size(500, 500));
                        }

                        List<MatOfPoint> temp = new ArrayList();
                        temp.add(new MatOfPoint(possibleHull.get(i)));
                        temp.add(new MatOfPoint(possibleHull.get(j)));
                        insideSquares.add(possibleHull.get(j));
                        outsideSquares.add(possibleHull.get(i));
                        drawContours(web_img, temp, -1, new Scalar(0, 0, 255), 3);

                        test = test_t;
                        gray = gray_t;
                    }
                }
                // */
                //</editor-fold>

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

    //<editor-fold defaultstate="collapsed" desc="Adapted Gift Wrapper from http://yoshihitoyagi.com/projects/mesh/convexhull/giftwrapping/GiftWrapping.java">
    public static boolean small(Point current, Point smallest, Point i) {
        double xa, ya, xb, yb, val;
        xa = smallest.x - current.x;
        xb = i.x - current.x;
        ya = smallest.y - current.y;
        yb = i.y - current.y;

        val = xa * yb - xb * ya;
        if (val > 0) {
            return true;
        } else if (val < 0) {
            return false;
        } else {
            if (xa * xb + ya * yb < 0) {
                return false;
            } else {
                if (xa * xa + ya * ya > xb * xb + yb * yb) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    public static Point[] giftWrapping(Point pts[]) {

        ArrayList<Point> hull = new ArrayList<>();

        // convex hull
        int min = 0;
        for (int i = 1; i < pts.length; i++) {
            if (pts[i].y == pts[min].y) {
                if (pts[i].x < pts[min].x) {
                    min = i;
                }
            } else if (pts[i].y < pts[min].y) {
                min = i;
            }
        }
        //System.out.println("min: " + min);

        int num = 0;
        int smallest;
        int current = min;
        do {
            if (num >= pts.length || current >= pts.length) {
                return new Point[]{};
            }
            hull.add(pts[current]);
            num++;
            //System.out.println("num: " + num + ", current: " + current + "(" + xPoints[current] + ", " + yPoints[current] + ")");
            smallest = 0;
            if (smallest == current) {
                smallest = 1;
            }
            for (int i = 0; i < pts.length; i++) {
                if ((current == i) || (smallest == i)) {
                    continue;
                }
                if (small(pts[current], pts[smallest], pts[i])) {
                    smallest = i;
                }
            }
            current = smallest;
        } while (current != min);

        return hull.toArray(new Point[]{});
    }
    //</editor-fold>

    public static boolean isQuadInQuad(Point outsideHull[], Point insideHull[]) {
        boolean hits[] = new boolean[insideHull.length];
        assert outsideHull.length == 4;
        for (int i = 0; i < hits.length; i++) {
            hits[i] = false;
        }

        for (int i = 0; i < insideHull.length; i++) {
            if (pntInTriangle(insideHull[i], outsideHull[0], outsideHull[1], outsideHull[2])) {
                hits[i] = true;
            }
        }

        for (int i = 0; i < insideHull.length; i++) {
            if (pntInTriangle(insideHull[i], outsideHull[0], outsideHull[3], outsideHull[2])) {
                hits[i] = true;
            }
        }

        for (boolean hit : hits) {
            if (!hit) {
                return false;
            }
        }

        return true;
    }

    //<editor-fold defaultstate="collapsed" desc="Point in triangle checker from http://stackoverflow.com/questions/2464902/determine-if-a-point-is-inside-a-triangle-formed-by-3-points-with-given-latitude/2656515#2656515">
    public static boolean pntInTriangle(Point p, Point t1, Point t2, Point t3) {
        if (p == null || t1 == null || t2 == null || t3 == null) {
            return false;
        }
        return pntInTriangle(p.x, p.y, t1.x, t1.y, t2.x, t2.y, t3.x, t3.y);
    }

    public static boolean pntInTriangle(double px, double py, double x1, double y1, double x2, double y2, double x3, double y3) {

        int o1 = getOrientationResult(x1, y1, x2, y2, px, py);
        int o2 = getOrientationResult(x2, y2, x3, y3, px, py);
        int o3 = getOrientationResult(x3, y3, x1, y1, px, py);

        return o1 == o2 && o2 == o3;
    }

    private static int getOrientationResult(double x1, double y1, double x2, double y2, double px, double py) {
        double orientation = ((x2 - x1) * (py - y1)) - ((px - x1) * (y2 - y1));
        if (orientation > 0) {
            return 1;
        } else if (orientation < 0) {
            return -1;
        } else {
            return 0;
        }
    }
    //</editor-fold>

    public static BufferedImage getImage(Mat mat) throws IOException {
        MatOfByte mb = new MatOfByte();
        Highgui.imencode(".jpg", mat, mb);
        return ImageIO.read(new ByteArrayInputStream(mb.toArray()));
    }

    public static void addDir(String s) throws IOException {
        try {
            // This enables the java.library.path to be modified at runtime
            // From a Sun engineer at http://forums.sun.com/thread.jspa?threadID=707176
            // 
            Field field = ClassLoader.class.getDeclaredField("usr_paths");
            field.setAccessible(true);
            String[] paths = (String[]) field.get(null);
            for (int i = 0; i < paths.length; i++) {
                if (s.equals(paths[i])) {
                    return;
                }
            }
            String[] tmp = new String[paths.length + 1];
            System.arraycopy(paths, 0, tmp, 0, paths.length);
            tmp[paths.length] = s;
            field.set(null, tmp);
            System.setProperty("java.library.path", System.getProperty("java.library.path") + File.pathSeparator + s);
        } catch (IllegalAccessException e) {
            throw new IOException("Failed to get permissions to set library path");
        } catch (NoSuchFieldException e) {
            throw new IOException("Failed to get field handle to set library path");
        }
    }

    public static void printMem() {
        int mb = 1024 * 1024;

        //Getting the runtime reference from system
        Runtime runtime = Runtime.getRuntime();

        System.out.println("##### Heap utilization statistics [MB] #####");

        //Print used memory
        System.out.println("Used Memory:"
                + (runtime.totalMemory() - runtime.freeMemory()) / mb);

        //Print free memory
        System.out.println("Free Memory:"
                + runtime.freeMemory() / mb);

        //Print total available memory
        System.out.println("Total Memory:" + runtime.totalMemory() / mb);

        //Print Maximum available memory
        System.out.println("Max Memory:" + runtime.maxMemory() / mb);
    }

    public static double distance(Point p1, Point p2) {
        return Math.sqrt(p1.x * p2.x + p1.y * p2.y);
    }
}
