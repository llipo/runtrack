package cz.tmartinik.runtrack;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEventCallback;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;

import com.tbruyelle.rxpermissions.RxPermissions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cz.tmartinik.runtrack.logic.bus.RxBus;
import cz.tmartinik.runtrack.logic.event.TrackingStateEvent;
import cz.tmartinik.runtrack.ui.StartFragment;
import cz.tmartinik.runtrack.ui.TrackingFragment;
import rx.Subscription;
import rx.functions.Action1;

public class MainActivity extends WearableActivity {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm:ss", Locale.US);
    private static final String TAG = "BLE";

    private View mContainerView;
    private boolean mUpdating = false;
    public boolean mGranted = false;

    // A reference to the service used to get location updates.
    private TrackingService mService = null;

    // Tracks the bound state of the service.
    private boolean mBound = false;
    // Monitors the state of the connection to the service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            TrackingService.LocalBinder binder = (TrackingService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mUpdating = mService.isUpdating();
            if (mUpdating) {
                Log.d(TAG, "Service updating");
                //TODO: show normal UI
                getFragmentManager().beginTransaction().replace(R.id.container, new TrackingFragment(), "tracking").commit();
            } else {
                Log.d(TAG, "Service not updating");
                //TODO:
                getFragmentManager().beginTransaction().replace(R.id.container, new StartFragment(), "start").commit();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };
    private Sensor mStepDetectorSensor;
    private SensorEventCallback mStepListener;
    private int mSteps = -1;
    private SensorManager mSensorManager;
    private List<Subscription> mRegistrations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = findViewById(R.id.container);
        RxPermissions rxPermissions = new RxPermissions(MainActivity.this);
        rxPermissions
                .request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BODY_SENSORS, Manifest.permission.BLUETOOTH)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        // Bind to the service. If the service is in foreground mode, this signals to the service
                        // that since this activity is in the foreground, the service can exit foreground mode.
                        if (granted) {
                            mGranted = granted;
                        } else {
                            //TODO: Show error
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mService == null) {
            bindService(new Intent(MainActivity.this, TrackingService.class), mServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

//    private void registerStepsSensor() {
//        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)) {
//            mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
//            mStepDetectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
//            mStepListener = new SensorEventCallback() {
//                @Override
//                public void onSensorChanged(SensorEvent event) {
//                    if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
//                        if (mSteps == -1) {
//                            mSteps = -(int) event.values[0];
//                        }
//                        mSteps += event.values[0];
//                        String msg = "" + mSteps;
//                        mTextView.setText(msg);
//                        Log.d("Steps", msg);
//                    } else
//                        Log.d(TAG, "Unknown sensor type");
//                }
//            };
//            mSensorManager.registerListener(mStepListener, mStepDetectorSensor, mSensorManager.SENSOR_DELAY_FASTEST);
//        } else {
//            Log.d(TAG, "No steps detector");
//        }
//    }


    @Override
    protected void onResume() {
        super.onResume();
        register(TrackingStateEvent.class, event -> {
            MainActivity.this.runOnUiThread(() -> handleTrackingEvent(event));
        });
    }

    public <T> void register(Class<T> eventClass, Action1<T> eventAction) {
        mRegistrations.add(RxBus.getInstance().register(eventClass, eventAction));
    }

    private void handleTrackingEvent(TrackingStateEvent event) {
        switch (event.getAction()) {
            case START:
                getFragmentManager().beginTransaction().replace(R.id.container, new TrackingFragment(), "tracking").commit();
                break;
            case STOP:
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        for (Subscription s : mRegistrations) {
            s.unsubscribe();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (mUpdating) {
            if (isAmbient()) {
                mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
                //TODO: Delegate to fragments
            } else {
                mContainerView.setBackground(null);
            }
        }
    }
}
