/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hstar.javavr.aparapi;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicReference;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/**
 *
 * @author Saswat
 */
public class GameEngine {

    private static boolean closeRequested = false;
    private final static AtomicReference<Dimension> newCanvasSize = new AtomicReference<>();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Frame frame = new Frame("Test");
        frame.setLayout(new BorderLayout());
        final Canvas canvas = new Canvas();

        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                newCanvasSize.set(canvas.getSize());
            }
        });

        frame.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                canvas.requestFocusInWindow();
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeRequested = true;
            }
        });

        frame.add(canvas, BorderLayout.CENTER);
        try {
            Display.setParent(canvas);
            Display.setVSyncEnabled(true);

            frame.setPreferredSize(new Dimension(1024, 786));
            frame.setMinimumSize(new Dimension(800, 600));
            frame.pack();
            frame.setVisible(true);
            Display.create();

            Dimension newDim;

            while (!Display.isCloseRequested() && !closeRequested) {
                newDim = newCanvasSize.getAndSet(null);

                if (newDim != null) {
                    GL11.glViewport(0, 0, newDim.width, newDim.height);
                }

                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                Display.update();
            }

            Display.destroy();
            frame.dispose();
            System.exit(0);
        } catch (LWJGLException e) {
            e.printStackTrace();
        }
    }
}
