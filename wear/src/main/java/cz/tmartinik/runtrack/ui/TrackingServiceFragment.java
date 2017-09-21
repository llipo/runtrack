package cz.tmartinik.runtrack.ui;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import cz.tmartinik.runtrack.TrackingService;
import cz.tmartinik.runtrack.logic.bus.RxBus;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by Tom on 20. 9. 2017.
 */

public class TrackingServiceFragment extends Fragment implements MenuItem.OnMenuItemClickListener {

    private TrackingService mService;
    private ServiceConnection mServiceConnection;
    private boolean mBound;

    private List<Subscription> mRegistrations = new ArrayList<>();

    @Override
    public void onStart() {
        super.onStart();
        bindService();
    }

    @Override
    public void onStop() {
        super.onStop();
        unBindService();
    }

    protected void onServiceConnected(TrackingService service){
    }

    public int getMenuResource(){
        return 0;
    }

    protected void onServiceDisconnected(){
    }

    private void bindService() {
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                Log.d(StartFragment.TAG, "Service connected");
                TrackingService.LocalBinder binder = (TrackingService.LocalBinder) service;
                mService = binder.getService();
                TrackingServiceFragment.this.onServiceConnected(mService);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mService = null;
                TrackingServiceFragment.this.onServiceDisconnected();
            }
        };
        mBound = getActivity().bindService(new Intent(getActivity(), TrackingService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void unBindService() {
        if(mBound) {
            getActivity().unbindService(mServiceConnection);
            mBound = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mRegistrations.forEach(s -> s.unsubscribe());
    }

    public TrackingService getService() {
        return mService;
    }

    public <T> void register(Class<T> eventClass, Action1<T> eventAction) {
        mRegistrations.add(RxBus.getInstance().register(eventClass, eventAction));
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return false;
    }
}
