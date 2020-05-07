package com.example.currentplacedetailsonmap;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;

/**
 * An activity that displays a map showing the place at the device's current location.
 */
public class MapsActivityCurrentPlace extends AppCompatActivity
        implements OnMapReadyCallback {

    private static final String TAG = MapsActivityCurrentPlace.class.getSimpleName();
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private int myId;

    // The entry point to the Places API.
    private PlacesClient mPlacesClient;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    // Used for selecting the current place.
    private static final int M_MAX_ENTRIES = 5;
    private String[] mLikelyPlaceNames;
    private String[] mLikelyPlaceAddresses;
    private List[] mLikelyPlaceAttributions;
    private LatLng[] mLikelyPlaceLatLngs;

    ////////////////////////////////////////////////////////////////////////////////
    //////////// Important Values for your localization and bluetooth //////////////
    ////////////////////////////////////////////////////////////////////////////////
    // Selected current place
    private LatLng markerLatLng;
    private String markerSnippet;
    private String markerPlaceName;

    private static final String IP = ""; //ENTER IP HERE (SAME AS SERVER)
    private static final int PORT = -1; //ENTER PORT HERE (SAME AS SERVER)

    private int age;
    private String sex;

    private Intent serviceIntent;
    private GetDeviceLocationService service;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    // New Bluetooth Devices Number
    private int btDevicesCount;
    ////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        sharedPref = getSharedPreferences("mypref", 0);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_maps);

        // Construct a PlacesClient
        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        mPlacesClient = Places.createClient(this);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Buttons
        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                getTrips(1);
                getDeviceLocation();
            }
        });

        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                getTrips(7);
                getDeviceLocation();
            }
        });

        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                getTrips(14);
                getDeviceLocation();
            }
        });

        Button button4 = findViewById(R.id.button4);
        button4.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                getTrips(30);
                getDeviceLocation();
            }
        });

        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        myId = sharedPref.getInt("myId", -1);
        if (myId == -1){
            initConnect();
        } else {
            scheduleAlarm();
        }
    }

    @Override
    protected void onDestroy(){
        stopService(serviceIntent);
        super.onDestroy();
    }

    private void drawCoords(double[][] coords, String[] times){
        if (coords == null){
            return;
        }
        mMap.clear();
        for (int i = 0; i < coords.length; i++){
            double[] coord = coords[i];
            mMap.addMarker(new MarkerOptions().position(new LatLng(coord[0], coord[1])).title(times[i]));
        }
    }

    private double[][] getTrips(int time_diff){
        try {
            Socket clientSocket = new Socket(IP, PORT);

            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            JSONObject obj = new JSONObject();
            obj.put("type", "query");
            obj.put("query", "get_trips");
            obj.put("id", myId);
            obj.put("time_diff", time_diff);
            String msg = obj.toString();
            out.writeUTF(msg);
            out.flush();

            String response = "";

            while (response.length() == 0 || response.charAt(response.length()-1) != 'Q'){
                if (in.ready()) {
                    response += in.readLine();
                    Log.d("RESPONSE_TESTING", "IN LOOP: " + response);
                }
            }

            response = response.substring(0, response.length() - 1);
            if (response.length() == 0){
                return null;
            }
            JSONObject ret = new JSONObject(response);
            if (!ret.getString("trips").equals("0")){
                drawCoords(cvrt2dDoubleArr(ret.getJSONArray("trips")), cvrt1dStringArr(ret.getJSONArray("times")));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String[] cvrt1dStringArr(JSONArray arr){
        try {
            String[] toReturn = new String[arr.length()];
            for (int i = 0; i < arr.length(); i++) {
                toReturn[i] = arr.getString(i);
            }
            return toReturn;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private double[][] cvrt2dDoubleArr(JSONArray coords){
        try{
            double[][] toReturn = new double[coords.length()][2];
            for (int i = 0; i < coords.length(); i++){
                double[] temp = new double[2];
                temp[0] = coords.getJSONArray(i).getDouble(0);
                temp[1] = coords.getJSONArray(i).getDouble(1);
                toReturn[i] = temp;
            }
            return toReturn;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void scheduleAlarm(){
        service = new GetDeviceLocationService();
        serviceIntent = new Intent(this, service.getClass());
        serviceIntent.putExtra("myId", myId);
        serviceIntent.putExtra("IP", IP);
        serviceIntent.putExtra("PORT", PORT);
        if (!serviceRunning(service.getClass())){
            startService(serviceIntent);
        }
    }

    private boolean serviceRunning(Class<?> serviceClass){
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())){
                return true;
            }
        }
        return false;
    }

    private void initConnect(){

        final AlertDialog.Builder sex_builder = new AlertDialog.Builder(this);
        sex_builder.setTitle("Sex");

        final EditText sex_input = new EditText(this);
        sex_input.setInputType(InputType.TYPE_CLASS_TEXT);
        sex_builder.setView(sex_input);
        sex_builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which){
                try {
                    sex = "" + sex_input.getText().toString().toLowerCase().charAt(0);
                } catch (Exception e){
                    sex = "";
                }
                if (!(sex.equals("f") || sex.equals("m"))){
                    sex = "";
                }
                try {
                    Socket clientSocket = new Socket(IP, PORT);

                    DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                    JSONObject obj = new JSONObject();
                    obj.put("type", "init");
                    obj.put("age", age);
                    obj.put("sex", sex);
                    String msg = obj.toString();
                    out.writeUTF(msg);
                    out.flush();

                    while (true){
                        if (in.ready()) {
                            String s =  in.readLine();
                            JSONObject ret = new JSONObject(s);
                            out.close();
                            in.close();
                            clientSocket.close();
                            myId = ret.getInt("id");
                            editor = sharedPref.edit();
                            editor.putInt("myId", myId);
                            editor.commit();
                            scheduleAlarm();
                            getDeviceLocation();
                            return;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        AlertDialog.Builder age_builder = new AlertDialog.Builder(this);
        age_builder.setTitle("Age");

        final EditText age_input = new EditText(this);
        age_input.setInputType(InputType.TYPE_CLASS_NUMBER);
        age_builder.setView(age_input);
        age_builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which){
                try {
                    age = Integer.parseInt(age_input.getText().toString());
                } catch (Exception e){
                    age = -1;
                }
                sex_builder.show();
            }
        });
        age_builder.show();
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Sets up the options menu.
     *
     * @param menu The options menu.
     * @return Boolean.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.current_place_menu, menu);
        return true;
    }

    /**
     * Handles a click on the menu option to get a place.
     *
     * @param item The menu item to handle.
     * @return Boolean.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("BUTTON_TESTING", "Pressed: " + item.getItemId());
        if (item.getItemId() == R.id.myScores) {
            //showCurrentPlace();
            Intent myScoresIntent = new Intent(this, MyScoresActivity.class);
            myScoresIntent.putExtra("IP", IP);
            myScoresIntent.putExtra("PORT", PORT);
            myScoresIntent.putExtra("myId", myId);
            startActivity(myScoresIntent);
        } else if (item.getItemId() == R.id.demoScores) {
            // Launch the DeviceListActivity to see devices and do scan
            //Intent serverIntent = new Intent(this, DeviceListActivity.class);
            //startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            Intent demoScoresIntent = new Intent(this, DemographicScoresActivtiy.class);
            demoScoresIntent.putExtra("IP", IP);
            demoScoresIntent.putExtra("PORT", PORT);
            demoScoresIntent.putExtra("myId", myId);
            startActivity(demoScoresIntent);
        }
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    ////////////////////////////////////////////////////////////////////////////
                    //////////////////////   WRITE YOUR CODE HERE ! ////////////////////////////
                    ////////////////////////////////////////////////////////////////////////////
                    // Example:
                    btDevicesCount = data.getExtras()
                            .getInt(DeviceListActivity.EXTRA_DEVICE_COUNT);
                    Log.d(TAG, "Device number:" + btDevicesCount);
                    // You can change the address to the number of certain type of devices, or
                    // other variables you want to use. Remember to change the corresponding
                    // name at DeviceListActivity.java.
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    ////////////////////////////////////////////////////////////////////////////
                    //////////////////////   WRITE YOUR CODE HERE ! ////////////////////////////
                    ////////////////////////////////////////////////////////////////////////////
                    // Example:
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                }
        }
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;


        // TEST
//        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                        (FrameLayout) findViewById(R.id.map), false);

                TextView title = infoWindow.findViewById(R.id.title);
                title.setText(marker.getTitle());

                TextView snippet = infoWindow.findViewById(R.id.snippet);
                snippet.setText(marker.getSnippet());

                return infoWindow;
            }
        });

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        if(myId != -1) {
            getDeviceLocation();
        }
    }

    private void sendLocationToServer(Location loc){
        try {
            Socket clientSocket = new Socket(IP, PORT);
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

            JSONObject obj = new JSONObject();
            obj.put("type", "insert");
            obj.put("id", myId);
            obj.put("lat", loc.getLatitude());
            obj.put("lon", loc.getLongitude());
            String msg = obj.toString();
            out.writeUTF(msg);
            out.flush();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private boolean foregrounded(){
        ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(appProcessInfo);
        return (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE);
    }

    /**
     * Gets the current location of the device, send to server, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            if (mLastKnownLocation != null) {
                                if (foregrounded()) {
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                            new LatLng(mLastKnownLocation.getLatitude(),
                                                    mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                                }
                                // Send location to server
                                sendLocationToServer(mLastKnownLocation);
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    /**
     * Prompts the user to select the current place from a list of likely places, and shows the
     * current place on the map - provided the user has granted location permission.
     */
    private void showCurrentPlace() {
        if (mMap == null) {
            return;
        }

        if (mLocationPermissionGranted) {
            // Use fields to define the data types to return.
            List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS,
                    Place.Field.LAT_LNG);

            // Use the builder to create a FindCurrentPlaceRequest.
            FindCurrentPlaceRequest request =
                    FindCurrentPlaceRequest.newInstance(placeFields);

            // Get the likely places - that is, the businesses and other points of interest that
            // are the best match for the device's current location.
            @SuppressWarnings("MissingPermission") final Task<FindCurrentPlaceResponse> placeResult =
                    mPlacesClient.findCurrentPlace(request);
            placeResult.addOnCompleteListener(new OnCompleteListener<FindCurrentPlaceResponse>() {
                @Override
                public void onComplete(@NonNull Task<FindCurrentPlaceResponse> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FindCurrentPlaceResponse likelyPlaces = task.getResult();

                        // Set the count, handling cases where less than 5 entries are returned.
                        int count;
                        if (likelyPlaces.getPlaceLikelihoods().size() < M_MAX_ENTRIES) {
                            count = likelyPlaces.getPlaceLikelihoods().size();
                        } else {
                            count = M_MAX_ENTRIES;
                        }

                        int i = 0;
                        mLikelyPlaceNames = new String[count];
                        mLikelyPlaceAddresses = new String[count];
                        mLikelyPlaceAttributions = new List[count];
                        mLikelyPlaceLatLngs = new LatLng[count];

                        for (PlaceLikelihood placeLikelihood : likelyPlaces.getPlaceLikelihoods()) {
                            // Build a list of likely places to show the user.
                            mLikelyPlaceNames[i] = placeLikelihood.getPlace().getName();
                            mLikelyPlaceAddresses[i] = placeLikelihood.getPlace().getAddress();
                            mLikelyPlaceAttributions[i] = placeLikelihood.getPlace()
                                    .getAttributions();
                            mLikelyPlaceLatLngs[i] = placeLikelihood.getPlace().getLatLng();

                            i++;
                            if (i > (count - 1)) {
                                break;
                            }
                        }

                        // Show a dialog offering the user the list of likely places, and add a
                        // marker at the selected place.
                        MapsActivityCurrentPlace.this.openPlacesDialog();
                    } else {
                        Log.e(TAG, "Exception: %s", task.getException());
                    }
                }
            });
        } else {
            // The user has not granted permission.
            Log.i(TAG, "The user did not grant location permission.");

            // Add a default marker, because the user hasn't selected a place.
            mMap.addMarker(new MarkerOptions()
                    .title(getString(R.string.default_info_title))
                    .position(mDefaultLocation)
                    .snippet(getString(R.string.default_info_snippet)));

            // Prompt the user for permission.
            getLocationPermission();
        }
    }

    /**
     * Displays a form allowing the user to select a place from a list of likely places.
     */
    private void openPlacesDialog() {
        // Ask the user to choose the place where they are now.
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // The "which" argument contains the position of the selected item.
                markerLatLng = mLikelyPlaceLatLngs[which];
                markerSnippet = mLikelyPlaceAddresses[which];
                markerPlaceName = mLikelyPlaceNames[which];

                if (mLikelyPlaceAttributions[which] != null) {
                    markerSnippet = markerSnippet + "\n" + mLikelyPlaceAttributions[which];
                }

                // Add a marker for the selected place, with an info window
                // showing information about that place.
                mMap.addMarker(new MarkerOptions()
                        .title(markerPlaceName)
                        .position(markerLatLng)
                        .snippet(markerSnippet));

                // Position the map's camera at the location of the marker.
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerLatLng,
                        DEFAULT_ZOOM));
            }
        };

        // Display the dialog.
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.pick_place)
                .setItems(mLikelyPlaceNames, listener)
                .show();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }
}
