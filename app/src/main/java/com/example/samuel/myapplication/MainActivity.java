package com.example.samuel.myapplication;

import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.rajawali3d.surface.IRajawaliSurface;
import org.rajawali3d.surface.RajawaliSurfaceView;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;


public class MainActivity extends ActionBarActivity implements SensorEventListener {
    private Renderer mRenderer;
    private SensorManager mySensorManager;
    private Sensor myRotationSensor;
    private float mGravity[] = {0,0,0};

    private final float ALPHA = 0.8f;
    private final int SENSITIVITY = 5;
    private SeekBar sensitivity = null;
    private TextView mTextView = null;
    private Properties myProp = null;
    private int progressChanged;
    private String myPreferenceFile = "sensitivity";
    private SharedPreferences sensitivityPreference = null;
    private Spinner myDropDown = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //final RajawaliSurfaceView surface = new RajawaliSurfaceView(this);
        mRenderer = new Renderer(this);// by default we load model 1
        mRenderer.surface = (RajawaliSurfaceView)findViewById(R.id.surface);
        mRenderer.surface.setFrameRate(60.0);
        mRenderer.surface.setRenderMode(IRajawaliSurface.RENDERMODE_WHEN_DIRTY);

/*
        //Obtain Persisted Sensitivity
        sensitivityPreference = getSharedPreferences(myPreferenceFile, MODE_PRIVATE);

        Renderer.sensitivity = sensitivityPreference.getInt("sensitivity", 25);


        //End
       // addContentView(surface, new ActionBar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT));
        mTextView = (TextView)findViewById(R.id.textView);
        mTextView.setText("" + Renderer.sensitivity);

        //sensitivity = (SeekBar)findViewById(R.id.seekBar);
       // sensitivity.setProgress(Renderer.sensitivity); // this should be whatever value is in the assets folder

        sensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {


            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                mTextView.setText("" + progress);
                Log.d("gDebug", "Progress  " + progress);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                Renderer.sensitivity = progressChanged;
                SharedPreferences.Editor mEditor = sensitivityPreference.edit();
                mEditor.putInt("sensitivity", Renderer.sensitivity);
                mEditor.commit();
                Log.d("gDebug", "Progress  " + "Gesture finished");

            }
        });*/


        mRenderer.surface.setSurfaceRenderer(mRenderer);

        try{

            mySensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
            myRotationSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            mySensorManager.registerListener(this, myRotationSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }catch(Exception e){

            Log.d("gDebug", "issue with sensor");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }



    @Override
    public void onSensorChanged(SensorEvent event) {

        if(event.sensor == myRotationSensor){

            mRenderer.setRotation(event.values);

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public boolean onTouchEvent(MotionEvent event) {


        mRenderer.logTouch(event);

        return true;
    }


}
