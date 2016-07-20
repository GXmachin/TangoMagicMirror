package com.example.samuel.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;

import org.rajawali3d.Object3D;
import org.rajawali3d.animation.Animation;
import org.rajawali3d.animation.Animation3D;
import org.rajawali3d.animation.EllipticalOrbitAnimation3D;
import org.rajawali3d.animation.RotateOnAxisAnimation;
import org.rajawali3d.animation.TranslateAnimation3D;
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
import org.rajawali3d.materials.textures.CubeMapTexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Cube;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.RajawaliRenderer;
import org.rajawali3d.surface.RajawaliSurfaceView;

import java.nio.FloatBuffer;
import java.text.ParseException;
import java.util.Random;

/**
 * Created by samuel on 8/18/2015.
 */

public class Renderer extends RajawaliRenderer implements SensorEventListener{
    private DirectionalLight mLight;
    private Object3D mObjectGroup;
    private Animation3D mCameraAnim, mLightAnim;
    private Cube mySphere;
    public static int sensitivity = 25 ;
    float rotationVec[]={0,0,0};
    Material faceMaterial;
    private LoaderOBJ objParser = null;
    private LoaderOBJ objParser2 = null;
    public RajawaliSurfaceView surface;

    public Renderer(Context context ) {
        super(context);
        this.mContext = context;
        setFrameRate(60);

    }

    @Override
    protected void initScene() {

        mLight = new DirectionalLight(1.0f, 0.2f, -1.0f);
        mLight.setPosition(1, 2, 0);
        mLight.setColor(1.0f, 1.0f, 1.0f);
        mLight.setPower(2);

        faceMaterial = new Material();
        faceMaterial.enableLighting(true);
        faceMaterial.setColor(Color.GREEN);
        faceMaterial.setDiffuseMethod(new DiffuseMethod.Lambert());


        mySphere = new Cube(1);
        mySphere.setMaterial(faceMaterial);

        getCurrentScene().addLight(mLight);
        getCurrentCamera().enableLookAt();
        getCurrentCamera().setFarPlane(10000);
        getCurrentCamera().setNearPlane(0.2);
        // getCurrentCamera().setFieldOfView(60);
       // getCurrentCamera().setZ(1000.0);
      //  getCurrentCamera().setX(0.0);
       // getCurrentCamera().setY(0.0);
      //  getCurrentCamera().setUpAxis(0.0f, 1.0f, 0.0f);
        //face 1 up is y , z was ,uch less

        getCurrentScene().addChild(mySphere);
        getCurrentCamera().setLookAt(0.0, 0.0, 0.0);

//Change R.raw.XXX to R.raw.name_of_new_obj
 /*       objParser = new LoaderOBJ(mContext.getResources(),
                mTextureManager, R.raw.goblin_obj);

        try {
            objParser.parse();
            mObjectGroup = objParser.getParsedObject();
            mObjectGroup.setMaterial(faceMaterial);

            getCurrentScene().addChild(mObjectGroup);

        } catch (Exception e) {
            e.printStackTrace();
        }
*/
    }

    @Override
    public void onOffsetsChanged(float v, float v1, float v2, float v3, int i, int i1) {

    }

    @Override
    public void onTouchEvent(MotionEvent motionEvent) {


    }

    public void onRender(final long elapsedTime, final double deltaTime) {

        super.onRender(elapsedTime, deltaTime);
       

        FloatBuffer depthBuff = FloatBuffer.allocate(100);



    }


    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void setRotation(float[] rotationAngles){


        rotationVec[0] = sensitivity*rotationAngles[0];
        rotationVec[1] = sensitivity*rotationAngles[1];
        rotationVec[2] = sensitivity*rotationAngles[2];
    }


    public boolean logTouch(MotionEvent event) {
        int x = (int)event.getX();
        int y = (int)event.getY();


        if(event.getAction() ==  MotionEvent.ACTION_DOWN) {
            int[] oglScreen = getInOpenGLScreenCoord(x,y);

            Vector3 unProjectedN = unProject((double) oglScreen[0], (double) oglScreen[1], 0);
            Vector3 unProjectedF = unProject((double)oglScreen[0] , (double)oglScreen[1], 1 );
            Vector3 vectDiff = unProjectedF.subtract(unProjectedN);
            double alpha = (0.5 - unProjectedN.z) / (vectDiff.z);
            Log.d("gDebug", "x :  " + x + " y: " + y + "view openGL: " + oglScreen[0] + " :" + oglScreen[1]);
            Log.d("gDebug", "ux :  " + unProjectedN.x + " uY: " + unProjectedN.y + " uZ: " + unProjectedN.z +  " fX" + unProjectedF.x + " :" + unProjectedF.y +" fZ: " + unProjectedF.z + "currentZ: " + getCurrentCamera().getZ());

            Vector3 eV = unProjectedN.add(vectDiff.multiply(alpha));
            Log.d("gDebug :" , "eX: " + eV.x + " eY: " + eV.y + " eZ: " + eV.z);
        }
        return true;
    }

    int[] getInOpenGLScreenCoord(int x, int y){


        int[] viewLoc = new int[2];
        surface.getLocationOnScreen(viewLoc);

        Log.d("gDebug" , " " + viewLoc[0] + " :"  +viewLoc[1]);
        return new int[]{x-viewLoc[0], y-viewLoc[1]};


    }

}
