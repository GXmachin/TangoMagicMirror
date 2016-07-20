package com.example.samuel.myapplication;

import android.graphics.Color;

import org.rajawali3d.materials.Material;

import java.nio.FloatBuffer;

/**
 * Created by samuel on 7/20/16.
 */
public class PointCloud extends Points {

    public static final float CLOUD_MAX_Z = 5;

    private float[] mColorArray;
    private final int[] mPalette;
    public static final int PALETTE_SIZE = 360;
    public static final float HUE_BEGIN = 0;
    public static final float HUE_END = 320;
    public double xProjNear =0;
    public double yProjNear =0 ;
    public double zProjNear = 0.0;

    public PointCloud(int maxPoints) {
        super(maxPoints, true);
        mPalette = createPalette();
        mColorArray = new float[maxPoints * 4];
        Material m = new Material();
        m.useVertexColors(true);
        setMaterial(m);
    }

    /**
     * Pre-calculate a palette to be used to translate between point distance and RGB color.
     */
    private int[] createPalette() {
        int[] palette = new int[PALETTE_SIZE];
        float[] hsv = new float[3];
        hsv[1] = hsv[2] = 1;
        for (int i = 0; i < PALETTE_SIZE; i++) {
            hsv[0] = (HUE_END - HUE_BEGIN) * i / PALETTE_SIZE + HUE_BEGIN;
            palette[i] = Color.HSVToColor(hsv);
        }
        return palette;
    }

    /**
     * Calculate the right color for each point in the point cloud.
     */
    private void calculateColors(int pointCount, FloatBuffer pointCloudBuffer) {
        float[] points = new float[pointCount * 3];
        pointCloudBuffer.rewind();
        pointCloudBuffer.get(points);
        pointCloudBuffer.rewind();

        int color;
        int colorIndex;
        float z;
        for (int i = 0; i < pointCount; i++) {
            z = points[i * 3 + 2];
            colorIndex = (int) Math.min(z / CLOUD_MAX_Z * mPalette.length, mPalette.length - 1);
            colorIndex = Math.max(colorIndex, 0);
            color = mPalette[colorIndex];
            mColorArray[i * 4] = Color.red(color) / 255f;
            mColorArray[i * 4 + 1] = Color.green(color) / 255f;
            mColorArray[i * 4 + 2] = Color.blue(color) / 255f;
            mColorArray[i * 4 + 3] = Color.alpha(color) / 255f;

            if((Math.abs(points[i * 3 ]  - xProjNear) +   Math.abs(points[i * 3  + 1]  - yProjNear)) < 0.001 ){

                //use the depth of this point to calculate the alpha

                mColorArray[i * 4] = 0;
                mColorArray[i * 4 + 1] = 0;
                mColorArray[i * 4 + 2] =0;
                mColorArray[i * 4 + 3] =0;

            }
        }
    }

    /**
     * Update the points and colors in the point cloud.
     */
    public void updateCloud(int pointCount, FloatBuffer pointBuffer) {
        calculateColors(pointCount, pointBuffer);
        updatePoints(pointCount, pointBuffer, mColorArray);
    }


}
