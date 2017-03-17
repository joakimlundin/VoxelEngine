package org.pixelpop.voxelspace;

public class VoxelView implements Runnable {
    private VoxelSpace voxelSpace;
    private int[][] clearRaster;
    private int[][] imageRaster;
    private int[] image;


    //Image size
    private int width;
    private int height;

    //Camera properties
    private double positionX;
    private double positionY;
    private double positionZ;
    private double rotationZ;
    private double rotationX;
    private int viewDepth;
    private double fieldOfView;
    private double hover;

    private final double[] rayLengths;
    private final double[] depthSteps;
    private final double[] xAdjustments;
    private final double[] xSteps;
    private final double[] ySteps;

    private Thread[] renderers;

    private int horizonHeight;

    /**
     * Instantiates a VoxelView
     * @param width the width of the view screen
     * @param height the height of the view screen
     */
    public VoxelView(VoxelSpace voxelSpace, int width, int height) {
        this.voxelSpace = voxelSpace;
        this.width = width;
        this.height = height;
        clearRaster = new int[width][height];
        imageRaster = new int[width][height];
        image = new int[width * height];
        fieldOfView = 1.5;

        viewDepth = 400;
        positionX = 512;
        positionY = 800;
        positionZ = 300;
        rotationX = 0;
        rotationZ = 0;
        hover = 100;

        xAdjustments = new double[width];
        rayLengths = new double[width];
        depthSteps = new double[width];
        xSteps = new double[width];
        ySteps = new double[width];

        for(int i = 0; i < width; i++) {
            xAdjustments[i] = ((i - (width / 2)) * fieldOfView);
            rayLengths[i] = Math.sqrt(xAdjustments[i] * xAdjustments[i] + viewDepth * viewDepth);
            depthSteps[i] = viewDepth / rayLengths[i];
            xSteps[i] = xAdjustments[i] / rayLengths[i];
            ySteps[i] = viewDepth / rayLengths[i];
        }

        horizonHeight = height / 2;

        for(int i = 0; i < clearRaster.length; i++) {
            for(int j = 0; j < clearRaster[i].length; j++) {
                clearRaster[i][j] = 0;
            }
        }

        renderers = new Thread[4];

        for(int i = 0; i < renderers.length; i++) {
            int rendererNumber = i;
            int rasterSize = width / renderers.length;
            renderers[i] = new Thread(new Runnable() {
                private int adjustment = rasterSize * (rendererNumber);
                @Override
                public void run() {
                    for(int i = 0; i < rasterSize; i++) {
                        double rayPositionX = ((Math.sin(rotationZ) * viewDepth) + (Math.cos(rotationZ) * xAdjustments[adjustment + i]));
                        double rayPositionY = ((Math.cos(rotationZ) * viewDepth) - (Math.sin(rotationZ) * xAdjustments[adjustment + i]));

                        traceRay(adjustment + i, rayPositionX, rayPositionY);
                    }
                }
            });
        }
    }

    /**
     * Updates the view screen according to the properties of the camera
     */
    public void update() throws InterruptedException {
        for(int i = 0; i < imageRaster.length; i++) {
            System.arraycopy(clearRaster[i], 0, imageRaster[i], 0, clearRaster[i].length);
        }

        for(Thread thread : renderers) {
            thread.run();
        }

        for(Thread thread : renderers) {
            thread.join();
        }

        for(int i = 0; i < width * height; i++) {
            image[i] = imageRaster[i % width][i / width];
        }
    }

    public void traceRay(int columnNo, double rayPositionX, double rayPositionY) {
        double pixelPositionX = positionX;
        double pixelPositionY = positionY;

        double xStep = rayPositionX / rayLengths[columnNo];
        double yStep = rayPositionY / rayLengths[columnNo];

        int painted = 0;
        for(int i = 1; i < rayLengths[columnNo]; i++) {
            pixelPositionX += xStep;
            pixelPositionY += yStep;
            int voxelColor = voxelSpace.colorMap[(((int)pixelPositionX) & (1023)) + ((((int)pixelPositionY) & (1023)) * 1024)];

            double voxelHeight = (voxelSpace.heightMap[(((int) pixelPositionX) & (1023)) + ((((int) pixelPositionY) & (1023)) * 1024)] & 0xFF) - positionZ;
            int absoluteHeight = (int)(horizonHeight + voxelHeight / (depthSteps[columnNo] * i) * 100);

            //double absoluteHeight = horizonHeight + relativeHeight;
            for(int k = painted; k < absoluteHeight; k++) {
                imageRaster[columnNo][height - k - 1] = voxelColor;
                painted = k;
            }

        }
    }

    public int[] getImage() {
        return image;
    }

    public void rotate(double motion) {
        rotationZ += motion;
    }

    public void move(double move) {
        positionX += Math.sin(rotationZ) * move;
        positionY += Math.cos(rotationZ) * move;

        positionZ = voxelSpace.getHeight((int)positionX, (int)positionY) + hover;
    }

    @Override
    public void run() {

    }
}
