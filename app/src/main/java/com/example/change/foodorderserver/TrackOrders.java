package com.example.change.foodorderserver;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.change.foodorderserver.Common.Common;
import com.example.change.foodorderserver.Common.DirectionJsonParser;
import com.example.change.foodorderserver.Remote.IGeoCoordinates;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrackOrders extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private final static int LOCATION_REQUEST = 1001;

    private Location lastLocation;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private static int UPDATE_INTERVAL = 1000;
    private static int FASTEST_INTERVAL = 5000;
    private static int DISPLACEMENT = 10;
    private static final String TAG = "TrackOrders";

    private IGeoCoordinates mService;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_orders);

        mService = Common.getGeoCodeService();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLocation();
        } else {
            if (checkPlayServices()) {
                buildGoogleApiClent();
                createLocationRequest();
            }
        }
        displayLocation();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void requestPermissionLocation() {
        ActivityCompat.requestPermissions(this, new String[]
                {Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPlayServices()) {
                        buildGoogleApiClent();
                        createLocationRequest();

                        displayLocation();

                    }
                }
        }
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLocation();
        } else {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (lastLocation != null) {
                double lat = lastLocation.getLatitude();
                double longt = lastLocation.getLongitude();


                LatLng shipLocation = new LatLng(lat, longt);
                mMap.addMarker(new MarkerOptions().position(shipLocation).title("Shipping Location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(shipLocation));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));

                //Marker For Specific Order and draw the route

                addRequestMarkerRoute(shipLocation,Common.currentRequest.getAddress());


            } else {
                //Toast.makeText(this, "We're unable to detect your Location", Toast.LENGTH_SHORT).show();

            }
        }


    }

    private void addRequestMarkerRoute(final LatLng shipLocation, String address) {

        mService.getGeoCode(address,"false","Your API Key Here").enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                try {
                    Log.i(TAG, "onResponse: This is getGeo" + response.body());
                    JSONObject jsonObject = new JSONObject(response.body().toString());
                    String lat = ((JSONArray)jsonObject.get("results"))
                            .getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONObject("location")
                            .get("lat").toString();


                    String lng = ((JSONArray)jsonObject.get("results"))
                            .getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONObject("location")
                            .get("lng").toString();

                    LatLng orderLocation = new LatLng(Double.parseDouble(lat),Double.parseDouble(lng));
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.package_box);

                    bitmap = Common.scaleBitmap(bitmap,70,70);

                    MarkerOptions marker = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                            .title("Order of " + Common.currentRequest.getEmail())
                            .position(orderLocation);
                    mMap.addMarker(marker);

                    mService.getDirections(shipLocation.latitude+ ","+shipLocation.longitude,
                            orderLocation.latitude+","+orderLocation.longitude,"false","AIzaSyB95eskfrIUWbvkxqIxnhWWIsjsQpUysUw")
                            .enqueue(new Callback<String>() {
                                @Override
                                public void onResponse(Call<String> call, Response<String> response) {
                                    Log.i(TAG, "onResponse: " + response.body());
                                    new ParserTask().execute(response.body().toString());

                                }

                                @Override
                                public void onFailure(Call<String> call, Throwable t) {

                                }
                            });




                }
                catch (JSONException e){

                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });

    }

    private void createLocationRequest() {
        mLocationRequest =  LocationRequest.create();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);

    }

    protected synchronized void buildGoogleApiClent() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mGoogleApiClient.connect();
    }


    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(this, "Turn on the Location Service", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
        displayLocation();

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        updateLocation();

    }

    private void updateLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,  this);

    }


    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }

    private  class ParserTask extends AsyncTask<String,Integer,List<List<HashMap<String,String>>>> {
        ProgressDialog dialog = new ProgressDialog(TrackOrders.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage("Hang On...");
            dialog.show();
        }

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject object;
            List<List<HashMap<String,String>>> routes = null;
            try {
                object = new JSONObject(strings[0]);
                DirectionJsonParser parser = new DirectionJsonParser();
                routes = parser.parse(object);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            dialog.dismiss();

            ArrayList points = null;
            PolylineOptions polylineOptions = null;

            for (int i=0;i<lists.size();i++){
                 points = new ArrayList();
                 polylineOptions = new PolylineOptions();

                 List<HashMap<String,String>> path = lists.get(i);

                 for (int j=0;j<path.size();j++){
                     HashMap<String,String> point = path.get(j);

                     double lat = Double.parseDouble(point.get("lat"));
                     double lng = Double.parseDouble(point.get("lng"));

                     LatLng pos = new LatLng(lat,lng);

                     points.add(pos);
                 }

                 polylineOptions.addAll(points);
                 polylineOptions.width(12);
                 polylineOptions.color(Color.BLUE);
                 polylineOptions.geodesic(true);

            }
            mMap.addPolyline(polylineOptions);


        }
    }
}
