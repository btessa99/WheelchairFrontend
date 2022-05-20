package it.unipi.dii.aide.msss.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import it.unipi.dii.aide.msss.myapplication.databinding.ActivityMapsBinding;
import it.unipi.dii.aide.msss.myapplication.entities.Landmark;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private ArrayList<Landmark> landmarks = new ArrayList<>();
    private FusedLocationProviderClient locationClient;
    private LatLng location = new LatLng(43.724591,10.382981);
    private boolean permissionDenied = false;
    private LocationRequest locationRequest;
    private TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //this textView is used to show routing results only.
        textView = (TextView) findViewById(R.id.textView);
        textView.setVisibility(View.INVISIBLE);

        //initialize client for getting GPS
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        //get landmarks stored in memory
        landmarks = Utils.getLandmarks();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);


        //handle clicks on map
       mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {

                setCamera(latLng);

            }
        });

        placeLandmarks();
        setGpsLocation();

    }

    private void placeLandmarks(){

        System.out.println(landmarks);
        Log.d("connect", String.valueOf(landmarks.size()));

        //set Landmarks on the Map
        for(Landmark landmark: landmarks) {

            LatLng position = new LatLng(landmark.getLatitude(), landmark.getLongitude());
            // score computation
            double score = landmark.getScore();
            double bound = landmark.getBound();
            double finalScore = Math.abs(score / bound);

            // change the color of the landmark depending on the score:
            // below 0.7 orange, ahead 0.7 red
            BitmapDescriptor bitmapDescriptor;
            MarkerOptions options = new MarkerOptions().position(position);
            if(finalScore < 0.7)
                bitmapDescriptor = BitmapDescriptorFactory.defaultMarker((int) BitmapDescriptorFactory.HUE_ORANGE);
            else
                bitmapDescriptor = BitmapDescriptorFactory.defaultMarker((int) BitmapDescriptorFactory.HUE_RED);
            options.icon(bitmapDescriptor);
            mMap.addMarker(options);
        }


    }

    //gets currents GPS position

    @SuppressLint("MissingPermission")
    private void setGpsLocation(){

        // initialize parameters for location request
        locationRequest = Utils.initializeLocationRequest();
        Log.d("mytag",locationRequest.toString());


        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                //Location received
                Location currentLocation = locationResult.getLastLocation();
                LatLng currentCoordinates = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentCoordinates.latitude, currentCoordinates.longitude), 12.0f));
            }
        };

        //perform API call
        locationClient.requestLocationUpdates(locationRequest, locationCallback, null);

    }

    //change camera focus on map
    private void setCamera(LatLng currentPosition){

        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition));
    }


}