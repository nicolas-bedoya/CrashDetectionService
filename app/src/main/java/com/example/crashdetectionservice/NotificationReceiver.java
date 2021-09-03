package com.example.crashdetectionservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class NotificationReceiver extends BroadcastReceiver implements Globals{
    public static final String TAG = "NotificationReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra("id", -1);
        Intent alertIntent = new Intent(Globals.DISMISS_ALERT_DIALOG);
        Log.d(TAG, "ID of getIntExtra: " + id);
        if (id >= 0) {
            if (id == 1) {
                alertIntent.putExtra("id", id);
            } else if (id == 2) {
                alertIntent.putExtra("id", id);
            }
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(alertIntent);
        // send broadcast to activityCrashDetection to stop
    }
}
