package com.example.samuel.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.google.android.gms.vision.text.Line;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import org.opencv.core.Mat;
import org.rajawali3d.Object3D;
import org.rajawali3d.animation.Animation;
import org.rajawali3d.animation.Animation3D;
import org.rajawali3d.animation.EllipticalOrbitAnimation3D;
import org.rajawali3d.animation.RotateOnAxisAnimation;
import org.rajawali3d.animation.TranslateAnimation3D;
import org.rajawali3d.bounds.BoundingBox;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.lights.PointLight;
import org.rajawali3d.loader.LoaderAWD;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.loader.ParsingException;
import org.rajawali3d.loader.fbx.LoaderFBX;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.methods.SpecularMethod;
import org.rajawali3d.materials.plugins.FogMaterialPlugin;
import org.rajawali3d.materials.textures.ASingleTexture;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.CubeMapTexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.Matrix;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Cube;
import org.rajawali3d.primitives.Line3D;
import org.rajawali3d.primitives.RectangularPrism;
import org.rajawali3d.primitives.ScreenQuad;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.RajawaliRenderer;
import org.rajawali3d.surface.RajawaliSurfaceView;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.text.ParseException;
import java.util.Random;
import java.util.Stack;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by samuel on 8/18/2015.
 */

public class Renderer extends RajawaliRenderer implements SensorEventListener {
    private DirectionalLight mLight;
    private Object3D mObjectGroup;
    private Animation3D mCameraAnim, mLightAnim;
    private Cube mySphere;
    public static int sensitivity = 25;
    float rotationVec[] = {0, 0, 0};
    Material faceMaterial;
    private LoaderOBJ objParser = null;
    public RajawaliSurfaceView surface;
    public int[] oglScreen;
    private Stack<Vector3> lines = new Stack();

    // for the camera stream
    public ATexture mTangoCameraTexture;

    private static final float CAMERA_NEAR = 0.01f;
    private static final float CAMERA_FAR = 200f;
    private static final int MAX_NUMBER_OF_POINTS = 60000;
    private PointCloud mPointCloud;
    public Matrix4 curreentMVP = null;
    public SurfaceHolder surfaceHolder = null;
    public float[] depthMap;
    public boolean detectH = false;
    private boolean rectSet = false;

    public DrawView boundingRect;

    public Mat imgData;

    public Renderer(Context context) {
        super(context);
        this.mContext = context;
        setFrameRate(60);

    }

    @Override
    protected void initScene() {

        //set up paremeters to use color camera
        ScreenQuad backgroundQuad = new ScreenQuad();
        Material tangoCameraMaterial = new Material();
        tangoCameraMaterial.setColorInfluence(0);




        // We need to use Rajawali's {@code StreamingTexture} since it sets up the texture
        // for GL_TEXTURE_EXTERNAL_OES rendering
        mTangoCameraTexture =
                new StreamingTexture("camera", (StreamingTexture.ISurfaceListener) null);

        try {
            tangoCameraMaterial.addTexture(mTangoCameraTexture);
            backgroundQuad.setMaterial(tangoCameraMaterial);
        } catch (ATexture.TextureException e) {
            Log.e("gDebug", "Exception creating texture for RGB camera contents", e);
        }

        getCurrentScene().addChildAt(backgroundQuad, 0);

        // Add a directional light in an arbitrary direction.
        DirectionalLight light = new DirectionalLight(1, 0.2, -1);
        light.setColor(1, 1, 1);
        light.setPower(0.8f);
        light.setPosition(3, 2, 4);
        getCurrentScene().addLight(light);




       //point cloud setup
        mPointCloud = new PointCloud(MAX_NUMBER_OF_POINTS);
        mPointCloud.currentRender = this;
       // getCurrentScene().addChild(mPointCloud);
        getCurrentScene().setBackgroundColor(Color.WHITE);

        getCurrentCamera().setFarPlane(CAMERA_FAR);
        getCurrentCamera().setNearPlane(CAMERA_NEAR);
        getCurrentCamera().setFieldOfView(37.5f); // data obtained from pointcloud sample


        //initialize the array used for the depth map
        depthMap = new float[getDefaultViewportHeight() * getDefaultViewportWidth()];


        //load the model into the system
        objParser = new LoaderOBJ(mContext.getResources(),
                mTextureManager, R.raw.goblin_obj);

        faceMaterial = new Material();
        faceMaterial.enableLighting(true);
        faceMaterial.setColor(Color.GREEN);
        faceMaterial.setDiffuseMethod(new DiffuseMethod.Lambert());

        try {
            objParser.parse();
            mObjectGroup = objParser.getParsedObject();
            mObjectGroup.setMaterial(faceMaterial);

            mObjectGroup.setScale(0.0005);
       //     mObjectGroup.setPosition(0,0,3);

            getCurrentScene().addChild(mObjectGroup);

        } catch (Exception e) {
            e.printStackTrace();
        }

            //add point cloud for debug
         getCurrentScene().addChild(mPointCloud);


    }

    @Override
    public void onOffsetsChanged(float v, float v1, float v2, float v3, int i, int i1) {

    }

    @Override
    public void onTouchEvent(MotionEvent motionEvent) {


    }

