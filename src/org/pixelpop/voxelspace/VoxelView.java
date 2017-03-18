package org.pixelpop.voxelspace;

/**
 * VoxelView is the view of the Voxel Space Engine and provides an RGB image representation of the current frame.
 * The class must have a reference to an instance of VoxelSpace which acts as the model that the VoxelView is presenting.
 * VoxelView encapsulates information on how the VoxelSpace is intended to be displayed such as from what coordinate for
 * how for into the distance and by what angle.
 *
 * @author Joakim Lundin (joakim.lundin@dynabyte.se)
 *
 */
public class VoxelView {
    //The "model"
    private VoxelSpace voxelSpace;

    //The image resources used to reference, clear and update the view
    private final int[][] clearRaster;
    private final int[][] imageRaster;
    private final int[] image;

    //Properties for the image representation.
    private final int width;
    private final int height;

    //"Camera" properties
    private double positionX;
    private double positionY;
    private double positionZ;
    private double rotationZ;
    private double rotationX;
    private int viewDepth;
    private double fieldOfView;
    private double hover;

    //This values will be precalculated in the constructor to take load from the view update
    private final double[] rayLengths;
    private final double[] depthSteps;
    private final double[] xAdjustments;

    //The renderers used by the rendering threads
    private Runnable[] renderers;
    private Thread[] renderThreads;

    //Used as a member variable to avoid redundant calculations. The horizont is used
    private int horizonHeight;

    //The number of threads that will be used to trace the image
    private static final int numberOfThreads = 4;

    /**
     * Instantiates a VoxelView. The constructor will also make a series of precalculated values used during update of
     * the VoxelView.
     * @param voxelSpace the VoxelSpace - the model that the view will be presenting
     * @param width the width of the view screen
     * @param height the height of the view screen
     */
    public VoxelView(VoxelSpace voxelSpace, int width, int height) {
        //Assigning key values and resources
        this.voxelSpace = voxelSpace;
        this.width = width;
        this.height = height;

        //Creating image resources
        clearRaster = new int[width][height];
        imageRaster = new int[width][height];
        image = new int[width * height];

        //Setting view properties
        fieldOfView = 1.2;
        viewDepth = 400;
        positionX = 512;
        positionY = 800;
        positionZ = 300;
        rotationX = 0;
        rotationZ = 0;
        hover = 100;

        //Creating value stores for pre-calculated values
        xAdjustments = new double[width];
        rayLengths = new double[width];
        depthSteps = new double[width];

        //Pre-calculating values for each raster column
        for(int i = 0; i < width; i++) {
            xAdjustments[i] = ((i - (width / 2)) * fieldOfView);
            rayLengths[i] = Math.sqrt(xAdjustments[i] * xAdjustments[i] + viewDepth * viewDepth);
            depthSteps[i] = viewDepth / rayLengths[i];
        }

        //Setting the horizon height which will be used during perspective calculations
        horizonHeight = height / 2;

        //Setting the background image. In this case a sky is created in a "crude" way
        for(int i = 0; i < clearRaster.length; i++) {
            for(int j = 0; j < clearRaster[i].length; j++) {
                int skyColor = (50 << 16) + (50 << 8) + 150;

                if(j < horizonHeight * 1.5) {
                    int whiteFactor = 100 * (j * j) / (horizonHeight * horizonHeight);
                    int whiteAddition = (whiteFactor << 16) + (whiteFactor << 8);
                    skyColor += whiteAddition;
                }

                clearRaster[i][j] = skyColor;
            }
        }

        /* Creating the renderers. The renderer is an anonymous implementation of Runnable which is assigned a raster
        column interval in which it will ray trace */
        renderers = new Runnable[numberOfThreads];
        for(int i = 0; i < renderers.length; i++) {
            int rendererNumber = i;
            int rasterSize = width / renderers.length;
            renderers[i] = new Runnable() {
                private int adjustment = rasterSize * (rendererNumber);
                @Override
                public void run() {
                    for(int i = 0; i < rasterSize; i++) {
                        double rayPositionX = ((Math.sin(rotationZ) * viewDepth) + (Math.cos(rotationZ) * xAdjustments[adjustment + i]));
                        double rayPositionY = ((Math.cos(rotationZ) * viewDepth) - (Math.sin(rotationZ) * xAdjustments[adjustment + i]));

                        traceRay(adjustment + i, rayPositionX, rayPositionY);
                    }
                }
            };
        }
        //This doesn't really save a lot of time - but while I'm at it...
        renderThreads = new Thread[renderers.length];
    }

