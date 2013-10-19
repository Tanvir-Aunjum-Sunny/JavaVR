/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hstar.javavr.aparapi;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import static org.opencv.core.Core.*;
import org.opencv.core.CvType;
import static org.opencv.core.CvType.*;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import static org.opencv.imgproc.Imgproc.*;

/**
 *
 * @author Saswat
 */
public class JavaVRAparapi {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        printMem();

        //*
        try {
            addDir("C:\\Users\\Saswat\\Dropbox\\Documents\\Java\\libraries\\opencv");
            addDir("C:\\Users\\Saswat\\Dropbox\\Documents\\Java\\libraries\\Aparapi");
        } catch (IOException ex) {
            Logger.getLogger(JavaVRAparapi.class.getName()).log(Level.SEVERE, null, ex);
        }// */

        System.loadLibrary("opencv_java246");

        /*Random r = new Random(System.currentTimeMillis());

         int n = Integer.MAX_VALUE / 1000;

         final float inA[] = new float[n];
         final float inB[] = new float[n];
         final float result[] = new float[inA.length];

         for (int i = 0; i < n; i++) {
         inA[i] = r.nextFloat();
         inB[i] = r.nextFloat();
         }
        
        
         Kernel kernel = new Kernel() {
         public void run() {
         int i = getGlobalId();
         result[i] = pow(inA[i], inB[i]);
         }
         };
         Range range = Range.create(result.length);

         long t = System.currentTimeMillis();
         kernel.execute(range);

         System.out.println(System.currentTimeMillis() - t);
         //System.out.println(Arrays.toString(result));*/


        final VideoCapture capture = new VideoCapture(0);

