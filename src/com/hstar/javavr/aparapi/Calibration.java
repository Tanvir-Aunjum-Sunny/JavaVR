/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hstar.javavr.aparapi;

import com.hstar.javavr.aparapi.tools.Utilities;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.opencv.calib3d.Calib3d.*;
import static org.opencv.imgproc.Imgproc.*;
import static org.opencv.core.CvType.*;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;

/**
 * Use with checker board
 *
 * @author Saswat
 */
public class Calibration {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        Utilities.librarySetup();

        final VideoCapture capture = new VideoCapture(0);
        Mat web_img = new Mat();

        final AtomicBoolean ab = new AtomicBoolean(false);

        BID.setOnClick(new Runnable() {
            @Override
            public void run() {
                ab.set(true);
            }
        }, 5);
        
        Mat cameraMatrix = getCameraMat(), distCoeffs = getDistortionMat();
        
        while (capture.read(web_img)) {
            MatOfPoint2f corners = new MatOfPoint2f();
            /*if (findChessboardCorners(web_img, new Size(7, 5), corners)) {
             System.out.println("calibrating");
             Mat cameraMatrix = Mat.eye(3, 3, CV_32FC1);
             Mat distCoeffs = Mat.zeros(4, 1, CV_32FC1);

             calibrate(web_img, cameraMatrix, distCoeffs);
             ab.set(false);
                
             drawChessboardCorners(web_img, new Size(7, 5), corners, true);
             }*/

            Mat undis = new Mat();

            undistort(web_img, undis, cameraMatrix, distCoeffs);
            BID.display(web_img, 5);
            BID.display(undis, 1);

            if (ab.get()) {
            }
        }
    }

    public static Mat getCameraMat() {
        Mat cameraMatrix = Mat.eye(3, 3, CV_32FC1);
        
        cameraMatrix.put(0, 0, 1.09183262e+004, 0, 2.80353943e+002);
        cameraMatrix.put(1, 0, 0, 7.22333057e+003, 2.46577347e+002);
        cameraMatrix.put(2, 0, 0, 0, 1);
        
        return cameraMatrix;
    }
    
    public static Mat getDistortionMat() {
        Mat distCoeffs = Mat.zeros(4, 1, CV_32FC1);
        
        distCoeffs.put(0, 0, 2.02777100e+001, -1.81827109e+004, 4.64891642e-001, -4.35319960e-001);
        
        return distCoeffs;
    }

    public static void calibrate(Mat src, Mat cameraMatrix, Mat distCoeffs) {
        MatOfPoint2f corners = new MatOfPoint2f();
        if (!findChessboardCorners(src, new Size(7, 5), corners)) {
            throw new NullPointerException("No Chessboard");
        }
        List<Mat> objCorners = new ArrayList<>();
        calcBoardCornerPositions(new Size(7, 5), 5, objCorners);

        List<Mat> imgCorners = new ArrayList<>();
        for (Point p : corners.toList()) {
            imgCorners.add(new MatOfPoint2f(p));
        }

        ArrayList<Mat> rvecs = new ArrayList<>();
        ArrayList<Mat> tvecs = new ArrayList<>();

        System.out.println(objCorners.size());
        System.out.println(imgCorners.size());

        calibrateCamera(objCorners, imgCorners, src.size(), cameraMatrix, distCoeffs, rvecs, tvecs);
    }

    static void calcBoardCornerPositions(Size boardSize, float squareSize, List<Mat> corners) {

        for (int i = 0; i < boardSize.height; ++i) {
            for (int j = 0; j < boardSize.width; ++j) {
                corners.add(new MatOfPoint3f(new Point3((j * squareSize), (i * squareSize), 0)));
            }
        }
    }
}
