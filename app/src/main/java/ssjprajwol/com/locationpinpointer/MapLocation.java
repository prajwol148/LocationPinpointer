package ssjprajwol.com.locationpinpointer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapLocation extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mgoogleMap; // This is the main class of the Google Maps SDK for Android and is the entry point for all methods related to the map.
    private FusedLocationProviderClient fusedLocationProviderClient; //This very class provides the current location.
    private Location location; //This represents the geographical location.
    private LocationCallback locationCallback; //This class will update the location of user.
    private View mapView;

    private TextView longitude;
    private TextView latitude;
    private TextView country;
    private TextView address;
    private TextView city;
    private TextView postal;


    private final float DEFAULT_ZOOM = 18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_location);

        longitude = findViewById(R.id.longitude);
        country = findViewById(R.id.country);
        city = findViewById(R.id.city);
        address = findViewById(R.id.address);
        latitude = findViewById(R.id.latitude);
        postal=findViewById(R.id.postal);

        //This is used to load the map fragment I setup in content_map_location (map window).
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(this); //getMapAsync sets a callback object which will be triggered when the GoogleMap finishes loading and the instance is ready to be used.
        mapView = supportMapFragment.getView();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) { // This is a callback function which will be called when map is loaded and ready to be used.
        mgoogleMap = googleMap;
        mgoogleMap.setMyLocationEnabled(true);
        //This enables location layer when true. The layer continuously draws an indication of a user's current location.
        // Here, I got a warning which is why @SuppressLint is used above. This reminds you to take location permission. I have already taken permission so I supressed the warning.
        mgoogleMap.getUiSettings().setMyLocationButtonEnabled(true); //This displays gps button which on clicking shows the user's location.
        //getUiSettings: Gets the user interface settings for the map. UiSettings: Settings for the user interface of a GoogleMap



        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapLocation.this); //This initializes usedLocationProviderClient

        //The code below is used just to change the position of location button according to own preference
        /*if(mapView != null && mapView.findViewById(Integer.parseInt("1")) != null){
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 40, 180);
        }*/

        //Now before updating the location of user in map, I had to make sure that GPS is on
        // else the function would not work. So, confirming if GPS is on. And asking user to
        // turn it on if it is off.
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(MapLocation.this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());
        //This functions actually check the location setting and determine if any changes has to be made.

        task.addOnSuccessListener(MapLocation.this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                getDeviceLocation();
            }
        });
        task.addOnFailureListener(MapLocation.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if(e instanceof ResolvableApiException){
                    ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                    try {
                        resolvableApiException.startResolutionForResult(MapLocation.this, 41);
                    } catch (IntentSender.SendIntentException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

    }

    //Now, to check if the user accept or deny the request to enable the location service
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 51) {  // if user accept to enable gps, get location of user
            if (resultCode == RESULT_OK) {
                getDeviceLocation();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void getDeviceLocation() {
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            //Here, system will ask FusedLocationProviderClient to fetch the last known location of the user and will add CompletionListener to it.

            @Override
            public void onComplete(@NonNull Task<Location> task) {

                if(task.isSuccessful()){
                    location = task.getResult();
                    //even if the task is successful, the last location can be null so for that case:
                    if(location != null){
                        mgoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM));



                        try {
                            //Initialize geoCoder to get lati. and longi. value to put for location description
                            Geocoder geocoder = new Geocoder(MapLocation.this, Locale.getDefault());
                            List<Address> addresses = geocoder.getFromLocation(location
                                    .getLatitude(),location.getLongitude(),1);


                            longitude.setText(Html.fromHtml(" <font color='#006a4e'><b>Longitude: </b><br></font>"+addresses.get(0).getLongitude()));
                            latitude.setText(Html.fromHtml(" <font color='#006a4e'><b>Latitude: : </b><br></font>"+addresses.get(0).getLatitude()));
                            country.setText(Html.fromHtml(" <font color='#006a4e'><b>Country: : </b><br></font>"+addresses.get(0).getCountryName()));
                            address.setText(Html.fromHtml(" <font color='#006a4e'><b>Address: : </b><br></font>"+addresses.get(0).getAddressLine(0)));
                            city.setText(Html.fromHtml(" <font color='#006a4e'><b>City: </b><br></font>"+addresses.get(0).getLocality()));
                            postal.setText(Html.fromHtml(" <font color='#006a4e'><b>Postal Code: </b><br></font>"+addresses.get(0).getPostalCode()));


                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    }
                    else {
                        final LocationRequest locationRequest = LocationRequest.create();
                        locationRequest.setInterval(10000); //interval for location updates
                        locationRequest.setFastestInterval(5000);  //Set the desired interval for active location updates, in milliseconds.
                        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //PRIORITY_HIGH_ACCURACY uses GPS such that location is precise
                        locationCallback = new LocationCallback(){
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                super.onLocationResult(locationResult);
                                if(locationResult == null){
                                    return;
                                }
                                location = locationResult.getLastLocation();
                                mgoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                                        location.getLongitude()), DEFAULT_ZOOM)); //setting the view to retrived long. and lat. values
                                fusedLocationProviderClient.removeLocationUpdates(locationCallback);// stopping the app to retrieve location after the location update is successfully provided

                                // What is being done here is that, first the last location is tried to get retrieved from fusedlocationProvider.
                                //but if provides null value then we request onLocataionResult to update the location and then return whatever value we get
                            }
                        };
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    }
                }
                else {
                    Toast.makeText(MapLocation.this, "Sorry, the app is unable to retrieve your location. Try checking internet and location permission again", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}