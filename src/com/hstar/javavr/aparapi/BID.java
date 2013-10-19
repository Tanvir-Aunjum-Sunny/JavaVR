/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hstar.javavr.aparapi;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import javax.swing.JFrame;
import org.opencv.core.Mat;

/**
 * Buffered Image Display :P
 * @author Saswat
 */
public class BID {

    private static HashMap<Integer, BID> displays = new HashMap<>();
    private JFrame frame;
    private JavaVRAparapiV2.ImgView view = new JavaVRAparapiV2.ImgView();

    private BID(int id, Dimension size) {
        frame = new JFrame();
        view.setBackground(Color.black);
        frame.add(view);
        
        frame.setTitle("Display: "+id);
        frame.setVisible(true);
        frame.setSize(size);
        switch(id) {
            case 1:
                break;
            case 2:
                frame.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width - frame.getSize().width, 0);
                break;
            case 3:
                frame.setLocation(0,Toolkit.getDefaultToolkit().getScreenSize().height - frame.getSize().height);
                break;
            case 4:
                frame.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width - frame.getSize().width,
                        Toolkit.getDefaultToolkit().getScreenSize().height - frame.getSize().height);
                break;
            default:
                frame.setLocationRelativeTo(null);
        }
        
    }
    
    /**
     * Create/updates a JFrame display
     *
     * @param mat the image to display, converts to BufferedImage and won't display of there is a problem
     * @param id the id of the frame
     */
    public static void display(Mat mat, int id) {
        try {
            BufferedImage bi = JavaVRAparapiV2.getImage(mat);
            display(bi, id);
        } catch (Exception e) {
            
        }
    }

    /**
     * Create/updates a JFrame display
     *
     * @param bi the image to display
     * @param id the id of the frame
     */
    public static void display(BufferedImage bi, int id) {
        BID dis;
        if(!displays.containsKey(id)) {
            displays.put(id, dis = new BID(id, new Dimension(bi.getWidth(), bi.getHeight())));
        } else {
            dis = displays.get(id);
        }
        dis.view.setImage(bi);
        dis.view.repaint();
    }
}
