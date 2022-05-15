package it.unipi.dii.aide.msss.myapplication;

import android.annotation.SuppressLint;
import android.location.Location;
import android.util.JsonReader;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.gavaghan.geodesy.*;

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

        @Override
        public ArrayList<Landmark> call() throws Exception {
            return fetchLandmarks();
        }

        private ArrayList<Landmark> fetchLandmarks() {
            ArrayList<Landmark> landmarks = new ArrayList<>();
            try {
                URL serverEndpoint = new URL("http://127.0.0.1:12345/locations/inaccessible");
                HttpURLConnection connection = (HttpURLConnection) serverEndpoint.openConnection();
                connection.setRequestProperty("User-Agent", "my-rest-app-v0.1");


                if (connection.getResponseCode() == 200) {
                    InputStream responseBody = connection.getInputStream();
                    InputStreamReader responseBodyReader = new InputStreamReader(responseBody, "UTF-8");
                    JsonReader jsonReader = new JsonReader(responseBodyReader);
                    jsonReader.beginObject();
                    double latitude = 0.0;
                    double longitude = 0.0;
                    String label = "";
                    //find all landmarks returned and store them
                    while (jsonReader.hasNext()) {
                        String key = jsonReader.nextName();
                        switch (key) {
                            case "latitude":
                                latitude = jsonReader.nextDouble();
                                break;
                            case "longitude":
                                longitude = jsonReader.nextDouble();
                                break;
                            case "class":
                                label = jsonReader.nextString();
                                break;
                            default:
                                jsonReader.skipValue();
                                break;
                        }

                        if (latitude != 0.0 && longitude != 0.0 && !label.equals("")) {
                            Landmark newLandmark = new Landmark(latitude, longitude, label);
                            landmarks.add(newLandmark);
                        }
                    }
                } else {
                    // Error handling code goes here
                    System.out.println("server not reachable");
                }
            }catch (Exception e){e.printStackTrace();}

            return landmarks;
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


    public static void initializeMap(GoogleMap mMap, ArrayList<Landmark> landmarks,FusedLocationProviderClient client){


        //handle clicks on map
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                setCamera(latLng,mMap);
            }
        });

        placeLandmarks(landmarks,mMap);
        setGpsLocation(client,mMap);
    }


    private static void placeLandmarks(ArrayList<Landmark> landmarks,GoogleMap mMap){

        //set Landmarks on the Map
        for(Landmark landmark: landmarks) {
            LatLng position = new LatLng(landmark.getLatitude(), landmark.getLongitude());
            mMap.addMarker(new MarkerOptions().position(position).title(landmark.getLabel()));
        }


    }

    //gets currents GPS position
    @SuppressLint("MissingPermission")
    private static void setGpsLocation(FusedLocationProviderClient locationClient, GoogleMap mMap){

        locationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            LatLng position = new LatLng(location.getLatitude(),location.getLongitude());
                            setCamera(position,mMap);
                        }

                    }
                });
    }

    //change camera focus on map
    private static void setCamera(LatLng currentPosition,GoogleMap mMap){

        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition));
    }

    public static double geoDistance(LatLng pointA, LatLng pointB){
        GeodeticCalculator geoCalc = new GeodeticCalculator();

        Ellipsoid reference = Ellipsoid.WGS84;


        GlobalPosition posA = new GlobalPosition(pointA.latitude, pointA.longitude, 0.0);
        GlobalPosition posB = new GlobalPosition(pointB.latitude, pointB.longitude, 0.0);
        return geoCalc.calculateGeodeticCurve(reference, posB, posA).getEllipsoidalDistance();
    }
}
