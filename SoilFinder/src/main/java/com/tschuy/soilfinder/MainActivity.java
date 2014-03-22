package com.tschuy.soilfinder;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationListener;
import android.os.Bundle;
import android.text.method.DigitsKeyListener;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebViewFragment;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.widget.EditText;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.util.List;
import java.util.Locale;


public class MainActivity extends Activity
        implements GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerDragListener {

    /* Declare initial variables/strings/views, etc */

    // Location variables
    LocationManager mlocManager;
    LocationListener mlocListener = new MyLocationListener();

    // Default accuracy
    int accuracy = 50;

    // Declare textview for queries and map for main fragment
    private TextView text;
    private GoogleMap myMap;
    public WebViewFragment myWebViewFragment = new WebViewFragment();

    /* Classes */

    // Listen for location updates
    // Upon location fix, call loadCoordinates
    public class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc)
        {
            // Output accuracy
            if (loc.getAccuracy() < accuracy) {
                // After GPS reaches adequate accuracy load details page and stop GPS
                Toast.makeText(getApplicationContext(), R.string.loading_message,
                        Toast.LENGTH_LONG).show();

                // pass lat and long to loadCoordinates
                LatLng userCoordinates = new LatLng(loc.getLatitude(), loc.getLongitude());
                loadCoordinates(userCoordinates);
                mlocManager.removeUpdates(mlocListener);
            }
            else {
                Toast.makeText(getApplicationContext(), getString(R.string.accuracy_prefix)
                        + loc.getAccuracy() + getString(R.string.accuracy_suffix),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Required
        public void onProviderDisabled(String provider){};
        public void onProviderEnabled(String provider){};
        public void onStatusChanged(String provider, int status, Bundle extras){};
    }

    /* Main activity functions */

    // Called on Activity creation/app startup
    @Override
    public void onCreate(Bundle myInstance) {
        super.onCreate(myInstance);
        setContentView(R.layout.activity_main);

        FragmentManager myFragmentManager = getFragmentManager();
        MapFragment myMapFragment
                = (MapFragment)myFragmentManager.findFragmentById(R.id.map);

        myMap = myMapFragment.getMap();

        CameraUpdate initialPosition = CameraUpdateFactory.newLatLngZoom(new LatLng(39.746859, -101.742427), 3);
        myMap.moveCamera(initialPosition);

        myMap.setMyLocationEnabled(true);

        myMap.setOnMapClickListener(this);
        myMap.setOnMapLongClickListener(this);
        myMap.setOnMarkerDragListener(this);

        // TODO: Get rid of this terrible hack
        gpsLocation();
        stopLocationUpdates();
    }

    // Called when Activity is quit/Home button is pressed
    public void onStop(){
        // On home button stop GPS
        //mlocManager.removeUpdates(mlocListener);
        super.onStop();
    }

    @Override
    public void onBackPressed(){
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            super.onBackPressed();
        } else {
            getFragmentManager().popBackStack();
            getActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    /* Google Map functions */

    @Override
    public void onMapLongClick(LatLng point) {}
    @Override
    public void onMarkerDrag(Marker marker) {}
    @Override
    public void onMarkerDragEnd(Marker marker) {}
    @Override
    public void onMarkerDragStart(Marker marker) {}

    // Call loadCoordinates when location on map is tapped
    @Override
    public void onMapClick(LatLng point) {
        point = new LatLng(Math.round(point.latitude * 10000.0) / 10000.0, Math.round(point.longitude * 10000.0) / 10000.0);
        loadCoordinates(point);
    }

    /* WebView functions */

    // Loads WebView fragment with given URL
    public void makeWebview(String webURL){
        getActionBar().setDisplayHomeAsUpEnabled(true);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction
                .setCustomAnimations(R.anim.slide_in_left,R.anim.slide_out_left,R.anim.slide_in_left,R.anim.slide_out_left)
                .replace(R.id.map, myWebViewFragment)
                .addToBackStack(null)
                .commit(); // Replaces current fragment with the webview
        fragmentManager.executePendingTransactions();

        Toast.makeText(getApplicationContext(), R.string.loading_message, Toast.LENGTH_LONG).show();
        myWebViewFragment.getWebView().setWebViewClient(new SSLTolerantWebViewClient());
        myWebViewFragment.getWebView().loadUrl(webURL);
    }

    // calls makeWebview with string generated from LatLng point
    public void loadCoordinates(LatLng point) {
        //setContentView(R.layout.activity_web_browse);
        makeWebview(
                "http://casoilresource.lawr.ucdavis.edu/soil_web/list_components.php?lon="
                        + point.longitude + "&lat=" + point.latitude);
    }

    /* Functions used to do non-particular work */

    // Stop mLocListener from polling
    public void stopLocationUpdates(){
        mlocManager.removeUpdates(mlocListener);
    }

    private LatLng getLatLongFromAddress(String address) {
        LatLng point = new LatLng(0,0);

        Geocoder geoCoder = new Geocoder(this, Locale.getDefault());
        try
        {
            List<Address> addresses = geoCoder.getFromLocationName(address , 1);
            if (addresses.size() > 0)
            {
                point = new LatLng(addresses.get(0).getLatitude(),addresses.get(0).getLongitude());
                myMap.addMarker(new MarkerOptions().position(point));
                return point;
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return point;
    }

    /* ActionBar button functions */

    // GPS accuracy gauge
    public void accuracySelector() {

        // Create SeekBar
        final AlertDialog.Builder accuracy_picker = new AlertDialog.Builder(this);
        SeekBar seek=new SeekBar(this);
        seek.setPadding(50,0,50,0);

        seek.setProgress(100-accuracy);

        accuracy_picker.setTitle(R.string.slider_header);
        accuracy_picker.setMessage(R.string.slider_message);

        LinearLayout linear=new LinearLayout(this);

        linear.setOrientation(1);
        text=new TextView(this);
        text.setPadding(45, 10, 10, 10);
        text.setText(getString(R.string.current_accuracy_prefix) + accuracy + getString(R.string.current_accuracy_suffix));


        linear.addView(seek);
        linear.addView(text);

        accuracy_picker.setView(linear);

        accuracy_picker.setPositiveButton(R.string.ok,new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog,int id) {}
        });

        /*accuracy_picker.setNegativeButton(R.string.cancel,new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog,int id) {}
        }); */

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Set accuracy to current value
                accuracy=-(progress-100);
                accuracy_picker.setMessage(R.string.slider_var_text + Integer.toString(progress));
                text.setText(getString(R.string.current_accuracy_prefix) + accuracy + getString(R.string.current_accuracy_suffix));
            }
        });

        accuracy_picker.show();

    }

    // Query soil by Series name
    public void querySoil() {
        // Alert text box
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.soil_series_name);

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setPadding(50, 25, 50, 25);
        alert.setView(input);

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Cancelled.
            }
        });

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                // Go to webpage
                // this if catches a null input
                if (input.getText().length() != 0) {
                    String soilName = (input.getText().toString()).toUpperCase();
                    makeWebview("https://soilseries.sc.egov.usda.gov/OSD_Docs/" +
                            soilName.charAt(0) + "/" + soilName + ".html");
                }
            }
        });

        alert.show();
    }

    // Search by place name
    public void searchByPlace() {

        // Alert text box
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.location_name);

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setPadding(50,25,50,25);
        alert.setView(input);

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Cancelled.
            }
        });

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                // this if catches a null input
                if (input.getText().length() != 0) {
                    //String coordinates = readJSON(input.getText().toString());
                    //webURL = ("http://casoilresource.lawr.ucdavis.edu/soil_web/list_components.php?lon=" + (coordinates.split("\\,")[0]) + "&lat=" + (coordinates.split("\\,")[1]));
                    //myWebView.loadUrl(webURL);
                    Toast.makeText(getApplicationContext(), R.string.loading_location, Toast.LENGTH_LONG).show();

                    LatLng placeCoordinate = getLatLongFromAddress(input.getText().toString());

                    CameraUpdate initialPosition = CameraUpdateFactory.newLatLngZoom(placeCoordinate, 15);
                    myMap.animateCamera(initialPosition);
                }
            }
        });
        alert.show();
    }

    // GPS button
    // Starts location listening
    public void gpsLocation() {
        mlocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        mlocManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, mlocListener);
    }

    // Takes string (such as -45.40) and checks to see if it contains -'s in the wrong places
    public boolean isValidCoordinate(String coordinate) {
        // Skip 0 as 0 is allowed to be a minus
        int periods = 0;
        for(int i = 1; i < coordinate.length(); i++) {
            if (coordinate.charAt(i) == '-') return false;
            if (coordinate.charAt(i) == '.') periods++;
        }
        if (periods > 1) return false;
        else return true;
    }

    // Query Coordinates
    // Prompts user for coordinates, then calls loadCoordinates() if valid
    public void queryCoordinates() {

        // Alert text box
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.query_coordinates);

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setPadding(50,25,50,25);
        alert.setMessage(R.string.coords_message);
        alert.setView(input);
        input.setKeyListener(DigitsKeyListener.getInstance("0123456789,.-"));

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Cancelled.
            }
        });

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                // Go to webpage
                // this if catches a null input

                if (input.getText().length() != 0) {
                    int commas = 0;
                    String coordinates = input.getText().toString();
                    for (int i = 0; i < coordinates.length(); i++) {
                        if (coordinates.charAt(i) == ',') commas++;
                    }
                    if  (coordinates.charAt(0) == ',' ||
                            coordinates.charAt(coordinates.length()-1) == ',') commas = -1;

                    if (commas == 1) {
                        String coord1 = (input.getText().toString()).split("\\,")[0];
                        String coord2 = (input.getText().toString()).split("\\,")[1];

                        if (isValidCoordinate(coord1) && isValidCoordinate(coord2)) {
                            LatLng point = new LatLng(Double.valueOf(coord1), Double.valueOf(coord2));
                            loadCoordinates(point);
                        }
                        else Toast.makeText(getApplicationContext(),
                                R.string.invalid_coordinate, Toast.LENGTH_LONG).show();
                    }
                    else if (commas > 1) {
                        Toast.makeText(getApplicationContext(),
                                R.string.too_many_commas, Toast.LENGTH_LONG).show();
                    }
                    else if (commas < 0) {
                        Toast.makeText(getApplicationContext(),
                                R.string.invalid_commas, Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(getApplicationContext(),
                                R.string.too_few_commas, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        alert.show();
    }

    /* Required ActionBar functions */

    // Add actionbar items
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // Actionbar item selection
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_gps:
                // (Re-)enable GPS searching
                Toast.makeText(getApplicationContext(),
                        R.string.getting_location, Toast.LENGTH_LONG).show();
                gpsLocation();
                return true;

            case R.id.action_search:
                stopLocationUpdates();
                searchByPlace();
                return true;

            case R.id.action_accuracy:
                accuracySelector();
                return true;

            case R.id.action_coordinates:
                stopLocationUpdates();
                queryCoordinates();
                return true;

            case R.id.action_query:
                stopLocationUpdates();
                querySoil();
                return true;

            case R.id.action_about:
                stopLocationUpdates();
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;

            case android.R.id.home:
                getFragmentManager().popBackStack();
                getActionBar().setDisplayHomeAsUpEnabled(false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}