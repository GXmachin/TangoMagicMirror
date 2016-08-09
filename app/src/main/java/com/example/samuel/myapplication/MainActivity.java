package com.example.samuel.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.opengl.GLES20;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.vision.Frame;
import com.google.atap.tango.TangoJNINative;
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

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.HOGDescriptor;
import org.rajawali3d.scene.ASceneFrameCallback;
import org.rajawali3d.surface.IRajawaliSurface;
import org.rajawali3d.surface.RajawaliSurfaceView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;



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
    private Button saveData;
    private Button saveFrame;
    private Button saveDepth;
    public ByteBuffer mPixelBUffer;
    String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
    private int rectTopLextX, rectTopLeftY, rectWidth, rectHeight;


    //set up/initialize opencv
    private final String debugTag = "gDebug";

    private HOGDescriptor hog;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status){
                case BaseLoaderCallback.SUCCESS:
                {
                    Log.d(debugTag, "OpenCV initialized");
                    try {

                        Log.d(debugTag ,   "Here ----------->");
                        //load the cascade file from where you saved it, I put mine in my sd card root

                        String cascadeFileName = baseDir + File.separator + "haarcascade_fullbody.xml";
                        File file = new File(cascadeFileName);
                        Log.d(debugTag, " " + file.isAbsolute());

                        //initialize the cascade classifier

                        if (true  ) {
                            Log.e(debugTag, "Failed to load cascade classifier");

                            hog = new HOGDescriptor(new Size(new Point(64, 128)), new Size(new Point(16, 16)), new Size(new Point(8, 8)),
                                    new Size(new Point(8, 8)), 9);

                            hog.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());

                        } else
                            Log.i(debugTag, "Loaded cascade classifier from " + file.getAbsolutePath());



                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(debugTag, "Failed to load cascade. Exception thrown: " + e);
                    }


                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;

            }
            //super.onManagerConnected(status);
        }
    };

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
        mRenderer.surfaceHolder = mRenderer.surface.getHolder();


        //get the view used to carry the  bounding rect
        mRenderer.boundingRect = (DrawView)findViewById(R.id.drawView);
         //initialize to see
        mRenderer.boundingRect.setViewRect(10, 10, 100, 200);


        mRenderer.surface.setSurfaceRenderer(mRenderer);

        saveData = (Button)findViewById(R.id.savData);

        saveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRenderer.setSaveCloud();
            }
        });

        saveFrame = (Button)findViewById(R.id.saveFrame);

        saveFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            mRenderer.tryDrawing(mRenderer.surface.getHolder());
                //attempt to save frame

             /*
                //alocate buffer
                mPixelBUffer = ByteBuffer.allocate(mRenderer.surface.getWidth()*mRenderer.surface.getHeight()*4);
                Log.d("gDebug   -=------" , " " + mRenderer.surface.getWidth()*mRenderer.surface.getHeight()*4);
                mPixelBUffer.order(ByteOrder.LITTLE_ENDIAN);

                mPixelBUffer.rewind();
                GLES20.glReadPixels(0, 0, mRenderer.surface.getWidth(), mRenderer.surface.getHeight(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                        mPixelBUffer);
                BufferedOutputStream bos = null;
                try {
                    bos = new BufferedOutputStream(new FileOutputStream(baseDir + File.separator +"frame.png"));
                    Bitmap bmp = Bitmap.createBitmap(mRenderer.surface.getWidth(), mRenderer.surface.getHeight(), Bitmap.Config.ARGB_8888);
                    mPixelBUffer.rewind();
                    bmp.copyPixelsFromBuffer(mPixelBUffer);
                    bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
                    bmp.recycle();
                    if (bos != null) bos.close();
                } catch(Exception e) {
                    Log.e(debugTag, e.getMessage());
                }
                Log.d(debugTag, "saved");
                */
            }
        });

//initialize save depth button
        saveDepth = (Button)findViewById(R.id.setSaveDepth);

        saveDepth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRenderer.detectH = true;
            }
        });

        //initialize point cloud manager
        mPointCloudManager = new TangoPointCloudManager();

        mTangoUX = setupTangoUxAndLayout();

        mRenderer.surface.setDrawingCacheEnabled(true);

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
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
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

                        if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {

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



                    if(mRenderer.detectH){

                        try {
                            String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
                           // View v1 = mRenderer.surface.setDrawingCacheEnabled(true);   //getWindow().getDecorView().getRootView();

                            mRenderer.createBitmapFromGLSurface(0,0,mRenderer.getDefaultViewportWidth(), mRenderer.getDefaultViewportHeight());
                           // v1.setDrawingCacheEnabled(true);
                            //Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
                            Bitmap bitmap =  mRenderer.createBitmapFromGLSurface(0, 0, mRenderer.getDefaultViewportWidth(), mRenderer.getDefaultViewportHeight()); //Bitmap.createBitmap(mRenderer.surface.getDrawingCache());

                            Log.d("gDebug", "  " + bitmap.getHeight());

                           // mRenderer.surface.setDrawingCacheEnabled(false);
                           // v1.setDrawingCacheEnabled(false);

                          //  FileOutputStream screen = new FileOutputStream(baseDir + File.separator + "frame2.png");

                          //  bitmap.compress(Bitmap.CompressFormat.PNG, 100, screen);

                            //Create face Detector
                            Context context = getApplicationContext();
                         FaceDetector detector = new FaceDetector.Builder(context)
                                    .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                                    .build();

                            //get the bitmap as frame

                             Frame tFrame = new Frame.Builder().setBitmap(bitmap).build();

                            SparseArray<Face> faces = detector.detect(tFrame);

                            if(faces!=null && faces.size()>0) {
                                Face face1 = faces.valueAt(0);

                                Log.d("gDebug", " -------------_> " + face1.getPosition().toString());
                                rectTopLextX = (int) face1.getPosition().x;
                                rectTopLeftY = (int) face1.getPosition().y;
                                rectWidth =  (int) (face1.getWidth());
                                rectHeight =  (int) ( 7.5 * face1.getHeight());

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        mRenderer.boundingRect.setViewRect(rectTopLextX, rectTopLeftY, rectWidth, rectHeight);

                                    }
                                });




                            }


                         //   screen.flush();
                         //   screen.close();

                            bitmap.recycle();

                        }catch(Exception e){

                            Log.e("gDebug" , e.getMessage());
                            e.printStackTrace();

                        }

                        mRenderer.detectH = false;
                    }

                }

                @Override //use this to ensure the preFrame callback is registered
                public boolean callPreFrame() {
                    return true;
                }

                @Override //use this to ensure the preFrame callback is registered
                public boolean callPostFrame() {
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
