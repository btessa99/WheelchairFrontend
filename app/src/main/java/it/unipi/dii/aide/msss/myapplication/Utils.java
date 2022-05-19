package it.unipi.dii.aide.msss.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.util.JsonReader;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.Response;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import org.gavaghan.geodesy.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import it.unipi.dii.aide.msss.myapplication.entities.Landmark;

public class Utils {
    private static class RetrieveLandmarks implements Callable<ArrayList<Landmark>> {

        private final String url = "https://ad34-62-205-14-42.eu.ngrok.io/locations/inaccessible/scores";


        @Override
        public ArrayList<Landmark> call() throws Exception {
            return fetchLandmarks();
        }

        private ArrayList<Landmark> fetchLandmarks() {
            ArrayList<Landmark> landmarks = new ArrayList<>();
            try {
                URL serverEndpoint = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) serverEndpoint.openConnection();
                connection.setRequestProperty("User-Agent", "my-rest-app-v0.1");


                Log.d("connect","code: " + connection.getResponseCode());

                if (connection.getResponseCode() == 200) {

                    InputStream responseBody = connection.getInputStream();
                    System.out.println(responseBody);
                    InputStreamReader responseBodyReader = new InputStreamReader(responseBody, "UTF-8");
                    System.out.println(responseBodyReader);



                    JsonReader jsonReader = new JsonReader(responseBodyReader);
                    jsonReader.setLenient(true);

                    landmarks = readLandmarksArray(jsonReader);

                } else {
                    // Error handling code goes here
                    System.out.println("server not reachable");
                    // test landmarks
                    landmarks.add(new Landmark(44.04633, 10.06371, 54, 55));
                    landmarks.add(new Landmark(44.05, 10.06, 32, 77)); // Gelateria enzo, great peanut Ice Cream
                    landmarks.add(new Landmark(37.401437,-116.86773, 833334, 833334));
                }
            }catch (Exception e){e.printStackTrace();}

            return landmarks;
        }


        private static ArrayList<Landmark> readLandmarksArray(JsonReader reader) throws IOException {

            ArrayList<Landmark> landmarksRead = new ArrayList<>();

            reader.beginArray();
            while (reader.hasNext()) {
                landmarksRead.add(readLandmark(reader));
            }
            reader.endArray();

            return landmarksRead;
        }

        private static Landmark readLandmark(JsonReader jsonReader) throws IOException {



            double latitude = 0.0;
            double longitude = 0.0;
            int label = 0, bound = 0;
            Log.d("connect","inside if");
            //find all landmarks returned and store them
            Landmark newLandmark = new Landmark();
            jsonReader.beginObject();



            while (jsonReader.hasNext()) {


                String key = jsonReader.nextName();
                Log.d("json", key);
                switch (key) {
                    case "latitude":
                        latitude = jsonReader.nextDouble();
                        newLandmark.setLatitude(latitude);
                        Log.d("json", "latitude  " + latitude);
                        break;
                    case "longitude":
                        longitude = jsonReader.nextDouble();
                        newLandmark.setLongitude(longitude);
                        Log.d("json", "latitude  " + longitude);
                        break;
                    case "score":
                        label = jsonReader.nextInt();
                        newLandmark.setScore(label);
                        Log.d("json", "score  " + label);
                        break;
                    case "bound":
                        bound = jsonReader.nextInt();
                        newLandmark.setBound(bound);
                        Log.d("json", "bound  " + bound);
                        break;
                    default:
                        jsonReader.skipValue();
                        break;
                }


            }


            System.out.println(newLandmark);
            jsonReader.endObject();
            return newLandmark;
        }

    }

    private static ArrayList<Landmark> landmarks = new ArrayList<>();

    public static void setLandmarks(){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<ArrayList<Landmark>> newLandmarks = executor.submit(new RetrieveLandmarks());
        try {
            landmarks = newLandmarks.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static ArrayList<Landmark> getLandmarks(){
        return landmarks;
    }


    public static double geoDistance(LatLng pointA, LatLng pointB){
        GeodeticCalculator geoCalc = new GeodeticCalculator();

        Ellipsoid reference = Ellipsoid.WGS84;


        GlobalPosition posA = new GlobalPosition(pointA.latitude, pointA.longitude, 0.0);
        GlobalPosition posB = new GlobalPosition(pointB.latitude, pointB.longitude, 0.0);
        return geoCalc.calculateGeodeticCurve(reference, posB, posA).getEllipsoidalDistance();
    }

    public static LocationRequest initializeLocationRequest(boolean onlyOneUpdate){

        LocationRequest req = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(30 * 1000) // set a delay in the request to make sure the GPS
                //actually returns a location and not null
                .setFastestInterval(5 * 1000);  //we need one update ony

        if(onlyOneUpdate)
            req.setNumUpdates(1);

        return req;
    }



}
