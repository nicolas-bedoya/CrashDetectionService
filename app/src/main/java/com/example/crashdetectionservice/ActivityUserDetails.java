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

public class ActivityUserDetails extends Activity {
    private static final String USER_CONTACT_FILE_NAME =  "UserContactInfo.txt";
    private static final String ACCELERATION_DATA_FILE_NAME = "AccelerationData.txt";
    private static final String TAG = "ActivityLocationInitialise";

    EditText txtUserFirstName, txtUserLastName, txtUserPhoneNumber;
    String userFirstName, userLastName, userPhoneNumber; // variables assigned to user details

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        Button butSubmitUserDetails = findViewById(R.id.butSubmitUserDetails);
        butSubmitUserDetails.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("LongLogTag")
            @Override
            public void onClick(View v) {
                boolean phoneNumberSatisfied = false;  // true/false used to determine if phone number is valid in if statements
                boolean nameSatisfied = false; // true/false used to determine if the name is valid

                txtUserFirstName = (EditText)findViewById(R.id.editTextFirstName);
                txtUserLastName = (EditText)findViewById(R.id.editTextLastName);
                txtUserPhoneNumber = (EditText)findViewById(R.id.editTextPhoneNumber);

                userFirstName = txtUserFirstName.getText().toString(); // stores first name of the user
                userLastName = txtUserLastName.getText().toString(); // stores surname of user
                userPhoneNumber = txtUserPhoneNumber.getText().toString(); // stores phone number of user

                // check to see if all entries to user input is filled
                if (userFirstName.length() == 0 || userLastName.length() == 0 || userPhoneNumber.length() == 0) {
                    //show warning that everything should be filled
                    Toast errorToast = Toast.makeText(ActivityUserDetails.this, "Error, please ensure everything is filled in the boxes provided", Toast.LENGTH_LONG);
                    errorToast.show();
                    Log.d(TAG, "contact strings are empty");
                }

                // if condition to check if number and name is entered correctly
                if (userFirstName.length() > 0 && userLastName.length() > 0) {
                    nameSatisfied = true;
                }
                // if condition to check if phone number starts with 04 has the right number of characters
                if (userPhoneNumber.length() == 10 && userPhoneNumber.startsWith("04")) {
                    phoneNumberSatisfied   = true;
                }
                // if user input not entered correctly, warn user to check inputs again
                else {
                    Toast errorToast = Toast.makeText(ActivityUserDetails.this, "Error, please ensure details are entered correctly", Toast.LENGTH_LONG);
                    errorToast.show();
                }

                //if user input is valid, add user data into UserContactInfo.txt
                if (phoneNumberSatisfied && nameSatisfied) {
                    CreateUserContactTxtFile();
                    AddDataUserContactTxtFile();
                    // proceed to creating emergency contact
                    Intent intent = new Intent(ActivityUserDetails.this, ActivityEmergencyContacts.class);
                    startActivity(intent);
                }

            }
        });

    }

    // function to create text file for user
    @SuppressLint("LongLogTag")
    public void CreateUserContactTxtFile() {
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(USER_CONTACT_FILE_NAME, MODE_PRIVATE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                    Log.d(TAG, "UserTxtFile saved!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // function to store the data onto phone
    @SuppressLint("LongLogTag")
    public void AddDataUserContactTxtFile() {
        FileOutputStream fos = null;
        String txtUserFirstName = userFirstName + "\n";
        String txtUserLastName = userLastName + "\n";
        String txtUserPhoneNumber = userPhoneNumber + "\n";

        try {

            fos = openFileOutput(USER_CONTACT_FILE_NAME, MODE_APPEND);
            fos.write(txtUserFirstName.getBytes());
            fos.write(txtUserLastName.getBytes());
            fos.write(txtUserPhoneNumber.getBytes());
            Log.d(TAG,"data saved");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                    Log.d(TAG, "txt Added");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

