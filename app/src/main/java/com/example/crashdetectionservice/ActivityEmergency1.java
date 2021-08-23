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

public class ActivityEmergency1 extends AppCompatActivity implements View.OnClickListener {
    private static final String EMERGENCY_CONTACT_FILE_NAME = "EmergencyContactInfo.txt";

    EditText first_name1, last_name1, phone_number1;
    private static final String TAG = ".ActivityEmergency1";
    public static String[] emergency1 = {" "," "," "};

    String FirstName1, LastName1, PhoneNumber1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contact1);

        Button butSaveEmergency1 = findViewById(R.id.butSaveEmergency1);
        butSaveEmergency1.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(ActivityEmergency1.this, ActivityEmergencyContacts.class);
        boolean phone_number_satisfied = false;
        boolean name_satisfied = false;
        first_name1 = (EditText) findViewById(R.id.editTextFirstName1);
        last_name1 = (EditText) findViewById(R.id.editTextLastName1);
        phone_number1 = (EditText) findViewById(R.id.editTextPhoneNumber1);

        FirstName1 = first_name1.getText().toString();
        LastName1 = last_name1.getText().toString();
        PhoneNumber1 = phone_number1.getText().toString();

        // check whether user filled all text inputs
        if (FirstName1.length() == 0 || LastName1.length() == 0 || PhoneNumber1.length() == 0) {
            // show warning that everything should be filled
            Toast errorToast = Toast.makeText(ActivityEmergency1.this, "Error, please ensure everything is filled in the boxes provided", Toast.LENGTH_LONG);
            errorToast.show();
            Log.d(TAG, "contact strings are empty");
        }

        // check whether text inputs are valid
        else {
            // if condition to check names are entered correctly
            if (FirstName1.length() > 0 && LastName1.length() > 0) {
                name_satisfied = true;
            }
            // if condition to check phone numbers are entered correctly
            if (PhoneNumber1.length() == 10 && PhoneNumber1.startsWith("04")) {
                phone_number_satisfied = true;
            }
            // if none of the conditions are satisfied, warn user that details are not entered correctly
            if (!name_satisfied || !phone_number_satisfied) {
                Toast errorToast = Toast.makeText(ActivityEmergency1.this, "Error, please ensure details are entered correctly", Toast.LENGTH_LONG);
                errorToast.show();
            }

            else if (name_satisfied && phone_number_satisfied) {
                emergency1 = new String[]{FirstName1, LastName1, PhoneNumber1};
                startActivity(intent);
            }
        }
    }
}
