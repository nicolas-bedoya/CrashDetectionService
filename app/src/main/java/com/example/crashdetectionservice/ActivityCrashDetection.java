package com.example.crashdetectionservice;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import com.example.crashdetectionservice.ActivityService.LocalBinder;


import static com.example.crashdetectionservice.ActivityNotification.CHANNEL_ID;

public class ActivityCrashDetection extends AppCompatActivity implements View.OnClickListener, Globals {

    ActivityService LocationSensorService;
    boolean isBound = false;
    boolean DismissBroadcast = false;
    private static final String TAG = "ActivityCrashDetection";
    private static final int TIMEOUT = 120; // units seconds
    private Context context;

    Runnable runnableImpactCheck;
    String[] UserDetails = new String[3];
    String userFirstName, userLastName, userPhone;

    String[] EmergencyDetails = new String[6];
    String emergencyFirstName1, emergencyLastName1, emergencyPhone1;
    String emergencyFirstName2, emergencyLastName2, emergencyPhone2;
    String longitude, latitude, address;
    String[] LocationString = new String[3];

    NotificationManagerCompat notificationManager;

    int impactVelocityTimer = 0, impactSensorTimer = 0;
    boolean impactVelocity = false, impactAccelerometer = false, impactGyroscope = false, crashDetected = false;
    boolean ActivityServiceEnd = true;
    boolean DismissAlert = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_detection);

        ActivityCreateLoadContacts aclc = new ActivityCreateLoadContacts();
        EmergencyDetails = aclc.LoadEmergencyContactTxtFile(this);

        emergencyFirstName1 = EmergencyDetails[0]; emergencyLastName1 = EmergencyDetails[1];
        emergencyPhone1 = EmergencyDetails[2]; emergencyFirstName2 = EmergencyDetails[3];
        emergencyLastName2 = EmergencyDetails[4]; emergencyPhone2 = EmergencyDetails[5];

        UserDetails = aclc.LoadUserContactTxtFile(this);
        userFirstName = UserDetails[0]; userLastName = UserDetails[1]; userPhone = UserDetails[2];

        Button butStartService = findViewById(R.id.butStartService);
        butStartService.setOnClickListener(this);

        Button butEndService = findViewById(R.id.butEndService);
        butEndService.setOnClickListener(this);

        TextView txtSpeed = findViewById(R.id.txtSpeed);
        TextView txtAngularVelocity = findViewById(R.id.txtAngularVelocity);
        TextView txtAcceleration = findViewById(R.id.txtAcceleration);
    }

    // LocationSensorConnection used for binding to ActivityService through LocationSensorService
    private ServiceConnection LocationSensorConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocalBinder binder = (LocalBinder) service;
            LocationSensorService = binder.getService();
            isBound = true; // to determine whether the service is bounded or not
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false; // service is disconnected (not bounded)
        }

    };

    // broadcast receiver to call start checking for crash detection (CrashDetectionCheck())
    // received from ActivityService
    private BroadcastReceiver mMessageReceiverCrashCheck = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive called");
            CrashDetectionCheck(); // calls check whether crash has occurred through the function
        }
    };

    // broadcast receiver used to trigger flag ActivityServiceEnd to true
    // this as a result breaks the runnable/handle in CrashDetectionCheck() function
    // received from ActivityService
    private BroadcastReceiver mMessageReceiverServiceEnd = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive called");
            ActivityServiceEnd = true;
        }
    };

    // broadcast from notification to dismiss or confirm
    private BroadcastReceiver mMessageReceiverDismissAlert = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            int id = intent.getIntExtra("id", -1);
            Log.d(TAG, "ID of getIntExtra: " + id);
            if (id >= 0) {
                if (id == 2) {
                    // confirm
                    notificationManager.cancel(2);
                    DismissBroadcast = false;
                    DismissAlert = true;
                }

                else if (id == 1) {
                    // dismiss
                    notificationManager.cancel(2);
                    DismissAlert = true;
                    DismissBroadcast = true;
                    Log.d(TAG, "dismiss called");
                }
            }
        }
    };

    // used to notify user that a crash was detected by the app
    private void CrashNotification() {
        Notification crashNotification = null;

        //intent to return back to the activity (may not be needed for now)
        Intent activityIntent = new Intent(this, ActivityCrashDetection.class);
        PendingIntent actionIntent = PendingIntent.getActivity(this, 0,
                activityIntent, 0);

        // broadcast intent to stop the alertDialog through dismiss button (continues crash detection)
        Intent broadcastConfirmIntent = new Intent(this, NotificationReceiver.class);
        broadcastConfirmIntent.putExtra("id", 2);

        PendingIntent actionConfirmIntent = PendingIntent.getBroadcast(this, 2,
                broadcastConfirmIntent, 0);

        // broadcast intent to stop crash detection check ('yes' pressed on notification)
        Intent broadcastDismissIntent = new Intent(this, NotificationReceiver.class);
        broadcastDismissIntent.putExtra("id", 1);

        PendingIntent actionDismissIntent= PendingIntent.getBroadcast(this, 1,
                broadcastDismissIntent, 0);

        //Intent dummyIntent = new Intent(this, )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            crashNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Crash Detected")
                    .setContentText("Have you experienced a crash?")
                    .setSmallIcon(R.drawable.ic_vintage)
                    //.setPriority(NotificationCompat.PRIORITY_HIGH);
                    .addAction(R.mipmap.ic_launcher_round, "Yes", actionConfirmIntent)
                    .addAction(R.mipmap.ic_launcher, "Dismiss", actionDismissIntent)
                    //.setContentIntent(intent)
                    .build();
        }

        notificationManager = NotificationManagerCompat.from(this);
        // id defined for crashNotification as 2
        notificationManager.notify(2, crashNotification);

    }
    private void AlertDialog() {
        // broadcast request used to reactivate sensors
        Intent intent = new Intent(Globals.ACTIVATE_SENSOR_REQUEST);
        Intent serviceIntent = new Intent(this, ActivityService.class);

        Log.d(TAG, "Alert dialogue - latitude: " + latitude + " longitude: " + longitude + " address: " + address);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("WARNING")
                .setMessage("Have you experienced a crash?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopService(serviceIntent);
                        unbindService(LocationSensorConnection);
                        LocalBroadcastManager.getInstance(ActivityCrashDetection.this).unregisterReceiver(mMessageReceiverCrashCheck);
                        LocalBroadcastManager.getInstance(ActivityCrashDetection.this).unregisterReceiver(mMessageReceiverServiceEnd);
                        LocalBroadcastManager.getInstance(ActivityCrashDetection.this).unregisterReceiver(mMessageReceiverDismissAlert);
                        ActivityServiceEnd = true;
                        SendSMS(); // user pressed yes, therefore SMS will be sent to emergency contacts
                        dialog.dismiss(); // dialog removed
                    }
                })
                .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        crashDetected = false;
                        // Activating sensors again through broadcast
                        LocalBroadcastManager.getInstance(ActivityCrashDetection.this).sendBroadcast(intent);
                        dialog.dismiss();
                        Log.d(TAG, "dismiss called");
                        CrashDetectionCheck(); // continue checking crash flags
                    }
                })
                .create();

        dialog.show();

        // timer added to alertDialog with a time of 120 second
        Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            int timeout = TIMEOUT;
            @Override
            public void run() {
                if (!DismissAlert) {
                    if (!dialog.isShowing()) {
                        notificationManager.cancel(2); // remove crashNotification if dialog not showing
                        Log.d(TAG, "Ending loop");
                    } else if (timeout > 0) {
                        dialog.setMessage("Have you experienced a crash?\n" + timeout);
                        Log.d(TAG, "Timeout in: " + timeout);
                        timeout--;
                        handler.postDelayed(this, 1000); // delay of 1 second
                    } else {
                        if (dialog.isShowing()) {
                            Log.d(TAG, "Timeout.");
                            stopService(serviceIntent);
                            unbindService(LocationSensorConnection);
                            LocalBroadcastManager.getInstance(ActivityCrashDetection.this).unregisterReceiver(mMessageReceiverCrashCheck);
                            LocalBroadcastManager.getInstance(ActivityCrashDetection.this).unregisterReceiver(mMessageReceiverServiceEnd);
                            LocalBroadcastManager.getInstance(ActivityCrashDetection.this).unregisterReceiver(mMessageReceiverDismissAlert);
                            ActivityServiceEnd = true;
                            SendSMS(); // user pressed yes, therefore SMS will be sent to emergency contacts
                            dialog.dismiss(); // dialog removed
                        }
                    }
                } else {
                    if (DismissBroadcast) {
                        dialog.dismiss();
                        DismissAlert = false;
                        LocalBroadcastManager.getInstance(ActivityCrashDetection.this).sendBroadcast(intent);
                        CrashDetectionCheck();
                        DismissBroadcast = false;
                    } else {
                        Log.d(TAG, "CRASH CONFIRMED FROM NOTIFICATION");
                        DismissAlert = false;
                        stopService(serviceIntent);
                        unbindService(LocationSensorConnection);
                        LocalBroadcastManager.getInstance(ActivityCrashDetection.this).unregisterReceiver(mMessageReceiverCrashCheck);
                        LocalBroadcastManager.getInstance(ActivityCrashDetection.this).unregisterReceiver(mMessageReceiverServiceEnd);
                        LocalBroadcastManager.getInstance(ActivityCrashDetection.this).unregisterReceiver(mMessageReceiverDismissAlert);
                        ActivityServiceEnd = true;
                        SendSMS(); // user pressed yes, therefore SMS will be sent to emergency contacts
                        dialog.dismiss(); // dialog removed

                    }
                }
            }
        };
        handler.post(runnable); // what exactly does this do?
    }

    // sends location to both emergency contacts
    // From testing, there is a limit to the amount of characters in an sms string, therefore
    // longitude and latitude were rounded to 2 decimal places to reduce the error
    public void SendSMS() {

        try {
            double longitudeTmp = Double.parseDouble(longitude);
            double latitudeTmp = Double.parseDouble(latitude);

            String location_longitude = String.format("%.2f", longitudeTmp);
            String location_latitude = String.format("%.2f", latitudeTmp);
            Log.d(TAG, location_longitude + " " + longitudeTmp);
            String SMS1 = "Hi " + emergencyFirstName1 + " " + emergencyLastName1 + ", " + userFirstName +
                    " " + userLastName + " has experienced a crash. " + "They are located at (" + location_latitude
                    + "," + location_longitude + "), "  + address;

            String SMS2 = "Hi " + emergencyFirstName2 + " " + emergencyLastName2 + ", " + userFirstName +
                    " " + userLastName + " has experienced a crash. " + "They are located at (" + location_latitude
                    + "," + location_longitude + "), "  + address;

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(emergencyPhone1, null, SMS1, null, null);
            smsManager.sendTextMessage(emergencyPhone2, null, SMS2, null, null);

            Log.d(TAG, "message is sent");
            Log.d(TAG, SMS1);
            Toast.makeText(this, "Message is sent", Toast.LENGTH_SHORT).show();
        }

        catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "failed to send message");
            Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
        }

    }

    public void CrashDetectionCheck() {
        crashDetected = false;
        // DismissAlert = false;
        Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // flags obtained from ActivityService through reference defined from LocationSensorService
                impactVelocity = LocationSensorService.impactVelocity;
                impactAccelerometer = LocationSensorService.impactAccelerometer;
                impactGyroscope = LocationSensorService.impactGyroscope;

                Log.d(TAG, "crash flags: " + impactVelocity + " " + impactAccelerometer + " "+ impactGyroscope);

                if (impactVelocity) {
                    if (impactVelocityTimer < 4) {
                        impactVelocityTimer++;
                    } else {
                        impactVelocityTimer = 0;
                        LocationSensorService.impactVelocity = false;
                    }
                }

                if ((impactAccelerometer || impactGyroscope)) {
                    if (impactSensorTimer < 4) {
                        impactSensorTimer++;

                        // if velocity change was detected and impact is detected from accelerometer or gyroscope
                        // assume crash has occurred
                        impactVelocity = true;
                        if (impactVelocity) {
                            LocationSensorService.impactVelocity = false;
                            LocationSensorService.impactAccelerometer = false;
                            LocationSensorService.impactGyroscope = false;
                            LocationSensorService.impactSensorTimer = 0;
                            LocationSensorService.impactVelocityTimer = 0;

                            LocationString = LocationSensorService.getCompleteAddressString();
                            address = LocationString[0];
                            latitude = LocationString[1];
                            longitude = LocationString[2];
                            crashDetected = true;

                        }

                    } else {
                        // reset the sensor timer back to 0, as well as setting flags back to false
                        impactSensorTimer = 0;
                        impactVelocityTimer = 0;
                        LocationSensorService.impactAccelerometer = false;
                        LocationSensorService.impactGyroscope = false;
                    }
                }

                if (crashDetected) {
                    Log.d(TAG, "Crash detected activated");
                    Intent unregisterUpdateIntent = new Intent(Globals.UNREGISTER_SENSOR_REQUEST);
                    // unregister sensor and location listeners from ActivityService
                    LocalBroadcastManager.getInstance(ActivityCrashDetection.this).sendBroadcast(unregisterUpdateIntent);

                    CrashNotification();
                    AlertDialog();
                }

                if(!crashDetected && !ActivityServiceEnd) {
                    //Log.d(TAG, "Delayed applied to crashDetectionCheck");
                    handler.postDelayed(this, 1000); // delay of 1 second
                }
            }
        };
        handler.post(runnable); // WHAT DOES THIS DO? (messageQueue?)
    }

    @Override
    public void onClick(View v) {
        Button b = (Button) v;
        Intent intent = new Intent(this, ActivityService.class);
        switch(b.getId()) {
            case R.id.butStartService:

                if (ActivityServiceEnd) {
                    Log.d(TAG, "startService method called");

                    // receives broadcast messages from ActivityService
                    LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiverCrashCheck,
                            new IntentFilter(Globals.START_CRASH_DETECTION_CHECK));

                    LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiverServiceEnd,
                            new IntentFilter(Globals.END_CRASH_CHECK));

                    LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiverDismissAlert,
                            new IntentFilter(Globals.DISMISS_ALERT_DIALOG));

                    startService(intent);
                    bindService(intent, LocationSensorConnection, Context.BIND_AUTO_CREATE);
                    ActivityServiceEnd = false;
                }

                break;

            case R.id.butEndService:
                Log.d(TAG, "Stop service button called " + isBound);
                if (isBound) {
                    unbindService(LocationSensorConnection);
                    isBound = false;
                }
                stopService(intent);
                LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiverCrashCheck);
                LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiverServiceEnd);
                LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiverDismissAlert);
                ActivityServiceEnd = true;
                DismissBroadcast = false;
                DismissAlert = false;
                break;
        }
    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent(this, ActivityService.class);
        ActivityServiceEnd = true;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiverCrashCheck);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiverServiceEnd);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiverDismissAlert);
        if (!isBound) {
            unbindService(LocationSensorConnection); // check if service is bounded before unbinding
        }
        stopService(intent); // kill service if on destroy is called, ie. app is removed in task manager
        super.onDestroy();
    }

}