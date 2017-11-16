package com.liveroads.db;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by liveroads on 29/08/17.
 */

@IgnoreExtraProperties
public class NavigationItem {
    // TODO: add new fields
    // time stamps,
    // created,
    // last accessed,
    // number of times accessed

    public String location;
    public String province;
    public double latitude;
    public double longitude;
    public double distance;


    public NavigationItem() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public NavigationItem(String location, String province, double latitude, double longitude, double distance) {
        this.location = location;
        this.province = province;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("location", location);
        result.put("province", province);
        result.put("latitude", latitude);
        result.put("longitude", longitude);
        result.put("distance", distance);

        return result;
    }

}
