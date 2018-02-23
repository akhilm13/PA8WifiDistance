package app.jugaad.pa8wifidistance;

import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

/**
 * Created by Akhil on 12-02-2018.
 */


public class CalibrationTask extends AsyncTask<MainActivity, Void, Double> {

    MainActivity activity;
    double finalValue;

    @Override
    protected Double doInBackground(MainActivity... mainActivities) {

        activity = mainActivities[0];
        activity.powerToCalculate = 0;

        activity.wifiManager.startScan();

        return null;
    }

    public void calibrateValue(){

    }
}


