package org.pixelpop.voxelspace;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

/**
 * VoxelSpace represents the model in the Voxel Engine.
 * This class contains the color information, the collision information and the height rendering information. These
 * values are stored in three equally sized arrays which are accessible package wide.
 *
 * @author Joakim Lundin (joakim.lundin@dynabyte.se)
 */
public class VoxelSpace {
    protected int[] colorMap;
    protected short[] renderHeightMap;
    protected short[] heightMap;
    protected final int width;
    protected final int height;

    /**
     * Creates a VoxelSpace based on the input stream of a color map resource and a height map resource. VoxelSpace
     * width and height is set from the color map and the height map is expected and required to share the same size
     * and proportions.
     * @param colorMapResource the color map resource
     * @param heightMapResource the height map resource
     * @throws IOException
     */
    public VoxelSpace(InputStream colorMapResource, InputStream heightMapResource) throws IOException {
        //Retrieve color map as an array from the input stream
        BufferedImage indexedImage = ImageIO.read(colorMapResource);
        BufferedImage rgbImage = new BufferedImage(indexedImage.getWidth(), indexedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgbImage.getGraphics().drawImage(indexedImage, 0, 0, null);
        colorMap = ((DataBufferInt)rgbImage.getRaster().getDataBuffer()).getData();

        width = rgbImage.getWidth();
        height = rgbImage.getHeight();
        heightMap = new short[width * height];

        //Retrieve the heightmap as an array
        BufferedImage heightImage = ImageIO.read(heightMapResource);
        BufferedImage blImage = new BufferedImage(heightImage.getWidth(), heightImage.getHeight(), BufferedImage.TYPE_USHORT_GRAY);
        blImage.getGraphics().drawImage(heightImage, 0, 0, null);
        renderHeightMap = ((DataBufferUShort)blImage.getRaster().getDataBuffer()).getData();

        System.arraycopy(renderHeightMap, 0, heightMap, 0, renderHeightMap.length);
    }

    /**
     * Creates a VoxelSpace based on the given width and height values for the VoxelSpace. This constructor will create
     * a static terrain generated from code.
     * @param width the width of the VoxelSpace
     * @param height the height of th VoxelSpace
     */
    public VoxelSpace(int width, int height) {
        //Set width and height
        this.width = width;
        this.height = height;

        //Create VoxelSpace resources
        colorMap = new int[width * height];
        renderHeightMap = new short[width * height];
        heightMap = new short[width * height];

        //Generate some hills
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

        //Based on the height value map some color; 0 is water, 1-5 is beach, 6 and higher is grass
        for(int i = 0; i < colorMap.length; i++) {
            if(renderHeightMap[i] > 5) {
                //Let the grass grow!
                if(Math.random() > 0.8) renderHeightMap[i] += (int)(Math.random() * 30);
                colorMap[i] = (100 + (int) (Math.random() * 50)) << 8;
            } else if(renderHeightMap[i] > 0) {
                colorMap[i] = ((100 + (int) (Math.random() * 50)) << 16) + ((100 + (int) (Math.random() * 50)) << 8);
            } else {
                colorMap[i] = 100 + (int) (Math.random() * 50);
            }
        }

    }

    /**
     * Creates a hill based on a sin curve. The hill will be placed atop of the existing terrain.
     *
     * @param bumpHeight the height of the bump
     * @param steepness how steep the hill sides should be. The steepness is the radius of the hill. The lower the value
     *                  is the steeper the hill will be
     * @param x the x position of where the bump should be placed
     * @param y the y position of where the bump should be placed
     */
    private void addBump(int bumpHeight, int steepness, int x, int y) {
        double maxDistance = steepness;

        for(int i = 0; i < steepness * 2; i++) {
            for(int j = 0; j < steepness * 2; j++) {
                double distance = Math.sqrt((i - steepness) * (i - steepness) + (j - steepness) * (j - steepness));
                if(distance > maxDistance) continue;
                renderHeightMap[((x - steepness + i) & (width - 1)) + (((y - steepness + j) & (height - 1)) * width)] += (short)((Math.cos((distance * Math.PI)/steepness) + 1) * bumpHeight / 2);
            }
        }

        System.arraycopy(renderHeightMap, 0, heightMap, 0, renderHeightMap.length);
    }

}