        JFrame frame = new JFrame();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                capture.release();
                System.exit(0);
                super.windowClosing(e);
            }
        });

        ImgView view = new ImgView();
        view.setBackground(Color.black);
        frame.add(view);

        JFrame frame2 = new JFrame();
        ImgView view2 = new ImgView();
        view2.setBackground(Color.black);
        frame2.add(view2);


        JFrame frame3 = new JFrame();
        ImgView view3 = new ImgView();
        view3.setBackground(Color.black);
        frame3.add(view3);

        Mat web_img = new Mat();

        if (capture.isOpened()) {
            frame.setVisible(true);
            capture.read(web_img);
            frame.setSize(web_img.width(), web_img.height());
            frame.setLocationRelativeTo(null);

            frame2.setVisible(true);
            frame2.setSize(web_img.width(), web_img.height());

            frame3.setVisible(true);
            frame3.setSize(web_img.width(), web_img.height());

            int fps = 0;
            long time = System.currentTimeMillis();
            while (capture.read(web_img)) {

                //medianBlur(web_img, web_img, 5);

                flip(web_img, web_img, 1);

                Mat gray = new Mat();
                Mat test = new Mat();

                //edge detect (Sobel)
                erode(web_img, gray, getStructuringElement(MORPH_RECT, new Size(5, 5)));

                cvtColor(gray, gray, COLOR_RGB2GRAY);//convert to grayscale

                Mat gradx = new Mat();
                Mat grady = new Mat();

                Sobel(gray, gradx, CV_16S, 1, 0, 3, 1, 0, BORDER_DEFAULT);//dx
                convertScaleAbs(gradx, gradx);

                Sobel(gray, grady, CV_16S, 0, 1, 3, 1, 0, BORDER_DEFAULT);//dy
                convertScaleAbs(grady, grady);

                addWeighted(gradx, 0.5, grady, 0.5, 0, gray);// add

                erode(gray, gray, getStructuringElement(MORPH_RECT, new Size(1, 1)));

                threshold(gray, gray, 20, 255, THRESH_BINARY);// *///threshold

                //edge detect (Canny)
                //dilate(web_img, gray, new Mat());
                //Canny(gray, gray, 100, 100);


                Mat lines = new Mat();

                //HoughLinesP(gray, lines, 1, Math.PI / 90, 100, 20, 50);//Sobel
                //HoughLinesP(gray, lines, 1, Math.PI/360, 100, 10, 10);//Canny

                //drawLines(web_img, lines);

                /*System.out.println("h Lines: " + lines.cols());

                 threshold(web_img, gray, 120, 255, THRESH_BINARY_INV);

                 cvtColor(gray, test, COLOR_RGB2GRAY);//convert to grayscale
                 threshold(test, test, 200, 255, THRESH_BINARY);

                 bitwise_not(test, test);*/

                //Blob detection
                List<MatOfPoint> contours = new ArrayList<>();
                findContours(gray, contours, new Mat(), RETR_LIST, CHAIN_APPROX_SIMPLE);

                //drawContours(web_img, contours, -1, new Scalar(0, 255, 0), -1);
                int biggest = 0;
                int biggest_area = 0;
                for (int i = 0; i < contours.size(); i++) {
                    double area;
                    if ((area = contourArea(contours.get(i))) > 2) {
                        if (biggest_area < area) {
                            biggest = i;
                            //drawContours(web_img, contours, biggest, new Scalar(0, 255, 0), 3);
                        } else {
                            //drawContours(web_img, contours, i, new Scalar(0, 255, 0), 3);
                        }
                    }
                }
                //drawContours(web_img, contours, biggest, new Scalar(0, 255, 255), 3);

                //System.out.println(contours.size());

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
                                || (hull = giftWrapping(points)).length < 0) {
                            continue;
                        }
                        //System.out.println("gw=" + giftWrapping(points).length);
                        List<MatOfPoint> temp = new ArrayList();
                        temp.add(mop);

                        Point center = new Point((points[0].x + points[1].x + points[2].x + points[3].x) / 4,
                                (points[0].y + points[1].y + points[2].y + points[3].y) / 4);

                        /*int i1 = -1, i2 = -1, i3 = -1, i4 = -1;
                        
                         for(int i = 0; i < 4; i++) {
                         if(points[i].x > center.x && points[i].y > center.y) {
                         i1 = i;
                         }
                         if(points[i].x > center.x && points[i].y < center.y) {
                         i2 = i;
                         }
                         if(points[i].x < center.x && points[i].y < center.y) {
                         i3 = i;
                         }
                         if(points[i].x < center.x && points[i].y > center.y) {
                         i4 = i;
                         }
                         }*/

                        //possibleRect.add(new Point[]{points[i1], points[i2], points[i3], points[i4]});
                        possibleRect.add(points);
                        possibleHull.add(hull);
                        possibleArea.add(area);
                        //drawContours(web_img, temp, 0, new Scalar(0, 0, 255), 1);
                    }

                    //System.out.println(approx.size());
                }
                System.out.println(possibleRect.size());

                for (int i = 0; i < possibleRect.size(); i++) {
                    Point pts[] = possibleRect.get(i);
                    
                    for (int j = 0; j < possibleRect.size(); j++) {
                        if (j == i) {
                            continue;
                        }
                        if(possibleArea.get(i) - possibleArea.get(j) < 100) {//area between rectangles
                            continue;
                        }
                        if (!isQuadInQuad(pts, possibleRect.get(j))) {
                            continue;
                        }
                        System.out.println("special!");
                        List<MatOfPoint> temp = new ArrayList();
                        temp.add(new MatOfPoint(pts));
                        temp.add(new MatOfPoint(possibleRect.get(j)));
                        drawContours(web_img, temp, -1, new Scalar(0, 0, 255), 3);

                    }
                    //System.out.println(distance(pts[0], pts[1]) + "," + distance(pts[1], pts[2]) + "," + distance(pts[2], pts[3])+ "," + distance(pts[3], pts[1]));
                    System.out.println(Arrays.toString(pts));
                }

                // */

                /*for(MatOfPoint contour: contours) {
                 if(contourArea(contour) < 2) {
                 continue;
                 }
                 Mat mask = Mat.zeros(contour.rows(), contour.cols(), CvType.CV_8UC1);
                 floodFill(web_img, mask, new Point(3, 3), new Scalar(0,0,255));
                 }*/
                //HoughLinesP(test, lines, 1, Math.PI/180, 100, 20, 50);
                //drawLines(web_img, lines);

                /*/Corner detection
                 cornerHarris(gray, lines, 21, 5, .04, BORDER_DEFAULT);
                 for (int j = 0; j < lines.rows(); j++) {
                 for (int i = 0; i < lines.cols(); i++) {
                 if (lines.get(j, i)[0] > 2) {
                 circle(web_img, new Point(i, j), 5, new Scalar(255, 255, 0), 2);
                 }
                 }
                 }
                 System.out.println(lines.rows()+", "+lines.cols()+"  "+lines.get(240, 360)[0]);//*/

                BufferedImage bf = getImage(web_img);
                BufferedImage bf2 = getImage(gray);
                //BufferedImage bf3 = getImage(test);

                view.setImage(bf);
                view.repaint();

                view2.setImage(bf2);
                view2.repaint();

                //view3.setImage(bf3);
                view3.repaint();
                //fps counter
                if (System.currentTimeMillis() - time > 1000) {
                    time = System.currentTimeMillis();
                    System.out.println("FPS: " + fps);
                    fps = 0;
                }
                fps++;
            }
        }

        printMem();
    }

    /*public static Point[] arrangePoints(Point pts[]) {
     Point center = new Point((pts[0].x + pts[1].x + pts[2].x+pts[3].x)/4,
     (pts[0].y + pts[1].y + pts[2].y+pts[3].y)/4);
     Point arr[] = new Point[4];
        
        
     return null;
     }*/
    //<editor-fold defaultstate="collapsed" desc="Flawed gift wrap">
    /*
     public static Point[] giftWrap(Point pts[]) {
        
     System.out.println(Arrays.toString(pts));
        
     ArrayList<Point> hull = new ArrayList<>();
        
     int minIndex = 0;
     Point origin = new Point(0, 0);
     double distance = distance(pts[minIndex], origin);
     for (int i = 1; i < pts.length; i++) {
     double d = distance(pts[i],origin);
     if(distance > d) {
     distance = d;
     minIndex = i;
     }
     }
        
     hull.add(pts[minIndex]);
        
     int prevIndex = minIndex;
     int index = minIndex;
     double curAngle = 0;//Math.PI;
     do {
     //HashMap<Integer, Double> map = new HashMap<>();//stores index of reach point and angle off x-axis(in radians)
     System.out.println(index);
     double minAngle = -Math.PI;
     int tindex = index;
            
     for(int i = 0; i < pts.length; i++) {
     if(i == index || i == prevIndex) continue;
     double angle = Math.atan2(pts[i].y-pts[index].y, pts[i].x-pts[index].x);//Range -PI -> PI
                
     //if(angle < 0) {//Now the range is from 0 -> 2PI
     //    angle = angle*-1 + Math.PI;
     //}
                
     //if(curAngle - angle < 0) {
     //    continue;
     //}
                
     System.out.println("    "+i+" = "+(curAngle - angle) + " vs " + (curAngle - minAngle));
     if(curAngle - angle < curAngle - minAngle) {
     tindex = i;
     minAngle = angle;
     }
     }
            
     prevIndex = index;
     curAngle = minAngle;
     System.out.println("  "+tindex+" chosen @ theta = "+minAngle);
     minAngle = 0;
     index = tindex;
     hull.add(pts[index]);
     } while(index != minIndex);
        
     return hull.toArray(new Point[]{});
     }
     */
    //</editor-fold>
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

        Point hull[] = new Point[pts.length];

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
            if (num >= hull.length || current >= pts.length) {
                return new Point[]{};
            }
            hull[num] = pts[current];
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

        return hull;
    }
    //</editor-fold>

    public static boolean isQuadInQuad(Point outsideHull[], Point insideHull[]) {
        boolean hits[] = new boolean[insideHull.length];

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

    public static boolean pntInTriangle(Point p, Point t1, Point t2, Point t3) {
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

    public static void drawLines(Mat canvas, Mat lines) {
        for (int x = 0; x < lines.cols(); x++) {
            double[] vec = lines.get(0, x);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);
            //System.out.println("Line: (x1["+x1+"],y1["+y1+"]) (x2["+x2+"],y2["+y2+"])");
            line(canvas, start, end, new Scalar(255, 0, 0), 2);
        }
    }

    public static BufferedImage getImage(Mat mat) {
        MatOfByte mb = new MatOfByte();
        Highgui.imencode(".jpg", mat, mb);
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(new ByteArrayInputStream(mb.toArray()));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return bufferedImage;
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

    /**
     * Computes the area of a quad from 4 points Divides the quad into 2
     * triangles, takes the area of each, and combines the areas A(triangle) =
     * .5*|x1(y2-y3)+x2(y3-y1)+x3(y1-y2)
     *
     * @param points the 4 points
     * @return the area
     */
    public static double quadArea(Point[] points) {
        System.out.println(Arrays.toString(points));

        Point p1 = points[0];
        Point p2 = points[1];
        Point p3 = points[2];
        Point p4 = points[3];

        double a1 = .5 * Math.abs(p1.x * (p2.y - p3.y) + p2.x * (p3.y - p1.y) + p3.x * (p1.y - p2.y));
        double a2 = .5 * Math.abs(p4.x * (p2.y - p3.y) + p2.x * (p3.y - p4.y) + p3.x * (p4.y - p2.y));

        return a1 + a2;
    }

    public static double distance(Point p1, Point p2) {
        return Math.sqrt(p1.x * p2.x + p1.y * p2.y);
    }

    /*void find_squares(Mat image, MatOfPoint2f squares) {
     // blur will enhance edge detection
     Mat blurred = new Mat();

     medianBlur(image, blurred, 9);

     Mat gray0 = new Mat();
     Mat gray = new Mat();

     MatOfPoint2f contours[];

     // try several threshold levels
     int threshold_level = 2;
     for (int l = 0; l < threshold_level; l++) {
     // Use Canny instead of zero threshold level!
     // Canny helps to catch squares with gradient shading
     if (l == 0) {
     Canny(gray0, gray, 100, 100); // 

     // Dilate helps to remove potential holes between edge segments
     dilate(gray, gray, new Mat(), new Point(-1, -1), 1);
     } else {
     gray = gray0 >= (l + 1) * 255 / threshold_level;
     }

     // Find contours and store them in a list
     findContours(gray, contours, RETR_LIST, CHAIN_APPROX_SIMPLE);

     // Test contours
     MatOfPoint2f approx;
     for (int i = 0; i < contours.length; i++) {
     // approximate contour with accuracy proportional
     // to the contour perimeter
     approxPolyDP(contours[i], approx, arcLength(contours[i], true) * 0.02, true);

     // Note: absolute value of an area is used because
     // area may be positive or negative - in accordance with the
     // contour orientation
     if (approx.size() == 4
     && fabs(contourArea(Mat(approx))) > 1000
     && isContourConvex(Mat(approx))) {
     double maxCosine = 0;

     for (int j = 2; j < 5; j++) {
     double cosine = fabs(angle(approx[j % 4], approx[j - 2], approx[j - 1]));
     maxCosine = MAX(maxCosine, cosine);
     }

     if (maxCosine < 0.3) {
     squares.push_back(approx);
     }
     }
     }
     }

     }*/
    static class ImgView extends JPanel {

        BufferedImage image;

        public ImgView() {
        }

        public void setImage(BufferedImage image) {
            this.image = image;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) {
                return;
            }
            g.drawImage(image, 0, 0, null);
        }
    }
}
