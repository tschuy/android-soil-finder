package com.tschuy.soilfinder;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.os.Build;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;

import com.tschuy.soilfinder.R;
import android.widget.NumberPicker;
import android.view.View.OnClickListener;
import android.widget.Button;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Toast;
import android.widget.EditText;
import android.util.Log;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class MainActivity extends Activity {
    public WebView myWebView;
    String webURL;
    LocationManager mlocManager;
    LocationListener mlocListener;
    int accuracy = 50; // TODO: Allow user adjustable accuracy through actionbar item
    String soilName = "";
    int pro = 0;
    private TextView text;

    String html = "<html><body><h1>Loading local soil profile, please wait...</h1></body></html>";
    String mime = "text/html";
    String encoding = "utf-8";

    @Override
    public void onCreate(Bundle myInstance) {
        super.onCreate(myInstance);
        setContentView(R.layout.activity_main);

        myWebView=(WebView)findViewById(R.id.webview);
        myWebView.setWebViewClient(new SSLTolerantWebViewClient());
        myWebView.loadDataWithBaseURL(null, html, mime, encoding, null);
        mlocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        mlocListener = new MyLocationListener();
        mlocManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, mlocListener);
    }

    public void onStop(){
        super.onStop();
        mlocManager.removeUpdates(mlocListener);
    }

    private class SSLTolerantWebViewClient extends WebViewClient {
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed(); // Ignore SSL certificate errors
        }
    }

    public class MyLocationListener implements LocationListener
    {
        @Override
        public void onLocationChanged(Location loc)
        {
            Toast.makeText(getApplicationContext(), "Accuracy is " + loc.getAccuracy() + " meters", Toast.LENGTH_SHORT).show();
            if (loc.getAccuracy() < accuracy) {
                Toast.makeText(getApplicationContext(), "Loading soil profile...", Toast.LENGTH_LONG).show();
                webURL = "http://casoilresource.lawr.ucdavis.edu/soil_web/list_components.php?iphone_user=1&lon=" + loc.getLongitude() + "&lat=" + loc.getLatitude();
                myWebView.loadUrl(webURL);
                mlocManager.removeUpdates(mlocListener);
            }
        }
        public void onProviderDisabled(String provider){};
        public void onProviderEnabled(String provider){};
        public void onStatusChanged(String provider, int status, Bundle extras){};
    }

    // Everything below this line is for the action bar

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_refresh:
                Toast.makeText(getApplicationContext(), "Getting location...", Toast.LENGTH_LONG).show();
                mlocManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, mlocListener);
                return true;

            case R.id.action_accuracy:

                final AlertDialog.Builder accuracy_picker = new AlertDialog.Builder(this);
                SeekBar seek=new SeekBar(this);

                seek.setProgress(100-accuracy);

                accuracy_picker.setTitle("Select Accuracy");
                accuracy_picker.setMessage("Minimum GPS accuracy in meters");

                LinearLayout linear=new LinearLayout(this);

                linear.setOrientation(1);
                text=new TextView(this);
                text.setPadding(10, 10, 10, 10);
                text.setText("Current Accuracy: " + accuracy + " meters");


                linear.addView(seek);
                linear.addView(text);

                accuracy_picker.setView(linear);

                accuracy_picker.setPositiveButton("Ok",new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog,int id)
                    {
                    }
                });

                accuracy_picker.setNegativeButton("Cancel",new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog,int id)
                    {
                    }
                });

                seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // TODO Auto-generated method stub
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // TODO Auto-generated method stub
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        // TODO Auto-generated method stub
                        accuracy=-(progress-100);  //we can use the progress value of pro as anywhere
                        accuracy_picker.setMessage("Accuracy in meters: " + Integer.toString(progress));
                        text.setText("Current Accuracy: " + accuracy + " meters");
                    }
                });

                accuracy_picker.show();
                return true;

            case R.id.action_query:
                final AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Enter Soil Series Name");

                // Set an EditText view to get user input
                final EditText input = new EditText(this);
                alert.setView(input);

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Cancelled.
                    }
                });

                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        // Do something with value!
                        soilName = (input.getText().toString()).toUpperCase();
                        webURL = "https://soilseries.sc.egov.usda.gov/OSD_Docs/" + soilName.charAt(0) + "/" + soilName + ".html";
                        myWebView.loadUrl(webURL);
                    }
                });

                alert.show();
                mlocManager.removeUpdates(mlocListener);
                return true;

            case R.id.action_demo:
                Toast.makeText(getApplicationContext(), "Loading soil profile for Plymouth County, MA", Toast.LENGTH_LONG).show();
                webURL = "http://casoilresource.lawr.ucdavis.edu/soil_web/list_components.php?iphone_user=1&lon=-70.8454&lat=41.93039";
                myWebView.loadUrl(webURL);
                mlocManager.removeUpdates(mlocListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}