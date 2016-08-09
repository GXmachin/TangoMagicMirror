package com.example.samuel.myapplication;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.textures.ASingleTexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.math.Matrix;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.opencv.imgproc.Imgproc.CV_SHAPE_ELLIPSE;
import static org.opencv.imgproc.Imgproc.CV_SHAPE_RECT;
import static org.opencv.imgproc.Imgproc.dilate;
import static org.opencv.imgproc.Imgproc.getStructuringElement;

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
    public double ave = 0;
    public boolean saveCloud = false;
    private static int fileIndex = 0;


    public Renderer currentRender;
    private int[] pixelCoord;
    private float maxZ;



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
        String lineHolder; // for composing a line of xyz data
        FileWriter fw = null;


        //attempt to set the points
        if(currentRender.imgData != null )
        currentRender.imgData.put(0,0, points);

    //    imgData = Mat.zeros(currentRender.getDefaultViewportHeight() , currentRender.getViewportWidth(), CvType.CV_32FC1);

        if(saveCloud){
            String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            String fileName = "tangoData_" + (fileIndex++) +".gPly";
            String filePath = baseDir + File.separator + fileName; //seperator is mostlikely '/'
            File file = new File(filePath);

            try {

                fw = new FileWriter(file);

            }catch (Exception e){
                Log.e("gDebug" , e.getMessage());
                saveCloud = false;
            }


        }


       // Log.d("gDebug" , "count: " + pointCount);

        int color;
        int colorIndex;
        float z;
        int withinCount =0;

        //try to get the depth buffer as image


        for (int i = 0; i < pointCount; i++) {

            int xIndex = i * 3;

            z = points[xIndex + 2];
            colorIndex = (int) Math.min(z / CLOUD_MAX_Z * mPalette.length, mPalette.length - 1);
            colorIndex = Math.max(colorIndex, 0);
            color = mPalette[colorIndex];
            mColorArray[i * 4] = Color.red(color) / 255f;
            mColorArray[i * 4 + 1] = Color.green(color) / 255f;
            mColorArray[i * 4 + 2] = Color.blue(color) / 255f;
            mColorArray[i * 4 + 3] = Color.alpha(color) / 255f;



            //keep the depth buffer as needed
            if(currentRender.detectH){



                if(z > maxZ) maxZ = z;

                currentPoint = new Vector3(points[xIndex], points[xIndex + 1], points[xIndex + 2]);

                currentprojectedPoint = currentPoint.clone().project(currentRender.curreentMVP);

                pixelCoord = convertToPixelCoord(currentprojectedPoint);


                if(pixelCoord[0] >= 0 && pixelCoord[0] < currentRender.getDefaultViewportWidth() && pixelCoord[1] >= 0  && pixelCoord[1] < currentRender.getDefaultViewportHeight())
                currentRender.depthMap[pixelCoord[1] * currentRender.getDefaultViewportWidth() + pixelCoord[0]] = z;

            }

            //set OpenCV mat

            //Export pointCloud for later use and test
            if (saveCloud && fw != null) {

                try{
                    lineHolder =  " " + points[xIndex] + "  " +  points[xIndex + 1] + "  " +  points[xIndex + 2] + "\n";
                    fw.append(lineHolder);

                }catch (Exception e){

                    Log.e("gDebug" , e.getMessage());
                    saveCloud = false;
                }

            } else {

                if (currentRender.oglScreen != null && pointTouched) {

                    currentPoint = new Vector3(points[xIndex], points[xIndex + 1], points[xIndex + 2]);
                    currentprojectedPoint = currentPoint.clone().project(currentRender.curreentMVP);


                    pixelCoord = convertToPixelCoord(currentprojectedPoint);



                    if (Math.abs(pixelCoord[0] - currentRender.oglScreen[0]) + Math.abs(pixelCoord[1] - currentRender.oglScreen[1]) < 50) {

                        ave+= z;
                        withinCount++;

                    }
                }else if(aveReady && Math.abs(z - ave) < 0.10){ // make the selected region white

                    mColorArray[i * 4] = 1.0f;
                    mColorArray[i * 4 + 1] = 1.0f;
                    mColorArray[i * 4 + 2] = 1.0f;
                }
            }
        }
        pointTouched = false;

        //dilate the image

        if(currentRender.detectH){

            Mat dilated = new  Mat(currentRender.getDefaultViewportHeight(), currentRender.getDefaultViewportWidth(), CvType.CV_32FC1);
            dilated.put(0, 0, currentRender.depthMap);
            dilate(dilated, dilated, getStructuringElement(CV_SHAPE_RECT, new Size(20, 20)));

            dilated.get(0, 0, currentRender.depthMap);
        }

       // currentRender.detectH = false;
        if(withinCount != 0 ){
            ave/=withinCount;
            //aveReady = true;
            Log.d("gDebug" , "Average is ready");
            aveReady = false;
        }

        if(fw != null && saveCloud) {
            try {
                fw.close();
                saveCloud = false;
                Log.d("gdebug", "File Saveddddddd........................-->" + fileIndex);
            } catch (Exception e) {

                Log.e("gDebug", e.getMessage());
            }
        }


    }

    /**
     * Update the points and colors in the point cloud.
     */
    public void updateCloud(int pointCount, FloatBuffer pointBuffer) {
        calculateColors(pointCount, pointBuffer);
        updatePoints(pointCount, pointBuffer, mColorArray); // can comment this out aswell

        if(currentRender.detectH){

            //attempt to detect where the human is

            //code to obtain region containing the human being
        //    getRegion(currentRender.depthMap, currentRender.oglScreen);

            //test to get correct

            //code to save as bitmap
            saveAsBitmap(currentRender.depthMap, currentRender.getViewportWidth(), currentRender.getViewportHeight());
            currentRender.detectH = false;
        }

    }


   public int[] convertToPixelCoord(Vector3 xyz){


            double x = (xyz.x + 1)* currentRender.getDefaultViewportWidth()/2;
            double y = (xyz.y + 1)* currentRender.getDefaultViewportHeight()/2;

            x = currentRender.getDefaultViewportWidth() -x;
            y = currentRender.getDefaultViewportHeight() - y;

            int[] xy = new int[2];

            xy[0]= (int)x;// + viewLoc[0];
            xy[1] = (int)y;// + viewLoc[1];


     return xy;

    }

    //function returns rectangle estimated to define the location of the human. This is called when point is touched
    public DetectedHuman getRegion(float[] imgArray, int[] touchedPoint){

        //start by grwing the width

        //grow to the right

        int topLeftX = touchedPoint[0];
        int topLeftY = touchedPoint[1];
        int windowSizeX = 10;
        int windowSizeY = 10;
        float diffThresh = 0.5f;

        //float average = getWindowAverage(imgArray, topLeftX, topLeftY, windowSizeX, windowSizeY);

        //up right
        while(topLeftY >=0){
            topLeftX = touchedPoint[0];
            while(topLeftX+ windowSizeX < currentRender.getDefaultViewportWidth()){

                float average = getWindowAverage(imgArray, topLeftX, topLeftY, windowSizeX, windowSizeY);
            //    Log.d("gDebug" , "aveE: " + average + "ave: " + ave + "MaxZ " + maxZ);
                if(Math.abs(average - ave) > diffThresh){

                    break;
                }

                topLeftX+=windowSizeX;

            }

            topLeftY -= windowSizeY;
        }

        //down right
        topLeftY = touchedPoint[1] + windowSizeY;
        while(topLeftY + windowSizeY < currentRender.getDefaultViewportHeight()){
            topLeftX = touchedPoint[0];
            while(topLeftX+ windowSizeX < currentRender.getDefaultViewportWidth()){

                float average = getWindowAverage(imgArray, topLeftX, topLeftY, windowSizeX, windowSizeY);
              //  Log.d("gDebug" , "aveE: " + average + "ave: " + ave + "MaxZ " + maxZ);
                if(Math.abs(average - ave) > diffThresh){

                    break;
                }

                topLeftX+=windowSizeX;

            }

            topLeftY += windowSizeY;
        }

        //up left
        topLeftY = touchedPoint[1];
        while(topLeftY >=0){
            topLeftX = touchedPoint[0] - windowSizeX;
            while(topLeftX >= 0){

                float average = getWindowAverage(imgArray, topLeftX, topLeftY, windowSizeX, windowSizeY);
               // Log.d("gDebug" , "aveE: " + average + "ave: " + ave + "MaxZ " + maxZ);
                if(Math.abs(average - ave) > diffThresh){

                    break;
                }

                topLeftX-=windowSizeX;

            }

            topLeftY -= windowSizeY;
        }

    //down left
        topLeftY = touchedPoint[1] + windowSizeY;
        while(topLeftY + windowSizeY < currentRender.getDefaultViewportHeight()){
            topLeftX = touchedPoint[0] - windowSizeX;
            while(topLeftX >= 0){

                float average = getWindowAverage(imgArray, topLeftX, topLeftY, windowSizeX, windowSizeY);
                //  Log.d("gDebug" , "aveE: " + average + "ave: " + ave + "MaxZ " + maxZ);
                if(Math.abs(average - ave) > diffThresh){

                    break;
                }

                topLeftX-=windowSizeX;

            }

            topLeftY += windowSizeY;
        }


        return  new DetectedHuman();

    }

    private float  getWindowAverage(float[] imgArray, int topLeftX, int topLeftY, int windowSizeX, int windowSizeY ){

            float average =0.0f;
            int itrCount =0 ;
            for (int i=topLeftY ; i < topLeftY+windowSizeY ; i++) {
                for (int j = topLeftX; j < topLeftX + windowSizeX ; j++) {

                        if(imgArray[i* currentRender.getDefaultViewportWidth() + j] != 0){

                            average += imgArray[i* currentRender.getDefaultViewportWidth() + j];

                            itrCount++;

                        }
                    imgArray[i* currentRender.getDefaultViewportWidth() +j] = maxZ;
                }
            }

        return itrCount!=0 ? average/itrCount : 0;
    }

    private void saveAsBitmap(float[] depthData, int width, int height){
        double mulFact = 255.0/5.0;

        try {
        int[] depthMap = new int[width*height];
        int grayPixVal = 0;

        for(int i =0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                //obtain grayscale representation
                grayPixVal = (int) (255 * (depthData[i * currentRender.getDefaultViewportWidth() + j] / maxZ));
              //  grayPixVal = (int) (mulFact * (5.0 - depthData[i][j]));

                depthMap[i*width + j] = Color.rgb(grayPixVal, grayPixVal, grayPixVal);


            }
        }

       final Bitmap Image = Bitmap.createBitmap(depthMap, width, height, Bitmap.Config.ARGB_8888);
        BufferedOutputStream bos = null;
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();

            bos = new BufferedOutputStream(new FileOutputStream(baseDir + File.separator +"frame.png"));
            Image.compress(Bitmap.CompressFormat.PNG, 90, bos);
            Image.recycle();
            if (bos != null) bos.close();
        } catch(Exception e) {
            Log.e("gDebug", e.getMessage());
        }

    }

}
