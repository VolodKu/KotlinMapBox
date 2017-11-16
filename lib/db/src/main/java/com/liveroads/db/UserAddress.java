package com.liveroads.db;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Almond on 9/1/2017.
 */

public class UserAddress implements ValueEventListener {
    final static String dbname = "user-address";

    Context context;

    NavigationItem home_address = null;
    NavigationItem work_address = null;

    DatabaseReference database;
    String uid;

    public UserAddress(Context context, FirebaseDatabase db, String uid) {
        this.context = context;
        this.uid = uid;
        this.database = db.getReference(dbname);

        Query userHistory = database.child(uid);
        userHistory.addValueEventListener(this);
        userHistory.addListenerForSingleValueEvent(this);
    }

    public void addRecord(String key, String location, String provice, double latitude, double longitude, double distance){
        String ls = database.child(dbname).child(key).getKey();

        NavigationItem hp = new NavigationItem(location, provice, latitude, longitude, distance);

        Map<String, Object> postValues = hp.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/" + uid + "/" + key, postValues);

        database.updateChildren(childUpdates);
    }

    public NavigationItem getHomeAddress(){
        return this.home_address;
    }

    public NavigationItem getWordAddress(){
        return this.work_address;
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        if (dataSnapshot.exists()) {

            for (DataSnapshot record : dataSnapshot.getChildren()) {
                NavigationItem item = record.getValue(NavigationItem.class);

                if (record.getKey().equals("home")) home_address = item;
                else work_address = item;
            }

            Intent intent = new Intent("ACTION_HOME_WORK_ADDRESS_CHANGED");
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        }
    }

    @Override
    public void onCancelled(DatabaseError error) {
        Log.w("LR", "Failed to read value.", error.toException());
    }
}
