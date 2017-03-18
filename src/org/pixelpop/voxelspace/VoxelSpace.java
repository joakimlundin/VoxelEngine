package org.pixelpop.voxelspace;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class VoxelSpace {
    public int[] colorMap;
    public short[] heightMap;
    private int width;
    private int height;

    public VoxelSpace(InputStream colorMapResource, InputStream heightMapResource) throws IOException {
        /**
         * Retrieve color map as an array from the input stream
         */
        BufferedImage indexedImage = ImageIO.read(colorMapResource);
        BufferedImage rgbImage = new BufferedImage(indexedImage.getWidth(), indexedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgbImage.getGraphics().drawImage(indexedImage, 0, 0, null);
        colorMap = ((DataBufferInt)rgbImage.getRaster().getDataBuffer()).getData();
        width = rgbImage.getWidth();
        height = rgbImage.getHeight();

        /**
         * Retrieve the heightmap as an array
         */
        BufferedImage heightImage = ImageIO.read(heightMapResource);
        BufferedImage blImage = new BufferedImage(heightImage.getWidth(), heightImage.getHeight(), BufferedImage.TYPE_USHORT_GRAY);
        blImage.getGraphics().drawImage(heightImage, 0, 0, null);
        heightMap = ((DataBufferUShort)blImage.getRaster().getDataBuffer()).getData();

    }

    public VoxelSpace(int width, int height) {
        this.width = width;
        this.height = height;

        colorMap = new int[width * height];
        heightMap = new short[width * height];

        addBump(90, 400, 400, 400);
        addBump(110, 70, 200, 200);
        addBump(130, 40, 300, 300);
        addBump(120, 80, 500, 500);
        addBump(100, 30, 600, 600);
        addBump(170, 80, 700, 700);
        addBump(100, 20, 800, 800);
        addBump(100, 80, 200, 800);
        addBump(80, 200, 300, 700);
        addBump(140, 150, 400, 900);

        for(int i = 0; i < colorMap.length; i++) {
            if(heightMap[i] > 20) {
                colorMap[i] = (100 + (int) (Math.random() * 50)) << 8;
            } else {
                colorMap[i] = 100 + (int) (Math.random() * 50);
            }
        }

    }

    private void addBump(int bumpHeight, int steepness, int x, int y) {
        double maxDistance = steepness;

        for(int i = 0; i < steepness * 2; i++) {
            for(int j = 0; j < steepness * 2; j++) {
                double distance = Math.sqrt((i - steepness) * (i - steepness) + (j - steepness) * (j - steepness));
                if(distance > maxDistance) continue;
                heightMap[((x - steepness + i) & (width - 1)) + (((y - steepness + j) & (height - 1)) * width)] += (short)((Math.cos((distance * Math.PI)/steepness) + 1) * bumpHeight / 2);
            }
        }
    }

    public int getColor(int x, int y) {
        return colorMap[(x & (width - 1)) + ((y & (height - 1)) * width)];
    }

    public int getHeight(int x, int y) {
        return heightMap[(x & (width - 1)) + ((y & (height - 1)) * width)] & 0xFF;
    }

}
