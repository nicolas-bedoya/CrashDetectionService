package com.example.crashdetectionservice;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.example.crashdetectionservice.ActivityNotification.CHANNEL_ID;


public class ActivityService extends Service implements LocationListener, SensorEventListener {

    protected Context context;
    //this.findViewById(android.R.id.content)
    private static final int TIMEOUT = 10; // units seconds
    private static final int DELAY = 250000;
    private static final String TAG = "ActivityService";

    private static final String ACCELERATION_DATA_FILE_NAME = "AccelerationData.txt";
    private static final String GYROSCOPE_DATA_FILE_NAME = "GyroscopeData.txt";

    private SensorManager sensorManager;
    Sensor accelerometer, gyroscope;
    double xA = 0, yA = 0, zA = 0;
    double xG = 0, yG = 0, zG = 0;
    double xA_kalman = 0, yA_kalman = 0, zA_kalman = 0; // variables that will be used to store the kalman filter of xA,yA,zA
    double xG_kalman = 0, yG_kalman = 0, zG_kalman = 0; // variable that will be used to store the kalman filter of xG,yG,zG

    double[] accelerationData = {0,0,0};
    double[] gyroscopeData = {0,0,0};

    protected LocationManager locationManager;
    boolean isBound = false;

    boolean impactAccelerometer = false; // true/false statement to detect large acceleration
    boolean impactGyroscope = false; // true/false statement to detect large angular velocity

    double longitude, latitude, speed;
    String address;
    ArrayList<String> LocationPacket = new ArrayList<String>(); // used to send to broadcast receiver in ActivityCrashDetection

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // given that the permission has already been accepted by the user, permission will
            // not need to be given
            return;
        }
        // updating location every 50ms (but really its approximately 1 second)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 50, 0, this);
        Log.d(TAG, "Service is created");

        File accelerationFile = getFileStreamPath(ACCELERATION_DATA_FILE_NAME); // used for loading acceleration data into txt file
        File gyroscopeFile = getFileStreamPath(GYROSCOPE_DATA_FILE_NAME); // used for loading gyroscope data into txt file

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(ActivityService.this, accelerometer, DELAY);
        sensorManager.registerListener(ActivityService.this, gyroscope, DELAY);

        // if acceleration file doesn't exist, then create it
        if (!accelerationFile.exists()) {
           CreateSensorFile(ACCELERATION_DATA_FILE_NAME);
        }
        // if gyroscope file doesn't exist, then create it
        if (!gyroscopeFile.exists()) {
            CreateSensorFile(GYROSCOPE_DATA_FILE_NAME);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand (location) called");
        // starting foreground, therefore notification has to be made clear to alert user
        // channel ID obtained from ActivityNotification so notifications
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Service")
                .setContentText("location service")
                .setSmallIcon(R.drawable.ic_vintage)
                //.setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        // I'm not entirely clear on the different between START_NOT_STICKY and START_STICKY
        return START_NOT_STICKY;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        speed = location.getSpeed()*3.6; // units km/h
        longitude = location.getLongitude();
        latitude = location.getLatitude();

        Log.d(TAG, "speed: " + speed + " long: " + longitude + " latitude: " + latitude );

        // this checks whether an "impact" has been detected from sensor listeners
        if (impactAccelerometer || impactGyroscope) {
            // if speed is greater than 0 (ie. user is moving)
            if (speed > 0) {
                address = getCompleteAddressString();
                LocationPacket.add(String.valueOf(latitude));
                LocationPacket.add(String.valueOf(longitude));
                LocationPacket.add(address);

                Log.d(TAG, "broadcast receiver to be called");
                //intent with data message to be sent
                Intent intent = new Intent("alert-dialog-request");
                intent.putStringArrayListExtra("LocationPacket", LocationPacket);

                //sends data from locationPacket to broadcast that is receiving "alert-dialog-request"
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

            } else {
                Log.d(TAG, "user is stationary, crash detection not activated");
            }

            impactAccelerometer = false;
            impactGyroscope = false;

        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            xG = event.values[0];
            yG = event.values[1];
            zG = event.values[2];

            gyroscopeData[0] = xG; gyroscopeData[1] = yG; gyroscopeData[2] = zG;
            AddDataToSensorTxt(gyroscopeData, GYROSCOPE_DATA_FILE_NAME);
            // gyroscope value of 25rad/s was allocated as a threshold for now
            if (Math.abs(xG) > 25 || Math.abs(yG) > 25 || Math.abs(zG) > 25) {
                impactGyroscope = true;
                Log.d(TAG, "impactGyroscope true");
            }
        }

        else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && impactAccelerometer == false) {
            xA = event.values[0];
            yA = event.values[1];
            zA = event.values[2];

            accelerationData[0] = xA; accelerationData[1] = yA; accelerationData[2] = zA;
            AddDataToSensorTxt(accelerationData, ACCELERATION_DATA_FILE_NAME);
            // acceleration of 35 was allocated as a threshold for now
            if (Math.abs(xA) > 35 || Math.abs(yA) > 35 || Math.abs(zA) > 35 ) {
                impactAccelerometer = true;
                Log.d(TAG, "impactAccelerometer true");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // finding the address of user from geocoder
    private String getCompleteAddressString() {
        String strAdd = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder("");

                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
                }
                strAdd = strReturnedAddress.toString();
                Log.d(TAG, strReturnedAddress.toString());
            } else {
                Log.d(TAG, "No Address returned!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Cannot get Address!");
        }
        return strAdd;
    }

    public void CreateSensorFile(String file) {
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(file, MODE_PRIVATE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                    Log.d(TAG, file + " created!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void AddDataToSensorTxt(double sensor[], String file) {
        String text = sensor[0] + ", " + sensor[1] + ", " + sensor[2] + "\n";
        FileOutputStream fos = null;

        try {

            fos = openFileOutput(file, MODE_APPEND);
            fos.write(text.getBytes());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                    //Log.d(TAG, "txt Added");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy LocationServices called");
        isBound = false;
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }

}
