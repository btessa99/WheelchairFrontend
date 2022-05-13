package com.example.recordapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ContributorActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener, LocationListener {
    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope, magnetometer, gps;

    private List records;
    private float lastAccX, lastAccY, lastAccZ;
    private float lastGyrX, lastGyrY, lastGyrZ;
    private float lastMagX, lastMagY, lastMagZ;
    private double lastLat, lastLong;

    private static final String urlString = "http://127.0.0.1:12345/locations/update";

    private static final int REQUEST_INTERVAL = 300000;

    LocationManager locationManager;

    Timer timer;

    private static class RequestTask extends TimerTask {
        List records;

        RequestTask(List list) {
            this.records = list;
        }

        @Override
        public void run() {
            try {
                sendRequest();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendRequest() throws IOException {

            URL url = new URL(urlString);
            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) con;
            http.setRequestMethod("POST"); // POST request to the flask-server
            http.setDoOutput(true);

            String serializedList = serializeRecordList();

            if (serializedList.equals("")) // check if there is at least 1 record
                return;

            String jsonString = "[" + serializedList + "]"; // JSON document to send to the flask-server

            byte[] out = jsonString.getBytes(StandardCharsets.UTF_8);
            int length = out.length;

            http.setFixedLengthStreamingMode(length);
            http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            http.connect();
            try (OutputStream os = http.getOutputStream()) {
                os.write(out); // send the JSON to the flask-server
            }
        }

        public String serializeRecordList() {
            String serializedList = "";

            if (records.isEmpty()) // check if there is at least 1 record
                return "";

            serializedList = (String) records.get(0);
            records.remove(0);

            for (Object r : records) {
                serializedList = serializedList + ", " + r;
            }

            return serializedList;
        }
    }

    //final int REQUEST_CODE_ASK_PERMISSIONS = 123;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contributor);
        initializeViews();
        //checkPermissions();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

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

        timer = new Timer();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.startButton) {
            records = new ArrayList<String>();
            if (sensorManager == null) {
                sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);
                Toast.makeText(getBaseContext(), "Starting the recording ", Toast.LENGTH_LONG).show();
                timer.scheduleAtFixedRate(new RequestTask(records), REQUEST_INTERVAL, REQUEST_INTERVAL);
            }
        } else if(v.getId() == R.id.stopButton){
            Toast.makeText(getBaseContext(), "Stopping the recording", Toast.LENGTH_LONG).show();
            if(sensorManager != null) {
                sensorManager.unregisterListener(this);
                sensorManager = null;
                timer.cancel();

                // TODO : Send request containing records to python server


            }
        }
    }

    public void initializeViews() {
        Button button = findViewById(R.id.startButton);
        button.setOnClickListener(this);
        button = findViewById(R.id.stopButton);
        button.setOnClickListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    @Override
    public void onLocationChanged(Location location){
        lastLat = location.getLatitude();
        lastLong = location.getLongitude();
        createRecord();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float deltaX = Math.abs(lastAccX - event.values[0]);
            float deltaY = Math.abs(lastAccY - event.values[1]);
            float deltaZ = Math.abs(lastAccZ - event.values[2]);
            lastAccX = event.values[0];
            lastAccY = event.values[1];
            lastAccZ = event.values[2];
        } else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            float deltaX = Math.abs(lastGyrX - event.values[0]);
            float deltaY = Math.abs(lastGyrY - event.values[1]);
            float deltaZ = Math.abs(lastGyrZ - event.values[2]);
            lastGyrX = event.values[0];
            lastGyrY = event.values[1];
            lastGyrZ = event.values[2];
        } else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            float deltaX = Math.abs(lastMagX - event.values[0]);
            float deltaY = Math.abs(lastMagY - event.values[1]);
            float deltaZ = Math.abs(lastMagZ - event.values[2]);
            lastMagX = event.values[0];
            lastMagY = event.values[1];
            lastMagZ = event.values[2];
        } else return;

        createRecord();
    }

    public void createRecord(){
        String timestamp = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss.SSS").format(new java.util.Date());
        String record = "{" +
                "'timestamp': "+ timestamp + ", " +
                "'latitude': " + lastLat + ", " +
                "'longitude': " + lastLong + ", " +
                "'ACC_X': " + lastAccX + ", " +
                "'ACC_Y': " + lastAccY + ", " +
                "'ACC_Z': " + lastAccZ + ", " +
                "'GYR_X': " + lastGyrX + ", " +
                "'GYR_Y': " + lastGyrY + ", " +
                "'GYR_Z': " + lastGyrZ + ", " +
                "'MAG_X': " + lastMagX + ", " +
                "'MAG_Y': " + lastMagY + ", " +
                "'MAG_Z': " + lastMagZ
                +"}";
        records.add(record);
    }


    /*
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermissions() {
        int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }
        Toast.makeText(getBaseContext(), "Permission is already granted", Toast.LENGTH_LONG).show();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getBaseContext(), "Permission Granted", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getBaseContext(), "WRITE_EXTERNAL_STORAGE Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }*/

}