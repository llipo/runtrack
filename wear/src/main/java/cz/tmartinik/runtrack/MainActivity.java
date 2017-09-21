package cz.tmartinik.runtrack;

import android.Manifest;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEventCallback;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.tbruyelle.rxpermissions.RxPermissions;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.tmartinik.runtrack.logic.bus.RxBus;
import cz.tmartinik.runtrack.logic.event.TrackingStateEvent;
import cz.tmartinik.runtrack.ui.StartFragment;
import cz.tmartinik.runtrack.ui.TrackingFragment;
import cz.tmartinik.runtrack.ui.TrackingServiceFragment;
import rx.Subscription;
import rx.functions.Action1;

public class MainActivity extends WearableActivity implements
        MenuItem.OnMenuItemClickListener, WearableNavigationDrawerView.OnItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.container) View mContainerView;
    @BindView(R.id.bottom_action_drawer) WearableActionDrawerView mActionDrawer;


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
                //tracking in progress
                Log.d(TAG, "Service updating");
                switchFragment(new TrackingFragment(), "tracking");
            } else {
                //Show start
                Log.d(TAG, "Service not updating");
                switchFragment(new StartFragment(), "start");
            }
            //TODO: paused
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    private void switchFragment(Fragment fragment, String tag) {
        getFragmentManager().beginTransaction().replace(R.id.container, fragment, tag).commit();
        if (fragment instanceof TrackingServiceFragment) {
            prepareActionMenu((TrackingServiceFragment) fragment);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() ) {
            case (MotionEvent.ACTION_DOWN):
                Log.d(TAG, "Action was DOWN");
                mActionDrawer.getController().peekDrawer();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void prepareActionMenu(TrackingServiceFragment fragment) {
        TrackingServiceFragment menuFragment = fragment;
        if (menuFragment.getMenuResource() != 0) {
            Menu menu = mActionDrawer.getMenu();
            getMenuInflater().inflate(menuFragment.getMenuResource(), menu);
            mActionDrawer.getController().peekDrawer();
            mActionDrawer.setIsLocked(false);
            mActionDrawer.setOnMenuItemClickListener(menuFragment);

        }else{
            mActionDrawer.setLockedWhenClosed(true);
        }
    }

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

        ButterKnife.bind(this);
        requestPermissions();


    }

    private void requestPermissions() {
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

    @Override
    protected void onResume() {
        super.onResume();
        register(TrackingStateEvent.class, event -> {
            handleTrackingEvent(event);
        });
    }

    public <T> void register(Class<T> eventClass, Action1<T> eventAction) {
        mRegistrations.add(RxBus.getInstance().register(eventClass, eventAction));
    }

    private void handleTrackingEvent(TrackingStateEvent event) {
        switch (event.getAction()) {
            case START:
                switchFragment(new TrackingFragment(), "tracking");
                break;
            case STOP:
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRegistrations.forEach(Subscription::unsubscribe);
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

    @Override
    public void onItemSelected(int pos) {

    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return false;
    }
}
