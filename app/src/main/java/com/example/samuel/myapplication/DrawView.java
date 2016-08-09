package com.example.samuel.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by samuel on 7/27/16.
 */
public class DrawView extends View {

    private Rect rectangle;
    private Paint paint;

    int x = 50;
    int y = 50;
    int width = 0;
    int height = 0;


    public DrawView(Context context, AttributeSet attrs){

        super(context, attrs);
        renderview();
    }

    public DrawView(Context context) {
        super(context);
        renderview();

    }

    private void renderview(){


        // create a rectangle that we'll draw later
        rectangle = new Rect(x, y, x+width, y+height);

        // create the Paint and set its color
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
       // canvas.drawColor(Color.BLUE);
        canvas.drawRect(rectangle, paint);

        Log.d("gDebug" , "I am always Drawn!!!!");
    }

    public void setViewRect(int xVal, int yVal , int wVal, int hVal){

            x = xVal;
            y = yVal;
            width = wVal;
            height = hVal;

            rectangle.set(x,y,x+width, y+height);
            this.invalidate();
    }
}
