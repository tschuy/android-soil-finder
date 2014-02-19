package com.tschuy.soilfinder;

import android.app.Activity;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.widget.EditText;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.method.DigitsKeyListener;

// Used for downloading and parsing the search JSON

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class MainActivity extends Activity {

    // Declare initial variables/strings/views, etc
    public WebView myWebView;
    String webURL;
    LocationManager mlocManager;
    LocationListener mlocListener;
    int accuracy = 50;
    String soilName = "";
    private TextView text;

    String html = "<html><body></body></html>";
    String mime = "text/html";
    String encoding = "utf-8";
    String jsonURL = "http://maps.google.com/maps/api/geocode/json?address=null&sensor=false";

    // Main basic code functions

    @Override
    public void onCreate(Bundle myInstance) {
        super.onCreate(myInstance);
        setContentView(R.layout.activity_main);

        // Start webview
        myWebView=(WebView)findViewById(R.id.webview);
        myWebView.setWebViewClient(new SSLTolerantWebViewClient());
        myWebView.loadDataWithBaseURL(null, html, mime, encoding, null);

        // Start requesting GPS data
        mlocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        mlocListener = new MyLocationListener();
        mlocManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, mlocListener);
    }

    public void onStop(){
        // On home button stop GPS
        super.onStop();
        mlocManager.removeUpdates(mlocListener);
    }

    private class SSLTolerantWebViewClient extends WebViewClient {
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed(); // Ignore SSL certificate errors for name queries
        }
    }

    public class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc)
        {
            // Output accuracy
            Toast.makeText(getApplicationContext(), getString(R.string.accuracy_prefix) + loc.getAccuracy() + getString(R.string.accuracy_suffix), Toast.LENGTH_SHORT).show();
            if (loc.getAccuracy() < accuracy) {
                // After GPS reaches adequate accuracy load details page and stop GPS
                Toast.makeText(getApplicationContext(), R.string.loading_message, Toast.LENGTH_LONG).show();
                myWebView.loadUrl("http://casoilresource.lawr.ucdavis.edu/soil_web/list_components.php?lon=" + loc.getLongitude() + "&lat=" + loc.getLatitude());
                mlocManager.removeUpdates(mlocListener);
            }
        }

        // Required
        public void onProviderDisabled(String provider){};
        public void onProviderEnabled(String provider){};
        public void onStatusChanged(String provider, int status, Bundle extras){};
    }

    // JSON downloading and parsing



    // Make the action bar buttons do stuff

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

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
                    soilName = (input.getText().toString()).toUpperCase();
                    webURL = "https://soilseries.sc.egov.usda.gov/OSD_Docs/" + soilName.charAt(0) + "/" + soilName + ".html";
                    myWebView.loadUrl(webURL);
                }
            }
        });

        alert.show();
        mlocManager.removeUpdates(mlocListener);
    }

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
                    Toast.makeText(getApplicationContext(), input.getText().toString(), Toast.LENGTH_LONG).show();
                }
            }
        });

        mlocManager.removeUpdates(mlocListener);
        alert.show();
    }

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
                    if  (coordinates.charAt(0) == ',' || coordinates.charAt(coordinates.length()-1) == ',') commas = -1;

                    if (commas == 1) {
                        webURL = ("http://casoilresource.lawr.ucdavis.edu/soil_web/list_components.php?lon=" + ((input.getText().toString()).split("\\,")[0]) + "&lat=" + ((input.getText().toString()).split("\\,")[1]));
                        myWebView.loadUrl(webURL);
                    }
                    else if (commas > 1) {
                        Toast.makeText(getApplicationContext(), R.string.too_many_commas, Toast.LENGTH_LONG).show();
                    }
                    else if (commas < 0) {
                        Toast.makeText(getApplicationContext(), R.string.invalid_commas, Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(getApplicationContext(), R.string.too_few_commas, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        alert.show();
        mlocManager.removeUpdates(mlocListener);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_gps:
                // Re-enable GPS searching
                Toast.makeText(getApplicationContext(), R.string.getting_location, Toast.LENGTH_LONG).show();
                mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mlocListener);
                return true;

            case R.id.action_search:
                searchByPlace();
                return true;

            case R.id.action_accuracy:
                accuracySelector();
                return true;

            case R.id.action_coordinates:
                queryCoordinates();
                return true;

            case R.id.action_query:
                querySoil();
                return true;

            case R.id.action_about:
                return true;

            /*
            case R.id.action_demo:
                // Load soil profile as example
                Toast.makeText(getApplicationContext(), R.string.example_profile, Toast.LENGTH_LONG).show();
                webURL = "http://casoilresource.lawr.ucdavis.edu/soil_web/list_components.php?lon=-70.8454&lat=41.93039";
                myWebView.loadUrl(webURL);
                mlocManager.removeUpdates(mlocListener);
                return true;
                */
        }
        return super.onOptionsItemSelected(item);
    }
}