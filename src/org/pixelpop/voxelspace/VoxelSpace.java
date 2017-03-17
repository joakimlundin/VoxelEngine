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

    public int getColor(int x, int y) {
        return colorMap[(x & (width - 1)) + ((y & (height - 1)) * width)];
    }

    public int getHeight(int x, int y) {
        return heightMap[(x & (width - 1)) + ((y & (height - 1)) * width)] & 0xFF;
    }

}
