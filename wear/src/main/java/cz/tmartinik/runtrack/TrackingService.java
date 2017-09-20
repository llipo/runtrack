package cz.tmartinik.runtrack;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.joda.time.Duration;

import java.util.concurrent.atomic.AtomicBoolean;

import cz.tmartinik.runtrack.logic.bus.RxBus;
import cz.tmartinik.runtrack.logic.event.TrackingHrEvent;
import cz.tmartinik.runtrack.logic.event.TrackingLocationEvent;
import cz.tmartinik.runtrack.logic.event.TrackingStateEvent;
import cz.tmartinik.runtrack.logic.sensor.HrSensorEvent;
import cz.tmartinik.runtrack.logic.sensor.HrSensorProvider;
import cz.tmartinik.runtrack.logic.sensor.InternalHrSensorProvider;
import cz.tmartinik.runtrack.logic.sensor.SensorListener;
import cz.tmartinik.runtrack.logic.store.MemoryTrackingStore;
import cz.tmartinik.runtrack.logic.store.TrackingStore;
import cz.tmartinik.runtrack.model.Tempo;
import cz.tmartinik.runtrack.model.Track;
import cz.tmartinik.runtrack.model.TrackSegment;


/**
 * A bound and started service that is promoted to a foreground service when location updates have
 * been requested and all clients unbind.
 * <p>
 * For apps running in the background on "O" devices, location is computed only once every 10
 * minutes and delivered batched every 30 minutes. This restriction applies even to apps
 * targeting "N" or lower which are run on "O" devices.
 * <p>
 * This sample show how to use a long-running service for location updates. When an activity is
 * bound to this service, frequent location updates are permitted. When the activity is removed
 * from the foreground, the service promotes itself to a foreground service, and location updates
 * continue. When the activity comes back to the foreground, the foreground service stops, and the
 * notification assocaited with that service is removed.
 */
public class TrackingService extends Service implements SensorListener<HrSensorEvent> {

    private static final String PACKAGE_NAME =
            "cz.tmartinik.runtrack";

    private static final String TAG = TrackingService.class.getSimpleName();

    public static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";

    static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
            ".started_from_notification";
    static final String EXTRA_HR = PACKAGE_NAME + ".heartRate";

    private AtomicBoolean mUpdating = new AtomicBoolean(false);

    private final IBinder mBinder = new LocalBinder();

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 4000;

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * The identifier for the notification displayed for the foreground service.
     */
    private static final int NOTIFICATION_ID = 12345678;

    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     */
    private boolean mChangingConfiguration = false;

    private NotificationManager mNotificationManager;

    /**
     * Contains parameters used by {@link com.google.android.gms.location.FusedLocationProviderApi}.
     */
    private LocationRequest mLocationRequest;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Callback for changes in location.
     */
    private LocationCallback mLocationCallback;

    private Handler mServiceHandler;

    /**
     * The current location.
     */
    private Location mLocation;
    private HrSensorProvider mHrProvider;
    private TrackingStore mStore;
    private TrackSegment mActualSegment;

    private Integer mLastHr;

    public TrackingService() {
    }

    @Override
    public void onCreate() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        mStore = MemoryTrackingStore.getInstance();
        createLocationRequest();
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started");
        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,
                false);

        // We got here because the user decided to remove location updates from the notification.
        if (startedFromNotification) {
            stopTracking();
            stopSelf();
        }

        if(mActualSegment == null) {
            mActualSegment = mStore.track().addSegment();
            mActualSegment.start();
        }
        busPost(new TrackingStateEvent(TrackingStateEvent.Action.START));

        // Tells the system to not try to recreate the service after it has been killed.
        return START_NOT_STICKY;
    }

    private void busPost(Object event) {
        RxBus.getInstance().post(event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Log.i(TAG, "in onBind()");
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "in onRebind()");
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Last client unbound from service");

        // Called when the last client (MainActivity in case of this sample) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
        if (!mChangingConfiguration && Utils.requestingLocationUpdates(this)) {
            Log.i(TAG, "Starting foreground service");
            startForeground(NOTIFICATION_ID, getNotification());
        }
        return true; // Ensures onRebind() is called when a client re-binds.
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Makes a request for location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void startTracking() {
        Log.i(TAG, "Requesting location updates");
        Utils.setRequestingLocationUpdates(this, true);
        startService(new Intent(getApplicationContext(), TrackingService.class));
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
            mUpdating.set(true);

            //TODO: Create request with hrMonitor, etc
//            mHrProvider = new BluetoothHrSensorProvider("40:00:00:00:11:C0");
            mHrProvider = new InternalHrSensorProvider();
            mHrProvider.register(this, this);

        } catch (SecurityException unlikely) {
            Utils.setRequestingLocationUpdates(this, false);
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    /**
     * Removes location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void stopTracking() {
        Log.i(TAG, "Removing location updates");
        try {
            mActualSegment.stop();
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            mUpdating.set(false);
            mHrProvider.unregister();
            Utils.setRequestingLocationUpdates(this, false);
            busPost(new TrackingStateEvent(TrackingStateEvent.Action.STOP));
            stopSelf();
        } catch (SecurityException unlikely) {
            Utils.setRequestingLocationUpdates(this, true);
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    public Track finishTracking(){
       return mStore.finish();
    }

    public boolean isUpdating() {
        return mUpdating.get();
    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
    private Notification getNotification() {
        Intent intent = new Intent(this, TrackingService.class);

        CharSequence text = Utils.getLocationText(mLocation);

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // The PendingIntent to launch activity.
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        return new NotificationCompat.Builder(this)
//                .addAction(R.drawable.ic_launch, getString(R.string.launch_activity),
//                        activityPendingIntent)
//                .addAction(R.drawable.ic_cancel, getString(R.string.remove_location_updates),
//                        servicePendingIntent)
                .setContentText(text)
                .setContentTitle(Utils.getLocationTitle(this))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(text)
                .setWhen(System.currentTimeMillis()).build();
    }

    private void onNewLocation(Location location) {
        Log.i(TAG, "New location: " + location);

        mLocation = location;
        mActualSegment.add(location, mLastHr);
        RxBus.getInstance().post(new TrackingLocationEvent(mActualSegment.getDistance(), new Tempo(location.getSpeed())));

        // Update notification content if running as a foreground service.
        if (serviceIsRunningInForeground(this)) {
            mNotificationManager.notify(NOTIFICATION_ID, getNotification());
        }
    }

    /**
     * Sets the location request parameters.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onSensorEvent(HrSensorEvent event) {
        switch (event.getType()) {
            case DATA:
                this.mLastHr = event.getHeartRate();
                RxBus.getInstance().post(new TrackingHrEvent(event.getHeartRate()));
                break;
        }
    }

    public Duration getElapsedTime() {
        return mStore.getElapsedTime();
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public TrackingService getService() {
            return TrackingService.this;
        }
    }

    /**
     * Returns true if this is a foreground service.
     *
     * @param context The {@link Context}.
     */
    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }
}
