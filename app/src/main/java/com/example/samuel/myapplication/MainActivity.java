package com.example.samuel.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;

import com.google.atap.tango.ux.TangoUx;
import com.google.atap.tango.ux.TangoUxLayout;
import com.google.atap.tango.ux.UxExceptionEvent;
import com.google.atap.tango.ux.UxExceptionEventListener;
import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.projecttango.tangosupport.TangoPointCloudManager;
import com.projecttango.tangosupport.TangoSupport;

import org.rajawali3d.scene.ASceneFrameCallback;
import org.rajawali3d.surface.IRajawaliSurface;
import org.rajawali3d.surface.RajawaliSurfaceView;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;


public class MainActivity extends Activity {
    private Renderer mRenderer;

    //Tango items
    private Tango mTango;
    private TangoUx mTangoUX;
    private TangoConfig mTangoConfig;
    private TangoPointCloudManager mPointCloudManager;
    private AtomicBoolean tConnected = new AtomicBoolean(false);
    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false); // to track if a frame is available for renderning
    private final String gDebug = "gDebug";
    private final String TAG = "gDebug";
    private int mConnectedTextureIdGlThread = 0;//updated from glThread

    private static final ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();

    {
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //final RajawaliSurfaceView surface = new RajawaliSurfaceView(this);
        mRenderer = new Renderer(this);// by default we load model 1
        mRenderer.surface = (RajawaliSurfaceView)findViewById(R.id.surface);
        mRenderer.surface.setEGLContextClientVersion(2);
      //  mRenderer.surface.setFrameRate(60.0);
       // mRenderer.surface.setRenderMode(IRajawaliSurface.RENDERMODE_WHEN_DIRTY);



        mRenderer.surface.setSurfaceRenderer(mRenderer);

        //initialize point cloud manager
        mPointCloudManager = new TangoPointCloudManager();

        mTangoUX = setupTangoUxAndLayout();

    }

    @Override
    protected void onPause(){

        super.onPause();

        if(tConnected.compareAndSet(true, false)) {
            try {
                mTangoUX.stop();
                //disconnect Tango service so other applications can use it
                mRenderer.getCurrentScene().clearFrameCallbacks();
                mTango.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                mConnectedTextureIdGlThread=0;
                mTango.disconnect();
            } catch (TangoException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    protected void onResume(){

        super.onResume();
        //obtain the tango configuration

        if (tConnected.compareAndSet(false, true)) {
            mTangoUX.start(new TangoUx.StartParams());

            mTango = new Tango(MainActivity.this, new Runnable() {
                // Pass in a Runnable to be called from UI thread when Tango is ready,
                // this Runnable will be running on a new thread.
                // When Tango is ready, we can call Tango functions safely here only
                // when there is no UI thread changes involved.
                @Override
                public void run() {
                    try {
                        TangoSupport.initialize();
                        setTango();
                        connectRenderer();
                    } catch (TangoOutOfDateException tE) {

                        tE.printStackTrace();
                    }
                }
            });
        }

    }

    private void setTango(){

                mTangoConfig = mTango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
                mTangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true); //activate depth sensing
                mTangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
                mTangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);// need to watch.. claim to provide lower latency estimate

                mTango.connect(mTangoConfig);

                mTango.connectListener(framePairs, new Tango.OnTangoUpdateListener() {
                    @Override
                    public void onPoseAvailable(TangoPoseData tangoPoseData) {
                        if (mTangoUX != null) {
                            mTangoUX.updatePoseStatus(tangoPoseData.statusCode);
                        }
                    }

                    @Override
                    public void onXyzIjAvailable(TangoXyzIjData pointCloud) {

                        if (mTangoUX != null) {
                            mTangoUX.updateXyzCount(pointCloud.xyzCount);
                        }

                        mPointCloudManager.updateXyzIj(pointCloud);

                    }

                    @Override
                    public void onFrameAvailable(int cameraId) {

                        // Check if the frame available is for the camera we want and update its frame
                        // on the view.
                             Log.d("gDebug" , "frame update");
                        if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {
                            Log.d("gDebug" , "frame update");
                            // Mark a camera frame is available for rendering in the OpenGL thread
                            mIsFrameAvailableTangoThread.set(true);
                            // Trigger an Rajawali render to update the scene with the new RGB data.
                            mRenderer.surface.requestRender();
                        }
                    }

                    @Override
                    public void onTangoEvent(TangoEvent tangoEvent) {
                        if (mTangoUX != null) {
                            mTangoUX.updateTangoEvent(tangoEvent);
                        }
                    }
                });



    }

    public void connectRenderer(){

            mRenderer.getCurrentScene().registerFrameCallback(new ASceneFrameCallback() {
                @Override
                public void onPreFrame(long sceneTime, double deltaTime) {

                    synchronized (MainActivity.this) {

                        if (!tConnected.get()) {
                            return;
                        }

                        if (mConnectedTextureIdGlThread != mRenderer.getTextureId()) {
                            mTango.connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                    mRenderer.getTextureId());
                            mConnectedTextureIdGlThread = mRenderer.getTextureId();
                            Log.d(TAG, "connected to texture id: " + mRenderer.getTextureId());
                        }


                        // If there is a new RGB camera frame available, update the texture with it
                        if (mIsFrameAvailableTangoThread.compareAndSet(true, false)) {
                           // mRgbTimestampGlThread = //used to monitor the frame, so we update camera pose on new frame
                                    mTango.updateTexture(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                        }

                        TangoXyzIjData pointCloud = mPointCloudManager.getLatestXyzIj();

                        if (pointCloud != null) {

                            //get transformation from depth camera frame to Opengl frame. Needs so we can represents the points appropriately in OpenGL
                            TangoSupport.TangoMatrixTransformData mTransform = TangoSupport.getMatrixTransformAtTime(
                                    pointCloud.timestamp,
                                    TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                    TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                                    TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                    TangoSupport.TANGO_SUPPORT_ENGINE_TANGO
                            );
                            if (mTransform.statusCode == TangoPoseData.POSE_VALID) {

                                mRenderer.updatePointCloud(pointCloud, mTransform.matrix);

                            }

                        }

                        // Update current camera pose
                        try {
                            // Calculate the last camera color pose. Using the color pose helps align out obtained points with the color camera
                            TangoPoseData lastFramePose = TangoSupport.getPoseAtTime(0,
                                    TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                    TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                                    TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL, 0);
                            mRenderer.updateCameraPose(lastFramePose); // used to update the virtual camera

                        } catch (TangoErrorException e) {
                            Log.e(gDebug, "Could not get valid transform");
                        }
                    }
                }

                @Override
                public void onPreDraw(long sceneTime, double deltaTime) {

                }

                @Override
                public void onPostFrame(long sceneTime, double deltaTime) {


                }

                @Override //use this to ensure the preFrame callback is registered
                public boolean callPreFrame() {
                    return true;
                }
            });




    }

    public boolean onTouchEvent(MotionEvent event) {


        mRenderer.logTouch(event);

        return true;
    }


    private TangoUx setupTangoUxAndLayout() {
        TangoUxLayout uxLayout = (TangoUxLayout) findViewById(R.id.layout_tango);
        TangoUx tangoUx = new TangoUx(this);
        tangoUx.setLayout(uxLayout);
        tangoUx.setUxExceptionEventListener(mUxExceptionListener);
        return tangoUx;
    }


    private UxExceptionEventListener mUxExceptionListener = new UxExceptionEventListener() {

        @Override
        public void onUxExceptionEvent(UxExceptionEvent uxExceptionEvent) {
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_LYING_ON_SURFACE) {
                Log.i(TAG, "Device lying on surface ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FEW_DEPTH_POINTS) {
                Log.i(TAG, "Very few depth points in mPoint cloud ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FEW_FEATURES) {
                Log.i(TAG, "Invalid poses in MotionTracking ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_INCOMPATIBLE_VM) {
                Log.i(TAG, "Device not running on ART");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_MOTION_TRACK_INVALID) {
                Log.i(TAG, "Invalid poses in MotionTracking ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_MOVING_TOO_FAST) {
                Log.i(TAG, "Invalid poses in MotionTracking ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_OVER_EXPOSED) {
                Log.i(TAG, "Camera Over Exposed");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_TANGO_SERVICE_NOT_RESPONDING) {
                Log.i(TAG, "TangoService is not responding ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_UNDER_EXPOSED) {
                Log.i(TAG, "Camera Under Exposed ");
            }

        }
    };


}
