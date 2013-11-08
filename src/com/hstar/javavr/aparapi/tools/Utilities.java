/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.hstar.javavr.aparapi.tools;

import com.hstar.javavr.aparapi.JavaVRAparapiV2;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import static org.opencv.core.Core.norm;
import static org.opencv.core.Core.normalize;
import static org.opencv.core.CvType.CV_32FC1;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;

/**
 *
 * @author Saswat
 */
public class Utilities {
    
    public static void librarySetup() {
        Path libsDir;
        try {
            libsDir = Files.createTempDirectory("JVR libs");
        } catch (IOException ex) {
            Logger.getLogger(JavaVRAparapiV2.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        try {
            nativeCp(libsDir, "opencv_java246.dll");
            nativeCp(libsDir, "aparapi_x86_64.dll");
            nativeCp(libsDir, "lwjgl64.dll");
            
            addDir(libsDir.toString());
        } catch (IOException ex) {
            Logger.getLogger(JavaVRAparapiV2.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        System.loadLibrary("opencv_java246");
    }
    
    private static void nativeCp(Path libsDir, String libName) throws IOException {
        InputStream is = Utilities.class.getResource(libName).openStream();
        Files.copy(is, libsDir.resolve(libName));
    }
    
    //<editor-fold defaultstate="collapsed" desc="Adapted Gift Wrapper from http://yoshihitoyagi.com/projects/mesh/convexhull/giftwrapping/GiftWrapping.java">
    private static boolean small(Point current, Point smallest, Point i) {
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
                return xa * xa + ya * ya > xb * xb + yb * yb;
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

    //From http://stackoverflow.com/questions/8927771/computing-camera-pose-with-homography-matrix-based-on-4-coplanar-points?rq=1
    public static void cameraPoseFromHomography(Mat H, Mat pose) {
        pose.setTo(Mat.eye(3, 4, CV_32FC1));      // 3x4 matrix, the camera pose
        float norm1 = (float)norm(H.col(0));
        float norm2 = (float)norm(H.col(1));  
        float tnorm = (norm1 + norm2) / 2.0f; // Normalization value

        Mat p1 = H.col(0);       // Pointer to first column of H
        Mat p2 = pose.col(0);    // Pointer to first column of pose (empty)

        normalize(p1, p2);   // Normalize the rotation, and copies the column to pose

        p1 = H.col(1);           // Pointer to second column of H
        p2 = pose.col(1);        // Pointer to second column of pose (empty)

        normalize(p1, p2);   // Normalize the rotation and copies the column to pose

        p1 = pose.col(0);
        p2 = pose.col(1);

        Mat p3 = p1.cross(p2);   // Computes the cross-product of p1 and p2
        Mat c2 = pose.col(2);    // Pointer to third column of pose
        p3.copyTo(c2);       // Third column is the crossproduct of columns one and two
        
        Mat co2 = H.col(2);
        
        for(int i = 0; i < co2.rows(); i++) {
            co2.put(i, 0, co2.get(i, 0)[0]/tnorm);
        }
        
        pose.col(3).setTo(co2);  //vector t [R|t] is the last column of pose
    }
    
    public static Point midpoint(Point p1, Point p2) {
        return new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
    }

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
    public static boolean pntInQuad(Point p, Point t1, Point t2, Point t3, Point t4) {
        return pntInTriangle(p, t1, t2, t3) || pntInTriangle(p, t1, t4, t3);
    }

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

    private static void addDir(String s) throws IOException {
        try {
            // This enables the java.library.path to be modified at runtime
            // From a Sun engineer at http://forums.sun.com/thread.jspa?threadID=707176
            // 
            Field field = ClassLoader.class.getDeclaredField("usr_paths");
            field.setAccessible(true);
            String[] paths = (String[]) field.get(null);
            for (String path : paths) {
                if (s.equals(path)) {
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
        //return Math.sqrt(p1.x * p2.x + p1.y * p2.y);
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    public static double areaQuad(Point p1, Point p2, Point p3, Point p4) {
        return areaTriangle(p1, p2, p3) + areaTriangle(p1, p4, p3);
    }

    public static double areaTriangle(Point p1, Point p2, Point p3) {
        return .5 * Math.abs(p1.x * (p2.y - p3.y) + p2.x * (p3.y - p1.y) + p3.x * (p1.y - p2.y));
    }
}