    public void onRender(final long elapsedTime, final double deltaTime) {

        super.onRender(elapsedTime, deltaTime);

       Log.d("gDebug", mObjectGroup.getPosition().toString());


    }


    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    public Vector3 unProjectG(double x, double y, double z) {
        x = mDefaultViewportWidth - x;
        y = mDefaultViewportHeight - y;

        final double[] in = new double[4], out = new double[4];

        Matrix4 MVPMatrix = getCurrentCamera().getProjectionMatrix().clone();
        MVPMatrix.inverse();

        in[0] = (x / mDefaultViewportWidth) * 2 - 1;
        in[1] = (y / mDefaultViewportHeight) * 2 - 1;
        in[2] = 2 * z - 1;
        in[3] = 1;

        Matrix.multiplyMV(out, 0, MVPMatrix.getDoubleValues(), 0, in, 0);

        if (out[3] == 0)
            return null;

        out[3] = 1 / out[3];
        return new Vector3(out[0] * out[3], out[1] * out[3], out[2] * out[3]);
    }

    public boolean logTouch(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();


        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            oglScreen = getInOpenGLScreenCoord(x, y);


         /*   Log.d("gDebug", "Fov: " + getCurrentCamera().getFieldOfView());
            Vector3 unProjectedN = unProjectG((double) oglScreen[0], (double) oglScreen[1], 0);
            Vector3 unProjectedF = unProjectG((double) oglScreen[0], (double) oglScreen[1], 1);
            Vector3 rayDirection = unProjectedF.subtract(unProjectedN);
            mPointCloud.rayStart = unProjectedN;
            mPointCloud.rayDirection = rayDirection;

            */
         //   double alpha = (0.5 - unProjectedN.z) / (vectDiff.z);
         //   Log.d("gDebug", "x :  " + x + " y: " + y + "view openGL: " + oglScreen[0] + " :" + oglScreen[1]);
         //   Log.d("gDebug", "ux :  " + unProjectedN.x + " uY: " + unProjectedN.y + " uZ: " + unProjectedN.z + " fX" + unProjectedF.x + " :" + unProjectedF.y + " fZ: " + unProjectedF.z + "currentZ: " + getCurrentCamera().getZ());

         //   Vector3 eV = unProjectedN.add(vectDiff.multiply(alpha));
         //   Log.d("gDebug :", "eX: " + eV.x + " eY: " + eV.y + " eZ: " + eV.z);

        mPointCloud.pointTouched = true;
        detectH = true;
        mPointCloud.aveReady = false;
    }

        //point touched..
        if(mObjectGroup != null){
            mObjectGroup.setPosition(0,0,mPointCloud.ave);
        }

        return true;
    }

    int[] getInOpenGLScreenCoord(int x, int y) {


        int[] viewLoc = new int[2];
        surface.getLocationOnScreen(viewLoc);

       // Log.d("gDebug", " " + viewLoc[0] + " :" + viewLoc[1]);
        return new int[]{x - viewLoc[0], y - viewLoc[1]};


    }


    public void updatePointCloud(TangoXyzIjData xyzIjData, float[] openGlTdepth) {
        mPointCloud.updateCloud(xyzIjData.xyzCount, xyzIjData.xyz);
        Matrix4 openGlTdepthMatrix = new Matrix4(openGlTdepth);


        mPointCloud.setPosition(openGlTdepthMatrix.getTranslation());

        // Conjugating the Quaternion is need because Rajawali uses left handed convention.
        mPointCloud.setOrientation(new Quaternion().fromMatrix(openGlTdepthMatrix).conjugate());


    }


    //Now that we have this points, it is important we render them appropriately. Our camera orientation is irrespetive of where the points are
    public void updateCameraPose(TangoPoseData cameraPose){

        float[] rotation = cameraPose.getRotationAsFloats();
        float[] translation = cameraPose.getTranslationAsFloats();

        Quaternion quaternion = new Quaternion(rotation[3], rotation[0], rotation[1], rotation[2]);
        Vector3 position  = new Vector3(translation[0], translation[1], translation[2]);
        getCurrentCamera().setPosition(position);

        //changed to conjugate as its not like that in the sample code. Perhaps the different version of Rajawli used is the cause
        getCurrentCamera().setOrientation(quaternion.conjugate());


        //update the camera matrix
        curreentMVP = getCurrentCamera().getProjectionMatrix().clone();
      //  curreentMVP.multiply(getCurrentCamera().getViewMatrix().clone());

    }


    public int getTextureId() {
        return mTangoCameraTexture == null ? -1 : mTangoCameraTexture.getTextureId();
    }

    public void setSaveCloud(){

        mPointCloud.saveCloud = true;

    }


    public void tryDrawing(SurfaceHolder holder) {
        Log.i("gDebug", "Trying to draw...");

        Canvas canvas = holder.lockCanvas();
        if (canvas == null) {
            Log.e("gDebug", "Cannot draw onto the canvas as it's null");
        } else {
            drawMyStuff(canvas);
            holder.unlockCanvasAndPost(canvas);
        }
    }


    private void drawMyStuff(final Canvas canvas) {
        Random random = new Random();
        Log.i("gDebug", "Drawing...");
        canvas.drawRGB(255, 128, 128);
    }


    public Bitmap createBitmapFromGLSurface(int x, int y, int w, int h)
            throws OutOfMemoryError {
        int bitmapBuffer[] = new int[w * h];
        int bitmapSource[] = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);

        try {
            GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, intBuffer);
            int offset1, offset2;
            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (GLException e) {
            return null;
        }

        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
    }


}
