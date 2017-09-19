package cz.tmartinik.runtrack;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventCallback;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.wear.widget.BoxInsetLayout;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tbruyelle.rxpermissions.RxPermissions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cz.tmartinik.runtrack.logic.bus.RxBus;
import cz.tmartinik.runtrack.logic.event.TrackingEvent;
import cz.tmartinik.runtrack.logic.event.TrackingHrEvent;
import cz.tmartinik.runtrack.logic.event.TrackingStateEvent;
import cz.tmartinik.runtrack.ui.StartFragment;
import rx.Subscription;
import rx.functions.Action1;

public class MainActivity extends WearableActivity implements StartFragment.OnFragmentInteractionListener {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm:ss", Locale.US);
    private static final String TAG = "BLE";

    private BoxInsetLayout mContainerView;
    private View container;
    private TextView mTextView;
    private TextView mClockView;
    private TextView mDistanceView;
    private TextView mHrView;
    private boolean mUpdating = false;
    public boolean mGranted = false;
    public boolean connected = false;

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
    private Subscription mReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        mClockView = (TextView) findViewById(R.id.clock);
        mDistanceView = (TextView) findViewById(R.id.distance);
        mHrView = (TextView) findViewById(R.id.hr);

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
                            mClockView.setText("Permissions missing");
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

    private void registerStepsSensor() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)) {
            mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
            mStepDetectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            mStepListener = new SensorEventCallback() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                        if (mSteps == -1) {
                            mSteps = -(int) event.values[0];
                        }
                        mSteps += event.values[0];
                        String msg = "" + mSteps;
                        mTextView.setText(msg);
                        Log.d("Steps", msg);
                    } else
                        Log.d(TAG, "Unknown sensor type");
                }
            };
            mSensorManager.registerListener(mStepListener, mStepDetectorSensor, mSensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Log.d(TAG, "No steps detector");
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mReg = RxBus.getInstance().register(TrackingEvent.class, event -> {
            MainActivity.this.runOnUiThread(() -> handleTrackingEvent(event));
        });
        RxBus.getInstance().register(TrackingHrEvent.class, event -> {
            MainActivity.this.runOnUiThread(() -> handleTrackingEvent(event));
        });
    }

    private void handleTrackingEvent(TrackingEvent event) {
        if(event instanceof TrackingStateEvent) {
            switch (((TrackingStateEvent) event).getAction()) {
                case START:
                    break;
                case STOP:
            }
        }else if(event instanceof TrackingHrEvent){
            mHrView.setText(((TrackingHrEvent) event).getHr());
        }
        //TODO handle tracking events
    }

    @Override
    protected void onPause() {
        super.onPause();
        mReg.unsubscribe();
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
                mTextView.setTextColor(getResources().getColor(android.R.color.white));
                mClockView.setVisibility(View.VISIBLE);

                mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
            } else {
                mContainerView.setBackground(null);
                mTextView.setTextColor(getResources().getColor(android.R.color.black));
                mClockView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onTrackingStarted() {

    }
}
