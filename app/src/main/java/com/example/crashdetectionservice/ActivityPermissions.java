package com.example.crashdetectionservice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ActivityPermissions extends Activity {
    private static final String USER_CONTACT_FILE_NAME =  "UserContactInfo.txt";
    private static final String TAG = "ActivityLocationInitialise";

    public Button button;
    public EditText editText;


    EditText txtUserFirstName, txtUserLastName, txtUserPhoneNumber;
    String userFirstName, userLastName, userPhoneNumber;
    Switch switchExternalStorage;

    boolean permissionGranted = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        Switch switchFineLocation = findViewById(R.id.switchFineLocation);
        switchFineLocation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
                        else {
                            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},1);
                        }
                    }
                }
            }
        });

        Switch switchSMS = findViewById(R.id.switchSMS);
        switchSMS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED);
                        else {
                            requestPermissions(new String[] {Manifest.permission.SEND_SMS},1);
                        }
                    }
                }
            }
        });

        Switch switchExternalStorage = findViewById(R.id.switchExternalStorage);
        switchExternalStorage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                            ;
                        else {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        }
                    }
                }
            }
        });

        Button butProceedPermissions = findViewById(R.id.butProceedPermissions);
        butProceedPermissions.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("LongLogTag")
            @Override
            public void onClick(View v) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                            checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                        permissionGranted = true;
                    } else {
                        Toast.makeText(ActivityPermissions.this, "Ensure all permissions are ticked", Toast.LENGTH_LONG).show();
                    }
                }

                if (permissionGranted) {
                    Intent intent = new Intent(ActivityPermissions.this, ActivityUserDetails.class);
                    startActivity(intent);
                }


            }
        });

    }

}