    /**
     * Updates the view screen according to the properties of the camera. The update method will spawn a number of
     * threads (depending on the member variable "numberOfThreads". The threads will each trace the VoxelSpace for their
     * assigned column interval. When the threads are all finshed the result will be compiled into an RGB image
     * represented in an integer array.
     *
     * @throws InterruptedException
     */
    public void update() throws InterruptedException {
        //Replace the view image with the background image
        for(int i = 0; i < imageRaster.length; i++) {
            System.arraycopy(clearRaster[i], 0, imageRaster[i], 0, clearRaster[i].length);
        }

        //Create new Threads for each renderer
        for(int i = 0; i < renderers.length; i++) {
            renderThreads[i] = new Thread(renderers[i]);
            renderThreads[i].start();
        }

        //Wait for all threads to finish
        for(Thread thread : renderThreads) {
            thread.join();
        }

        //Compile final array represented images
        for(int i = 0; i < width * height; i++) {
            image[i] = imageRaster[i % width][i / width];
        }
    }

    /**
     * Traces a ray on the VoxelSpace for the given column in the image raster and to the given X, Y coordinates. This
     * is "where the magic happens". Each ray traces and paints a column in the image raster. The ray originates from
     * the X, Y coordinates for the VoxelView and paints each pixel progressively towards the given X, Y parameters.
     * The column number helps reference the pre-calculated values.
     *
     * @param columnNo the column in the image raster that the ray tracing is executed for
     * @param rayPositionX the xPosition of the end of the ray
     * @param rayPositionY the yPosition of the end of the ray
     */
    private void traceRay(int columnNo, double rayPositionX, double rayPositionY) {
        //Setting the origin for the ray tracing
        double pixelPositionX = positionX;
        double pixelPositionY = positionY;

        //Calculating the distance in the X and the Y direction each step will move the ray
        double xStep = rayPositionX / rayLengths[columnNo];
        double yStep = rayPositionY / rayLengths[columnNo];

        //The highest pointed in the raster which is painted. Anything lower than this will be ignored
        int painted = 0;

        //To make pixels further away to appear darker their color will be multiplied with the shadowFactor
        double shadowFactor = 1;

        //This is where we step through the pixels of the terrain image
        for(int i = 1; i < rayLengths[columnNo]; i++) {
            //We decrease the shadow factor based on the distance we have traced
            if(i % 20 == 0) shadowFactor = (rayLengths[columnNo] - i) / rayLengths[columnNo];

            //Increasing the X, Y coordinate with each step
            pixelPositionX += xStep;
            pixelPositionY += yStep;

            //Retrieve the Voxel color from the color map of the VoxelSpace for the current coordinate
            int voxelColor = voxelSpace.colorMap[(((int)pixelPositionX) & (voxelSpace.width - 1)) + ((((int)pixelPositionY) & (voxelSpace.height - 1)) * voxelSpace.width)];

            //We "filter" the color making it appear darker the further away it is
            voxelColor = filterColor(voxelColor, shadowFactor);

            /* Retrieving the voxel height value and subtracting the VoxelView Z position (height) as it has bearing on
            how height the voxel will appear */
            double voxelHeight = (voxelSpace.renderHeightMap[(((int) pixelPositionX) & (voxelSpace.width - 1)) + ((((int) pixelPositionY) & (voxelSpace.height - 1)) * voxelSpace.width)] & 0xFF ) - positionZ;

            //Setting the absolute height on the image raster by calculating the perspective
            int absoluteHeight = (int)(horizonHeight + voxelHeight / (depthSteps[columnNo] * i) * 100);

            //Paint pixels in the raster based to the height the voxel will reach
            for(int k = painted; k < absoluteHeight; k++) {
                //Set the color value in the raster
                imageRaster[columnNo][height - k - 1] = voxelColor;

                //Increase the painted voxel value. No point into painting anything lower than this
                painted = k;
            }

        }
    }

    /**
     * Makes voxel appear darker the further away they are. This method will multiply each RGB value with the given
     * factor value. The factor is expected to be between 0.0 and 1.0
     * @param color the color value that should be made darker
     * @param factor the factor of the darkening
     * @return the new, darker color value
     */
    private int filterColor(int color, double factor) {
        int r = color >> 16;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        int filteredColor = (((int)(r * factor)) << 16) + (((int)(g * factor)) << 8) + ((int)(b * factor));

        return  filteredColor;
    }

    /**
     * Returns the an int RGB array representation of the VoxelView
     * @return the RGB array
     */
    public int[] getImage() {
        return image;
    }

    /**
     * Rotates the view with the given value of radians
     * @param motion the value in radians that the VoxelView should be rotated
     */
    public void rotate(double motion) {
        rotationZ += motion;
    }

    /**
     * Moves the view from it current position either forward (positive) or backwards (negative) based on the given
     * value. The movement is based upon the direction of VoxelView, i.e. the rotation around the Z axis.
     * @param move the amount that the VoxelView should be moved
     */
    public void move(double move) {
        positionX += Math.sin(rotationZ) * move;
        positionY += Math.cos(rotationZ) * move;

        positionZ = (voxelSpace.heightMap[(((int)positionX) & (voxelSpace.width - 1)) + ((((int)positionY) & (voxelSpace.height - 1)) * voxelSpace.width)] & 0xFF) + hover;
    }
}
