package io.github.matt_porter.biosporthrservice;

//Stuff from example: https://developers.google.com/fit/android/new-sensors
//import com.google.android.gms.common.*;
//import com.google.android.gms.common.api.*;
//import com.google.android.gms.fitness.*;
import android.content.Context;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.fitness.data.*;
import com.google.android.gms.fitness.service.*;

import com.intel.heartratecore.api.HeartRateCore;
import com.intel.heartratecore.api.HeartRateCoreListener;
import com.intel.heartratecore.api.HeartRateCoreStatus;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by matt on 07/09/16.
 */

public class BioSportHrSensorService extends FitnessSensorService implements HeartRateCoreListener {
    private HeartRateCore hrc;
    private FitnessSensorServiceRequest request;
    private String tag = "BioSportHrSensorService";
    private DataSource datasource = new DataSource.Builder()
            .setDataType(DataType.TYPE_HEART_RATE_BPM)
            .setType(DataSource.TYPE_RAW)
            .build();

    /*
        Google fit/sensor stuff first.
     */

    @Override
    public void onCreate() {
        Log.d(this.tag, "onCreate");
        super.onCreate();
        this.hrc = HeartRateCore.getInstance(this, this);
        Log.d(this.tag, "starting in onCreate");
        this.hrc.start();
        Log.d(this.tag, "Done onCreate");
        // 1. Initialize your software sensor(s).
        // 2. Create DataSource representations of your software sensor(s).
        // 3. Initialize some data structure to keep track of a registration for each sensor.

    }

    @Override
    public List<DataSource> onFindDataSources(List<DataType> dataTypes) {
        Log.d(this.tag, "onFindDataSources");

        // 1. Find which of your software sensors provide the data types requested.
        // 2. Return those as a list of DataSource objects.
        List<DataSource> dataSources = new ArrayList<>();
        dataSources.add(this.datasource);
        return dataSources;
    }

    @Override
    public boolean onRegister(FitnessSensorServiceRequest request) {
        // 1. Determine which sensor to register with request.getDataSource().
        // 2. If a registration for this sensor already exists, replace it with this one.
        // 3. Keep (or update) a reference to the request object.
        // 4. Configure your sensor according to the request parameters.
        // 5. When the sensor has new data, deliver it to the platform by calling
        //    request.getDispatcher().publish(List<DataPoint> dataPoints)
        Log.d(this.tag, "onRegister");
        if (request.getDataSource() == this.datasource) {
            Log.d(this.tag, "Starting...");
            this.hrc.start();
            this.request = request;
        }
        Log.d(this.tag, "Done onRegister");
        return true;
    }

    @Override
    public boolean onUnregister(DataSource dataSource) {
        // 1. Configure this sensor to stop delivering data to the platform
        // 2. Discard the reference to the registration request object
        Log.d(this.tag, "onUnregister::"+ dataSource.toString());
        this.hrc.stop();
        this.request = null;
        return true;
    }

    /*
     * Intel related stuff below
     */

    @Override
    public void onStatusChanged(@NonNull EnumSet<HeartRateCoreStatus> enumSet) {
        Log.d(this.tag, "onStatusChanged::" + enumSet.toString());
    }

    @Override
    public void onHeartRateChanged(int i) {
        Log.d(this.tag, "Heart rate changed: " + i);
        DataPoint dp = DataPoint.create(this.datasource);
        dp.setTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        dp.getValue(Field.FIELD_BPM).setFloat((float)i);
        List<DataPoint> dataPoints = new ArrayList();
        dataPoints.add(dp);
//        try {
//            this.request.getDispatcher().publish(dataPoints);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
        Log.d(this.tag, "Done HR change");
    }
}

