package com.example.crashdetectionservice;

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

public class ActivityEmergencyContacts extends AppCompatActivity {
    private static final String EMERGENCY_CONTACT_FILE_NAME = "EmergencyContactInfo.txt";

    public Button button;
    public EditText editText;

    // since 2 emergency contacts are going to be added, there are 2 sets of data that we keep record of
    // hence, first_name1 and first_name2 etc.
    EditText first_name1, first_name2, last_name1, last_name2, phone_number1, phone_number2;
    private static final String TAG = "ActivityContact";
    String FirstName1, LastName1, PhoneNumber1, FirstName2, LastName2, PhoneNumber2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        // this button is used for ease for debugging, instead of manually inputting data, just
        // press this and it will set Rich and Nick as default emergency contacts
        Button butDefaultContact = findViewById(R.id.butDefaultContact);
        butDefaultContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
                Intent intent = new Intent(ActivityEmergencyContacts.this, ActivityCrashDetection.class);
                startActivity(intent);
            }
        });

        //@Override
        Button butSensor = findViewById(R.id.butSensor);
        butSensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean phone_number_satisfied = false;
                boolean name_satisfied = false;
                first_name1 = (EditText)findViewById(R.id.editTextFirstName1);
                last_name1 = (EditText)findViewById(R.id.editTextLastName1);
                phone_number1 = (EditText)findViewById(R.id.editTextPhoneNumber1);

                first_name2 = (EditText)findViewById(R.id.editTextFirstName2);
                last_name2 = (EditText)findViewById(R.id.editTextLastName2);
                phone_number2 = (EditText)findViewById(R.id.editTextPhoneNumber2);

                FirstName1= first_name1.getText().toString();
                LastName1 = last_name1.getText().toString();
                PhoneNumber1 = phone_number1.getText().toString();

                FirstName2 = first_name2.getText().toString();
                LastName2 = last_name2.getText().toString();
                PhoneNumber2 = phone_number2.getText().toString();

                // check whether user filled all text inputs
                if (FirstName1.length() == 0 || LastName1.length() == 0 || PhoneNumber1.length() == 0
                        || FirstName2.length() == 0 || LastName2.length() == 0 || PhoneNumber2.length() == 0) {
                    // show warning that everything should be filled
                    Toast errorToast = Toast.makeText(ActivityEmergencyContacts.this, "Error, please ensure everything is filled in the boxes provided", Toast.LENGTH_LONG);
                    errorToast.show();
                    Log.d(TAG, "contact strings are empty");
                }

                // check whether text inputs are valid
                else {
                    // if condition to check names are entered correctly
                    if (FirstName1.length() > 0 && LastName1.length() > 0 && FirstName2.length() > 0
                            && LastName2.length() > 0) {
                        name_satisfied = true;
                    }
                    // if condition to check phone numbers are entered correctly
                    if (PhoneNumber1.length() == 10 && PhoneNumber1.startsWith("04") &&
                            PhoneNumber2.length() == 10 && PhoneNumber2.startsWith("04")) {
                        phone_number_satisfied = true;
                    }
                    // if none of the conditions are satisfied, warn user that details are not entered correctly
                    if (!name_satisfied || !phone_number_satisfied){
                        Toast errorToast = Toast.makeText(ActivityEmergencyContacts.this, "Error, please ensure details are entered correctly", Toast.LENGTH_LONG);
                        errorToast.show();
                    }
                    // otherwise, load details into EmergencyContactInfo.txt
                    else if (name_satisfied && phone_number_satisfied) {
                        Log.d(TAG, "name: " + name_satisfied + "phone: " + phone_number_satisfied);
                        Log.d(TAG, "FN: " + FirstName1 + " SN: " + LastName1 + " PN: " + PhoneNumber1 );
                        Log.d(TAG, "PN.length: " + PhoneNumber1.length());
                        AddDataToContactTxt();
                        Intent intent = new Intent(ActivityEmergencyContacts.this, ActivityCrashDetection.class);
                        startActivity(intent);
                    }
                }
            }
        });

    }

    public void AddDataToContactTxt() {
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


}
