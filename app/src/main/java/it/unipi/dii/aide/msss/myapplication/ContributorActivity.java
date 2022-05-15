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

    //private static final int REQUEST_INTERVAL = 300000;

    private static final int REQUEST_INTERVAL = 30000;

    int LOCATION_REFRESH_TIME = 5000; // 5 seconds to update
    int LOCATION_REFRESH_DISTANCE = 1; // 1 meters to update

    LocationManager locationManager;

    private ScheduledExecutorService scheduleTaskExecutor;

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

       scheduleTaskExecutor = Executors.newScheduledThreadPool(1);

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.startButton) {
            Toast.makeText(getBaseContext(), "Starting the recording", Toast.LENGTH_LONG).show();
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
                    //return;
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE, this);
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);

                scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        // Do stuff here!
/*
                        String serializedList = serializeRecordList();

                        if (serializedList.equals("")) // check if there is at least 1 record
                            return;

                        String jsonString = "[" + serializedList + "]"; // JSON document to send to the flask-server

                        try {
                            sendRequest(jsonString);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
*/
                        //writeToFile(jsonString);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Do stuff to update UI here!
                                Toast.makeText(MainActivity.this, "Sending data to the server", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                }, 30, 30, TimeUnit.SECONDS);
            }

        } else if(v.getId() == R.id.stopButton){
            Toast.makeText(getBaseContext(), "Stopping the recording", Toast.LENGTH_LONG).show();
            if(sensorManager != null) {
                sensorManager.unregisterListener(this);
                sensorManager = null;
                Toast.makeText(getBaseContext(), "Herepepepe", Toast.LENGTH_LONG).show();
                scheduleTaskExecutor.shutdown();
                //writeToFile(serializeRecordList());

                // TODO : Send request containing records to python server

                Toast.makeText(getBaseContext(), "Here", Toast.LENGTH_LONG).show();
/*
                String serializedList = serializeRecordList();
                Toast.makeText(getBaseContext(), "Here2", Toast.LENGTH_LONG).show();
                if (serializedList.equals("")) // check if there is at least 1 record
                    return;
                Toast.makeText(getBaseContext(), "Here3", Toast.LENGTH_LONG).show();

                String jsonString = "[" + serializedList + "]"; // JSON document to send to the flask-server
*/
/*
                try {
                    sendRequest(jsonString);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //writeToFile(jsonString);

                Toast.makeText(getBaseContext(), "Here4", Toast.LENGTH_LONG).show();
*/
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onLocationChanged(Location location){
        lastLat = location.getLatitude();
        lastLong = location.getLongitude();
        createRecord();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
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

    public void sendRequest(String jsonString) throws IOException {

        URL url = new URL(urlString);
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod("POST"); // POST request to the flask-server
        http.setDoOutput(true);

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
            serializedList = serializedList + ", " + r; // records concatenation
        }

        records.clear(); // removes all the element of the ArrayList

        return serializedList;
    }

    public void writeToFile(String content){
        content += '\n';
        File path = getApplicationContext().getFilesDir();
        Toast.makeText(getBaseContext(), "Path: " + path.getPath(), Toast.LENGTH_LONG).show();
        try {
            FileOutputStream writer = new FileOutputStream(new File(path,"records.csv"), true);
            writer.write(content.getBytes());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
