package runtrack.tmartinik.cz.runtrack;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.wear.widget.BoxInsetLayout;
import android.support.wearable.activity.WearableActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanResult;
import com.polidea.rxandroidble.scan.ScanSettings;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import rx.Subscription;
import rx.functions.Action1;

public class MainActivity extends WearableActivity {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm:ss", Locale.US);

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;
    private TextView mDistanceView;
    private TextView mHrView;
    private boolean mUpdating = false;


    // A reference to the service used to get location updates.
    private LocationUpdatesService mService = null;

    private MyReceiver myReceiver;

    // Tracks the bound state of the service.
    private boolean mBound = false;
    // Monitors the state of the connection to the service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
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

        RxPermissions rxPermissions = new RxPermissions(MainActivity.this);
        rxPermissions
                .request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BODY_SENSORS, Manifest.permission.BLUETOOTH)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        if (granted) {
                            // Bind to the service. If the service is in foreground mode, this signals to the service
                            // that since this activity is in the foreground, the service can exit foreground mode.
//                            bindService(new Intent(MainActivity.this, LocationUpdatesService.class), mServiceConnection,
//                                    Context.BIND_AUTO_CREATE);
                            scanDevices();

// When done, just unsubscribe.

                        } else {
                            mClockView.setText("Permissions missing");
                        }
                    }
                });
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
                        Log.d("BLE",device.getMacAddress() +"; " +device.getName()+"; " +device.getConnectionState() +"; " + device.getBluetoothDevice().getType() +);
                        mTextView.setText("Found devices");
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScanSubscription.unsubscribe();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mService.removeLocationUpdates();
//        unbindService(mServiceConnection);
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
     * Receiver for broadcasts sent by {@link LocationUpdatesService}.
     */
    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
            if (location != null) {
                mTextView.setText(Utils.getLocationText(location));
            }
        }
    }


}
