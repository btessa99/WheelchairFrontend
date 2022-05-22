package it.unipi.dii.aide.msss.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


public class ContributorActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener, LocationListener {
    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope, magnetometer;

    private float lastAccX, lastAccY, lastAccZ;
    private float lastGyrX, lastGyrY, lastGyrZ;
    private float lastMagX, lastMagY, lastMagZ;
    private double lastLat, lastLong;

    private JSONArray ja = new JSONArray(); // list of recorded records

    private static final String SERVER_IP = "https://3030-2-198-79-124.eu.ngrok.io";
    private static final String PATH = "/locations/update";

    private static final int REQUEST_INTERVAL = 60; // every 60sec the app sends records to the server

    int LOCATION_REFRESH_TIME = 2000; // 2 seconds to update location
    int LOCATION_REFRESH_DISTANCE = 1; // 1 meters to update location


    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;

    private ScheduledExecutorService scheduleTaskExecutor; // timer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contributor);
        initializeViews();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //initialize client for getting GPS
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback =  new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // location has changed
                onLocationChanged(locationResult.getLastLocation());
            }
        };

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
        sensorManager = null;

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.startButton) {
            Toast.makeText(getBaseContext(), "Starting the recording", Toast.LENGTH_LONG).show();
            Log.d("TEST", "Starting the recording");

            if (sensorManager == null) {
                Log.d("TEST", "Initializing sensor manager");

                sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

                // checking if location permissions are granted or not
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.d("TEST", "Permissions are not granted, asking permissions");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION} , 1);
                    return;
                }

                Log.d("TEST", "Permissions already granted");

                // setting last known location
                locationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        lastLat = location.getLatitude();
                        lastLong = location.getLongitude();
                        Log.d("TEST", "Last known location: ("+lastLat + ","+ lastLong+")");
                    }
                });

                LocationRequest mLocationRequest = new LocationRequest();
                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                mLocationRequest.setInterval(LOCATION_REFRESH_TIME);
                mLocationRequest.setSmallestDisplacement(LOCATION_REFRESH_DISTANCE);

                // setting the location client to receive location updates from GPS
                locationClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper());

                // registering sensor listeners to sense vibration data
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);

                Log.d("TEST", "initialized sensors and location manager");


                // scheduling a new runnable every 60sec to send collected data to the server
                scheduleTaskExecutor = Executors.newScheduledThreadPool(2);
                scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("TEST", "inside run()");

                        // send data to the server
                        postData();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getBaseContext(), "Sending data to the server", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                }, REQUEST_INTERVAL, REQUEST_INTERVAL, TimeUnit.SECONDS);
            }

        } else if(v.getId() == R.id.stopButton){ // click on STOP button
            Log.d("TEST", "STOP contribution");

            Toast.makeText(getBaseContext(), "Stopping the recording", Toast.LENGTH_LONG).show();

            if(sensorManager != null) { // checking if START button was clicked or not
                // unregistering the sensor listener
                sensorManager.unregisterListener(this);
                sensorManager = null;
                Log.d("TEST", "unregistered the sensor listener");

                // unregistering the reception of location updates
                locationClient.removeLocationUpdates(locationCallback);

                // stop the recurring task that sends the request to the flask-server
                scheduleTaskExecutor.shutdown();

                Log.d("TEST", "shut down the schedule task executor");

                // send data to the server
                postData();
            }
        }
    }


    public void initializeViews() {
        // initializes the buttons of the UI
        Button button = findViewById(R.id.startButton);
        button.setOnClickListener(this);
        button = findViewById(R.id.stopButton);
        button.setOnClickListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onLocationChanged(Location location){
        // location has changed
        lastLat = location.getLatitude();
        lastLong = location.getLongitude();
        Log.d("LOCATION_CHANGED", "lat: " + lastLat + ", long: "+lastLong);

        // saving the json object relative to the collected record
        createJsonRecord();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // a sensor changed its value
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            lastAccX = event.values[0];
            lastAccY = event.values[1];
            lastAccZ = event.values[2];
        } else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            lastGyrX = event.values[0];
            lastGyrY = event.values[1];
            lastGyrZ = event.values[2];
        } else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            lastMagX = event.values[0];
            lastMagY = event.values[1];
            lastMagZ = event.values[2];
        } else return;

        // saving the json object relative to the collected record
        createJsonRecord();
    }


    public void createJsonRecord(){

        // creating a new json object for the collected record
        JSONObject jsonParam = new JSONObject();
        try {
            String timestamp = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss.SSS").format(new java.util.Date());
            jsonParam.put("timestamp", timestamp);
            jsonParam.put("latitude",lastLat);
            jsonParam.put("longitude", lastLong);
            jsonParam.put("ACC_X", lastAccX);
            jsonParam.put("ACC_Y", lastAccY);
            jsonParam.put("ACC_Z", lastAccZ);
            jsonParam.put("GYR_X", lastGyrX);
            jsonParam.put("GYR_Y", lastGyrY);
            jsonParam.put("GYR_Z", lastGyrZ);
            jsonParam.put("MAG_X", lastMagX);
            jsonParam.put("MAG_Y", lastMagY);
            jsonParam.put("MAG_Z", lastMagZ);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // inserting the collected record into the array of records
        synchronized(this) { // synchronization is needed since the scheduled job will access "ja"
            ja.put(jsonParam);
        }

    }

    public void postData() {
        Log.i("TEST", "Preparing POST request to send to the server");
        try {
            // creating http connection to the server
            String urlString = SERVER_IP + PATH;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept","application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            // preparing the JSON object to send to the server
            JSONObject jsonParam = new JSONObject();
            synchronized(this) {
                jsonParam.put("data", ja);
                ja = new JSONArray();
            }

            // sending the JSON object containing the collected data to the server
            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes(jsonParam.toString());
            os.flush();
            os.close();

            Log.i("TEST", "http POST status:" + conn.getResponseCode());
            Log.i("TEST" , "http POST response message" + conn.getResponseMessage());

            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: { // request multiple permissions
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                Log.e("TEST", "ACCESS_COARSE_LOCATION granted>");

                            }
                        } else if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                Log.e("TEST", "ACCESS_FINE_LOCATION granted");
                            }
                        }
                    }
                }
            }
        }
    }

}