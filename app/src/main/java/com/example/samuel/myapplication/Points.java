package com.example.samuel.myapplication;

import android.opengl.GLES10;
import android.opengl.GLES20;

import org.rajawali3d.Object3D;

import java.nio.FloatBuffer;

/**
 * Created by samuel on 7/20/16.
 */
public class Points extends Object3D {

    private int mMaxNumberOfVertices;
    public int drawingMode = GLES20.GL_POINTS;

    public Points(int numberOfPoints, boolean isCreateColors) {
        super();
        mMaxNumberOfVertices = numberOfPoints;
        init(true, isCreateColors);
    }

    // Initialize the buffers for Points primitive.
    // Since only vertex, index and color buffers are used,
    // we only initialize them using setData call.
    protected void init(boolean createVBOs, boolean createColors) {
        float[] vertices = new float[mMaxNumberOfVertices * 3];
        int[] indices = new int[mMaxNumberOfVertices];
        for (int i = 0; i < indices.length; ++i) {
            indices[i] = i;
        }
        float[] colors = null;
        if (createColors) {
            colors = new float[mMaxNumberOfVertices * 4];
        }
        setData(vertices, null, null, colors, indices, true);
    }

    // Update the geometry of the points based on the provided points float buffer.
    public void updatePoints(int pointCount, FloatBuffer pointCloudBuffer) {
        mGeometry.setNumIndices(pointCount);
        mGeometry.setVertices(pointCloudBuffer);
        mGeometry.changeBufferData(mGeometry.getVertexBufferInfo(), mGeometry.getVertices(), 0,
                pointCount * 3);
    }

    // Update the geometry of the points based on the provided points float buffer and corresponding
    // colors based on the provided float array.
    public void updatePoints(int pointCount, FloatBuffer points, float[] colors) {
        if (pointCount > mMaxNumberOfVertices) {
            throw new RuntimeException(
                    String.format("pointClount = %d exceeds maximum number of points = %d",
                            pointCount, mMaxNumberOfVertices));
        }
        mGeometry.setNumIndices(pointCount);
        mGeometry.setVertices(points);
        mGeometry.changeBufferData(mGeometry.getVertexBufferInfo(), mGeometry.getVertices(), 0,
                pointCount * 3);
        mGeometry.setColors(colors);
        mGeometry.changeBufferData(mGeometry.getColorBufferInfo(), mGeometry.getColors(), 0,
                pointCount * 4);
    }

    public void preRender() {
        super.preRender();
        setDrawingMode(drawingMode );
        GLES10.glPointSize(5.0f);
    }

}
