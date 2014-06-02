package com.indyhack.civicpotholes.model;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by david on 5/31/14 for IndyCivicHackPotholes
 */
public class Pothole {

    WeakReference<Context> contextReference = null;

    private LatLng location;
    private String address = null;

    public Pothole(Context c)
    {
        contextReference = new WeakReference<Context>(c);
    }

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public String getAddress()
    {
        if(address == null)
        {
            List<Address> addrs = null;
            Context c = contextReference.get();
            if(c != null) {
                Geocoder geo = new Geocoder(c);
                try {
                    addrs = geo.getFromLocation(getLocation().latitude, getLocation().longitude, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if( addrs != null)
                {
                    address = addrs.get(0).getThoroughfare();
                }
            }

        }
        return address;
    }
}
