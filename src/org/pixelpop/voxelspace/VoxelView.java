package org.pixelpop.voxelspace;

public class VoxelView {
    private VoxelSpace voxelSpace;
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

    /**
     * Instantiates a VoxelView
     * @param width the width of the view screen
     * @param height the height of the view screen
     */
    public VoxelView(VoxelSpace voxelSpace, int width, int height) {
        this.voxelSpace = voxelSpace;
        this.width = width;
        this.height = height;
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

        heightTwo = height / 2;
    }
    int heightTwo;

    private void clear() {
        for(int i = 0; i < width * height; i++) {
        image[i] = 0;
    }
}

    /**
     * Updates the view screen according to the properties of the camera
     */
    public void update() {
        long timetamp = System.currentTimeMillis();
        clear();
        long afterClear = System.currentTimeMillis();
        long afterDirections = 0;
        long fromStart = System.nanoTime();
        for(int i = 0; i < width; i++) {
            if(i == 0) {
                fromStart = System.nanoTime();
            }
            double rayPositionX = ((Math.sin(rotationZ) * viewDepth) + (Math.cos(rotationZ) * xAdjustments[i]));
            double rayPositionY = ((Math.cos(rotationZ) * viewDepth) - (Math.sin(rotationZ) * xAdjustments[i]));
            if(i == 0) {
                afterDirections = System.nanoTime();
            }
            traceRay(i, rayPositionX, rayPositionY);
            if(i == 0) {
                System.out.println("Trace timings: " + (System.nanoTime() - afterDirections) + " " + (afterDirections - fromStart));
            }

        }
        System.out.println(afterClear - timetamp + " " + (System.currentTimeMillis() - afterClear));
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
            int absoluteHeight = (int)(heightTwo + voxelHeight / (depthSteps[columnNo] * i) * 100);

            //double absoluteHeight = heightTwo + relativeHeight;
            for(int k = painted; k < absoluteHeight; k++) {
                image[width * (height - k - 1) + columnNo] = voxelColor;
                painted = k;
            }

        }
    }

    /*private void traceRay(int columnNo, double rayPositionX, double rayPositionY) {
        double pixelPositionX = positionX;
        double pixelPositionY = positionY;

        double xStep = rayPositionX / rayLengths[columnNo];
        double yStep = rayPositionY / rayLengths[columnNo];

        int painted = 0;
        for(int i = 1; i < rayLengths[columnNo]; i++) {
            pixelPositionX += xStep;
            pixelPositionY += yStep;
            //int voxelColor2 = voxelSpace.getColor((int)pixelPositionX, (int)pixelPositionY);
            //int voxelHeight2 = voxelSpace.getHeight((int)pixelPositionX, (int)pixelPositionY);
            int voxelColor = voxelSpace.colorMap[(((int)pixelPositionX) & (1024 - 1)) + ((((int)pixelPositionY) & (1024 - 1)) * 1024)];
            int voxelHeight = voxelSpace.heightMap[(((int)pixelPositionX) & (1024 - 1)) + ((((int)pixelPositionY) & (1024 - 1)) * 1024)] & 0xFF;
            //if(columnNo == 0 && (voxelColor != voxelColor2 || voxelHeight != voxelHeight2)) System.out.println(voxelColor + "  " + voxelHeight + " | " + voxelColor2 + "  " + voxelHeight2);

            voxelHeight -= (double)positionZ;

            double relativeHeight = voxelHeight / (depthSteps[columnNo] * i) * 100;

            double absoluteHeight = (height / 2) + relativeHeight;
            for(int k = painted; k < (int) absoluteHeight; k++) {
                image[width * (height - k - 1) + columnNo] = voxelColor;
                painted = k;
            }

        }
    }*/

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

}
