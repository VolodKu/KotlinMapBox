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
 * Created by liveroads on 29/08/17.
 */

public class NavigationHistory implements ValueEventListener{
    final static String dbname = "navigation-history";

    Context context;

    ArrayList<NavigationItem> history = new ArrayList<>();

    DatabaseReference database;
    String uid;

    public NavigationHistory(Context context, FirebaseDatabase db, String uid) {
        this.context = context;
        this.uid = uid;
        this.database = db.getReference(dbname);

        Query userHistory = database.child(uid);
        userHistory.addValueEventListener(this);
    }

    public void addRecord(String location, String provice, double latitude, double longitude, double distance){
        String key = database.child(dbname).push().getKey();
        NavigationItem hp = new NavigationItem(location, provice, latitude, longitude, distance);

        Map<String, Object> postValues = hp.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/" + uid + "/" + key, postValues);

        database.updateChildren(childUpdates);
    }

    public ArrayList<NavigationItem> getHistory(){
        return this.history;
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        if (dataSnapshot.exists()) {
            history.clear();
            for (DataSnapshot record : dataSnapshot.getChildren()) {
                NavigationItem item = record.getValue(NavigationItem.class);
                Log.i("LR", ":::" + item.location);
                history.add(item);
            }

            Intent intent = new Intent("ACTION_RECENT_SEARCHING_HISTORY_CHANGE");
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        }
    }

    @Override
    public void onCancelled(DatabaseError error) {
        Log.w("LR", "Failed to read value.", error.toException());
    }
}
