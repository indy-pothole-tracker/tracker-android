package com.indyhack.civicpotholes.service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by mike on 5/31/14.
 */
public class SensorService implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor sensor;

    private double currentX, currentY, currentZ;
    private int accuracy;

    public SensorService(Context c) {
        sensorManager = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public double getLinearZAcceleration() {
        //Log.d("civic-pothole-detection", "" + currentZ);
        return currentZ;
    }

    public void close() {
        sensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        currentX = event.values[0];
        currentY = event.values[1];
        currentZ = event.values[2];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        this.accuracy = accuracy;
    }
}
