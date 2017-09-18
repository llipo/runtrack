package cz.tmartinik.runtrack.logic.sensor;

import android.content.Context;

/**
 * Created by tmartinik on 18.9.2017.
 */

public interface SensorProvider<T extends SensorEvent> {
    void register(Context context, SensorListener<T> listener);

    void unregister();
}
