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

public class ActivityEmergency2 extends AppCompatActivity implements View.OnClickListener {
    private static final String EMERGENCY_CONTACT_FILE_NAME = "EmergencyContactInfo.txt";

    EditText first_name2, last_name2, phone_number2;
    public static String[] emergency2 = {"", "", ""};
    private static final String TAG = ".ActivityEmergency2";

    String FirstName2, LastName2, PhoneNumber2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contact2);

        Button butSaveEmergency1 = findViewById(R.id.butSaveEmergency2);
        butSaveEmergency1.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(ActivityEmergency2.this, ActivityEmergencyContacts.class);
        boolean phone_number_satisfied = false;
        boolean name_satisfied = false;
        first_name2 = (EditText) findViewById(R.id.editTextFirstName2);
        last_name2 = (EditText) findViewById(R.id.editTextLastName2);
        phone_number2 = (EditText) findViewById(R.id.editTextPhoneNumber2);

        FirstName2 = first_name2.getText().toString();
        LastName2 = last_name2.getText().toString();
        PhoneNumber2 = phone_number2.getText().toString();

        // check whether user filled all text inputs
        if (FirstName2.length() == 0 || LastName2.length() == 0 || PhoneNumber2.length() == 0) {
            // show warning that everything should be filled
            Toast errorToast = Toast.makeText(ActivityEmergency2.this, "Error, please ensure everything is filled in the boxes provided", Toast.LENGTH_LONG);
            errorToast.show();
            Log.d(TAG, "contact strings are empty");
        }

        // check whether text inputs are valid
        else {
            // if condition to check names are entered correctly
            if (FirstName2.length() > 0 && LastName2.length() > 0) {
                name_satisfied = true;
            }
            // if condition to check phone numbers are entered correctly
            if (PhoneNumber2.length() == 10 && PhoneNumber2.startsWith("04")) {
                phone_number_satisfied = true;
            }
            // if none of the conditions are satisfied, warn user that details are not entered correctly
            if (!name_satisfied || !phone_number_satisfied) {
                Toast errorToast = Toast.makeText(ActivityEmergency2.this, "Error, please ensure details are entered correctly", Toast.LENGTH_LONG);
                errorToast.show();
            }

            else if (name_satisfied && phone_number_satisfied) {
                emergency2 = new String[]{FirstName2, LastName2, PhoneNumber2};
                startActivity(intent);
            }
        }
    }
}
