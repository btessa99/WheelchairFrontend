package it.unipi.dii.aide.msss.myapplication;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;

import com.google.maps.model.Duration;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.TravelMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import it.unipi.dii.aide.msss.myapplication.databinding.ActivityMapsBinding;
import it.unipi.dii.aide.msss.myapplication.entities.Landmark;

public class PathActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private ArrayList<Landmark> landmarks = new ArrayList<>();
    private FusedLocationProviderClient locationClient;
    private final String API_KEY = "AIzaSyDEM0FFaaLAtaux54IVvpSP8RlDdJ_q-SE";

    private LatLng coordinatesStart;



    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        locationClient = LocationServices.getFusedLocationProviderClient(this);

        //Retrieving of all landmarks
        landmarks = Utils.getLandmarks();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationClient = LocationServices.getFusedLocationProviderClient(this);


    }

    @SuppressLint("MissingPermission")
    public void setCurrentPosition(){


        Log.d("mytag","SON QUI");
        LocationRequest locationRequest = Utils.initializeLocationRequest();
        Log.d("mytag",locationRequest.toString());

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                //Location received
                Location currentLocation = locationResult.getLastLocation();
                coordinatesStart = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(coordinatesStart.latitude, coordinatesStart.longitude), 12.0f));
                mMap.addMarker(new MarkerOptions().position(coordinatesStart).title("Start"));
                System.out.println(coordinatesStart.latitude + " " + coordinatesStart.longitude);
            }
        };

        //perform API call
        locationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        setCurrentPosition();


        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) { //calculate route to the selected destination
                clearmap(); //clear map
                designRoute(latLng); //draw path
            }
        });
    }

    //remove polyline and markers on the map
    public void clearmap(){
            mMap.clear();
    }

    public void designRoute(LatLng coordinatesEnd){

        //get current position on GPS
        setCurrentPosition();
        System.out.println(coordinatesStart);

        //map the start of the path
        LatLng startPoint = null;
        //map the start of the path
        if(coordinatesStart != null)
            startPoint = coordinatesStart;
        else {
            startPoint = new LatLng(43.416667, 10.716667); //center of Pisa

        }
        mMap.addMarker(new MarkerOptions().position(startPoint).title("Start"));

        //map the end of the path
        mMap.addMarker(new MarkerOptions().position(coordinatesEnd).title("End"));


        //Execute Directions API request
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(API_KEY)
                .build();
        DirectionsApiRequest req = DirectionsApi.getDirections(context, startPoint.latitude+","+startPoint.longitude, coordinatesEnd.latitude+","+coordinatesEnd.longitude)
                                                .mode(TravelMode.WALKING); //inizialize request
        try {
            DirectionsResult res = req.await();
            System.out.println("son qui");
            System.out.println("len " + res.routes.length);
            //Loop through legs and steps to get encoded polylines of each step
            if (res.routes != null && res.routes.length > 0) {

                DirectionsRoute route = res.routes[0];
                if (route.legs !=null) {
                    System.out.println("len legs" + route.legs.length);
                    for(int i=0; i<route.legs.length; i++) {
                        DirectionsLeg leg = route.legs[i];
                        if (leg.steps != null) {
                            for (int j=0; j<leg.steps.length;j++){
                                DirectionsStep step = leg.steps[j];
                                if (step.steps != null && step.steps.length >0) {
                                    for (int k=0; k<step.steps.length;k++){
                                        List<LatLng> path = new ArrayList<>();
                                        //used to check whether a landmark was encountered or not
                                        HashMap<Landmark, Integer> encounteredLandmarks = new HashMap<>();
                                        double segmentDistance = 0;

                                        DirectionsStep step1 = step.steps[k];
                                        EncodedPolyline points1 = step1.polyline;

                                        if (points1 != null) {
                                            //Decode polyline and add points to list of route coordinates
                                            List<com.google.maps.model.LatLng> polylineCoords= points1.decodePath();
                                            LatLng first = new LatLng(polylineCoords.get(0).lat, polylineCoords.get(0).lng);
                                            LatLng last = new LatLng(polylineCoords.get(polylineCoords.size() -1).lat, polylineCoords.get(polylineCoords.size() -1).lng);
                                            segmentDistance = Utils.geoDistance(last, first);
                                            if(segmentDistance == 0){
                                                continue;
                                            }
                                            for (com.google.maps.model.LatLng c : polylineCoords) {
                                                LatLng coord = new LatLng(c.lat, c.lng);
                                                path.add(coord);

                                                for(Landmark landmark: landmarks){
                                                    if(!encounteredLandmarks.containsKey(landmark)) //check for each landmark if it is on the path
                                                    if(Utils.geoDistance(coord, new LatLng(landmark.getLatitude(), landmark.getLongitude())) < 3) { //check if landmark is close enough to that position
                                                        // if not already counted, count the landmark adding it to the hashmap
                                                        encounteredLandmarks.put(landmark, 1);
                                                        mMap.addMarker(new MarkerOptions().position(coord));
                                                    }
                                                }
                                            }
                                        }


                                        //Draw the polyline
                                        if (path.size() > 0) {
                                            // score: number of landmarks per kilometer
                                            double score = (double) encounteredLandmarks.size() / segmentDistance;
                                            PolylineOptions opts =new PolylineOptions().addAll(path).width(5);
                                            if(score < 1) // good score, green polyline
                                                opts.color(Color.GREEN);
                                            else if (score < 3) //medium score, orange polyline
                                                opts.color(Color.rgb(255, 165, 0));
                                            else
                                                opts.color(Color.RED);
                                            mMap.addPolyline(opts);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch(Exception ex) {
            Log.e("myMess", ex.getLocalizedMessage());
        }


        mMap.getUiSettings().setZoomControlsEnabled(true);


    }

}
