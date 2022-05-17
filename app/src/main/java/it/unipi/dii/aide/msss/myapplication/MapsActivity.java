package it.unipi.dii.aide.msss.myapplication;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
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
    private LatLng location = new LatLng(43.724591,10.382981);
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean permissionDenied = false;
    private LocationRequest locationRequest;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //initialize client for getting GPS
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        // HTTP connection for retrieving the landmarks
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

        //set Landmarks on the Map
        for(Landmark landmark: landmarks) {
            LatLng position = new LatLng(landmark.getLatitude(), landmark.getLongitude());
            mMap.addMarker(new MarkerOptions().position(position).title(landmark.getLabel()));
        }


    }

    //gets currents GPS position

    @SuppressLint("MissingPermission")
    private void setGpsLocation(){

        // initialize parameters for location request

        Log.d("mytag","SON QUI");
        locationRequest = Utils.initializeLocationRequest();


        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                //Location received
                Log.d("mytag",locationResult.getLastLocation().toString());
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