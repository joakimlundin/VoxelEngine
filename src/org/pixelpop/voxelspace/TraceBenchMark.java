package org.pixelpop.voxelspace;

import java.io.IOException;

/**
 * Created by Joakim on 3/15/2017.
 */
public class TraceBenchMark {
    VoxelSpace space;
    VoxelView view;

    public TraceBenchMark() throws IOException {
        view = new VoxelView(space, 800, 450);
    }

    public void run() {
        long timeStamp = System.currentTimeMillis();
        for(int i = 0; i < 1000000; i++) {
            view.traceRay(0, -600d, 400d);
        }
        System.out.println("Done: " + (System.currentTimeMillis() - timeStamp));
    }

    public static void main(String[] args) throws IOException {
        TraceBenchMark benchMark = new TraceBenchMark();
        benchMark.run();
    }
}
