/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hstar.javavr.aparapi;

import static com.hstar.javavr.aparapi.JavaVRAparapiV2.addDir;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.opencv.calib3d.Calib3d.*;
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

        try {
            addDir("C:\\Users\\Saswat\\Dropbox\\Documents\\Java\\libraries\\opencv");
            addDir("C:\\Users\\Saswat\\Dropbox\\Documents\\Java\\libraries\\Aparapi");
        } catch (IOException ex) {
            Logger.getLogger(JavaVRAparapiV2.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.loadLibrary("opencv_java246");

        final VideoCapture capture = new VideoCapture(0);
        Mat web_img = new Mat();

        final AtomicBoolean ab = new AtomicBoolean(false);

        BID.setOnClick(new Runnable() {
            @Override
            public void run() {
                ab.set(true);
            }
        }, 5);

        while (capture.read(web_img)) {
            MatOfPoint2f corners = new MatOfPoint2f();
            if (findChessboardCorners(web_img, new Size(7, 5), corners)) {
                drawChessboardCorners(web_img, new Size(7, 5), corners, true);
                System.out.println("calibrating");
                Mat cameraMatrix = Mat.eye(3, 3, CV_64F);
                Mat distCoeffs = Mat.zeros(8, 1, CV_64F);

                calibrate(web_img, cameraMatrix, distCoeffs);
                ab.set(false);
            }
            BID.display(web_img, 5);

            if (ab.get()) {
                
            }
        }
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

        calibrateCamera(objCorners, imgCorners, src.size(), cameraMatrix, distCoeffs, null, null);
    }

    static void calcBoardCornerPositions(Size boardSize, float squareSize, List<Mat> corners) {

        for (int i = 0; i < boardSize.height; ++i) {
            for (int j = 0; j < boardSize.width; ++j) {
                corners.add(new MatOfPoint3f(new Point3((j * squareSize), (i * squareSize), 0)));
            }
        }
    }
}
