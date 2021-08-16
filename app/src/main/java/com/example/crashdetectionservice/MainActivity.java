package com.example.crashdetectionservice;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
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
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // bool statement used to determine if the user has accepted all permissions related to the app
    boolean permissionsComplete;

    // ACCELERATION... and GYROSCOPE... used to store data from accelerometer in the format of csv
    // this is done so I can load the data into debug terminal so I can transfer data into MATLAB for graphing
    private static final String ACCELERATION_DATA_FILE_NAME = "AccelerationData.txt";
    private static final String GYROSCOPE_DATA_FILE_NAME = "GyroscopeData.txt";
    private static final String USER_CONTACT_FILE_NAME = "UserContactInfo.txt"; // stores user contact info
    private static final String EMERGENCY_CONTACT_FILE_NAME = "EmergencyContactInfo.txt"; // stores emergency contact info


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button butProceed = findViewById(R.id.butProceed);
        butProceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File userContactFile = getFileStreamPath(USER_CONTACT_FILE_NAME);
                File emergencyContactFile = getFileStreamPath(EMERGENCY_CONTACT_FILE_NAME);
                File accelerationFile = getFileStreamPath(ACCELERATION_DATA_FILE_NAME);
                File gyroscopeFile = getFileStreamPath(GYROSCOPE_DATA_FILE_NAME);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Checking if all related permissions have not accepted by user
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        permissionsComplete = false;
                        Log.d(TAG, "permissions have not been ticked, permissionsComplete : " + permissionsComplete);
                        // Takes user to a permission activity to approve permissions
                        Intent intent = new Intent(MainActivity.this, ActivityPermissions.class);
                        startActivity(intent);

                    } else {
                        permissionsComplete = true;
                        Log.d(TAG, "permissions have been ticked");
                    }
                }

                // if all permissions have been accepted
                if (permissionsComplete) {
                    Log.d(TAG, "permissionsComplete is true : " + permissionsComplete);

                    // if userContactFile has not been created then take user to set up details in ActivityUserDetails
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
                        LoadTxtSensorFile(ACCELERATION_DATA_FILE_NAME);
                    }
                    // load previous gyroscope for plotting
                    if (gyroscopeFile.exists()) {
                        LoadTxtSensorFile(GYROSCOPE_DATA_FILE_NAME);
                    }
                }


            }
        });
    }

    // takes string argument which relates to the file being scanned
    public void LoadTxtSensorFile(String file) {
        FileInputStream fis = null;
        try {
            fis = openFileInput(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String text;

            while ((text = br.readLine())!=null) {
                sb.append(text).append("\n");
            }
            // print line by line of text file into terminal
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
        Log.d(TAG, "onDestroy called");
        super.onDestroy();
    }
}