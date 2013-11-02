/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hstar.javavr.aparapi;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.opencv.core.Mat;

/**
 * Buffered Image Display :P
 *
 * @author Saswat
 */
public class BID {

    private static HashMap<Integer, BID> displays = new HashMap<>();
    private JFrame frame;
    private ImgView view = new ImgView();
    private int id;

    private BID(int id) {
        this.id = id;
        frame = new JFrame();
        view.setBackground(Color.black);
        frame.add(view);

        frame.setTitle("Display: " + id);
        //frame.setVisible(true);
        //frame.setSize(size);

    }

    private void calcLocation() {
        switch (id) {
            case 1:
                break;
            case 2:
                frame.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width - frame.getSize().width, 0);
                break;
            case 3:
                frame.setLocation(0, Toolkit.getDefaultToolkit().getScreenSize().height - frame.getSize().height);
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
     * @param mat the image to display, converts to BufferedImage and won't
     * display of there is a problem
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
        if (bi == null) {
            return;
        }
        BID dis = getBID(id);
        if (!dis.frame.isVisible()) {
            dis.frame.setVisible(true);
            dis.frame.setSize(bi.getWidth(), bi.getHeight());
            if (dis.frame.getLocation().equals(new Point(0, 0))) {
                dis.calcLocation();
            }
            System.out.println(id);
        }
        dis.view.setImage(bi);
        dis.view.repaint();
    }

    public static void setOnClick(Runnable r, int id) {
        getBID(id).view.run = r;

    }

    public static JFrame getFrame(int id) {
        return getBID(id).frame;
    }

    private static BID getBID(int id) {
        BID dis;
        if (!displays.containsKey(id)) {
            displays.put(id, dis = new BID(id));
        } else {
            dis = displays.get(id);
        }
        return dis;
    }

    static class ImgView extends JPanel {

        BufferedImage image;
        Runnable run = null;

        public ImgView() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (run != null) {
                        run.run();
                    }
                }
            });
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
