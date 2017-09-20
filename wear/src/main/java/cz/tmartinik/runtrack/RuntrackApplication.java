package cz.tmartinik.runtrack;

import android.app.Application;

import net.danlew.android.joda.JodaTimeAndroid;

/**
 * Created by tmartinik on 20.9.2017.
 */

public class RuntrackApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        JodaTimeAndroid.init(this);
    }
}
