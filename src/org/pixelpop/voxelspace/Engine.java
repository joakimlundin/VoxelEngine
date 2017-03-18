package org.pixelpop.voxelspace;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Created by Joakim on 3/13/2017.
 */
public class Engine {
    private int width = 800;
    private int height = 450;

    private VoxelView voxelView;
    private VoxelSpace voxelSpace;

    private BufferedImage rendering;
    private JFrame frame;

    private Engine() throws IOException {
        frame = new JFrame();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setUndecorated(true);
        frame.setVisible(true);

        int frameWidth = (int)frame.getSize().getWidth();
        JPanel canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(rendering.getScaledInstance(frameWidth, -1, Image.SCALE_FAST), 0, 0, null);
                //g.drawImage(rendering, 0, 0, null);
            }
        };

        canvas.addMouseMotionListener(new MouseMotionListener() {
            private int lastMousePositionY = -1;
            private int lastMousePositionX = -1;

            @Override
            public void mouseDragged(MouseEvent e) {

            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if(lastMousePositionX != -1) {
                    voxelView.rotate(((double)e.getX() - lastMousePositionX) / 100);
                }
                if(lastMousePositionY != -1) {
                    voxelView.move(-1 * ((double)e.getY() - lastMousePositionY));
                }

                lastMousePositionX = e.getX();
                lastMousePositionY = e.getY();
            }
        });

        frame.add(canvas);
        frame.validate();
        frame.repaint();

        rendering = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        voxelSpace = new VoxelSpace(1024, 1024);
        voxelView = new VoxelView(voxelSpace, width, height);

    }

    private void close() {
        frame.dispose();
    }

    private void update() {
        try {
            voxelView.update();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        rendering.getRaster().setDataElements(0, 0, width, height, voxelView.getImage());
        frame.repaint();

        voxelView.move(1);
        voxelView.rotate(-.001);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int noFrames = 1000;
        long frameTime = 16;
        Engine engine = new Engine();

        long origin = System.currentTimeMillis();
        long timestamp = System.currentTimeMillis();
        long checkTime;

        for(int i = 0; i < noFrames; i++) {
            engine.update();

            if((checkTime = System.currentTimeMillis() - timestamp) < frameTime)
                Thread.sleep(frameTime - checkTime);
            timestamp += checkTime;
        }
        System.out.println((double)noFrames / ((System.currentTimeMillis() - origin) / 1000d));

        engine.close();

    }
}
