package it.unipi.dii.aide.msss.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;

import com.google.maps.model.EncodedPolyline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import it.unipi.dii.aide.msss.myapplication.databinding.ActivityMapsBinding;
import it.unipi.dii.aide.msss.myapplication.entities.Landmark;

public class PathActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private ArrayList<Landmark> landmarks = new ArrayList<>();
    private FusedLocationProviderClient locationClient;

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
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
    }

    public void designRoute(){

        //start = getStartLocation();
        //end = get destinationLocation();

        //TODO: now null. We should then include the start and end destination

        // TODO: check if start and end of path are equals

        LatLng barcelona = new LatLng(41.385064,2.173403);
        mMap.addMarker(new MarkerOptions().position(barcelona).title("Marker in Barcelona"));

        LatLng madrid = new LatLng(40.416775,-3.70379);
        mMap.addMarker(new MarkerOptions().position(madrid).title("Marker in Madrid"));

        LatLng zaragoza = new LatLng(41.648823,-0.889085);

        //Execute Directions API request
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey("YOUR_API_KEY")
                .build();
        DirectionsApiRequest req = DirectionsApi.getDirections(context, "41.385064,2.173403", "40.416775,-3.70379");
        try {
            DirectionsResult res = req.await();

            //Loop through legs and steps to get encoded polylines of each step
            if (res.routes != null && res.routes.length > 0) {
                DirectionsRoute route = res.routes[0];

                if (route.legs !=null) {
                    for(int i=0; i<route.legs.length; i++) {
                        DirectionsLeg leg = route.legs[i];
                        if (leg.steps != null) {
                            for (int j=0; j<leg.steps.length;j++){
                                DirectionsStep step = leg.steps[j];
                                if (step.steps != null && step.steps.length >0) {
                                    for (int k=0; k<step.steps.length;k++){
                                        List<LatLng> path = new ArrayList<>();
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
                                                //check for each landmark if it is on the path
                                                for(Landmark landmark: landmarks){
                                                    if(Utils.geoDistance(coord, new LatLng(landmark.getLatitude(), landmark.getLongitude())) < 3){
                                                        // if not already counted, count the landmark adding it to the hashmap
                                                        if(!encounteredLandmarks.containsKey(landmark))
                                                            encounteredLandmarks.put(landmark, 1);
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

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(zaragoza, 6));
    }

}
