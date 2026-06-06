package com.ishland.c2me.opts.dfc.common.util;

public final class DfcCoordinateFiller {

    private DfcCoordinateFiller() {
    }

    public static void fillMainCellCoordinates(int[] x, int[] y, int[] z, int startBlockX, int startBlockY, int startBlockZ, int horizontalCellBlockCount, int verticalCellBlockCount) {
        int index = 0;
        for (int i = verticalCellBlockCount - 1; i >= 0; i--) {
            int blockY = startBlockY + i;
            for (int j = 0; j < horizontalCellBlockCount; j++) {
                int blockX = startBlockX + j;
                for (int k = 0; k < horizontalCellBlockCount; k++) {
                    x[index] = blockX;
                    y[index] = blockY;
                    z[index] = startBlockZ + k;
                    index++;
                }
            }
        }
    }

    public static void fillColumnCoordinates(int[] x, int[] y, int[] z, int startBlockX, int startBlockZ, int cellBlockX, int cellBlockZ, int minimumCellY, int verticalCellBlockCount, int verticalCellCount) {
        for (int i = 0; i < verticalCellCount + 1; i++) {
            x[i] = startBlockX + cellBlockX;
            y[i] = (i + minimumCellY) * verticalCellBlockCount;
            z[i] = startBlockZ + cellBlockZ;
        }
    }
}
