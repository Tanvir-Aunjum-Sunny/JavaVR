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
import org.lwjgl.util.glu.GLU;

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

            GameEngine ge = new GameEngine();
            ge.initGL(1024, 786);

            while (!Display.isCloseRequested() && !closeRequested) {
                //newDim = newCanvasSize.getAndSet(null);

                //if (newDim != null) {
                //    GL11.glViewport(0, 0, newDim.width, newDim.height);
                //}
                //GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                ge.run();
                Display.update();
            }

            Display.destroy();
            frame.dispose();
            System.exit(0);
        } catch (LWJGLException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        render();
    }

    private boolean render() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);          // Clear The Screen And The Depth Buffer

        GL11.glLoadIdentity();                          // Reset The Current Modelview Matrix
        //GL11.glTranslatef(1.5f, 0.0f, -7.0f);             // Move Right 1.5 Units And Into The Screen 6.0
        GL11.glColor3f(0.5f, 0.5f, 1.0f);                 // Set The Color To Blue One Time Only
        GL11.glBegin(GL11.GL_QUADS);                        // Draw A Quad
        GL11.glColor3f(0.0f, 1.0f, 0.0f);             // Set The Color To Green
        GL11.glVertex3f(1.0f, 1.0f, -1.0f);         // Top Right Of The Quad (Top)
        GL11.glVertex3f(-1.0f, 1.0f, -1.0f);         // Top Left Of The Quad (Top)
        GL11.glVertex3f(-1.0f, 1.0f, 1.0f);         // Bottom Left Of The Quad (Top)
        GL11.glVertex3f(1.0f, 1.0f, 1.0f);         // Bottom Right Of The Quad (Top)
        GL11.glColor3f(1.0f, 0.5f, 0.0f);             // Set The Color To Orange
        GL11.glVertex3f(1.0f, -1.0f, 1.0f);         // Top Right Of The Quad (Bottom)
        GL11.glVertex3f(-1.0f, -1.0f, 1.0f);         // Top Left Of The Quad (Bottom)
        GL11.glVertex3f(-1.0f, -1.0f, -1.0f);         // Bottom Left Of The Quad (Bottom)
        GL11.glVertex3f(1.0f, -1.0f, -1.0f);         // Bottom Right Of The Quad (Bottom)
        GL11.glColor3f(1.0f, 0.0f, 0.0f);             // Set The Color To Red
        GL11.glVertex3f(1.0f, 1.0f, 1.0f);         // Top Right Of The Quad (Front)
        GL11.glVertex3f(-1.0f, 1.0f, 1.0f);         // Top Left Of The Quad (Front)
        GL11.glVertex3f(-1.0f, -1.0f, 1.0f);         // Bottom Left Of The Quad (Front)
        GL11.glVertex3f(1.0f, -1.0f, 1.0f);         // Bottom Right Of The Quad (Front)
        GL11.glColor3f(1.0f, 1.0f, 0.0f);             // Set The Color To Yellow
        GL11.glVertex3f(1.0f, -1.0f, -1.0f);         // Bottom Left Of The Quad (Back)
        GL11.glVertex3f(-1.0f, -1.0f, -1.0f);         // Bottom Right Of The Quad (Back)
        GL11.glVertex3f(-1.0f, 1.0f, -1.0f);         // Top Right Of The Quad (Back)
        GL11.glVertex3f(1.0f, 1.0f, -1.0f);         // Top Left Of The Quad (Back)
        GL11.glColor3f(0.0f, 0.0f, 1.0f);             // Set The Color To Blue
        GL11.glVertex3f(-1.0f, 1.0f, 1.0f);         // Top Right Of The Quad (Left)
        GL11.glVertex3f(-1.0f, 1.0f, -1.0f);         // Top Left Of The Quad (Left)
        GL11.glVertex3f(-1.0f, -1.0f, -1.0f);         // Bottom Left Of The Quad (Left)
        GL11.glVertex3f(-1.0f, -1.0f, 1.0f);         // Bottom Right Of The Quad (Left)
        GL11.glColor3f(1.0f, 0.0f, 1.0f);             // Set The Color To Violet
        GL11.glVertex3f(1.0f, 1.0f, -1.0f);         // Top Right Of The Quad (Right)
        GL11.glVertex3f(1.0f, 1.0f, 1.0f);         // Top Left Of The Quad (Right)
        GL11.glVertex3f(1.0f, -1.0f, 1.0f);         // Bottom Left Of The Quad (Right)
        GL11.glVertex3f(1.0f, -1.0f, -1.0f);         // Bottom Right Of The Quad (Right)
        GL11.glEnd();                                       // Done Drawing The Quad

        return true;
    }

    private void initGL(int windowWidth, int windowHeight) {
        GL11.glEnable(GL11.GL_TEXTURE_2D); // Enable Texture Mapping
        GL11.glShadeModel(GL11.GL_SMOOTH); // Enable Smooth Shading
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // Black Background
        GL11.glClearDepth(1.0); // Depth Buffer Setup
        GL11.glEnable(GL11.GL_DEPTH_TEST); // Enables Depth Testing
        GL11.glDepthFunc(GL11.GL_LEQUAL); // The Type Of Depth Testing To Do

        GL11.glMatrixMode(GL11.GL_PROJECTION); // Select The Projection Matrix
        GL11.glLoadIdentity(); // Reset The Projection Matrix

        // Calculate The Aspect Ratio Of The Window
        GLU.gluPerspective(
                45.0f,
                (float) windowWidth / (float) windowHeight,
                0.1f,
                100.0f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); // Select The Modelview Matrix

        // Really Nice Perspective Calculations
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);
    }

}
