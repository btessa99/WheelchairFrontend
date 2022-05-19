package it.unipi.dii.aide.msss.myapplication;

import android.Manifest;
import android.app.Activity;
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
import androidx.core.content.ContextCompat;

import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ContributorActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener, LocationListener {
    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope, magnetometer;

    private List<String> records;
    private float lastAccX, lastAccY, lastAccZ;
    private float lastGyrX, lastGyrY, lastGyrZ;
    private float lastMagX, lastMagY, lastMagZ;
    private double lastLat, lastLong;
    private JSONArray ja = new JSONArray();

    private static final String urlString = "https://54b2-131-114-208-187.eu.ngrok.io/locations/update";

    private static final int REQUEST_INTERVAL = 30;

    int LOCATION_REFRESH_TIME = 5000; // 5 seconds to update
    int LOCATION_REFRESH_DISTANCE = 1; // 1 meters to update

    LocationManager locationManager;

    private ScheduledExecutorService scheduleTaskExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contributor);
        initializeViews();

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
            records = new ArrayList<>();
            if (sensorManager == null) {
                Log.d("TEST", "Initializing sensor manager");

                sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.d("TEST", "Permissions are not granted, asking permissions");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION} , 1);


                    return;
                }
                Log.d("TEST", "Permissions ok");
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE, this);
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);


                Log.d("TEST", "initialized sensors and location manager");

                scheduleTaskExecutor = Executors.newScheduledThreadPool(2);
                scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        // Do stuff here!
                        Log.d("TEST", "inside run()");

                        //sendRecordsToServer();

                        //writeToFile();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Do stuff to update UI here!
                                Toast.makeText(getBaseContext(), "Sending data to the server", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                }, REQUEST_INTERVAL, REQUEST_INTERVAL, TimeUnit.SECONDS);
            }

        } else if(v.getId() == R.id.stopButton){
            Log.d("TEST", "STOP contribution");

            Toast.makeText(getBaseContext(), "Stopping the recording", Toast.LENGTH_LONG).show();

            if(sensorManager != null) {
                // unregister the sensor listener
                sensorManager.unregisterListener(this);
                sensorManager = null;
                Log.d("TEST", "unregistered the sensor listener");
            }

            if(locationManager!=null){
                // unregister the location listener
                locationManager.removeUpdates(this);
                Log.d("TEST", "unregistered the location listener");
            }

            // stop the recurring task that sends the request to the flask-server
            scheduleTaskExecutor.shutdown();

            Log.d("TEST", "shut down the schedule task executor");

            //sendRecordsToServer();
            postData();
            writeToFile();
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
        //createRecord();
        createJsonRecord();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Log.d("EVENT", event.sensor.getStringType());
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
        //createRecord();
        createJsonRecord();
    }

    public void createRecord(){
        String timestamp = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss.SSS").format(new java.util.Date());
        String record = "{" +
                "\"timestamp\": \""+ timestamp + "\", " +
                "\"latitude\": " + lastLat + ", " +
                "\"longitude\":" + lastLong + ", " +
                "\"ACC_X\": " + lastAccX + ", " +
                "\"ACC_Y\": " + lastAccY + ", " +
                "\"ACC_Z\": " + lastAccZ + ", " +
                "\"GYR_X\": " + lastGyrX + ", " +
                "\"GYR_Y\": " + lastGyrY + ", " +
                "\"GYR_Z\": " + lastGyrZ + ", " +
                "\"MAG_X\": " + lastMagX + ", " +
                "\"MAG_Y\": " + lastMagY + ", " +
                "\"MAG_Z\": " + lastMagZ
                +"}";
        records.add(record);
        //Log.d("RECORD", record);
    }

    public void createJsonRecord(){
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
        Log.i("JSON", jsonParam.toString());
        ja.put(jsonParam);
    }

    public void sendRecordsToServer(){
        String jsonString = serializeRecordList(); // prepare the POST body
        if (jsonString.equals("")) // check if there is at least 1 record
            return;
        Log.d("TEST", jsonString);
        //sendRequest(jsonString);
        postData();
    }
    public void sendRequest(String jsonString) throws IOException {
        Log.d("TEST", "sending POST request to server");
        URL url = new URL(urlString);
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;
        http.setDoOutput(true);
        http.setRequestMethod("POST"); // POST request to the flask-server
        byte[] out = jsonString.getBytes(StandardCharsets.UTF_8);
        int length = out.length;
        Log.d("OUTPUT", jsonString);
        http.setFixedLengthStreamingMode(length);
        //http.setRequestProperty("Content-Type", "application/json");
        http.connect();
        try (OutputStream os = http.getOutputStream()) {
            os.write(out); // send the JSON to the flask-server
        }
    }

    public void postData() {
        // Create a new HttpClient and Post Header
        Log.i("D", "POSTDATA");
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept","application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            Log.i("JSON Array", ja.toString());
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("data", ja);
            Log.i("JSON Full", jsonParam.toString());
            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
            os.writeBytes(jsonParam.toString());
            os.flush();
            os.close();
            Log.i("STATUS", String.valueOf(conn.getResponseCode()));
            Log.i("MSG" , conn.getResponseMessage());
            conn.disconnect();
            ja = new JSONArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String serializeRecordList() {
        String serializedList;

        Log.d("TEST", "serializing " + records.size() + " records");

        //Log.d("PRINT", String.valueOf(records));
        if (records.isEmpty()) // check if there is at least 1 record
            return "";

        serializedList = (String) records.get(0);
        records.remove(0);

        for (Object r : records) {
            serializedList = serializedList + ", " + r; // records concatenation
        }

        records.clear(); // removes all the element of the ArrayList

        return "{\"data\": [" + serializedList + "]}";
    }

    public void writeToFile(){
        Log.d("TEST", "writing records to file");
        String content = serializeRecordList() + '\n';
        File path = getApplicationContext().getFilesDir();

        try {
            FileOutputStream writer = new FileOutputStream(new File(path,"records.txt"), true);
            writer.write(content.getBytes());
            writer.close();
        } catch (IOException e) {
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
                                Log.e("msg", "ACCESS_FINE_LOCATION granted");
                            }
                        }
                    }
                }
            }
        }
    }

}