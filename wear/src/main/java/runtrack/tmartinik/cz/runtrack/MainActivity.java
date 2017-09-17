package runtrack.tmartinik.cz.runtrack;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventCallback;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.wear.widget.BoxInsetLayout;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.helpers.ValueInterpreter;
import com.polidea.rxandroidble.scan.ScanResult;
import com.polidea.rxandroidble.scan.ScanSettings;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import rx.Subscription;
import rx.functions.Action1;

public class MainActivity extends WearableActivity {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm:ss", Locale.US);
    private static final String TAG = "BLE";

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;
    private TextView mDistanceView;
    private TextView mHrView;
    private boolean mUpdating = false;
    public boolean mGranted = false;
    public boolean connected = false;

    private SensorEventCallback mSensorListener;


    // A reference to the service used to get location updates.
    private TrackingService mService = null;

    private MyReceiver myReceiver;

    // Tracks the bound state of the service.
    private boolean mBound = false;
    // Monitors the state of the connection to the service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TrackingService.LocalBinder binder = (TrackingService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mUpdating = mService.isUpdating();
            if (mUpdating) {
                //TODO:
            } else {
                mService.requestLocationUpdates();
                //TODO:
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };
    private Subscription mScanSubscription;
    private Sensor mHeartRateSensor;
    private SensorManager mSensorManager;
    private Sensor mStepDetectorSensor;
    private SensorEventCallback mStepListener;
    private int mSteps = -1;

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

        myReceiver = new MyReceiver();
        registerReceiver(myReceiver, new IntentFilter(TrackingService.ACTION_BROADCAST));

        RxPermissions rxPermissions = new RxPermissions(MainActivity.this);
        rxPermissions
                .request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BODY_SENSORS, Manifest.permission.BLUETOOTH)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        if (granted) {
                            mGranted = granted;
                            // Bind to the service. If the service is in foreground mode, this signals to the service
                            // that since this activity is in the foreground, the service can exit foreground mode.
                            bindService(new Intent(MainActivity.this, TrackingService.class), mServiceConnection,
                                    Context.BIND_AUTO_CREATE);
                        } else {
                            mClockView.setText("Permissions missing");
                        }
                    }
                });
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

    private void registerInternalHrSenzor() {
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        mSensorListener = new SensorEventCallback() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
                    String msg = "" + (int) event.values[0];
                    mHrView.setText(msg);
                    Log.d("Watch HR", msg);
                } else
                    Log.d(TAG, "Unknown sensor type");
            }
        };
        mSensorManager.registerListener(mSensorListener, mHeartRateSensor, mSensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGranted) {
        }
    }

    private void scanDevices() {
        RxBleClient rxBleClient = RxBleClient.create(MainActivity.this);
        // change if needed
// change if needed
// add filters if needed
        mScanSubscription = rxBleClient.scanBleDevices(
                new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // change if needed
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // change if needed
                        .build()
//                , new ScanFilter[]{new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("00002a37-0000-1000-8000-00805f9b34fb")).build()}
                // add filters if needed
        )
                .subscribe(new Action1<ScanResult>() {
                    public void call(ScanResult result) {
                        RxBleDevice device = result.getBleDevice();
                        Log.d("BLE", device.getMacAddress() + "; " + device.getName() + "; " + device.getConnectionState() + "; " + device.getBluetoothDevice().getAddress());
                        mTextView.setText("Found devices");
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver);
        mService.removeLocationUpdates();
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

    /**
     * Receiver for broadcasts sent by {@link TrackingService}.
     */
    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(TrackingService.EXTRA_LOCATION);
            Integer hr = intent.getParcelableExtra(TrackingService.EXTRA_HR);
            if (location != null) {
                MainActivity.this.runOnUiThread(() ->
                        mTextView.setText(Utils.getLocationText(location))
                );
            }

            if (hr != null) {
                MainActivity.this.runOnUiThread(() ->
                        mHrView.setText(hr + " Bpm")
                );

            }
        }
    }


}
