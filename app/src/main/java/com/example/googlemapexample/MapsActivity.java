package com.example.googlemapexample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.googlemapexample.placesearch.PlaceSearchAPI;
import com.example.googlemapexample.placesearch.PlaceSearchResponseBody;
import com.example.googlemapexample.placesearch.Result;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener{

    private static final String TAG = MapsActivity.class.getSimpleName();
    private GoogleMap mMap;
    private FusedLocationProviderClient providerClient;
    private LatLng myLocation = null;
    private MyConnectivityReceiver receiver;
    private Spinner placeSP;


    private String placeType="restaurant";
    private int radius=1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        receiver = new MyConnectivityReceiver();
        providerClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        placeSP=findViewById(R.id.placeSP);




        placeSP.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mMap.clear();
                getDeviceCurrentLocation();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // setOnItemSelectedListener
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
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
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if(isLocationPermissionGranted()){
            mMap.setMyLocationEnabled(true);
        }
        mMap.setOnMapLongClickListener(this);
        //mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        //Toast.makeText(this, "Fist Call fdjkdhdfjkghdfkjgh", Toast.LENGTH_SHORT).show();


        getDeviceCurrentLocation();
        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sydney, 15f));*/
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 15f));//zoom level - 1 to 20
    }

    private boolean isLocationPermissionGranted(){
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 111);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 111 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            mMap.setMyLocationEnabled(true);
        }
    }

    private void getDeviceCurrentLocation(){
        providerClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if(location == null){
                            return;
                        }


                        myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f));

                        placeType= String.valueOf(placeSP.getSelectedItem());

                        //placeType= String.valueOf(placeSP.getSelectedItem());

                        int radius=1500;

                        getNearbyPlaces(placeType,radius);
                    }
                });
    }



    @Override
    public void onMapLongClick(LatLng latLng) {
        mMap.addMarker(new MarkerOptions().position(latLng).title("Unknown"));
        Log.e(TAG, "onMapLongClick: "+latLng.latitude+","+latLng.longitude);
    }


    private void getNearbyPlaces(String placeName,int radius){

        //Toast.makeText(this, "Call After click", Toast.LENGTH_SHORT).show();
        //String placeName= String.valueOf(placeSP.getSelectedItem());

        //placeName="restaurant";

        //int radius=1500;

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/maps/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        String endurl =
                String.format("place/nearbysearch/json?location=%f,%f&radius=%d&type=%s&key=%s",
                        myLocation.latitude, myLocation.longitude,radius,placeName, getString(R.string.place_api_key));


        final PlaceSearchAPI searchAPI = retrofit.create(PlaceSearchAPI.class);
        searchAPI.getPlaces(endurl)
                .enqueue(new Callback<PlaceSearchResponseBody>() {
                    @Override
                    public void onResponse(Call<PlaceSearchResponseBody> call, Response<PlaceSearchResponseBody> response) {
                        if (response.isSuccessful()){
                            final PlaceSearchResponseBody responseBody =
                                    response.body();
                            List<Result> placeList = responseBody.getResults();
                            putPlacesOnMap(placeList);

                        }
                    }

                    @Override
                    public void onFailure(Call<PlaceSearchResponseBody> call, Throwable t) {
                        Log.e(TAG, "onFailure: "+t.getLocalizedMessage());
                    }
                });
    }

    private void putPlacesOnMap(List<Result> placeList) {

        //mMap.addMarker(null);

        for (Result r : placeList){
            String name = r.getName();
            double latitude = r.getGeometry().getLocation().getLat();
            double longitude = r.getGeometry().getLocation().getLng();
            LatLng place = new LatLng(latitude, longitude);
            mMap.addMarker(new MarkerOptions().position(place).title(name));
        }
    }


    private class MyConnectivityReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();
            if (info != null && info.isConnected()){
                Toast.makeText(context, "connected", Toast.LENGTH_SHORT).show();
                if (myLocation != null){
                    getNearbyPlaces(placeType,radius);
                }
            }else{
                Toast.makeText(context, "You are not connected to internet", Toast.LENGTH_LONG).show();
            }
        }
    }

}
