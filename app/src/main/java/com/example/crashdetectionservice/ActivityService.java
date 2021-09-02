package com.example.crashdetectionservice;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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

    private final IBinder LocationSensorBinder = new LocalBinder();
    protected Context context;
    private static final int TIMEOUT = 10; // units seconds
    private static final int DELAY = 250000;
    private static final String TAG = "ActivityService";

    private static final String ACCELERATION_DATA_FILE_NAME = "AccelerationData.txt";
    private static final String GYROSCOPE_DATA_FILE_NAME = "GyroscopeData.txt";

    private static final String ACTIVATE_SENSOR_REQUEST = "activate-sensor-request";
    private static final String START_CRASH_DETECTION_CHECK = "start-crash-detection-check";
    private static final String UNREGISTER_SENSOR_REQUEST = "unregister-sensor-request";
    private static final String END_CRASH_CHECK = "end-crash-check";

    private SensorManager sensorManager;
    Sensor accelerometer, gyroscope, magnetic;
    double xA = 0, yA = 0, zA = 0; // acceleration variables (xyz)
    double xG = 0, yG = 0, zG = 0; // gyroscope variables (xyz)
    double xM = 0, yM = 0, zM = 0; // magnetic field variables (xyz)
    double xA_kalman = 0, yA_kalman = 0, zA_kalman = 0; // variables that will be used to store the kalman filter of xA,yA,zA
    double xG_kalman = 0, yG_kalman = 0, zG_kalman = 0; // variable that will be used to store the kalman filter of xG,yG,zG

    boolean firstVelocityInstance = false;
    double previousVelocity = 0, currentVelocity = 0, velocityChange = 0;

    double[] accelerationData = new double[3];
    float[] accelerationFloat = new float[3];

    double[] magneticData = new double[3];
    float[] magneticFloat = new float[3];

    double[] gyroscopeData = new double[3];

    float[] RotationFloat = new float[9]; // storing the rotation matrix
    float[] IdentityFloat = new float[9]; // storing the identity matrix (not needed yet)

    float[] OrientationFloat = new float[3];
    double[] Orientation = new double[3];

    double[] maxAcceleration = {0,0,0}, maxGyroscope = {0,0,0};

    protected LocationManager locationManager;

    boolean impactAccelerometer = false; // true/false statement to detect large acceleration
    boolean impactGyroscope = false; // true/false statement to detect large angular velocity
    boolean impactVelocity = false; // true/false statement to detect velocity changes (linear acceleration)
    int impactSensorTimer = 0, impactVelocityTimer = 0;

    boolean firstLocationInstance = true;

    double longitude, latitude, speed, locationAccuracy;
    String address;
    ArrayList<String> LocationPacket = new ArrayList<String>(); // used to send to broadcast receiver in ActivityCrashDetection

    String[] txtViewString = new String[3];

    @Override
    public void onCreate() {
        super.onCreate();

        File accelerationFile = getFileStreamPath(ACCELERATION_DATA_FILE_NAME); // used for loading acceleration data into txt file
        File gyroscopeFile = getFileStreamPath(GYROSCOPE_DATA_FILE_NAME); // used for loading gyroscope data into txt file

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        Log.d(TAG, "onCreate ActivityService called");

        // broadcast definition to listen from ActivityCrashDetection
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiverRegister,
                new IntentFilter(ACTIVATE_SENSOR_REQUEST));

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiverUnregister,
                new IntentFilter(UNREGISTER_SENSOR_REQUEST));

        registerUpdates();

        // if acceleration file doesn't exist, then create it
        if (!accelerationFile.exists()) {
           CreateSensorFile(ACCELERATION_DATA_FILE_NAME);
        }
        // if gyroscope file doesn't exist, then create it
        if (!gyroscopeFile.exists()) {
            CreateSensorFile(GYROSCOPE_DATA_FILE_NAME);
        }
    }


    // used to listen for when to activate listeners from ActivityCrashDetection
    private BroadcastReceiver mMessageReceiverRegister = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "sensor restart called");
            registerUpdates();
        }
    };

    private BroadcastReceiver mMessageReceiverUnregister = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "sensor unregister called");
            unRegisterUpdates();
        }
    };

    public ActivityService() {}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return LocationSensorBinder;
    }

    // creating reference to ActivityService accessed through Binder
    public class LocalBinder extends Binder {
        ActivityService getService() {
            return ActivityService.this;
        }
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
        return START_NOT_STICKY;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (firstLocationInstance) {
            Log.d(TAG, "firstLocationInstance");
            Intent intent = new Intent(START_CRASH_DETECTION_CHECK);
            LocalBroadcastManager.getInstance(ActivityService.this).sendBroadcast(intent);
            firstLocationInstance = false;
        }

        speed = location.getSpeed()*3.6; // units km/h
        longitude = location.getLongitude();
        latitude = location.getLatitude();

        locationAccuracy = location.getAccuracy();

        if (!firstVelocityInstance) {
            currentVelocity = speed;
            firstVelocityInstance = true;
        } else {
            previousVelocity = currentVelocity;
            currentVelocity = speed;
            velocityChange = currentVelocity - previousVelocity;
        }

        // previous velocity is greater than current velocity in moments of crash, therefore
        // check to see if velocity change is negative
        if (locationAccuracy > 0 && velocityChange < 0) {
            // if the velocity change is greater than 10, set impactVelocity to true
            if (Math.abs(velocityChange) > 10) {
                impactVelocity = true;
            }
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

            // finding max gyroscope
            if (Math.abs(xG) > Math.abs(maxGyroscope[0])) {
                maxGyroscope[0] = xG;
            }
            if (Math.abs(yG) > Math.abs(maxGyroscope[1])) {
                maxGyroscope[1] = yG;
            }
            if (Math.abs(zG) > Math.abs(maxGyroscope[2])) {
                maxGyroscope[2] = zG;
            }
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
            accelerationFloat = new float[]{(float) xA, (float) yA, (float) zA};

            //finding max acceleration
            if (Math.abs(xA) > Math.abs(maxAcceleration[0])) {
                maxAcceleration[0] = xA;
            }
            if (Math.abs(yA) > Math.abs(maxAcceleration[1])) {
                maxAcceleration[1] = yA;
            }
            if (Math.abs(zA) > Math.abs(maxAcceleration[2])) {
                maxAcceleration[2] = zA;
            }

            AddDataToSensorTxt(accelerationData, ACCELERATION_DATA_FILE_NAME);
            // acceleration of 35 was allocated as a threshold for now
            if (Math.abs(xA) > 35 || Math.abs(yA) > 35 || Math.abs(zA) > 35 ) {
                impactAccelerometer = true;
                Log.d(TAG, "impactAccelerometer true");

            }
        }

        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            xM = event.values[0];
            yM = event.values[1];
            zM = event.values[2];

            magneticData[0] = xM; magneticData[1] = yM; magneticData[2] = zM;
            magneticFloat = new float[]{(float) xM, (float) yM, (float) zM};

            SensorManager.getRotationMatrix(RotationFloat, IdentityFloat, accelerationFloat, magneticFloat);
            SensorManager.getOrientation(RotationFloat, OrientationFloat);

            Orientation = new double[]{(double) OrientationFloat[0], (double) OrientationFloat[1],
                    (double) OrientationFloat[2]};

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // finding the address of user from geocoder
    public String[] getCompleteAddressString() {
        String strAdd = "";
        Geocoder geocoder = new Geocoder(ActivityService.this, Locale.getDefault());
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
        return new String[]{strAdd, String.valueOf(latitude), String.valueOf(longitude)};
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

    public void registerUpdates() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // given that the permission has already been accepted by the user, permission will
            // not need to be given
            return;
        }
        // updating location every 50ms (but really its approximately 1 second)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 50, 0, this);
        sensorManager.registerListener(ActivityService.this, accelerometer, DELAY);
        sensorManager.registerListener(ActivityService.this, gyroscope, DELAY);
        sensorManager.registerListener(ActivityService.this, magnetic, DELAY);
    }

    public void unRegisterUpdates() {
        // Unregister SensorManager listeners.
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, gyroscope);
        sensorManager.unregisterListener(this, magnetic);
        // Cease listening for location updates.
        locationManager.removeUpdates(this);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy ActivityService called");
        stopForeground(true);
        firstLocationInstance = true;

        Intent intent = new Intent(END_CRASH_CHECK);
        LocalBroadcastManager.getInstance(ActivityService.this).sendBroadcast(intent);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiverRegister);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiverUnregister);

        unRegisterUpdates();
        super.onDestroy();
    }

}
