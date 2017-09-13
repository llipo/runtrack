package runtrack.tmartinik.cz.runtrack;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wear.widget.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends WearableActivity {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm:ss", Locale.US);
    private static final String TAG = "WATCH";
    private static final int REQUEST_PERMISSIONS_REQUEST_LOCATION = 1;
    private static final int REQUEST_PERMISSIONS_REQUEST_SENSORS = 2;

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;
    private TextView mDistanceView;
    private TextView mHrView;


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
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

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

        // Check that the user hasn't revoked permissions by going to Settings.
        if (!checkPermissions(Manifest.permission.ACCESS_FINE_LOCATION) || !checkPermissions(Manifest.permission.BODY_SENSORS)) {
            requestPermissions(REQUEST_PERMISSIONS_REQUEST_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,  Manifest.permission.BODY_SENSORS);
        }

        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(new Intent(this, LocationUpdatesService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    private boolean checkPermissions(String permission) {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                permission);
    }

    private void requestPermissions(int code, String... permissions) {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");

            ActivityCompat.requestPermissions(MainActivity.this,
                    permissions,
                    code);
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    permissions,
                    code);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                mService.requestLocationUpdates();
            } else {
                // Permission denied.
                // Build intent that displays the App settings screen.
                Intent intent = new Intent();
                intent.setAction(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package",
                        BuildConfig.APPLICATION_ID, null);
                intent.setData(uri);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
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

    private boolean hasGps() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
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
