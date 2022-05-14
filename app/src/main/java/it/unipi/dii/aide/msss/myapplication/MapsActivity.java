package it.unipi.dii.aide.msss.myapplication;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.util.JsonReader;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import it.unipi.dii.aide.msss.myapplication.databinding.ActivityMapsBinding;
import it.unipi.dii.aide.msss.myapplication.entities.Landmark;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private ArrayList<Landmark> landmarks = new ArrayList<>();
    private FusedLocationProviderClient locationClient;
    private final String serverAddress = "http://127.0.0.1:12345/locations/inaccessible";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        locationClient = LocationServices.getFusedLocationProviderClient(this);
        // HTTP connection for retrieving the landmarks
        getLandmarks();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void getLandmarks(){
        try {

            //initialize server request
            URL serverEndpoint = new URL(serverAddress);
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
                while(jsonReader.hasNext()){
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

                    if(latitude != 0.0 && longitude != 0.0 && !label.equals("")){
                        Landmark newLandmark = new Landmark(latitude,longitude,label);
                        landmarks.add(newLandmark);
                        latitude = 0.0;
                        longitude = 0.0;
                        label = "";
                    }
                }
            } else {
                // Error handling code goes here
                System.out.println("server not reachable");
            }
        } catch(Exception e){
            System.err.println("Il server fa caa");
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

       // Utils.initializeMap(mMap,landmarks,locationClient);

        //handle clicks on map
       mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {

                setCamera(latLng);

            }
        });

        setLandmarks();
        setGpsLocation();

    }

    private void setLandmarks(){

        //set Landmarks on the Map
        for(Landmark landmark: landmarks) {
            LatLng position = new LatLng(landmark.getLatitude(), landmark.getLongitude());
            mMap.addMarker(new MarkerOptions().position(position).title(landmark.getLabel()));
        }


    }

    //gets currents GPS position
    @SuppressLint("MissingPermission")
    private void setGpsLocation(){

        locationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            LatLng position = new LatLng(location.getLatitude(),location.getLongitude());
                            setCamera(position);
                        }

                    }
                });
    }

    //change camera focus on map
    private void setCamera(LatLng currentPosition){

        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition));
    }
}