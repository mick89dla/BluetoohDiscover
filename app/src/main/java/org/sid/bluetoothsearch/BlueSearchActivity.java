package org.sid.bluetoothsearch;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.sid.bluetoothsearch.bluetoothviewmodel.BluetoothViewModel;
import org.sid.bluetoothsearch.datarepository.Bluetooth;
import org.sid.bluetoothsearch.elements.Divider;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class BlueSearchActivity extends FragmentActivity
        implements OnMapReadyCallback,
        BlAdapter.OnItemClickListener,
        GoogleMap.OnInfoWindowClickListener {

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 100;
    private static final int REQUEST_CHECK_SETTINGS = 101;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 3456;
    private static final String ERROR_MSG = "Google Play services are unavailable.";

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private LocationRequest mLocationRequest;

    private SettingsClient mSettingsClient;
    private LocationSettingsRequest mLocationSettingsRequest;

    private GoogleMap mMap;


    private List<Marker> mMarkers = new ArrayList<>();
    private boolean mAllowLocationUpdates = true;
    private boolean mAllowBluetoothDiscovery = true;
    private boolean mSwapTheme = false;

    private static final int ENABLED_BLUETOOTH = 102;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private List<Bluetooth> deviceList = new ArrayList<>();
    private static boolean discoverabilityRegistered = false;

    private BlAdapter mAdapter;

    private final BroadcastReceiver discoveryMonitorReceiver = new BluetoothDiscoveryReceiver();

    private Double cLattitude;
    private Double cLongitude;
    private Double fixedLat;
    private Double fixedlong;
    private int added_device_nbr = 0;
    private List<BluetoothDevice> markersList = new ArrayList<>();
    private List<String> addedMACS = new ArrayList<>();
    private boolean isReady;

    private BluetoothViewModel mBluetoothViewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // keep the screen turned on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_bluesearch);

        //get a reference to the layout
        LinearLayout parentLayout = findViewById(R.id.blueLayout);

        // get a reference to the RecyclerView
        RecyclerView deviceListRecycler  = findViewById(R.id.deviceListRecycler);
        deviceListRecycler.setBackgroundColor(Color.WHITE);

        // Create RecyclerView's adapter
        mAdapter = new BlAdapter(this, this, parentLayout);
        deviceListRecycler.setAdapter(mAdapter);

        // Recycler should display items in a vertical list
        deviceListRecycler.setLayoutManager(new LinearLayoutManager(this));

        // To improve performance cause changes
        // in content do not change the layout size of the RecyclerView
        deviceListRecycler.setHasFixedSize(true);

        // attach a custom ItemDecoration to draw dividers between list items
        deviceListRecycler.addItemDecoration(new Divider(this));

        // Get a new or existing ViewModel from the ViewModelProvider.
        mBluetoothViewModel = ViewModelProviders.of(this).get(BluetoothViewModel.class);

        // Add an observer on the LiveData returned by getAllBluetooth.
        // The onChanged() method fires when the observed data changes and the activity is
        // in the foreground i.e when a new device is found
        mBluetoothViewModel.getAllBluetooth().observe(this, bluetoothList -> {
            deviceList.addAll(bluetoothList);

            for (Bluetooth bd: bluetoothList) {
                LatLng latLng1 = new LatLng(bd.getbLatitude(), bd.getbLongitute());
                String markerTitle = bd.getbName();
                String markerSnipet = bd.getMajorClass();

                mMarkers.add(
                        mMap.addMarker(new MarkerOptions()
                                .position(latLng1)
                                .title(markerTitle)
                                .snippet(markerSnipet)
                                .draggable(true)
                        )
                );
                addedMACS.add(bd.getbAddress());
            }

            // Update the cached copy of the words in the adapter.
            mAdapter.swapDataset(bluetoothList);
        });

        // Get a reference to the Swap theme button
        Button swapThemeBtn = findViewById(R.id.btn_swap_theme);
        swapThemeBtn.setOnClickListener(v -> {
           // LogWrapper.d("Swap Button Clicked...");
            if(mMap != null) {
                mSwapTheme = !mSwapTheme;
                boolean success;
                try {
                    if(mSwapTheme) {
                        //LogWrapper.d("Swapping theme to Dark...");
                        success =
                                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(BlueSearchActivity.this
                                        , R.raw.map_style_black));
                        // Change the items to white on dark
                        for (int iter = 0; iter < deviceListRecycler.getLayoutManager().getChildCount(); iter++) {
                            View view = deviceListRecycler.getLayoutManager().getChildAt(iter);
                            TextView textView = view.findViewById(R.id.device_tv);
                            textView.setTextColor(Color.WHITE);
                            view.setBackgroundColor(Color.rgb(73, 82, 89));
                        }
                        deviceListRecycler.setBackgroundColor(Color.BLACK);
                        mAdapter.isDarkTheme = true;
                    } else {
                        //LogWrapper.d("Swapping theme to Standard...");
                        success =
                                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(BlueSearchActivity.this
                                        , R.raw.map_style_standard));
                        // Change the items to white on dark
                        for (int iter = 0; iter < deviceListRecycler.getLayoutManager().getChildCount(); iter++) {
                            View view = deviceListRecycler.getLayoutManager().getChildAt(iter);
                            TextView textView = view.findViewById(R.id.device_tv);
                            textView.setTextColor(Color.BLACK);
                            view.setBackgroundColor(Color.rgb(223, 221, 226));
                        }
                        deviceListRecycler.setBackgroundColor(Color.WHITE);
                        mAdapter.isDarkTheme = false;
                    }

                    if(!success) {
                        //LogWrapper.e("Style parsing failed");
                    }
                }catch ( Resources.NotFoundException ex) {
                    //LogWrapper.e("Can't find style", ex);
                }

            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        Objects.requireNonNull(mapFragment).getMapAsync(this);

        //LogWrapper.d("Checking if Google Play services are available...");
        checkPlayServices();

        //LogWrapper.d("Initializing Fused Provider client...");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects
        createLocationRequest();
        createLocationCallback();
        buildLocationSettingsRequest();
        //LogWrapper.d("Exit");
    }


    @Override
    public void onFavoriteIconClicked(Bluetooth device, ImageView icon) {
        if(device.isFavorited) {
            icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite));
        } else {
            icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_unfavorite));
        }
        mBluetoothViewModel.update(device);
    }

    @Override
    public void onShareIconClicked(Bluetooth device) {
        Intent shareintent = new Intent(Intent.ACTION_SEND);
        shareintent.setType("text/plain");
        String shareBody = String.format("Name: %s\nMAC: %s\n%s\n%s\n%s\n----------\n",
                device.getbName(), device.getbAddress(), device.getbType(), device.getbBounded(), device.getMajorClass());
        shareintent.putExtra(Intent.EXTRA_SUBJECT, "Shared Buetooth device");
        shareintent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareintent, "Share avec"));

    }

    /**
     * Sets up the location request. This app uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     *
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a slow update
     * interval (10 min), the Fused Location Provider API returns location updates that are
     * accurate to within a city block (approximately 100 meters)
     *
     */
    private void createLocationRequest() {

        //LogWrapper.d("Setting up Location request... ");
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is inexact. You
        // may not receive updates at all if no location sources are available, or you may
        // receive them slower than requested. You may also receive update faster than requested
        // if other application are requesting location at a faster interval

        //UPDATE_INTERVAL_IN_MILLISECONDS + "seconds");
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive update faster than this value
        //LogWrapper.d("Settings fastest update rate to -> " +
                //FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS + " seconds");
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        //LogWrapper.d("Settings priority to : PRIORITY_HIGH_ACCURACY");
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //LogWrapper.d("Exit");
    }



    /**
     * Creates a callback for receiving location events
     */
    private void createLocationCallback() {

       // LogWrapper.d("Setting up Location callback...");
        mLocationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {


                super.onLocationResult(locationResult);
                if(locationResult == null) return;

                mCurrentLocation = locationResult.getLastLocation();
                cLattitude = mCurrentLocation.getLatitude();
                cLongitude = mCurrentLocation.getLongitude();
                LatLng latLng = new LatLng(cLattitude, cLongitude);
                Calendar c = Calendar.getInstance();
                String dateTime =
                        DateFormat.format("EEE, MMM d ''yy, HH:mm:ss", c.getTime()).toString();

            //    LogWrapper.d("Current Location -> " + latLng + " Last update time -> " + dateTime);

                if(mMap == null) return;

                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                fixedLat =  (fixedLat == null) ? cLattitude: fixedLat;
                fixedlong =  (fixedlong == null) ? cLongitude: fixedlong;
                isReady = true;
                putMarkersOnMap();
            }
        };

        //LogWrapper.d("Exit");
    }

    private void putMarkersOnMap() {
        Double earthRadius = 6371000.0;
        for (BluetoothDevice bd : markersList) {
            if (!addedMACS.contains(bd.getAddress())) {
                addedMACS.add(bd.getAddress());
                Double deltax = 0.0;
                Double deltay = 0.0;
                /**
                 We add markers in concentric circles of 8 devices with radius 10, 20, 30... meters
                 */
                Double radialdelx = Math.toDegrees( 10.0 * ( Math.floor(added_device_nbr/ 8) + 1) / ( earthRadius * Math.cos(Math.toRadians(fixedLat))) );
                Double radialdely = Math.toDegrees( 10.0 * ( Math.floor(added_device_nbr/ 8) + 1) / earthRadius );
                //LogWrapper.d(String.format("current distance: %g",radialdelx));

                int cycler = added_device_nbr % 8;
                switch (cycler) {
                    case 0:
                        deltax = radialdelx;
                        break;
                    case 1:
                        deltax = radialdelx * Math.sqrt(2)/2;
                        deltay = radialdely * Math.sqrt(2)/2;
                        break;
                    case 2:
                        deltay = radialdely;
                        break;
                    case 3:
                        deltax = -radialdelx * Math.sqrt(2)/2;
                        deltay = radialdely * Math.sqrt(2)/2;
                        break;
                    case 4:
                        deltax = -radialdelx;
                        break;
                    case 5:
                        deltax = -radialdelx * Math.sqrt(2)/2;
                        deltay = -radialdely * Math.sqrt(2)/2;
                        break;
                    case 6:
                        deltay = -radialdely;
                        break;
                    case 7:
                        deltax = radialdelx * Math.sqrt(2)/2;
                        deltay = -radialdely * Math.sqrt(2)/2;
                        break;
                    default:
                        //LogWrapper.wtf("should not be here");
                        break;
                }

                LatLng latLng1 = new LatLng(fixedLat + deltay, fixedlong + deltax);
                String markerTitle = (bd.getName() == null) ? "None" : bd.getName();
                String markerSnipet = getMajorDeviceClass(bd);
                //LogWrapper.d("putting the marker on the map");

                mMarkers.add(
                        mMap.addMarker(new MarkerOptions()
                                .position(latLng1)
                                .title(markerTitle)
                                .snippet(markerSnipet)
                                .draggable(true)
                        )
                );

                Bluetooth bluetooth = new Bluetooth(
                        (bd.getName() == null) ? "None" : bd.getName(),
                        bd.getAddress(),
                        getDeviceType(bd),
                        getBondedState(bd),
                        getMajorDeviceClass(bd),
                        latLng1.latitude,
                        latLng1.latitude, false);

                mBluetoothViewModel.insert(bluetooth);


                added_device_nbr ++;
               // LogWrapper.d(String.format("Added %d devices", added_device_nbr));
            }
        }
    }

    private void buildLocationSettingsRequest() {
        mLocationSettingsRequest =
                new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest).build();
    }

    /**
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user
     * will be prompted to install it inside the SupportMapFragment.
     * This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        //LogWrapper.d("Map is ready...making initial setup");
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.animateCamera(CameraUpdateFactory.zoomTo(19F));
        mMap.setBuildingsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.setMyLocationEnabled(true);
        mMap.setOnInfoWindowClickListener(this);

        mMap.setOnMarkerClickListener(marker -> {
           // LogWrapper.d("Clicked Marker -> " + marker);

            // Return false to display the Info Window.
            return  false;
        });

        //LogWrapper.d("Exit");
    }

    @Override
    protected void onStart() {
        super.onStart();
        //LogWrapper.d("Activity Starting...");
        mAllowLocationUpdates = true;
        mAllowBluetoothDiscovery = true;
        //LogWrapper.d("Exit");
    }

    /**
     * Here we resume receiving location updates
     */
    @Override
    protected void onResume() {
        super.onResume();
        //LogWrapper.d("Resuming location updates...");

       // LogWrapper.d("mAllowLocationUpdates -> " + mAllowLocationUpdates);
        if(mAllowLocationUpdates && hasPermission()) {
            //Start location updates
            startLocationUpdates();
            // Initialize bluetooth
            initBluetooth();
        } else if(!hasPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions();
        }
        //LogWrapper.d("Exit");
    }

    /**
     * Within {@code onPause()}, we remove location updates to save battery
     */
    @Override
    protected void onPause() {
        super.onPause();
        // Remove location updates to save battery
        stopLocationUpdates();
        // Stop bluetooth discovery
        stopDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(discoveryMonitorReceiver);
    }


    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
       // LogWrapper.d("Checking if the device has the necessary location settings...");
        // Checking if the device has the necessary location settings
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, locationSettingsResponse -> {
                   // LogWrapper.d("Requesting location update...");

                    mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                            mLocationCallback, Looper.myLooper());

                    //LogWrapper.d("New Location update requested...");

                })
                .addOnFailureListener(this, e -> {
                   // LogWrapper.d("Location settings error occurred...");
                    if(!(e instanceof ApiException)) return;

                    int statusCode = ((ApiException) e).getStatusCode();
                    switch (statusCode) {

                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED :
                            //LogWrapper.d("Location settings are not satisfied. Attempting to" +
                                  //  " upgrade location settings");

                            try {

                                // Show the dialog by calling startResolutionForResult(), and
                                // check the result in onActivityResult()
                                ResolvableApiException rae = (ResolvableApiException) e;
                                rae.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);

                            } catch (IntentSender.SendIntentException ex) {
                                //LogWrapper.e("PendingIntent unable to execute request", ex);
                            }

                            break;

                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE :

                            //LogWrapper.e(getString(R.string.location_settings_inadequate));
                            showToast(getString(R.string.location_settings_inadequate), Toast
                                    .LENGTH_LONG);
                            mAllowLocationUpdates = false;
                            break;
                    }

                });
        //LogWrapper.d("Exit");
    }

    /**
     * Removes location updates from the FusedLocationApi.
     * It is good practice to remove location request when the activity is in a paused or stopped
     * state. Doing so helps battery performance.
     */
    private void stopLocationUpdates() {
       // LogWrapper.d("Stopping location updates...");
        if(!mAllowLocationUpdates) {
           // LogWrapper.d("stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, task -> {
                    mAllowLocationUpdates = false;
                    //LogWrapper.d("Location updates stopped");
                });

        //LogWrapper.d("Exit");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

       // LogWrapper.d("Permission request completed. RequestCode -> " + requestCode);

        switch (requestCode) {

            case REQUEST_PERMISSIONS_REQUEST_CODE: {
                if(grantResults.length <= 0) {
                    // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                    //LogWrapper.d( "User interaction was cancelled.");

                } else if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted. initialize bluetooth, start location updates
                    initBluetooth();
                    //LogWrapper.d( "Permission granted, starting location updates");
                    startLocationUpdates();
                }

                break;
            }

            default: {// permission denied
                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackBar(R.string.permission_denied_explanation,
                        R.string.action_settings, view -> {
                            // Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);

                            Uri uri = Uri.fromParts("package",
                                    BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        });
            }

        }

        //  LogWrapper.d("Exit");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        //LogWrapper.d("Activity result completed. RequestCode -> " + requestCode);
        switch (requestCode) {
            // Check for the integer request code originally supplied to startForResolutionResult()
            case REQUEST_CHECK_SETTINGS :
                handleLocationSettingsResult(resultCode);
                break;

            case ENABLED_BLUETOOTH:
                handleBluetoothSettingsResult(resultCode);
                break;

            default:
               // LogWrapper.wtf("We shouldn't get here");
                break;

        } // end outer switch
        //LogWrapper.d("Exit");
    }

    private void handleLocationSettingsResult(int resultCode) {
        switch (resultCode) {
            case AppCompatActivity.RESULT_OK: {
                //LogWrapper.d("User agreed to make required location settings changes");
                // Nothing to do. startLocationUpdates() gets called in onResume again
                break;
            }

            case AppCompatActivity.RESULT_CANCELED: {
                //LogWrapper.d("User chose not to make required location settings changes");
                mAllowLocationUpdates = false;
                break;
            }

            default:
                //LogWrapper.wtf("We shouldn't get here");
        }
    }

    private void handleBluetoothSettingsResult(int resultCode) {
        switch (resultCode) {
            case AppCompatActivity.RESULT_OK: {
                // Bluetooth has been enabled, initialize the UI.
                //LogWrapper.d("Bluetooth is enabled...");
                startDiscovery();
                break;
            }

            case AppCompatActivity.RESULT_CANCELED: {
                // Bluetooth enable request canceled
               // LogWrapper.d("Bluetooth enable request canceled...");
                showSnackBar(R.string.permission_denied_explanation,
                        R.string.action_settings, view -> {
                            // Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);

                            Uri uri = Uri.fromParts("package",
                                    BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        });
                break;
            }

            default:
                //LogWrapper.wtf("We shouldn't get here");
        }
    }


    private void initBluetooth() {
        //LogWrapper.d("Initializing Bluetooth...");
        if(!discoverabilityRegistered) {
            monitorDiscovery();
            discoverabilityRegistered = true;
        }

        if(!mBluetoothAdapter.isEnabled()){
            //LogWrapper.d("Bluetooth isn't enabled, asking the user to turn it on.");
            // Bluetooth isn't enabled, prompt the user to turn it on.
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, ENABLED_BLUETOOTH);
        } else {
            // Bluetooth is enabled
            //LogWrapper.d("Bluetooth is enabled...");
            startDiscovery();
        }

        //LogWrapper.d("Exit");
    }

    private void startDiscovery() {

        if(mBluetoothAdapter.isEnabled() && !mBluetoothAdapter.isDiscovering()) {
            //LogWrapper.d("Starting discovery...");
            //deviceList.clear();
            mBluetoothAdapter.startDiscovery();
        }
        //LogWrapper.d("Exit");
    }

    private void monitorDiscovery() {
        // Register for broadcasts when discovery is started.
        registerReceiver(discoveryMonitorReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        // Register for broadcasts when discovery is started.
        registerReceiver(discoveryMonitorReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        // Register for broadcasts when a device is discovered.
        registerReceiver(discoveryMonitorReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    private void stopDiscovery() {
        if(mBluetoothAdapter.isEnabled() && mBluetoothAdapter.isDiscovering()) {
            mAllowBluetoothDiscovery = false;
            //LogWrapper.d("Stopping Discovery...");
            mBluetoothAdapter.cancelDiscovery();
        }
        //LogWrapper.d("Exit..");
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean hasPermission() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;

        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPermissions() {
        //LogWrapper.d("Requesting permissions...");
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this
                ,Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if(shouldProvideRationale) {
            //LogWrapper.d("Displaying permission rationale to provide additional context.");
            showSnackBar(R.string.permission_rationale, android.R.string.ok, view -> {
                // request permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERMISSIONS_REQUEST_CODE);
            });
        } else {
            //LogWrapper.d("Requesting permission...");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
        //LogWrapper.d("Exit");
    }

    private void showSnackBar(int mainTextStringId, int actionStringId,
                              View.OnClickListener listener) {

        Snackbar.make(findViewById(android.R.id.content), getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE).setAction(getString(actionStringId), listener).show();
    }

    /**
     * Shows a toast with the given text and length
     */
    private void showToast(String text, int length) {
        Toast.makeText(this, text, length).show();
    }

    private void checkPlayServices() {

        // Check that Google Play services is available
        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        int result = availability.isGooglePlayServicesAvailable(this);
        if(ConnectionResult.SUCCESS == result) {
            //LogWrapper.d("Google Play services is available");
        } else if (availability.isUserResolvableError(result)) {
            availability.getErrorDialog(this, result, PLAY_SERVICES_RESOLUTION_REQUEST);
        } else {
            showToast(ERROR_MSG, Toast.LENGTH_LONG);
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    private String getDeviceType(BluetoothDevice device) {
        String type;
        switch (device.getType()) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC : type = "Type: Classic - BR/EDR devices";
                break;
            case BluetoothDevice.DEVICE_TYPE_DUAL: type = "Type: Dual Mode - BR/EDR/LE";
                break;
            case BluetoothDevice.DEVICE_TYPE_LE: type = "Type: Low Energy - LE-only";
                break;
            default: type = "Unknown";
        }
        return type;
    }

    private String getBondedState(BluetoothDevice device) {
        String bonded;
        switch (device.getBondState()) {
            case BluetoothDevice.BOND_BONDED: bonded = "Bonded State: paired";
                break;
            case BluetoothDevice.BOND_BONDING: bonded = "Bonded State: in paring mode";
                break;
            default: bonded = "Bonded State: not paired";
        }

        return bonded;
    }

    private String getMajorDeviceClass(BluetoothDevice device) {
        String deviceClass;
        switch (device.getBluetoothClass().getMajorDeviceClass()) {

            case BluetoothClass.Device.Major.AUDIO_VIDEO:
                deviceClass = "Class: AUDIO_VIDEO";
                break;
            case BluetoothClass.Device.Major.COMPUTER:
                deviceClass = "Class: COMPUTER";
                break;
            case BluetoothClass.Device.Major.PHONE:
                deviceClass = "Class: PHONE";
                break;
            case BluetoothClass.Device.Major.HEALTH:
                deviceClass = "Class: HEALTH";
                break;
            case BluetoothClass.Device.Major.IMAGING:
                deviceClass = "Class: IMAGING";
                break;
            case BluetoothClass.Device.Major.MISC:
                deviceClass = "Class: MISC";
                break;
            case BluetoothClass.Device.Major.NETWORKING:
                deviceClass = "Class: NETWORKING";
                break;
            case BluetoothClass.Device.Major.PERIPHERAL:
                deviceClass = "Class: PERIPHERAL";
                break;
            case BluetoothClass.Device.Major.TOY:
                deviceClass = "Class: TOY";
                break;
            case BluetoothClass.Device.Major.WEARABLE:
                deviceClass = "Class: WEARABLE";
                break;

            default: deviceClass = "Class: UNCATEGORIZED";
        }
        return deviceClass;
    }

    /**
     * Nested BroadcastReceiver class for ACTION_FOUND, ACTION_DISCOVERY_STARTED,
     * ACTION_DISCOVERY_FINISHED
     */
    class BluetoothDiscoveryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action == null) return;
            switch (action) {
                case BluetoothDevice.ACTION_FOUND : {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //LogWrapper.d("Discovered -> " + device.getName());

					/*Bluetooth bluetooth = new Bluetooth(
						(device.getName() == null) ? "None" : device.getName(),
						device.getAddress(),
						getDeviceType(device),
						getBondedState(device),
						getMajorDeviceClass(device),
						false);
					*/

                    if(!markersList.contains(device)) {
                        markersList.add(device);
                        //deviceList.add(bluetooth);
                        //mBluetoothViewModel.insert(bluetooth);
                        if (isReady) {
                            putMarkersOnMap();
                        }
                    }
                    break;
                }

                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    //LogWrapper.d("Discovery has started");
                    break;

                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    //LogWrapper.d("Discovery completed");
                    if(mAllowBluetoothDiscovery)
                        startDiscovery();
                    break;

                default: //LogWrapper.wtf("We Should not get here. Unknown action");
            }
        }
    }
}

