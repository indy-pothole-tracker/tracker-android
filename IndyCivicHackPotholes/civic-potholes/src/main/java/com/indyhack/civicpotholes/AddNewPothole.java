package com.indyhack.civicpotholes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.IntentSender;
import android.location.Geocoder;
import android.location.Address;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.indyhack.civicpotholes.task.AddPotholeTask;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class AddNewPothole extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    EditText address;
    LocationClient mLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_pothole);

        address = (EditText) findViewById(R.id.address);
        mLocationClient = new LocationClient(this, this, this);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_new_pothole, menu);
        return true;
    }

    @Override
    public void onConnected(Bundle bun) {
        Log.d("MainActivity", "The mLocationHandler has been connected!");

        Log.v("MainActivity","Zooming camera!!!");
        if(mLocationClient == null) {
            return;
        }
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
            } catch (IntentSender.SendIntentException e) {
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

        Log.d("SafeWalk", "The mLocationHandler has been disconnected...");

        mLocationClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mLocationClient != null && mLocationClient.isConnected()) {
            mLocationClient.disconnect();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (mLocationClient != null && (!mLocationClient.isConnected() && !mLocationClient.isConnecting())) {
            mLocationClient.connect();
        }
    }

    private LatLng convertAddrToLatLng(String addr) {
        Geocoder geoCoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        try {
            List<Address> address = geoCoder.getFromLocationName(addr, 1);
            double latitude = address.get(0).getLatitude();
            double longitude = address.get(0).getLongitude();

            return new LatLng(latitude, longitude);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addPothole(View v) {
        uploadPothole(new LatLng(mLocationClient.getLastLocation().getLatitude(), mLocationClient.getLastLocation().getLongitude()));
    }

    public void uploadPothole(LatLng latlng) {
        new AddPotholeTask(latlng.latitude, latlng.longitude).execute();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_accept:
                Log.i("Address", address.getText().toString());
                uploadPothole(convertAddrToLatLng(address.getText().toString()));
                return true;
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
