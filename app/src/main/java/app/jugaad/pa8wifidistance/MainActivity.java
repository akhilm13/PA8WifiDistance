package app.jugaad.pa8wifidistance;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements  AdapterView.OnItemSelectedListener {

    WifiManager wifiManager;
    List<ScanResult> resultsList;
    String selectedNameOfAP;
    BroadcastReceiver receiver;
    String ssidToCalibrate;
    List<ScanResult> resultsListForCalibration;
    int powerToCalculate = 0;
    double valueOfN = 0.0;
    final int MAXCALIB = 3;
    int calibIteration = 0; //calibrationIterations
    int noCalib = 0;
    BroadcastReceiver customReceiver;
    double powerReference;
    double distanceReference;
    double distanceToSelectedHotspot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((TextView)findViewById(R.id.status_text)).setText("Ready");
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                resultsList = wifiManager.getScanResults();
                display(resultsList);

            }
        };

        registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));


    }

    //Function that initiates Wifi Scanning after checking for permissions
    public void startScanning (View view){
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0x12345);
        }

        if (customReceiver != null) {
            Toast.makeText(this, "Calibration is running. Please wait", Toast.LENGTH_SHORT).show();
            return;
        }
        wifiManager.startScan();

    }

    //Function to display the available hotspots in a spinner in the UI
    void display(List<ScanResult> scanResults){
        Spinner spinner = (Spinner) findViewById(R.id.available_APs_spinner);

        List<String> ssidList = new ArrayList<String>(); //Creating a List of all SSIDs
        for (ScanResult result :scanResults
                ) {
            ssidList.add(result.SSID);
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.addAll(ssidList);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

    }

    //Function to get the ScanResult object for given SSID
    ScanResult getAccessPoint( String ssid, List<ScanResult> results){
        for (ScanResult result: results) {
            if (result.SSID.equals(ssid))
                return result;
        }
        return null;
    }

    //function to perform the actual calibration. Creates a background service that takes care of the calibration.
    //Unregisters the receiver to prevent unnecessary UI updating
    //Calls doCalibrate function
    public void startCalibration(View view){

        if (calibIteration >0){
            Toast.makeText(this, "Calibration already running", Toast.LENGTH_SHORT).show();
            return;
        }
        ((TextView) findViewById(R.id.status_text)).setText("Calibrating");
        try {
            powerReference = Double.parseDouble(((EditText) findViewById(R.id.reference_power_input)).getText().toString());
            distanceReference = Double.parseDouble(((EditText) findViewById(R.id.distance_input)).getText().toString());
        }
        catch  (Exception e){
            Toast.makeText(this, "Could not extract values of power or distance.", Toast.LENGTH_SHORT).show();
            return;
        }
        ssidToCalibrate = selectedNameOfAP;
        Log.e("Selected name: ", selectedNameOfAP);
        unregisterReceiver(receiver); //unregistering the receiver for the WiFi scan to register a custom receiver
        customReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                resultsListForCalibration = wifiManager.getScanResults();
                ScanResult result = getAccessPoint(ssidToCalibrate, resultsListForCalibration);

                if(result != null)
                doCalibrate(result);
                else
                    Log.e("Error: ","Access point to calibrate is not found" );
            }
        };

        registerReceiver(customReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }
    //Runs iteration MAXCALIB number of times. To change the number of iterations, change the value of MAXCALIB
    public void doCalibrate(ScanResult accessPoint){

        ((TextView) findViewById(R.id.status_text)).setText("Calibration: "+calibIteration+1);
        valueOfN+= (accessPoint.level - powerReference)/(-10 * Math.log10(distanceReference));

        calibIteration++;
        if (calibIteration == MAXCALIB){
            Toast.makeText(this, "Calibration Complete", Toast.LENGTH_SHORT).show();
            ((TextView) findViewById(R.id.status_text)).setText("Calibration Complete");

            //unregistering customReceiver
            unregisterReceiver(customReceiver);
            //setting it to null
            customReceiver = null;
            //register the scan receiver
            registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            calibIteration = 0;
            noCalib++;
        }
        else
            wifiManager.startScan();
    }

    //This function calculates the value of
    public void endCalibration(View view){

        valueOfN/=(MAXCALIB*noCalib);
        noCalib = 0;
        ((TextView) findViewById(R.id.status_text)).setText("N calulated to: "+valueOfN);

    }

    public void resetCalibration(View view){

        calibIteration = 0;
        valueOfN = 0;
        Toast.makeText(this, "Calibration was reset", Toast.LENGTH_SHORT).show();
        ((TextView)findViewById(R.id.status_text)).setText("Ready");
    }

    public void calculateDistance(View view){

        ConstraintLayout constraintLayout = (ConstraintLayout) findViewById(R.id.main_activity_layout);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_layout,null);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // show the popup window
        popupWindow.showAtLocation(constraintLayout, Gravity.CENTER, 0, 0);

        // dismiss the popup window when touched
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });

        distanceToSelectedHotspot = 0;
        try {
            powerReference = Double.parseDouble(((EditText) findViewById(R.id.reference_power_input)).getText().toString());
        }catch (Exception e)
        {
            Toast.makeText(this, "Could not get refernce power value", Toast.LENGTH_SHORT).show();
            return;
        }
        ScanResult result = getAccessPoint(selectedNameOfAP, resultsList);
        distanceToSelectedHotspot = Math.pow(10.0, ((double)result.level- powerReference)/(-10.0*valueOfN));
        ((TextView) findViewById(R.id.status_text)).setText("");
        ((TextView) findViewById(R.id.distance_text)).setText(distanceToSelectedHotspot+" m");



    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectedNameOfAP = parent.getItemAtPosition(position).toString();
        Toast.makeText(this, "Selected Access Point: "+selectedNameOfAP, Toast.LENGTH_SHORT).show();
        TextView powerText = (TextView) findViewById(R.id.power_text);
        ScanResult accessPoint = getAccessPoint(selectedNameOfAP, resultsList);
        powerText.setText(accessPoint.level+" dBm");
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
