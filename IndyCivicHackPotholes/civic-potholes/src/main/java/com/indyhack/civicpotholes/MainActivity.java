package com.indyhack.civicpotholes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.indyhack.civicpotholes.service.PotholeDetectionService;
import com.indyhack.civicpotholes.task.AddPotholeTask;


public class MainActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        MapActivity {


    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private GoogleMap mMap;
    private LocationClient mLocationClient;
    private PopulateMapTask mPopulaterTask;

    public static SharedPreferences prefs;
    public static final String SHARED_PREFS_NAME = "civic_hack_potholes_prefs";
    public static final String PREF_ENABLE_POTHOLE_DETECTION = "enable_pothole_detection";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(SHARED_PREFS_NAME, 0);

        final Bitmap notificationIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

        final Context c = getBaseContext();
        PotholeDetectionService service = new PotholeDetectionService(c, new PotholeDetectionService.OnPotholeDetectedListener() {
            public void onPotholeDetected() {
                Toast.makeText(getApplicationContext(), "Pothole detected.", Toast.LENGTH_SHORT).show();
                Log.d("civic-pothole-detection", "Pothole detected");

                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(getApplicationContext())
                                .setSmallIcon(R.drawable.pothole)
                                .setLargeIcon(notificationIcon)
                                .setContentTitle("Pothole Detected")
                                .setContentText("A pothole hole was detected and has been reported.");
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(0, mBuilder.build());

                new AddPotholeTask(mLocationClient.getLastLocation().getLatitude(), mLocationClient.getLastLocation().getLongitude()).execute();
            }
        });
        service.start();

        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapView)).getMap();
        mMap.setMyLocationEnabled(true);

        mPopulaterTask = new PopulateMapTask(MainActivity.this);
        mPopulaterTask.execute();

        mLocationClient = new LocationClient(this, this, this);
    }

    @Override
    public void onConnected(Bundle bun) {
        Log.d("MainActivity", "The mLocationHandler has been connected!");

            Log.v("MainActivity","Zooming camera!!!");
            if(mLocationClient == null || mMap == null) {
                Log.e("LocationClient", (mLocationClient == null ? "mLocationClient" : "mMap") + " is null!");
                return;
            }

            CameraPosition.Builder cameraPositionBuilder = new CameraPosition.Builder();
	        /*if(mLocationClient.getLastLocation() == null) {
	            Log.e("LocationClient", "Last location is null!");
	            mLocationClient.disconnect();
	            return;
	        }*/
            cameraPositionBuilder.target(new LatLng(mLocationClient
                    .getLastLocation().getLatitude(), mLocationClient
                    .getLastLocation().getLongitude()));
            cameraPositionBuilder.zoom((float) 12);
            mMap.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPositionBuilder.build()), new GoogleMap.CancelableCallback() {
                @Override
                public void onCancel()
                {

                }

                @Override
                public void onFinish()
                {
                    //Toast.makeText(MainActivity.this, "Finished zoom!", Toast.LENGTH_LONG).show();
                }
            });
    }

    /**
     * Called when we fail to connect to the mLocationClient.
     */
    @Override
    public void onConnectionFailed(ConnectionResult res) {
        if (res.isSuccess() == true)
            return;

        if (res.hasResolution() == true) {
            try {
                res.startResolutionForResult(this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (SendIntentException e) {
                e.printStackTrace();
                this.onConnectionFailed(res); // HACK: Possible stack overflow.
            }
        } else {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setTitle("Error connection to GPS");
            alertBuilder.setMessage("Connection result: " + res.toString());
            alertBuilder.show();
        }
    }


    @Override
    public void onDisconnected() {
        mLocationClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mLocationClient != null && mLocationClient.isConnected()) {
            mLocationClient.disconnect();
        }
        if( mPopulaterTask != null && !mPopulaterTask.isCancelled())
        {
            mPopulaterTask.cancel(true);
        }

        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_NAME, 0);
        prefs.edit().putBoolean(PREF_ENABLE_POTHOLE_DETECTION, false).commit();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(mMap != null)
        {
            mMap.clear();
            mPopulaterTask = new PopulateMapTask(MainActivity.this);
            mPopulaterTask.execute();
        }
        if (mLocationClient != null && (!mLocationClient.isConnected() && !mLocationClient.isConnecting())) {
            mLocationClient.connect();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.action_new:
                Intent intent = new Intent(this, AddNewPothole.class);
                startActivity(intent);
                return true;
            case R.id.detect_toggle:
                item.setChecked(!item.isChecked());
                prefs.edit().putBoolean(PREF_ENABLE_POTHOLE_DETECTION, item.isChecked()).commit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public GoogleMap getMap() {
        return mMap;
    }

}
