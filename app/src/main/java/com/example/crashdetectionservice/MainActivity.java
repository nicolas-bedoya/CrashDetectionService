package com.example.crashdetectionservice;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Globals {
    private static final String TAG = "MainActivity";
    ActivityCreateLoadContacts aclc = new ActivityCreateLoadContacts();
    // bool statement used to determine if the user has accepted all permissions related to the app
    boolean permissionsComplete = true;

    String[] PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Checking if all related permissions have not accepted by user
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                Log.d(TAG, "permissions have not been ticked, permissionsComplete : " + permissionsComplete);
                requestPermissions(PERMISSIONS, 1);
            }

        }

        Button butProceed = findViewById(R.id.butProceed);
        butProceed.setOnClickListener(this);

    }

    private void AlertDialog() {
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Permissions!")
                .setMessage("Ensure that all permissions are approved. Press OK to approve the permissions, then press Proceed.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(PERMISSIONS,1);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Log.d(TAG, "dismiss called");
                    }
                })
                .create();
        dialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View v) {
        // because there are no other buttons, no other cases will have to be considered
        // i.e. using switch->case
        File userContactFile = getFileStreamPath(Globals.USER_CONTACT_FILE_NAME);
        File emergencyContactFile = getFileStreamPath(Globals.EMERGENCY_CONTACT_FILE_NAME);
        File accelerationFile = getFileStreamPath(Globals.ACCELERATION_DATA_FILE_NAME);
        File gyroscopeFile = getFileStreamPath(Globals.GYROSCOPE_DATA_FILE_NAME);

        // if all permissions have been accepted
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsComplete = false;
            AlertDialog();
        } else {
            permissionsComplete = true;
        }

        if (permissionsComplete) {
            Log.d(TAG, "permissionsComplete is true : " + permissionsComplete);

            // if userContactFile is not created then take user to set up details in ActivityUserDetails
            if (!userContactFile.exists()) {
                Intent intent = new Intent(MainActivity.this, ActivityUserDetails.class);
                startActivity(intent);
            }
            // else if emergencyContactFile has not been created then take user to set up details in ActivityEmergencyContacts
            else if (!emergencyContactFile.exists()) {
                Intent intent = new Intent(MainActivity.this, ActivityEmergencyContacts.class);
                startActivity(intent);
            }
            // else all required text files have been created, go to ActivityCrashDetection
            else {
                Intent intent = new Intent(MainActivity.this, ActivityCrashDetection.class);
                startActivity(intent);
            }
            // load previous acceleration for plotting
            if (accelerationFile.exists()) {
                aclc.LoadTxtSensorFile(Globals.ACCELERATION_DATA_FILE_NAME, this);
            }
            // load previous gyroscope for plotting
            if (gyroscopeFile.exists()) {
                aclc.LoadTxtSensorFile(Globals.GYROSCOPE_DATA_FILE_NAME, this);
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called");
        super.onDestroy();
    }
}

