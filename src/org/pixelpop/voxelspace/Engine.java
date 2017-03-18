package org.pixelpop.voxelspace;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * The VoxelEngine is the entry point of program and is expected to bind the VoxelView to the VoxelSpace.
 * The class also creates the swing components on which the view will be displayed and which will receive input from the
 * user.
 *
 * bn k  cb YB   GVC  GG V N //Oskar :-)
 *
 * @author Joakim Lundin (joakim.lundin@dynabyte.se)
 */
public class Engine {
    //The resolution
    private final int width = 800;
    private final int height = 450;

    private final VoxelView voxelView;
    private final VoxelSpace voxelSpace;

    private final BufferedImage rendering;
    private final JFrame frame;

    private Engine() throws IOException {
        //Creates a full screen, no fuzz, JFrame
        frame = new JFrame();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setUndecorated(true);
        frame.setVisible(true);

        /* Creates an anonymous implements of JPanel which will be the canvas where everything will be painted. The
        paintComponent method is overloaded to let the view to painted through a buffered image which is scaled to the
        full screen display */
        int frameWidth = (int)frame.getSize().getWidth();
        JPanel canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(rendering.getScaledInstance(frameWidth, -1, Image.SCALE_FAST), 0, 0, null);
            }
        };


        canvas.addMouseMotionListener(new MouseMotionListener() {
            //Positions are used to keep track of the mouse movement between updates
            private int lastMousePositionY = -1;
            private int lastMousePositionX = -1;

            @Override
            public void mouseDragged(MouseEvent e) {
                //We're not dragging things...
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if(lastMousePositionX != -1) {
                    /* The view is rotated according to the X-axis movement of the mouse. The divisor 100 is an arbitrary
                    value used to smooth things out as positions are converted into radians (rotation) */
                    voxelView.rotate(((double)e.getX() - lastMousePositionX) / 100);
                }
                if(lastMousePositionY != -1) {
                    // The view is moved forward or backwards according to the Y-axis value of the mouse
                    voxelView.move(-1 * ((double)e.getY() - lastMousePositionY));
                }

                lastMousePositionX = e.getX();
                lastMousePositionY = e.getY();
            }
        });

        //Put everything together
        frame.add(canvas);
        frame.validate();
        frame.repaint();

        //This is the image that we will paint the voxelView onto
        rendering = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        //These are the key components of the engine!
        voxelSpace = new VoxelSpace(1024, 1024);
        voxelView = new VoxelView(voxelSpace, width, height);
    }

    /**
     * Updates the view and renders it onto the screen.
     */
    private void update() {
        //Update the VoxelView
        try {
            voxelView.update();
        } catch (InterruptedException e) {
            //TODO: Improve error handling
            e.printStackTrace();
        }

        //Render the updated VoxelView onto to the BufferedImage
        rendering.getRaster().setDataElements(0, 0, width, height, voxelView.getImage());
        frame.repaint();

        /* Provide some movement and rotation. This is done to benchmark the application without random user input.
        This should however be placed elsewhere... */
        //TODO: Put this in a separate method
        voxelView.move(1);
        voxelView.rotate(-.001);
    }

    /**
     * Invokes the application.
     * Instantiates an Engine which is run for a preset amount of frames. At completion the average FPS is presented
     * to standard output.
     *
     * @param args the arguments to the main method. These are ignored
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        //The number of frames that will be rendered before the program is exited
        int noFrames = 1000;
        //The time that we allow for each frame. 16 ms = 62.5 FPS
        long frameTime = 16;

        //We create the engine
        Engine engine = new Engine();

        //Start time to measure total FPS
        long origin = System.currentTimeMillis();

        //A time stamp is used to make sure that
        long timestamp = System.currentTimeMillis();
        long checkTime;

        for(int i = 0; i < noFrames; i++) {
            engine.update();

            if((checkTime = System.currentTimeMillis() - timestamp) < frameTime)
                Thread.sleep(frameTime - checkTime);
            timestamp += checkTime;
        }

        //Display the FPS
        System.out.println((double)noFrames / ((System.currentTimeMillis() - origin) / 1000d));

        //Close the JFrame and exit
        engine.frame.dispose();

    }
}
