package com.indyhack.civicpotholes.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.indyhack.civicpotholes.MainActivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by mike on 5/31/14.
 */
public class PotholeDetectionService {

    public interface OnPotholeDetectedListener {
        public void onPotholeDetected();
    }


    private Context c;
    private OnPotholeDetectedListener listener;
    private List<Double> linearAccelerationValues;

    private boolean run = true;

    public PotholeDetectionService(Context c, OnPotholeDetectedListener listener) {
        this.c = c;
        this.listener = listener;
        linearAccelerationValues = Collections.synchronizedList(new ArrayList<Double>());
    }

    public void addLinearAccelerationValue(double value) {
        // Add the value to the list of values
        if (linearAccelerationValues.size() > 1000) {
            linearAccelerationValues.remove(0);
        }
        linearAccelerationValues.add(value);

    }
    
    public void start() {

        final SensorService service = new SensorService(c);
        new Thread(new Runnable() {
            public void run() {
                while (true) {

                    // Update the data with a new reading
                    double z = service.getLinearZAcceleration();
                    addLinearAccelerationValue(z);

                    // Wait a bit of time
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                    }

                    analyzeData();

                    while (!run) { try { Thread.sleep(1000); } catch (InterruptedException e) {} }

                }
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    SharedPreferences prefs = c.getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0);
                    boolean isEnabled = prefs.getBoolean(MainActivity.PREF_ENABLE_POTHOLE_DETECTION, true);
                    if (isEnabled) {
                        run = true;
                    } else {
                        run = false;
                    }

                    try { Thread.sleep(1000); } catch (InterruptedException e) {}
                }
            }
        }).start();
    }

    private boolean analyzeData() {

        // We log like 1000 points of data at a time.
        // Through testing, we've determined that the upward tick of a pothole hit takes
        // roughly 30 ms. Then the downward tick takes ~50ms.

        // Take a partial derivative of every piece of 3 points of data, looking for significant
        // increases in linear acceleration

        int N_INCREASE = 2;
        double D_INCREASE_THRESH = 2;
        int N_DECREASE = 2;
        double D_DECREASE_THRESH = -2;


        boolean sigIncreaseFound = false;
        int at = -1;
        for (int i = 0; i < linearAccelerationValues.size()-N_INCREASE; i+=N_INCREASE) {
            double start = linearAccelerationValues.get(i);
            double end = linearAccelerationValues.get(i+N_INCREASE);
            double d = (end - start) / (N_INCREASE+1);

            if (d > D_INCREASE_THRESH) {
                sigIncreaseFound = true;
                at = i;
                break;
            }
        }

        // If we found a significant increase, next look for a significant decrease
        // which takes place over around 3 points of data.

        boolean sigDecreaseFound = false;
        if (sigIncreaseFound) {
            for (int i = at; i < linearAccelerationValues.size() - N_DECREASE; i += N_DECREASE) {
                double start = linearAccelerationValues.get(i);
                double end = linearAccelerationValues.get(i + N_DECREASE);
                double d = (end - start) / (N_DECREASE+1);

                if (d < D_DECREASE_THRESH) {
                    sigDecreaseFound = true;
                    break;
                }
            }
        }

        // If we found both, register a pothole as hit
        if (sigIncreaseFound && sigDecreaseFound) {
            registerHit();
            return true;
        }

        return false;
    }

    private void fullPurge() {
        linearAccelerationValues = Collections.synchronizedList(new LinkedList<Double>());
    }

    private void registerHit() {
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            public void run() {
                listener.onPotholeDetected();
            }
        });
        fullPurge();
        try {Thread.sleep(1000); } catch (InterruptedException e) {}
        Log.d("civic-pothole-detection", "Continuing pothole detection");
    }

}
