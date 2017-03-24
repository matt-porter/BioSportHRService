package io.github.matt_porter.biosporthrservice;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataSourcesResult;
import com.intel.heartratecore.api.HeartRateCore;
import com.intel.heartratecore.api.HeartRateCoreListener;
import com.intel.heartratecore.api.HeartRateCoreStatus;

import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

public class HRView extends AppCompatActivity {

    final String TAG = "HRView";
    OnDataPointListener mListener = null;
    GoogleApiClient mClient = null;
    SensorManager mSensorManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.SENSORS_API)
                .build();
        setContentView(R.layout.activity_hrview);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Intent serviceIntent = new Intent(this, BioSportHrSensorService.class);
        ComponentName cn = this.startService(serviceIntent);
        Log.d(TAG, cn.toString());
        FloatingActionButton start = (FloatingActionButton) findViewById(R.id.start);
        start.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
            @Override
            public void onClick(View view) {
                // Pops up a little message...
                Log.d(TAG, "onClick");
                Snackbar.make(view, "Looking for sources", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                Log.d(TAG, "HR Sensors: " + mSensorManager.getSensorList(Sensor.TYPE_HEART_RATE).toString());

                SensorRequest sr = new SensorRequest.Builder()
                        .setDataType(DataType.TYPE_HEART_RATE_BPM)
                        .setSamplingRate(10, TimeUnit.SECONDS)  // sample every 10s
                        .build();

                mListener = new OnDataPointListener() {
                    @Override
                    public void onDataPoint(DataPoint dataPoint) {
                        for (Field field : dataPoint.getDataType().getFields()) {
                            Value val = dataPoint.getValue(field);
                            Log.i(TAG, "Detected DataPoint field: " + field.getName());
                            Log.i(TAG, "Detected DataPoint value: " + val);
                        }
                    }
                };

                PendingResult<Status> result = Fitness.SensorsApi.add(
                        mClient,
                        sr,
                        mListener);
                Log.i(TAG, result.toString());
                result.setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        Log.i(TAG, "Result received " + status.toString());
                    }
                });
                Fitness.SensorsApi.findDataSources(mClient, new DataSourcesRequest.Builder()
                    // At least one datatype must be specified.
                    .setDataTypes(DataType.TYPE_HEART_RATE_BPM)
                    // Can specify whether data type is raw or derived.
                    .setDataSourceTypes(DataSource.TYPE_RAW)
                    .build())
                    .setResultCallback(new ResultCallback<DataSourcesResult>() {
                        @Override
                        public void onResult(DataSourcesResult dataSourcesResult) {
                            Log.d(TAG, "Result: " + dataSourcesResult.getStatus().toString());
                            for (DataSource dataSource : dataSourcesResult.getDataSources()) {
                                Log.d(TAG, "Data source found: " + dataSource.toString());
                                Log.d(TAG, "Data Source type: " + dataSource.getDataType().getName());

                                //Let's register a listener to receive Activity data!
                                if (dataSource.getDataType().equals(DataType.TYPE_HEART_RATE_BPM)
                                        && mListener == null) {
                                    Log.i(TAG, "Data source for TYPE_HEART_RATE_BPM found!  Registering.");
                                    registerFitnessDataListener(dataSource,
                                            DataType.TYPE_HEART_RATE_BPM);
                                }
                            }
                        }
                    });
            }
        });
    }

    private boolean registerFitnessDataListener(DataSource dataSource, DataType dataType) {
        mListener = new OnDataPointListener() {
            @Override
            public void onDataPoint(DataPoint dataPoint) {
                for (Field field : dataPoint.getDataType().getFields()) {
                    Value val = dataPoint.getValue(field);
                    Log.i(TAG, "Detected DataPoint field: " + field.getName());
                    Log.i(TAG, "Detected DataPoint value: " + val);
                }
            }
        };

        Fitness.SensorsApi.add(
                mClient,
                new SensorRequest.Builder()
                        .setDataSource(dataSource) // Optional but recommended for custom data sets.
                        .setDataType(dataType) // Can't be omitted.
                        .setSamplingRate(1, TimeUnit.SECONDS)
                        .build(),
                mListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Listener registered!");
                        } else {
                            Log.i(TAG, "Listener not registered.");
                        }
                    }
                });
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_hrview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
