package com.example.crashdetectionservice;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.crashdetectionservice.ActivityCrashDetection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ActivityEmergencyContacts extends AppCompatActivity implements View.OnClickListener{
    private static final String EMERGENCY_CONTACT_FILE_NAME = "EmergencyContactInfo.txt";

    public Button button;
    public EditText editText;

    // since 2 emergency contacts are going to be added, there are 2 sets of data that we keep record of
    // hence, first_name1 and first_name2 etc.
    private static final String TAG = "ActivityEmergencyContacts";
    String FirstName1, LastName1, PhoneNumber1, FirstName2, LastName2, PhoneNumber2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        Button butEmergency1 = findViewById(R.id.butEmergency1);
        butEmergency1.setOnClickListener(this);

        Button butEmergency2 = findViewById(R.id.butEmergency2);
        butEmergency2.setOnClickListener(this);

        // this button is used for ease for debugging, instead of manually inputting data, just
        // press this and it will set Rich and Nick as default emergency contacts
        Button butDefaultContact = findViewById(R.id.butDefaultContact);
        butDefaultContact.setOnClickListener(this);

        //@Override
        Button butSaveEmergencyContacts = findViewById(R.id.butSaveEmergencyContacts);
        butSaveEmergencyContacts.setOnClickListener(this);

    }

    @SuppressLint("LongLogTag")
    public void AddDataToContactTxt() {
        FirstName1 = ActivityEmergency1.emergency1[0];
        LastName1 = ActivityEmergency1.emergency1[1];
        PhoneNumber1 = ActivityEmergency1.emergency1[2];

        FirstName2 = ActivityEmergency2.emergency2[0];
        LastName2 = ActivityEmergency2.emergency2[1];
        PhoneNumber2 = ActivityEmergency2.emergency2[2];

        String firstName1 = FirstName1 + "\n";
        String lastName1 = LastName1 + "\n";
        String phoneNumber1 = PhoneNumber1 + "\n";

        String firstName2 = FirstName2 + "\n";
        String lastName2 = LastName2 + "\n";
        String phoneNumber2 = PhoneNumber2 + "\n";

        FileOutputStream fos = null;
        // Add data line by line into text file
        try {
            fos = openFileOutput(EMERGENCY_CONTACT_FILE_NAME, MODE_APPEND);
            fos.write(firstName1.getBytes());
            fos.write(lastName1.getBytes());
            fos.write(phoneNumber1.getBytes());
            fos.write(firstName2.getBytes());
            fos.write(lastName2.getBytes());
            fos.write(phoneNumber2.getBytes());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                    Log.d(TAG, "txt added");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @SuppressLint("LongLogTag")
    @Override
    public void onClick(View v) {
        Button b = (Button) v;
        Intent intent = new Intent(ActivityEmergencyContacts.this, ActivityCrashDetection.class);

        switch (b.getId()) {
            case R.id.butDefaultContact:
                FirstName2 = (String) "Richard";
                LastName2 = (String) "Liang";
                PhoneNumber2 = (String) "0469896996";

                FirstName1 = (String) "Nicolas";
                LastName1 = (String) "Bedoya";
                PhoneNumber1 = (String) "0424586376";

                Log.d(TAG, "FN1: " + FirstName1 + " SN1: " + LastName1 + " PN1: " + PhoneNumber1 );
                Log.d(TAG, "FN2: " + FirstName2 + " SN2: " + LastName2 + " PN2: " + PhoneNumber2 );
                Log.d(TAG, "PN1.length: " + PhoneNumber1.length());

                AddDataToContactTxt(); // Add emergency contacts to EmergencyContactInfo.txt

                // proceed to ActivityCrashDetection once data is stored into corresponding txt files
                startActivity(intent);
                break;

            case R.id.butSaveEmergencyContacts:
                AddDataToContactTxt();
                startActivity(intent);
                break;

            case R.id.butEmergency1:
                Intent intentEmergency1 = new Intent(ActivityEmergencyContacts.this, ActivityEmergency1.class);
                startActivity(intentEmergency1);
                break;

            case R.id.butEmergency2:
                Intent intentEmergency2 = new Intent(ActivityEmergencyContacts.this, ActivityEmergency2.class);
                startActivity(intentEmergency2);
                break;
        }
    }
}
