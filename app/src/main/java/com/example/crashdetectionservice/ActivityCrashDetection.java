package com.example.crashdetectionservice;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import static com.example.crashdetectionservice.ActivityNotification.CHANNEL_ID;

public class ActivityCrashDetection extends AppCompatActivity {
    private static final String TAG = "ActivityCrashDetection";
    private static final String USER_CONTACT_FILE_NAME = "UserContactInfo.txt";
    private static final String EMERGENCY_CONTACT_FILE_NAME = "EmergencyContactInfo.txt";
    private static final int TIMEOUT = 120; // units seconds
    private Context context;

    String userFirstName, userLastName, userPhone;
    String emergencyFirstName1, emergencyLastName1, emergencyPhone1;
    String emergencyFirstName2, emergencyLastName2, emergencyPhone2;
    String longitude, latitude, address;

    boolean isBound = false;
    ArrayList<String> LocationPacket = new ArrayList<String>();

    NotificationManagerCompat notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_detection);

        LoadEmergencyContactTxtFile();
        LoadUserContactTxtFile();

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("alert-dialog-request"));

    }

    // broadcast receiver from ActivityService
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive called");
            // Data that is sent through broadcast receiver is of type string Array list
            LocationPacket = intent.getStringArrayListExtra("LocationPacket");
            latitude = LocationPacket.get(0);
            longitude = LocationPacket.get(1);
            address = LocationPacket.get(2);

            CrashNotification(); // function called to notify user of whether crash has occurred
            AlertDialog(); // function called to provide a dialog, requesting user to respond
        }
    };

    //method called when start button pressed
    public void startService(View view) {
        Intent intent = new Intent(this, ActivityService.class);
        startService(intent);

        Log.d(TAG, "startService method called");
    }

    //method called when end button pressed from activity_crash_detection.xml (layout file)
    public void endService(View view) {
        Intent intent = new Intent(this, ActivityService.class);
        stopService(intent);
    }

    //called from broadcastReceiver
    private void CrashNotification() {
        Notification crashNotification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            crashNotification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Crash Detected")
                    .setContentText("Have you experienced a crash?")
                    .setSmallIcon(R.drawable.ic_vintage)
                    //.setContentIntent(pendingIntent)
                    .build();
        }

        notificationManager = NotificationManagerCompat.from(this);
        // id defined for crashNotification as 2
        notificationManager.notify(2, crashNotification);

    }

    private void AlertDialog() {
        Log.d(TAG, "Alert dialogue - latitude: " + latitude + " longitude: " + longitude + " address: " + address);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("WARNING")
                .setMessage("Have you experienced a crash?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SendSMS(); // user pressed yes, therefore SMS will be sent to emergency contacts
                        dialog.dismiss(); // dialog removed
                    }
                })
                .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Log.d(TAG, "dismiss called");
                    }
                })
                .create();

        dialog.show();

        // timer added to alertDialog with a time of 120 seconds
        Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            int timeout = TIMEOUT;
            @Override
            public void run() {
                if(!dialog.isShowing()) {
                    notificationManager.cancel(2); // remove crashNotification if dialog not showing
                    Log.d(TAG, "Ending loop");
                }
                else if(timeout > 0) {
                    dialog.setMessage("Have you experienced a crash?\n" + timeout);
                    Log.d(TAG, "Timeout in: " + timeout);
                    timeout--;
                    handler.postDelayed(this, 1000); // delay of 1 second
                }
                else {
                    if (dialog.isShowing()) {
                        Log.d(TAG, "Timeout.");
                        dialog.dismiss();
                        SendSMS(); // send SMS to emergency contacts with user details
                    }
                }

            }
        };

        handler.post(runnable);
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

    public void LoadEmergencyContactTxtFile() {
        FileInputStream fis = null;
        try {
            fis = openFileInput(EMERGENCY_CONTACT_FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String text;

            while ((text = br.readLine())!=null) {
                sb.append(text).append("\n");
            }

            String[] contactDetails = sb.toString().split("\n",6);
            emergencyFirstName1 = contactDetails[0];
            emergencyLastName1= contactDetails[1];
            emergencyPhone1 = contactDetails[2];

            emergencyFirstName2 = contactDetails[3];
            emergencyLastName2 = contactDetails[4];
            emergencyPhone2 = contactDetails[5];

            Log.d(TAG, emergencyFirstName1 + " " + emergencyLastName1 + " " + emergencyPhone1);
            Log.d(TAG, emergencyFirstName2 + " " + emergencyLastName2 + " " + emergencyPhone2);

            Log.d(TAG, sb.toString());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void LoadUserContactTxtFile() {
        FileInputStream fis = null;
        try {
            fis = openFileInput(USER_CONTACT_FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String text;

            while ((text = br.readLine())!=null) {
                sb.append(text).append("\n");
            }

            String[] userContactDetails = sb.toString().split("\n",3);
            userFirstName = userContactDetails[0];
            userLastName = userContactDetails[1];
            userPhone = userContactDetails[2];

            Log.d(TAG, userFirstName + " " + userLastName + " " + userPhone);
            Log.d(TAG, sb.toString());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent(this, ActivityService.class);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        if (isBound) {
            isBound = false;
        }
        stopService(intent);
        super.onDestroy();
    }

}