package it.unipi.dii.aide.msss.myapplication;


import android.util.JsonReader;
import android.util.Log;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;

import org.gavaghan.geodesy.*;

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

        private final String url = "https://3030-2-198-79-124.eu.ngrok.io/locations/inaccessible/scores";


        @Override
        public ArrayList<Landmark> call() throws Exception {
            return fetchLandmarks();
        }

        private ArrayList<Landmark> fetchLandmarks() {
            ArrayList<Landmark> landmarks = new ArrayList<>();
            try {

                //perform HTTP request
                URL serverEndpoint = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) serverEndpoint.openConnection();
                connection.setRequestProperty("User-Agent", "my-rest-app-v0.1");


                //connection successful
                if (connection.getResponseCode() == 200) {

                    //get response body (it's a JSON array)
                    InputStream responseBody = connection.getInputStream();
                    InputStreamReader responseBodyReader = new InputStreamReader(responseBody, "UTF-8");

                    JsonReader jsonReader = new JsonReader(responseBodyReader);
                    jsonReader.setLenient(true);

                    //parse landmark in JSON array
                    landmarks = readLandmarksArray(jsonReader);

                } else {
                    // connection failed
                    System.out.println("server not reachable");
                }
            }catch (Exception e){e.printStackTrace();}

            return landmarks;
        }



        private static ArrayList<Landmark> readLandmarksArray(JsonReader reader) throws IOException {

            ArrayList<Landmark> landmarksRead = new ArrayList<>();

            reader.beginArray();

            while (reader.hasNext()) { //parses every object of array
                landmarksRead.add(readLandmark(reader));
            }
            reader.endArray();

            return landmarksRead;
        }

        private static Landmark readLandmark(JsonReader jsonReader) throws IOException {

            double latitude = 0.0;
            double longitude = 0.0;
            int label = 0, bound = 0;

            //find all landmarks returned and store them
            Landmark newLandmark = new Landmark();
            jsonReader.beginObject();

            while (jsonReader.hasNext()) {

                //read every single field and build new landmark object

                String key = jsonReader.nextName();
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
            jsonReader.endObject();
            return newLandmark;
        }

    }

    private static ArrayList<Landmark> landmarks = new ArrayList<>();

    //gets landmark in background
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


    //calculates geodesic distance between two coordinated and return distance in meters
    public static double geoDistance(LatLng pointA, LatLng pointB){
        GeodeticCalculator geoCalc = new GeodeticCalculator();

        Ellipsoid reference = Ellipsoid.WGS84;


        GlobalPosition posA = new GlobalPosition(pointA.latitude, pointA.longitude, 0.0);
        GlobalPosition posB = new GlobalPosition(pointB.latitude, pointB.longitude, 0.0);
        return geoCalc.calculateGeodeticCurve(reference, posB, posA).getEllipsoidalDistance();
    }

    //initializes client for GPS location requests
    public static LocationRequest initializeLocationRequest(){

        LocationRequest req = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(30 * 1000) // set a delay in the request to make sure the GPS
                //actually returns a location and not null
                .setFastestInterval(5 * 1000);  //we need one update ony

        return req;
    }



}
