package cz.tmartinik.runtrack.ui;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import cz.tmartinik.runtrack.TrackingService;
import cz.tmartinik.runtrack.logic.bus.RxBus;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by Tom on 20. 9. 2017.
 */

public class TrackingServiceFragment extends Fragment {

    private TrackingService mService;
    private ServiceConnection mServiceConnection;

    private List<Subscription> mRegistrations = new ArrayList<>();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                Log.d(StartFragment.TAG, "Service connected");
                TrackingService.LocalBinder binder = (TrackingService.LocalBinder) service;
                mService = binder.getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mService = null;
            }
        };
        getContext().bindService(new Intent(getActivity(), TrackingService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mService.unbindService(mServiceConnection);
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
}
