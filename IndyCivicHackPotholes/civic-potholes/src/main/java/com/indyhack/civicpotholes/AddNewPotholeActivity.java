package com.indyhack.civicpotholes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.IntentSender;
import android.net.Uri;
import android.os.AsyncTask;
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
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.indyhack.civicpotholes.task.AddPotholeTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class AddNewPotholeActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        AsyncCallback<LatLng> {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    EditText address;
    LocationClient mLocationClient;

    LatLng potholePosition;

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_pothole);

        address = (EditText) findViewById(R.id.address);
        mLocationClient = new LocationClient(this, this, this);

        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapView)).getMap();
        mMap.setMyLocationEnabled(true);
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                mMap.clear();
                potholePosition = latLng;
                mMap.addMarker( new MarkerOptions().position(latLng) );
                invalidateOptionsMenu();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_new_pothole, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_accept);

        item.setEnabled(potholePosition != null);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if(potholePosition != null) {
            potholePosition = null;
            mMap.clear();
            invalidateOptionsMenu();
        }
        else
            super.onBackPressed();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if(item.getItemId() == R.id.action_accept) {
            uploadPothole(potholePosition);
            Toast.makeText(this, "Reported @ " + potholePosition.latitude + ", " + potholePosition.longitude, Toast.LENGTH_SHORT).show();
            finish();
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onConnected(Bundle bun) {
        Log.d("MainActivity", "The mLocationHandler has been connected!");

        Log.v("MainActivity","Zooming camera!!!");
        if(mLocationClient == null || mMap == null) {
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
                .newCameraPosition(cameraPositionBuilder.build()));
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

    private void convertAddrToLatLng(final String addr, final AsyncCallback callback) {


        new AsyncTask<String, Void, LatLng>() {

            @Override
            protected LatLng doInBackground(String... params) {


                HttpClient client = new DefaultHttpClient();


                String url = "https://maps.googleapis.com/maps/api/geocode/json?key=" + getString(R.string.google_key);

                Uri uri = new Uri.Builder()
                        .scheme("https")
                        .authority("maps.googleapis.com")
                        .path("maps/api/geocode/json")
                        .appendQueryParameter("key", getString(R.string.google_key))
                        .appendQueryParameter("address", addr)
                        .build();


                HttpGet get = new HttpGet(uri.toString());

                HttpResponse response = null;
                InputStream stream;

                try {
                    response = client.execute(get);
                    stream = response.getEntity().getContent();

                    BufferedReader r = new BufferedReader(new InputStreamReader(stream));
                    StringBuilder total = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        total.append(line);
                    }

                    try {
                        JSONObject json = new JSONObject(total.toString());

                        JSONObject location = json.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location");


                        long lat = location.getLong("lat");
                        long lng = location.getLong("lng");

                        return new LatLng(lat, lng);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return null;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(final LatLng latLng) {
                if(latLng != null) {
                    callback.onResult(latLng);
                }
                else {
                    callback.onFail();
                }

            }
        }.execute();

//        Geocoder geoCoder = new Geocoder(getApplicationContext(), Locale.getDefault());
//        try {
//            List<Address> address = geoCoder.getFromLocationName(addr, 1);
//            double latitude = address.get(0).getLatitude();
//            double longitude = address.get(0).getLongitude();
//
//            return new LatLng(latitude, longitude);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }

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
                convertAddrToLatLng(address.getText().toString(), this);
                return true;
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResult(LatLng result) {
        uploadPothole(result);
        Toast.makeText(this, "Reported @ " + result.latitude + ", " + result.longitude, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onFail() {
        Toast.makeText(this, "Failed to get LatLng", Toast.LENGTH_SHORT).show();
    }
}
