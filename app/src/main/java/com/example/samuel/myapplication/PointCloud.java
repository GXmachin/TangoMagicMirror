package com.example.samuel.myapplication;

import android.graphics.Color;
import android.util.Log;

import org.rajawali3d.materials.Material;
import org.rajawali3d.math.Matrix;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

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
    public Vector3 rayDirection = new Vector3(1,1,1);
    public Vector3 rayStart = new Vector3(0,0,0);
    private Vector3 currentPoint = null;
    private Vector3 currentprojectedPoint = null;
    private double threshold = 0.02;
    private double alpha =0;
    public boolean pointTouched = false;
    public boolean aveReady = false;
    private double ave = 0;

    public Renderer currentRender;
    private int[] pixelCoord;

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




        Log.d("gDebug" , "count: " + pointCount);

        int color;
        int colorIndex;
        float z;
        int withinCount =0;
        for (int i = 0; i < pointCount; i++) {
            z = points[i * 3 + 2];
            colorIndex = (int) Math.min(z / CLOUD_MAX_Z * mPalette.length, mPalette.length - 1);
            colorIndex = Math.max(colorIndex, 0);
            color = mPalette[colorIndex];
            mColorArray[i * 4] = Color.red(color) / 255f;
            mColorArray[i * 4 + 1] = Color.green(color) / 255f;
            mColorArray[i * 4 + 2] = Color.blue(color) / 255f;
            mColorArray[i * 4 + 3] = Color.alpha(color) / 255f;

            if(currentRender.oglScreen != null) {
                int xIndex = i * 3;
                currentPoint = new Vector3(points[xIndex], points[xIndex + 1], points[xIndex + 2]);
           //     Vector3 currentprojectedPoint2 = currentPoint.clone().project(currentRender.curreentMVP.multiply(this.getModelMatrix().clone()));
                currentprojectedPoint = currentPoint.clone().project(currentRender.curreentMVP);


             //   Log.d("gDebug" , "currentPoint : x" + currentPoint.x + " y: " + currentPoint.y + "z " + currentPoint.z);
             //   Log.d("gDebug" , "NDC : x" + currentprojectedPoint.x + " y: " + currentprojectedPoint.y + "z" + currentprojectedPoint.z);
             //   Log.d("gDebug" , "  --  >>  " + currentRender.curreentMVP.toString());

                pixelCoord = convertToPixelCoord(currentprojectedPoint);



                if (Math.abs(pixelCoord[0] - currentRender.oglScreen[0]) +  Math.abs(pixelCoord[1] - currentRender.oglScreen[1]) < 50) {

                  //  Log.d("gDebug" , "currentPoint : x" + currentPoint.x + " y: " + currentPoint.y);
                  //  Log.d("gDebug" , "NDC : x" + currentprojectedPoint.x + " y: " + currentprojectedPoint.y);
                 //   Log.d("gDebug" , "NDC2 : x" + currentprojectedPoint2.x + " y: " + currentprojectedPoint2.y);
                    Log.d("gDebug" , "current : x" + currentRender.oglScreen[0] + " y: " + currentRender.oglScreen[1]);
                    Log.d("gDebug" , "Estimate : x" + pixelCoord[0] + " y: " + pixelCoord[1]);


                    mColorArray[i * 4] = 1.0f;
                    mColorArray[i * 4 + 1] = 1.0f;
                    mColorArray[i * 4 + 2] = 1.0f;


                }
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


   public int[] convertToPixelCoord(Vector3 xyz){


         //   double x = currentRender.getDefaultViewportWidth() - xyz.x;
         //   double y = currentRender.getDefaultViewportHeight() - xyz.y;


            double x = (xyz.x + 1)* currentRender.getDefaultViewportWidth()/2;
            double y = (xyz.y + 1)* currentRender.getDefaultViewportHeight()/2;

            x = currentRender.getDefaultViewportWidth() -x;
            y = currentRender.getDefaultViewportHeight() - y;

            int[] xy = new int[2];

       //convert to location on screen before using
    //   int[] viewLoc = new int[2];
    //   currentRender.surface.getLocationOnScreen(viewLoc);

            xy[0]= (int)x;// + viewLoc[0];
            xy[1] = (int)y;// + viewLoc[1];


     return xy;

    }
}
