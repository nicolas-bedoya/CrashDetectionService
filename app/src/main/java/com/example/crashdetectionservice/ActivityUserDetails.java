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

public class ActivityUserDetails extends Activity implements View.OnClickListener {

    private static final String TAG = "ActivityUserDetails";
    ActivityCreateLoadContacts aclc = new ActivityCreateLoadContacts();

    EditText txtUserFirstName, txtUserLastName, txtUserPhoneNumber;
    String userFirstName, userLastName, userPhoneNumber; // variables assigned to user details

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        Button butSubmitUserDetails = findViewById(R.id.butSubmitUserDetails);
        butSubmitUserDetails.setOnClickListener(this);

    }

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
            aclc.CreateUserContactTxtFile(this);
            aclc.AddDataUserContactTxtFile(userFirstName, userLastName, userPhoneNumber, this);

            // proceed to creating emergency contact
            Intent intent = new Intent(ActivityUserDetails.this, ActivityEmergencyContacts.class);
            startActivity(intent);
        }

    }
}

