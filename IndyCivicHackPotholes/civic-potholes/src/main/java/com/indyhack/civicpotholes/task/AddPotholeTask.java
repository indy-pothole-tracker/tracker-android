package com.indyhack.civicpotholes.task;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mike on 5/31/14.
 */
public class AddPotholeTask extends AsyncTask<Void, Void, Void> {

    private double lat;
    private double lng;

    public AddPotholeTask(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    @Override
    protected Void doInBackground(Void... params) {

        HttpClient c = new DefaultHttpClient();
        HttpPost post = new HttpPost("http://aqueous-citadel-2041.herokuapp.com/addpothole");

        try {
            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(new BasicNameValuePair("latitude", ""+lat));
            pairs.add(new BasicNameValuePair("longitude", ""+lng));
            post.setEntity(new UrlEncodedFormEntity(pairs));

            HttpResponse response = c.execute(post);
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return null;
    }

}
